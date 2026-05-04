---
name: system-status
action: system_status
---

## Params
- 无

## Returns
- data.system_state: 前台 Activity、电量、网络状态文本

## Steps
1. 在执行关键步骤前调用 `system_status` 获取环境快照。
2. 关注前台 Activity 是否与预期应用一致。
3. 若网络依赖任务失败，先检查 network 字段再重试。

## Example
```json
{"id":"13","action":"system_status","params":{}}
```
