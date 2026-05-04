# ClawWorker Agent Execution Template

本模板用于指导 OpenClaw 在调用任意 skill 时采用统一执行框架，提升稳定性与可解释性。

## 0) 读取顺序

1. 先读取 `ClawWorkerSkills.md`。
2. 再读取本模板 `./skills/ExecutionTemplate.md`。
3. 按需读取目标 skill 的 `detail_doc`。

## 1) 标准流程

### Precheck
- 解析用户意图，确认目标动作与目标对象。
- 确认参数完整性：
  - 坐标型操作必须有坐标
- 若目标依赖时间、网络或前台应用，先调用辅助 skill（如 `current_time`、`system_status`）。

### Observe
- 调用 `query_ui` 获取当前界面：
  - `ui_dump` 用于语义判断
  - `ui_points` 用于坐标执行
- 必要时调用 `screen_capture` 记录视觉证据。

### Plan
- 生成单步计划，避免一次混入多个不确定动作。
- 优先选择成功率高的动作路径。
- 为失败场景准备一个替代动作。

### Execute
- 使用目标 skill 发起执行。
- 操作参数来自最新观测，不使用过期坐标或过期索引。

### Verify
- 立即执行 `query_ui` 进行结果校验。
- 判断目标是否达成，不达成进入 Retry。

### Retry
- 仅重试一次或两次。
- 每次重试前必须重新观察界面。
- 重试应使用替代参数或替代路径，避免原样重复。

### Fail
- 输出失败原因，包含：
  - 已执行动作
  - 关键参数
  - 错误信息（如 `error_msg`）
  - 下一步建议

## 2) 推荐模式

### UI 操作模式
1. `query_ui`
2. 从 `ui_points` 取坐标
3. `click/swipe/long_press`
4. `query_ui` 验证

### 输入模式
1. `query_ui` 找到目标输入框 `center_x/center_y`
2. `input` 传 `x/y/text`
3. `query_ui` 验证文本变化

### 应用启动模式
1. `open_app`（应用名）
2. `query_ui` 验证
3. 若失败，`open_app`（包名）
4. `query_ui` 再验证

## 3) 错误处理基线

- `missing_coordinates`：补全坐标并重试。
- `focus failed`：先点击目标输入框再输入。
- `unknown_action`：检查 skill 名与 action 映射。

## 4) 记录建议

- 每一步保留：
  - 请求体
  - 响应体
  - 验证结论
- 有截图时记录 `screenshot_path`，便于回放排错。
