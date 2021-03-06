package com.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.android.dao.AssignEmployeeDAO;
import com.android.emobilepos.models.Discount;
import com.android.emobilepos.models.Product;
import com.android.emobilepos.models.realms.AssignEmployee;
import com.android.support.Global;
import com.android.support.MyPreferences;

import net.sqlcipher.database.SQLiteStatement;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import util.StringUtil;

public class ProductsHandler {

    public static final String prod_prices_group_id = "prod_prices_group_id";
    private static final String prod_id = "prod_id";
    private static final String prod_type = "prod_type";
    private static final String prod_disc_type = "prod_disc_type";
    private static final String cat_id = "cat_id";
    private static final String prod_sku = "prod_sku";
    private static final String prod_upc = "prod_upc";
    private static final String prod_name = "prod_name";
    private static final String prod_desc = "prod_desc";
    private static final String prod_extradesc = "prod_extradesc";
    private static final String prod_onhand = "prod_onhand";
    private static final String prod_onorder = "prod_onorder";
    private static final String prod_uom = "prod_uom";
    private static final String prod_price = "prod_price";
    private static final String prod_cost = "prod_cost";
    private static final String prod_taxcode = "prod_taxcode";
    private static final String prod_taxtype = "prod_taxtype";
    private static final String prod_glaccount = "prod_glaccount";
    private static final String prod_mininv = "prod_mininv";
    private static final String prod_update = "prod_update";
    private static final String isactive = "isactive";
    private static final String isGC = "isGC";
    private static final String prod_showOnline = "prod_showOnline";
    private static final String prod_ispromo = "prod_ispromo";
    private static final String prod_shipping = "prod_shipping";
    private static final String prod_weight = "prod_weight";
    private static final String prod_expense = "prod_expense";
    private static final String prod_disc_type_points = "prod_disc_type_points";
    private static final String prod_price_points = "prod_price_points";
    private static final String prod_value_points = "prod_value_points";
    private static final List<String> attr = Arrays.asList(prod_id, prod_type, prod_disc_type, cat_id,
            prod_sku, prod_upc, prod_name, prod_desc, prod_extradesc, prod_onhand, prod_onorder, prod_uom, prod_price,
            prod_cost, prod_taxcode, prod_taxtype, prod_glaccount, prod_mininv, prod_update, isactive, prod_showOnline,
            prod_ispromo, prod_shipping, prod_weight, prod_expense, prod_disc_type_points, prod_price_points,
            prod_value_points, prod_prices_group_id, isGC);

    private static final String table_name = "Products";
    private StringBuilder sb1, sb2;
    private HashMap<String, Integer> attrHash;
    private MyPreferences myPref;

