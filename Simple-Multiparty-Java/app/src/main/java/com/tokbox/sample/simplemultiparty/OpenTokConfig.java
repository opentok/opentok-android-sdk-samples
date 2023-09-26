package com.tokbox.sample.simplemultiparty;

import android.text.TextUtils;
import androidx.annotation.NonNull;

public class OpenTokConfig {
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects
    */

    // Replace with a API key
    public static final String API_KEY = "100";

    // Replace with a generated Session ID
    public static final String SESSION_ID = "2_MX4xMDB-fjE2ODA3MTk0Njc5MDh-LzQ2NkFlTGpzUzR3VlJMNnQ3V2wrcG9Tfn5-";

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD0xMDAmc2RrX3ZlcnNpb249djQuMTIuMCZzaWc9YWNkMzFjNjEwM2U1YjYzNmYwODY1NzI4N2VjNDNjYzAyZjRlNGM3YzpzZXNzaW9uX2lkPTJfTVg0eE1EQi1makUyT0RBM01UazBOamM1TURoLUx6UTJOa0ZsVEdwelV6UjNWbEpNTm5RM1Yyd3JjRzlUZm41LSZjcmVhdGVfdGltZT0xNjk1NjY3NTAxJnJvbGU9bW9kZXJhdG9yJm5vbmNlPTE2OTU2Njc1MDEuNjMyOTE0OTYwNjQzMDYmZXhwaXJlX3RpbWU9MTY5ODI1OTUwMQ==";

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
