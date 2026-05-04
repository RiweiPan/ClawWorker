package com.example.clawworker.action;

public class ActionResult {
    private final boolean success;
    private final String message;
    private final String actionType;
    private final float startX;
    private final float startY;
    private final float endX;
    private final float endY;
    private final int targetElementIndex;

    public ActionResult(boolean success, String message, String actionType,
                        float startX, float startY, float endX, float endY,
                        int targetElementIndex) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.actionType = actionType == null ? "" : actionType;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.targetElementIndex = targetElementIndex;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getActionType() {
        return actionType;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public float getEndX() {
        return endX;
    }

    public float getEndY() {
        return endY;
    }

    public int getTargetElementIndex() {
        return targetElementIndex;
    }
}
