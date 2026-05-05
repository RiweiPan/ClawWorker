---
name: clipboard-set
action: clipboard_set
---
---
name: clipboard-paste
action: clipboard_paste
---

## clipboard-set

Params:
- `text` string, 必填

Rule:
- 将文本写入系统剪贴板，供后续 `clipboard_paste` 粘贴。
- 支持任意 Unicode 文本（包括中文），无需通过键盘输入法。

Steps:
1. 调用 `clipboard_set` 将目标文本写入剪贴板。
2. 通过 `click` 聚焦目标输入框。
3. 调用 `clipboard_paste` 粘贴文本。
4. `query_ui` 验证文本已更新。

Failure:
- `clipboard permission denied`: 剪贴板权限不足。
- `clipboard service unavailable`: 剪贴板服务不可用。

Example:
```json
{"id":"15","action":"clipboard_set","params":{"text":"你好世界"}}
```

---

## clipboard-paste

Params:
- `x` number, 可选 — 粘贴前点击的 x 坐标
- `y` number, 可选 — 粘贴前点击的 y 坐标

Rule:
- 通过注入 Ctrl+V 组合键粘贴系统剪贴板内容。
- 若提供了 x/y 坐标，先点击该位置获取焦点再粘贴。
- 若 Ctrl+V 注入失败，回退到 shell `input keyevent 279`（KEYCODE_PASTE）。

Steps:
1. 确保已通过 `clipboard_set` 将目标文本写入剪贴板。
2. 若提供坐标：点击 `(x, y)` 获取焦点。
3. 调用 `clipboard_paste` 执行粘贴。
4. 重新 `query_ui` 验证文本是否出现在目标字段。

Failure:
- `paste failed`: 粘贴注入失败，可重试一次。

Example:
```json
{"id":"16","action":"clipboard_paste","params":{}}
```
```json
{"id":"17","action":"clipboard_paste","params":{"x":540,"y":1180}}
```
