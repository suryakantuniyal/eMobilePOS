package com.android.emobilepos.ordering;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.dao.CustomerCustomFieldsDAO;
import com.android.dao.OrderProductAttributeDAO;
import com.android.database.AddressHandler;
import com.android.database.CustomerInventoryHandler;
import com.android.database.CustomersHandler;
import com.android.database.OrderProductsAttr_DB;
import com.android.database.OrderProductsHandler;
import com.android.database.OrdersHandler;
import com.android.database.PayMethodsHandler;
import com.android.database.ProductsHandler;
import com.android.database.SalesTaxCodesHandler;
import com.android.emobilepos.R;
import com.android.emobilepos.adapters.OrderProductListAdapter;
import com.android.emobilepos.customer.ViewCustomers_FA;
import com.android.emobilepos.firebase.PollingNotificationService;
import com.android.emobilepos.mainmenu.MainMenu_FA;
import com.android.emobilepos.mainmenu.SalesTab_FR;
import com.android.emobilepos.models.DataTaxes;
import com.android.emobilepos.models.OrderSeatProduct;
import com.android.emobilepos.models.Product;
import com.android.emobilepos.models.orders.Order;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.realms.CustomerCustomField;
import com.android.emobilepos.models.realms.OrderAttributes;
import com.android.emobilepos.models.realms.Payment;
import com.android.emobilepos.models.realms.ProductAttribute;
import com.android.emobilepos.payment.SelectPayMethod_FA;
import com.android.payments.EMSPayGate_Default;
import com.android.saxhandler.SAXProcessCardPayHandler;
import com.android.soundmanager.SoundManager;
import com.android.support.CreditCardInfo;
import com.android.support.Customer;
import com.android.support.Encrypt;
import com.android.support.GenerateNewID;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.NetworkUtils;
import com.android.support.NumberUtils;
import com.android.support.OrderProductUtils;
import com.android.support.Post;
import com.android.support.TerminalDisplay;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;
import com.bbpos.bbdevice.BBDeviceController;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.honeywell.decodemanager.DecodeManager;
import com.honeywell.decodemanager.DecodeManager.SymConfigActivityOpeartor;
import com.honeywell.decodemanager.SymbologyConfigs;
import com.honeywell.decodemanager.barcode.CommonDefine;
import com.honeywell.decodemanager.barcode.DecodeResult;
import com.honeywell.decodemanager.symbologyconfig.SymbologyConfigCodeUPCA;
import com.pax.poslink.peripheries.POSLinkScanner;
import com.pax.poslink.peripheries.ProcessResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import drivers.EMSELO;
import drivers.EMSIDTechUSB;
import drivers.EMSMagtekAudioCardReader;
import drivers.EMSRover;
import drivers.EMSUniMagDriver;
import interfaces.BCRCallbacks;
import interfaces.EMSCallBack;
import util.json.JsonUtils;
import util.json.UIUtils;

