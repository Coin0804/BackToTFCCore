# TODO — BackToTFC Core

## 近期完成 (2026-07-16)

### 生命维持装置大修

- [x] **LifeSupportBlockEntity** — `float[6]` NBT 存储恢复目标值（grain/fruit/veg/protein/dairy/saturation），构造默认全 0.5
- [x] **BE 注册** — `DeferredRegister<BlockEntityType<?>>` + 主类注册
- [x] **LifeSupportDeviceBlock** — `Block` → `BaseEntityBlock`，实现 `codec()` / `getRenderShape()` / `newBlockEntity()`
- [x] **Restore 按钮** — 从 `level.getBlockEntity(pos)` 读 targets，前 5 个传入 `buildFoodNbt` 生成食物 NBT
- [x] **Collect 按钮** — 销毁前 `writeTargetsToTag()` → `DataComponents.BLOCK_ENTITY_DATA` + `CustomData.of()`，放置自动恢复
- [x] **NutritionCalculator** — `Math.max(0, result)` 防止负营养值进入 foodNbt
- [x] **状态提示** — 底部 `status_label`，按钮点击后 3 秒 S2C 反馈（`backtotfccore.gui.record.done` / `restore.done`）
- [x] **ProgressBar 内部 label** — 从 CSS 隐藏改为 S2C 绑定 BE target 显示
- [x] **Sync ID 对齐** — `setupNutrientBars` 两端都执行，消除 LDLib2 `unknown sync value` 警告
- [x] **日志清理** — 去掉 DEBUG 日志，保留 ERROR/WARN/按钮操作 INFO

### 其他

- [x] **LDLib2 BlockUI** — XML `life_support.xml` + `LifeSupportBlockUI.java` 完整 GUI，替代旧 `LifeSupportScreen`
- [x] **nutrient ratio** — `nut_X_ratio` Label 移入 S2C 绑定，进度条实时更新

---

## 待办

### 生命维持装置

- [ ] **自定义纹理/模型** — 当前使用 `minecraft:block/iron_block` 占位
- [ ] **BE target 游戏内编辑** — 目前仅 NBT 读写，无 UI 编辑入口
- [ ] **Record 按钮后续功能** — 目前仅设置重生点，可扩展

### 工作台系统

- [ ] 工作台分级配方完善
- [ ] 泥砖工作台功能验证

### 杂项

- [ ] 模组版本号 bump（目前 0.0.2 保持，累计变更较多）
