package com.android.support;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.dao.AssignEmployeeDAO;
import com.android.dao.ClerkDAO;
import com.android.dao.CustomerCustomFieldsDAO;
import com.android.dao.DeviceTableDAO;
import com.android.dao.DinningTableDAO;
import com.android.dao.EmobileBiometricDAO;
import com.android.dao.EmployeePermissionDAO;
import com.android.dao.MixMatchDAO;
import com.android.dao.OrderAttributesDAO;
import com.android.dao.OrderProductAttributeDAO;
import com.android.dao.ShiftDAO;
import com.android.dao.TermsNConditionsDAO;
import com.android.dao.UomDAO;
import com.android.database.ConsignmentTransactionHandler;
import com.android.database.CustomerInventoryHandler;
import com.android.database.CustomersHandler;
import com.android.database.DBManager;
import com.android.database.OrderProductsHandler;
import com.android.database.OrdersHandler;
import com.android.database.PayMethodsHandler;
import com.android.database.PaymentsHandler;
import com.android.database.PaymentsXML_DB;
import com.android.database.PriceLevelHandler;
import com.android.database.PriceLevelItemsHandler;
import com.android.database.ProductAddonsHandler;
import com.android.database.ProductAliases_DB;
import com.android.database.ProductsHandler;
import com.android.database.TemplateHandler;
import com.android.database.TimeClockHandler;
import com.android.database.TransferLocations_DB;
import com.android.database.VoidTransactionsHandler;
import com.android.emobilepos.BuildConfig;
import com.android.emobilepos.OnHoldActivity;
import com.android.emobilepos.R;
import com.android.emobilepos.mainmenu.SyncTab_FR;
import com.android.emobilepos.models.ItemPriceLevel;
import com.android.emobilepos.models.PriceLevel;
import com.android.emobilepos.models.Product;
import com.android.emobilepos.models.ProductAddons;
import com.android.emobilepos.models.ProductAlias;
import com.android.emobilepos.models.orders.Order;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.realms.AssignEmployee;
import com.android.emobilepos.models.realms.Clerk;
import com.android.emobilepos.models.realms.CustomerCustomField;
import com.android.emobilepos.models.realms.DinningTable;
import com.android.emobilepos.models.realms.EmobileBiometric;
import com.android.emobilepos.models.realms.MixMatch;
import com.android.emobilepos.models.realms.OrderAttributes;
import com.android.emobilepos.models.realms.PaymentMethod;
import com.android.emobilepos.models.realms.Shift;
import com.android.emobilepos.models.response.ClerkEmployeePermissionResponse;
import com.android.emobilepos.models.salesassociates.DinningLocationConfiguration;
import com.android.emobilepos.ordering.OrderingMain_FA;
import com.android.saxhandler.SAXParserPost;
import com.android.saxhandler.SAXPostHandler;
import com.android.saxhandler.SAXPostTemplates;
import com.android.saxhandler.SAXProcessCardPayHandler;
import com.android.saxhandler.SAXSendConsignmentTransaction;
import com.android.saxhandler.SAXSendCustomerInventory;
import com.android.saxhandler.SAXSendInventoryTransfer;
import com.android.saxhandler.SAXSyncNewCustomerHandler;
import com.android.saxhandler.SAXSyncPayPostHandler;
import com.android.saxhandler.SAXSyncVoidTransHandler;
import com.android.saxhandler.SAXSynchHandler;
import com.android.saxhandler.SAXSynchOrdPostHandler;
import com.android.saxhandler.SaxLoginHandler;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import io.realm.Realm;
import oauthclient.OAuthClient;
import oauthclient.OAuthManager;
import util.json.JsonUtils;

public class SynchMethods {
    MyPreferences preferences;
    private Post post;
    private Context context;
    private String xml;
    private InputSource inSource;
    private SAXParser sp;
    private XMLReader xr;
    private List<String[]> data;
    private String tempFilePath;
    private boolean checkoutOnHold = false, downloadHoldList = false;
    //    private ProgressDialog myProgressDialog;
    private int type;
    private boolean didSendData = true;
    private boolean isFromMainMenu = false;
    private Intent onHoldIntent;
    private HttpClient client;
    private Gson gson = JsonUtils.getInstance();
    private boolean isReceive = false;
    private boolean isSending = false;
    private String _server_time = "";

