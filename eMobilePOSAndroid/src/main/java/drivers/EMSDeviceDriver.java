package drivers;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.RemoteException;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.dao.AssignEmployeeDAO;
import com.android.dao.ClerkDAO;
import com.android.dao.ShiftDAO;
import com.android.dao.ShiftExpensesDAO;
import com.android.dao.StoredPaymentsDAO;
import com.android.dao.TermsNConditionsDAO;
import com.android.database.CustomersHandler;
import com.android.database.InvProdHandler;
import com.android.database.InvoicesHandler;
import com.android.database.MemoTextHandler;
import com.android.database.OrderProductsHandler;
import com.android.database.OrdersHandler;
import com.android.database.PayMethodsHandler;
import com.android.database.PaymentsHandler;
import com.android.database.ProductsHandler;
import com.android.emobilepos.BuildConfig;
import com.android.emobilepos.R;
import com.android.emobilepos.models.ClockInOut;
import com.android.emobilepos.models.DataTaxes;
import com.android.emobilepos.models.EMVContainer;
import com.android.emobilepos.models.Orders;
import com.android.emobilepos.models.PaymentDetails;
import com.android.emobilepos.models.Receipt;
import com.android.emobilepos.models.SplittedOrder;
import com.android.emobilepos.models.Tax;
import com.android.emobilepos.models.orders.Order;
import com.android.emobilepos.models.orders.OrderProduct;
import com.android.emobilepos.models.orders.SalesByClerk;
import com.android.emobilepos.models.realms.AssignEmployee;
import com.android.emobilepos.models.realms.Clerk;
import com.android.emobilepos.models.realms.OrderAttributes;
import com.android.emobilepos.models.realms.Payment;
import com.android.emobilepos.models.realms.Shift;
import com.android.emobilepos.models.realms.ShiftExpense;
import com.android.emobilepos.models.realms.TermsNConditions;
import com.android.emobilepos.payment.ProcessGenius_FA;
import com.android.support.ConsignmentTransaction;
import com.android.support.Customer;
import com.android.support.DateUtils;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.TaxesCalculator;
import com.crashlytics.android.Crashlytics;
import com.elo.device.peripherals.Printer;
import com.mpowa.android.sdk.powapos.PowaPOS;
import com.pax.poslink.peripheries.POSLinkPrinter;
import com.pax.poslink.peripheries.ProcessResult;
import com.printer.aidl.PService;
import com.printer.command.EscCommand;
import com.printer.command.PrinterCom;
import com.printer.command.PrinterUtils;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;
import com.thefactoryhka.android.pa.TfhkaAndroid;
import com.uniquesecure.meposconnect.MePOS;
import com.uniquesecure.meposconnect.MePOSConnectionType;
import com.uniquesecure.meposconnect.MePOSException;
import com.uniquesecure.meposconnect.MePOSPrinterCallback;
import com.uniquesecure.meposconnect.MePOSReceipt;
import com.uniquesecure.meposconnect.MePOSReceiptImageLine;
import com.uniquesecure.meposconnect.MePOSReceiptLine;
import com.uniquesecure.meposconnect.MePOSReceiptTextLine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import POSSDK.POSSDK;
import datamaxoneil.connection.Connection_Bluetooth;
import datamaxoneil.printer.DocumentLP;
import drivers.elo.utils.PrinterAPI;
import drivers.pax.utils.PosLinkHelper;
import drivers.star.utils.MiniPrinterFunctions;
import drivers.star.utils.PrinterFunctions;
import drivers.star.utils.sdk31.starprntsdk.PrinterSetting;
import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import main.EMSDeviceManager;
import plaintext.EMSPlainTextHelper;
import util.BitmapUtils;
import util.StringUtil;
import wangpos.sdk4.libbasebinder.Core;

import static com.android.support.DateUtils.getEpochTime;
import static drivers.EMSGPrinterPT380.PRINTER_ID;
import static jpos.POSPrinterConst.PTR_BM_ASIS;
import static jpos.POSPrinterConst.PTR_BM_CENTER;
import static jpos.POSPrinterConst.PTR_S_RECEIPT;

public class EMSDeviceDriver {
    private static final boolean PRINT_TO_LOG = BuildConfig.PRINT_TO_LOG;
    /*static PrinterApiContext printerApi;*/
    static Object printerTFHKA;
    private static int PAPER_WIDTH;
    private final int SIZE_LIMIT = 2;
    private final int SLEEP_TIME = 100;
    protected final String FORMAT = "windows-1252";
    private final int ALIGN_LEFT = 0, ALIGN_CENTER = 1;
    protected EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
    protected List<String> printPref;
    protected MyPreferences myPref;
    protected Context activity;
    protected StarIOPort port;
    protected String encodedSignature;
    protected boolean isPOSPrinter = false;
    protected String encodedQRCode = "";
    protected Connection_Bluetooth device;
    byte[] enableCenter, disableCenter;
    PowaPOS powaPOS;
    MePOS mePOS;
    POSSDK pos_sdk = null;
    PrinterAPI eloPrinterApi;
    Printer eloPrinterRefresh;
    POSPrinter bixolonPrinter;
    POSPrinter hpPrinter;
    PService mPService = null;
    com.epson.epos2.printer.Printer epsonPrinter = null;
    MePOSReceipt mePOSReceipt;
    InputStream inputStream;
    OutputStream outputStream;
    Typeface typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
    private double saveAmount;
    private StarIoExt.Emulation emulation = StarIoExt.Emulation.StarGraphic;
    private EscCommand esc;
    POSLinkPrinter.PrintDataFormatter printDataFormatter;
    private static final int BMP_WIDTH_OF_TIMES = 4;
    private static final int BYTE_PER_PIXEL = 3;

    wangpos.sdk4.libbasebinder.Printer aptPrinter;
    Core aptCore;

    private static byte[] convertFromListbyteArrayTobyteArray(List<byte[]> ByteArray) {
        int dataLength = 0;
        for (int i = 0; i < ByteArray.size(); i++) {
            dataLength += ByteArray.get(i).length;
        }

        int distPosition = 0;
        byte[] byteArray = new byte[dataLength];
        for (int i = 0; i < ByteArray.size(); i++) {
            System.arraycopy(ByteArray.get(i), 0, byteArray, distPosition, ByteArray.get(i).length);
            distPosition += ByteArray.get(i).length;
        }

        return byteArray;
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        int margins = Double.valueOf(PAPER_WIDTH * .10).intValue();
        float ratio = Integer.valueOf(PAPER_WIDTH - margins).floatValue() / Integer.valueOf(b.getWidth()).floatValue();
        int width = Math.round(ratio * b.getWidth());
        int height = Math.round(ratio * b.getHeight());
        b = Bitmap.createScaledBitmap(b, width, height, true);
        return b;
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(maxImageSize / realImage.getWidth(),
                maxImageSize / realImage.getHeight());
        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());

        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    public void connect(Context activity, int paperSize, boolean isPOSPrinter, EMSDeviceManager edm) {
    }

    public boolean autoConnect(Activity activity, EMSDeviceManager edm, int paperSize, boolean isPOSPrinter,
                               String portName, String portNumber) {
        return false;
    }

    public void registerAll() {
    }

    public void setPaperWidth(int lineWidth) {
        if (this instanceof EMSStar) {
            switch (lineWidth) {
                case 32:
                    PAPER_WIDTH = 408;
                    break;
                case 44:
                case 48:
                    PAPER_WIDTH = 576;
                    break;
                case 69:
                    PAPER_WIDTH = 832;// 5400
                    break;
            }
        } else {
            switch (lineWidth) {
                case 32:
                    PAPER_WIDTH = 420;
                    break;
                case 42:
                    PAPER_WIDTH = 576;
                    break;
                case 48:
                    PAPER_WIDTH = 1600;
                    break;
                case 69:
                    PAPER_WIDTH = 300;// 5400
                    break;
            }
        }
    }

    private void addTotalLines(Context context, Order anOrder, List<OrderProduct> orderProducts, StringBuilder sb, int lineWidth) {
        BigDecimal itemDiscTotal = new BigDecimal(0);
        for (OrderProduct orderProduct : orderProducts) {
            try {
                itemDiscTotal = itemDiscTotal.add(Global.getBigDecimalNum(orderProduct.getDiscount_value()));
            } catch (NumberFormatException e) {
                itemDiscTotal = new BigDecimal(0);
            }
        }
        saveAmount = itemDiscTotal.add(TextUtils.isEmpty(anOrder.ord_discount) ? new BigDecimal(0) : new BigDecimal(anOrder.ord_discount)).doubleValue();
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(context.getString(R.string.receipt_subtotal),
                Global.getCurrencyFormat(Global.getBigDecimalNum(anOrder.ord_subtotal).add(itemDiscTotal).toString()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(context.getString(R.string.receipt_discount_line_item),
                Global.getCurrencyFormat(String.valueOf(itemDiscTotal)), lineWidth, 0));

        String discountName = "";
        if (anOrder.ord_discount_id != null && !anOrder.ord_discount_id.isEmpty()) {
            ProductsHandler productDBHandler = new ProductsHandler(activity);
            discountName = productDBHandler.getDiscountName(anOrder.ord_discount_id);
        }
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                context.getString(R.string.receipt_global_discount) + " " + discountName,
                Global.getCurrencyFormat(anOrder.ord_discount), lineWidth, 0));

