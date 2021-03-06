package drivers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.emobilepos.R;
import com.android.emobilepos.models.ClockInOut;
import com.android.emobilepos.models.EMSEpayLoginInfo;
import com.android.emobilepos.models.EMVContainer;
import com.android.emobilepos.models.Orders;
import com.android.emobilepos.models.SplittedOrder;
import com.android.emobilepos.models.orders.Order;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.realms.Payment;
import com.android.emobilepos.models.realms.ShiftExpense;
import com.android.emobilepos.settings.SettingListActivity;
import com.android.support.ConsignmentTransaction;
import com.android.support.CreditCardInfo;
import com.android.support.GenerateNewID;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.emsutils.EMSUtils;
import com.payments.core.CoreRefund;
import com.payments.core.CoreRefundResponse;
import com.payments.core.CoreResponse;
import com.payments.core.CoreSale;
import com.payments.core.CoreSaleKeyed;
import com.payments.core.CoreSaleResponse;
import com.payments.core.CoreSecureCardResponse;
import com.payments.core.CoreSettings;
import com.payments.core.CoreSignature;
import com.payments.core.CoreTransactions;
import com.payments.core.CoreUnreferencedRefund;
import com.payments.core.admin.AndroidTerminal;
import com.payments.core.admin.DeviceConnectionType;
import com.payments.core.common.contracts.CoreAPIListener;
import com.payments.core.common.enums.CoreDeviceError;
import com.payments.core.common.enums.CoreError;
import com.payments.core.common.enums.CoreMessage;
import com.payments.core.common.enums.CoreMode;
import com.payments.core.common.enums.Currency;
import com.payments.core.common.enums.DeviceEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import interfaces.EMSCallBack;
import interfaces.EMSDeviceManagerPrinterDelegate;
import main.EMSDeviceManager;

public class EMSNomad extends EMSDeviceDriver implements CoreAPIListener, EMSDeviceManagerPrinterDelegate {

