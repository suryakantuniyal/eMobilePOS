package drivers;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.emobilepos.R;
import com.android.emobilepos.models.ClockInOut;
import com.android.emobilepos.models.EMVContainer;
import com.android.emobilepos.models.Orders;
import com.android.emobilepos.models.SplittedOrder;
import com.android.emobilepos.models.orders.Order;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.realms.Payment;
import com.android.emobilepos.models.realms.ShiftExpense;
import com.android.emobilepos.payment.ProcessCreditCard_FA;
import com.android.soundmanager.SoundManager;
import com.android.support.CardParser;
import com.android.support.ConsignmentTransaction;
import com.android.support.CreditCardInfo;
import com.android.support.Encrypt;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.elo.device.DeviceManager;
import com.elo.device.ProductInfo;
import com.elo.device.enums.EloPlatform;
import com.elo.device.enums.Status;
import com.elo.device.exceptions.UnsupportedEloPlatform;
import com.elo.device.peripherals.BarCodeReader;
import com.elotouch.paypoint.register.barcodereader.BarcodeReader;
import com.elotouch.paypoint.register.printer.SerialPort;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTEMVEvent;
import com.magtek.mobile.android.mtlib.MTSCRA;
import com.magtek.mobile.android.mtlib.MTSCRAEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import drivers.elo.utils.PrinterAPI;
import interfaces.EMSCallBack;
import interfaces.EMSDeviceManagerPrinterDelegate;
import main.EMSDeviceManager;

/**
 * Created by Guarionex on 12/3/2015.
 */
public class EMSELO extends EMSDeviceDriver implements EMSDeviceManagerPrinterDelegate {
    private final int LINE_WIDTH = 32;
    String scannedData = "";
    BarCodeReaderAdapter barcodereader;
    private EMSCallBack scannerCallBack;
    private Encrypt encrypt;
    private EMSDeviceManager edm;
    private EMSELO thisInstance;
    private Handler handler;
    private boolean didConnect;
    private MTSCRA m_scra;
    private Handler m_scraHandler;
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
    private SoundManager soundManager;

    public static boolean isEloPaypoint2() {
        try {
            return DeviceManager.getPlatformInfo() != null && DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_2;
        } catch (Exception e) {
            return false;
        }
    }

