package drivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.widget.Toast;

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
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.handpoint.api.Device;
import com.starmicronics.stario.StarIOPortException;
import com.uniquesecure.meposconnect.MePOS;
import com.uniquesecure.meposconnect.MePOSConnectionManager;
import com.uniquesecure.meposconnect.MePOSConnectionType;
import com.uniquesecure.meposconnect.MePOSException;

import java.util.HashMap;
import java.util.List;

import interfaces.EMSCallBack;
import interfaces.EMSDeviceManagerPrinterDelegate;
import main.EMSDeviceManager;

/**
 * Created by guarionex on 10/6/16.
 */

public class EMSmePOS extends EMSDeviceDriver implements EMSDeviceManagerPrinterDelegate {

    private static final int LINE_WIDTH = 42;
    protected static Device device;
    private static boolean connected = false;
    String msg = "Failed to connect";
    private EMSDeviceManager edm;

    @Override
    public void connect(final Context activity, int paperSize, boolean isPOSPrinter, EMSDeviceManager edm) {
        this.activity = activity;
        this.edm = edm;
        myPref = new MyPreferences(activity);
        init();
        if (mePOS != null && mePOS.getConnectionManager().getConnectionStatus() == MePOSConnectionManager.STATUS_CONNECTED_USB) {
            edm.driverDidConnectToDevice(this, true, activity);
        } else {
            edm.driverDidNotConnectToDevice(this, msg, true, activity);
        }
    }


    @Override
    public boolean autoConnect(final Activity activity, EMSDeviceManager edm, int paperSize, boolean isPOSPrinter, String portName, String portNumber) {
        this.activity = activity;
        this.edm = edm;
        myPref = new MyPreferences(activity);
        init();
        if (mePOS != null && mePOS.getConnectionManager().getConnectionStatus() == MePOSConnectionManager.STATUS_CONNECTED_USB) {
            edm.driverDidConnectToDevice(this, false, activity);
        } else {
            edm.driverDidNotConnectToDevice(this, msg, false, activity);
        }
        return true;
    }

