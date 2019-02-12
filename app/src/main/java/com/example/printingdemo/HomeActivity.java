package com.example.printingdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.printingdemo.Utils.Resultset;
import com.example.printingdemo.Utils.UrlManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import device.common.DecodeResult;
import device.common.DecodeStateCallback;
import device.common.ScanConst;
import device.sdk.ScanManager;

@SuppressWarnings("unused")
public class HomeActivity extends Activity {
    private static final String TAG = "tScanner";

    private static ScanManager mScanner;
    private static DecodeResult mDecodeResult;
    private boolean mKeyLock = false;

    private static TextView mBarType = null;
    private static TextView mResult = null;
    private static CheckBox mAutoScanOption = null;
    private static CheckBox mBeepOption = null;
    private static Button mEnabledProp = null;
    private static CheckBox mEventCheck = null;

    private AlertDialog mDialog = null;
    private int mBackupResultType = ScanConst.ResultType.DCD_RESULT_COPYPASTE;
    private Context mContext;
    private ProgressDialog mWaitDialog = null;
    private final Handler mHandler = new Handler();



    public static class ScanResultReceiver extends BroadcastReceiver {
        RequestQueue requestQueue;
        Intent printIntent;
        String network_error="No Internet Connection Available Please check your Settings";
        private ArrayList<Resultset>printableResult=new ArrayList<>();
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mScanner != null) {
                if (ScanConst.INTENT_USERMSG.equals(intent.getAction())) {
                    mScanner.aDecodeGetResult(mDecodeResult.recycle());
                    mBarType.setText(mDecodeResult.symName);
                    mResult.setText(getUserId(mDecodeResult.toString()));
                    //mResult.append("\n"+getNames(mDecodeResult.toString()));
                    printIntent=new Intent(context,PrintActivity.class);
                    if(isNetworkAvailable(context)){
                        sendRequest(context,printIntent,getUserId(mDecodeResult.toString()));
                    }else{
                       Toast.makeText(context,network_error,Toast.LENGTH_LONG).show();
                    }
                } else if (ScanConst.INTENT_EVENT.equals(intent.getAction())) {

                }
            }
        }
        private boolean isNetworkAvailable(Context context) {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        private void RatingDialog(Context context,boolean status){
            Dialog dialog= new Dialog(context);
            dialog.setContentView(R.layout.loader_layout);
            dialog.setCancelable(false);
            if(status){
                dialog.show();
            }else{
                dialog.dismiss();
            }
        }
        private void sendRequest(final Context context,final Intent printIntent,String nid){
            if(requestQueue==null){
                requestQueue= Volley.newRequestQueue(context);
            }
            String api_request=UrlManager.API_ENDPOINT+nid;
            final String error_msg="That ID is not found in the System Please Try again if there is a system problem";
// Request a string response from the provided URL.
            //RatingDialog(context,true);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, api_request,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            //RatingDialog(context,false);
                            // Display the first 500 characters of the response string.
                            try {
                                JSONArray jsonArray=new JSONArray(response);
                                if(jsonArray.length()>0){
                                    for(int i=0;i<jsonArray.length();i++){
                                        JSONObject jsonObject=jsonArray.getJSONObject(i);
                                        String names=jsonObject.getString("trainee_names");
                                        String number=jsonObject.getString("trainee_number");
                                        String pc=jsonObject.getString("pc");
                                        String reg_number=jsonObject.getString("reg_number");
                                        String training_date=jsonObject.getString("training_date");
                                        printIntent.putExtra("names",names);
                                        printIntent.putExtra("number",number);
                                        printIntent.putExtra("pc",pc);
                                        printIntent.putExtra("reg_number",reg_number);
                                        printIntent.putExtra("training_date",training_date);
                                        printIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        printIntent.putExtra("nid",getUserId(mDecodeResult.toString()));
                                        context.startActivity(printIntent);
                                    }
                                }else{
                                    Toast.makeText(context,error_msg,Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    RatingDialog(context,false);
                    Toast.makeText(context,error.getMessage(),Toast.LENGTH_LONG).show();
                }
            });

// Add the request to the RequestQueue.
            requestQueue.add(stringRequest);
        }
        private String getUserId(String message){
            String user_id="";
            if(message.length()>16){
                user_id=message.substring(0,21);
            }else{
                user_id=message;
            }
            user_id=user_id.replaceAll("\\s+","");
            return user_id;
        }
        private String getNames(String message){
            String names="";
            names=message.substring(28,40);
            return names;
        }
    }

    private DecodeStateCallback mStateCallback = new DecodeStateCallback(mHandler) {
        public void onChangedState(int state) {
            switch (state) {
                case ScanConst.STATE_ON:
                case ScanConst.STATE_TURNING_ON:
                    if (getEnableDialog().isShowing()) {
                        getEnableDialog().dismiss();
                    }
                    break;
                case ScanConst.STATE_OFF:
                case ScanConst.STATE_TURNING_OFF:
                    if (!getEnableDialog().isShowing()) {
                        getEnableDialog().show();
                    }
                    break;
            }
        };
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        mContext = HomeActivity.this;
        mScanner = new ScanManager();
        mDecodeResult = new DecodeResult();

        mBarType = (TextView) findViewById(R.id.textview_bar_type);
        mResult = (TextView) findViewById(R.id.textview_scan_result);

        mAutoScanOption = (CheckBox) findViewById(R.id.check_autoscan);
        mAutoScanOption.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mScanner != null) {
                    if (isChecked) {
                        mScanner.aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_AUTO);
                    } else {
                        mScanner.aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_ONESHOT);
                    }
                }
            }
        });

        mEventCheck = (CheckBox) findViewById(R.id.check_event);
        mEventCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mScanner != null) {
                    if (isChecked) {
                        mScanner.aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_EVENT);
                    } else {
                        mScanner.aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_USERMSG);
                    }
                }
            }
        });

        mBeepOption = (CheckBox) findViewById(R.id.check_beep);
        mBeepOption.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mScanner != null) {
                    if (isChecked) {
                        mScanner.aDecodeSetBeepEnable(1);
                    } else {
                        mScanner.aDecodeSetBeepEnable(0);
                    }
                }
            }
        });

        ((Button) findViewById(R.id.button_scan_on)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mScanner != null) {
                    mScanner.aDecodeSetTriggerOn(1);
                }
            }
        });

        ((Button) findViewById(R.id.button_scan_off)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mScanner != null) {
                    if (mAutoScanOption.isChecked()) {
                        mScanner.aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_ONESHOT);
                    }

                    mScanner.aDecodeSetTriggerOn(0);

                    if (mAutoScanOption.isChecked()) {
                        mScanner.aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_AUTO);
                    }
                }
            }
        });

        ((Button) findViewById(R.id.button_enalbe_upc)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mScanner != null) {
                    mScanner.aDecodeSymSetEnable(ScanConst.SymbologyID.DCD_SYM_UPCA, 1);
                }
            }
        });

        ((Button) findViewById(R.id.button_disalbe_upc)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mScanner != null) {
                    mScanner.aDecodeSymSetEnable(ScanConst.SymbologyID.DCD_SYM_UPCA, 0);
                }
            }
        });

        mEnabledProp = (Button) findViewById(R.id.button_prop_enalbe);
        mEnabledProp.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mScanner != null) {
                    int symID = ScanConst.SymbologyID.DCD_SYM_UPCA;
                    int propCnt = mScanner.aDecodeSymGetLocalPropCount(symID);
                    int propIndex = 0;

                    for (int i = 0; i < propCnt; i++) {
                        String propName = mScanner.aDecodeSymGetLocalPropName(symID, i);
                        if (propName.equals("Send Check Character")) {
                            propIndex = i;
                            break;
                        }
                    }

                    if (mKeyLock == false) {
                        mEnabledProp.setText(R.string.property_enable);
                        mKeyLock = true;
                        mScanner.aDecodeSymSetLocalPropEnable(symID, propIndex, 0);
                    } else {
                        mEnabledProp.setText(R.string.property_disable);
                        mKeyLock = false;
                        mScanner.aDecodeSymSetLocalPropEnable(symID, propIndex, 1);
                    }
                }
            }
        });
    }

    private void initScanner() {
        if (mScanner != null) {
            mScanner.aRegisterDecodeStateCallback(mStateCallback);
            mBackupResultType = mScanner.aDecodeGetResultType();
            mScanner.aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_USERMSG);
            mEventCheck.setChecked(false);
            if (mScanner.aDecodeGetTriggerMode() == ScanConst.TriggerMode.DCD_TRIGGER_MODE_AUTO) {
                mAutoScanOption.setChecked(true);
            } else {
                mAutoScanOption.setChecked(false);
            }
            if (mScanner.aDecodeGetBeepEnable() == 1) {
                mBeepOption.setChecked(true);
            } else {
                mBeepOption.setChecked(false);
            }
        }
    }

    private Runnable mStartOnResume = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initScanner();
                    if (mWaitDialog != null && mWaitDialog.isShowing()) {
                        mWaitDialog.dismiss();
                    }
                }
            });
        }
    };

    private AlertDialog getEnableDialog() {
        if (mDialog == null) {
            AlertDialog dialog = new AlertDialog.Builder(this).create();
            dialog.setTitle(R.string.app_name);
            dialog.setMessage("Your scanner is disabled. Do you want to enable it?");

            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(ScanConst.LAUNCH_SCAN_SETTING_ACITON);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            dialog.dismiss();
                        }
                    });
            dialog.setCancelable(false);
            mDialog = dialog;
        }
        return mDialog;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWaitDialog = ProgressDialog.show(mContext, "", getString(R.string.msg_wait), true);
        mHandler.postDelayed(mStartOnResume, 1000);
    }

    @Override
    protected void onPause() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
            mScanner.aUnregisterDecodeStateCallback(mStateCallback);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
        }
        mScanner = null;
        super.onDestroy();
    }
}
