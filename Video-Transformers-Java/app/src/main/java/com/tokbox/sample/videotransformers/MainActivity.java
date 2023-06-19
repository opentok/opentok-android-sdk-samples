package com.tokbox.sample.videotransformers;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Image;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.tokbox.sample.videotransformers.network.APIService;
import com.tokbox.sample.videotransformers.network.GetSessionResponse;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_CODE = 124;

    private Retrofit retrofit;
    private APIService apiService;

    private Session session;
    private Publisher publisher;
    private Subscriber subscriber;

    private FrameLayout publisherViewContainer;
    private FrameLayout subscriberViewContainer;
    private Context context;

    private Button buttonVideoTransformers;

    public ArrayList<PublisherKit.VideoTransformer> videoTransformers = new ArrayList<>();

    private PublisherKit.PublisherListener publisherListener = new PublisherKit.PublisherListener() {
        @Override
        public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Own stream " + stream.getStreamId());
        }

        @Override
        public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream " + stream.getStreamId());
        }

        @Override
        public void onError(PublisherKit publisherKit, OpentokError opentokError) {
            finishWithMessage("PublisherKit onError: " + opentokError.getMessage());
        }
    };

    private Session.SessionListener sessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "onConnected: Connected to session: " + session.getSessionId());

            publisher = new Publisher.Builder(MainActivity.this).build();
            publisher.setPublisherListener(publisherListener);
            publisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            
            publisherViewContainer.addView(publisher.getView());

            if (publisher.getView() instanceof GLSurfaceView) {
                ((GLSurfaceView) publisher.getView()).setZOrderOnTop(true);
            }

            session.publish(publisher);
        }

        @Override
        public void onDisconnected(Session session) {
            Log.d(TAG, "onDisconnected: Disconnected from session: " + session.getSessionId());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(TAG, "onStreamReceived: New Stream Received " + stream.getStreamId() + " in session: " + session.getSessionId());

            if (subscriber == null) {
                subscriber = new Subscriber.Builder(MainActivity.this, stream).build();
                subscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
                subscriber.setSubscriberListener(subscriberListener);
                session.subscribe(subscriber);
                subscriberViewContainer.addView(subscriber.getView());
            }
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream Dropped: " + stream.getStreamId() + " in session: " + session.getSessionId());

            if (subscriber != null) {
                subscriber = null;
                subscriberViewContainer.removeAllViews();
            }
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }
    };

    SubscriberKit.SubscriberListener subscriberListener = new SubscriberKit.SubscriberListener() {
        @Override
        public void onConnected(SubscriberKit subscriberKit) {
            Log.d(TAG, "onConnected: Subscriber connected. Stream: " + subscriberKit.getStream().getStreamId());
        }

        @Override
        public void onDisconnected(SubscriberKit subscriberKit) {
            Log.d(TAG, "onDisconnected: Subscriber disconnected. Stream: " + subscriberKit.getStream().getStreamId());
        }

        @Override
        public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
            finishWithMessage("SubscriberKit onError: " + opentokError.getMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_main);
        buttonVideoTransformers = findViewById(R.id.setvideotransformers);

        publisherViewContainer = findViewById(R.id.publisher_container);
        subscriberViewContainer = findViewById(R.id.subscriber_container);

        requestPermissions();

        // Get the image in bitmap format
        image = BitmapFactory.decodeResource(context.getResources(), R.drawable.vonage_logo);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (session != null) {
            session.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session != null) {
            session.onResume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ": " + perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        finishWithMessage("onPermissionsDenied: " + requestCode + ": " + perms);
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        if (EasyPermissions.hasPermissions(this, perms)) {

            if (ServerConfig.hasChatServerUrl()) {
                // Custom server URL exists - retrieve session config
                if(!ServerConfig.isValid()) {
                    finishWithMessage("Invalid chat server url: " + ServerConfig.CHAT_SERVER_URL);
                    return;
                }

                initRetrofit();
                getSession();
            } else {
                // Use hardcoded session config
                if(!OpenTokConfig.isValid()) {
                    finishWithMessage("Invalid OpenTokConfig. " + OpenTokConfig.getDescription());
                    return;
                }

                initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN);
            }
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, perms);
        }
    }

    /* Make a request for session data */
    private void getSession() {
        Log.i(TAG, "getSession");

        Call<GetSessionResponse> call = apiService.getSession();

        call.enqueue(new Callback<GetSessionResponse>() {
            @Override
            public void onResponse(Call<GetSessionResponse> call, Response<GetSessionResponse> response) {
                GetSessionResponse body = response.body();
                initializeSession(body.apiKey, body.sessionId, body.token);
            }

            @Override
            public void onFailure(Call<GetSessionResponse> call, Throwable t) {
                throw new RuntimeException(t.getMessage());
            }
        });
    }

    private void initializeSession(String apiKey, String sessionId, String token) {
        Log.i(TAG, "apiKey: " + apiKey);
        Log.i(TAG, "sessionId: " + sessionId);
        Log.i(TAG, "token: " + token);

        /*
        The context used depends on the specific use case, but usually, it is desired for the session to
        live outside of the Activity e.g: live between activities. For a production applications,
        it's convenient to use Application context instead of Activity context.
         */
        session = new Session.Builder(this, apiKey, sessionId).build();
        session.setSessionListener(sessionListener);
        session.connect(token);
    }

    private void initRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(ServerConfig.CHAT_SERVER_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(APIService.class);
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }

    // Logo to be loaded
    Bitmap image;

    private customVideoTransformer logoTransformer = new customVideoTransformer();

    public class customVideoTransformer implements PublisherKit.CustomVideoTransformer {

        public Bitmap resizeImage(Bitmap image, int width, int height) {
            return Bitmap.createScaledBitmap(image, width, height, true);
        }

        @Override
        public void onTransform(BaseVideoRenderer.Frame frame){

            // Obtain the Y  plane of the video frame
            ByteBuffer yPlane = frame.getYplane();

            // Get the dimensions of the video frame
            int videoWidth = frame.getWidth();
            int videoHeight = frame.getHeight();

            // Calculate the desired size of the image
            int desiredWidth = videoWidth / 8; // Adjust this value as needed
            int desiredHeight = (int) (image.getHeight() * ((float) desiredWidth / image.getWidth()));

            // Resize the image to the desired size
            image = resizeImage(image, desiredWidth, desiredHeight);

            int logoWidth = image.getWidth();
            int logoHeight = image.getHeight();

            // Location of the image (center of video)
            int logoPositionX = videoWidth * 1/5 - logoWidth; // Adjust this as needed for the desired position
            int logoPositionY = videoHeight * 9/10 - logoHeight; // Adjust this as needed for the desired position

            // Overlay the logo on the video frame
            for (int y = 0; y < logoHeight; y++) {
                for (int x = 0; x < logoWidth; x++) {
                    int frameOffset = (logoPositionY + y) * videoWidth + (logoPositionX + x);

                    // Get the logo pixel color
                    int logoPixel = image.getPixel(x, y);

                    // Extract the color channels (ARGB)
                    int logoAlpha = (logoPixel >> 24) & 0xFF;
                    int logoRed = (logoPixel >> 16) & 0xFF;

                    // Overlay the logo pixel on the video frame
                    int framePixel = yPlane.get(frameOffset) & 0xFF;

                    // Calculate the blended pixel value
                    int blendedPixel = ((logoAlpha * logoRed + (255 - logoAlpha) * framePixel) / 255) & 0xFF;

                    // Set the blended pixel value in the video frame
                    yPlane.put(frameOffset, (byte) blendedPixel);
                }
            }
        }
    }

    private boolean isSet = false;
    public void SetVideoTransformers(View view) {
        if(!isSet) {
            videoTransformers.clear();
            PublisherKit.VideoTransformer backgroundBlur = publisher.new VideoTransformer("BackgroundBlur", "{\"radius\":\"High\"}");
            PublisherKit.VideoTransformer myCustomTransformer = publisher.new VideoTransformer("myTransformer", logoTransformer);
            videoTransformers.add(backgroundBlur);
            videoTransformers.add(myCustomTransformer);
            publisher.setVideoTransformers(videoTransformers);
            isSet = true;
            buttonVideoTransformers.setText("Reset");
        } else {
            ResetVideoTransformers();
            isSet = false;
            buttonVideoTransformers.setText("Set");
        }

    }

    public void ResetVideoTransformers() {
        videoTransformers.clear();
        publisher.setVideoTransformers(videoTransformers);
    }
}
