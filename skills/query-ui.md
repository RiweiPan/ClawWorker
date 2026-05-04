***

name: query-ui
action: query\_ui
-----------------

## Params

- include\_screenshot: boolean，可选
- screenshot\_path: string，可选
- vision\_enabled: boolean，可选

## Returns

- data.ui\_dump: 文本化 UI 索引
- data.element\_count: 元素数量
- data.ui\_points: 元素坐标数组
  - index
  - center\_x
  - center\_y
  - left, top, right, bottom
  - class\_name, resource\_id, text
- data.screenshot\_path: 当 include\_screenshot=true 时返回

## Steps

1. 在需要点击/滑动前先调用一次 `query_ui`。
2. 如果返回的是一个空UI索引，可以再次获取一次，如果连续`3`次获取失败，才返回失败。
3. 从 `data.ui_points` 选取目标元素坐标。
4. 单点操作使用 `center_x/center_y` 传给 `click` 或 `long_press`。
5. 滑动操作使用两个元素的中心点或边界点组合成 `start_x/start_y/end_x/end_y`。
6. 动作执行后再次 `query_ui`，用新坐标继续下一步。

## Example

```json
{"id":"4","action":"query_ui","params":{"include_screenshot":true,"screenshot_path":"/data/local/tmp/ui.png"}}
```

