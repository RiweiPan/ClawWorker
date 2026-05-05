# ClawWorker Skills for OpenClaw

本文件是 ClawWorker 的能力总览与操作约定。ClawWorker 的核心作用是为 OpenClaw 提供一套可编排的设备侧执行能力，包括：
- 观察：读取 UI 树、UI 索引和截图
- 操作：点击、滑动、长按、输入、按键、启动应用
- 系统任务：闹钟、日历、系统状态、等待

推荐将 ClawWorker 用作“观察-决策-操作-再观察”中的AgentLoop中的执行器。

## SKILLS 目录

- 总目录：`./skills/`
- 目录索引：`./skills/README.md`
- 执行模板：`./skills/ExecutionTemplate.md`
- 规则：一个 skill 一个 markdown 文件

## 推荐加载流程

1. 读取本文件，拿到 skill 名称、action 名称和 `detail_doc`。
2. 读取执行模板 `./skills/ExecutionTemplate.md`，采用统一执行框架。
3. 按需读取 `detail_doc`，理解参数语义、边界行为、失败处理和步骤。
4. 先调用 `query_ui` 获取 `ui_points` 与当前界面上下文。
5. 将请求通过 `clawshell` 发往 `/dev/socket/claw_worker`。
6. 动作后再次 `query_ui`，基于新 UI 状态继续决策。

## 常规使用方法

### 1) 观察阶段
- 先执行 `query_ui`，拿到：
  - `data.ui_dump`：可读的 UI 索引文本
  - `data.ui_points`：每个元素的像素坐标
- 若任务依赖视觉确认，开启 `include_screenshot=true` 并读取截图路径。

### 2) 操作阶段
- click/long_press/swipe 等基础操作统一使用像素坐标。
- 坐标优先来自最近一次 `query_ui` 的 `ui_points`，避免使用过期坐标。

### 3) 校验阶段
- 操作后重新执行 `query_ui`。
- 对比关键元素是否出现/消失，确认目标是否达成。
- 若失败，尝试一次替代坐标或替代路径，避免无限重试。

## Transport

- Socket: `/dev/socket/claw_worker`
- CLI bridge: `clawshell`
- 请求方式: `echo '<json>' | clawshell`
- 响应格式: JSON
- Termux脚本桥接: 安装后生成 `/sdcard/ClawWorkerSkills/claw_call.sh`

### Termux 脚本调用

1. 直接传 JSON：
```bash
sh /sdcard/ClawWorkerSkills/claw_call.sh '{"id":"t1","action":"current_time","params":{}}'
```

2. 传请求文件：
```bash
sh /sdcard/ClawWorkerSkills/claw_call.sh --file /sdcard/cw_req_t1.json
```

3. 可选超时秒数（默认 10）：
```bash
sh /sdcard/ClawWorkerSkills/claw_call.sh --file /sdcard/cw_req_t1.json 15
```

## 通用请求协议

```json
{
  "id": "uuid-12345",
  "action": "skill_name",
  "params": {},
  "timeout": 5000
}
```

## 通用响应协议

```json
{
  "id": "uuid-12345",
  "status": "success | error",
  "data": {},
  "error_msg": ""
}
```

## 失败处理约定

- `status=error` 时以 `error_msg` 为主错误信息。
- 常见错误：
  - `missing_coordinates`：坐标参数缺失
  - `invalid_json`：请求 JSON 非法
  - `unknown_action`：action 名称不存在
- 建议重试策略：
  - 参数缺失/非法：修正参数后重试
  - 环境问题（如目标界面变化）：先 `query_ui` 再重试

## Skill Catalog

### click
- description: 点击元素，仅接受像素坐标
- action: `click`
- detail_doc: `./skills/click.md`
- params:
  - `x` number 必填
  - `y` number 必填
- example:
```json
{"id":"1","action":"click","params":{"x":540,"y":1200}}
```

### input
- description: 聚焦目标元素并输入文本
- action: `input`
- detail_doc: `./skills/input.md`
- params:
  - `x` number 必填
  - `y` number 必填
  - `text` string 必填
- example:
```json
{"id":"2","action":"input","params":{"x":540,"y":1180,"text":"hello"}}
```

### swipe
- description: 执行滑动，仅接受像素坐标
- action: `swipe`
- detail_doc: `./skills/swipe.md`
- params:
  - `start_x` number 必填
  - `start_y` number 必填
  - `end_x` number 必填
  - `end_y` number 必填
- example:
```json
{"id":"3","action":"swipe","params":{"start_x":540,"start_y":1600,"end_x":540,"end_y":400}}
```

### swipe-dir
- description: 按方向滑动屏幕
- action: `swipe_direction`
- detail_doc: `./skills/swipe-dir.md`
- params:
  - `direction` string 必填，取值 `up | down | left | right`
  - `vision_enabled` boolean 可选
- example:
```json
{"id":"4","action":"swipe_direction","params":{"direction":"down"}}
```

### longpress
- description: 长按元素，仅接受像素坐标
- action: `long_press`
- detail_doc: `./skills/longpress.md`
- params:
  - `x` number 必填
  - `y` number 必填
  - `duration_ms` number 可选
