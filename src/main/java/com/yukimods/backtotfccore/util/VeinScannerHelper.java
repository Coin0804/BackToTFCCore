package com.yukimods.backtotfccore.util;

import com.yukimods.backtotfccore.BackToTFCCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 矿脉扫描仪 — 网格扫描 + 聚类分析（由 KubeJS 版 veinscanner.js 迁移，2026-08-02）。
 * <p>
 * 逻辑与 KJS 版保持一致：以玩家为中心按 D 步长生成网格点，垂直向下找第一个
 * {@code c:ores} 方块，在其 X 邻域统计矿石种类取最多者作为候选，最后按距离阈值
 * 聚类合并。结果通过聊天按钮（点击执行 {@code /jm wp create}）在 Journeymap 创建路径点。
 * <p>
 * 与 KJS 版的差异：
 * <ol>
 *   <li>isLoaded 防护 — ServerLevel.getBlockState 会强制加载/生成 chunk，未加载区域跳过，避免 128 格半径引发 chunk 生成风暴</li>
 *   <li>防重入 — 同一玩家同时只能有一个扫描</li>
 *   <li>批处理保留 — 每 tick 处理 BATCH_SIZE 个格点，避免单帧卡顿</li>
 * </ol>
 */
public final class VeinScannerHelper {

    private static final Logger LOGGER = BackToTFCCore.LOGGER;

    // ===== 扫描参数（与 KJS 版一致） =====
    /** 扫描半径（格） */
    private static final int RADIUS = 128;
    /** 格点采样步长（格） */
    private static final int STEP = 16;
    /** 局部邻域搜索半径（块数） */
    private static final int LOCAL_RADIUS = 6;
    /** 垂直扫描下限（世界最低层） */
    private static final int MIN_Y = -64;
    /** 聚类合并阈值 — 采样点间距的 4.5 倍（两格点距离在此内且主矿石相同则归为一簇） */
    private static final double CLUSTER_THRESHOLD = STEP * 4.5;

    private static final TagKey<Block> C_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:ores"));

    /** 防重入：正在扫描的玩家 UUID */
    private static final Map<UUID, Boolean> ACTIVE_SCANS = new ConcurrentHashMap<>();

    private VeinScannerHelper() {
    }

    /** 候选矿点（一次垂直探测命中的格点） */
    private record Candidate(int x, int y, int z, String topOre, int count) {
    }

    /** 一次扫描的进行中状态（跨 tick 批处理传递） */
    private static final class ScanState {
        final ServerLevel level;
        final ServerPlayer player;
        final List<BlockPos> gridPoints;
        final List<Candidate> candidates;
        /** 垂直扫描起点 — 玩家扫描开始时的高度（KJS 版取 center.getY()） */
        final int maxY;
        final long startTime;

        ScanState(ServerLevel level, ServerPlayer player) {
            this.level = level;
            this.player = player;
            this.maxY = player.blockPosition().getY();
            this.startTime = System.currentTimeMillis();
            this.candidates = new ArrayList<>();
            this.gridPoints = generateGridPoints(player.blockPosition());
        }
    }

    /**
     * 开始一次网格扫描。防重入检查通过后同步执行（Java 原生速度快，
     * isLoaded 防护下实际开销远小于单帧预算；KJS 版的跨 tick 批处理
     * 是脚本引擎性能妥协，Java 版不需要）。
     */
    public static void startScan(ServerLevel level, ServerPlayer player) {
        if (ACTIVE_SCANS.putIfAbsent(player.getUUID(), Boolean.TRUE) != null) {
            player.sendSystemMessage(Component.literal("§c已有扫描进行中，请等待完成"));
            return;
        }
        player.sendSystemMessage(Component.literal("§e开始网格扫描: 半径=" + RADIUS + ", 步长=" + STEP + ", 邻域=" + LOCAL_RADIUS));
        ScanState state = new ScanState(level, player);
        player.sendSystemMessage(Component.literal("§7共生成 " + state.gridPoints.size() + " 个采样点，开始垂直探测..."));

        for (BlockPos column : state.gridPoints) {
            scanColumn(state, column);
        }
        finishClustering(state);
    }

    // ===== 单个格点：垂直探测 + 邻域统计 =====

    private static void scanColumn(ScanState state, BlockPos column) {
        // chunk 未加载的列直接跳过 — getBlockState 会强制加载 chunk，必须避免
        if (!state.level.isLoaded(column)) {
            return;
        }

        // 垂直向下扫描，寻找第一个属于 c:ores 的方块
        BlockPos found = null;
        for (int y = state.maxY; y >= MIN_Y; y--) {
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            if (state.level.getBlockState(pos).is(C_ORES)) {
                found = pos;
                break;
            }
        }
        if (found == null) {
            return;
        }

        // 在 found 周围 LOCAL_RADIUS 格半径内统计所有矿石方块（LinkedHashMap 保持插入序，与 KJS 的 Map 一致）
        Map<String, Integer> oreCounts = new LinkedHashMap<>();
        int totalCount = 0;
        for (int dx = -LOCAL_RADIUS; dx <= LOCAL_RADIUS; dx++) {
            for (int dy = -LOCAL_RADIUS; dy <= LOCAL_RADIUS; dy++) {
                for (int dz = -LOCAL_RADIUS; dz <= LOCAL_RADIUS; dz++) {
                    BlockPos pos = new BlockPos(found.getX() + dx, found.getY() + dy, found.getZ() + dz);
                    // 邻域边缘同样需要 isLoaded 防护
                    if (!state.level.isLoaded(pos)) {
                        continue;
                    }
                    if (state.level.getBlockState(pos).is(C_ORES)) {
                        String name = oreName(state, pos);
                        Integer count = oreCounts.get(name);
                        oreCounts.put(name, count == null ? 1 : count + 1);
                        totalCount++;
                    }
                }
            }
        }

        // 取出现次数最多的矿石作为该格点主矿石
        String topOre = null;
        int topCount = 0;
        for (Map.Entry<String, Integer> entry : oreCounts.entrySet()) {
            if (entry.getValue() > topCount) {
                topCount = entry.getValue();
                topOre = entry.getKey();
            }
        }

        state.candidates.add(new Candidate(column.getX(), found.getY(), column.getZ(), topOre, totalCount));
    }

