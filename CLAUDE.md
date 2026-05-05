# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

ClawWorker is a privileged Android system app (platform-signed, `system_ext` partition) that serves as the **device-side execution engine for OpenClaw**, an AI agent framework for mobile automation. It exposes device capabilities — UI observation, touch injection, app launching, system actions — through a JSON-based Unix domain socket protocol.

## Build

This is an AOSP Soong module. Build it within a full AOSP tree:

```bash
# Build the app
mmm packages/apps/ClawWorker

# Build just the clawshell CLI bridge
make clawshell

# Build and push to device
mmm packages/apps/ClawWorker && adb sync system_ext
```

There is no standalone Gradle build — this depends on platform APIs (`UiAutomation`, `InputManager.injectInputEvent`, etc.) and internal AOSP libraries (`gson`).

## Architecture

### Transport: Unix Domain Socket → ToolDispatcher

```
clawshell (C++ CLI) ──stdin JSON──▶ /dev/socket/claw_worker
                                      or abstract socket "claw_worker"
                                      or /data/local/tmp/claw_worker.sock
                                              │
                                              ▼
                                   LocalSocketThread (Java)
                                     reads JSON, calls
                                              │
                                              ▼
                                      ToolDispatcher
                                   routes by action name
                                              │
                              ┌───────────────┼───────────────┐
                              ▼               ▼               ▼
                        ActionExecutor   UiTreeManager   ScreenCapture
                              │               │
                              ▼               ▼
                       Concrete Tools   UiTreeParser
                    (ClickTool, etc.)   → TreeFilter → IndexedFormatter
```

**Key flow**: JSON request → `ToolDispatcher.handleRequest()` dispatches via `action` field → `ActionExecutor` delegates to concrete tool classes in `tools/` package → tool returns `ActionResult` → response JSON written back to socket.

`ToolDispatcher` also handles `query_ui` and `screen_capture` directly (they don't go through `ActionExecutor`).

### Request/Response Protocol

Requests are single-line JSON sent to the socket:
```json
{"id": "uuid", "action": "click", "params": {"x": 540, "y": 1200}}
```

Responses:
```json
{"id": "uuid", "status": "success", "data": {...}, "error_msg": ""}
```

### UI Tree Pipeline (`parser/` package)

1. **`UiTreeManager`** — wraps `UiAutomation` (hidden Android API) to get `AccessibilityNodeInfo` root from the active window
2. **`UiTreeParser.parse()`** — builds an `UiElement` tree from the accessibility node tree, extracting class name, resource ID, text, bounds
3. **`TreeFilter`** (interface) — two implementations:
   - `DetailedFilter` (default): passes through all clickable/editable/scrollable/text-bearing elements
   - `ConciseFilter` (vision mode): prunes the tree, used when a screenshot is available for visual grounding
4. **`IndexedFormatter`** — assigns sequential indices to filtered elements and produces the `ui_dump` text output + `ui_points` coordinates

### Tools (`tools/` package)

All tools inject input via `android.hardware.input.InputManager` (platform API, requires `INJECT_EVENTS` permission). They are thin wrappers: each tool maps to one `action` name in the protocol.

- **UI actions**: `ClickTool`, `InputTool`, `SwipeTool`, `LongPressTool` — pixel-coordinate-based input injection
- **System actions**: `OpenAppTool` (launches by app name or package name), `KeyEventTool` (Android key codes), `SetAlarmTool`, `CalendarReminderTool`, `CurrentTimeTool`, `WaitTool`, `SystemStatusTool`
- **Observation**: `ScreenCaptureTool` uses `DisplayManager` + hidden surface capture APIs

### Skill Installer

`SkillInstaller` copies markdown skill files from APK assets (`skills/` directory) to `/sdcard/ClawWorkerSkills/` (or `/sdcard/Documents/ClawWorkerSkills/`, or `/data/local/tmp/ClawWorkerSkills/`). It also writes a shell bridge script (`claw_call.sh`) for Termux-based invocation. Versioning is based on SHA-256 digest of all skill files — re-install only happens on content change.

### Persistence

`CommandHistoryStore` keeps the last 300 command records in a JSONL file at `command_history.jsonl` in the app's internal files directory, loaded into memory on init.

### Key files by role

| Role | File |
|------|------|
| Build definition | `Android.bp` |
| Manifest + permissions | `AndroidManifest.xml`, `etc/privapp-permissions-clawworker.xml` |
| Socket server & dispatch | `src/.../LocalSocketThread.java`, `src/.../ToolDispatcher.java` |
| Shell CLI bridge | `clawshell.cpp` |
| Skill catalog (runbook for agents) | `ClawWorkerSkills.md`, `skills/*.md` |
| UI tree parsing | `src/.../parser/UiTreeParser.java`, `UiTreeManager.java` |
| Action orchestration | `src/.../action/ActionExecutor.java` |
| Skill installation to storage | `src/.../SkillInstaller.java` |
| Debug UI (Activity) | `src/.../MainActivity.java` |
| Agent execution policy | `uiskills/ui-control-policy/SKILL.md` |

## Permissions

The app requires platform-level privileged permissions declared in both the manifest and `privapp-permissions-clawworker.xml`:
- `INJECT_EVENTS` — input injection (core functionality)
- `CAPTURE_VIDEO_OUTPUT`, `READ_FRAME_BUFFER` — screen capture
- `RETRIEVE_WINDOW_CONTENT` — accessibility tree access
- `REAL_GET_TASKS`, `QUERY_ALL_PACKAGES` — app discovery
- Calendar read/write, storage read/write — for alarm/reminder/screenshot tools

## Skill → Action mapping

The skill names used by OpenClaw map to internal action names in the protocol:

| Skill name | `action` field |
|------------|---------------|
| `click` | `click` |
| `input` | `input` |
| `swipe` | `swipe` |
| `swipe-dir` | `swipe_direction` |
| `longpress` | `long_press` |
| `open-app` | `open_app` |
| `keyevent` | `key_event` |
| `set-alarm` | `set_alarm` |
| `current-time` | `current_time` |
| `calendar-reminder` | `calendar_reminder` |
| `wait` | `wait` |
| `system-status` | `system_status` |
| `screen-capture` | `screen_capture` |
| `query-ui` | `query_ui` |
