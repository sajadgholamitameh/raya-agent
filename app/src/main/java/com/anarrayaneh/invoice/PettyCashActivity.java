package com.anarrayaneh.invoice;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.*;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PettyCashActivity extends Activity {
    static final int BURGUNDY=Color.rgb(125,23,50), GOLD=Color.rgb(216,163,33), BG=Color.rgb(250,246,244), DARK=Color.rgb(58,42,46);
    android.content.SharedPreferences prefs;
    int year, month;
    TextView monthLabel, receiptView, expenseView, balanceView, countView;
    EditText opening, date, desc, docNo, party, amount, note;
    Spinner type, category;
    LinearLayout list;
    JSONArray entries=new JSONArray();

    final String[] TYPES={"هزینه","دریافت"};
    final String[] CATS={"خرید لوازم","رفت‌وآمد","پذیرایی","تعمیرات","قبوض","اداری","متفرقه","دریافت تنخواه"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BURGUNDY);
        getWindow().setNavigationBarColor(BURGUNDY);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        prefs=getSharedPreferences("monthly_petty_cash",MODE_PRIVATE);
        int[] j=todayJ(); year=j[0]; month=j[1];
        setContentView(build());
        loadMonth();
    }

    View build(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root=v(); root.setPadding(dp(14),dp(14),dp(14),dp(36)); scroll.addView(root);

        LinearLayout hero=v(); hero.setPadding(dp(18),dp(18),dp(18),dp(18)); hero.setBackground(round(BURGUNDY,22));
        TextView title=t("تنخواه ماهانه",28,Color.WHITE,true); title.setGravity(Gravity.CENTER); hero.addView(title);
        TextView sub=t("ثبت دریافت و هزینه • فایل مستقل برای هر ماه • خروجی Excel",13,Color.rgb(245,218,225),false); sub.setGravity(Gravity.CENTER); hero.addView(sub);
        root.addView(hero,margin(0,0,0,14));

        LinearLayout nav=h(); Button prev=btn("ماه قبل",BURGUNDY), next=btn("ماه بعد",BURGUNDY); monthLabel=t("",20,DARK,true); monthLabel.setGravity(Gravity.CENTER);
        nav.addView(prev,w(1)); nav.addView(monthLabel,w(1.25f)); nav.addView(next,w(1)); root.addView(nav);
        prev.setOnClickListener(v->shiftMonth(-1)); next.setOnClickListener(v->shiftMonth(1));

        root.addView(section("موجودی اول ماه"));
        LinearLayout openRow=h(); opening=ed("موجودی اول ماه (تومان)",InputType.TYPE_CLASS_NUMBER); Button saveOpen=btn("ثبت موجودی",GOLD); openRow.addView(opening,w(2)); openRow.addView(saveOpen,w(1)); root.addView(openRow);
        saveOpen.setOnClickListener(v->{prefs.edit().putLong(openKey(),money(opening.getText().toString())).apply(); updateSummary(); toast("موجودی اول ماه ثبت شد.");});

        LinearLayout stats=h();
        receiptView=stat("دریافتی",0); expenseView=stat("هزینه",0); balanceView=stat("مانده",0);
        stats.addView(receiptView,w(1)); stats.addView(expenseView,w(1)); stats.addView(balanceView,w(1)); root.addView(stats);
        countView=t("",12,Color.DKGRAY,false); countView.setGravity(Gravity.CENTER); root.addView(countView);

        root.addView(section("ثبت تراکنش"));
        date=ed("تاریخ شمسی",InputType.TYPE_CLASS_TEXT); root.addView(date);
        LinearLayout sp=h(); type=spinner(TYPES); category=spinner(CATS); sp.addView(type,w(1)); sp.addView(category,w(1)); root.addView(sp);
        desc=ed("شرح هزینه / دریافت",InputType.TYPE_CLASS_TEXT); root.addView(desc);
        LinearLayout two=h(); docNo=ed("شماره سند / فاکتور",InputType.TYPE_CLASS_TEXT); party=ed("طرف حساب",InputType.TYPE_CLASS_TEXT); two.addView(docNo,w(1)); two.addView(party,w(1)); root.addView(two);
        amount=ed("مبلغ (تومان)",InputType.TYPE_CLASS_NUMBER); root.addView(amount);
        note=ed("توضیحات (اختیاری)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE); note.setMinLines(2); root.addView(note);
        Button add=btn("+ ثبت تراکنش",BURGUNDY); root.addView(add,margin(0,8,0,8)); add.setOnClickListener(v->addEntry());

        Button export=btn("خروجی Excel این ماه",GOLD); root.addView(export,margin(0,2,0,12)); export.setOnClickListener(v->exportExcel());

        root.addView(section("تراکنش‌های ماه")); list=v(); root.addView(list);
        TextView foot=t("هر ماه به‌صورت مستقل ذخیره می‌شود و خروجی آن فایل جداگانه دارد.",12,Color.GRAY,false); foot.setGravity(Gravity.CENTER); root.addView(foot,margin(0,14,0,0));
        return scroll;
    }

    void shiftMonth(int d){ month+=d; if(month<1){month=12;year--;} if(month>12){month=1;year++;} loadMonth(); }

    String monthKey(){return String.format(Locale.US,"%04d-%02d",year,month);} String openKey(){return "opening_"+monthKey();} String dataKey(){return "data_"+monthKey();}

    void loadMonth(){
        monthLabel.setText(String.format(Locale.US,"%04d / %02d",year,month));
        opening.setText(fmt(prefs.getLong(openKey(),0)));
        try{entries=new JSONArray(prefs.getString(dataKey(),"[]"));}catch(Exception e){entries=new JSONArray();}
        int[] now=todayJ(); date.setText(now[0]==year&&now[1]==month?String.format(Locale.US,"%04d/%02d/%02d",now[0],now[1],now[2]):String.format(Locale.US,"%04d/%02d/01",year,month));
        renderList(); updateSummary();
    }

    void addEntry(){
        String d=date.getText().toString().trim(), ds=desc.getText().toString().trim(); long a=money(amount.getText().toString());
        if(d.isEmpty()||ds.isEmpty()||a<=0){toast("تاریخ، شرح و مبلغ را کامل وارد کنید.");return;}
        try{
            JSONObject o=new JSONObject(); o.put("date",d); o.put("type",type.getSelectedItem().toString()); o.put("category",category.getSelectedItem().toString()); o.put("desc",ds); o.put("doc",docNo.getText().toString().trim()); o.put("party",party.getText().toString().trim()); o.put("amount",a); o.put("note",note.getText().toString().trim()); o.put("ts",System.currentTimeMillis()); entries.put(o); saveEntries();
            desc.setText(""); docNo.setText(""); party.setText(""); amount.setText(""); note.setText(""); type.setSelection(0); renderList(); updateSummary(); toast("تراکنش ثبت شد.");
        }catch(Exception e){toast("خطا در ثبت تراکنش");}
    }

    void saveEntries(){prefs.edit().putString(dataKey(),entries.toString()).apply();}

    void renderList(){
        list.removeAllViews(); if(entries.length()==0){TextView e=t("هنوز تراکنشی برای این ماه ثبت نشده است.",13,Color.GRAY,false);e.setGravity(Gravity.CENTER);list.addView(e);return;}
        for(int i=entries.length()-1;i>=0;i--){ final int idx=i; try{JSONObject o=entries.getJSONObject(i); LinearLayout card=v();card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackground(round(Color.WHITE,14));
            LinearLayout top=h(); TextView left=t(o.optString("type")+"  •  "+o.optString("category"),13,o.optString("type").equals("دریافت")?Color.rgb(30,110,60):BURGUNDY,true); TextView money=t(fmt(o.optLong("amount"))+" تومان",15,DARK,true);top.addView(left,w(1));top.addView(money,w(1));card.addView(top);
            card.addView(t(o.optString("desc"),14,DARK,true)); String meta=o.optString("date")+(o.optString("party").isEmpty()?"":"  •  "+o.optString("party"))+(o.optString("doc").isEmpty()?"":"  •  سند: "+o.optString("doc"));card.addView(t(meta,11,Color.DKGRAY,false)); if(!o.optString("note").isEmpty())card.addView(t(o.optString("note"),11,Color.GRAY,false));
            Button del=btn("حذف",Color.rgb(145,63,77));card.addView(del);del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف تراکنش").setMessage("این تراکنش حذف شود؟").setNegativeButton("خیر",null).setPositiveButton("بله",(d,w)->{entries.remove(idx);saveEntries();renderList();updateSummary();}).show());
            list.addView(card,margin(0,0,0,8));
        }catch(Exception ignored){}}
    }

    long receipts(){long s=0;for(int i=0;i<entries.length();i++)try{JSONObject o=entries.getJSONObject(i);if(o.optString("type").equals("دریافت"))s+=o.optLong("amount");}catch(Exception ignored){}return s;}
    long expenses(){long s=0;for(int i=0;i<entries.length();i++)try{JSONObject o=entries.getJSONObject(i);if(o.optString("type").equals("هزینه"))s+=o.optLong("amount");}catch(Exception ignored){}return s;}
    void updateSummary(){long r=receipts(), e=expenses(), b=prefs.getLong(openKey(),0)+r-e; receiptView.setText("دریافتی\n"+fmt(r)); expenseView.setText("هزینه\n"+fmt(e)); balanceView.setText("مانده\n"+fmt(b)); countView.setText("تعداد تراکنش‌ها: "+entries.length()+"  •  ماه "+monthKey());}

    void exportExcel(){
        try{
            String name="تنخواه_"+monthKey()+".xlsx"; ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,name);cv.put(MediaStore.Downloads.MIME_TYPE,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");cv.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/تنخواه");Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);if(u==null)throw new Exception("مسیر ذخیره ایجاد نشد");OutputStream os=getContentResolver().openOutputStream(u);ZipOutputStream z=new ZipOutputStream(os);
            put(z,"[Content_Types].xml",contentTypes());put(z,"_rels/.rels",rootRels());put(z,"xl/workbook.xml",workbookXml());put(z,"xl/_rels/workbook.xml.rels",workbookRels());put(z,"xl/styles.xml",stylesXml());put(z,"xl/worksheets/sheet1.xml",sheetXml());z.finish();z.close();toast("فایل "+name+" در Downloads/تنخواه ذخیره شد.");
        }catch(Exception e){toast("خطا در خروجی Excel: "+e.getMessage());}
    }

    String contentTypes(){return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>";}
    String rootRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";}
    String workbookXml(){return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><bookViews><workbookView/></bookViews><sheets><sheet name=\"تنخواه "+monthKey()+"\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";}
    String workbookRels(){return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>";}
    String stylesXml(){return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Arial\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"12\"/><name val=\"Arial\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF7D1732\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"4\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"><alignment horizontal=\"center\"/></xf><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"><alignment horizontal=\"center\"/></xf><xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";}

    String sheetXml(){
        long op=prefs.getLong(openKey(),0), r=receipts(), e=expenses(), bal=op+r-e;StringBuilder x=new StringBuilder();x.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"/></sheetViews><cols><col min=\"1\" max=\"1\" width=\"14\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"12\" customWidth=\"1\"/><col min=\"3\" max=\"3\" width=\"18\" customWidth=\"1\"/><col min=\"4\" max=\"4\" width=\"32\" customWidth=\"1\"/><col min=\"5\" max=\"6\" width=\"18\" customWidth=\"1\"/><col min=\"7\" max=\"7\" width=\"18\" customWidth=\"1\"/><col min=\"8\" max=\"8\" width=\"30\" customWidth=\"1\"/></cols><sheetData>");
        row(x,1,new String[]{sc("A1","تنخواه ماهانه - "+monthKey(),1)}); row(x,2,new String[]{sc("A2","واحد پول",0),sc("B2","تومان",0)}); row(x,4,new String[]{sc("A4","موجودی اول ماه",2),nc("B4",op,3),sc("C4","جمع دریافتی",2),nc("D4",r,3),sc("E4","جمع هزینه",2),nc("F4",e,3),sc("G4","مانده",2),nc("H4",bal,3)});
        String[] hs={"تاریخ","نوع","دسته‌بندی","شرح","شماره سند/فاکتور","طرف حساب","مبلغ (تومان)","توضیحات"};String[] hc=new String[8];for(int i=0;i<8;i++)hc[i]=sc(col(i+1)+"7",hs[i],2);row(x,7,hc);
        int rr=8;for(int i=0;i<entries.length();i++)try{JSONObject o=entries.getJSONObject(i);row(x,rr,new String[]{sc("A"+rr,o.optString("date"),0),sc("B"+rr,o.optString("type"),0),sc("C"+rr,o.optString("category"),0),sc("D"+rr,o.optString("desc"),0),sc("E"+rr,o.optString("doc"),0),sc("F"+rr,o.optString("party"),0),nc("G"+rr,o.optLong("amount"),3),sc("H"+rr,o.optString("note"),0)});rr++;}catch(Exception ignored){}
        x.append("</sheetData><mergeCells count=\"1\"><mergeCell ref=\"A1:H1\"/></mergeCells></worksheet>");return x.toString();
    }
    void row(StringBuilder x,int n,String[] cells){x.append("<row r=\"").append(n).append("\">");for(String c:cells)x.append(c);x.append("</row>");}
    String sc(String ref,String value,int style){return "<c r=\""+ref+"\" t=\"inlineStr\" s=\""+style+"\"><is><t>"+xml(value)+"</t></is></c>";}
    String nc(String ref,long value,int style){return "<c r=\""+ref+"\" s=\""+style+"\"><v>"+value+"</v></c>";}
    String col(int n){String s="";while(n>0){n--;s=(char)('A'+n%26)+s;n/=26;}return s;}
    String xml(String s){if(s==null)return "";return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    void put(ZipOutputStream z,String name,String text)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(text.getBytes(StandardCharsets.UTF_8));z.closeEntry();}

    Spinner spinner(String[] vals){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,vals);s.setAdapter(a);s.setBackground(round(Color.WHITE,10));s.setPadding(dp(8),0,dp(8),0);return s;}
    EditText ed(String hint,int input){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(14);e.setTextColor(DARK);e.setHintTextColor(Color.GRAY);e.setGravity(Gravity.RIGHT);e.setInputType(input);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(round(Color.WHITE,11));e.setLayoutParams(margin(0,4,0,4));return e;}
    TextView section(String s){TextView t=t(s,17,BURGUNDY,true);t.setPadding(0,dp(14),0,dp(5));return t;}
    TextView stat(String label,long val){TextView t=t(label+"\n"+fmt(val),13,DARK,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(5),dp(12),dp(5),dp(12));t.setBackground(round(Color.WHITE,14));return t;}
    Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setAllCaps(false);b.setBackground(round(c,11));return b;}
    TextView t(String s,int size,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(c);t.setGravity(Gravity.RIGHT);t.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));t.setPadding(dp(6),dp(6),dp(6),dp(6));return t;}
    LinearLayout v(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;} LinearLayout h(){LinearLayout l=v();l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    LinearLayout.LayoutParams w(float f){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,f);p.setMargins(dp(3),dp(3),dp(3),dp(3));return p;} LinearLayout.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;} int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);} void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    long money(String s){try{return Long.parseLong(norm(s).replace(",","").replace("٬","").trim());}catch(Exception e){return 0;}} String norm(String s){String f="۰۱۲۳۴۵۶۷۸۹",a="٠١٢٣٤٥٦٧٨٩";for(int i=0;i<10;i++){s=s.replace(f.charAt(i),(char)('0'+i));s=s.replace(a.charAt(i),(char)('0'+i));}return s;} String fmt(long n){return NumberFormat.getNumberInstance(Locale.US).format(n);}
    int[] todayJ(){Calendar c=Calendar.getInstance();return g2j(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));}
    int[] g2j(int gy,int gm,int gd){int[]d={0,31,59,90,120,151,181,212,243,273,304,334};int gy2=gm>2?gy+1:gy;long days=355666L+365L*gy+(gy2+3)/4-(gy2+99)/100+(gy2+399)/400+gd+d[gm-1];int jy=-1595+33*(int)(days/12053);days%=12053;jy+=4*(int)(days/1461);days%=1461;if(days>365){jy+=(int)((days-1)/365);days=(days-1)%365;}int jm,jd;if(days<186){jm=1+(int)(days/31);jd=1+(int)(days%31);}else{jm=7+(int)((days-186)/30);jd=1+(int)((days-186)%30);}return new int[]{jy,jm,jd};}
}
