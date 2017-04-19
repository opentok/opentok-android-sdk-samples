package com.tokbox.android.tutorials;

public class OpenTokConfig {
    // *** Fill the following variables using your own Project info from the OpenTok dashboard  ***
    // ***                      https://dashboard.tokbox.com/projects                           ***

    // Replace with your OpenTok API key
    public static final String API_KEY = "";
    // Replace with a generated Session ID
    public static final String SESSION_ID = "";
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "";

    /*                           ***** OPTIONAL*****
     If you have set up a server to provide session information replace the null value
     in CHAT_SERVER_URL with it.

     For example (if using a heroku subdomain): "https://yoursubdomain.herokuapp.com"
    */
    public static final String CHAT_SERVER_URL = null;
    public static final String SESSION_INFO_ENDPOINT = CHAT_SERVER_URL + "/session";
}
