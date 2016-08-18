package com.opentok.android.demo.opentoksamples;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * Main demo app for getting started with the OpenTok Android SDK. It contains:
 * - a basic hello-world activity - a basic hello-world activity with control
 * bar with action buttons to switch camera, audio mute and end call. - a basic
 * hello-world activity with a customer video capturer out of SDK.
 */
public class OpenTokSamples extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String LOGTAG = "demo-opentok-sdk";

    private static class PermissionRecord {
        private String  mPermission;
        private int     mRequestMsgId;
        private int     mGrantMsgId;
        private boolean mIsGranted;

        public PermissionRecord(String permission, int requestMsgId, int grantMsgId) {
            mPermission     = permission;
            mRequestMsgId   = requestMsgId;
            mGrantMsgId     = grantMsgId;
            mIsGranted      = false;
        }

        public String getPermission() {
            return mPermission;
        }

        public int getRequestMsgId() {
            return mRequestMsgId;
        }

        public int getGrantMsgId() {
            return mGrantMsgId;
        }

        public boolean isPermissionGranted() {
            return mIsGranted;
        }

        public void grantPermission() {
            mIsGranted = true;
        }
    }

    private static final int REQUEST_CAMERA         = 0xf5;
    private static final int REQUEST_INTERNET       = 0x11;
    private static final int REQUEST_RECORDAUDIO    = 0x35;
    private static final int REQUEST_AUDIOSETTINGS  = 0xe9;
    private static final int WRITE_EXTERNALSTORAGE  = 0x50;

    private static final SparseArray<PermissionRecord> mPermissionState = new SparseArray<PermissionRecord>() {
        {
            append( REQUEST_CAMERA,
                    new PermissionRecord(
                        Manifest.permission.CAMERA,
                        R.string.permission_camera_rationale,
                        R.string.permission_camera_granted
                    )
            );
            append( REQUEST_INTERNET,
                    new PermissionRecord(
                        Manifest.permission.INTERNET,
                        R.string.permission_internet_rationale,
                        R.string.permission_internet_granted
                    )
            );
            append( REQUEST_RECORDAUDIO,
                    new PermissionRecord(
                        Manifest.permission.RECORD_AUDIO,
                        R.string.permission_audiorecord_rationale,
                        R.string.permission_audiorecord_granted
                    )
            );
            append( REQUEST_AUDIOSETTINGS,
                    new PermissionRecord(
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        R.string.permission_audiosettings_rationale,
                        R.string.permission_audiosettings_granted
                    )
            );
            append( WRITE_EXTERNALSTORAGE,
                    new PermissionRecord(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        R.string.permission_writeexternalstorage_rationale,
                        R.string.permission_writeexternalstorage_granted
                    )
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);


        final ListView listActivities = (ListView) findViewById(R.id.listview);
        String[] activityNames = {getString(R.string.helloworld),
                "Publisher Preview",
                getString(R.string.helloworldui),
                getString(R.string.helloworldcapturer),
                getString(R.string.helloworldrenderer),
                getString(R.string.helloworldsubclassing),
                getString(R.string.voinceonly),
                getString(R.string.audiodevice),
                getString(R.string.helloworldemulator),
                getString(R.string.screensharing),
                getString(R.string.defaultcameracapturer),
                getString(R.string.screenshot)};

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, activityNames);

        listActivities.setAdapter(adapter);
        listActivities.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position,
                                    long id) {
                // these positions are hard-coded to some example activities,
                // they match
                // the array contents of activityNames above.
                if (0 == position) {
                    startHelloWorld();
                } else if (1 == position) {
                    startPublisherPreview();
                } else if (2 == position) {
                    startHelloWorldUI();
                } else if (3 == position) {
                    startHelloWorldVideoCapturer();
                } else if (4 == position) {
                    startHelloWorldVideoRenderer();
                } else if (5 == position) {
                    startHelloWorldSubclassing();
                } else if (6 == position) {
                    startVoiceOnly();
                } else if (7 == position) {
                    startAudioDevice();
                } else if (8 == position) {
                    startHelloWorldEmulator();
                } else if (9 == position) {
                    startScreensharing();
                } else if (10 == position) {
                    startDefaultCameraCapturer();
                } else if (11 == position) {
                    startScreenshot();
                } else {
                    Log.wtf(LOGTAG, "unknown item clicked?");
                }
            }
        });
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(0);
        } else {
            // hide button for settings if not atleast android m
            Button settings_btn = (Button)findViewById(R.id.btn_system_settings);
            settings_btn.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Starts the Hello-World demo app. See OpenTokHelloWorld.java
     */
    public void startHelloWorld() {

        Log.i(LOGTAG, "starting hello-world app");

        Intent intent = new Intent(OpenTokSamples.this, HelloWorldActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void startPublisherPreview() {
        Log.i(LOGTAG, "starting hello-world app");

        Intent intent = new Intent(OpenTokSamples.this, PublisherPreviewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Starts the Hello-World app with UI. See OpenTokUI.java
     */
    public void startHelloWorldUI() {

        Log.i(LOGTAG, "starting hello-world app with UI");

        Intent intent = new Intent(OpenTokSamples.this, UIActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Starts the Hello-World app using a custom video capturer. See
     * VideoCapturerActivity.java
     */
    public void startHelloWorldVideoCapturer() {

        Log.i(LOGTAG,
                "starting hello-world app using a customer video capturer");

        Intent intent = new Intent(OpenTokSamples.this,
                VideoCapturerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Starts the Hello-World app using a custom video renderer. See
     * VideoRendererActivity.java
     */
    public void startHelloWorldVideoRenderer() {

        Log.i(LOGTAG,
                "starting hello-world app using a customer video capturer");

        Intent intent = new Intent(OpenTokSamples.this,
                VideoRendererActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Starts the Hello-World app using subclassing. See
     * MultipartyActivity.java
     */
    public void startHelloWorldSubclassing() {

        Log.i(LOGTAG, "starting hello-world app using subclassing");

        Intent intent = new Intent(OpenTokSamples.this,
                MultipartyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Starts the voice only Hello-World app. See
     * VoiceOnlyActivity.java
     */
    public void startVoiceOnly() {

        Log.i(LOGTAG, "starting hello-world app using voice only");

        Intent intent = new Intent(OpenTokSamples.this,
                VoiceOnlyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Starts the Hello-World app using a custom audio device. See
     * AudioDeviceActivity.java
     */
    public void startAudioDevice() {

        Log.i(LOGTAG, "starting hello-world app using a custom audio device");

        Intent intent = new Intent(OpenTokSamples.this,
                AudioDeviceActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Starts the Hello-World app in the emulator. See
     * EmulatorActivity.java
     */
    public void startHelloWorldEmulator() {

        Log.i(LOGTAG, "starting hello-world app for Android emulator");

        Intent intent = new Intent(OpenTokSamples.this,
                EmulatorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Starts the Hello-World app to share a website. See
     * ScreenSharingActivity.java
     */
    public void startScreensharing() {

        Log.i(LOGTAG, "starting hello-world app for screensharing");

        Intent intent = new Intent(OpenTokSamples.this,
                ScreenSharingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Starts the Hello-World app using the default video capturer feature. See
     * DefaultCameraCapturerActivity.java
     */
    public void startDefaultCameraCapturer() {

        Log.i(LOGTAG, "starting hello-world app for default video capturer setting a preferred resolution and frame rate");

        Intent intent = new Intent(OpenTokSamples.this,
                DefaultCameraCapturerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Starts the Hello-World app using a custom video renderer with the screenshot option. See
     * ScreenshotActivity.java
     */
    public void startScreenshot() {

        Log.i(LOGTAG, "starting hello-world app with screenshot option");

        Intent intent = new Intent(OpenTokSamples.this,
                ScreenshotActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        final int               requestKey  = requestCode;
        final View              layout      = findViewById(R.id.listview);
        final PermissionRecord  permission  = mPermissionState.get(requestCode);
        Snackbar.Callback       snackBarCB  = new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                requestPermissions(mPermissionState.indexOfKey(requestKey) + 1);
            }
        };

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permission.isPermissionGranted();
            Snackbar.make(
                    layout,
                    permission.getGrantMsgId(),
                    Snackbar.LENGTH_SHORT
            ).setCallback(snackBarCB).show();
        } else {
            Snackbar.make(
                    layout,
                    R.string.permission_notgranted,
                    Snackbar.LENGTH_SHORT
            ).setCallback(snackBarCB).show();
        }
    }

    private void requestPermissions(int permissionIndex) {

        View layout = findViewById(R.id.listview);

        if (permissionIndex < mPermissionState.size()) {
            final PermissionRecord permissionInfo = mPermissionState.valueAt(permissionIndex);
            final int permissionTag = mPermissionState.keyAt(permissionIndex);

            Log.d(LOGTAG, "Requesting permission: " + permissionInfo.getPermission());
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permissionInfo.getPermission())) {
                final String permissionArray[] = new String[]{permissionInfo.getPermission()};
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionInfo.getPermission())) {
                    Snackbar.make(
                            layout,
                            permissionInfo.getRequestMsgId(),
                            Snackbar.LENGTH_INDEFINITE
                    ).setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(
                                    OpenTokSamples.this,
                                    permissionArray,
                                    permissionTag
                            );
                        }
                    }).show();
                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            permissionArray,
                            permissionTag
                    );
                }
            } else {
                permissionInfo.grantPermission();
                requestPermissions(permissionIndex + 1);
            }
        }
    }

    public void onSystemSettingsClick(View v) {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(i);

    }
}