    /*
     *
     * Prints/Displays Text on Customer Facing Display.
     *
     * */
    public static void printTextOnCFD(String Line1, String Line2, Context context) {
        DeviceManager deviceManager;
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform != EloPlatform.PAYPOINT_1) {
                deviceManager = DeviceManager.getInstance(EloPlatform.PAYPOINT_2, context);
                if (deviceManager != null) {
                    com.elo.device.peripherals.CFD cfd = deviceManager.getCfd();
                    if (cfd != null) {
                        cfd.setBacklight(true);
                        cfd.clear();
                        cfd.setLine(1, Line1);
                        cfd.setLine(2, Line2);
                    }
                }
            }
        } catch (UnsupportedEloPlatform unsupportedEloPlatform) {

        }

    }

    @Override
    public void connect(Context activity, int paperSize, boolean isPOSPrinter, EMSDeviceManager edm) {
        soundManager = SoundManager.getInstance(activity);
        this.activity = activity;
        myPref = new MyPreferences(this.activity);
        encrypt = new Encrypt(activity);
        this.edm = edm;
        thisInstance = this;
        playSound();
        ProductInfo platformInfo = DeviceManager.getPlatformInfo();
        isPOSPrinter = platformInfo.eloPlatform == EloPlatform.PAYPOINT_1;
        if (platformInfo.eloPlatform == EloPlatform.PAYPOINT_1 || isPOSPrinter) {
            if (Global.mainPrinterManager == null || Global.mainPrinterManager.getCurrentDevice() == null) {
                new processConnectionAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, true);
            }
        } else {
            edm.driverDidConnectToDevice(thisInstance, false, activity);
        }
    }

    @Override
    public boolean autoConnect(Activity activity, EMSDeviceManager edm, int paperSize, boolean isPOSPrinter,
                               String _portName, String _portNumber) {
        soundManager = SoundManager.getInstance(activity);
        this.activity = activity;
        myPref = new MyPreferences(this.activity);
        encrypt = new Encrypt(activity);
        this.edm = edm;
        thisInstance = this;
        playSound();
        ProductInfo platformInfo = DeviceManager.getPlatformInfo();
        isPOSPrinter = platformInfo.eloPlatform == EloPlatform.PAYPOINT_1;
        if (platformInfo.eloPlatform == EloPlatform.PAYPOINT_1 || isPOSPrinter) {
            if (Global.mainPrinterManager == null || Global.mainPrinterManager.getCurrentDevice() == null) {
                new processConnectionAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
            }
        } else {
            edm.driverDidConnectToDevice(thisInstance, false, activity);
        }
        return true;
    }

    @Override
    public boolean printTransaction(String ordID, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold, EMVContainer emvContainer) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                printReceipt(ordID, LINE_WIDTH, fromOnHold, saleTypes, isFromHistory, emvContainer);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                printReceipt(ordID, LINE_WIDTH, fromOnHold, saleTypes, isFromHistory, emvContainer);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean printTransaction(Order order, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold, EMVContainer emvContainer) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                printReceipt(order, LINE_WIDTH, fromOnHold, saleTypes, isFromHistory, emvContainer);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                printReceipt(order, LINE_WIDTH, fromOnHold, saleTypes, isFromHistory, emvContainer);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean printTransaction(String ordID, Global.OrderType type, boolean isFromHistory, boolean fromOnHold) {
        printTransaction(ordID, type, isFromHistory, fromOnHold, null);
        return true;
    }

    @Override
    public boolean printTransaction(Order order, Global.OrderType saleTypes, boolean isFromHistory, boolean fromOnHold) {
        return printTransaction(order, saleTypes, isFromHistory, fromOnHold, null);
    }

    @Override
    public boolean printGiftReceipt(OrderProduct orderProduct,
                                    Order order,
                                    Global.OrderType saleTypes, boolean isFromHistory,
                                    boolean fromOnHold) {
        return false;
    }

    @Override
    public boolean printPaymentDetails(String payID, int isFromMainMenu, boolean isReprint, EMVContainer emvContainer) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printPaymentDetailsReceipt(payID, isFromMainMenu, isReprint, LINE_WIDTH, emvContainer);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printPaymentDetailsReceipt(payID, isFromMainMenu, isReprint, LINE_WIDTH, emvContainer);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean printBalanceInquiry(HashMap<String, String> values) {
        return printBalanceInquiry(values, LINE_WIDTH);
    }

    @Override
    public boolean printConsignment(List<ConsignmentTransaction> myConsignment, String encodedSignature) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printConsignmentReceipt(myConsignment, encodedSignature, LINE_WIDTH);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printConsignmentReceipt(myConsignment, encodedSignature, LINE_WIDTH);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean printConsignmentPickup(List<ConsignmentTransaction> myConsignment, String encodedSignature) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printConsignmentPickupReceipt(myConsignment, encodedSignature, LINE_WIDTH);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printConsignmentPickupReceipt(myConsignment, encodedSignature, LINE_WIDTH);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean printConsignmentHistory(HashMap<String, String> map, Cursor c, boolean isPickup) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printConsignmentHistoryReceipt(map, c, isPickup, LINE_WIDTH);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printConsignmentHistoryReceipt(map, c, isPickup, LINE_WIDTH);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean printRemoteStation(List<Orders> orders, String ordID) {
//        try {
//            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
//                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
//                String receipt = super.printStationPrinterReceipt(orderProducts, ordID, LINE_WIDTH, cutPaper, printHeader);
//                return receipt;
//            } else {
//                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
//                eloPrinterApi = new PrinterAPI(eloPrinterPort);
//                String receipt = super.printStationPrinterReceipt(orderProducts, ordID, LINE_WIDTH, cutPaper, printHeader);
//                eloPrinterPort.getInputStream().close();
//                eloPrinterPort.getOutputStream().close();
//                eloPrinterPort.close();
//                return receipt;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return false;
    }

    @Override
    public boolean printOpenInvoices(String invID) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printOpenInvoicesReceipt(invID, LINE_WIDTH);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printOpenInvoicesReceipt(invID, LINE_WIDTH);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        try {
                soundManager.playSound(1,1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean printReport(String curDate) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printReportReceipt(curDate, LINE_WIDTH);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printReportReceipt(curDate, LINE_WIDTH);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void printEndOfDayReport(String date, String clerk_id, boolean printDetails) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printEndOfDayReportReceipt(date, LINE_WIDTH, printDetails);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printEndOfDayReportReceipt(date, LINE_WIDTH, printDetails);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printShiftDetailsReport(String shiftID) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printShiftDetailsReceipt(LINE_WIDTH, shiftID);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printShiftDetailsReceipt(LINE_WIDTH, shiftID);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerAll() {
        this.registerPrinter();
    }

    public void registerPrinter() {
        edm.setCurrentDevice(this);
    }

    public void unregisterPrinter() {
        edm.setCurrentDevice(null);
        turnOffBCR();
    }

    @Override
    public void loadCardReader(final EMSCallBack callBack, boolean isDebitCard) {
        this.scannerCallBack = callBack;
        if (m_scra == null) {
            m_scraHandler = new Handler(new SCRAHandlerCallback());
            m_scra = new MTSCRA(activity, m_scraHandler);
            m_scra.setConnectionType(MTConnectionType.USB);
            m_scra.setAddress(null);
            m_scra.setConnectionRetry(true);
            m_scra.openDevice();
        }
    }

    @Override
    public void loadScanner(EMSCallBack callBack) {
        if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_1) {
            barcodereader = new Elo1Barcodereader();
        } else {
            barcodereader = new EloRefreshBarcodereader();
        }
        scannerCallBack = callBack;
        if (handler == null)
            handler = new Handler();
        if (callBack != null) {
            turnOnBCR();
        } else {
            turnOffBCR();
        }
    }

    @Override
    public void releaseCardReader() {
    }

    @Override
    public void openCashDrawer() {
        if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
            try {
                DeviceManager deviceManager = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity);
                com.elo.device.peripherals.CashDrawer cashDrawer = deviceManager.getCashDrawer();
                if (!cashDrawer.isOpen()) {
                    cashDrawer.open();
                }
            } catch (UnsupportedEloPlatform unsupportedEloPlatform) {

            }
        }
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
        if (barcodereader != null) {
            barcodereader.setBarCodeReaderEnabled(true);
        }
    }

    @Override
    public void printReceiptPreview(SplittedOrder splitedOrder) {
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printReceiptPreview(splitedOrder, LINE_WIDTH);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                setPaperWidth(LINE_WIDTH);
                super.printReceiptPreview(splitedOrder, LINE_WIDTH);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedEloPlatform unsupportedEloPlatform) {

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
        try {
            if (DeviceManager.getPlatformInfo().eloPlatform == EloPlatform.PAYPOINT_REFRESH) {
                eloPrinterRefresh = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getPrinter();
                super.printClockInOut(timeClocks, LINE_WIDTH, clerkID);
            } else {
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                super.printClockInOut(timeClocks, LINE_WIDTH, clerkID);
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printExpenseReceipt(ShiftExpense expense) {
        printExpenseReceipt(LINE_WIDTH, expense);
    }

    /*
     *
     * Code to Read Barcode through Barcode Reader.
     * BarcodeReader automatically populates the widget that is currently in focus.
     * Here the "bar_code" edittext widget has focus.
     *
     * */
    private void readBarcode() {
        turnOnBCR();
    }

    public void turnOnBCR() {
        if (barcodereader != null && !barcodereader.isBarCodeReaderEnabled()) {
//            barcodereader.turnOnLaser();
            barcodereader.setBarCodeReaderEnabled(true);
        }
    }

    public void turnOffBCR() {
        if (barcodereader != null && barcodereader.isBarCodeReaderEnabled()) {
//            barcodereader.turnOnLaser();
            barcodereader.setBarCodeReaderEnabled(false);
        }
    }

    public interface BarCodeReaderAdapter {
        boolean isBarCodeReaderEnabled();

        void setBarCodeReaderEnabled(boolean enabled);

        boolean isBarCodeReaderKbdMode();

        void setBarCodeReaderKbdMode();
    }

    private class SCRAHandlerCallback implements Handler.Callback {
        private static final String TAG = "Magtek";

        public boolean handleMessage(Message msg) {
            try {
                Log.i(TAG, "*** Callback " + msg.what);
                switch (msg.what) {
                    case MTSCRAEvent.OnDeviceConnectionStateChanged:
//                        OnDeviceStateChanged((MTConnectionState) msg.obj);
                        break;
                    case MTSCRAEvent.OnCardDataStateChanged:
//                        OnCardDataStateChanged((MTCardDataState) msg.obj);
                        break;
                    case MTSCRAEvent.OnDataReceived:
                        if (m_scra.getResponseData() != null) {
                            CreditCardInfo cardInfo = new CreditCardInfo();
                            if (m_scra.getKSN().equals("00000000000000000000")) {
                                CardParser.parseCreditCard(activity, m_scra.getMaskedTracks(), cardInfo);
                            } else {
                                cardInfo.setCardOwnerName(m_scra.getCardName());
                                if (m_scra.getCardExpDate() != null && !m_scra.getCardExpDate().isEmpty()) {
                                    String year = m_scra.getCardExpDate().substring(0, 2);
                                    String month = m_scra.getCardExpDate().substring(2, 4);
                                    cardInfo.setCardExpYear(year);
                                    cardInfo.setCardExpMonth(month);
                                }
                                cardInfo.setCardType(ProcessCreditCard_FA.getCardType(m_scra.getCardIIN()));
                                cardInfo.setCardLast4(m_scra.getCardLast4());
                                cardInfo.setEncryptedTrack1(m_scra.getTrack1());
                                cardInfo.setEncryptedTrack2(m_scra.getTrack2());
                                cardInfo.setCardNumAESEncrypted(encrypt.encryptWithAES(m_scra.getCardPAN()));
                                if (m_scra.getTrack1Masked() != null && !m_scra.getTrack1Masked().isEmpty())
                                    cardInfo.setEncryptedAESTrack1(encrypt.encryptWithAES(m_scra.getTrack1Masked()));
                                if (m_scra.getTrack2Masked() != null && !m_scra.getTrack2Masked().isEmpty())
                                    cardInfo.setEncryptedAESTrack2(encrypt.encryptWithAES(m_scra.getTrack2Masked()));
                                cardInfo.setDeviceSerialNumber(m_scra.getDeviceSerial());
                                cardInfo.setMagnePrint(m_scra.getMagnePrint());
                                cardInfo.setCardNumUnencrypted(m_scra.getCardPAN());
                                cardInfo.setMagnePrintStatus(m_scra.getMagnePrintStatus());
                                cardInfo.setTrackDataKSN(m_scra.getKSN());
                            }
                            scannerCallBack.cardWasReadSuccessfully(true, cardInfo);
                        }
                        break;
                    case MTSCRAEvent.OnDeviceResponse:
//                        OnDeviceResponse((String) msg.obj);
                        break;
                    case MTEMVEvent.OnTransactionStatus:
//                        OnTransactionStatus((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnDisplayMessageRequest:
//                        OnDisplayMessageRequest((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnUserSelectionRequest:
//                        OnUserSelectionRequest((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnARQCReceived:
//                        OnARQCReceived((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnTransactionResult:
//                        OnTransactionResult((byte[]) msg.obj);
                        break;

                    case MTEMVEvent.OnEMVCommandResult:
//                        OnEMVCommandResult((byte[]) msg.obj);
                        break;

                    case MTEMVEvent.OnDeviceExtendedResponse:
//                        OnDeviceExtendedResponse((String) msg.obj);
                        break;
                }
            } catch (Exception ex) {

            }

            return true;
        }
    }

    public class processConnectionAsync extends AsyncTask<Boolean, String, Boolean> {
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            SerialPort port;
            try {
                port = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                OutputStream stream = port.getOutputStream();
                InputStream iStream = port.getInputStream();
                SerialPort eloPrinterPort = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);
                eloPrinterApi = new PrinterAPI(eloPrinterPort);
                if (!eloPrinterApi.isPaperAvailable()) {
//                    Toast.makeText(activity, "Printer out of paper!", Toast.LENGTH_LONG).show();
                }
                eloPrinterPort.getInputStream().close();
                eloPrinterPort.getOutputStream().close();
                eloPrinterPort.close();

                didConnect = true;
            } catch (IOException e) {
                didConnect = false;
                e.printStackTrace();
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(Boolean showAlert) {
            if (didConnect) {
                playSound();
                edm.driverDidConnectToDevice(thisInstance, showAlert, activity);
            } else {
                edm.driverDidNotConnectToDevice(thisInstance, "", showAlert, activity);
            }
        }
    }

    class Elo1Barcodereader implements BarCodeReaderAdapter {
        BarcodeReader barcodeReader = new BarcodeReader();

        @Override
        public boolean isBarCodeReaderEnabled() {
            return barcodeReader.isBcrOn();
        }

        @Override
        public void setBarCodeReaderEnabled(boolean enabled) {
            barcodeReader.turnOnLaser();
        }

        @Override
        public boolean isBarCodeReaderKbdMode() {
            return true;
        }

        @Override
        public void setBarCodeReaderKbdMode() {

        }
    }

    class EloRefreshBarcodereader implements BarCodeReaderAdapter {
        BarCodeReader barCodeReader;

        EloRefreshBarcodereader() {
            try {
                barCodeReader = DeviceManager.getInstance(DeviceManager.getPlatformInfo().eloPlatform, activity).getBarCodeReader();
            } catch (UnsupportedEloPlatform unsupportedEloPlatform) {

            }
        }

        @Override
        public boolean isBarCodeReaderEnabled() {
            return barCodeReader.getStatus() == Status.ENABLED;
        }

        @Override
        public void setBarCodeReaderEnabled(boolean enabled) {
            barCodeReader.setEnabled(enabled);
        }

        @Override
        public boolean isBarCodeReaderKbdMode() {
            return barCodeReader.isKbMode(activity);
        }

        @Override
        public void setBarCodeReaderKbdMode() {
            barCodeReader.setKbMode(activity);
        }
    }
}