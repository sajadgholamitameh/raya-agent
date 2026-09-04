package com.anarrayaneh.invoice;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.text.NumberFormat;
import java.util.Locale;

public class InvoiceTemplateView extends View {
    public static final int BASE_W=1055, BASE_H=1491;
    private static final int CHERRY=Color.rgb(144,8,29);
    private static final int DARK=Color.rgb(87,7,23);
    private static final int GOLD=Color.rgb(226,171,39);
    private final InvoiceData data;
    private final Bitmap logo;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);

    public InvoiceTemplateView(Context c, InvoiceData d, Bitmap logo){
        super(c); this.data=d; this.logo=logo; setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }
    @Override protected void onMeasure(int ws,int hs){ setMeasuredDimension(BASE_W,BASE_H); }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float sx=getWidth()/(float)BASE_W, sy=getHeight()/(float)BASE_H;
        c.save(); c.scale(sx,sy); c.drawColor(Color.WHITE);
        drawHeader(c); drawInfo(c); drawTable(c); drawTotals(c); drawSignatures(c); drawFooter(c);
        c.restore();
    }
    private void drawHeader(Canvas c){
        p.setStyle(Paint.Style.FILL); p.setColor(CHERRY);
        Path red=new Path(); red.moveTo(0,0); red.lineTo(470,0); red.cubicTo(405,70,350,130,0,185); red.close(); c.drawPath(red,p);
        p.setColor(GOLD); Path g=new Path(); g.moveTo(0,180); g.cubicTo(230,145,370,110,480,0); g.lineTo(505,0); g.cubicTo(395,125,235,170,0,205); g.close(); c.drawPath(g,p);
        text(c,"راهکارهای هوشمند",48,48,29,Color.WHITE,true,Paint.Align.LEFT);
        text(c,"برای آینده روشن",48,86,29,Color.WHITE,true,Paint.Align.LEFT);
        p.setColor(GOLD);p.setStrokeWidth(3);c.drawLine(45,124,170,124,p);
        if(logo!=null)c.drawBitmap(logo,null,new RectF(475,12,705,210),p);
        text(c,"06633213562",980,62,24,Color.BLACK,true,Paint.Align.RIGHT);
        text(c,"09167479581",980,102,24,Color.BLACK,true,Paint.Align.RIGHT);
        text(c,"لرستان خیابان انقلاب",980,147,21,Color.BLACK,true,Paint.Align.RIGHT);
        text(c,"کوچه قدس جنب بانک سپه",980,178,21,Color.BLACK,true,Paint.Align.RIGHT);
        text(c,"تجهیز امروز، آسایش فردا",980,224,18,DARK,false,Paint.Align.RIGHT);
        p.setColor(GOLD);p.setStrokeWidth(3);c.drawLine(745,226,790,226,p);c.drawLine(990,226,1025,226,p);
        p.setColor(CHERRY); c.drawRoundRect(new RectF(28,215,420,285),32,32,p);
        p.setColor(GOLD); c.drawRoundRect(new RectF(28,215,52,285),24,24,p);
        text(c,"فاکتور فروش",385,262,38,Color.WHITE,true,Paint.Align.RIGHT);
        text(c,"S A L E S   I N V O I C E",80,315,18,Color.BLACK,false,Paint.Align.LEFT);
    }
    private void drawInfo(Canvas c){
        box(c,28,330,1027,455,Color.rgb(255,253,252),Color.rgb(220,55,65),2,16);
        text(c,"شماره فاکتور:",1000,360,20,CHERRY,true,Paint.Align.RIGHT);
        fillBox(c,744,338,875,377,Color.rgb(247,241,237),10);
        text(c,fa(data.number),860,365,20,DARK,true,Paint.Align.RIGHT);
        text(c,"تاریخ:",1000,410,20,CHERRY,true,Paint.Align.RIGHT);
        fillBox(c,744,387,875,428,Color.rgb(247,241,237),10);
        text(c,fa(data.date),860,414,20,DARK,true,Paint.Align.RIGHT);
        field(c,"نام مشتری:",data.customer,690,360,65,360);
        field(c,"شماره تماس مشتری:",data.phone,690,402,65,402);
        field(c,"آدرس مشتری:",data.address,690,442,65,442);
    }
    private void field(Canvas c,String label,String value,float lx,float y,float start,float vy){
        text(c,label,lx,y,19,CHERRY,true,Paint.Align.RIGHT);
        p.setColor(Color.rgb(110,110,110));p.setStrokeWidth(1.5f);c.drawLine(start,y+4,lx-175,y+4,p);
        String v=value==null?"":value.trim(); if(!v.isEmpty()) text(c,v,lx-185,vy,18,Color.rgb(45,45,45),false,Paint.Align.RIGHT);
    }
    private void drawTable(Canvas c){
        float left=28,right=1027,top=472,headerH=52,rowH=39.8f;
        p.setColor(CHERRY);c.drawRect(left,top,right,top+headerH,p);
        float x0=28,x1=238,x2=435,x3=532,x4=930,x5=1027;
        String[] heads={"مبلغ کل (تومان)","قیمت واحد (تومان)","تعداد","شرح کالا / خدمات","ردیف"};
        float[] centers={(x0+x1)/2,(x1+x2)/2,(x2+x3)/2,(x3+x4)/2,(x4+x5)/2};
        for(int i=0;i<heads.length;i++) text(c,heads[i],centers[i],507,19,Color.WHITE,true,Paint.Align.CENTER);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(Color.rgb(205,205,205));
        for(int r=0;r<=10;r++){float y=top+headerH+r*rowH;c.drawLine(left,y,right,y,p);} 
        float[] xs={x0,x1,x2,x3,x4,x5};for(float x:xs)c.drawLine(x,top,x,top+headerH+10*rowH,p);
        p.setStyle(Paint.Style.FILL);
        if(logo!=null){p.setAlpha(38);c.drawBitmap(logo,null,new RectF(365,590,705,865),p);p.setAlpha(255);}        
        int n=Math.min(data.items.size(),10);
        for(int i=0;i<10;i++){
            float cy=top+headerH+(i+.67f)*rowH;
            text(c,fa(String.valueOf(i+1)),1000,cy,18,Color.BLACK,false,Paint.Align.CENTER);
            if(i<n){InvoiceData.Item it=data.items.get(i);text(c,it.description,910,cy,18,Color.rgb(45,45,45),false,Paint.Align.RIGHT);text(c,fa(qty(it.quantity)),483,cy,18,Color.BLACK,false,Paint.Align.CENTER);text(c,fa(fmt(it.unitPrice)),422,cy,17,Color.BLACK,false,Paint.Align.RIGHT);text(c,fa(fmt(it.total)),225,cy,17,Color.BLACK,false,Paint.Align.RIGHT);}
        }
    }
    private void drawTotals(Canvas c){
        float y=940;
        box(c,28,y,510,1095,Color.WHITE,Color.rgb(220,70,70),1.5f,12);
        float mid=285; p.setColor(CHERRY);c.drawRect(mid,y,510,y+52,p);c.drawRect(mid,y+52,510,y+104,p);p.setColor(GOLD);c.drawRect(mid,y+104,510,y+155,p);
        text(c,"جمع مبلغ",485,y+34,22,Color.WHITE,true,Paint.Align.RIGHT);
        text(c,"تخفیف",485,y+86,22,Color.WHITE,true,Paint.Align.RIGHT);
        text(c,"جمع نهایی",485,y+139,23,Color.BLACK,true,Paint.Align.RIGHT);
        text(c,fa(fmt(data.sum))+" تومان",265,y+34,20,Color.BLACK,true,Paint.Align.RIGHT);
        text(c,fa(fmt(data.discount))+" تومان",265,y+86,20,Color.BLACK,true,Paint.Align.RIGHT);
        text(c,fa(fmt(data.total))+" تومان",265,y+139,21,DARK,true,Paint.Align.RIGHT);
        box(c,530,y,1027,1095,Color.rgb(255,253,252),Color.rgb(220,70,70),1.5f,12);
        text(c,"توضیحات:",1000,y+34,21,CHERRY,true,Paint.Align.RIGHT);
        String notes=data.notes==null?"":data.notes.trim();
        if(notes.isEmpty()){
            p.setColor(Color.rgb(90,90,90));p.setStrokeWidth(1);for(int i=0;i<3;i++)c.drawLine(560,y+62+i*27,995,y+62+i*27,p);
        }else{
            drawWrapped(c,notes,995,y+65,410,18,Color.rgb(45,45,45));
        }
        p.setColor(GOLD);p.setStrokeWidth(3);c.drawLine(620,y+132,675,y+132,p);c.drawLine(900,y+132,955,y+132,p);
        text(c,"از اعتماد شما سپاسگزاریم",815,y+139,17,DARK,true,Paint.Align.CENTER);
    }
    private void drawSignatures(Canvas c){
        box(c,28,1115,510,1300,Color.WHITE,Color.rgb(220,70,70),1.5f,12);
        box(c,530,1115,1027,1300,Color.WHITE,Color.rgb(220,70,70),1.5f,12);
        text(c,"مهر و امضای شرکت",275,1148,20,Color.BLACK,true,Paint.Align.CENTER);
        text(c,"امضا و تایید مشتری",780,1148,20,Color.BLACK,true,Paint.Align.CENTER);
        if(logo!=null)c.drawBitmap(logo,null,new RectF(150,1160,380,1290),p);
    }
    private void drawFooter(Canvas c){
        Path red=new Path();red.moveTo(0,1320);red.cubicTo(260,1295,455,1420,695,1360);red.cubicTo(825,1328,920,1285,1055,1240);red.lineTo(1055,1491);red.lineTo(0,1491);red.close();p.setColor(CHERRY);c.drawPath(red,p);
        Path gold=new Path();gold.moveTo(0,1312);gold.cubicTo(270,1287,455,1402,690,1345);gold.cubicTo(850,1306,940,1260,1055,1225);gold.lineTo(1055,1245);gold.cubicTo(930,1285,850,1327,695,1365);gold.cubicTo(455,1425,260,1302,0,1330);gold.close();p.setColor(GOLD);c.drawPath(gold,p);
        text(c,"06633213562  |  09167479581",70,1450,18,Color.WHITE,true,Paint.Align.LEFT);
        text(c,"کیفیت • تعهد • اعتماد",985,1450,19,Color.WHITE,true,Paint.Align.RIGHT);
    }
    private void drawWrapped(Canvas c,String s,float right,float y,float maxW,float size,int color){
        p.setTextSize(size);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setColor(color);p.setTextAlign(Paint.Align.RIGHT);
        String[] words=s.split("\\s+");String line="";float yy=y;for(String w:words){String test=line.isEmpty()?w:w+" "+line;if(p.measureText(test)>maxW&&!line.isEmpty()){c.drawText(line,right,yy,p);yy+=25;line=w;}else line=test;}if(!line.isEmpty())c.drawText(line,right,yy,p);
    }
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align a){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTextAlign(a);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s==null?"":s,x,y,p);}
    private void box(Canvas c,float l,float t,float r,float b,int fill,int stroke,float sw,float rad){p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRoundRect(new RectF(l,t,r,b),rad,rad,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw);p.setColor(stroke);c.drawRoundRect(new RectF(l,t,r,b),rad,rad,p);p.setStyle(Paint.Style.FILL);}
    private void fillBox(Canvas c,float l,float t,float r,float b,int fill,float rad){p.setColor(fill);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(l,t,r,b),rad,rad,p);}
    private String fmt(long n){return NumberFormat.getNumberInstance(Locale.US).format(n);} private String qty(double q){return q==(long)q?String.valueOf((long)q):String.format(Locale.US,"%.2f",q);}    
    private String fa(String s){if(s==null)return "";String a="0123456789",f="۰۱۲۳۴۵۶۷۸۹";for(int i=0;i<10;i++)s=s.replace(a.charAt(i),f.charAt(i));return s;}
}
