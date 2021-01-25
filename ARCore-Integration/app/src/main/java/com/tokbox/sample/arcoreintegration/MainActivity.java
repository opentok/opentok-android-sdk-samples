package com.tokbox.sample.arcoreintegration;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.AugmentedFaceNode;
import com.opentok.android.*;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener, Session.SessionListener, PublisherKit.PublisherListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    // Tokbox variables
    private static final int RC_VIDEO_APP_PERM = 124;
    private Session mSession;
    private FrameLayout mPublisherViewContainer;

    // ARCore variables
    private FaceArFragment mFragment;
    private ModelRenderable mRenderable;
    private Texture mTexture;
    private Scene mScene;
    private ArSceneView mSceneView;

    private final HashMap<AugmentedFace, AugmentedFaceNode> mFaceNodeMap = new HashMap<>();

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_augmented_face);

        if (!checkIsSupportedDeviceOrFinish(this)) return;

        // Load the model Renderable from the resource
        ModelRenderable.builder()
                .setSource(this, R.raw.fox_face)
                .build()
                .thenAccept(modelRenderable -> {
                    mRenderable = modelRenderable;
                    modelRenderable.setShadowCaster(false);
                    modelRenderable.setShadowReceiver(false);
                });

        // Load the face mesh Texture
        Texture.builder()
                .setSource(this, R.drawable.fox_face_mesh_texture)
                .build()
                .thenAccept(texture -> mTexture = texture);

        requestPermissions();
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, permissions)) {
            // ARCore Initialisation
            mFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.publisher_ar);
            mSceneView = mFragment.getArSceneView();
            mSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
            mScene = mSceneView.getScene();
            mScene.addOnUpdateListener(this);

            // TokBox Initialisation
            mPublisherViewContainer = findViewById(R.id.publisher_container);
            mSession = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
            mSession.setSessionListener(this);
            mSession.connect(OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make a video calls", RC_VIDEO_APP_PERM, permissions);
        }
    }

    // SessionListener methods
    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");
        CustomVideoCapturer capturer = new CustomVideoCapturer(mFragment.getArSceneView());
        Publisher customPublisher = new Publisher.Builder(MainActivity.this)
                .name("publisher-capturer")
                .capturer(capturer)
                .build();

        customPublisher.setPublisherListener(this);
        mPublisherViewContainer.addView(customPublisher.getView());
        mSession.publish(customPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Session Disconnected");
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.e(LOG_TAG, "Session error: " + opentokError.getMessage());
    }

    // Scene.OnUpdateListener modules
    @Override
    public void onUpdate(FrameTime frameTime) {
        if (mRenderable == null || mTexture == null) return;
        Collection<AugmentedFace> faceList = mSceneView.getSession().getAllTrackables(AugmentedFace.class);

        // Make new AugmentedFaceNodes for any new faces.
        for (AugmentedFace face : faceList) {
            if (!mFaceNodeMap.containsKey(face)) {
                AugmentedFaceNode faceNode = new AugmentedFaceNode(face);
                faceNode.setParent(mScene);
                faceNode.setFaceRegionsRenderable(mRenderable);
                faceNode.setFaceMeshTexture(mTexture);
                mFaceNodeMap.put(face, faceNode);
            }
        }

        // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
        Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iter = mFaceNodeMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iter.next();
            AugmentedFace face = entry.getKey();
            if (face.getTrackingState() == TrackingState.STOPPED) {
                AugmentedFaceNode faceNode = entry.getValue();
                faceNode.setParent(null);
                iter.remove();
            }
        }
    }

    // PublisherListener methods
    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamCreated");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamDestroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.e(LOG_TAG, "Publisher error: " + opentokError.getMessage());
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (ArCoreApk.getInstance().checkAvailability(activity) == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(LOG_TAG, "Augmented Faces requires ARCore.");
            Toast.makeText(activity, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString = ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(LOG_TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        return true;
    }
}
