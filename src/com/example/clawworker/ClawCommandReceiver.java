package com.example.clawworker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Base64;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class ClawCommandReceiver extends BroadcastReceiver {
    public static final String ACTION_CALL = "com.example.clawworker.ACTION_CALL";
    public static final String ACTION_RESULT = "com.example.clawworker.ACTION_RESULT";
    public static final String EXTRA_PAYLOAD = "payload";
    public static final String EXTRA_PAYLOAD_PATH = "payload_path";
    public static final String EXTRA_PAYLOAD_B64 = "payload_b64";
    public static final String EXTRA_RESPONSE = "response";
    public static final String EXTRA_RESULT_PATH = "result_path";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_REQUEST_ID = "request_id";
    private static final String TAG = "ClawCommandReceiver";
    private static final String RESULT_DIR_NAME = "ClawWorkerResults";
    private final Gson gson = new Gson();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        if (!ACTION_CALL.equals(intent.getAction())) {
            return;
        }
        Log.i(TAG, "onReceive action=" + intent.getAction());
        final Context appContext = context.getApplicationContext();
        final String payload = intent.getStringExtra(EXTRA_PAYLOAD);
        final String payloadPath = intent.getStringExtra(EXTRA_PAYLOAD_PATH);
        final String payloadB64 = intent.getStringExtra(EXTRA_PAYLOAD_B64);
        final PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                handleCall(appContext, payload, payloadPath, payloadB64);
            } finally {
                pendingResult.finish();
            }
        }, "ClawCommandReceiverThread").start();
    }

    private void handleCall(Context context, String payload, String payloadPath, String payloadB64) {
        String finalPayload = payload;
        if (TextUtils.isEmpty(finalPayload) && !TextUtils.isEmpty(payloadB64)) {
            finalPayload = decodeBase64(payloadB64);
        }
        if (TextUtils.isEmpty(finalPayload) && !TextUtils.isEmpty(payloadPath)) {
            finalPayload = readFile(payloadPath);
        }
        CommandHistoryStore.getInstance().init(context);
        CommandHistoryStore.getInstance().logReceived(finalPayload);
        ClawResponse response;
        String requestId = "";
        if (TextUtils.isEmpty(finalPayload)) {
            response = new ClawResponse();
            response.id = "";
            response.status = "error";
            response.error_msg = "missing_payload";
            requestId = "";
        } else {
            try {
                ClawRequest request = gson.fromJson(finalPayload, ClawRequest.class);
                if (request != null && request.id != null) {
                    requestId = request.id;
                }
            } catch (Exception e) {
                requestId = "";
            }
            ToolDispatcher dispatcher = new ToolDispatcher(context);
            response = dispatcher.handleRequest(finalPayload);
        }
        String responseJson = gson.toJson(response);
        CommandHistoryStore.getInstance().logExecuted(finalPayload, responseJson,
                response == null ? "error" : response.status);
        String resultPath = writeResultFile(responseJson, requestId);
        Log.i(TAG, "handleCall done requestId=" + requestId + " status="
                + (response == null ? "error" : response.status) + " resultPath=" + resultPath);
        sendResultBroadcast(context, response, responseJson, resultPath, requestId);
    }

    private void sendResultBroadcast(Context context, ClawResponse response, String responseJson,
                                     String resultPath, String requestId) {
        Intent result = new Intent(ACTION_RESULT);
        result.setPackage(context.getPackageName());
        result.putExtra(EXTRA_RESPONSE, responseJson);
        result.putExtra(EXTRA_RESULT_PATH, resultPath == null ? "" : resultPath);
        result.putExtra(EXTRA_STATUS, response == null ? "error" : response.status);
        result.putExtra(EXTRA_REQUEST_ID, requestId == null ? "" : requestId);
        context.sendBroadcast(result);
    }

    private String writeResultFile(String responseJson, String requestId) {
        String fileName = (TextUtils.isEmpty(requestId) ? String.valueOf(System.currentTimeMillis()) : requestId)
                + ".json";
        String primaryBase = getExternalBaseDir();
        String primaryPath = primaryBase + "/" + sanitizeFileName(fileName);
        if (writeFile(primaryPath, responseJson)) {
            return primaryPath;
        }
        String fallbackPath = "/data/local/tmp/" + RESULT_DIR_NAME + "/" + sanitizeFileName(fileName);
        if (writeFile(fallbackPath, responseJson)) {
            return fallbackPath;
        }
        return "";
    }

    private String getExternalBaseDir() {
        File ext = Environment.getExternalStorageDirectory();
        String root = ext != null ? ext.getAbsolutePath() : "/sdcard";
        return root + "/" + RESULT_DIR_NAME;
    }

    private boolean writeFile(String path, String content) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Log.w(TAG, "mkdirs_failed path=" + parent.getAbsolutePath());
            return false;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            fos.flush();
            Log.i(TAG, "write_result_success path=" + path);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "write_result_failed path=" + path + " err=" + e.getMessage());
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String sanitizeFileName(String name) {
        String n = name == null ? "result.json" : name;
        return n.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String readFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = new FileInputStream(path);
            bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            return "";
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String decodeBase64(String data) {
        if (TextUtils.isEmpty(data)) {
            return "";
        }
        try {
            byte[] decoded = Base64.decode(data, Base64.NO_WRAP);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
