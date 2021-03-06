package com.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.android.dao.AssignEmployeeDAO;
import com.android.emobilepos.models.Discount;
import com.android.emobilepos.models.Orders;
import com.android.emobilepos.models.Product;
import com.android.emobilepos.models.Tax;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.orders.SalesByClerk;
import com.android.emobilepos.models.realms.AssignEmployee;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.google.gson.Gson;

import net.sqlcipher.database.SQLiteStatement;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import util.StringUtil;
import util.json.JsonUtils;

public class OrderProductsHandler {

    public static final String table_name = "OrderProduct";
    private static final String ord_id = "ord_id";
    private static final String addon = "addon";
    private static final String isAdded = "isAdded";
    private static final String isPrinted = "isPrinted";
    private static final String item_void = "item_void";
    private static final String ordprod_id = "ordprod_id";
    private static final String addon_ordprod_id = "addon_ordprod_id";
    private static final String prod_id = "prod_id";
    private static final String prod_sku = "prod_sku";
    private static final String prod_upc = "prod_upc";
    private static final String ordprod_qty = "ordprod_qty";
    private static final String overwrite_price = "overwrite_price";
    private static final String reason_id = "reason_id";
    private static final String ordprod_name = "ordprod_name";// add
    private static final String ordprod_comment = "ordprod_comment";
    private static final String ordprod_desc = "ordprod_desc";
    private static final String pricelevel_id = "pricelevel_id";
    private static final String prod_seq = "prod_seq";
    private static final String uom_name = "uom_name";
    private static final String uom_conversion = "uom_conversion";
    private static final String uom_id = "uom_id";
    private static final String prod_taxId = "prod_taxId"; // add
    private static final String prod_taxValue = "prod_taxValue"; // add
    private static final String discount_id = "discount_id";
    private static final String discount_value = "discount_value";
    private static final String cat_id = "cat_id";
    private static final String cat_name = "cat_name";
    private static final String prod_istaxable = "prod_istaxable";
    private static final String discount_is_taxable = "discount_is_taxable";
    private static final String discount_is_fixed = "discount_is_fixed";
    private static final String onHand = "onHand";
    private static final String imgURL = "imgURL";
    private static final String prod_price = "prod_price";
    private static final String prod_type = "prod_type";
    private static final String itemTotal = "itemTotal";
    private static final String itemSubtotal = "itemSubtotal";
    private static final String addon_section_name = "addon_section_name";
    private static final String addon_position = "addon_position";
    private static final String hasAddons = "hasAddons";
    private static final String assignedSeat = "assignedSeat";
    private static final String seatGroupId = "seatGroupId";
    private static final String prodPricePoints = "prod_price_points";
    private static final String isGC = "isGC";
    private static final String product_taxes_json = "product_taxes_json";
    public static final List<String> attr = Arrays.asList(addon, isAdded, isPrinted, item_void, ordprod_id,
            ord_id, prod_id, prod_sku, prod_upc, ordprod_qty, overwrite_price, reason_id, ordprod_name, ordprod_comment, ordprod_desc,
            pricelevel_id, prod_seq, uom_name, uom_conversion, uom_id, prod_taxId, prod_taxValue, discount_id,
            discount_value, prod_istaxable, discount_is_taxable, discount_is_fixed, onHand, imgURL, prod_price,
            prod_type, itemTotal, itemSubtotal, addon_section_name, addon_position, hasAddons, cat_id, cat_name, assignedSeat,
            seatGroupId, addon_ordprod_id, prodPricePoints, isGC, product_taxes_json);
    public final String empStr = "";
    public HashMap<String, Integer> attrHash;
    SQLiteStatement sqlinsert;
    Type taxListType = new com.google.gson.reflect.TypeToken<List<Tax>>() {
    }.getType();
    Gson gson = JsonUtils.getInstance();
    private AssignEmployee assignEmployee;
    private StringBuilder sb1, sb2, sb3;
    //    public Global global;
    private List<String[]> data;
    private List<HashMap<String, Integer>> dictionaryListMap;
    private MyPreferences myPref;
    private ProductsHandler productsHandler;

    public OrderProductsHandler(Context activity) {
//        global = (Global) activity.getApplication();
        attrHash = new HashMap<>();
        sb1 = new StringBuilder();
        sb2 = new StringBuilder();
        sb3 = new StringBuilder();
        new DBManager(activity);
        myPref = new MyPreferences(activity);
        productsHandler = new ProductsHandler(activity);
        initDictionary();
    }

    public void initDictionary() {
        int size = attr.size();
        for (int i = 0; i < size; i++) {
            attrHash.put(attr.get(i), i + 1);
            if ((i + 1) < size) {
                sb1.append(attr.get(i)).append(",");
                sb3.append("op.").append(attr.get(i)).append(",");
                sb2.append("?").append(",");
            } else {
                sb1.append(attr.get(i));
                sb3.append("op.").append(attr.get(i));
                sb2.append("?");
            }
        }
    }

    private String getData(String tag, int record) {
        Integer i = dictionaryListMap.get(record).get(tag);
        if (i != null) {
            return data.get(record)[i];
        }
        return empStr;
    }

    private int index(String tag) {
        return attrHash.get(tag);
    }

