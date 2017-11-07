package com.tokbox.android.tutorials.basic_video_chat;

import android.content.Context;
import android.util.Log;

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

    private final Context context;
    private Listener delegate;

    public WebServiceCoordinator(Context context, Listener delegate) {

        this.context = context;
        this.delegate = delegate;
    }

    public void fetchSessionConnectionData(String sessionInfoUrlEndpoint) {

        RequestQueue reqQueue = Volley.newRequestQueue(context);
        reqQueue.add(new JsonObjectRequest(Request.Method.GET, sessionInfoUrlEndpoint,
                                            null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String apiKey = response.getString("apiKey");
                    String sessionId = response.getString("sessionId");
                    String token = response.getString("token");

                    Log.i(LOG_TAG, "WebServiceCoordinator returned session information");

                    delegate.onSessionConnectionDataReady(apiKey, sessionId, token);

                } catch (JSONException e) {
                    delegate.onWebServiceCoordinatorError(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        }));
    }

    public static interface Listener {

        void onSessionConnectionDataReady(String apiKey, String sessionId, String token);
        void onWebServiceCoordinatorError(Exception error);
    }
}

