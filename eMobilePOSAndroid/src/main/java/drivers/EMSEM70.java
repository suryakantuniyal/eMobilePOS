package drivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;

import com.android.emobilepos.models.ClockInOut;
import com.android.emobilepos.models.EMVContainer;
import com.android.emobilepos.models.Orders;
import com.android.emobilepos.models.SplittedOrder;
import com.android.emobilepos.models.orders.Order;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.realms.Payment;
import com.android.emobilepos.models.realms.ShiftExpense;
import com.android.support.ConsignmentTransaction;
import com.android.support.CreditCardInfo;
import com.android.support.Encrypt;
import com.android.support.Global;
import com.android.support.MyPreferences;

import java.util.HashMap;
import java.util.List;

import interfaces.EMSCallBack;
import interfaces.EMSDeviceManagerPrinterDelegate;
import main.EMSDeviceManager;

/**
 * Created by Guarionex on 12/3/2015.
 */
public class EMSEM70 extends EMSDeviceDriver implements EMSDeviceManagerPrinterDelegate {

    public static final String SCANNER_DATA_KEY = "com.partner.barcode.data";
    public static final String ACTION_NEW_DATA1 = "android.intent.action.bcr.newdata";
    public static final String ACTION_NEW_DATA = "android.intent.action.scanner.data";
    String scannedData = "";
    private EMSCallBack scannerCallBack;
    final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scannedData = intent.getStringExtra(SCANNER_DATA_KEY);
            scannerCallBack.scannerWasRead(scannedData);
        }
    };
    private Encrypt encrypt;
    private CreditCardInfo cardManager;
    private EMSDeviceManager edm;
    private EMSEM70 thisInstance;
    private Handler handler;
    private Runnable runnableScannedData = new Runnable() {
        public void run() {
            try {
                if (scannerCallBack != null)
                    scannerCallBack.scannerWasRead(scannedData);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    @Override
    public void connect(Context activity, int paperSize, boolean isPOSPrinter, EMSDeviceManager edm) {
        this.activity = activity;
        myPref = new MyPreferences(this.activity);
        cardManager = new CreditCardInfo();
        encrypt = new Encrypt(activity);
        this.edm = edm;
        thisInstance = this;
        this.edm.driverDidConnectToDevice(thisInstance, false, activity);
    }

    @Override
    public boolean autoConnect(Activity activity, EMSDeviceManager edm, int paperSize, boolean isPOSPrinter,
                               String _portName, String _portNumber) {
        this.activity = activity;
        myPref = new MyPreferences(this.activity);
        cardManager = new CreditCardInfo();
        encrypt = new Encrypt(activity);
        this.edm = edm;
        thisInstance = this;
        this.edm.driverDidConnectToDevice(thisInstance, false, activity);
        return true;
    }

    @Override
    public boolean printTransaction(String ordID, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold, EMVContainer emvContainer) {
        return false;
    }

    @Override
    public boolean printTransaction(Order order, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold, EMVContainer emvContainer) {
        return false;
    }

    @Override
    public boolean printTransaction(String ordID, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold) {
        return false;
    }

    @Override
    public boolean printTransaction(Order order, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold) {
        return false;
    }

    @Override
    public boolean printPaymentDetails(String payID, int isFromMainMenu, boolean isReprint, EMVContainer emvContainer) {
        return false;
    }

    @Override
    public boolean printBalanceInquiry(HashMap<String, String> values) {
        return false;
    }

    @Override
    public boolean printConsignment(List<ConsignmentTransaction> myConsignment, String encodedSignature) {
        return false;
    }

    @Override
    public boolean printConsignmentPickup(List<ConsignmentTransaction> myConsignment, String encodedSignature) {
        return false;
    }

    @Override
    public boolean printConsignmentHistory(HashMap<String, String> map, Cursor c, boolean isPickup) {
        return false;
    }

    @Override
    public boolean printRemoteStation(List<Orders> orders, String ordID) {
        return false;
    }

    @Override
    public boolean printOpenInvoices(String invID) {
        return false;
    }

    @Override
    public boolean printOnHold(Object onHold) {
        return false;
    }

    @Override
    public void setBitmap(Bitmap bmp) {

    }

    @Override
    public void playSound() {

    }

    @Override
    public void turnOnBCR() {

    }

    @Override
    public void turnOffBCR() {

    }

    @Override
    public boolean printReport(String curDate) {
        return false;
    }

    @Override
    public void printEndOfDayReport(String date, String clerk_id, boolean printDetails) {

    }

    @Override
    public void printShiftDetailsReport(String shiftID) {
    }

    @Override
    public void registerAll() {
        this.registerPrinter();
    }

    @Override
    public void registerPrinter() {
        edm.setCurrentDevice(this);
    }

    @Override
    public void unregisterPrinter() {
        edm.setCurrentDevice(null);
    }

    @Override
    public void loadCardReader(EMSCallBack callBack, boolean isDebitCard) {

    }

    @Override
    public void loadScanner(EMSCallBack callBack) {

        scannerCallBack = callBack;
        if (handler == null)
            handler = new Handler();
        if (callBack != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.bcr.newdata");
            filter.addAction("android.intent.action.scanner.data");
            activity.registerReceiver(mBroadcastReceiver, filter);
            Intent intent = ((Activity) activity).getIntent();
            String action = intent.getAction();
            if ("android.intent.action.bcr.newdata".equals(action)) {
                String SCANNER_DATA_KEY = "com.partner.barcode.data";
                String data = intent.getStringExtra(SCANNER_DATA_KEY);
            }
        }
    }

    @Override
    public void releaseCardReader() {
        try {
            activity.unregisterReceiver(mBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void printReceiptPreview(View view) {
//
//    }

    @Override
    public void openCashDrawer() {

    }

    @Override
    public void printHeader() {

    }

    @Override
    public void printFooter() {

    }

    @Override
    public boolean isUSBConnected() {
        return false;
    }

    @Override
    public void toggleBarcodeReader() {

    }

    @Override
    public void printReceiptPreview(SplittedOrder splitedOrder) {

    }

    @Override
    public void salePayment(Payment payment, CreditCardInfo creditCardInfo) {

    }

    @Override
    public void saleReversal(Payment payment, String originalTransactionId, CreditCardInfo creditCardInfo) {

    }

    @Override
    public void refund(Payment payment, CreditCardInfo creditCardInfo) {

    }

    @Override
    public void refundReversal(Payment payment, String originalTransactionId, CreditCardInfo creditCardInfo) {

    }

    @Override
    public void printEMVReceipt(String text) {

    }

    @Override
    public void sendEmailLog() {

    }

    @Override
    public void updateFirmware() {

    }

    @Override
    public void submitSignature() {

    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void printClockInOut(List<ClockInOut> timeClocks, String clerkID) {
    }

    @Override
    public void printExpenseReceipt(ShiftExpense expense) {

    }
    @Override
    public boolean printGiftReceipt(OrderProduct orderProduct,
                                    Order order,
                                    Global.OrderType saleTypes, boolean isFromHistory,
                                    boolean fromOnHold) {
        return false;
    }
}