public class OrderingMain_FA extends BaseFragmentActivityActionBar implements Receipt_FR.AddProductBtnCallback,
        Receipt_FR.UpdateHeaderTitleCallback, OnClickListener, Catalog_FR.RefreshReceiptViewCallback,
        OrderLoyalty_FR.SwiperLoyaltyCallback, OrderRewards_FR.SwiperRewardCallback, EMSCallBack, BCRCallbacks {

    private static final String DATA_STRING_TAG = "com.motorolasolutions.emdk.datawedge.data_string";
    public static boolean returnItem = false;
    private static String ourIntentAction = "";
    private static String savedHeaderTitle = "";
    private static boolean msrWasLoaded = false;
    private static boolean cardReaderConnected = false;
    private static boolean wasReadFromReader = false;
    private final int SCANTIMEOUT = 500000;
    public EditText invisibleSearchMain;
    public boolean rewardsWasRead = false;
    public Global.TransactionType mTransType = null;
    public EMSIDTechUSB _msrUsbSams;
    public EMSCallBack callBackMSR;
    public boolean isToGo = true;
    public boolean openFromHold;
    public boolean openFromRecovery;
    public boolean buildOrderStarted = false;
    public Global global;
    OrderingAction orderingAction = OrderingAction.NONE;
    private String giftCardgeneratedURL;
    private TextView headerTitle;
    private LinearLayout headerContainer;
    private int orientation;
    private LinearLayout catalogContainer, receiptContainer;
    private Catalog_FR catalogFr;
    private Receipt_FR leftFragment;
    private OrderLoyalty_FR loyaltyFragment;
    private MyPreferences myPref;
    private boolean hasBeenCreated = false;
    private ProductsHandler handler;
    // Honeywell Dolphin black
    private DecodeManager mDecodeManager = null;
    private boolean scannerInDecodeMode = false;
    private EMSUniMagDriver uniMagReader;
    private EMSMagtekAudioCardReader magtekReader;
    private TextView swiperLabel;
    private EditText swiperField;
    private ProgressDialog myProgressDialog;
    private CreditCardInfo cardInfoManager;
    private Button btnCheckout;
    private Button btnScan;
    private Bundle extras;
    private Global.RestaurantSaleType restaurantSaleType = Global.RestaurantSaleType.TO_GO;
    private int selectedSeatsAmount;
    private String selectedDinningTableNumber;
    private String selectedSeatNumber = "1";
    private String associateId;
    private List<OrderAttributes> orderAttributes;
    private ArrayList<DataTaxes> listOrderTaxes;
    private boolean loyaltySwiped = false;
    private Dialog dlogMSR;
    private SoundManager soundManager;
    private POSLinkScanner posLinkScanner;
    //    public Handler receiptListHandler;
    private Handler ScanResultHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DecodeManager.MESSAGE_DECODER_COMPLETE:
                    String strDecodeResult;
                    DecodeResult decodeResult = (DecodeResult) msg.obj;

                    strDecodeResult = decodeResult.barcodeData.trim();
                    if (myPref.isRemoveLeadingZerosFromUPC()) {
                        strDecodeResult = NumberUtils.removeLeadingZeros(strDecodeResult);
                    }
                    if (!strDecodeResult.isEmpty()) {
                        soundManager.playSound(1, 1);
                        scanAddItem(strDecodeResult);
                    }
                    break;

                case DecodeManager.MESSAGE_DECODER_FAIL: {
                    soundManager.playSound(2, 1);
                }
                break;
                case DecodeManager.MESSAGE_DECODER_READY: {
                    if (mDecodeManager != null) {
                        SymConfigActivityOpeartor operator = mDecodeManager.getSymConfigActivityOpeartor();
                        operator.removeAllSymFromConfigActivity();
                        SymbologyConfigCodeUPCA upca = new SymbologyConfigCodeUPCA();
                        upca.enableSymbology(true);
                        upca.enableCheckTransmit(true);
                        upca.enableSendNumSys(true);

                        SymbologyConfigs symconfig = new SymbologyConfigs();
                        symconfig.addSymbologyConfig(upca);

                        try {
                            mDecodeManager.setSymbologyConfigs(symconfig);
                        } catch (RemoteException e) {
                            Crashlytics.logException(e);
                        }
                    }
                }
                break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };
    private BBDeviceController bbDeviceController;
    private BBPosShelpaDeviceDriver listener;


    public static void voidTransaction(Activity activity, Order order, List<ProductAttribute> ordProdAttr) {
        if (!Global.lastOrdID.isEmpty()) {

            OrdersHandler dbOrders = new OrdersHandler(activity);
            boolean existsOrder = dbOrders.existsOrder(order.ord_id);
            if (!existsOrder && order.getOrderProducts().isEmpty()) {
                return;
            } else if (!existsOrder && !order.getOrderProducts().isEmpty()) {
                Global global = (Global) activity.getApplication();
                order = Receipt_FR.buildOrder(activity, global, "", "", ((OrderingMain_FA) activity).getSelectedDinningTableNumber(),
                        ((OrderingMain_FA) activity).getAssociateId(), ((OrderingMain_FA) activity).getOrderAttributes(),
                        ((OrderingMain_FA) activity).getListOrderTaxes(), order.getOrderProducts());
                OrderProductsHandler dbOrdProd = new OrderProductsHandler(activity);
                OrderProductsAttr_DB dbOrdAttr = new OrderProductsAttr_DB(activity);
                dbOrders.insert(order);
                dbOrdProd.deleteAllOrdProd(order.ord_id);
                dbOrdProd.insert(order.getOrderProducts());
                dbOrdAttr.insert(ordProdAttr);
            }
            new VoidTransactionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, activity, order);

        }
    }

    public static void automaticAddOrder(Activity activity, boolean isFromAddon, Global global,
                                         OrderProduct orderProduct, String selectedSeatNumber, Global.TransactionType mTransType) {
        if (OrderingMain_FA.returnItem)
            orderProduct.setReturned(true);
        String val = orderProduct.getFinalPrice();
        if (val == null || val.isEmpty())
            val = "0.00";
        BigDecimal total = Global.getBigDecimalNum(Global.formatNumToLocale(Double.parseDouble(val)));
        if (isFromAddon) {
            total = total.add(Global.getBigDecimalNum(Global.formatNumToLocale(Global.addonTotalAmount)));
        }
        List<OrderProduct> list = Collections.singletonList(orderProduct);
        boolean attributeCompleted = OrderingMain_FA.isRequiredAttributeCompleted(activity, list);
        orderProduct.setAttributesCompleted(attributeCompleted);
        total = total.multiply(OrderingMain_FA.returnItem && mTransType != Global.TransactionType.RETURN ? new BigDecimal(-1) : new BigDecimal(1));
        orderProduct.setItemTotal(total.toString());
        GenerateNewID generator = new GenerateNewID(activity);
        MyPreferences myPref = new MyPreferences(activity);
        if (!Global.isFromOnHold && Global.lastOrdID.isEmpty()) {
            Global.lastOrdID = generator.getNextID(GenerateNewID.IdType.ORDER_ID);
        }
        orderProduct.setOrd_id(Global.lastOrdID);
        if (global.order.getOrderProducts() == null) {
            global.order.setOrderProducts(new ArrayList<OrderProduct>());
        }
        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();
        orderProduct.setOrdprod_id(randomUUIDString);

        // update required product attributes
        if (orderProduct.getRequiredProductAttributes() != null &&
                orderProduct.getRequiredProductAttributes().size() > 0) {
            for (ProductAttribute attribute : orderProduct.getRequiredProductAttributes()) {
                attribute.setProductId(uuid.toString());
            }
            global.ordProdAttr.addAll(orderProduct.getRequiredProductAttributes());
        }

        if (isFromAddon) {
            Global.addonTotalAmount = 0;
        }
        String row1 = orderProduct.getOrdprod_name();
        String row2 = Global.getCurrencyFormat(orderProduct.getFinalPrice());
        TerminalDisplay.setTerminalDisplay(myPref, row1, row2);
        global.order.getOrderProducts().add(orderProduct);
    }

    public static boolean isRequiredAttributeCompleted(Context context, List<OrderProduct> products) {
        for (OrderProduct product : products) {
            List<ProductAttribute> attributes = OrderProductAttributeDAO.getByProdId(product.getProd_id());
            for (ProductAttribute attribute : attributes) {
                if (fillWithCustomerAttribute(context, product, attribute)) {
                    return true;
                }
                if (!product.getRequiredProductAttributes().contains(attribute)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean fillWithCustomerAttribute(Context context, OrderProduct product, ProductAttribute attribute) {
        MyPreferences preferences = new MyPreferences(context);
        String custID = preferences.getCustID();
        CustomerCustomField customField = CustomerCustomFieldsDAO.findEMWSCardIdByCustomerId(custID);
        if (customField != null) {
            if (customField.getCustFieldId().equalsIgnoreCase("EMS_CARD_ID_NUM") &&
                    (attribute.getAttributeId().equalsIgnoreCase("EMS_CARD_ID_NUM") ||
                            attribute.getAttributeId().equalsIgnoreCase("membership_id"))) {
                attribute.setValue(customField.getCustValue());
                product.getRequiredProductAttributes().add(attribute);
                return true;
            }
        }
        return false;
    }

    public void switchHeaderTitle(boolean newTitle, String title, Global.TransactionType mTransType) {
        if (mTransType == Global.TransactionType.RETURN || newTitle) {
            savedHeaderTitle = headerTitle.getText().toString();
            headerTitle.setText(title);
            headerContainer.setBackgroundColor(Color.RED);

        } else {
            headerTitle.setText(savedHeaderTitle);
            headerContainer.setBackgroundResource(R.drawable.blue_gradient_header_horizontal);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        rewardsWasRead = savedInstanceState.getBoolean("rewardsWasRead");
        if (rewardsWasRead) {
            leftFragment.orderRewardsFr.hideTapButton();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("rewardsWasRead", rewardsWasRead);
    }

    public void disableCheckoutButton() {
        btnCheckout.setEnabled(false);
    }

    public void enableCheckoutButton() {
        btnCheckout.setEnabled(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_main_layout);
        global = (Global) getApplication();
        Global.lockOrientation(this);
        if (savedInstanceState == null) {
            global.resetOrderDetailsValues();
            global.clearListViewData();
            global.order = new Order();
        }
        listener = new BBPosShelpaDeviceDriver(this, this);
        bbDeviceController = BBDeviceController.getInstance(
                this, listener);
        callBackMSR = this;
        handler = new ProductsHandler(this);
        receiptContainer = findViewById(R.id.order_receipt_frag_container);
        catalogContainer = findViewById(R.id.order_catalog_frag_container);
        invisibleSearchMain = findViewById(R.id.invisibleSearchMain);
        btnCheckout = findViewById(R.id.btnCheckOut);
        btnCheckout.setOnClickListener(this);
        if (MyPreferences.isPaxA920()) {
            btnScan = findViewById(R.id.btnScan);
            btnScan.setVisibility(View.VISIBLE);
            btnScan.setOnClickListener(this);
        }
        myPref = new MyPreferences(this);
        if (myPref.isCustSelected()) {
            CustomersHandler customersHandler = new CustomersHandler(this);
            Customer customer = customersHandler.getCustomer(myPref.getCustID());
            SalesTaxCodesHandler taxHandler = new SalesTaxCodesHandler(this);
            SalesTaxCodesHandler.TaxableCode taxable = taxHandler.checkIfCustTaxable(customer.getCust_taxable());
            myPref.setCustTaxCode(taxable, customer.getCust_salestaxcode());
        }

        extras = getIntent().getExtras();
        mTransType = (Global.TransactionType) extras.get("option_number");
        setRestaurantSaleType((Global.RestaurantSaleType) extras.get("RestaurantSaleType"));
        selectedSeatsAmount = extras.getInt("selectedSeatsAmount", 0);
        selectedDinningTableNumber = extras.getString("selectedDinningTableNumber");
        setAssociateId(extras.getString("associateId", ""));

        openFromHold = extras.getBoolean("openFromHold", false);
        openFromRecovery = extras.getBoolean("openFromRecovery", false);
        Global.isFromOnHold = openFromHold;
        String onHoldOrderJson = extras.getString("onHoldOrderJson");
        Order onHoldOrder = null;
        Order recoveryOrder = null;
        if (onHoldOrderJson != null && !onHoldOrderJson.isEmpty()) {
            Gson gson = JsonUtils.getInstance();
            onHoldOrder = gson.fromJson(onHoldOrderJson, Order.class);
            OrdersHandler ordersHandler = new OrdersHandler(this);
            onHoldOrder = ordersHandler.getOrder(onHoldOrder.ord_id);
            onHoldOrder.setProductRequiredAttributeCompleted();
            Global.lastOrdID = onHoldOrder.ord_id;
            Global.taxID = onHoldOrder.tax_id;
        } else if (openFromRecovery) {
            OrdersHandler ordersHandler = new OrdersHandler(this);
            String recoveryOrderId = extras.getString("recoveryOrderId");
            recoveryOrder = ordersHandler.getOrder(recoveryOrderId);
            recoveryOrder.setProductRequiredAttributeCompleted();
            Global.lastOrdID = recoveryOrder.ord_id;
            Global.taxID = recoveryOrder.tax_id;
        }
        isToGo = getRestaurantSaleType() == Global.RestaurantSaleType.TO_GO;

        if (!myPref.isTablet())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (savedInstanceState == null) {
            if (!myPref.isTablet())
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            orientation = getResources().getConfiguration().orientation;
            setCatalogFr(new Catalog_FR());
            if (recoveryOrder != null) {
                leftFragment = Receipt_FR.getInstance(recoveryOrder);
            } else {
                if (onHoldOrder == null) {
                    leftFragment = new Receipt_FR();
                } else {
                    leftFragment = Receipt_FR.getInstance(onHoldOrder);
                }
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.order_receipt_frag_container, leftFragment);
            ft.add(R.id.order_catalog_frag_container, getCatalogFr());
            ft.commit();

            msrWasLoaded = false;
            cardReaderConnected = false;
            wasReadFromReader = false;
        }

        handleFragments();
        setupTitle();
        setCustomerShipToAddress();
        invisibleSearchMain.addTextChangedListener(textWatcher());
        invisibleSearchMain.requestFocus();

        ourIntentAction = getString(R.string.intentAction);
        // in case we have been launched by the DataWedge intent plug-in
        // using the StartActivity method let's handle the intent
        Intent i = getIntent();
        handleDecodeData(i);
        soundManager = SoundManager.getInstance(this);
        hasBeenCreated = true;

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (myPref.isCustomerRequired() && !myPref.isCustSelected()) {
            Intent intent = new Intent(this, ViewCustomers_FA.class);
            startActivityForResult(intent, Global.FROM_CUSTOMER_SELECTION_ACTIVITY);
        }
    }

    private void setupTitle() {
        headerTitle = findViewById(R.id.headerTitle);

        headerContainer = findViewById(R.id.headerTitleContainer);
        if (myPref.isCustSelected()) {

            switch (mTransType) {
                case SALE_RECEIPT: {
                    headerTitle.setText(R.string.sales_receipt);
                    break;
                }
                case ORDERS: {
                    headerTitle.setText(R.string.order);
                    break;
                }
                case RETURN: {
                    setReturnConfiguration(R.string.return_title);
                    break;
                }
                case INVOICE: {
                    headerTitle.setText(R.string.invoice);
                    break;
                }
                case ESTIMATE: {
                    headerTitle.setText(R.string.estimate);
                    break;
                }
                case CONSIGNMENT: {
                    if (!Global.isConsignment) {
                        CustomerInventoryHandler custInventoryHandler = new CustomerInventoryHandler(this);
                        custInventoryHandler.getCustomerInventory();
                        Global.consignSummaryMap = new HashMap<>();
                        Global.consignMapKey = new ArrayList<>();
                        Global.isConsignment = true;

                        // consignmentType 0 = Rack, 1 = Returns, 2 = Fill-up, 3 = Pick-up
                        Global.OrderType consignmentType = (Global.OrderType) extras.get("consignmentType");

                        if (Global.custInventoryKey == null || Global.custInventoryKey.size() <= 0) {
                            consignmentType = Global.OrderType.CONSIGNMENT_FILLUP;
                        }
                        Global.consignmentType = consignmentType;
                    }
                    switch (Global.consignmentType) {
                        case ORDER:
                            headerTitle.setText(R.string.consignment_stacked);
                            break;
                        case CONSIGNMENT_RETURN:
                            setReturnConfiguration(R.string.consignment_returned);
                            break;
                        case CONSIGNMENT_FILLUP:
                            headerTitle.setText(R.string.consignment_filledup);
                            break;
                        case CONSIGNMENT_PICKUP:
                            headerTitle.setText(R.string.consignment_pickup);
                            break;
                    }

                    break;
                }
                case LOCATION:// Inventory Transfer
                    headerTitle.setText(R.string.inventory_transfer);
                    break;
            }
        } else {
            switch (mTransType) {
                case SALE_RECEIPT: {
                    headerTitle.setText(R.string.sales_receipt);
                    break;
                }
                case ORDERS: {
                    headerTitle.setText(R.string.return_tag);
                    break;
                }
                case RETURN: {
                    setReturnConfiguration(R.string.return_title);
                    break;
                }
                case LOCATION:// Inventory Transfer
                    headerTitle.setText(R.string.inventory_transfer);
                    break;
            }
        }
    }

    private void setReturnConfiguration(int titleResId) {
        headerTitle.setText(getString(titleResId));
        headerContainer.setBackgroundColor(Color.RED);
        if (mTransType != Global.TransactionType.RETURN) {
            OrderingMain_FA.returnItem = true;
        }
    }

    private void handleFragments() {
        int _orientation = getResources().getConfiguration().orientation;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (leftFragment == null)
            leftFragment = (Receipt_FR) fm.findFragmentById(R.id.order_receipt_frag_container);
        if (getCatalogFr() == null)
            setCatalogFr((Catalog_FR) fm.findFragmentById(R.id.order_catalog_frag_container));
        if (orientation != _orientation) // screen orientation occurred
        {
            if (_orientation == Configuration.ORIENTATION_PORTRAIT) {
                catalogContainer.setVisibility(View.GONE);
            } else // changing from Portrait to Landscape
            {
                catalogContainer.setVisibility(View.VISIBLE);
            }
            ft.commit();
        } else {
            if (_orientation == Configuration.ORIENTATION_PORTRAIT) {
                catalogContainer.setVisibility(View.GONE);
            } else {
                catalogContainer.setVisibility(View.VISIBLE);

            }
        }
        orientation = _orientation;
    }

    @Override
    public void onBackPressed() {
        boolean tablet = myPref.isTablet();
        orderingAction = OrderingAction.BACK_PRESSED;
        if (catalogContainer.getVisibility() == View.VISIBLE && (!tablet || orientation == Configuration.ORIENTATION_PORTRAIT)) {
            catalogContainer.setVisibility(View.GONE);
            receiptContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_left_right));
            receiptContainer.setVisibility(View.VISIBLE);
        } else {
            if (openFromHold)
                showDlog(true);
            else
                showDlog(false);
        }
    }

    @Override
    public void addProductServices() {
        catalogContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_right_left));
        catalogContainer.setVisibility(View.VISIBLE);
        receiptContainer.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (UIUtils.singleOnClick(v)) {
            if (!buildOrderStarted) {
                switch (v.getId()) {
                    case R.id.btnCheckOut:
                        orderingAction = OrderingAction.CHECKOUT;
                        if (leftFragment != null) {
                            leftFragment.checkoutOrder();
                        }
                        break;
                    case R.id.btnScan:
                        paxScan();
                        break;
                    case R.id.headerMenubutton:
                        showSeatHeaderPopMenu(v);
                        break;
                }
            } else {
                Log.d("Checkout", "Checkout clicks bypass");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.toggleEloBCR: {
                if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null) {
                    Global.mainPrinterManager.getCurrentDevice().toggleBarcodeReader();
                } else {
                    if (bbDeviceController != null) {
                        scannerInDecodeMode = true;
                        bbDeviceController.startBarcodeReader();
                        bbDeviceController.getBarcode();
                    }
                }
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSeatHeaderPopMenu(final View v) {
        final OrderSeatProduct orderSeatProduct = (OrderSeatProduct) v.getTag();
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.receiptlist_header_menu, popup.getMenu());
        final HashMap<Integer, String> subMenus = new HashMap<>();
        final HashMap<Integer, String> subMenusJoinSeat = new HashMap<>();
        leftFragment.receiptListView.smoothScrollToPosition(leftFragment.mainLVAdapter.orderSeatProductList.indexOf(orderSeatProduct));
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.deleteSeat:
                        removeSeat(orderSeatProduct.seatNumber);
                        if (leftFragment.mainLVAdapter.orderSeatProductList.size() > 0) {
                            setSelectedSeatNumber(leftFragment.mainLVAdapter.orderSeatProductList.get(0).seatNumber);
                        }
                        break;
                    case R.id.moveSeatItems:
                        int id = 0;
                        for (OrderSeatProduct seatProduct : leftFragment.mainLVAdapter.orderSeatProductList) {
                            if (seatProduct.rowType == OrderProductListAdapter.RowType.TYPE_HEADER) {
                                if (!seatProduct.seatNumber.equalsIgnoreCase(orderSeatProduct.seatNumber)) {
                                    item.getSubMenu().add(0, id, SubMenu.NONE, getString(R.string.move_items_to_seat) + seatProduct.seatNumber);
                                    subMenus.put(id, seatProduct.seatNumber);
                                    id++;
                                }
                            }
                        }
                        break;
                    case R.id.joinSeats:
                        int jid = 100;
                        for (OrderSeatProduct seatProduct : leftFragment.mainLVAdapter.orderSeatProductList) {
                            if (seatProduct.rowType == OrderProductListAdapter.RowType.TYPE_HEADER) {
                                if (!seatProduct.seatNumber.equalsIgnoreCase(orderSeatProduct.seatNumber)) {
                                    item.getSubMenu().add(0, jid, SubMenu.NONE, getString(R.string.join_with_seat) + seatProduct.seatNumber);
                                    subMenusJoinSeat.put(jid, seatProduct.seatNumber);
                                    jid++;
                                }
                            }
                        }
                        break;
                    default:
                        if (subMenus.containsKey(item.getItemId())) {
                            String targetSeat = subMenus.get(item.getItemId());
                            setSelectedSeatNumber(targetSeat);
                            leftFragment.mainLVAdapter.moveSeatItems(leftFragment.mainLVAdapter.getOrderProducts(orderSeatProduct.seatNumber), targetSeat);
                        } else if (subMenusJoinSeat.containsKey(item.getItemId())) {
                            String targetSeatNumber = subMenusJoinSeat.get(item.getItemId());
                            OrderSeatProduct targetSeat = leftFragment.mainLVAdapter.getSeat(targetSeatNumber);
                            leftFragment.mainLVAdapter.joinSeatsGroupId(orderSeatProduct.getSeatGroupId(), targetSeat.getSeatGroupId());
                            leftFragment.mainLVAdapter.notifyDataSetChanged();
                        }

                        break;
                }
                return true;
            }
        });
        List<OrderProduct> orderProducts = leftFragment.mainLVAdapter.getOrderProducts(orderSeatProduct.seatNumber);
        popup.getMenu().findItem(R.id.deleteSeat).setEnabled(orderProducts.isEmpty() && leftFragment.mainLVAdapter.getSeatsAmount() > 1);
        popup.getMenu().findItem(R.id.moveSeatItems).setEnabled(!orderProducts.isEmpty() && leftFragment.mainLVAdapter.getSeatsAmount() > 1);
        popup.getMenu().findItem(R.id.joinSeats).setEnabled(!orderProducts.isEmpty() && leftFragment.mainLVAdapter.getSeatsAmount() > 1);
        popup.show();
    }

    private void removeSeat(String seatNumber) {
        leftFragment.mainLVAdapter.removeSeat(seatNumber);
        leftFragment.mainLVAdapter.notifyDataSetChanged();
        leftFragment.reCalculate();
    }

    @Override
    public void refreshView() {
        if (leftFragment != null)
            leftFragment.refreshView();
        if (getCatalogFr() != null)
            getCatalogFr().refreshListView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        buildOrderStarted = false;
        if (resultCode == Global.FROM_ORDER_ATTRIBUTES_ACTIVITY) {
            Gson gson = JsonUtils.getInstance();
            Type listType = new com.google.gson.reflect.TypeToken<List<OrderAttributes>>() {
            }.getType();
            orderAttributes = gson.fromJson(data.getStringExtra("orderAttributesValue"), listType);
        } else if (resultCode == Global.FROM_DRAW_RECEIPT_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (resultCode == 1) {
            Bundle extras = data.getExtras();
            String newName = extras.getString("customer_name");
            Global.taxID = "";
            leftFragment.setCustName();
            if (leftFragment.orderTotalDetailsFr != null) {
                leftFragment.orderTotalDetailsFr.initSpinners();
            }
            if (getCatalogFr() != null) {
                getCatalogFr().loadCursor();
            }

            prefetchLoyalty(true);
            ProductsHandler productsHandler = new ProductsHandler(this);
            List<OrderProduct> toRemove = new ArrayList<>();
            List<OrderProduct> list = new ArrayList<>();
            list.addAll(global.order.getOrderProducts());
            for (OrderProduct orderProduct : list) {
                Product product = productsHandler.getUPCProducts(orderProduct.getProd_id(), true);
                getCatalogFr().automaticAddOrder(product,null,null);
                toRemove.add(orderProduct);
            }
            global.order.getOrderProducts().removeAll(toRemove);
            if (getLeftFragment().orderTotalDetailsFr != null) {
                getLeftFragment().orderTotalDetailsFr.initSpinners();
            }

        } else if (resultCode == -1 || resultCode == 3) // Void transaction from Sales Receipt
        {
            global.resetOrderDetailsValues();
            global.clearListViewData();
            Global.showCDTDefault(this);
            reloadDefaultTransaction();
        } else if (resultCode == 2 || resultCode == 0) {
            this.refreshView();
        } else if (resultCode == Global.FROM_CUSTOMER_SELECTION_ACTIVITY) {
            Bundle extras = data.getExtras();
            boolean goto_main = extras.getBoolean("GOTO_MAIN", false);
            if (goto_main) {
                finish();
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 0) {
            fragOnKeyDown(keyCode);
            return true;
        } else if (keyCode == 138) {
            if (bbDeviceController != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bbDeviceController.stopBarcodeReader();
                        bbDeviceController.startBarcodeReader();
                    }
                }).start();
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 138) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == 138) {
            if (bbDeviceController != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bbDeviceController.startBarcodeReader();
                        bbDeviceController.getBarcode();
                    }
                }).start();
            }
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }


    private void reloadDefaultTransaction() {
        if (myPref.getPreferencesValue(MyPreferences.pref_default_transaction).equals("-1"))
            finish();
        else {
            if (myPref.isClearCustomerAfterTransaction()) {
                myPref.resetCustInfo(getString(R.string.no_customer));
            }
            String value = myPref.getPreferencesValue(MyPreferences.pref_default_transaction);
            Global.TransactionType type = Global.TransactionType.getByCode(Integer.parseInt(value));
            SalesTab_FR.startDefault(this, type);
            finish();
        }
    }

    @Override
    public void onResume() {
        buildOrderStarted = false;
        if ((myPref.isPollingHoldsEnable()
                || myPref.isAutoSyncEnable())
                && !PollingNotificationService.isServiceRunning(this)) {
            MainMenu_FA.checkPlayServices(this);
        }
        if (global.isApplicationSentToBackground())
            Global.loggedIn = false;
        global.stopActivityTransitionTimer();
        if (hasBeenCreated && !Global.loggedIn) {
            if (global.getGlobalDlog() != null && global.getGlobalDlog().isShowing()) {
                global.getGlobalDlog().dismiss();
            }
            global.promptForMandatoryLogin(this);
        }

        if (myPref.isDolphin(true, false) && mDecodeManager == null) {
            mDecodeManager = new DecodeManager(this, ScanResultHandler);
            try {
                mDecodeManager.disableSymbology(CommonDefine.SymbologyID.SYM_CODE39);
                mDecodeManager.setSymbologyDefaults(CommonDefine.SymbologyID.SYM_UPCA);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (Global.deviceHasBarcodeScanner(myPref.getPrinterType()) ||
                Global.deviceHasBarcodeScanner(myPref.getSwiperType())
                || Global.deviceHasBarcodeScanner(myPref.sledType(true, -2))) {
            if (Global.btSwiper != null && Global.btSwiper.getCurrentDevice() != null)
                Global.btSwiper.getCurrentDevice().loadScanner(callBackMSR);
            if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null)
                Global.mainPrinterManager.getCurrentDevice().loadScanner(callBackMSR);
            if (Global.btSled != null && Global.btSled.getCurrentDevice() != null)
                Global.btSled.getCurrentDevice().loadScanner(callBackMSR);
        }
        if (bbDeviceController != null) {
            bbDeviceController.startBarcodeReader();
        }
        super.onResume();
    }

    public void prefetchLoyalty(boolean isLoyalty) {
        if (myPref.isCustSelected() && myPref.isGiftCardAutoBalanceRequest()) {
            if (!TextUtils.isEmpty(myPref.getCustID())) {
                CustomerCustomField customField = CustomerCustomFieldsDAO.findEMWSCardIdByCustomerId(myPref.getCustID());
                if (customField != null) {
                    processBalanceInquiry(isLoyalty, customField.getCustValue());
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bbDeviceController != null) {
            bbDeviceController.stopBarcodeReader();
        }
        if (myPref.getPreferencesValue(MyPreferences.pref_default_transaction).equals("-1")
                || (!myPref.getPreferencesValue(MyPreferences.pref_default_transaction).equals("-1")
                && orderingAction == OrderingAction.BACK_PRESSED)) {
            if (mDecodeManager != null) {
                try {
                    mDecodeManager.release();
                    mDecodeManager = null;
                } catch (IOException e) {
                    Crashlytics.logException(e);
                }
            }

            resetMSRValues();

            if (uniMagReader != null)
                uniMagReader.release();
            if (magtekReader != null)
                magtekReader.closeDevice();
            if (Global.btSwiper != null && Global.btSwiper.getCurrentDevice() != null)
                Global.btSwiper.getCurrentDevice().releaseCardReader();
            if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null) {
                Global.mainPrinterManager.getCurrentDevice().releaseCardReader();
                Global.mainPrinterManager.getCurrentDevice().turnOffBCR();
            }
            if (Global.btSled != null && Global.btSled.getCurrentDevice() != null)
                Global.btSled.getCurrentDevice().releaseCardReader();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        global.startActivityTransitionTimer();
    }

    private void resetMSRValues() {
        cardReaderConnected = false;
        msrWasLoaded = false;
        rewardsWasRead = false;
        wasReadFromReader = false;
    }

    @Override
    public void onNewIntent(Intent i) {
        super.onNewIntent(i);
        handleDecodeData(i);
    }

    private void setCustomerShipToAddress() {
        AddressHandler addressHandler = new AddressHandler(this);
        List<String[]> addressDownloadedItems = addressHandler.getAddress();
        int size = addressDownloadedItems.size();
        if (size == 1) {
            StringBuilder sb = new StringBuilder();
            String temp;
            for (int i = 0; i < size; i++) {

                temp = addressDownloadedItems.get(i)[1];
                if (!temp.isEmpty()) // address 1
                    sb.append(temp).append(" ");
                temp = addressDownloadedItems.get(i)[2];
                if (!temp.isEmpty()) // address 2
                    sb.append(temp).append(" ");
                temp = addressDownloadedItems.get(i)[3];
                if (!temp.isEmpty()) // address 3
                    sb.append(temp).append("\t\t");
                temp = addressDownloadedItems.get(i)[4];
                if (!temp.isEmpty()) // address country
                    sb.append(temp).append(" ");
                temp = addressDownloadedItems.get(i)[5];
                if (!temp.isEmpty()) // address city
                    sb.append(temp).append(",");
                temp = addressDownloadedItems.get(i)[6]; // address state
                if (!temp.isEmpty())
                    sb.append(temp).append(" ");

                temp = addressDownloadedItems.get(i)[7]; // address zip code
                if (!temp.isEmpty())
                    sb.append(temp);
            }
            global.setSelectedAddressString(sb.toString());
            global.setSelectedAddress(1);
        }
    }

    private void handleDecodeData(Intent i) {
        // check the intent action is for us
        if (i.getAction() != null && i.getAction().contentEquals(ourIntentAction)) {

            // get the data from the intent
            String data = i.getStringExtra(DATA_STRING_TAG);

            if (dlogMSR != null && dlogMSR.isShowing()) {
                this.cardInfoManager = Global.parseSimpleMSR(this, data);
                wasReadFromReader = true;
                cardInfoManager.setWasSwiped(true);
                if (loyaltySwiped)
                    Global.loyaltyCardInfo = cardInfoManager;
                else
                    Global.rewardCardInfo = cardInfoManager;
                swiperField.setText(cardInfoManager.getCardNumUnencrypted());
            } else {
                Product product = handler.getUPCProducts(data, false);

                if (product.getId() != null) {

                    if (myPref.getPreferences(MyPreferences.pref_fast_scanning_mode)) {
                        if (validAutomaticAddQty(product)) {
                            if (myPref.isGroupReceiptBySku(isToGo)) {
                                int foundPosition = global.checkIfGroupBySKU(this, product.getId(), "1");
                                if (foundPosition != -1) // product already
                                // exist in list
                                {
                                    global.refreshParticularOrder(OrderingMain_FA.this, foundPosition, product);
                                } else
                                    getCatalogFr().automaticAddOrder(product,null,null);
                            } else
                                getCatalogFr().automaticAddOrder(product,null,null);
                            refreshView();
                        } else {
                            Global.showPrompt(this, R.string.dlog_title_error,
                                    this.getString(R.string.limit_onhand));
                        }
                    } else {
                        getCatalogFr().searchUPC(data);
                    }
                }
            }
        }
    }

    private void showDlog(final boolean isFromOnHold) {
        final Dialog dlog = new Dialog(this, R.style.Theme_TransparentTest);
        dlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlog.setCancelable(false);
        dlog.setCanceledOnTouchOutside(false);
        dlog.setContentView(R.layout.dlog_btn_left_right_layout);

        TextView viewTitle = dlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = dlog.findViewById(R.id.dlogMessage);

        Button btnLeft = dlog.findViewById(R.id.btnDlogLeft);
        Button btnRight = dlog.findViewById(R.id.btnDlogRight);
        dlog.findViewById(R.id.btnDlogCancel).setVisibility(View.GONE);

        if (isFromOnHold) {
            viewMsg.setVisibility(View.GONE);
            viewTitle.setText(R.string.cust_dlog_choose_action);
            btnLeft.setText(R.string.button_void);
            btnRight.setText(R.string.button_cancel);
        } else {
            viewTitle.setText(R.string.warning_title);
            viewMsg.setText(R.string.warning_exit_now);
            btnLeft.setText(R.string.button_yes);
            btnRight.setText(R.string.button_no);
        }

        btnLeft.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
                if (isFromOnHold)
                {
                    leftFragment.voidCancelOnHold(1);
                    if (mTransType == Global.TransactionType.SALE_RECEIPT) // is sales receipt
                        voidTransaction(OrderingMain_FA.this, global.order, global.ordProdAttr);
                    else if (mTransType == Global.TransactionType.CONSIGNMENT) {
                        if (Global.consignment_order != null && !Global.consignment_order.ord_id.isEmpty()) {
                            OrdersHandler.deleteTransaction(OrderingMain_FA.this, Global.consignment_order.ord_id);
                        }
                        if (Global.cons_return_order != null && !Global.cons_return_order.ord_id.isEmpty()) {
                            OrdersHandler.deleteTransaction(OrderingMain_FA.this, Global.cons_return_order.ord_id);
                        }
                        if (Global.cons_fillup_order != null && !Global.cons_fillup_order.ord_id.isEmpty()) {
                            OrdersHandler.deleteTransaction(OrderingMain_FA.this, Global.cons_fillup_order.ord_id);
                        }
                        if (Global.consignment_order != null && !Global.consignment_order.ord_id.isEmpty()) {
                            OrdersHandler.deleteTransaction(OrderingMain_FA.this, Global.consignment_order.ord_id);
                        }
                    } else {
                        OrdersHandler.deleteTransaction(OrderingMain_FA.this, global.order.ord_id);
                    }
                    Intent intent = new Intent(OrderingMain_FA.this, MainMenu_FA.class);
                    startActivity(intent);
                }
                else {

                    if (mTransType == Global.TransactionType.SALE_RECEIPT) // is sales receipt
                        voidTransaction(OrderingMain_FA.this, global.order, global.ordProdAttr);
                    else if (mTransType == Global.TransactionType.CONSIGNMENT) {
                        if (Global.consignment_order != null && !Global.consignment_order.ord_id.isEmpty()) {
                            OrdersHandler.deleteTransaction(OrderingMain_FA.this, Global.consignment_order.ord_id);
                        }
                        if (Global.cons_return_order != null && !Global.cons_return_order.ord_id.isEmpty()) {
                            OrdersHandler.deleteTransaction(OrderingMain_FA.this, Global.cons_return_order.ord_id);
                        }
                        if (Global.cons_fillup_order != null && !Global.cons_fillup_order.ord_id.isEmpty()) {
                            OrdersHandler.deleteTransaction(OrderingMain_FA.this, Global.cons_fillup_order.ord_id);
                        }
                        if (Global.consignment_order != null && !Global.consignment_order.ord_id.isEmpty()) {
                            OrdersHandler.deleteTransaction(OrderingMain_FA.this, Global.consignment_order.ord_id);
                        }
                    } else {
                        OrdersHandler.deleteTransaction(OrderingMain_FA.this, global.order.ord_id);
                    }

                    global.clearListViewData();
                    Global.isInventoryTransfer = false;
                    msrWasLoaded = false;
                    cardReaderConnected = false;
                    leftFragment.mainLVAdapter.notifyDataSetChanged();
                    leftFragment.receiptListView.invalidateViews();
                    finish();
                }
            }
        });
        btnRight.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
            }
        });
        dlog.show();
    }

    public Receipt_FR getLeftFragment() {
        return leftFragment;
    }

    private TextWatcher textWatcher() {

        return new TextWatcher() {
            boolean doneScanning = false;

            @Override
            public void afterTextChanged(Editable s) {
                if (doneScanning) {
                    doneScanning = false;
                    processScan();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                UIUtils.startBCR(invisibleSearchMain, invisibleSearchMain, OrderingMain_FA.this);
                if (s.toString().contains("\n") || s.toString().contains("\r"))
                    doneScanning = true;
            }
        };
    }
    public enum EmbeddedCode{
        UPCA,EAN13
    }
    private class EmbeddedBarcode{
        String barcode = "";
        private String barcodeType = "";
        public String getSKU(){
            if(isUPCA()){
                return ""+ Integer.parseInt(barcode.substring(1,6));
            }else if(isEAN13()){
                return ""+ Integer.parseInt(barcode.substring(2,7));
            }
            return "";
        }
        public String getPriceOrWeight(){

            if(isUPCA()){
                String entero = barcode.substring(6,9);
                String decimal= barcode.substring(9,11);
                return new BigDecimal(""+entero+"."+decimal).toString();
            }else if(isEAN13()){
                String entero = barcode.substring(2,5);
                String decimal= barcode.substring(5,7);
                return new BigDecimal(""+entero+"."+decimal).toString();
            }
            return "1.0";
        }
        EmbeddedCode embeddedCode = null;
        public EmbeddedBarcode(String barcode){
            // The barcode determines the format
            this.barcode = barcode;
            if(isUPCA()){
                embeddedCode = EmbeddedCode.UPCA;
            }else if(isEAN13()){
                embeddedCode = EmbeddedCode.EAN13;
            }
        }
        public boolean isUPCA(){
            if(barcode != null && barcode.length() == 12){
                String barcodeType = barcode.substring(0,1);
                if(barcodeType.equals("2"))// Embedded Barcodes start with 2.
                    return true;
                else
                    return false;
            }else{
                return false;
            }

        }
        public boolean isEAN13(){
            if(barcode != null && barcode.length() == 13){
                String barcodeType = barcode.substring(0,2);
                if(barcodeType.equals("02"))// Embedded Barcodes start with 02.
                    return true;
                else
                    return false;
            } else{
                return false;
            }
        }
        public boolean isEmbeddedBarcode(){
            return isUPCA() || isEAN13();
        }

        public String getBarcodeType() {
            return barcodeType;
        }

        public void setBarcodeType(String barcodeType) {
            this.barcodeType = barcodeType;
        }
    }

    private void processScan() {
        if (EMSELO.isEloPaypoint2()) {
            if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null) {
                Global.mainPrinterManager.getCurrentDevice().playSound();
                Global.mainPrinterManager.getCurrentDevice().turnOnBCR();
            }
        }
        String upc = invisibleSearchMain.getText().toString().trim().replace("\n", "").replace("\r", "");
        String quantity = null;
        String price    = null;
        EmbeddedBarcode embeddedBarcode = new EmbeddedBarcode(upc);
        embeddedBarcode.setBarcodeType(myPref.getPrefEmbeddedBarcodeType());
        if(embeddedBarcode.isEmbeddedBarcode()){
            upc = embeddedBarcode.getSKU();
            if(embeddedBarcode.getBarcodeType().equalsIgnoreCase("weight")){
                quantity = embeddedBarcode.getPriceOrWeight();
            }if(embeddedBarcode.getBarcodeType().equalsIgnoreCase("price")){
                price = embeddedBarcode.getPriceOrWeight();
            }
        }
        if (!upc.isEmpty()) {
            if (myPref.isRemoveLeadingZerosFromUPC()) {
                upc = NumberUtils.removeLeadingZeros(upc);
            }
            Product product = handler.getUPCProducts(upc, false);

            if (product.getId() != null) {
                if (myPref.getPreferences(MyPreferences.pref_fast_scanning_mode)) {
                    if (validAutomaticAddQty(product)) {
                        if (myPref.isGroupReceiptBySku(isToGo)) {
                            int foundPosition = global.checkIfGroupBySKU(OrderingMain_FA.this, product.getId(), "1");
                            if (foundPosition != -1 && !OrderingMain_FA.returnItem) // product
                            {
                                global.refreshParticularOrder(OrderingMain_FA.this, foundPosition, product);
                            } else
                                getCatalogFr().automaticAddOrder(product,quantity,price);
                        } else
                            getCatalogFr().automaticAddOrder(product,quantity,price);
                        refreshView();
                        if (OrderingMain_FA.returnItem) {
                            OrderingMain_FA.returnItem = !OrderingMain_FA.returnItem;
                            switchHeaderTitle(OrderingMain_FA.returnItem, "Return", mTransType);
                        }
                    } else {
                        Global.showPrompt(OrderingMain_FA.this, R.string.dlog_title_error,
                                OrderingMain_FA.this.getString(R.string.limit_onhand));
                    }
                } else {
                    getCatalogFr().searchUPC(upc);
                }
            } else {
                Global.showPrompt(OrderingMain_FA.this, R.string.dlog_title_error,
                        getString(R.string.dlog_msg_item_not_found));
            }
        }
        invisibleSearchMain.setText("");
    }

    @Override
    public void updateHeaderTitle(String val) {
        headerTitle.setText(val);
        if ((Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP || Global.consignmentType == Global.OrderType.CONSIGNMENT_RETURN) && getCatalogFr() != null) {
            getCatalogFr().loadCursor();
        }
    }

    private void fragOnKeyDown(int key_code) {
        if (key_code == 0) {
            if (scannerInDecodeMode) {
                try {
                    mDecodeManager.cancelDecode();
                    scannerInDecodeMode = false;
                    DoScan();
                } catch (RemoteException e) {
                    Crashlytics.logException(e);
                }
            } else
                DoScan();
        } else if (key_code == 138) {
        }
    }

    // -----Honeywell scanner
    private void DoScan() {
        try {
            if (mDecodeManager != null) {
                mDecodeManager.doDecode(SCANTIMEOUT);
                scannerInDecodeMode = true;
            }
        } catch (RemoteException e) {
            Crashlytics.logException(e);
        }
    }

    private void paxScan() {
        // open scanner
        posLinkScanner = POSLinkScanner.getPOSLinkScanner(this, POSLinkScanner.ScannerType.REAR);
        ProcessResult result = posLinkScanner.open();
        if (!result.getCode().equals(ProcessResult.CODE_OK)) {
            paxScanError(result.getMessage());
        }

        // start scanner
        invisibleSearchMain.setText("");
        if (posLinkScanner == null) {
            paxScanError("Error Starting Scanner.");
            return;
        }
        posLinkScanner.start(new POSLinkScanner.ScannerListener() {
            @Override
            public void onRead(String s) {
                invisibleSearchMain.setText(String.format("%s\n", s));
            }

            @Override
            public void onFinish() {
                // close scanner
                if (posLinkScanner == null) {
                    paxScanError("Error Closing Scanner.");
                    return;
                }
                ProcessResult result = posLinkScanner.close();
                if (!result.getCode().equals(ProcessResult.CODE_OK)) {
                    paxScanError(result.getMessage());
                }
            }
        });
    }

    private void paxScanError(String errorMessage) {
        Crashlytics.log("PAX Scanner Error: " + errorMessage);
    }

    private void scanAddItem(String upc) {
        ProductsHandler handler = new ProductsHandler(this);
        Product product = handler.getUPCProducts(upc, false);
        if (product.getId() != null) {
            String quantity = "1";
            String price    = "2";
            if (myPref.getPreferences(MyPreferences.pref_fast_scanning_mode)) {
                if (validAutomaticAddQty(product)) {
                    if (myPref.isGroupReceiptBySku(isToGo)) {
                        int foundPosition = global.checkIfGroupBySKU(this, product.getId(), "1");
                        if (foundPosition != -1) {
                            global.refreshParticularOrder(OrderingMain_FA.this, foundPosition, product);
                        } else
                            getCatalogFr().automaticAddOrder(product, quantity, price);
                    } else
                        getCatalogFr().automaticAddOrder(product, quantity, price);
                    refreshView();
                } else {
                    Global.showPrompt(this, R.string.dlog_title_error, this.getString(R.string.limit_onhand));
                }

            } else {
                getCatalogFr().searchUPC(upc);
            }
        } else {
            Global.showPrompt(OrderingMain_FA.this, R.string.dlog_title_error,
                    getString(R.string.dlog_msg_item_not_found));
        }
    }

    public boolean validAutomaticAddQty(Product product) {
        List<OrderProduct> list;
        if (global.order != null && global.order.getOrderProducts() != null) {
            list = OrderProductUtils.getOrderProducts(global.order.getOrderProducts(), product.getId());
        } else {
            list = new ArrayList<>();
        }
        String addedQty = list.isEmpty() ? "0" : list.get(0).getOrdprod_qty();
        double newQty = Double.parseDouble(addedQty) + 1;
        double onHandQty = Double.parseDouble(product.getProdOnHand());
        return !((myPref.getPreferences(MyPreferences.pref_limit_products_on_hand) && !product.getProdType().equals("Service")
                && (((Global.ord_type == Global.OrderType.SALES_RECEIPT || Global.ord_type == Global.OrderType.INVOICE)
                && (newQty > onHandQty))))
                || (Global.isConsignment && !product.getProdType().equals("Service")
                && !validConsignment(newQty, onHandQty, product.getId())));
    }

    private boolean validConsignment(double selectedQty, double onHandQty, String prodID) {
        if (Global.isConsignment) {
            if (Global.consignmentType == Global.OrderType.CONSIGNMENT_FILLUP && (onHandQty <= 0 || selectedQty > onHandQty))
                return false;
            else if (Global.consignmentType != Global.OrderType.CONSIGNMENT_FILLUP && !Global.custInventoryMap.containsKey(prodID))
                return false;
            else if (Global.consignmentType != Global.OrderType.CONSIGNMENT_FILLUP) {
                if (Global.consignmentType == Global.OrderType.ORDER
                        && selectedQty > Double.parseDouble(Global.custInventoryMap.get(prodID)[2]))
                    return false;
                else if (Global.consignmentType == Global.OrderType.CONSIGNMENT_RETURN) {
                    if (Global.consignment_qtyCounter != null && Global.consignment_qtyCounter.containsKey(prodID)) // verify
                    // rack
                    {
                        double rackQty = Double.parseDouble(Global.consignment_qtyCounter.get(prodID));
                        double origQty = Double.parseDouble(Global.custInventoryMap.get(prodID)[2]);
                        if (rackQty == origQty || (rackQty + selectedQty > origQty))
                            return false;
                    }
                } else if (Global.consignmentType == Global.OrderType.CONSIGNMENT_PICKUP
                        && selectedQty > Double.parseDouble(Global.custInventoryMap.get(prodID)[2]))
                    return false;
            }

        }
        return true;
    }

    @Override
    public void startLoyaltySwiper() {
        showMSRprompt(true);
        loyaltySwiped = true;
    }

    @Override
    public void prefetchLoyaltyPoints() {
        if (myPref.isCustSelected() && myPref.isGiftCardAutoBalanceRequest()) {
            if (NetworkUtils.isConnectedToInternet(OrderingMain_FA.this)) {
                loyaltySwiped = true;
                prefetchLoyalty(true);
            }
        }
    }

    @Override
    public void startRewardSwiper() {
        showMSRprompt(false);
        loyaltySwiped = false;
    }

    @Override
    public void prefetchRewardsBalance() {
        if (NetworkUtils.isConnectedToInternet(this)) {
            loyaltySwiped = false;
            prefetchLoyalty(false);
        }
    }

    @SuppressWarnings("deprecation")
    private void setUpCardReader() {
        Global.isEncryptSwipe = false;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isWiredHeadsetOn()) {
            String _audio_reader_type = myPref.getPreferencesValue(MyPreferences.pref_audio_card_reader);
            if (_audio_reader_type != null && !_audio_reader_type.isEmpty() && !_audio_reader_type.equals("-1")) {
                switch (_audio_reader_type) {
                    case Global.AUDIO_MSR_UNIMAG:
                        uniMagReader = new EMSUniMagDriver();
                        uniMagReader.initializeReader(this);
                        break;
                    case Global.AUDIO_MSR_MAGTEK:
                        magtekReader = new EMSMagtekAudioCardReader(this);
                        new Thread(new Runnable() {
                            public void run() {
                                magtekReader.connectMagtek(true, callBackMSR);
                            }
                        }).start();
                        break;
                    case Global.AUDIO_MSR_ROVER:
                        EMSRover roverReader = new EMSRover();
                        roverReader.initializeReader(this, false);
                        break;
                }
            }

        } else {
            int _swiper_type = myPref.getSwiperType();
            int _printer_type = myPref.getPrinterType();
            if (_swiper_type != -1 && Global.btSwiper != null && Global.btSwiper.getCurrentDevice() != null
                    && !cardReaderConnected) {
                Global.btSwiper.getCurrentDevice().loadCardReader(callBackMSR, false);
            } else if (_printer_type != -1 && Global.deviceHasMSR(_printer_type)) {
                if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null
                        && !cardReaderConnected) {
                    Global.mainPrinterManager.getCurrentDevice().loadCardReader(callBackMSR, false);
                    swiperLabel.setText(R.string.connected);
                    swiperLabel.setTextColor(Color.BLUE);
                    msrWasLoaded = true;
                }
            } else {
                swiperLabel.setText(R.string.disconnected);
                swiperLabel.setTextColor(Color.RED);
            }
        }
        // }
        if (myPref.isET1(true, false) || myPref.isMC40(true, false)) {
            swiperLabel.setText(R.string.connected);
            swiperLabel.setTextColor(Color.BLUE);
            msrWasLoaded = true;
        } else if (myPref.isSam4s()) {
            _msrUsbSams = new EMSIDTechUSB(this, callBackMSR);
            _msrUsbSams.OpenDevice();
            if (_msrUsbSams.isDeviceOpen() && !_msrUsbSams.isDeviceReading())
                _msrUsbSams.StartReadingThread();

            swiperLabel.setText(R.string.connected);
            swiperLabel.setTextColor(Color.BLUE);
            msrWasLoaded = true;
        }
    }

    private void showMSRprompt(final boolean isLoyaltyCard) {
        if (_msrUsbSams != null && _msrUsbSams.isDeviceOpen() && !_msrUsbSams.isDeviceReading())
            _msrUsbSams.StartReadingThread();

        bbDeviceController = BBDeviceController.getInstance(this,
                new MyBBDeviceControllerListener(this, new EMSCallBack() {
                    @Override
                    public void cardWasReadSuccessfully(boolean read, CreditCardInfo cardManager) {

                    }

                    @Override
                    public void readerConnectedSuccessfully(boolean value) {

                    }

                    @Override
                    public void scannerWasRead(String data) {
                        swiperField.setText(data);
                    }

                    @Override
                    public void startSignature() {

                    }

                    @Override
                    public void nfcWasRead(String nfcUID) {

                    }
                }));
        if (bbDeviceController != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    bbDeviceController.startBarcodeReader();
                    bbDeviceController.getBarcode();
                }
            }).start();

        }
        dlogMSR = new Dialog(this, R.style.Theme_TransparentTest);
        dlogMSR.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlogMSR.setCancelable(false);
        dlogMSR.setCanceledOnTouchOutside(false);
        dlogMSR.setContentView(R.layout.dlog_swiper_layout);

        swiperLabel = dlogMSR.findViewById(R.id.dlogMessage);
        Button btnOK = dlogMSR.findViewById(R.id.btnDlogLeft);
        Button btnCancel = dlogMSR.findViewById(R.id.btnDlogRight);
        swiperField = dlogMSR.findViewById(R.id.dlogFieldSingle);
        swiperField.requestFocus();
        EditText swiperHiddenField = dlogMSR.findViewById(R.id.hiddenField);
        swiperHiddenField.addTextChangedListener(hiddenTxtWatcher(swiperHiddenField));
        swiperLabel.setText(R.string.loading);
        swiperLabel.setTextColor(Color.DKGRAY);
        if (myPref.isCustSelected()) {
            if (!TextUtils.isEmpty(myPref.getCustID())) {
                CustomerCustomField customField = CustomerCustomFieldsDAO.findEMWSCardIdByCustomerId(myPref.getCustID());
                if (customField != null) {
                    swiperField.setText(customField.getCustValue());
                }
            }
        }
        btnOK.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dlogMSR.dismiss();
                if (bbDeviceController != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            bbDeviceController.stopBarcodeReader();
                            bbDeviceController.startBarcodeReader();
                            reloadBBPosBCR();
                        }
                    }).start();
                }
                String temp = swiperField.getText().toString().trim();
                if (temp.length() > 0) {
                    processBalanceInquiry(isLoyaltyCard, temp);
                }
            }
        });
        btnCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dlogMSR.dismiss();
                if (bbDeviceController != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            bbDeviceController.stopBarcodeReader();
                            bbDeviceController.startBarcodeReader();
                            reloadBBPosBCR();
                        }
                    }).start();
                }
                if (_msrUsbSams != null && _msrUsbSams.isDeviceOpen() && _msrUsbSams.isDeviceReading())
                    _msrUsbSams.StopReadingThread();
            }
        });
        dlogMSR.show();
        if (!msrWasLoaded)
            setUpCardReader();
        else {
            swiperLabel.setText(R.string.connected);
            swiperLabel.setTextColor(Color.BLUE);
        }
    }

    private void reloadBBPosBCR() {
        if (bbDeviceController != null) {
            bbDeviceController = BBDeviceController.getInstance(
                    this, listener);
            bbDeviceController.startBarcodeReader();
        }
    }

    private TextWatcher hiddenTxtWatcher(final EditText hiddenField) {

        return new TextWatcher() {
            boolean doneScanning = false;
            String temp;

            @Override
            public void afterTextChanged(Editable s) {
                if (doneScanning) {
                    doneScanning = false;
                    String data = hiddenField.getText().toString().replace("\n", "");
                    hiddenField.setText("");
                    cardInfoManager = Global.parseSimpleMSR(OrderingMain_FA.this, data);
                    swiperField.setText(cardInfoManager.getCardNumUnencrypted());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                temp = s.toString();

                if (temp.contains(";") && temp.contains("?") && temp.contains("\n")) {
                    doneScanning = true;
                }

            }
        };
    }

    private void populateCardInfo(String cardNumber) {
        if (!wasReadFromReader) {

            Encrypt encrypt = new Encrypt(this);
            cardInfoManager = new CreditCardInfo();
            int size = cardNumber.length();
            if (size > 4) {
                String last4Digits = (String) cardNumber.subSequence(size - 4, size);
                cardInfoManager.setCardLast4(last4Digits);
            }
            cardInfoManager.setCardNumAESEncrypted(encrypt.encryptWithAES(cardNumber));
            cardInfoManager.setCardNumUnencrypted(cardNumber);
            cardInfoManager.setWasSwiped(false);
            if (loyaltySwiped)
                Global.loyaltyCardInfo = cardInfoManager;
            else
                Global.rewardCardInfo = cardInfoManager;
        }
    }

    private void processBalanceInquiry(boolean isLoyaltyCard, String cardNumber) {
        Payment payment = new Payment(this);
        populateCardInfo(cardNumber);

        if (isLoyaltyCard)
            payment.setPaymethod_id(PayMethodsHandler.getPayMethodID("LoyaltyCard"));
        else
            payment.setPaymethod_id(PayMethodsHandler.getPayMethodID("Reward"));

        payment.setPay_name(cardInfoManager.getCardOwnerName());
        payment.setPay_ccnum(cardInfoManager.getCardNumAESEncrypted());

        payment.setCcnum_last4(cardInfoManager.getCardLast4());
        payment.setPay_expmonth(cardInfoManager.getCardExpMonth());
        payment.setPay_expyear(cardInfoManager.getCardExpYear());
        payment.setPay_seccode(cardInfoManager.getCardEncryptedSecCode());

        payment.setTrack_one(cardInfoManager.getEncryptedAESTrack1());
        payment.setTrack_two(cardInfoManager.getEncryptedAESTrack2());

        String cardType = "LoyaltyCard";
        if (!isLoyaltyCard)
            cardType = "Reward";

        payment.setCard_type(cardType);
        payment.setPay_type("0");

        EMSPayGate_Default payGate = new EMSPayGate_Default(this, payment);
        String generatedURL;

        if (isLoyaltyCard) {
            generatedURL = payGate.paymentWithAction(EMSPayGate_Default.EAction.BalanceLoyaltyCardAction, wasReadFromReader, cardType,
                    cardInfoManager);
        } else {
            generatedURL = payGate.paymentWithAction(EMSPayGate_Default.EAction.BalanceRewardAction, wasReadFromReader, cardType,
                    cardInfoManager);
        }
        payGate.reset();
        giftCardgeneratedURL = payGate.paymentWithAction(EMSPayGate_Default.EAction.BalanceGiftCardAction, wasReadFromReader, "GiftCard",
                cardInfoManager);
        new ProcessAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, generatedURL);
    }

    public int getSelectedSeatsAmount() {
        return selectedSeatsAmount;
    }

    public String getSelectedDinningTableNumber() {
        return selectedDinningTableNumber;
    }

    public String getSelectedSeatNumber() {
        return selectedSeatNumber;
    }

    public void setSelectedSeatNumber(String seatNumber) {
        selectedSeatNumber = seatNumber;

    }

    public ArrayList<DataTaxes> getListOrderTaxes() {
        return listOrderTaxes;
    }

    public void setListOrderTaxes(ArrayList<DataTaxes> listOrderTaxes) {
        this.listOrderTaxes = listOrderTaxes;
    }

    public Global.RestaurantSaleType getRestaurantSaleType() {
        return restaurantSaleType;
    }

    public void setRestaurantSaleType(Global.RestaurantSaleType restaurantSaleType) {
        if (restaurantSaleType == null) {
            this.restaurantSaleType = Global.RestaurantSaleType.TO_GO;
        } else {
            this.restaurantSaleType = restaurantSaleType;
        }
    }

    @Override
    public void cardWasReadSuccessfully(boolean read, CreditCardInfo cardManager) {
        wasReadFromReader = true;
        this.cardInfoManager = cardManager;
        cardInfoManager.setWasSwiped(true);
        if (loyaltySwiped)
            Global.loyaltyCardInfo = cardInfoManager;
        else
            Global.rewardCardInfo = cardInfoManager;
        if (swiperField != null) {
            swiperField.setText(cardManager.getCardNumUnencrypted());
        }
        if (uniMagReader != null && uniMagReader.readerIsConnected()) {
            uniMagReader.startReading();
        } else if (magtekReader == null && Global.btSwiper == null && _msrUsbSams == null
                && Global.mainPrinterManager != null)
            Global.mainPrinterManager.getCurrentDevice().loadCardReader(callBackMSR, false);
    }

    @Override
    public void readerConnectedSuccessfully(boolean didConnect) {
        if (didConnect) {
            msrWasLoaded = true;
            cardReaderConnected = true;
            if (uniMagReader != null && uniMagReader.readerIsConnected())
                uniMagReader.startReading();
            swiperLabel.setText(R.string.connected);
            swiperLabel.setTextColor(Color.BLUE);
        } else {
            cardReaderConnected = false;
            swiperLabel.setText(R.string.disconnected);
            swiperLabel.setTextColor(Color.RED);
        }
    }

    @Override
    public void scannerWasRead(String data) {
        soundManager.playSound(1, 1);
        scannerInDecodeMode = false;
        if (!data.isEmpty()) {
            if (myPref.isRemoveLeadingZerosFromUPC()) {
                data = NumberUtils.removeLeadingZeros(data);
            }
            scanAddItem(data);
        }
    }

    @Override
    public void startSignature() {
    }

    @Override
    public void nfcWasRead(String nfcUID) {

    }

    public String getAssociateId() {
        return associateId;
    }

    public void setAssociateId(String associateId) {
        this.associateId = associateId;
    }

    public List<OrderAttributes> getOrderAttributes() {
        return orderAttributes;
    }

    public void setOrderAttributes(List<OrderAttributes> orderAttributes) {
        this.orderAttributes = orderAttributes;
    }

    public OrderLoyalty_FR getLoyaltyFragment() {
        return loyaltyFragment;
    }

    public void setLoyaltyFragment(OrderLoyalty_FR loyaltyFragment) {
        this.loyaltyFragment = loyaltyFragment;
    }

    public Catalog_FR getCatalogFr() {
        return catalogFr;
    }

    public void setCatalogFr(Catalog_FR catalogFr) {
        this.catalogFr = catalogFr;
    }

    @Override
    public void executeBCR() {
        processScan();
        Log.d("Scan callback:", invisibleSearchMain.getText().toString());
    }

    public enum OrderingAction {
        HOLD, CHECKOUT, NONE, BACK_PRESSED
    }

    private static class VoidTransactionTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            Order order = (Order) params[1];
            SelectPayMethod_FA.voidTransaction((Activity) params[0], order.ord_id, order.ord_type);

            return null;
        }
    }

    private class ProcessAsync extends AsyncTask<String, String, HashMap<String, String>[]> {
        private String urlToPost;
        private boolean wasProcessed = false;
        private String errorMsg = "Request could not be processed.";

        @Override
        protected void onPreExecute() {
            myProgressDialog = new ProgressDialog(OrderingMain_FA.this);
            myProgressDialog.setMessage("Processing Balance Inquiry...");
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();

            if (_msrUsbSams != null && _msrUsbSams.isDeviceOpen()) {
                _msrUsbSams.CloseTheDevice();
            }
        }

        @Override
        protected HashMap<String, String>[] doInBackground(String... params) {
            Post httpClient = new Post(OrderingMain_FA.this);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXProcessCardPayHandler handler = new SAXProcessCardPayHandler();
            urlToPost = params[0];
            HashMap[] parsedMap = new HashMap[2];

            try {
                String xml = httpClient.postData(13, urlToPost);
                String xmlGiftCard = httpClient.postData(13, giftCardgeneratedURL);
                switch (xml) {
                    case Global.TIME_OUT:
                        errorMsg = getString(R.string.timeout_try_again);
                        break;
                    case Global.NOT_VALID_URL:
                        errorMsg = getString(R.string.can_not_proceed);
                        break;
                    default:
                        InputSource inSource = new InputSource(new StringReader(xml));
                        SAXParser sp = spf.newSAXParser();
                        XMLReader xr = sp.getXMLReader();
                        xr.setContentHandler(handler);
                        xr.parse(inSource);
                        parsedMap[0] = getHashMap(xml);
                        parsedMap[1] = getHashMap(xmlGiftCard);
                        if (parsedMap[0] != null && parsedMap[0].size() > 0 && parsedMap[0].get("epayStatusCode").equals("APPROVED"))
                            wasProcessed = true;
                        else if (parsedMap[0] != null && parsedMap[0].size() > 0) {
                            errorMsg = "statusCode = " + parsedMap[0].get("statusCode") + "\n" + parsedMap[0].get("statusMessage");
                        } else
                            errorMsg = xml;


                        break;
                }

            } catch (Exception e) {
                Crashlytics.logException(e);
                wasProcessed = false;
                errorMsg = e.getMessage();
            }
            return parsedMap;
        }

        @Override
        protected void onPostExecute(HashMap<String, String>[] parsedMap) {
            if (myProgressDialog != null && myProgressDialog.isShowing()) {
                myProgressDialog.dismiss();
            }
            if (wasProcessed && parsedMap[0] != null) {
                String temp = (parsedMap[0].get("CardBalance") == null ? "0.0" : parsedMap[0].get("CardBalance"));
                String giftBalance = (parsedMap[1] == null || parsedMap[1].get("CardBalance") == null ? "0.0" : parsedMap[1].get("CardBalance"));
                LinearLayout ll = findViewById(R.id.giftBalanceLinearLayout);
                ll.setVisibility(View.VISIBLE);
                TextView giftBalanceTextView = findViewById(R.id.giftBalancetextView44);
                if (giftBalanceTextView != null) {
                    giftBalanceTextView.setText(giftBalance);
                }
                if (loyaltySwiped && loyaltyFragment != null) {
                    loyaltyFragment.hideTapButton();
                    loyaltyFragment.setPointBalance(temp);
                } else if (leftFragment.orderRewardsFr != null) {
                    leftFragment.orderRewardsFr.hideTapButton();
                    leftFragment.orderRewardsFr.setRewardBalance(temp);
                    rewardsWasRead = true;
                }
            } else {
                Global.showPrompt(OrderingMain_FA.this, R.string.dlog_title_error, errorMsg);
            }
        }

        private HashMap<String, String> getHashMap(String xml) throws ParserConfigurationException, SAXException, IOException {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXProcessCardPayHandler handler = new SAXProcessCardPayHandler();
            InputSource inSource = new InputSource(new StringReader(xml));
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(handler);
            xr.parse(inSource);
            HashMap<String, String> parsedMap = handler.getData();
            return parsedMap;
        }

    }

}
