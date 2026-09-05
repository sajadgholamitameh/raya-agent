package com.anarrayaneh.invoice;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class ModernHesabyarActivity extends HesabyarActivity {
    static final int DEEP = Color.rgb(4, 38, 70);
    static final int DEEP2 = Color.rgb(1, 68, 99);
    static final int AQUA = Color.rgb(0, 166, 166);
    static final int AQUA_DARK = Color.rgb(0, 128, 128);
    static final int SKY = Color.rgb(49, 125, 222);
    static final int ORANGE = Color.rgb(242, 145, 46);
    static final int GREEN = Color.rgb(21, 163, 118);
    static final int PAGE = Color.rgb(247, 249, 252);
    static final int LINE = Color.rgb(229, 234, 241);
    static final int CARD = Color.WHITE;
    static final int INK = Color.rgb(17, 32, 51);
    static final int SOFT_TEXT = Color.rgb(105, 120, 139);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(DEEP);
        getWindow().setNavigationBarColor(DEEP);
    }

    @Override void base(String title) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(PAGE);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(28));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content = root;
        sv.addView(root);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(14), dp(18), dp(14));
        hero.setBackground(gradient(new int[]{DEEP, DEEP2, AQUA_DARK}, 24));
        hero.setElevation(dp(5));

        LinearLayout top = row();
        TextView bell = iconText("◌", 21, Color.WHITE);
        bell.setGravity(Gravity.CENTER);
        top.addView(bell, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView brand = text("حسابیار", 27, Color.WHITE, true);
        TextView subtitle = text(title, 12, Color.rgb(215, 232, 242), false);
        center.addView(brand);
        center.addView(subtitle);
        top.addView(center, new LinearLayout.LayoutParams(0, dp(58), 1));

        TextView menu = iconText("☰", 22, Color.WHITE);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> showMoreMenu());
        top.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(44)));
        hero.addView(top);
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(86)));
        space(14);

        page.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        if (title.contains("مدیریت مالی") || title.contains("داشبورد")) {
            page.addView(bottomNav(), new LinearLayout.LayoutParams(-1, dp(66)));
        }
        setContentView(page);
    }

    @Override void showDashboard() {
        ShopInfo sh = db.shop(activeShopId);
        if (sh == null) {
            activeShopId = db.firstShopId();
            sh = db.shop(activeShopId);
        }
        final ShopInfo shop = sh;
        base("مدیریت مالی کسب‌وکار");

        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.HORIZONTAL);
        selector.setGravity(Gravity.CENTER_VERTICAL);
        selector.setPadding(dp(14), dp(8), dp(14), dp(8));
        selector.setBackground(pillBg(Color.WHITE, DEEP2));
        selector.setElevation(dp(2));
        TextView storeIcon = iconBubble("▣", AQUA, Color.rgb(224, 249, 246));
        selector.addView(storeIcon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout st = new LinearLayout(this);
        st.setOrientation(LinearLayout.VERTICAL);
        TextView active = text("فروشگاه فعال", 11, SOFT_TEXT, false);
        TextView storeName = text(shop == null ? "فروشگاه" : shop.name, 16, INK, true);
        st.addView(active); st.addView(storeName);
        selector.addView(st, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = iconText("⌄", 24, DEEP2);
        arrow.setGravity(Gravity.CENTER);
        selector.addView(arrow, new LinearLayout.LayoutParams(dp(40), dp(40)));
        selector.setOnClickListener(v -> showShops());
        content.addView(selector, new LinearLayout.LayoutParams(-1, dp(62)));
        space(14);

        long sales = db.sum("SALE", currentMonth, activeShopId);
        long expenses = db.sum("EXPENSE", currentMonth, activeShopId);
        long receivable = db.totalReceivable(activeShopId);
        long banksBalance = 0;
        for (BankOption bo : db.bankOptions(activeShopId, false)) banksBalance += db.bankBalance(bo.id);

        LinearLayout s1 = row();
        s1.addView(metric("▥", "فروش این ماه", money(sales), GREEN, Color.rgb(229, 248, 239)), new LinearLayout.LayoutParams(0, dp(112), 1));
        gap(s1);
        s1.addView(metric("▣", "هزینه این ماه", money(expenses), RED, Color.rgb(253, 235, 238)), new LinearLayout.LayoutParams(0, dp(112), 1));
        content.addView(s1); space(9);
        LinearLayout s2 = row();
        s2.addView(metric("◉", "مطالبات", money(receivable), ORANGE, Color.rgb(255, 243, 225)), new LinearLayout.LayoutParams(0, dp(112), 1));
        gap(s2);
        s2.addView(metric("▤", "مانده حساب‌ها", money(banksBalance), SKY, Color.rgb(231, 241, 255)), new LinearLayout.LayoutParams(0, dp(112), 1));
        content.addView(s2); space(18);

        content.addView(section("عملیات سریع"));
        LinearLayout q = row();
        q.addView(quickTile("＋", "فروش جدید", GREEN, Color.rgb(228, 248, 239), v -> saleDialog()), new LinearLayout.LayoutParams(0, dp(92), 1));
        gap(q);
        q.addView(quickTile("−", "ثبت هزینه", RED, Color.rgb(253, 232, 236), v -> expenseDialog()), new LinearLayout.LayoutParams(0, dp(92), 1));
        gap(q);
        q.addView(quickTile("⇩", "دریافت", ORANGE, Color.rgb(255, 242, 224), v -> receiptDialog()), new LinearLayout.LayoutParams(0, dp(92), 1));
        gap(q);
        q.addView(quickTile("▥", "گزارشات", SKY, Color.rgb(231, 241, 255), v -> showReport(currentMonth)), new LinearLayout.LayoutParams(0, dp(92), 1));
        content.addView(q); space(14);

        LinearLayout q2 = row();
        q2.addView(softMenu("فروشگاه‌ها", "چند شعبه", "▣", v -> showShops()), new LinearLayout.LayoutParams(0, dp(74), 1)); gap(q2);
        q2.addView(softMenu("حساب‌ها", "بانکی و نقدی", "▤", v -> showBankAccounts()), new LinearLayout.LayoutParams(0, dp(74), 1)); gap(q2);
        q2.addView(softMenu("بکاپ", "Gmail", "☁", v -> showBackup()), new LinearLayout.LayoutParams(0, dp(74), 1));
        content.addView(q2); space(20);

        LinearLayout recentHead = row();
        TextView rh = section("آخرین تراکنش‌ها");
        recentHead.addView(rh, new LinearLayout.LayoutParams(0, -2, 1));
        TextView see = text("مشاهده گزارش", 12, SKY, true);
        see.setGravity(Gravity.CENTER);
        see.setOnClickListener(v -> showReport(currentMonth));
        recentHead.addView(see, new LinearLayout.LayoutParams(dp(95), dp(36)));
        content.addView(recentHead);

        Cursor cur = db.raw("SELECT t.id,t.date,t.type,t.description,t.amount,t.invoice_no,c.name,b.bank_name,b.card_no FROM tx t LEFT JOIN customers c ON c.id=t.customer_id LEFT JOIN bank_accounts b ON b.id=t.bank_account_id WHERE t.shop_id=? ORDER BY t.id DESC LIMIT 6", new String[]{String.valueOf(activeShopId)});
        if (!cur.moveToFirst()) {
            content.addView(empty("هنوز تراکنشی ثبت نشده است. اولین فروش یا هزینه را ثبت کنید."));
        } else do {
            long id = cur.getLong(0), amount = cur.getLong(4);
            String date = cur.getString(1), type = cur.getString(2), desc = cur.getString(3), inv = cur.getString(5), cust = cur.getString(6), bank = cur.getString(7), cardNo = cur.getString(8);
            String title = type.equals("SALE") ? "فروش" + (inv == null ? "" : " • " + inv) : type.equals("EXPENSE") ? "هزینه" : "دریافت از مشتری";
            String detail = (cust == null || cust.isEmpty() ? "" : cust + " • ") + (desc == null ? "" : desc) + "\n" + date + (bank == null ? " • نقدی" : " • " + bank + maskCard(cardNo));
            String signed = type.equals("EXPENSE") ? "− " + money(amount) : "+ " + money(amount);
            LinearLayout card = transactionCard(title, detail, signed, type.equals("EXPENSE") ? RED : GREEN);
            if (type.equals("SALE")) card.setOnClickListener(v -> invoiceOptions(id));
            content.addView(card); space(8);
        } while (cur.moveToNext());
        cur.close();

        TextView foot = text("حسابیار 1.3 • توسعه و پشتیبانی: " + DEV_COMPANY, 11, SOFT_TEXT, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(16), 0, dp(6));
        content.addView(foot);
    }

    void showMoreMenu() {
        new AlertDialog.Builder(this)
                .setTitle("حسابیار")
                .setItems(new String[]{"مدیریت فروشگاه‌ها", "حساب‌های بانکی", "بکاپ و بازیابی", "تنظیمات فروشگاه", "ارتباط با ما"}, (d, which) -> {
                    if (which == 0) showShops();
                    else if (which == 1) showBankAccounts();
                    else if (which == 2) showBackup();
                    else if (which == 3) settingsDialog();
                    else showContact();
                }).show();
    }

    LinearLayout bottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(5), dp(10), dp(5));
        nav.setBackground(gradient(new int[]{DEEP, Color.rgb(3, 55, 88)}, 0));
        nav.setElevation(dp(9));
        nav.addView(navItem("⌂", "خانه", true, v -> showDashboard()), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem("▤", "حساب‌ها", false, v -> showBankAccounts()), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem("▥", "گزارشات", false, v -> showReport(currentMonth)), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem("•••", "بیشتر", false, v -> showMoreMenu()), new LinearLayout.LayoutParams(0, -1, 1));
        return nav;
    }

    View navItem(String icon, String label, boolean selected, View.OnClickListener click) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        TextView i = text(icon, 19, selected ? GOLD : Color.rgb(185, 206, 219), true);
        i.setGravity(Gravity.CENTER);
        TextView t = text(label, 10, selected ? Color.WHITE : Color.rgb(185, 206, 219), selected);
        t.setGravity(Gravity.CENTER);
        l.addView(i); l.addView(t);
        l.setOnClickListener(click);
        return l;
    }

    View metric(String icon, String label, String value, int accent, int bubble) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(13), dp(11), dp(13), dp(10));
        box.setBackground(cardBg(18));
        box.setElevation(dp(2));
        LinearLayout top = row();
        TextView ic = iconBubble(icon, accent, bubble);
        top.addView(ic, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView lb = text(label, 12, SOFT_TEXT, false);
        top.addView(lb, new LinearLayout.LayoutParams(0, dp(34), 1));
        box.addView(top);
        TextView val = text(value, 16, INK, true);
        val.setPadding(0, dp(7), 0, 0);
        box.addView(val);
        return box;
    }

    View quickTile(String icon, String label, int accent, int bubble, View.OnClickListener click) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(dp(4), dp(7), dp(4), dp(6));
        l.setBackground(cardBg(17));
        l.setElevation(dp(2));
        TextView ic = iconBubble(icon, accent, bubble);
        l.addView(ic, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView t = text(label, 11, INK, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(5), 0, 0);
        l.addView(t);
        l.setOnClickListener(click);
        return l;
    }

    View softMenu(String title, String sub, String icon, View.OnClickListener click) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(dp(10), dp(8), dp(10), dp(8));
        l.setBackground(cardBg(16));
        l.setElevation(dp(1));
        TextView ic = iconBubble(icon, SKY, Color.rgb(232, 241, 252));
        l.addView(ic, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout tx = new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(text(title, 13, INK, true));
        tx.addView(text(sub, 10, SOFT_TEXT, false));
        l.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        l.setOnClickListener(click);
        return l;
    }

    TextView iconBubble(String icon, int color, int bg) {
        TextView t = iconText(icon, 17, color);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(bg, 13));
        return t;
    }

    TextView iconText(String value, int size, int color) {
        TextView t = text(value, size, color, true);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    GradientDrawable cardBg(float radius) {
        GradientDrawable g = round(CARD, radius);
        g.setStroke(dp(1), LINE);
        return g;
    }

    GradientDrawable pillBg(int color, int stroke) {
        GradientDrawable g = round(color, 22);
        g.setStroke(dp(1), Color.argb(45, Color.red(stroke), Color.green(stroke), Color.blue(stroke)));
        return g;
    }

    GradientDrawable gradient(int[] colors, float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        g.setCornerRadius(dp(radius));
        return g;
    }

    @Override View stat(String label, String value, int accent) {
        return metric("●", label, value, accent, Color.rgb(240, 245, 250));
    }

    @Override TextView section(String s) {
        TextView t = text(s, 17, INK, true);
        t.setPadding(dp(2), dp(2), dp(2), dp(9));
        return t;
    }

    @Override TextView empty(String s) {
        TextView t = text(s, 13, SOFT_TEXT, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(18), dp(24), dp(18), dp(24));
        t.setBackground(cardBg(17));
        t.setElevation(dp(1));
        return t;
    }

    @Override Button action(String s, int color, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(13); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(round(color, 16));
        b.setElevation(dp(2));
        b.setOnClickListener(click);
        return b;
    }

    @Override Button outlineAction(String s, int color, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(13); b.setTextColor(color); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable g = round(Color.WHITE, 16); g.setStroke(dp(1), color);
        b.setBackground(g); b.setElevation(dp(1)); b.setOnClickListener(click);
        return b;
    }

    @Override Button smallButton(String s, int color) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(12); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(round(color, 15)); b.setElevation(dp(1));
        return b;
    }

    @Override LinearLayout transactionCard(String title, String detail, String amount, int accent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(12), dp(11), dp(12), dp(11));
        box.setBackground(cardBg(17));
        box.setElevation(dp(1));
        TextView icon = iconBubble(accent == RED ? "−" : "+", accent, accent == RED ? Color.rgb(253,235,238) : Color.rgb(229,248,239));
        box.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout tx = new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL); tx.setPadding(dp(8),0,0,0);
        TextView a = text(title, 14, INK, true), d = text(detail, 11, SOFT_TEXT, false); d.setPadding(0, dp(4), 0, 0);
        tx.addView(a); tx.addView(d); box.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        TextView am = text(amount, 13, accent, true); am.setGravity(Gravity.CENTER);
        box.addView(am, new LinearLayout.LayoutParams(dp(118), -1));
        return box;
    }

    @Override EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setText(value); e.setTextDirection(View.TEXT_DIRECTION_RTL); e.setTextSize(15);
        e.setTextColor(INK); e.setHintTextColor(Color.rgb(150,160,174)); e.setSingleLine(false);
        e.setPadding(dp(14), dp(12), dp(14), dp(12)); e.setMinHeight(dp(52));
        e.setBackground(cardBg(14));
        return e;
    }

    @Override Spinner spinner(String[] a) {
        Spinner s = super.spinner(a);
        s.setBackground(cardBg(14)); s.setPadding(dp(10), dp(8), dp(10), dp(8)); s.setMinimumHeight(dp(50));
        return s;
    }

    @Override Spinner bankSpinner(long shopId, boolean includeCash) {
        Spinner s = super.bankSpinner(shopId, includeCash);
        s.setBackground(cardBg(14)); s.setPadding(dp(10), dp(8), dp(10), dp(8)); s.setMinimumHeight(dp(50));
        return s;
    }

    @Override void saleDialog() {
        LinearLayout f = form();
        EditText customer = input("نام مشتری (اختیاری)"), phone = input("موبایل مشتری (اختیاری)"), desc = input("شرح کالا / خدمات"), amount = moneyInput("مبلغ کل (تومان)"), paid = moneyInput("پرداختی الان (تومان)"), date = input("تاریخ شمسی", Jalali.today());
        Spinner bank = bankSpinner(activeShopId, true);
        f.addView(customer); spaceInside(f, 7); f.addView(phone); spaceInside(f, 7); f.addView(desc); spaceInside(f, 7); f.addView(amount); spaceInside(f, 7); f.addView(paid); spaceInside(f, 7); f.addView(label("حساب دریافت وجه")); f.addView(bank); spaceInside(f, 7); f.addView(date);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("فاکتور فروش جدید").setView(wrap(f)).setNegativeButton("انصراف", null).setPositiveButton("ثبت فاکتور", null).create();
        d.setOnShowListener(x -> d.getButton(-1).setOnClickListener(v -> {
            long a = num(amount.getText().toString()); if (a <= 0) { amount.setError("مبلغ را وارد کنید"); return; }
            long p = num(paid.getText().toString()); if (p > a) p = a;
            String dt = date.getText().toString().trim(); if (!validDate(dt)) { date.setError("مثال: 1405/06/14"); return; }
            long cid = 0; String cn = customer.getText().toString().trim();
            if (!cn.isEmpty()) cid = db.customer(cn, phone.getText().toString().trim(), activeShopId); else p = a;
            BankOption bo = (BankOption) bank.getSelectedItem(); long bid = (p > 0 && bo != null) ? bo.id : 0;
            String month = dt.substring(0,7), inv = db.nextInvoice(month, activeShopId);
            long id = db.addTx(dt, month, "SALE", cid, desc.getText().toString().trim(), a, p, inv, activeShopId, bid);
            d.dismiss(); showDashboard();
            new AlertDialog.Builder(this)
                    .setTitle("فاکتور ثبت شد")
                    .setMessage("شماره فاکتور: " + inv + "\nمبلغ: " + money(a) + "\nمانده مشتری: " + money(a-p))
                    .setNegativeButton("بستن", null)
                    .setNeutralButton("ذخیره PDF", (q,w) -> exportInvoice(id))
                    .setPositiveButton("ارسال فاکتور", (q,w) -> shareInvoice(id))
                    .show();
        }));
        d.show();
    }

    @Override void invoiceOptions(long id) {
        new AlertDialog.Builder(this).setTitle("فاکتور فروش")
                .setItems(new String[]{"ارسال فاکتور PDF", "ذخیره PDF فاکتور", "حذف این فروش"}, (d,w) -> {
                    if (w == 0) shareInvoice(id);
                    else if (w == 1) exportInvoice(id);
                    else confirmDelete(id);
                }).show();
    }

    @Override void exportInvoice(long id) {
        Uri uri = createInvoicePdf(id);
        if (uri != null) toast("فاکتور PDF در Downloads/Hesabyar/Invoices ذخیره شد");
    }

    void shareInvoice(long id) {
        Uri uri = createInvoicePdf(id);
        if (uri == null) return;
        Cursor c = db.raw("SELECT t.invoice_no,t.amount,c.name,s.name FROM tx t LEFT JOIN customers c ON c.id=t.customer_id LEFT JOIN shops s ON s.id=t.shop_id WHERE t.id=?", new String[]{String.valueOf(id)});
        String inv = "", customer = "", shop = "حسابیار"; long amount = 0;
        if (c.moveToFirst()) { inv = nz(c.getString(0)); amount = c.getLong(1); customer = nz(c.getString(2)); shop = nz(c.getString(3)); }
        c.close();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, "فاکتور " + inv + " - " + shop);
        String msg = "فاکتور فروش " + inv + "\n" + (customer.isEmpty() ? "" : "مشتری: " + customer + "\n") + "مبلغ: " + money(amount) + "\nارسال‌شده با حسابیار";
        send.putExtra(Intent.EXTRA_TEXT, msg);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newRawUri("invoice", uri));
        try { startActivity(Intent.createChooser(send, "ارسال فاکتور با")); }
        catch (Exception e) { toast("برنامه‌ای برای ارسال فایل پیدا نشد"); }
    }

    Uri createInvoicePdf(long id) {
        Cursor c = db.raw("SELECT t.date,t.description,t.amount,t.paid,t.invoice_no,c.name,c.phone,s.name,s.phone,s.address,b.bank_name,b.card_no FROM tx t LEFT JOIN customers c ON c.id=t.customer_id LEFT JOIN shops s ON s.id=t.shop_id LEFT JOIN bank_accounts b ON b.id=t.bank_account_id WHERE t.id=?", new String[]{String.valueOf(id)});
        if (!c.moveToFirst()) { c.close(); return null; }
        String date = c.getString(0), desc = c.getString(1), inv = c.getString(4), customer = c.getString(5), custPhone = c.getString(6), shopName = c.getString(7), shopPhone = c.getString(8), address = c.getString(9), bank = c.getString(10), cardNo = c.getString(11);
        long amount = c.getLong(2), paid = c.getLong(3); c.close();
        if (shopName == null || shopName.isEmpty()) shopName = "فروشگاه";

        PdfDocument pdf = new PdfDocument();
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
        Canvas cv = page.getCanvas(); Paint p = new Paint(1); p.setTypeface(Typeface.create("sans", Typeface.NORMAL)); p.setTextAlign(Paint.Align.RIGHT);
        p.setColor(DEEP); cv.drawRect(0,0,595,126,p);
        p.setColor(AQUA); cv.drawRect(0,120,595,126,p);
        p.setColor(Color.WHITE); p.setTextSize(29); p.setTypeface(Typeface.DEFAULT_BOLD); cv.drawText(shopName,550,48,p);
        p.setTextSize(14); p.setTypeface(Typeface.DEFAULT); cv.drawText("فاکتور فروش رسمی کسب‌وکار",550,76,p);
        p.setTextSize(13); cv.drawText("شماره: " + inv + "     تاریخ: " + date,550,104,p);

        p.setColor(INK); p.setTextSize(16); p.setTypeface(Typeface.DEFAULT_BOLD); int y=164;
        cv.drawText("مشخصات مشتری",550,y,p); y+=20;
        p.setColor(Color.rgb(244,247,250)); cv.drawRoundRect(40,y,555,y+72,14,14,p);
        p.setColor(INK); p.setTypeface(Typeface.DEFAULT); p.setTextSize(14);
        cv.drawText("نام: " + (customer==null||customer.isEmpty()?"مشتری نقدی":customer),535,y+27,p);
        cv.drawText("تلفن: " + (custPhone==null||custPhone.isEmpty()?"—":custPhone),535,y+53,p);
        y+=104;

        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(16); cv.drawText("شرح فاکتور",550,y,p); y+=20;
        p.setColor(Color.rgb(244,247,250)); cv.drawRoundRect(40,y,555,y+108,14,14,p);
        p.setColor(INK); p.setTypeface(Typeface.DEFAULT); p.setTextSize(14);
        cv.drawText("شرح: " + shorten(desc,48),535,y+28,p);
        cv.drawText("مبلغ کل: " + money(amount),535,y+58,p);
        cv.drawText("پرداختی: " + money(paid),535,y+86,p);
        y+=142;

        p.setColor(amount-paid>0?RED:GREEN); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(21);
        cv.drawText("مانده: " + money(amount-paid),535,y,p); y+=38;
        p.setColor(INK); p.setTextSize(14); p.setTypeface(Typeface.DEFAULT);
        cv.drawText("روش دریافت: " + (bank==null?"نقدی":bank+maskCard(cardNo)),535,y,p);

        p.setColor(DEEP); cv.drawRoundRect(40,690,555,792,16,16,p);
        p.setColor(Color.WHITE); p.setTextSize(14); p.setTypeface(Typeface.DEFAULT_BOLD); cv.drawText(shopName,535,720,p);
        p.setTypeface(Typeface.DEFAULT); p.setTextSize(12);
        if (shopPhone!=null&&!shopPhone.isEmpty()) cv.drawText("تماس: " + shopPhone,535,746,p);
        if (address!=null&&!address.isEmpty()) cv.drawText(shorten(address,58),535,770,p);
        p.setColor(Color.rgb(200,220,231)); p.setTextSize(10); cv.drawText("صادر شده با نرم‌افزار حسابیار",535,788,p);
        pdf.finishPage(page);

        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, "فاکتور_" + inv + ".pdf");
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Hesabyar/Invoices");
            Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new IOException("no uri");
            OutputStream os = getContentResolver().openOutputStream(u); pdf.writeTo(os); os.close(); pdf.close(); return u;
        } catch (Exception e) {
            try { pdf.close(); } catch (Exception ignored) {}
            toast("خطا در ساخت فاکتور PDF"); return null;
        }
    }
}
