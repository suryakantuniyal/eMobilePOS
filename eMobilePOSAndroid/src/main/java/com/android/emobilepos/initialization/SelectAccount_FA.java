package com.android.emobilepos.initialization;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.android.dao.AssignEmployeeDAO;
import com.android.database.DBManager;
import com.android.emobilepos.R;
import com.android.emobilepos.mainmenu.MainMenu_FA;
import com.android.emobilepos.models.realms.AssignEmployee;
import com.android.saxhandler.SaxLoginHandler;
import com.android.support.Global;
import com.android.support.MyPreferences;
import com.android.support.Post;
import com.android.support.SynchMethods;
import com.android.support.fragmentactivity.BaseFragmentActivityActionBar;
import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class SelectAccount_FA extends BaseFragmentActivityActionBar {
    private Context thisContext;
    private ProgressDialog myProgressDialog;
    private Dialog promptDialog;
    private DBManager dbManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            super.onCreate(savedInstanceState);
            Fabric.with(this, new Crashlytics());
            final MyPreferences myPref = new MyPreferences(this);
            if (myPref.getLogIn()) {
                dbManager = new DBManager(this, Global.FROM_LOGIN_ACTIVITTY);
                if (dbManager.isNewDBVersion()) {
                    dbManager.alterTables();
                    AssignEmployee assignEmployee = AssignEmployeeDAO.getAssignEmployee();
                    if (assignEmployee == null && !myPref.getEmpIdFromPreferences().isEmpty()) {
                        assignEmployee = new AssignEmployee();
                        assignEmployee.setEmpId(Integer.parseInt(myPref.getEmpIdFromPreferences()));
                        List<AssignEmployee> assignEmployees = new ArrayList<>();
                        assignEmployees.add(assignEmployee);
                        try {
                            AssignEmployeeDAO.insertAssignEmployee(assignEmployees);
                        } catch (Exception e) {
                            Crashlytics.logException(e);
                        }
                    }
                    if (dbManager.unsynchItemsLeft()) {
                        //there are unsynch item left...
                        Intent intent = new Intent(this, MainMenu_FA.class);
                        Bundle extras = new Bundle();
                        extras.putBoolean("unsynched_items", true);
                        intent.putExtras(extras);
                        startActivity(intent);
                        finish();
                    } else {
                        AlertDialog.Builder alertDlogBuilder = new AlertDialog.Builder(this);
                        promptDialog = alertDlogBuilder.setTitle("Urgent").setCancelable(false)
                                .setMessage("A new Database version must be installed...").
                                        setPositiveButton("Install", new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface thisDialog, int which) {
                                                dbManager.updateDB();
//                                            SynchMethods sm = new SynchMethods(dbManager);
//                                            sm.synchReceive(Global.FROM_REGISTRATION_ACTIVITY, activity);
                                                promptDialog.dismiss();
                                                new SyncReceiveTask().execute(dbManager);
                                            }
                                        }).create();
                        promptDialog.show();
                    }

                } else {
                    Intent intent = new Intent(this, MainMenu_FA.class);
                    startActivityForResult(intent, 0);
                    finish();
                }
            } else {
                setContentView(R.layout.initialization_layout);
                final EditText acctNumber = (EditText) findViewById(R.id.initAccountNumber);
                final EditText acctPassword = (EditText) findViewById(R.id.initPassword);
                Button login = (Button) findViewById(R.id.loginButton);
                thisContext = this;
                login.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (!myPref.getLogIn()) {
                            myPref.setAcctNumber(acctNumber.getText().toString());
                            myPref.setAcctPassword(acctPassword.getText().toString());
                            String android_id = Secure.getString(thisContext.getContentResolver(), Secure.ANDROID_ID);
                            myPref.setDeviceID(android_id);
                            new ValidateLoginAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
                        }
                    }
                });
                checkPermissions();
            }
        }catch (Throwable x){
            x.printStackTrace();
        }
    }

    private void checkPermissions() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            requestPermissions(Global.permissions, 42);
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1) {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    }

    public enum PermissionType {
        ACCESS_FINE_LOCATION(0), ACCESS_COARSE_LOCATION(1), WRITE_EXTERNAL_STORAGE(2), CAMERA(3), READ_PHONE_STATE(4), ACCESS_MICROPHONE(5), NONE(99);

        private int code;

        PermissionType(int code) {
            this.code = code;
        }

        public static PermissionType getByCode(int code) {
            switch (code) {
                case 0:
                    return ACCESS_FINE_LOCATION;
                case 1:
                    return ACCESS_COARSE_LOCATION;
                case 2:
                    return WRITE_EXTERNAL_STORAGE;
                case 3:
                    return CAMERA;
                case 4:
                    return READ_PHONE_STATE;
                case 5:
                    return ACCESS_MICROPHONE;
                default:
                    return NONE;
            }
        }

        public int getCode() {
            return this.code;
        }
    }

    private class SyncReceiveTask extends AsyncTask<DBManager, Void, Boolean> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(SelectAccount_FA.this);
            dialog.setTitle(R.string.sync_title);
            dialog.setIndeterminate(true);
            dialog.setMessage(getString(R.string.sync_inprogress));
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(DBManager... params) {
            DBManager dbManager = params[0];
            SynchMethods sm = new SynchMethods(dbManager);
            return sm.syncReceive();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            if (!result) {
                Global.showPrompt(SelectAccount_FA.this, R.string.sync_title, getString(R.string.sync_fail));
            } else {
                Intent intent = new Intent(SelectAccount_FA.this, MainMenu_FA.class);
                setResult(-1);
                startActivity(intent);
                finish();
            }
        }
    }

    private class ValidateLoginAsync extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            myProgressDialog = new ProgressDialog(thisContext);
            myProgressDialog.setMessage(getString(R.string.loading));
            myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            myProgressDialog.setCancelable(false);
            myProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Post post = new Post(SelectAccount_FA.this);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SaxLoginHandler handler = new SaxLoginHandler();
            boolean proceed = false;
            try {
                String xml = post.postData(0, "");
                InputSource inSource = new InputSource(new StringReader(xml));
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                xr.setContentHandler(handler);
                xr.parse(inSource);
                proceed = Boolean.parseBoolean(handler.getData().toLowerCase(Locale.getDefault()));
            } catch (Exception e) {
                Crashlytics.logException(e);
            }
            return proceed;
        }

        @Override
        protected void onPostExecute(Boolean proceed) {
            if (myProgressDialog != null && myProgressDialog.isShowing()) {
                myProgressDialog.dismiss();
            }
            MyPreferences myPref = new MyPreferences(SelectAccount_FA.this);
            if (proceed) {
                Intent intent = new Intent(thisContext, SelectEmployee_FA.class);
                startActivityForResult(intent, 0);

            } else {
                myPref.setLogIn(false);
                AlertDialog.Builder dialog = new AlertDialog.Builder(thisContext);
                dialog.setTitle("Error");
                dialog.setMessage("The provided information could not be validated. Please try again.");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog.create().show();
            }
        }
    }

}
