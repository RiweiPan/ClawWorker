package com.example.clawworker.tools;

import com.example.clawworker.action.ActionResult;

public class WaitTool {
    private static final long MAX_WAIT_MS = 300000L;

    public ActionResult waitMillis(long milliseconds) {
        if (milliseconds < 0) {
            return new ActionResult(false, "invalid wait duration", "wait", 0, 0, 0, 0, -1);
        }
        if (milliseconds > MAX_WAIT_MS) {
            return new ActionResult(false, "wait duration too long", "wait", 0, 0, 0, 0, -1);
        }
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ActionResult(false, "wait interrupted", "wait", 0, 0, 0, 0, -1);
        }
        String message = "waited_ms=" + milliseconds;
        return new ActionResult(true, message, "wait", 0, 0, 0, 0, -1);
    }
}
