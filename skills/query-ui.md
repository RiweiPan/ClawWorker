---
name: query-ui
action: query_ui
---

## Params

- `include_screenshot`: boolean, 可选
- `screenshot_path`: string, 可选
- `vision_enabled`: boolean, 可选

## Returns

- `data.package_name`: 当前前台应用的包名（如 `com.taobao.movie.android`），用于识别应用和 debug
- `data.ui_dump`: 文本化 UI 索引，带层级缩进
- `data.element_count`: 元素数量（含可交互元素和可见文本元素）
- `data.ui_points`: 元素坐标数组
  - `index`: 元素索引（`·` 开头的上下文行无 index，不在此数组中）
  - `center_x`, `center_y`: 用于 click/long_press
  - `left`, `top`, `right`, `bottom`: 用于 swipe 组合
  - `class_name`, `resource_id`, `text`
- `data.screenshot_path`: 当 `include_screenshot=true` 时返回

## ui_dump 格式说明

每行格式：
```
{indent}{index}. {ClassName}: {resourceId}, "{text}" - (left,top,right,bottom)
```

- `indent`: 每层嵌套缩进 2 空格，表达 UI 层级关系
- `index`: 可交互元素和叶子文本元素的连续编号（0, 1, 2, ...）
- 以 `·` 开头的行是无索引的上下文行（带文本的容器节点，用于理解 UI 结构但不支持直接操作）
- fallback 文本（text 与 resourceId 或 className 相同）不会在 `ui_dump` 中显示，列为 `""` 以减噪

示例输出：
```
0.  Toolbar: , "" - (0,0,1080,150)
  1.    ImageButton: ic_back, "Navigate up" - (50,30,120,120)
  2.    TextView: , "Settings" - (200,40,880,140)
  3.    RecyclerView: , "" - (0,150,1080,1920)
  ·     LinearLayout: , "Wi-Fi Section" - (0,150,1080,260)
    4.      CompoundButton: switch_wifi, "" - (900,160,1000,220)
    5.      TextView: , "Connected to my_network" - (100,230,800,280)
```

## 包含规则

`ui_dump` 中出现的元素基于以下规则：
1. **可交互**: `clickable` 或 `editable` 为 true — 分配索引
2. **有可见文本的叶子节点**: 如 `TextView`、有 `contentDescription` 的 `ImageView` — 分配索引
3. **有可见文本的容器节点**: 如带 `contentDescription="Wi-Fi Section"` 的 `LinearLayout` — 以 `·` 上下文行展示
4. **跳过冗余容器**: 如 clickable 容器同时满足 (a) 自身无可见文本 (b) 子孙中存在可交互元素 — 该容器不输出，子元素直接提升到当前层级，避免双重索引

纯结构性容器（无文本、非交互）不出现在输出中，但其子元素正常处理。

## Steps

1. 在需要点击/滑动前先调用一次 `query_ui`。
2. 如果返回空 UI 索引，可重试最多 3 次（内部已实现自动重试）。
3. 从 `data.ui_points` 选取目标元素坐标。
4. 单点操作使用 `center_x/center_y` 传给 `click` 或 `long_press`。
5. 滑动操作使用两个元素的中心点或边界点组合成 `start_x/start_y/end_x/end_y`。
6. 动作执行后再次 `query_ui`，用新坐标继续下一步。

## Example

```json
{"id":"4","action":"query_ui","params":{"include_screenshot":true,"screenshot_path":"/data/local/tmp/ui.png","vision_enabled":true}}
```