        sb.append(textHandler.twoColumnLineWithLeftAlignedText(context.getString(R.string.receipt_tax),
                Global.getCurrencyFormat(anOrder.ord_taxamount), lineWidth, 0));
    }

    private void addTaxesLine(List<DataTaxes> taxes, Order order, int lineWidth, StringBuilder sb) {
        if (myPref.getPreferences(MyPreferences.pref_print_taxes_breakdown)) {
            if (myPref.isRetailTaxes()) {
                HashMap<String, String[]> prodTaxes = new HashMap<>();
                for (OrderProduct product : order.getOrderProducts()) {
                    if (product.getTaxes() != null) {
                        for (Tax tax : product.getTaxes()) {
                            if (prodTaxes.containsKey(tax.getTaxRate())) {
                                BigDecimal taxAmount = new BigDecimal(prodTaxes.get(tax.getTaxRate())[1]);
                                taxAmount = taxAmount.add(TaxesCalculator.taxRounder(tax.getTaxAmount()));
                                String[] arr = new String[2];
                                arr[0] = tax.getTaxName();
                                arr[1] = String.valueOf(taxAmount);
                                prodTaxes.put(tax.getTaxRate(), arr);
                            } else {
                                BigDecimal taxAmount = TaxesCalculator.taxRounder(tax.getTaxAmount());
                                String[] arr = new String[2];
                                arr[0] = tax.getTaxName();
                                arr[1] = String.valueOf(taxAmount);
                                prodTaxes.put(tax.getTaxRate(), arr);
                            }
                        }
                    }
                }
                Iterator it = prodTaxes.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String[]> pair = (Map.Entry<String, String[]>) it.next();

                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(pair.getValue()[0],
                            Global.getCurrencyFormat(String.valueOf(pair.getValue()[1])), lineWidth, 2));
                    it.remove();
                }
            } else if (taxes != null) {
                for (DataTaxes tax : taxes) {
                    BigDecimal taxAmount = new BigDecimal(0);
                    List<BigDecimal> rates = new ArrayList<>();
                    rates.add(new BigDecimal(tax.getTax_rate()));
                    for (OrderProduct product : order.getOrderProducts()) {
                        taxAmount = taxAmount.add(TaxesCalculator.calculateTax(product.getProductPriceTaxableAmountCalculated(), rates));
                    }
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(tax.getTax_name(),
                            Global.getCurrencyFormat(String.valueOf(taxAmount)), lineWidth, 2));
                }
            }
        }
    }

    public void releasePrinter() {
        if (this instanceof EMSStar) {
            if (port != null) {
                try {
                    StarIOPort.releasePort(port);
                } catch (Exception e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);
                } finally {
                    port = null;
                }
            }
        } else if (this instanceof EMSOneil4te) {
            if (device != null && device.getIsOpen())
                device.close();
        }
    }

    protected boolean SendCmd(String cmd) {
        Log.d("BixolonCMD", cmd);
        if (printerTFHKA instanceof TfhkaAndroid) {
            return ((TfhkaAndroid) printerTFHKA).SendCmd(cmd);
        } else {
            return ((com.thefactoryhka.android.rd.TfhkaAndroid) printerTFHKA).SendCmd(cmd);
        }
    }

    protected void print(String str) {
        str = removeAccents(str);
        if (PRINT_TO_LOG) {
            Log.d("Print", str);
            return;
        }
        if (this instanceof EMSBixolonRD) {
            String[] split = str.split(("\n"));
            for (String line : split) {
                SendCmd(String.format("80*%s", line));
            }
        } else if (this instanceof EMSAPT50) {
            apt50RasterPrint(str);
        } else if (this instanceof EMSGPrinterPT380) {
            // print line
            esc = new EscCommand();
            esc.addInitializePrinter();
            esc.addText(str);
            esc.addQueryPrinterStatus();
            printGPrinter(esc);
        } else if (this instanceof EMSELO) {
            if (eloPrinterRefresh != null) {
                eloPrinterRefresh.print(str);
            } else
                eloPrinterApi.print(str);
        } else if (this instanceof EMSmePOS) {
            mePOSReceipt.addLine(new MePOSReceiptTextLine(str, MePOS.TEXT_STYLE_NONE, MePOS.TEXT_SIZE_NORMAL, MePOS.TEXT_POSITION_LEFT));
        } else if (this instanceof EMSStar) {
            try {
                printStar(str, 0, PrinterFunctions.Alignment.Left);
//                port.writePort(str.getBytes(), 0, str.length());
            } catch (StarIOPortException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (this instanceof EMSBlueBambooP25) {
            byte[] header = {0x1B, 0x21, 0x01};
            byte[] lang = new byte[]{(byte) 0x1B, (byte) 0x4B, (byte) 0x31, (byte) 0x1B, (byte) 0x52, 48};

            try {
                this.outputStream.write(header);
                this.outputStream.write(lang);
                this.outputStream.write(str.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (this instanceof EMSOneil4te) {
            device.write(str);
        } else if (this instanceof EMSHPEngageOnePrimePrinter) {
            try {
                hpPrinter.open("HPEngageOnePrimePrinter");
                hpPrinter.claim(10000);
                hpPrinter.setDeviceEnabled(true);
                hpPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, str);
            } catch (JposException e) {
                e.printStackTrace();
            } finally {
                try {
                    hpPrinter.close();
                } catch (JposException e) {
                    e.printStackTrace();
                }
            }
        } else if (this instanceof EMSBixolon) {
            try {
                bixolonPrinter.open(myPref.getPrinterName());
                bixolonPrinter.claim(10000);
                bixolonPrinter.setDeviceEnabled(true);
                bixolonPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, str);
            } catch (JposException e) {
                e.printStackTrace();
            } finally {
                try {
                    bixolonPrinter.close();
                } catch (JposException e) {
                    e.printStackTrace();
                }
            }
        } else if (this instanceof EMSPowaPOS) {
            powaPOS.printText(str);
        } else if (this instanceof EMSsnbc) {
            byte[] send_buf;
            try {
                send_buf = str.getBytes("GB18030");
                pos_sdk.textPrint(send_buf, send_buf.length);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (this instanceof EMSPaxA920) {
            try {
                if (myPref.isRasterModePrint()) {
                    posLinkRasterPrint(str);
                } else {
                    posLinkPrint(str);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void print(byte[] byteArray) {
        if (PRINT_TO_LOG) {
            Log.d("Print", new String(byteArray));
            return;
        }
        if (this instanceof EMSBixolonRD) {
            String[] split = new String(byteArray).split(("\n"));
            for (String line : split) {
                SendCmd(String.format("80*%s", line));
            }
        } else if (this instanceof EMSGPrinterPT380) {
            print(new String(byteArray));
        } else if (this instanceof EMSELO) {
            if (eloPrinterRefresh != null) {
                eloPrinterRefresh.print(new String(byteArray));
            } else {
                eloPrinterApi.print(new String(byteArray));
            }
        } else if (this instanceof EMSmePOS) {
            mePOSReceipt.addLine(new MePOSReceiptTextLine(new String(byteArray), MePOS.TEXT_STYLE_NONE, MePOS.TEXT_SIZE_NORMAL, MePOS.TEXT_POSITION_LEFT));
        } else if (this instanceof EMSStar) {
            try {
                printStar(new String(byteArray), 0, PrinterFunctions.Alignment.Left);
            } catch (StarIOPortException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (this instanceof EMSAPT50) {
            String str = new String(byteArray);
            apt50RasterPrint(str);
        } else if (this instanceof EMSBlueBambooP25) {
            try {
                outputStream.write(byteArray);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (this instanceof EMSOneil4te) {
            device.write(byteArray);
        } else if (this instanceof EMSPowaPOS) {
            powaPOS.printText(new String(byteArray));
        } else if (this instanceof EMSsnbc) {
            byte[] send_buf;
            try {
                send_buf = new String(byteArray).getBytes("GB18030");
                pos_sdk.textPrint(send_buf, send_buf.length);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (this instanceof EMSHPEngageOnePrimePrinter) {
            try {
                hpPrinter.open("HPEngageOnePrimePrinter");
                hpPrinter.claim(10000);
                hpPrinter.setDeviceEnabled(true);
                hpPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, new String(byteArray));
            } catch (JposException e) {
                e.printStackTrace();
            } finally {
                try {
                    hpPrinter.close();
                } catch (JposException e) {
                    e.printStackTrace();
                }
            }
        } else if (this instanceof EMSBixolon) {
            try {
                bixolonPrinter.open(myPref.getPrinterName());
                bixolonPrinter.claim(10000);
                bixolonPrinter.setDeviceEnabled(true);
                bixolonPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, new String(byteArray));
            } catch (JposException e) {
                e.printStackTrace();
            } finally {
                try {
                    bixolonPrinter.close();
                } catch (JposException e) {
                    e.printStackTrace();
                }
            }
        } else if (this instanceof EMSPaxA920) {
            try {
                String str = new String(byteArray);
                if (myPref.isRasterModePrint()) {
                    posLinkRasterPrint(str);
                } else {
                    posLinkPrint(str);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String removeAccents(String str) {
        str = Normalizer.normalize(str, Normalizer.Form.NFD);
        str = str.replaceAll("[^\\p{ASCII}]", "");
        return str;
    }

    public void print(String str, String FORMAT) {
        print(str, FORMAT, 0, PrinterFunctions.Alignment.Left);
    }

    public void print(String str, int size, PrinterFunctions.Alignment alignment) {
        print(str, FORMAT, size, alignment);
    }

    public void startReceipt() {
        if (myPref.getPrinterName().toUpperCase().contains("MPOP")) {
            emulation = StarIoExt.Emulation.StarPRNT;
        } else {
            emulation = StarIoExt.Emulation.StarGraphic;
        }
        if (this instanceof EMSmePOS) {
            mePOSReceipt = new MePOSReceipt();
        }
    }

    private void finishReceipt() {
        if (this instanceof EMSmePOS) {
            try {
                int loopCount = 0;
                while (mePOS.printerBusy()) {
                    Thread.sleep(8000);
                    loopCount++;
                    if (loopCount > 6) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            MePOSReceipt receipt = new MePOSReceipt();
            receipt.setCutType(0);

            for (MePOSReceiptLine line : mePOSReceipt.getLines()) {
                if (line instanceof MePOSReceiptTextLine) {
                    String[] split = ((MePOSReceiptTextLine) line).text.split(("\n"));
                    for (String str : split) {
                        MePOSReceiptTextLine textLine = new MePOSReceiptTextLine(str, MePOS.TEXT_STYLE_NONE, MePOS.TEXT_SIZE_NORMAL, MePOS.TEXT_POSITION_LEFT);
                        receipt.addLine(textLine);
                    }
                } else {
                    receipt.addLine(line);
                }
            }

            mePOS.print(receipt, new MePOSPrinterCallback() {
                @Override
                public void onPrinterStarted(MePOSConnectionType mePOSConnectionType, String s) {
                }

                @Override
                public void onPrinterCompleted(MePOSConnectionType mePOSConnectionType, String s) {
                    if (mePOSReceipt != null) {
                        synchronized (mePOSReceipt) {
                            mePOSReceipt.notifyAll();
                        }
                    }
                }

                @Override
                public void onPrinterError(MePOSException e) {
                    if (mePOSReceipt != null) {
                        synchronized (mePOSReceipt) {
                            mePOSReceipt.notifyAll();
                        }
                    }
                }
            });
            synchronized (mePOSReceipt) {
//                try {
//                    mePOSReceipt.wait(12000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
            mePOSReceipt = null;
        } else if (this instanceof EMSGPrinterPT380) {
            // print footer
            esc = new EscCommand();
            esc.addInitializePrinter();
            esc.addPrintAndFeedLines((byte) 3);
            esc.addQueryPrinterStatus();
            printGPrinter(esc);
        }
    }

    private void posLinkPrint(String stringToPrint) {
        if (!stringToPrint.isEmpty()) {
            printDataFormatter.clear();
            printDataFormatter.addLeftAlign().addContent(stringToPrint);
            POSLinkPrinter.getInstance(activity).print(printDataFormatter.build(),
                    POSLinkPrinter.CutMode.FULL_PAPER_CUT,
                    PosLinkHelper.getCommSetting(myPref.getPaymentDevice(),myPref.getPaymentDeviceIP()),
                    new POSLinkPrinter.PrintListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(ProcessResult processResult) {

                        }
                    });
        }
    }

    private void posLinkPrint(Bitmap bitmapToPrint) {
        if (bitmapToPrint.getHeight() > 0 && bitmapToPrint.getWidth() > 0) {
            POSLinkPrinter.getInstance(activity).print(bitmapToPrint,
                    POSLinkPrinter.CutMode.FULL_PAPER_CUT,
                    new POSLinkPrinter.PrintListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(ProcessResult processResult) {
                        }
                    });
        }
    }

    private void apt50PrintPaper() {
        if (this instanceof EMSAPT50) {
            try {
                aptPrinter.printInit();
                aptPrinter.clearPrintDataCache();
                aptPrinter.printPaper(30);
                aptPrinter.printFinish();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void apt50Leds(String led) {
        try {
            switch (led) {
                case "on":
                    aptCore.led(1, 1, 1, 1, 1);
                    break;
                case "off":
                    aptCore.led(0, 0, 0, 0, 0);
                    break;
                case "blue":
                    aptCore.led(1, 0, 0, 0, 1);
                    break;
                case "yellow":
                    aptCore.led(0, 1, 0, 0, 1);
                    break;
                case "green":
                    aptCore.led(0, 0, 1, 0, 1);
                    break;
                case "red":
                    aptCore.led(0, 0, 0, 1, 1);
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void apt50RasterPrint(String stringToPrint) {
        Bitmap bitmapFromString = BitmapUtils.createBitmapFromText(
                stringToPrint, 20, 450, typeface);
        if (bitmapFromString.getHeight() > 0 && bitmapFromString.getWidth() > 0) {
            try {
                apt50Leds("yellow");
                aptPrinter.printInit();
                aptPrinter.clearPrintDataCache();
                aptPrinter.printImageBase(rescaleBitmap(bitmapFromString),
                        bitmapFromString.getWidth(),
                        bitmapFromString.getHeight(),
                        wangpos.sdk4.libbasebinder.Printer.Align.LEFT,
                        0);
                aptPrinter.printFinish();
                apt50Leds("off");
                apt50Leds("blue");
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    }

    private void posLinkRasterPrint(String stringToPrint) {
        Bitmap bitmapFromString = BitmapUtils.createBitmapFromText(
                stringToPrint, 26, 450, typeface);
        if (bitmapFromString.getHeight() > 0 && bitmapFromString.getWidth() > 0) {
            POSLinkPrinter.getInstance(activity).print(bitmapFromString,
                    POSLinkPrinter.CutMode.FULL_PAPER_CUT,
                    new POSLinkPrinter.PrintListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(ProcessResult processResult) {
                        }
                    });
        }
    }

    private void printGPrinter(EscCommand escCommand) {
        Vector<Byte> datas = escCommand.getCommand();
        byte[] bytes = PrinterUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rs;
        try {
            rs = mPService.sendEscCommand(PRINTER_ID, sss);
            PrinterCom.ERROR_CODE r = PrinterCom.ERROR_CODE.values()[rs];
            if (r != PrinterCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(activity, PrinterCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void printStar(String str, int size, PrinterFunctions.Alignment alignment) throws StarIOPortException, UnsupportedEncodingException {
        if (port == null) {
            return;
        }
        if (!isPOSPrinter) {
            if (size > 0) {
                MiniPrinterFunctions.PrintText(activity, port.getPortName(), port.getPortSettings()
                        , false, false, false, false,
                        Integer.valueOf(size).byteValue(), Integer.valueOf(size).byteValue(),
                        0, alignment
                        , str.getBytes());
            } else {
                port.writePort(new byte[]{0x1d, 0x57, (byte) 0x80, 0x31}, 0, 4);
                port.writePort(new byte[]{0x1d, 0x21, 0x00}, 0, 3);
                port.writePort(new byte[]{0x1b, 0x74, 0x11}, 0, 3); // set to
                // windows-1252
                port.writePort(str.getBytes(), 0, str.length());
            }
        } else if (size > 0) {
            ArrayList<byte[]> commands = new ArrayList<>();
            commands.add(new byte[]{0x1b, 0x40}); // Initialization
            byte[] characterheightExpansion = new byte[]{0x1b, 0x68, 0x00};
            characterheightExpansion[2] = 49;
            commands.add(characterheightExpansion);
            byte[] characterwidthExpansion = new byte[]{0x1b, 0x57, 0x00};
            characterwidthExpansion[2] = 49;
            commands.add(characterwidthExpansion);
            commands.add(str.getBytes());
            commands.add(new byte[]{0x0a});
            byte[] commandToSendToPrinter = convertFromListbyteArrayTobyteArray(commands);
            port.writePort(commandToSendToPrinter, 0, commandToSendToPrinter.length);
        } else {
            if (myPref.isRasterModePrint()) {
                Bitmap bitmapFromText = BitmapUtils.createBitmapFromText(str, 20
                        , PAPER_WIDTH, typeface);
                ICommandBuilder builder = StarIoExt.createCommandBuilder(emulation);
                builder.beginDocument();
                builder.appendBitmap(bitmapFromText, false);
                builder.endDocument();
                byte[] cmds = builder.getCommands();
                port.writePort(cmds, 0, cmds.length);
            } else {
                ArrayList<byte[]> commands = new ArrayList<>();
                commands.add(new byte[]{0x1b, 0x40}); // Initialization
                byte[] characterheightExpansion = new byte[]{0x1b, 0x68, 0x00};
                characterheightExpansion[2] = 48;
                commands.add(characterheightExpansion);
                byte[] characterwidthExpansion = new byte[]{0x1b, 0x57, 0x00};
                characterwidthExpansion[2] = 48;
                commands.add(characterwidthExpansion);
                commands.add(new byte[]{0x0a});
                byte[] commandToSendToPrinter = convertFromListbyteArrayTobyteArray(commands);
                port.writePort(commandToSendToPrinter, 0, commandToSendToPrinter.length);
                port.writePort(str.getBytes(FORMAT), 0, str.length());
            }
        }
    }

    protected void print(String str, String FORMAT, int size, PrinterFunctions.Alignment alignment) {
        str = removeAccents(str);
        if (PRINT_TO_LOG) {
            Log.d("Print", str);
            return;
        }
        if (this instanceof EMSBixolonRD) {
            String[] split = str.split(("\n"));
            for (String line : split) {
                if (size > 0) {
                    SendCmd(String.format("80>%s", str));
                } else {
                    SendCmd(String.format("80*%s", line));
                }
            }
        } else if (this instanceof EMSGPrinterPT380) {
            print(str);
        } else if (this instanceof EMSELO) {
            if (eloPrinterRefresh != null) {
                eloPrinterRefresh.print(str);
            } else
                eloPrinterApi.print(str);
        } else if (this instanceof EMSmePOS) {
            mePOSReceipt.addLine(new MePOSReceiptTextLine(str, MePOS.TEXT_STYLE_NONE, MePOS.TEXT_SIZE_NORMAL, MePOS.TEXT_POSITION_LEFT));
        } else if (this instanceof EMSStar) {
            try {
                printStar(str, size, alignment);
            } catch (StarIOPortException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (this instanceof EMSAPT50) {
            print(str);
        } else if (this instanceof EMSBlueBambooP25) {
            print(str);
        } else if (this instanceof EMSOneil4te) {
            try {
                device.write(str.getBytes(FORMAT));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (this instanceof EMSPowaPOS) {
            powaPOS.printText(str);
        } else if (this instanceof EMSsnbc) {
            print(str);
        } else if (this instanceof EMSHPEngageOnePrimePrinter) {
            print(str);
        } else if (this instanceof EMSBixolon) {
            try {
                bixolonPrinter.open(myPref.getPrinterName());
                bixolonPrinter.claim(10000);
                bixolonPrinter.setDeviceEnabled(true);
                bixolonPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, str);
            } catch (JposException e) {
                e.printStackTrace();
            } finally {
                try {
                    bixolonPrinter.close();
                } catch (JposException e) {
                    e.printStackTrace();
                }
            }
        } else if (this instanceof EMSPaxA920) {
            try {
                if (myPref.isRasterModePrint()) {
                    posLinkRasterPrint(str);
                } else {
                    posLinkPrint(str);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printEMVSection(EMVContainer emvContainer, int lineWidth) {
        if (emvContainer != null && emvContainer.getGeniusResponse() != null) {
            StringBuilder sb = new StringBuilder();
            if (emvContainer.getGeniusResponse().getAdditionalParameters() != null &&
                    emvContainer.getGeniusResponse().getAdditionalParameters().getEMV() != null) {

                // Entry Method
                String entryMethod = emvContainer.getGeniusResponse()
                        .getAdditionalParameters().getEMV().getEntryModeMessage();
                if (!TextUtils.isEmpty(entryMethod)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                            getString(R.string.pax_entry_method),
                            entryMethod, lineWidth, 0));
                }

                // Application Label
                String applicationLabel = emvContainer.getGeniusResponse().getAdditionalParameters()
                        .getEMV().getApplicationInformation().getApplicationLabel();
                if (!TextUtils.isEmpty(applicationLabel)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                            getString(R.string.genius_application_label),
                            applicationLabel, lineWidth, 0));
                }

                // AID
                String aid = emvContainer.getGeniusResponse().getAdditionalParameters()
                        .getEMV().getApplicationInformation().getAid();
                if (!TextUtils.isEmpty(aid)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                            getString(R.string.genius_aid),
                            aid, lineWidth, 0));
                }

                if (emvContainer.getGeniusResponse().getPaymentType().equalsIgnoreCase(ProcessGenius_FA.Limiters.DISCOVER.name()) ||
                        emvContainer.getGeniusResponse().getPaymentType().equalsIgnoreCase(ProcessGenius_FA.Limiters.AMEX.name()) ||
                        emvContainer.getGeniusResponse().getPaymentType().equalsIgnoreCase("EMVCo")) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.card_exp_date),
                            emvContainer.getGeniusResponse().getAdditionalParameters().getEMV().getCardInformation().getCardExpiryDate(), lineWidth, 0));
                }
                if (emvContainer.getGeniusResponse().getPaymentType().equalsIgnoreCase(ProcessGenius_FA.Limiters.AMEX.name())) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.cryptogram_type),
                            emvContainer.getGeniusResponse().getAdditionalParameters().getEMV().getApplicationCryptogram().getCryptogramType(), lineWidth, 0));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.cryptogram),
                            emvContainer.getGeniusResponse().getAdditionalParameters().getEMV().getApplicationCryptogram().getCryptogram(), lineWidth, 0));
                }

                // PIN Statement
                String pinStatement = emvContainer.getGeniusResponse().getAdditionalParameters()
                        .getEMV().getPINStatement();
                if (!TextUtils.isEmpty(pinStatement)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                            getString(R.string.pin_statement),
                            pinStatement, lineWidth, 0));
                }

                // TVR
                String tvr = emvContainer.getGeniusResponse().getAdditionalParameters()
                        .getEMV().getTVR();
                if (!TextUtils.isEmpty(tvr)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                            getString(R.string.pax_tvr),
                            tvr, lineWidth, 0));
                }

                // IAD
                String iad = emvContainer.getGeniusResponse().getAdditionalParameters()
                        .getEMV().getIAD();
                if (!TextUtils.isEmpty(iad)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                            getString(R.string.pax_iad),
                            iad, lineWidth, 0));
                }

                // TSI
                String tsi = emvContainer.getGeniusResponse().getAdditionalParameters()
                        .getEMV().getTSI();
                if (!TextUtils.isEmpty(tsi)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                            getString(R.string.pax_tsi_atc),
                            tsi, lineWidth, 0));
                }

                // AC
                String ac = emvContainer.getGeniusResponse().getAdditionalParameters()
                        .getEMV().getAC();
                if (!TextUtils.isEmpty(ac)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                            getString(R.string.pax_ac),
                            ac, lineWidth, 0));
                }

                sb.append("\n\n");
                print(sb.toString());
            }
        }
    }

    public void printReceiptPreview(Bitmap bitmap, int lineWidth) {
        startReceipt();
        setPaperWidth(lineWidth);
        printPref = myPref.getPrintingPreferences();
        printImage(bitmap);
        cutPaper();
    }

    public void printReceiptPreview(SplittedOrder splitedOrder, int lineWidth) {
        AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
        startReceipt();
        setPaperWidth(lineWidth);
        printPref = myPref.getPrintingPreferences();
        printImage(0);
        printHeader(lineWidth);
        StringBuilder sb = new StringBuilder();
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.sales_receipt) + ":", splitedOrder.ord_id,
                lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_date),
                DateUtils.getDateAsString(new Date(), "MMM/dd/yyyy"), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_employee),
                String.format("%s(%s)", employee.getEmpName(), employee.getEmpId()), lineWidth, 0));
        for (OrderProduct product : splitedOrder.getOrderProducts()) {
            BigDecimal qty = Global.getBigDecimalNum(product.getOrdprod_qty());
            sb.append(textHandler.oneColumnLineWithLeftAlignedText(String.format("%sx %s", product.getOrdprod_qty(), product.getOrdprod_name()), lineWidth, 1));
            for (OrderProduct addon : product.addonsProducts) {
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("- " + addon.getOrdprod_name(),
                        Global.getCurrencyFormat(addon.getFinalPrice())
                        , lineWidth, 3));
            }
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_price),
                    Global.getCurrencyFormat(product.getFinalPrice()), lineWidth, 3));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_discount),
                    Global.getCurrencyFormat(product.getDiscount_value()), lineWidth, 3));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total),
                    Global.getCurrencyFormat(Global.getBigDecimalNum(product.getItemTotal())
                            .multiply(qty).toString()), lineWidth, 3));
            sb.append(textHandler.oneColumnLineWithLeftAlignedText(getString(R.string.receipt_description), lineWidth, 3));
            if (!TextUtils.isEmpty(product.getOrdprod_desc())) {
                StringTokenizer tokenizer = new StringTokenizer(product.getOrdprod_desc(), "<br/>");
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(tokenizer.nextToken(), lineWidth, 3));
            } else {
                sb.append(textHandler.oneColumnLineWithLeftAlignedText("", lineWidth, 3));
            }
        }
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.subtotal_receipt),
                Global.getCurrencyFormat(Global.getBigDecimalNum(splitedOrder.ord_subtotal).toString()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_discount_line_item),
                Global.getCurrencyFormat(Global.getBigDecimalNum(splitedOrder.ord_lineItemDiscount).toString()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_global_discount),
                Global.getCurrencyFormat(Global.getBigDecimalNum(splitedOrder.ord_discount).toString()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_tax),
                Global.getCurrencyFormat(Global.getBigDecimalNum(splitedOrder.ord_taxamount).toString()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_grandtotal),
                Global.getCurrencyFormat(Global.getBigDecimalNum(splitedOrder.gran_total).toString()), lineWidth, 0));
        sb.append(textHandler.newLines(2));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_tip), textHandler.newDivider('_', lineWidth / 2), lineWidth, 0));
        sb.append(textHandler.newLines(2));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total), textHandler.newDivider('_', lineWidth / 2), lineWidth, 0));
        sb.append(textHandler.newLines(2));
        print(sb.toString());
        printFooter(lineWidth);
        cutPaper();
    }

    public String getCustName(String custId) {
        String name = "";
        if (!TextUtils.isEmpty(custId)) {
            CustomersHandler handler = new CustomersHandler(activity);
            Customer customer = handler.getCustomer(custId);
            if (customer != null) {
                String displayName = myPref.getCustomerDisplayName();
                switch (displayName) {
                    case "cust_name":
                        name = customer.getCust_name();
                        break;
                    case "fullName":
                        name = String.format("%s %s", StringUtil.nullStringToEmpty(customer.getCust_firstName())
                                , StringUtil.nullStringToEmpty(customer.getCust_lastName()));
                        break;
                    case "CompanyName":
                        name = customer.getCompanyName();
                        break;
                    default:
                        name = customer.getCust_name();
                }
            }
        }
        return name;
    }

    public String getCustAccount(String custId) {
        String name = "";
        if (!TextUtils.isEmpty(custId)) {
            CustomersHandler handler = new CustomersHandler(activity);
            Customer customer = handler.getCustomer(custId);
            if (customer != null) {
                return customer.getCustAccountNumber();
            }
        }
        return "";
    }

    protected void printReceipt(String ordID, int lineWidth, boolean fromOnHold, Global.OrderType type, boolean isFromHistory, EMVContainer emvContainer) {
        OrdersHandler orderHandler = new OrdersHandler(activity);
        Order anOrder = orderHandler.getPrintedOrder(ordID);
        printReceipt(anOrder, lineWidth, fromOnHold, type, isFromHistory, emvContainer);
    }

    protected void printTicketReceipt(Order order, int lineWidth) {
        MemoTextHandler handler = new MemoTextHandler(activity);
        String[] header = handler.getHeader();
        AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
        startReceipt();
        setPaperWidth(lineWidth);
        printPref = myPref.getPrintingPreferences();
        List<OrderProduct> orderProducts = order.getOrderProducts();
        StringBuilder sb = new StringBuilder();
        String date = Global.formatToDisplayDate(order.ord_timecreated, 3);

        if (orderProducts != null) {
            for (OrderProduct orderProduct : orderProducts) {
                if (printPref.contains(MyPreferences.print_header)) {
                    for (String str : header) {
                        sb.append(textHandler.centeredString(str, lineWidth));
                    }
                    print(sb.toString(), 0, PrinterFunctions.Alignment.Center);
                    print(textHandler.newLines(1), 0, PrinterFunctions.Alignment.Left);
                }
                sb.setLength(0);
                sb.append(textHandler.centeredString(orderProduct.getOrdprod_name(), lineWidth / 2));
                sb.append(textHandler.centeredString(String.format("%s: %s", getString(R.string.fee), Global.getCurrencyFormat(orderProduct.getFinalPrice())), lineWidth / 2));
                sb.append(textHandler.newLines(1));
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(activity.getString(R.string.date), lineWidth / 2, 0));
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(date, lineWidth / 2, 0));
                sb.append(textHandler.newLines(1));
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(activity.getString(R.string.beach), lineWidth / 2, 0));
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(employee.getEmpName(), lineWidth / 2, 0));
                sb.append(textHandler.newLines(1));
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(activity.getString(R.string.license_num), lineWidth / 2, 0));
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(order.customer.getCust_name(), lineWidth / 2, 0));
                sb.append(textHandler.newLines(1));
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(activity.getString(R.string.permit), lineWidth / 2, 0));
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(orderProduct.getProd_id(), lineWidth / 2, 0));
                print(sb.toString(), 1, PrinterFunctions.Alignment.Left);
                sb.setLength(0);
                if (printPref.contains(MyPreferences.print_footer)) {
                    printFooter(lineWidth);
                }
                printTermsNConds();
                printEnablerWebSite(lineWidth);
                print(textHandler.newLines(2), 0, PrinterFunctions.Alignment.Left);
                cutPaper();
            }
        }


    }

    protected void printReceipt(Order anOrder, int lineWidth, boolean fromOnHold, Global.OrderType type, boolean isFromHistory, EMVContainer emvContainer) {

        try {
            if (myPref.isUsePermitReceipt()) {
                printTicketReceipt(anOrder, lineWidth);
                return;
            }
            AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
            Clerk clerk = ClerkDAO.getByEmpId(Integer.parseInt(myPref.getClerkID()));
            startReceipt();
            setPaperWidth(lineWidth);
            printPref = myPref.getPrintingPreferences();
            OrderProductsHandler orderProductsHandler = new OrderProductsHandler(activity);
            List<DataTaxes> listOrdTaxes = anOrder.getListOrderTaxes();
            List<OrderProduct> orderProducts = anOrder.getOrderProducts();

            boolean payWithLoyalty = false;
            StringBuilder sb = new StringBuilder();
            int size = orderProducts.size();
            printImage(0);
            if (printPref.contains(MyPreferences.print_header))
                printHeader(lineWidth);
            if (anOrder.isVoid.equals("1"))
                sb.append(textHandler.centeredString("*** VOID ***", lineWidth)).append("\n\n");
            print(sb.toString());
            sb.setLength(0);
            print(getOrderTypeDetails(fromOnHold, anOrder, lineWidth, type));

            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_date),
                    Global.formatToDisplayDate(anOrder.ord_timeStarted, 3), lineWidth, 0));

            if (ShiftDAO.isShiftOpen() && myPref.isUseClerks()) {
                String clerk_id = anOrder.clerk_id;
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_clerk),
                        clerk.getEmpName() + "(" + clerk_id + ")", lineWidth, 0));
            }
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_employee),
                    employee.getEmpName() + "(" + employee.getEmpId() + ")", lineWidth, 0));
            String custName = getCustName(anOrder.cust_id);
            if (custName != null && !custName.isEmpty()) {
                if (!TextUtils.isEmpty(custName)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_customer), custName,
                            lineWidth, 0));
                }
            }
            custName = getCustAccount(anOrder.cust_id);
            if (printPref.contains(MyPreferences.print_customer_id) && custName != null && !TextUtils.isEmpty(custName))
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_customer_id),
                        custName, lineWidth, 0));

            String ordComment = anOrder.ord_comment;
            if (!TextUtils.isEmpty(ordComment)) {
                sb.append("\n\n");
                sb.append("Comments:\n");
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(ordComment, lineWidth, 3)).append("\n");
            }

            print(sb.toString());

            sb.setLength(0);
            int totalItemstQty = 0;
            if (!myPref.getPreferences(MyPreferences.pref_wholesale_printout)) {
                boolean isRestMode = myPref.isRestaurantMode();

                for (int i = 0; i < size; i++) {
                    if (!TextUtils.isEmpty(orderProducts.get(i).getProd_price_points()) && Integer.parseInt(orderProducts.get(i).getProd_price_points()) > 0) {
                        payWithLoyalty = true;
                    }
                    totalItemstQty += TextUtils.isEmpty(orderProducts.get(i).getOrdprod_qty()) ? 0 : Double.parseDouble(orderProducts.get(i).getOrdprod_qty());
                    String uomDescription = "";
                    if (!TextUtils.isEmpty(orderProducts.get(i).getUom_name())) {
                        uomDescription = orderProducts.get(i).getUom_name() + "(" + orderProducts.get(i).getUom_conversion() + ")";
                    }
                    if (isRestMode) {
                        if (!orderProducts.get(i).isAddon()) {
                            sb.append(textHandler.oneColumnLineWithLeftAlignedText(
                                    orderProducts.get(i).getOrdprod_qty() + "x " + orderProducts.get(i).getOrdprod_name()
                                            + " "
                                            + uomDescription, lineWidth, 1));
                            if (orderProducts.get(i).getHasAddons()) {
                                List<OrderProduct> addons = orderProductsHandler.getOrderProductAddons(orderProducts.get(i).getOrdprod_id());
                                for (OrderProduct addon : addons) {
                                    if (addon.isAdded()) {
                                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                                                " >" + addon.getOrdprod_name(),
                                                Global.getCurrencyFormat(addon.getFinalPrice()), lineWidth, 2));
                                    } else {
                                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                                                " >NO " + addon.getOrdprod_name(),
                                                Global.getCurrencyFormat(addon.getFinalPrice()), lineWidth, 2));
                                    }
                                }
                            }

                            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_price),
                                    Global.getCurrencyFormat(String.valueOf(orderProducts.get(i).getItemTotalCalculated())), lineWidth, 3));
                            if (orderProducts.get(i).getDiscount_id() != null && !TextUtils.isEmpty(orderProducts.get(i).getDiscount_id())) {
                                ProductsHandler productDBHandler = new ProductsHandler(activity);
                                String discountName = productDBHandler.getDiscountName(orderProducts.get(i).getDiscount_id());
                                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_discount) + " " + discountName,
                                        Global.getCurrencyFormat(orderProducts.get(i).getDiscount_value()), lineWidth, 3));
                            }
                            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total),
                                    Global.getCurrencyFormat(orderProducts.get(i).getItemTotal()), lineWidth, 3));

                            List<OrderProduct> giftcardvalues = orderProductsHandler.getOrdProdGiftCardNumber(orderProducts.get(i).getOrdprod_id());
                            for (OrderProduct giftCard : giftcardvalues) {
                                sb.append(textHandler.twoColumnLineWithLeftAlignedText(giftCard.getGiftcardName() + ":", giftCard.getGiftcardNumber(), lineWidth, 3));
                            }

                            if (printPref.contains(MyPreferences.print_descriptions)) {
                                sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                                        getString(R.string.receipt_description), "", lineWidth, 3));
                                sb.append(textHandler.oneColumnLineWithLeftAlignedText(
                                        orderProducts.get(i).getOrdprod_desc(), lineWidth, 5));
                            }
                        }
                    } else {
                        sb.append(textHandler.oneColumnLineWithLeftAlignedText(
                                orderProducts.get(i).getOrdprod_qty() + "x " + orderProducts.get(i).getOrdprod_name()
                                        + " "
                                        + uomDescription, lineWidth, 1));
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_price),
                                Global.getCurrencyFormat(orderProducts.get(i).getFinalPrice()), lineWidth, 3));

                        if (orderProducts.get(i).getDiscount_id() != null && !TextUtils.isEmpty(orderProducts.get(i).getDiscount_id())) {
                            ProductsHandler productDBHandler = new ProductsHandler(activity);
                            String discountName = productDBHandler.getDiscountName(orderProducts.get(i).getDiscount_id());
                            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_discount) + " " + discountName,
                                    Global.getCurrencyFormat(orderProducts.get(i).getDiscount_value()), lineWidth, 3));
                        }

                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total),
                                Global.getCurrencyFormat(orderProducts.get(i).getItemTotal()), lineWidth, 3));


                        List<OrderProduct> giftcardvalues = orderProductsHandler.getOrdProdGiftCardNumber(orderProducts.get(i).getOrdprod_id());
                        for (OrderProduct giftCard : giftcardvalues) {
                            sb.append(textHandler.twoColumnLineWithLeftAlignedText(giftCard.getGiftcardName() + ":", giftCard.getGiftcardNumber(), lineWidth, 3));
                        }


                        if (printPref.contains(MyPreferences.print_descriptions)) {
                            sb.append(textHandler.twoColumnLineWithLeftAlignedText(
                                    getString(R.string.receipt_description), "", lineWidth, 3));
                            sb.append(textHandler.oneColumnLineWithLeftAlignedText(orderProducts.get(i).getOrdprod_desc(),
                                    lineWidth, 5));
                        }

                    }
                    print(sb.toString(), FORMAT);
                    sb.setLength(0);

                    if (this instanceof EMSStar && !isPOSPrinter && size > SIZE_LIMIT) {
                        // wait to fix printing incomplete issues on SM-T300i models.
                        Thread.sleep(SLEEP_TIME);
                    }
                }
            } else {
                int padding = lineWidth / 4;
                String tempor = Integer.toString(padding);
                StringBuilder tempSB = new StringBuilder();
                tempSB.append("%").append(tempor).append("s").append("%").append(tempor).append("s").append("%")
                        .append(tempor).append("s").append("%").append(tempor).append("s");

                sb.append(String.format(tempSB.toString(), "Item", "Qty", "Price", "Total")).append("\n");

                for (int i = 0; i < size; i++) {
                    if (!TextUtils.isEmpty(orderProducts.get(i).getProd_price_points()) && Integer.parseInt(orderProducts.get(i).getProd_price_points()) > 0) {
                        payWithLoyalty = true;
                    }
                    totalItemstQty += TextUtils.isEmpty(orderProducts.get(i).getOrdprod_qty()) ? 0
                            : Double.parseDouble(orderProducts.get(i).getOrdprod_qty());
                    sb.append(orderProducts.get(i).getOrdprod_name()).append("-").append(orderProducts.get(i).getOrdprod_desc())
                            .append("\n");

                    sb.append(String.format("Discount %s\n", Global.getCurrencyFormat(orderProducts.get(i).getDiscountTotal().toString())));
                    sb.append(String.format(tempSB.toString(), "   ", orderProducts.get(i).getOrdprod_qty(),
                            Global.getCurrencyFormat(orderProducts.get(i).getFinalPrice()),
                            Global.getCurrencyFormat(orderProducts.get(i).getItemTotal()))).append("\n");
                    print(sb.toString(), FORMAT);
                    sb.setLength(0);

                    if (this instanceof EMSStar && !isPOSPrinter && size > SIZE_LIMIT) {
                        // wait to fix printing incomplete issues on SM-T300i models.
                        Thread.sleep(SLEEP_TIME);
                    }
                }
            }
            print(sb.toString(), FORMAT);
            sb.setLength(0);

            print(textHandler.lines(lineWidth), FORMAT);
            addTotalLines(this.activity, anOrder, orderProducts, sb, lineWidth);

            addTaxesLine(listOrdTaxes, anOrder, lineWidth, sb);

            sb.append("\n");
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_itemsQtyTotal),
                    String.valueOf(totalItemstQty), lineWidth, 0));
            sb.append("\n");
            String granTotal = "0";
            if (!TextUtils.isEmpty(anOrder.gran_total)) {
                granTotal = anOrder.gran_total;
            } else if (!TextUtils.isEmpty(anOrder.ord_total)) {
                granTotal = anOrder.ord_total;
            }
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_grandtotal),
                    Global.getCurrencyFormat(granTotal), lineWidth, 0));
            sb.append("\n");
            print(sb.toString(), FORMAT);
            sb.setLength(0);

            PaymentsHandler payHandler = new PaymentsHandler(activity);
            List<PaymentDetails> detailsList = payHandler.getPaymentForPrintingTransactions(anOrder.ord_id);
            if (myPref.getPreferences(MyPreferences.pref_use_store_and_forward)) {
                detailsList.addAll(StoredPaymentsDAO.getPaymentForPrintingTransactions(anOrder.ord_id));
            }
            String receiptSignature;
            size = detailsList.size();

            double tempGrandTotal = Double.parseDouble(granTotal);
            double tempAmount = 0;
            if (size == 0) {
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_amountpaid),
                        Global.formatDoubleToCurrency(tempAmount), lineWidth, 0));

                if (type == Global.OrderType.INVOICE) // Invoice
                {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_balance_due),
                            Global.formatDoubleToCurrency(tempGrandTotal - tempAmount), lineWidth, 0));
                }
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total_tip_paid),
                        Global.formatDoubleToCurrency(0.00), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_amountreturned),
                        Global.formatDoubleToCurrency(0.00), lineWidth, 0));
            } else {

                double paidAmount = 0;
                double tempTipAmount = 0;
                double totalAmountTendered = 0;

                StringBuilder tempSB = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    String _pay_type = detailsList.get(i).getPaymethod_name().toUpperCase(Locale.getDefault()).trim();
                    tempAmount = tempAmount + formatStrToDouble(detailsList.get(i).getPay_amount());
                    if (Payment.PaymentType.getPaymentTypeByCode(detailsList.get(i).getPayType()) != Payment.PaymentType.VOID) {
                        paidAmount += formatStrToDouble(detailsList.get(i).getPay_amount());
                    }
                    totalAmountTendered += detailsList.get(i).getAmountTender();
                    tempTipAmount = tempTipAmount + formatStrToDouble(detailsList.get(i).getPay_tip());
                    tempSB.append(textHandler
                            .oneColumnLineWithLeftAlignedText(Global.getCurrencyFormat(detailsList.get(i).getPay_amount())
                                    + "[" + detailsList.get(i).getPaymethod_name() + "]", lineWidth, 1));
                    if (!_pay_type.equals("CASH") && !_pay_type.equals("CHECK")) {
                        tempSB.append(textHandler.oneColumnLineWithLeftAlignedText("TransID: " + StringUtil.nullStringToEmpty(detailsList.get(i).getPay_transid()),
                                lineWidth, 1));
                        tempSB.append(textHandler.oneColumnLineWithLeftAlignedText("CC#: *" + detailsList.get(i).getCcnum_last4(),
                                lineWidth, 1));
                    } else {
                        tempSB.append(textHandler
                                .oneColumnLineWithLeftAlignedText(getString(R.string.amount_tendered) + Global.formatDoubleToCurrency(detailsList.get(i).getAmountTender()), lineWidth, 1));
                        tempSB.append(textHandler
                                .oneColumnLineWithLeftAlignedText(getString(R.string.changeLbl) +
                                        Global.formatDoubleToCurrency(detailsList.get(i).getAmountTender() - formatStrToDouble(detailsList.get(i).getPay_amount())), lineWidth, 1));
                    }
                }
                if (type == Global.OrderType.ORDER) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_amountreturned),
                            Global.formatDoubleToCurrency(tempAmount), lineWidth, 0));
                } else {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_amountpaid),
                            Global.getCurrencyFormat(Double.toString(paidAmount)), lineWidth, 0));
                }
                sb.append(tempSB.toString());
                if (type == Global.OrderType.INVOICE) // Invoice
                {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_balance_due),
                            Global.formatDoubleToCurrency(tempGrandTotal - tempAmount), lineWidth, 0));
                }
                if (type != Global.OrderType.ORDER) {
                    if (myPref.isRestaurantMode() &&
                            myPref.getPreferences(MyPreferences.pref_enable_togo_eatin)) {
                        if (tempTipAmount == 0) {
                            sb.append("\n");
                            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total_tip_paid),
                                    textHandler.lines(lineWidth / 2), lineWidth, 0));
                            sb.append("\n");
                            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total),
                                    textHandler.lines(lineWidth / 2), lineWidth, 0));
                        } else {
                            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total_tip_paid),
                                    Global.getCurrencyFormat(Double.toString(tempTipAmount)), lineWidth, 0));
                        }

                    } else {
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total_tip_paid),
                                Global.getCurrencyFormat(Double.toString(tempTipAmount)), lineWidth, 0));
                    }

                    if (type == Global.OrderType.RETURN) {
                        tempAmount = paidAmount;
                    } else if (tempGrandTotal >= totalAmountTendered) {
                        tempAmount = 0.00;
                    } else {
                        if (tempGrandTotal > 0) {
                            tempAmount = totalAmountTendered - tempGrandTotal;
                        } else {
                            tempAmount = Math.abs(tempGrandTotal);
                        }
                    }
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_amountreturned),
                            Global.getCurrencyFormat(Double.toString(tempAmount)), lineWidth, 0))
                            .append("\n");
                }

            }
            print(sb.toString(), FORMAT);

            // Gratuities line
            if (myPref.isGratuitySelected() && myPref.getGratuityOne() != null
                    && myPref.getGratuityTwo() != null
                    && myPref.getGratuityThree() != null) {
                String title = getString(R.string.suggested_gratuity_title);
                String gratuityLine = anOrder.getGratuityLines(title,
                        myPref.getGratuityOne(),
                        myPref.getGratuityTwo(),
                        myPref.getGratuityThree(),
                        lineWidth);
                print(gratuityLine, FORMAT);
                sb.setLength(0);
            }
            // End of gratuity
            print(textHandler.newLines(1), FORMAT);
            if (type != Global.OrderType.ORDER && saveAmount > 0)
                printYouSave(String.valueOf(saveAmount), lineWidth);
            sb.setLength(0);
            if (Global.isIvuLoto && detailsList.size() > 0) {
                if (!printPref.contains(MyPreferences.print_ivuloto_qr)) {
                    printIVULoto(detailsList.get(0).getIvuLottoNumber(), lineWidth);
                } else {
                    printImage(2);
                    printIVULoto(detailsList.get(0).getIvuLottoNumber(), lineWidth);
                }
                sb.setLength(0);
            }