- example:
```json
{"id":"5","action":"long_press","params":{"x":520,"y":980,"duration_ms":1000}}
```

### open-app
- description: 通过应用名或包名拉起应用
- action: `open_app`
- detail_doc: `./skills/open-app.md`
- params:
  - `name` string 必填
- example:
```json
{"id":"6","action":"open_app","params":{"name":"Settings"}}
```

### keyevent
- description: 发送 Android key code
- action: `key_event`
- detail_doc: `./skills/keyevent.md`
- params:
  - `key_code` number 必填
- example:
```json
{"id":"7","action":"key_event","params":{"key_code":3}}
```

### set-alarm
- description: 打开闹钟设置
- action: `set_alarm`
- detail_doc: `./skills/set-alarm.md`
- params:
  - `hour` number 必填
  - `minute` number 必填
  - `message` string 可选
  - `days_of_week` number[] 可选
  - `repeat` boolean 可选
- example:
```json
{"id":"8","action":"set_alarm","params":{"hour":8,"minute":30,"message":"wake","days_of_week":[1,2,3],"repeat":true}}
```

### current-time
- description: 获取设备当前时间信息
- action: `current_time`
- detail_doc: `./skills/current-time.md`
- params: 无
- example:
```json
{"id":"9","action":"current_time","params":{}}
```

### calendar-reminder
- description: 创建日历事件和提醒（请务必读取详细文档字段语义）
- action: `calendar_reminder`
- detail_doc: `./skills/calendar-reminder.md`
- params:
  - `start_epoch_ms` number 必填
  - `end_epoch_ms` number 可选
  - `title` string 必填
  - `description` string 可选
  - `reminder_minutes` number 可选
- example:
```json
{"id":"10","action":"calendar_reminder","params":{"start_epoch_ms":1730000000000,"end_epoch_ms":1730003600000,"title":"meeting","description":"sync","reminder_minutes":10}}
```

### wait
- description: 等待指定毫秒
- action: `wait`
- detail_doc: `./skills/wait.md`
- params:
  - `milliseconds` number 必填
- example:
```json
{"id":"11","action":"wait","params":{"milliseconds":500}}
```

### system-status
- description: 获取前台 Activity、电量、网络状态
- action: `system_status`
- detail_doc: `./skills/system-status.md`
- params: 无
- example:
```json
{"id":"12","action":"system_status","params":{}}
```

### screen-capture
- description: 截图到文件
- action: `screen_capture`
- detail_doc: `./skills/screen-capture.md`
- params:
  - `path` string 可选，默认 `/data/local/tmp/clawworker_<ts>.png`
- example:
```json
{"id":"13","action":"screen_capture","params":{"path":"/data/local/tmp/s1.png"}}
```

### query-ui
- description: 读取 UI 索引文本并返回每个元素的坐标点
- action: `query_ui`
- detail_doc: `./skills/query-ui.md`
- params:
  - `include_screenshot` boolean 可选
  - `screenshot_path` string 可选
  - `vision_enabled` boolean 可选
- returns:
  - `data.ui_points[].center_x/center_y`: 可直接作为 click/long_press 的输入
  - `data.ui_points[].left/top/right/bottom`: 可用于生成 swipe 输入
- example:
```json
{"id":"14","action":"query_ui","params":{"include_screenshot":true,"screenshot_path":"/data/local/tmp/ui.png","vision_enabled":true}}
```

### clipboard-set
- description: 将文本写入系统剪贴板，支持任意 Unicode 文本包括中文
- action: `clipboard_set`
- detail_doc: `./skills/clipboard.md`
- params:
  - `text` string 必填
- example:
```json
{"id":"15","action":"clipboard_set","params":{"text":"你好世界"}}
```

### clipboard-paste
- description: 通过 Ctrl+V 粘贴剪贴板内容，可选先点击坐标获取焦点
- action: `clipboard_paste`
- detail_doc: `./skills/clipboard.md`
- params:
  - `x` number 可选
  - `y` number 可选
- example:
```json
{"id":"16","action":"clipboard_paste","params":{"x":540,"y":1180}}
```

## skills/call 映射约定

如果 OpenClaw 使用统一的 `skills/call` 调用形式，可按以下方式映射到 ClawWorker 请求体：

```json
{
  "jsonrpc": "2.0",
  "id": "skill-1",
  "method": "skills/call",
  "params": {
    "name": "open-app",
    "arguments": {
      "name": "Settings"
    }
  }
}
```

映射规则：
- `params.name` -> `action`（按上文 skill 名到 action 的对应关系转换）
- `params.arguments` -> `params`
- `id` 透传到 ClawWorker `id`

## clawshell 调用示例

```bash
echo '{"id":"1","action":"open_app","params":{"name":"Settings"}}' | clawshell
```

```bash
echo '{"id":"2","action":"query_ui","params":{"include_screenshot":true,"screenshot_path":"/data/local/tmp/ui.png","vision_enabled":true}}' | clawshell
```
