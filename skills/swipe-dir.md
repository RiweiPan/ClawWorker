---
name: swipe-dir
action: swipe_direction
---

## Params
- direction: string，必填，up|down|left|right
- vision_enabled: boolean，可选

## Steps
1. 确定目标方向（列表翻页通常用 up/down）。
2. 调用 `swipe_direction` 执行方向滑动。
3. 调用 `query_ui` 验证页面是否变化。
4. 未变化时调整方向或连续滑动一次。

## Failure
- `unknown direction`：方向参数不是 up/down/left/right。
- `swipe failed`：手势注入失败。

## Example
```json
{"id":"6","action":"swipe_direction","params":{"direction":"down"}}
```
