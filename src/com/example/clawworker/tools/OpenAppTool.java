package com.example.clawworker.tools;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.example.clawworker.action.ActionResult;
import java.util.List;
import java.util.Locale;

public class OpenAppTool {
    private final Context context;

    public OpenAppTool(Context context) {
        this.context = context;
    }

    public ActionResult openAppByName(String name) {
        String pkg = resolveOpenAppPackage(name);
        if (pkg == null) {
            return new ActionResult(false, "package not found", "open_app",
                    0, 0, 0, 0, -1);
        }
        boolean ok = openAppLikeMonkey(context, pkg);
        return new ActionResult(ok, ok ? "open app ok" : "open app failed", "open_app",
                0, 0, 0, 0, -1);
    }

    private String resolveOpenAppPackage(String userText) {
        if (userText == null) {
            return null;
        }
        String norm = userText.trim();
        if (norm.isEmpty()) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        if (isPackageName(norm) && pm.getLaunchIntentForPackage(norm) != null) {
            return norm;
        }
        String[] tokens = norm.split("[/|,;\\s]+");
        for (String t : tokens) {
            String token = t.toLowerCase(Locale.US);
            if (isPackageName(t) && pm.getLaunchIntentForPackage(t) != null) {
                return t;
            }
            if (token.contains("wechat") || token.contains("wecaht") || t.contains("微信")) {
                if (pm.getLaunchIntentForPackage("com.tencent.mm") != null) {
                    return "com.tencent.mm";
                }
            } else if (token.contains("settings") || token.contains("setting") || t.contains("设置")) {
                if (pm.getLaunchIntentForPackage("com.android.settings") != null) {
                    return "com.android.settings";
                }
            } else if (token.contains("gallery") || token.contains("photos") || token.contains("photo")
                    || token.contains("album") || t.contains("相册") || t.contains("图库")) {
                if (pm.getLaunchIntentForPackage("com.google.android.apps.photos") != null) {
                    return "com.google.android.apps.photos";
                }
                if (pm.getLaunchIntentForPackage("com.android.gallery3d") != null) {
                    return "com.android.gallery3d";
                }
            }
            String byLabel = findPackageByLabel(t);
            if (byLabel != null) {
                return byLabel;
            }
        }
        return null;
    }

    private boolean isPackageName(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            return false;
        }
        int dot = v.indexOf('.');
        if (dot <= 0 || dot == v.length() - 1) {
            return false;
        }
        return true;
    }

    private String findPackageByLabel(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        for (ApplicationInfo app : apps) {
            String label = String.valueOf(pm.getApplicationLabel(app));
            if (label.equalsIgnoreCase(q) || label.contains(q)) {
                if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                    return app.packageName;
                }
            }
        }
        return null;
    }

    private boolean openAppLikeMonkey(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        try {
            Intent implicit = new Intent(Intent.ACTION_MAIN);
            implicit.addCategory(Intent.CATEGORY_LAUNCHER);
            implicit.setPackage(packageName);
            implicit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(implicit);
            return true;
        } catch (ActivityNotFoundException e) {
            return openAppExplicitly(context, pm, packageName);
        } catch (SecurityException e) {
            return openAppExplicitly(context, pm, packageName);
        }
    }

    private boolean openAppExplicitly(Context context, PackageManager pm, String packageName) {
        Intent queryIntent = new Intent(Intent.ACTION_MAIN);
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        queryIntent.setPackage(packageName);
        List<ResolveInfo> resolves = pm.queryIntentActivities(queryIntent, 0);
        try {
            if (resolves != null && !resolves.isEmpty()) {
                ResolveInfo first = resolves.get(0);
                if (first.activityInfo != null) {
                    ComponentName component = new ComponentName(
                            first.activityInfo.packageName,
                            first.activityInfo.name
                    );
                    Intent launch = new Intent(Intent.ACTION_MAIN);
                    launch.addCategory(Intent.CATEGORY_LAUNCHER);
                    launch.setComponent(component);
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    context.startActivity(launch);
                    return true;
                }
            }
            Intent fallback = pm.getLaunchIntentForPackage(packageName);
            if (fallback == null) {
                return false;
            }
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(fallback);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}
