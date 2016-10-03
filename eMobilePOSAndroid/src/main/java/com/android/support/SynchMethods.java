
package com.android.support;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dao.DeviceTableDAO;
import com.android.dao.DinningTableDAO;
import com.android.dao.MixMatchDAO;
import com.android.dao.OrderProductAttributeDAO;
import com.android.dao.SalesAssociateDAO;
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
import com.android.database.ShiftPeriodsDBHandler;
import com.android.database.TemplateHandler;
import com.android.database.TimeClockHandler;
import com.android.database.TransferLocations_DB;
import com.android.database.VoidTransactionsHandler;
import com.android.emobilepos.BuildConfig;
import com.android.emobilepos.OnHoldActivity;
import com.android.emobilepos.R;
import com.android.emobilepos.mainmenu.MainMenu_FA;
import com.android.emobilepos.mainmenu.SyncTab_FR;
import com.android.emobilepos.models.ItemPriceLevel;
import com.android.emobilepos.models.Order;
import com.android.emobilepos.models.OrderProduct;
import com.android.emobilepos.models.PriceLevel;
import com.android.emobilepos.models.Product;
import com.android.emobilepos.models.ProductAddons;
import com.android.emobilepos.models.ProductAlias;
import com.android.emobilepos.models.realms.DinningTable;
import com.android.emobilepos.models.realms.MixMatch;
import com.android.emobilepos.models.realms.PaymentMethod;
import com.android.emobilepos.models.realms.SalesAssociate;
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
import com.android.saxhandler.SaxSelectedEmpHandler;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

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
import java.net.MalformedURLException;
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

import oauthclient.OAuthClient;
import oauthclient.OAuthManager;
import util.json.JsonUtils;

public class SynchMethods {
    private Post post;
    private Activity activity;
    private String xml;
    private InputSource inSource;
    private SAXParser sp;
    private XMLReader xr;
    private List<String[]> data;
    private String tempFilePath;
    private boolean checkoutOnHold = false, downloadHoldList = false;

    private ProgressDialog myProgressDialog;
    private int type;
    private DBManager dbManager;

    private boolean didSendData = true;
    private boolean isFromMainMenu = false;

    private Intent onHoldIntent;
    private HttpClient client;
    private Gson gson = JsonUtils.getInstance();

    private static OAuthManager getOAuthManager(Activity activity) {
        MyPreferences preferences = new MyPreferences(activity);
        return OAuthManager.getInstance(activity, preferences.getAcctNumber(), preferences.getAcctPassword());

    }

