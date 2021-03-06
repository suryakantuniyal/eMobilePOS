package com.android.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.android.dao.AssignEmployeeDAO;
import com.android.dao.DinningTableOrderDAO;
import com.android.emobilepos.holders.Recoveries_Holder;
import com.android.emobilepos.models.DataTaxes;
import com.android.emobilepos.models.orders.Order;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.realms.AssignEmployee;
import com.android.emobilepos.models.realms.OrderAttributes;
import com.android.emobilepos.models.realms.ProductAttribute;
import com.android.support.DateUtils;
import com.android.support.GenerateNewID;
import com.android.support.Global;
import com.google.gson.Gson;

import net.sqlcipher.database.SQLiteStatement;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import util.json.JsonUtils;

import static com.android.database.OrderTaxes_DB.tax_amount;
import static com.android.database.OrderTaxes_DB.tax_name;

public class OrdersHandler {
    public static final String table_name = "Orders";
    private final static String ord_id = "ord_id";
    private final static String qbord_id = "qbord_id";
    private final static String emp_id = "emp_id";
    private final static String cust_id = "cust_id";
    private final static String custidkey = "custidkey";
    private final static String ord_po = "ord_po";
    private final static String total_lines = "total_lines";
    private final static String total_lines_pay = "total_lines_pay";
    private final static String ord_total = "ord_total";
    private final static String ord_signature = "ord_signature";
    private final static String ord_comment = "ord_comment";
    private final static String ord_delivery = "ord_delivery";
    private final static String ord_timecreated = "ord_timecreated";
    private final static String ord_timesync = "ord_timesync";
    private final static String qb_synctime = "qb_synctime";
    private final static String emailed = "emailed";
    private final static String processed = "processed";
    private final static String ord_type = "ord_type";
    private final static String ord_claimnumber = "ord_claimnumber";
    private final static String ord_rganumber = "ord_rganumber";
    private final static String ord_returns_pu = "ord_returns_pu";
    private final static String ord_inventory = "ord_inventory";
    private final static String ord_issync = "ord_issync";
    private final static String tax_id = "tax_id";
    private final static String ord_shipvia = "ord_shipvia";
    private final static String ord_shipto = "ord_shipto";
    private final static String ord_terms = "ord_terms";
    private final static String ord_custmsg = "ord_custmsg";
    private final static String ord_class = "ord_class";
    private final static String ord_subtotal = "ord_subtotal";
    private final static String ord_taxamount = "ord_taxamount";
    private final static String ord_discount = "ord_discount";
    private final static String c_email = "c_email";
    private final static String c_phone = "c_phone";
    private final static String isOnHold = "isOnHold";
    private final static String ord_HoldName = "ord_HoldName";
    private final static String orderAttributes = "orderAttributes";
    private final static String clerk_id = "clerk_id";
    private final static String ord_discount_id = "ord_discount_id";
    private final static String ord_latitude = "ord_latitude";
    private final static String ord_longitude = "ord_longitude";
    private final static String tipAmount = "tipAmount";
    private final static String isVoid = "isVoid";
    private final static String assignedTable = "assignedTable";
    private final static String numberOfSeats = "numberOfSeats";
    private final static String associateID = "associateID";
    private final static String is_stored_fwd = "is_stored_fwd";
    private final static String VAT = "VAT";
    private final static String bixolonTransactionId = "bixolonTransactionId";
    private static String ord_timeStarted = "ord_timeStarted";
    private final static String[] attr = {ord_id, qbord_id, emp_id, cust_id, clerk_id, c_email, c_phone,
            ord_signature, ord_po, total_lines, total_lines_pay, ord_total, ord_comment, ord_delivery, ord_timecreated,
            ord_timesync, qb_synctime, emailed, processed, ord_type, ord_claimnumber, ord_rganumber, ord_returns_pu,
            ord_inventory, ord_issync, tax_id, ord_shipvia, ord_shipto, ord_terms, ord_custmsg, ord_class, ord_subtotal,
            ord_taxamount, ord_discount, ord_discount_id, ord_latitude, ord_longitude, tipAmount, isVoid, custidkey,
            isOnHold, ord_HoldName, is_stored_fwd, VAT, assignedTable, numberOfSeats, associateID,
            ord_timeStarted, orderAttributes, bixolonTransactionId};
    private static CustomersHandler custHandler;
    private static OrderTaxes_DB taxes_db;
    private static OrderProductsHandler orderProductsHandler;
    SQLiteStatement sqlinsert;
    private StringBuilder sb1, sb2;
    private HashMap<String, Integer> attrHash;
    private Context activity;

    public OrdersHandler(Context activity) {
        attrHash = new HashMap<>();
        this.activity = activity;
        sb1 = new StringBuilder();
        new DBManager(activity);
        sb2 = new StringBuilder();
        initDictionary();
    }

    public static OrdersHandler getInstance(Context activity) {
        return new OrdersHandler(activity);
    }

    public static void deleteTransaction(Activity activity, String orderId) {
        if (!orderId.isEmpty()) {
            Global global = (Global) activity.getApplication();
            OrdersHandler dbOrders = new OrdersHandler(activity);
            OrderProductsHandler dbOrdProd = new OrderProductsHandler(activity);
            OrderProductsAttr_DB dbOrdAttr = new OrderProductsAttr_DB(activity);
            dbOrders.deleteOrder(orderId);
            dbOrdProd.deleteAllOrdProd(orderId);
            for (ProductAttribute val : global.ordProdAttr)
                dbOrdAttr.deleteOrderProduct(String.valueOf(val.getId()));
            DinningTableOrderDAO.deleteByOrderId(orderId);
        }
    }

