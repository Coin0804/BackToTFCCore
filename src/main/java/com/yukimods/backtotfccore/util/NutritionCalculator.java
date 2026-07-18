package com.yukimods.backtotfccore.util;

import net.dries007.tfc.config.TFCConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NutritionCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger("NutritionCalculator");

    private static final int DEFAULT_HUNGER_WINDOW = 80;
    private static final float DEFAULT_NUTRITION = 0.5f;
    private static final float DEFAULT_DAIRY_NUTRITION = 0.0f;

    private static final int BASE_HUNGER = 0;
    private static final int FOOD_HUNGER = 20;
    private static final int LOCAL_1 = Math.max(0, 20 - BASE_HUNGER);
    private static final float WEIGHT = Math.max(FOOD_HUNGER, 4);

    private NutritionCalculator() {}

    public static float requiredFoodNutrient(float targetNutrient, boolean isDairy) {
        return requiredFoodNutrient(targetNutrient, isDairy, getHungerWindow());
    }

    /** 纯数学版本，不依赖 TFCConfig，适合单元测试 */
    public static float requiredFoodNutrient(float targetNutrient, boolean isDairy, int hungerWindow) {
        float base = isDairy ? DEFAULT_DAIRY_NUTRITION : DEFAULT_NUTRITION;
        float ratio = 1f - (float) LOCAL_1 / hungerWindow;
        float result = (targetNutrient - base * ratio) * hungerWindow / WEIGHT;
        result = Math.max(0, result); // 不允许负营养值
        LOGGER.info("reqFoodNut(target={}, dairy={}, window={}): ratio={} → result={}",
            targetNutrient, isDairy, hungerWindow, ratio, result);
        return result;
    }

    public static ListTag buildFoodNbt(float[] targetNutrients) {
        return buildFoodNbt(targetNutrients, getHungerWindow());
    }

    /** 纯数学版本，不依赖 TFCConfig，适合单元测试 */
    public static ListTag buildFoodNbt(float[] targetNutrients, int hungerWindow) {
        if (targetNutrients.length < 5) {
            throw new IllegalArgumentException("Need 5 nutrient values, got " + targetNutrients.length);
        }

        float g = requiredFoodNutrient(targetNutrients[0], false, hungerWindow);
        float f = requiredFoodNutrient(targetNutrients[1], false, hungerWindow);
        float v = requiredFoodNutrient(targetNutrients[2], false, hungerWindow);
        float p = requiredFoodNutrient(targetNutrients[3], false, hungerWindow);
        float d = requiredFoodNutrient(targetNutrients[4], true, hungerWindow);

        LOGGER.info("buildFoodNbt targets=[{},{},{},{},{}] → foodNutrients=[{},{},{},{},{}]",
            targetNutrients[0], targetNutrients[1], targetNutrients[2],
            targetNutrients[3], targetNutrients[4],
            g, f, v, p, d);

        CompoundTag food = new CompoundTag();
        food.putInt("hunger", FOOD_HUNGER);
        food.putFloat("water", 0f);
        food.putFloat("saturation", 0f);
        food.putInt("intoxication", 0);
        food.putFloat("grain",      g);
        food.putFloat("fruit",      f);
        food.putFloat("vegetables", v);
        food.putFloat("protein",    p);
        food.putFloat("dairy",      d);
        food.putFloat("decay_modifier", 0f);

        ListTag list = new ListTag();
        list.add(food);
        return list;
    }

    private static int getHungerWindow() {
        try {
            int w = TFCConfig.SERVER.nutritionRotationHungerWindow.get();
            LOGGER.info("getHungerWindow from config: {}", w);
            return w;
        } catch (Exception e) {
            LOGGER.warn("getHungerWindow fallback to {}", DEFAULT_HUNGER_WINDOW);
            return DEFAULT_HUNGER_WINDOW;
        }
    }
}
