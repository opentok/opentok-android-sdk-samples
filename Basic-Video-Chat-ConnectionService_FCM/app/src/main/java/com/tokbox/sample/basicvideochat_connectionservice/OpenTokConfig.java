package com.tokbox.sample.basicvideochat_connectionservice;

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
    public static final String API_KEY = "47521351";
    
    // Replace with a generated Session ID
    public static final String SESSION_ID = "1_MX40NzUyMTM1MX5-MTc0NzI5ODM2NjY2MX5LTW9OUFVwVlJOVEZFUitsY3BLUTBTVGZ-fn4";
    
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD00NzUyMTM1MSZzaWc9ZTVkZmEwMmZkYjUxNGEzYmYwNDhhMTMxYTllMWQ1NDJiYTBhYTE2YTpzZXNzaW9uX2lkPTFfTVg0ME56VXlNVE0xTVg1LU1UYzBOekk1T0RNMk5qWTJNWDVMVFc5T1VGVndWbEpPVkVaRlVpdHNZM0JMVVRCVFZHWi1mbjQmY3JlYXRlX3RpbWU9MTc0NzI5ODM3MiZub25jZT0wLjg2NjY5MzcwMTc0NDU3MyZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNzQ5ODkwMzcxJmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";

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
