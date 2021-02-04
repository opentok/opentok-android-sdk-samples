package com.tokbox.sample.basicvideochat;

import android.text.TextUtils;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;

public class OpenTokConfig {
    // *** Fill the following variables using your own Project info from the OpenTok dashboard  ***
    // ***                      https://dashboard.tokbox.com/projects                           ***

    // Replace with your OpenTok API key
    public static final String API_KEY = "47074394";
    // Replace with a generated Session ID
    public static final String SESSION_ID = "2_MX40NzA3NDM5NH5-MTYxMjQxNTM4NTE1MH55NmpZbUhod2htTmNGRkhhS3VjejQzS1J-fg";
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD00NzA3NDM5NCZzaWc9YzdlYmY0MzRkNDIyMzQ1MTNjMWJmYTNlOWY1ZGU2MTgyYmVjOTY2NjpzZXNzaW9uX2lkPTJfTVg0ME56QTNORE01Tkg1LU1UWXhNalF4TlRNNE5URTFNSDU1Tm1wWmJVaG9kMmh0VG1OR1JraGhTM1ZqZWpRelMxSi1mZyZjcmVhdGVfdGltZT0xNjEyNDE1Mzg1JnJvbGU9bW9kZXJhdG9yJm5vbmNlPTE2MTI0MTUzODUuMTgzMzMwMzg3MTcmZXhwaXJlX3RpbWU9MTYxMzAyMDE4NSZjb25uZWN0aW9uX2RhdGE9bmFtZSUzREpvaG5ueSZpbml0aWFsX2xheW91dF9jbGFzc19saXN0PWZvY3Vz";

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
