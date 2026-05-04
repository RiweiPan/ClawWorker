package com.example.clawworker;

import android.content.Context;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CommandHistoryStore {
    private static final int MAX_RECORDS = 300;
    private static final String FILE_NAME = "command_history.jsonl";
    private static CommandHistoryStore instance;
    private final Object lock = new Object();
    private final Gson gson = new Gson();
    private final List<CommandRecord> records = new ArrayList<>();
    private File historyFile;

    private CommandHistoryStore() {
    }

    public static CommandHistoryStore getInstance() {
        synchronized (CommandHistoryStore.class) {
            if (instance == null) {
                instance = new CommandHistoryStore();
            }
            return instance;
        }
    }

    public void init(Context context) {
        if (context == null) {
            return;
        }
        synchronized (lock) {
            if (historyFile == null) {
                historyFile = new File(context.getFilesDir(), FILE_NAME);
                loadFromDiskLocked();
            }
        }
    }

    public void logReceived(String request) {
        addRecord(new CommandRecord(System.currentTimeMillis(), "RECEIVED", request, "", "pending"));
    }

    public void logExecuted(String request, String response, String status) {
        addRecord(new CommandRecord(System.currentTimeMillis(), "EXECUTED", request, response, status));
    }

    public void logError(String request, String message) {
        addRecord(new CommandRecord(System.currentTimeMillis(), "ERROR", request, message, "error"));
    }

    public List<CommandRecord> snapshot() {
        synchronized (lock) {
            return new ArrayList<>(records);
        }
    }

    private void addRecord(CommandRecord record) {
        synchronized (lock) {
            records.add(record);
            while (records.size() > MAX_RECORDS) {
                records.remove(0);
            }
            appendToDiskLocked(record);
        }
    }

    private void loadFromDiskLocked() {
        if (historyFile == null || !historyFile.exists()) {
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(historyFile), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                CommandRecord rec = gson.fromJson(line, CommandRecord.class);
                if (rec != null) {
                    records.add(rec);
                }
            }
            while (records.size() > MAX_RECORDS) {
                records.remove(0);
            }
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void appendToDiskLocked(CommandRecord record) {
        if (historyFile == null || record == null) {
            return;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(historyFile, true);
            String line = gson.toJson(record) + "\n";
            fos.write(line.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (IOException e) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
