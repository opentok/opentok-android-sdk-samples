package com.tokbox.sample.basicvideorenderer;

import android.text.TextUtils;
import androidx.annotation.NonNull;

public class OpenTokConfig {
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects
    */

    public static final String API_KEY = "46787984";
    // Replace with a generated Session ID
    public static final String SESSION_ID = "2_MX40Njc4Nzk4NH5-MTYxMDQ0MzgwOTU3Nn4xRmx2Q2RjWlpGYjVrRkhmRUFlKzZ4M1d-fg";
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD00Njc4Nzk4NCZzaWc9NDE0MzdiZjFkMDM0MzZiZjM3NWZjMjRkNWFiZjRmM2IwNmM3ZWM1OTpzZXNzaW9uX2lkPTJfTVg0ME5qYzROems0Tkg1LU1UWXhNRFEwTXpnd09UVTNObjR4Um14MlEyUmpXbHBHWWpWclJraG1SVUZsS3paNE0xZC1mZyZjcmVhdGVfdGltZT0xNjEwNDQzODczJm5vbmNlPTAuMzcyMTAxOTMxNzgxODY3JnJvbGU9cHVibGlzaGVyJmV4cGlyZV90aW1lPTE2MTMwMzU4NzMmaW5pdGlhbF9sYXlvdXRfY2xhc3NfbGlzdD0=";

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
