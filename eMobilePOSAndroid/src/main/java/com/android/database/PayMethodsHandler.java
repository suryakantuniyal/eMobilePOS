package com.android.database;

import android.content.Context;
import android.database.Cursor;

import com.android.dao.PaymentMethodDAO;
import com.android.emobilepos.models.realms.PaymentMethod;
import com.android.support.MyPreferences;

import net.sqlcipher.database.SQLiteStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import util.StringUtil;

public class PayMethodsHandler {


    private final static String paymethod_id = "paymethod_id";
    private static final String table_name = "PayMethods";
    private final String paymethod_name = "paymethod_name";
    private final String paymentmethod_type = "paymentmethod_type";
    private final String paymethod_update = "paymethod_update";
    private final String isactive = "isactive";
    private final String paymethod_showOnline = "paymethod_showOnline";
    private final String image_url = "image_url";
    private final String OriginalTransid = "OriginalTransid";
    private final List<String> attr = Arrays.asList(paymethod_id, paymethod_name, paymentmethod_type, paymethod_update,
            isactive, paymethod_showOnline, image_url, OriginalTransid);
    private StringBuilder sb1, sb2;
    private HashMap<String, Integer> attrHash;
    private MyPreferences myPref;

    public PayMethodsHandler(Context activity) {
        attrHash = new HashMap<>();
        sb1 = new StringBuilder();
        sb2 = new StringBuilder();
        myPref = new MyPreferences(activity);
        new DBManager(activity);
        initDictionary();
    }

    public static String getPayMethodID(String methodType) {
        //SQLiteDatabase db = dbManager.openReadableDB();
        Cursor cursor = null;
        try {
            String data = "";
            String[] fields = new String[]{paymethod_id};

            cursor = DBManager.getDatabase().query(
                    true,
                    table_name,
                    fields,
                    "paymentmethod_type= '" + methodType + "' COLLATE NOCASE",
                    null, null, null,
                    null, null);
            if (cursor.moveToFirst()) {
                do {
                    data = cursor.getString(cursor.getColumnIndex(paymethod_id));
                } while (cursor.moveToNext());
            }
            cursor.close();

            if (data.isEmpty()) { // Visa defaulting
                cursor = DBManager.getDatabase().query(
                        true,
                        table_name,
                        fields,
                        "paymentmethod_type= 'Visa' COLLATE NOCASE",
                        null, null, null,
                        null, null);
                if (cursor.moveToFirst()) {
                    data = cursor.getString(cursor.getColumnIndex(paymethod_id));
                }
                cursor.close();
            }

            return data;
        } finally {
            {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
    }

    private void initDictionary() {
        int size = attr.size();
        for (int i = 0; i < size; i++) {
            attrHash.put(attr.get(i), i + 1);
            if ((i + 1) < size) {
                sb1.append(attr.get(i)).append(",");
                sb2.append("?").append(",");
            } else {
                sb1.append(attr.get(i));
                sb2.append("?");
            }
        }
    }

    private int index(String tag) {
        return attrHash.get(tag);
    }

    public void insert(List<PaymentMethod> paymentMethods) {
        SQLiteStatement insert = null;
        DBManager.getDatabase().beginTransaction();
        try {
            insert = DBManager.getDatabase().compileStatement("INSERT INTO " + table_name + " (" + sb1.toString() + ") " + "VALUES (" + sb2.toString() + ")");

            for (PaymentMethod method : paymentMethods) {
                insert.bindString(index(paymethod_id), StringUtil.nullStringToEmpty(method.getPaymethod_id()));
                insert.bindString(index(paymethod_name), StringUtil.nullStringToEmpty(method.getPaymethod_name()));
                insert.bindString(index(paymentmethod_type), StringUtil.nullStringToEmpty(method.getPaymentmethod_type()));
                insert.bindString(index(paymethod_update), StringUtil.nullStringToEmpty(method.getPaymethod_update()));
                insert.bindString(index(isactive), StringUtil.nullStringToEmpty(method.getIsactive()));
                insert.bindString(index(paymethod_showOnline), StringUtil.nullStringToEmpty(method.getPaymethod_showOnline()));
                insert.bindString(index(image_url), StringUtil.nullStringToEmpty(method.getImage_url()));
                insert.bindString(index(OriginalTransid), Boolean.parseBoolean(method.getOriginalTransid()) ? "1" : "0");
                insert.execute();
                insert.clearBindings();
            }
            if (myPref.getPreferences(MyPreferences.pref_mw_with_genius)) {
                paymentMethods.add(PaymentMethod.getGeniusPaymentMethod());
            }
            if (myPref.getPreferences(MyPreferences.pref_pay_with_tupyx)) {
                paymentMethods.add(PaymentMethod.getTupyxPaymentMethod());
            }
            if (myPref.getPreferences(MyPreferences.pref_pay_with_card_on_file)) {
                paymentMethods.add(PaymentMethod.getCardOnFilePaymentMethod());
            }
            PaymentMethodDAO.insert(paymentMethods);
            //   insert.close();
            DBManager.getDatabase().setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (insert != null) {
                insert.close();
            }
            DBManager.getDatabase().endTransaction();
        }
    }

    public void emptyTable() {
        DBManager.getDatabase().execSQL("DELETE FROM " + table_name);
        PaymentMethodDAO.truncate();
    }

    public List<String[]> getPayMethodsName() {
        Cursor cursor = null;
        try {
            List<String[]> list = new ArrayList<>();

            String[] fields = new String[]{paymethod_id, paymethod_name};

            cursor = DBManager.getDatabase().query(true, table_name, fields, "paymethod_id!=''", null, null, null, paymethod_name + " ASC", null);

            //--------------- add additional payment methods ----------------
            if (myPref.getPreferences(MyPreferences.pref_mw_with_genius)) {
                String[] extraMethods = new String[]{"Genius", "Genius", "Genius", "", "0"};
                list.add(extraMethods);
            }
            if (myPref.getPreferences(MyPreferences.pref_pay_with_tupyx)) {
                String[] extraMethods = new String[]{"Wallet", "Tupyx", "Wallet", "", "0"};
                list.add(extraMethods);
            }

            if (cursor.moveToFirst()) {
                String[] values = new String[2];
                int i_paymethod_id = cursor.getColumnIndex(paymethod_id);
                int i_paymethod_name = cursor.getColumnIndex(paymethod_name);
                do {

                    values[0] = cursor.getString(i_paymethod_id);
                    values[1] = cursor.getString(i_paymethod_name);
                    list.add(values);
                    values = new String[2];

                } while (cursor.moveToNext());
            }
            cursor.close();
            //db.close();
            return list;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public String getSpecificPayMethodId(String methodName) {
        Cursor cursor = null;
        try {
            String[] fields = new String[]{paymethod_id};
            cursor = DBManager.getDatabase().query(true, table_name, fields, "paymethod_name = '" + methodName + "'", null, null, null, null, null);
            String data = "";
            if (cursor.moveToFirst()) {
                do {
                    data = cursor.getString(cursor.getColumnIndex(paymethod_id));
                } while (cursor.moveToNext());
            }
            cursor.close();
            return data;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

}
