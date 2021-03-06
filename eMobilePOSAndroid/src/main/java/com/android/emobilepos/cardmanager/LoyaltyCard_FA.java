package com.android.emobilepos.cardmanager;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.emobilepos.R;
import com.android.emobilepos.adapters.GiftLoyaltyRewardLV_Adapter;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;

public class LoyaltyCard_FA extends BaseFragmentActivityActionBar implements OnItemClickListener {

    private final int ACTIVATE = 0, BALANCE_INQUIRY = 1, MANUAL_BALANCE = 2;
    private Global global;
    private boolean hasBeenCreated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        global = (Global) getApplication();
        MyPreferences myPref = new MyPreferences(this);
        setContentView(R.layout.gift_loyal_reward_main_layout);
        TextView headerTitle = findViewById(R.id.HeaderTitle);
        headerTitle.setText(String.format(getString(R.string.default_unit_name_format), myPref.getDefaultUnitsName()));

        ListView lView = findViewById(R.id.listView);
        GiftLoyaltyRewardLV_Adapter adapter = new GiftLoyaltyRewardLV_Adapter(this, 1);
        lView.setAdapter(adapter);
        lView.setOnItemClickListener(this);
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
//        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//        boolean isScreenOn = powerManager.isScreenOn();
//        MyPreferences myPref = new MyPreferences(this);
//        if (!isScreenOn && myPref.isExpireUserSession())
//            Global.loggedIn = false;
        global.startActivityTransitionTimer();
    }


    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
        Intent intent = new Intent(this, CardManager_FA.class);
        intent.putExtra("CARD_TYPE", CardManager_FA.CASE_LOYALTY);
        switch (pos) {
            case ACTIVATE:
                intent.putExtra("PROCESS_TYPE", CardManager_FA.GiftCardActions.CASE_ACTIVATE.getCode());
                startActivity(intent);
                break;
            case BALANCE_INQUIRY:
                intent.putExtra("PROCESS_TYPE", CardManager_FA.GiftCardActions.CASE_BALANCE_INQUIRY.getCode());
                startActivity(intent);
                break;
            case MANUAL_BALANCE:
                intent.putExtra("PROCESS_TYPE", CardManager_FA.GiftCardActions.CASE_MANUAL_ADD.getCode());
                promptManagerPassword(intent);
                break;
        }

    }

    private void promptManagerPassword(final Intent intent) {
        final Dialog globalDlog = new Dialog(this, R.style.Theme_TransparentTest);
        globalDlog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        globalDlog.setCancelable(true);
        globalDlog.setContentView(R.layout.dlog_field_single_layout);


        final MyPreferences myPref = new MyPreferences(this);
        final EditText viewField = globalDlog.findViewById(R.id.dlogFieldSingle);
        viewField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        TextView viewTitle = globalDlog.findViewById(R.id.dlogTitle);
        TextView viewMsg = globalDlog.findViewById(R.id.dlogMessage);
        viewTitle.setText(R.string.dlog_title_confirm);

        viewMsg.setText(R.string.dlog_title_enter_manager_password);
        Button btnCancel = globalDlog.findViewById(R.id.btnCancelDlogSingle);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                globalDlog.dismiss();
            }
        });
        Button btnOk = globalDlog.findViewById(R.id.btnDlogSingle);
        btnOk.setText(R.string.button_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                globalDlog.dismiss();
                String pass = viewField.getText().toString();
                if (!pass.isEmpty() && myPref.loginManager(pass.trim())) {
                    startActivity(intent);
                } else {
                    promptManagerPassword(intent);
                }
            }
        });
        globalDlog.show();
    }
}
