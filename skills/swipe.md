---
name: swipe
action: swipe
---

## Params
- start_x: number，必填
- start_y: number，必填
- end_x: number，必填
- end_y: number，必填

## Rule
- 仅按像素坐标滑动。
- 推荐先调用 query-ui，从 `data.ui_points` 读取坐标。

## Steps
1. 调用 `query_ui` 获取起点和终点候选坐标。
2. 组合 `start_x/start_y/end_x/end_y`。
3. 调用 `swipe` 执行滑动。
4. 再次调用 `query_ui`，确认列表或页面已滚动。

## Failure
- `missing_coordinates`：坐标参数不完整。
- `swipe failed`：滑动注入失败。

## Example
```json
{"id":"2","action":"swipe","params":{"start_x":540,"start_y":1600,"end_x":540,"end_y":400}}
```
