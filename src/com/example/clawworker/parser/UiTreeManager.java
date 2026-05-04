package com.example.clawworker.parser;

import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class UiTreeManager {
    private final Object lock = new Object();
    private final Context appContext;
    private UiAutomation uiAutomation;
    private boolean isConnected;
    private boolean permissionDenied;

    public UiTreeManager(Context context) {
        this.appContext = context == null ? null : context.getApplicationContext();
    }

    public boolean init() {
        synchronized (lock) {
            if (permissionDenied) {
                return false;
            }
            if (uiAutomation == null && appContext != null) {
                UiAutomationConnection connection = new UiAutomationConnection();
                uiAutomation = new UiAutomation(appContext, connection);
            }
            if (uiAutomation == null) {
                return false;
            }
            if (!isConnected) {
                try {
                    uiAutomation.connect();
                    isConnected = true;
                } catch (SecurityException e) {
                    permissionDenied = true;
                    isConnected = false;
                    return false;
                }
            }
        }
        return true;
    }

    public void release() {
        synchronized (lock) {
            if (uiAutomation != null && isConnected) {
                uiAutomation.disconnect();
                isConnected = false;
            }
        }
    }

    public List<AccessibilityNodeInfo> fetchCurrentUiTree() {
        UiAutomation automation;
        synchronized (lock) {
            automation = uiAutomation;
        }
        if (automation == null) {
            return new ArrayList<>();
        }
        AccessibilityNodeInfo root = automation.getRootInActiveWindow();
        if (root == null) {
            return new ArrayList<>();
        }
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        Deque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            boolean matched = node.isClickable()
                    || node.isScrollable()
                    || node.isEditable()
                    || node.getText() != null;
            if (matched) {
                result.add(node);
            }
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    queue.add(child);
                }
            }
            if (!matched) {
                node.recycle();
            }
        }
        return result;
    }

    public AccessibilityNodeInfo getRootNode() {
        UiAutomation automation;
        synchronized (lock) {
            automation = uiAutomation;
        }
        if (automation == null) {
            return null;
        }
        return automation.getRootInActiveWindow();
    }

    public void recycleNodes(List<AccessibilityNodeInfo> nodes) {
        if (nodes == null) {
            return;
        }
        for (AccessibilityNodeInfo node : nodes) {
            if (node != null) {
                node.recycle();
            }
        }
    }
}
