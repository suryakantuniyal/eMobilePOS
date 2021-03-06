package com.android.emobilepos.security;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.dao.ClerkDAO;
import com.android.dao.EmobileBiometricDAO;
import com.android.emobilepos.R;
import com.android.emobilepos.customer.ViewCustomerDetails_FA;
import com.android.emobilepos.models.realms.BiometricFid;
import com.android.emobilepos.models.realms.Clerk;
import com.android.emobilepos.models.realms.EmobileBiometric;
import com.android.support.Global;
import com.android.support.MyPreferences;

import drivers.digitalpersona.DigitalPersona;
import interfaces.BiometricCallbacks;


public class ClerkManagementDetailActivity extends Activity implements BiometricCallbacks, View.OnClickListener {
    int clerkId;
    private Clerk clerk;
    Button fingerLeft1;
    Button fingerLeft2;
    Button fingerLeft3;
    Button fingerLeft4;
    DigitalPersona digitalPersona;
    MyPreferences preferences;
    private EmobileBiometric biometric;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clerk_management_detail);
        preferences = new MyPreferences(this);
        Bundle extras = getIntent().getExtras();
        clerkId = extras.getInt("clerkId", 0);
        clerk = ClerkDAO.getByEmpId(clerkId);
        digitalPersona = new DigitalPersona(this, this, EmobileBiometric.UserType.CLERK);
        biometric = EmobileBiometricDAO.getBiometrics(String.valueOf(clerkId), EmobileBiometric.UserType.CLERK);
        setUI();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (preferences.isAPT120() && preferences.getPreferences(MyPreferences.pref_use_navigationbar)) {
            getMenuInflater().inflate(R.menu.activity_main_menu, menu);

            MenuItem back, hold, logout, elo;
            invalidateOptionsMenu();
            back = menu.findItem(R.id.menu_back);
            logout = menu.findItem(R.id.logoutMenuItem);
            hold = menu.findItem(R.id.refreshHolds);
            elo = menu.findItem(R.id.toggleEloBCR);

            back.setVisible(true);
            hold.setVisible(false);
            logout.setVisible(false);
            elo.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_back: {
                onBackPressed();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUI() {
        fingerLeft1 = (Button) findViewById(R.id.fingerOneLeftbutton6);
        fingerLeft2 = (Button) findViewById(R.id.fingerTwoLeftbutton5);
        fingerLeft3 = (Button) findViewById(R.id.fingerThreeLeftbutton4);
        fingerLeft4 = (Button) findViewById(R.id.fingerFourLeftbutton3);
        fingerLeft1.setOnClickListener(this);
        fingerLeft2.setOnClickListener(this);
        fingerLeft3.setOnClickListener(this);
        fingerLeft4.setOnClickListener(this);
        TextView clerkId = (TextView) findViewById(R.id.clerkIdtextView);
        TextView clerkName = (TextView) findViewById(R.id.clerkNametextView41);
        clerkId.setText(String.valueOf(clerk.getEmpId()));
        clerkName.setText(clerk.getEmpName());
        setFingerPrintUI();
    }

    @Override
    public void biometricsWasRead(EmobileBiometric emobileBiometric) {

    }

    @Override
    public void biometricsReadNotFound() {

    }

    @Override
    public void biometricsWasEnrolled(BiometricFid biometricFid) {
        EmobileBiometricDAO.deleteFinger(String.valueOf(clerkId), EmobileBiometric.UserType.CLERK,
                ViewCustomerDetails_FA.Finger.getByCode(biometricFid.getFingerCode()));
        biometric.setUserType(EmobileBiometric.UserType.CLERK);
        biometric.setEntityid(String.valueOf(clerkId));
        biometric.getFids().add(biometricFid);
        biometric.setRegid(preferences.getAcctNumber());
        EmobileBiometricDAO.upsert(biometric);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setFingerPrintUI();
            }
        });
    }

    private void setFingerPrintUI() {
        fingerLeft1.setBackgroundResource(R.color.black_transparency);
        fingerLeft2.setBackgroundResource(R.color.black_transparency);
        fingerLeft3.setBackgroundResource(R.color.black_transparency);
        fingerLeft4.setBackgroundResource(R.color.black_transparency);
        if (biometric != null) {
            for (BiometricFid fid : biometric.getFids()) {
                ViewCustomerDetails_FA.Finger finger = ViewCustomerDetails_FA.Finger.getByCode(fid.getFingerCode());
                switch (finger) {
                    case FINGER_ONE_LEFT:
                        fingerLeft1.setBackgroundColor(Color.GREEN);
                        break;
                    case FINGER_TWO_LEFT:
                        fingerLeft2.setBackgroundColor(Color.GREEN);
                        break;
                    case FINGER_THREE_LEFT:
                        fingerLeft3.setBackgroundColor(Color.GREEN);
                        break;
                    case FINGER_FOUR_LEFT:
                        fingerLeft4.setBackgroundColor(Color.GREEN);
                        break;
                }
            }
        }
    }

    @Override
    public void biometricsDuplicatedEnroll(EmobileBiometric emobileBiometric, BiometricFid biometricFid) {
        if ((emobileBiometric.getUserType() == EmobileBiometric.UserType.CLERK
                && emobileBiometric.getEntityid().equalsIgnoreCase(String.valueOf(clerkId)))
                || emobileBiometric.getUserType() != EmobileBiometric.UserType.CLERK) {
            boolean alreadyRegistered=false;
            for (BiometricFid fid : emobileBiometric.getFids()) {
                if(fid.getFingerCode()!=biometricFid.getFingerCode()){
                    alreadyRegistered=true;
                    break;
                }
            }
            if(!alreadyRegistered) {
                EmobileBiometricDAO.deleteFinger(String.valueOf(clerkId), EmobileBiometric.UserType.CLERK,
                        ViewCustomerDetails_FA.Finger.getByCode(biometricFid.getFingerCode()));
                biometric.setUserType(EmobileBiometric.UserType.CLERK);
                biometric.setEntityid(String.valueOf(clerkId));
                biometric.getFids().add(biometricFid);
                biometric.setRegid(preferences.getAcctNumber());
                EmobileBiometricDAO.upsert(biometric);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setFingerPrintUI();
                    }
                });
            }else{
                showDuplicateMesage();
            }
        } else {
            if (!emobileBiometric.getEntityid().equalsIgnoreCase(String.valueOf(clerkId))) {
               showDuplicateMesage();
            }
        }
    }

    private void showDuplicateMesage(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Global.showPrompt(ClerkManagementDetailActivity.this, R.string.fingerprint_enrollment_title, getString(R.string.duplicated_fingerprint));
            }
        });
    }
    @Override
    public void biometricsUnregister(ViewCustomerDetails_FA.Finger finger) {
        EmobileBiometricDAO.deleteFinger(String.valueOf(clerkId), EmobileBiometric.UserType.CLERK, finger);
        biometric = EmobileBiometricDAO.getBiometrics(String.valueOf(clerkId), EmobileBiometric.UserType.CLERK);
        setFingerPrintUI();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fingerOneLeftbutton6:
                digitalPersona.startFingerPrintEnrollment(ViewCustomerDetails_FA.Finger.FINGER_ONE_LEFT);
                break;
            case R.id.fingerTwoLeftbutton5:
                digitalPersona.startFingerPrintEnrollment(ViewCustomerDetails_FA.Finger.FINGER_TWO_LEFT);
                break;
            case R.id.fingerThreeLeftbutton4:
                digitalPersona.startFingerPrintEnrollment(ViewCustomerDetails_FA.Finger.FINGER_THREE_LEFT);
                break;
            case R.id.fingerFourLeftbutton3:
                digitalPersona.startFingerPrintEnrollment(ViewCustomerDetails_FA.Finger.FINGER_FOUR_LEFT);
                break;
        }
    }
}
