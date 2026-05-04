---
name: current-time
action: current_time
---

## Params
- 无

## Returns
- data.action_result.message: 包含 now, timezone, epoch_ms

## Steps
1. 在涉及日程、提醒、定时任务前先调用 `current_time`。
2. 使用返回的 timezone 与 epoch_ms 对齐后续时间计算。
3. 再调用 `set_alarm` 或 `calendar_reminder`。

## Example
```json
{"id":"10","action":"current_time","params":{}}
```
