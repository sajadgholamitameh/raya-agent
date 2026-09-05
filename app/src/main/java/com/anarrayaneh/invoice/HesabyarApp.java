package com.anarrayaneh.invoice;

import android.app.Activity;
import android.app.Application;
import android.app.backup.BackupManager;
import android.os.Bundle;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Keeps a compact JSON safety snapshot in app-private storage and notifies
 * Android's Backup Manager whenever the user leaves the app. Android Auto
 * Backup then syncs the app database/preferences/snapshot with the Google
 * account configured for device backup. If Android restores only the snapshot
 * but the database is empty, this class restores the snapshot automatically.
 */
public class HesabyarApp extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String DIR = "auto_backup";
    private static final String FILE = "latest.json";
    private long lastSnapshotAt = 0L;

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof HesabyarActivity) {
            maybeRestoreSnapshot((HesabyarActivity) activity);
        }
    }

    @Override public void onActivityPaused(Activity activity) {
        if (activity instanceof HesabyarActivity) {
            saveSnapshot((HesabyarActivity) activity, false);
        }
    }

    @Override public void onActivityStopped(Activity activity) {
        if (activity instanceof HesabyarActivity) {
            saveSnapshot((HesabyarActivity) activity, true);
        }
    }

    private synchronized void saveSnapshot(HesabyarActivity h, boolean force) {
        if (h.db == null || h.prefs == null) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastSnapshotAt < 15000L) return;
        try {
            JSONObject settings = new JSONObject();
            settings.put("backup_email", h.prefs.getString("backup_email", ""));
            settings.put("active_shop_id", h.activeShopId);

            JSONObject root = new JSONObject();
            root.put("app", "Hesabyar");
            root.put("schema", 3);
            root.put("created_jalali", HesabyarActivity.Jalali.today());
            root.put("created_at", now);
            root.put("settings", settings);
            root.put("data", h.db.toJson());

            File dir = new File(getFilesDir(), DIR);
            if (!dir.exists() && !dir.mkdirs()) return;
            File tmp = new File(dir, FILE + ".tmp");
            File target = new File(dir, FILE);
            Writer w = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8");
            w.write(root.toString());
            w.flush();
            w.close();
            if (target.exists()) target.delete();
            if (!tmp.renameTo(target)) {
                copy(tmp, target);
                tmp.delete();
            }
            lastSnapshotAt = now;
            new BackupManager(this).dataChanged();
        } catch (Exception ignored) {
            // Backup must never interrupt accounting operations.
        }
    }

    private void maybeRestoreSnapshot(HesabyarActivity h) {
        try {
            File target = new File(new File(getFilesDir(), DIR), FILE);
            if (!target.exists() || h.db == null) return;

            long tx = count(h, "tx");
            long customers = count(h, "customers");
            long banks = count(h, "bank_accounts");
            long shops = count(h, "shops");
            if (tx > 0 || customers > 0 || banks > 0 || shops > 1) return;

            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(target), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();

            JSONObject root = new JSONObject(sb.toString());
            if (!"Hesabyar".equals(root.optString("app"))) return;
            JSONObject data = root.optJSONObject("data");
            if (data == null) return;

            h.db.restore(data);
            JSONObject settings = root.optJSONObject("settings");
            long wanted = h.db.firstShopId();
            if (settings != null) {
                String email = settings.optString("backup_email", h.prefs.getString("backup_email", ""));
                wanted = settings.optLong("active_shop_id", wanted);
                h.prefs.edit().putString("backup_email", email).apply();
            }
            h.activeShopId = h.db.shopExists(wanted) ? wanted : h.db.firstShopId();
            h.prefs.edit().putLong("active_shop_id", h.activeShopId).apply();
            h.currentMonth = HesabyarActivity.Jalali.today().substring(0, 7);
            h.showDashboard();
        } catch (Exception ignored) {
            // If the cloud snapshot is unavailable/corrupt, keep the local DB.
        }
    }

    private long count(HesabyarActivity h, String table) {
        android.database.Cursor c = h.db.raw("SELECT COUNT(*) FROM " + table, null);
        long n = c.moveToFirst() ? c.getLong(0) : 0;
        c.close();
        return n;
    }

    private void copy(File src, File dst) throws Exception {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        out.flush();
        out.close();
        in.close();
    }

    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}