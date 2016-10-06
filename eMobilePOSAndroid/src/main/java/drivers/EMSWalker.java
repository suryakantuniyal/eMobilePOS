package drivers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.emobilepos.R;
import com.android.emobilepos.models.EMVContainer;
import com.android.emobilepos.models.Orders;
import com.android.emobilepos.models.Payment;
import com.android.emobilepos.payment.ProcessCreditCard_FA;
import com.android.support.ConsignmentTransaction;
import com.android.support.CreditCardInfo;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.payments.core.CoreRefundResponse;
import com.payments.core.CoreResponse;
import com.payments.core.CoreSale;
import com.payments.core.CoreSaleKeyed;
import com.payments.core.CoreSaleResponse;
import com.payments.core.CoreSettings;
import com.payments.core.CoreSignature;
import com.payments.core.CoreTransactions;
import com.payments.core.admin.AndroidTerminal;
import com.payments.core.common.contracts.CoreAPIListener;
import com.payments.core.common.enums.CoreDeviceError;
import com.payments.core.common.enums.CoreError;
import com.payments.core.common.enums.CoreMessage;
import com.payments.core.common.enums.CoreMode;
import com.payments.core.common.enums.Currency;
import com.payments.core.common.enums.DeviceEnum;
import com.payments.core.common.enums.TerminalType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import interfaces.EMSCallBack;
import interfaces.EMSDeviceManagerPrinterDelegate;
import main.EMSDeviceManager;

public class EMSWalker extends EMSDeviceDriver implements CoreAPIListener, EMSDeviceManagerPrinterDelegate {

    private Activity activity;
    private AndroidTerminal terminal;
    private String TERMINAL_ID = "1007";
    private String SECRET = "secretpass";
    private CreditCardInfo cardManager;
    public static CoreSignature signature;
    public boolean isReadingCard = false;
    private Handler handler;
    private EMSCallBack msrCallBack;

    public boolean failedProcessing = false;
    private ProgressDialog dialog;
    private EMSDeviceManager edm;
    private static ProgressDialog myProgressDialog;
    private boolean isAutoConnect = false;

