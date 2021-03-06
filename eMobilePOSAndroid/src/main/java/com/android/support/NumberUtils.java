package com.android.support;

import android.text.Selection;
import android.text.TextUtils;
import android.widget.EditText;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Created by tirizar on 12/3/2015.
 */
public class NumberUtils {
    private static boolean formated = false;

    public static String cleanCurrencyFormatedNumber(String s) {
        return s.replaceAll("[^[+-]?\\d\\.]", "").trim();
    }

    public static String cleanCurrencyFormatedNumber(EditText s) {
        return cleanCurrencyFormatedNumber(s.getText().toString());
    }

    public static String cleanCurrencyFormatedNumber(StringBuilder s) {
        return cleanCurrencyFormatedNumber(s.toString());
    }

    public static void parseInputedCurrency(CharSequence s, EditText editText) {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols sym = format.getDecimalFormatSymbols();
        /*StringBuilder sb = new StringBuilder();
        sb.append("^\\").append(sym.getCurrencySymbol()).append("\\s(\\d{1,3}(\\").append(sym.getGroupingSeparator()).append("\\d{3})*|(\\d+))(");
        sb.append(sym.getDecimalSeparator()).append("\\d{2})?$");*/

        if (!formated) {
//        if (!s.toString().matches(sb.toString())) {
            String userInput = "" + s.toString().replaceAll("[^\\d]", "");
            StringBuilder cashAmountBuilder = new StringBuilder(userInput);

            while (cashAmountBuilder.length() > 3 && cashAmountBuilder.charAt(0) == '0') {
                cashAmountBuilder.deleteCharAt(0);
            }
            while (cashAmountBuilder.length() < 3) {
                cashAmountBuilder.insert(0, '0');
            }
            String currency = Global.formatDoubleToCurrency(new BigDecimal(cashAmountBuilder.toString()).multiply(new BigDecimal(.01)).doubleValue());
            formated = true;
            editText.setText(currency);
        }
        Selection.setSelection(editText.getText(), editText.getText().length());
        formated = false;
    }

    public static String removeLeadingZeros(String value) {
        if (!TextUtils.isEmpty(value)) {
            return value.replaceFirst("^0+(?!$)", "");
        }
        return value;
    }
}
