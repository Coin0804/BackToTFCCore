# TODO — BackToTFC Core

## 近期完成 (2026-07-18)

- [x] **JEI 书签热物品冻结** — `HeatComponentMixin` + `HeatComponentAccessor` + `JeiBookmarkFactoryMixin`，`lastTick=-2` 方案

## 近期完成 (2026-07-16)

- [x] **LifeSupportBlockEntity** — `float[6]` NBT 存储（5营养+1饱食度），默认 0.5
- [x] **BE 注册 + Block 改造** — `BaseEntityBlock` + `DeferredRegister<BlockEntityType<?>>`
- [x] **Restore / Collect 按钮** — NBT 读写 + `DataComponents.BLOCK_ENTITY_DATA` 持久化
- [x] **NutritionCalculator** — `Math.max(0, result)` 防负值
- [x] **状态提示** — 底部 label，按钮点击 3 秒 S2C 反馈（中英 i18n）
- [x] **ProgressBar label** — S2C 绑定 BE target，显示"目标值：0.5"
- [x] **Sync ID 对齐** — 客户端也创建 S2C 绑定消除 `unknown sync value` 警告
- [x] **LDLib2 BlockUI** — `life_support.xml` + `LifeSupportBlockUI.java` 完整 GUI

---

## 待办

### 生命维持装置

- [ ] **自定义纹理/模型** — 当前使用 `minecraft:block/iron_block` 占位
- [ ] **BE target 游戏内编辑** — 目前仅 NBT 读写，无 UI 编辑入口
- [ ] **Record 按钮功能扩展** — 目前仅设置重生点

### 工作台系统

- [ ] 工作台分级配方完善
- [ ] 泥砖工作台功能验证

### 杂项

- [ ] 模组版本号 bump（目前 0.0.3，累计变更较多——考虑 0.1.0？）