    @Override
    public void connect(Activity activity, int paperSize, boolean isPOSPrinter, EMSDeviceManager edm) {
        this.activity = activity;
        isAutoConnect = false;
        terminal = new AndroidTerminal(this);
        myPref = new MyPreferences(this.activity);
        this.edm = edm;
//        synchronized (terminal) {
        new connectWalkerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

//            try {
//                terminal.wait(10000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    @Override
    public boolean autoConnect(Activity activity, EMSDeviceManager edm, int paperSize, boolean isPOSPrinter, String portName, String portNumber) {
        Looper.prepare();
        this.activity = activity;
        isAutoConnect = true;
        terminal = new AndroidTerminal(this);
        myPref = new MyPreferences(this.activity);
        this.edm = edm;
        initDevice();
//        synchronized (terminal) {
//        new connectWalkerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//            try {
//                terminal.wait(10000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        return true;
    }

    private void initDevice() {
        terminal.setMode(CoreMode.DEMO);
        terminal.setCurrency(Currency.USD);
        terminal.initWithConfiguration(activity, TERMINAL_ID, SECRET);
    }

    @Override
    public void registerAll() {
        this.registerPrinter();
    }

    @Override
    public void registerPrinter() {
        edm.setCurrentDevice(this);
        Global.btSwiper.setCurrentDevice(this);
    }

    @Override
    public boolean printTransaction(String ordID, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold, EMVContainer emvContainer) {
        return false;
    }

    @Override
    public boolean printTransaction(String ordID, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold) {
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
    public String printStationPrinter(List<Orders> orderProducts, String ordID, boolean cutPaper, boolean printHeader) {
        return null;
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
    public boolean printReport(String curDate) {
        return false;
    }

    @Override
    public void printShiftDetailsReport(String shiftID) {

    }

    @Override
    public void printEndOfDayReport(String date, String clerk_id, boolean printDetails) {

    }


    @Override
    public void unregisterPrinter() {

    }

    @Override
    public void loadCardReader(EMSCallBack callBack, boolean isDebitCard) {
        if (handler == null)
            handler = new Handler();
        msrCallBack = callBack;
        handler.post(doUpdateDidConnect);
    }

    private Runnable doUpdateDidConnect = new Runnable() {
        public void run() {
            try {
                if (msrCallBack != null)
                    msrCallBack.readerConnectedSuccessfully(true);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    @Override
    public void loadScanner(EMSCallBack _callBack) {

    }

    @Override
    public void releaseCardReader() {

    }

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
    public void printReceiptPreview(View view) {

    }

    @Override
    public void salePayment(Payment payment) {
        CoreSale sale = new CoreSale(new BigDecimal(payment.getPay_amount()));
        terminal.processSale(sale);
    }

    @Override
    public void saleReversal(Payment payment, String originalTransactionId) {

    }

    @Override
    public void refund(Payment payment) {

    }

    @Override
    public void refundReversal(Payment payment, String originalTransactionId) {

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

    private class connectWalkerAsync extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            showDialog(R.string.connecting_handpoint);
        }

        @Override
        protected Void doInBackground(Void... params) {
            initDevice();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
        }
    }

    public void startReading(CreditCardInfo cardInfo, ProgressDialog dialog) {
        isReadingCard = true;
        this.dialog = dialog;
        if (terminal.getDevice().equals(DeviceEnum.NODEVICE)) {
            CoreSaleKeyed sale = new CoreSaleKeyed(cardInfo.dueAmount);
            sale.setCardHolderName(cardInfo.getCardOwnerName());
            sale.setCardNumber(cardInfo.getCardNumUnencrypted());
            sale.setCardCvv(cardInfo.getCardLast4());
            sale.setCardType(cardInfo.getCardType());
            sale.setExpiryDate(cardInfo.getCardExpMonth() + cardInfo.getCardExpYear());
            sale.setAutoReady(true);
            terminal.processSale(sale);
        } else {
            CoreSale sale = new CoreSale(cardInfo.dueAmount);
            terminal.processSale(sale);
        }

        while (isReadingCard) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean deviceConnected() {
        return terminal.getDevice().equals(DeviceEnum.NOMAD);
    }

    public void submitSignature() {
//        dialog.setMessage(EMSWalker.this.activity.getString(R.string.processing_credit_card));
        if (signature.checkSignature()) {
            terminal.submitSignature(signature);
            // signature.signatureText();
//            signature.submitSignature();
        }

    }

    @Override
    public void onError(CoreError coreError, String s) {
        System.out.print(s.toString());
        failedProcessing = true;
        isReadingCard = false;
        if (!TextUtils.isEmpty(s)) {
            msrCallBack.cardWasReadSuccessfully(false, new CreditCardInfo());
        }
    }

    @Override
    public void onLoginUrlRetrieved(String arg0) {

    }

    @Override
    public void onMessage(CoreMessage msg) {
        System.out.print(msg.toString());
        if (isReadingCard) {
            if (msg.equals(CoreMessage.DEVICE_NOT_CONNECTED)) {
                failedProcessing = true;
                isReadingCard = false;
            } else if (msg.equals(CoreMessage.CARD_ERROR))
                isReadingCard = false;

        }

    }

    @Override
    public void onRefundResponse(CoreRefundResponse arg0) {

    }

    @Override
    public void onSaleResponse(CoreSaleResponse response) {
        isReadingCard = false;
        try {
            EMSCallBack callBack = (EMSCallBack) activity;
            cardManager = new CreditCardInfo();
            cardManager.setCardOwnerName(response.getCardHolderName());
            cardManager.setCardType(response.getCardType());
            cardManager.authcode = response.getApprovalCode();
            cardManager.transid = response.getUniqueRef();
            cardManager.setWasSwiped(false);
            cardManager.setCardLast4(response.getCardNumber().substring(response.getCardNumber().length() - 4));
            callBack.cardWasReadSuccessfully(true, cardManager);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onSettingsRetrieved(CoreSettings arg0) {
//        if (devicePlugged) {
//        terminal.initDevice(DeviceEnum.NOMAD);
        terminal.initDevice(DeviceEnum.NOMAD);
//            try {
//                EMSCallBack callBack = (EMSCallBack) activity;
//                callBack.readerConnectedSuccessfully(true);
//
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        } else {
//            terminal.initDevice(DeviceEnum.NODEVICE);
//            this.edm.driverDidNotConnectToDevice(this, msg, false);
//
//        }

    }

    @Override
    public void onSignatureRequired(CoreSignature _signature) {
        signature = _signature;
        try {
            EMSCallBack callBack = (EMSCallBack) msrCallBack;
            callBack.startSignature();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onTransactionListResponse(CoreTransactions arg0) {
        System.out.print(arg0.toString());
    }

    @Override
    public void onDeviceConnected(DeviceEnum deviceEnum, HashMap<String, String> arg1) {
        Toast.makeText(this.activity, deviceEnum.name() + " connected", Toast.LENGTH_SHORT).show();

//        try {
//            EMSCallBack callBack = (EMSCallBack) activity;
//            callBack.readerConnectedSuccessfully(true);
//        } catch (Exception ex) {
//
//        }
//        synchronized (terminal) {
//        terminal.notifyAll();
        this.edm.driverDidConnectToDevice(this, !isAutoConnect);
//        }
        if (!isAutoConnect) {
            dismissDialog();
        } else {
            Looper.myLooper().quit();
        }
    }

    @Override
    public void onDeviceDisconnected(DeviceEnum deviceEnum) {
        Toast.makeText(this.activity, deviceEnum.name() + " disconnected", Toast.LENGTH_SHORT).show();
        this.edm.driverDidNotConnectToDevice(this, activity.getString(R.string.fail_to_connect), false);
        dismissDialog();

    }

    @Override
    public void onDeviceError(CoreDeviceError arg0, String arg1) {
        this.edm.driverDidNotConnectToDevice(this, arg1, false);
        dismissDialog();
    }

    @Override
    public void onSelectApplication(ArrayList<String> arg0) {

    }

    @Override
    public void onSelectBTDevice(ArrayList<String> devices) {
        CharSequence items[] = devices.toArray(new CharSequence[devices.size()]);
        if (items.length > 0) {
            terminal.selectBTDevice(0);
        } else {
            this.edm.driverDidNotConnectToDevice(this, activity.getString(R.string.fail_to_connect), false);
        }
    }

    @Override
    public void onDeviceConnectionError() {
        this.edm.driverDidNotConnectToDevice(this, activity.getString(R.string.fail_to_connect), false);
        dismissDialog();
    }

    @Override
    public void onAutoConfigProgressUpdate(String s) {

    }

    @Override
    public void onReversalRetrieved(CoreResponse coreResponse) {

    }

    private void showDialog(int messageRsId) {
        if (myProgressDialog != null && myProgressDialog.isShowing()) {
            myProgressDialog.dismiss();
        }
        myProgressDialog = new ProgressDialog(activity);
        myProgressDialog.setMessage(activity.getString(messageRsId));
        myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        myProgressDialog.setCancelable(true);
        myProgressDialog.show();
    }

    private void dismissDialog() {
        if (myProgressDialog != null && myProgressDialog.isShowing()) {
            myProgressDialog.dismiss();
        }
        myProgressDialog = null;
    }
}