//            printOrderAttributes(lineWidth, anOrder);
            if (printPref.contains(MyPreferences.print_footer))
                printFooter(lineWidth);

            print(textHandler.newLines(1), FORMAT);
            if (payWithLoyalty && Global.loyaltyCardInfo != null && !TextUtils.isEmpty(Global.loyaltyCardInfo.getCardNumAESEncrypted())
                    && !TextUtils.isEmpty(Global.loyaltyCardInfo.getCardLast4())) {
                print(String.format("%s *%s\n", getString(R.string.receipt_cardnum), Global.loyaltyCardInfo.getCardLast4()));
                print(String.format("%s %s %s\n", getString(R.string.receipt_point_used), Global.loyaltyCharge, getString(R.string.points)));
                print(String.format("%s %s %s\n", getString(R.string.receipt_reward_balance), Global.loyaltyPointsAvailable, getString(R.string.points)));
                print(textHandler.newLines(1), FORMAT);
            }
            if (Global.rewardCardInfo != null && !TextUtils.isEmpty(Global.rewardCardInfo.getCardNumAESEncrypted())
                    && !TextUtils.isEmpty(Global.rewardCardInfo.getCardLast4())) {
                print(String.format("%s *%s\n", getString(R.string.receipt_cardnum), Global.rewardCardInfo.getCardLast4()));
                print(String.format("%s %s %s\n", getString(R.string.receipt_reward_balance), Global.rewardCardInfo.getOriginalTotalAmount(), getString(R.string.points)));
                print(textHandler.newLines(1), FORMAT);
            }
            receiptSignature = anOrder.ord_signature;
            if (!TextUtils.isEmpty(receiptSignature)) {
                encodedSignature = receiptSignature;
                printImage(1);
                sb.setLength(0);
                sb.append("x").append(textHandler.lines(lineWidth / 2)).append("\n");
                sb.append(getString(R.string.receipt_signature)).append(textHandler.newLines(1));
                print(sb.toString(), FORMAT);

            }

            if (isFromHistory) {
                sb.setLength(0);
                sb.append(textHandler.centeredString("*** Copy ***", lineWidth));
                print(sb.toString());
                print(textHandler.newLines(1));
            }
            printTermsNConds();
            printEnablerWebSite(lineWidth);
            cutPaper();
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }

    }

    public String getOrderTypeDetails(boolean fromOnHold, Order anOrder, int lineWidth, Global.OrderType type) {
        StringBuilder sb = new StringBuilder();
        if (fromOnHold) {
            sb.append(textHandler.twoColumnLineWithLeftAlignedText("[" + getString(R.string.on_hold) + "]",
                    anOrder.ord_HoldName, lineWidth, 0));
        }

        switch (type) {
            case ORDER: // Order
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.order) + ":", anOrder.ord_id,
                        lineWidth, 0));
                break;
            case RETURN: // Return
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.return_tag) + ":", anOrder.ord_id,
                        lineWidth, 0));
                break;
            case INVOICE: // Invoice
            case CONSIGNMENT_INVOICE:// Consignment Invoice
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.invoice) + ":", anOrder.ord_id,
                        lineWidth, 0));
                break;
            case ESTIMATE: // Estimate
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.estimate) + ":", anOrder.ord_id,
                        lineWidth, 0));
                break;
            case SALES_RECEIPT: // Sales Receipt
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.sales_receipt) + ":", anOrder.ord_id,
                        lineWidth, 0));
                break;
        }
        return sb.toString();
    }

    private void printOrderAttributes(int lineWidth, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        for (OrderAttributes attr : order.orderAttributes) {
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(attr.getOrdAttrName(), attr.getInputValue(), lineWidth, 0));
        }
        print(sb.toString());
    }

    private void printIVULoto(String ivuLottoNumber, int lineWidth) {
        print((textHandler.ivuLines(2 * lineWidth / 3) + "\n" + activity.getString(R.string.ivuloto_control_label) + ivuLottoNumber + "\n" + getString(R.string.enabler_prefix) + "\n" + getString(R.string.powered_by_enabler) + "\n" + textHandler.ivuLines(2 * lineWidth / 3)).getBytes());
    }

    private void printEnablerWebSite(int lineWidth) {
        if (myPref.isPrintWebSiteFooterEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.setLength(0);
            sb.append(textHandler.centeredString(getString(R.string.enabler_website) + "\n\n\n\n", lineWidth));
            print(sb.toString());
        }
        apt50PrintPaper();
    }

    public void cutPaper() {
        if (PRINT_TO_LOG) {
            Log.d("Cut", "Paper Cut");
            return;
        }

        if (this instanceof EMSPaxA920) {
            print(textHandler.newLines(4));
        }

        if (this instanceof EMSBixolonRD) {
            SendCmd(String.format("81*%s", " "));
        } else if (this instanceof EMSHPEngageOnePrimePrinter) {
            try {
                hpPrinter.open("HPEngageOnePrimePrinter");
                hpPrinter.claim(10000);
                hpPrinter.setDeviceEnabled(true);
                hpPrinter.cutPaper(100);
            } catch (JposException e) {
                e.printStackTrace();
            } finally {
                try {
                    hpPrinter.setDeviceEnabled(false);
                    hpPrinter.close();
                } catch (JposException e) {
                    e.printStackTrace();
                }
            }
        } else if (this instanceof EMSsnbc) {
            // ******************************************************************************************
            // print in page mode
            pos_sdk.pageModePrint();
            pos_sdk.systemCutPaper(66, 0);

            // *****************************************************************************************
            // clear buffer in page mode
            pos_sdk.pageModeClearBuffer();
        } else if (isPOSPrinter) {
            ICommandBuilder builder = StarIoExt.createCommandBuilder(emulation);
            builder.beginDocument();
            builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);
            builder.endDocument();
            byte[] cmds = builder.getCommands();
            try {
                port.writePort(cmds, 0, cmds.length);
            } catch (StarIOPortException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
//            print(new byte[]{0x1b, 0x64, 0x02}); // Cut
        } else if (this instanceof EMSmePOS || this instanceof EMSGPrinterPT380) {
            finishReceipt();
        }
    }

    /**
     * METHODS FOR INSTANCES OF HPENGAGEONEPRIMEPRINTER or BIXOLON
     * storeImage() -> Copies logo.png from data/data/com.emobilepos.app/files/logo.png
     * to a public directory called eMobileAssets.
     *
     * @param imageData Bitmap you want to save
     * @param fname     Name that will be assigned to image. Include the format of image(image.png, image.jpeg, image.bmp, etc...)
     */
    public void storeImage(Bitmap imageData, String fname) {
        String Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/eMobileAssets/";
        File dir = new File(Path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            String filePath = dir.getAbsolutePath() + "/" + fname;
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);

            imageData.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            Log.w("TAG", "Error saving image file: " + e.getMessage());
        } catch (IOException e) {
            Log.w("TAG", "Error saving image file: " + e.getMessage());
        }
    }

    /**
     * changeImageHeaders()
     *
     * @param orgBitmap Bitmap to have its header information changed
     * @param filePath  Directory of the bitmap
     *                  <p>
     *                  writeInt() and writeShort() methods belong and are only used in changeImageHeaders()
     */
    public static boolean changeImageHeaders(Bitmap orgBitmap, String filePath) throws IOException {
        long start = System.currentTimeMillis();
        if (orgBitmap == null) {
            return false;
        }

        if (filePath == null) {
            return false;
        }

        boolean isSaveSuccess = true;

        //image size
        int width = orgBitmap.getWidth();
        int height = orgBitmap.getHeight();

        //image dummy data size
        //reason : the amount of bytes per image row must be a multiple of 4 (requirements of bmp format)
        byte[] dummyBytesPerRow = null;
        boolean hasDummy = false;
        int rowWidthInBytes = BYTE_PER_PIXEL * width; //source image width * number of bytes to encode one pixel.
        if (rowWidthInBytes % BMP_WIDTH_OF_TIMES > 0) {
            hasDummy = true;
            //the number of dummy bytes we need to add on each row
            dummyBytesPerRow = new byte[(BMP_WIDTH_OF_TIMES - (rowWidthInBytes % BMP_WIDTH_OF_TIMES))];
            //just fill an array with the dummy bytes we need to append at the end of each row
            for (int i = 0; i < dummyBytesPerRow.length; i++) {
                dummyBytesPerRow[i] = (byte) 0xFF;
            }
        }

        //an array to receive the pixels from the source image
        int[] pixels = new int[width * height];

        //the number of bytes used in the file to store raw image data (excluding file headers)
        int imageSize = (rowWidthInBytes + (hasDummy ? dummyBytesPerRow.length : 0)) * height;
        //file headers size
        int imageDataOffset = 0x36;

        //final size of the file
        int fileSize = imageSize + imageDataOffset;

        //Android Bitmap Image Data
        orgBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);

        /**
         * BITMAP FILE HEADER Write Start
         **/
        buffer.put((byte) 0x42);
        buffer.put((byte) 0x4D);

        //size
        buffer.put(writeInt(fileSize));

        //reserved
        buffer.put(writeShort((short) 0));
        buffer.put(writeShort((short) 0));

        //image data start offset
        buffer.put(writeInt(imageDataOffset));

        /** BITMAP FILE HEADER Write End */

        //*******************************************

        /** BITMAP INFO HEADER Write Start */
        //size
        buffer.put(writeInt(0x28));

        //width, height
        //if we add 3 dummy bytes per row : it means we add a pixel (and the image width is modified.
        buffer.put(writeInt(width + (hasDummy ? (dummyBytesPerRow.length == 3 ? 1 : 0) : 0)));
        buffer.put(writeInt(height));

        //planes
        buffer.put(writeShort((short) 1));

        //bit count
        buffer.put(writeShort((short) 24));

        //bit compression
        buffer.put(writeInt(0));

        //image data size
        buffer.put(writeInt(imageSize));

        //horizontal resolution in pixels per meter
        buffer.put(writeInt(0));

        //vertical resolution in pixels per meter (unreliable)
        buffer.put(writeInt(0));

        buffer.put(writeInt(0));

        buffer.put(writeInt(0));

        /** BITMAP INFO HEADER Write End */

        int row = height;
        int col = width;
        int startPosition = (row - 1) * col;
        int endPosition = row * col;
        while (row > 0) {
            for (int i = startPosition; i < endPosition; i++) {
                buffer.put((byte) (pixels[i] & 0x000000FF));
                buffer.put((byte) ((pixels[i] & 0x0000FF00) >> 8));
                buffer.put((byte) ((pixels[i] & 0x00FF0000) >> 16));
            }
            if (hasDummy) {
                buffer.put(dummyBytesPerRow);
            }
            row--;
            endPosition = startPosition;
            startPosition = startPosition - col;
        }

        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(buffer.array());
        fos.close();
        Log.v("AndroidBmpUtil", System.currentTimeMillis() - start + " ms");

        return isSaveSuccess;
    }

    private static byte[] writeInt(int value) throws IOException {
        byte[] b = new byte[4];

        b[0] = (byte) (value & 0x000000FF);
        b[1] = (byte) ((value & 0x0000FF00) >> 8);
        b[2] = (byte) ((value & 0x00FF0000) >> 16);
        b[3] = (byte) ((value & 0xFF000000) >> 24);

        return b;
    }

    private static byte[] writeShort(short value) throws IOException {
        byte[] b = new byte[2];

        b[0] = (byte) (value & 0x00FF);
        b[1] = (byte) ((value & 0xFF00) >> 8);

        return b;
    }

    /**
     * RescaleBitmap()
     *
     * @param bmp Bitmap to be rescaled and fit a paper width of 300
     */
    private Bitmap rescaleBitmap(Bitmap bmp) {
        if (bmp.getWidth() > 300) {
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            float scale = 0;
            scale = (float) 300 / width;
            width = (int) (width * scale);
            height = (int) (height * scale);
            return Bitmap.createScaledBitmap(bmp, width, height, false);
        }
        return bmp;
    }

    /**
     * processImageBitmap()
     *
     * @param type       Type of image to be processed(logo, signature or QRCode)
     * @param myBitmap   Bitmap to be stored and processed
     * @param bitmapPath Full path directory where bitmap is stored -Should be in the eMobileAssets Folder on the Public directory-
     */
    private void processImageBitmap(int type, Bitmap myBitmap, String bitmapPath) {
        String imageName = "";
        switch (type) {
            case 0:
                imageName = "logo.bmp";
                break;
            case 1:
                imageName = "signature.bmp";
                break;
            case 2:
                imageName = "qrCode.bmp";
                break;
        }
        storeImage(myBitmap, imageName);
        Bitmap bmp = BitmapFactory.decodeFile(bitmapPath);
        Bitmap newBitmap = rescaleBitmap(bmp);
        storeImage(newBitmap, imageName);
        try {
            changeImageHeaders(newBitmap, bitmapPath);
        } catch (IOException e) {
            Log.e("HPPrinter", "Could not change image headers");
            e.printStackTrace();
        }
    }

    /**
     * ------------------------END OF METHODS FOR INSTANCES--------------------------
     */
    protected void printImage(int type) {
        String bitmapPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/eMobileAssets";

        if (PRINT_TO_LOG) {
            Log.d("Print", "*******Image Print***********");
            return;
        }
        Bitmap myBitmap = null;
        switch (type) {
            case 0: // Logo
            {
                File imgFile = new File(myPref.getAccountLogoPath());
                if (imgFile.exists()) {
                    myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    if (this instanceof EMSHPEngageOnePrimePrinter) {
                        bitmapPath = bitmapPath + "/logo.bmp";
                        processImageBitmap(type, myBitmap, bitmapPath);
                    }
                }
                break;
            }
            case 1: // signature
            {
                if (!TextUtils.isEmpty(encodedSignature)) {
                    byte[] img = Base64.decode(encodedSignature, Base64.DEFAULT);
                    myBitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                    if (this instanceof EMSHPEngageOnePrimePrinter) {
                        bitmapPath = bitmapPath + "/signature.bmp";
                        processImageBitmap(type, myBitmap, bitmapPath);
                    }
                }
                break;
            }
            case 2: {
                if (!TextUtils.isEmpty(encodedQRCode)) {
                    byte[] img = Base64.decode(encodedQRCode, Base64.DEFAULT);
                    myBitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                    if (this instanceof EMSHPEngageOnePrimePrinter) {
                        bitmapPath = bitmapPath + "/qrCode.bmp";
                        processImageBitmap(type, myBitmap, bitmapPath);
                    }
                }
                break;
            }
        }

        if (myBitmap != null) {
            if (this instanceof EMSStar) {
                byte[] data;
                StarIoExt.Emulation emu = emulation;
                if (!isPOSPrinter) {
                    emu = StarIoExt.Emulation.EscPosMobile;
                }
                try {
                    data = PrinterFunctions.createCommandsEnglishRasterModeCoupon(
                            myBitmap, emu, PAPER_WIDTH);
                    port.writePort(data, 0, data.length);
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
            } else if (this instanceof EMSAPT50) {
                try {
                    int[] status = new int[1];
                    int ret = aptPrinter.getPrinterStatus(status);
                    if (ret == 0) {
                        aptPrinter.printInit();
                        aptPrinter.clearPrintDataCache();
                        aptPrinter.printImageBase(rescaleBitmap(myBitmap),
                                myBitmap.getWidth(),
                                myBitmap.getHeight(),
                                wangpos.sdk4.libbasebinder.Printer.Align.CENTER,
                                0);
                        aptPrinter.printFinish();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            } else if (this instanceof EMSBlueBambooP25) {
                EMSBambooImageLoader loader = new EMSBambooImageLoader();
                ArrayList<ArrayList<Byte>> arrayListList = loader.bambooDataWithAlignment(0, myBitmap);
                for (ArrayList<Byte> arrayList : arrayListList) {
                    byte[] byteArray = new byte[arrayList.size()];
                    int size = arrayList.size();
                    for (int i = 0; i < size; i++) {
                        byteArray[i] = arrayList.get(i);
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    print(byteArray);
                }
            } else if (this instanceof EMSOneil4te) {
                // print image
                DocumentLP documentLP = new DocumentLP("$");
                if (type == 1) {
                    Bitmap bmp = myBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    int w = bmp.getWidth();
                    int h = bmp.getHeight();
                    int pixel;
                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < h; y++) {
                            pixel = bmp.getPixel(x, y);
                            if (pixel == Color.TRANSPARENT)
                                bmp.setPixel(x, y, Color.WHITE);
                        }
                    }
                    documentLP.clear();
                    documentLP.writeImage(bmp, 832);
                    device.write(documentLP.getDocumentData());
                } else {
                    documentLP.clear();
                    documentLP.writeImage(myBitmap, 832);
                    device.write(documentLP.getDocumentData());
                }
            } else if (this instanceof EMSPowaPOS) {
                powaPOS.printImage(myBitmap);
            } else if (this instanceof EMSmePOS) {
                float ratio = Integer.valueOf(PAPER_WIDTH).floatValue() / Integer.valueOf(myBitmap.getWidth()).floatValue();
                int width = Math.round(ratio * myBitmap.getWidth());
                int height = Math.round(ratio * myBitmap.getHeight());
                Bitmap bmpMonochrome = Bitmap.createBitmap(width / 2, height / 2, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmpMonochrome);
                ColorMatrix ma = new ColorMatrix();
                ma.setSaturation(0);
                Paint paint = new Paint();
                paint.setColorFilter(new ColorMatrixColorFilter(ma));
                canvas.drawBitmap(myBitmap, 0, 0, paint);
                mePOSReceipt.addLine(new MePOSReceiptImageLine(bmpMonochrome));
            } else if (this instanceof EMSsnbc) {
                int PrinterWidth = 640;
                pos_sdk.textStandardModeAlignment(ALIGN_CENTER);
                pos_sdk.imageStandardModeRasterPrint(myBitmap, PrinterWidth);
                pos_sdk.textStandardModeAlignment(ALIGN_LEFT);
            } else if (this instanceof EMSELO) {
                if (eloPrinterRefresh == null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    matrix.preScale(1.0f, -1.0f);
                    Bitmap rotatedBmp = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true);
                    eloPrinterApi.print_image(activity, rotatedBmp);
                }
            } else if (this instanceof EMSGPrinterPT380) {
                // print logo
                esc = new EscCommand();
                esc.addInitializePrinter();
                esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                esc.addRastBitImage(myBitmap, 384, 0);
                esc.addPrintAndFeedLines((byte) 2);
                esc.addQueryPrinterStatus();
                printGPrinter(esc);
            } else if (this instanceof EMSBixolon) {
//                ByteBuffer buffer = ByteBuffer.allocate(4);
//                buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
//                buffer.put((byte) 50);
//                buffer.put((byte) 0x00);
//                buffer.put((byte) 0x00);
                try {
                    bixolonPrinter.open(myPref.getPrinterName());
                    bixolonPrinter.claim(10000);
                    bixolonPrinter.setDeviceEnabled(true);
//                    bixolonPrinter.printBitmap(buffer.getInt(0), myBitmap,
//                            bixolonPrinter.getRecLineWidth(), POSPrinterConst.PTR_BM_LEFT);
                    bixolonPrinter.printBitmap(PTR_S_RECEIPT,
                            bitmapPath,
                            PTR_BM_ASIS,
                            PTR_BM_CENTER);
                } catch (JposException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bixolonPrinter.close();
                    } catch (JposException e) {
                        e.printStackTrace();
                    }
                }
            } else if (this instanceof EMSHPEngageOnePrimePrinter) {
                try {
                    hpPrinter.open("HPEngageOnePrimePrinter");
                    hpPrinter.claim(10000);
                    hpPrinter.setDeviceEnabled(true);
                    hpPrinter.printBitmap(PTR_S_RECEIPT,
                            bitmapPath,
                            PTR_BM_ASIS,
                            PTR_BM_CENTER);
                } catch (JposException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        hpPrinter.close();
                    } catch (JposException e) {
                        e.printStackTrace();
                    }
                }
            } else if (this instanceof EMSPaxA920) {
                try {
                    posLinkPrint(myBitmap);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }

    }

    protected void printImage(Bitmap bitmap) {
        if (PRINT_TO_LOG) {
            Log.d("Print", "*******Image Print***********");
            return;
        }
        if (bitmap != null) {

            if (this instanceof EMSStar) {
                PrinterSetting setting = new PrinterSetting(activity);
                StarIoExt.Emulation emulation = setting.getEmulation();
                byte[] data;

                if (!isPOSPrinter) {
                    emulation = StarIoExt.Emulation.EscPosMobile;
                }
                try {
                    data = PrinterFunctions.createCommandsEnglishRasterModeCoupon(
                            bitmap, emulation, PAPER_WIDTH);
                    port.writePort(data, 0, data.length);
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
            } else if (this instanceof EMSAPT50) {
                try {
                    int[] status = new int[1];
                    int ret = aptPrinter.getPrinterStatus(status);
                    if (ret == 0) {
                        aptPrinter.printInit();
                        aptPrinter.clearPrintDataCache();
                        aptPrinter.printImageBase(rescaleBitmap(bitmap),
                                bitmap.getWidth(),
                                bitmap.getHeight(),
                                wangpos.sdk4.libbasebinder.Printer.Align.CENTER,
                                0);
                        aptPrinter.printFinish();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            } else if (this instanceof EMSmePOS) {
                mePOSReceipt.addLine(new MePOSReceiptImageLine(bitmap));
            } else if (this instanceof EMSBlueBambooP25) {
                EMSBambooImageLoader loader = new EMSBambooImageLoader();
                ArrayList<ArrayList<Byte>> arrayListList = loader.bambooDataWithAlignment(0, bitmap);
                for (ArrayList<Byte> arrayList : arrayListList) {
                    byte[] byteArray = new byte[arrayList.size()];
                    int size = arrayList.size();
                    for (int i = 0; i < size; i++) {
                        byteArray[i] = arrayList.get(i);
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    print(byteArray);
                }
            } else if (this instanceof EMSOneil4te) {
                // print image
                DocumentLP documentLP = new DocumentLP("$");
                documentLP.clear();
                documentLP.writeImage(bitmap, 832);
                device.write(documentLP.getDocumentData());
            } else if (this instanceof EMSPowaPOS) {
                powaPOS.printImage(bitmap);
            } else if (this instanceof EMSsnbc) {
                int PrinterWidth = 640;
                // download bitmap
                pos_sdk.textStandardModeAlignment(ALIGN_CENTER);
                pos_sdk.imageStandardModeRasterPrint(bitmap, PrinterWidth);
                pos_sdk.textStandardModeAlignment(ALIGN_LEFT);
            } else if (this instanceof EMSELO) {
                if (eloPrinterRefresh == null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    matrix.preScale(1.0f, -1.0f);
                    Bitmap rotatedBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    eloPrinterApi.print_image(activity, rotatedBmp);
                }
                print("\n\n\n\n");

            } else if (this instanceof EMSBixolon) {
//                ByteBuffer buffer = ByteBuffer.allocate(4);
//                buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
//                buffer.put((byte) 50);
//                buffer.put((byte) 1);
//                buffer.put((byte) 0x00);
                try {
                    String bitmapPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/eMobileAssets/logo.bmp";
                    processImageBitmap(0, bitmap, bitmapPath);
                    bixolonPrinter.open(myPref.getPrinterName());
                    bixolonPrinter.claim(10000);
                    bixolonPrinter.setDeviceEnabled(true);
//                    bixolonPrinter.printBitmap(buffer.getInt(0), bitmap,
//                            bixolonPrinter.getRecLineWidth(), POSPrinterConst.PTR_BM_CENTER);
                    bixolonPrinter.printBitmap(PTR_S_RECEIPT,
                            bitmapPath,
                            PTR_BM_ASIS,
                            PTR_BM_CENTER);
                } catch (JposException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bixolonPrinter.close();
                    } catch (JposException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        matrix.preScale(1.0f, -1.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public void printHeader(int lineWidth) {

        EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
        StringBuilder sb = new StringBuilder();

        MemoTextHandler handler = new MemoTextHandler(activity);
        String[] header = handler.getHeader();

        if (!TextUtils.isEmpty(header[0]))
            sb.append(textHandler.centeredString(header[0], lineWidth));
        if (!TextUtils.isEmpty(header[1]))
            sb.append(textHandler.centeredString(header[1], lineWidth));
        if (!TextUtils.isEmpty(header[2]))
            sb.append(textHandler.centeredString(header[2], lineWidth));

        if (!TextUtils.isEmpty(sb.toString())) {
            sb.insert(0, textHandler.newLines(1));
            sb.append(textHandler.newLines(1));
            print(sb.toString(), 0, PrinterFunctions.Alignment.Left);
        }

    }

    private void printYouSave(String saveAmount, int lineWidth) {
        EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
        StringBuilder sb = new StringBuilder(saveAmount);

        print(textHandler.ivuLines(lineWidth), FORMAT);
        sb.setLength(0);
        sb.append(textHandler.newLines(1));

        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_youSave),
                Global.getCurrencyFormat(saveAmount), lineWidth, 0));

        sb.append(textHandler.newLines(1));
        print(sb.toString());
        print(textHandler.ivuLines(lineWidth), FORMAT);

    }

    public void printTermsNConds() {
        printPref = myPref.getPrintingPreferences();
        if (printPref.contains(MyPreferences.print_terms_conditions)) {
            List<TermsNConditions> termsNConditions = TermsNConditionsDAO.getTermsNConds();
            if (termsNConditions != null) {
                for (TermsNConditions terms : termsNConditions) {
                    print(terms.getTcTerm());
                }
                print("\n");
            }
        }
    }

    public void printClockInOut(List<ClockInOut> timeClocks, int lineWidth, String clerkID) {
        startReceipt();
        EMSPlainTextHelper textHelper = new EMSPlainTextHelper();
        Clerk clerk = ClerkDAO.getByEmpId(Integer.parseInt(clerkID));
        StringBuilder str = new StringBuilder();
        str.append(textHelper.centeredString(activity.getString(R.string.clockReceipt), lineWidth));
        str.append(textHelper.newLines(1));
        str.append(textHelper.twoColumnLineWithLeftAlignedText(
                DateUtils.getDateAsString(new Date(), DateUtils.DATE_yyyy_MM_dd_h_mm_a),
                String.format("%s%s", activity.getString(R.string.receipt_employee),
                        clerk.getEmpName()), lineWidth, 0));
        str.append(textHelper.newDivider('-', lineWidth));
        str.append(textHelper.fourColumnLineWithLeftAlignedTextPercentWidth(
                activity.getString(R.string.date), 25,
                activity.getString(R.string.clock_in), 25,
                activity.getString(R.string.clock_out), 25,
                activity.getString(R.string.hours), 25,
                lineWidth, 0));

        double hours;
        double totalHours = 0;

        for (ClockInOut clock : timeClocks) {
            Date in = DateUtils.getDateStringAsDate(clock.getClockIn(), DateUtils.DATE_PATTERN);
            Date out = null;
            hours = 0;

            if (clock.getClockOut() != null) {
                out = DateUtils.getDateStringAsDate(clock.getClockOut(), DateUtils.DATE_PATTERN);
                hours = DateUtils.computeDiffInHours(in, out);
                totalHours += hours;
            }

            str.append(textHelper.fourColumnLineWithLeftAlignedTextPercentWidth(
                    DateUtils.getDateAsString(in, DateUtils.DATE_MM_DD), 25,
                    DateUtils.getDateAsString(in, DateUtils.DATE_h_mm_a), 25,
                    DateUtils.getDateAsString(out, DateUtils.DATE_h_mm_a), 25,
                    String.format(Locale.getDefault(), "%.2f", hours), 25,
                    lineWidth, 0));
        }
        str.append(textHelper.newLines(1));
        str.append(textHelper.centeredString(String.format(Locale.getDefault(), "%s  %.2f",
                activity.getString(R.string.total_hours_worked), totalHours), lineWidth));
        str.append(textHelper.newLines(4));
        print(str.toString());
        cutPaper();
    }

    public void printFooter(int lineWidth) {

        EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
        StringBuilder sb = new StringBuilder();
        MemoTextHandler handler = new MemoTextHandler(activity);
        String[] footer = handler.getFooter();

        if (!TextUtils.isEmpty(footer[0]))
            sb.append(textHandler.centeredString(footer[0], lineWidth));
        if (!TextUtils.isEmpty(footer[1]))
            sb.append(textHandler.centeredString(footer[1], lineWidth));
        if (!TextUtils.isEmpty(footer[2]))
            sb.append(textHandler.centeredString(footer[2], lineWidth));

        if (!TextUtils.isEmpty(sb.toString())) {
            sb.append(textHandler.newLines(1));
            print(sb.toString());

        }
    }

    private double formatStrToDouble(String val) {
        if (TextUtils.isEmpty(val))
            return 0.00;
        return Double.parseDouble(val);
    }

    protected String getString(int id) {
        return (activity.getResources().getString(id));
    }

    protected boolean printBalanceInquiry(HashMap<String, String> values, int lineWidth) {
        try {
            startReceipt();
            printPref = myPref.getPrintingPreferences();
            StringBuilder sb = new StringBuilder();
            printImage(0);
            if (myPref.isCustSelected()) {
                sb.append(textHandler.centeredString(myPref.getCustName(), lineWidth));
            }
            if (printPref.contains(MyPreferences.print_header))
                printHeader(lineWidth);
            if (values.containsKey("amountAdded")) {
                sb.append("* ").append(getString(R.string.add_balance));
            } else {
                sb.append("* ").append(getString(R.string.balance_inquiry));
            }
            sb.append(" *\n");
            print(textHandler.centeredString(sb.toString(), lineWidth), FORMAT);
            sb.setLength(0);
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_date),
                    getString(R.string.receipt_time), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(DateUtils.getDateAsString(new Date(), "MMM/dd/yyyy"), DateUtils.getDateAsString(new Date(), "hh:mm:ss"), lineWidth, 0))
                    .append("\n");

            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.card_number),
                    "*" + values.get("pay_maccount"), lineWidth, 0));
            if (values.containsKey("amountAdded")) {
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.status),
                        values.get("epayStatusCode"), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.amount_added),
                        values.get("amountAdded"), lineWidth, 0));
            }

            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.balanceAmount),
                    Global.getCurrencyFormat(values.get("CardBalance")), lineWidth, 0));

            print(sb.toString());

            sb.setLength(0);
            printFooter(lineWidth);
            sb.append("\n");
            print(sb.toString(), FORMAT);
            sb.setLength(0);
            printEnablerWebSite(lineWidth);
            cutPaper();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    void printPaymentDetailsReceipt(String payID, int type, boolean isReprint, int lineWidth, EMVContainer emvContainer) {
        try {
            startReceipt();
            EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
            printPref = myPref.getPrintingPreferences();
            PaymentsHandler payHandler = new PaymentsHandler(activity);
            Spanned fromHtml = null;
            if (emvContainer != null && emvContainer.getHandpointResponse() != null && emvContainer.getHandpointResponse().getCustomerReceipt() != null) {
                fromHtml = Html.fromHtml(emvContainer.getHandpointResponse().getCustomerReceipt());
            }
            PaymentDetails payArray;
            boolean isStoredFwd = false;
            long pay_count = payHandler.paymentExist(payID, true);
            if (pay_count == 0) {
                isStoredFwd = true;
                if (emvContainer != null && emvContainer.getGeniusResponse() != null && emvContainer.getGeniusResponse().getStatus().equalsIgnoreCase("DECLINED")) {
                    type = 2;
                }
                payArray = StoredPaymentsDAO.getPrintingForPaymentDetails(payID, type);
            } else {
                payArray = payHandler.getPrintingForPaymentDetails(payID, type);
            }
            StringBuilder sb = new StringBuilder();
            boolean isCashPayment = false;
            boolean isCheckPayment = false;
            String includedTip = null;
            String creditCardFooting = "";
            if (payArray.getPaymethod_name() != null && payArray.getPaymethod_name().toUpperCase(Locale.getDefault()).trim().equals("CASH"))
                isCashPayment = true;
            else if (payArray.getPaymethod_name() != null && payArray.getPaymethod_name().toUpperCase(Locale.getDefault()).trim().equals("CHECK"))
                isCheckPayment = true;
            else {
                includedTip = getString(R.string.receipt_included_tip);
                creditCardFooting = getString(R.string.receipt_creditcard_terms);
            }
            printImage(0);

            if (fromHtml == null) {
                if (printPref.contains(MyPreferences.print_header))
                    printHeader(lineWidth);
                sb.append("* ").append(payArray.getPaymethod_name());
                if (payArray.getIs_refund() != null && payArray.getIs_refund().equals("1"))
                    sb.append(" Refund *\n");
                else
                    sb.append(" Sale *\n");
                print(textHandler.centeredString(sb.toString(), lineWidth), FORMAT);

                sb.setLength(0);
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_date),
                        getString(R.string.receipt_time), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(payArray.getPay_date(), payArray.getPay_timecreated(), lineWidth, 0))
                        .append("\n");

                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_customer), getCustName(payArray.getCustomerId()),
                        lineWidth, 0));

                if (payArray.getJob_id() != null && !TextUtils.isEmpty(payArray.getJob_id()))
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_order_id),
                            payArray.getJob_id(), lineWidth, 0));
                else if (payArray.getInv_id() != null && !TextUtils.isEmpty(payArray.getInv_id())) // invoice
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_invoice_ref),
                            payArray.getInv_id(), lineWidth, 0));

                if (!isStoredFwd)
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_idnum), payID,
                            lineWidth, 0));

                if (!isCashPayment && !isCheckPayment) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_cardnum),
                            "*" + payArray.getCcnum_last4(), lineWidth, 0));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("TransID:", payArray.getPay_transid(), lineWidth, 0)).append("\n");
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Auth Code:", payArray.getAuthcode(), lineWidth, 0)).append("\n");
                } else if (isCheckPayment) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_checknum),
                            payArray.getPay_check(), lineWidth, 0));
                }
                print(sb.toString());
                sb.setLength(0);
                printEMVSection(payArray.getEmvContainer(), lineWidth);
                String status = payArray.getEmvContainer() != null && payArray.getEmvContainer().getGeniusResponse() != null ? payArray.getEmvContainer().getGeniusResponse().getStatus() : getString(R.string.approved);
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.credit_approval_status),
                        status, lineWidth, 0));
                sb.append(textHandler.newLines(1));
                if (Global.isIvuLoto && Global.subtotalAmount > 0 && !TextUtils.isEmpty(payArray.getTax1_amount())
                        && !TextUtils.isEmpty(payArray.getTax2_amount())) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_subtotal),
                            Global.getCurrencyFormat(String.valueOf(Global.subtotalAmount)), lineWidth, 0));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(payArray.getTax1_name(),
                            Global.getCurrencyFormat(payArray.getTax1_amount()), lineWidth, 2));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(payArray.getTax2_name(),
                            Global.getCurrencyFormat(payArray.getTax2_amount()), lineWidth, 2));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(payArray.getTax3_name(),
                            Global.getCurrencyFormat(payArray.getTax3_amount()), lineWidth, 2));
                }

                if (emvContainer != null && emvContainer.getGeniusResponse() != null && emvContainer.getGeniusResponse().getAmountApproved() != null) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_amount),
                            Global.getCurrencyFormat(emvContainer.getGeniusResponse().getAmountApproved()), lineWidth, 0));
                } else {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_amount),
                            Global.getCurrencyFormat(payArray.getPay_amount()), lineWidth, 0));
                }
                String change = payArray.getChange();
                if (isCashPayment && isCheckPayment && !TextUtils.isEmpty(change) && change.contains(".")
                        && Double.parseDouble(change) > 0) {
                    change = "";
                }
                sb.append("\n");
                print(sb.toString(), FORMAT);
                sb.setLength(0);
                if (includedTip != null) {
                    if (Double.parseDouble(change) > 0) {
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(includedTip,
                                Global.getCurrencyFormat(change), lineWidth, 0));
                    } else if (myPref.isRestaurantMode() &&
                            myPref.getPreferences(MyPreferences.pref_enable_togo_eatin)) {
                        print(textHandler.newLines(1), FORMAT);
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_tip),
                                textHandler.lines(lineWidth / 2), lineWidth, 0))
                                .append("\n");
                        print(sb.toString(), FORMAT);
                        sb.setLength(0);
                        print(textHandler.newLines(1), FORMAT);
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total),
                                textHandler.lines(lineWidth / 2), lineWidth, 0))
                                .append("\n");
                        print(sb.toString(), FORMAT);
                        sb.setLength(0);
                    }
                }
                sb.append("\n");
                print(sb.toString(), FORMAT);
            } else {
                sb.setLength(0);
                sb.append("\n\n");
                sb.append(fromHtml.toString());
                print(sb.toString(), FORMAT);
            }
            sb.setLength(0);
            if (!isCashPayment && !isCheckPayment) {
                if (myPref.getPreferences(MyPreferences.pref_handwritten_signature)) {
                    sb.append(textHandler.newLines(1));
                } else if (payArray.getPay_signature() != null && !TextUtils.isEmpty(payArray.getPay_signature())) {
                    encodedSignature = payArray.getPay_signature();
                    try {
                        /*
                        Pause before printing because it was printing the signature after the header.
                        The app was printing the image after the header.
                        Se añadió una pausa porque estaba imprimiendo la firma luego de el encabezado.
                         */
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    printImage(1);
                }
                sb.append("x").append(textHandler.lines(lineWidth / 2)).append("\n");
                sb.append(getString(R.string.receipt_signature)).append(textHandler.newLines(1));
                print(sb.toString(), FORMAT);
                sb.setLength(0);
            }
            if (Global.isIvuLoto) {
                sb = new StringBuilder();

                if (!printPref.contains(MyPreferences.print_ivuloto_qr)) {
                    printIVULoto(payArray.getIvuLottoNumber(), lineWidth);
                } else {

                    printIVULoto(payArray.getIvuLottoNumber(), lineWidth);

                }
                sb.setLength(0);
            }
//            sb.append("\n");
//            print(sb.toString(), FORMAT);
            sb.setLength(0);
            printFooter(lineWidth);
//            sb.append("\n");
//            print(sb.toString(), FORMAT);
//            sb.setLength(0);

            if (fromHtml == null) {
                if (!isCashPayment && !isCheckPayment) {
                    print(creditCardFooting, FORMAT);
                    String temp = textHandler.newLines(1);
                    print(temp, FORMAT);
                }
            }

            sb.setLength(0);
            if (isReprint) {
                sb.append(textHandler.centeredString("*** Copy ***", lineWidth));
                print(sb.toString(), FORMAT);
            }
            printTermsNConds();
            printEnablerWebSite(lineWidth);
            cutPaper();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String printStationPrinterReceipt(List<Orders> orders, String ordID, int lineWidth, boolean cutPaper, boolean printheader) {
        try {
            AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
            setPaperWidth(lineWidth);
            EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
            OrdersHandler orderHandler = new OrdersHandler(activity);
            OrderProductsHandler ordProdHandler = new OrderProductsHandler(activity);
            Order anOrder = orderHandler.getPrintedOrder(ordID);
            StringBuilder sb = new StringBuilder();
            int size = orders.size();
            if (printheader) {
                if (!TextUtils.isEmpty(anOrder.ord_HoldName))
                    sb.append(getString(R.string.receipt_name)).append(anOrder.ord_HoldName).append("\n");
                if (!TextUtils.isEmpty(anOrder.cust_name))
                    sb.append(anOrder.cust_name).append("\n");
                sb.append(getString(R.string.order)).append(": ").append(ordID).append("\n");
                sb.append(getString(R.string.receipt_started)).append(" ")
                        .append(Global.formatToDisplayDate(anOrder.ord_timeStarted, -1)).append("\n");

                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                sdf1.setTimeZone(Calendar.getInstance().getTimeZone());
                Date startedDate = sdf1.parse(anOrder.ord_timecreated);
                Date sentDate = new Date();

                String clerkName = "";
                if (anOrder.clerk_id != null && !anOrder.clerk_id.isEmpty()) {
                    try {
                        Clerk clerk = ClerkDAO.getByEmpId(Integer.parseInt(anOrder.clerk_id));
                        clerkName = clerk.getEmpName();
                    } catch (Exception e) {
                        // invalid clerk id, leave name blank
                        clerkName = "";
                    }
                }
                sb.append(getString(R.string.receipt_sent_by)).append(" ").append(clerkName).append("\n");
                sb.append(getString(R.string.receipt_terminal)).append(" ").append(employee.getEmpName()).append(" (");

                if (((float) (sentDate.getTime() - startedDate.getTime()) / 1000) > 60)
                    sb.append(Global.formatToDisplayDate(sdf1.format(sentDate.getTime()), -1)).append(")");
                else
                    sb.append(Global.formatToDisplayDate(anOrder.ord_timecreated, -1)).append(")");

                String ordComment = anOrder.ord_comment;
                if (ordComment != null && !TextUtils.isEmpty(ordComment)) {
                    sb.append("\nComments:\n");
                    sb.append(textHandler.oneColumnLineWithLeftAlignedText(ordComment, lineWidth, 3));
                }
                sb.append("\n");
                sb.append(textHandler.newDivider('=', lineWidth / 2)); //add double line divider
                sb.append("\n");
            }

            int m;
            for (int i = 0; i < size; i++) {
                if (orders.get(i).hasAddon()) {
                    m = i;
                    ordProdHandler.updateIsPrinted(orders.get(m).getOrdprodID());
                    sb.append(orders.get(m).getQty()).append("x ").append(orders.get(m).getName()).append("\n");
                    if (!TextUtils.isEmpty(orders.get(m).getAttrDesc()))
                        sb.append("  [").append(orders.get(m).getAttrDesc()).append("]\n");
                    if ((m + 1) < size && orders.get(m + 1).isAddon()) {
                        for (int j = i + 1; j < size; j++) {
                            ordProdHandler.updateIsPrinted(orders.get(j).getOrdprodID());
                            if (orders.get(j).isAdded())
                                sb.append("  >").append(orders.get(j).getName()).append("\n");
                            else
                                sb.append("  >NO ").append(orders.get(j).getName()).append("\n");

                            if ((j + 1 < size && !orders.get(j + 1).isAddon()) || (j + 1 >= size)) {
                                i = j;
                                break;
                            }
                        }
                    }

                    if (!TextUtils.isEmpty(orders.get(m).getOrderProdComment()))
                        sb.append("  ").append(orders.get(m).getOrderProdComment()).append("\n");
                    sb.append(textHandler.newDivider('_', lineWidth / 2)); //add line divider
                    sb.append("\n");

                } else {
                    ordProdHandler.updateIsPrinted(orders.get(i).getOrdprodID());
                    sb.append(orders.get(i).getQty()).append("x ").append(orders.get(i).getName()).append("\n");
                    if (!TextUtils.isEmpty(orders.get(i).getOrderProdComment()))
                        sb.append("  ").append(orders.get(i).getOrderProdComment()).append("\n");
                    sb.append(textHandler.newDivider('_', lineWidth / 2)); //add line divider
                    sb.append("\n");

                }
            }
            sb.append(textHandler.newLines(1));
//
            return sb.toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    void printOpenInvoicesReceipt(String invID, int lineWidth) {
        try {
            startReceipt();
            EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
            StringBuilder sb = new StringBuilder();
            String[] rightInfo;
            List<String[]> productInfo;
            printPref = myPref.getPrintingPreferences();
            InvoicesHandler invHandler = new InvoicesHandler(activity);
            rightInfo = invHandler.getSpecificInvoice(invID);
            InvProdHandler invProdHandler = new InvProdHandler(activity);
            productInfo = invProdHandler.getInvProd(invID);
            setPaperWidth(lineWidth);
            printImage(0);
            if (printPref.contains(MyPreferences.print_header))
                printHeader(lineWidth);
            sb.append(textHandler.centeredString("Open Invoice Summary", lineWidth)).append("\n\n");
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_invoice), rightInfo[1],
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_invoice_ref),
                    rightInfo[2], lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_customer), rightInfo[0],
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_PO), rightInfo[10],
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_terms), rightInfo[9],
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_created), rightInfo[5],
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_ship), rightInfo[7],
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_due), rightInfo[6],
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_paid), rightInfo[8],
                    lineWidth, 0));
            print(sb.toString(), FORMAT);
            sb.setLength(0);
            int size = productInfo.size();
            for (int i = 0; i < size; i++) {
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(
                        productInfo.get(i)[2] + "x " + productInfo.get(i)[0], lineWidth, 1));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_price),
                        Global.getCurrencyFormat(productInfo.get(i)[3]), lineWidth, 3)).append("\n");
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total),
                        Global.getCurrencyFormat(productInfo.get(i)[5]), lineWidth, 3)).append("\n");

                if (printPref.contains(MyPreferences.print_descriptions)) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_description), "",
                            lineWidth, 3)).append("\n");
                    sb.append(textHandler.oneColumnLineWithLeftAlignedText(productInfo.get(i)[1], lineWidth, 5))
                            .append("\n\n");
                } else
                    sb.append(textHandler.newLines(1));

                if (this instanceof EMSStar && !isPOSPrinter && size > SIZE_LIMIT) {
                    // wait to fix printing incomplete issues on SM-T300i models.
                    Thread.sleep(SLEEP_TIME);
                }

                print(sb.toString(), FORMAT);
                sb.setLength(0);
            }
            sb.append(textHandler.lines(lineWidth)).append("\n");
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_invoice_total),
                    Global.getCurrencyFormat(rightInfo[11]), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_amount_collected),
                    Global.getCurrencyFormat(rightInfo[13]), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_balance_due),
                    Global.getCurrencyFormat(rightInfo[12]), lineWidth, 0)).append("\n\n\n");
            sb.append(textHandler.centeredString(getString(R.string.receipt_thankyou), lineWidth));
            print(sb.toString(), FORMAT);
            print(textHandler.newLines(1), FORMAT);
            printEnablerWebSite(lineWidth);
            cutPaper();
        } catch (Exception e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    protected void printConsignmentReceipt(List<ConsignmentTransaction> myConsignment, String encodedSig, int lineWidth) {
        try {
            AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
            startReceipt();
            encodedSignature = encodedSig;
            printPref = myPref.getPrintingPreferences();
            EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
            StringBuilder sb = new StringBuilder();
            ProductsHandler productDBHandler = new ProductsHandler(activity);
            HashMap<String, String> map;
            double ordTotal = 0, totalSold = 0, totalReturned = 0, totalDispached = 0, totalLines = 0, returnAmount,
                    subtotalAmount;
            int size = myConsignment.size();
            setPaperWidth(lineWidth);
            printImage(0);
            if (printPref.contains(MyPreferences.print_header))
                printHeader(lineWidth);
            sb.append(textHandler.centeredString("Consignment Summary", lineWidth)).append("\n\n");
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_customer),
                    myPref.getCustName(), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_employee),
                    employee.getEmpName(), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_cons_trans_id),
                    myConsignment.get(0).ConsTrans_ID, lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_date),
                    Global.formatToDisplayDate(DateUtils.getDateAsString(new Date(), DateUtils.DATE_yyyy_MM_ddTHH_mm_ss), 3), lineWidth, 0));
            sb.append(textHandler.newLines(1));
            for (int i = 0; i < size; i++) {
                if (!myConsignment.get(i).ConsOriginal_Qty.equals("0")) {
                    map = productDBHandler.getProductMap(myConsignment.get(i).ConsProd_ID, true);
                    sb.append(textHandler.oneColumnLineWithLeftAlignedText(map.get("prod_name"), lineWidth, 0));
                    if (printPref.contains(MyPreferences.print_descriptions)) {
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_description),
                                "", lineWidth, 3)).append("\n");
                        sb.append(textHandler.oneColumnLineWithLeftAlignedText(map.get("prod_desc"), lineWidth, 5))
                                .append("\n");
                    } else
                        sb.append(textHandler.newLines(1));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Original Qty:",
                            myConsignment.get(i).ConsOriginal_Qty, lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Rack Qty:",
                            myConsignment.get(i).ConsStock_Qty, lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Returned Qty:",
                            myConsignment.get(i).ConsReturn_Qty, lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Sold Qty:",
                            myConsignment.get(i).ConsInvoice_Qty, lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Dispatched Qty:",
                            myConsignment.get(i).ConsDispatch_Qty, lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("New Qty:", myConsignment.get(i).ConsNew_Qty,
                            lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Product Price:",
                            Global.getCurrencyFormat(map.get("prod_price")), lineWidth, 5));
                    returnAmount = Global.formatNumFromLocale(myConsignment.get(i).ConsReturn_Qty)
                            * Global.formatNumFromLocale(map.get("prod_price"));
                    subtotalAmount = Global.formatNumFromLocale(myConsignment.get(i).invoice_total) + returnAmount;
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Subtotal:",
                            Global.formatDoubleToCurrency(subtotalAmount), lineWidth, 5));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Credit Memo:",
                            Global.formatDoubleToCurrency(returnAmount), lineWidth, 5));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total:",
                            Global.getCurrencyFormat(myConsignment.get(i).invoice_total), lineWidth, 5))
                            .append(textHandler.newLines(1));
                    totalSold += Double.parseDouble(myConsignment.get(i).ConsInvoice_Qty);
                    totalReturned += Double.parseDouble(myConsignment.get(i).ConsReturn_Qty);
                    totalDispached += Double.parseDouble(myConsignment.get(i).ConsDispatch_Qty);
                    totalLines += 1;
                    ordTotal += Double.parseDouble(myConsignment.get(i).invoice_total);
                    print(sb.toString(), FORMAT);
                    sb.setLength(0);

                    if (this instanceof EMSStar && !isPOSPrinter && size > SIZE_LIMIT) {
                        // wait to fix printing incomplete issues on SM-T300i models.
                        Thread.sleep(SLEEP_TIME);
                    }
                }
            }

            sb.append(textHandler.lines(lineWidth));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Items Sold:", Double.toString(totalSold),
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Items Returned",
                    Double.toString(totalReturned), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Items Dispatched",
                    Double.toString(totalDispached), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Line Items", Double.toString(totalLines),
                    lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText("Grand Total:",
                    Global.formatDoubleToCurrency(ordTotal), lineWidth, 0));
            sb.append(textHandler.newLines(1));
            print(sb.toString(), FORMAT);
            if (printPref.contains(MyPreferences.print_descriptions))
                printFooter(lineWidth);
            try {
                printImage(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            printEnablerWebSite(lineWidth);
            print(textHandler.newLines(1), FORMAT);
            cutPaper();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void printConsignmentHistoryReceipt(HashMap<String, String> map, Cursor c, boolean isPickup, int lineWidth) {
        try {
            AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
            startReceipt();
            encodedSignature = map.get("encoded_signature");
            printPref = myPref.getPrintingPreferences();
            EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
            StringBuilder sb = new StringBuilder();
            String prodDesc;
            int size = c.getCount();
            setPaperWidth(lineWidth);
            printImage(0);
            if (printPref.contains(MyPreferences.print_header))
                printHeader(lineWidth);
            if (!isPickup)
                sb.append(textHandler.centeredString(getString(R.string.consignment_summary), lineWidth))
                        .append("\n\n");
            else
                sb.append(textHandler.centeredString(getString(R.string.consignment_pickup), lineWidth))
                        .append("\n\n");
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_customer),
                    map.get("cust_name"), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_employee),
                    employee.getEmpName(), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_cons_trans_id),
                    map.get("ConsTrans_ID"), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_date),
                    Global.formatToDisplayDate(DateUtils.getDateAsString(new Date(), DateUtils.DATE_yyyy_MM_ddTHH_mm_ss), 3), lineWidth, 0));
            sb.append(textHandler.newLines(1));
            for (int i = 0; i < size; i++) {
                c.moveToPosition(i);
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(c.getString(c.getColumnIndex("prod_name")),
                        lineWidth, 0));
                if (printPref.contains(MyPreferences.print_descriptions)) {
                    prodDesc = c.getString(c.getColumnIndex("prod_desc"));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_description), "",
                            lineWidth, 3)).append("\n");
                    if (!TextUtils.isEmpty(prodDesc))
                        sb.append(textHandler.oneColumnLineWithLeftAlignedText(
                                c.getString(c.getColumnIndex("prod_desc")), lineWidth, 5)).append("\n");
                } else
                    sb.append(textHandler.newLines(1));
                if (!isPickup) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Original Qty:",
                            c.getString(c.getColumnIndex("ConsOriginal_Qty")), lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Rack Qty:",
                            c.getString(c.getColumnIndex("ConsStock_Qty")), lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Returned Qty:",
                            c.getString(c.getColumnIndex("ConsReturn_Qty")), lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Sold Qty:",
                            c.getString(c.getColumnIndex("ConsInvoice_Qty")), lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Dispatched Qty:",
                            c.getString(c.getColumnIndex("ConsDispatch_Qty")), lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("New Qty:",
                            c.getString(c.getColumnIndex("ConsNew_Qty")), lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Product Price:",
                            Global.getCurrencyFormat(c.getString(c.getColumnIndex("price"))), lineWidth, 5));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Subtotal:",
                            Global.getCurrencyFormat(c.getString(c.getColumnIndex("item_subtotal"))),
                            lineWidth, 5));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Credit Memo:",
                            Global.getCurrencyFormat(c.getString(c.getColumnIndex("credit_memo"))), lineWidth,
                            5));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total:",
                            Global.getCurrencyFormat(c.getString(c.getColumnIndex("item_total"))), lineWidth,
                            5)).append(textHandler.newLines(1));
                } else {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Original Qty:",
                            c.getString(c.getColumnIndex("ConsOriginal_Qty")), lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Picked up Qty:",
                            c.getString(c.getColumnIndex("ConsPickup_Qty")), lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("New Qty:",
                            c.getString(c.getColumnIndex("ConsNew_Qty")), lineWidth, 3)).append("\n\n\n");
                }
                print(sb.toString(), FORMAT);
                sb.setLength(0);

                if (this instanceof EMSStar && !isPOSPrinter && size > SIZE_LIMIT) {
                    // wait to fix printing incomplete issues on SM-T300i models.
                    Thread.sleep(SLEEP_TIME);
                }
            }
            sb.append(textHandler.lines(lineWidth));
            if (!isPickup) {
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Items Sold:", map.get("total_items_sold"),
                        lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Items Returned",
                        map.get("total_items_returned"), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Items Dispatched",
                        map.get("total_items_dispatched"), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Line Items", map.get("total_line_items"),
                        lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Grand Total:",
                        Global.getCurrencyFormat(map.get("total_grand_total")), lineWidth, 0));
            }
            sb.append(textHandler.newLines(1));
            print(sb.toString(), FORMAT);
            if (printPref.contains(MyPreferences.print_footer))
                printFooter(lineWidth);
            printImage(1);
            print(textHandler.newLines(3), FORMAT);
            printEnablerWebSite(lineWidth);
            cutPaper();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void printConsignmentPickupReceipt(List<ConsignmentTransaction> myConsignment, String encodedSig, int lineWidth) {
        try {
            AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
            startReceipt();
            printPref = myPref.getPrintingPreferences();
            EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
            StringBuilder sb = new StringBuilder();
            ProductsHandler productDBHandler = new ProductsHandler(activity);
            HashMap<String, String> map;
            String prodDesc;
            int size = myConsignment.size();
            setPaperWidth(lineWidth);
            printImage(0);
            if (printPref.contains(MyPreferences.print_header))
                printHeader(lineWidth);
            sb.append(textHandler.centeredString("Consignment Pickup Summary", lineWidth)).append("\n\n");
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_customer),
                    myPref.getCustName(), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_employee),
                    employee.getEmpName(), lineWidth, 0));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_date),
                    Global.formatToDisplayDate(DateUtils.getDateAsString(new Date(), DateUtils.DATE_yyyy_MM_ddTHH_mm_ss), 3), lineWidth, 0));
            sb.append(textHandler.newLines(1));
            for (int i = 0; i < size; i++) {
                map = productDBHandler.getProductMap(myConsignment.get(i).ConsProd_ID, true);
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(map.get("prod_name"), lineWidth, 0));
                if (printPref.contains(MyPreferences.print_descriptions)) {
                    prodDesc = map.get("prod_desc");
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_description), "",
                            lineWidth, 3)).append("\n");
                    if (!TextUtils.isEmpty(prodDesc))
                        sb.append(textHandler.oneColumnLineWithLeftAlignedText(prodDesc, lineWidth, 5)).append("\n");
                }
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Original Qty:",
                        myConsignment.get(i).ConsOriginal_Qty, lineWidth, 3));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Picked up Qty:",
                        myConsignment.get(i).ConsPickup_Qty, lineWidth, 3));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("New Qty:", myConsignment.get(i).ConsNew_Qty,
                        lineWidth, 3)).append("\n\n\n");
                print(sb.toString(), FORMAT);
                sb.setLength(0);

                if (this instanceof EMSStar && !isPOSPrinter && size > SIZE_LIMIT) {
                    // wait to fix printing incomplete issues on SM-T300i models.
                    Thread.sleep(SLEEP_TIME);
                }
            }

            if (printPref.contains(MyPreferences.print_footer))
                printFooter(lineWidth);
            if (!TextUtils.isEmpty(encodedSig)) {
                encodedSignature = encodedSig;
                printImage(1);
                sb.setLength(0);
                sb.append("x").append(textHandler.lines(lineWidth / 2)).append("\n");
                sb.append(getString(R.string.receipt_signature)).append(textHandler.newLines(1));
                print(sb.toString(), FORMAT);
                print(textHandler.newLines(1), FORMAT);
            }
            printEnablerWebSite(lineWidth);
            cutPaper();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void printEndOfDayReportReceipt(String curDate, int lineWidth, boolean printDetails) {
        AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
        startReceipt();
        String mDate = Global.formatToDisplayDate(curDate, 4);
        StringBuilder sb = new StringBuilder();
        EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
        StringBuilder sb_ord_types = new StringBuilder();
        OrdersHandler ordHandler = new OrdersHandler(activity);
        OrderProductsHandler ordProdHandler = new OrderProductsHandler(activity);
        PaymentsHandler paymentHandler = new PaymentsHandler(activity);
        boolean showTipField = false;

        //determine if we should include the tip field
        if (myPref.getPreferences(MyPreferences.pref_show_tips_for_cash)) {
            showTipField = true;
        }
        sb.append(textHandler.centeredString("End Of Day Report", lineWidth));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Date", Global.formatToDisplayDate(curDate, 1), lineWidth, 0));
        sb.append(textHandler.newLines(1));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Employee", employee.getEmpName(), lineWidth, 0));
        sb.append(textHandler.newLines(1));
        sb.append(textHandler.centeredString("Summary", lineWidth));
        sb.append(textHandler.newLines(1));

        BigDecimal returnAmount = new BigDecimal("0");
        BigDecimal salesAmount = new BigDecimal("0");
        BigDecimal invoiceAmount = new BigDecimal("0");
        BigDecimal onHoldAmount = new BigDecimal("0");

        sb_ord_types.append(textHandler.centeredString("Totals By Order Types", lineWidth));
        List<Order> listOrder = ordHandler.getOrderDayReport(null, mDate, false);
        HashMap<String, List<DataTaxes>> taxesBreakdownHashMap = ordHandler.getOrderDayReportTaxesBreakdown(null, mDate);
        List<Order> listOrderHolds = ordHandler.getOrderDayReport(null, mDate, true);
        for (Order ord : listOrder) {
            switch (Global.OrderType.getByCode(Integer.parseInt(ord.ord_type))) {
                case RETURN:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Return", lineWidth, 0));
                    returnAmount = new BigDecimal(ord.ord_total);
                    break;
                case ESTIMATE:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Estimate", lineWidth, 0));
                    break;
                case ORDER:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Order", lineWidth, 0));
                    break;
                case SALES_RECEIPT:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Sales Receipt", lineWidth, 0));
                    salesAmount = new BigDecimal(ord.ord_total);
                    break;
                case INVOICE:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Invoice", lineWidth, 0));
                    invoiceAmount = new BigDecimal(ord.ord_total);
                    break;
                case CONSIGNMENT_FILLUP:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Fill Up", lineWidth, 0));
                    invoiceAmount = new BigDecimal(ord.ord_total);
                    break;
                case CONSIGNMENT_PICKUP:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Pickup", lineWidth, 0));
                    invoiceAmount = new BigDecimal(ord.ord_total);
                    break;
                case CONSIGNMENT_INVOICE:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Consignment Invoice", lineWidth, 0));
                    invoiceAmount = new BigDecimal(ord.ord_total);
                    break;
            }

            sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText("SubTotal", Global.getCurrencyFormat(ord.ord_subtotal), lineWidth, 3));
            sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText("Discounts", Global.getCurrencyFormat(ord.ord_discount), lineWidth, 3));

            if (taxesBreakdownHashMap.containsKey(ord.ord_type)) {
                sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText("Taxes", lineWidth, 3));
                for (DataTaxes dataTax : taxesBreakdownHashMap.get(ord.ord_type)) {
                    sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText(dataTax.getTax_name(), Global.getCurrencyFormat(dataTax.getTax_amount()), lineWidth, 6));
                }
            } else {
                sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText("Taxes", "N/A", lineWidth, 3));
            }

            sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText("Total", Global.getCurrencyFormat(ord.ord_total), lineWidth, 3));
        }
        if (listOrderHolds != null && !listOrderHolds.isEmpty()) {
            onHoldAmount = new BigDecimal(listOrderHolds.get(0).ord_total);
        }
        print(sb.toString());
        sb.setLength(0);
        listOrder.clear();
        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Return", "(" + Global.getCurrencyFormat(returnAmount.toString()) + ")", lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Sales Receipt", Global.getCurrencyFormat(salesAmount.toString()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Invoice", Global.getCurrencyFormat(invoiceAmount.toString()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText("On Holds", Global.getCurrencyFormat(onHoldAmount.toString()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total", Global.getCurrencyFormat(salesAmount.add(invoiceAmount).subtract(returnAmount).toString()), lineWidth, 0));
        listOrder = ordHandler.getARTransactionsDayReport(null, mDate);
        if (listOrder.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("A/R Transactions", lineWidth));
            sb.append(textHandler.threeColumnLineItem("ID", 40, "Customer", 40, "Amount", 20, lineWidth, 0));
            for (Order ord : listOrder) {
                if (ord.ord_id != null)
                    sb.append(textHandler.threeColumnLineItem(ord.ord_id, 40, ord.cust_name, 40, Global.getCurrencyFormat(ord.ord_total), 20, lineWidth, 0));
            }
            listOrder.clear();
        }
        print(sb.toString());
        sb.setLength(0);
        List<Shift> listShifts = ShiftDAO.getShift(DateUtils.getDateStringAsDate(curDate, DateUtils.DATE_yyyy_MM_dd));
        if (listShifts.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Totals By Shift", lineWidth));
            for (Shift shift : listShifts) {
                Clerk clerk = ClerkDAO.getByEmpId(shift.getClerkId());
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Sales Clerk", clerk.getEmpName(), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("From", DateUtils.getDateAsString(shift.getStartTime(), DateUtils.DATE_yyyy_MM_dd), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("To", DateUtils.getDateAsString(shift.getEndTime(), DateUtils.DATE_yyyy_MM_dd), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.begging_petty_cash), Global.getCurrencyFormat(shift.getBeginningPettyCash()), lineWidth, 3));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.total_expenses), "(" + Global.getCurrencyFormat(shift.getTotalExpenses()) + ")", lineWidth, 3));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.ending_petty_cash), Global.getCurrencyFormat(shift.getEndingPettyCash()), lineWidth, 3));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Transactions Cash", Global.getCurrencyFormat(shift.getTotalTransactionsCash()), lineWidth, 3));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Total Ending Cash", Global.getCurrencyFormat(shift.getTotal_ending_cash()), lineWidth, 3));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Entered Close Amount", Global.getCurrencyFormat(shift.getEnteredCloseAmount()), lineWidth, 3));
                sb.append(textHandler.newLines(1));
            }
            listShifts.clear();
        }
        print(sb.toString());
        sb.setLength(0);
        sb.append(textHandler.newLines(1));
        sb.append(sb_ord_types);
        List<OrderProduct> listProd = ordProdHandler.getProductsDayReport(true, null, mDate);
        if (listProd.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Items Sold", lineWidth));
            sb.append(textHandler.threeColumnLineItem("Name", 60, "Qty", 20, "Total", 20, lineWidth, 0));

            for (OrderProduct prod : listProd) {
                String calc;
                if (new BigDecimal(prod.getOrdprod_qty()).compareTo(new BigDecimal(0)) != 0) {
                    calc = Global.getCurrencyFormat(prod.getFinalPrice());
                } else {
                    calc = Global.formatDoubleToCurrency(0);
                }

                sb.append(textHandler.threeColumnLineItem(prod.getOrdprod_name(), 60,
                        prod.getOrdprod_qty(), 20,
                        calc,
                        20, lineWidth, 0));
                if (printDetails) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("UPC:" + prod.getProd_upc(), "", lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("SKU:" + prod.getProd_sku(), "", lineWidth, 3));
                }
            }
            listProd.clear();
        }
        print(sb.toString());
        sb.setLength(0);
        listProd = ordProdHandler.getDepartmentDayReport(true, null, mDate);
        if (listProd.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Department Sales", lineWidth));
            sb.append(textHandler.threeColumnLineItem("Name", 60, "Qty", 20, "Total", 20, lineWidth, 0));
            for (OrderProduct prod : listProd) {
                sb.append(textHandler.threeColumnLineItem(prod.getCat_name(), 60, prod.getOrdprod_qty(), 20, Global.getCurrencyFormat(prod.getFinalPrice()), 20, lineWidth, 0));
            }
            listProd.clear();
        }
        print(sb.toString());
        sb.setLength(0);
        listProd = ordProdHandler.getDepartmentDayReport(false, null, mDate);
        if (listProd.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Department Returns", lineWidth));
            sb.append(textHandler.threeColumnLineItem("Name", 60, "Qty", 20, "Total", 20, lineWidth, 0));
            for (OrderProduct prod : listProd) {
                sb.append(textHandler.threeColumnLineItem(prod.getCat_name(), 60, prod.getOrdprod_qty(), 20, Global.getCurrencyFormat(prod.getFinalPrice()), 20, lineWidth, 0));
            }
            listProd.clear();
        }
        print(sb.toString());
        sb.setLength(0);
        List<Payment> listPayments = paymentHandler.getPaymentsGroupDayReport(0, null, mDate);
        if (listPayments.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Payments", lineWidth));
            for (Payment payment : listPayments) {
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(payment.getCard_type(), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 2));
                if (printDetails) {
                    //check if tip should be printed
                    if (showTipField) {
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Tip", Global.getCurrencyFormat(payment.getPay_tip()), lineWidth, 2));
                    }
                    sb.append(textHandler.oneColumnLineWithLeftAlignedText("Details", lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("ID", payment.getPay_id(), lineWidth, 4));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 4));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Invoice", payment.getJob_id(), lineWidth, 4));
                    sb.append(textHandler.newLines(1));
                    print(sb.toString());
                    sb.setLength(0);
                }
            }
            listPayments.clear();
        }

        listPayments = paymentHandler.getPaymentsGroupDayReport(1, null, mDate);
        if (listPayments.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Voids", lineWidth));
            for (Payment payment : listPayments) {
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(payment.getCard_type(), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 2));
                if (printDetails) {
                    //check if tip should be printed
                    if (showTipField) {
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Tip", Global.getCurrencyFormat(payment.getPay_tip()), lineWidth, 2));
                    }
                    sb.append(textHandler.oneColumnLineWithLeftAlignedText("Details", lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("ID", payment.getPay_id(), lineWidth, 4));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 4));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Invoice", payment.getJob_id(), lineWidth, 4));
                    sb.append(textHandler.newLines(1));
                    print(sb.toString());
                    sb.setLength(0);
                }
            }
            listPayments.clear();
        }

        listPayments = paymentHandler.getPaymentsGroupDayReport(2, null, mDate);
        if (listPayments.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Refunds", lineWidth));
            for (Payment payment : listPayments) {
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(payment.getCard_type(), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 2));

                if (printDetails) {
                    //check if tip should be printed
                    if (showTipField) {
                        sb.append(textHandler.twoColumnLineWithLeftAlignedText("Tip", Global.getCurrencyFormat(payment.getPay_tip()), lineWidth, 2));
                    }
                    sb.append(textHandler.oneColumnLineWithLeftAlignedText("Details", lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("ID", payment.getPay_id(), lineWidth, 4));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 4));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("Invoice", payment.getJob_id(), lineWidth, 4));
                    sb.append(textHandler.newLines(1));
                    print(sb.toString());
                    sb.setLength(0);
                }
            }
            listPayments.clear();
        }

        listProd = ordProdHandler.getProductsDayReport(false, null, mDate);
        if (listProd.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Items Returned", lineWidth));
            sb.append(textHandler.threeColumnLineItem("Name", 60, "Qty", 20, "Total", 20, lineWidth, 0));
            for (OrderProduct prod : listProd) {
                sb.append(textHandler.threeColumnLineItem(prod.getOrdprod_name(), 60, prod.getOrdprod_qty(), 20, Global.getCurrencyFormat(prod.getFinalPrice()), 20, lineWidth, 0));
                if (printDetails) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("UPC:" + prod.getProd_upc(), "", lineWidth, 3));
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText("SKU:" + prod.getProd_sku(), "", lineWidth, 3));
                }
            }
            listProd.clear();
        }
        print(sb.toString());

        sb.setLength(0);
        sb.append(textHandler.centeredString("** End of report **", lineWidth));
        sb.append(textHandler.newLines(4));
        print(sb.toString(), FORMAT);
        cutPaper();
    }

    protected void printShiftDetailsReceipt(int lineWidth, String shiftID) {
        AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
        startReceipt();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb_ord_types = new StringBuilder();
        EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
        sb.append(textHandler.centeredString(activity.getString(R.string.shift_details), lineWidth));
        Shift shift = ShiftDAO.getShift(shiftID);
        if (shift.getEndTime() == null) {
            // shift is not ended
            shift.setEndTime(new Date());
        }
        Clerk clerk = ClerkDAO.getByEmpId(shift.getClerkId());
        sb.append(textHandler.newLines(1));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.shift_id),
                String.format("%s-%s", clerk.getEmpId(), getEpochTime(shift.getCreationDate())),
                lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.sales_clerk), clerk == null ?
                shift.getAssigneeName() : clerk.getEmpName(), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.receipt_employee), employee.getEmpName(), lineWidth, 0));
        sb.append(textHandler.newLines(1));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.from), DateUtils.getDateAsString(shift.getStartTime()), lineWidth, 0));

        sb.append(textHandler.newLines(1));
        if (shift.getShiftStatus() == Shift.ShiftStatus.OPEN) {
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.to), Shift.ShiftStatus.OPEN.name(), lineWidth, 0));
        } else {
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.to), DateUtils.getDateAsString(shift.getEndTime()), lineWidth, 0));
        }
        sb.append(textHandler.newLines(1));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.begging_petty_cash), Global.getCurrencyFormat(shift.getBeginningPettyCash()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.total_expenses), Global.getCurrencyFormat(shift.getTotalExpenses()), lineWidth, 0));
        List<ShiftExpense> shiftExpenses = ShiftExpensesDAO.getShiftExpenses(shiftID);
        if (shiftExpenses != null) {
            for (ShiftExpense expense : shiftExpenses) {
                sb.append(textHandler.twoColumnLineWithLeftAlignedText(expense.getProductName(),
                        Global.getCurrencyFormat(expense.getCashAmount()), lineWidth, 3));
            }
        }
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.total_transactions_cash), Global.getCurrencyFormat(shift.getTotalTransactionsCash()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.total_ending_cash), Global.getCurrencyFormat(shift.getTotal_ending_cash()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.entered_close_amount), Global.getCurrencyFormat(shift.getEnteredCloseAmount()), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.shortage_overage_amount), Global.getCurrencyFormat(shift.getOver_short()), lineWidth, 0));
        sb.append(textHandler.newLines(1));

        OrderProductsHandler orderProductsHandler = new OrderProductsHandler(activity);
        String startDate = DateUtils.getDateAsString(
                shift.getCreationDate(), "yyyy-MM-dd HH:mm");
        String endDate = DateUtils.getDateAsString(
                shift.getEndTime(), "yyyy-MM-dd HH:mm");

        List<SalesByClerk> listDeptSalesByClerk = orderProductsHandler
                .getSalesShiftReportByClerk(true, null, startDate, endDate);
        sb.append(textHandler.centeredString(
                activity.getString(R.string.eod_report_sales_by_clerk), lineWidth));

        for (SalesByClerk salesByClerk : listDeptSalesByClerk) {
            String clerkName = "";
            if (!salesByClerk.getClerkId().isEmpty()) {
                Clerk reportClerk = ClerkDAO.getByEmpId(Integer.parseInt(salesByClerk.getClerkId())); // clerk id
                if (reportClerk != null) {
                    clerkName = String.format(
                            "%s (%s)", reportClerk.getEmpName(), reportClerk.getEmpId());
                }
            } else {
                clerkName = employee.getEmpName();
            }
            sb.append(
                    textHandler.threeColumnLineItem(clerkName, // clerk name
                            60,
                            salesByClerk.getOrdProdQuantity(), // total orders
                            20,
                            Global.getCurrencyFormat(salesByClerk.getOverwritePrice()), // total
                            20, lineWidth, 0));
        }
        sb.append(textHandler.newLines(1));

        OrdersHandler ordHandler = new OrdersHandler(activity);

        sb_ord_types.append(textHandler.centeredString("Totals By Order Types", lineWidth));
        List<Order> listOrder = ordHandler.getOrderShiftReport(null, startDate, endDate);
        HashMap<String, List<DataTaxes>> taxesBreakdownHashMap =
                ordHandler.getOrderShiftReportTaxesBreakdown(null, startDate, endDate);

        for (Order ord : listOrder) {
            switch (Global.OrderType.getByCode(Integer.parseInt(ord.ord_type))) {
                case RETURN:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText(
                            "Return", lineWidth, 0));
                    break;
                case ESTIMATE:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText(
                            "Estimate", lineWidth, 0));
                    break;
                case ORDER:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText(
                            "Order", lineWidth, 0));
                    break;
                case SALES_RECEIPT:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText(
                            "Sales Receipt", lineWidth, 0));
                    break;
                case INVOICE:
                    sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText(
                            "Invoice", lineWidth, 0));
                    break;
            }

            sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText("SubTotal",
                    Global.getCurrencyFormat(ord.ord_subtotal), lineWidth, 3));
            sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText("Discounts",
                    Global.getCurrencyFormat(ord.ord_discount), lineWidth, 3));

            if (taxesBreakdownHashMap.containsKey(ord.ord_type)) {
                sb_ord_types.append(textHandler.oneColumnLineWithLeftAlignedText(
                        "Taxes", lineWidth, 3));
                for (DataTaxes dataTax : taxesBreakdownHashMap.get(ord.ord_type)) {
                    sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText(
                            dataTax.getTax_name(), Global.getCurrencyFormat(
                                    dataTax.getTax_amount()), lineWidth, 6));
                }
            } else {
                sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText(
                        "Taxes", "N/A", lineWidth, 3));
            }

            sb_ord_types.append(textHandler.twoColumnLineWithLeftAlignedText(
                    "Total", Global.getCurrencyFormat(ord.ord_total), lineWidth, 3));
        }

        sb.append(sb_ord_types);
        sb.append(textHandler.newLines(1));

        List<OrderProduct> listDeptSales = orderProductsHandler.getDepartmentDayReport(
                true, null, startDate, endDate);
        List<OrderProduct> listDeptReturns = orderProductsHandler.getDepartmentDayReport(
                false, null, startDate, endDate);
        if (!listDeptSales.isEmpty()) {
            sb.append(textHandler.centeredString(activity.getString(R.string.eod_report_dept_sales), lineWidth));
            for (OrderProduct product : listDeptSales) {
                sb.append(textHandler.threeColumnLineItem(product.getCat_name(), 60,
                        product.getOrdprod_qty(), 20,
                        Global.getCurrencyFormat(product.getFinalPrice()),
                        20, lineWidth, 0));
            }
        }
        sb.append(textHandler.newLines(1));
        if (!listDeptReturns.isEmpty()) {
            sb.append(textHandler.centeredString(activity.getString(R.string.eod_report_return), lineWidth));
            for (OrderProduct product : listDeptReturns) {
                sb.append(textHandler.threeColumnLineItem(product.getCat_name(), 60,
                        product.getOrdprod_qty(), 20,
                        Global.getCurrencyFormat(product.getFinalPrice()),
                        20, lineWidth, 0));
            }
        }

        PaymentsHandler paymentHandler = new PaymentsHandler(activity);
        List<Payment> listPayments = paymentHandler.getPaymentsGroupShiftReport(0, null, startDate, endDate);
        if (listPayments.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Payments", lineWidth));
            for (Payment payment : listPayments) {
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(payment.getCard_type(), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 2));
            }
            listPayments.clear();
        }

        listPayments = paymentHandler.getPaymentsGroupShiftReport(1, null, startDate, endDate);
        if (listPayments.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Voids", lineWidth));
            for (Payment payment : listPayments) {
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(payment.getCard_type(), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 2));
            }
            listPayments.clear();
        }

        listPayments = paymentHandler.getPaymentsGroupShiftReport(2, null, startDate, endDate);
        if (listPayments.size() > 0) {
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.centeredString("Refunds", lineWidth));
            for (Payment payment : listPayments) {
                sb.append(textHandler.oneColumnLineWithLeftAlignedText(payment.getCard_type(), lineWidth, 0));
                sb.append(textHandler.twoColumnLineWithLeftAlignedText("Amount", Global.getCurrencyFormat(payment.getPay_amount()), lineWidth, 2));
            }
            listPayments.clear();
        }

        sb.append(textHandler.newLines(1));
        sb.append(textHandler.centeredString(activity.getString(R.string.endShiftReport), lineWidth));
        sb.append(textHandler.newLines(4));
        print(sb.toString(), FORMAT);
        cutPaper();
    }


    protected void printExpenseReceipt(int lineWidth, ShiftExpense expense) {
        AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
        startReceipt();
        StringBuilder sb = new StringBuilder();
        EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
        sb.append(textHandler.centeredString(activity.getString(R.string.shift_expense), lineWidth));
        Shift shift = ShiftDAO.getShift(expense.getShiftId());
        Clerk clerk = ClerkDAO.getByEmpId(shift.getClerkId());
        sb.append(textHandler.newLines(1));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.sales_clerk), clerk == null ?
                shift.getAssigneeName() : clerk.getEmpName(), lineWidth, 0));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.receipt_employee), employee.getEmpName(), lineWidth, 0));
        sb.append(textHandler.newLines(1));
        sb.append(textHandler.twoColumnLineWithLeftAlignedText(activity.getString(R.string.date), DateUtils.getDateAsString(expense.getCreationDate()), lineWidth, 0));

        sb.append(textHandler.newLines(1));

        sb.append(textHandler.twoColumnLineWithLeftAlignedText(expense.getProductName(),
                Global.getCurrencyFormat(expense.getCashAmount()), lineWidth, 3));
        sb.append(textHandler.centeredString(activity.getString(R.string.receipt_description), lineWidth));
        sb.append(textHandler.oneColumnLineWithLeftAlignedText(expense.getProductDescription(), lineWidth, 0));
        sb.append(textHandler.newLines(4));
        print(sb.toString(), FORMAT);
        cutPaper();
    }

    void printReportReceipt(String curDate, int lineWidth) {
        try {
            AssignEmployee employee = AssignEmployeeDAO.getAssignEmployee();
            startReceipt();
            PaymentsHandler paymentHandler = new PaymentsHandler(activity);
            PayMethodsHandler payMethodHandler = new PayMethodsHandler(activity);
            EMSPlainTextHelper textHandler = new EMSPlainTextHelper();
            StringBuilder sb = new StringBuilder();
            StringBuilder sb_refunds = new StringBuilder();
            print(textHandler.newLines(1), FORMAT);
            sb.append(textHandler.centeredString("REPORT", lineWidth));
            sb.append(textHandler.centeredString(Global.formatToDisplayDate(curDate, 0), lineWidth));
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.oneColumnLineWithLeftAlignedText(getString(R.string.receipt_pay_summary), lineWidth,
                    0));
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText("Employee", employee.getEmpName(), lineWidth, 0));
            sb.append(textHandler.newLines(1));
            sb_refunds.append(textHandler.oneColumnLineWithLeftAlignedText(getString(R.string.receipt_refund_summmary),
                    lineWidth, 0));
            HashMap<String, String> paymentMap = paymentHandler
                    .getPaymentsRefundsForReportPrinting(Global.formatToDisplayDate(curDate, 4), 0);
            HashMap<String, String> refundMap = paymentHandler
                    .getPaymentsRefundsForReportPrinting(Global.formatToDisplayDate(curDate, 4), 1);
            List<String[]> payMethodsNames = payMethodHandler.getPayMethodsName();
            int size = payMethodsNames.size();
            double payGranTotal = 0.00;
            double refundGranTotal = 0.00;
            print(sb.toString(), FORMAT);
            sb.setLength(0);
            for (int i = 0; i < size; i++) {
                if (paymentMap.containsKey(payMethodsNames.get(i)[0])) {
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(payMethodsNames.get(i)[1],
                            Global.getCurrencyFormat(paymentMap.get(payMethodsNames.get(i)[0])), lineWidth,
                            3));
                    payGranTotal += Double.parseDouble(paymentMap.get(payMethodsNames.get(i)[0]));
                } else
                    sb.append(textHandler.twoColumnLineWithLeftAlignedText(payMethodsNames.get(i)[1],
                            Global.formatDoubleToCurrency(0.00), lineWidth, 3));
                if (refundMap.containsKey(payMethodsNames.get(i)[0])) {
                    sb_refunds.append(textHandler.twoColumnLineWithLeftAlignedText(payMethodsNames.get(i)[1],
                            Global.getCurrencyFormat(refundMap.get(payMethodsNames.get(i)[0])), lineWidth, 3));
                    refundGranTotal += Double.parseDouble(refundMap.get(payMethodsNames.get(i)[0]));
                } else
                    sb_refunds.append(textHandler.twoColumnLineWithLeftAlignedText(payMethodsNames.get(i)[1],
                            Global.formatDoubleToCurrency(0.00), lineWidth, 3));
            }
            sb.append(textHandler.newLines(1));
            sb.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total),
                    Global.getCurrencyFormat(Double.toString(payGranTotal)), lineWidth, 4));
            sb.append(textHandler.newLines(1));
            sb_refunds.append(textHandler.newLines(1));
            sb_refunds.append(textHandler.twoColumnLineWithLeftAlignedText(getString(R.string.receipt_total),
                    Global.getCurrencyFormat(Double.toString(refundGranTotal)), lineWidth, 4));
            //print earnings
            print(sb.toString(), FORMAT);
            print(textHandler.newLines(1), FORMAT);
            //print refunds
            print(sb_refunds.toString(), FORMAT);
            print(textHandler.newLines(5), FORMAT);

            printEnablerWebSite(lineWidth);
            if (isPOSPrinter) {
                port.writePort(new byte[]{0x1b, 0x64, 0x02}, 0, 3); // Cut
            }
            cutPaper();
        } catch (StarIOPortException ignored) {
        }
    }
    protected void printGiftReceipt(Receipt receipt, int lineWidth) {
        StringBuilder sb = new StringBuilder();
        if (receipt.getMerchantLogo() != null)
            printImage(receipt.getMerchantLogo());
        if (receipt.getMerchantHeader() != null)
            sb.append((receipt.getMerchantHeader()));
        if (receipt.getSpecialHeader() != null)
            sb.append((receipt.getSpecialHeader()));
        if (receipt.getRemoteStationHeader() != null)
            sb.append((receipt.getRemoteStationHeader()));
        if (receipt.getHeader() != null)
            sb.append((receipt.getHeader()));
        if (receipt.getEmvDetails() != null)
            sb.append((receipt.getEmvDetails()));
        if (receipt.getSeparator() != null)
            sb.append((receipt.getSeparator()));
        for (String s : receipt.getItems()) {
            if (s != null)
                sb.append((s));
        }
        for (String s : receipt.getRemoteStationItems()) {
            if (s != null)
                sb.append((s));
        }
        if (receipt.getSeparator() != null)
            sb.append((receipt.getSeparator()));
        if (receipt.getTotals() != null)
            sb.append((receipt.getTotals()));
        if (receipt.getTaxes() != null)
            sb.append((receipt.getTaxes()));
        if (receipt.getTotalItems() != null)
            sb.append((receipt.getTotalItems()));
        if (receipt.getGrandTotal() != null)
            sb.append((receipt.getGrandTotal()));
        if (receipt.getPaymentsDetails() != null)
            sb.append((receipt.getPaymentsDetails()));
        if (receipt.getYouSave() != null)
            sb.append((receipt.getYouSave()));
        if (receipt.getIvuLoto() != null)
            sb.append((receipt.getIvuLoto()));
        if (receipt.getLoyaltyDetails() != null)
            sb.append((receipt.getLoyaltyDetails()));
        if (receipt.getRewardsDetails() != null)
            sb.append((receipt.getRewardsDetails()));
        try {
            printPref = myPref.getPrintingPreferences();
            print(sb.toString(), FORMAT);
            if (printPref != null && printPref.contains(MyPreferences.print_descriptions))
                printFooter(lineWidth);
            try {
                printImage(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            printEnablerWebSite(lineWidth);
            print(textHandler.newLines(1), FORMAT);
            cutPaper();
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }
}
