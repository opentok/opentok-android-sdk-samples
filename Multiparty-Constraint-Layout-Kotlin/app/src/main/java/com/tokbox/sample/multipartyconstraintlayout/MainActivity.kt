package com.tokbox.sample.multipartyconstraintlayout

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.PublisherKit.PublisherListener
import com.opentok.android.Session
import com.opentok.android.Session.SessionListener
import com.opentok.android.Session.SessionOptions
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.tokbox.sample.multipartyconstraintlayout.MainActivity
import com.tokbox.sample.multipartyconstraintlayout.OpenTokConfig.description
import com.tokbox.sample.multipartyconstraintlayout.OpenTokConfig.isValid
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.util.ArrayList
import java.util.HashMap

class MainActivity : AppCompatActivity(), PermissionCallbacks {
    private var session: Session? = null
    private var publisher: Publisher? = null
    private val subscribers = ArrayList<Subscriber>()
    private val subscriberStreams = HashMap<Stream, Subscriber>()
    private lateinit var container: ConstraintLayout

    private val publisherListener: PublisherListener = object : PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamCreated: Own stream ${stream.streamId} created")
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamDestroyed: Own stream ${stream.streamId} destroyed")
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            finishWithMessage("PublisherKit error: ${opentokError.message}")
        }
    }
    private val sessionListener: SessionListener = object : SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "onConnected: Connected to session ${session.sessionId}")
            session.publish(publisher)
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "onDisconnected: disconnected from session ${session.sessionId}")
            this@MainActivity.session = null
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: ${opentokError.message}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamReceived: New stream ${stream.streamId} in session ${session.sessionId}")
            val subscriber = Subscriber.Builder(this@MainActivity, stream).build()
            session.subscribe(subscriber)
            subscribers.add(subscriber)
            subscriberStreams[stream] = subscriber
            val subId = getResIdForSubscriberIndex(subscribers.size - 1)
            subscriber.view.id = subId
            container.addView(subscriber.view)
            calculateLayout()
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamDropped: Stream ${stream.streamId} dropped from session ${session.sessionId}")
            val subscriber = subscriberStreams[stream] ?: return
            subscribers.remove(subscriber)
            subscriberStreams.remove(stream)
            container.removeView(subscriber.view)

            // Recalculate view Ids
            for (i in subscribers.indices) {
                subscribers[i].view.id = getResIdForSubscriberIndex(i)
            }
            calculateLayout()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!isValid) {
            finishWithMessage("Invalid OpenTokConfig. $description")
            return
        }
        container = findViewById(R.id.main_container)
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (session == null) {
            return
        }

        session?.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (session == null) {
            return
        }

        session?.onPause()
        if (isFinishing) {
            disconnectSession()
        }
    }

    override fun onDestroy() {
        disconnectSession()
        super.onDestroy()
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

    private fun getResIdForSubscriberIndex(index: Int): Int {
        val arr = resources.obtainTypedArray(R.array.subscriber_view_ids)
        val subId = arr.getResourceId(index, 0)
        arr.recycle()
        return subId
    }

    private fun startPublisherPreview() {
        publisher = Publisher.Builder(this).build()
        publisher?.setPublisherListener(publisherListener)
        publisher?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
        publisher?.startPreview()
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        if (EasyPermissions.hasPermissions(this, *perms)) {
            session = Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID)
                .sessionOptions(object : SessionOptions() {
                    override fun useTextureViews(): Boolean {
                        return true
                    }
                }).build()

            session?.setSessionListener(sessionListener)
            session?.connect(OpenTokConfig.TOKEN)
            startPublisherPreview()
            publisher?.view?.id = R.id.publisher_view_id
            container.addView(publisher?.view)
            calculateLayout()
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.rationale_video_app),
                PERMISSIONS_REQUEST_CODE,
                *perms
            )
        }
    }

    private fun calculateLayout() {
        val set = ConstraintSetHelper(R.id.main_container)
        val size = subscribers.size
        if (size == 0) {
            // Publisher full screen
            set.layoutViewFullScreen(R.id.publisher_view_id)
        } else if (size == 1) {
            // Publisher
            // Subscriber
            set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(0))
            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container)
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(0), R.id.main_container)
            set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container)
            set.layoutViewAllContainerWide(getResIdForSubscriberIndex(0), R.id.main_container)
            set.layoutViewHeightPercent(R.id.publisher_view_id, .5f)
            set.layoutViewHeightPercent(getResIdForSubscriberIndex(0), .5f)
        } else if (size > 1 && size % 2 == 0) {
            //  Publisher
            // Sub1 | Sub2
            // Sub3 | Sub4
            //    .....
            val rows = size / 2 + 1
            val heightPercent = 1f / rows
            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container)
            set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container)
            set.layoutViewHeightPercent(R.id.publisher_view_id, heightPercent)
            var i = 0

            while (i < size) {
                if (i == 0) {
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i))
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i + 1))
                } else {
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 2), getResIdForSubscriberIndex(i))
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 1), getResIdForSubscriberIndex(i + 1))
                }
                set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i + 1))
                set.layoutViewHeightPercent(getResIdForSubscriberIndex(i), heightPercent)
                set.layoutViewHeightPercent(getResIdForSubscriberIndex(i + 1), heightPercent)
                i += 2
            }
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container)
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container)
        } else if (size > 1) {
            // Pub  | Sub1
            // Sub2 | Sub3
            // Sub3 | Sub4
            //    .....
            val rows = (size + 1) / 2
            val heightPercent = 1f / rows
            set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container)
            set.layoutViewHeightPercent(R.id.publisher_view_id, heightPercent)
            set.layoutViewWithTopBound(getResIdForSubscriberIndex(0), R.id.main_container)
            set.layoutViewHeightPercent(getResIdForSubscriberIndex(0), heightPercent)
            set.layoutTwoViewsOccupyingAllRow(R.id.publisher_view_id, getResIdForSubscriberIndex(0))
            var i = 1

            while (i < size) {
                if (i == 1) {
                    set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i))
                    set.layoutViewHeightPercent(R.id.publisher_view_id, heightPercent)
                    set.layoutViewAboveView(getResIdForSubscriberIndex(0), getResIdForSubscriberIndex(i + 1))
                    set.layoutViewHeightPercent(getResIdForSubscriberIndex(0), heightPercent)
                } else {
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 2), getResIdForSubscriberIndex(i))
                    set.layoutViewHeightPercent(getResIdForSubscriberIndex(i - 2), heightPercent)
                    set.layoutViewAboveView(getResIdForSubscriberIndex(i - 1), getResIdForSubscriberIndex(i + 1))
                    set.layoutViewHeightPercent(getResIdForSubscriberIndex(i - 1), heightPercent)
                }
                set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i + 1))
                i += 2
            }
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container)
            set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container)
        }
        set.applyToLayout(container, true)
    }

    private fun disconnectSession() {
        if (session == null) {
            return
        }

        if (subscribers.size > 0) {
            for (subscriber in subscribers) {
                session?.unsubscribe(subscriber)
            }
        }

        if (publisher != null) {
            session?.unpublish(publisher)
            container.removeView(publisher?.view)
            publisher = null
        }
        session?.disconnect()
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