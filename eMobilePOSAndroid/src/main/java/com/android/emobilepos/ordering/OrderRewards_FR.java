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

import com.android.emobilepos.R;
import com.android.emobilepos.models.Payment;
import com.android.support.CreditCardInfo;
import com.android.support.GenerateNewID;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.PaymentTask;

import java.math.BigDecimal;

public class OrderRewards_FR extends Fragment implements OnClickListener {

    private static OrderRewards_FR myFrag;
    private SwiperRewardCallback callBackRewardSwiper;
    private ImageButton btnTap;
    private TextView tapTxtLabel;
    private static EditText fieldRewardBalance;
    private static TextView subTotalValue;
    private static String balance = "";
    private static String subtotal = "0";
    private Button btnPayRewards;


    public interface SwiperRewardCallback {
        void startRewardSwiper();
    }


    public static OrderRewards_FR init(int val) {
        OrderRewards_FR frag = new OrderRewards_FR();
        Bundle args = new Bundle();
        args.putInt("val", val);
        frag.setArguments(args);
        myFrag = frag;
        return frag;
    }


    public static OrderRewards_FR getFrag() {
        return myFrag;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.order_rewards_layout, container, false);

        callBackRewardSwiper = (SwiperRewardCallback) getActivity();
        myFrag = this;
        btnTap = (ImageButton) view.findViewById(R.id.btnTapReward);
        btnTap.setOnClickListener(this);

        btnPayRewards = (Button) view.findViewById(R.id.btnPayWithRewards);
        btnPayRewards.setOnClickListener(this);
        tapTxtLabel = (TextView) view.findViewById(R.id.tapLabel);

        fieldRewardBalance = (EditText) view.findViewById(R.id.fieldRewardBalance);

        subTotalValue = (TextView) view.findViewById(R.id.subtotalValue);

        if (savedInstanceState == null && OrderTotalDetails_FR.getFrag() != null) {
            Global global = (Global) getActivity().getApplication();
            OrderTotalDetails_FR.getFrag().reCalculate(global.orderProducts);
        }

        if (OrderingMain_FA.rewardsWasRead)
            hideTapButton();
        return view;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnTapReward:
                callBackRewardSwiper.startRewardSwiper();
                break;
            case R.id.btnPayWithRewards:
                Global global = (Global) getActivity().getApplication();
                BigDecimal bdBalance = new BigDecimal(balance);
                OrderingMain_FA mainFa = (OrderingMain_FA) getActivity();
                BigDecimal rewardDiscount = mainFa.getLeftFragment().applyRewardDiscount(bdBalance, global.orderProducts);

                new ProcessRewardPaymentTask().execute(rewardDiscount);

//                Global global = (Global) getActivity().getApplication();
//                BigDecimal bdBalance = new BigDecimal(balance);
//                OrderingMain_FA mainFa = (OrderingMain_FA) getActivity();
//                BigDecimal rewardDiscount = mainFa.getLeftFragment().applyRewardDiscount(bdBalance, global.orderProducts);
//                PaymentTask.processRewardPayment(getActivity(), rewardDiscount);


                BigDecimal bdSubtotal = new BigDecimal(subtotal);
                if (bdBalance.compareTo(bdSubtotal) == 1)//bgBalance>bgSubtotal
                {
                    bdBalance = new BigDecimal(subtotal);
                    Global.rewardChargeAmount = bdBalance;
                } else {
                    Global.rewardChargeAmount = bdBalance;
                }
                if (OrderTotalDetails_FR.getFrag() != null) {
                    OrderTotalDetails_FR.getFrag().reCalculate(global.orderProducts);
                }
                btnPayRewards.setClickable(false);
                break;
        }
    }

    private class ProcessRewardPaymentTask extends AsyncTask<BigDecimal, Void, Boolean> {

        @Override
        protected Boolean doInBackground(BigDecimal... params) {
            Payment payment = getPayment(false, params[0]);
            return PaymentTask.processRewardPayment(getActivity(), params[0], Global.rewardCardInfo, payment);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {

            }
        }
    }

    private Payment getPayment(boolean isLoyalty, BigDecimal chargeAmount) {

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

        GenerateNewID generator = new GenerateNewID(getActivity());
        String tempPay_id;

        tempPay_id = generator.getNextID(GenerateNewID.IdType.PAYMENT_ID);

        loyaltyRewardPayment.pay_id = tempPay_id;

        loyaltyRewardPayment.cust_id = Global.getValidString(preferences.getCustID());
        loyaltyRewardPayment.custidkey = Global.getValidString(preferences.getCustIDKey());
        loyaltyRewardPayment.emp_id = preferences.getEmpID();
        loyaltyRewardPayment.job_id = Global.lastOrdID;

        loyaltyRewardPayment.pay_name = cardInfoManager.getCardOwnerName();
        loyaltyRewardPayment.pay_ccnum = cardInfoManager.getCardNumAESEncrypted();

        loyaltyRewardPayment.ccnum_last4 = cardInfoManager.getCardLast4();
        loyaltyRewardPayment.pay_expmonth = cardInfoManager.getCardExpMonth();
        loyaltyRewardPayment.pay_expyear = cardInfoManager.getCardExpYear();
        loyaltyRewardPayment.pay_seccode = cardInfoManager.getCardEncryptedSecCode();

        loyaltyRewardPayment.track_one = cardInfoManager.getEncryptedAESTrack1();
        loyaltyRewardPayment.track_two = cardInfoManager.getEncryptedAESTrack2();

        cardInfoManager.setRedeemAll("0");
        cardInfoManager.setRedeemType("Only");

        loyaltyRewardPayment.paymethod_id = cardType;
        loyaltyRewardPayment.card_type = cardType;
        loyaltyRewardPayment.pay_type = "0";

        if (isLoyalty) {
            loyaltyRewardPayment.pay_amount = String.valueOf(chargeAmount);
            loyaltyRewardPayment.pay_amount = Global.loyaltyAddAmount;
            loyaltyRewardPayment.pay_amount = Global.loyaltyCharge;

        } else {
            loyaltyRewardPayment.originalTotalAmount = chargeAmount.toString();
            loyaltyRewardPayment.pay_amount = chargeAmount.toString();
        }
        return loyaltyRewardPayment;
    }

    public void hideTapButton() {
        btnTap.setVisibility(View.GONE);
        tapTxtLabel.setVisibility(View.GONE);
    }


    public static void setRewardBalance(String value) {

        balance = value;
        if (fieldRewardBalance != null)
            fieldRewardBalance.setText(balance);
        Global.rewardCardInfo.setOriginalTotalAmount(value);
    }


    public static void setRewardSubTotal(String value) {
        subtotal = value;
        if (subTotalValue != null) {
            Global.rewardAccumulableSubtotal = new BigDecimal(subtotal);
            subTotalValue.setText(value);
        }
    }
}