    private void insertAddon(String ord_id, SQLiteStatement insert, List<OrderProduct> addonsProducts) {
//        global.orderProductAddons = addonsProducts;
        int size = addonsProducts.size();
        for (int i = 0; i < size; i++) {
            OrderProduct prod = addonsProducts.get(i);
            insert.bindString(index(addon), String.valueOf(prod.isAddon())); // addon
            insert.bindString(index(isAdded), String.valueOf(prod.isAdded())); // isAdded
            insert.bindString(index(isPrinted), String.valueOf(prod.isPrinted())); // isPrinted
            insert.bindString(index(item_void), TextUtils.isEmpty(prod.getItem_void()) ? "0" : prod.getItem_void()); // item_void
            insert.bindString(index(ordprod_id), prod.getOrdprod_id() == null ? "" : prod.getOrdprod_id()); // ordprod_id
            insert.bindString(index(addon_ordprod_id), prod.getAddon_ordprod_id() == null ? "" : prod.getAddon_ordprod_id());
            insert.bindString(index(OrderProductsHandler.ord_id), ord_id); // ord_id
            insert.bindString(index(prod_id), prod.getProd_id() == null ? "" : prod.getProd_id()); // prod_id
            insert.bindString(index(ordprod_qty), TextUtils.isEmpty(prod.getOrdprod_qty()) ? "0" : prod.getOrdprod_qty()); // ordprod_qty
            insert.bindString(index(overwrite_price),
                    prod.getOverwrite_price() == null ? "0" : prod.getOverwrite_price().toString()); // overwrite_price
            insert.bindString(index(reason_id), prod.getReason_id() == null ? "" : prod.getReason_id()); // reason_id
            insert.bindString(index(ordprod_name), prod.getOrdprod_name() == null ? "" : prod.getOrdprod_name()); // ordprod_name
            insert.bindString(index(ordprod_desc), prod.getOrdprod_desc() == null ? "" : prod.getOrdprod_desc());
            insert.bindString(index(ordprod_comment), prod.getOrdprod_comment() == null ? "" : prod.getOrdprod_comment());
            insert.bindString(index(pricelevel_id), prod.getPricelevel_id() == null ? "" : prod.getPricelevel_id()); // pricelevel_id
            insert.bindString(index(prod_seq), TextUtils.isEmpty(prod.getProd_seq()) ? "1" : prod.getProd_seq()); // prod_seq
            insert.bindString(index(uom_name), prod.getUom_name() == null ? "" : prod.getUom_name()); // uom_name
            insert.bindString(index(uom_conversion), prod.getUom_conversion() == null ? "" : prod.getUom_conversion()); // uom_conversion
            insert.bindString(index(uom_id), prod.getUom_id() == null ? "" : prod.getUom_id()); // uom_id
            insert.bindString(index(prod_taxId), prod.getProd_taxId() == null ? "" : prod.getProd_taxId()); // prod_taxId
            insert.bindDouble(index(prod_taxValue), prod.getProd_taxValue() == null ? 0 : prod.getProd_taxValue().doubleValue()); // prod_taxValue
            insert.bindString(index(discount_id), prod.getDiscount_id() == null ? "" : prod.getDiscount_id()); // discount_id
            insert.bindString(index(discount_value),
                    TextUtils.isEmpty(prod.getDiscount_value()) ? "0" : prod.getDiscount_value()); // discount_value
            insert.bindString(index(prod_istaxable),
                    TextUtils.isEmpty(prod.getProd_istaxable()) ? "0" : prod.getProd_istaxable()); // prod_istaxable
            insert.bindString(index(discount_is_taxable),
                    TextUtils.isEmpty(prod.getDiscount_is_taxable()) ? "0" : prod.getDiscount_is_taxable()); // discount_is_taxable
            insert.bindString(index(discount_is_fixed),
                    TextUtils.isEmpty(prod.getDiscount_is_fixed()) ? "0" : prod.getDiscount_is_fixed()); // discount_is_fixed
            insert.bindString(index(onHand), TextUtils.isEmpty(prod.getOnHand()) ? "0" : prod.getOnHand()); // onHand
            insert.bindString(index(imgURL), prod.getImgURL() == null ? "" : prod.getImgURL()); // imgURL
            insert.bindString(index(prod_price), TextUtils.isEmpty(prod.getProd_price()) ? "0" : prod.getProd_price()); // prod_price
            insert.bindString(index(prod_type), TextUtils.isEmpty(prod.getProd_type()) ? "" : prod.getProd_type()); // prod_type
            insert.bindString(index(itemTotal), TextUtils.isEmpty(prod.getItemTotal()) ? "0" : prod.getItemTotal()); // itemTotal
            insert.bindString(index(itemSubtotal), String.valueOf(prod.getItemSubtotalCalculated() == null ? "0" : prod.getItemSubtotalCalculated())); // itemSubtotal
            insert.bindString(index(hasAddons), String.valueOf(prod.getHasAddons())); // hasAddons
            insert.bindString(index(addon_section_name),
                    prod.getAddon_section_name() == null ? "" : prod.getAddon_section_name());
            insert.bindString(index(addon_position),
                    TextUtils.isEmpty(prod.getAddon_position()) ? "0" : prod.getAddon_position());
            insert.bindString(index(cat_id), prod.getCat_id() == null ? "" : prod.getCat_id());
            insert.execute();
            insert.clearBindings();
        }
    }

    public void deleteOrderProduct(String ordprod_id) {
        DBManager.getDatabase().delete(table_name, "ordprod_id = ? or addon_ordprod_id = ?", new String[]{ordprod_id, ordprod_id});
    }

    public int deleteAllOrdProd(String _ord_id) {
        return DBManager.getDatabase().delete(table_name, "ord_id = ?", new String[]{_ord_id});
    }

