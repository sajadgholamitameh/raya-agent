package com.anarrayaneh.invoice;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DashboardActivity extends Activity {
    private static final int CHERRY = Color.rgb(128,14,39);
    private static final int DARK = Color.rgb(84,8,25);
    private static final int GOLD = Color.rgb(210,157,45);
    private static final int BG = Color.rgb(250,246,244);
    private static final int TEXT = Color.rgb(55,55,55);
    private Bitmap logo;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        try {
            byte[] z= Base64.decode(MainActivity.LOGO, Base64.DEFAULT);
            logo= BitmapFactory.decodeByteArray(z,0,z.length);
        } catch(Exception ignored){}
        setContentView(build());
    }

    private View build(){
        ScrollView s=new ScrollView(this);
        s.setFillViewport(true);
        s.setBackgroundColor(BG);
        LinearLayout root=v();
        root.setPadding(dp(16),dp(16),dp(16),dp(28));
        s.addView(root,new ScrollView.LayoutParams(-1,-2));

        LinearLayout hero=h();
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(18),dp(18),dp(18),dp(18));
        hero.setBackground(round(DARK,22));
        ImageView im=new ImageView(this);
        if(logo!=null) im.setImageBitmap(logo);
        im.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(dp(92),dp(92));
        ilp.setMarginEnd(dp(12));
        hero.addView(im,ilp);
        LinearLayout ht=v();
        ht.addView(t("انار رایانه",28,Color.WHITE,true));
        ht.addView(t("شرکت فنی مهندسی",14,Color.rgb(246,221,225),false));
        TextView ss=t("مدیریت هوشمند فاکتور و فروش",13,Color.rgb(230,188,196),false);
        ss.setPadding(0,dp(3),0,0);
        ht.addView(ss);
        hero.addView(ht,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(hero,margin(0,0,0,16));

        TextView welcome=t("به فاکتور انار رایانه خوش آمدید",20,DARK,true);
        root.addView(welcome);
        TextView sub=t("صدور فاکتور رسمی با تاریخ شمسی، واحد تومان و خروجی PDF و تصویر",13,Color.DKGRAY,false);
        sub.setPadding(0,dp(4),0,dp(14));
        root.addView(sub);

        LinearLayout r1=h();
        r1.addView(card("صدور فاکتور","فاکتور جدید","＋",v->openInvoice()),weight());
        r1.addView(card("فاکتورهای من","مشاهده و ثبت سوابق","☰",v->openInvoice()),weight());
        root.addView(r1,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout r2=h();
        r2.addView(card("کالا و خدمات","ثبت شرح خدمات و کالا","▦",v->openInvoice()),weight());
        r2.addView(card("گزارشات","فروش و عملکرد","▥",v->toast("گزارشات پیشرفته در نسخه بعدی اضافه می‌شود.")),weight());
        root.addView(r2,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout quick=v();
        quick.setPadding(dp(16),dp(15),dp(16),dp(15));
        quick.setBackground(round(Color.WHITE,18));
        TextView qh=t("امکانات نسخه اختصاصی",16,DARK,true);
        quick.addView(qh);
        quick.addView(feature("✓  استفاده کاملاً آفلاین"));
        quick.addView(feature("✓  شماره‌گذاری فاکتور و تاریخ شمسی"));
        quick.addView(feature("✓  ذخیره مستقیم به صورت PDF"));
        quick.addView(feature("✓  ذخیره فاکتور به صورت تصویر PNG"));
        quick.addView(feature("✓  لوگوی انار رایانه داخل خود فاکتور"));
        quick.addView(feature("✓  تم قرمز آلبالویی و طلایی"));
        root.addView(quick,margin(0,12,0,12));

        LinearLayout c=v();
        c.setPadding(dp(16),dp(14),dp(16),dp(14));
        c.setBackground(round(Color.rgb(255,252,246),16));
        c.addView(t("انار رایانه • شرکت فنی مهندسی",14,DARK,true));
        c.addView(t("06633213562   |   09167479581",13,TEXT,false));
        c.addView(t("لرستان، خیابان انقلاب، کوچه قدس، جنب بانک سپه",12,TEXT,false));
        root.addView(c);

        TextView foot=t("Anar Rayaneh Invoice • نسخه اختصاصی",11,Color.GRAY,false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot,margin(0,18,0,0));
        return s;
    }

    private void openInvoice(){
        startActivity(new Intent(this,MainActivity.class));
    }
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}

    private LinearLayout card(String title,String subtitle,String icon,View.OnClickListener l){
        LinearLayout x=v();
        x.setPadding(dp(15),dp(14),dp(15),dp(14));
        x.setBackground(round(Color.WHITE,18));
        x.setElevation(dp(2));
        TextView ic=t(icon,30,GOLD,true); ic.setGravity(Gravity.RIGHT);
        TextView a=t(title,16,DARK,true);
        TextView b=t(subtitle,12,Color.DKGRAY,false);
        x.addView(ic);x.addView(a);x.addView(b);
        x.setOnClickListener(l);
        return x;
    }
    private TextView feature(String s){TextView x=t(s,13,TEXT,false);x.setPadding(0,dp(5),0,dp(5));return x;}
    private LinearLayout v(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    private LinearLayout h(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    private TextView t(String s,int size,int color,boolean bold){TextView x=new TextView(this);x.setText(s);x.setTextSize(size);x.setTextColor(color);x.setGravity(Gravity.RIGHT);x.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);x.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));return x;}
    private GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(145),1f);p.setMargins(dp(5),dp(5),dp(5),dp(5));return p;}
    private LinearLayout.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
