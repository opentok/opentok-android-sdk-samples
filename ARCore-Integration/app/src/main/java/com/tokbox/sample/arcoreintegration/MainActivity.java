package com.tokbox.sample.arcoreintegration;

import android.Manifest;
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
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPEN_GL_VERSION = 3.0;

    // Tokbox variables
    private static final int RC_VIDEO_APP_PERM = 124;
    private Session session;
    private FrameLayout publisherViewContainer;

    // ARCore variables
    private FaceArFragment faceFragment;
    private ModelRenderable modelRenderable;
    private Texture texture;
    private Scene scene;
    private ArSceneView arSceneView;

    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeMap = new HashMap<>();

    private PublisherKit.PublisherListener publisherListener = new PublisherKit.PublisherListener() {
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
    };

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {

            Log.i(LOG_TAG, "Session Connected");

            CustomVideoCapturer capturer = new CustomVideoCapturer(faceFragment.getArSceneView());
            Publisher custopublisher = new Publisher.Builder(MainActivity.this)
                    .name("publisher-capturer")
                    .capturer(capturer)
                    .build();

            custopublisher.setPublisherListener(publisherListener);
            publisherViewContainer.addView(custopublisher.getView());
            session.publish(custopublisher);
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
    };

    private Scene.OnUpdateListener onUpdateListener = new Scene.OnUpdateListener() {
        @Override
        public void onUpdate(FrameTime frameTime) {
            if (modelRenderable == null || texture == null) return;
            Collection<AugmentedFace> faceList = arSceneView.getSession().getAllTrackables(AugmentedFace.class);

            // Make new AugmentedFaceNodes for any new faces.
            for (AugmentedFace face : faceList) {
                if (!faceNodeMap.containsKey(face)) {
                    AugmentedFaceNode faceNode = new AugmentedFaceNode(face);
                    faceNode.setParent(scene);
                    faceNode.setFaceRegionsRenderable(modelRenderable);
                    faceNode.setFaceMeshTexture(texture);
                    faceNodeMap.put(face, faceNode);
                }
            }

            // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
            Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iter = faceNodeMap.entrySet().iterator();
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_augmented_face);

        OpenTokConfig.verifyConfig();

        if (!checkIsSupportedDevice()) {
            String message = "Augmented Faces requires ARCore";

            Log.e(LOG_TAG, message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            this.finish();
            return;
        }

        // Load the model Renderable from the resource
        ModelRenderable.builder()
                .setSource(this, R.raw.fox_face)
                .build()
                .thenAccept(modelRenderable -> {
                    this.modelRenderable = modelRenderable;
                    modelRenderable.setShadowCaster(false);
                    modelRenderable.setShadowReceiver(false);
                });

        // Load the face mesh Texture
        Texture.builder()
                .setSource(this, R.drawable.fox_face_mesh_texture)
                .build()
                .thenAccept(texture -> this.texture = texture);

        requestPermissions();
    }

    private boolean checkIsSupportedDevice() {
        if (ArCoreApk.getInstance().checkAvailability(this) == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(LOG_TAG, "Augmented Faces requires ARCore");
            Toast.makeText(this, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show();
            return false;
        }
        String openGlVersionString = ((ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();

        if (Double.parseDouble(openGlVersionString) < MIN_OPEN_GL_VERSION) {
            Log.e(LOG_TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(this, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] permissions = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, permissions)) {
            // ARCore Initialisation
            faceFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.publisher_ar);
            arSceneView = faceFragment.getArSceneView();
            arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
            scene = arSceneView.getScene();
            scene.addOnUpdateListener(onUpdateListener);

            // TokBox Initialisation
            publisherViewContainer = findViewById(R.id.publisher_container);
            session = new Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID).build();
            session.setSessionListener(sessionListener);
            session.connect(OpenTokConfig.TOKEN);
        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make a video calls", RC_VIDEO_APP_PERM, permissions);
        }
    }
}
