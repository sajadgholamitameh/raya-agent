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
 * Automatic safety snapshot + Android Auto Backup integration.
 * Database, settings, trial state and license identity are kept in app-private
 * storage and shared preferences; Android can restore them through the Google
 * account configured for device backup.
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
        if (activity instanceof HesabyarActivity) maybeRestoreSnapshot((HesabyarActivity) activity);
    }

    @Override public void onActivityPaused(Activity activity) {
        if (activity instanceof HesabyarActivity) saveSnapshot((HesabyarActivity) activity, false);
    }

    @Override public void onActivityStopped(Activity activity) {
        if (activity instanceof HesabyarActivity) saveSnapshot((HesabyarActivity) activity, true);
    }

    private synchronized void saveSnapshot(HesabyarActivity h, boolean force) {
        if (h.db == null || h.prefs == null) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastSnapshotAt < 15000L) return;
        try {
            JSONObject settings = new JSONObject();
            settings.put("backup_email", h.prefs.getString("backup_email", ""));
            settings.put("active_shop_id", h.activeShopId);
            settings.put("trial_start_ms", h.prefs.getLong("trial_start_ms", 0L));
            settings.put("last_seen_ms", h.prefs.getLong("last_seen_ms", 0L));
            settings.put("license_activated", h.prefs.getBoolean("license_activated", false));
            settings.put("license_email", h.prefs.getString("license_email", ""));
            settings.put("license_key", h.prefs.getString("license_key", ""));
            settings.put("license_serial", h.prefs.getInt("license_serial", 0));
            settings.put("license_activated_at", h.prefs.getLong("license_activated_at", 0L));

            JSONObject root = new JSONObject();
            root.put("app", "Hesabyar");
            root.put("schema", 4);
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
            // Accounting must keep working even if a backup attempt fails.
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
            boolean hasLicense = h.prefs.getBoolean("license_activated", false);
            if (tx > 0 || customers > 0 || banks > 0 || shops > 1 || hasLicense) return;

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
            JSONObject s = root.optJSONObject("settings");
            long wanted = h.db.firstShopId();
            if (s != null) {
                wanted = s.optLong("active_shop_id", wanted);
                android.content.SharedPreferences.Editor e = h.prefs.edit();
                e.putString("backup_email", s.optString("backup_email", h.prefs.getString("backup_email", "")));
                e.putLong("trial_start_ms", s.optLong("trial_start_ms", h.prefs.getLong("trial_start_ms", 0L)));
                e.putLong("last_seen_ms", s.optLong("last_seen_ms", h.prefs.getLong("last_seen_ms", 0L)));
                e.putBoolean("license_activated", s.optBoolean("license_activated", false));
                e.putString("license_email", s.optString("license_email", ""));
                e.putString("license_key", s.optString("license_key", ""));
                e.putInt("license_serial", s.optInt("license_serial", 0));
                e.putLong("license_activated_at", s.optLong("license_activated_at", 0L));
                e.apply();
            }
            h.activeShopId = h.db.shopExists(wanted) ? wanted : h.db.firstShopId();
            h.prefs.edit().putLong("active_shop_id", h.activeShopId).apply();
            h.currentMonth = HesabyarActivity.Jalali.today().substring(0, 7);
            h.showDashboard();
        } catch (Exception ignored) {
            // If the restored snapshot is unavailable/corrupt, preserve local data.
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
        out.flush(); out.close(); in.close();
    }

    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}