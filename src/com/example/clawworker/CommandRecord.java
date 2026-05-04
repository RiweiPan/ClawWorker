package com.example.clawworker;

public class CommandRecord {
    public long timestampMs;
    public String stage;
    public String request;
    public String response;
    public String status;

    public CommandRecord(long timestampMs, String stage, String request, String response, String status) {
        this.timestampMs = timestampMs;
        this.stage = stage == null ? "" : stage;
        this.request = request == null ? "" : request;
        this.response = response == null ? "" : response;
        this.status = status == null ? "" : status;
    }
}
