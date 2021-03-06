package com.android.emobilepos.report;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import com.android.emobilepos.R;
import com.android.emobilepos.adapters.ReportEndDayAdapter;
import com.android.emobilepos.models.realms.Device;
import com.android.support.DateUtils;
import com.android.support.DeviceUtils;
import com.android.support.Global;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;

import java.util.Calendar;
import java.util.Date;

import main.EMSDeviceManager;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ViewEndOfDayReport_FA extends BaseFragmentActivityActionBar implements OnClickListener {


    private static String curDate, mDate;
    private Activity activity;
    private Button btnDate;
    private Global global;
    private ProgressDialog myProgressDialog;
    private static ReportEndDayAdapter adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_end_day_layout);
        activity = this;
        global = (Global) activity.getApplication();
        curDate = DateUtils.getDateAsString(new Date(), DateUtils.DATE_yyyy_MM_ddTHH_mm_ss);
        btnDate = findViewById(R.id.btnDate);
        Button btnPrint = findViewById(R.id.btnPrint);
        btnDate.setOnClickListener(this);
        btnPrint.setOnClickListener(this);
        mDate = Global.formatToDisplayDate(curDate, 0);
        btnDate.setText(mDate);
        final StickyListHeadersListView myListview = findViewById(R.id.listView);
        myListview.setAreHeadersSticky(false);

        final ProgressDialog myProgressDialog = new ProgressDialog(activity);
        myProgressDialog.setMessage(activity.getString(R.string.loading));
        myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        myProgressDialog.setCancelable(false);
        myProgressDialog.show();

        // moved data loading to a new thread to prevent UI from freezing
        // when the databases has lots of data
        // (quick fix requested until a better refactoring is done)
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean firstLoad = adapter == null;
                if (firstLoad) {
                    adapter = new ReportEndDayAdapter(ViewEndOfDayReport_FA.this,
                            Global.formatToDisplayDate(curDate, 4), null);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (firstLoad)
                            myListview.setAdapter(adapter);
                        if (myProgressDialog.isShowing()) {
                            myProgressDialog.dismiss();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        adapter = null;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        if (global.isApplicationSentToBackground())
            Global.loggedIn = false;
        global.stopActivityTransitionTimer();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        global.startActivityTransitionTimer();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDate:
                DialogFragment dateDialog = new DateDialog();
                FragmentManager fm = getSupportFragmentManager();
                dateDialog.show(fm, "dialog");
                break;
            case R.id.btnPrint:
                showPrintDetailsDlg();
                break;
        }
    }

    private void showPrintDlg() {
        final Dialog dlog = new Dialog(activity, R.style.Theme_TransparentTest);
        dlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlog.setCancelable(true);
        dlog.setCanceledOnTouchOutside(true);
        dlog.setContentView(R.layout.dlog_btn_left_right_layout);
        TextView viewTitle = dlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = dlog.findViewById(R.id.dlogMessage);
        viewTitle.setText(R.string.dlog_title_confirm);
        viewTitle.setText(R.string.dlog_title_error);
        viewMsg.setText(R.string.dlog_msg_failed_print);
        dlog.findViewById(R.id.btnDlogCancel).setVisibility(View.GONE);
        Button btnYes = dlog.findViewById(R.id.btnDlogLeft);
        Button btnNo = dlog.findViewById(R.id.btnDlogRight);
        Button btnCancel = dlog.findViewById(R.id.btnDlogCancel);
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
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlog.dismiss();
            }
        });
        dlog.show();
    }

    private void showPrintDetailsDlg() {
        final Dialog dlog = new Dialog(activity, R.style.Theme_TransparentTest);
        dlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlog.setCancelable(true);
        dlog.setCanceledOnTouchOutside(true);
        dlog.setContentView(R.layout.dlog_btn_left_right_layout);
        TextView viewTitle = dlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = dlog.findViewById(R.id.dlogMessage);
        viewTitle.setText(R.string.dlog_title_print_details);
        viewMsg.setText(R.string.dlog_msg_print_details);
        Button btnYes = dlog.findViewById(R.id.btnDlogLeft);
        Button btnNo = dlog.findViewById(R.id.btnDlogRight);
        Button btnCancel = dlog.findViewById(R.id.btnDlogCancel);
        btnYes.setText(R.string.button_yes);
        btnNo.setText(R.string.button_no);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlog.dismiss();
            }
        });
        btnYes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
                new printAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, true);
            }
        });
        btnNo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
                new printAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
            }
        });
        dlog.show();
    }

    private class printAsync extends AsyncTask<Boolean, Void, Void> {
        private boolean printSuccessful = true;

        @Override
        protected void onPreExecute() {
            myProgressDialog = new ProgressDialog(activity);
            myProgressDialog.setMessage(getString(R.string.printing_message));
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Boolean... params) {
            EMSDeviceManager emsDeviceManager = DeviceUtils.getEmsDeviceManager(Device.Printables.REPORTS, Global.printerDevices);
            if (emsDeviceManager != null && emsDeviceManager.getCurrentDevice() != null) {
                emsDeviceManager.getCurrentDevice().printEndOfDayReport(curDate, null, params[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            myProgressDialog.dismiss();
            if (!printSuccessful)
                showPrintDlg();
        }
    }


    public static class DateDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private ViewEndOfDayReport_FA activity;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onAttach(Context context) {
            this.activity = (ViewEndOfDayReport_FA) context;
            super.onAttach(context);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar cal = Calendar.getInstance();
            cal.set(year, monthOfYear, dayOfMonth);
            curDate = DateUtils.getDateAsString(cal.getTime(), DateUtils.DATE_yyyy_MM_dd);
            adapter.setNewDate(curDate);
            mDate = DateUtils.getDateAsString(cal.getTime(), "MMM dd, yyyy");
            activity.btnDate.setText(mDate);
        }
    }

}