    public static Order getOrder(Cursor cursor, Context activity) {
        Order order = new Order(activity);
        order.numberOfSeats = cursor.getInt(cursor.getColumnIndex(numberOfSeats));
        order.assignedTable = cursor.getString(cursor.getColumnIndex(assignedTable));
        order.associateID = cursor.getString(cursor.getColumnIndex(associateID));
        order.ord_id = cursor.getString(cursor.getColumnIndex(ord_id));
        order.qbord_id = cursor.getString(cursor.getColumnIndex(qbord_id));
        order.emp_id = cursor.getString(cursor.getColumnIndex(emp_id));
        order.cust_id = cursor.getString(cursor.getColumnIndex(cust_id));
        order.clerk_id = cursor.getString(cursor.getColumnIndex(clerk_id));
        order.c_email = cursor.getString(cursor.getColumnIndex(c_email));
        order.c_phone = cursor.getString(cursor.getColumnIndex(c_phone));
        order.ord_signature = cursor.getString(cursor.getColumnIndex(ord_signature));
        order.ord_po = cursor.getString(cursor.getColumnIndex(ord_po));
        order.total_lines = cursor.getString(cursor.getColumnIndex(total_lines));
        order.total_lines_pay = cursor.getString(cursor.getColumnIndex(total_lines_pay));
        order.ord_total = cursor.getString(cursor.getColumnIndex(ord_total));
        order.ord_comment = cursor.getString(cursor.getColumnIndex(ord_comment));
        order.ord_delivery = cursor.getString(cursor.getColumnIndex(ord_delivery));
        order.ord_timecreated = cursor.getString(cursor.getColumnIndex(ord_timecreated));
        order.ord_timeStarted = cursor.getString(cursor.getColumnIndex(ord_timeStarted));
        order.ord_timesync = cursor.getString(cursor.getColumnIndex(ord_timesync));
        order.qb_synctime = cursor.getString(cursor.getColumnIndex(qb_synctime));
        order.emailed = cursor.getString(cursor.getColumnIndex(emailed));
        order.processed = cursor.getString(cursor.getColumnIndex(processed));
        order.ord_type = cursor.getString(cursor.getColumnIndex(ord_type));
        order.ord_claimnumber = cursor.getString(cursor.getColumnIndex(ord_claimnumber));
        order.ord_rganumber = cursor.getString(cursor.getColumnIndex(ord_rganumber));
        order.ord_returns_pu = cursor.getString(cursor.getColumnIndex(ord_returns_pu));
        order.ord_inventory = cursor.getString(cursor.getColumnIndex(ord_inventory));
        order.ord_issync = cursor.getString(cursor.getColumnIndex(ord_issync));
        order.tax_id = cursor.getString(cursor.getColumnIndex(tax_id));
        order.ord_shipvia = cursor.getString(cursor.getColumnIndex(ord_shipvia));
        order.ord_shipto = cursor.getString(cursor.getColumnIndex(ord_shipto));
        order.ord_terms = cursor.getString(cursor.getColumnIndex(ord_terms));
        order.ord_custmsg = cursor.getString(cursor.getColumnIndex(ord_custmsg));
        order.ord_class = cursor.getString(cursor.getColumnIndex(ord_class));
        order.ord_subtotal = cursor.getString(cursor.getColumnIndex(ord_subtotal));
        order.ord_taxamount = cursor.getString(cursor.getColumnIndex(ord_taxamount));
        order.ord_discount = cursor.getString(cursor.getColumnIndex(ord_discount));
        order.ord_discount_id = cursor.getString(cursor.getColumnIndex(ord_discount_id));
        order.ord_latitude = cursor.getString(cursor.getColumnIndex(ord_latitude));
        order.ord_longitude = cursor.getString(cursor.getColumnIndex(ord_longitude));
        order.tipAmount = cursor.getString(cursor.getColumnIndex(tipAmount));
        order.isVoid = cursor.getString(cursor.getColumnIndex(isVoid));
        order.VAT = Boolean.toString(cursor.getString(cursor.getColumnIndex("VAT")).equals("1"));
        order.custidkey = cursor.getString(cursor.getColumnIndex(custidkey));
        order.isOnHold = cursor.getString(cursor.getColumnIndex(isOnHold));
        order.ord_HoldName = cursor.getString(cursor.getColumnIndex(ord_HoldName));
        order.is_stored_fwd = cursor.getString(cursor.getColumnIndex(is_stored_fwd));
        order.custidkey = cursor.getString(cursor.getColumnIndex(custidkey));
        order.setBixolonTransactionId(cursor.getString(cursor.getColumnIndex(bixolonTransactionId)));

        custHandler = new CustomersHandler(activity);
        order.customer = custHandler.getCustomer(order.cust_id);
        String attributes = cursor.getString(cursor.getColumnIndex(orderAttributes));
        if (!TextUtils.isEmpty(attributes)) {
            Gson gson = JsonUtils.getInstance();
            Type listType = new com.google.gson.reflect.TypeToken<List<OrderAttributes>>() {
            }.getType();
            order.orderAttributes = gson.fromJson(attributes, listType);
        }
        taxes_db = getOrderTaxes_DB();
        List<DataTaxes> orderTaxes = taxes_db.getOrderTaxes(order.ord_id);
        order.setListOrderTaxes(orderTaxes);
        orderProductsHandler = getOrderProductsHandler(activity);
        order.setOrderProducts(orderProductsHandler.getOrderProducts(order.ord_id));
        return order;
    }

    private static OrderTaxes_DB getOrderTaxes_DB() {
        if (taxes_db == null) {
            taxes_db = new OrderTaxes_DB();
        }
        return taxes_db;
    }

    private static OrderProductsHandler getOrderProductsHandler(Context context) {
        if (orderProductsHandler == null) {
            orderProductsHandler = new OrderProductsHandler(context);
        }
        return orderProductsHandler;
    }

