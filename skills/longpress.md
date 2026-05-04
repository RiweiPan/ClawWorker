---
name: longpress
action: long_press
---

## Params
- x: number，必填
- y: number，必填
- duration_ms: number，可选

## Rule
- 仅按像素坐标长按。
- 推荐先调用 query-ui，从 `data.ui_points` 读取坐标。

## Steps
1. 调用 `query_ui` 获取目标元素中心坐标。
2. 设定 `duration_ms`（常用 600~1200ms）。
3. 调用 `long_press`。
4. 通过 `query_ui` 验证是否出现上下文菜单或拖拽状态。

## Failure
- `missing_coordinates`：未提供 x 或 y。
- `long_press failed`：长按注入失败。

## Example
```json
{"id":"3","action":"long_press","params":{"x":520,"y":980,"duration_ms":900}}
```