    public static CoreSignature signature;
    private static AndroidTerminal terminal;
    private static String TERMINAL_ID = "2993001";
    private static String SECRET = "password";
    private static EMSCallBack msrCallBack;
    private static EMSDeviceManager edm;
    private static ProgressDialog myProgressDialog;
    private Context activity;
    private boolean isReadingCard = false;
    private Handler handler;
    private boolean isAutoConnect = false;
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
    public void connect(Context activity, int paperSize, boolean isPOSPrinter, EMSDeviceManager edm) {
        this.activity = activity;
        isAutoConnect = false;
        if (terminal == null) {
            terminal = new AndroidTerminal(this);
        }
        myPref = new MyPreferences(this.activity);
        EMSNomad.edm = edm;
//        synchronized (terminal) {
        new ConnectNomadAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

//            try {
//                terminal.wait(10000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    @Override
    public boolean autoConnect(Activity activity, EMSDeviceManager edm, int paperSize, boolean isPOSPrinter, String portName, String portNumber) {
//        if (activity instanceof SettingListActivity) {
//            Looper.prepare();
//        }
        this.activity = activity;
        isAutoConnect = true;
        if (terminal != null && (activity instanceof SettingListActivity
                || !deviceConnected())) {
            terminal.releaseResources();
        } else if (terminal != null && deviceConnected()) {
            if (Global.btSwiper.getCurrentDevice() == null) {
                registerAll();
            }
            return true;
        }
        myPref = new MyPreferences(this.activity);
        EMSNomad.edm = edm;
        terminal = new AndroidTerminal(this);

//        initDevice();
//        synchronized (myPref) {
        new ConnectNomadAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//            try {
//                myPref.wait(30000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        if (activity instanceof SettingListActivity) {
//            Looper.myLooper().quit();
//        }
        EMSNomad.edm.driverDidConnectToDevice(this, false, activity);
        return true;
    }

    private void initDevice() {
        terminal.setMode(CoreMode.LIVE);
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

    @Override
    public void loadScanner(EMSCallBack _callBack) {

    }

    @Override
    public void releaseCardReader() {
        terminal.cancelTransaction();
//        initDevice();
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
    public void printReceiptPreview(SplittedOrder splitedOrder) {

    }

//    @Override
//    public void printReceiptPreview(View view) {
//
//    }


    @Override
    public void salePayment(Payment payment, CreditCardInfo creditCardInfo) {
        if (creditCardInfo == null || TextUtils.isEmpty(creditCardInfo.getCardNumUnencrypted())) {
            CoreSale sale = new CoreSale(new BigDecimal(payment.getPay_amount()));
            if (!TextUtils.isEmpty(payment.getTipAmount())
                    && new BigDecimal(payment.getTipAmount()).compareTo(new BigDecimal(0)) > 0) {
                sale.addTipWithAmount(new BigDecimal(payment.getTipAmount()));
            }
            terminal.processSale(sale);
        } else {
            CoreSaleKeyed sale = new CoreSaleKeyed(new BigDecimal(payment.getPay_amount()));
            if (!TextUtils.isEmpty(payment.getTipAmount())
                    && new BigDecimal(payment.getTipAmount()).compareTo(new BigDecimal(0)) > 0) {
                sale.addTipWithAmount(new BigDecimal(payment.getTipAmount()));
            }
            sale.setCardHolderName(creditCardInfo.getCardOwnerName());
            sale.setCardNumber(creditCardInfo.getCardNumUnencrypted());
            sale.setCardCvv(creditCardInfo.getCardUnEncryptedSecCode());
            sale.setCardType(creditCardInfo.getCardType());
            sale.setExpiryDate(creditCardInfo.getCardExpMonth() + creditCardInfo.getCardExpYear().substring(2));
            terminal.processSale(sale);
        }
    }

    @Override
    public void saleReversal(Payment payment, String originalTransactionId, CreditCardInfo creditCardInfo) {
        refund(payment, creditCardInfo);
    }

    @Override
    public void refund(Payment payment, CreditCardInfo creditCardInfo) {
        if (creditCardInfo == null || TextUtils.isEmpty(creditCardInfo.getCardNumUnencrypted())) {
            GenerateNewID newID = new GenerateNewID(activity);
            payment.setPay_id(newID.getNextID(GenerateNewID.IdType.PAYMENT_ID));
            CoreRefund refund = new CoreRefund(new BigDecimal(payment.getPay_amount()));
            refund.setUniqueRef(payment.getPay_id());
            refund.setReason("payment refund");
            terminal.processRefund(refund);
        } else {
            CoreUnreferencedRefund refund = new CoreUnreferencedRefund(new BigDecimal(payment.getPay_amount()));
            refund.setCardHolderName(creditCardInfo.getCardOwnerName());
            refund.setCardNumber(creditCardInfo.getCardNumUnencrypted());
            refund.setCardCvv(creditCardInfo.getCardUnEncryptedSecCode());
            refund.setCardType(creditCardInfo.getCardType());
            refund.setReason("payment refund");
            refund.setExpiryDate(creditCardInfo.getCardExpMonth() + creditCardInfo.getCardExpYear().substring(2));
            terminal.processUnreferencedRefund(refund);
        }
    }

    @Override
    public void refundReversal(Payment payment, String originalTransactionId, CreditCardInfo creditCardInfo) {
        refund(payment, creditCardInfo);
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

    public void startReading(CreditCardInfo cardInfo, ProgressDialog dialog) {
        isReadingCard = true;
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

    private boolean deviceConnected() {
        return !(terminal == null || terminal.getDevice() == null) && terminal.getDevice().equals(DeviceEnum.BBPOSDEVICE);
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
    public void onError(CoreError coreError, String s) {
        System.out.print(s);
        isReadingCard = false;
        if (msrCallBack != null) {
            msrCallBack.cardWasReadSuccessfully(false, new CreditCardInfo());
        }
        if (!isAutoConnect) {
            dismissDialog();
            edm.driverDidNotConnectToDevice(this, activity.getString(R.string.fail_to_connect), true, activity);
        } else {
            dismissDialog();
//            MainMenu_FA.handler.sendEmptyMessage(0);
//            Looper.myLooper().quit();
        }
    }

    @Override
    public void onLoginUrlRetrieved(String arg0) {

    }

    @Override
    public void onMessage(CoreMessage msg) {
        Log.d("NOMAD onMessage:", msg.toString());
        if (isReadingCard) {
            if (msg.equals(CoreMessage.DEVICE_NOT_CONNECTED)) {
                isReadingCard = false;
            } else if (msg.equals(CoreMessage.CARD_ERROR))
                isReadingCard = false;
        }

    }

    @Override
    public void onRefundResponse(CoreRefundResponse response) {

    }

    @Override
    public void onSaleResponse(CoreSaleResponse response) {
        isReadingCard = false;
        try {
            CreditCardInfo cardManager = new CreditCardInfo();
            cardManager.setCardOwnerName(response.getCardHolderName());
            cardManager.setCardType(response.getCardType());
            cardManager.authcode = response.getApprovalCode();
            cardManager.transid = response.getUniqueRef();
            cardManager.setWasSwiped(true);
            cardManager.setCardLast4(response.getCardNumber().substring(response.getCardNumber().length() - 4));
            msrCallBack.cardWasReadSuccessfully(response.getCode().equalsIgnoreCase("A"), cardManager);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onSettingsRetrieved(CoreSettings arg0) {
//        if (devicePlugged) {
//        terminal.initDevice(DeviceEnum.NOMAD);
        terminal.initDevice(DeviceEnum.BBPOSDEVICE, DeviceConnectionType.BLUETOOTH, null);
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
            EMSCallBack callBack = msrCallBack;
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
        edm.driverDidConnectToDevice(this, !isAutoConnect, activity);
//        }
        if (!isAutoConnect) {
            dismissDialog();
        } else {
            dismissDialog();
//            MainMenu_FA.handler.sendEmptyMessage(0);
//            Looper.myLooper().quit();
        }
    }

    @Override
    public void onDeviceDisconnected(DeviceEnum deviceEnum) {
        Toast.makeText(this.activity, deviceEnum.name() + " disconnected", Toast.LENGTH_SHORT).show();
        edm.driverDidNotConnectToDevice(this, activity.getString(R.string.fail_to_connect), false, activity);
        initDevice();
        if (!isAutoConnect) {
//            dismissDialog();
        } else {
//            synchronized (activity) {
//                activity.notifyAll();
//        }
//            MainMenu_FA.handler.sendEmptyMessage(0);
        }

    }

    @Override
    public void onDeviceError(CoreDeviceError arg0, String arg1) {
        edm.driverDidNotConnectToDevice(this, arg1, !isAutoConnect, activity);
        if (msrCallBack != null) {
            msrCallBack.cardWasReadSuccessfully(false, new CreditCardInfo());
        }
        if (!isAutoConnect) {
            dismissDialog();
        }
//        else {
//            synchronized (activity) {
//                activity.notifyAll();
//            }
//            MainMenu_FA.handler.sendEmptyMessage(0);
//        }
    }

    @Override
    public void onSelectApplication(ArrayList<String> arg0) {

    }

    @Override
    public void onSelectBTDevice(ArrayList<String> devices) {
        int nomadIdx = -1;
        int i = 0;
        for (String device : devices) {
            if (device.startsWith("WP")) {
                nomadIdx = i;
                break;
            }
            i++;
        }
        if (nomadIdx > -1) {
            terminal.selectBTDevice(i);
//            } else {
//                this.edm.driverDidNotConnectToDevice(this, activity.getString(R.string.fail_to_connect), false);
//            }
//        } else {
//            terminal.initDevice(DeviceEnum.NOMAD);
        }
    }

    @Override
    public void onDeviceConnectionError() {
        edm.driverDidNotConnectToDevice(this, activity.getString(R.string.fail_to_connect), false, activity);
        if (!isAutoConnect) {
            dismissDialog();
        }
//        else {
//            synchronized (activity) {
//                activity.notifyAll();
//            }
//            MainMenu_FA.handler.sendEmptyMessage(0);
//        }
    }

    @Override
    public void onAutoConfigProgressUpdate(String s) {

    }

    @Override
    public void onReversalRetrieved(CoreResponse coreResponse) {

    }

    @Override
    public void onSelectSerialPort(ArrayList<String> arrayList) {

    }

    @Override
    public void onDeviceInfoReturned(HashMap<String, String> hashMap) {

    }

    @Override
    public void onSecureCardResponse(CoreSecureCardResponse coreSecureCardResponse) {

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

    private class ConnectNomadAsync extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            showDialog(R.string.connecting_handpoint);
        }

        @Override
        protected Void doInBackground(Void... params) {
            EMSEpayLoginInfo loginInfo = EMSUtils.getEmsEpayLoginInfo(activity);
            SECRET = loginInfo.getSecret();
            TERMINAL_ID = loginInfo.getTerminalId();
            initDevice();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
        }
    }
    @Override
    public boolean printGiftReceipt(OrderProduct orderProduct,
                                    Order order,
                                    Global.OrderType saleTypes, boolean isFromHistory,
                                    boolean fromOnHold) {
        return false;
    }
}
