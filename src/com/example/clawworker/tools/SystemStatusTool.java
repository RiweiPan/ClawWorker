package com.example.clawworker.tools;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class SystemStatusTool {
    private final Context context;

    public SystemStatusTool(Context context) {
        this.context = context;
    }

    public String buildSystemState() {
        StringBuilder sb = new StringBuilder();
        sb.append("foreground_activity: ").append(getForegroundActivityComponent()).append("\n");
        sb.append("battery: ").append(getBatteryStatus()).append("\n");
        sb.append("network: ").append(getNetworkStatus());
        return sb.toString();
    }

    private String getForegroundActivityComponent() {
        try {
            Class<?> atm = Class.forName("android.app.ActivityTaskManager");
            Method getService = atm.getDeclaredMethod("getService");
            Object service = getService.invoke(null);
            Method getTasks = service.getClass().getMethod("getTasks", int.class);
            Object listObj = getTasks.invoke(service, 1);
            if (listObj instanceof List) {
                List<?> tasks = (List<?>) listObj;
                if (!tasks.isEmpty()) {
                    Object info = tasks.get(0);
                    try {
                        Field topActivity = info.getClass().getField("topActivity");
                        Object comp = topActivity.get(info);
                        if (comp instanceof ComponentName) {
                            return ((ComponentName) comp).flattenToShortString();
                        }
                    } catch (NoSuchFieldException e) {
                        Field topActivity = info.getClass().getDeclaredField("topActivity");
                        topActivity.setAccessible(true);
                        Object comp = topActivity.get(info);
                        if (comp instanceof ComponentName) {
                            return ((ComponentName) comp).flattenToShortString();
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        try {
            ActivityManager am = context == null
                    ? null
                    : (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                if (tasks != null && !tasks.isEmpty()) {
                    ComponentName comp = tasks.get(0).topActivity;
                    if (comp != null) {
                        return comp.flattenToShortString();
                    }
                }
            }
        } catch (Exception e) {
        }
        return "UNKNOWN";
    }

    private String getBatteryStatus() {
        BatteryManager bm = context == null
                ? null
                : (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int level = bm == null ? -1 : bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        Intent intent = context == null
                ? null
                : context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = intent == null ? -1 : intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        return "level=" + level + "%, charging=" + charging;
    }

    private String getNetworkStatus() {
        ConnectivityManager cm = context == null
                ? null
                : (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return "unknown";
        }
        Network network = cm.getActiveNetwork();
        if (network == null) {
            return "none";
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) {
            return "unknown";
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "wifi";
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "cellular";
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return "ethernet";
        }
        return "other";
    }
}