    public SynchMethods(DBManager managerInst) {
        context = managerInst.getContext();
        post = new Post(context);
        client = new HttpClient();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        data = new ArrayList<>();
        preferences = new MyPreferences(context);
        if (OAuthManager.isExpired(context) && NetworkUtils.isConnectedToInternet(context)) {
            OAuthManager oAuthManager = getOAuthManager(context);
            try {
                oAuthManager.requestToken();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tempFilePath = context.getApplicationContext().getFilesDir().getAbsolutePath() + "/temp.xml";
        try {
            sp = spf.newSAXParser();
            xr = sp.getXMLReader();
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        }
    }

    private static OAuthManager getOAuthManager(Context activity) {
        MyPreferences preferences = new MyPreferences(activity);
        return OAuthManager.getInstance(activity, preferences.getAcctNumber(), preferences.getAcctPassword());
    }


    public static void syncBiometrics(Context context) {
        if (OAuthManager.isExpired(context)) {
            getOAuthManager(context);
        }
        StringBuilder url = new StringBuilder(context.getString(R.string.sync_enablermobile_biometrics));
        OAuthClient authClient = OAuthManager.getOAuthClient(context);
        oauthclient.HttpClient httpClient = new oauthclient.HttpClient();
        try {
            String response = httpClient.getString(url.toString(), authClient);
            Type listType = new com.google.gson.reflect.TypeToken<List<EmobileBiometric>>() {
            }.getType();
            Gson gson = JsonUtils.getInstance();
            List<EmobileBiometric> emobileBiometrics = gson.fromJson(response, listType);
            EmobileBiometricDAO.truncate();
            EmobileBiometricDAO.upsert(emobileBiometrics);

        } catch (IOException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }
    public static void postSalesAssociatesConfiguration(Activity activity, List<Clerk> clerks) throws Exception {
        List<DinningLocationConfiguration> configurations = new ArrayList<>();

        HashMap<String, List<Clerk>> locations = ClerkDAO.getSalesAssociatesByLocation();
        for (Map.Entry<String, List<Clerk>> location : locations.entrySet()) {
            DinningLocationConfiguration configuration = new DinningLocationConfiguration();
            configuration.setLocationId(location.getKey());
            configuration.setClerks(location.getValue());
            configurations.add(configuration);
        }
        AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee(false);

        MyPreferences preferences = new MyPreferences(activity);
        StringBuilder url = new StringBuilder(activity.getString(R.string.sync_enablermobile_mesasconfig));
        url.append("/").append(URLEncoder.encode(String.valueOf(assignEmployee.getEmpId()), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getDeviceID(), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getActivKey(), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getBundleVersion(), GenerateXML.UTF_8));

        if (OAuthManager.isExpired(activity)) {
            getOAuthManager(activity);
        }
        OAuthClient authClient = OAuthManager.getOAuthClient(activity);
        Gson gson = JsonUtils.getInstance();
        String json = gson.toJson(configurations);
        oauthclient.HttpClient httpClient = new oauthclient.HttpClient();
        httpClient.post(url.toString(), json, authClient);
    }
    public static void postEmobileBiometrics(Context context) throws Exception {
        List<EmobileBiometric> emobileBiometrics = EmobileBiometricDAO.getBiometrics();
        AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee(false);
        MyPreferences preferences = new MyPreferences(context);
        StringBuilder url = new StringBuilder(context.getString(R.string.sync_enablermobile_biometrics));
        url.append("/").append(URLEncoder.encode(String.valueOf(assignEmployee.getEmpId()), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getDeviceID(), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getActivKey(), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getBundleVersion(), GenerateXML.UTF_8));

        if (OAuthManager.isExpired(context)) {
            getOAuthManager(context);
        }
        OAuthClient authClient = OAuthManager.getOAuthClient(context);
        Gson gson = JsonUtils.getInstance();
        String json = gson.toJson(emobileBiometrics);
        oauthclient.HttpClient httpClient = new oauthclient.HttpClient();
        String response = httpClient.post(url.toString(), json, authClient);
    }
    public static void synchSalesAssociateDinnindTablesConfiguration(Context activity) throws IOException, SAXException {
        oauthclient.HttpClient client = new oauthclient.HttpClient();
        Gson gson = JsonUtils.getInstance();
        if (OAuthManager.isExpired(activity)) {
            getOAuthManager(activity);
        }
        OAuthClient oauthClient = OAuthManager.getOAuthClient(activity);
//            String s = client.getString(activity.getString(R.string.sync_enablermobile_mesasconfig), oauthClient);
        InputStream inputStream = client.get(activity.getString(R.string.sync_enablermobile_mesasconfig), oauthClient);
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<DinningLocationConfiguration> configurations = new ArrayList<>();
        reader.beginArray();
        String defaultLocation = AssignEmployeeDAO.getAssignEmployee(false).getDefaultLocation();
        while (reader.hasNext()) {
            DinningLocationConfiguration configuration = gson.fromJson(reader, DinningLocationConfiguration.class);
            if (configuration.getLocationId().equalsIgnoreCase(defaultLocation)) {
                configurations.add(configuration);
            }
        }

        reader.endArray();
        reader.close();
        for (DinningLocationConfiguration configuration : configurations) {
            for (Clerk associate : configuration.getClerks()) {
                ClerkDAO.clearAllAssignedTable(associate);
                for (DinningTable table : associate.getAssignedDinningTables()) {
                    DinningTable dinningTable = DinningTableDAO.getById(table.getId());
                    if (dinningTable != null) {
                        ClerkDAO.addAssignedTable(associate, dinningTable);
                    }
                }
            }
        }
    }

    public static String getTagValue(String xml, String tagName) {
        return xml.split("<" + tagName + ">")[1].split("</" + tagName + ">")[0];
    }


    public static void synchOrdersOnHoldList(Context context) throws SAXException, IOException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        String json = new HttpClient().httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("GetOrdersOnHoldList"));
        Type listType = new com.google.gson.reflect.TypeToken<List<Order>>() {
        }.getType();
        List<Order> orders = gson.fromJson(json, listType);
        OrdersHandler ordersHandler = new OrdersHandler(context);
        List<Order> ordersToDelete = ordersHandler.getOrdersOnHold();
        int i = 0;
        for (Order order : orders) {
            order.ord_issync = "1";
            order.isOnHold = "1";
            Order onHoldOrder = ordersHandler.getOrder(order.ord_id);
            if (onHoldOrder == null || TextUtils.isEmpty(onHoldOrder.ord_id) || onHoldOrder.isOnHold.equals("1")) {
                ordersToDelete.remove(order);
                i++;
            }
            if (i == 1000) {
                ordersHandler.insert(orders);
                orders.clear();
                i = 0;
            }
            synchOrdersOnHoldDetails(context, order.ord_id);
        }
        ordersHandler.insert(orders);
        ordersHandler.deleteOnHoldsOrders(ordersToDelete);
    }

    public static void synchOrdersOnHoldDetails(Context activity, String ordID) throws SAXException, IOException {
        HttpClient client = new HttpClient();
        Gson gson = JsonUtils.getInstance();
        List<OrderProduct> orderProducts = new ArrayList<>();
        GenerateXML xml = new GenerateXML(activity);
        String json = client.httpJsonRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.getOnHold(Global.S_ORDERS_ON_HOLD_DETAILS, ordID));
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String seatGroupId = jsonObject.optString("seatGroupId");
                if (TextUtils.isEmpty(seatGroupId)) {
                    jsonObject.put("seatGroupId", "0");
                }
                orderProducts.add(gson.fromJson(jsonObject.toString(), OrderProduct.class));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Type listType = new com.google.gson.reflect.TypeToken<List<OrderProduct>>() {
//        }.getType();
//        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
//        List<OrderProduct> orderProducts = gson.fromJson(jsonArray, listType);
        OrderProductsHandler orderProductsHandler = new OrderProductsHandler(activity);
//        reader.beginArray();
//        int i = 0;
        ProductsHandler productsHandler = new ProductsHandler(activity);
        for (OrderProduct product : orderProducts) {
//            while (reader.hasNext()) {
            double discAmount = 0;
//            OrderProduct product = gson.fromJson(reader, OrderProduct.class);
            double total = (Double.parseDouble(product.getOrdprod_qty())) * Double.parseDouble(product.getFinalPrice());
            String[] discountInfo = productsHandler.getDiscount(product.getDiscount_id(), product.getFinalPrice());
            if (discountInfo != null) {
                if (discountInfo[1] != null && discountInfo[1].equals("Fixed")) {
                    product.setDiscount_is_fixed("1");
                }
                if (discountInfo[2] != null) {
                    discAmount = Double.parseDouble(discountInfo[4]);
                }
                if (discountInfo[3] != null) {
                    product.setDiscount_is_taxable(discountInfo[3]);
                }
                if (discountInfo[4] != null) {
                    product.setDisTotal(discountInfo[4]);
                    discAmount = Double.parseDouble(discountInfo[4]);
                    product.setDiscount_value(discountInfo[4]);
                }
            }
            product.setDisAmount(String.valueOf(discAmount));
            product.setItemTotal(Double.toString(total - discAmount));
//                product.setItemSubtotal(Double.toString(total));
//                orderProducts.add(product);
//                i++;
//                if (i == 1000) {
//                    OrderProductUtils.assignAddonsOrderProduct(orderProducts);
//                    orderProductsHandler.insert(orderProducts);
//                    orderProducts.clear();
//                    i = 0;
//                }
        }
        OrderProductUtils.assignAddonsOrderProduct(orderProducts);
        orderProductsHandler.completeProductFields(orderProducts, activity);
        orderProductsHandler.insert(orderProducts);
//            reader.endArray();
//            reader.close();
    }

    public boolean syncReceive() {
        try {
            synchGetServerTime();
            synchEmployeeData();
            synchAddresses();
            synchCategories();
            synchCustomers();
            synchEmpInv();
            synchProdInv();
            synchInvoices();
            synchPaymentMethods();
            synchPriceLevel();
            synchCustomerCustomFields();
            synchItemsPriceLevel();
            synchPrinters();
            synchProdCatXref();
            synchProdChain();
            synchProdAddon();
            synchProducts();
            synchOrderAttributes();
            synchOrderAttributes();
            synchProductAliases();
            synchProductImages();
            synchDownloadProductsAttr();
            synchGetOrdProdAttr();
            synchSalesTaxCode();
            synchShippingMethods();
            synchTaxes();
            synchTaxGroup();
            synchTerms();
            synchMemoText();
            synchAccountLogo();
            synchDeviceDefaultValues();
            synchDownloadLastPayID();
            synchVolumePrices();
            synchUoM();
            synchGetTemplates();

            if (Global.isIvuLoto) {
                synchIvuLottoDrawDates();
            }
            synchDownloadCustomerInventory();
            synchDownloadConsignmentTransaction();
            synchShifts();
            synchDownloadClerks();
            synchClerkPersmissions();
            synchDownloadDinnerTable();
            synchSalesAssociateDinnindTablesConfiguration(context);
            synchDownloadMixMatch();
            synchDownloadTermsAndConditions();
            syncBiometrics(context);
            if (preferences.getPreferences(MyPreferences.pref_enable_location_inventory)) {
                synchLocations();
                synchLocationsInventory();
            }
            synchUpdateSyncTime();
            preferences.setLastReceiveSync(DateUtils.getDateAsString(new Date(), DateUtils.DATE_MMM_dd_yyyy_h_mm_a));
            compactRealm();
        } catch (Exception e) {
            preferences.setLastReceiveSync("Sync Fail");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void compactRealm() {
        int count = Realm.getGlobalInstanceCount(Realm.getDefaultConfiguration());
        if (count == 0) {
            boolean compactRealm = Realm.compactRealm(Realm.getDefaultConfiguration());
            if(!compactRealm){
                Crashlytics.log("Realm compact fail.");
            }
        } else {
            Crashlytics.log("Realm compact fail. All realm instance must be closed before compactrealm. EmobilePOS Logger.");
        }
        File realmFile = new File(Realm.getDefaultConfiguration().getPath());
        Crashlytics.log(String.format(Locale.getDefault(), "Account: %s. Realm database file size:%d", preferences.getAcctNumber(), realmFile.length()));

    }

    public void getLocationsInventory(Activity activity) {
        new AsyncGetLocationsInventory(activity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void synchSend(int type, boolean isFromMainMenu, Context activity) {
        Global.isForceUpload = false;
        this.type = type;
        this.isFromMainMenu = isFromMainMenu;
        if (!isSending)
            new SendAsync(activity).execute("");
    }

    public void synchForceSend(Activity activity) {
        Global.isForceUpload = true;
        if (!isSending)
            new ForceSendAsync(activity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void synchGetOnHoldDetails(int type, Intent intent, String ordID, Activity activity) {
        onHoldIntent = intent;
        this.type = type;
        new SynchDownloadOnHoldDetails(activity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ordID);
    }

    public boolean synchSendOnHold(boolean downloadHoldList, boolean checkoutOnHold, Activity activity, String ord_id) {
        this.downloadHoldList = downloadHoldList;
        this.checkoutOnHold = checkoutOnHold;
//        new SynchSendOrdersOnHold(activity).execute(ord_id);

        String err_msg;
        boolean isError = false;
        try {

            if (NetworkUtils.isConnectedToInternet(context)) {
                err_msg = sendOrdersOnHold();
                if (err_msg.isEmpty()) {
                    if (checkoutOnHold) {
                        post.postData(Global.S_CHECKOUT_ON_HOLD, ord_id);
                    }
                } else
                    isError = true;
            } else {
                isError = true;
                err_msg = context.getString(R.string.dlog_msg_no_internet_access);
            }
        } catch (Exception e) {
            isError = true;
            err_msg = "Unhandled Exception";
        }

        if (downloadHoldList) {
            if (!isError) {
                new SynchDownloadOnHoldProducts(activity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                Intent intent = new Intent(context, OnHoldActivity.class);
                context.startActivity(intent);
            }
        }
        return isError;
    }

    /************************************
     * Send Methods
     ************************************/

    private void sendReverse(Object task) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXProcessCardPayHandler handler = new SAXProcessCardPayHandler();
            HashMap<String, String> parsedMap;

            PaymentsXML_DB _paymentsXML_DB = new PaymentsXML_DB(context);
            Cursor c = _paymentsXML_DB.getReversePayments();
            int size = c.getCount();
            if (size > 0) {
                if (Global.isForceUpload)
                    ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_reverse));
                else
                    ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_reverse));

                do {

                    String xml = post.postData(13, c.getString(c.getColumnIndex("payment_xml")));

                    if (!xml.equals(Global.TIME_OUT)
                            && !xml.equals(Global.NOT_VALID_URL)) {
                        InputSource inSource = new InputSource(
                                new StringReader(xml));

                        SAXParser sp = spf.newSAXParser();
                        XMLReader xr = sp.getXMLReader();
                        xr.setContentHandler(handler);
                        xr.parse(inSource);
                        parsedMap = handler.getData();

                        if (parsedMap != null
                                && parsedMap.size() > 0
                                && (parsedMap.get("epayStatusCode").equals(
                                "APPROVED") || parsedMap.get(
                                "epayStatusCode").equals("DECLINE"))) {
                            _paymentsXML_DB.deleteRow(c.getString(c
                                    .getColumnIndex("app_id")));
                        }
                    }

                } while (c.moveToNext());
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendNewCustomers(Object task) throws IOException, SAXException {
        SAXSyncNewCustomerHandler custSaxHandler = new SAXSyncNewCustomerHandler();
        CustomersHandler custHandler = new CustomersHandler(context);
        if (custHandler.getNumUnsyncCustomers() > 0) {
            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_cust));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_cust));
            xml = post.postData(Global.S_SUBMIT_CUSTOMER, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(custSaxHandler);
            xr.parse(inSource);
            data = custSaxHandler.getEmpData();
            custHandler.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }

    }

    private void sendPayments(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXSyncPayPostHandler handler2 = new SAXSyncPayPostHandler();
        PaymentsHandler payHandler = new PaymentsHandler(context);
        long totlaPaments = payHandler.getNumUnsyncPayments();
        while (payHandler.getNumUnsyncPayments() > 0) {
            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(String.format(Locale.getDefault(),
                        "%s %d/%d", context.getString(R.string.sync_sending_payment), payHandler.getNumUnsyncPayments(), totlaPaments));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_payment));
            xml = post.postData(Global.S_SUBMIT_PAYMENTS, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler2);
            xr.parse(inSource);
            data = handler2.getEmpData();
            payHandler.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

    private void sendVoidTransactions(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXSyncVoidTransHandler voidHandler = new SAXSyncVoidTransHandler();
        VoidTransactionsHandler voidTrans = new VoidTransactionsHandler();

        if (voidTrans.getNumUnsyncVoids() > 0) {
            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_void));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_void));
            xml = post.postData(Global.S_SUBMIT_VOID_TRANSACTION, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(voidHandler);
            xr.parse(inSource);
            data = voidHandler.getEmpData();
            voidTrans.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

    private void sendOrders(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXSynchOrdPostHandler handler = new SAXSynchOrdPostHandler();
        OrdersHandler ordersHandler = new OrdersHandler(context);
        long totalOrders = ordersHandler.getNumUnsyncOrders();
        while ((Global.isForceUpload && ordersHandler.getNumUnsyncOrders() > 0) ||
                (!Global.isForceUpload && ordersHandler.getNumUnsyncProcessedOrders() > ordersHandler.getNumUnsyncOrdersStoredFwd())) {
            if (Global.isForceUpload) {
                ((ForceSendAsync) task).updateProgress(String.format(Locale.getDefault(),
                        "%s %d/%d", context.getString(R.string.sync_sending_orders), ordersHandler.getNumUnsyncOrders(), totalOrders));
            } else {
                ((SendAsync) task).updateProgress(String.format(Locale.getDefault(),
                        "%s %d/%d", context.getString(R.string.sync_sending_orders), ordersHandler.getNumUnsyncOrders(), totalOrders));
            }
            xml = post.postData(Global.S_GET_XML_ORDERS, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler);
            xr.parse(inSource);
            data = handler.getEmpData();
            ordersHandler.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

    private void sendInventoryTransfer(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXSendInventoryTransfer saxHandler = new SAXSendInventoryTransfer();
        TransferLocations_DB dbHandler = new TransferLocations_DB(context);
        if (dbHandler.getNumUnsyncTransfers() > 0) {
            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_inventory_transfer));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_inventory_transfer));
            xml = post.postData(Global.S_SUBMIT_LOCATIONS_INVENTORY, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(saxHandler);
            xr.parse(inSource);
            data = saxHandler.getEmpData();
            dbHandler.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

    private void sendTimeClock(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXPostHandler handler = new SAXPostHandler();
        TimeClockHandler timeClockHandler = new TimeClockHandler(context);

        if (timeClockHandler.getNumUnsyncTimeClock() > 0) {
            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_time_clock));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_time_clock));
            xml = post.postData(Global.S_SUBMIT_TIME_CLOCK, null);
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler);
            xr.parse(inSource);
            int size = handler.getSize();

            if (size > 0) {

                for (int i = 0; i < size; i++) {
                    if (!handler.getData("timeclockid", i).isEmpty())
                        timeClockHandler.updateIsSync(handler.getData("timeclockid", i), handler.getData("status", i));
                }

            }
        }
    }

    private void sendCustomerInventory(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXSendCustomerInventory handler = new SAXSendCustomerInventory();
        CustomerInventoryHandler custInventoryHandler = new CustomerInventoryHandler(context);
        if (custInventoryHandler.getNumUnsyncItems() > 0) {
            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_customer_inventory));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_customer_inventory));
            xml = post.postData(Global.S_SUBMIT_CUSTOMER_INVENTORY, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler);
            xr.parse(inSource);
            data = handler.getData();
            custInventoryHandler.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

    private void sendConsignmentTransaction(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXSendConsignmentTransaction handler = new SAXSendConsignmentTransaction();
        ConsignmentTransactionHandler consTransDBHandler = new ConsignmentTransactionHandler(context);
        if (consTransDBHandler.getNumUnsyncItems() > 0) {
            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_consignment_transaction));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_consignment_transaction));
            xml = post.postData(Global.S_SUBMIT_CONSIGNMENT_TRANSACTION, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler);
            xr.parse(inSource);
            data = handler.getData();
            consTransDBHandler.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

