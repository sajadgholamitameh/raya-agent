package com.anarrayaneh.invoice;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class HesabyarActivity extends Activity {
    static final int NAVY = Color.rgb(21,34,56), TEAL = Color.rgb(0,168,150), GOLD = Color.rgb(244,185,66), BG = Color.rgb(245,247,250), TEXT = Color.rgb(33,40,52), MUTED = Color.rgb(105,114,130), RED = Color.rgb(210,74,74);
    LinearLayout root, content;
    DB db;
    String currentMonth;
    SharedPreferences prefs;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY); getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        db = new DB(this); prefs = getSharedPreferences("hesabyar", MODE_PRIVATE);
        currentMonth = Jalali.today().substring(0,7);
        showDashboard();
    }

    void base(String title){
        ScrollView sv = new ScrollView(this); sv.setFillViewport(true); sv.setBackgroundColor(BG);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(10),dp(16),dp(28)); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); sv.addView(root);
        content = root;
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setOrientation(LinearLayout.HORIZONTAL);
        TextView t = text(title,24,Color.WHITE,true); t.setPadding(dp(14),0,0,0); top.addView(t,new LinearLayout.LayoutParams(0,dp(58),1));
        TextView brand = text("حسابیار",18,GOLD,true); brand.setGravity(Gravity.CENTER); top.addView(brand,new LinearLayout.LayoutParams(dp(105),dp(58)));
        top.setBackground(round(NAVY,18)); root.addView(top,new LinearLayout.LayoutParams(-1,dp(58)));
        space(12);
        setContentView(sv);
    }

    void showDashboard(){
        base("مدیریت مالی کسب‌وکار");
        String business = prefs.getString("business","کسب‌وکار من");
        TextView biz = text(business,20,TEXT,true); biz.setGravity(Gravity.RIGHT); content.addView(biz);
        TextView sub = text("فروش، هزینه، مشتری و مطالبات — کاملاً آفلاین",13,MUTED,false); content.addView(sub); space(12);
        long sales = db.sum("SALE", currentMonth), expenses = db.sum("EXPENSE", currentMonth), receivable = db.totalReceivable();
        LinearLayout r1 = row(); r1.addView(stat("فروش این ماه",money(sales),TEAL),new LinearLayout.LayoutParams(0,dp(105),1)); gap(r1); r1.addView(stat("هزینه این ماه",money(expenses),RED),new LinearLayout.LayoutParams(0,dp(105),1)); content.addView(r1); space(8);
        LinearLayout r2 = row(); r2.addView(stat("سود تقریبی",money(sales-expenses),GOLD),new LinearLayout.LayoutParams(0,dp(105),1)); gap(r2); r2.addView(stat("مطالبات",money(receivable),NAVY),new LinearLayout.LayoutParams(0,dp(105),1)); content.addView(r2); space(16);

        content.addView(section("عملیات سریع"));
        LinearLayout a = row(); a.addView(action("فاکتور / فروش جدید", TEAL, v->saleDialog()),new LinearLayout.LayoutParams(0,dp(70),1)); gap(a); a.addView(action("ثبت هزینه", RED, v->expenseDialog()),new LinearLayout.LayoutParams(0,dp(70),1)); content.addView(a); space(8);
        LinearLayout b = row(); b.addView(action("دریافت از مشتری", NAVY, v->receiptDialog()),new LinearLayout.LayoutParams(0,dp(70),1)); gap(b); b.addView(action("مشتریان و بدهکاران", GOLD, v->showCustomers()),new LinearLayout.LayoutParams(0,dp(70),1)); content.addView(b); space(8);
        LinearLayout c = row(); c.addView(action("گزارش ماهانه", Color.rgb(72,89,115), v->showReport(currentMonth)),new LinearLayout.LayoutParams(0,dp(64),1)); gap(c); c.addView(action("تنظیمات کسب‌وکار", Color.rgb(82,82,82), v->settingsDialog()),new LinearLayout.LayoutParams(0,dp(64),1)); content.addView(c); space(18);

        content.addView(section("آخرین تراکنش‌ها"));
        Cursor cur = db.raw("SELECT t.id,t.date,t.type,t.description,t.amount,t.invoice_no,c.name FROM tx t LEFT JOIN customers c ON c.id=t.customer_id ORDER BY t.id DESC LIMIT 8",null);
        if(!cur.moveToFirst()) content.addView(empty("هنوز تراکنشی ثبت نشده است."));
        else do {
            long id=cur.getLong(0), amount=cur.getLong(4); String date=cur.getString(1), type=cur.getString(2), desc=cur.getString(3), inv=cur.getString(5), cust=cur.getString(6);
            String ttl = type.equals("SALE") ? "فروش"+(inv==null?"":" • "+inv) : type.equals("EXPENSE")?"هزینه":"دریافت";
            String d = (cust==null||cust.isEmpty()?"":cust+" • ") + (desc==null?"":desc) + "\n" + date;
            LinearLayout card = transactionCard(ttl,d,money(amount), type.equals("EXPENSE")?RED:TEAL);
            if(type.equals("SALE")) card.setOnClickListener(v->invoiceOptions(id));
            content.addView(card); space(7);
        } while(cur.moveToNext()); cur.close();
        TextView foot=text("نسخه MVP • مناسب تست بازار و ارائه به مشتری اولیه",12,MUTED,false); foot.setGravity(Gravity.CENTER); foot.setPadding(0,dp(20),0,0); content.addView(foot);
    }

    void saleDialog(){
        LinearLayout f=form(); EditText customer=input("نام مشتری (اختیاری)"); EditText phone=input("موبایل مشتری (اختیاری)"); EditText desc=input("شرح کالا / خدمات"); EditText amount=moneyInput("مبلغ کل (تومان)"); EditText paid=moneyInput("پرداختی الان (تومان)"); EditText date=input("تاریخ شمسی",Jalali.today());
        f.addView(customer); f.addView(phone); f.addView(desc); f.addView(amount); f.addView(paid); f.addView(date);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("فاکتور / فروش جدید").setView(wrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ثبت",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            long a=num(amount.getText().toString()); if(a<=0){amount.setError("مبلغ را وارد کنید");return;}
            long p=num(paid.getText().toString()); if(p>a)p=a; String dt=date.getText().toString().trim(); if(!validDate(dt)){date.setError("مثال: 1405/06/14");return;}
            long cid=0; String cn=customer.getText().toString().trim(); if(!cn.isEmpty()) cid=db.customer(cn,phone.getText().toString().trim()); else p=a;
            String month=dt.substring(0,7), inv=db.nextInvoice(month); long id=db.addTx(dt,month,"SALE",cid,desc.getText().toString().trim(),a,p,inv);
            d.dismiss(); showDashboard(); new AlertDialog.Builder(this).setTitle("فروش ثبت شد").setMessage("شماره فاکتور: "+inv+"\nمانده: "+money(a-p)).setNegativeButton("بستن",null).setPositiveButton("ذخیره PDF فاکتور",(q,w)->exportInvoice(id)).show();
        })); d.show();
    }

    void expenseDialog(){
        LinearLayout f=form(); EditText desc=input("شرح هزینه"); Spinner cat=spinner(new String[]{"خرید کالا","حمل‌ونقل","اجاره","قبوض","حقوق","تبلیغات","لوازم مصرفی","سایر"}); EditText amount=moneyInput("مبلغ (تومان)"); EditText date=input("تاریخ شمسی",Jalali.today()); f.addView(desc); f.addView(cat); f.addView(amount); f.addView(date);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("ثبت هزینه").setView(wrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ثبت",null).create(); d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{long a=num(amount.getText().toString()); if(a<=0){amount.setError("مبلغ را وارد کنید");return;} String dt=date.getText().toString().trim(); if(!validDate(dt)){date.setError("تاریخ نامعتبر");return;} String note=cat.getSelectedItem()+" • "+desc.getText().toString().trim(); db.addTx(dt,dt.substring(0,7),"EXPENSE",0,note,a,0,null); d.dismiss(); showDashboard(); toast("هزینه ثبت شد");})); d.show();
    }

    void receiptDialog(){
        ArrayList<String> names=db.customerNames(); if(names.isEmpty()){new AlertDialog.Builder(this).setMessage("ابتدا یک فروش برای مشتری ثبت کنید.").setPositiveButton("باشه",null).show();return;}
        LinearLayout f=form(); Spinner customer=spinner(names.toArray(new String[0])); EditText amount=moneyInput("مبلغ دریافتی (تومان)"); EditText desc=input("توضیح (مثلاً کارت به کارت)"); EditText date=input("تاریخ شمسی",Jalali.today()); f.addView(customer); f.addView(amount); f.addView(desc); f.addView(date);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("دریافت از مشتری").setView(wrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ثبت",null).create(); d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{long a=num(amount.getText().toString()); if(a<=0){amount.setError("مبلغ را وارد کنید");return;} String dt=date.getText().toString().trim(); if(!validDate(dt)){date.setError("تاریخ نامعتبر");return;} long cid=db.customerId((String)customer.getSelectedItem()); db.addTx(dt,dt.substring(0,7),"RECEIPT",cid,desc.getText().toString().trim(),a,0,null); d.dismiss(); showDashboard(); toast("دریافت ثبت شد");})); d.show();
    }

    void showCustomers(){
        base("مشتریان و بدهکاران"); Button back=smallButton("بازگشت",NAVY); back.setOnClickListener(v->showDashboard()); content.addView(back); space(10);
        Cursor c=db.raw("SELECT c.id,c.name,c.phone, COALESCE((SELECT SUM(amount-paid) FROM tx WHERE type='SALE' AND customer_id=c.id),0)-COALESCE((SELECT SUM(amount) FROM tx WHERE type='RECEIPT' AND customer_id=c.id),0) bal FROM customers c ORDER BY bal DESC,c.name",null);
        if(!c.moveToFirst()) content.addView(empty("مشتری ثبت نشده است.")); else do { long id=c.getLong(0), bal=c.getLong(3); String name=c.getString(1),phone=c.getString(2); LinearLayout card=transactionCard(name,phone==null?"":phone,money(bal),bal>0?RED:TEAL); card.setOnClickListener(v->customerHistory(id,name)); content.addView(card); space(7);}while(c.moveToNext()); c.close();
    }

    void customerHistory(long cid,String name){
        base(name); Button back=smallButton("بازگشت به مشتریان",NAVY); back.setOnClickListener(v->showCustomers()); content.addView(back); space(10); long bal=db.customerBalance(cid); TextView b=text("مانده حساب: "+money(bal),20,bal>0?RED:TEAL,true); b.setPadding(dp(12),dp(16),dp(12),dp(16)); b.setBackground(round(Color.WHITE,14)); content.addView(b); space(10);
        Cursor c=db.raw("SELECT id,date,type,description,amount,paid,invoice_no FROM tx WHERE customer_id=? ORDER BY id DESC",new String[]{String.valueOf(cid)}); if(!c.moveToFirst())content.addView(empty("تراکنشی وجود ندارد.")); else do {long id=c.getLong(0),amount=c.getLong(4),paid=c.getLong(5);String date=c.getString(1),type=c.getString(2),desc=c.getString(3),inv=c.getString(6);String ttl=type.equals("SALE")?"فروش "+inv:"دریافت";String detail=(desc==null?"":desc)+"\n"+date+(type.equals("SALE")?" • پرداختی: "+money(paid):"");LinearLayout card=transactionCard(ttl,detail,money(amount),type.equals("SALE")?NAVY:TEAL);if(type.equals("SALE"))card.setOnClickListener(v->invoiceOptions(id));content.addView(card);space(7);}while(c.moveToNext());c.close();
    }

    void invoiceOptions(long id){ new AlertDialog.Builder(this).setTitle("فاکتور").setItems(new String[]{"ذخیره PDF فاکتور","حذف این فروش"},(d,w)->{if(w==0)exportInvoice(id);else confirmDelete(id);}).show(); }
    void confirmDelete(long id){new AlertDialog.Builder(this).setTitle("حذف فروش؟").setMessage("این عملیات قابل بازگشت نیست.").setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->{db.deleteTx(id);showDashboard();}).show();}

    void showReport(String month){
        currentMonth=month; base("گزارش ماه "+month); LinearLayout nav=row(); Button prev=smallButton("ماه قبل",NAVY), home=smallButton("خانه",Color.DKGRAY), next=smallButton("ماه بعد",NAVY); prev.setOnClickListener(v->showReport(Jalali.shiftMonth(month,-1))); next.setOnClickListener(v->showReport(Jalali.shiftMonth(month,1))); home.setOnClickListener(v->showDashboard()); nav.addView(prev,new LinearLayout.LayoutParams(0,dp(48),1));gap(nav);nav.addView(home,new LinearLayout.LayoutParams(0,dp(48),1));gap(nav);nav.addView(next,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(nav);space(12);
        long sales=db.sum("SALE",month), exp=db.sum("EXPENSE",month), rec=db.sum("RECEIPT",month); LinearLayout rr=row();rr.addView(stat("فروش",money(sales),TEAL),new LinearLayout.LayoutParams(0,dp(100),1));gap(rr);rr.addView(stat("هزینه",money(exp),RED),new LinearLayout.LayoutParams(0,dp(100),1));content.addView(rr);space(8);LinearLayout rr2=row();rr2.addView(stat("دریافت مشتریان",money(rec),NAVY),new LinearLayout.LayoutParams(0,dp(100),1));gap(rr2);rr2.addView(stat("فروش - هزینه",money(sales-exp),GOLD),new LinearLayout.LayoutParams(0,dp(100),1));content.addView(rr2);space(14);
        LinearLayout ex=row();Button csv=smallButton("خروجی Excel/CSV",TEAL),pdf=smallButton("گزارش PDF",NAVY);csv.setOnClickListener(v->exportCsv(month));pdf.setOnClickListener(v->exportReportPdf(month));ex.addView(csv,new LinearLayout.LayoutParams(0,dp(54),1));gap(ex);ex.addView(pdf,new LinearLayout.LayoutParams(0,dp(54),1));content.addView(ex);space(14);content.addView(section("تراکنش‌های ماه"));
        Cursor c=db.raw("SELECT t.id,t.date,t.type,t.description,t.amount,t.paid,t.invoice_no,c.name FROM tx t LEFT JOIN customers c ON c.id=t.customer_id WHERE t.month=? ORDER BY t.id DESC",new String[]{month});if(!c.moveToFirst())content.addView(empty("برای این ماه تراکنشی وجود ندارد."));else do{long id=c.getLong(0),a=c.getLong(4);String type=c.getString(2),desc=c.getString(3),date=c.getString(1),inv=c.getString(6),cust=c.getString(7);String ttl=type.equals("SALE")?"فروش "+inv:type.equals("EXPENSE")?"هزینه":"دریافت";String detail=(cust==null?"":cust+" • ")+(desc==null?"":desc)+"\n"+date;LinearLayout card=transactionCard(ttl,detail,money(a),type.equals("EXPENSE")?RED:TEAL);if(type.equals("SALE"))card.setOnClickListener(v->invoiceOptions(id));content.addView(card);space(7);}while(c.moveToNext());c.close();
    }

    void settingsDialog(){
        LinearLayout f=form();EditText biz=input("نام کسب‌وکار",prefs.getString("business","کسب‌وکار من"));EditText phone=input("شماره تماس",prefs.getString("phone",""));EditText address=input("آدرس / توضیح پایین فاکتور",prefs.getString("address",""));f.addView(biz);f.addView(phone);f.addView(address);new AlertDialog.Builder(this).setTitle("تنظیمات کسب‌وکار").setView(wrap(f)).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{prefs.edit().putString("business",biz.getText().toString().trim()).putString("phone",phone.getText().toString().trim()).putString("address",address.getText().toString().trim()).apply();showDashboard();}).show();
    }

    void exportInvoice(long id){
        Cursor c=db.raw("SELECT t.date,t.description,t.amount,t.paid,t.invoice_no,c.name,c.phone FROM tx t LEFT JOIN customers c ON c.id=t.customer_id WHERE t.id=?",new String[]{String.valueOf(id)});if(!c.moveToFirst()){c.close();return;}String date=c.getString(0),desc=c.getString(1),inv=c.getString(4),customer=c.getString(5),phone=c.getString(6);long amount=c.getLong(2),paid=c.getLong(3);c.close();
        PdfDocument pdf=new PdfDocument();PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());Canvas cv=page.getCanvas();Paint p=new Paint(1);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setColor(NAVY);cv.drawRect(0,0,595,105,p);p.setTextAlign(Paint.Align.RIGHT);p.setColor(Color.WHITE);p.setTextSize(28);p.setTypeface(Typeface.DEFAULT_BOLD);cv.drawText(prefs.getString("business","کسب‌وکار من"),555,48,p);p.setTextSize(15);p.setTypeface(Typeface.DEFAULT);cv.drawText("فاکتور فروش • "+inv,555,78,p);
        p.setColor(TEXT);p.setTextSize(16);int y=145;cv.drawText("تاریخ: "+date,555,y,p);y+=32;cv.drawText("مشتری: "+(customer==null?"مشتری نقدی":customer),555,y,p);if(phone!=null&&!phone.isEmpty()){y+=28;cv.drawText("تلفن: "+phone,555,y,p);}y+=45;p.setColor(Color.rgb(230,234,240));cv.drawRoundRect(40,y,555,y+110,12,12,p);p.setColor(TEXT);p.setTextSize(17);cv.drawText("شرح: "+(desc==null?"":desc),535,y+35,p);cv.drawText("مبلغ کل: "+money(amount),535,y+70,p);cv.drawText("پرداختی: "+money(paid),535,y+98,p);y+=150;p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(21);p.setColor(amount-paid>0?RED:TEAL);cv.drawText("مانده: "+money(amount-paid),535,y,p);p.setTypeface(Typeface.DEFAULT);p.setColor(MUTED);p.setTextSize(13);String contact=prefs.getString("phone","");String address=prefs.getString("address","");if(!contact.isEmpty())cv.drawText("تماس: "+contact,535,760,p);if(!address.isEmpty())cv.drawText(address,535,785,p);pdf.finishPage(page);savePdf(pdf,"فاکتور_"+inv+".pdf");
    }

    void exportReportPdf(String month){
        long sales=db.sum("SALE",month),exp=db.sum("EXPENSE",month),rec=db.sum("RECEIPT",month);PdfDocument pdf=new PdfDocument();PdfDocument.Page pg=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());Canvas cv=pg.getCanvas();Paint p=new Paint(1);p.setTextAlign(Paint.Align.RIGHT);p.setColor(NAVY);cv.drawRect(0,0,595,90,p);p.setColor(Color.WHITE);p.setTextSize(26);p.setTypeface(Typeface.DEFAULT_BOLD);cv.drawText("گزارش مالی ماه "+month,555,48,p);p.setTextSize(15);cv.drawText(prefs.getString("business","کسب‌وکار من"),555,73,p);p.setColor(TEXT);p.setTextSize(18);int y=135;cv.drawText("فروش: "+money(sales),555,y,p);y+=35;cv.drawText("هزینه: "+money(exp),555,y,p);y+=35;cv.drawText("دریافت از مشتریان: "+money(rec),555,y,p);y+=35;p.setColor(TEAL);cv.drawText("فروش منهای هزینه: "+money(sales-exp),555,y,p);y+=55;p.setColor(TEXT);p.setTextSize(14);Cursor c=db.raw("SELECT date,type,description,amount,invoice_no FROM tx WHERE month=? ORDER BY id DESC",new String[]{month});while(c.moveToNext()&&y<790){String type=c.getString(1),desc=c.getString(2),inv=c.getString(4);String line=c.getString(0)+"  |  "+(type.equals("SALE")?"فروش "+(inv==null?"":inv):type.equals("EXPENSE")?"هزینه":"دریافت")+"  |  "+shorten(desc,24)+"  |  "+money(c.getLong(3));cv.drawText(line,555,y,p);y+=25;}c.close();pdf.finishPage(pg);savePdf(pdf,"گزارش_"+month.replace('/','-')+".pdf");
    }

    void exportCsv(String month){
        try{ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,"حسابیار_"+month.replace('/','-')+".csv");v.put(MediaStore.MediaColumns.MIME_TYPE,"text/csv");v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/Hesabyar");Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);OutputStream os=getContentResolver().openOutputStream(u);os.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});Writer w=new OutputStreamWriter(os,"UTF-8");w.write("تاریخ,نوع,شماره فاکتور,مشتری,شرح,مبلغ,پرداختی\n");Cursor c=db.raw("SELECT t.date,t.type,t.invoice_no,c.name,t.description,t.amount,t.paid FROM tx t LEFT JOIN customers c ON c.id=t.customer_id WHERE t.month=? ORDER BY t.id",new String[]{month});while(c.moveToNext()){w.write(csv(c.getString(0))+","+csv(typeFa(c.getString(1)))+","+csv(c.getString(2))+","+csv(c.getString(3))+","+csv(c.getString(4))+","+c.getLong(5)+","+c.getLong(6)+"\n");}c.close();w.flush();w.close();toast("فایل CSV در Downloads/Hesabyar ذخیره شد");}catch(Exception e){toast("خطا در ذخیره فایل");}
    }

    void savePdf(PdfDocument pdf,String name){try{ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,name);v.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/Hesabyar");Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);OutputStream os=getContentResolver().openOutputStream(u);pdf.writeTo(os);os.close();pdf.close();toast("PDF در Downloads/Hesabyar ذخیره شد");}catch(Exception e){pdf.close();toast("خطا در ساخت PDF");}}

    LinearLayout form(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(8),dp(8),dp(8),dp(8));l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    ScrollView wrap(View v){ScrollView s=new ScrollView(this);s.addView(v);return s;}
    EditText input(String hint){return input(hint,"");} EditText input(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setTextSize(16);e.setSingleLine(false);e.setPadding(dp(10),dp(12),dp(10),dp(12));return e;} EditText moneyInput(String hint){EditText e=input(hint);e.setInputType(2);return e;}
    Spinner spinner(String[] a){Spinner s=new Spinner(this);ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,a);ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(ad);s.setPadding(dp(4),dp(8),dp(4),dp(8));return s;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER);return l;} void gap(LinearLayout l){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(dp(8),1));} void space(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    TextView text(String s,int z,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(color);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);t.setTextDirection(View.TEXT_DIRECTION_RTL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    TextView section(String s){TextView t=text(s,17,TEXT,true);t.setPadding(0,0,dp(3),dp(8));return t;}
    TextView empty(String s){TextView t=text(s,14,MUTED,false);t.setGravity(Gravity.CENTER);t.setPadding(dp(12),dp(26),dp(12),dp(26));t.setBackground(round(Color.WHITE,14));return t;}
    View stat(String label,String value,int accent){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(12),dp(10),dp(12),dp(10));l.setGravity(Gravity.CENTER_VERTICAL);l.setBackground(round(Color.WHITE,15));TextView a=text(label,13,MUTED,false);TextView b=text(value,18,accent,true);b.setPadding(0,dp(7),0,0);l.addView(a);l.addView(b);return l;}
    Button action(String s,int color,View.OnClickListener click){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setTextColor(Color.WHITE);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setBackground(round(color,14));b.setOnClickListener(click);return b;}
    Button smallButton(String s,int color){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setAllCaps(false);b.setBackground(round(color,12));return b;}
    LinearLayout transactionCard(String title,String detail,String amount,int accent){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.HORIZONTAL);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackground(round(Color.WHITE,13));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);TextView a=text(title,15,TEXT,true),d=text(detail,12,MUTED,false);d.setPadding(0,dp(3),0,0);tx.addView(a);tx.addView(d);box.addView(tx,new LinearLayout.LayoutParams(0,-2,1));TextView am=text(amount,15,accent,true);am.setGravity(Gravity.CENTER);box.addView(am,new LinearLayout.LayoutParams(dp(125),-1));return box;}
    GradientDrawable round(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);} String money(long v){return String.format(Locale.US,"%,d تومان",v);} void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    long num(String s){try{return Long.parseLong(normalize(s).replaceAll("[^0-9]",""));}catch(Exception e){return 0;}} String normalize(String s){String p="۰۱۲۳۴۵۶۷۸۹",a="٠١٢٣٤٥٦٧٨٩";for(int i=0;i<10;i++)s=s.replace(p.charAt(i),(char)('0'+i)).replace(a.charAt(i),(char)('0'+i));return s;} boolean validDate(String s){s=normalize(s);return s.matches("1[34][0-9]{2}/(0[1-9]|1[0-2])/(0[1-9]|[12][0-9]|3[01])");}
    String shorten(String s,int n){if(s==null)return "";return s.length()>n?s.substring(0,n)+"…":s;} String csv(String s){if(s==null)return "";return "\""+s.replace("\"","\"\"")+"\"";} String typeFa(String t){return t.equals("SALE")?"فروش":t.equals("EXPENSE")?"هزینه":"دریافت";}

    static class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"hesabyar.db",null,1);} public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE,phone TEXT)");d.execSQL("CREATE TABLE tx(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,month TEXT,type TEXT,customer_id INTEGER DEFAULT 0,description TEXT,amount INTEGER DEFAULT 0,paid INTEGER DEFAULT 0,invoice_no TEXT)");}public void onUpgrade(SQLiteDatabase d,int o,int n){}
        Cursor raw(String q,String[] a){return getReadableDatabase().rawQuery(q,a);} long customer(String name,String phone){SQLiteDatabase d=getWritableDatabase();Cursor c=d.rawQuery("SELECT id FROM customers WHERE name=?",new String[]{name});if(c.moveToFirst()){long id=c.getLong(0);c.close();if(phone!=null&&!phone.isEmpty()){ContentValues v=new ContentValues();v.put("phone",phone);d.update("customers",v,"id=?",new String[]{String.valueOf(id)});}return id;}c.close();ContentValues v=new ContentValues();v.put("name",name);v.put("phone",phone);return d.insert("customers",null,v);}long customerId(String name){Cursor c=raw("SELECT id FROM customers WHERE name=?",new String[]{name});long id=c.moveToFirst()?c.getLong(0):0;c.close();return id;}
        ArrayList<String> customerNames(){ArrayList<String>a=new ArrayList<>();Cursor c=raw("SELECT name FROM customers ORDER BY name",null);while(c.moveToNext())a.add(c.getString(0));c.close();return a;}
        long addTx(String date,String month,String type,long cid,String desc,long amount,long paid,String inv){ContentValues v=new ContentValues();v.put("date",date);v.put("month",month);v.put("type",type);v.put("customer_id",cid);v.put("description",desc);v.put("amount",amount);v.put("paid",paid);v.put("invoice_no",inv);return getWritableDatabase().insert("tx",null,v);}void deleteTx(long id){getWritableDatabase().delete("tx","id=?",new String[]{String.valueOf(id)});}
        long sum(String type,String month){Cursor c=raw("SELECT COALESCE(SUM(amount),0) FROM tx WHERE type=? AND month=?",new String[]{type,month});long x=c.moveToFirst()?c.getLong(0):0;c.close();return x;}long totalReceivable(){Cursor c=raw("SELECT COALESCE((SELECT SUM(amount-paid) FROM tx WHERE type='SALE' AND customer_id>0),0)-COALESCE((SELECT SUM(amount) FROM tx WHERE type='RECEIPT'),0)",null);long x=c.moveToFirst()?c.getLong(0):0;c.close();return x;}long customerBalance(long id){Cursor c=raw("SELECT COALESCE((SELECT SUM(amount-paid) FROM tx WHERE type='SALE' AND customer_id=?),0)-COALESCE((SELECT SUM(amount) FROM tx WHERE type='RECEIPT' AND customer_id=?),0)",new String[]{String.valueOf(id),String.valueOf(id)});long x=c.moveToFirst()?c.getLong(0):0;c.close();return x;}
        String nextInvoice(String month){Cursor c=raw("SELECT COUNT(*) FROM tx WHERE type='SALE' AND month=?",new String[]{month});int n=c.moveToFirst()?c.getInt(0)+1:1;c.close();return "F-"+month.replace("/","")+"-"+String.format(Locale.US,"%04d",n);}
    }

    static class Jalali{
        static String today(){LocalDate g=LocalDate.now();int[] j=toJ(g.getYear(),g.getMonthValue(),g.getDayOfMonth());return String.format(Locale.US,"%04d/%02d/%02d",j[0],j[1],j[2]);}
        static String shiftMonth(String m,int delta){String[]p=m.split("/");int y=Integer.parseInt(p[0]),mo=Integer.parseInt(p[1])+delta;while(mo<1){mo+=12;y--;}while(mo>12){mo-=12;y++;}return String.format(Locale.US,"%04d/%02d",y,mo);}
        static int[] toJ(int gy,int gm,int gd){int[]gdm={0,31,59,90,120,151,181,212,243,273,304,334};int jy=(gy<=1600)?0:979;gy-=(gy<=1600)?621:1600;int gy2=gm>2?gy+1:gy;long days=365L*gy+(gy2+3)/4-(gy2+99)/100+(gy2+399)/400-80+gd+gdm[gm-1];jy+=33*(days/12053);days%=12053;jy+=4*(days/1461);days%=1461;if(days>365){jy+=(days-1)/365;days=(days-1)%365;}int jm,jd;if(days<186){jm=1+(int)(days/31);jd=1+(int)(days%31);}else{jm=7+(int)((days-186)/30);jd=1+(int)((days-186)%30);}return new int[]{jy,jm,jd};}
    }
}
