---
name: query-ui
action: query_ui
---

## Params

- `include_screenshot`: boolean, 可选
- `screenshot_path`: string, 可选
- `vision_enabled`: boolean, 可选

## Returns

- `data.package_name`: 当前前台应用的包名（如 `com.taobao.movie.android`）
- `data.ui_dump`: 文本化 UI 索引，带层级缩进、同行文本合并、重复卡片分组
- `data.element_count`: 元素数量
- `data.ui_points`: 元素坐标数组
  - `index`: 元素索引（`·` 和 `---` 行无 index）
  - `center_x`, `center_y`: 用于 click/long_press
  - `left`, `top`, `right`, `bottom`: 用于 swipe 组合
  - `class_name`, `resource_id`, `text`
- `data.screenshot_path`: 当 `include_screenshot=true` 时返回

## 语义分组

ClawWorker 在生成 `ui_dump` 前对 UI 树做两步预处理：

### 1. 同行文本合并

同一行内、水平相邻的非交互式 `TextView` 被合并为单个 `text` 元素。
例如 `"￥"` + `"29.9"` + `"起"` → `"￥29.9起"`。

### 2. 重复卡片识别

当检测到 ≥2 个结构相同的兄弟容器（同 ClassName、同子元素结构、高度容差 < 15%），识别为卡片组。
每张卡片以 `--- {title} ---` 分隔行呈现，卡内元素缩进一层。

### 3. 卡内同行标签合并

卡片内同一水平行上 ≥2 个短 `TextView`（宽度 < 200px）合并为 `tags` 行，用 ` · ` 连接。

## ui_dump 格式说明

每行格式：
```
{indent}{index}. {ClassName}: {resourceId}, "{text}" - (left,top,right,bottom)
```

特殊行：
- `·` 前缀：上下文行，无 index，表示带文本的容器节点
- `---` 包裹行：卡片分隔符，`--- title ---` 后跟卡片内容

示例输出（普通布局，无卡片）：
```
0.  Toolbar: , "" - (0,0,1080,150)
  1.    ImageButton: ic_back, "Navigate up" - (50,30,120,120)
  2.    TextView: , "Settings" - (200,40,880,140)
```

示例输出（含卡片组，如电影院列表）：
```
0.  Toolbar: , "" - (0,136,1080,268)
  1.    ImageButton: titlebar_left_btn, "" - (44,136,132,267)
  ·     ActionBar$Tab: , "" - (11,268,403,378)
  2.      TextView: date, "今天" - (35,297,107,349)
  3.      TextView: day, "05月12日" - (107,297,274,349)
4.  ViewGroup: filter_mall_subway, "" - (13,390,203,473)
  ·     ViewGroup: filter_brand, "" - (220,390,410,473)
  ·     ViewGroup: filter_all, "" - (427,390,551,473)
--- 横店电影城（大涌店）---
  5.    TextView: oscar_cinemalist_cinema_name, "横店电影城（大涌店）" - (68,624,508,688)
  6.    text: "￥29.9起" - (872,616,1012,673)
  7.    TextView: oscar_cinemalist_cinema_address, "南沙区大涌东路星河智荟广场2楼" - (68,694,549,742)
  8.    TextView: oscar_cinemalist_cinema_distance, "79.9km" - (572,698,775,737)
  9.    tags: "影城卡 · 券包·4.1折起 · 退票 · 改签 · 3D眼镜收费 · 观影小食 · 可停车"
  10.   TextView: oscar_cinemalist_cinema_schedules_intro, "近期场次：13:30  |  14:50  |  ..." - (69,874,1045,922)
--- 中影南方影城（南沙华汇店）---
  11.   TextView: oscar_cinemalist_cinema_name, "中影南方影城（南沙华汇店）" - (68,1003,640,1067)
  12.   text: "￥19.8起" - (872,995,1012,1052)
  ...
```

## Steps

1. 在需要点击/滑动前先调用一次 `query_ui`。
2. 如果返回空 UI 索引，可重试最多 3 次（内部已实现自动重试）。
3. 卡片组以 `---` 分隔行展示，卡内元素已关联归属，无需 Agent 自行推断。
4. 从 `data.ui_points` 选取目标元素坐标（`center_x/center_y`）。
5. 动作执行后再次 `query_ui`，用新坐标继续下一步。

## Example

```json
{"id":"4","action":"query_ui","params":{"include_screenshot":true,"screenshot_path":"/data/local/tmp/ui.png","vision_enabled":true}}
```
