package com.example.clawworker;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.system.ErrnoException;
import android.system.Os;
import android.system.UnixSocketAddress;
import android.util.Log;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LocalSocketThread extends Thread {
    private static final String TAG = "ClawWorkerSocket";
    private static final String SOCKET_NAME = "claw_worker";
    private static final String FS_SOCKET_PATH = "/data/local/tmp/claw_worker.sock";
    private static final int IO_TIMEOUT_MS = 5000;
    private volatile boolean running = true;
    private LocalServerSocket serverSocket;
    private final Gson gson = new Gson();
    private final Context context;
    private ToolDispatcher toolDispatcher;
    private final CommandHistoryStore historyStore = CommandHistoryStore.getInstance();

    public LocalSocketThread(Context context) {
        super("ClawWorkerSocketThread");
        this.context = context == null ? null : context.getApplicationContext();
        historyStore.init(this.context);
    }

    public void shutdown() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close_failed", e);
            }
        }
        interrupt();
    }

    @Override
    public void run() {
        try {
            boolean started = false;
            // Try filesystem socket first for shell connectivity
            try {
                File fsFile = new File(FS_SOCKET_PATH);
                if (fsFile.exists()) {
                    // best-effort cleanup
                    //noinspection ResultOfMethodCallIgnored
                    fsFile.delete();
                }
                FileDescriptor fd = Os.socket(android.system.OsConstants.AF_UNIX,
                        android.system.OsConstants.SOCK_STREAM, 0);
                UnixSocketAddress addr = UnixSocketAddress.createFileSystem(FS_SOCKET_PATH);
                Os.bind(fd, addr);
                Os.chmod(FS_SOCKET_PATH, 0666);
                serverSocket = new LocalServerSocket(fd);
                Log.d(TAG, "socket_started path=" + FS_SOCKET_PATH + " namespace=FILESYSTEM");
                started = true;
            } catch (ErrnoException | IOException e) {
                Log.w(TAG, "fs_socket_bind_failed path=" + FS_SOCKET_PATH + " err=" + e.getMessage());
            }
            if (!started) {
                serverSocket = new LocalServerSocket(SOCKET_NAME);
                Log.d(TAG, "socket_started name=" + SOCKET_NAME + " namespace=ABSTRACT");
                started = true;
            }
            if (toolDispatcher == null) {
                toolDispatcher = new ToolDispatcher(context);
            }
            while (running) {
                LocalSocket client = serverSocket.accept();
                handleClient(client);
            }
        } catch (IOException e) {
            Log.e(TAG, "socket_failed", e);
        }
    }

    private void handleClient(LocalSocket client) {
        if (client == null) {
            return;
        }
        String payload = "";
        try {
            client.setSoTimeout(IO_TIMEOUT_MS);
            payload = readAll(client.getInputStream());
            historyStore.logReceived(payload);
            ClawResponse response = toolDispatcher == null
                    ? buildErrorResponse("", "dispatcher_not_ready")
                    : toolDispatcher.handleRequest(payload);
            String responseJson = gson.toJson(response);
            OutputStream os = client.getOutputStream();
            os.write(responseJson.getBytes(StandardCharsets.UTF_8));
            os.flush();
            historyStore.logExecuted(payload, responseJson,
                    response == null ? "error" : response.status);
        } catch (IOException e) {
            Log.e(TAG, "client_io_failed", e);
            historyStore.logError(payload, "client_io_failed");
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                Log.e(TAG, "client_close_failed", e);
            }
        }
    }

    private String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private ClawResponse buildErrorResponse(String id, String message) {
        ClawResponse response = new ClawResponse();
        response.id = id == null ? "" : id;
        response.status = "error";
        response.error_msg = message == null ? "" : message;
        return response;
    }
}
