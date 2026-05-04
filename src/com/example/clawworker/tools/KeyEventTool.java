package com.example.clawworker.tools;

import android.view.KeyEvent;
import com.example.clawworker.action.ActionResult;

public class KeyEventTool {
    private final InputInjector injector;

    public KeyEventTool(InputInjector injector) {
        this.injector = injector;
    }

    public ActionResult pressKey(int keyCode) {
        if (keyCode <= 0) {
            return new ActionResult(false, "invalid keycode", "key_event",
                    0, 0, 0, 0, -1);
        }
        boolean ok = injector.injectKeyCode(keyCode);
        String msg = ok ? "key ok" : "key failed";
        String detail = msg + " " + KeyEvent.keyCodeToString(keyCode);
        return new ActionResult(ok, detail, "key_event",
                0, 0, 0, 0, -1);
    }
}
