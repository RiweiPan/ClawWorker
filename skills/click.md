---
name: click
action: click
---

## Params
- x: number，必填
- y: number，必填

## Rule
- 仅按像素坐标点击。
- 推荐先调用 query-ui，从 `data.ui_points` 读取坐标。

## Steps
1. 先调用 `query_ui` 获取最新 `ui_points`。
2. 选取目标元素的 `center_x/center_y`。
3. 调用 `click` 执行点击。
4. 再次 `query_ui` 验证页面变化。

## Failure
- `missing_coordinates`：未提供 x 或 y。
- `click failed`：点击注入失败。

## Example
```json
{"id":"1","action":"click","params":{"x":540,"y":1200}}
```