    public void emptyTable() {
        DBManager.getDatabase().execSQL("DELETE FROM " + table_name);
    }

    private boolean checkIfExist(String ordID) {
        Cursor c = null;
        try {
            c = DBManager.getDatabase().rawQuery("SELECT 1 FROM " + table_name + " WHERE ordprod_id = '" + ordID + "'", null);
            boolean exists = (c.getCount() > 0);
            c.close();

            return exists;
        } finally {
            if (c != null) {
                c.close();
            }
        }

    }

    public void updateIsPrinted(String ordprodID) {

        ContentValues args = new ContentValues();

        args.put(isPrinted, "true");
        DBManager.getDatabase().update(table_name, args, ordprod_id + " = ?", new String[]{ordprodID});
    }

    public Cursor getCursorData(String orderId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(sb1.toString()).append(",");
//        if (getAssignEmployee().isVAT())
        sb.append("itemTotal AS 'totalLineValue' FROM ");
//        else
//            sb.append("(itemTotal+prod_taxValue) AS 'totalLineValue' FROM ");
        sb.append(table_name).append(" WHERE ord_id = ?");

        return DBManager.getDatabase().rawQuery(sb.toString(), new String[]{orderId});
    }

    public List<OrderProduct> getOrderProductAddons(String ordprod_id) {
        Cursor cursor = null;
        try {
            List<OrderProduct> orderProducts = new ArrayList<>();
            String[] cols = new String[attr.size()];
            attr.toArray(cols);
            cursor = DBManager.getDatabase().query(table_name, cols, addon_ordprod_id + " = ?", new String[]{ordprod_id},
                    null, null, null);
            while (cursor.moveToNext()) {
                orderProducts.add(getOrderProduct(cursor));
            }
            cursor.close();
            return orderProducts;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public void insert(List<OrderProduct> orderProducts) {

        DBManager.getDatabase().beginTransaction();
        try {
            boolean isRestaurantMode = myPref.isRestaurantMode();

            String sql = "INSERT OR REPLACE INTO " + table_name + " (" + sb1.toString() + ") " + "VALUES (" + sb2.toString() + ")";
            sqlinsert = DBManager.getDatabase().compileStatement(sql);
            int size = orderProducts.size();
            if (!orderProducts.isEmpty()) {
                deleteAllOrdProd(orderProducts.get(0).getOrd_id());
            }
            for (int i = 0; i < size; i++) {
                DBUtils dbUtils = DBUtils.getInstance(sqlinsert, DBUtils.DBChild.ORDER_PRODUCT);
                OrderProduct prod = orderProducts.get(i);
                double qty = Double.parseDouble(prod.getOrdprod_qty());
                if (qty == 0) {
                    continue;
                }
                dbUtils.bindString(index(addon), String.valueOf(prod.isAddon())); // addon
                dbUtils.bindString(index(isAdded), String.valueOf(prod.isAdded())); // isAdded
                dbUtils.bindString(index(isPrinted), String.valueOf(prod.isPrinted())); // isPrinted
                dbUtils.bindString(index(item_void), TextUtils.isEmpty(prod.getItem_void()) ? "0" : prod.getItem_void()); // item_void
                dbUtils.bindString(index(ordprod_id), prod.getOrdprod_id() == null ? "" : prod.getOrdprod_id()); // ordprod_id
                dbUtils.bindString(index(ord_id), prod.getOrd_id() == null ? "" : prod.getOrd_id()); // ord_id
                dbUtils.bindString(index(prod_id), prod.getProd_id() == null ? "" : prod.getProd_id()); // prod_id
                dbUtils.bindString(index(prod_sku), prod.getProd_sku() == null ? "" : prod.getProd_sku()); // prod_sku
                dbUtils.bindString(index(prod_upc), prod.getProd_upc() == null ? "" : prod.getProd_upc()); // prod_upc
                dbUtils.bindString(index(ordprod_qty), TextUtils.isEmpty(prod.getOrdprod_qty()) ? "0" : prod.getOrdprod_qty()); // ordprod_qty
                if (prod.getOverwrite_price() != null) {
                    dbUtils.bindDouble(index(overwrite_price), prod.getOverwrite_price().doubleValue());
                } else {
                    dbUtils.bindNull(index(overwrite_price));
                }
                dbUtils.bindString(index(reason_id), prod.getReason_id() == null ? "" : prod.getReason_id()); // reason_id
                dbUtils.bindString(index(ordprod_name), prod.getOrdprod_name() == null ? "" : prod.getOrdprod_name()); // ordprod_name
                if (prod.getOrdprod_comment() != null && !prod.getOrdprod_comment().isEmpty())
                    dbUtils.bindString(index(ordprod_desc),
                            prod.getOrdprod_desc() == null ? "" : prod.getOrdprod_desc() + "-" + prod.getOrdprod_comment()); // ordprod_desc
                else
                    dbUtils.bindString(index(ordprod_desc), prod.getOrdprod_desc() == null ? "" : prod.getOrdprod_desc());
                dbUtils.bindString(index(ordprod_comment), prod.getOrdprod_comment() == null ? "" : prod.getOrdprod_comment());
                dbUtils.bindString(index(pricelevel_id), prod.getPricelevel_id() == null ? "" : prod.getPricelevel_id()); // pricelevel_id
                dbUtils.bindString(index(prod_seq), TextUtils.isEmpty(prod.getProd_seq()) ? "1" : prod.getProd_seq()); // prod_seq
                dbUtils.bindString(index(uom_name), prod.getUom_name() == null ? "" : prod.getUom_name()); // uom_name
                dbUtils.bindString(index(uom_conversion), prod.getUom_conversion() == null ? "" : prod.getUom_conversion()); // uom_conversion
                dbUtils.bindString(index(uom_id), prod.getUom_id() == null ? "" : prod.getUom_id()); // uom_id
                dbUtils.bindString(index(prod_taxId), prod.getProd_taxId() == null ? "" : prod.getProd_taxId()); // prod_taxId
                dbUtils.bindDouble(index(prod_taxValue),
                        prod.getProd_taxValue() == null ? 0 : Double.valueOf(String.valueOf(Global.getRoundBigDecimal(prod.getProd_taxValue(), 2))));
                dbUtils.bindString(index(discount_id), prod.getDiscount_id() == null ? "" : prod.getDiscount_id()); // discount_id
                dbUtils.bindString(index(discount_value),
                        TextUtils.isEmpty(prod.getDiscount_value()) ? "0" : prod.getDiscount_value()); // discount_value
                dbUtils.bindString(index(prod_istaxable),
                        TextUtils.isEmpty(prod.getProd_istaxable()) ? "0" : prod.getProd_istaxable()); // prod_istaxable
                dbUtils.bindString(index(discount_is_taxable),
                        TextUtils.isEmpty(prod.getDiscount_is_taxable()) ? "0" : prod.getDiscount_is_taxable()); // discount_is_taxable
                dbUtils.bindString(index(discount_is_fixed),
                        TextUtils.isEmpty(prod.getDiscount_is_fixed()) ? "0" : prod.getDiscount_is_fixed()); // discount_is_fixed
                dbUtils.bindString(index(onHand), TextUtils.isEmpty(prod.getOnHand()) ? "0" : prod.getOnHand()); // onHand
                dbUtils.bindString(index(imgURL), prod.getImgURL() == null ? "" : prod.getImgURL()); // imgURL
                dbUtils.bindString(index(prod_price), TextUtils.isEmpty(prod.getProd_price()) ? "0" : prod.getProd_price()); // prod_price
                dbUtils.bindString(index(prod_type), prod.getProd_type() == null ? "" : prod.getProd_type()); // prod_type
                dbUtils.bindString(index(itemTotal), TextUtils.isEmpty(prod.getItemTotal()) ? "0" : prod.getItemTotal()); // itemTotal
                dbUtils.bindString(index(itemSubtotal), String.valueOf(prod.getItemSubtotalCalculated() == null ?
                        "0" : prod.getItemSubtotalCalculated())); // itemSubtotal
                dbUtils.bindString(index(hasAddons), String.valueOf(prod.getHasAddons())); // hasAddons
                dbUtils.bindString(index(addon_section_name),
                        TextUtils.isEmpty(prod.getAddon_section_name()) ? "" : prod.getAddon_section_name());
                dbUtils.bindString(index(addon_position),
                        TextUtils.isEmpty(prod.getAddon_position()) ? "0" : prod.getAddon_position());
                dbUtils.bindString(index(cat_id), prod.getCat_id() == null ? "" : prod.getCat_id());
                dbUtils.bindString(index(product_taxes_json), StringUtil.nullStringToEmpty(gson.toJson(prod.getTaxes())));
                dbUtils.bindString(index(cat_name), StringUtil.nullStringToEmpty(prod.getCat_name()));
                dbUtils.bindString(index(addon_ordprod_id), prod.getAddon_ordprod_id() == null ? "" : prod.getAddon_ordprod_id());

                dbUtils.bindString(index(assignedSeat), prod.getAssignedSeat() == null ? "" : prod.getAssignedSeat());
                dbUtils.bindLong(index(seatGroupId), prod.getSeatGroupId());
                dbUtils.bindLong(index(prodPricePoints), Double.valueOf(prod.getProd_price_points()).longValue());
                dbUtils.bindString(index(isGC), String.valueOf(prod.isGC()));
                sqlinsert.execute();
                sqlinsert.clearBindings();
//                Log.d("Insert OrderProduct", prod.toString());
                if (isRestaurantMode && !prod.addonsProducts.isEmpty()) {
                    insertAddon(prod.getOrd_id(), sqlinsert, prod.addonsProducts);
                }
            }
            DBManager.getDatabase().setTransactionSuccessful();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sqlinsert != null) {
                sqlinsert.close();
            }
            DBManager.getDatabase().endTransaction();

        }
    }

    public OrderProduct getOrderProduct(Cursor cursor) {
        OrderProduct product = new OrderProduct();
        product.setAddon(Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex(addon))));
        product.setAddon_ordprod_id(cursor.getString(cursor.getColumnIndex(addon_ordprod_id)));
        product.setAdded(Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex(isAdded))));
        product.setPrinted(Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex(isPrinted))));
        product.setGC(Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex(isGC))));
        product.setItem_void(cursor.getString(cursor.getColumnIndex(item_void)));
        product.setOrdprod_id(cursor.getString(cursor.getColumnIndex(ordprod_id)));
        product.setOrd_id(cursor.getString(cursor.getColumnIndex(ord_id)));
        product.setProd_id(cursor.getString(cursor.getColumnIndex(prod_id)));

        String json = cursor.getString(cursor.getColumnIndex(product_taxes_json));
        product.setTaxes((List<Tax>) gson.fromJson(json, taxListType));
        product.setProd_sku(cursor.getString(cursor.getColumnIndex(prod_sku)));
        product.setProd_upc(cursor.getString(cursor.getColumnIndex(prod_upc)));
        product.setOrdprod_qty(cursor.getString(cursor.getColumnIndex(ordprod_qty)));
        product.setOverwrite_price(cursor.getString(cursor.getColumnIndex(overwrite_price)) == null
                ? null : new BigDecimal(cursor.getString(cursor.getColumnIndex(overwrite_price))));
        product.setReason_id(cursor.getString(cursor.getColumnIndex(reason_id)));
        product.setOrdprod_name(cursor.getString(cursor.getColumnIndex(ordprod_name)));
        product.setOrdprod_desc(cursor.getString(cursor.getColumnIndex(ordprod_desc)));
        product.setOrdprod_comment(cursor.getString(cursor.getColumnIndex(ordprod_comment)));
        product.setPricelevel_id(cursor.getString(cursor.getColumnIndex(pricelevel_id)));
        product.setProd_seq(cursor.getString(cursor.getColumnIndex(prod_seq)));
        product.setUom_name(cursor.getString(cursor.getColumnIndex(uom_name)));
        product.setUom_conversion(cursor.getString(cursor.getColumnIndex(uom_conversion)));
        product.setUom_id(cursor.getString(cursor.getColumnIndex(uom_id)));
        product.setProd_taxId(cursor.getString(cursor.getColumnIndex(prod_taxId)));
        product.setProd_taxValue(new BigDecimal(cursor.getDouble(cursor.getColumnIndex(prod_taxValue))));
        product.setDiscount_id(cursor.getString(cursor.getColumnIndex(discount_id)));
        product.setDiscount_value(cursor.getString(cursor.getColumnIndex(discount_value)));
        product.setProd_istaxable(cursor.getString(cursor.getColumnIndex(prod_istaxable)));
        product.setDiscount_is_taxable(cursor.getString(cursor.getColumnIndex(discount_is_taxable)));
        product.setDiscount_is_fixed(cursor.getString(cursor.getColumnIndex(discount_is_fixed)));
        product.setOnHand(cursor.getString(cursor.getColumnIndex(onHand)));
        product.setImgURL(cursor.getString(cursor.getColumnIndex(imgURL)));
        product.setProd_price(cursor.getString(cursor.getColumnIndex(prod_price)));
        product.setProd_type(cursor.getString(cursor.getColumnIndex(prod_type)));
        product.setItemTotal(cursor.getString(cursor.getColumnIndex(itemTotal)));
