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
    public static final String API_KEY = "48048351";
    
    // Replace with a generated Session ID
    public static final String SESSION_ID = "2_MX40ODA0ODM1MX5-MTc0OTE5NTY2MTc5NX5naDd3LzJDVDBOMTlTYmZHWDhTWDVqa2l-fn4";
    
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD00ODA0ODM1MSZzaWc9Y2EzMmMxYmExMGQxNjUzYTQ2Zjc4NWU4N2ZiMTU5ODIzNzFjYzJiODpzZXNzaW9uX2lkPTJfTVg0ME9EQTBPRE0xTVg1LU1UYzBPVEU1TlRZMk1UYzVOWDVuYURkM0x6SkRWREJPTVRsVFltWkhXRGhUV0RWcWEybC1mbjQmY3JlYXRlX3RpbWU9MTc0OTczMDA4OSZub25jZT0wLjk3NDc1MDIwMDc1NDY3ODImcm9sZT1tb2RlcmF0b3ImZXhwaXJlX3RpbWU9MTc0OTczMTg4Njk0MiZpbml0aWFsX2xheW91dF9jbGFzc19saXN0PQ==";

    @NonNull
    public static String getDescription() {
        return "OpenTokConfig:" + "\n"
                + "API_KEY: " + OpenTokConfig.API_KEY + "\n"
                + "SESSION_ID: " + OpenTokConfig.SESSION_ID + "\n"
                + "TOKEN: " + OpenTokConfig.TOKEN + "\n";
    }
}
