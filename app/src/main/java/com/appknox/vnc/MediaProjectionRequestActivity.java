package com.appknox.vnc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;

public class MediaProjectionRequestActivity extends AppCompatActivity {

    private static final String TAG = "MPRequestActivity";
    private static final int REQUEST_MEDIA_PROJECTION = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        Log.i(TAG, "Requesting confirmation");
        // This initiates a prompt dialog for the user to confirm screen projection.
        startActivityForResult(
                mMediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK)
                Log.i(TAG, "User cancelled");
            else
                Log.i(TAG, "User acknowledged");

            Intent intent = new Intent(this, VNCService.class);
            intent.setAction(VNCService.ACTION_HANDLE_MEDIA_PROJECTION_RESULT);
            intent.putExtra(VNCService.EXTRA_MEDIA_PROJECTION_RESULT_CODE, resultCode);
            intent.putExtra(VNCService.EXTRA_MEDIA_PROJECTION_RESULT_DATA, data);
            startService(intent);
            finish();
        }
    }

}