    public SynchMethods(DBManager managerInst) {
        post = new Post();
        client = new HttpClient();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        activity = managerInst.getActivity();
        dbManager = managerInst;
        data = new ArrayList<>();
        if(OAuthManager.isExpired(activity)) {
            OAuthManager oAuthManager = getOAuthManager(activity);
            try {
                oAuthManager.requestToken();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tempFilePath = activity.getApplicationContext().getFilesDir().getAbsolutePath() + "/temp.xml";
        try {
            sp = spf.newSAXParser();
            xr = sp.getXMLReader();
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        }
    }


    private boolean isReceive = false;

    public void synchReceive(int type) {
        this.type = type;
        isReceive = true;
        new resynchAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    public void getLocationsInventory() {
        new asyncGetLocationsInventory().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    private class resynchAsync extends AsyncTask<String, String, String> {
        MyPreferences myPref = new MyPreferences(activity);

        @Override
        protected void onPreExecute() {
            int orientation = activity.getResources().getConfiguration().orientation;
            activity.setRequestedOrientation(Global.getScreenOrientation(activity));
            myProgressDialog = new ProgressDialog(activity);
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... params) {
            myProgressDialog.setMessage(params[0]);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(String... params) {
            try {

                synchGetServerTime(this);

                synchAddresses(this);

                synchCategories(this);

                synchCustomers(this);

                synchEmpInv(this);

                synchProdInv(this);

                synchInvoices(this);

                synchPaymentMethods(this);

                synchPriceLevel(this);

                synchItemsPriceLevel(this);

                synchPrinters(this);

                synchProdCatXref(this);

                synchProdChain(this);

                synchProdAddon(this);

                synchProducts(this);


                synchProductAliases(this);

                synchProductImages(this);

                synchDownloadProductsAttr(this);

                synchGetOrdProdAttr(this);

                synchSalesTaxCode(this);

                synchShippingMethods(this);

                synchTaxes(this);

                synchTaxGroup(this);

                synchTerms(this);

                synchMemoText(this);

                synchEmployeeData(this);

                synchAccountLogo(this);

                synchDeviceDefaultValues(this);

                synchDownloadLastPayID(this);

                synchVolumePrices(this);

                synchUoM(this);

                synchGetTemplates(this);

                if (Global.isIvuLoto) {
                    synchIvuLottoDrawDates(this);
                }
                synchDownloadCustomerInventory(this);
                synchDownloadConsignmentTransaction(this);
                synchDownloadClerks(this);
                synchDownloadSalesAssociate(this);
                synchDownloadDinnerTable(this);
                synchSalesAssociateDinnindTablesConfiguration(activity);
                synchDownloadMixMatch(this);
                synchDownloadTermsAndConditions(this);
                if (myPref.getPreferences(MyPreferences.pref_enable_location_inventory)) {
                    synchLocations(this);
                    synchLocationsInventory(this);
                }
                synchUpdateSyncTime(this);

            } catch (Exception ignored) {

            }
            return null;
        }

        protected void onPostExecute(String unused) {
            isReceive = false;
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy h:mm a", Locale.getDefault());
            String date = sdf.format(new Date());

            myPref.setLastReceiveSync(date);

            if (myProgressDialog != null && myProgressDialog.isShowing()) {
                myProgressDialog.dismiss();
            }
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (type == Global.FROM_LOGIN_ACTIVITTY) {
                Intent intent = new Intent(activity, MainMenu_FA.class);
                activity.startActivity(intent);
                activity.finish();
            } else if (type == Global.FROM_REGISTRATION_ACTIVITY) {
                Intent intent = new Intent(activity, MainMenu_FA.class);
                activity.setResult(-1);
                activity.startActivity(intent);
                activity.finish();
            } else if (type == Global.FROM_SYNCH_ACTIVITY) {
                SyncTab_FR.syncTabHandler.sendEmptyMessage(0);
            }
        }

    }


    private boolean isSending = false;

    public void synchSend(int type, boolean isFromMainMenu) {
        Global.isForceUpload = false;
        this.type = type;
        this.isFromMainMenu = isFromMainMenu;
        if (!isSending)
            new sendAsync().execute("");

    }

    public void synchForceSend() {
        Global.isForceUpload = true;
        if (!isSending)
            new forceSendAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    private class sendAsync extends AsyncTask<String, String, String> {
        boolean proceed = false;
        MyPreferences myPref = new MyPreferences(activity);
        String synchStage = "";
        TextView synchTextView;

        @Override
        protected void onPreExecute() {
            isSending = true;
            int orientation = activity.getResources().getConfiguration().orientation;

            activity.setRequestedOrientation(Global.getScreenOrientation(activity));

            if (isFromMainMenu) {
                MainMenu_FA synchActivity = (MainMenu_FA) activity;
                synchTextView = synchActivity.getSynchTextView();
            }

            myProgressDialog = new ProgressDialog(activity);

            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.setMax(100);
            // myProgressDialog.show();

        }

        @Override
        protected void onProgressUpdate(String... params) {
            if (!isFromMainMenu) {
                if (!myProgressDialog.isShowing())
                    myProgressDialog.show();
                myProgressDialog.setMessage(params[0]);
            } else {
                if (!synchTextView.isShown())
                    synchTextView.setVisibility(View.VISIBLE);
                synchTextView.setText(params[0]);
            }
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(String... params) {

            updateProgress("Please Wait...");
            if (NetworkUtils.isConnectedToInternet(activity)) {
                try {

                    synchStage = activity.getString(R.string.sync_sending_reverse);
                    sendReverse(this);

                    synchStage = activity.getString(R.string.sync_sending_payment);
                    sendPayments(this);

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_void);
                        sendVoidTransactions(this);

                    }

                    // add signatures
                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_templates);
                        sendTemplates(this);
                    }

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_cust);
                        sendNewCustomers(this);
                    }

                    // add shifts

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_wallet_order);
                        sendWalletOrders(this);
                    }

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_orders);
                        sendOrders(this);
                    }

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_inventory_transfer);
                        sendInventoryTransfer(this);
                    }

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_customer_inventory);
                        sendCustomerInventory(this);
                    }

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_consignment_transaction);
                        sendConsignmentTransaction(this);
                    }

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_shifts);
                        sendShifts(this);
                    }

                    if (didSendData) {
                        synchStage = activity.getString(R.string.sync_sending_time_clock);
                        sendTimeClock(this);
                    }

                    if (didSendData)
                        proceed = true;

                } catch (Exception e) {

                }
            } else
                xml = activity.getString(R.string.dlog_msg_no_internet_access);
            return null;
        }

        @Override
        protected void onPostExecute(String unused) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy h:mm a", Locale.getDefault());
            String date = sdf.format(new Date());
            myPref.setLastSendSync(date);

            isSending = false;
            myProgressDialog.dismiss();

            if (type == Global.FROM_SYNCH_ACTIVITY) {
                if (!isFromMainMenu) {
                    SyncTab_FR.syncTabHandler.sendEmptyMessage(0);
                } else {
                    synchTextView.setVisibility(View.GONE);
                }

            }

            if (proceed && dbManager.isSendAndReceive()) {
                dbManager.updateDB();
            } else if (!proceed) {
                // failed to synch....
                Global.showPrompt(activity, R.string.dlog_title_error, xml);
            }

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }


    private class forceSendAsync extends AsyncTask<Void, String, Void> {


        @Override
        protected void onPreExecute() {
            isSending = true;
            int orientation = activity.getResources().getConfiguration().orientation;

            activity.setRequestedOrientation(Global.getScreenOrientation(activity));

            myProgressDialog = new ProgressDialog(activity);

            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.setMax(100);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }


        @Override
        protected void onProgressUpdate(String... params) {

            if (!myProgressDialog.isShowing())
                myProgressDialog.show();
            myProgressDialog.setMessage(params[0]);

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
                sendShifts(this);
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
            MyPreferences myPref = new MyPreferences(activity);
            myPref.setLastSendSync(date);

            isSending = false;
            myProgressDialog.dismiss();

            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    public void synchGetOnHoldProducts() {
        new synchDownloadOnHoldProducts().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

    }

    public void synchGetOnHoldDetails(int type, Intent intent, String ordID) {
        onHoldIntent = intent;
        this.type = type;
        new synchDownloadOnHoldDetails().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ordID);
    }

    public void synchSendOnHold(boolean downloadHoldList, boolean checkoutOnHold) {
        this.downloadHoldList = downloadHoldList;
        this.checkoutOnHold = checkoutOnHold;
        new synchSendOrdersOnHold().execute();
    }


    private class synchDownloadOnHoldProducts extends AsyncTask<String, String, String> {
        MyPreferences myPref = new MyPreferences(activity);

        @Override
        protected void onPreExecute() {

            int orientation = activity.getResources().getConfiguration().orientation;
            activity.setRequestedOrientation(Global.getScreenOrientation(activity));

            myProgressDialog = new ProgressDialog(activity);
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();
        }


        @Override
        protected void onProgressUpdate(String... params) {
            myProgressDialog.setMessage(params[0]);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(String... params) {
            if (NetworkUtils.isConnectedToInternet(activity)) {
                try {
                    updateProgress(activity.getString(R.string.sync_dload_ordersonhold));
                    synchOrdersOnHoldList(activity);
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
            myProgressDialog.dismiss();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            Intent intent = new Intent(activity, OnHoldActivity.class);
            activity.startActivity(intent);
        }

    }


    private class synchDownloadOnHoldDetails extends AsyncTask<String, String, String> {
        boolean proceedToView = false;

        @Override
        protected void onPreExecute() {

            int orientation = activity.getResources().getConfiguration().orientation;
            activity.setRequestedOrientation(Global.getScreenOrientation(activity));
            myProgressDialog = new ProgressDialog(activity);
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();
        }


        @Override
        protected void onProgressUpdate(String... params) {
            myProgressDialog.setMessage(params[0]);
        }

        public void updateProgress(String msg) {
            publishProgress(msg);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                updateProgress(activity.getString(R.string.sync_dload_ordersonhold));
                synchOrdersOnHoldDetails(activity, params[0]);
                OrderProductsHandler orderProdHandler = new OrderProductsHandler(activity);
                Cursor c = orderProdHandler.getOrderProductsOnHold(params[0]);
                if (BuildConfig.DELETE_INVALID_HOLDS || (c != null && c.getCount() > 0)) {
                    proceedToView = true;
                    if (type == 0)
                        ((OnHoldActivity) activity).addOrderProducts(activity, c);
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
            myProgressDialog.dismiss();
            if (proceedToView) {
                if (type == 0) {
                    activity.startActivityForResult(onHoldIntent, 0);
                    activity.finish();
                } else//print
                {
                    ((OnHoldActivity) activity).printOnHoldTransaction();
                }
            } else
                Toast.makeText(activity, "Failed to download...", Toast.LENGTH_LONG).show();
        }

    }


    private class synchSendOrdersOnHold extends AsyncTask<Void, String, String> {
        boolean isError = false;
        String err_msg = "";


        @Override
        protected void onPreExecute() {
            int orientation = activity.getResources().getConfiguration().orientation;
            activity.setRequestedOrientation(Global.getScreenOrientation(activity));
            if (myProgressDialog != null && myProgressDialog.isShowing())
                myProgressDialog.dismiss();
            myProgressDialog = new ProgressDialog(activity);
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);

            if (!checkoutOnHold) {
                myProgressDialog.show();
            }

        }


        @Override
        protected void onProgressUpdate(String... params) {
            myProgressDialog.setMessage(params[0]);
        }


        public void updateProgress(String msg) {
            publishProgress(msg);
        }


        @Override
        protected String doInBackground(Void... params) {
            try {
                if (NetworkUtils.isConnectedToInternet(activity)) {
                    err_msg = sendOrdersOnHold(this);
                    if (err_msg.isEmpty()) {
                        if (checkoutOnHold)
                            post.postData(Global.S_CHECKOUT_ON_HOLD, activity, Global.lastOrdID);
                    } else
                        isError = true;
                } else {
                    isError = true;
                    err_msg = activity.getString(R.string.dlog_msg_no_internet_access);
                }
            } catch (Exception e) {
                isError = true;
                err_msg = "Unhandled Exception";
            }
            return null;
        }

        protected void onPostExecute(String unused) {
            if (!activity.isFinishing() && myProgressDialog != null && myProgressDialog.isShowing())
                myProgressDialog.dismiss();
            if (!downloadHoldList) {
                boolean closeActivity = true;
                if (activity instanceof OrderingMain_FA &&
                        ((OrderingMain_FA) activity).getRestaurantSaleType() == Global.RestaurantSaleType.EAT_IN) {
                    closeActivity = false;
                }
                if (!checkoutOnHold && closeActivity) {
                    activity.finish();
                }
            } else if (!isError) {
                synchGetOnHoldProducts();
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                Intent intent = new Intent(activity, OnHoldActivity.class);
                activity.startActivity(intent);
            }
        }

    }


    private class asyncGetLocationsInventory extends AsyncTask<Void, String, Void> {
        @Override
        protected void onPreExecute() {

            int orientation = activity.getResources().getConfiguration().orientation;
            activity.setRequestedOrientation(Global.getScreenOrientation(activity));
            myProgressDialog = new ProgressDialog(activity);
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();
        }


        @Override
        protected void onProgressUpdate(String... params) {
            myProgressDialog.setMessage(params[0]);
        }


        public void updateProgress(String msg) {
            publishProgress(msg);
        }


        @Override
        protected Void doInBackground(Void... params) {
            try {
                synchLocations(this);
                synchLocationsInventory(this);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            myProgressDialog.dismiss();
        }

    }

    /************************************
     * Send Methods
     ************************************/

    private void sendReverse(Object task) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXProcessCardPayHandler handler = new SAXProcessCardPayHandler(
                    activity);
            HashMap<String, String> parsedMap;

            PaymentsXML_DB _paymentsXML_DB = new PaymentsXML_DB(activity);
            Cursor c = _paymentsXML_DB.getReversePayments();
            int size = c.getCount();
            if (size > 0) {
                if (Global.isForceUpload)
                    ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_reverse));
                else
                    ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_reverse));

                do {

                    String xml = post.postData(13, activity,
                            c.getString(c.getColumnIndex("payment_xml")));

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
        CustomersHandler custHandler = new CustomersHandler(activity);
        if (custHandler.getNumUnsyncCustomers() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_cust));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_cust));
            xml = post.postData(Global.S_SUBMIT_CUSTOMER, activity, "");
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
        PaymentsHandler payHandler = new PaymentsHandler(activity);
        if (payHandler.getNumUnsyncPayments() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_payment));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_payment));
            xml = post.postData(Global.S_SUBMIT_PAYMENTS, activity, "");
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
        VoidTransactionsHandler voidTrans = new VoidTransactionsHandler(activity);

        if (voidTrans.getNumUnsyncVoids() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_void));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_void));
            xml = post.postData(Global.S_SUBMIT_VOID_TRANSACTION, activity, "");
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
        OrdersHandler ordersHandler = new OrdersHandler(activity);
        if ((Global.isForceUpload && ordersHandler.getNumUnsyncOrders() > 0) ||
                (!Global.isForceUpload && ordersHandler.getNumUnsyncProcessedOrders() > ordersHandler.getNumUnsyncOrdersStoredFwd())) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_orders));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_orders));
            xml = post.postData(Global.S_GET_XML_ORDERS, activity, "");
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
        TransferLocations_DB dbHandler = new TransferLocations_DB(activity);
        if (dbHandler.getNumUnsyncTransfers() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_inventory_transfer));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_inventory_transfer));
            xml = post.postData(Global.S_SUBMIT_LOCATIONS_INVENTORY, activity, "");
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
        SAXPostHandler handler = new SAXPostHandler(activity);
        TimeClockHandler timeClockHandler = new TimeClockHandler(activity);

        if (timeClockHandler.getNumUnsyncTimeClock() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_time_clock));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_time_clock));
            xml = post.postData(Global.S_SUBMIT_TIME_CLOCK, activity, null);
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
        CustomerInventoryHandler custInventoryHandler = new CustomerInventoryHandler(activity);
        if (custInventoryHandler.getNumUnsyncItems() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_customer_inventory));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_customer_inventory));
            xml = post.postData(Global.S_SUBMIT_CUSTOMER_INVENTORY, activity, "");
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
        ConsignmentTransactionHandler consTransDBHandler = new ConsignmentTransactionHandler(activity);
        if (consTransDBHandler.getNumUnsyncItems() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_consignment_transaction));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_consignment_transaction));
            xml = post.postData(Global.S_SUBMIT_CONSIGNMENT_TRANSACTION, activity, "");
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

    private void sendShifts(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXParserPost handler = new SAXParserPost();
        ShiftPeriodsDBHandler dbHandler = new ShiftPeriodsDBHandler(activity);

        if (dbHandler.getNumUnsyncShifts() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_shifts));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_shifts));
            xml = post.postData(Global.S_SUBMIT_SHIFT, activity, "");
            inSource = new InputSource(new StringReader(xml));
            xr.setContentHandler(handler);
            xr.parse(inSource);
            data = handler.getData();
            dbHandler.updateIsSync(data);
            if (data.isEmpty())
                didSendData = false;
            data.clear();
        }
    }

    private void sendWalletOrders(Object task) throws IOException, SAXException, ParserConfigurationException {
        SAXParserPost handler = new SAXParserPost();
        OrdersHandler dbHandler = new OrdersHandler(activity);

        if (dbHandler.getNumUnsyncTupyxOrders() > 0) {

            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_wallet_order));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_wallet_order));
            xml = post.postData(Global.S_SUBMIT_WALLET_RECEIPTS, activity, "");
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

        TemplateHandler templateHandler = new TemplateHandler(activity);
        if (templateHandler.getNumUnsyncTemplates() > 0) {
            if (Global.isForceUpload)
                ((forceSendAsync) task).updateProgress(activity.getString(R.string.sync_sending_templates));
            else
                ((sendAsync) task).updateProgress(activity.getString(R.string.sync_sending_templates));
            xml = post.postData(Global.S_SUBMIT_TEMPLATES, activity, "");
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


    private String sendOrdersOnHold(synchSendOrdersOnHold task) throws IOException, SAXException, ParserConfigurationException {
        SAXSynchOrdPostHandler handler = new SAXSynchOrdPostHandler();
        OrdersHandler ordersHandler = new OrdersHandler(activity);
        if (ordersHandler.getNumUnsyncOrdersOnHold() > 0) {
            task.updateProgress(activity.getString(R.string.sync_sending_orders));
            xml = post.postData(Global.S_SUBMIT_ON_HOLD, activity, "");
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

    public static String getTagValue(String xml, String tagName) {
        return xml.split("<" + tagName + ">")[1].split("</" + tagName + ">")[0];
    }

    public static void synchOrdersOnHoldList(Activity activity) throws SAXException, IOException {
        try {
            Gson gson = JsonUtils.getInstance();
            GenerateXML xml = new GenerateXML(activity);
            InputStream inputStream = new HttpClient().httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.downloadAll("GetOrdersOnHoldList"));
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            List<Order> orders = new ArrayList<>();
            OrdersHandler ordersHandler = new OrdersHandler(activity);
            ordersHandler.emptyTable();
            reader.beginArray();
            int i = 0;
            while (reader.hasNext()) {
                Order order = gson.fromJson(reader, Order.class);
                order.ord_issync = "1";
                order.isOnHold = "1";
                orders.add(order);
                i++;
                if (i == 1000) {
                    ordersHandler.insert(orders);
                    orders.clear();
                    i = 0;
                }
            }
            ordersHandler.insert(orders);
            reader.endArray();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void synchOrdersOnHoldDetails(Activity activity, String ordID) throws SAXException, IOException {
        try {
            HttpClient client = new HttpClient();
            Gson gson = JsonUtils.getInstance();
            GenerateXML xml = new GenerateXML(activity);
            InputStream inputStream = client.httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.getOnHold(Global.S_ORDERS_ON_HOLD_DETAILS, ordID));
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            List<OrderProduct> orderProducts = new ArrayList<>();
            OrderProductsHandler orderProductsHandler = new OrderProductsHandler(activity);
            reader.beginArray();
            int i = 0;
            while (reader.hasNext()) {
                OrderProduct product = gson.fromJson(reader, OrderProduct.class);
                orderProducts.add(product);
                i++;
                if (i == 1000) {
                    orderProductsHandler.insert(orderProducts);
                    orderProducts.clear();
                    i = 0;
                }
            }
            orderProductsHandler.insert(orderProducts);
            reader.endArray();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /************************************
     * Receive Methods
     ************************************/


    private void synchAddresses(resynchAsync task) throws SAXException, IOException {

        task.updateProgress(activity.getString(R.string.sync_dload_address));
        post.postData(7, activity, "Address");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_ADDRESS);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_address));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchCategories(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_categories));
        post.postData(7, activity, "Categories");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_CATEGORIES);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_categories));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchCustomers(resynchAsync task) throws IOException, SAXException {

        task.updateProgress(activity.getString(R.string.sync_dload_cust));
        post.postData(7, activity, "Customers");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_CUSTOMERS);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_customers));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchEmpInv(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_emp_inv));
        post.postData(7, activity, "EmpInv");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_EMPLOYEE_INVOICES);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_emp_inv));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchProdInv(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_prod_inv));
        post.postData(7, activity, "InvProducts");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_PRODUCTS_INVOICES);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_prod_inv));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchInvoices(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_invoices));
        post.postData(7, activity, "Invoices");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_INVOICES);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_invoices));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchPaymentMethods(resynchAsync task) throws IOException, SAXException {
        try {
            task.updateProgress(activity.getString(R.string.sync_dload_pay_methods));
            Gson gson = JsonUtils.getInstance();
            GenerateXML xml = new GenerateXML(activity);
            InputStream inputStream = client.httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.downloadAll("PayMethods"));
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            List<PaymentMethod> methods = new ArrayList<>();
            PayMethodsHandler payMethodsHandler = new PayMethodsHandler(activity);
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
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void synchPriceLevel(resynchAsync task) throws IOException, SAXException {
        try {
            task.updateProgress(activity.getString(R.string.sync_dload_price_levels));
            Gson gson = JsonUtils.getInstance();
            GenerateXML xml = new GenerateXML(activity);
            InputStream inputStream = client.httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
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
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void synchItemsPriceLevel(resynchAsync task) throws IOException, SAXException {
        try {
            task.updateProgress(activity.getString(R.string.sync_dload_item_price_levels));
            Gson gson = JsonUtils.getInstance();
            GenerateXML xml = new GenerateXML(activity);
            InputStream inputStream = client.httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.downloadAll("PriceLevelItems"));
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            List<ItemPriceLevel> itemPriceLevels = new ArrayList<>();
            PriceLevelItemsHandler levelItemsHandler = new PriceLevelItemsHandler(activity);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchPrinters(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_printers));
        client = new HttpClient();
        GenerateXML xml = new GenerateXML(activity);
        String jsonRequest = client.httpJsonRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.downloadAll("Printers"));
        try {
            DeviceTableDAO.truncate();
            DeviceTableDAO.insert(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchProdCatXref(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_prodcatxref));
        post.postData(7, activity, "ProdCatXref");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_PRODCATXREF);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_prodcatxref));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchProdChain(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_productchainxref));
        post.postData(7, activity, "ProductChainXRef");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_PROD_CHAIN);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_productchainxref));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchProdAddon(resynchAsync task) throws IOException, SAXException {
        try {
            task.updateProgress(activity.getString(R.string.sync_dload_product_addons));
            Gson gson = JsonUtils.getInstance();
            GenerateXML xml = new GenerateXML(activity);
            InputStream inputStream = client.httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.downloadAll("Product_addons"));
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            List<ProductAddons> addonses = new ArrayList<>();
            ProductAddonsHandler addonsHandler = new ProductAddonsHandler(activity);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchProducts(resynchAsync task) throws IOException, SAXException {
        try {
            ProductsHandler productsHandler = new ProductsHandler(activity);
            task.updateProgress(activity.getString(R.string.sync_dload_products));
            Gson gson = JsonUtils.getInstance();
            GenerateXML xml = new GenerateXML(activity);
            InputStream inputStream = client.httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void postSalesAssociatesConfiguration(Activity activity, List<SalesAssociate> salesAssociates) throws Exception {
        List<DinningLocationConfiguration> configurations = new ArrayList<>();

        HashMap<String, List<SalesAssociate>> locations = SalesAssociateDAO.getSalesAssociatesByLocation();
        for (Map.Entry<String, List<SalesAssociate>> location : locations.entrySet()) {
            DinningLocationConfiguration configuration = new DinningLocationConfiguration();
            configuration.setLocationId(location.getKey());
            configuration.setSalesAssociates(location.getValue());
            configurations.add(configuration);
        }

        MyPreferences preferences = new MyPreferences(activity);
        StringBuilder url = new StringBuilder(activity.getString(R.string.sync_enablermobile_mesasconfig));
        url.append("/").append(URLEncoder.encode(preferences.getEmpID(), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getDeviceID(), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getActivKey(), GenerateXML.UTF_8));
        url.append("/").append(URLEncoder.encode(preferences.getBundleVersion(), GenerateXML.UTF_8));

        if(OAuthManager.isExpired(activity)) {
            getOAuthManager(activity);
        }
        OAuthClient authClient = OAuthManager.getOAuthClient(activity);
        Gson gson = JsonUtils.getInstance();
        String json = gson.toJson(configurations);
        oauthclient.HttpClient httpClient = new oauthclient.HttpClient();
        httpClient.post(url.toString(), json, authClient);
    }

    public static void synchSalesAssociateDinnindTablesConfiguration(Activity activity) throws IOException, SAXException {
        try {
            oauthclient.HttpClient client = new oauthclient.HttpClient();
            Gson gson = JsonUtils.getInstance();
            if(OAuthManager.isExpired(activity)) {
                getOAuthManager(activity);
            }
            OAuthClient oauthClient = OAuthManager.getOAuthClient(activity);
            String s = client.getString(activity.getString(R.string.sync_enablermobile_mesasconfig), oauthClient);
            InputStream inputStream = client.get(activity.getString(R.string.sync_enablermobile_mesasconfig), oauthClient);
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            List<DinningLocationConfiguration> configurations = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
                DinningLocationConfiguration configuration = gson.fromJson(reader, DinningLocationConfiguration.class);
                configurations.add(configuration);
            }

            reader.endArray();
            reader.close();
            for (DinningLocationConfiguration configuration : configurations) {
                for (SalesAssociate associate : configuration.getSalesAssociates()) {
                    SalesAssociateDAO.clearAllAssignedTable(associate);
                    for (DinningTable table : associate.getAssignedDinningTables()) {
                        DinningTable dinningTable = DinningTableDAO.getById(table.getId());
                        SalesAssociateDAO.addAssignedTable(associate, dinningTable);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchProductAliases(resynchAsync task) throws IOException, SAXException {
        try {
            ProductAliases_DB productAliasesDB = new ProductAliases_DB(activity);
            task.updateProgress(activity.getString(R.string.sync_dload_product_aliases));
            Gson gson = JsonUtils.getInstance();
            GenerateXML xml = new GenerateXML(activity);
            Log.d("GSon Start", new Date().toString());
            InputStream inputStream = client.httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.downloadAll("ProductAliases"));
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            Log.d("GSon Start Reading", new Date().toString());
            List<ProductAlias> productAliases = new ArrayList<>();
            productAliasesDB.emptyTable();
            reader.beginArray();
            int i = 0;
            while (reader.hasNext()) {
                ProductAlias alias = gson.fromJson(reader, Product.class);
                productAliases.add(alias);
                i++;
                if (i == 1000) {
                    productAliasesDB.insert(productAliases);
                    productAliases.clear();
                    i = 0;
                    Log.d("GSon Insert 1000", new Date().toString());
                }
            }
            productAliasesDB.insert(productAliases);
            reader.endArray();
            reader.close();
            Log.d("GSon Finish", new Date().toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void synchProductImages(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_products_images));
        post.postData(7, activity, "Products_Images");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_PROD_IMG);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_products_images));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchSalesTaxCode(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_salestaxcodes));
        post.postData(7, activity, "SalesTaxCodes");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_SALES_TAX_CODE);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_salestaxcodes));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchShippingMethods(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_shipmethod));
        post.postData(7, activity, "ShipMethod");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_SHIP_METHODS);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_shipmethod));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchTaxes(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_taxes));
        post.postData(7, activity, "Taxes");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_TAXES);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_taxes));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchTaxGroup(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_taxes_group));
        post.postData(7, activity, "Taxes_Group");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_TAX_GROUP);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_taxes_group));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchTerms(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_terms));
        post.postData(7, activity, "Terms");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_TERMS);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_terms));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchMemoText(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_memotext));
        post.postData(7, activity, "memotext");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_MEMO_TXT);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_memotext));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchGetTemplates(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_templates));
        post.postData(7, activity, "Templates");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_TEMPLATES);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_templates));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchDownloadCustomerInventory(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_customer_inventory));
        post.postData(7, activity, "GetCustomerInventory");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_CUSTOMER_INVENTORY);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_customer_inventory));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchDownloadConsignmentTransaction(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_consignment_transaction));
        post.postData(7, activity, "GetConsignmentTransaction");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_CONSIGNMENT_TRANSACTION);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_consignment_transaction));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchIvuLottoDrawDates(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_ivudrawdates));
        post.postData(7, activity, "ivudrawdates");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_IVU_LOTTO);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_ivudrawdates));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchAccountLogo(resynchAsync task) {
        GenerateXML generator = new GenerateXML(activity);
        MyPreferences myPref = new MyPreferences(activity);
        URL url;
        InputStream is;
        try {
            task.updateProgress(activity.getString(R.string.sync_dload_logo));
            url = new URL(generator.getAccountLogo());
            is = url.openStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);

            task.updateProgress(activity.getString(R.string.sync_saving_logo));
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            float scale = 0;
            if (width > 300) {
                scale = (float) 300 / width;
                width = (int) (width * scale);
                height = (int) (height * scale);
            }
            Bitmap newBitmap = Bitmap.createScaledBitmap(bmp, width, height, false);
            String externalPath = activity.getApplicationContext().getFilesDir().getAbsolutePath() + "/";
            myPref.setAccountLogoPath(externalPath + "logo.png");
            File file = new File(externalPath, "logo.png");
            OutputStream os = new FileOutputStream(file);
            newBitmap.compress(CompressFormat.PNG, 0, os);
            is.close();
            os.close();
            bmp.recycle();
            newBitmap.recycle();

        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (Exception e) {
        }
    }

    private void synchDownloadProductsAttr(resynchAsync task) throws SAXException, IOException {
        task.updateProgress(activity.getString(R.string.sync_dload_products_attributes));
        post.postData(7, activity, "ProductsAttr");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_PRODUCTS_ATTRIBUTES);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_products_attributes));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchDownloadClerks(resynchAsync task) throws SAXException, IOException {
        task.updateProgress(activity.getString(R.string.sync_dload_clerks));
        post.postData(7, activity, "Clerks");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_CLERKS);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_clerks));
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


    private void synchDownloadDinnerTable(resynchAsync task) throws SAXException, IOException {
        task.updateProgress(activity.getString(R.string.sync_dload_dinnertables));
        client = new HttpClient();
        GenerateXML xml = new GenerateXML(activity);
        String jsonRequest = client.httpJsonRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                xml.getDinnerTables());
        try {
            DinningTableDAO.truncate();
            DinningTableDAO.insert(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchDownloadMixMatch(resynchAsync task) throws SAXException, IOException {
        try {
            task.updateProgress(activity.getString(R.string.sync_dload_mixmatch));
            client = new HttpClient();
            GenerateXML xml = new GenerateXML(activity);


            InputStream inputStream = client.httpInputStreamRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void synchDownloadSalesAssociate(resynchAsync task) throws SAXException, IOException {
        try {
            task.updateProgress(activity.getString(R.string.sync_dload_salesassociate));
            client = new HttpClient();
            GenerateXML xml = new GenerateXML(activity);
            String jsonRequest = client.httpJsonRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.getSalesAssociate());
            task.updateProgress(activity.getString(R.string.sync_saving_salesassociate));
            try {
                SalesAssociateDAO.truncate();
                SalesAssociateDAO.insert(jsonRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchDownloadTermsAndConditions(resynchAsync task) throws SAXException, IOException {
        task.updateProgress(activity.getString(R.string.sync_dload_termsandconditions));
        post.postData(7, activity, "TermsAndConditions");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_TERMS_AND_CONDITIONS);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_termsandconditions));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchUoM(resynchAsync task) throws IOException, SAXException {
        try {
            task.updateProgress(activity.getString(R.string.sync_dload_uom));
            client = new HttpClient();
            GenerateXML xml = new GenerateXML(activity);
            String jsonRequest = client.httpJsonRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.downloadAll("UoM"));
            task.updateProgress(activity.getString(R.string.sync_saving_uom));
            try {
                UomDAO.truncate();
                UomDAO.insert(jsonRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void synchGetOrdProdAttr(resynchAsync task) throws IOException, SAXException {

        try {
            task.updateProgress(activity.getString(R.string.sync_dload_ordprodattr));
            client = new HttpClient();
            GenerateXML xml = new GenerateXML(activity);
            String jsonRequest = client.httpJsonRequest(activity.getString(R.string.sync_enablermobile_deviceasxmltrans) +
                    xml.downloadAll("GetOrderProductsAttr"));
            task.updateProgress(activity.getString(R.string.sync_saving_uom));
            try {
                OrderProductAttributeDAO.truncate();
                OrderProductAttributeDAO.insert(jsonRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String _server_time = "";

    private void synchGetServerTime(resynchAsync task) throws IOException, SAXException {
        task.updateProgress("Getting Server Time");
        xml = post.postData(Global.S_GET_SERVER_TIME, activity, null);
        SAXPostHandler handler = new SAXPostHandler(activity);
        inSource = new InputSource(new StringReader(xml));
        xr.setContentHandler(handler);
        xr.parse(inSource);
        _server_time = handler.getData("serverTime", 0);
    }

    private void synchUpdateSyncTime(resynchAsync task) throws IOException, SAXException {
        task.updateProgress("Updating Sync Time");
        post.postData(Global.S_UPDATE_SYNC_TIME, activity, _server_time);
    }

    private void synchEmployeeData(resynchAsync task) throws IOException, SAXException {

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SaxSelectedEmpHandler handler = new SaxSelectedEmpHandler(activity);

        try {
            task.updateProgress(activity.getString(R.string.sync_dload_employee_data));
            String xml = post.postData(4, activity, "");
            InputSource inSource = new InputSource(new StringReader(xml));
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(handler);
            task.updateProgress(activity.getString(R.string.sync_saving_employee_data));
            xr.parse(inSource);
            MyPreferences myPref = new MyPreferences(activity);
            data = handler.getEmpData();
            myPref.setAllEmpData(data);


        } catch (Exception e) {
        }
        data.clear();
    }

    private void synchDownloadLastPayID(resynchAsync task) {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SaxLoginHandler handler = new SaxLoginHandler();

        try {
            task.updateProgress(activity.getString(R.string.sync_dload_last_pay_id));
            String xml = post.postData(6, activity, "");
            InputSource inSource = new InputSource(new StringReader(xml));
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(handler);
            task.updateProgress(activity.getString(R.string.sync_saving_last_pay_id));
            xr.parse(inSource);
            if (!handler.getData().isEmpty()) {
                MyPreferences myPref = new MyPreferences(activity);
                myPref.setLastPayID(handler.getData());
            }

        } catch (Exception e) {
        }
    }

    private void synchDeviceDefaultValues(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_device_default_values));
        post.postData(7, activity, "DeviceDefaultValues");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_DEVICE_DEFAULT_VAL);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_device_default_values));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();

    }

    private void synchVolumePrices(resynchAsync task) throws IOException, SAXException {
        task.updateProgress(activity.getString(R.string.sync_dload_volume_prices));
        post.postData(7, activity, "VolumePrices");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_VOLUME_PRICES);
        File tempFile = new File(tempFilePath);
        task.updateProgress(activity.getString(R.string.sync_saving_volume_prices));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchLocations(Object task) throws IOException, SAXException {
        if (isReceive)
            ((resynchAsync) task).updateProgress(activity.getString(R.string.sync_dload_locations));
        else
            ((asyncGetLocationsInventory) task).updateProgress(activity.getString(R.string.sync_dload_locations));
        post.postData(7, activity, "GetLocations");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_LOCATIONS);
        //tempGenerateFile(false);
        File tempFile = new File(tempFilePath);
        if (isReceive)
            ((resynchAsync) task).updateProgress(activity.getString(R.string.sync_saving_locations));
        else
            ((asyncGetLocationsInventory) task).updateProgress(activity.getString(R.string.sync_saving_locations));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

    private void synchLocationsInventory(Object task) throws IOException, SAXException {
        if (isReceive)
            ((resynchAsync) task).updateProgress(activity.getString(R.string.sync_dload_locations_inventory));
        else
            ((asyncGetLocationsInventory) task).updateProgress(activity.getString(R.string.sync_dload_locations_inventory));
        post.postData(7, activity, "GetLocationsInventory");
        SAXSynchHandler synchHandler = new SAXSynchHandler(activity, Global.S_LOCATIONS_INVENTORY);
        File tempFile = new File(tempFilePath);
        if (isReceive)
            ((resynchAsync) task).updateProgress(activity.getString(R.string.sync_saving_locations_inventory));
        else
            ((asyncGetLocationsInventory) task).updateProgress(activity.getString(R.string.sync_saving_locations_inventory));
        sp.parse(tempFile, synchHandler);
        tempFile.delete();
    }

}