package com.example.clawworker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SkillInstaller skillInstaller = new SkillInstaller();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshList();
            handler.postDelayed(this, 1000L);
        }
    };
    private TextView statusView;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommandHistoryStore.getInstance().init(getApplicationContext());
        startService(new Intent(this, ClawWorkerService.class));
        setContentView(buildContentView());
        refreshSkillsStatus();
        refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("ClawWorker Command Monitor");
        title.setTextSize(22f);
        title.setGravity(Gravity.START);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        statusView = new TextView(this);
        statusView.setTextSize(14f);
        root.addView(statusView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button refreshButton = new Button(this);
        refreshButton.setText("Refresh");
        refreshButton.setOnClickListener(v -> {
            refreshSkillsStatus();
            refreshList();
        });
        root.addView(refreshButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button updateSkillsButton = new Button(this);
        updateSkillsButton.setText("Update Skills");
        updateSkillsButton.setOnClickListener(v -> {
            SkillInstaller.InstallResult result = skillInstaller.ensureInstalled(getApplicationContext(), true);
            String text = "skills_updated=" + result.installed
                    + " dir=" + result.targetDir
                    + (TextUtils.isEmpty(result.error) ? "" : " error=" + result.error);
            statusView.setText(text);
            refreshList();
        });
        root.addView(updateSkillsButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView listView = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void refreshSkillsStatus() {
        SkillInstaller.InstallResult result = skillInstaller.getCurrentStatus();
        String text = "skills_manual_mode=true installed=" + result.installed
                + " dir=" + result.targetDir
                + (TextUtils.isEmpty(result.error) ? "" : " error=" + result.error);
        statusView.setText(text);
    }

    private void refreshList() {
        List<CommandRecord> records = CommandHistoryStore.getInstance().snapshot();
        List<String> lines = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);
        for (int i = records.size() - 1; i >= 0; i--) {
            CommandRecord r = records.get(i);
            String time = format.format(new Date(r.timestampMs));
            String one = time + " [" + r.stage + "] status=" + r.status;
            String two = "req=" + truncate(r.request, 160);
            String three = TextUtils.isEmpty(r.response) ? "" : "resp=" + truncate(r.response, 160);
            if (three.isEmpty()) {
                lines.add(one + "\n" + two);
            } else {
                lines.add(one + "\n" + two + "\n" + three);
            }
        }
        adapter.clear();
        adapter.addAll(lines);
        adapter.notifyDataSetChanged();
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "...";
    }
}
