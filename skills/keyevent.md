---
name: keyevent
action: key_event
---

## Params
- key_code: number，必填

## Steps
1. 根据任务选择 key_code，例如 HOME=3、BACK=4、ENTER=66。
2. 调用 `key_event` 发送按键。
3. 调用 `query_ui` 验证按键效果是否生效。

## Failure
- `invalid keycode`：key_code 非法或小于等于 0。
- `key failed`：按键注入失败。

## Example
```json
{"id":"8","action":"key_event","params":{"key_code":3}}
```
