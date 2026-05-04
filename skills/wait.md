---
name: wait
action: wait
---

## Params
- milliseconds: number，必填，范围 0~300000

## Steps
1. 在已触发异步 UI 变化后调用 `wait`。
2. 等待完成后立即调用 `query_ui`。
3. 若页面仍未稳定，可短等待一次而不是长等待一次。

## Failure
- `invalid wait duration`：等待值小于 0。
- `wait duration too long`：等待值超过上限 300000ms。

## Example
```json
{"id":"12","action":"wait","params":{"milliseconds":500}}
```
