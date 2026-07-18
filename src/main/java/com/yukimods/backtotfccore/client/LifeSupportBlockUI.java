package com.yukimods.backtotfccore.client;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUIHolder;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import com.yukimods.backtotfccore.BackToTFCCore;
import com.yukimods.backtotfccore.block.LifeSupportBlockEntity;
import com.yukimods.backtotfccore.block.ModBlocks;
import com.yukimods.backtotfccore.util.NutritionCalculator;
import com.yukimods.backtotfccore.util.PlayerFullResetHelper;
import net.dries007.tfc.common.player.IPlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * LDLib2 UI 工厂 — 生命维持设备界面。
 */
public class LifeSupportBlockUI {

    private static final Logger LOGGER = BackToTFCCore.LOGGER;
    private static final String[] NUT_KEYS = {
        "backtotfccore.nutrient.grain", "backtotfccore.nutrient.fruit",
        "backtotfccore.nutrient.vegetables", "backtotfccore.nutrient.protein",
        "backtotfccore.nutrient.dairy"
    };
    private static final int[] NUT_COLORS = {
        0xFFFFAA00, 0xFF55FF55, 0xFF00AA00, 0xFFFF5555, 0xFFAA00AA
    };
    private static final float BARMAX = 1.0f;
    private static final int NUTRIENT_COUNT = 5;

    public static ModularUI createUI(BlockUIHolder holder) {
        var doc = XmlUtils.loadXml(
            ResourceLocation.parse("backtotfccore:ui/life_support.xml"));
        var ui = UI.of(doc);

        // 始终执行 — 客户端也需要创建绑定以匹配 sync ID
        setupNutrientLabels(ui);
        setupNutrientBars(ui, holder.player, holder.pos);

        // 状态消息（按钮点击后显示 3 秒反馈）
        final Component[] statusMsg = new Component[]{Component.empty()};
        final long[] statusUntil = new long[]{0};
        setupStatusLabel(ui, holder.player, statusMsg, statusUntil);

        setupButtons(ui, holder, statusMsg, statusUntil);

        return ModularUI.of(ui, holder.player);
    }

    // =================================================================
    //  营养名（静态）
    // =================================================================
    private static void setupNutrientLabels(UI ui) {
        for (int i = 0; i < NUTRIENT_COUNT; i++) {
            int idx = i;
            var label = findLabel(ui, "nut_" + idx + "_label");
            label.setText(Component.translatable(NUT_KEYS[idx]));
            label.textStyle(ts -> ts.textColor(NUT_COLORS[idx]));
        }
    }

    // =================================================================
    //  进度条 + 比例文字（S2C 绑定，实时更新）
    // =================================================================
    private static void setupNutrientBars(UI ui, Player player, BlockPos pos) {
        for (int i = 0; i < NUTRIENT_COUNT; i++) {
            setupOneBar(ui, player, i, pos);
            setupOneRatio(ui, player, i);
        }
    }

    // 一条进度条
    private static void setupOneBar(UI ui, Player player, int idx, BlockPos pos) {
        var bar = findProgressBar(ui, "nut_" + idx + "_bar");
        bar.setRange(0, 1);

        Supplier<Float> dataSource;
        if (player instanceof ServerPlayer sp) {
            dataSource = () -> {
                float raw = IPlayerInfo.get(sp).nutrition().getNutrients()[idx];
                return Math.min(1.0f, raw / BARMAX);
            };
        } else {
            dataSource = () -> 0.0f;
        }

        var binding = DataBindingBuilder.floatValS2C(dataSource).build();
        bar.bind(binding);

        bar.bar(b -> b.style(s -> s.background(new ColorRectTexture(NUT_COLORS[idx]))));

        // 进度条内部 label 显示 BE 目标值
        Supplier<Component> targetSource;
        if (player instanceof ServerPlayer sp) {
            targetSource = () -> {
                float t = BARMAX;
                if (sp.serverLevel().getBlockEntity(pos) instanceof LifeSupportBlockEntity be) {
                    t = be.getTargets()[idx];
                }
                return Component.translatable("backtotfccore.gui.target_value", String.format("%.1f", t));
            };
        } else {
            targetSource = () -> Component.empty();
        }
        var targetBinding = DataBindingBuilder.componentS2C(targetSource).build();
        bar.label(l -> l.bind(targetBinding));
    }

    // 一条比例文字
    private static void setupOneRatio(UI ui, Player player, int idx) {
        var label = findLabel(ui, "nut_" + idx + "_ratio");

        Supplier<Component> dataSource;
        if (player instanceof ServerPlayer sp) {
            dataSource = () -> {
                float v = IPlayerInfo.get(sp).nutrition().getNutrients()[idx];
                return Component.literal(String.format("%.1f/%.1f", v, BARMAX));
            };
        } else {
            dataSource = () -> Component.empty();
        }

        var binding = DataBindingBuilder.componentS2C(dataSource).build();
        label.bind(binding);
    }

