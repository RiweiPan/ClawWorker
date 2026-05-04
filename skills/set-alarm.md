---
name: set-alarm
action: set_alarm
---

## Params
- hour: number，必填，0-23
- minute: number，必填，0-59
- message: string，可选
- days_of_week: number[]，可选
- repeat: boolean，可选

## Steps
1. 校验 `hour/minute` 是否在合法范围。
2. 若为重复闹钟，补充 `days_of_week` 与 `repeat=true`。
3. 调用 `set_alarm` 拉起系统闹钟设置。
4. 调用后可执行 `query-ui`，确认是否进入闹钟相关界面。

## Example
```json
{"id":"9","action":"set_alarm","params":{"hour":8,"minute":30,"message":"wake","days_of_week":[1,2,3],"repeat":true}}
```
