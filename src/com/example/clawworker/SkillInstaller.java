package com.example.clawworker;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkillInstaller {
    private static final String TAG = "ClawWorkerSkills";
    private static final String ASSET_ROOT = "skills";
    private static final String META_FILE = ".install_meta.json";
    private static final String BRIDGE_SCRIPT_NAME = "claw_call.sh";
    private static final String BRIDGE_SCRIPT_USER_LINE = "USER_ID=\"${CLAW_USER_ID:-0}\"";
    private static final String VERSION = "2026-03-23-v2";
    private final Gson gson = new Gson();

    public static class InstallResult {
        public final boolean installed;
        public final String targetDir;
        public final String digest;
        public final String error;

        InstallResult(boolean installed, String targetDir, String digest, String error) {
            this.installed = installed;
            this.targetDir = targetDir;
            this.digest = digest;
            this.error = error;
        }
    }

    private static class Meta {
        String version;
        String digest;
    }

    public InstallResult ensureInstalled(Context context) {
        return ensureInstalled(context, false);
    }

    public InstallResult getCurrentStatus() {
        List<File> candidates = buildCandidateDirs();
        for (File dir : candidates) {
            if (dir == null || !dir.exists() || !dir.isDirectory()) {
                continue;
            }
            File meta = new File(dir, META_FILE);
            File script = new File(dir, BRIDGE_SCRIPT_NAME);
            boolean installed = meta.exists() || script.exists();
            return new InstallResult(installed, dir.getAbsolutePath(), "", installed ? "" : "not_initialized");
        }
        return new InstallResult(false, "", "", "not_installed");
    }

    public InstallResult ensureInstalled(Context context, boolean forceUpdate) {
        if (context == null) {
            Log.e(TAG, "ensureInstalled context_null");
            return new InstallResult(false, "", "", "context_null");
        }
        AssetManager assets = context.getAssets();
        List<String> files;
        try {
            files = listMarkdownAssets(assets, ASSET_ROOT);
            if (files.isEmpty()) {
                Log.w(TAG, "ensureInstalled assets not found in root=skills, fallback to root scan");
                files = listMarkdownAssets(assets, "");
            }
        } catch (IOException e) {
            Log.e(TAG, "ensureInstalled list_assets_failed", e);
            return new InstallResult(false, "", "", "list_assets_failed");
        }
        if (files.isEmpty()) {
            Log.e(TAG, "ensureInstalled no_skills_assets root=" + ASSET_ROOT);
            return new InstallResult(false, "", "", "no_skills_assets");
        }
        Log.d(TAG, "ensureInstalled assets_count=" + files.size());
        String digest = computeDigest(assets, files);
        File target = resolveTargetDir();
        if (target == null) {
            Log.e(TAG, "ensureInstalled target_dir_unavailable");
            return new InstallResult(false, "", digest, "target_dir_unavailable");
        }
        try {
            if (!forceUpdate && isInstalled(target, digest)) {
                writeBridgeScript(assets, target);
                Log.d(TAG, "ensureInstalled already_installed dir=" + target.getAbsolutePath());
                return new InstallResult(true, target.getAbsolutePath(), digest, "");
            }
            for (String assetPath : files) {
                String relative = normalizeRelativeAssetPath(assetPath);
                File out = new File(target, relative);
                Log.d(TAG, "copy asset=" + assetPath + " to=" + out.getAbsolutePath());
                copyAsset(assets, assetPath, out);
                setReadable(out);
            }
            writeMeta(target, digest);
            writeBridgeScript(assets, target);
            Log.i(TAG, "ensureInstalled success force=" + forceUpdate
                    + " dir=" + target.getAbsolutePath() + " digest=" + digest);
            return new InstallResult(true, target.getAbsolutePath(), digest, "");
        } catch (IOException e) {
            Log.e(TAG, "install_failed", e);
            return new InstallResult(false, target.getAbsolutePath(), digest, "install_failed");
        }
    }

    private File resolveTargetDir() {
        List<File> candidates = buildCandidateDirs();
        for (File dir : candidates) {
            Log.d(TAG, "resolveTargetDir try=" + dir.getAbsolutePath());
            if (prepareDir(dir)) {
                Log.d(TAG, "resolveTargetDir selected=" + dir.getAbsolutePath());
                return dir;
            }
        }
        return null;
    }

    private List<File> buildCandidateDirs() {
        List<File> candidates = new ArrayList<>();
        File external = Environment.getExternalStorageDirectory();
        if (external != null) {
            candidates.add(new File(external, "ClawWorkerSkills"));
            candidates.add(new File(external, "Documents/ClawWorkerSkills"));
        }
        candidates.add(new File("/data/local/tmp/ClawWorkerSkills"));
        return candidates;
    }

    private boolean prepareDir(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "prepareDir mkdirs_failed dir=" + dir.getAbsolutePath());
            return false;
        }
        if (!dir.isDirectory()) {
            Log.w(TAG, "prepareDir not_directory dir=" + dir.getAbsolutePath());
            return false;
        }
        setReadable(dir);
        boolean canWrite = dir.canWrite();
        boolean canRead = dir.canRead();
        Log.d(TAG, "prepareDir dir=" + dir.getAbsolutePath()
                + " canRead=" + canRead + " canWrite=" + canWrite);
        return canWrite || canRead;
    }

    private boolean isInstalled(File target, String digest) {
        File meta = new File(target, META_FILE);
        if (!meta.exists()) {
            return false;
        }
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(meta.toPath());
            Meta m = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), Meta.class);
            if (m == null) {
                return false;
            }
            if (!VERSION.equals(m.version)) {
                return false;
            }
            return digest.equals(m.digest);
        } catch (IOException e) {
            Log.w(TAG, "isInstalled meta_read_failed path=" + meta.getAbsolutePath(), e);
            return false;
        }
    }

    private void writeMeta(File target, String digest) throws IOException {
        Meta meta = new Meta();
        meta.version = VERSION;
        meta.digest = digest;
        File out = new File(target, META_FILE);
        byte[] bytes = gson.toJson(meta).getBytes(StandardCharsets.UTF_8);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(out);
            fos.write(bytes);
            fos.flush();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        setReadable(out);
    }

    private String computeDigest(AssetManager assets, List<String> files) {
        Collections.sort(files);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String f : files) {
                String openPath = normalizeRelativeAssetPath(f);
                md.update(openPath.getBytes(StandardCharsets.UTF_8));
                md.update((byte) '\n');
                InputStream is = null;
                try {
                    is = assets.open(openPath);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = is.read(buffer)) > 0) {
                        md.update(buffer, 0, n);
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
            byte[] bytes = md.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            StringBuilder joined = new StringBuilder();
            for (String f : files) {
                joined.append(f).append('\n');
            }
            return String.valueOf(joined.toString().hashCode());
        }
    }

    private List<String> listMarkdownAssets(AssetManager assets, String dir) throws IOException {
        List<String> out = new ArrayList<>();
        String[] entries = assets.list(dir);
        if (entries == null || entries.length == 0) {
            if (dir.endsWith(".md")) {
                out.add(dir);
            }
            return out;
        }
        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            String child = dir + "/" + entry;
            String[] childEntries = assets.list(child);
            if (childEntries == null || childEntries.length == 0) {
                if (child.endsWith(".md")) {
                    out.add(child);
                }
            } else {
                out.addAll(listMarkdownAssets(assets, child));
            }
        }
        return out;
    }

    private String normalizeRelativeAssetPath(String assetPath) {
        if (assetPath == null || assetPath.trim().isEmpty()) {
            return "unknown.md";
        }
        String p = assetPath;
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        String prefix = ASSET_ROOT + "/";
        if (p.startsWith(prefix)) {
            return p.substring(prefix.length());
        }
        return p;
    }

    private void copyAsset(AssetManager assets, String assetPath, File out) throws IOException {
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
            setReadable(parent);
        }
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            String openPath = normalizeRelativeAssetPath(assetPath);
            is = assets.open(openPath);
            fos = new FileOutputStream(out);
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) > 0) {
                fos.write(buffer, 0, n);
            }
            fos.flush();
            Log.d(TAG, "copyAsset success path=" + out.getAbsolutePath());
        } finally {
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    private void setReadable(File file) {
        if (file == null) {
            return;
        }
        file.setReadable(true, false);
        file.setWritable(true, true);
        if (file.isDirectory()) {
            file.setExecutable(true, false);
        }
    }

    private void writeBridgeScript(AssetManager assets, File targetDir) throws IOException {
        if (assets == null || targetDir == null) {
            return;
        }
        InputStream scriptInput = null;
        byte[] scriptBytes;
        try {
            scriptInput = assets.open(BRIDGE_SCRIPT_NAME);
            scriptBytes = readAllBytes(scriptInput);
        } catch (IOException first) {
            if (scriptInput != null) {
                try {
                    scriptInput.close();
                } catch (IOException ignored) {
                }
            }
            scriptInput = assets.open(ASSET_ROOT + "/" + BRIDGE_SCRIPT_NAME);
            scriptBytes = readAllBytes(scriptInput);
        } finally {
            if (scriptInput != null) {
                try {
                    scriptInput.close();
                } catch (IOException ignored) {
                }
            }
        }
        scriptBytes = ensureBridgeUserLine(scriptBytes);
        File scriptFile = new File(targetDir, BRIDGE_SCRIPT_NAME);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(scriptFile);
            fos.write(scriptBytes);
            fos.flush();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        scriptFile.setReadable(true, false);
        scriptFile.setWritable(true, true);
        scriptFile.setExecutable(true, false);
        Log.i(TAG, "bridge_script_ready path=" + scriptFile.getAbsolutePath());
    }

    private byte[] ensureBridgeUserLine(byte[] originalBytes) {
        String content = new String(originalBytes == null ? new byte[0] : originalBytes,
                StandardCharsets.UTF_8);
        if (content.contains(BRIDGE_SCRIPT_USER_LINE)) {
            return content.getBytes(StandardCharsets.UTF_8);
        }
        String marker = "AM_BIN=\"$(command -v am)\"";
        int idx = content.indexOf(marker);
        if (idx >= 0) {
            int lineEnd = content.indexOf('\n', idx);
            if (lineEnd >= 0) {
                String patched = content.substring(0, lineEnd + 1)
                        + BRIDGE_SCRIPT_USER_LINE + "\n"
                        + content.substring(lineEnd + 1);
                return patched.getBytes(StandardCharsets.UTF_8);
            }
        }
        String fallback = "#!/system/bin/sh\n"
                + BRIDGE_SCRIPT_USER_LINE + "\n"
                + content;
        return fallback.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        if (is == null) {
            return new byte[0];
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
