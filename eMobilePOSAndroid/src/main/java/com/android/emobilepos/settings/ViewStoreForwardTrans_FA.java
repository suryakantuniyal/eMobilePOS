package com.android.emobilepos.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.android.dao.StoredPaymentsDAO;
import com.android.database.OrdersHandler;
import com.android.database.PaymentsHandler;
import com.android.emobilepos.R;
import com.android.emobilepos.models.realms.Payment;
import com.android.emobilepos.models.realms.StoreAndForward;
import com.android.emobilepos.storedforward.BoloroPayment;
import com.android.payments.EMSPayGate_Default;
import com.android.saxhandler.SAXProcessCardPayHandler;
import com.android.support.GenerateNewID;
import com.android.support.GenerateNewID.IdType;
import com.android.support.Global;
import com.android.support.NetworkUtils;
import com.android.support.Post;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;
import com.crashlytics.android.Crashlytics;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import io.realm.Realm;
import io.realm.RealmResults;
import util.json.UIUtils;

public class ViewStoreForwardTrans_FA extends BaseFragmentActivityActionBar implements OnItemClickListener, OnClickListener {
    private Activity activity;
    private Global global;
    private boolean hasBeenCreated = false;
    private CustomCursorAdapter adapter;
    private RealmResults<StoreAndForward> storeAndForwards;
    private Button btnProcessAll;
    private Realm realm;
    private boolean livePaymentRunning = false;
    private ProgressDialog myProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_store_forward_trans_layout);
        activity = this;
        global = (Global) getApplication();
        realm = Realm.getDefaultInstance();
        btnProcessAll = findViewById(R.id.btnProcessAll);
        btnProcessAll.setOnClickListener(this);
        btnProcessAll.setEnabled(true);
        RecyclerView listView = findViewById(R.id.listView);
        storeAndForwards = realm.where(StoreAndForward.class).findAll();
        adapter = new CustomCursorAdapter(storeAndForwards);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        listView.setLayoutManager(mLayoutManager);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setAdapter(adapter);
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
            global.promptForMandatoryLogin(this);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        global.startActivityTransitionTimer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnProcessAll:
                btnProcessAll.setEnabled(false);
                if (UIUtils.singleOnClick(v)) {
                    new ProcessLivePaymentAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                break;
        }
    }

    private class ProcessLivePaymentAsync extends AsyncTask<Void, Void, Void> {
        private HashMap<String, String> boloroHashMap = new HashMap<>();
        private HashMap<String, String> parsedMap = new HashMap<>();
        private int _count_decline = 0, _count_conn_error = 0, _count_merch_account = 0;

        private void checkPaymentStatus(StoreAndForward storeAndForward, String verify_payment_xml, String charge_xml) throws SAXException, ParserConfigurationException, IOException {
            OrdersHandler dbOrdHandler = new OrdersHandler(activity);
            Post httpClient = new Post(activity);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXProcessCardPayHandler handler = new SAXProcessCardPayHandler();
            String xml = httpClient.postData(13, verify_payment_xml);

            if (xml.equals(Global.TIME_OUT) || xml.equals(Global.NOT_VALID_URL) || xml.isEmpty()) {
                //do nothing
                _count_conn_error++;
            } else {
                InputSource inSource = new InputSource(new StringReader(xml));

                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                xr.setContentHandler(handler);
                xr.parse(inSource);
                parsedMap = handler.getData();

                if (parsedMap != null && parsedMap.size() > 0) {
                    String _job_id = storeAndForward.getPayment().getJob_id();
//                    String _pay_uuid = storeAndForward.getPayment().getPay_uuid();//myCursor.getString(myCursor.getColumnIndex("pay_uuid"));

                    if (parsedMap.get("epayStatusCode").equals("APPROVED")) {
                        //Create Payment and delete from StoredPayment
                        saveApprovedPayment(storeAndForward, parsedMap);
                        //Remove as pending stored & forward if no more payments are pending to be processed.
                        if (StoredPaymentsDAO.getCountPendingStoredPayments(_job_id) <= 0)
                            dbOrdHandler.updateOrderStoredFwd(_job_id, "0");
                    } else if (parsedMap.get("epayStatusCode").equals("DECLINE")) {
                        if (parsedMap.get("statusCode").equals("102")) {
                            _count_merch_account++;
                            StoredPaymentsDAO.updateStoredPaymentForRetry(storeAndForward);
                        } else {
                            //remove from StoredPayment and change order to Invoice
                            StringBuilder sb = new StringBuilder();
                            sb.append(dbOrdHandler.getColumnValue("ord_comment", _job_id)).append("  ");
                            sb.append("(Card Holder: ").append(storeAndForward.getPayment().getPay_name());
                            sb.append("; Last 4: ").append(storeAndForward.getPayment().getCcnum_last4());
                            sb.append("; Exp date: ").append(storeAndForward.getPayment().getPay_expmonth());
                            sb.append("/").append(storeAndForward.getPayment().getPay_expyear());
                            sb.append("; Status Msg: ").append(parsedMap.get("statusMessage"));
                            sb.append("; Status Code: ").append(parsedMap.get("statusCode"));
                            sb.append("; TransID: ").append(parsedMap.get("CreditCardTransID"));
                            sb.append("; Auth Code: ").append(parsedMap.get("AuthorizationCode")).append(")");

                            StoredPaymentsDAO.updateStatusDeleted(storeAndForward);
                            if (dbOrdHandler.getColumnValue("ord_type", _job_id).equals(Global.OrderType.SALES_RECEIPT.getCodeString()))
                                dbOrdHandler.updateOrderTypeToInvoice(_job_id);
                            dbOrdHandler.updateOrderComment(_job_id, sb.toString());
                            //Remove as pending stored & forward if no more payments are pending to be processed.
                            if (StoredPaymentsDAO.getCountPendingStoredPayments(_job_id) <= 0)
                                dbOrdHandler.updateOrderStoredFwd(_job_id, "0");

                            _count_decline++;
                        }
                    } else {
                        //Payment doesn't exist try to process the payment
                        processPayment(storeAndForward, charge_xml);
                    }
                } else {
                    //mark StoredPayment for retry
                    _count_conn_error++;
                }
            }
        }

        private void processPayment(StoreAndForward storeAndForward, String charge_xml) throws ParserConfigurationException, SAXException, IOException {
            Realm realm = Realm.getDefaultInstance();
            OrdersHandler dbOrdHandler = new OrdersHandler(activity);
            Post httpClient = new Post(activity);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXProcessCardPayHandler handler = new SAXProcessCardPayHandler();
            String xml = httpClient.postData(13, charge_xml);
            if (xml.equals(Global.TIME_OUT) || xml.equals(Global.NOT_VALID_URL) || xml.isEmpty()) {
                //mark StoredPayment for retry
                StoredPaymentsDAO.updateStoreForwardPaymentToRetry(storeAndForward);
                _count_conn_error++;
            } else {
                InputSource inSource = new InputSource(new StringReader(xml));

                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                xr.setContentHandler(handler);
                xr.parse(inSource);
                parsedMap = handler.getData();
                if (parsedMap != null && parsedMap.size() > 0) {
                    if (parsedMap.get("epayStatusCode").equals("APPROVED")) {
                        //Create Payment and delete from StoredPayment
                        String job_id = storeAndForward.getPayment().getJob_id();
                        saveApprovedPayment(storeAndForward, parsedMap);

                        if (StoredPaymentsDAO.getCountPendingStoredPayments(job_id) <= 0)
                            dbOrdHandler.updateOrderStoredFwd(job_id, "0");
                    } else if (parsedMap.get("epayStatusCode").equals("DECLINE")) {
                        if (parsedMap.get("statusCode").equals("102")) {
                            _count_merch_account++;
                            StoredPaymentsDAO.updateStoreForwardPaymentToRetry(storeAndForward);
                        } else {
                            //remove from StoredPayment and change order to Invoice
                            StringBuilder sb = new StringBuilder();
                            sb.append(dbOrdHandler.getColumnValue("ord_comment", storeAndForward.getPayment().getJob_id())).append("  ");
                            sb.append("(Card Holder: ").append(storeAndForward.getPayment().getPay_name());
                            sb.append("; Last 4: ").append(storeAndForward.getPayment().getCcnum_last4());
                            sb.append("; Exp date: ").append(storeAndForward.getPayment().getPay_expmonth());
                            sb.append("/").append(storeAndForward.getPayment().getPay_expyear());
                            sb.append("; Status Msg: ").append(parsedMap.get("statusMessage"));
                            sb.append("; Status Code: ").append(parsedMap.get("statusCode"));
                            sb.append("; TransID: ").append(parsedMap.get("CreditCardTransID"));
                            sb.append("; Auth Code: ").append(parsedMap.get("AuthorizationCode")).append(")");
                            StoreAndForward norealmStrFwd = realm.copyFromRealm(storeAndForward);
                            if (dbOrdHandler.getColumnValue("ord_type", norealmStrFwd.getPayment().getJob_id())
                                    .equals(Global.OrderType.SALES_RECEIPT.getCodeString()))
                                dbOrdHandler.updateOrderTypeToInvoice(norealmStrFwd.getPayment().getJob_id());
                            dbOrdHandler.updateOrderComment(norealmStrFwd.getPayment().getJob_id(), sb.toString());
                            StoredPaymentsDAO.updateStatusDeleted(storeAndForward);
                            //Remove as pending stored & forward if no more payments are pending to be processed.
                            if (StoredPaymentsDAO.getCountPendingStoredPayments(norealmStrFwd.getPayment().getJob_id()) <= 0)
                                dbOrdHandler.updateOrderStoredFwd(norealmStrFwd.getPayment().getJob_id(), "0");
                            insertDeclinedPayment(norealmStrFwd.getPayment());

                            _count_decline++;
                        }
                    } else {
                        //mark StoredPayment for retry
                        StoredPaymentsDAO.updateStoreForwardPaymentToRetry(storeAndForward);
                    }
                } else {
                    //mark StoredPayment for retry
                    StoredPaymentsDAO.updateStoreForwardPaymentToRetry(storeAndForward);
                    _count_conn_error++;
                }
            }
        }

        @Override
        protected void onPreExecute() {
            myProgressDialog = new ProgressDialog(activity);
            myProgressDialog.setMessage(getString(R.string.processing_payments));
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            myProgressDialog.setCancelable(false);
            myProgressDialog.setProgress(0);
            myProgressDialog.show();

            _count_merch_account = 0;
            _count_conn_error = 0;
            _count_decline = 0;

        }

        @Override
        protected Void doInBackground(Void... params) {
            Realm realm = Realm.getDefaultInstance();
            try {
                if (NetworkUtils.isConnectedToInternet(activity) && !livePaymentRunning) {
                    realm.beginTransaction();
                    storeAndForwards = realm.where(StoreAndForward.class).findAll();
                    myProgressDialog.setMax(storeAndForwards.size());
                    realm.commitTransaction();
                    int i = 0;
                    for (StoreAndForward storeAndForward : storeAndForwards) {
                        myProgressDialog.setProgress(i);
                        i++;
                        if (!livePaymentRunning) {
                            livePaymentRunning = true;
                            String _charge_xml = storeAndForward.getPaymentXml();
                            String _verify_payment_xml = _charge_xml.replaceAll("<action>.*?</action>", "<action>" + EMSPayGate_Default.getPaymentAction("CheckTransactionStatus") + "</action>");

                            try {
                                if (storeAndForward.isRetry()) {
                                    switch (storeAndForward.getPaymentType()) {
                                        case BOLORO:
                                            boloroHashMap = BoloroPayment.executeNFCCheckout(activity, storeAndForward.getPaymentXml(), storeAndForward.getPayment());
                                            if (boloroHashMap != null && boloroHashMap.containsKey("next_action")
                                                    && boloroHashMap.get("next_action").equals("SUCCESS")) {
                                                //Create Payment and delete from StoredPayment
                                                String job_id = storeAndForward.getPayment().getJob_id();
                                                OrdersHandler dbOrdHandler = new OrdersHandler(activity);
                                                StoredPaymentsDAO.updateStatusDeleted(storeAndForward);
                                                //Remove as pending stored & forward if no more payments are pending to be processed.
                                                if (StoredPaymentsDAO.getCountPendingStoredPayments(job_id) <= 0)
                                                    dbOrdHandler.updateOrderStoredFwd(job_id, "0");
                                            } else {
                                                if (TextUtils.isEmpty(storeAndForward.getPayment().getJob_id())) {
                                                    insertDeclinedPayment(storeAndForward.getPayment());
                                                } else {
                                                    BoloroPayment.seveBoloroAsInvoice(activity, storeAndForward, boloroHashMap);
                                                }
                                            }
                                            break;
                                        case CREDIT_CARD:
                                        case GIFT_CARD:
                                            checkPaymentStatus(storeAndForward, _verify_payment_xml, _charge_xml);
                                            break;
                                    }
                                } else {
                                    switch (storeAndForward.getPaymentType()) {
                                        case BOLORO:
                                            boloroHashMap = BoloroPayment.executeNFCCheckout(activity, storeAndForward.getPaymentXml(), storeAndForward.getPayment());
                                            if (boloroHashMap != null && boloroHashMap.containsKey("next_action")
                                                    && boloroHashMap.get("next_action").equals("SUCCESS")) {
                                                //Create Payment and delete from StoredPayment
                                                String job_id = storeAndForward.getPayment().getJob_id();
                                                StoredPaymentsDAO.updateStatusDeleted(storeAndForward);
                                                OrdersHandler dbOrdHandler = new OrdersHandler(activity);
                                                //Remove as pending stored & forward if no more payments are pending to be processed.
                                                if (StoredPaymentsDAO.getCountPendingStoredPayments(job_id) <= 0)
                                                    dbOrdHandler.updateOrderStoredFwd(job_id, "0");
                                            } else {
                                                if (TextUtils.isEmpty(storeAndForward.getPayment().getJob_id())) {
                                                    insertDeclinedPayment(storeAndForward.getPayment());
                                                } else {
                                                    BoloroPayment.seveBoloroAsInvoice(activity, storeAndForward, boloroHashMap);
                                                }
                                            }
                                            break;
                                        case CREDIT_CARD:
                                        case GIFT_CARD:
                                            processPayment(storeAndForward, _charge_xml);
                                            break;
                                    }
                                }

                            } catch (ParserConfigurationException e) {
                                e.printStackTrace();
                                Crashlytics.logException(e);
                            } catch (SAXException e) {
                                e.printStackTrace();
                                Crashlytics.logException(e);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Crashlytics.logException(e);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Crashlytics.logException(e);
                                StoredPaymentsDAO.updateStoreForwardPaymentToRetry(storeAndForward);
                            }

                            livePaymentRunning = false;
                        } else {
                            _count_conn_error++;
                        }
                    }
                } else {
                    _count_conn_error++;
                }
            } finally {
                if (realm != null) {
                    realm.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            myProgressDialog.dismiss();
            btnProcessAll.setEnabled(true);
            StoredPaymentsDAO.purgeDeletedStoredPayment();
            adapter.notifyDataSetChanged();
            btnProcessAll.setEnabled(true);
            StringBuilder sb = new StringBuilder();
            if (_count_conn_error > 0) {
                sb.append("\t -").append("Connection Error (").append(Integer.toString(_count_conn_error)).append("): ");
                sb.append(getString(R.string.dlog_msg_please_try_again)).append("\n");
            }
            if (_count_merch_account > 0) {
                sb.append("\t -").append("Merchant Account (").append(Integer.toString(_count_merch_account)).append("): ");
                sb.append(getString(R.string.dlog_msg_contact_support)).append("\n");
            }
            if (_count_decline > 0) {
                sb.append("\t -").append("Decline (").append(Integer.toString(_count_decline)).append("): ");
                sb.append(getString(R.string.dlog_msg_orders_changed_invoice)).append("\n");
            }

            if (!sb.toString().isEmpty())
                Global.showPrompt(activity, R.string.dlog_title_transaction_failed_to_process, sb.toString());
        }
    }

    private void insertDeclinedPayment(Payment payment) {
        if (TextUtils.isEmpty(payment.getJob_id())) {
            PaymentsHandler paymentsHandler = new PaymentsHandler(activity);
            paymentsHandler.insertDeclined(payment);
        }
    }

    private void saveApprovedPayment(StoreAndForward storeAndForward, HashMap<String, String> parsedMap) {
        Realm realm = Realm.getDefaultInstance();
        try {
            GenerateNewID generator = new GenerateNewID(this);
            Payment payment = realm.copyFromRealm(storeAndForward.getPayment());
            payment.setPay_id(generator.getNextID(IdType.PAYMENT_ID));
            payment.setProcessed("9");//newPayment.processed = "9";
            PaymentsHandler payHandler = new PaymentsHandler(this);
            payHandler.insert(payment);
            StoredPaymentsDAO.updateStatusDeleted(storeAndForward);
        } finally {
            if (realm != null) {
                realm.close();
            }
        }
    }

    private class CustomCursorAdapter extends RecyclerView.Adapter<CustomCursorAdapter.ViewHolder> {
        private List<StoreAndForward> storeAndForwards;

        public CustomCursorAdapter(List<StoreAndForward> storeAndForwards) {
            this.storeAndForwards = storeAndForwards;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_store_forward_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            StoreAndForward storeAndForward = storeAndForwards.get(position);
            Payment p = storeAndForward.getPayment();
            String cardName = TextUtils.isEmpty(p.getCard_type()) ? getString(R.string.card_credit_card) : p.getCard_type();
            holder.title.setText(String.format("%s  (%s)", cardName,
                    Global.getCurrencyFormat(p.getPay_amount())));
            holder.subtitle.setText(p.getPay_name());
        }

        @Override
        public int getItemCount() {
            return storeAndForwards.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView title, subtitle;

            public ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.tvTitle);
                subtitle = v.findViewById(R.id.tvSubtitle);
            }
        }
    }
}