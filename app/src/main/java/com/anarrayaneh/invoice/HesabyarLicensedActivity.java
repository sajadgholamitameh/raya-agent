package com.anarrayaneh.invoice;

import android.app.*;
import android.app.backup.BackupManager;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Hesabyar licensing layer.
 * - 15-day trial from first run.
 * - 1,800 pre-issued license serials.
 * - Activation records the customer's email + license key.
 * - License/trial state is included in Android Auto Backup and the app snapshot.
 *
 * This is an offline licensing model. It validates genuine issued keys locally.
 * A future server-backed edition can additionally enforce one-key/one-email
 * globally across multiple devices.
 */
public class HesabyarLicensedActivity extends HesabyarItemsActivity {
    static final int TRIAL_DAYS = 15;
    static final long DAY_MS = 24L * 60L * 60L * 1000L;
    static final long TRIAL_MS = TRIAL_DAYS * DAY_MS;
    static final int LICENSE_COUNT = 1800;
    static final String LIC_SECRET = "Hesabyar.License.2026.Raya#1800!Seed";
    static final Pattern KEY_PATTERN = Pattern.compile("^HYA-(\\d{4})-([A-F0-9]{5})-([A-F0-9]{5})$");
    boolean licenseDialogVisible = false;
    long lastGateAt = 0L;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        initializeTrial();
    }

    @Override protected void onResume() {
        super.onResume();
        new Handler(Looper.getMainLooper()).postDelayed(this::checkLicenseGate, 250);
    }

    void initializeTrial() {
        long now = System.currentTimeMillis();
        long start = prefs.getLong("trial_start_ms", 0L);
        if (start <= 0L) {
            prefs.edit().putLong("trial_start_ms", now).putLong("last_seen_ms", now).apply();
            new BackupManager(this).dataChanged();
        } else {
            long last = prefs.getLong("last_seen_ms", 0L);
            if (now > last) prefs.edit().putLong("last_seen_ms", now).apply();
        }
    }

    boolean isLicensed() {
        if (!prefs.getBoolean("license_activated", false)) return false;
        String email = prefs.getString("license_email", "");
        String key = prefs.getString("license_key", "");
        return validEmail(email) && validateLicenseKey(key) > 0;
    }

    long effectiveNow() {
        long now = System.currentTimeMillis();
        long last = prefs.getLong("last_seen_ms", 0L);
        long effective = Math.max(now, last);
        if (effective > last) prefs.edit().putLong("last_seen_ms", effective).apply();
        return effective;
    }

    long trialEnd() {
        long start = prefs.getLong("trial_start_ms", 0L);
        if (start <= 0L) start = effectiveNow();
        return start + TRIAL_MS;
    }

    boolean trialActive() {
        return effectiveNow() < trialEnd();
    }

    int trialDaysRemaining() {
        long remain = Math.max(0L, trialEnd() - effectiveNow());
        return (int)Math.max(0, (remain + DAY_MS - 1) / DAY_MS);
    }

    void checkLicenseGate() {
        if (isFinishing() || isDestroyed()) return;
        long now = System.currentTimeMillis();
        if (now - lastGateAt < 350L) return;
        lastGateAt = now;
        if (isLicensed() || trialActive()) return;
        showActivationDialog(true);
    }

    @Override void showDashboard() {
        super.showDashboard();
        if (root == null) return;
        if (!isLicensed() && trialActive()) {
            TextView banner = text("نسخه آزمایشی • " + trialDaysRemaining() + " روز باقی مانده   |   فعال‌سازی لایسنس", 12, Color.WHITE, true);
            banner.setGravity(Gravity.CENTER);
            banner.setPadding(dp(12), dp(9), dp(12), dp(9));
            banner.setBackground(gradient(new int[]{Color.rgb(31,136,163), Color.rgb(0,170,157)}, 16));
            banner.setElevation(dp(2));
            banner.setOnClickListener(v -> showActivationDialog(false));
            int index = Math.min(1, root.getChildCount());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(45));
            lp.topMargin = dp(8); lp.bottomMargin = dp(4);
            root.addView(banner, index, lp);
        }
    }

    @Override void showMoreMenu() {
        new AlertDialog.Builder(this)
                .setTitle("حسابیار")
                .setItems(new String[]{"مدیریت فروشگاه‌ها", "حساب‌های بانکی", "بکاپ و بازیابی", "تنظیمات فروشگاه", "لایسنس و اشتراک", "ارتباط با ما"}, (d, which) -> {
                    if (which == 0) showShops();
                    else if (which == 1) showBankAccounts();
                    else if (which == 2) showBackup();
                    else if (which == 3) settingsDialog();
                    else if (which == 4) showLicenseCenter();
                    else showContact();
                }).show();
    }

    void showLicenseCenter() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(12), dp(18), dp(4));
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        if (isLicensed()) {
            String email = prefs.getString("license_email", "");
            int serial = prefs.getInt("license_serial", 0);
            TextView ok = text("✓ حسابیار فعال است", 20, Color.rgb(17,145,105), true);
            TextView details = text("ایمیل لایسنس: " + email + "\nشماره لایسنس: " + String.format(Locale.US, "%04d", serial) + "\nوضعیت: دائمی", 14, INK, false);
            details.setPadding(0, dp(10), 0, dp(4));
            box.addView(ok); box.addView(details);
            new AlertDialog.Builder(this).setTitle("لایسنس حسابیار").setView(box).setPositiveButton("بستن", null).show();
        } else {
            String state = trialActive() ? "دوره آزمایشی فعال است؛ " + trialDaysRemaining() + " روز باقی مانده." : "دوره آزمایشی ۱۵ روزه به پایان رسیده است.";
            box.addView(text(state, 15, trialActive() ? DEEP2 : RED, true));
            new AlertDialog.Builder(this).setTitle("لایسنس حسابیار").setView(box).setNegativeButton("بستن", null).setPositiveButton("فعال‌سازی", (d,w) -> showActivationDialog(false)).show();
        }
    }

    void showActivationDialog(boolean mandatory) {
        if (licenseDialogVisible || isLicensed()) return;
        licenseDialogVisible = true;

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(2));
        form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView info = text(mandatory ? "دوره آزمایشی ۱۵ روزه تمام شده است. برای ادامه، لایسنس را فعال کنید." : "ایمیل مشتری و کد لایسنس را وارد کنید.", 13, mandatory ? RED : SOFT_TEXT, false);
        info.setPadding(0, 0, 0, dp(10));
        EditText email = input("ایمیل مشتری");
        email.setInputType(33);
        EditText key = input("HYA-0001-ABCDE-F1234");
        key.setInputType(1 | 524288);
        form.addView(info);
        form.addView(email, new LinearLayout.LayoutParams(-1, dp(56)));
        Space sp = new Space(this); form.addView(sp, new LinearLayout.LayoutParams(1, dp(8)));
        form.addView(key, new LinearLayout.LayoutParams(-1, dp(56)));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("فعال‌سازی حسابیار")
                .setView(form)
                .setPositiveButton("فعال‌سازی", null);
        if (!mandatory) builder.setNegativeButton("انصراف", null);
        else builder.setNegativeButton("خروج", (d,w) -> finishAffinity());

        AlertDialog dialog = builder.create();
        dialog.setCancelable(!mandatory);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(d -> licenseDialogVisible = false);
        dialog.setOnShowListener(x -> {
            Button activate = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            activate.setOnClickListener(v -> {
                String mail = email.getText().toString().trim().toLowerCase(Locale.US);
                String code = normalizeKey(key.getText().toString());
                if (!validEmail(mail)) { email.setError("ایمیل معتبر وارد کنید"); return; }
                int serial = validateLicenseKey(code);
                if (serial <= 0) { key.setError("کد لایسنس معتبر نیست"); return; }
                prefs.edit()
                        .putBoolean("license_activated", true)
                        .putString("license_email", mail)
                        .putString("license_key", code)
                        .putInt("license_serial", serial)
                        .putLong("license_activated_at", System.currentTimeMillis())
                        .putString("backup_email", mail)
                        .apply();
                new BackupManager(this).dataChanged();
                licenseDialogVisible = false;
                dialog.dismiss();
                toast("لایسنس با موفقیت برای " + mail + " فعال شد");
                showDashboard();
            });
        });
        dialog.show();
    }

    boolean validEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        return e.length() >= 6 && e.contains("@") && e.substring(e.indexOf('@') + 1).contains(".");
    }

    String normalizeKey(String raw) {
        if (raw == null) return "";
        return raw.trim().toUpperCase(Locale.US).replace('–', '-').replace('—', '-').replaceAll("\\s+", "");
    }

    int validateLicenseKey(String raw) {
        try {
            String key = normalizeKey(raw);
            Matcher m = KEY_PATTERN.matcher(key);
            if (!m.matches()) return -1;
            int serial = Integer.parseInt(m.group(1));
            if (serial < 1 || serial > LICENSE_COUNT) return -1;
            String expected = licenseSignature(serial);
            String actual = m.group(2) + m.group(3);
            return expected.equals(actual) ? serial : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    static String licenseSignature(int serial) throws Exception {
        String message = "HESABYAR-LIC-" + String.format(Locale.US, "%04d", serial);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(LIC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : out) hex.append(String.format(Locale.US, "%02X", b));
        return hex.substring(0, 10);
    }
}