    private static String getOrderAssignedTable(String orderId) {
        Cursor cursor = null;
        try {
            String tableNumber = "0";
            cursor = DBManager.getDatabase().query(table_name, attr, "ord_id=?",
                    new String[]{orderId}, null, null, null);
            if (cursor.moveToFirst()) {
                tableNumber = cursor.getString(cursor.getColumnIndex(assignedTable));
            }
            cursor.close();
            return tableNumber;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public void initDictionary() {
        int size = attr.length;
        for (int i = 0; i < size; i++) {
            attrHash.put(attr[i], i + 1);
            if ((i + 1) < size) {
                sb1.append(attr[i]).append(",");
                sb2.append("?").append(",");
            } else {
                sb1.append(attr[i]);
                sb2.append("?");
            }
        }
    }

    public int index(String tag) {
        return attrHash.get(tag);
    }

    public void insert(List<Order> orders) {

        DBManager.getDatabase().beginTransaction();
        try {
            for (Order order : orders) {

                String sb = "INSERT OR REPLACE INTO " + table_name + " (" + sb1.toString() + ") " +
                        "VALUES (" + sb2.toString() + ")";
                sqlinsert = DBManager.getDatabase().compileStatement(sb);
                DBUtils dbUtils = DBUtils.getInstance(sqlinsert, DBUtils.DBChild.ORDERS);
//                order.ord_id = generateNewID.getNextID(GenerateNewID.IdType.ORDER_ID);
                dbUtils.bindString(index(ord_id), order.ord_id == null ? "" : order.ord_id); // cust_id
                dbUtils.bindString(index(qbord_id), order.qbord_id == null ? "" : order.qbord_id); // cust_id
                dbUtils.bindString(index(emp_id), order.emp_id == null ? "" : order.emp_id); // cust_id
                dbUtils.bindString(index(cust_id), order.cust_id == null ? "" : order.cust_id); // cust_id
                dbUtils.bindString(index(clerk_id), order.clerk_id == null ? "" : order.clerk_id); // cust_id
                dbUtils.bindString(index(c_email), order.c_email == null ? "" : order.c_email); // cust_id
                dbUtils.bindString(index(c_phone), order.c_phone == null ? "" : order.c_phone);
                dbUtils.bindString(index(ord_signature), order.ord_signature == null ? "" : order.ord_signature); // cust_id
                dbUtils.bindString(index(ord_po), order.ord_po == null ? "" : order.ord_po); // cust_id
                dbUtils.bindString(index(total_lines), TextUtils.isEmpty(order.total_lines) ? "0" : order.total_lines); // cust_id
                dbUtils.bindString(index(total_lines_pay),
                        TextUtils.isEmpty(order.total_lines_pay) ? "0" : order.total_lines_pay); // cust_id
                dbUtils.bindString(index(ord_total), String.valueOf(TextUtils.isEmpty(order.ord_total) ? "0" : Global.getBigDecimalNum(order.ord_total, 2))); // cust_id
                dbUtils.bindString(index(ord_comment), order.ord_comment == null ? "" : order.ord_comment); // cust_id
                dbUtils.bindString(index(ord_delivery), order.ord_delivery == null ? "" : order.ord_delivery); // cust_id
                dbUtils.bindString(index(ord_timecreated), order.ord_timecreated == null ? "" : order.ord_timecreated); // cust_id
                dbUtils.bindString(index(ord_timesync), order.ord_timesync == null ? "" : order.ord_timesync); // cust_id
                dbUtils.bindString(index(qb_synctime), order.qb_synctime == null ? "" : order.qb_synctime); // cust_id
                dbUtils.bindString(index(emailed), TextUtils.isEmpty(order.emailed) ? "0" : order.emailed); // cust_id
                dbUtils.bindString(index(processed), TextUtils.isEmpty(order.processed) ? "1" : order.processed); // cust_id
                dbUtils.bindString(index(ord_type), order.ord_type == null ? "" : order.ord_type); // cust_id
                dbUtils.bindString(index(ord_claimnumber), order.ord_claimnumber == null ? "" : order.ord_claimnumber); // cust_id
                dbUtils.bindString(index(ord_rganumber), order.ord_rganumber == null ? "" : order.ord_rganumber); // cust_id
                dbUtils.bindString(index(ord_returns_pu), order.ord_returns_pu == null ? "" : order.ord_returns_pu); // cust_id
                dbUtils.bindString(index(ord_inventory), order.ord_inventory == null ? "" : order.ord_inventory); // cust_id
                dbUtils.bindString(index(ord_issync), TextUtils.isEmpty(order.ord_issync) ? "0" : order.ord_issync); // cust_id
                dbUtils.bindString(index(tax_id), order.tax_id == null ? "" : order.tax_id); // cust_id
                dbUtils.bindString(index(ord_shipvia), order.ord_shipvia == null ? "" : order.ord_shipvia); // cust_id
                dbUtils.bindString(index(ord_shipto), order.ord_shipto == null ? "" : order.ord_shipto); // cust_id
                dbUtils.bindString(index(ord_terms), order.ord_terms == null ? "" : order.ord_terms); // cust_id
                dbUtils.bindString(index(ord_custmsg), order.ord_custmsg == null ? "" : order.ord_custmsg); // cust_id
                dbUtils.bindString(index(ord_class), order.ord_class == null ? "" : order.ord_class); // cust_id
                dbUtils.bindString(index(ord_subtotal), TextUtils.isEmpty(order.ord_subtotal) ? "0" : order.ord_subtotal); // cust_id
                dbUtils.bindString(index(ord_taxamount), TextUtils.isEmpty(order.ord_taxamount) ? "0" : order.ord_taxamount); // cust_id
                dbUtils.bindString(index(ord_discount), TextUtils.isEmpty(order.ord_discount) ? "0" : order.ord_discount); // cust_id
                dbUtils.bindString(index(ord_discount_id), order.ord_discount_id == null ? "" : order.ord_discount_id); // cust_id
                dbUtils.bindString(index(ord_latitude), order.ord_latitude == null ? "" : order.ord_latitude); // cust_id
                dbUtils.bindString(index(ord_longitude), order.ord_longitude == null ? "" : order.ord_longitude); // cust_id
                dbUtils.bindString(index(tipAmount), TextUtils.isEmpty(order.tipAmount) ? "0" : order.tipAmount); // cust_id
                dbUtils.bindString(index(custidkey), order.custidkey == null ? "" : order.custidkey);
                dbUtils.bindString(index(isOnHold), TextUtils.isEmpty(order.isOnHold) ? "0" : order.isOnHold);
                dbUtils.bindString(index(ord_HoldName), order.ord_HoldName == null ? "" : order.ord_HoldName);
                dbUtils.bindString(index(is_stored_fwd), TextUtils.isEmpty(order.is_stored_fwd) ? "0" : order.is_stored_fwd);
                dbUtils.bindString(index(assignedTable), order.assignedTable == null ? "" : order.assignedTable);
                dbUtils.bindString(index(associateID), order.associateID == null ? "" : order.associateID);
                dbUtils.bindLong(index(numberOfSeats), order.numberOfSeats);
                if (order.orderAttributes != null) {
                    Gson gson = JsonUtils.getInstance();
                    String json = gson.toJson(order.orderAttributes);
                    dbUtils.bindString(index(orderAttributes), json);
                }
                if (TextUtils.isEmpty(order.ord_timeStarted)) {
                    order.ord_timeStarted = DateUtils.getDateAsString(new Date(), DateUtils.DATE_PATTERN);
                }
                dbUtils.bindString(index(ord_timeStarted), order.ord_timeStarted);
                dbUtils.bindString(index(isVoid), TextUtils.isEmpty(order.isVoid) ? "0" : order.isVoid);
                dbUtils.bindString(index(VAT), TextUtils.isEmpty(order.VAT) ? "0" : order.VAT);

                sqlinsert.execute();
                sqlinsert.clearBindings();
                sqlinsert.close();
                DinningTableOrderDAO.createDinningTableOrder(order);
                if (GenerateNewID.isValidLastId(order.ord_id, GenerateNewID.IdType.ORDER_ID)) {
                    AssignEmployeeDAO.updateLastOrderId(order.ord_id);
                }
            }
            DBManager.getDatabase().setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sqlinsert != null) {
                sqlinsert.close();
            }
            if (DBManager.getDatabase() != null && DBManager.getDatabase().inTransaction()) {
                DBManager.getDatabase().endTransaction();
            }
        }
    }

    public int deleteOrder(String ord_id) {
        int delete = DBManager.getDatabase().delete(table_name, "ord_id = ?", new String[]{ord_id});
        Log.d("Delete order:", ord_id);
        DinningTableOrderDAO.deleteByOrderId(ord_id);
        return delete;
    }

    public void emptyTableOnHold() {
        AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();
        DBManager.getDatabase().delete("OrderProduct",
                "OrderProduct.ord_id IN (SELECT op.ord_id FROM OrderProduct op LEFT JOIN Orders o ON op.ord_id=o.ord_id WHERE o.isOnHold = '1' AND o.emp_id != ?)",
                new String[]{String.valueOf(assignEmployee.getEmpId())});
        DBManager.getDatabase().delete(table_name, "isOnHold = '1' AND emp_id != ?", new String[]{String.valueOf(assignEmployee.getEmpId())});
        DinningTableOrderDAO.truncate();
    }

    private boolean checkIfExist(String ordID) {
        Cursor c = null;
        try {
            String sb = "SELECT 1 FROM " + table_name + " WHERE ord_id = '" +
                    ordID + "'";
            c = DBManager.getDatabase().rawQuery(sb, null);
            boolean exists = (c.getCount() > 0);
            c.close();
            return exists;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public Order getOrder(String orderId) {
        Cursor cursor = DBManager.getDatabase().query(table_name, attr, "ord_id=?",
                new String[]{orderId}, null, null, null);
        try {


            Order order = new Order(this.activity);
            if (cursor.moveToFirst()) {
                order = getOrder(cursor, activity);
            }
            return order;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public List<Recoveries_Holder> getRecoveriesOrders() {
        Cursor cursor = null;
        List<Recoveries_Holder> recoveries = new ArrayList<>();
        List<Order> orders;

        try {
            String sb = "SELECT * FROM " + table_name + " o " +
                    "WHERE o.ord_id NOT IN (SELECT job_id FROM Payments GROUP BY job_id) " +
                    "AND isVoid = '0' " +
                    "AND isOnHold = '0' " +
                    "AND ord_type = '5'";
            cursor = DBManager.getDatabase().rawQuery(sb, null);

            orders = getOrders(cursor);

            for (Order order : orders) {
                Recoveries_Holder recovery = new Recoveries_Holder();
                recovery.setRec_id(order.ord_id);
                recovery.setRec_name(String.format("%s: %s ", order.ord_id, Global.formatToDisplayDate(order.ord_timecreated, 3)));
                recoveries.add(recovery);
            }
            return recoveries;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public boolean existsOrder(String orderId) {
        Cursor cursor = null;
        try {
            String sb = "SELECT count(*) as count FROM " + table_name + " WHERE ord_id = '" +
                    orderId + "'";
            cursor = DBManager.getDatabase().rawQuery(sb, null);
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(cursor.getColumnIndex("count"));
            }
            cursor.close();
            return count > 0;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    private List<Order> getOrders(Cursor cursor) {
        List<Order> orders = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                orders.add(getOrder(cursor, activity));
            } while (cursor.moveToNext());
        }
        return orders;
    }

    public List<Order> getUnsyncOrders() // Will populate all unsynchronized orders
    // for XML post
    {
        Cursor cursor = null;
        try {
            StringBuilder sb = new StringBuilder();
            if (Global.isForceUpload)
                sb.append("SELECT * FROM ").append(table_name).append(" WHERE ord_issync = '0' LIMIT 5");
            else
                sb.append("SELECT ").append(sb1.toString()).append(" FROM ").append(table_name)
                        .append(" WHERE ord_issync = '0' AND isOnHold != '1' AND processed != '0' AND is_stored_fwd = '0' LIMIT 5");

            cursor = DBManager.getDatabase().rawQuery(sb.toString(), null);
            List<Order> orders = getOrders(cursor);
            cursor.close();
            return orders;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public Cursor getTupyxOrders() {
        return DBManager.getDatabase().rawQuery("SELECT * FROM Orders o LEFT OUTER JOIN Payments p ON o.ord_id = p.job_id LEFT OUTER JOIN Customers c ON o.cust_id = c.cust_id WHERE p.paymethod_id = 'Wallet' AND ord_issync = '0'", null);
    }

    public long getNumUnsyncTupyxOrders() {
        SQLiteStatement stmt = null;
        try {
            stmt = DBManager.getDatabase().compileStatement("SELECT Count(*) FROM " + table_name + " o LEFT OUTER JOIN Payments p ON o.ord_id = p.job_id WHERE p.paymethod_id = 'Wallet' AND o.ord_issync = '0'");
            long count = stmt.simpleQueryForLong();
            stmt.close();
            return count;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public List<Order> getUnsyncOrdersOnHold() {
        Cursor cursor = null;
        try {
            cursor = DBManager.getDatabase().rawQuery("SELECT * FROM " + table_name + " WHERE ord_issync = '0' AND isOnHold = '1'  LIMIT 5", null);
            List<Order> orders = getOrders(cursor);
            cursor.close();
            return orders;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public long getNumUnsyncOrdersOnHold() {
        SQLiteStatement stmt = null;
        try {
            stmt = DBManager.getDatabase().compileStatement("SELECT Count(*) FROM " + table_name + " WHERE ord_issync = '0' AND isOnHold = '1'");
            long count = stmt.simpleQueryForLong();
            stmt.close();
            return count;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public Cursor getOrdersOnHoldCursor() {
        Cursor c = DBManager.getDatabase()
                .rawQuery("SELECT ord_id as '_id',* FROM Orders WHERE isOnHold = '1' ORDER BY ord_id ASC", null);
        c.moveToFirst();
        return c;
    }

    public Cursor getOrdersOnHoldSyncCursor() {
        Cursor c = DBManager.getDatabase()
                .rawQuery("SELECT ord_id as '_id',* FROM Orders WHERE isOnHold = '1' AND ord_issync = '1' ORDER BY ord_id ASC", null);
        c.moveToFirst();
        return c;
    }

    public List<Order> getOrdersOnHold() {
        Cursor c = null;
        try {
            c = getOrdersOnHoldSyncCursor();
            List<Order> orders = getOrders(c);
            c.close();
            return orders;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public long getNumUnsyncOrders() {
        SQLiteStatement stmt = null;
        try {
            stmt = DBManager.getDatabase().compileStatement("SELECT Count(*) FROM " + table_name + " WHERE ord_issync = '0' AND isOnHold != '1'");
            long count = stmt.simpleQueryForLong();
            stmt.close();
            return count;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public String getLastOrderId(int deviceId, int year) {
        Cursor cursor = null;
        SQLiteStatement stmt = null;
        try {
            AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();
            String lastOrdID = assignEmployee.getMSLastOrderID();
            boolean getIdFromDB = false;
            StringBuilder sb = new StringBuilder();
            if (TextUtils.isEmpty(lastOrdID) || lastOrdID.length() <= 4) {
                getIdFromDB = true;
            } else {
                String[] tokens = assignEmployee.getMSLastOrderID().split("-");
                if (!tokens[2].equalsIgnoreCase(String.valueOf(year))) {
                    getIdFromDB = true;
                }
            }

            if (getIdFromDB) {
                sb.append("select max(ord_id) from ").append(table_name).append(" WHERE ord_id like '").append(assignEmployee.getEmpId())
                        .append("-%-").append(year).append("'");

                stmt = DBManager.getDatabase().compileStatement(sb.toString());
                cursor = DBManager.getDatabase().rawQuery(sb.toString(), null);
                cursor.moveToFirst();
                lastOrdID = cursor.getString(0);
                cursor.close();
                stmt.close();
                if (TextUtils.isEmpty(lastOrdID)) {
                    lastOrdID = assignEmployee.getEmpId() + "-" + "00001" + "-" + year;
                }
                AssignEmployeeDAO.updateLastOrderId(lastOrdID);
//            myPref.setLastOrdID(lastOrdID);
            }

            return lastOrdID;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }

    }

    public long getNumUnsyncProcessedOrders() {
        SQLiteStatement stmt = null;
        try {
            stmt = DBManager.getDatabase().compileStatement("SELECT Count(*) FROM " + table_name + " WHERE ord_issync = '0' AND processed != '0'");
            long count = stmt.simpleQueryForLong();
            stmt.close();
            return count;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public long getNumUnsyncOrdersStoredFwd() {
        SQLiteStatement stmt = null;
        try {
            stmt = DBManager.getDatabase().compileStatement("SELECT Count(*) FROM " + table_name + " WHERE is_stored_fwd = '1'");
            long count = stmt.simpleQueryForLong();
            stmt.close();
            return count;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    public boolean unsyncOrdersLeft() {
        SQLiteStatement stmt = null;
        try {
            stmt = DBManager.getDatabase().compileStatement("SELECT Count(*) FROM " + table_name + " WHERE ord_issync = '0' AND processed != '0'");
            long count = stmt.simpleQueryForLong();
            stmt.close();
            return count != 0;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private String getOrderTypesAsSQLArray(Global.OrderType[] orderTypes) {
        StringBuilder sb = new StringBuilder();
        for (Global.OrderType orderType : orderTypes) {
            sb.append("'").append(orderType.getCode()).append("',");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public Cursor getReceipts1Data(Global.OrderType[] orderTypes) // Transactions Receipts first
    // listview
    {
        String subquery1 = "SELECT ord_id as _id,ord_total,ord_issync,cust_id,isVoid,ord_type" +
                " FROM Orders WHERE ord_type IN (";
        String subquery2 = ") AND isOnHold = '0' ORDER BY rowid DESC";
        Cursor cursor = DBManager.getDatabase().rawQuery(subquery1 + getOrderTypesAsSQLArray(orderTypes) + subquery2, null);
        cursor.moveToFirst();
        return cursor;
    }

    public Cursor getReceipts1CustData(Global.OrderType[] orderTypes, String custID) {

        String subquery1 = "SELECT ord_id as _id,ord_total,ord_issync,cust_id,isVoid,ord_type FROM Orders WHERE ord_type IN (";
        String subquery2 = ") AND cust_id = ?";
        String subquery3 = " AND isOnHold = '0' ORDER BY rowid DESC";
        Cursor cursor = DBManager.getDatabase().rawQuery(subquery1 + getOrderTypesAsSQLArray(orderTypes) + subquery2 + subquery3, new String[]{custID});
        cursor.moveToFirst();
        return cursor;
    }

    public Cursor getSearchOrder(Global.OrderType[] orderTypes, String search, String customerID) // Transactions
    // Receipts
    // first
    // listview
    {
        String subqueries[] = new String[4];
        StringBuilder sb = new StringBuilder();
        String[] params;
        if (customerID == null) {
            subqueries[0] = "SELECT Orders.ord_id as _id,Orders.ord_total,Orders.ord_issync,Customers.cust_id,Orders.isVoid,Orders.ord_type FROM Orders JOIN Customers WHERE Orders.ord_type IN(";
            subqueries[1] = ") AND Orders.cust_id = Customers.cust_id AND Orders.ord_id LIKE ? ORDER BY Orders.rowid DESC";
            sb.append(subqueries[0]).append(getOrderTypesAsSQLArray(orderTypes)).append(subqueries[1]);// .append(search).append(subqueries[2]);
            params = new String[]{"%" + search + "%"};
        } else {
            subqueries[0] = "SELECT ord_id as _id,ord_total,ord_issync,cust_id,isVoid,ord_type FROM Orders WHERE ord_type IN(";
            subqueries[1] = ") AND cust_id = ?";
            subqueries[2] = " AND Orders.ord_id LIKE ? ORDER BY Orders.rowid DESC";

            sb.append(subqueries[0]).append(getOrderTypesAsSQLArray(orderTypes)).append(subqueries[1]).append(subqueries[2]);// .append(search).append(subqueries[3]);
            params = new String[]{customerID, "%" + search + "%"};
        }

        Cursor cursor = DBManager.getDatabase().rawQuery(sb.toString(), params);
        cursor.moveToFirst();
        return cursor;
    }

    public void updateIsSync(List<String[]> list) {
        StringBuilder sb = new StringBuilder();
        sb.append(ord_id).append(" = ?");

        ContentValues args = new ContentValues();

        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (list.get(i)[0].equals("0"))
                args.put(ord_issync, "1");
            else
                args.put(ord_issync, "0");
            DBManager.getDatabase().update(table_name, args, sb.toString(), new String[]{list.get(i)[1]});
        }
    }

    private void updateOnHoldSync(String ordID) {
        ContentValues args = new ContentValues();

        args.put(ord_issync, "1");
        DBManager.getDatabase().update(table_name, args, ord_id + " = ?", new String[]{ordID});
    }

    public String updateFinishOnHold(String ordID) {
        Cursor c = null;
        try {
            c = DBManager.getDatabase().rawQuery("SELECT ord_timecreated FROM Orders WHERE ord_id = ?",
                    new String[]{ordID});
            String dateCreated = DateUtils.getDateAsString(new Date(), DateUtils.DATE_yyyy_MM_ddTHH_mm_ss);

            if (c.moveToFirst())
                dateCreated = c.getString(c.getColumnIndex(ord_timecreated));

            DBManager.getDatabase().delete(table_name, "ord_id = ?", new String[]{ordID});
            DBManager.getDatabase().delete("OrderProduct", "ord_id = ?", new String[]{ordID});
            c.close();
            return dateCreated;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public void updateIsProcessed(String orderID, String updateValue) {
        ContentValues args = new ContentValues();
        args.put(processed, updateValue);
        args.put(isOnHold, "0");
        DBManager.getDatabase().update(table_name, args, ord_id + " = ?", new String[]{orderID});
        DinningTableOrderDAO.deleteByNumber(getOrderAssignedTable(orderID));
    }

    public void updateOrderTypeToInvoice(String orderID) {
        ContentValues args = new ContentValues();
        args.put(ord_type, Global.OrderType.INVOICE.getCodeString());
        DBManager.getDatabase().update(table_name, args, ord_id + " = ?", new String[]{orderID});
    }

    public void updateOrderComment(String orderID, String value) {
        ContentValues args = new ContentValues();
        args.put(ord_comment, value);
        DBManager.getDatabase().update(table_name, args, ord_id + " = ?", new String[]{orderID});
    }

    public void updateOrderStoredFwd(String _order_id, String value) {
        ContentValues args = new ContentValues();
        args.put(is_stored_fwd, value);
        DBManager.getDatabase().update(table_name, args, ord_id + " = ?", new String[]{_order_id});
    }

    public void updateIsTotalLinesPay(String orderID, String updateValue) {
        ContentValues args = new ContentValues();
        args.put(total_lines_pay, updateValue);
        DBManager.getDatabase().update(table_name, args, ord_id + " = ?", new String[]{orderID});
    }

    public void updateIsVoid(String param) {
        ContentValues args = new ContentValues();
        args.put(isVoid, "1");
        args.put(ord_issync, "0");
        args.put(processed, "9");
        DBManager.getDatabase().update(table_name, args, ord_id + " = ?", new String[]{param});
    }

    public boolean isOrderPaid(String orderID) {
        BigDecimal orderTotal = Global.getBigDecimalNum(getColumnValue("ord_total", orderID));
        PaymentsHandler paymentsHandler = new PaymentsHandler(activity);
        BigDecimal totalPaid = Global.getBigDecimalNum(paymentsHandler.getTotalPaidAmount(orderID));
        return orderTotal.equals(totalPaid);
    }

    public String getColumnValue(String key, String _ord_id) {
        Cursor c = DBManager.getDatabase().rawQuery("SELECT " + key + " FROM " + table_name + " WHERE ord_id = ?", new String[]{_ord_id});
        String value = "";
        if (c.moveToFirst()) {
            value = c.getString(c.getColumnIndex(key));
        }

        return value;
    }

    public boolean isOrderOffline(String ordID) {
        Cursor c = null;
        try {
            c = DBManager.getDatabase().rawQuery("SELECT ord_issync FROM Orders WHERE ord_id = ?", new String[]{ordID});

            boolean offline = false;
            if (c.moveToFirst()) {
                offline = c.getString(c.getColumnIndex("ord_issync")).equals("0");
            }
            c.close();
            return offline;
        } finally {
            c.close();
        }
    }

    public Order getPrintedOrder(String ordID) {
        Cursor cursor = null;
        try {
            Order anOrder = new Order(activity);
            OrderProductsHandler productsHandler = new OrderProductsHandler(activity);
            String sb = ("SELECT " +
                    "o.ord_id, " +
                    "o.ord_timecreated, " +
                    "o.ord_total, " +
                    "o.ord_subtotal, " +
                    "o.ord_discount_id, " +
                    "o.ord_discount, " +
                    "o.ord_taxamount, " +
                    "c.cust_name, " +
                    "c.AccountNumnber, " +
                    "o.cust_id, " +
                    "o.orderAttributes, " +
                    "o.ord_total AS 'gran_total', " +
                    "tipAmount, " +
                    "ord_signature, " +
                    "o.ord_HoldName, " +
                    "o.clerk_id, " +
                    "o.ord_comment, " +
                    "o.isVoid, " +
                    "o.ord_timeStarted " +
                    "FROM Orders o LEFT OUTER JOIN Customers c ON o.cust_id = c.cust_id " +
                    "WHERE o.ord_id = '") + ordID + "'";

            cursor = DBManager.getDatabase().rawQuery(sb, null);
            if (cursor.moveToFirst()) {
                do {
                    anOrder.ord_id = getValue(cursor.getString(cursor.getColumnIndex(ord_id)));
                    anOrder.ord_timecreated = cursor.getString(cursor.getColumnIndex(ord_timecreated));
                    anOrder.ord_total = getValue(cursor.getString(cursor.getColumnIndex(ord_total)));
                    anOrder.ord_subtotal = getValue(cursor.getString(cursor.getColumnIndex(ord_subtotal)));
                    anOrder.ord_discount_id = getValue(cursor.getString(cursor.getColumnIndex(ord_discount_id)));
                    anOrder.ord_discount = getValue(cursor.getString(cursor.getColumnIndex(ord_discount)));
                    anOrder.ord_taxamount = getValue(cursor.getString(cursor.getColumnIndex(ord_taxamount)));
                    anOrder.cust_name = getValue(cursor.getString(cursor.getColumnIndex("cust_name")));
                    anOrder.gran_total = getValue(cursor.getString(cursor.getColumnIndex("gran_total")));
                    anOrder.tipAmount = getValue(cursor.getString(cursor.getColumnIndex(tipAmount)));
                    anOrder.ord_signature = getValue(cursor.getString(cursor.getColumnIndex(ord_signature)));
                    anOrder.ord_HoldName = getValue(cursor.getString(cursor.getColumnIndex(ord_HoldName)));
                    anOrder.clerk_id = getValue(cursor.getString(cursor.getColumnIndex(clerk_id)));
                    anOrder.ord_comment = getValue(cursor.getString(cursor.getColumnIndex(ord_comment)));
                    anOrder.cust_id = getValue(cursor.getString(cursor.getColumnIndex(cust_id)));
                    anOrder.isVoid = getValue(cursor.getString(cursor.getColumnIndex(isVoid)));
                    anOrder.ord_timeStarted = getValue(cursor.getString(cursor.getColumnIndex(ord_timeStarted)));
                    List<OrderProduct> orderProducts = productsHandler.getOrderProducts(ordID);
                    anOrder.setOrderProducts(orderProducts);
                    String json = getValue(cursor.getString(cursor.getColumnIndex(orderAttributes)));
                    Gson gson = JsonUtils.getInstance();
                    Type listType = new com.google.gson.reflect.TypeToken<List<OrderAttributes>>() {
                    }.getType();
                    if (!TextUtils.isEmpty(json)) {
                        anOrder.orderAttributes = gson.fromJson(json, listType);
                    } else {
                        anOrder.orderAttributes = new ArrayList<>();
                    }
                    taxes_db = getOrderTaxes_DB();
                    List<DataTaxes> orderTaxes = taxes_db.getOrderTaxes(anOrder.ord_id);
                    anOrder.setListOrderTaxes(orderTaxes);

                } while (cursor.moveToNext());
            }
            cursor.close();
            return anOrder;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    private String getValue(String value) {
        if (value == null)
            value = "";
        return value;
    }

    public List<Order> getOrderDayReport(String clerk_id, String date, boolean onlyOnholds) {
        Cursor c = null;
        try {
            List<Order> listOrder = new ArrayList<>();
            String isOnHolds = onlyOnholds ? "1" : "0";
            StringBuilder query = new StringBuilder();
            query.append(
                    "SELECT ord_type,sum(ord_subtotal) as 'ord_subtotal',sum(ord_discount) as 'ord_discount', sum(ord_taxamount) as 'ord_taxamount' ,  ");
            query.append("sum(ord_total) as 'ord_total',date(ord_timecreated,'localtime') as 'date' FROM Orders ");

            String[] where_values = null;
            if (clerk_id != null && !clerk_id.isEmpty()) {
                query.append("WHERE clerk_id = ? AND isVoid = '0' AND isOnHold = ? ");
                where_values = new String[]{clerk_id, isOnHolds};

                if (date != null && !date.isEmpty()) {
                    query.append(" AND date = ? ");
                    where_values = new String[]{clerk_id, isOnHolds, date};
                }
            } else if (date != null && !date.isEmpty()) {
                query.append(" WHERE  date = ? AND isVoid = '0' AND isOnHold = ? ");
                where_values = new String[]{date, isOnHolds};
            } else {
                where_values = new String[]{isOnHolds};
                query.append(" WHERE  isVoid = '0' AND isOnHold = ? ");
            }
            query.append(" GROUP BY ord_type");

            c = DBManager.getDatabase().rawQuery(query.toString(), where_values);
            if (c.moveToFirst()) {
                int i_ord_type = c.getColumnIndex(ord_type);
                int i_ord_subtotal = c.getColumnIndex(ord_subtotal);
                int i_ord_discount = c.getColumnIndex(ord_discount);
                int i_ord_taxamount = c.getColumnIndex(ord_taxamount);
                int i_ord_total = c.getColumnIndex(ord_total);
                do {
                    Order ord = new Order(activity);
                    ord.ord_type = c.getString(i_ord_type);
                    ord.ord_subtotal = c.getString(i_ord_subtotal);
                    ord.ord_discount = c.getString(i_ord_discount);
                    ord.ord_taxamount = c.getString(i_ord_taxamount);
                    ord.ord_total = c.getString(i_ord_total);

                    listOrder.add(ord);
                } while (c.moveToNext());
            }

            c.close();
            return listOrder;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public List<Order> getOrderShiftReport(String clerk_id, String startDate, String endDate) {
        Cursor c = null;
        try {
            List<Order> listOrder = new ArrayList<>();
            StringBuilder query = new StringBuilder();
            query.append("SELECT ");
            query.append("round(sum(ord_subtotal),2) as 'ord_subtotal', ");
            query.append("ord_type, ");
            query.append("sum(ord_discount) as 'ord_discount', ");
            query.append("sum(ord_taxamount) as 'ord_taxamount', ");
            query.append("sum(ord_total) as 'ord_total', ");
            query.append("ord_timecreated ");
            query.append("FROM Orders ");
            query.append("WHERE isVoid = '0' AND processed != '10' ");

            ArrayList<String> where_values = new ArrayList<>();
            if (clerk_id != null && !clerk_id.isEmpty()) {
                query.append(" AND clerk_id = ? ");
                where_values.add(clerk_id);
            }

            boolean hasStartDate = (startDate != null && !startDate.isEmpty())?true:false;
            boolean hasEndDate = (endDate != null && !endDate.isEmpty())?true:false;

            if (hasStartDate && hasEndDate) {
                query.append(" AND datetime(ord_timecreated,'utc') BETWEEN datetime(?,'utc')  ");
                query.append(" and datetime(?,'utc')");
                where_values.add(startDate);
                where_values.add(endDate + ":59");
            }else if(hasStartDate || hasEndDate) {
                if (hasStartDate) {
                    query.append(" AND datetime(ord_timecreated,'utc') >= datetime(?, 'utc')");
                    where_values.add(startDate);
                } else {
                    query.append(" AND datetime(ord_timecreated,'utc') <= datetime(?, 'utc') ");
                    where_values.add(endDate + ":59");
                }
            }

            query.append(" GROUP BY ord_type");

            c = DBManager.getDatabase().rawQuery(query.toString(), where_values.toArray(new String[0]));
            if (c.moveToFirst()) {
                int i_ord_type = c.getColumnIndex(ord_type);
                int i_ord_subtotal = c.getColumnIndex(ord_subtotal);
                int i_ord_discount = c.getColumnIndex(ord_discount);
                int i_ord_taxamount = c.getColumnIndex(ord_taxamount);
                int i_ord_total = c.getColumnIndex(ord_total);
                do {
                    Order ord = new Order(activity);
                    ord.ord_type = c.getString(i_ord_type);
                    ord.ord_subtotal = c.getString(i_ord_subtotal);
                    ord.ord_discount = c.getString(i_ord_discount);
                    ord.ord_taxamount = c.getString(i_ord_taxamount);
                    ord.ord_total = c.getString(i_ord_total);

                    listOrder.add(ord);
                } while (c.moveToNext());
            }

            c.close();
            return listOrder;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public HashMap<String, List<DataTaxes>> getOrderShiftReportTaxesBreakdown(
            String clerk_id, String startDate, String endDate) {
        Cursor c = null;
        try {
            HashMap<String, List<DataTaxes>> taxesBreakdownHashMap = new HashMap<>();
            List<DataTaxes> dataTaxesList = new ArrayList<>();
            DataTaxes dataTax;
            StringBuilder query = new StringBuilder();

            query.append("SELECT ");
            query.append("ord_type, ");
            query.append("tax_id, ");
            query.append("tax_name, ");
            query.append("sum(tax_amount) AS 'tax_amount' , ");
            query.append("ord_timecreated ");
            query.append("FROM Orders ");
            query.append("INNER JOIN OrderTaxes ON Orders.ord_id = OrderTaxes.ord_id ");
            query.append("WHERE isVoid = '0' ");

            ArrayList<String> where_values = new ArrayList<>();
            if (clerk_id != null && !clerk_id.isEmpty()) {
                query.append("AND clerk_id = ? ");
                where_values.add(clerk_id);
            }

            boolean hasStartDate = (startDate != null && !startDate.isEmpty())?true:false;
            boolean hasEndDate = (endDate != null && !endDate.isEmpty())?true:false;

            if (hasStartDate && hasEndDate) {
                query.append(" AND datetime(ord_timecreated,'utc') BETWEEN datetime(?,'utc')  ");
                query.append(" and datetime(?,'utc')");
                where_values.add(startDate);
                where_values.add(endDate + ":59");
            }else if(hasStartDate || hasEndDate) {
                if (hasStartDate) {
                    query.append(" AND datetime(ord_timecreated,'utc') >= datetime(?, 'utc')");
                    where_values.add(startDate);
                } else {
                    query.append(" AND datetime(ord_timecreated,'utc') <= datetime(?, 'utc') ");
                    where_values.add(endDate + ":59");
                }
            }

            query.append(
                    "AND Orders.ord_id IN (SELECT job_id FROM Payments GROUP BY job_id) " +
                    "GROUP BY ord_type, tax_name");

            c = DBManager.getDatabase().rawQuery(query.toString(), where_values.toArray(new String[0]));
            if (c.moveToFirst()) {
                String orderTypeBreaker = "-1";
                String orderType;
                int i_ord_type = c.getColumnIndex(ord_type);
                int i_tax_name = c.getColumnIndex(tax_name);
                int i_tax_amount = c.getColumnIndex(tax_amount);
                do {
                    orderType = c.getString(i_ord_type);
                    dataTax = new DataTaxes();
                    dataTax.setTax_name(c.getString(i_tax_name));
                    dataTax.setTax_amount(c.getString(i_tax_amount));

                    if (!orderType.equalsIgnoreCase(orderTypeBreaker)) {
                        taxesBreakdownHashMap.put(orderTypeBreaker, dataTaxesList);
                        dataTaxesList = new ArrayList<>();
                        orderTypeBreaker = orderType;
                    }

                    dataTaxesList.add(dataTax);

                } while (c.moveToNext());
                taxesBreakdownHashMap.put(orderTypeBreaker, dataTaxesList);
            }

            c.close();
            return taxesBreakdownHashMap;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public HashMap<String, List<DataTaxes>> getOrderDayReportTaxesBreakdown(
            String clerk_id, String date) {
        Cursor c = null;
        try {
            HashMap<String, List<DataTaxes>> taxesBreakdownHashMap = new HashMap<>();
            List<DataTaxes> dataTaxesList = new ArrayList<>();
            DataTaxes dataTax;
            StringBuilder query = new StringBuilder();

            query.append(
                    "SELECT " +
                            "ord_type, " +
                            "tax_id, " +
                            "tax_name, " +
                            "sum(tax_amount) AS 'tax_amount' , " +
                            "date(ord_timecreated,'localtime') as 'date' " +
                            "FROM Orders " +
                            "INNER JOIN OrderTaxes ON Orders.ord_id = OrderTaxes.ord_id ");

            String[] where_values = null;
            if (clerk_id != null && !clerk_id.isEmpty()) {
                query.append("WHERE " +
                        "clerk_id = ? " +
                        "AND isVoid = '0' ");
                where_values = new String[]{clerk_id};

                if (date != null && !date.isEmpty()) {
                    query.append(" AND date = ? ");
                    where_values = new String[]{clerk_id, date};
                }
            } else if (date != null && !date.isEmpty()) {
                query.append(" WHERE " +
                        "date = ? " +
                        "AND isVoid = '0' ");
                where_values = new String[]{date};
            } else {
                query.append(" WHERE " +
                        "isVoid = '0' ");
            }

            query.append(
                    "AND Orders.ord_id IN (SELECT job_id FROM Payments GROUP BY job_id) " +
                    "GROUP BY ord_type, tax_name");

            c = DBManager.getDatabase().rawQuery(query.toString(), where_values);
            if (c.moveToFirst()) {
                String orderTypeBreaker = "-1";
                String orderType;
                int i_ord_type = c.getColumnIndex(ord_type);
                int i_tax_name = c.getColumnIndex(tax_name);
                int i_tax_amount = c.getColumnIndex(tax_amount);
                do {
                    orderType = c.getString(i_ord_type);
                    dataTax = new DataTaxes();
                    dataTax.setTax_name(c.getString(i_tax_name));
                    dataTax.setTax_amount(c.getString(i_tax_amount));

                    if (!orderType.equalsIgnoreCase(orderTypeBreaker)) {
                        taxesBreakdownHashMap.put(orderTypeBreaker, dataTaxesList);
                        dataTaxesList = new ArrayList<>();
                        orderTypeBreaker = orderType;
                    }

                    dataTaxesList.add(dataTax);

                } while (c.moveToNext());
                taxesBreakdownHashMap.put(orderTypeBreaker, dataTaxesList);
            }

            c.close();
            return taxesBreakdownHashMap;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public List<Order> getARTransactionsDayReport(String clerk_id, String date) {
        Cursor c = null;
        try {
            List<Order> listOrder = new ArrayList<>();

            StringBuilder query = new StringBuilder();
            query.append(
                    "SELECT o.ord_id, c.cust_name , sum(o.ord_total) as 'ord_total'," +
                            "date(o.ord_timecreated,'localtime') as 'date' FROM Orders o LEFT JOIN Customers c ");
            query.append("ON o.cust_id = c.cust_id WHERE o.ord_type = '2' ");

            String[] where_values = null;
            if (clerk_id != null && !clerk_id.isEmpty()) {
                query.append("AND clerk_id = ? ");
                where_values = new String[]{clerk_id};

                if (date != null && !date.isEmpty()) {
                    query.append(" AND date = ? ");
                    where_values = new String[]{clerk_id, date};
                }
            } else if (date != null && !date.isEmpty()) {
                query.append(" AND date = ? ");
                where_values = new String[]{date};
            }

            c = DBManager.getDatabase().rawQuery(query.toString(), where_values);
            if (c.moveToFirst()) {
                int i_ord_id = c.getColumnIndex(ord_id);
                int i_cust_name = c.getColumnIndex("cust_name");
                int i_ord_total = c.getColumnIndex(ord_total);
                int i_ord_timecreated = c.getColumnIndex("date");

                do {
                    if (c.getString(i_ord_id) != null) {
                        Order ord = new Order(activity);
                        ord.ord_id = c.getString(i_ord_id);
                        ord.cust_name = c.getString(i_cust_name);
                        ord.ord_total = c.getString(i_ord_total);
                        ord.ord_timecreated = c.getString(i_ord_timecreated);
                        listOrder.add(ord);
                    }
                } while (c.moveToNext());
            }

            c.close();
            return listOrder;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public void insert(Order order) {
        insert(Arrays.asList(order));
    }

    public int deleteOnHoldsOrders(List<Order> ordersToDelete) {
        int delete = 0;
        for (Order order : ordersToDelete) {
            delete += deleteOrder(order.ord_id);
        }
//        int delete = DBManager.getDatabase().delete(table_name, "isOnHold = ?", new String[]{"1"});
        return delete;
    }

    public void updateBixolonTransactionId(Order order) {
        ContentValues args = new ContentValues();
        args.put(bixolonTransactionId, order.getBixolonTransactionId());
        DBManager.getDatabase().update(table_name, args, ord_id + " = ?", new String[]{order.ord_id});
    }
}