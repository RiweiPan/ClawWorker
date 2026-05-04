---
name: calendar-reminder
action: calendar_reminder
---

## Params
- start_epoch_ms: number，必填，事件开始时间，UTC epoch 毫秒
- end_epoch_ms: number，可选，事件结束时间，UTC epoch 毫秒
- title: string，必填，系统日历事件标题
- description: string，可选，事件描述
- reminder_minutes: number，可选，提前提醒分钟数

## 语义细节
- title 是写入系统日历的事件标题，不是技能名或分类标签。
- end_epoch_ms 缺失或 `<= start_epoch_ms` 时，自动补为 `start + 30分钟`。
- reminder_minutes 小于 0 时不创建提醒记录。
- 依赖系统日历可写权限与可写 calendar。

## Steps
1. 先用 `current_time` 获取设备时区和当前时间基准。
2. 将自然语言时间换算为 `start_epoch_ms/end_epoch_ms`。
3. 生成清晰的 `title`，再补充 `description`。
4. 调用 `calendar_reminder` 创建事件。
5. 检查返回 `action_result.message` 中的事件 id，失败则调整时间或标题重试。

## Example
```json
{"id":"11","action":"calendar_reminder","params":{"start_epoch_ms":1730000000000,"end_epoch_ms":1730003600000,"title":"Project Sync","description":"Weekly review","reminder_minutes":10}}
```
