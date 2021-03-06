package com.android.emobilepos.mainmenu;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dao.AssignEmployeeDAO;
import com.android.dao.ClerkDAO;
import com.android.dao.DinningTableDAO;
import com.android.dao.ShiftDAO;
import com.android.database.CustomersHandler;
import com.android.database.SalesTaxCodesHandler;
import com.android.emobilepos.OnHoldActivity;
import com.android.emobilepos.R;
import com.android.emobilepos.adapters.DinningTableSeatsAdapter;
import com.android.emobilepos.adapters.SalesMenuAdapter;
import com.android.emobilepos.cardmanager.GiftCard_FA;
import com.android.emobilepos.cardmanager.LoyaltyCard_FA;
import com.android.emobilepos.cardmanager.RewardCard_FA;
import com.android.emobilepos.consignment.ConsignmentMain_FA;
import com.android.emobilepos.customer.ViewCustomerDetails_FA;
import com.android.emobilepos.customer.ViewCustomers_FA;
import com.android.emobilepos.history.HistoryOpenInvoices_FA;
import com.android.emobilepos.holders.Locations_Holder;
import com.android.emobilepos.initialization.SelectAccount_FA;
import com.android.emobilepos.locations.LocationsPickerDlog_FR;
import com.android.emobilepos.locations.LocationsPicker_Listener;
import com.android.emobilepos.mainmenu.restaurant.DinningTablesActivity;
import com.android.emobilepos.models.BCRMacro;
import com.android.emobilepos.models.realms.BiometricFid;
import com.android.emobilepos.models.realms.Clerk;
import com.android.emobilepos.models.realms.DinningTable;
import com.android.emobilepos.models.realms.EmobileBiometric;
import com.android.emobilepos.models.realms.Shift;
import com.android.emobilepos.ordering.OrderingMain_FA;
import com.android.emobilepos.ordering.SplittedOrderSummary_FA;
import com.android.emobilepos.payment.SelectPayMethod_FA;
import com.android.emobilepos.payment.TipAdjustmentFA;
import com.android.emobilepos.security.SecurityManager;
import com.android.emobilepos.settings.SettingListActivity;
import com.android.emobilepos.shifts.ShiftExpensesList_FA;
import com.android.emobilepos.shifts.ShiftsActivity;
import com.android.support.CreditCardInfo;
import com.android.support.Customer;
import com.android.support.DeviceUtils;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.NumberUtils;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.pax.poslink.peripheries.POSLinkScanner;
import com.pax.poslink.peripheries.ProcessResult;

import java.util.Collection;
import java.util.HashMap;

import drivers.EMSDeviceDriver;
import drivers.EMSPowaPOS;
import drivers.EMSmePOS;
import drivers.digitalpersona.DigitalPersona;
import interfaces.BCRCallbacks;
import interfaces.BiometricCallbacks;
import interfaces.EMSCallBack;
import util.StringUtil;
import util.json.JsonUtils;
import util.json.UIUtils;

import static com.android.support.Global.FROM_CUSTOMER_SELECTION_ACTIVITY;

