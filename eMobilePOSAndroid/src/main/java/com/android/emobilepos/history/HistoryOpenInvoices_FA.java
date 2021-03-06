package com.android.emobilepos.history;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.database.InvoicePaymentsHandler;
import com.android.database.InvoicesHandler;
import com.android.emobilepos.R;
import com.android.emobilepos.history.details.HistoryOpenInvoicesDetails_FA;
import com.android.emobilepos.models.realms.Device;
import com.android.emobilepos.payment.SelectPayMethod_FA;
import com.android.support.DeviceUtils;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import main.EMSDeviceManager;

public class HistoryOpenInvoices_FA extends BaseFragmentActivityActionBar implements OnClickListener, OnItemClickListener {

    private boolean hasBeenCreated = false;
    private Global global;
    private Activity activity;

    private CustomCursorAdapter myAdapter;
    private Cursor myCursor;

    private ListView myListView;
    private InvoicesHandler handler;
    private String chosenInvID = "";
    private String balanceDue, totalCostAmount = "";
    private boolean isFromMainMenu = false;
    private MyPreferences myPref;
    private List<String> inv_list;
    private List<String> txnIDList;
    private List<Double> total_list;
    private List<String> chosenInvIDList;
    private boolean isMultiInvoice = false;
    private ProgressDialog myProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hist_invoices_listview);
        global = (Global) getApplication();
        activity = this;

        myListView = findViewById(R.id.invoiceLV);
        TextView headerTitle = findViewById(R.id.invoicesHeaderTitle);
        Button payButton = findViewById(R.id.payInvoiceButton);
        headerTitle.setText(getResources().getString(R.string.hist_open_inv));
        this.handler = new InvoicesHandler(this);


        Bundle extras = getIntent().getExtras();
        if (extras != null)
            isFromMainMenu = extras.getBoolean("isFromMainMenu", false);
        myPref = new MyPreferences(this);

        if (!isFromMainMenu) {
            if (myCursor != null) {
                myCursor.close();
            }
            myCursor = handler.getInvoicesList();
            payButton.setVisibility(View.INVISIBLE);
        } else {
            if (myCursor != null) {
                myCursor.close();
            }
            myCursor = handler.getListSpecificInvoice(myPref.getCustID());
        }

        EditText field = findViewById(R.id.searchField);
        field.setOnEditorActionListener(getEditorActionListener());
        field.addTextChangedListener(getTextChangedListener());

        payButton.setOnClickListener(this);

        myAdapter = new CustomCursorAdapter(this, myCursor, CursorAdapter.NO_SELECTION);
        myListView.setOnItemClickListener(this);
        myListView.setAdapter(myAdapter);

        hasBeenCreated = true;
    }

    @Override
    public void onResume() {

        if (global.isApplicationSentToBackground())
            Global.loggedIn = false;
        global.stopActivityTransitionTimer();

        if (hasBeenCreated && !Global.loggedIn) {
            if (global.getGlobalDlog() != null)
                global.getGlobalDlog().dismiss();
            global.promptForMandatoryLogin(activity);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
//        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//        boolean isScreenOn = powerManager.isScreenOn();
//        if (!isScreenOn && myPref.isExpireUserSession())
//            Global.loggedIn = false;
        global.startActivityTransitionTimer();
    }

    @Override
    public void onDestroy() {
        if (myCursor != null && !myCursor.isClosed()) {
            myCursor.close();
        }
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        myCursor.moveToPosition(position);
        if (myCursor.getString(myCursor.getColumnIndex("inv_ispaid"))
                .equals("0"))
            showPrintDlg(position, false);
        else
            showPrintDlg(position, true);
    }

    @Override
    public void onClick(View v) {
        int size = myAdapter.getCheckedItemSize();
        if (size > 0) {
            isMultiInvoice = true;
            intentMultiplePayment(-1);
        } else
            Toast.makeText(this, "Please select an Invoice...", Toast.LENGTH_LONG).show();
    }


    private TextWatcher getTextChangedListener() {

        return new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence s, int arg1, int arg2,
                                      int arg3) {
                String test = s.toString().trim();
                if (test.isEmpty()) {
                    if (myCursor != null) {
                        myCursor.close();
                    }

                    if (!isFromMainMenu)
                        myCursor = handler.getInvoicesList();
                    else
                        myCursor = handler.getListSpecificInvoice(myPref.getCustID());

                    myAdapter = new CustomCursorAdapter(activity, myCursor, CursorAdapter.NO_SELECTION);
                    myListView.setAdapter(myAdapter);
                }
            }
        };
    }

    private OnEditorActionListener getEditorActionListener() {

        return new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String text = v.getText().toString().trim();
                    if (!text.isEmpty())
                        performSearch(text);
                    return true;
                }
                return false;
            }
        };
    }


    private void intentSinglePayment(int position) {
        myCursor.moveToPosition(position);
        Intent intent = new Intent(this, SelectPayMethod_FA.class);
        intent.putExtra("histinvoices", true);
        intent.putExtra("isMultipleInvoice", isMultiInvoice);
        chosenInvID = myCursor.getString(myCursor.getColumnIndex("_id"));
        String txnIDChosen = myCursor.getString(myCursor.getColumnIndex("txnID"));
        totalCostAmount = myCursor.getString(myCursor.getColumnIndex("inv_total"));
        balanceDue = myCursor.getString(myCursor.getColumnIndex("inv_balance"));

        intent.putExtra("cust_id", myCursor.getString(myCursor.getColumnIndex("cust_id")));
        intent.putExtra("custidkey", myCursor.getString(myCursor.getColumnIndex("custidkey")));
        intent.putExtra("inv_id", txnIDChosen);
        intent.putExtra("job_id", txnIDChosen);
        intent.putExtra("typeOfProcedure", Global.OrderType.INVOICE);
        intent.putExtra("ord_type", Global.OrderType.INVOICE);

        String temp = myCursor.getString(myCursor
                .getColumnIndex("inv_balance"));
        intent.putExtra("amount", temp);

        temp = Double.toString(Global.formatNumFromLocale(Global.addSubsStrings(false, Global.formatNumToLocale(Double.parseDouble(totalCostAmount)), Global.formatNumToLocale(Double.parseDouble(balanceDue)))));
        intent.putExtra("paid", temp);

        startActivityForResult(intent, Global.FROM_OPEN_INVOICES);
    }

    private void intentMultiplePayment(int position) {
        myAdapter.getCheckedItems();
        Intent intent = new Intent(this, SelectPayMethod_FA.class);
        intent.putExtra("histinvoices", true);
        intent.putExtra("isMultipleInvoice", isMultiInvoice);

        String[] inv_array;
        Double[] balance_array;
        String[] txnID_array;

        if (inv_list.size() == 0) {
            myCursor.moveToPosition(position);
            chosenInvIDList.add(myCursor.getString(myCursor.getColumnIndex("_id")));
            txnIDList.add(myCursor.getString(myCursor.getColumnIndex("txnID")));
            inv_list.add(myCursor.getString(myCursor.getColumnIndex("_id")));
            String tempBalanceDue = myCursor.getString(myCursor.getColumnIndex("inv_balance"));
            total_list.add(Double.parseDouble(tempBalanceDue));

            totalCostAmount = myCursor.getString(myCursor.getColumnIndex("inv_total"));
            balanceDue = tempBalanceDue;

            intent.putExtra("cust_id", myCursor.getString(myCursor.getColumnIndex("cust_id")));
            intent.putExtra("custidkey", myCursor.getString(myCursor.getColumnIndex("custidkey")));

            inv_array = new String[inv_list.size()];
            balance_array = new Double[total_list.size()];
            txnID_array = new String[txnIDList.size()];
        } else {

            intent.putExtra("cust_id", myPref.getCustID());
            intent.putExtra("custidkey", myPref.getCustIDKey());
            inv_array = new String[inv_list.size()];
            balance_array = new Double[total_list.size()];
            txnID_array = new String[txnIDList.size()];
        }


        inv_list.toArray(inv_array);
        total_list.toArray(balance_array);
        txnIDList.toArray(txnID_array);

        intent.putExtra("inv_id_array", inv_array);
        intent.putExtra("txnID_array", txnID_array);
        intent.putExtra("balance_array", toPrimitiveDouble(balance_array));


        String tempBalance = balanceDue;
        intent.putExtra("amount", tempBalance);

        tempBalance = Double.toString(Global.formatNumFromLocale(Global.addSubsStrings(false, Global.formatNumToLocale(Double.parseDouble(totalCostAmount)), Global.formatNumToLocale(Double.parseDouble(balanceDue)))));
        intent.putExtra("paid", tempBalance);
//        intent.putExtra("typeOfProcedure", Global.FROM_JOB_INVOICE);
        intent.putExtra("typeOfProcedure", Global.OrderType.INVOICE);
        intent.putExtra("ord_type", Global.OrderType.INVOICE);

        startActivityForResult(intent, Global.FROM_OPEN_INVOICES);
    }


    private void showPrintDlg(final int pos, boolean isPaid) {
        myCursor.moveToPosition(pos);
        final Dialog dlog = new Dialog(activity, R.style.Theme_TransparentTest);
        dlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlog.setCancelable(true);
        dlog.setCanceledOnTouchOutside(true);
        dlog.setContentView(R.layout.dlog_btn_left_right_layout);

        TextView viewTitle = dlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = dlog.findViewById(R.id.dlogMessage);
        viewTitle.setText(R.string.dlog_title_choose_action);
        viewMsg.setVisibility(View.GONE);
        Button btnPrint = dlog.findViewById(R.id.btnDlogLeft);
        Button btnPay = dlog.findViewById(R.id.btnDlogRight);
        Button btnCancel = dlog.findViewById(R.id.btnDlogCancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlog.dismiss();
            }
        });
        dlog.findViewById(R.id.btnDlogCancel).setVisibility(View.GONE);

        btnPrint.setText(R.string.button_print);
        btnPay.setText(R.string.button_pay);
        if (isPaid)
            btnPay.setVisibility(View.GONE);

        btnPrint.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
                new printAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myCursor.getString(myCursor.getColumnIndex("_id")));

            }
        });
        btnPay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
                intentSinglePayment(pos);
            }
        });
        dlog.show();
    }


    private void showReprintDlg(final String _id, boolean isReprint) {
        final Dialog dlog = new Dialog(activity, R.style.Theme_TransparentTest);
        dlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlog.setCancelable(false);
        dlog.setContentView(R.layout.dlog_btn_left_right_layout);

        TextView viewTitle = dlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = dlog.findViewById(R.id.dlogMessage);
        if (isReprint) {
            viewTitle.setText(R.string.dlog_title_confirm);
            viewMsg.setText(R.string.dlog_msg_want_to_reprint);
        } else {
            viewTitle.setText(R.string.dlog_title_error);
            viewMsg.setText(R.string.dlog_msg_failed_print);
        }


        dlog.findViewById(R.id.btnDlogCancel).setVisibility(View.GONE);
        Button btnYes = dlog.findViewById(R.id.btnDlogLeft);
        Button btnNo = dlog.findViewById(R.id.btnDlogRight);
        btnYes.setText(R.string.button_yes);
        btnNo.setText(R.string.button_no);

        btnYes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlog.dismiss();
                new printAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, _id);
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

    private double[] toPrimitiveDouble(Double[] val) {
        int size = val.length;
        double[] tempArray = new double[size];
        for (int i = 0; i < size; i++) {
            tempArray[i] = val[i];
        }
        return tempArray;
    }

    public void performSearch(String text) {
        if (myCursor != null)
            myCursor.close();

        myCursor = this.handler.getSearchedInvoicesList(text, isFromMainMenu);

        myAdapter = new CustomCursorAdapter(this, myCursor,
                CursorAdapter.NO_SELECTION);
        myListView.setAdapter(myAdapter);

    }

    public String format(String text) {
        DecimalFormat frmt = new DecimalFormat("0.00");
        if (TextUtils.isEmpty(text))
            return "0.00";
        return frmt.format(Double.parseDouble(text));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Global.FROM_PAYMENT || resultCode == 3) {
            InvoicesHandler invHandler = new InvoicesHandler(this);
            if (!isMultiInvoice) {

                String t = Global.addSubsStrings(false, Global.formatNumToLocale(Double.parseDouble(balanceDue)), Global.formatNumToLocale(Global.overallPaidAmount));
                Global.overallPaidAmount = 0;


                double remainingBalance = Global.formatNumFromLocale(t);

                if (remainingBalance <= 0) {
                    // has been paid in total
                    invHandler.updateIsPaid(true, chosenInvID, null);
                } else {
                    // hasn't been paid in total
                    invHandler.updateIsPaid(false, chosenInvID, Double.toString(remainingBalance));
                }
            } else {
                InvoicePaymentsHandler invPayHandler = new InvoicePaymentsHandler(this);
                double invPaid;
                int size = inv_list.size();
                double remainingBalance;
                for (int i = 0; i < size; i++) {
                    invPaid = invPayHandler.getTotalPaidAmount(inv_list.get(i));
                    if (invPaid != -1) {
                        remainingBalance = total_list.get(i) - invPaid;
                        if (remainingBalance <= 0)
                            invHandler.updateIsPaid(true, chosenInvIDList.get(i), null);
                        else
                            invHandler.updateIsPaid(false, chosenInvIDList.get(i), Double.toString(remainingBalance));
                    }
                }

            }
            finish();
        } else if (resultCode == Global.FROM_OPEN_INVOICES_DETAILS) {
            finish();
        } else
            myAdapter.notifyDataSetChanged();
    }

    private class printAsync extends AsyncTask<String, String, String> {
        private String _inv_id = "";
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
        protected String doInBackground(String... params) {
            _inv_id = params[0];
            EMSDeviceManager emsDeviceManager = DeviceUtils.getEmsDeviceManager(Device.Printables.PAYMENT_RECEIPT_REPRINT, Global.printerDevices);
            if (emsDeviceManager != null && emsDeviceManager.getCurrentDevice() != null)
                printSuccessful = emsDeviceManager.getCurrentDevice().printOpenInvoices(_inv_id);

            return null;
        }

        @Override
        protected void onPostExecute(String unused) {
            myProgressDialog.dismiss();
            if (!printSuccessful) {
                showReprintDlg(_inv_id, false);
            } else if (myPref.isMultiplePrints()) {
                showReprintDlg(_inv_id, true);
            }
        }
    }

    public class CustomCursorAdapter extends CursorAdapter {
        LayoutInflater inflater;
        boolean isPaidDivider = false;
        boolean isOpenedDivider = false;

        boolean insideOpened = false;
        boolean insidePaid = false;
        SparseBooleanArray mSparseBooleanArray;
        OnCheckedChangeListener mCheckedChangeListener = new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                mSparseBooleanArray.put((Integer) buttonView.getTag(), isChecked);

            }

        };

        public CustomCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            inflater = LayoutInflater.from(context);
            mSparseBooleanArray = new SparseBooleanArray();
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            StringBuilder sb = new StringBuilder();
            TextView clientName = view
                    .findViewById(R.id.invoiceClientName);
            TextView txnID = view.findViewById(R.id.invoiceTXNID);
            final TextView uid = view.findViewById(R.id.invoiceUID);
            TextView createdDate = view
                    .findViewById(R.id.invoiceCreatedContent);
            TextView dueDate = view
                    .findViewById(R.id.invoiceDueContent);
            TextView shipDate = view
                    .findViewById(R.id.invoiceShipContent);
            TextView total = view
                    .findViewById(R.id.invoiceTotalContent);
            TextView balance = view
                    .findViewById(R.id.invoiceBalanceContent);
            TextView isPaidTag = view
                    .findViewById(R.id.invoicePaidTitle);

            ImageView moreDetails = view
                    .findViewById(R.id.invoiceMoreDetailIcon);

            CheckBox checkBox = view.findViewById(R.id.invoiceCheckBox);
            if (isFromMainMenu) {

                checkBox.setTag(cursor.getPosition());
                checkBox.setChecked(mSparseBooleanArray.get(cursor.getPosition()));
                checkBox.setOnCheckedChangeListener(mCheckedChangeListener);
            } else
                checkBox.setVisibility(View.INVISIBLE);


            if (clientName != null && txnID != null && uid != null
                    && createdDate != null && dueDate != null
                    && shipDate != null && total != null && balance != null) {
                clientName.setText(cursor.getString(cursor
                        .getColumnIndex("cust_name")));
                uid.setText(cursor.getString(cursor.getColumnIndex("_id")));
                sb.append("(txnID: ")
                        .append(cursor.getString(cursor.getColumnIndex("txnID")))
                        .append(")");
                txnID.setText(sb.toString());
                createdDate.setText(Global.formatToDisplayDate(cursor
                                .getString(cursor.getColumnIndex("inv_timecreated")),
                        0));
                dueDate.setText(Global.formatToDisplayDate(
                        cursor.getString(cursor.getColumnIndex("inv_duedate")),
                        0));
                shipDate.setText(Global.formatToDisplayDate(
                        cursor.getString(cursor.getColumnIndex("inv_shipdate")),
                        0));

                double tempVal = Double.parseDouble(cursor.getString(cursor
                        .getColumnIndex("inv_total")));
                total.setText(Global.getCurrencyFormat(Global
                        .formatNumToLocale(tempVal)));
                tempVal = Double.parseDouble(cursor.getString(cursor
                        .getColumnIndex("inv_balance")));
                balance.setText(Global.getCurrencyFormat(Global
                        .formatNumToLocale(tempVal)));


                moreDetails.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(activity, HistoryOpenInvoicesDetails_FA.class);
                        intent.putExtra("uid", uid.getText().toString());
                        chosenInvID = uid.getText().toString();
                        startActivityForResult(intent, 0);
                    }
                });

                if (cursor.getString(cursor.getColumnIndex("inv_ispaid"))
                        .equals("0"))
                    isPaidTag.setVisibility(View.INVISIBLE);
                else {
                    checkBox.setVisibility(View.INVISIBLE);
                    isPaidTag.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public int getCount() {
            return myCursor.getCount(); // plus the 2 dividers
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.hist_invoices_lvadapter,
                    parent, false);
        }

        public void getCheckedItems() {
            inv_list = new ArrayList<String>();
            txnIDList = new ArrayList<String>();
            total_list = new ArrayList<Double>();
            chosenInvIDList = new ArrayList<String>();

            int size = myCursor.getCount();
            myCursor.moveToFirst();
            int i_id = myCursor.getColumnIndex("_id");
            int i_txnid = myCursor.getColumnIndex("txnID");
            int i_balance = myCursor.getColumnIndex("inv_balance");
            int i_total = myCursor.getColumnIndex("inv_total");
            double totalAmount = 0.0;
            double totalBalance = 0.0;
            String temp;
            for (int i = 0; i < size; i++) {
                if (mSparseBooleanArray.get(i)) {
                    chosenInvIDList.add(myCursor.getString(i_id));
                    txnIDList.add(myCursor.getString(i_txnid));
                    inv_list.add(myCursor.getString(i_id));
                    temp = myCursor.getString(i_balance);
                    total_list.add(Double.parseDouble(temp));
                    totalAmount += Double.parseDouble(myCursor.getString(i_total));
                    totalBalance += Double.parseDouble(temp);
                }
                myCursor.moveToNext();
            }


            totalCostAmount = Double.toString(totalAmount);
            balanceDue = Double.toString(totalBalance);
        }

        public int getCheckedItemSize() {
            int size = myCursor.getCount();
            int counter = 0;
            for (int i = 0; i < size; i++) {
                if (mSparseBooleanArray.get(i)) {
                    counter++;
                }
            }
            return counter;
        }

    }

}
