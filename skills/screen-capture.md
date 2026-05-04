---
name: screen-capture
action: screen_capture
---

## Params
- path: string，可选

## Returns
- data.screenshot_path: 实际写入路径

## Steps
1. 在关键动作前或后调用 `screen_capture` 保留证据。
2. 若未指定 path，读取返回的 `data.screenshot_path` 作为实际文件。
3. 结合 `query_ui` 的结构信息和截图做双重校验。

## Example
```json
{"id":"14","action":"screen_capture","params":{"path":"/data/local/tmp/s1.png"}}
```
