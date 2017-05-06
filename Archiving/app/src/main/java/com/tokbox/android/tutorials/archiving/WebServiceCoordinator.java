package com.tokbox.android.tutorials.archiving;

import android.content.Context;
import android.util.Log;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class WebServiceCoordinator {

    private static final String LOG_TAG = WebServiceCoordinator.class.getSimpleName();

    private static RequestQueue reqQueue;

    private final Context context;
    private Listener delegate;

    public WebServiceCoordinator(Context context, Listener delegate) {
        this.context = context;
        this.delegate = delegate;
        this.reqQueue = Volley.newRequestQueue(context);
    }

    public void fetchSessionConnectionData() {
        RequestQueue reqQueue = Volley.newRequestQueue(context);
            reqQueue.add(
                    new JsonObjectRequest(
                        Request.Method.GET,
                        OpenTokConfig.SESSION_INFO_ENDPOINT,
                        null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    String apiKey = response.getString("apiKey");
                                    String sessionId = response.getString("sessionId");
                                    String token = response.getString("token");

                                    Log.i(LOG_TAG, apiKey);
                                    Log.i(LOG_TAG, sessionId);
                                    Log.i(LOG_TAG, token);

                                    delegate.onSessionConnectionDataReady(apiKey, sessionId, token);
                                } catch (JSONException e) {
                                    delegate.onWebServiceCoordinatorError(e);
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                delegate.onWebServiceCoordinatorError(error);
                            }
                        }
                    )
            );
    }

    public static interface Listener {
        void onSessionConnectionDataReady(String apiKey, String sessionId, String token);
        void onWebServiceCoordinatorError(Exception error);
    }

    public void startArchive(String sessionId) {
        String requestUrl = OpenTokConfig.ARCHIVE_START_ENDPOINT.replace(":sessionId", sessionId);
        this.reqQueue.add(new JsonObjectRequest(Request.Method.POST, requestUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, "archive started");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        }));
    }

    public void stopArchive(String archiveId) {
        String requestUrl = OpenTokConfig.ARCHIVE_STOP_ENDPOINT.replace(":archiveId", archiveId);
        this.reqQueue.add(new JsonObjectRequest(Request.Method.POST, requestUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, "archive stopped");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        }));
    }

    public Uri archivePlaybackUri(String archiveId) {
        return Uri.parse(OpenTokConfig.ARCHIVE_PLAY_ENDPOINT.replace(":archiveId", archiveId));
    }
}

