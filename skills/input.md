---
name: input
action: input
---

## Params
- x: number，必填
- y: number，必填
- text: string，必填

## Rule
- 先点击 `(x,y)` 对应位置获取焦点，再输入 text。
- 对 EditText 类型输入框，先点击再输入通常有更高成功率，应作为默认策略。
- 若目标输入框存在预填充文本，执行器会先尝试清空再输入新文本。

## Steps
1. 先执行 `query_ui`，从 `ui_points` 找到目标输入框的 `center_x/center_y`。
2. 调用 `input`，传入 `x/y/text`。
3. 再次 `query_ui`，确认文本已更新。
4. 若文本未更新，重新获取最新坐标后重试一次。

## Failure
- `focus failed`：目标输入框无法获取焦点。
- `input failed`：输入事件注入失败。

## Example
```json
{"id":"5","action":"input","params":{"x":540,"y":1180,"text":"hello"}}
```