    private void init() {
        try {
            mePOS = new MePOS(activity, MePOSConnectionType.USB);
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
            intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
            final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().contains("ATTACHED")) {
                        mePOS = new MePOS(context, MePOSConnectionType.USB);
                        Toast.makeText(activity, "MePOS connected", Toast.LENGTH_SHORT).show();
                        connected = true;
                    } else if (intent.getAction().contains("DETACHED")) {
                        mePOS.getConnectionManager().disconnect();
                        Toast.makeText(activity, "MePOS disconnected", Toast.LENGTH_SHORT).show();
                        connected = false;
                    }
                }
            };
            activity.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
        } catch (Exception e) {
            mePOS = null;
            e.printStackTrace();
        }
    }


    private void verifyPrinterStatus() {
        while (mePOSReceipt != null) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean printTransaction(String ordID, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold, EMVContainer emvContainer) {
        verifyPrinterStatus();
        setPaperWidth(LINE_WIDTH);
        printReceipt(ordID, LINE_WIDTH, fromOnHold, saleTypes, isFromHistory, emvContainer);
        return true;
    }

    @Override
    public boolean printTransaction(Order order, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold, EMVContainer emvContainer) {
        verifyPrinterStatus();
        setPaperWidth(LINE_WIDTH);
        printReceipt(order, LINE_WIDTH, fromOnHold, saleTypes, isFromHistory, emvContainer);
        return true;
    }

    @Override
    public boolean printTransaction(String ordID, Global.OrderType type, boolean isFromHistory, boolean fromOnHold) {
        verifyPrinterStatus();
        setPaperWidth(LINE_WIDTH);
        printTransaction(ordID, type, isFromHistory, fromOnHold, null);
        return true;
    }

    @Override
    public boolean printTransaction(Order order, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold) {
        verifyPrinterStatus();
        setPaperWidth(LINE_WIDTH);
        printReceipt(order, LINE_WIDTH, fromOnHold, saleTypes, isFromHistory, null);
        return true;
    }


    @Override
    public boolean printPaymentDetails(String payID, int type, boolean isReprint, EMVContainer emvContainer) {
        verifyPrinterStatus();
        setPaperWidth(LINE_WIDTH);
        printPaymentDetailsReceipt(payID, type, isReprint, LINE_WIDTH, emvContainer);
        return true;
    }

    @Override
    public boolean printBalanceInquiry(HashMap<String, String> values) {
        setPaperWidth(LINE_WIDTH);
        return printBalanceInquiry(values, LINE_WIDTH);
    }

    @Override
    public boolean printConsignment(List<ConsignmentTransaction> myConsignment, String encodedSig) {
        setPaperWidth(LINE_WIDTH);
        printConsignmentReceipt(myConsignment, encodedSig, LINE_WIDTH);
        return true;
    }

    @Override
    public boolean printConsignmentPickup(List<ConsignmentTransaction> myConsignment, String encodedSig) {
        setPaperWidth(LINE_WIDTH);
        printConsignmentPickupReceipt(myConsignment, encodedSig, LINE_WIDTH);
        return true;
    }

    @Override
    public boolean printConsignmentHistory(HashMap<String, String> map, Cursor c, boolean isPickup) {
        setPaperWidth(LINE_WIDTH);
        printConsignmentHistoryReceipt(map, c, isPickup, LINE_WIDTH);
        return true;
    }

    @Override
    public boolean printRemoteStation(List<Orders> orders, String ordID) {
//        return printStationPrinterReceipt(orders, ordID, LINE_WIDTH, cutPaper, printHeader);
        return false;
    }

    @Override
    public boolean printOpenInvoices(String invID) {
        setPaperWidth(LINE_WIDTH);
        printOpenInvoicesReceipt(invID, LINE_WIDTH);
        return true;
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
        setPaperWidth(LINE_WIDTH);
        printReportReceipt(curDate, LINE_WIDTH);
        return true;
    }

    @Override
    public void printShiftDetailsReport(String shiftID) {
        setPaperWidth(LINE_WIDTH);
        printShiftDetailsReceipt(LINE_WIDTH, shiftID);
    }

    @Override
    public void printEndOfDayReport(String curDate, String clerk_id, boolean printDetails) {
        setPaperWidth(LINE_WIDTH);
        printEndOfDayReportReceipt(curDate, LINE_WIDTH, printDetails);
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
    public void loadScanner(EMSCallBack _callBack) {

    }

    @Override
    public void releaseCardReader() {

    }

    @Override
    public void openCashDrawer() {

        new Thread(new Runnable() {
            public void run() {
                if (mePOS != null) {
                    try {
                        mePOS.openCashDrawer();
                    } catch (MePOSException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void printHeader() {

    }

    @Override
    public void printFooter() {

    }

    @Override
    public boolean isUSBConnected() {
        return connected;
    }

    @Override
    public void toggleBarcodeReader() {

    }

//    @Override
//    public void printReceiptPreview(View view) {
//
//    }

    @Override
    public void printReceiptPreview(SplittedOrder splitedOrder) {
        try {
            setPaperWidth(LINE_WIDTH);
//            Bitmap bitmap = loadBitmapFromView(view);
            super.printReceiptPreview(splitedOrder, LINE_WIDTH);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        super.printClockInOut(timeClocks, LINE_WIDTH, clerkID);
    }

    @Override
    public void printExpenseReceipt(ShiftExpense expense) {
        printExpenseReceipt(LINE_WIDTH, expense);
    }
    @Override
    public boolean printGiftReceipt(OrderProduct orderProduct,
                                    Order order,
                                    Global.OrderType saleTypes, boolean isFromHistory,
                                    boolean fromOnHold) {
        return false;
    }
}