//    private void sendShifts(Object task) throws IOException, SAXException, ParserConfigurationException {
//        SAXParserPost handler = new SAXParserPost();
//        ShiftPeriodsDBHandler dbHandler = new ShiftPeriodsDBHandler(context);
//
//        if (dbHandler.getNumUnsyncShifts() > 0) {
//            if (Global.isForceUpload)
//                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_shifts));
//            else
//                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_shifts));
//            xml = post.postData(Global.S_SUBMIT_SHIFT, context, "");
//            inSource = new InputSource(new StringReader(xml));
//            xr.setContentHandler(handler);
//            xr.parse(inSource);
//            data = handler.getData();
//            dbHandler.updateIsSync(data);
//            if (data.isEmpty())
//                didSendData = false;
//            data.clear();
//        }
//    }

    private void synchClerkPersmissions() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        String jsonRequest = client.httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("ClerkPermissions"));
        ClerkEmployeePermissionResponse response = gson.fromJson(jsonRequest, ClerkEmployeePermissionResponse.class);
        EmployeePermissionDAO.truncate();
        ClerkDAO.truncate();
        ClerkDAO.insert(response.getClerks());
        EmployeePermissionDAO.insertOrUpdate(response.getEmployeePersmissions());
    }

    public void synchShifts() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        String jsonRequest = client.httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("Shifts"));
        Type listType = new com.google.gson.reflect.TypeToken<List<Shift>>() {
        }.getType();
        List<Shift> shifts = gson.fromJson(jsonRequest, listType);
        ShiftDAO.insertOrUpdatePendingShift(shifts, Integer.parseInt(preferences.getClerkID()));
    }

    private void sendWalletOrders(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXParserPost handler = new SAXParserPost();
        OrdersHandler dbHandler = new OrdersHandler(context);

        if (dbHandler.getNumUnsyncTupyxOrders() > 0) {

            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_wallet_order));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_wallet_order));
            xml = post.postData(Global.S_SUBMIT_WALLET_RECEIPTS, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler);
            xr.parse(inSource);
            data = handler.getData();
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

    private void sendTemplates(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXPostTemplates handler = new SAXPostTemplates();

        TemplateHandler templateHandler = new TemplateHandler(context);
        if (templateHandler.getNumUnsyncTemplates() > 0) {
            if (Global.isForceUpload)
                ((ForceSendAsync) task).updateProgress(context.getString(R.string.sync_sending_templates));
            else
                ((SendAsync) task).updateProgress(context.getString(R.string.sync_sending_templates));
            xml = post.postData(Global.S_SUBMIT_TEMPLATES, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler);
            xr.parse(inSource);
            data = handler.getEmpData();
            templateHandler.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

    /************************************
     * Send On Holds
     ************************************/

    private String sendOrdersOnHold() throws IOException, SAXException, ParserConfigurationException {
        SAXSynchOrdPostHandler handler = new SAXSynchOrdPostHandler();
        OrdersHandler ordersHandler = new OrdersHandler(context);
        if (ordersHandler.getNumUnsyncOrdersOnHold() > 0) {
//            task.updateProgress(context.getString(R.string.sync_sending_orders));
            xml = post.postData(Global.S_SUBMIT_ON_HOLD, "");
            if (xml.contains("error")) {
                return getTagValue(xml, "error");
            } else {
                inSource = new InputSource(new StringReader(xml));
                xr.setContentHandler(handler);
                xr.parse(inSource);
                data = handler.getEmpData();
                ordersHandler.updateIsSync(data);
                if (data.isEmpty())
                    didSendData = false;
                data.clear();
            }
        }
        return "";
    }

    /************************************
     * Receive Methods
     ************************************/

    private void synchAddresses() throws SAXException, IOException {
        post.postData(7, "Address");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_ADDRESS);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchCategories() throws IOException, SAXException {
        post.postData(7, "Categories");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_CATEGORIES);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

//    private void synchCustomers() throws IOException, SAXException {
//        post.postData(7, "Customers");
//        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_CUSTOMERS);
//        File tempFile = new File(tempFilePath);
//        sp.parse(tempFile, synchHandler);
//        tempFile.delete();
//    }

    private void synchCustomers() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("Customers"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<Customer> customers = new ArrayList<>();
        CustomersHandler handler = new CustomersHandler(context);
        handler.emptyTable();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            Customer customer = gson.fromJson(reader, Customer.class);
            customers.add(customer);
            i++;
            if (i == 1000) {
                handler.insert(customers);
                customers.clear();
                i = 0;
            }
        }
        handler.insert(customers);
        reader.endArray();
        reader.close();
    }


    private void synchEmpInv() throws IOException, SAXException {
        post.postData(7, "EmpInv");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_EMPLOYEE_INVOICES);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchProdInv() throws IOException, SAXException {
        post.postData(7, "InvProducts");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_PRODUCTS_INVOICES);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchInvoices() throws IOException, SAXException {
        post.postData(7, "Invoices");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_INVOICES);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchPaymentMethods() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("PayMethods"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<PaymentMethod> methods = new ArrayList<>();
        PayMethodsHandler payMethodsHandler = new PayMethodsHandler(context);
        payMethodsHandler.emptyTable();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            PaymentMethod method = gson.fromJson(reader, PaymentMethod.class);
            methods.add(method);
            i++;
            if (i == 1000) {
                payMethodsHandler.insert(methods);
                methods.clear();
                i = 0;
            }
        }
        payMethodsHandler.insert(methods);
        reader.endArray();
        reader.close();
    }

    private void synchPriceLevel() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("PriceLevel"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<PriceLevel> priceLevels = new ArrayList<>();
        PriceLevelHandler priceLevelHandler = new PriceLevelHandler();
        priceLevelHandler.emptyTable();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            PriceLevel priceLevel = gson.fromJson(reader, PriceLevel.class);
            priceLevels.add(priceLevel);
            i++;
            if (i == 1000) {
                priceLevelHandler.insert(priceLevels);
                priceLevels.clear();
                i = 0;
            }
        }
        priceLevelHandler.insert(priceLevels);
        reader.endArray();
        reader.close();
    }

    private void synchCustomerCustomFields() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("CustomerCustomFields"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<CustomerCustomField> customFields = new ArrayList<>();
        CustomerCustomFieldsDAO.emptyTable();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            CustomerCustomField customField = gson.fromJson(reader, CustomerCustomField.class);
            customFields.add(customField);
            i++;
            if (i == 1000) {
                CustomerCustomFieldsDAO.insert(customFields);
                customFields.clear();
                i = 0;
            }
        }
        CustomerCustomFieldsDAO.insert(customFields);
        reader.endArray();
        reader.close();
    }


    private void synchItemsPriceLevel() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("PriceLevelItems"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<ItemPriceLevel> itemPriceLevels = new ArrayList<>();
        PriceLevelItemsHandler levelItemsHandler = new PriceLevelItemsHandler(context);
        levelItemsHandler.emptyTable();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            ItemPriceLevel itemPriceLevel = gson.fromJson(reader, ItemPriceLevel.class);
            itemPriceLevels.add(itemPriceLevel);
            i++;
            if (i == 1000) {
                levelItemsHandler.insert(itemPriceLevels);
                itemPriceLevels.clear();
                i = 0;
            }
        }
        levelItemsHandler.insert(itemPriceLevels);
        reader.endArray();
        reader.close();
    }

    private void synchPrinters() throws IOException, SAXException {
        client = new HttpClient();
        GenerateXML xml = new GenerateXML(context);
        String jsonRequest = client.httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("Printers"));
        try {
            DeviceTableDAO.truncate();
            DeviceTableDAO.insert(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchProdCatXref() throws IOException, SAXException {
        post.postData(7, "ProdCatXref");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_PRODCATXREF);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchProdChain() throws IOException, SAXException {
        post.postData(7, "ProductChainXRef");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_PROD_CHAIN);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchProdAddon() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("Product_addons"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<ProductAddons> addonses = new ArrayList<>();
        ProductAddonsHandler addonsHandler = new ProductAddonsHandler(context);
        addonsHandler.emptyTable();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            ProductAddons addons = gson.fromJson(reader, ProductAddons.class);
            addonses.add(addons);
            i++;
            if (i == 1000) {
                addonsHandler.insert(addonses);
                addonses.clear();
                i = 0;
            }
        }
        addonsHandler.insert(addonses);
        reader.endArray();
        reader.close();
    }

    private void synchProducts() throws IOException, SAXException {
        ProductsHandler productsHandler = new ProductsHandler(context);
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("Products"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<Product> products = new ArrayList<>();
        productsHandler.emptyTable();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            Product product = gson.fromJson(reader, Product.class);
            products.add(product);
            i++;
            if (i == 1000) {
                productsHandler.insert(products);
                products.clear();
                i = 0;
            }
        }
        productsHandler.insert(products);
        reader.endArray();
        reader.close();

    }

    private void synchOrderAttributes() throws IOException, SAXException {
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("OrderAttributes"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<OrderAttributes> orderAttributes = new ArrayList<>();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            OrderAttributes attributes = gson.fromJson(reader, OrderAttributes.class);
            orderAttributes.add(attributes);
            i++;
            if (i == 1000) {
                OrderAttributesDAO.insert(orderAttributes);
                orderAttributes.clear();
                i = 0;
            }
        }
        OrderAttributesDAO.insert(orderAttributes);
        reader.endArray();
        reader.close();
    }

    public void postShift(Context context) throws Exception {
        SAXParserPost handler = new SAXParserPost();
        List<Shift> pendingSyncShifts = ShiftDAO.getPendingSyncShifts();
        if (pendingSyncShifts != null && !pendingSyncShifts.isEmpty()) {
            xml = post.postData(Global.S_SUBMIT_SHIFT, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler);
            xr.parse(inSource);
            for (Shift s : pendingSyncShifts) {
                s.setSync(true);
            }
            ShiftDAO.updateShiftToSync(pendingSyncShifts);
        }
    }

    private void synchProductAliases() throws IOException, SAXException {
        ProductAliases_DB productAliasesDB = new ProductAliases_DB(context);
        Gson gson = JsonUtils.getInstance();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("ProductAliases"));
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<ProductAlias> productAliases = new ArrayList<>();
        productAliasesDB.emptyTable();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            ProductAlias alias = gson.fromJson(reader, ProductAlias.class);
            productAliases.add(alias);
            i++;
            if (i == 1000) {
                productAliasesDB.insert(productAliases);
                productAliases.clear();
                i = 0;
            }
        }
        productAliasesDB.insert(productAliases);
        reader.endArray();
        reader.close();
    }

    private void synchProductImages() throws IOException, SAXException {
        post.postData(7, "Products_Images");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_PROD_IMG);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchSalesTaxCode() throws IOException, SAXException {
        post.postData(7, "SalesTaxCodes");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_SALES_TAX_CODE);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchShippingMethods() throws IOException, SAXException {
        post.postData(7, "ShipMethod");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_SHIP_METHODS);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchTaxes() throws IOException, SAXException {
        post.postData(7, "Taxes");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_TAXES);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchTaxGroup() throws IOException, SAXException {
        post.postData(7, "Taxes_Group");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_TAX_GROUP);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchTerms() throws IOException, SAXException {
        post.postData(7, "Terms");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_TERMS);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchMemoText() throws IOException, SAXException {
        post.postData(7, "memotext");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_MEMO_TXT);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchGetTemplates() throws IOException, SAXException {
        post.postData(7, "Templates");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_TEMPLATES);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchDownloadCustomerInventory() throws IOException, SAXException {
        post.postData(7, "GetCustomerInventory");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_CUSTOMER_INVENTORY);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchDownloadConsignmentTransaction() throws IOException, SAXException {
        post.postData(7, "GetConsignmentTransaction");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_CONSIGNMENT_TRANSACTION);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchIvuLottoDrawDates() throws IOException, SAXException {
        post.postData(7, "ivudrawdates");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_IVU_LOTTO);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchAccountLogo() throws IOException {
        GenerateXML generator = new GenerateXML(context);
        MyPreferences myPref = new MyPreferences(context);
        URL url;
        InputStream is;
        url = new URL(generator.getAccountLogo());
        is = url.openStream();
        Bitmap bmp = BitmapFactory.decodeStream(is);
        if (bmp != null) {
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            float scale = 0;
            if (width > 300) {
                scale = (float) 300 / width;
                width = (int) (width * scale);
                height = (int) (height * scale);
            }
            Bitmap newBitmap = Bitmap.createScaledBitmap(bmp, width, height, false);
            String externalPath = context.getApplicationContext().getFilesDir().getAbsolutePath() + "/";
            myPref.setAccountLogoPath(externalPath + "logo.png");
            File file = new File(externalPath, "logo.png");
            OutputStream os = new FileOutputStream(file);
            newBitmap.compress(CompressFormat.PNG, 0, os);
            is.close();
            os.close();
            bmp.recycle();
            newBitmap.recycle();
        }

    }

    private void synchDownloadProductsAttr() throws SAXException, IOException {
        post.postData(7, "ProductsAttr");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_PRODUCTS_ATTRIBUTES);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchDownloadClerks() throws SAXException, IOException {
        post.postData(7, "Clerks");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_CLERKS);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private String readTempFile(File file) {
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
        }
        return text.toString();
    }

    private void synchDownloadDinnerTable() throws SAXException, IOException {
        client = new HttpClient();
        GenerateXML xml = new GenerateXML(context);
        String jsonRequest = client.httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.getDinnerTables());
        try {
            DinningTableDAO.truncate();
            DinningTableDAO.insert(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchDownloadMixMatch() throws SAXException, IOException {
        client = new HttpClient();
        GenerateXML xml = new GenerateXML(context);
        InputStream inputStream = client.httpInputStreamRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.getMixMatch());
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        List<MixMatch> mixMatches = new ArrayList<MixMatch>();
        MixMatchDAO.truncate();
        reader.beginArray();
        int i = 0;
        while (reader.hasNext()) {
            MixMatch mixMatch = gson.fromJson(reader, MixMatch.class);
            mixMatches.add(mixMatch);
            i++;
            if (i == 1000) {
                MixMatchDAO.insert(mixMatches);
                mixMatches.clear();
                i = 0;
            }
        }
        MixMatchDAO.insert(mixMatches);
        reader.endArray();
        reader.close();
    }

    private void synchDownloadSalesAssociate() throws SAXException, IOException {
        client = new HttpClient();
        GenerateXML xml = new GenerateXML(context);
        String jsonRequest = client.httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.getSalesAssociate());
        try {
            ClerkDAO.truncate();
            ClerkDAO.insert(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchDownloadTermsAndConditions() throws SAXException, IOException {
        GenerateXML xml = new GenerateXML(context);
        String jsonRequest = client.httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("TermsAndConditions"));
        TermsNConditionsDAO.insert(jsonRequest);
    }

    private void synchUoM() throws IOException, SAXException {
        client = new HttpClient();
        GenerateXML xml = new GenerateXML(context);
        String jsonRequest = client.httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("UoM"));
        UomDAO.truncate();
        UomDAO.insert(jsonRequest);
    }

    private void synchGetOrdProdAttr() throws IOException, SAXException {
        client = new HttpClient();
        GenerateXML xml = new GenerateXML(context);
        String jsonRequest = client.httpJsonRequest(context.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("GetOrderProductsAttr"));
        OrderProductAttributeDAO.truncate();
        OrderProductAttributeDAO.insert(jsonRequest);
    }

    private void synchGetServerTime() throws IOException, SAXException {
        xml = post.postData(Global.S_GET_SERVER_TIME, null);
        SAXPostHandler handler = new SAXPostHandler();
        inSource = new InputSource(new StringReader(xml));
        xr.setContentHandler(handler);
        xr.parse(inSource);
        _server_time = handler.getData("serverTime", 0);
    }

    private void synchUpdateSyncTime() throws IOException, SAXException {
        post.postData(Global.S_UPDATE_SYNC_TIME, _server_time);
    }

    private void synchEmployeeData() throws Exception {
        String xml = post.postData(4, "");
        Gson gson = JsonUtils.getInstance();
        Type listType = new com.google.gson.reflect.TypeToken<List<AssignEmployee>>() {
        }.getType();
        List<AssignEmployee> assignEmployees = gson.fromJson(xml, listType);
        AssignEmployeeDAO.insertAssignEmployee(assignEmployees);

    }

    private void synchDownloadLastPayID() throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SaxLoginHandler handler = new SaxLoginHandler();
        String xml = post.postData(6, "");
        InputSource inSource = new InputSource(new StringReader(xml));
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();
        xr.setContentHandler(handler);
        xr.parse(inSource);
        if (!handler.getData().isEmpty()) {
            MyPreferences myPref = new MyPreferences(context);
            myPref.setLastPayID(handler.getData());
        }


    }

    private void synchDeviceDefaultValues() throws IOException, SAXException {
        post.postData(7, "DeviceDefaultValues");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_DEVICE_DEFAULT_VAL);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();

    }

    private void synchVolumePrices() throws IOException, SAXException {
        post.postData(7, "VolumePrices");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_VOLUME_PRICES);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchLocations() throws IOException, SAXException {
        post.postData(7, "GetLocations");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_LOCATIONS);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchLocationsInventory() throws IOException, SAXException {
        post.postData(7, "GetLocationsInventory");
        SAXSynchHandler synchHandler = new SAXSynchHandler(context, Global.S_LOCATIONS_INVENTORY);
        File tempFile = new File(tempFilePath);
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }


    private class SendAsync extends AsyncTask<String, String, String> {
        boolean proceed = false;
        MyPreferences myPref = new MyPreferences(context);
        String synchStage = "";
        private Context activity;

        private SendAsync(Context activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            isSending = true;
            int orientation = context.getResources().getConfiguration().orientation;
        }

        @Override
        protected void onProgressUpdate(String... params) {
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(String... params) {

            updateProgress(context.getString(R.string.please_wait_message));
            if (NetworkUtils.isConnectedToInternet(context)) {
                try {

                    synchStage = context.getString(R.string.sync_sending_reverse);
                    sendReverse(this);

                    synchStage = context.getString(R.string.sync_sending_payment);
                    sendPayments(this);

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_void);
                        sendVoidTransactions(this);

                    }

                    // add signatures
                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_templates);
                        sendTemplates(this);
                    }

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_cust);
                        sendNewCustomers(this);
                        postEmobileBiometrics(context);
                    }

                    // add shifts

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_wallet_order);
                        sendWalletOrders(this);
                    }

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_orders);
                        sendOrders(this);
                    }

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_inventory_transfer);
                        sendInventoryTransfer(this);
                    }

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_customer_inventory);
                        sendCustomerInventory(this);
                    }

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_consignment_transaction);
                        sendConsignmentTransaction(this);
                    }

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_shifts);
                        postShift(activity);
                    }

                    if (didSendData) {
                        synchStage = context.getString(R.string.sync_sending_time_clock);
                        sendTimeClock(this);
                    }

                    if (didSendData)
                        proceed = true;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else
                xml = context.getString(R.string.dlog_msg_no_internet_access);
            return null;
        }

        @Override
        protected void onPostExecute(String unused) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy h:mm a", Locale.getDefault());
            String date = sdf.format(new Date());
            myPref.setLastSendSync(date);
            compactRealm();
            isSending = false;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                if (!activity.isDestroyed()) {
//                    if (myProgressDialog != null && myProgressDialog.isShowing()) {
//                        myProgressDialog.dismiss();
//                    }
//                }
//            } else {
//            if (myProgressDialog != null && myProgressDialog.isShowing()) {
//                myProgressDialog.dismiss();
//            }
//            }

            if (type == Global.FROM_SYNCH_ACTIVITY) {
//                if (isFromMainMenu) {
//                    synchTextView.setVisibility(View.GONE);
//                }
                if (SyncTab_FR.syncTabHandler != null) {
                    SyncTab_FR.syncTabHandler.sendEmptyMessage(0);
                }
            }

//            if (proceed && dbManager.isSendAndReceive()) {
//                dbManager.updateDB();
//            } else
            if (!proceed) {
                // failed to synch....
                if (TextUtils.isEmpty(xml)) {
                    xml = context.getString(R.string.sync_fail);
                }
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                    if (!activity.isFinishing() && !activity.isDestroyed()) {
//                        Global.showPrompt(activity, R.string.dlog_title_error, xml);
//                    }
//                } else if (!activity.isFinishing()) {
//                    Global.showPrompt(activity, R.string.dlog_title_error, xml);
//                }
            }

//            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private class ForceSendAsync extends AsyncTask<Void, String, Void> {

        private Activity activity;

        private ForceSendAsync(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            isSending = true;
            int orientation = context.getResources().getConfiguration().orientation;

//            activity.setRequestedOrientation(Global.getScreenOrientation(context));

//            myProgressDialog = new ProgressDialog(context);
//
//            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//            myProgressDialog.setCancelable(false);
//            myProgressDialog.setMax(100);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected void onProgressUpdate(String... params) {

//            if (!myProgressDialog.isShowing())
//                myProgressDialog.show();
//            myProgressDialog.setMessage(params[0]);

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                sendReverse(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendPayments(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendVoidTransactions(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendTemplates(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendNewCustomers(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendWalletOrders(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendOrders(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendInventoryTransfer(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendCustomerInventory(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendConsignmentTransaction(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                postShift(activity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                sendTimeClock(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            Global.isForceUpload = false;
//            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy h:mm a", Locale.getDefault());
            String date = DateUtils.getDateAsString(new Date(), DateUtils.DATE_MMM_dd_yyyy_h_mm_a);
            MyPreferences myPref = new MyPreferences(context);
            myPref.setLastSendSync(date);

            isSending = false;
//            myProgressDialog.dismiss();

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private class SynchDownloadOnHoldProducts extends AsyncTask<String, String, String> {
        MyPreferences myPref = new MyPreferences(context);
        private Activity activity;

        private SynchDownloadOnHoldProducts(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {

            int orientation = context.getResources().getConfiguration().orientation;
//            activity.setRequestedOrientation(Global.getScreenOrientation(context));

//            myProgressDialog = new ProgressDialog(context);
//            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//            myProgressDialog.setCancelable(false);
//            myProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... params) {
//            myProgressDialog.setMessage(params[0]);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(String... params) {
            if (NetworkUtils.isConnectedToInternet(context)) {
                try {
                    updateProgress(context.getString(R.string.sync_dload_ordersonhold));
                    synchOrdersOnHoldList(context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onPostExecute(String unused) {
//            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy h:mm a", Locale.getDefault());
            String date = DateUtils.getDateAsString(new Date(), DateUtils.DATE_MMM_dd_yyyy_h_mm_a);
            myPref.setLastReceiveSync(date);
//            myProgressDialog.dismiss();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            Intent intent = new Intent(context, OnHoldActivity.class);
            context.startActivity(intent);
        }

    }

    private class SynchDownloadOnHoldDetails extends AsyncTask<String, String, String> {
        boolean proceedToView = false;
        private Activity activity;

        private SynchDownloadOnHoldDetails(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {

            int orientation = context.getResources().getConfiguration().orientation;
//            activity.setRequestedOrientation(Global.getScreenOrientation(context));
//            myProgressDialog = new ProgressDialog(context);
//            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//            myProgressDialog.setCancelable(false);
//            myProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... params) {
//            myProgressDialog.setMessage(params[0]);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                updateProgress(context.getString(R.string.sync_dload_ordersonhold));
//                synchOrdersOnHoldDetails(context, params[0]);
                OrderProductsHandler orderProdHandler = new OrderProductsHandler(context);
                Cursor c = orderProdHandler.getOrderProductsOnHold(params[0]);
                if (BuildConfig.DELETE_INVALID_HOLDS || (c != null && c.getCount() > 0)) {
                    proceedToView = true;
//                    if (type == 0)
//                        OnHoldActivity.addOrderProducts(activity, c);
                } else
                    proceedToView = false;
                if (c != null) {
                    c.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String unused) {
//            myProgressDialog.dismiss();
            if (proceedToView) {
                if (type == 0) {
                    activity.startActivityForResult(onHoldIntent, 0);
                    activity.finish();
                } else//print
                {
                    ((OnHoldActivity) context).printOnHoldTransaction();
                }
            } else
                Toast.makeText(context, "Failed to download...", Toast.LENGTH_LONG).show();
        }

    }

    private class SynchSendOrdersOnHold extends AsyncTask<String, String, String> {
        boolean isError = false;
        String err_msg = "";
        private Activity activity;
        private ProgressDialog myProgressDialog;

        private SynchSendOrdersOnHold(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            int orientation = context.getResources().getConfiguration().orientation;
            if (context instanceof OrderingMain_FA) {
                activity.setRequestedOrientation(Global.getScreenOrientation(context));
                if (myProgressDialog != null && myProgressDialog.isShowing())
                    myProgressDialog.dismiss();
                myProgressDialog = new ProgressDialog(context);
                myProgressDialog.setIndeterminate(true);
                myProgressDialog.setMessage(context.getString(R.string.sync_on_hold));
                myProgressDialog.setCancelable(false);

                if (!checkoutOnHold) {
                    myProgressDialog.show();
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... params) {
//            myProgressDialog.setMessage(params[0]);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(String... params) {
            try {

                if (NetworkUtils.isConnectedToInternet(context)) {
                    err_msg = sendOrdersOnHold();
                    if (err_msg.isEmpty()) {
                        if (checkoutOnHold) {
                            String orderId = params[0];
                            post.postData(Global.S_CHECKOUT_ON_HOLD, orderId);
                        }
                    } else
                        isError = true;
                } else {
                    isError = true;
                    err_msg = context.getString(R.string.dlog_msg_no_internet_access);
                }
            } catch (Exception e) {
                isError = true;
                err_msg = "Unhandled Exception";
            }
            return null;
        }

        protected void onPostExecute(String unused) {
//            if (!activity.isFinishing() && myProgressDialog != null && myProgressDialog.isShowing())
//                myProgressDialog.dismiss();
            if (context instanceof OrderingMain_FA) {
                Global.dismissDialog((Activity) context, myProgressDialog);
            }
            if (!downloadHoldList) {
                boolean closeActivity = true;
                if ((context instanceof OrderingMain_FA &&
                        ((OrderingMain_FA) context).getRestaurantSaleType() == Global.RestaurantSaleType.EAT_IN)
                        || context instanceof OnHoldActivity) {
                    closeActivity = false;
                }
                if (!checkoutOnHold && closeActivity) {
                    activity.finish();
                }
            } else if (!isError) {
                new SynchDownloadOnHoldProducts(activity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                Intent intent = new Intent(context, OnHoldActivity.class);
                context.startActivity(intent);
            }
        }

    }

    private class AsyncGetLocationsInventory extends AsyncTask<Void, String, Void> {
        private Activity activity;

        private AsyncGetLocationsInventory(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {

            int orientation = context.getResources().getConfiguration().orientation;
//            activity.setRequestedOrientation(Global.getScreenOrientation(context));
//            myProgressDialog = new ProgressDialog(context);
//            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//            myProgressDialog.setCancelable(false);
//            myProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... params) {
//            myProgressDialog.setMessage(params[0]);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (isReceive)
                    updateProgress(context.getString(R.string.sync_dload_locations));
                else
                    updateProgress(context.getString(R.string.sync_dload_locations));
                synchLocations();
                if (isReceive)
                    updateProgress(context.getString(R.string.sync_dload_locations_inventory));
                else
                    updateProgress(context.getString(R.string.sync_dload_locations_inventory));
                synchLocationsInventory();
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
//            myProgressDialog.dismiss();
        }

    }

}