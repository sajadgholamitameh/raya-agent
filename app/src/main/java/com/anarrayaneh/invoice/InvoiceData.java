package com.anarrayaneh.invoice;

import java.util.ArrayList;

public class InvoiceData {
    public String number="";
    public String date="";
    public String customer="";
    public String phone="";
    public String address="";
    public String notes="";
    public long sum=0;
    public long discount=0;
    public long total=0;
    public final ArrayList<Item> items=new ArrayList<>();

    public static class Item {
        public String description="";
        public double quantity=1;
        public long unitPrice=0;
        public long total=0;
    }
}
