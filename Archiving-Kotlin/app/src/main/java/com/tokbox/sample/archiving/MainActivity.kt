package com.tokbox.sample.archiving

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.PublisherKit.PublisherListener
import com.opentok.android.Session
import com.opentok.android.Session.ArchiveListener
import com.opentok.android.Session.SessionListener
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit
import com.opentok.android.SubscriberKit.SubscriberListener
import com.tokbox.sample.archiving.MainActivity
import com.tokbox.sample.archiving.ServerConfig.isValid
import com.tokbox.sample.archiving.network.APIService
import com.tokbox.sample.archiving.network.EmptyCallback
import com.tokbox.sample.archiving.network.GetSessionResponse
import com.tokbox.sample.archiving.network.StartArchiveRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : AppCompatActivity(), PermissionCallbacks {
    private lateinit var retrofit: Retrofit
    private lateinit var apiService: APIService

    private var sessionId: String? = null
    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null
    private var currentArchiveId: String? = null
    private var playableArchiveId: String? = null
    private lateinit var publisherViewContainer: FrameLayout
    private lateinit var subscriberViewContainer: FrameLayout
    private lateinit var archivingIndicatorView: ImageView
    private var menu: Menu? = null
    private val publisherListener: PublisherListener = object : PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.i(TAG, "Publisher Stream Created")
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.i(TAG, "Publisher Stream Destroyed")
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            finishWithMessage("PublisherKit error: " + opentokError.message)
        }
    }
    private val sessionListener: SessionListener = object : SessionListener {
        override fun onConnected(session: Session) {
            Log.i(TAG, "Session Connected")
            publisher = Publisher.Builder(this@MainActivity).build().also {
                it.setPublisherListener(publisherListener)
                it.renderer.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            }

            publisherViewContainer.addView(publisher?.view, 0)
            session.publish(publisher)
            setStartArchiveEnabled(true)
        }

        override fun onDisconnected(session: Session) {
            Log.i(TAG, "Session Disconnected")

            setStartArchiveEnabled(false)
            setStopArchiveEnabled(false)
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.i(TAG, "Stream Received")
            if (subscriber == null) {
                subscriber = Subscriber.Builder(this@MainActivity, stream).build().also {
                    it.setSubscriberListener(subscriberListener)
                    it.renderer.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
                    session.subscribe(it)
                }
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.i(TAG, "Stream Dropped")

            if (subscriber != null) {
                subscriber = null
                subscriberViewContainer.removeAllViews()
            }
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: " + opentokError.message)
        }
    }
    private val subscriberListener: SubscriberListener = object : SubscriberListener {
        override fun onConnected(subscriberKit: SubscriberKit) {
            Log.i(TAG, "Subscriber Connected")

            subscriberViewContainer.addView(subscriber?.view)
        }

        override fun onDisconnected(subscriberKit: SubscriberKit) {
            Log.i(TAG, "Subscriber Disconnected")
        }

        override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {
            finishWithMessage("SubscriberKit onError: " + opentokError.message)
        }
    }
    private val archiveListener: ArchiveListener = object : ArchiveListener {
        override fun onArchiveStarted(session: Session, archiveId: String, archiveName: String) {
            currentArchiveId = archiveId
            setStopArchiveEnabled(true)
            archivingIndicatorView.visibility = View.VISIBLE
        }

        override fun onArchiveStopped(session: Session, archiveId: String) {
            playableArchiveId = archiveId
            currentArchiveId = null
            setPlayArchiveEnabled(true)
            setStartArchiveEnabled(true)
            archivingIndicatorView.visibility = View.INVISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isValid) {
            finishWithMessage("Invalid chat server url: " + ServerConfig.CHAT_SERVER_URL)
            return
        }

        publisherViewContainer = findViewById(R.id.publisher_container)
        subscriberViewContainer = findViewById(R.id.subscriber_container)
        archivingIndicatorView = findViewById(R.id.archiving_indicator_view)

        requestPermissions()
    }

    private fun initRetrofit() {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(ServerConfig.CHAT_SERVER_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()

        apiService = retrofit?.create(APIService::class.java)
    }

    override fun onPause() {
        super.onPause()
        session?.onPause()
    }

    override fun onResume() {
        super.onResume()
        session?.onResume()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d(TAG, "onPermissionsGranted:$requestCode: $perms")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        finishWithMessage("onPermissionsDenied: $requestCode: $perms")
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            initRetrofit()
            getSession()
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.rationale_video_app),
                PERMISSIONS_REQUEST_CODE,
                *perms
            )
        }
    }

    /* Make a request for session data */
    private fun getSession() {
        Log.i(TAG, "getSession")

        apiService.session?.enqueue(object : Callback<GetSessionResponse?> {
            override fun onResponse(call: Call<GetSessionResponse?>, response: Response<GetSessionResponse?>) {
                response.body()?.also {
                    initializeSession(it.apiKey, it.sessionId, it.token)
                }
            }

            override fun onFailure(call: Call<GetSessionResponse?>, t: Throwable) {
                throw RuntimeException(t.message)
            }
        })
    }

    private fun initializeSession(apiKey: String, sessionId: String, token: String) {
        Log.i(TAG, "apiKey: $apiKey")
        Log.i(TAG, "sessionId: $sessionId")
        Log.i(TAG, "token: $token")
        this.sessionId = sessionId

        session = Session.Builder(this, apiKey, sessionId).build().also {
            it.setSessionListener(sessionListener)
            it.setArchiveListener(archiveListener)
            it.connect(token)
        }
    }

    private fun startArchive() {
        Log.i(TAG, "startArchive")
        if (session != null) {
            val startArchiveRequest = StartArchiveRequest()
            startArchiveRequest.sessionId = sessionId
            setStartArchiveEnabled(false)

            val call = apiService.startArchive(startArchiveRequest)
            call.enqueue(EmptyCallback())
        }
    }

    private fun stopArchive() {
        Log.i(TAG, "stopArchive")
        val call = apiService.stopArchive(currentArchiveId)
        call.enqueue(EmptyCallback<Any?>())
        setStopArchiveEnabled(false)
    }

    private fun playArchive() {
        Log.i(TAG, "playArchive")
        val archiveUrl = ServerConfig.CHAT_SERVER_URL + "/archive/" + playableArchiveId + "/view"
        val archiveUri = Uri.parse(archiveUrl)
        val browserIntent = Intent(Intent.ACTION_VIEW, archiveUri)
        startActivity(browserIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the app bar if it is present.
        menuInflater.inflate(R.menu.menu_chat, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle app bar item clicks here. The app bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_start_archive -> {
                startArchive()
                true
            }
            R.id.action_stop_archive -> {
                stopArchive()
                true
            }
            R.id.action_play_archive -> {
                playArchive()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setStartArchiveEnabled(enabled: Boolean) {
        menu?.findItem(R.id.action_start_archive)?.setEnabled(enabled)?.isVisible = enabled
    }

    private fun setStopArchiveEnabled(enabled: Boolean) {
        menu?.findItem(R.id.action_stop_archive)?.setEnabled(enabled)?.isVisible = enabled
    }

    private fun setPlayArchiveEnabled(enabled: Boolean) {
        menu?.findItem(R.id.action_play_archive)?.setEnabled(enabled)?.isVisible = enabled
    }

    private fun finishWithMessage(message: String) {
        Log.e(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSIONS_REQUEST_CODE = 124
    }
}