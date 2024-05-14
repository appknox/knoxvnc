package com.appknox.vnc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_KEY_REVERSE_VNC_LAST_HOST = "reverse_vnc_last_host" ;
    private static final String PREFS_KEY_REPEATER_VNC_LAST_HOST = "repeater_vnc_last_host" ;
    private static final String PREFS_KEY_REPEATER_VNC_LAST_ID = "repeater_vnc_last_id" ;

    private Button mButtonToggle;
    private TextView mAddress;
    private boolean mIsMainServiceRunning;
    private BroadcastReceiver mMainServiceBroadcastReceiver;
    private AlertDialog mOutgoingConnectionWaitDialog;
    private String mLastMainServiceRequestId;
    private String mLastReverseHost;
    private int mLastReversePort;
    private String mLastRepeaterHost;
    private int mLastRepeaterPort;
    private String mLastRepeaterId;
    private Defaults mDefaults;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDefaults = new Defaults(this);

        mButtonToggle = findViewById(R.id.toggle);
        mButtonToggle.setOnClickListener(view -> {

            Intent intent = new Intent(MainActivity.this, VNCService.class);
            intent.putExtra(VNCService.EXTRA_PORT, prefs.getInt(Constants.PREFS_KEY_SETTINGS_PORT, mDefaults.getPort()));
            intent.putExtra(VNCService.EXTRA_PASSWORD, prefs.getString(Constants.PREFS_KEY_SETTINGS_PASSWORD, mDefaults.getPassword()));
            intent.putExtra(VNCService.EXTRA_FILE_TRANSFER, prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_FILE_TRANSFER, mDefaults.getFileTransfer()));
            intent.putExtra(VNCService.EXTRA_VIEW_ONLY, prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_VIEW_ONLY, mDefaults.getViewOnly()));
            intent.putExtra(VNCService.EXTRA_SHOW_POINTERS, prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_SHOW_POINTERS, mDefaults.getShowPointers()));
            intent.putExtra(VNCService.EXTRA_SCALING, prefs.getFloat(Constants.PREFS_KEY_SETTINGS_SCALING, mDefaults.getScaling()));
            if(mIsMainServiceRunning) {
                intent.setAction(VNCService.ACTION_STOP);
            }
            else {
                intent.setAction(VNCService.ACTION_START);
            }
            mButtonToggle.setEnabled(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

        });

        mAddress = findViewById(R.id.address);

        Button reverseVNC = findViewById(R.id.reverse_vnc);
        reverseVNC.setOnClickListener(view -> {

            final EditText inputText = new EditText(this);
            inputText.setInputType(InputType.TYPE_CLASS_TEXT);
            inputText.setHint(getString(R.string.main_activity_reverse_vnc_hint));
            String lastHost = prefs.getString(PREFS_KEY_REVERSE_VNC_LAST_HOST, null);
            if(lastHost != null) {
                inputText.setText(lastHost);
                // select all to make new input quicker
                inputText.setSelectAllOnFocus(true);
            }
            inputText.requestFocus();
            inputText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout inputLayout = new LinearLayout(this);
            inputLayout.setPadding(
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, Resources.getSystem().getDisplayMetrics()),
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, Resources.getSystem().getDisplayMetrics()),
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, Resources.getSystem().getDisplayMetrics()),
                    0
            );
            inputLayout.addView(inputText);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.main_activity_reverse_vnc_button)
                    .setView(inputLayout)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        // parse host and port parts
                        String[] parts = inputText.getText().toString().split("\\:");
                        String host = parts[0];
                        int port = mDefaults.getPortReverse();
                        if (parts.length > 1) {
                            try {
                                port = Integer.parseInt(parts[1]);
                            } catch(NumberFormatException unused) {
                                // stays at default reverse port
                            }
                        }
                        Log.d(TAG, "reverse vnc " + host + ":" + port);
                        mLastMainServiceRequestId = UUID.randomUUID().toString();
                        mLastReverseHost = host;
                        mLastReversePort = port;
                        Intent request = new Intent(MainActivity.this, VNCService.class);
                        request.setAction(VNCService.ACTION_CONNECT_REVERSE);
                        request.putExtra(VNCService.EXTRA_HOST, host);
                        request.putExtra(VNCService.EXTRA_PORT, port);
                        request.putExtra(VNCService.EXTRA_REQUEST_ID, mLastMainServiceRequestId);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(request);
                        } else {
                            startService(request);
                        }

                        // show a progress dialog
                        ProgressBar progressBar = new ProgressBar(this);
                        progressBar.setPadding(0,0,0, (int) (30 * getResources().getDisplayMetrics().density));
                        mOutgoingConnectionWaitDialog = new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setTitle(R.string.main_activity_reverse_vnc_button)
                                .setMessage(getString(R.string.main_activity_connecting_to, host + ":" + port))
                                .setView(progressBar)
                                .show();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                    .create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        });

        Button repeaterVNC = findViewById(R.id.repeater_vnc);
        repeaterVNC.setOnClickListener(view -> {

            final EditText hostInputText = new EditText(this);
            hostInputText.setInputType(InputType.TYPE_CLASS_TEXT);
            hostInputText.setHint(getString(R.string.main_activity_repeater_vnc_hint));
            String lastHost = prefs.getString(PREFS_KEY_REPEATER_VNC_LAST_HOST, "");
            hostInputText.setText(lastHost); //host:port
            hostInputText.requestFocus();
            hostInputText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            final EditText idInputText = new EditText(this);
            idInputText.setInputType(InputType.TYPE_CLASS_NUMBER);
            idInputText.setHint(getString(R.string.main_activity_repeater_vnc_hint_id));
            String lastID = prefs.getString(PREFS_KEY_REPEATER_VNC_LAST_ID, "");
            idInputText.setText(lastID); //host:port
            idInputText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout inputLayout = new LinearLayout(this);
            inputLayout.setPadding(
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, Resources.getSystem().getDisplayMetrics()),
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, Resources.getSystem().getDisplayMetrics()),
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, Resources.getSystem().getDisplayMetrics()),
                    0
            );
            inputLayout.setOrientation(LinearLayout.VERTICAL);
            inputLayout.addView(hostInputText);
            inputLayout.addView(idInputText);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.main_activity_repeater_vnc_button)
                    .setView(inputLayout)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        // parse host and port parts
                        String[] parts = hostInputText.getText().toString().split("\\:");
                        String host = parts[0];
                        int port = mDefaults.getPortRepeater();
                        if (parts.length > 1) {
                            try {
                                port = Integer.parseInt(parts[1]);
                            } catch(NumberFormatException unused) {
                                // stays at default repeater port
                            }
                        }
                        // parse ID
                        String repeaterId = idInputText.getText().toString();
                        // sanity-check
                        if (host.isEmpty() || repeaterId.isEmpty()) {
                            Toast.makeText(MainActivity.this, getString(R.string.main_activity_repeater_vnc_input_missing), Toast.LENGTH_LONG).show();
                            return;
                        }
                        // done
                        Log.d(TAG, "repeater vnc " + host + ":" + port + ":" + repeaterId);
                        mLastMainServiceRequestId = UUID.randomUUID().toString();
                        mLastRepeaterHost = host;
                        mLastRepeaterPort = port;
                        mLastRepeaterId = repeaterId;
                        Intent request = new Intent(MainActivity.this, VNCService.class);
                        request.setAction(VNCService.ACTION_CONNECT_REPEATER);
                        request.putExtra(VNCService.EXTRA_HOST, host);
                        request.putExtra(VNCService.EXTRA_PORT, port);
                        request.putExtra(VNCService.EXTRA_REPEATER_ID, repeaterId);
                        request.putExtra(VNCService.EXTRA_REQUEST_ID, mLastMainServiceRequestId);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(request);
                        } else {
                            startService(request);
                        }
                        // show a progress dialog
                        ProgressBar progressBar = new ProgressBar(this);
                        progressBar.setPadding(0,0,0, (int) (30 * getResources().getDisplayMetrics().density));
                        mOutgoingConnectionWaitDialog = new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setTitle(R.string.main_activity_repeater_vnc_button)
                                .setMessage(getString(R.string.main_activity_connecting_to, host + ":" + port + " - " + repeaterId))
                                .setView(progressBar)
                                .show();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel())
                    .create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        });


        final EditText port = findViewById(R.id.settings_port);
        if(prefs.getInt(Constants.PREFS_KEY_SETTINGS_PORT, mDefaults.getPort()) < 0) {
            port.setHint(R.string.main_activity_settings_port_not_listening);
        } else {
            port.setText(String.valueOf(prefs.getInt(Constants.PREFS_KEY_SETTINGS_PORT, mDefaults.getPort())));
        }
        port.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putInt(Constants.PREFS_KEY_SETTINGS_PORT, Integer.parseInt(charSequence.toString()));
                    ed.apply();
                } catch(NumberFormatException e) {
                    // nop
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(port.getText().length() == 0) {
                    // hint that not listening
                    port.setHint(R.string.main_activity_settings_port_not_listening);
                    // and set default
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putInt(Constants.PREFS_KEY_SETTINGS_PORT, -1);
                    ed.apply();
                }
            }
        });
        port.setOnFocusChangeListener((v, hasFocus) -> {
            // move cursor to end of text
            port.setSelection(port.getText().length());
        });

        final EditText password = findViewById(R.id.settings_password);
        password.setText(prefs.getString(Constants.PREFS_KEY_SETTINGS_PASSWORD, mDefaults.getPassword()));
        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // only save new value if it differs from the default and was not saved before
                if(!(prefs.getString(Constants.PREFS_KEY_SETTINGS_PASSWORD, null) == null && charSequence.toString().equals(mDefaults.getPassword()))) {
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putString(Constants.PREFS_KEY_SETTINGS_PASSWORD, charSequence.toString());
                    ed.apply();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        // show/hide password on focus change. NB that this triggers onTextChanged above, so we have
        // to take special precautions there.
        password.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                password.setTransformationMethod(new SingleLineTransformationMethod());
            } else {
                password.setTransformationMethod(new PasswordTransformationMethod());
            }
            // move cursor to end of text
            password.setSelection(password.getText().length());
        });

        final EditText accessKey = findViewById(R.id.settings_access_key);
        accessKey.setText(prefs.getString(Constants.PREFS_KEY_SETTINGS_ACCESS_KEY, mDefaults.getAccessKey()));
        accessKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // only save new value if it differs from the default and was not saved before
                if(!(prefs.getString(Constants.PREFS_KEY_SETTINGS_ACCESS_KEY, null) == null && charSequence.toString().equals(mDefaults.getAccessKey()))) {
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putString(Constants.PREFS_KEY_SETTINGS_ACCESS_KEY, charSequence.toString());
                    ed.apply();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        // show/hide access key on focus change. NB that this triggers onTextChanged above, so we have
        // to take special precautions there.
        accessKey.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                accessKey.setTransformationMethod(new SingleLineTransformationMethod());
            } else {
                accessKey.setTransformationMethod(new PasswordTransformationMethod());
                // if value just saved was empty, reset preference and UI back to default
                String savedAccessKey = prefs.getString(Constants.PREFS_KEY_SETTINGS_ACCESS_KEY, null);
                if(savedAccessKey != null && savedAccessKey.isEmpty()) {
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putString(Constants.PREFS_KEY_SETTINGS_ACCESS_KEY, mDefaults.getAccessKey());
                    ed.apply();
                    accessKey.setText(mDefaults.getAccessKey());
                }
            }
            // move cursor to end of text
            accessKey.setSelection(accessKey.getText().length());
        });

        final EditText startOnBootDelay = findViewById(R.id.settings_start_on_boot_delay);
        startOnBootDelay.setText(String.valueOf(prefs.getInt(Constants.PREFS_KEY_SETTINGS_START_ON_BOOT_DELAY, mDefaults.getStartOnBootDelay())));
        startOnBootDelay.setEnabled(prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_START_ON_BOOT, mDefaults.getStartOnBoot()));
        startOnBootDelay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putInt(Constants.PREFS_KEY_SETTINGS_START_ON_BOOT_DELAY, Integer.parseInt(charSequence.toString()));
                    ed.apply();
                } catch(NumberFormatException e) {
                    // nop
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // if value just saved was empty, reset preference and UI back to default
                String savedAccessKey = prefs.getString(Constants.PREFS_KEY_SETTINGS_ACCESS_KEY, null);
                if(savedAccessKey != null && savedAccessKey.isEmpty()) {
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putString(Constants.PREFS_KEY_SETTINGS_ACCESS_KEY, mDefaults.getAccessKey());
                    ed.apply();
                    accessKey.setText(mDefaults.getAccessKey());
                }

                if(startOnBootDelay.getText().length() == 0) {
                    // reset to default
                    startOnBootDelay.setHint(String.valueOf(mDefaults.getStartOnBootDelay()));
                    // and remove preference
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.remove(Constants.PREFS_KEY_SETTINGS_START_ON_BOOT_DELAY);
                    ed.apply();
                }
            }
        });
        // move cursor to end of text
        startOnBootDelay.setOnFocusChangeListener((v, hasFocus) -> startOnBootDelay.setSelection(startOnBootDelay.getText().length()));

        final SwitchMaterial startOnBoot = findViewById(R.id.settings_start_on_boot);
        startOnBoot.setChecked(prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_START_ON_BOOT, mDefaults.getStartOnBoot()));
        startOnBoot.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean(Constants.PREFS_KEY_SETTINGS_START_ON_BOOT, b);
            ed.apply();
            startOnBootDelay.setEnabled(b);
        });

        if(Build.VERSION.SDK_INT >= 33) {
            // no use asking for permission on Android 13+, always denied.
            // users can always read/write Documents and Downloads tough.
            findViewById(R.id.settings_row_file_transfer).setVisibility(View.GONE);
        } else {
            final SwitchMaterial fileTransfer = findViewById(R.id.settings_file_transfer);
            fileTransfer.setChecked(prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_FILE_TRANSFER, mDefaults.getFileTransfer()));
            fileTransfer.setOnCheckedChangeListener((compoundButton, b) -> {
                SharedPreferences.Editor ed = prefs.edit();
                ed.putBoolean(Constants.PREFS_KEY_SETTINGS_FILE_TRANSFER, b);
                ed.apply();
            });
        }

        Slider scaling = findViewById(R.id.settings_scaling);
        scaling.setValue(prefs.getFloat(Constants.PREFS_KEY_SETTINGS_SCALING, mDefaults.getScaling())*100);
        scaling.setLabelFormatter(value -> Math.round(value) + " %");
        scaling.addOnChangeListener((slider, value, fromUser) -> {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putFloat(Constants.PREFS_KEY_SETTINGS_SCALING, value/100);
            ed.apply();
        });

        final SwitchMaterial showPointers = findViewById(R.id.settings_show_pointers);
        showPointers.setChecked(prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_SHOW_POINTERS, mDefaults.getShowPointers()));
        showPointers.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean(Constants.PREFS_KEY_SETTINGS_SHOW_POINTERS, b);
            ed.apply();
        });
        showPointers.setEnabled(!prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_VIEW_ONLY, mDefaults.getViewOnly()));

        final SwitchMaterial viewOnly = findViewById(R.id.settings_view_only);
        viewOnly.setChecked(prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_VIEW_ONLY, mDefaults.getViewOnly()));
        viewOnly.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean(Constants.PREFS_KEY_SETTINGS_VIEW_ONLY, b);
            ed.apply();
            // pointers depend on this one
            showPointers.setEnabled(!b);
        });

        TextView about = findViewById(R.id.about);
        // about.setText(getString(R.string.main_activity_about, BuildConfig.VERSION_NAME));

        mMainServiceBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (VNCService.ACTION_START.equals(intent.getAction())) {
                    if(intent.getBooleanExtra(VNCService.EXTRA_REQUEST_SUCCESS, false)) {
                        // was a successful START requested by anyone (but sent by VNCService, as the receiver is not exported!)
                        Log.d(TAG, "got VNCService started success event");
                        onServerStarted();
                    } else {
                        // was a failed START requested by anyone (but sent by VNCService, as the receiver is not exported!)
                        Log.d(TAG, "got VNCService started fail event");
                        // if it was, by us, re-enable the button!
                        mButtonToggle.setEnabled(true);
                        // let focus stay on button
                        mButtonToggle.requestFocus();
                    }
                }

                if (VNCService.ACTION_STOP.equals(intent.getAction())
                        && (intent.getBooleanExtra(VNCService.EXTRA_REQUEST_SUCCESS, true))) {
                    // was a successful STOP requested by anyone (but sent by VNCService, as the receiver is not exported!)
                    // or a STOP without any extras
                    Log.d(TAG, "got VNCService stopped event");
                    onServerStopped();
                }

                if (VNCService.ACTION_CONNECT_REVERSE.equals(intent.getAction())
                        && mLastMainServiceRequestId != null
                        && mLastMainServiceRequestId.equals(intent.getStringExtra(VNCService.EXTRA_REQUEST_ID))) {
                    // was a CONNECT_REVERSE requested by us
                    if (intent.getBooleanExtra(VNCService.EXTRA_REQUEST_SUCCESS, false)) {
                        Toast.makeText(MainActivity.this,
                                        getString(R.string.main_activity_reverse_vnc_success,
                                                mLastReverseHost,
                                                mLastReversePort),
                                        Toast.LENGTH_LONG)
                                .show();
                        SharedPreferences.Editor ed = prefs.edit();
                        ed.putString(PREFS_KEY_REVERSE_VNC_LAST_HOST,
                                mLastReverseHost + ":" + mLastReversePort);
                        ed.apply();
                    } else
                        Toast.makeText(MainActivity.this,
                                        getString(R.string.main_activity_reverse_vnc_fail,
                                                mLastReverseHost,
                                                mLastReversePort),
                                        Toast.LENGTH_LONG)
                                .show();

                    // reset this
                    mLastMainServiceRequestId = null;
                    try {
                        mOutgoingConnectionWaitDialog.dismiss();
                    } catch(NullPointerException ignored) {
                    }
                }

                if (VNCService.ACTION_CONNECT_REPEATER.equals(intent.getAction())
                        && mLastMainServiceRequestId != null
                        && mLastMainServiceRequestId.equals(intent.getStringExtra(VNCService.EXTRA_REQUEST_ID))) {
                    // was a CONNECT_REPEATER requested by us
                    if (intent.getBooleanExtra(VNCService.EXTRA_REQUEST_SUCCESS, false)) {
                        Toast.makeText(MainActivity.this,
                                        getString(R.string.main_activity_repeater_vnc_success,
                                                mLastRepeaterHost,
                                                mLastRepeaterPort,
                                                mLastRepeaterId),
                                        Toast.LENGTH_LONG)
                                .show();
                        SharedPreferences.Editor ed = prefs.edit();
                        ed.putString(PREFS_KEY_REPEATER_VNC_LAST_HOST,
                                mLastRepeaterHost + ":" + mLastRepeaterPort);
                        ed.putString(PREFS_KEY_REPEATER_VNC_LAST_ID,
                                mLastRepeaterId);
                        ed.apply();
                    }
                    else
                        Toast.makeText(MainActivity.this,
                                        getString(R.string.main_activity_repeater_vnc_fail,
                                                mLastRepeaterHost,
                                                mLastRepeaterPort,
                                                mLastRepeaterId),
                                        Toast.LENGTH_LONG)
                                .show();

                    // reset this
                    mLastMainServiceRequestId = null;
                    try {
                        mOutgoingConnectionWaitDialog.dismiss();
                    } catch(NullPointerException ignored) {
                    }
                }

            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(VNCService.ACTION_START);
        filter.addAction(VNCService.ACTION_STOP);
        filter.addAction(VNCService.ACTION_CONNECT_REVERSE);
        filter.addAction(VNCService.ACTION_CONNECT_REPEATER);
        // register the receiver as NOT_EXPORTED so it only receives broadcasts sent by VNCService,
        // not a malicious fake broadcaster like
        // `adb shell am broadcast -a net.christianbeier.com.appknox.knoxvnc.ACTION_STOP --ez net.christianbeier.com.appknox.knoxvnc.EXTRA_REQUEST_SUCCESS true`
        // for instance
        ContextCompat.registerReceiver(this, mMainServiceBroadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // setup UI initial state
        if (VNCService.isServerActive()) {
            Log.d(TAG, "Found server to be started");
            onServerStarted();
        } else {
            Log.d(TAG, "Found server to be stopped");
            onServerStopped();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();

        /*
            Update Input permission display.
         */
        TextView inputStatus = findViewById(R.id.permission_status_input);
        if(InputService.isConnected()) {
            inputStatus.setText(R.string.main_activity_granted);
            inputStatus.setTextColor(getColor(R.color.granted));
        } else {
            inputStatus.setText(R.string.main_activity_denied);
            inputStatus.setTextColor(getColor(R.color.denied));
        }


        /*
            Update File Access permission display. Only show on < Android 13.
         */
        if(Build.VERSION.SDK_INT < 33) {
            TextView fileAccessStatus = findViewById(R.id.permission_status_file_access);
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                fileAccessStatus.setText(R.string.main_activity_granted);
                fileAccessStatus.setTextColor(getColor(R.color.granted));
            } else {
                fileAccessStatus.setText(R.string.main_activity_denied);
                fileAccessStatus.setTextColor(getColor(R.color.denied));
            }
        } else {
            findViewById(R.id.permission_row_file_access).setVisibility(View.GONE);
        }

        /*
           Update Screen Capturing permission display.
        */
        TextView screenCapturingStatus = findViewById(R.id.permission_status_screen_capturing);
        if(VNCService.isMediaProjectionEnabled() == 1) {
            screenCapturingStatus.setText(R.string.main_activity_granted);
            screenCapturingStatus.setTextColor(getColor(R.color.granted));
        }
        if(VNCService.isMediaProjectionEnabled() == 0) {
            screenCapturingStatus.setText(R.string.main_activity_denied);
            screenCapturingStatus.setTextColor(getColor(R.color.denied));
        }
        if(VNCService.isMediaProjectionEnabled() == -1) {
            screenCapturingStatus.setText(R.string.main_activity_unknown);
            screenCapturingStatus.setTextColor(getColor(android.R.color.darker_gray));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unregisterReceiver(mMainServiceBroadcastReceiver);
    }

    private void onServerStarted() {
        mButtonToggle.post(() -> {
            mButtonToggle.setText(R.string.stop);
            mButtonToggle.setEnabled(true);
            // let focus stay on button
            mButtonToggle.requestFocus();
        });

        if(VNCService.getPort() >= 0) {
            // uhh there must be a nice functional way for this
            ArrayList<String> hosts = VNCService.getIPv4s();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hosts.size(); ++i) {
                sb.append(hosts.get(i) + ":" + VNCService.getPort());
                if (i != hosts.size() - 1)
                    sb.append(" ").append(getString(R.string.or)).append(" ");
            }
            mAddress.post(() -> mAddress.setText(getString(R.string.main_activity_address) + " " + sb));
        } else {
            mAddress.post(() -> mAddress.setText(R.string.main_activity_not_listening));
        }

        // show outbound connection interface
        findViewById(R.id.outbound_text).setVisibility(View.VISIBLE);
        findViewById(R.id.outbound_buttons).setVisibility(View.VISIBLE);

        // indicate that changing these settings does not have an effect when the server is running
        findViewById(R.id.settings_port).setEnabled(false);
        findViewById(R.id.settings_password).setEnabled(false);
        findViewById(R.id.settings_access_key).setEnabled(false);
        findViewById(R.id.settings_scaling).setEnabled(false);
        findViewById(R.id.settings_view_only).setEnabled(false);
        findViewById(R.id.settings_file_transfer).setEnabled(false);
        findViewById(R.id.settings_show_pointers).setEnabled(false);

        mIsMainServiceRunning = true;
    }

    private void onServerStopped() {
        mButtonToggle.post(() -> {
            mButtonToggle.setText(R.string.start);
            mButtonToggle.setEnabled(true);
            // let focus stay on button
            mButtonToggle.requestFocus();
        });
        mAddress.post(() -> mAddress.setText(""));

        // hide outbound connection interface
        findViewById(R.id.outbound_text).setVisibility(View.GONE);
        findViewById(R.id.outbound_buttons).setVisibility(View.GONE);

        // indicate that changing these settings does have an effect when the server is stopped
        findViewById(R.id.settings_port).setEnabled(true);
        findViewById(R.id.settings_password).setEnabled(true);
        findViewById(R.id.settings_access_key).setEnabled(true);
        findViewById(R.id.settings_scaling).setEnabled(true);
        findViewById(R.id.settings_view_only).setEnabled(true);
        findViewById(R.id.settings_file_transfer).setEnabled(true);
        if(!((SwitchMaterial)findViewById(R.id.settings_view_only)).isChecked()) {
            // pointers depend on view-only being disabled
            findViewById(R.id.settings_show_pointers).setEnabled(true);
        }

        mIsMainServiceRunning = false;
    }

}