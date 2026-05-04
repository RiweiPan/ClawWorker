package com.example.clawworker.tools;

import com.example.clawworker.action.ActionResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CurrentTimeTool {
    public ActionResult getCurrentTime() {
        ZonedDateTime now = ZonedDateTime.now();
        String iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now);
        String zone = ZoneId.systemDefault().getId();
        long epochMs = Instant.now().toEpochMilli();
        String message = "now=" + iso + ", timezone=" + zone + ", epoch_ms=" + epochMs;
        return new ActionResult(true, message, "current_time", 0, 0, 0, 0, -1);
    }
}
