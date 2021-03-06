package com.android.database;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;

import net.sqlcipher.database.SQLiteStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TermsHandler {

    private static final String table_name = "Terms";
    private final String terms_id = "terms_id";
    private final String terms_name = "terms_name";
    private final String terms_stdduedays = "terms_stdduedays";
    private final String terms_stddiscdays = "terms_stddiscdays";
    private final String terms_discpct = "terms_discpct";
    private final String terms_update = "terms_update";
    private final String isactive = "isactive";
    private final List<String> attr = Arrays.asList(terms_id, terms_name, terms_stdduedays, terms_stddiscdays, terms_discpct, isactive,
            terms_update);
    private StringBuilder sb1, sb2;
    private HashMap<String, Integer> attrHash;
    private List<String[]> addrData;
    private List<HashMap<String, Integer>> dictionaryListMap;

    public TermsHandler(Context activity) {
        attrHash = new HashMap<>();
        addrData = new ArrayList<>();
        sb1 = new StringBuilder();
        sb2 = new StringBuilder();
        new DBManager(activity);
        initDictionary();
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

    private String getData(String tag, int record) {
        Integer i = dictionaryListMap.get(record).get(tag);
        if (i != null) {
            return addrData.get(record)[i];
        }
        return "";
    }

    private int index(String tag) {
        return attrHash.get(tag);
    }


    public void insert(List<String[]> data, List<HashMap<String, Integer>> dictionary) {
        DBManager.getDatabase().beginTransaction();
        try {

            addrData = data;
            dictionaryListMap = dictionary;
            SQLiteStatement insert = null;
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(table_name).append(" (").append(sb1.toString()).append(") ").append("VALUES (").append(sb2.toString()).append(")");
            insert = DBManager.getDatabase().compileStatement(sb.toString());

            int size = addrData.size();

            for (int j = 0; j < size; j++) {
                insert.bindString(index(terms_id), getData(terms_id, j)); // terms_id
                insert.bindString(index(terms_name), getData(terms_name, j)); // terms_name
                insert.bindString(index(terms_stdduedays), getData(terms_stdduedays, j)); // terms_stdduedays
                insert.bindString(index(terms_stddiscdays), getData(terms_stddiscdays, j)); // terms_stddiscdays
                insert.bindString(index(terms_discpct), getData(terms_discpct, j)); // terms_discpct
                insert.bindString(index(terms_update), getData(terms_update, j)); // terms_update
                insert.bindString(index(isactive), getData(isactive, j)); // isactive

                insert.execute();
                insert.clearBindings();

            }
            insert.close();
            DBManager.getDatabase().setTransactionSuccessful();
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage()).append(" [com.android.emobilepos.TermsHandler (at Class.insert)]");

//			Tracker tracker = EasyTracker.getInstance(activity);
//			tracker.send(MapBuilder.createException(sb.toString(), false).build());
        } finally {
            DBManager.getDatabase().endTransaction();
        }
    }

    public void emptyTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(table_name);
        DBManager.getDatabase().execSQL(sb.toString());
    }


    public List<String[]> getAllTerms() {
        //SQLiteDatabase db = dbManager.openReadableDB();
        Cursor cursor = null;
        try {
            String query = "SELECT terms_name,terms_id FROM Terms WHERE isactive='1' ORDER BY terms_name";
            cursor = DBManager.getDatabase().rawQuery(query, null);
            List<String[]> arrayList = new ArrayList<String[]>();
            String[] arrayValues = new String[2];
            //int i = 0;
            if (cursor.moveToFirst()) {
                int nameColumnIndex = cursor.getColumnIndex(terms_name);
                int idColumnIndex = cursor.getColumnIndex(terms_id);
                do {
                    arrayValues[0] = cursor.getString(nameColumnIndex);
                    arrayValues[1] = cursor.getString(idColumnIndex);
                    arrayList.add(arrayValues);
                    arrayValues = new String[2];
                } while (cursor.moveToNext());
            }

            cursor.close();
            //db.close();
            return arrayList;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }
}