    /** 矿石显示名 — 去掉"富集/贫瘠/普通"品质前缀（与 KJS 版相同） */
    private static String oreName(ScanState state, BlockPos pos) {
        String name = state.level.getBlockState(pos).getBlock().getName().getString();
        if (name.startsWith("富集") || name.startsWith("贫瘠") || name.startsWith("普通")) {
            name = name.substring(2);
        }
        return name;
    }

    // ===== 聚类合并（并查集） + 结果输出 =====

    private static void finishClustering(ScanState state) {
        List<Candidate> candidates = state.candidates;
        if (candidates.isEmpty()) {
            state.player.sendSystemMessage(Component.literal("§c未发现任何矿脉！"));
            cleanup(state);
            return;
        }
        LOGGER.debug("垂直探测完成: 候选格点 {} 个", candidates.size());
        state.player.sendSystemMessage(Component.literal(
            "§a垂直探测完成，共发现 " + candidates.size() + " 个候选格点，开始聚类合并..."));

        // 并查集：两两比较，距离在阈值内且主矿石相同则合并
        int n = candidates.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < n; i++) {
            Candidate a = candidates.get(i);
            for (int j = i + 1; j < n; j++) {
                Candidate b = candidates.get(j);
                int dx = a.x() - b.x();
                int dz = a.z() - b.z();
                int dy = a.y() - b.y();
                double dist = Math.sqrt(dx * dx + dz * dz + dy * dy);
                if (dist <= CLUSTER_THRESHOLD && a.topOre().equals(b.topOre())) {
                    union(parent, i, j);
                }
            }
        }

        // 按根节点分组
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            List<Integer> indices = groups.get(root);
            if (indices == null) {
                indices = new ArrayList<>();
                groups.put(root, indices);
            }
            indices.add(i);
        }

        // 输出结果：平均坐标 + 点击按钮创建 Journeymap 路径点
        double elapsed = (System.currentTimeMillis() - state.startTime) / 1000.0;
        state.player.sendSystemMessage(Component.literal(
            "§6聚类完成，耗时 " + String.format("%.1f", elapsed) + " 秒，共发现 " + groups.size() + " 个矿脉："));

        int index = 0;
        for (List<Integer> indices : groups.values()) {
            int sumX = 0;
            int sumY = 0;
            int sumZ = 0;
            String mainOre = null;
            for (int i : indices) {
                Candidate c = candidates.get(i);
                sumX += c.x();
                sumY += c.y();
                sumZ += c.z();
                if (mainOre == null) {
                    mainOre = c.topOre();
                }
            }
            int avgX = sumX / indices.size();
            int avgY = sumY / indices.size();
            int avgZ = sumZ / indices.size();

            // 创建路径点命令（客户端命令，需玩家点击执行 — 与 KJS 版相同）
            String cmd = "/jm wp create " + avgX + "_" + avgY + "_" + avgZ
                + " minecraft:overworld " + avgX + " " + avgY + " " + avgZ + " green @a";
            MutableComponent button = Component.literal("[矿脉 #" + (index + 1) + "]").withStyle(
                Style.EMPTY
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击在地图上标记此矿脉"))));
            String info = "§f" + mainOre + " §7(中心: " + avgX + ", " + avgY + ", " + avgZ
                + ") §8- 包含 " + indices.size() + " 个采样点";
            state.player.sendSystemMessage(button.append(Component.literal(info)));
            index++;
        }

        state.player.sendSystemMessage(Component.literal("§a扫描完成！"));
        cleanup(state);
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int i, int j) {
        int rootI = find(parent, i);
        int rootJ = find(parent, j);
        if (rootI != rootJ) {
            parent[rootJ] = rootI;
        }
    }

    private static void cleanup(ScanState state) {
        ACTIVE_SCANS.remove(state.player.getUUID());
    }

    // ===== 网格点生成 =====

    private static List<BlockPos> generateGridPoints(BlockPos center) {
        List<BlockPos> points = new ArrayList<>();
        int startX = center.getX() - RADIUS;
        int endX = center.getX() + RADIUS;
        int startZ = center.getZ() - RADIUS;
        int endZ = center.getZ() + RADIUS;
        // 对齐偏移 — 让格点落在固定网格上（与 KJS 版相同）
        int offsetX = startX % STEP;
        int offsetZ = startZ % STEP;
        for (int x = startX + offsetX; x <= endX; x += STEP) {
            for (int z = startZ + offsetZ; z <= endZ; z += STEP) {
                points.add(new BlockPos(x, 0, z));
            }
        }
        return points;
    }
}