public class SalesTab_FR extends Fragment implements BiometricCallbacks, BCRCallbacks, EMSCallBack {
    EMSCallBack emsCallBack;
    private SalesMenuAdapter myAdapter;
    private GridView myListview;
    private Context thisContext;
    private boolean isCustomerSelected = false;
    private TextView selectedCust;
    private MyPreferences myPref;
    private Button salesInvoices;
    private EditText hiddenField;
    private DinningTable selectedDinningTable;
    private int selectedSeatsAmount;
    private DigitalPersona digitalPersona;
    private boolean isReaderConnected;
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Global.loggedIn) {
                digitalPersona.loadForScan();
            } else {
                digitalPersona.releaseReader();
            }
            if (Global.deviceHasBarcodeScanner(myPref.getPrinterType()) ||
                    Global.deviceHasBarcodeScanner(myPref.getSwiperType())
                    || Global.deviceHasBarcodeScanner(myPref.sledType(true, -2))) {
                if (Global.btSwiper != null && Global.btSwiper.getCurrentDevice() != null)
                    Global.btSwiper.getCurrentDevice().loadScanner(emsCallBack);
                if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null)
                    if (emsCallBack != null) {
                        Global.mainPrinterManager.getCurrentDevice().loadScanner(emsCallBack);
                    }
                if (Global.btSled != null && Global.btSled.getCurrentDevice() != null)
                    if (emsCallBack != null) {
                        Global.btSled.getCurrentDevice().loadScanner(emsCallBack);
                    }
            }
        }
    };
    private POSLinkScanner posLinkScanner;

    public static void checkAutoLogout(Activity activity) {
        MyPreferences myPref = new MyPreferences(activity);
        if (myPref.isUseClerksAutoLogout() && Global.loggedIn) {
            Global.loggedIn = false;
            Global global = (Global) activity.getApplication();
            global.promptForMandatoryLogin(activity);
        }
    }

    public static void startDefault(Activity activity, Global.TransactionType type) {
        if (type != null) {
            if (validateClerkShift(type, activity))
                if (activity != null && type != null) {
                    Intent intent = new Intent(activity, OrderingMain_FA.class);
                    intent.putExtra("option_number", type);
                    activity.startActivityForResult(intent, 0);
                }
        }
    }

    private static boolean validateClerkShift(Global.TransactionType transactionType, Context context) {
        SecurityManager.SecurityResponse response = SecurityManager.validateClerkShift(context, transactionType);
        switch (response) {
            case CHECK_USER_CLERK_REQUIRED_SETTING:
                Global.showPrompt(context, R.string.dlog_title_error, context.getString(R.string.use_clerk_check_required));
                return false;
            case OPEN_SHIFT_REQUIRED:
                if (transactionType != Global.TransactionType.SHIFTS) {
                    Global.showPrompt(context, R.string.dlog_title_error, context.getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    return false;
                }
                break;
            case SHIFT_ALREADY_OPEN:
                Shift openShift = ShiftDAO.getOpenShift();
                Clerk clerk = null;
                if (openShift != null) {
                    clerk = ClerkDAO.getByEmpId(openShift.getClerkId());
                }
                Global.showPrompt(context, R.string.dlog_title_error,
                        String.format(context.getString(R.string.dlog_msg_error_shift_already_open), clerk != null ? clerk.getEmpName() : ""));
                return false;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        checkPermissions();
        View view = inflater.inflate(R.layout.sales_layout, container, false);
        Button btnScan = null;
        if (MyPreferences.isPaxA920()) {
            btnScan = view.findViewById(R.id.btnScan);
            if(btnScan != null){
                btnScan.setVisibility(View.VISIBLE);
                btnScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        paxScan();
                    }
                });
            }
        }
        myPref = new MyPreferences(getActivity());
        myPref.setLogIn(true);
        SettingListActivity.loadDefaultValues(getActivity());
        myListview = view.findViewById(R.id.salesGridLayout);
        emsCallBack = this;
        thisContext = getActivity();
        selectedCust = view.findViewById(R.id.salesCustomerName);
        salesInvoices = view.findViewById(R.id.invoiceButton);
        hiddenField = view.findViewById(R.id.hiddenField);
        if(hiddenField != null){
            hiddenField.addTextChangedListener(textWatcher());
            hiddenField.requestFocus();
        }

        Collection<UsbDevice> usbDevices = DeviceUtils.getUSBDevices(getActivity());
        isReaderConnected = usbDevices != null && usbDevices.size() > 0;
        digitalPersona = new DigitalPersona(getActivity().getApplicationContext(), this, EmobileBiometric.UserType.CUSTOMER);
        if (isTablet())
            myPref.setIsTablet(true);
        else
            myPref.setIsTablet(false);

        if (myPref.isCustSelected()) {
            isCustomerSelected = true;
            setCustName();
        } else {
            salesInvoices.setVisibility(View.GONE);
            isCustomerSelected = false;
            selectedCust.setText(getString(R.string.no_customer));
        }
        myAdapter = new SalesMenuAdapter(getActivity(), isCustomerSelected);

        LinearLayout customersBut = view.findViewById(R.id.salesCustomerBut);

        customersBut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(thisContext, ViewCustomers_FA.class);
                startActivityForResult(intent, FROM_CUSTOMER_SELECTION_ACTIVITY);
            }
        });

        ImageButton clear = view.findViewById(R.id.clearCustomerBut);
        clear.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                salesInvoices.setVisibility(View.GONE);
                myPref.resetCustInfo(getString(R.string.no_customer));

                isCustomerSelected = false;
                selectedCust.setText(getString(R.string.no_customer));
                myAdapter = new SalesMenuAdapter(getActivity(), false);
                myListview.setAdapter(myAdapter);
                myListview.setOnItemClickListener(new MyListener());

            }
        });

        salesInvoices.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (myPref.isCustSelected()) {
                    Intent intent = new Intent(getActivity(), HistoryOpenInvoices_FA.class);
                    intent.putExtra("isFromMainMenu", true);
                    startActivity(intent);
                }
            }
        });

        return view;

    }
    private void paxScan() {
        // open scanner
        posLinkScanner = POSLinkScanner.getPOSLinkScanner(getContext(), POSLinkScanner.ScannerType.REAR);
        ProcessResult result = posLinkScanner.open();
        if (!result.getCode().equals(ProcessResult.CODE_OK)) {
            paxScanError(result.getMessage());
        }

        // start scanner
        if (posLinkScanner == null) {
            paxScanError("Error Starting Scanner.");
            return;
        }
        posLinkScanner.start(new POSLinkScanner.ScannerListener() {
            @Override
            public void onRead(String s) {
                hiddenField.setText(String.format("%s\n", s));
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

    private void checkPermissions() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            requestPermissions(Global.permissions, 42);
                        }
                    }
                }
            }
        }).start();
    }

    public void setCustName() {
        if (myPref.isCustSelected()) {
            CustomersHandler handler = new CustomersHandler(getActivity());
            Customer customer = handler.getCustomer(myPref.getCustID());
            if (customer != null) {
                if (!TextUtils.isEmpty(customer.getCust_firstName())) {
                    selectedCust.setText(String.format("%s %s", StringUtil.nullStringToEmpty(customer.getCust_firstName())
                            , StringUtil.nullStringToEmpty(customer.getCust_lastName())));
                } else if (!TextUtils.isEmpty(customer.getCompanyName())) {
                    selectedCust.setText(customer.getCompanyName());
                } else {
                    selectedCust.setText(customer.getCust_name());
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getActivity().unregisterReceiver(messageReceiver);
        } catch (Exception e) {

        }
    }

    @Override
    public void onResume() {
        Global global = (Global) getActivity().getApplication();
        global.resetOrderDetailsValues();
        global.clearListViewData();
        getActivity().registerReceiver(messageReceiver, new IntentFilter(MainMenu_FA.NOTIFICATION_LOGIN_STATECHANGE));

        if (isReaderConnected && Global.loggedIn) {
            digitalPersona.loadForScan();
        }
        if (myPref.isCustSelected()) {
            isCustomerSelected = true;
            setCustName();
            myAdapter = new SalesMenuAdapter(getActivity(), true);
            myListview.setAdapter(myAdapter);
            myListview.setOnItemClickListener(new MyListener());
            myListview.invalidateViews();
        } else {
            salesInvoices.setVisibility(View.GONE);
            isCustomerSelected = false;
            selectedCust.setText(getString(R.string.no_customer));
            myAdapter = new SalesMenuAdapter(getActivity(), false);
            myListview.setAdapter(myAdapter);
            myListview.setOnItemClickListener(new MyListener());
        }

        if (Global.deviceHasBarcodeScanner(myPref.getPrinterType()) ||
                Global.deviceHasBarcodeScanner(myPref.getSwiperType())
                || Global.deviceHasBarcodeScanner(myPref.sledType(true, -2))) {
            if (Global.btSwiper != null && Global.btSwiper.getCurrentDevice() != null)
                Global.btSwiper.getCurrentDevice().loadScanner(emsCallBack);
            if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null)
                Global.mainPrinterManager.getCurrentDevice().loadScanner(emsCallBack);
            if (Global.btSled != null && Global.btSled.getCurrentDevice() != null)
                Global.btSled.getCurrentDevice().loadScanner(emsCallBack);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        digitalPersona.releaseReader();
        if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null) {
            Global.mainPrinterManager.getCurrentDevice().releaseCardReader();
            Global.mainPrinterManager.getCurrentDevice().turnOffBCR();
            Global.mainPrinterManager.getCurrentDevice().loadScanner(null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == 1) {

            salesInvoices.setVisibility(View.VISIBLE);
            Bundle extras = data.getExtras();
            setCustName();

            myPref.setCustName(extras.getString("customer_name"));
            myPref.setCustSelected(true);

            isCustomerSelected = true;
            myAdapter = new SalesMenuAdapter(getActivity(), true);
            myListview.setAdapter(myAdapter);

            myListview.setOnItemClickListener(new MyListener());

        } else if (resultCode == 2) {
            salesInvoices.setVisibility(View.GONE);
            isCustomerSelected = false;
            selectedCust.setText(getString(R.string.no_customer));
            myAdapter = new SalesMenuAdapter(getActivity(), false);
            myListview.setAdapter(myAdapter);
            myListview.setOnItemClickListener(new MyListener());

        } else if (resultCode == SplittedOrderSummary_FA.NavigationResult.TABLE_SELECTION.getCode()) {
            Bundle extras = data.getExtras();
            String tableId = extras.getString("tableId");
            selectedDinningTable = DinningTableDAO.getById(tableId);
            if (myPref.getPreferences(MyPreferences.pref_ask_seats)) {
                selectSeatAmount();
            } else {
                startSaleRceipt(Global.RestaurantSaleType.EAT_IN, selectedDinningTable.getSeats(), selectedDinningTable.getNumber());
            }
        }
    }

    private void performListViewClick(final int pos) {
        Intent intent;
        if (isCustomerSelected) {
            switch (Global.TransactionType.getByCode(pos)) {
                case TIP_ADJUSTMENT: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.TIP_ADJUSTMENT);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), TipAdjustmentFA.class);
                        startActivity(intent);
                    } else {
                        promptWithCustomer();
                    }
                    break;
                }
                case SALE_RECEIPT: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            if (myPref.isCustomerRequired()) {
                                if (myPref.isRestaurantMode() &&
                                        myPref.getPreferences(MyPreferences.pref_enable_togo_eatin)) {
                                    askEatInToGo();
                                } else {
                                    intent = new Intent(getActivity(), OrderingMain_FA.class);
                                    intent.putExtra("RestaurantSaleType", Global.RestaurantSaleType.TO_GO);
                                    intent.putExtra("option_number", Global.TransactionType.SALE_RECEIPT);
                                    startActivityForResult(intent, 0);
                                }

                            } else {
                                promptWithCustomer();
                            }
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }

                    break;
                }
                case ORDERS: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), OrderingMain_FA.class);
                        intent.putExtra("option_number", Global.TransactionType.ORDERS);
                        startActivityForResult(intent, 0);
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case RETURN: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), OrderingMain_FA.class);
                        intent.putExtra("option_number", Global.TransactionType.RETURN);
                        startActivityForResult(intent, 0);
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case INVOICE: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), OrderingMain_FA.class);
                        intent.putExtra("option_number", Global.TransactionType.INVOICE);
                        startActivityForResult(intent, 0);
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case ESTIMATE: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), OrderingMain_FA.class);
                        intent.putExtra("option_number", Global.TransactionType.ESTIMATE);
                        startActivityForResult(intent, 0);
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case PAYMENT: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TAKE_PAYMENT);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), SelectPayMethod_FA.class);
                        intent.putExtra("salespayment", true);
                        intent.putExtra("amount", "0.00");
                        intent.putExtra("paid", "0.00");
                        intent.putExtra("isFromMainMenu", true);

                        if (isCustomerSelected) {
                            intent.putExtra("cust_id", myPref.getCustID());
                            intent.putExtra("custidkey", myPref.getCustIDKey());
                        }
                        startActivity(intent);
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case GIFT_CARD: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TAKE_PAYMENT);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), GiftCard_FA.class);
                        startActivity(intent);
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }

                    break;
                }
                case LOYALTY_CARD: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TAKE_PAYMENT);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), LoyaltyCard_FA.class);
                        startActivity(intent);
                    } else {
                        Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    }

                    break;
                }
                case REWARD_CARD: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TAKE_PAYMENT);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), RewardCard_FA.class);
                        startActivity(intent);
                    } else {
                        Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    }

                    break;
                }
                case REFUND: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), SelectPayMethod_FA.class);
                        intent.putExtra("salesrefund", true);
                        intent.putExtra("amount", "0.00");
                        intent.putExtra("paid", "0.00");
                        intent.putExtra("isFromMainMenu", true);
                        if (myPref.isCustSelected()) {
                            intent.putExtra("cust_id", myPref.getCustID());
                            intent.putExtra("custidkey", myPref.getCustIDKey());
                        }
                        startActivity(intent);

                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case ROUTE: {
                    break;
                }
                case ON_HOLD: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), OnHoldActivity.class);
                        getActivity().startActivity(intent);
                    } else {
                        Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    }

                    break;
                }
                case CONSIGNMENT: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        intent = new Intent(getActivity(), ConsignmentMain_FA.class);
                        startActivity(intent);
                    } else {
                        Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    }

                    break;
                }
                case LOCATION:
                    pickLocations(true);
                    break;
                case SHIFTS: {
                    boolean hasPermissions = myPref.isUseClerks() && SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.SHIFT_CLERK);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            intent = new Intent(getActivity(), ShiftsActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;

                }
                case SHIFT_EXPENSES: {
                    boolean hasPermissions = myPref.isUseClerks() && SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.NO_SALE);
                    if (hasPermissions) {
                        if (!myPref.isUseClerks() || ShiftDAO.isShiftOpen()) {
                            if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                                intent = new Intent(getActivity(), ShiftExpensesList_FA.class);
                                startActivity(intent);
                            }
                        } else {
                            Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;

                }
            }

        } else {
            switch (Global.TransactionType.getByCode(pos)) {
                case SALE_RECEIPT: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            if (myPref.isCustomerRequired()) {
                                Global.showPrompt(getActivity(), R.string.dlog_title_error, getString(R.string.dlog_msg_select_customer));
                            } else {
                                if (myPref.isRestaurantMode() &&
                                        myPref.getPreferences(MyPreferences.pref_enable_togo_eatin)) {
                                    askEatInToGo();
                                } else {
                                    intent = new Intent(getActivity(), OrderingMain_FA.class);
                                    intent.putExtra("option_number", Global.TransactionType.SALE_RECEIPT);
                                    startActivityForResult(intent, 0);
                                }
                            }
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case RETURN: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            if (myPref.isCustomerRequired()) {
                                Global.showPrompt(getActivity(), R.string.dlog_title_error, getString(R.string.dlog_msg_select_customer));
                            } else {
                                intent = new Intent(getActivity(), OrderingMain_FA.class);
                                intent.putExtra("option_number", Global.TransactionType.RETURN);
                                startActivityForResult(intent, 0);
                            }
                        }

                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case PAYMENT: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TAKE_PAYMENT);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            if (myPref.isCustomerRequired()) {
                                Global.showPrompt(getActivity(), R.string.dlog_title_error, getString(R.string.dlog_msg_select_customer));
                            } else {
                                intent = new Intent(getActivity(), SelectPayMethod_FA.class);
                                intent.putExtra("salespayment", true);
                                intent.putExtra("amount", "0.00");
                                intent.putExtra("paid", "0.00");
                                intent.putExtra("isFromMainMenu", true);
                                startActivity(intent);
                            }
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case GIFT_CARD: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TAKE_PAYMENT);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            intent = new Intent(getActivity(), GiftCard_FA.class);
                            startActivity(intent);
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    }

                    break;
                }
                case LOYALTY_CARD: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TAKE_PAYMENT);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            intent = new Intent(getActivity(), LoyaltyCard_FA.class);
                            startActivity(intent);
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    }

                    break;
                }
                case REWARD_CARD: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TAKE_PAYMENT);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            intent = new Intent(getActivity(), RewardCard_FA.class);
                            startActivity(intent);
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    }

                    break;
                }
                case REFUND: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {

                            intent = new Intent(getActivity(), SelectPayMethod_FA.class);
                            intent.putExtra("salesrefund", true);
                            intent.putExtra("amount", "0.00");
                            intent.putExtra("paid", "0.00");
                            intent.putExtra("isFromMainMenu", true);
                            if (myPref.isCustSelected()) {
                                intent.putExtra("cust_id", myPref.getCustID());
                                intent.putExtra("custidkey", myPref.getCustIDKey());
                            }
                            startActivity(intent);
                        }

                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case ON_HOLD: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(), SecurityManager.SecurityAction.OPEN_ORDER);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            intent = new Intent(getActivity(), OnHoldActivity.class);
                            getActivity().startActivity(intent);
                        }

                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case LOCATION:
                    pickLocations(true);
                    break;
                case TIP_ADJUSTMENT: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.TIP_ADJUSTMENT);
                    if (hasPermissions) {
                        if (validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            intent = new Intent(getActivity(), TipAdjustmentFA.class);
                            startActivity(intent);
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.shift_open_shift, getString(R.string.dlog_msg_error_shift_needs_to_be_open));
                    }

                    break;
                }
                case SHIFTS: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.SHIFT_CLERK);
                    if (hasPermissions) {
                        if (myPref.isUseClerks() && validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            intent = new Intent(getActivity(), ShiftsActivity.class);
                            startActivity(intent);
                        } else if (!myPref.isUseClerks()) {
                            promptClerkLogin(Global.TransactionType.getByCode(pos));
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
                case SHIFT_EXPENSES: {
                    boolean hasPermissions = SecurityManager.hasPermissions(getActivity(),
                            SecurityManager.SecurityAction.NO_SALE);
                    if (hasPermissions) {
                        if (myPref.isUseClerks() && validateClerkShift(Global.TransactionType.getByCode(pos), getActivity())) {
                            intent = new Intent(getActivity(), ShiftExpensesList_FA.class);
                            startActivity(intent);
                        } else if (!myPref.isUseClerks()) {
                            promptClerkLogin(Global.TransactionType.getByCode(pos));
                        }
                    } else {
                        Global.showPrompt(getActivity(), R.string.security_alert, getString(R.string.permission_denied));
                    }
                    break;
                }
            }
        }
    }

    private void promptClerkLogin(final Global.TransactionType transactionType) {
        final Dialog dialog = new Dialog(getActivity(), R.style.Theme_TransparentTest);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dlog_field_single_layout);
        final EditText viewField = dialog.findViewById(R.id.dlogFieldSingle);
        viewField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        TextView viewTitle = dialog.findViewById(R.id.dlogTitle);
        final TextView viewMsg = dialog.findViewById(R.id.dlogMessage);
        Button systemLoginButton = dialog.findViewById(R.id.systemLoginbutton2);
        TextView infoSystemLogin = dialog.findViewById(R.id.infotextView23);
        systemLoginButton.setVisibility(View.GONE);
        infoSystemLogin.setVisibility(View.GONE);
        viewTitle.setText(R.string.dlog_title_enter_clerk_manager_password);
        Button btnOk = dialog.findViewById(R.id.btnDlogSingle);
        Button btnCancel = dialog.findViewById(R.id.btnCancelDlogSingle);
        btnOk.setText(R.string.button_ok);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredPass = viewField.getText().toString().trim();
                Clerk clerk = ClerkDAO.login(enteredPass, myPref, false);
                if (clerk == null && !myPref.loginManager(enteredPass)) {
                    viewMsg.setText(R.string.invalid_password);
                } else {
                    dialog.dismiss();
                    if (clerk != null) {
                        myPref.setClerkID(String.valueOf(clerk.getEmpId()));
                        myPref.setClerkName(clerk.getEmpName());
                    }
                    if (validateClerkShift(transactionType, getActivity())) {
                        switch (transactionType) {
                            case SHIFTS: {
                                Intent intent = new Intent(getActivity(), ShiftsActivity.class);
                                startActivity(intent);
                                break;
                            }
                            case SHIFT_EXPENSES: {
                                Intent intent = new Intent(getActivity(), ShiftExpensesList_FA.class);
                                startActivity(intent);
                                break;
                            }
                        }
                    }
                }
            }
        });
        dialog.show();
    }

    private void askEatInToGo() {
        final Dialog popDlog = new Dialog(getActivity(), R.style.TransparentDialog);
        popDlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        popDlog.setCancelable(true);
        popDlog.setCanceledOnTouchOutside(true);
        popDlog.setContentView(R.layout.dlog_ask_togo_eatin_layout);
        Button toGoBtn = popDlog.findViewById(R.id.toGobutton);
        Button eatInBtn = popDlog.findViewById(R.id.eatInbutton);
        toGoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popDlog.dismiss();
                Intent intent = new Intent(getActivity(), OrderingMain_FA.class);
                intent.putExtra("option_number", Global.TransactionType.SALE_RECEIPT);
                intent.putExtra("RestaurantSaleType", Global.RestaurantSaleType.TO_GO);
                startActivityForResult(intent, 0);
            }
        });
        eatInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popDlog.dismiss();
                if (myPref.getPreferences(MyPreferences.pref_enable_table_selection)) {
                    selectDinnerTable();
                } else if (myPref.getPreferences(MyPreferences.pref_ask_seats)) {
                    selectedDinningTable = DinningTable.getDefaultDinningTable();
                    selectSeatAmount();
                } else {
                    selectedDinningTable = DinningTable.getDefaultDinningTable();
                    startSaleRceipt(Global.RestaurantSaleType.EAT_IN, selectedDinningTable.getSeats(), selectedDinningTable.getNumber());
                }
            }
        });
        popDlog.show();
    }

    private void selectSeatAmount() {
        final int[] seats = this.getResources().getIntArray(R.array.dinningTableSeatsArray);
        final Dialog popDlog = new Dialog(getActivity(), R.style.TransparentDialogFullScreen);
        popDlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        popDlog.setCancelable(true);
        popDlog.setCanceledOnTouchOutside(true);
        popDlog.setContentView(R.layout.dlog_ask_table_seats_amount_layout);
        TextView title = popDlog.findViewById(R.id.dlogTitle);
        title.setText(R.string.select_number_guests);
        GridView gridView = popDlog.findViewById(R.id.tablesGridLayout);
        final DinningTableSeatsAdapter adapter = new DinningTableSeatsAdapter(getActivity(), seats);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedSeatsAmount = seats[position];
                popDlog.dismiss();
                startSaleRceipt(Global.RestaurantSaleType.EAT_IN, selectedSeatsAmount, selectedDinningTable.getNumber());
            }
        });
        popDlog.show();
    }

    public void selectDinnerTable() {
        Intent intent = new Intent(getActivity(), DinningTablesActivity.class);
        int empId;
        if (myPref.isUseClerks()) {
            Shift openShift = ShiftDAO.getOpenShift();
            if (openShift != null) {
                empId = openShift.getClerkId();
            } else {
                empId = Integer.parseInt(myPref.getClerkID());
            }
        } else {
            empId = AssignEmployeeDAO.getAssignEmployee().getEmpId();
        }
        intent.putExtra("associateId", empId);
        startActivityForResult(intent, 0);
    }

    private void pickLocations(final boolean showOrigin) {
        LocationsPickerDlog_FR picker = new LocationsPickerDlog_FR();
        Bundle args = new Bundle();
        args.putBoolean("showOrigin", showOrigin);
        picker.setArguments(args);
        final DialogFragment newFrag = picker;
        picker.setListener(new LocationsPicker_Listener() {
            @Override
            public void onSelectLocation(Locations_Holder location) {
                newFrag.dismiss();
                if (showOrigin) {
                    Global.locationFrom = location;
                    pickLocations(false);
                } else {
                    Global.locationTo = location;
                    confirmSelectedLocations();
                }
            }
        });
        newFrag.show(this.getFragmentManager(), "dialog");
    }

    /* if update information is needed on layout */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();

        if (activity != null) {
            myAdapter = new SalesMenuAdapter(activity, isCustomerSelected);

            if (myListview != null) {
                myListview.setAdapter(myAdapter);
                myListview.setOnItemClickListener(new MyListener());
            }

            Global.showCDTDefault(activity);
        }
    }

    private void confirmSelectedLocations() {
        final Dialog globalDlog = new Dialog(getActivity(), R.style.Theme_TransparentTest);
        globalDlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        globalDlog.setCancelable(true);
        globalDlog.setCanceledOnTouchOutside(false);
        globalDlog.setContentView(R.layout.dlog_btn_left_right_layout);

        TextView viewTitle = globalDlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = globalDlog.findViewById(R.id.dlogMessage);
        viewTitle.setText(R.string.dlog_title_confirm);

        String sb = "Locations selected for transfer:\n" +
                "From: " + Global.locationFrom.getLoc_name() + "\n" +
                "To: " + Global.locationTo.getLoc_name();
        viewMsg.setText(sb);
        globalDlog.findViewById(R.id.btnDlogCancel).setVisibility(View.GONE);

        Button btnOk = globalDlog.findViewById(R.id.btnDlogLeft);
        btnOk.setText(R.string.button_ok);
        Button btnCancel = globalDlog.findViewById(R.id.btnDlogRight);
        btnCancel.setText(R.string.button_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                globalDlog.dismiss();
            }
        });
        btnOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                globalDlog.dismiss();
                Global.isInventoryTransfer = true;
                Intent intent = new Intent(getActivity(), OrderingMain_FA.class);
                intent.putExtra("option_number", Global.TransactionType.LOCATION);
                startActivityForResult(intent, 0);
            }
        });
        globalDlog.show();
    }

    private void startSaleRceipt(Global.RestaurantSaleType restaurantSaleType, int selectedSeatsAmount, String tableNumber) {
        Intent intent = new Intent(getActivity(), OrderingMain_FA.class);
        intent.putExtra("option_number", Global.TransactionType.SALE_RECEIPT);
        intent.putExtra("RestaurantSaleType", restaurantSaleType);

        if (restaurantSaleType == Global.RestaurantSaleType.EAT_IN) {
            Shift openShift = ShiftDAO.getOpenShift();
            int empId;
            if (openShift != null) {
                empId = openShift.getAssigneeId();
            } else {
                empId = Integer.parseInt(myPref.getClerkID());
            }
            intent.putExtra("associateId", empId);
            intent.putExtra("selectedSeatsAmount", selectedSeatsAmount);
            intent.putExtra("selectedDinningTableNumber", tableNumber);
        }
        startActivityForResult(intent, 0);
    }

    private void promptWithCustomer() {
        if (!myPref.getPreferences(MyPreferences.pref_direct_customer_selection)) {
            final Dialog dialog = new Dialog(getActivity(), R.style.Theme_TransparentTest);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setContentView(R.layout.pre_dialog_layout);
            Button withCust = dialog.findViewById(R.id.withCustBut);
            Button withoutCust = dialog.findViewById(R.id.withOutCustBut);
            Button cancel = dialog.findViewById(R.id.cancelPredialogBut);

            withCust.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    startOrderingMain();
                    dialog.dismiss();
                }
            });
            withoutCust.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    salesInvoices.setVisibility(View.GONE);
                    myPref.resetCustInfo(getString(R.string.no_customer));
                    isCustomerSelected = false;
                    startOrderingMain();
                    dialog.dismiss();
                }
            });

            cancel.setOnClickListener(new Button.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        } else {
            startOrderingMain();
        }
    }

    private void startOrderingMain() {
        if (myPref.isRestaurantMode() &&
                myPref.getPreferences(MyPreferences.pref_enable_togo_eatin)) {
            askEatInToGo();
        } else {
            Intent intent = new Intent(getActivity(), OrderingMain_FA.class);
            intent.putExtra("option_number", Global.TransactionType.SALE_RECEIPT);
            intent.putExtra("RestaurantSaleType", Global.RestaurantSaleType.TO_GO);
            startActivityForResult(intent, 0);
        }
    }

    private boolean isTablet() {
        EMSDeviceDriver usbDeviceDriver = DeviceUtils.getUSBDeviceDriver(getActivity());
        String model = Build.MODEL;
        if (usbDeviceDriver != null && usbDeviceDriver instanceof EMSPowaPOS) {
            myPref.setIsPOWA(true);
            return true;
        } else if (model.equals("ET1")) {
            myPref.isET1(false, true);
            return true;
        } else if (model.equals("MC40N0")) {
            myPref.isMC40(false, true);
            return false;
        } else if (usbDeviceDriver != null && usbDeviceDriver instanceof EMSmePOS) {
            myPref.setIsMEPOS(true);
            return true;
        } else if (model.equals("M2MX60P") || model.equals("M2MX6OP")) {
            myPref.setSams4s(true);
            return true;
        } else if (model.equals("JE971")) {
            return true;
        } else if (model.equals("Dolphin Black 70e")) {
            myPref.isDolphin(false, true);
            return false;
        } else if (model.equals("PAT-215")) {
            myPref.setIsPAT215(true);
            return true;
        } else if (model.equals("EM100")) {
            myPref.setIsEM100(true);
            return true;
        } else if (model.equals("EM70")) {
            myPref.setIsEM70(true);
            return true;
        } else if (model.equals("OT-310")) {
            myPref.setIsOT310(true);
            return true;
        } else if (model.toUpperCase().contains("PAYPOINT") || model.toUpperCase().contains("ELO")) {
            myPref.setIsESY13P1(true);
            return true;
        }else if (model.toUpperCase().contains("WPOS-TAB")) {
            myPref.setIsAPT120(true);
            return true;
        } else {
            return (getActivity().getResources().getConfiguration().screenLayout
                    & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        }
    }

    private TextWatcher textWatcher() {

        return new TextWatcher() {
            private boolean doneScanning = false;
            private String val = "";
            private CustomersHandler custHandler = new CustomersHandler(getActivity());
            private HashMap<String, String> map = new HashMap<String, String>();

            @Override
            public void afterTextChanged(Editable s) {
                if (doneScanning) {
                    doneScanning = false;
                    hiddenField.setText("");
                    map = custHandler.getCustomerInfo(val.replace("\n", "").trim());
                    if (MyPreferences.isPaxA920()) {
                        scannerWasRead(val.replace("\n", "").trim());
                    }else{
                        if (map.size() > 0) {
                            SalesTaxCodesHandler taxHandler = new SalesTaxCodesHandler(getActivity());
                            SalesTaxCodesHandler.TaxableCode taxable = taxHandler.checkIfCustTaxable(map.get("cust_taxable"));
                            myPref.setCustTaxCode(taxable, map.get("cust_taxable"));
                            myPref.setCustID(map.get("cust_id"));    //getting cust_id as _id
                            myPref.setCustName(map.get("cust_name"));
                            myPref.setCustIDKey(map.get("custidkey"));
                            myPref.setCustSelected(true);

                            myPref.setCustPriceLevel(map.get("pricelevel_id"));

                            myPref.setCustEmail(map.get("cust_email"));

                            setCustName();

                            salesInvoices.setVisibility(View.VISIBLE);
                            isCustomerSelected = true;
                            myAdapter = new SalesMenuAdapter(getActivity(), true);
                            myListview.setAdapter(myAdapter);

                            myListview.setOnItemClickListener(new MyListener());

                        } else {
                            isCustomerSelected = false;
                            myPref.resetCustInfo(getString(R.string.no_customer));
                            myPref.setCustSelected(false);

                            selectedCust.setText(getString(R.string.no_customer));
                            myAdapter = new SalesMenuAdapter(getActivity(), false);
                            myListview.setAdapter(myAdapter);
                            myListview.setOnItemClickListener(new MyListener());
                            salesInvoices.setVisibility(View.GONE);
                        }
                    }








                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                UIUtils.startBCR(getView(), hiddenField, SalesTab_FR.this);
                if (s.toString().contains("\n")) {
                    val = s.toString();
                    doneScanning = true;
                }
            }
        };
    }

    @Override
    public void executeBCR() {
        CustomersHandler custHandler = new CustomersHandler(getActivity());
        HashMap<String, String> map = custHandler.getCustomerInfo(hiddenField.getText().toString().replace("\n", "").trim());
        hiddenField.setText("");
        if (map.size() > 0) {
            SalesTaxCodesHandler taxHandler = new SalesTaxCodesHandler(getActivity());
            SalesTaxCodesHandler.TaxableCode taxable = taxHandler.checkIfCustTaxable(map.get("cust_taxable"));
            myPref.setCustTaxCode(taxable, map.get("cust_taxable"));
            myPref.setCustID(map.get("cust_id"));    //getting cust_id as _id
            myPref.setCustName(map.get("cust_name"));
            myPref.setCustIDKey(map.get("custidkey"));
            myPref.setCustSelected(true);
            myPref.setCustPriceLevel(map.get("pricelevel_id"));
            myPref.setCustEmail(map.get("cust_email"));
            setCustName();
            salesInvoices.setVisibility(View.VISIBLE);
            isCustomerSelected = true;
            myAdapter = new SalesMenuAdapter(getActivity(), true);
            myListview.setAdapter(myAdapter);
            myListview.setOnItemClickListener(new MyListener());
        }
    }

    @Override
    public void cardWasReadSuccessfully(boolean read, CreditCardInfo cardManager) {

    }

    @Override
    public void readerConnectedSuccessfully(boolean value) {

    }

    private void startOrderWithCustomer(String customerId, String data) {
        CustomersHandler customersHandler = new CustomersHandler(getActivity());
        Customer customer = customersHandler.getCustomer(customerId);
        SalesTaxCodesHandler taxHandler = new SalesTaxCodesHandler(getActivity());
        SalesTaxCodesHandler.TaxableCode taxable = taxHandler.checkIfCustTaxable(customer.getCust_taxable());
        myPref.setCustTaxCode(taxable, customer.getCust_taxable());
        myPref.setCustID(customer.getCust_id());
        myPref.setCustName(customer.getCust_name());
        myPref.setCustIDKey(customer.getCustIdKey());
        myPref.setCustSelected(true);
        myPref.setCustPriceLevel(customer.getPricelevel_id());
        myPref.setCustEmail(customer.getCust_email());
        setCustName();
        salesInvoices.setVisibility(View.VISIBLE);
        isCustomerSelected = true;
        myAdapter = new SalesMenuAdapter(getActivity(), true);
        myListview.setAdapter(myAdapter);
        myListview.setOnItemClickListener(new MyListener());
        Intent intent = new Intent(getActivity(), OrderingMain_FA.class);
        if (!TextUtils.isEmpty(data)) {
            intent.putExtra("BCRMacro", data);
        }
        intent.putExtra("option_number", Global.TransactionType.SALE_RECEIPT);
        startActivityForResult(intent, 0);
    }

    @Override
    public void scannerWasRead(String data) {
        if (!data.isEmpty()) {
            if (myPref.isRemoveLeadingZerosFromUPC()) {
                data = NumberUtils.removeLeadingZeros(data);
            }
            if (data.contains("START_ORDER")) {
                Gson gson = JsonUtils.getInstance();
                BCRMacro bcrMacro = gson.fromJson(data, BCRMacro.class);
                if (bcrMacro != null) {
                    startOrderWithCustomer(bcrMacro.getBcrMacroParams().getCustId(), data);
                }
            }
        }
    }

    @Override
    public void startSignature() {

    }

    @Override
    public void nfcWasRead(String nfcUID) {

    }

    @Override
    public void biometricsWasRead(final EmobileBiometric emobileBiometric) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startOrderWithCustomer(emobileBiometric.getEntityid(), null);

            }
        });
    }

    @Override
    public void biometricsReadNotFound() {

    }

    @Override
    public void biometricsWasEnrolled(BiometricFid biometricFid) {

    }

    @Override
    public void biometricsDuplicatedEnroll(EmobileBiometric emobileBiometric, BiometricFid biometricFid) {

    }


    @Override
    public void biometricsUnregister(ViewCustomerDetails_FA.Finger finger) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        switch (SelectAccount_FA.PermissionType.getByCode(requestCode)) {
//            case ACCESS_COARSE_LOCATION:
//            case ACCESS_FINE_LOCATION:
//                checkWritePermissions();
//                break;
//            case CAMERA:
//                checkPhoneStatePermissions();
//                break;
//            case WRITE_EXTERNAL_STORAGE:
//                checkCameraPermissions();
//                break;
//            case READ_PHONE_STATE:
//                checkMicrophonePermissions();
//                break;
//        }
    }

    public class MyListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
            final int adapterPos = (Integer) myAdapter.getItem(position);
            performListViewClick(adapterPos);
        }
    }
}
