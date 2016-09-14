package com.android.database;

import android.database.Cursor;

import com.android.emobilepos.holders.Locations_Holder;

import net.sqlcipher.database.SQLiteStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Locations_DB {
    public static final String loc_key = "loc_key";
    public static final String loc_id = "loc_id";
    public static final String loc_name = "loc_name";

    private static final List<String> attr = Arrays.asList(loc_key, loc_id, loc_name);

    private static final String TABLE_NAME = "Locations";
    private StringBuilder sb1, sb2;
    private HashMap<String, Integer> attrHash;
    private List<String[]> prodData;
    private List<HashMap<String, Integer>> dictionaryListMap;


    public Locations_DB() {
        attrHash = new HashMap<>();
        prodData = new ArrayList<>();
        sb1 = new StringBuilder();
        sb2 = new StringBuilder();
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
            return prodData.get(record)[i];
        }
        return "";
    }


    private int index(String tag) {
        return attrHash.get(tag);
    }


    public void insert(List<String[]> data, List<HashMap<String, Integer>> dictionary) {
        DBManager._db.beginTransaction();

        try {
            prodData = data;
            dictionaryListMap = dictionary;
            SQLiteStatement insert;
            insert = DBManager._db.compileStatement("INSERT INTO " + TABLE_NAME + " (" + sb1.toString() + ") " + "VALUES (" + sb2.toString() + ")");

            int size = prodData.size();

            for (int j = 0; j < size; j++) {
                insert.bindString(index(loc_key), getData(loc_key, j));
                insert.bindString(index(loc_id), getData(loc_id, j));
                insert.bindString(index(loc_name), getData(loc_name, j));

                insert.execute();
                insert.clearBindings();
            }
            insert.close();
            DBManager._db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBManager._db.endTransaction();
        }
    }


    public void emptyTable() {
        DBManager._db.execSQL("DELETE FROM " + TABLE_NAME);
    }

    public List<Locations_Holder> getLocationsList() {
        List<Locations_Holder> list = new ArrayList<>();
        Cursor c = DBManager._db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY loc_name ASC", null);
        if (c.moveToFirst()) {
            int i_loc_key = c.getColumnIndex(loc_key);
            int i_loc_id = c.getColumnIndex(loc_id);
            int i_loc_name = c.getColumnIndex(loc_name);

            Locations_Holder location;
            do {
                location = new Locations_Holder();
                location.setLoc_id(c.getString(i_loc_id));
                location.setLoc_key(c.getString(i_loc_key));
                location.setLoc_name(c.getString(i_loc_name));
                list.add(location);

            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public Locations_Holder getLocationInfo(String _loc_key) {
        Cursor c = DBManager._db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE loc_key = ?", new String[]{_loc_key});
        Locations_Holder location = new Locations_Holder();
        if (c.moveToFirst()) {
            int i_loc_key = c.getColumnIndex(loc_key);
            int i_loc_id = c.getColumnIndex(loc_id);
            int i_loc_name = c.getColumnIndex(loc_name);

            location.setLoc_id(c.getString(i_loc_id));
            location.setLoc_key(c.getString(i_loc_key));
            location.setLoc_name(c.getString(i_loc_name));
        }
        c.close();
        return location;
    }
}
