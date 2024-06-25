package com.tokbox.sample.videotransformers;

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
    public static final String SESSION_ID = "1_MX40NzUyMTM1MX5-MTcxODM1OTEyMzUyNH5oaUhWbUx4TXJQdldMY2tmRGJ1bFB3a2Z-fn4";
    
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD00NzUyMTM1MSZzaWc9OWU0YTYyZWVlMDk1YjY4YmFlNjhmZDRjZjkyYjU3ODU4MjcxZjQyNzpzZXNzaW9uX2lkPTFfTVg0ME56VXlNVE0xTVg1LU1UY3hPRE0xT1RFeU16VXlOSDVvYVVoV2JVeDRUWEpRZGxkTVkydG1SR0oxYkZCM2EyWi1mbjQmY3JlYXRlX3RpbWU9MTcxODM1OTEyOCZub25jZT0wLjAxMTc0MTEzNTQ5NDAxOTExNyZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNzIwOTUxMTI3JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";

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
