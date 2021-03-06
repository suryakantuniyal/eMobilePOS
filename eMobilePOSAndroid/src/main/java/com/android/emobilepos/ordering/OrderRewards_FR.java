package com.android.emobilepos.ordering;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dao.AssignEmployeeDAO;
import com.android.database.OrdersHandler;
import com.android.database.PayMethodsHandler;
import com.android.database.PaymentsHandler;
import com.android.emobilepos.R;
import com.android.emobilepos.models.orders.Order;
import com.android.emobilepos.models.realms.AssignEmployee;
import com.android.emobilepos.models.realms.Payment;
import com.android.support.CreditCardInfo;
import com.android.support.GenerateNewID;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.PaymentTask;

import java.math.BigDecimal;

public class OrderRewards_FR extends Fragment implements OnClickListener {

    private EditText fieldRewardBalance;
    private TextView subTotalValue;
    private String balance = "";
    private String subtotal = "0";
    private SwiperRewardCallback callBackRewardSwiper;
    private ImageButton btnTap;
    private TextView tapTxtLabel;
    private Button btnPayRewards;

    public static OrderRewards_FR init(int val) {
        OrderRewards_FR frag = new OrderRewards_FR();
        Bundle args = new Bundle();
        args.putInt("val", val);
        frag.setArguments(args);
        return frag;
    }

//    public static OrderRewards_FR getFrag() {
//        return myFrag;
//    }

    public void setRewardBalance(String value) {
        balance = value;
        if (fieldRewardBalance != null)
            fieldRewardBalance.setText(balance);
        Global.rewardCardInfo.setOriginalTotalAmount(value);
    }

    public void setRewardSubTotal(String value) {
        subtotal = value;
        if (subTotalValue != null) {
            Global.rewardAccumulableSubtotal = new BigDecimal(subtotal);
            subTotalValue.setText(value);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.order_rewards_layout, container, false);

        callBackRewardSwiper = (SwiperRewardCallback) getActivity();
        btnTap = (ImageButton) view.findViewById(R.id.btnTapReward);
        btnTap.setOnClickListener(this);

        btnPayRewards = (Button) view.findViewById(R.id.btnPayWithRewards);
        btnPayRewards.setOnClickListener(this);
        tapTxtLabel = (TextView) view.findViewById(R.id.tapLabel);

        fieldRewardBalance = (EditText) view.findViewById(R.id.fieldRewardBalance);

        subTotalValue = (TextView) view.findViewById(R.id.subtotalValue);

//        if (savedInstanceState == null && OrderTotalDetails_FR.getFrag() != null) {
//            Global global = (Global) getActivity().getApplication();
//            OrderTotalDetails_FR.getFrag().reCalculate(global.order.getOrderProducts());
//        }

        if (getOrderingMainFa().rewardsWasRead) {
            hideTapButton();
        }
//        else {
//            callBackRewardSwiper.prefetchRewardsBalance();
//        }
        return view;
    }

