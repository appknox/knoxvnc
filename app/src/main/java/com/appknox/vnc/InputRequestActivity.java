package com.appknox.vnc;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class InputRequestActivity extends AppCompatActivity {

    private static final String TAG = "InputRequestActivity";
    private static final int REQUEST_INPUT = 43;
    static final String EXTRA_DO_NOT_START_MAIN_SERVICE_ON_FINISH = "do_not_start_main_service_on_finish";
    private boolean mDoNotStartMainServiceOnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().getBooleanExtra(EXTRA_DO_NOT_START_MAIN_SERVICE_ON_FINISH, false)) {
            mDoNotStartMainServiceOnFinish = true;
        }

        boolean startOnBootRequested = Build.VERSION.SDK_INT >= 30 && PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREFS_KEY_SETTINGS_START_ON_BOOT,  new Defaults(this).getStartOnBoot());
        boolean inputRequested = getIntent().hasExtra(VNCService.EXTRA_VIEW_ONLY)
                ? !getIntent().getBooleanExtra(VNCService.EXTRA_VIEW_ONLY, new Defaults(this).getViewOnly())
                : !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREFS_KEY_SETTINGS_VIEW_ONLY,  new Defaults(this).getViewOnly());

        Log.d(TAG, "onCreate: input requested: " + inputRequested + " start on boot requested: " + startOnBootRequested);

        if(!inputRequested && !startOnBootRequested) {
            postResultAndFinish(false);
            return;
        }

        int msg;
        if (inputRequested && startOnBootRequested) {
            // input and boot requested
            msg = R.string.input_a11y_msg_input_and_boot;
        } else if (inputRequested) {
            // input requested
            msg = R.string.input_a11y_msg_input;
        } else {
            // boot requested
            msg = R.string.input_a11y_msg_boot;
        }

        if(!InputService.isConnected()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.input_a11y_title)
                    .setMessage(R.string.input_a11y_msg)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

                        // highlight entry on some devices, see https://stackoverflow.com/a/63214655/361413
                        final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
                        final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";
                        Bundle bundle = new Bundle();
                        String showArgs = getPackageName() + "/" + InputService.class.getName();
                        bundle.putString(EXTRA_FRAGMENT_ARG_KEY, showArgs);
                        intent.putExtra(EXTRA_FRAGMENT_ARG_KEY, showArgs);
                        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle);

                        if (intent.resolveActivity(getPackageManager()) != null && !intent.resolveActivity(getPackageManager()).toString().contains("Stub"))
                            startActivityForResult(intent, REQUEST_INPUT);
                        else
                            new AlertDialog.Builder(InputRequestActivity.this)
                                    .setMessage(R.string.input_a11y_act_not_found_msg)
                                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                        Intent generalSettingsIntent = new Intent(Settings.ACTION_SETTINGS);
                                        try {
                                            startActivityForResult(generalSettingsIntent, REQUEST_INPUT);
                                        } catch(ActivityNotFoundException ignored) {
                                            // This should not happen, but there were crashes reported from flaky devices
                                            // so in this case do nothing instead of crashing.
                                        }
                                    })
                                    .show();
                    })
                    .setNegativeButton(getString(R.string.no), (dialog, which) -> postResultAndFinish(false))
                    .show();
        } else {
            postResultAndFinish(true);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INPUT) {
            Log.d(TAG, "onActivityResult");
            postResultAndFinish(InputService.isConnected());
        }
    }

    private void postResultAndFinish(boolean isA11yEnabled) {

        if (isA11yEnabled)
            Log.i(TAG, "a11y enabled");
        else
            Log.i(TAG, "a11y disabled");

        Intent intent = new Intent(this, VNCService.class);
        intent.setAction(VNCService.ACTION_HANDLE_INPUT_RESULT);
        intent.putExtra(VNCService.EXTRA_INPUT_RESULT, isA11yEnabled);
        startService(intent);
        finish();
    }

}