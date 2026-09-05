package com.anarrayaneh.invoice;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;

import org.json.*;

import java.io.*;
import java.util.*;

/**
 * Hesabyar 1.5 invoice upgrade:
 * each invoice can contain multiple goods/services with quantity and unit price.
 */
public class HesabyarItemsActivity extends HesabyarFinalActivity {

    static class ItemRow {
        LinearLayout box;
        EditText name, qty, price;
        TextView subtotal;
        Button remove;
    }

    static class InvoiceItemData {
        String name;
        long qty, unitPrice, total;
        InvoiceItemData(String n, long q, long p) {
            name = n; qty = q; unitPrice = p; total = q * p;
        }
    }

    final ArrayList<ItemRow> currentRows = new ArrayList<>();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ensureInvoiceItemsTable();
    }

    void ensureInvoiceItemsTable() {
        SQLiteDatabase d = db.getWritableDatabase();
        d.execSQL("CREATE TABLE IF NOT EXISTS invoice_items(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "tx_id INTEGER NOT NULL," +
                "item_name TEXT NOT NULL," +
                "quantity INTEGER NOT NULL DEFAULT 1," +
                "unit_price INTEGER NOT NULL DEFAULT 0," +
                "line_total INTEGER NOT NULL DEFAULT 0," +
                "position INTEGER DEFAULT 0)");
        d.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_items_tx ON invoice_items(tx_id)");
    }

    @Override void saleDialog() {
        ensureInvoiceItemsTable();
        currentRows.clear();

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
        TextView close = text("×", 28, Color.WHITE, false);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        htop.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout ttl = new LinearLayout(this);
        ttl.setOrientation(LinearLayout.VERTICAL);
        ttl.addView(text("ثبت فاکتور", 26, Color.WHITE, true));
        ttl.addView(text("ثبت کالا، تعداد و قیمت هر واحد", 12, Color.rgb(214,233,243), false));
        htop.addView(ttl, new LinearLayout.LayoutParams(0, dp(58), 1));
        TextView mark = text("▥", 23, Color.rgb(36,229,205), true);
        mark.setGravity(Gravity.CENTER);
        htop.addView(mark, new LinearLayout.LayoutParams(dp(45), dp(45)));
        head.addView(htop);
        shell.addView(head, new LinearLayout.LayoutParams(-1, dp(92)));

        ScrollView sv = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(24));
        body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // Customer card
        LinearLayout customerCard = invoiceCard();
        customerCard.addView(text("مشخصات مشتری", 18, INK, true));
        TextView customerHint = text("برای فروش نقدی نام مشتری می‌تواند خالی باشد.", 11, SOFT_TEXT, false);
        customerHint.setPadding(0, dp(3), 0, dp(10));
        customerCard.addView(customerHint);
        EditText customer = input("نام مشتری");
        EditText phone = input("موبایل مشتری");
        addField(customerCard, "نام مشتری", customer, false);
        addField(customerCard, "موبایل مشتری", phone, false);
        body.addView(customerCard);

        // Items card
        LinearLayout itemsCard = invoiceCard();
        LinearLayout itemsTitle = row();
        TextView itemsHeading = text("اقلام فاکتور", 18, INK, true);
        itemsTitle.addView(itemsHeading, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button addItem = modernOutlineButton("+ افزودن کالا / خدمت");
        itemsTitle.addView(addItem, new LinearLayout.LayoutParams(dp(155), dp(44)));
        itemsCard.addView(itemsTitle);
        TextView itemHint = text("برای هر قلم، نام کالا/خدمت، تعداد و قیمت هر عدد را وارد کنید.", 11, SOFT_TEXT, false);
        itemHint.setPadding(0, dp(3), 0, dp(10));
        itemsCard.addView(itemHint);

        LinearLayout itemsHost = new LinearLayout(this);
        itemsHost.setOrientation(LinearLayout.VERTICAL);
        itemsCard.addView(itemsHost);
        LinearLayout.LayoutParams icp = new LinearLayout.LayoutParams(-1, -2);
        icp.topMargin = dp(12);
        body.addView(itemsCard, icp);

        // Payment card
        LinearLayout paymentCard = invoiceCard();
        paymentCard.addView(text("پرداخت و تاریخ", 18, INK, true));
        TextView paymentHint = text("جمع کل از روی اقلام به‌صورت خودکار محاسبه می‌شود.", 11, SOFT_TEXT, false);
        paymentHint.setPadding(0, dp(3), 0, dp(10));
        paymentCard.addView(paymentHint);
        EditText paid = moneyInput("پرداختی (تومان)");
        Spinner bank = bankSpinner(activeShopId, true);
        EditText date = input("تاریخ شمسی", Jalali.today());
        addField(paymentCard, "پرداختی", paid, false);
        paymentCard.addView(text("حساب دریافت وجه", 12, SOFT_TEXT, true));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(54));
        bp.bottomMargin = dp(9);
        paymentCard.addView(bank, bp);
        addField(paymentCard, "تاریخ شمسی *", date, false);
        LinearLayout.LayoutParams pcp = new LinearLayout.LayoutParams(-1, -2);
        pcp.topMargin = dp(12);
        body.addView(paymentCard, pcp);

        // Summary
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setPadding(dp(14), dp(12), dp(14), dp(12));
        summary.setBackground(gradient(new int[]{Color.rgb(226,252,244), Color.rgb(231,248,255)}, 18));
        summary.setElevation(dp(1));
        LinearLayout.LayoutParams sump = new LinearLayout.LayoutParams(-1, -2);
        sump.topMargin = dp(12);
        body.addView(summary, sump);
        summary.addView(text("خلاصه فاکتور", 16, INK, true));
        String previewInv = db.nextInvoice(Jalali.today().substring(0,7), activeShopId);
        TextView invLine = text("شماره فاکتور: " + previewInv, 12, SOFT_TEXT, false);
        invLine.setPadding(0, dp(6), 0, dp(3));
        summary.addView(invLine);
        TextView countLine = text("تعداد اقلام: 1", 12, SOFT_TEXT, false);
        summary.addView(countLine);
        TextView totalLine = text("جمع کل: 0 تومان", 15, INK, true);
        totalLine.setPadding(0, dp(4), 0, 0);
        summary.addView(totalLine);
        TextView remainLine = text("مانده: 0 تومان", 15, GREEN, true);
        remainLine.setPadding(0, dp(4), 0, 0);
        summary.addView(remainLine);

        Runnable recalc = () -> {
            long total = calculateInvoiceTotal();
            long p = num(paid.getText().toString());
            if (p > total) p = total;
            totalLine.setText("جمع کل: " + money(total));
            remainLine.setText("مانده: " + money(Math.max(0, total - p)));
            countLine.setText("تعداد اقلام: " + currentRows.size());
        };

        paid.addTextChangedListener(simpleWatcher(recalc));
        addItem.setOnClickListener(v -> {
            if (currentRows.size() >= 20) {
                toast("حداکثر 20 قلم در هر فاکتور");
                return;
            }
            addInvoiceItemRow(itemsHost, recalc);
            recalc.run();
        });
        addInvoiceItemRow(itemsHost, recalc);

        LinearLayout buttons = row();
        Button save = modernPrimaryButton("ثبت فاکتور");
        Button saveShare = modernOutlineButton("ثبت و ارسال PDF");
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(58), 1));
        gap(buttons);
        buttons.addView(saveShare, new LinearLayout.LayoutParams(0, dp(58), 1));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(58));
        blp.topMargin = dp(14);
        body.addView(buttons, blp);

        TextView note = text("مبلغ هر ردیف = تعداد × قیمت هر عدد. جمع فاکتور خودکار محاسبه می‌شود.", 11, SOFT_TEXT, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(8), dp(10), dp(8), 0);
        body.addView(note);

        save.setOnClickListener(v -> {
            long id = saveInvoiceWithItems(customer, phone, paid, bank, date);
            if (id <= 0) return;
            dialog.dismiss();
            showDashboard();
            new AlertDialog.Builder(this)
                    .setTitle("فاکتور ثبت شد")
                    .setMessage("اقلام فاکتور با تعداد و قیمت واحد ذخیره شدند.")
                    .setNegativeButton("بستن", null)
                    .setNeutralButton("ذخیره PDF", (d,w) -> exportInvoice(id))
                    .setPositiveButton("ارسال فاکتور", (d,w) -> shareInvoice(id))
                    .show();
        });

        saveShare.setOnClickListener(v -> {
            long id = saveInvoiceWithItems(customer, phone, paid, bank, date);
            if (id <= 0) return;
            dialog.dismiss();
            showDashboard();
            shareInvoice(id);
        });

        sv.addView(body);
        shell.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        dialog.setContentView(shell);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(x -> {
            Window win = dialog.getWindow();
            if (win != null) win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        });
        dialog.show();
    }

    LinearLayout invoiceCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(cardBg(20));
        card.setElevation(dp(2));
        return card;
    }

    TextWatcher simpleWatcher(Runnable r) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { r.run(); }
            public void afterTextChanged(Editable e) {}
        };
    }

    void addInvoiceItemRow(LinearLayout host, Runnable recalc) {
        final ItemRow row = new ItemRow();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(11), dp(10), dp(11), dp(10));
        GradientDrawable bg = round(Color.rgb(249,251,253), 16);
        bg.setStroke(dp(1), LINE);
        box.setBackground(bg);

        LinearLayout top = row();
        TextView number = text("قلم " + (currentRows.size() + 1), 12, DEEP2, true);
        top.addView(number, new LinearLayout.LayoutParams(0, dp(38), 1));
        Button remove = new Button(this);
        remove.setText("حذف");
        remove.setTextSize(10);
        remove.setTextColor(RED);
        remove.setAllCaps(false);
        GradientDrawable rg = round(Color.rgb(255,244,246), 12);
        rg.setStroke(dp(1), Color.rgb(250,210,218));
        remove.setBackground(rg);
        top.addView(remove, new LinearLayout.LayoutParams(dp(70), dp(38)));
        box.addView(top);

        EditText name = input("نام کالا یا خدمت");
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(52));
        np.bottomMargin = dp(8);
        box.addView(name, np);

        LinearLayout nums = row();
        EditText qty = input("تعداد", "1");
        qty.setInputType(2);
        EditText price = moneyInput("قیمت هر عدد");
        nums.addView(qty, new LinearLayout.LayoutParams(0, dp(52), 1));
        gap(nums);
        nums.addView(price, new LinearLayout.LayoutParams(0, dp(52), 2));
        box.addView(nums);

        TextView subtotal = text("جمع این قلم: 0 تومان", 13, GREEN, true);
        subtotal.setPadding(0, dp(8), 0, 0);
        box.addView(subtotal);

        row.box = box;
        row.name = name;
        row.qty = qty;
        row.price = price;
        row.subtotal = subtotal;
        row.remove = remove;
        currentRows.add(row);

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                long q = num(qty.getText().toString());
                long p = num(price.getText().toString());
                subtotal.setText("جمع این قلم: " + money(q * p));
                recalc.run();
            }
            public void afterTextChanged(Editable e) {}
        };
        qty.addTextChangedListener(watcher);
        price.addTextChangedListener(watcher);

        remove.setOnClickListener(v -> {
            if (currentRows.size() <= 1) {
                toast("فاکتور باید حداقل یک قلم داشته باشد");
                return;
            }
            host.removeView(box);
            currentRows.remove(row);
            renumberRows(host);
            recalc.run();
        });

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.bottomMargin = dp(9);
        host.addView(box, rp);
    }

    void renumberRows(LinearLayout host) {
        // Visual row labels are intentionally lightweight; item order is saved separately.
    }

    long calculateInvoiceTotal() {
        long total = 0;
        for (ItemRow r : currentRows) {
            long q = num(r.qty.getText().toString());
            long p = num(r.price.getText().toString());
            if (q > 0 && p > 0) total += q * p;
        }
        return total;
    }

    long saveInvoiceWithItems(EditText customer, EditText phone, EditText paid, Spinner bank, EditText date) {
        if (currentRows.isEmpty()) {
            toast("حداقل یک کالا یا خدمت اضافه کنید");
            return -1;
        }

        ArrayList<InvoiceItemData> items = new ArrayList<>();
        for (ItemRow r : currentRows) {
            String name = r.name.getText().toString().trim();
            long q = num(r.qty.getText().toString());
            long p = num(r.price.getText().toString());
            if (name.isEmpty()) {
                r.name.setError("نام کالا / خدمت را وارد کنید");
                r.name.requestFocus();
                return -1;
            }
            if (q <= 0) {
                r.qty.setError("تعداد باید حداقل 1 باشد");
                r.qty.requestFocus();
                return -1;
            }
            if (p <= 0) {
                r.price.setError("قیمت هر عدد را وارد کنید");
                r.price.requestFocus();
                return -1;
            }
            items.add(new InvoiceItemData(name, q, p));
        }

        long total = 0;
        for (InvoiceItemData i : items) total += i.total;
        long p = num(paid.getText().toString());
        if (p > total) p = total;
        String dt = date.getText().toString().trim();
        if (!validDate(dt)) {
            date.setError("مثال: 1405/06/14");
            date.requestFocus();
            return -1;
        }

        long cid = 0;
        String cn = customer.getText().toString().trim();
        if (!cn.isEmpty()) cid = db.customer(cn, phone.getText().toString().trim(), activeShopId);
        else p = total;

        BankOption bo = (BankOption) bank.getSelectedItem();
        long bid = (p > 0 && bo != null) ? bo.id : 0;
        String month = dt.substring(0, 7);
        String inv = db.nextInvoice(month, activeShopId);
        String summary = buildItemsSummary(items);
        long txId = db.addTx(dt, month, "SALE", cid, summary, total, p, inv, activeShopId, bid);
        if (txId <= 0) {
            toast("خطا در ثبت فاکتور");
            return -1;
        }

        SQLiteDatabase d = db.getWritableDatabase();
        d.beginTransaction();
        try {
            int pos = 0;
            for (InvoiceItemData i : items) {
                ContentValues v = new ContentValues();
                v.put("tx_id", txId);
                v.put("item_name", i.name);
                v.put("quantity", i.qty);
                v.put("unit_price", i.unitPrice);
                v.put("line_total", i.total);
                v.put("position", pos++);
                d.insertOrThrow("invoice_items", null, v);
            }
            d.setTransactionSuccessful();
        } catch (Exception e) {
            db.deleteTx(txId);
            toast("خطا در ذخیره اقلام فاکتور");
            return -1;
        } finally {
            d.endTransaction();
        }
        return txId;
    }

    String buildItemsSummary(ArrayList<InvoiceItemData> items) {
        StringBuilder s = new StringBuilder();
        s.append(items.size()).append(" قلم: ");
        for (int i = 0; i < items.size() && i < 3; i++) {
            if (i > 0) s.append("، ");
            s.append(items.get(i).name);
        }
        if (items.size() > 3) s.append(" و ...");
        return s.toString();
    }

    ArrayList<InvoiceItemData> loadInvoiceItems(long txId) {
        ensureInvoiceItemsTable();
        ArrayList<InvoiceItemData> out = new ArrayList<>();
        Cursor c = db.raw("SELECT item_name,quantity,unit_price FROM invoice_items WHERE tx_id=? ORDER BY position,id", new String[]{String.valueOf(txId)});
        while (c.moveToNext()) out.add(new InvoiceItemData(c.getString(0), c.getLong(1), c.getLong(2)));
        c.close();
        return out;
    }

    @Override void confirmDelete(long id) {
        new AlertDialog.Builder(this)
                .setTitle("حذف فاکتور؟")
                .setMessage("فاکتور و تمام اقلام آن حذف می‌شوند. این عملیات قابل بازگشت نیست.")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("حذف", (d,w) -> {
                    db.getWritableDatabase().delete("invoice_items", "tx_id=?", new String[]{String.valueOf(id)});
                    db.deleteTx(id);
                    showDashboard();
                }).show();
    }

    @Override Uri createInvoicePdf(long id) {
        ensureInvoiceItemsTable();
        Cursor c = db.raw("SELECT t.date,t.description,t.amount,t.paid,t.invoice_no,c.name,c.phone,s.name,s.phone,s.address,b.bank_name,b.card_no FROM tx t LEFT JOIN customers c ON c.id=t.customer_id LEFT JOIN shops s ON s.id=t.shop_id LEFT JOIN bank_accounts b ON b.id=t.bank_account_id WHERE t.id=?", new String[]{String.valueOf(id)});
        if (!c.moveToFirst()) { c.close(); return null; }
        String date = c.getString(0), fallbackDesc = c.getString(1), inv = c.getString(4), customer = c.getString(5), custPhone = c.getString(6), shopName = c.getString(7), shopPhone = c.getString(8), address = c.getString(9), bank = c.getString(10), cardNo = c.getString(11);
        long amount = c.getLong(2), paid = c.getLong(3);
        c.close();
        if (shopName == null || shopName.isEmpty()) shopName = "فروشگاه";

        ArrayList<InvoiceItemData> items = loadInvoiceItems(id);
        if (items.isEmpty()) items.add(new InvoiceItemData(fallbackDesc == null ? "کالا / خدمات" : fallbackDesc, 1, amount));

        PdfDocument pdf = new PdfDocument();
        int pageNo = 1;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNo).create());
        Canvas cv = page.getCanvas();
        Paint p = new Paint(1);
        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        p.setTextAlign(Paint.Align.RIGHT);
        int y = drawPdfHeader(cv, p, shopName, inv, date, customer, custPhone, pageNo);
        y = drawItemsHeader(cv, p, y);

        for (int i = 0; i < items.size(); i++) {
            if (y > 655) {
                pdf.finishPage(page);
                pageNo++;
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNo).create());
                cv = page.getCanvas();
                y = drawPdfContinuationHeader(cv, p, shopName, inv, pageNo);
                y = drawItemsHeader(cv, p, y);
            }
            InvoiceItemData item = items.get(i);
            if (i % 2 == 0) {
                p.setColor(Color.rgb(247,249,252));
                cv.drawRect(40, y - 22, 555, y + 17, p);
            }
            p.setColor(INK);
            p.setTextSize(12);
            p.setTypeface(Typeface.DEFAULT);
            cv.drawText(shorten(item.name, 28), 535, y, p);
            cv.drawText(String.valueOf(item.qty), 325, y, p);
            cv.drawText(formatNumber(item.unitPrice), 225, y, p);
            cv.drawText(formatNumber(item.total), 95, y, p);
            y += 40;
        }

        if (y > 625) {
            pdf.finishPage(page);
            pageNo++;
            page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNo).create());
            cv = page.getCanvas();
            y = drawPdfContinuationHeader(cv, p, shopName, inv, pageNo);
        }

        p.setColor(Color.rgb(232,249,245));
        cv.drawRoundRect(40, y, 555, y + 126, 14, 14, p);
        p.setTextSize(14);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(INK);
        cv.drawText("جمع کل: " + money(amount), 535, y + 32, p);
        cv.drawText("پرداختی: " + money(paid), 535, y + 62, p);
        p.setColor(amount - paid > 0 ? RED : GREEN);
        cv.drawText("مانده: " + money(Math.max(0, amount - paid)), 535, y + 92, p);
        p.setColor(SOFT_TEXT);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(11);
        cv.drawText("حساب دریافت: " + (bank == null ? "نقدی" : bank + maskCard(cardNo)), 535, y + 116, p);

        p.setColor(DEEP);
        cv.drawRoundRect(40, 748, 555, 810, 13, 13, p);
        p.setColor(Color.WHITE);
        p.setTextSize(11);
        p.setTypeface(Typeface.DEFAULT);
        String footer = (shopPhone == null || shopPhone.isEmpty() ? "" : "تماس: " + shopPhone + "   ") + (address == null ? "" : shorten(address, 48));
        cv.drawText(footer, 535, 777, p);
        p.setTextSize(9);
        p.setColor(Color.rgb(196,219,232));
        cv.drawText("صادر شده با حسابیار • صفحه " + pageNo, 535, 797, p);
        pdf.finishPage(page);

        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, "فاکتور_" + inv + ".pdf");
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Hesabyar/Invoices");
            Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new IOException("no uri");
            OutputStream os = getContentResolver().openOutputStream(u);
            pdf.writeTo(os);
            os.close();
            pdf.close();
            return u;
        } catch (Exception e) {
            try { pdf.close(); } catch (Exception ignored) {}
            toast("خطا در ساخت PDF فاکتور");
            return null;
        }
    }

    int drawPdfHeader(Canvas cv, Paint p, String shopName, String inv, String date, String customer, String phone, int pageNo) {
        p.setColor(DEEP);
        cv.drawRect(0, 0, 595, 128, p);
        p.setColor(AQUA);
        cv.drawRect(0, 122, 595, 128, p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setColor(Color.WHITE);
        p.setTextSize(27);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        cv.drawText(shopName, 550, 45, p);
        p.setTextSize(14);
        p.setTypeface(Typeface.DEFAULT);
        cv.drawText("فاکتور فروش • " + inv, 550, 73, p);
        cv.drawText("تاریخ: " + date + "   صفحه: " + pageNo, 550, 99, p);
        p.setColor(INK);
        p.setTextSize(14);
        cv.drawText("مشتری: " + (customer == null || customer.isEmpty() ? "مشتری نقدی" : customer), 550, 156, p);
        cv.drawText("موبایل: " + (phone == null || phone.isEmpty() ? "—" : phone), 550, 180, p);
        return 222;
    }

    int drawPdfContinuationHeader(Canvas cv, Paint p, String shopName, String inv, int pageNo) {
        p.setColor(DEEP);
        cv.drawRect(0, 0, 595, 82, p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setColor(Color.WHITE);
        p.setTextSize(21);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        cv.drawText(shopName + " • ادامه فاکتور " + inv, 550, 38, p);
        p.setTextSize(12);
        p.setTypeface(Typeface.DEFAULT);
        cv.drawText("صفحه " + pageNo, 550, 62, p);
        return 118;
    }

    int drawItemsHeader(Canvas cv, Paint p, int y) {
        p.setColor(Color.rgb(229,238,246));
        cv.drawRoundRect(40, y - 25, 555, y + 14, 8, 8, p);
        p.setColor(DEEP);
        p.setTextSize(11);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        cv.drawText("شرح کالا / خدمت", 535, y, p);
        cv.drawText("تعداد", 325, y, p);
        cv.drawText("قیمت واحد", 225, y, p);
        cv.drawText("جمع", 95, y, p);
        return y + 42;
    }

    String formatNumber(long v) {
        return String.format(Locale.US, "%,d", v);
    }

    @Override Uri createBackupFile() {
        ensureInvoiceItemsTable();
        try {
            JSONObject data = db.toJson();
            JSONArray arr = new JSONArray();
            Cursor c = db.raw("SELECT id,tx_id,item_name,quantity,unit_price,line_total,position FROM invoice_items ORDER BY id", null);
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("id", c.getLong(0));
                o.put("tx_id", c.getLong(1));
                o.put("item_name", c.getString(2));
                o.put("quantity", c.getLong(3));
                o.put("unit_price", c.getLong(4));
                o.put("line_total", c.getLong(5));
                o.put("position", c.getInt(6));
                arr.put(o);
            }
            c.close();
            data.put("invoice_items", arr);

            JSONObject settings = new JSONObject();
            settings.put("backup_email", prefs.getString("backup_email", ""));
            settings.put("active_shop_id", activeShopId);
            JSONObject all = new JSONObject();
            all.put("app", "Hesabyar");
            all.put("schema", 3);
            all.put("created", Jalali.today());
            all.put("settings", settings);
            all.put("data", data);

            String name = "Hesabyar_Backup_" + Jalali.today().replace('/', '-') + "_" + System.currentTimeMillis() + ".json";
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Hesabyar/Backup");
            Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new IOException("no uri");
            Writer w = new OutputStreamWriter(getContentResolver().openOutputStream(u), "UTF-8");
            w.write(all.toString());
            w.flush();
            w.close();
            return u;
        } catch (Exception e) {
            toast("خطا در ساخت فایل بکاپ");
            return null;
        }
    }

    @Override void restoreBackup(JSONObject root) {
        ensureInvoiceItemsTable();
        try {
            JSONObject data = root.getJSONObject("data");
            JSONArray itemArr = data.optJSONArray("invoice_items");
            db.getWritableDatabase().delete("invoice_items", null, null);
            db.restore(data);

            if (itemArr != null) {
                SQLiteDatabase d = db.getWritableDatabase();
                for (int i = 0; i < itemArr.length(); i++) {
                    JSONObject o = itemArr.getJSONObject(i);
                    ContentValues v = new ContentValues();
                    v.put("id", o.optLong("id", i + 1));
                    v.put("tx_id", o.optLong("tx_id", 0));
                    v.put("item_name", o.optString("item_name", "کالا / خدمت"));
                    v.put("quantity", o.optLong("quantity", 1));
                    v.put("unit_price", o.optLong("unit_price", 0));
                    v.put("line_total", o.optLong("line_total", 0));
                    v.put("position", o.optInt("position", i));
                    d.insert("invoice_items", null, v);
                }
            }

            JSONObject s = root.optJSONObject("settings");
            if (s != null) prefs.edit().putString("backup_email", s.optString("backup_email", prefs.getString("backup_email", ""))).apply();
            long wanted = s == null ? db.firstShopId() : s.optLong("active_shop_id", db.firstShopId());
            activeShopId = db.shopExists(wanted) ? wanted : db.firstShopId();
            prefs.edit().putLong("active_shop_id", activeShopId).apply();
            currentMonth = Jalali.today().substring(0, 7);
            toast("بازیابی با موفقیت انجام شد");
            showDashboard();
        } catch (Exception e) {
            toast("بازیابی انجام نشد");
        }
    }
}