//        product.setItemSubtotal(cursor.getString(cursor.getColumnIndex(itemSubtotal)));
        product.setAddon_section_name(cursor.getString(cursor.getColumnIndex(addon_section_name)));
        product.setAddon_position(cursor.getString(cursor.getColumnIndex(addon_position)));
        product.setCat_id(cursor.getString(cursor.getColumnIndex(cat_id)));
        product.setCat_name(cursor.getString(cursor.getColumnIndex(cat_name)));
        product.setAssignedSeat(cursor.getString(cursor.getColumnIndex(assignedSeat)));
        product.setAddon(Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex(addon))));
        String groupId = cursor.getString(cursor.getColumnIndex(seatGroupId));
        product.setProd_price_points(cursor.getString(cursor.getColumnIndex(prodPricePoints)));
        product.setSeatGroupId(groupId == null || groupId.isEmpty() ? 0 : Integer.parseInt(groupId));
//        if (product.getHasAddons()) {
        List<OrderProduct> orderProductAddons = getOrderProductAddons(product.getOrdprod_id());

        Discount discount = productsHandler.getDiscounts(product.getDiscount_id());
        product.setDisAmount(discount.getProductPrice());
        product.setDisTotal(product.getDiscount_value());
        product.addonsProducts = orderProductAddons;
