package com.opentok.android.demo.opentoksamples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Main demo app for getting started with the OpenTok Android SDK. It contains:
 * - a basic hello-world activity - a basic hello-world activity with control
 * bar with action buttons to switch camera, audio mute and end call. - a basic
 * hello-world activity with a customer video capturer out of SDK.
 */
public class OpenTokSamples extends Activity {

    private static final String LOGTAG = "demo-opentok-sdk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        final ListView listActivities = (ListView) findViewById(R.id.listview);
        String[] activityNames = {getString(R.string.helloworld),
                getString(R.string.helloworldui),
                getString(R.string.helloworldcapturer),
                getString(R.string.helloworldrenderer),
                getString(R.string.helloworldsubclassing),
                getString(R.string.voinceonly),
                getString(R.string.audiodevice),
                getString(R.string.helloworldemulator),
                getString(R.string.screensharing)};

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
                    startHelloWorldUI();
                } else if (2 == position) {
                    startHelloWorldVideoCapturer();
                } else if (3 == position) {
                    startHelloWorldVideoRenderer();
                } else if (4 == position) {
                    startHelloWorldSubclassing();
                } else if (5 == position) {
                    startVoiceOnly();
                } else if (6 == position) {
                    startAudioDevice();
                } else if (7 == position) {
                    startHelloWorldEmulator();
                } else if (8 == position) {
                    startScreensharing();
                } else {
                    Log.wtf(LOGTAG, "unknown item clicked?");
                }
            }
        });
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
     * Starts the Hello-World app using a custom audio device. See
     * AudioDeviceActivity.java
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
     * Starts the Hello-World app using a custom audio device. See
     * AudioDeviceActivity.java
     */
    public void startScreensharing() {

        Log.i(LOGTAG, "starting hello-world app for screensharing");

        Intent intent = new Intent(OpenTokSamples.this,
                ScreenSharingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }
}