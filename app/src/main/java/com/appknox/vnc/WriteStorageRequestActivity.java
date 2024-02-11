package com.appknox.vnc;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class WriteStorageRequestActivity extends AppCompatActivity {

    private static final String TAG = "WriteStorageRequestActivity";
    private static final int REQUEST_WRITE_STORAGE = 44;
    private static final String PREFS_KEY_PERMISSION_ASKED_BEFORE = "write_storage_permission_asked_before";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if file transfer not wanted, bail out early without bothering the user
        if(!getIntent().getBooleanExtra(VNCService.EXTRA_FILE_TRANSFER, new Defaults(this).getFileTransfer())) {
            postResultAndFinish(false);
            return;
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Has no permission! Ask!");
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            /*
                As per as per https://stackoverflow.com/a/34612503/361413 shouldShowRequestPermissionRationale()
                returns false also if user was never asked, so keep track of that with a shared preference. Ouch.
             */
            if (!prefs.getBoolean(PREFS_KEY_PERMISSION_ASKED_BEFORE, false) || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.write_storage_title)
                        .setMessage(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? R.string.write_storage_msg_android_11 : R.string.write_storage_msg)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                            SharedPreferences.Editor ed = prefs.edit();
                            ed.putBoolean(PREFS_KEY_PERMISSION_ASKED_BEFORE, true);
                            ed.apply();
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> postResultAndFinish(false))
                        .show();
            } else {
                postResultAndFinish(false);
            }
        } else {
            Log.i(TAG, "Permission already given!");
            postResultAndFinish(true);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            postResultAndFinish(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void postResultAndFinish(boolean isPermissionGiven) {

        if (isPermissionGiven)
            Log.i(TAG, "permission granted");
        else
            Log.i(TAG, "permission denied");

        Intent intent = new Intent(this, VNCService.class);
        intent.setAction(VNCService.ACTION_HANDLE_WRITE_STORAGE_RESULT);
        intent.putExtra(VNCService.EXTRA_WRITE_STORAGE_RESULT, isPermissionGiven);
        startService(intent);
        finish();
    }

}