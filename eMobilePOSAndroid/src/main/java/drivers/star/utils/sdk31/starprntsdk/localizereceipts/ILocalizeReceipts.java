package drivers.star.utils.sdk31.starprntsdk.localizereceipts;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt.CharacterCode;

import drivers.star.utils.sdk31.starprntsdk.PrinterSetting;

public abstract class ILocalizeReceipts {
    private int mPaperSize;
    private int mLanguage;

    protected String mLanguageCode;
    private String mPaperSizeStr;
    private String mScalePaperSizeStr;
    protected CharacterCode mCharacterCode;

    public static ILocalizeReceipts createLocalizeReceipts(int language, int paperSize) {
        ILocalizeReceipts localizeReceipts;

        switch (language) {
            case PrinterSetting.LANGUAGE_ENGLISH:
                localizeReceipts = new EnglishReceiptsImpl();
                break;
            case PrinterSetting.LANGUAGE_SPANISH:
                localizeReceipts = new SpanishReceiptsImpl();
                break;
            default:
                localizeReceipts = new EnglishReceiptsImpl();
        }

        switch (paperSize) {
            case PrinterSetting.PAPER_SIZE_TWO_INCH:
                localizeReceipts.setPaperSizeStr("2\"");
                localizeReceipts.setScalePaperSizeStr("3\"");   // 3inch -> 2inch
                break;
            case PrinterSetting.PAPER_SIZE_THREE_INCH:
            case PrinterSetting.PAPER_SIZE_ESCPOS_THREE_INCH:
            case PrinterSetting.PAPER_SIZE_DOT_THREE_INCH:
                localizeReceipts.setPaperSizeStr("3\"");
                localizeReceipts.setScalePaperSizeStr("4\"");   // 4inch -> 3inch
                break;
//            case PrinterSetting.PAPER_SIZE_FOUR_INCH :
            default:
                localizeReceipts.setPaperSizeStr("4\"");
                localizeReceipts.setScalePaperSizeStr("3\"");   // 3inch -> 4inch
                break;
        }

        localizeReceipts.setLanguage(language);
        localizeReceipts.setPaperSize(paperSize);

        return localizeReceipts;
    }

    public void appendTextReceiptData(ICommandBuilder builder, boolean utf8) {
        switch (mPaperSize) {
            case PrinterSetting.PAPER_SIZE_TWO_INCH:
                append2inchTextReceiptData(builder, utf8);
                break;
            case PrinterSetting.PAPER_SIZE_THREE_INCH:
                append3inchTextReceiptData(builder, utf8);
                break;
            case PrinterSetting.PAPER_SIZE_FOUR_INCH:
                append4inchTextReceiptData(builder, utf8);
                break;
            case PrinterSetting.PAPER_SIZE_ESCPOS_THREE_INCH:
                appendEscPos3inchTextReceiptData(builder, utf8);
                break;
//            case PrinterSetting.PAPER_SIZE_DOT_THREE_INCH:
            default:
                appendDotImpact3inchTextReceiptData(builder, utf8);
                break;
        }
    }

    public Bitmap createRasterReceiptImage(Resources resources, int resId) {
        Bitmap image;

        switch (mPaperSize) {
            case PrinterSetting.PAPER_SIZE_TWO_INCH:
                image = create2inchRasterReceiptImage();
                break;
            case PrinterSetting.PAPER_SIZE_THREE_INCH:
                image = create3inchRasterReceiptImage();
                break;
            case PrinterSetting.PAPER_SIZE_FOUR_INCH:
                image = create4inchRasterReceiptImage();
                break;
            case PrinterSetting.PAPER_SIZE_ESCPOS_THREE_INCH:
                image = createEscPos3inchRasterReceiptImage();
                break;
//            case PrinterSetting.PAPER_SIZE_DOT_THREE_INCH:
            default:
                image = createCouponImage(resources, resId);
                break;
        }

        return image;
    }

    public Bitmap createScaleRasterReceiptImage(Resources resources, int resId) {
        Bitmap image;

        switch (mPaperSize) {
            case PrinterSetting.PAPER_SIZE_TWO_INCH:
                image = create3inchRasterReceiptImage();      // 3inch -> 2inch
                break;
            case PrinterSetting.PAPER_SIZE_THREE_INCH:
            case PrinterSetting.PAPER_SIZE_ESCPOS_THREE_INCH:
                image = create4inchRasterReceiptImage();      // 4inch -> 3inch
                break;
            case PrinterSetting.PAPER_SIZE_FOUR_INCH:
                image = create3inchRasterReceiptImage();      // 3inch -> 4inch
                break;
//            case PrinterSetting.PAPER_SIZE_DOT_THREE_INCH:
            default:
                image = createCouponImage(resources, resId);
                break;
        }

        return image;
    }

    public int getLanguage() {
        return mLanguage;
    }

    public void setLanguage(int language) {
        mLanguage = language;
    }

    public void setPaperSize(int paperSize) {
        mPaperSize = paperSize;
    }

    public String getLanguageCode() {
        return mLanguageCode;
    }

    public String getPaperSizeStr() {
        return mPaperSizeStr;
    }

    public void setPaperSizeStr(String paperSizeStr) {
        mPaperSizeStr = paperSizeStr;
    }

    public String getScalePaperSizeStr() {
        return mScalePaperSizeStr;
    }

    public void setScalePaperSizeStr(String scalePaperSizeStr) {
        mScalePaperSizeStr = scalePaperSizeStr;
    }

    public CharacterCode getCharacterCode() {
        return mCharacterCode;
    }

    public abstract void append2inchTextReceiptData(ICommandBuilder builder, boolean utf8);

    public abstract void append3inchTextReceiptData(ICommandBuilder builder, boolean utf8);

    public abstract void append4inchTextReceiptData(ICommandBuilder builder, boolean utf8);

    public abstract Bitmap create2inchRasterReceiptImage();

    public abstract Bitmap create3inchRasterReceiptImage();

    public abstract Bitmap create4inchRasterReceiptImage();

    public abstract Bitmap createCouponImage(Resources resources, int resId);

    public abstract Bitmap createEscPos3inchRasterReceiptImage();

    public abstract void appendEscPos3inchTextReceiptData(ICommandBuilder builder, boolean utf8);

    public abstract void appendDotImpact3inchTextReceiptData(ICommandBuilder builder, boolean utf8);

    static public Bitmap createBitmapFromText(String printText, int textSize, int printWidth, Typeface typeface) {
        Paint paint = new Paint();
        Bitmap bitmap;
        Canvas canvas;

        paint.setTextSize(textSize);
        paint.setTypeface(typeface);

        paint.getTextBounds(printText, 0, printText.length(), new Rect());

        TextPaint textPaint = new TextPaint(paint);
        android.text.StaticLayout staticLayout = new StaticLayout(printText, textPaint, printWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

        // Create bitmap
        bitmap = Bitmap.createBitmap(staticLayout.getWidth(), staticLayout.getHeight(), Bitmap.Config.ARGB_8888);

        // Create canvas
        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.translate(0, 0);
        staticLayout.draw(canvas);

        return bitmap;
    }
}
