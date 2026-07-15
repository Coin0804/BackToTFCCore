package com.yukimods.backtotfccore.util;

import net.dries007.tfc.common.component.food.INutritionData;
import net.dries007.tfc.common.player.IPlayerInfo;
import net.dries007.tfc.common.player.PlayerInfo;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家状态重置工具类 — 由方块/物品调用者传入 NBT 格式的食物记录。
 * <p>
 * 直接调用 TFC 原版 {@link INutritionData#readFromNbt(net.minecraft.nbt.Tag)} 清空旧食物记录，
 * 只保留传入的虚拟食物，避免旧记录干扰营养计算。
 */
public class PlayerFullResetHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerFullResetHelper");

    /**
     * 重置玩家所有状态至完美值。
     *
     * @param player   服务端玩家对象
     * @param foodNbt  ListTag 格式的食物记录（直接传给 readFromNbt）
     */
    public static void resetPlayer(Player player, ListTag foodNbt) {
        if (player.level().isClientSide()) return;
        // 2. 回满生命
        player.setHealth(player.getMaxHealth());

        // 3. 获取 TFC 玩家扩展数据
        IPlayerInfo info = IPlayerInfo.get(player);

        // 4. 回满口渴值
        info.setThirst(PlayerInfo.MAX_THIRST);

        // 5. 重置营养 — readFromNbt 清空旧记录 + 解码 + 重算
        //    先设 nutrition.hunger=0 → calculateNutrition() 中 local1 = max(0, 20-0) = 20
        INutritionData nutrition = info.nutrition();
        nutrition.setHunger(0);
        nutrition.readFromNbt(foodNbt);

        player.getFoodData().setFoodLevel(20);
        // 7. 强制同步到客户端
        info.forceUpdate();

        // 8. Logger 检查重置后的营养值
        float[] nuts = info.nutrition().getNutrients();
        LOGGER.info("resetPlayer for {}: avg={} nuts=[{},{},{},{},{}]",
            player.getName().getString(),
            info.nutrition().getAverageNutrition(),
            nuts[0], nuts[1], nuts[2], nuts[3], nuts[4]);
    }

    /**
     * 调试指令：将玩家生命值、口渴值、饥饿值全设为 1。
     * 供 {@code /backtotfccore debug_set_one} 调用。
     */
    public static void debugSetOne(Player player) {
        if (player.level().isClientSide()) return;

        IPlayerInfo info = IPlayerInfo.get(player);

        player.setHealth(1);              // 生命 → 1
        info.setThirst(1.0f);             // 口渴 → 1
        player.getFoodData().setFoodLevel(1);  // 饥饿 → 1（vanilla foodLevel，UI 直接读这个）
        // 注：下个 tick TFC 会执行 nutrition.setHungerAndUpdate(food.getFoodLevel()) 自动同步

        info.forceUpdate();
    }
}