    private OrderingMain_FA getOrderingMainFa() {
        return (OrderingMain_FA) getActivity();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnTapReward:
                callBackRewardSwiper.startRewardSwiper();
                break;
            case R.id.btnPayWithRewards:
                Global global = (Global) getActivity().getApplication();
                Order order = Receipt_FR.buildOrder(getActivity(), global, "", "",
                        ((OrderingMain_FA) getActivity()).getSelectedDinningTableNumber(),
                        ((OrderingMain_FA) getActivity()).getAssociateId(), ((OrderingMain_FA) getActivity()).getOrderAttributes(),
                        ((OrderingMain_FA) getActivity()).getListOrderTaxes(), global.order.getOrderProducts());
                OrdersHandler ordersHandler = new OrdersHandler(getActivity());
                ordersHandler.insert(order);
                global.order = order;
                new ProcessRewardPaymentTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new BigDecimal(subtotal));
                break;
        }
    }

    private Payment getPayment(boolean isLoyalty, BigDecimal chargeAmount) {
        AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();

        Payment loyaltyRewardPayment = new Payment(getActivity());
        MyPreferences preferences = new MyPreferences(getActivity());
        String cardType = "LoyaltyCard";
        CreditCardInfo cardInfoManager;
        if (isLoyalty)
            cardInfoManager = Global.loyaltyCardInfo;
        else {
            cardInfoManager = Global.rewardCardInfo;
            cardType = "Reward";
        }
        cardInfoManager.setCardType(cardType);
        GenerateNewID generator = new GenerateNewID(getActivity());
        String tempPay_id;

        tempPay_id = generator.getNextID(GenerateNewID.IdType.PAYMENT_ID);

        loyaltyRewardPayment.setPay_id(tempPay_id);

        loyaltyRewardPayment.setCust_id(Global.getValidString(preferences.getCustID()));
        loyaltyRewardPayment.setCustidkey(Global.getValidString(preferences.getCustIDKey()));
        loyaltyRewardPayment.setEmp_id(String.valueOf(assignEmployee.getEmpId()));
        loyaltyRewardPayment.setJob_id(Global.lastOrdID);

        loyaltyRewardPayment.setPay_name(cardInfoManager.getCardOwnerName());
        loyaltyRewardPayment.setPay_ccnum(cardInfoManager.getCardNumAESEncrypted());

        loyaltyRewardPayment.setCcnum_last4(cardInfoManager.getCardLast4());
        loyaltyRewardPayment.setPay_expmonth(cardInfoManager.getCardExpMonth());
        loyaltyRewardPayment.setPay_expyear(cardInfoManager.getCardExpYear());
        loyaltyRewardPayment.setPay_seccode(cardInfoManager.getCardEncryptedSecCode());

        loyaltyRewardPayment.setTrack_one(cardInfoManager.getEncryptedAESTrack1());
        loyaltyRewardPayment.setTrack_two(cardInfoManager.getEncryptedAESTrack2());

        cardInfoManager.setRedeemAll("0");
        cardInfoManager.setRedeemType("Only");
        PayMethodsHandler payMethodsHandler = new PayMethodsHandler(getActivity());
        String methodId = payMethodsHandler.getSpecificPayMethodId("Rewards");
        loyaltyRewardPayment.setPaymethod_id(methodId);
        loyaltyRewardPayment.setCard_type(cardType);
        loyaltyRewardPayment.setPay_type("0");

        if (isLoyalty) {
            loyaltyRewardPayment.setPay_amount(String.valueOf(chargeAmount));
            loyaltyRewardPayment.setPay_amount(Global.loyaltyAddAmount);
            loyaltyRewardPayment.setPay_amount(Global.loyaltyCharge);

        } else {
            loyaltyRewardPayment.setOriginalTotalAmount(chargeAmount.toString());
            loyaltyRewardPayment.setPay_amount(chargeAmount.toString());
        }
        return loyaltyRewardPayment;
    }

    public void hideTapButton() {
        btnTap.setVisibility(View.GONE);
        tapTxtLabel.setVisibility(View.GONE);
    }


    public interface SwiperRewardCallback {
        void startRewardSwiper();

        void prefetchRewardsBalance();
    }

    private class ProcessRewardPaymentTask extends AsyncTask<BigDecimal, Void, PaymentTask.Response> {

        private Payment payment;

        @Override
        protected PaymentTask.Response doInBackground(BigDecimal... params) {
            payment = getPayment(false, params[0]);
            Global.rewardCardInfo.setRedeemAll("1");
            return PaymentTask.processRewardPayment(getActivity(), params[0], Global.rewardCardInfo, payment);
        }

        @Override
        protected void onPostExecute(PaymentTask.Response result) {
            OrderingMain_FA mainFa = (OrderingMain_FA) getActivity();
            if (result.getResponseStatus() == PaymentTask.Response.ResponseStatus.OK) {
//                Global.showPrompt(getActivity(), R.string.rewards, result.getMessage());
                Toast.makeText(getActivity(), result.getMessage(), Toast.LENGTH_LONG).show();
                Global global = (Global) getActivity().getApplication();
                BigDecimal rewardDiscount = mainFa.getLeftFragment()
                        .applyRewardDiscount(result.getApprovedAmount(), global.order.getOrderProducts());

//                if (OrderTotalDetails_FR.getFrag() != null) {
//                    OrderTotalDetails_FR.getFrag().reCalculate(global.order.getOrderProducts());
//                }
                btnPayRewards.setClickable(false);
                btnPayRewards.setEnabled(false);
                payment.setPay_issync("1");
                payment.setPay_transid(result.getTransactionId());
                PaymentsHandler paymentsHandler = new PaymentsHandler(getActivity());
                paymentsHandler.insert(payment);
                BigDecimal zero = new BigDecimal(0);
                BigDecimal newBalance = Global.getBigDecimalNum(balance).subtract(result.getApprovedAmount());
                if (newBalance.compareTo(zero) == -1) {
                    newBalance = zero;
                }
                setRewardBalance(String.valueOf(Global.getRoundBigDecimal(newBalance)));
            } else {
//                Toast.makeText(getActivity(), result.getMessage(), Toast.LENGTH_LONG).show();
                Global.showPrompt(getActivity(), R.string.rewards, result.getMessage());
            }
            mainFa.buildOrderStarted = false;
        }
    }
}