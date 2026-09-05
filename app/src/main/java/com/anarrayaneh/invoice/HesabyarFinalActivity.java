package com.anarrayaneh.invoice;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;

import java.util.*;

public class HesabyarFinalActivity extends ModernHesabyarActivity {

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(DEEP);
        getWindow().setNavigationBarColor(DEEP);
    }

    @Override void showDashboard() {
        ShopInfo sh = db.shop(activeShopId);
        if (sh == null) {
            activeShopId = db.firstShopId();
            sh = db.shop(activeShopId);
        }
        final ShopInfo shop = sh;

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(PAGE);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(12), 0, dp(12), dp(24));
        content = root;
        scroll.addView(root);

        // Hero header – implemented to match the approved mockup.
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(gradient(new int[]{Color.rgb(5,45,82), Color.rgb(3,77,111), Color.rgb(0,121,136)}, 0));

        View waveA = new View(this);
        GradientDrawable waveAG = gradient(new int[]{Color.rgb(0,198,183), Color.rgb(0,146,153)}, 48);
        waveA.setBackground(waveAG);
        waveA.setAlpha(0.60f);
        FrameLayout.LayoutParams wa = new FrameLayout.LayoutParams(dp(260), dp(82), Gravity.BOTTOM | Gravity.LEFT);
        wa.leftMargin = -dp(80); wa.bottomMargin = -dp(25);
        hero.addView(waveA, wa);

        View waveB = new View(this);
        GradientDrawable waveBG = gradient(new int[]{Color.rgb(6,107,145), Color.rgb(0,174,169)}, 52);
        waveB.setBackground(waveBG);
        waveB.setAlpha(0.45f);
        FrameLayout.LayoutParams wb = new FrameLayout.LayoutParams(dp(255), dp(66), Gravity.BOTTOM | Gravity.RIGHT);
        wb.rightMargin = -dp(85); wb.bottomMargin = dp(8);
        hero.addView(waveB, wb);

        LinearLayout heroContent = new LinearLayout(this);
        heroContent.setOrientation(LinearLayout.VERTICAL);
        heroContent.setPadding(dp(16), dp(12), dp(16), dp(18));
        heroContent.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout top = row();
        TextView bell = text("♧", 21, Color.WHITE, true);
        bell.setGravity(Gravity.CENTER);
        top.addView(bell, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout brandBox = new LinearLayout(this);
        brandBox.setOrientation(LinearLayout.VERTICAL);
        brandBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView brand = text("حسابیار", 29, Color.WHITE, true);
        TextView subtitle = text("مدیریت مالی کسب‌وکار، ساده‌تر", 12, Color.rgb(218,236,245), false);
        brandBox.addView(brand);
        brandBox.addView(subtitle);
        top.addView(brandBox, new LinearLayout.LayoutParams(0, dp(60), 1));

        TextView menu = text("☰", 23, Color.WHITE, true);
        menu.setGravity(Gravity.CENTER);
        menu.setOnClickListener(v -> showMoreMenu());
        top.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(44)));
        heroContent.addView(top);

        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.HORIZONTAL);
        selector.setGravity(Gravity.CENTER_VERTICAL);
        selector.setPadding(dp(13), dp(7), dp(13), dp(7));
        GradientDrawable selectorBg = round(Color.argb(38,255,255,255), 24);
        selectorBg.setStroke(dp(1), Color.argb(90,255,255,255));
        selector.setBackground(selectorBg);
        TextView store = text("▣", 19, Color.WHITE, true);
        store.setGravity(Gravity.CENTER);
        selector.addView(store, new LinearLayout.LayoutParams(dp(38), dp(38)));
        TextView storeName = text("فروشگاه فعال: " + (shop == null ? "فروشگاه" : shop.name), 14, Color.WHITE, true);
        storeName.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        selector.addView(storeName, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView down = text("⌄", 22, Color.WHITE, true); down.setGravity(Gravity.CENTER);
        selector.addView(down, new LinearLayout.LayoutParams(dp(38), dp(38)));
        selector.setOnClickListener(v -> showShops());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, dp(55));
        slp.topMargin = dp(4);
        heroContent.addView(selector, slp);

        LinearLayout greet = row();
        TextView growth = text("رشد امروز، آغاز فرداست", 11, Color.rgb(191,231,230), false);
        growth.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        greet.addView(growth, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView hello = text("سلام، روز خوبی داشته باشید 👋", 14, Color.WHITE, true);
        hello.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        greet.addView(hello, new LinearLayout.LayoutParams(0, dp(48), 1));
        heroContent.addView(greet);

        hero.addView(heroContent, new FrameLayout.LayoutParams(-1, -1));
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(210)));

        long sales = db.sum("SALE", currentMonth, activeShopId);
        long expenses = db.sum("EXPENSE", currentMonth, activeShopId);
        long receivable = db.totalReceivable(activeShopId);
        long bankBalance = 0;
        for (BankOption bo : db.bankOptions(activeShopId, false)) bankBalance += db.bankBalance(bo.id);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.VERTICAL);
        metrics.setPadding(dp(1), 0, dp(1), 0);
        metrics.setTranslationY(-dp(22));

        LinearLayout m1 = row();
        m1.addView(metric("▥", "فروش امروز", money(sales), GREEN, Color.rgb(226,247,237)), new LinearLayout.LayoutParams(0, dp(118), 1));
        gap(m1);
        m1.addView(metric("▣", "هزینه‌ها", money(expenses), RED, Color.rgb(253,232,237)), new LinearLayout.LayoutParams(0, dp(118), 1));
        metrics.addView(m1);
        Space mg = new Space(this); metrics.addView(mg, new LinearLayout.LayoutParams(1, dp(9)));
        LinearLayout m2 = row();
        m2.addView(metric("◉", "مطالبات", money(receivable), ORANGE, Color.rgb(255,241,224)), new LinearLayout.LayoutParams(0, dp(118), 1));
        gap(m2);
        m2.addView(metric("▤", "مانده حساب", money(bankBalance), SKY, Color.rgb(229,241,255)), new LinearLayout.LayoutParams(0, dp(118), 1));
        metrics.addView(m2);
        root.addView(metrics);

        TextView quickTitle = section("عملیات سریع");
        quickTitle.setTranslationY(-dp(12));
        root.addView(quickTitle);

        LinearLayout quick = row();
        quick.setTranslationY(-dp(12));
        quick.addView(quickTile("＋", "ثبت فاکتور", GREEN, Color.rgb(225,248,238), v -> saleDialog()), new LinearLayout.LayoutParams(0, dp(96), 1));
        gap(quick);
        quick.addView(quickTile("▣", "ثبت هزینه", RED, Color.rgb(253,231,236), v -> expenseDialog()), new LinearLayout.LayoutParams(0, dp(96), 1));
        gap(quick);
        quick.addView(quickTile("⇩", "دریافت", SKY, Color.rgb(230,241,255), v -> receiptDialog()), new LinearLayout.LayoutParams(0, dp(96), 1));
        gap(quick);
        quick.addView(quickTile("▥", "گزارشات", Color.rgb(100,80,225), Color.rgb(239,235,255), v -> showReport(currentMonth)), new LinearLayout.LayoutParams(0, dp(96), 1));
        root.addView(quick);

        // Prominent invoice banner from the approved design.
        LinearLayout invoiceBanner = new LinearLayout(this);
        invoiceBanner.setOrientation(LinearLayout.HORIZONTAL);
        invoiceBanner.setGravity(Gravity.CENTER_VERTICAL);
        invoiceBanner.setPadding(dp(14), dp(12), dp(14), dp(12));
        invoiceBanner.setBackground(gradient(new int[]{Color.rgb(224,251,248), Color.rgb(228,245,255)}, 20));
        invoiceBanner.setElevation(dp(2));
        LinearLayout invoiceText = new LinearLayout(this);
        invoiceText.setOrientation(LinearLayout.VERTICAL);
        TextView bt = text("صدور و ارسال فاکتور", 16, INK, true);
        TextView bs = text("فاکتور حرفه‌ای بسازید، PDF ذخیره کنید و از واتساپ، تلگرام یا Gmail ارسال کنید.", 11, SOFT_TEXT, false);
        bs.setPadding(0, dp(4), 0, 0);
        invoiceText.addView(bt); invoiceText.addView(bs);
        invoiceBanner.addView(invoiceText, new LinearLayout.LayoutParams(0, -2, 1));
        Button invoiceBtn = new Button(this);
        invoiceBtn.setText("ثبت فاکتور جدید");
        invoiceBtn.setTextColor(Color.WHITE);
        invoiceBtn.setTextSize(12);
        invoiceBtn.setAllCaps(false);
        invoiceBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        invoiceBtn.setBackground(gradient(new int[]{AQUA, DEEP2}, 20));
        invoiceBtn.setOnClickListener(v -> saleDialog());
        invoiceBanner.addView(invoiceBtn, new LinearLayout.LayoutParams(dp(128), dp(50)));
        LinearLayout.LayoutParams iblp = new LinearLayout.LayoutParams(-1, dp(104));
        iblp.topMargin = -dp(3); iblp.bottomMargin = dp(18);
        root.addView(invoiceBanner, iblp);

        LinearLayout recentHead = row();
        TextView see = text("مشاهده همه ←", 12, SKY, true);
        see.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        see.setOnClickListener(v -> showReport(currentMonth));
        recentHead.addView(see, new LinearLayout.LayoutParams(dp(105), dp(38)));
        TextView recent = section("آخرین تراکنش‌ها");
        recent.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        recentHead.addView(recent, new LinearLayout.LayoutParams(0, dp(38), 1));
        root.addView(recentHead);

        Cursor cur = db.raw("SELECT t.id,t.date,t.type,t.description,t.amount,t.invoice_no,c.name,b.bank_name,b.card_no FROM tx t LEFT JOIN customers c ON c.id=t.customer_id LEFT JOIN bank_accounts b ON b.id=t.bank_account_id WHERE t.shop_id=? ORDER BY t.id DESC LIMIT 6", new String[]{String.valueOf(activeShopId)});
        if (!cur.moveToFirst()) {
            root.addView(empty("هنوز تراکنشی ثبت نشده است. برای شروع «ثبت فاکتور» را بزنید."));
        } else do {
            long id = cur.getLong(0), amount = cur.getLong(4);
            String date = cur.getString(1), type = cur.getString(2), desc = cur.getString(3), inv = cur.getString(5), cust = cur.getString(6), bank = cur.getString(7), cardNo = cur.getString(8);
            String title = type.equals("SALE") ? "فروش" + (cust == null || cust.isEmpty() ? "" : " به " + cust) : type.equals("EXPENSE") ? "هزینه" : "دریافت از مشتری";
            String detail = date + (inv == null ? "" : " • فاکتور " + inv) + (desc == null || desc.isEmpty() ? "" : " • " + shorten(desc, 22)) + (bank == null ? " • نقدی" : " • " + bank + maskCard(cardNo));
            String signed = type.equals("EXPENSE") ? "− " + money(amount) : "+ " + money(amount);
            LinearLayout card = transactionCard(title, detail, signed, type.equals("EXPENSE") ? RED : GREEN);
            if (type.equals("SALE")) card.setOnClickListener(v -> invoiceOptions(id));
            root.addView(card);
            Space sp = new Space(this); root.addView(sp, new LinearLayout.LayoutParams(1, dp(8)));
        } while (cur.moveToNext());
        cur.close();

        TextView foot = text("حسابیار • توسعه و پشتیبانی: " + DEV_COMPANY, 11, SOFT_TEXT, false);
        foot.setGravity(Gravity.CENTER); foot.setPadding(0, dp(16), 0, dp(8));
        root.addView(foot);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        page.addView(finalBottomNav(), new LinearLayout.LayoutParams(-1, dp(74)));
        setContentView(page);
    }

    LinearLayout finalBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(5), dp(8), dp(5));
        nav.setBackground(gradient(new int[]{Color.rgb(3,43,75), Color.rgb(2,69,101)}, 0));
        nav.setElevation(dp(12));
        nav.addView(finalNavItem("⌂", "خانه", true, v -> showDashboard()), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(finalNavItem("▤", "حساب‌ها", false, v -> showBankAccounts()), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(fabItem(), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(finalNavItem("▥", "گزارشات", false, v -> showReport(currentMonth)), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(finalNavItem("•••", "بیشتر", false, v -> showMoreMenu()), new LinearLayout.LayoutParams(0, -1, 1));
        return nav;
    }

    View finalNavItem(String icon, String label, boolean active, View.OnClickListener click) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER);
        TextView i = text(icon, 19, active ? Color.rgb(22,231,206) : Color.rgb(184,205,218), true); i.setGravity(Gravity.CENTER);
        TextView t = text(label, 10, active ? Color.rgb(22,231,206) : Color.rgb(206,219,227), active); t.setGravity(Gravity.CENTER);
        l.addView(i); l.addView(t); l.setOnClickListener(click); return l;
    }

    View fabItem() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER);
        TextView plus = text("＋", 29, Color.WHITE, false); plus.setGravity(Gravity.CENTER);
        plus.setBackground(gradient(new int[]{Color.rgb(0,197,187), Color.rgb(0,137,151)}, 40));
        plus.setElevation(dp(8));
        l.addView(plus, new LinearLayout.LayoutParams(dp(54), dp(54)));
        TextView label = text("عملیات سریع", 9, Color.WHITE, true); label.setGravity(Gravity.CENTER);
        l.addView(label);
        l.setOnClickListener(v -> saleDialog());
        return l;
    }

    @Override void saleDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(PAGE);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18), dp(14), dp(18), dp(14));
        head.setBackground(gradient(new int[]{DEEP, DEEP2, AQUA_DARK}, 0));
        LinearLayout htop = row();
        TextView close = text("×", 28, Color.WHITE, false); close.setGravity(Gravity.CENTER); close.setOnClickListener(v -> dialog.dismiss());
        htop.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout ttl = new LinearLayout(this); ttl.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("ثبت فاکتور", 26, Color.WHITE, true);
        TextView sub = text("صدور فاکتور فروش در چند ثانیه", 12, Color.rgb(214,233,243), false);
        ttl.addView(title); ttl.addView(sub);
        htop.addView(ttl, new LinearLayout.LayoutParams(0, dp(58), 1));
        TextView mark = text("▥", 23, Color.rgb(36,229,205), true); mark.setGravity(Gravity.CENTER);
        htop.addView(mark, new LinearLayout.LayoutParams(dp(45), dp(45)));
        head.addView(htop);
        shell.addView(head, new LinearLayout.LayoutParams(-1, dp(92)));

        ScrollView sv = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(24));
        body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout formCard = new LinearLayout(this);
        formCard.setOrientation(LinearLayout.VERTICAL);
        formCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        formCard.setBackground(cardBg(20));
        formCard.setElevation(dp(2));
        formCard.addView(text("اطلاعات فاکتور", 18, INK, true));
        TextView fs = text("اطلاعات مشتری و اقلام فاکتور را وارد کنید", 11, SOFT_TEXT, false); fs.setPadding(0, dp(3), 0, dp(10)); formCard.addView(fs);

        EditText customer = input("نام مشتری");
        EditText phone = input("موبایل مشتری");
        EditText desc = input("شرح کالا / خدمات");
        EditText amount = moneyInput("مبلغ کل (تومان)");
        EditText paid = moneyInput("پرداختی (تومان)");
        Spinner bank = bankSpinner(activeShopId, true);
        EditText date = input("تاریخ شمسی", Jalali.today());

        addField(formCard, "نام مشتری", customer, false);
        addField(formCard, "موبایل مشتری", phone, false);
        addField(formCard, "شرح کالا / خدمات *", desc, false);
        addField(formCard, "مبلغ کل *", amount, false);
        addField(formCard, "پرداختی", paid, false);
        formCard.addView(text("حساب دریافت وجه", 12, SOFT_TEXT, true));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(54)); bp.bottomMargin = dp(9); formCard.addView(bank, bp);
        addField(formCard, "تاریخ شمسی *", date, false);
        body.addView(formCard);

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setPadding(dp(14), dp(12), dp(14), dp(12));
        summary.setBackground(gradient(new int[]{Color.rgb(226,252,244), Color.rgb(231,248,255)}, 18));
        summary.setElevation(dp(1));
        LinearLayout.LayoutParams sump = new LinearLayout.LayoutParams(-1, -2); sump.topMargin = dp(12); body.addView(summary, sump);
        summary.addView(text("خلاصه فاکتور", 16, INK, true));
        String previewInv = db.nextInvoice(Jalali.today().substring(0,7), activeShopId);
        TextView invLine = text("شماره فاکتور: " + previewInv, 12, SOFT_TEXT, false); invLine.setPadding(0, dp(6), 0, dp(3)); summary.addView(invLine);
        TextView totalLine = text("جمع کل: 0 تومان", 14, INK, true); summary.addView(totalLine);
        TextView remainLine = text("مانده: 0 تومان", 14, GREEN, true); remainLine.setPadding(0, dp(4), 0, 0); summary.addView(remainLine);

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                long a = num(amount.getText().toString());
                long p = num(paid.getText().toString());
                if (p > a) p = a;
                totalLine.setText("جمع کل: " + money(a));
                remainLine.setText("مانده: " + money(Math.max(0, a-p)));
            }
            public void afterTextChanged(Editable e) {}
        };
        amount.addTextChangedListener(watcher); paid.addTextChangedListener(watcher);

        LinearLayout buttons = row();
        Button save = modernPrimaryButton("ثبت فاکتور");
        Button saveShare = modernOutlineButton("ذخیره و ارسال PDF");
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(56), 1));
        gap(buttons);
        buttons.addView(saveShare, new LinearLayout.LayoutParams(0, dp(56), 1));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(56)); blp.topMargin = dp(14); body.addView(buttons, blp);

        TextView note = text("پس از ثبت می‌توانید PDF فاکتور را با واتساپ، تلگرام، Gmail یا هر برنامه دیگری ارسال کنید.", 11, SOFT_TEXT, false);
        note.setGravity(Gravity.CENTER); note.setPadding(dp(8), dp(10), dp(8), 0); body.addView(note);

        View.OnClickListener saveOnly = v -> {
            long id = saveInvoiceFromForm(customer, phone, desc, amount, paid, bank, date);
            if (id <= 0) return;
            dialog.dismiss();
            showDashboard();
            new AlertDialog.Builder(this).setTitle("فاکتور ثبت شد")
                    .setMessage("فاکتور با موفقیت ثبت شد.")
                    .setNegativeButton("بستن", null)
                    .setNeutralButton("ذخیره PDF", (d,w) -> exportInvoice(id))
                    .setPositiveButton("ارسال فاکتور", (d,w) -> shareInvoice(id)).show();
        };
        save.setOnClickListener(saveOnly);
        saveShare.setOnClickListener(v -> {
            long id = saveInvoiceFromForm(customer, phone, desc, amount, paid, bank, date);
            if (id <= 0) return;
            dialog.dismiss(); showDashboard(); shareInvoice(id);
        });

        sv.addView(body); shell.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        dialog.setContentView(shell);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(x -> {
            Window win = dialog.getWindow();
            if (win != null) win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        });
        dialog.show();
    }

    long saveInvoiceFromForm(EditText customer, EditText phone, EditText desc, EditText amount, EditText paid, Spinner bank, EditText date) {
        long a = num(amount.getText().toString());
        if (a <= 0) { amount.setError("مبلغ کل را وارد کنید"); amount.requestFocus(); return -1; }
        String description = desc.getText().toString().trim();
        if (description.isEmpty()) { desc.setError("شرح کالا یا خدمات را وارد کنید"); desc.requestFocus(); return -1; }
        long p = num(paid.getText().toString()); if (p > a) p = a;
        String dt = date.getText().toString().trim();
        if (!validDate(dt)) { date.setError("مثال: 1405/06/14"); date.requestFocus(); return -1; }
        long cid = 0; String cn = customer.getText().toString().trim();
        if (!cn.isEmpty()) cid = db.customer(cn, phone.getText().toString().trim(), activeShopId); else p = a;
        BankOption bo = (BankOption) bank.getSelectedItem();
        long bid = (p > 0 && bo != null) ? bo.id : 0;
        String month = dt.substring(0,7);
        String inv = db.nextInvoice(month, activeShopId);
        long id = db.addTx(dt, month, "SALE", cid, description, a, p, inv, activeShopId, bid);
        return id;
    }

    void addField(LinearLayout parent, String labelText, View field, boolean required) {
        TextView l = text(labelText, 12, SOFT_TEXT, true);
        l.setPadding(0, dp(4), 0, dp(4)); parent.addView(l);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, dp(54)); fp.bottomMargin = dp(9); parent.addView(field, fp);
    }

    Button modernPrimaryButton(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(gradient(new int[]{Color.rgb(0,185,177), Color.rgb(0,116,143)}, 18)); b.setElevation(dp(2)); return b;
    }

    Button modernOutlineButton(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(DEEP2); b.setTextSize(12); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable g = round(Color.WHITE, 18); g.setStroke(dp(1), SKY); b.setBackground(g); b.setElevation(dp(1)); return b;
    }
}
