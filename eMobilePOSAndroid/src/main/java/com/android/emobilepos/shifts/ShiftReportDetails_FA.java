package com.android.emobilepos.shifts;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.android.dao.ClerkDAO;
import com.android.dao.ShiftDAO;
import com.android.dao.ShiftExpensesDAO;
import com.android.emobilepos.R;
import com.android.emobilepos.models.realms.Clerk;
import com.android.emobilepos.models.realms.Shift;
import com.android.emobilepos.models.realms.ShiftExpense;
import com.android.support.Global;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;

import java.math.BigDecimal;

public class ShiftReportDetails_FA extends BaseFragmentActivityActionBar implements View.OnClickListener {

    private Global global;
    private ProgressDialog myProgressDialog;
    private Activity activity;
    private boolean hasBeenCreated = false;
    private String shiftID;
    private Shift shift;
    private BigDecimal totalExpenses, safeDropTotal, cashDropTotal, cashInTotal, buyGoodsTotal, nonCashGratuityTotal;
    private Clerk clerk;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.shift_details_layout);
        global = (Global) getApplication();
        Button btnPrint = findViewById(R.id.btnPrint);
        btnPrint.setOnClickListener(this);
        Bundle extras = this.getIntent().getExtras();
        shiftID = extras.getString("shift_id");
        shift = ShiftDAO.getShift(shiftID);
        clerk = ClerkDAO.getByEmpId(shift.getClerkId());
        totalExpenses = ShiftExpensesDAO.getShiftTotalExpenses(shiftID);
        safeDropTotal = ShiftExpensesDAO.getShiftTotalExpenses(shiftID, ShiftExpense.ExpenseProductId.SAFE_DROP);
        cashDropTotal = ShiftExpensesDAO.getShiftTotalExpenses(shiftID, ShiftExpense.ExpenseProductId.CASH_DROP);
        cashInTotal = ShiftExpensesDAO.getShiftTotalExpenses(shiftID, ShiftExpense.ExpenseProductId.CASH_IN);
        buyGoodsTotal = ShiftExpensesDAO.getShiftTotalExpenses(shiftID, ShiftExpense.ExpenseProductId.BUY_GOODS_SERVICES);
        nonCashGratuityTotal = ShiftExpensesDAO.getShiftTotalExpenses(shiftID, ShiftExpense.ExpenseProductId.NON_CASH_GRATUITY);
        hasBeenCreated = true;
        if (shift != null) {
            loadUIInfo();
        }
    }

    private void loadUIInfo() {
        ((TextView) findViewById(R.id.salesClerktextView26)).setText(clerk.getEmpName());
        ((TextView) findViewById(R.id.beginningPettyCashtextView26)).setText(Global.getCurrencyFormat(shift.getBeginningPettyCash()));
        ((TextView) findViewById(R.id.totalExpensestextView26)).setText(Global.getCurrencyFormat(String.valueOf(totalExpenses)));
//        ((TextView) findViewById(R.id.endingPettyCashtextView26)).setText(Global.formatDoubleStrToCurrency(shift.getEndingPettyCash()));
        ((TextView) findViewById(R.id.totalTransactionCashtextView26)).setText(Global.getCurrencyFormat(shift.getTotalTransactionsCash()));
        ((TextView) findViewById(R.id.totalEndingCashtextView26)).setText(Global.getCurrencyFormat(shift.getTotal_ending_cash()));
        ((TextView) findViewById(R.id.enteredCloseAmounttextView26)).setText(Global.getCurrencyFormat(shift.getEnteredCloseAmount()));
        ((TextView) findViewById(R.id.shortOverAmounttextView)).setText(Global.getCurrencyFormat(shift.getOver_short()));
        ((TextView) findViewById(R.id.safeDropExpensestextView)).setText(Global.getCurrencyFormat(String.valueOf(safeDropTotal)));
        ((TextView) findViewById(R.id.cashDropExpensestextView2)).setText(Global.getCurrencyFormat(String.valueOf(cashDropTotal)));
        ((TextView) findViewById(R.id.cashInExpensestextView4)).setText(Global.getCurrencyFormat(String.valueOf(cashInTotal)));
        ((TextView) findViewById(R.id.buyGoodsServicesExpensestextView6)).setText(Global.getCurrencyFormat(String.valueOf(buyGoodsTotal)));
        ((TextView) findViewById(R.id.nonCashGratuityExpensestextVie8)).setText(Global.getCurrencyFormat(String.valueOf(nonCashGratuityTotal)));

    }

    @Override
    public void onResume() {

        if (global.isApplicationSentToBackground())
            Global.loggedIn = false;
        global.stopActivityTransitionTimer();

        if (hasBeenCreated && !Global.loggedIn) {
            if (global.getGlobalDlog() != null)
                global.getGlobalDlog().dismiss();
            global.promptForMandatoryLogin(this);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        boolean isScreenOn = powerManager.isScreenOn();
        if (!isScreenOn)
            Global.loggedIn = false;
        global.startActivityTransitionTimer();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPrint:
                new printAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
        }
    }

    private void showPrintDlg() {
        final Dialog dlog = new Dialog(activity, R.style.Theme_TransparentTest);
        dlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlog.setCancelable(false);
        dlog.setContentView(R.layout.dlog_btn_left_right_layout);

        TextView viewTitle = dlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = dlog.findViewById(R.id.dlogMessage);
        viewTitle.setText(R.string.dlog_title_confirm);

        viewTitle.setText(R.string.dlog_title_error);
        viewMsg.setText(R.string.dlog_msg_failed_print);

        dlog.findViewById(R.id.btnDlogCancel).setVisibility(View.GONE);

        Button btnYes = dlog.findViewById(R.id.btnDlogLeft);
        Button btnNo = dlog.findViewById(R.id.btnDlogRight);
        btnYes.setText(R.string.button_yes);
        btnNo.setText(R.string.button_no);

        btnYes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
                new printAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        btnNo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
            }
        });
        dlog.show();
    }

    private class printAsync extends AsyncTask<Void, Void, Void> {
        private boolean printSuccessful = true;

        @Override
        protected void onPreExecute() {
            myProgressDialog = new ProgressDialog(activity);
            myProgressDialog.setMessage("Printing...");
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Global.mainPrinterManager != null && Global.mainPrinterManager.getCurrentDevice() != null)
                Global.mainPrinterManager.getCurrentDevice().printShiftDetailsReport(shiftID);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            myProgressDialog.dismiss();
            if (!printSuccessful)
                showPrintDlg();
        }
    }

}