    // =================================================================
    //  状态消息 — 按钮点击后显示 3 秒
    // =================================================================
    private static void setupStatusLabel(UI ui, Player player,
                                          Component[] statusMsg, long[] statusUntil) {
        var label = findLabel(ui, "status_label");

        Supplier<Component> dataSource;
        if (player instanceof ServerPlayer) {
            dataSource = () -> {
                if (System.currentTimeMillis() < statusUntil[0]) {
                    return statusMsg[0];
                }
                return Component.empty();
            };
        } else {
            dataSource = () -> Component.empty();
        }

        var binding = DataBindingBuilder.componentS2C(dataSource).build();
        label.bind(binding);
    }

    // =================================================================
    //  按钮
    // =================================================================
    private static void setupButtons(UI ui, BlockUIHolder holder,
                                      Component[] statusMsg, long[] statusUntil) {
        final var player = holder.player;
        final var pos = holder.pos;
        final var level = player.level();

        // ── 记录重生点 ──
        var btnRecord = findButton(ui, "btn_record");
        btnRecord.setText(Component.translatable("backtotfccore.gui.record"));
        btnRecord.setOnServerClick(event -> {
            if (player instanceof ServerPlayer sp) {
                sp.setRespawnPosition(level.dimension(), pos, sp.getYRot(), false, true);
                LOGGER.info("[LifeSupport] Record: respawn set for {}, dim={}, pos={}",
                    player.getName().getString(), level.dimension(), pos);
                statusMsg[0] = Component.translatable("backtotfccore.gui.record.done");
                statusUntil[0] = System.currentTimeMillis() + 3000;
            }
        });

        // ── 恢复状态 ──
        var btnRestore = findButton(ui, "btn_restore");
        btnRestore.setText(Component.translatable("backtotfccore.gui.restore"));
        btnRestore.setOnServerClick(event -> {
            if (player instanceof ServerPlayer sp) {
                // 从方块实体读取目标值，无 NBT 时默认全 0.5
                float[] targets = {0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
                if (level.getBlockEntity(pos) instanceof LifeSupportBlockEntity be) {
                    targets = be.getTargets();
                }

                var foodNbt = NutritionCalculator.buildFoodNbt(
                    new float[]{targets[0], targets[1], targets[2],
                                targets[3], targets[4]});
                PlayerFullResetHelper.resetPlayer(sp, foodNbt);

                statusMsg[0] = Component.translatable("backtotfccore.gui.restore.done");
                statusUntil[0] = System.currentTimeMillis() + 3000;
            }
        });

        // ── 收起 ──
        var btnCollect = findButton(ui, "btn_collect");
        btnCollect.setText(Component.translatable("backtotfccore.gui.collect"));
        btnCollect.setOnServerClick(event -> {
            LOGGER.info("[LifeSupport] Collect: destroying block at {}, player={}",
                pos, player.getName().getString());

            // 保存 BE 数据到掉落物，放置后可恢复
            ItemStack stack = new ItemStack(ModBlocks.LIFE_SUPPORT_DEVICE.get());
            if (level.getBlockEntity(pos) instanceof LifeSupportBlockEntity be) {
                CompoundTag tag = new CompoundTag();
                tag.putString("id", "backtotfccore:life_support_device");
                be.writeTargetsToTag(tag);
                stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
            }

            level.destroyBlock(pos, false);
            Block.popResource(level, pos, stack);
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
            }
        });
    }

    // =================================================================
    //  查找控件 — 缺失时报错
    // =================================================================
    private static Label findLabel(UI ui, String id) {
        var opt = ui.selectId(id, Label.class).findFirst();
        if (opt.isEmpty()) {
            LOGGER.error("[LifeSupportUI] Missing Label: id={}", id);
        }
        return opt.orElseThrow(() ->
            new IllegalStateException("LifeSupport UI: missing Label #" + id));
    }

    private static Button findButton(UI ui, String id) {
        var opt = ui.selectId(id, Button.class).findFirst();
        if (opt.isEmpty()) {
            LOGGER.error("[LifeSupportUI] Missing Button: id={}", id);
        }
        return opt.orElseThrow(() ->
            new IllegalStateException("LifeSupport UI: missing Button #" + id));
    }

    private static ProgressBar findProgressBar(UI ui, String id) {
        var opt = ui.selectId(id, ProgressBar.class).findFirst();
        if (opt.isEmpty()) {
            LOGGER.error("[LifeSupportUI] Missing ProgressBar: id={}", id);
        }
        return opt.orElseThrow(() ->
            new IllegalStateException("LifeSupport UI: missing ProgressBar #" + id));
    }
}
