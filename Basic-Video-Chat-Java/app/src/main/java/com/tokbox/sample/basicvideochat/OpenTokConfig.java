package com.tokbox.sample.basicvideochat;

import android.text.TextUtils;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;

public class OpenTokConfig {
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects

    Note that this application will ignore credentials in the `OpenTokConfig` file when `CHAT_SERVER_URL` contains a
    valid URL.
    */

    // Replace with a API key
    public static final String API_KEY = "1302";
    
    // Replace with a generated Session ID
    public static final String SESSION_ID = "1_MX4xMzAyfn4xNjk3NTg1NzEwNTcwfkdwLzY5Vy9YTDZqYUVhZHFBR0V1ZGRPRn5-fg";
    
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD0xMzAyJnNka192ZXJzaW9uPXY0LjEyLjAmc2lnPWU1OGE0ZGQyMjc5YzQ3MDVlNzQzMjY5NzkwODJhZjI2NjVmZjYxN2E6c2Vzc2lvbl9pZD0xX01YNHhNekF5Zm40eE5qazNOVGcxTnpFd05UY3dma2R3THpZNVZ5OVlURFpxWVVWaFpIRkJSMFYxWkdSUFJuNS1mZyZjcmVhdGVfdGltZT0xNjk3NTg1NzEwJnJvbGU9bW9kZXJhdG9yJm5vbmNlPTE2OTc1ODU3MTAuNzI0MTM5NjAyMjk4NCZleHBpcmVfdGltZT0xNzAwMTc3NzEw";

    // *** The code below is to validate this configuration file. You do not need to modify it  ***

    public static boolean isValid() {
        if (TextUtils.isEmpty(OpenTokConfig.API_KEY)
                || TextUtils.isEmpty(OpenTokConfig.SESSION_ID)
                || TextUtils.isEmpty(OpenTokConfig.TOKEN)) {
            return false;
        }

        return true;
    }

    @NonNull
    public static String getDescription() {
        return "OpenTokConfig:" + "\n"
                + "API_KEY: " + OpenTokConfig.API_KEY + "\n"
                + "SESSION_ID: " + OpenTokConfig.SESSION_ID + "\n"
                + "TOKEN: " + OpenTokConfig.TOKEN + "\n";
    }
}
