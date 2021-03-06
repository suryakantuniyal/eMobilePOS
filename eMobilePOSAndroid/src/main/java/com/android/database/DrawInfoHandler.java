package com.android.database;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;

import com.android.support.DateUtils;

import net.sqlcipher.database.SQLiteStatement;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DrawInfoHandler {
    private static final String table_name = "DrawDateInfo";
    private final String ID = "ID";
    private final String CalendarVersionID = "CalendarVersionID";
    private final String DrawNumber = "DrawNumber";
    private final String DrawDate = "DrawDate";
    private final String CutOffDate = "CutOffDate";
    private final String CutOffTime = "CutOffTime";
    private final String CutOffDateTime = "CutOffDateTime";
    private final String OpportunityFactor = "OpportunityFactor";
    private final List<String> attr = Arrays.asList(ID, CalendarVersionID, DrawNumber, DrawDate, CutOffDate,
            CutOffTime, CutOffDateTime, OpportunityFactor);
    private StringBuilder sb1, sb2;
    private HashMap<String, Integer> attrHash;
    private List<String[]> addrData;
    private List<HashMap<String, Integer>> dictionaryListMap;

    public DrawInfoHandler(Context activity) {
        attrHash = new HashMap<>();
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

    private int index(String tag) {
        return attrHash.get(tag);
    }

    private String getData(String tag, int record) {
        Integer i = dictionaryListMap.get(record).get(tag);
        if (i != null) {
            return addrData.get(record)[i];
        }
        return "";
    }

    public void insert(List<String[]> data, List<HashMap<String, Integer>> dictionary) {
        DBManager.getDatabase().beginTransaction();
        SQLiteStatement insert = null;
        try {

            addrData = data;
            dictionaryListMap = dictionary;

            insert = DBManager.getDatabase().compileStatement("INSERT INTO " + table_name + " (" + sb1.toString() + ") " + "VALUES (" + sb2.toString() + ")");

            int size = addrData.size();

            for (int i = 0; i < size; i++) {
                insert.bindString(index(ID), getData(ID, i));    //ID
                insert.bindString(index(CalendarVersionID), getData(CalendarVersionID, i)); //CalendarVersionID
                insert.bindString(index(DrawNumber), getData(DrawNumber, i));    //DrawNumber
                insert.bindString(index(DrawDate), getData(DrawDate, i));    //DrawDate
                insert.bindString(index(CutOffDate), getData(CutOffDate, i));    //CutOffDate
                insert.bindString(index(CutOffTime), getData(CutOffTime, i));    //CutOffTime
                insert.bindString(index(CutOffDateTime), getData(CutOffDateTime, i));    //CutOffDateTime
                insert.bindString(index(OpportunityFactor), getData(OpportunityFactor, i));    //OpportunityFactor

                insert.execute();
                insert.clearBindings();
            }
            insert.close();
            DBManager.getDatabase().setTransactionSuccessful();

        } catch (Exception e) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(e.getMessage()).append(" [com.android.emobilepos.DrawInfoHandler (at Class.insert)]");

//			Tracker tracker = EasyTracker.getInstance(activity);
//			tracker.send(MapBuilder.createException(sb.toString(), false).build());
        } finally {
            if (insert != null) {
                insert.close();
            }
            DBManager.getDatabase().endTransaction();
        }
    }


    public void emptyTable() {
        DBManager.getDatabase().execSQL("DELETE FROM " + table_name);
    }


    public String getDrawDate() {
        //SQLiteDatabase db = dbManager.openReadableDB();
        Cursor cursor = null;
        try {
            cursor = DBManager.getDatabase().rawQuery("SELECT DrawNumber,DrawDate FROM DrawDateInfo WHERE datetime(CutOffDateTime,'localtime') >= datetime('" + DateUtils.getDateAsString(new Date(), DateUtils.DATE_yyyy_MM_ddTHH_mm_ss) + "','localtime') ORDER BY CutOffDate ", null);

            String drawDate = "N/A";
            if (cursor.moveToFirst()) {
                drawDate = "EN DRAW000 00000000";
// 	drawDate = "EN DRAW"+cursor.getString(cursor.getColumnIndex(DrawNumber))+" "+cursor.getString(cursor.getColumnIndex(DrawDate));
            }

            cursor.close();
            //db.close();

            //drawDate = "EC DRAW001 Jan/01/12";

            return drawDate;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }


}
