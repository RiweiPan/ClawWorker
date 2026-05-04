---
name: open-app
action: open_app
---

## Params
- name: string，必填，应用名称或包名

## Steps
1. 优先将 `name` 作为应用名调用一次。
2. 调用后执行一次 `query-ui`，确认前台界面是否已切到目标应用。
3. 若未打开，尝试将 `name` 解析为包名后再次调用。
4. 再次执行 `query-ui` 做结果确认。
5. 若仍失败，返回失败原因并附带已尝试的名称/包名。

## 建议
- 当应用名存在歧义时，优先使用包名。
- 英文名和中文名都可尝试一次，再回退包名。

## Example
```json
{"id":"7","action":"open_app","params":{"name":"Settings"}}
```