//        }
        return product;
    }

    public List<OrderProduct> getOrderProducts(String orderId) {
        Cursor cursor = null;
        try {
            List<OrderProduct> products = new ArrayList<>();
            cursor = getCursorData(orderId);
            if (cursor.moveToFirst()) {
                OrderProduct product;
                do {
                    product = getOrderProduct(cursor);
                    if (!product.isAddon())
                        products.add(product);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return products;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public Cursor getWalletOrdProd(String ordID) {
        return DBManager.getDatabase().rawQuery(("SELECT op.*,pi.prod_img_name FROM " + table_name + " op LEFT OUTER JOIN Products_Images pi ON op.prod_id = pi.prod_id ") + "AND pi.type = 'I' WHERE ord_id = ?", new String[]{ordID});
    }

    public HashMap<String, List<Orders>> getStationPrinterProducts(String ordID) {
        Cursor c = null;
        try {
            List<Orders> list;
            c = DBManager.getDatabase().rawQuery("SELECT op.ordprod_id,op.ordprod_name,op.ordprod_desc,op.overwrite_price," +
                    "(op.overwrite_price*op.ordprod_qty) AS 'total', " + "op.ordprod_qty,op.ordprod_comment,op.addon," +
                    "op.isAdded,op.hasAddons,op.cat_id,IFNULL(pa.attr_desc,'') as 'attr_desc' " +
                    "FROM " + table_name + " op " + "LEFT OUTER JOIN ProductsAttr pa ON op.prod_id = pa.prod_id " +
                    "WHERE (ord_id = '" + ordID + "' AND isPrinted = 'false')  " +
                    "OR ordprod_id = '" + ordID + "'  ", null);
            Orders[] orders = new Orders[c.getCount()];
            HashMap<String, List<Orders>> tempMap = new HashMap<>();
            if (c.moveToFirst()) {
                int i_ordprod_id = c.getColumnIndex(ordprod_id);
                int i_ordprod_name = c.getColumnIndex(ordprod_name);
                int i_ordprod_desc = c.getColumnIndex(ordprod_desc);
                int i_overwrite_price = c.getColumnIndex(overwrite_price);
                int i_total = c.getColumnIndex("total");
                int i_ordprod_qty = c.getColumnIndex(ordprod_qty);
                int i_addon = c.getColumnIndex(addon);
                int i_isAdded = c.getColumnIndex(isAdded);
                int i_hasAddons = c.getColumnIndex(hasAddons);
                int i_cat_id = c.getColumnIndex(cat_id);
                int i_attr_desc = c.getColumnIndex("attr_desc");
                int i_ordprod_comment = c.getColumnIndex(ordprod_comment);
                int i = 0;
                boolean itHasAddons = false;
                String parentCatID = "";
                boolean inAddons = false;
                String tempCatID;
                do {
                    if (itHasAddons && !c.getString(i_addon).equals("true"))
                        inAddons = false;

                    orders[i] = new Orders();
                    orders[i].setOrdprodID(c.getString(i_ordprod_id));
                    orders[i].setName(c.getString(i_ordprod_name));
                    orders[i].setProdDescription(c.getString(i_ordprod_desc));
                    orders[i].setOverwritePrice(format(c.getString(i_overwrite_price)));
                    orders[i].setTotal(format(c.getString(i_total)));
                    orders[i].setQty(c.getString(i_ordprod_qty));
                    orders[i].setAddon(c.getString(i_addon));
                    orders[i].setIsAdded(c.getString(i_isAdded));
                    orders[i].setHasAddon(c.getString(i_hasAddons));
                    orders[i].setAttrDesc(c.getString(i_attr_desc));
                    orders[i].setOrdProdComment(c.getString(i_ordprod_comment));

                    if (tempMap.containsKey(c.getString(i_cat_id)) || (itHasAddons && inAddons)) {
                        if (itHasAddons && inAddons)
                            tempCatID = parentCatID;
                        else
                            tempCatID = c.getString(i_cat_id);
                        list = tempMap.get(tempCatID);
                        list.add(orders[i]);
                        tempMap.put(tempCatID, list);
                    } else {
                        list = new ArrayList<>();
                        list.add(orders[i]);
                        tempMap.put(c.getString(i_cat_id), list);
                    }
                    if (orders[i].hasAddon() && !inAddons) {
                        parentCatID = c.getString(i_cat_id);
                        itHasAddons = true;
                        inAddons = true;
                    } else if (!orders[i].hasAddon() && !inAddons) {
                        itHasAddons = false;
                    }
                    i++;
                } while (c.moveToNext());
            }
            c.close();
            return tempMap;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    private String format(String text) {
        DecimalFormat frmt = new DecimalFormat("0.00");

        if (TextUtils.isEmpty(text))
            return "0.00";

        return frmt.format(Double.parseDouble(text));
    }

    public Cursor getOrderProductsOnHold(String ordID) {
        Cursor c = DBManager.getDatabase().rawQuery("SELECT " + sb3.toString() + "," +
                "CASE WHEN p.prod_taxcode='' THEN '0' ELSE IFNULL(s.taxcode_istaxable,'1')  " +
                "END AS 'prod_istaxable'," +
                "p.prod_taxtype " +
                "FROM " + table_name + " op " +
                "LEFT OUTER JOIN Products p ON op.prod_id = p.prod_id " +
                "LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id " +
                "WHERE op.ord_id = '" + ordID + "' " +
                "ORDER BY prod_seq ASC", null);
        c.moveToFirst();
        return c;

    }

    public HashMap<String, String> getOrdProdGiftCard(String cardNumber) {
        Cursor c = null;
        try {
            HashMap<String, String> map = new HashMap<>();

            c = DBManager.getDatabase().rawQuery("SELECT * FROM " + table_name + " op LEFT JOIN OrderProductsAttr at ON op.ordprod_id = at.ordprod_id WHERE " + "at.value = ? AND op.cardIsActivated = '0' ORDER BY at.ordprodattr_id DESC LIMIT 1", new String[]{cardNumber});

            if (c.moveToFirst()) {
                map.put("overwrite_price", c.getString(c.getColumnIndex("overwrite_price")));
                map.put("ordprod_id", c.getString(c.getColumnIndex("ordprod_id")));
            }
            c.close();
            return map;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public List<OrderProduct> getOrdProdGiftCardNumber(String ordprodID) {
        StringBuilder sb = new StringBuilder();
        List<OrderProduct> list = new ArrayList<>();
        sb.append("SELECT name,value FROM ").append("OrderProductsAttr").append(" WHERE ordprod_id = ?");
        Cursor c = DBManager.getDatabase().rawQuery(sb.toString(),new String[]{ordprodID});
        if (c.moveToFirst()) {
            int gcNumber = c.getColumnIndex("value");
            int gcName = c.getColumnIndex("name");
            do{
                OrderProduct ordProd = new OrderProduct();
                ordProd.setGiftcardName(c.getString(gcName));
                ordProd.setGiftcardNumber(c.getString(gcNumber));
                list.add(ordProd);
            }while (c.moveToNext()) ;
        }
        c.close();

        return list;
    }

    public void updateOrdProdCardActivated(String ordProdID) {

        ContentValues args = new ContentValues();
        args.put("cardIsActivated", "1");
        DBManager.getDatabase().update(table_name, args, ordprod_id + " = ?", new String[]{ordProdID});
        // db.close();
    }

    public List<OrderProduct> getProductsDayReport(boolean isSales, String clerk_id, String date) {
        Cursor c = null;
        try {
            StringBuilder query = new StringBuilder();
            List<OrderProduct> listOrdProd = new ArrayList<>();

            query.append(
                    "SELECT prod_price, sum(itemsubtotal) as 'itemsubtotal_sum', ordprod_name, prod_id,prod_sku, prod_upc, " +
                            "sum(ordprod_qty) as 'ordprod_qty', " +
                            "sum(CASE WHEN overwrite_price = '' THEN prod_price * ordprod_qty " +
                            "ELSE IFNULL(overwrite_price, prod_price) * ordprod_qty " +
                            "END) AS 'overwrite_price', " +
                            "date(o.ord_timecreated,'localtime') as 'date' " +
                            "FROM " + table_name + " op ");
            query.append("LEFT JOIN Orders o ON op.ord_id = o.ord_id WHERE o.isVoid = '0' AND o.ord_type IN ");

            if (isSales)
                query.append("('2','5') ");
            else// returned items
                query.append("('1') ");

            String[] where_values = null;
            if (clerk_id != null && !clerk_id.isEmpty()) {
                query.append(" AND o.clerk_id = ? ");
                where_values = new String[]{clerk_id};
                if (date != null && !date.isEmpty()) {
                    query.append(" AND date = ? ");
                    where_values = new String[]{clerk_id, date};
                }
            } else if (date != null && !date.isEmpty()) {
                query.append(" AND date = ? ");
                where_values = new String[]{date};
            }

            query.append(" GROUP BY op.prod_id ORDER BY op.ordprod_name");

            c = DBManager.getDatabase().rawQuery(query.toString(), where_values);

            if (c.moveToFirst()) {
                int i_itemsubtotal_sum = c.getColumnIndex("itemsubtotal_sum");
                int i_ordprod_name = c.getColumnIndex(ordprod_name);
                int i_prod_id = c.getColumnIndex(prod_id);
                int i_prod_sku = c.getColumnIndex(prod_sku);
                int i_prod_upc = c.getColumnIndex(prod_upc);
                int i_ordprod_qty = c.getColumnIndex(ordprod_qty);
                int i_overwrite_price = c.getColumnIndex(overwrite_price);
                int i_prod_price = c.getColumnIndex(prod_price);
                do {
                    OrderProduct ordProd = new OrderProduct();
                    ordProd.setProd_price(c.getString(i_prod_price));
                    ordProd.setOverwrite_price(c.getString(i_overwrite_price) == null
                            ? null : new BigDecimal(c.getString(i_overwrite_price)));
                    ordProd.setOrdprod_name(c.getString(i_ordprod_name));
                    ordProd.setProd_id(c.getString(i_prod_id));
                    ordProd.setProd_sku(c.getString(i_prod_sku));
                    ordProd.setProd_upc(c.getString(i_prod_upc));
                    ordProd.setOrdprod_qty(c.getString(i_ordprod_qty));
                    ordProd.setItemTotal(c.getString(i_itemsubtotal_sum));
                    listOrdProd.add(ordProd);
                } while (c.moveToNext());
            }

            c.close();
            return listOrdProd;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public List<OrderProduct> getDepartmentDayReport(boolean isSales, String clerk_id,
                                                     String startDate) {
        return getDepartmentDayReport(isSales, clerk_id, startDate, null);
    }

    public List<OrderProduct> getDepartmentDayReport(boolean isSales, String clerk_id,
                                                     String startDate, String endDate) {
        Cursor c = null;
        try {
            StringBuilder query = new StringBuilder();
            List<OrderProduct> listOrdProd = new ArrayList<>();

            query.append(
                    "SELECT prod_price as 'prod_price', case when op.cat_name = '' THEN 'Other' else  " +
                            "op.cat_name end as cat_name, op.cat_id, sum(ordprod_qty) as 'ordprod_qty', " +
                            "round(sum(CASE WHEN overwrite_price = '' THEN prod_price * ordprod_qty " +
                            "ELSE IFNULL(overwrite_price, prod_price) * ordprod_qty  END),2) as 'overwrite_price', ");

            query.append("o.ord_timecreated " +
                    "FROM " + table_name + " op ");
            query.append(
                    "LEFT JOIN Categories c ON op.cat_id = c.cat_id " +
                            "LEFT JOIN Orders o ON op.ord_id = o.ord_id " +
                            "WHERE o.isVoid = '0' AND processed != '10' AND o.ord_type IN ");

            if (isSales)
                query.append("('2','5') ");
            else// returned items
                query.append("('1') ");

            ArrayList<String> where_values = new ArrayList<>();
            if (clerk_id != null && !clerk_id.isEmpty()) {
                query.append(" AND o.clerk_id = ? ");
                where_values.add(clerk_id);
            }

            boolean hasStartDate = (startDate != null && !startDate.isEmpty())?true:false;
            boolean hasEndDate = (endDate != null && !endDate.isEmpty())?true:false;

            if (hasStartDate && hasEndDate) {
                query.append(" AND datetime(o.ord_timecreated,'utc') BETWEEN datetime(?,'utc')  ");
                query.append(" and datetime(?,'utc')");
                where_values.add(startDate);
                where_values.add(endDate + ":59");
            }else if(hasStartDate || hasEndDate) {
                if (hasStartDate) {
                    query.append(" AND datetime(o.ord_timecreated,'utc') >= datetime(?, 'utc')");
                    where_values.add(startDate);
                } else {
                    query.append(" AND datetime(o.ord_timecreated,'utc') <= datetime(?, 'utc') ");
                    where_values.add(endDate + ":59");
                }
            }

            query.append(" AND o.ord_id IN (SELECT job_id FROM Payments GROUP BY job_id) ");
            query.append(" GROUP BY op.cat_id ORDER BY op.cat_name");

            c = DBManager.getDatabase().rawQuery(query.toString(), where_values.toArray(new String[0]));

            if (c.moveToFirst()) {
                int i_cat_name = c.getColumnIndex("cat_name");
                int i_cat_id = c.getColumnIndex(cat_id);
                int i_ordprod_qty = c.getColumnIndex(ordprod_qty);
                int i_overwrite_price = c.getColumnIndex(overwrite_price);
                int i_prod_price = c.getColumnIndex(prod_price);

                do {
                    OrderProduct ordProd = new OrderProduct();
                    ordProd.setProd_price(c.getString(i_prod_price));
                    ordProd.setOverwrite_price(c.getString(i_overwrite_price) == null
                            ? null : new BigDecimal(c.getString(i_overwrite_price)));
                    ordProd.setCat_name(c.getString(i_cat_name));
                    ordProd.setCat_id(c.getString(i_cat_id));
                    ordProd.setOrdprod_qty(c.getString(i_ordprod_qty));

                    listOrdProd.add(ordProd);
                } while (c.moveToNext());
            }

            c.close();
            return listOrdProd;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public List<SalesByClerk> getSalesShiftReportByClerk(boolean isSales, String clerk_id,
                                                         String startDate, String endDate) {
        Cursor c = null;
        try {
            StringBuilder query = new StringBuilder();
            List<SalesByClerk> salesByClerkList = new ArrayList<SalesByClerk>();

            String sqlDateFunction = "date";
            if (endDate != null) sqlDateFunction = "datetime";

            query.append(
                    "SELECT o.clerk_id AS 'clerk_id', count(o.ord_id) AS 'ordprod_qty', \n" +
                            "sum(o.ord_subtotal) as 'overwrite_price', ");
            query.append(sqlDateFunction);
            query.append("(o.ord_timecreated) as 'date'" +
                    " FROM Orders o ");
            query.append(" WHERE o.isVoid = '0' AND processed != '10' AND o.ord_type IN ");

            if (isSales)
                query.append("('2','5') ");
            else// returned items
                query.append("('1') ");

            ArrayList<String> where_values = new ArrayList<>();
            if (clerk_id != null && !clerk_id.isEmpty()) {
                query.append(" AND o.clerk_id = ? ");
                where_values.add(clerk_id);
            }

            boolean hasStartDate = (startDate != null && !startDate.isEmpty())?true:false;
            boolean hasEndDate = (endDate != null && !endDate.isEmpty())?true:false;

            if (hasStartDate && hasEndDate) {
                query.append(" AND datetime(o.ord_timecreated,'utc') BETWEEN datetime(?,'utc')  ");
                query.append(" and datetime(?,'utc')");
                where_values.add(startDate);
                where_values.add(endDate + ":59");
            }else if(hasStartDate || hasEndDate) {
                if (hasStartDate) {
                    query.append(" AND datetime(o.ord_timecreated,'utc') >= datetime(?, 'utc')");
                    where_values.add(startDate);
                } else {
                    query.append(" AND datetime(o.ord_timecreated,'utc') <= datetime(?, 'utc') ");
                    where_values.add(endDate + ":59");
                }
            }

            query.append(" GROUP BY o.clerk_id");

            c = DBManager.getDatabase().rawQuery(query.toString(), where_values.toArray(new String[0]));

            if (c.moveToFirst()) {
                int clerkId = c.getColumnIndex("clerk_id");
                int i_ordprod_qty = c.getColumnIndex(ordprod_qty); // total items
                int i_overwrite_price = c.getColumnIndex(overwrite_price); // total

                do {
                    SalesByClerk salesByClerk = new SalesByClerk();;
                    salesByClerk.setClerkId(c.getString(clerkId));
                    salesByClerk.setOrdProdQuantity(c.getString(i_ordprod_qty));
                    salesByClerk.setOverwritePrice(c.getString(i_overwrite_price) == null
                            ? null : c.getString(i_overwrite_price));
                    salesByClerkList.add(salesByClerk);

                } while (c.moveToNext());
            }

            c.close();
            return salesByClerkList;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public void completeProductFields(List<OrderProduct> orderProducts, Context activity) {
        ProductsHandler productsHandler = new ProductsHandler(activity);
        for (OrderProduct orderProduct : orderProducts) {
            Product product = productsHandler.getProductDetails(orderProduct.getProd_id());
            orderProduct.setProd_istaxable(product.getProdIstaxable());
            Discount discount = productsHandler.getDiscounts(orderProduct.getDiscount_id());
            orderProduct.setDisAmount(discount.getProductPrice());
            orderProduct.setPrices(orderProduct.getFinalPrice(), orderProduct.getOrdprod_qty());
//            orderProduct.setItemSubtotal(String.valueOf(orderProduct.getItemSubtotalCalculated()));
            completeProductFields(orderProduct.addonsProducts, activity);
        }
    }

    public AssignEmployee getAssignEmployee() {
        if (this.assignEmployee == null) {
            this.assignEmployee = AssignEmployeeDAO.getAssignEmployee();
        }
        return this.assignEmployee;
    }
}