    public ProductsHandler(Context activity) {
        attrHash = new HashMap<>();
        sb1 = new StringBuilder();
        sb2 = new StringBuilder();
        myPref = new MyPreferences(activity);
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

    public void insert(List<Product> products) {
        DBManager.getDatabase().beginTransaction();
        SQLiteStatement insert = null;
        try {
            insert = DBManager.getDatabase().compileStatement("INSERT INTO " + table_name + " (" + sb1.toString() + ") " + "VALUES (" + sb2.toString() + ")");

            for (Product product : products) {
                insert.bindString(index(prod_id), product.getId()); // prod_id
                insert.bindString(index(prod_type), StringUtil.nullStringToEmpty(product.getProdType())); // prod_type
                insert.bindString(index(prod_disc_type), StringUtil.nullStringToEmpty(product.getProd_disc_type())); // prod_disc_type
                insert.bindString(index(cat_id), StringUtil.nullStringToEmpty(product.getCatId())); // cat_id
                insert.bindString(index(prod_sku), StringUtil.nullStringToEmpty(product.getProd_sku())); // prod_sku
                insert.bindString(index(prod_upc), StringUtil.nullStringToEmpty(product.getProd_upc())); // prod_upc
                insert.bindString(index(prod_name), StringUtil.nullStringToEmpty(product.getProdName())); // prod_name
                insert.bindString(index(prod_desc), StringUtil.nullStringToEmpty(product.getProdDesc())); // prod_desc
                insert.bindString(index(prod_extradesc), StringUtil.nullStringToEmpty(product.getProdExtraDesc())); // prod_extradesc
                insert.bindString(index(prod_onhand), StringUtil.nullStringToEmpty(product.getProdOnHand())); // prod_onhand
                insert.bindString(index(prod_onorder), StringUtil.nullStringToEmpty(product.getProd_onorder())); // prod_onorder
                insert.bindString(index(prod_uom), StringUtil.nullStringToEmpty(product.getProd_uom())); // prod_uom
                insert.bindString(index(prod_price), StringUtil.nullStringToEmpty(product.getProdPrice())); // prod_price
                insert.bindString(index(prod_cost), StringUtil.nullStringToEmpty(product.getProd_cost())); // prod_cost
                insert.bindString(index(prod_taxcode), StringUtil.nullStringToEmpty(product.getProdTaxCode())); // prod_taxcode
                insert.bindString(index(prod_taxtype), StringUtil.nullStringToEmpty(product.getProdTaxType())); // prod_taxtype
                insert.bindString(index(prod_glaccount), StringUtil.nullStringToEmpty(product.getProd_glaccount())); // prod_glaccount
                insert.bindString(index(prod_mininv), StringUtil.nullStringToEmpty(product.getProd_mininv())); // prod_mininv
                insert.bindString(index(prod_update), StringUtil.nullStringToEmpty(product.getProd_update())); // prod_update
                insert.bindString(index(isactive), StringUtil.nullStringToEmpty(product.getIsactive())); //) isactive
                insert.bindString(index(prod_showOnline), StringUtil.nullStringToEmpty(product.getProd_showOnline())); // prod_showOnlne
                insert.bindString(index(prod_ispromo), StringUtil.nullStringToEmpty(product.getProd_ispromo())); // prod_ispromo
                insert.bindString(index(prod_shipping), StringUtil.nullStringToEmpty(product.getProd_shipping())); // prod_shipping
                insert.bindString(index(prod_weight), StringUtil.nullStringToEmpty(product.getProd_weight())); // prod_weigth
                insert.bindString(index(prod_expense), StringUtil.nullStringToEmpty(product.getProd_expense())); // prod_expense
                insert.bindString(index(prod_disc_type_points), StringUtil.nullStringToEmpty(product.getProd_disc_type_points())); // prod_disc_type_points
                insert.bindLong(index(prod_price_points), product.getProdPricePoints()); // prod_price_points
                insert.bindLong(index(prod_value_points), product.getProdValuePoints()); // prod_value_points
                insert.bindString(index(prod_prices_group_id), StringUtil.nullStringToEmpty(product.getPricesXGroupid())); // prod_value_points
                insert.bindString(index(isGC), String.valueOf(product.isGC()));

                insert.execute();
                insert.clearBindings();
            }
            insert.close();
            DBManager.getDatabase().setTransactionSuccessful();
            DBManager.getDatabase().endTransaction();
        } finally {
            if (insert != null) {
                insert.close();
            }
        }
    }

    public void emptyTable() {
        DBManager.getDatabase().execSQL("DELETE FROM " + table_name);
    }

    public Cursor getCatalogData(String categoryId, int limit, int offset) {


            String[] parameters;
            String query;
            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            String priceLevelID;
            if (myPref.isCustSelected())
                priceLevelID = myPref.getCustPriceLevel();
            else {
                AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();
                priceLevelID = StringUtil.nullStringToEmpty(assignEmployee.getPricelevelId());
            }

            if (TextUtils.isEmpty(categoryId)) {
                sb.append(
                        "SELECT  p.prod_id as '_id', p.isGC as 'isGC', p.prod_prices_group_id as 'prod_prices_group_id'," +
                                " p.prod_price as 'master_price'," +
                                "vp.price as 'volume_price', ch.over_price_net as 'chain_price',");
                sb.append(
                        "CASE WHEN pl.pricelevel_type = 'FixedPercentage' " +
                                "THEN (p.prod_price+(p.prod_price*(pl.pricelevel_fixedpct/100))) ");
                sb.append(
                        "ELSE pli.pricelevel_price END AS 'pricelevel_price',p.prod_price_points," +
                                "p.prod_value_points,p.prod_name,p.prod_desc, p.prod_sku, p.prod_upc," +
                                "p.prod_extradesc,p.prod_onhand as 'master_prod_onhand'," +
                                "ei.prod_onhand as 'local_prod_onhand',i.prod_img_name," +
                                "CASE WHEN p.prod_taxcode='' THEN '0' ELSE IFNULL(s.taxcode_istaxable,'1')  " +
                                "END AS 'prod_istaxable' ");
                sb.append(",p.prod_taxcode,p.prod_taxtype, p.prod_type,c.cat_id, c.cat_name as 'cat_name' ");

                if (myPref.isCustSelected() && myPref.getPreferences(MyPreferences.pref_filter_products_by_customer)) {
                    if (Global.isConsignment) {
                        sb.append(",ci.qty AS 'consignment_qty' ");

                        sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                        if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                            sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                                    .append("' WHERE p.prod_type != 'Discount' ");
                        else
                            sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                                    .append("' AND p.prod_type != 'Discount' ");

                        if (Global.consignmentType == Global.OrderType.ORDER)
                            sb2.append("AND ci.qty>0 ");
                    } else if (Global.isInventoryTransfer) {
                        sb.append(",li.prod_onhand AS 'location_qty' ");
                        sb2.append("LEFT JOIN LocationsInventory li ON li.prod_id = p.prod_id WHERE li.loc_id = ? ");
                    } else
                        sb2.append("WHERE p.prod_type != 'Discount' AND ch.cust_chain = ? ");// .append("'
                } else {
                    sb2.append("AND ch.cust_chain = ? ");
                    if (Global.isConsignment) {
                        sb.append(",ci.qty AS 'consignment_qty' ");

                        sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                        if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                            sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                                    .append("' WHERE p.prod_type != 'Discount' ");
                        else
                            sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                                    .append("' AND p.prod_type != 'Discount' ");
                        if (Global.consignmentType == Global.OrderType.ORDER)
                            sb2.append("AND ci.qty>0 ");
                    } else if (Global.isInventoryTransfer) {
                        sb.append(",li.prod_onhand AS 'location_qty' ");
                        sb2.append("LEFT JOIN LocationsInventory li ON li.prod_id = p.prod_id WHERE li.loc_id = ? ");
                    } else
                        sb2.append("WHERE p.prod_type != 'Discount' ");// ORDER BY
                    // p.prod_name");
                }
                if (myPref.getPreferences(MyPreferences.pref_enable_multi_category)) {
                    sb.append(
                            "FROM Products p " +
                                    "LEFT JOIN ProdCatXref xr ON p.prod_id = xr.prod_id  " +
                                    "LEFT JOIN Categories c ON c.cat_id = xr.cat_id " +
                                    "LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id " +
                                    "LEFT OUTER JOIN VolumePrices vp ON p.prod_id = vp.prod_id AND '1' " +
                                    "BETWEEN vp.minQty AND vp.maxQty  AND ");
                } else {
                    sb.append(
                            "FROM Products p " +
                                    "LEFT OUTER JOIN Categories c ON c.cat_id = p.cat_id " +
                                    "LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id " +
                                    "LEFT OUTER JOIN VolumePrices vp ON p.prod_id = vp.prod_id AND '1' " +
                                    "BETWEEN vp.minQty AND vp.maxQty  AND ");
                }
                sb.append(
                        "vp.pricelevel_id = ?  " +
                                "LEFT OUTER JOIN PriceLevelItems pli ON p.prod_id = pli.pricelevel_prod_id ");
                sb.append(
                        "AND pli.pricelevel_id = ? " +
                                "LEFT OUTER JOIN PriceLevel pl ON pl.pricelevel_id = ? " +
                                "LEFT OUTER JOIN Products_Images i ON p.prod_id = i.prod_id AND i.type = 'I' ");
                sb.append(
                        "LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id " +
                                "LEFT OUTER JOIN ProductChainXRef ch ON ch.prod_id = p.prod_id ");

                sb.append(sb2);

                if (myPref.getPreferences(MyPreferences.pref_group_in_catalog_by_name) ||
                        myPref.getPreferences(MyPreferences.pref_enable_multi_category)) {
                    sb.append(" GROUP BY p.prod_name ORDER BY p.prod_name");
                } else {
                    sb.append(" ORDER BY p.prod_name");
                }
                if (Global.isInventoryTransfer)
                    parameters = new String[]{priceLevelID, priceLevelID, priceLevelID, myPref.getCustID(),
                            Global.locationFrom.getLoc_id()};
                else
                    parameters = new String[]{priceLevelID, priceLevelID, priceLevelID, myPref.getCustID()};
                query = sb.toString();
            } else {

                sb.append(
                        "SELECT  p.prod_id as '_id',p.isGC as 'isGC', p.prod_prices_group_id as 'prod_prices_group_id', p.prod_sku, " +
                                "p.prod_upc, p.prod_price as 'master_price',vp.price as 'volume_price',ch.over_price_net as 'chain_price',");
                sb.append(
                        "CASE WHEN pl.pricelevel_type = 'FixedPercentage' THEN (p.prod_price+(p.prod_price*(pl.pricelevel_fixedpct/100))) ");
                sb.append(
                        "ELSE pli.pricelevel_price END AS 'pricelevel_price',p.prod_price_points,p.prod_value_points," +
                                "p.prod_name,p.prod_desc,p.prod_extradesc,p.prod_onhand as 'master_prod_onhand',ei.prod_onhand as 'local_prod_onhand',i.prod_img_name, CASE WHEN p.prod_taxcode='' THEN '0' ELSE IFNULL(s.taxcode_istaxable,'1')  END AS 'prod_istaxable' ");
                sb.append(",p.prod_taxcode,p.prod_taxtype, p.prod_type,c.cat_id, c.cat_name as 'cat_name' ");

                if (myPref.isCustSelected() && myPref.getPreferences(MyPreferences.pref_filter_products_by_customer)) {
                    if (Global.isConsignment) {
                        sb.append(",ci.qty AS 'consignment_qty' ");

                        sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                        if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                            sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                                    .append("' WHERE p.prod_type != 'Discount' ");
                        else
                            sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                                    .append("' AND p.prod_type != 'Discount' ");
                        if (Global.consignmentType == Global.OrderType.ORDER)
                            sb.append("AND ci.qty>0 ");
                    } else if (Global.isInventoryTransfer) {
                        sb.append(",li.prod_onhand AS 'location_qty' ");
                        sb2.append("LEFT JOIN LocationsInventory li ON li.prod_id = p.prod_id WHERE li.loc_id = ? ");
                    } else
                        sb.append("WHERE p.prod_type != 'Discount' AND ch.cust_chain = ? ");// .append("'
                } else {
                    sb2.append("AND ch.cust_chain = ? ");
                    if (Global.isConsignment) {
                        sb.append(",ci.qty AS 'consignment_qty' ");

                        sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                        if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                            sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                                    .append("' WHERE p.prod_type != 'Discount' ");
                        else
                            sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                                    .append("' AND p.prod_type != 'Discount' ");
                        if (Global.consignmentType == Global.OrderType.ORDER)
                            sb2.append("AND ci.qty>0 ");
                    } else if (Global.isInventoryTransfer) {
                        sb.append(",li.prod_onhand AS 'location_qty' ");
                        sb2.append("LEFT JOIN LocationsInventory li ON li.prod_id = p.prod_id WHERE li.loc_id = ? ");
                    } else
                        sb2.append("WHERE p.prod_type != 'Discount' ");// ORDER BY
                }
                if (myPref.getPreferences(MyPreferences.pref_enable_multi_category)) {
                    sb.append("FROM Products p " + "LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id " +
                            "INNER JOIN ProdCatXref xr ON p.prod_id = xr.prod_id  " +
                            "INNER JOIN Categories c ON c.cat_id = xr.cat_id AND " +
                            "xr.cat_id = '").append(categoryId).append("'");
                } else {
                    sb.append("FROM Products p " +
                            "LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id " +
                            "INNER JOIN Categories c ON c.cat_id = p.cat_id AND " +
                            "p.cat_id = '").append(categoryId).append("'");
                }

                sb.append(" LEFT OUTER JOIN VolumePrices vp ON p.prod_id = vp.prod_id AND '1' " +
                        "BETWEEN vp.minQty AND ");
                sb.append(
                        "vp.maxQty AND vp.pricelevel_id = ? " +
                                "LEFT OUTER JOIN PriceLevelItems pli ON p.prod_id = pli.pricelevel_prod_id AND ");
                sb.append(
                        "pli.pricelevel_id = ? " +
                                "LEFT OUTER JOIN PriceLevel pl ON pl.pricelevel_id = ? " +
                                "LEFT OUTER JOIN Products_Images i ON p.prod_id = i.prod_id AND i.type = 'I' ");
                sb.append(
                        "LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id " +
                                "LEFT OUTER JOIN ProductChainXRef ch ON ch.prod_id = p.prod_id ");

                sb.append(sb2);
                if (myPref.getPreferences(MyPreferences.pref_group_in_catalog_by_name)) {
                    sb.append(" GROUP BY p.prod_name ORDER BY p.prod_name");
                } else {
                    sb.append(" GROUP BY p.prod_id ORDER BY p.prod_name");
                }

                if (Global.isInventoryTransfer)
                    parameters = new String[]{priceLevelID, priceLevelID, priceLevelID, myPref.getCustID(),
                            Global.locationFrom.getLoc_id()};
                else
                    parameters = new String[]{priceLevelID, priceLevelID, priceLevelID, myPref.getCustID()};
                query = sb.toString();
            }

            Cursor cursor = DBManager.getDatabase().rawQuery(query + " LIMIT " + limit + " OFFSET " + offset, parameters);
            cursor.moveToFirst();

         return cursor;
    }

    //get list of products configured as an expense
    public Cursor getProductsTypeExpense() {

        String query = "SELECT  prod_id, prod_name, prod_expense FROM " +
                table_name +
                " WHERE prod_expense = 'true'";

        Cursor cursor = DBManager.getDatabase().rawQuery(query, null);
        cursor.moveToFirst();

        return cursor;

    }

    public Product getUPCProducts(String value, boolean includeSearchById) {
        Cursor cursor = null;
        try {
            String byIdCondition = "";
            if (value == null) {
                value = "";
            }
            if (includeSearchById) {
                byIdCondition = " OR p.prod_id = '" + value + "' ";
            }
            if (value.indexOf('\n') >= 0)
                value = new StringBuffer(value).deleteCharAt(value.indexOf('\n')).toString();
            if (value.indexOf('\r') >= 0)
                value = new StringBuffer(value).deleteCharAt(value.indexOf('\r')).toString();
            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            String query;
            Product product = new Product();

            String priceLevelID;
            if (myPref.isCustSelected())
                priceLevelID = myPref.getCustPriceLevel();
            else {
                AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();
                priceLevelID = StringUtil.nullStringToEmpty(assignEmployee.getPricelevelId());
            }

            sb.append(
                    "SELECT  p.prod_id as '_id',p.prod_price as 'master_price',p.prod_upc as 'prod_upc'," +
                            "p.prod_prices_group_id as prod_prices_group_id, vp.price as 'volume_price', " +
                            "ch.over_price_net as 'chain_price',");
            sb.append(
                    "CASE WHEN pl.pricelevel_type = 'FixedPercentage' THEN " +
                            "(p.prod_price+(p.prod_price*(pl.pricelevel_fixedpct/100))) ");
            sb.append(
                    "ELSE pli.pricelevel_price END AS 'pricelevel_price',p.prod_price_points as 'prod_price_points'" +
                            ",p.prod_value_points as 'prod_value_points'," +
                            "p.prod_name as 'prod_name',p.prod_desc as 'prod_desc',p.prod_extradesc as 'prod_extradesc'" +
                            ",p.prod_onhand as 'master_prod_onhand'," +
                            "ei.prod_onhand as 'local_prod_onhand',i.prod_img_name as 'prod_img_name'," +
                            "CASE WHEN p.prod_taxcode='' THEN '0' " +
                            "ELSE IFNULL(s.taxcode_istaxable,'1')  END AS 'prod_istaxable' ");
            sb.append(",p.prod_taxcode as 'prod_taxcode',p.prod_taxtype as 'prod_taxtype', p.prod_type as 'prod_type'" +
                    ",c.cat_id as 'cat_id', c.cat_name as 'cat_name' ");

            if (myPref.isCustSelected() && myPref.getPreferences(MyPreferences.pref_filter_products_by_customer)) {

                if (Global.isConsignment) {
                    sb.append(",ci.qty AS 'consignment_qty' ");

                    sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                    if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                        sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                                .append("' WHERE p.prod_type != 'Discount' ");
                    else
                        sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                                .append("' AND p.prod_type != 'Discount' ");
                    if (Global.consignmentType == Global.OrderType.ORDER)
                        sb.append("AND ci.qty>0 ");
                } else
                    sb2.append(
                            "WHERE p.prod_type != 'Discount' AND ch.cust_chain = ? ");
            } else {
                sb2.append("AND ch.cust_chain = ? ");
                if (Global.isConsignment) {
                    sb.append(",ci.qty AS 'consignment_qty' ");

                    sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                    if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                        sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                                .append("' WHERE p.prod_type != 'Discount' ");
                    else
                        sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                                .append("' AND p.prod_type != 'Discount' ");
                    if (Global.consignmentType == Global.OrderType.ORDER)
                        sb2.append("AND ci.qty>0 ");
                } else
                    sb2.append("WHERE p.prod_type != 'Discount' " +
                            " AND (i.type = 'I' OR i.type is NULL )" +
                            " AND ( vp.minQty is NULL OR vp.maxQty is NULL OR ('1' BETWEEN vp.minQty AND vp.maxQty)) "
                    );

            }

            if (myPref.getPreferences(MyPreferences.pref_enable_multi_category)) {
                sb.append(
                        "FROM " +
                                "(select p.* " +
                                "from products p " +
                                "LEFT JOIN ProductAliases pa ON p.prod_id = pa.prod_id " +
                                "where prod_type != 'Discount' AND  " +
                                "(prod_sku = '" + value + "' OR pa.prod_alias = '" + value +
                                "' OR prod_upc = '" + value + "'" + byIdCondition + " )) p " +
                                "LEFT JOIN ProdCatXref xr ON p.prod_id = xr.prod_id  " +
                                "LEFT JOIN Categories c ON c.cat_id = xr.cat_id " +
                                "LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id " +
                                "LEFT OUTER JOIN VolumePrices vp ON p.prod_id = vp.prod_id AND '1' " +
                                "BETWEEN vp.minQty AND vp.maxQty  AND ");
            } else {
                sb.append(
                        "FROM " +
                                "(select p.* " +
                                "from products p " +
                                "LEFT JOIN ProductAliases pa ON p.prod_id = pa.prod_id " +
                                "where prod_type != 'Discount' AND  " +
                                "(prod_sku = '" + value + "'  OR pa.prod_alias = '" + value
                                + "' OR prod_upc = '" + value + "'" + byIdCondition + " )) p " +
                                "LEFT OUTER JOIN Categories c ON c.cat_id = p.cat_id " +
                                "LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id " +
                                "LEFT OUTER JOIN VolumePrices vp ON p.prod_id = vp.prod_id AND '1' " +
                                "BETWEEN vp.minQty AND vp.maxQty  AND ");
            }
            sb.append(
                    "vp.pricelevel_id = ?  " +
                            "LEFT OUTER JOIN PriceLevelItems pli ON p.prod_id = pli.pricelevel_prod_id ");
            sb.append(
                    "AND pli.pricelevel_id = ? " +
                            "LEFT OUTER JOIN PriceLevel pl ON pl.pricelevel_id = ? " +
                            "LEFT OUTER JOIN Products_Images i ON p.prod_id = i.prod_id AND i.type = 'I' ");
            sb.append(
                    "LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id " +
                            "LEFT OUTER JOIN ProductChainXRef ch ON ch.prod_id = p.prod_id ");
            sb.append("LEFT JOIN ProductAliases pa ON p.prod_id = pa.prod_id ");

            sb.append(sb2);

            if (myPref.getPreferences(MyPreferences.pref_group_in_catalog_by_name)) {
                sb.append(" GROUP BY p.prod_name ORDER BY p.prod_name LIMIT 1");
            } else {
                sb.append(" LIMIT 1");
            }

            String[] parameters = new String[]{priceLevelID, priceLevelID, priceLevelID, myPref.getCustID()};
            query = sb.toString();

            cursor = DBManager.getDatabase().rawQuery(query, parameters);

            if (cursor.moveToFirst()) {

                product.setId(cursor.getString(cursor.getColumnIndex("_id")));
                product.setProdName(cursor.getString(cursor.getColumnIndex("prod_name")));

                String temp = cursor.getString(cursor.getColumnIndex("volume_price"));
                if (temp == null || temp.isEmpty()) {
                    temp = cursor.getString(cursor.getColumnIndex("pricelevel_price"));
                    if (temp == null || temp.isEmpty()) {
                        temp = cursor.getString(cursor.getColumnIndex("chain_price"));
                        if (temp == null || temp.isEmpty()) {
                            temp = cursor.getString(cursor.getColumnIndex("master_price"));
                            if (temp == null || temp.isEmpty())
                                temp = "0";
                        }
                    } else {
                        product.setPriceLevelId(priceLevelID);
                    }
                }
                product.setProdPrice(temp);
                product.setProd_upc(cursor.getString(cursor.getColumnIndex(prod_upc)));
                product.setProdDesc(cursor.getString(cursor.getColumnIndex("prod_desc")));

                temp = cursor.getString(cursor.getColumnIndex("local_prod_onhand"));
                if (temp == null || temp.isEmpty()) {
                    temp = cursor.getString(cursor.getColumnIndex("master_prod_onhand"));
                    if (temp == null || temp.isEmpty())
                        temp = "0";
                }
                product.setProdOnHand(temp);
                product.setPricesXGroupid(cursor.getString(cursor.getColumnIndex(ProductsHandler.prod_prices_group_id)));
                product.setProdImgName(cursor.getString(cursor.getColumnIndex("prod_img_name")));
                product.setProdIstaxable(cursor.getString(cursor.getColumnIndex("prod_istaxable")));
                product.setProdType(cursor.getString(cursor.getColumnIndex("prod_type")));
                product.setCatId(cursor.getString(cursor.getColumnIndex("cat_id")));
                product.setCategoryName(cursor.getString(cursor.getColumnIndex("cat_name")));
                product.setProdPricePoints(cursor.getInt(cursor.getColumnIndex("prod_price_points")));
                product.setProdValuePoints(cursor.getInt(cursor.getColumnIndex("prod_value_points")));
                product.setProdTaxType(cursor.getString(cursor.getColumnIndex("prod_taxtype")));
                product.setProdTaxCode(cursor.getString(cursor.getColumnIndex("prod_taxcode")));

            }

            cursor.close();
            return product;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public Product getProductDetails(String _prod_id) {
        Cursor cursor = null;
        try {
            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            String query;
            String priceLevelID;
            if (myPref.isCustSelected())
                priceLevelID = myPref.getCustPriceLevel();
            else {
                AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();
                priceLevelID = StringUtil.nullStringToEmpty(assignEmployee.getPricelevelId());
            }

            sb.append(
                    "SELECT  p.prod_id as '_id',p.isGC as 'isGC', p.prod_sku as 'prod_sku', p.prod_upc as 'prod_upc',p.prod_price as 'master_price'," +
                            "vp.price as 'volume_price', ch.over_price_net as 'chain_price',");
            sb.append(
                    "CASE WHEN pl.pricelevel_type = 'FixedPercentage' THEN (p.prod_price+(p.prod_price*(pl.pricelevel_fixedpct/100))) ");
            sb.append(
                    "ELSE pli.pricelevel_price END AS 'pricelevel_price',p.prod_price_points,p.prod_value_points,p.prod_name,p.prod_desc," +
                            "p.prod_extradesc,p.prod_onhand as 'master_prod_onhand',ei.prod_onhand as 'local_prod_onhand',i.prod_img_name, " +
                            "CASE WHEN p.prod_taxcode='' THEN '0' ELSE IFNULL(s.taxcode_istaxable,'1')  END AS 'prod_istaxable' ");
            sb.append(",p.prod_taxcode,p.prod_taxtype, p.prod_type,p.cat_id, c.cat_name as 'cat_name' ");

            if (myPref.isCustSelected() && myPref.getPreferences(MyPreferences.pref_filter_products_by_customer)) {
                if (Global.isConsignment) {
                    sb.append(",ci.qty AS 'consignment_qty' ");

                    sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                    if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                        sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                                .append("' WHERE p.prod_type != 'Discount' ");
                    else
                        sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                                .append("' AND p.prod_type != 'Discount' ");
                    if (Global.consignmentType == Global.OrderType.ORDER)
                        sb.append("AND ci.qty>0 ");
                    sb2.append("AND p.prod_id = ?");
                } else
                    sb2.append("WHERE p.prod_type != 'Discount' AND ch.cust_chain = ? AND p.prod_id = ? ");// .append("'

            } else {
                sb2.append("AND ch.cust_chain = ? ");
                if (Global.isConsignment) {
                    sb.append(",ci.qty AS 'consignment_qty' ");

                    sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                    if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                        sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                                .append("' WHERE p.prod_type != 'Discount' ");
                    else
                        sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                                .append("' AND p.prod_type != 'Discount' ");
                    if (Global.consignmentType == Global.OrderType.ORDER)
                        sb2.append("AND ci.qty>0 ");
                    sb2.append("AND p.prod_id = ? ");
                } else
                    sb2.append("WHERE p.prod_type != 'Discount' AND p.prod_id = ? ");// ORDER
            }

            sb.append(
                    "FROM Products p LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id LEFT OUTER JOIN VolumePrices vp ON " +
                            "p.prod_id = vp.prod_id AND '1' BETWEEN vp.minQty AND vp.maxQty  AND ");
            sb.append("vp.pricelevel_id = ? LEFT OUTER JOIN PriceLevelItems pli ON p.prod_id = pli.pricelevel_prod_id ");
            sb.append(
                    "AND pli.pricelevel_id = ? LEFT OUTER JOIN PriceLevel pl ON pl.pricelevel_id = ? LEFT OUTER JOIN Products_Images i ON p.prod_id = i.prod_id AND i.type = 'I' ");
            sb.append("LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id " +
                    " LEFT OUTER JOIN Categories c ON c.cat_id = p.cat_id " +
                    " LEFT OUTER JOIN ProductChainXRef ch ON ch.prod_id = p.prod_id ");
            sb.append("LEFT JOIN ProductAliases pa ON p.prod_id = pa.prod_id ");

            sb.append(sb2);

            if (myPref.getPreferences(MyPreferences.pref_group_in_catalog_by_name)) {
                sb.append(" GROUP BY p.prod_name ORDER BY p.prod_name LIMIT 1");
            } else {
                sb.append(" ORDER BY p.prod_name LIMIT 1");
            }

            String[] parameters = new String[]{priceLevelID, priceLevelID, priceLevelID, myPref.getCustID(), _prod_id};
            query = sb.toString();

            cursor = DBManager.getDatabase().rawQuery(query, parameters);
            Product product = new Product();

            if (cursor.moveToFirst()) {
                product.setId(cursor.getString(cursor.getColumnIndex("_id")));
                product.setProdName(cursor.getString(cursor.getColumnIndex("prod_name")));
                String temp = cursor.getString(cursor.getColumnIndex("volume_price"));
                product.setVolumePrice(temp);
                if (temp == null || temp.isEmpty()) {
                    temp = cursor.getString(cursor.getColumnIndex("pricelevel_price"));
                    product.setPriceLevelPrice(temp);
                    if (temp == null || temp.isEmpty()) {
                        temp = cursor.getString(cursor.getColumnIndex("chain_price"));
                        product.setChainPrice(temp);
                        if (temp == null || temp.isEmpty()) {
                            temp = cursor.getString(cursor.getColumnIndex("master_price"));
                            product.setMasterPrice(temp);
                        }
                    } else {
                        product.setPriceLevelId(priceLevelID);
                    }
                }
//            data[2] = temp;
                product.setProdDesc(cursor.getString(cursor.getColumnIndex("prod_desc")));
                temp = cursor.getString(cursor.getColumnIndex("local_prod_onhand"));
                product.setLocalProdOnhand(temp);
                if (temp == null || temp.isEmpty()) {
                    temp = cursor.getString(cursor.getColumnIndex("master_prod_onhand"));
                    product.setMasterProdOnhand(temp);
                    if (temp == null || temp.isEmpty()) {
                        temp = "0";
                        product.setLocalProdOnhand(temp);
                        product.setMasterProdOnhand(temp);
                    }
                }
//            data[4] = temp;

                product.setGC(Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex(isGC))));
                product.setProdImgName(cursor.getString(cursor.getColumnIndex("prod_img_name")));
                product.setProdIstaxable(cursor.getString(cursor.getColumnIndex("prod_istaxable")));
                product.setProdType(cursor.getString(cursor.getColumnIndex("prod_type")));
                product.setCatId(cursor.getString(cursor.getColumnIndex("cat_id")));
                product.setCategoryName(cursor.getString(cursor.getColumnIndex("cat_name")));
                String prod_price_points = cursor.getString(cursor.getColumnIndex("prod_price_points"));
                if (TextUtils.isEmpty(prod_price_points)) {
                    product.setProdPricePoints(0);
                } else {
                    product.setProdPricePoints(Integer.parseInt(prod_price_points));
                }

                String prod_value_points = cursor.getString(cursor.getColumnIndex("prod_value_points"));
                if (TextUtils.isEmpty(prod_value_points)) {
                    product.setProdValuePoints(0);
                } else {
                    product.setProdValuePoints(Integer.parseInt(prod_value_points));
                }
                product.setProdTaxType(cursor.getString(cursor.getColumnIndex("prod_taxtype")));
                product.setProdTaxCode(cursor.getString(cursor.getColumnIndex("prod_taxcode")));
                product.setProd_sku(cursor.getString(cursor.getColumnIndex("prod_sku")));
                product.setProd_upc(cursor.getString(cursor.getColumnIndex("prod_upc")));
            }
            cursor.close();
            return product;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public List<String> getProdInformation(String id) {
        Cursor cursor = null;
        try {
            List<String> list = new ArrayList<>();
            String sb = "SELECT p.prod_name, p.prod_desc, p.prod_extradesc, p.prod_type, t.taxcode_name as 'prod_taxcode', p.prod_price, p.prod_disc_type " +
                    ",p.prod_price_points,p.prod_value_points FROM Products p LEFT OUTER JOIN SalesTaxCodes t ON p.prod_taxcode = t.taxcode_id WHERE p.prod_id = ?";
            cursor = DBManager.getDatabase().rawQuery(sb, new String[]{id});
            DecimalFormat frmt = new DecimalFormat("0.00");
            if (cursor.moveToFirst()) {
                do {
                    String data = cursor.getString(cursor.getColumnIndex(prod_name));
                    list.add(data);
                    data = cursor.getString(cursor.getColumnIndex(prod_desc));
                    list.add(data);
                    data = cursor.getString(cursor.getColumnIndex(prod_extradesc));
                    list.add(data);
                    data = cursor.getString(cursor.getColumnIndex(prod_type));
                    list.add(data);
                    data = cursor.getString(cursor.getColumnIndex(prod_taxcode));
                    list.add(data);

                    data = cursor.getString(cursor.getColumnIndex(prod_price));
                    if (data.isEmpty())
                        list.add("$0.00");
                    else {
                        data = frmt.format(Double.parseDouble(data));
                        list.add("$" + data);
                    }

                    data = cursor.getString(cursor.getColumnIndex(prod_disc_type));
                    list.add(data);

                    data = cursor.getString(cursor.getColumnIndex(prod_price_points));
                    list.add(data);

                    data = cursor.getString(cursor.getColumnIndex(prod_value_points));
                    list.add(data);

                } while (cursor.moveToNext());
            }
            cursor.close();
            return list;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public List<String> getProdInventory(String id) {
        Cursor cursor = null;
        try {
            List<String> list = new ArrayList<>();
            cursor = DBManager.getDatabase().rawQuery("SELECT p.prod_onhand AS 'master_prod_onhand',ei.prod_onhand AS 'local_prod_onhand' FROM Products p LEFT OUTER JOIN " + "EmpInv ei ON p.prod_id = ei.prod_id WHERE p.prod_id = ?", new String[]{id});
            String master, local;
            if (cursor.moveToFirst()) {
                do {

                    master = cursor.getString(cursor.getColumnIndex("master_prod_onhand"));
                    local = cursor.getString(cursor.getColumnIndex("local_prod_onhand"));
                    if (local == null || local.isEmpty()) {
                        if (master == null || master.isEmpty())
                            master = "0";
                        list.add(master);
                    } else
                        list.add(local);

                } while (cursor.moveToNext());
            }

            cursor.close();
            return list;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public List<String> getProdIdentification(String id) {
        Cursor cursor = null;
        try {
            List<String> list = new ArrayList<>();
            String[] fields = new String[]{prod_sku, cat_id, prod_upc};
            String[] arguments = new String[]{id};
            cursor = DBManager.getDatabase().query(true, table_name, fields, "prod_id=?", arguments, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String data = cursor.getString(cursor.getColumnIndex(prod_sku));
                    list.add(data);

                    data = cursor.getString(cursor.getColumnIndex(cat_id));
                    list.add(data);

                    data = cursor.getString(cursor.getColumnIndex(prod_upc));
                    list.add(data);

                } while (cursor.moveToNext());
            }
            cursor.close();
            return list;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public List<Discount> getDiscounts() {
        Cursor cursor = null;
        try {
            List<Discount> list = new ArrayList<>();
            Discount data = new Discount();
            cursor = DBManager.getDatabase().rawQuery("SELECT p.prod_name,p.prod_disc_type,p.prod_price,IFNULL(s.taxcode_istaxable,1) as 'taxcode_istaxable'" + ",p.prod_id FROM Products p LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id WHERE p.prod_type = 'Discount' ORDER BY p.prod_name ASC", null);
            if (cursor.moveToFirst()) {
                do {
                    data.setProductName(cursor.getString(cursor.getColumnIndex(prod_name)));
                    data.setProductDiscountType(cursor.getString(cursor.getColumnIndex(prod_disc_type)));
                    data.setProductPrice(cursor.getString(cursor.getColumnIndex(prod_price)));
                    data.setTaxCodeIsTaxable(cursor.getString(cursor.getColumnIndex("taxcode_istaxable")));
                    data.setProductId(cursor.getString(cursor.getColumnIndex(prod_id)));
                    list.add(data);
                    data = new Discount();
                } while (cursor.moveToNext());
            }

            cursor.close();
            return list;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public Discount getDiscounts(String discount_id) {
        Cursor cursor = null;
        try {
            Discount data = new Discount();
            cursor = DBManager.getDatabase().rawQuery("SELECT p.prod_name,p.prod_disc_type,p.prod_price," +
                            "IFNULL(s.taxcode_istaxable,1) as 'taxcode_istaxable'" + ",p.prod_id " +
                            "FROM Products p " +
                            "LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id " +
                            "WHERE p.prod_type = 'Discount' AND prod_id LIKE ? " +
                            "ORDER BY p.prod_name ASC",
                    new String[]{StringUtil.nullStringToEmpty(discount_id)});
            if (cursor.moveToFirst()) {
                data.setProductName(cursor.getString(cursor.getColumnIndex(prod_name)));
                data.setProductDiscountType(cursor.getString(cursor.getColumnIndex(prod_disc_type)));
                data.setProductPrice(cursor.getString(cursor.getColumnIndex(prod_price)));
                data.setTaxCodeIsTaxable(cursor.getString(cursor.getColumnIndex("taxcode_istaxable")));
                data.setProductId(cursor.getString(cursor.getColumnIndex(prod_id)));
            }
            cursor.close();
            return data;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public HashMap<String, String> getDiscountDetail(String discount_id) {
        Cursor c = null;
        try {
            if (discount_id == null)
                discount_id = "";
            HashMap<String, String> map = new HashMap<>();

            c = DBManager.getDatabase().rawQuery("SELECT prod_disc_type,prod_price FROM Products WHERE prod_id LIKE ?", new String[]{discount_id});
            if (c.moveToFirst()) {
                map.put("discount_type", c.getString(c.getColumnIndex(prod_disc_type)));
                map.put("discount_price", c.getString(c.getColumnIndex(prod_price)));
            }

            c.close();
            return map;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public String getDiscountName(String discountId) {
        String discountName = "";
        Cursor c = null;
        try {
            if (discountId != null && !discountId.isEmpty()) {
                c = DBManager.getDatabase().rawQuery("SELECT prod_name FROM Products WHERE prod_id = ?", new String[]{discountId});
                if (c.moveToFirst()) {
                    discountName = c.getString(c.getColumnIndex(prod_name));
                }
                c.close();
            }
            return discountName;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public Cursor searchProducts(String search, String type) // Transactions
    // Receipts
    // first
    // listview
    {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        String priceLevelID;
        if (myPref.isCustSelected())
            priceLevelID = myPref.getCustPriceLevel();
        else {
            AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();
            priceLevelID = StringUtil.nullStringToEmpty(assignEmployee.getPricelevelId());
        }
        search = search.replace("'", "''");
        sb.append(
                "SELECT DISTINCT CASE WHEN  p.prod_name LIKE '" + search + "%' THEN '0' ELSE '1' END as weight, " +
                        "p.prod_id as '_id', p.prod_prices_group_id as 'prod_prices_group_id', p.prod_price as 'master_price'," +
                        "vp.price as 'volume_price', ch.over_price_net as 'chain_price', p.prod_sku as prod_sku, p.prod_upc as prod_upc, ");
        sb.append(
                "CASE WHEN pl.pricelevel_type = 'FixedPercentage' THEN (p.prod_price+(p.prod_price*(pl.pricelevel_fixedpct/100))) ");
        sb.append(
                "ELSE pli.pricelevel_price END AS 'pricelevel_price',p.prod_price_points,p.prod_value_points,p.prod_name, p.isGC as 'isGC', " +
                        "p.prod_desc,p.prod_extradesc,p.prod_onhand as 'master_prod_onhand',ei.prod_onhand as 'local_prod_onhand',i.prod_img_name, CASE WHEN p.prod_taxcode='' THEN '0' ELSE IFNULL(s.taxcode_istaxable,'1')  END AS 'prod_istaxable' ");
        sb.append(",p.prod_taxcode,p.prod_taxtype,p.prod_type,c.cat_id,c.cat_name as 'cat_name' ");

        if (myPref.isCustSelected() && myPref.getPreferences(MyPreferences.pref_filter_products_by_customer)) {

            if (Global.isConsignment) {
                sb.append(",ci.qty AS 'consignment_qty' ");

                sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                    sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                            .append("' WHERE p.prod_type != 'Discount' ");
                else
                    sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                            .append("' AND p.prod_type != 'Discount' ");
                if (Global.consignmentType == Global.OrderType.ORDER)
                    sb.append("AND ci.qty>0 ");
                sb2.append("AND (p.");
            } else
                sb2.append("WHERE p.prod_type != 'Discount' AND ch.cust_chain = ? AND (p.");

        } else {
            sb2.append("AND ch.cust_chain = ? ");
            if (Global.isConsignment) {
                sb.append(",ci.qty AS 'consignment_qty' ");

                sb2.append("LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id ");
                if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP)
                    sb2.append("AND ci.cust_id = '").append(myPref.getCustID())
                            .append("' WHERE p.prod_type != 'Discount' ");
                else
                    sb2.append("WHERE ci.cust_id = '").append(myPref.getCustID())
                            .append("' AND p.prod_type != 'Discount' ");
                if (Global.consignmentType == Global.OrderType.ORDER)
                    sb2.append("AND ci.qty>0 ");
                sb2.append("AND (p.");
            } else
                sb2.append("WHERE p.prod_type != 'Discount' AND (p.");// ORDER BY
        }

        if (myPref.getPreferences(MyPreferences.pref_enable_multi_category)) {
            sb.append(
                    "FROM Products p " +
                            "LEFT JOIN ProdCatXref xr ON p.prod_id = xr.prod_id  " +
                            "INNER JOIN Categories c ON c.cat_id = xr.cat_id " +
                            "LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id " +
                            "LEFT OUTER JOIN VolumePrices vp ON p.prod_id = vp.prod_id AND '1' " +
                            "BETWEEN vp.minQty AND vp.maxQty  AND ");
        } else {
            sb.append(
                    "FROM Products p " +
                            "LEFT OUTER JOIN Categories c ON c.cat_id = p.cat_id " +
                            "LEFT OUTER JOIN EmpInv ei ON ei.prod_id = p.prod_id " +
                            "LEFT OUTER JOIN VolumePrices vp ON p.prod_id = vp.prod_id AND '1' " +
                            "BETWEEN vp.minQty AND vp.maxQty  AND ");
        }
        sb.append(
                "vp.pricelevel_id = ?  " +
                        "LEFT OUTER JOIN PriceLevelItems pli ON p.prod_id = pli.pricelevel_prod_id ");
        sb.append(
                "AND pli.pricelevel_id = ? " +
                        "LEFT OUTER JOIN PriceLevel pl ON pl.pricelevel_id = ? " +
                        "LEFT OUTER JOIN Products_Images i ON p.prod_id = i.prod_id AND i.type = 'I' ");
        sb.append(
                "LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id " +
                        "LEFT JOIN ProductAliases pa ON p.prod_id = pa.prod_id " +
                        "LEFT OUTER JOIN ProductChainXRef ch ON ch.prod_id = p.prod_id ");
        sb.append(sb2);

        String subquery1 = sb.toString();
        StringBuilder subquery2 = new StringBuilder();

        String[] parameters;
        String like = "%" + search + "%";
        if (type.equalsIgnoreCase(prod_upc)) {
            subquery2.append(" LIKE ?  OR pa.prod_alias LIKE ? OR p.prod_sku LIKE ?)");
            parameters = new String[]{priceLevelID, priceLevelID, priceLevelID,
                    myPref.getCustID(), like, like, like};
        } else {
            subquery2.append(" LIKE ? )");
            parameters = new String[]{priceLevelID, priceLevelID, priceLevelID,
                    myPref.getCustID(), like};
        }

        if (myPref.getPreferences(MyPreferences.pref_group_in_catalog_by_name)) {
            subquery2.append(" GROUP BY p.prod_name ORDER BY weight, p.prod_name");
        } else {
            subquery2.append(" ORDER BY weight, p.prod_name");
        }

        sb.setLength(0);
        sb.append(subquery1).append(type).append(subquery2.toString());
        Cursor cursor = DBManager.getDatabase().rawQuery(sb.toString(), parameters);
        cursor.moveToFirst();
        return cursor;
    }

    public String[] getDiscount(String discountID, String prodPrice) {
        Cursor c = null;
        try {
            c = DBManager.getDatabase().rawQuery("SELECT p.prod_price,p.prod_disc_type, p.prod_sku as prod_sku, p.prod_upc as prod_upc, CASE WHEN p.prod_disc_type = 'Fixed' THEN (" + prodPrice + "-p.prod_price) ELSE ROUND((" + prodPrice + "-(" + prodPrice + "*CAST(p.prod_price AS REAL)/100)),2) END AS discount, " + "CASE WHEN p.prod_disc_type = 'Fixed' THEN (p.prod_price) ELSE ROUND(((" + prodPrice + "*CAST(p.prod_price AS REAL)/100)),2) END AS discAmount,CASE WHEN p.prod_taxcode='' THEN '0' ELSE IFNULL(s.taxcode_istaxable,'1')  END AS 'prod_istaxable' " + "FROM Products p LEFT OUTER JOIN SalesTaxCodes s ON p.prod_taxcode = s.taxcode_id WHERE prod_id = '" + discountID + "'", null);
            String[] values = new String[5];
            if (c.moveToFirst()) {
                values[0] = c.getString(c.getColumnIndex("prod_price"));
                values[1] = c.getString(c.getColumnIndex("prod_disc_type"));
                values[2] = c.getString(c.getColumnIndex("discount"));
                values[3] = c.getString(c.getColumnIndex("prod_istaxable")); // 0
                // for
                // not
                // and
                // 1
                // for
                // yes
                values[4] = c.getString(c.getColumnIndex("discAmount"));
            }

            c.close();
            return values;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public HashMap<String, String> getProductMap(String prodID, boolean isConsignment) {
        Cursor cursor = null;
        try {
            StringBuilder sb = new StringBuilder();
            String priceLevelID;
            if (myPref.isCustSelected())
                priceLevelID = myPref.getCustPriceLevel();
            else {
                AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();
                priceLevelID = StringUtil.nullStringToEmpty(assignEmployee.getPricelevelId());
            }

            sb.append(
                    "SELECT  p.prod_id as '_id',ci.price as 'inventory_price', p.prod_price as 'master_price',vp.price as 'volume_price', p.prod_sku as prod_sku, p.prod_upc as prod_upc, ch.over_price_net as 'chain_price',");
            sb.append(
                    "CASE WHEN pl.pricelevel_type = 'FixedPercentage' THEN (p.prod_price+(p.prod_price*(pl.pricelevel_fixedpct/100))) ");
            sb.append(
                    "ELSE pli.pricelevel_price END AS 'pricelevel_price',p.prod_price_points,p.prod_value_points,p.prod_name,p.prod_desc,p.prod_extradesc ");
            sb.append(
                    "FROM Products p  LEFT OUTER JOIN VolumePrices vp ON p.prod_id = vp.prod_id AND '1' BETWEEN vp.minQty AND vp.maxQty  AND ");
            sb.append("vp.pricelevel_id = ? LEFT OUTER JOIN PriceLevelItems pli ON p.prod_id = pli.pricelevel_prod_id ");
            sb.append("AND pli.pricelevel_id = ? LEFT OUTER JOIN PriceLevel pl ON pl.pricelevel_id = ? ");
            sb.append("LEFT OUTER JOIN ProductChainXRef ch ON ch.prod_id = p.prod_id AND ch.cust_chain = ? ");
            sb.append(
                    "LEFT OUTER JOIN CustomerInventory ci ON ci.prod_id = p.prod_id AND ci.cust_id = ? WHERE p.prod_id = ?");

            cursor = DBManager.getDatabase().rawQuery(sb.toString(), new String[]{priceLevelID, priceLevelID, priceLevelID,
                    myPref.getCustID(), myPref.getCustID(), prodID});
            String tempPrice = "";
            HashMap<String, String> map = new HashMap<>();
            if (cursor.moveToFirst()) {

                if (isConsignment)
                    tempPrice = cursor.getString(cursor.getColumnIndex("inventory_price"));

                if (tempPrice == null || tempPrice.isEmpty()) {
                    tempPrice = cursor.getString(cursor.getColumnIndex("volume_price"));

                    if (tempPrice == null || tempPrice.isEmpty()) {
                        tempPrice = cursor.getString(cursor.getColumnIndex("pricelevel_price"));
                        if (tempPrice == null || tempPrice.isEmpty()) {
                            tempPrice = cursor.getString(cursor.getColumnIndex("chain_price"));

                            if (tempPrice == null || tempPrice.isEmpty()) {
                                tempPrice = cursor.getString(cursor.getColumnIndex("master_price"));
                                if (tempPrice == null || tempPrice.isEmpty())
                                    tempPrice = "0";
                            }
                        }
                    }
                }
                map.put("prod_price", tempPrice);

                map.put(prod_name, cursor.getString(cursor.getColumnIndex(prod_name)));
                map.put(prod_desc, cursor.getString(cursor.getColumnIndex(prod_desc)));
            }

            cursor.close();
            return map;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }

        }
    }

    public String getProductPrice(String prodID) {
        Cursor c = null;
        try {
            c = DBManager.getDatabase().rawQuery("SELECT prod_price FROM Products WHERE prod_id = ?", new String[]{prodID});
            String price = "0";
            if (c.moveToFirst())
                price = c.getString(c.getColumnIndex("prod_price"));

            if (price.isEmpty())
                price = "0";
            c.close();
            return price;
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
    }

    public void updateProductOnHandQty(String prodID, double qty) {
        StringBuilder sb = new StringBuilder();
        sb.append(prod_id).append(" = ?");

        ContentValues args = new ContentValues();

        args.put(prod_onhand, Double.toString(qty));
        if (DBManager.getDatabase().update("EmpInv", args, sb.toString(), new String[]{prodID}) == 0)
            DBManager.getDatabase().update(table_name, args, sb.toString(), new String[]{prodID});
    }

    public enum SearchField {PRODUCT_UPC, PRODUCT_ID}
}
