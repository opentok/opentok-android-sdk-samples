package com.tokbox.sample.basicvideochatconnectionservice

import android.text.TextUtils

object OpenTokConfig {
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects

    Note that this application will ignore credentials in the `OpenTokConfig` file when `CHAT_SERVER_URL` contains a
    valid URL.
    */
    // Replace with a API key
    const val API_KEY: String = "1cc1c8de-7e50-497b-b0b1-7d8e1ae46d38"

 //   {
 //       "sessionId": "2_MX4xY2MxYzhkZS03ZTUwLTQ5N2ItYjBiMS03ZDhlMWFlNDZkMzh-fjE3NjIzNDk5NzY0ODF-ZXFIa3pQZ2l5SUxCeC84QStnWHJDK3Zkfn5-",
 //       "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6InNlc3Npb24uY29ubmVjdCIsInNlc3Npb25faWQiOiIyX01YNHhZMk14WXpoa1pTMDNaVFV3TFRRNU4ySXRZakJpTVMwM1pEaGxNV0ZsTkRaa016aC1makUzTmpJek5EazVOelkwT0RGLVpYRklhM3BRWjJsNVNVeENlQzg0UVN0bldISkRLM1prZm41LSIsInJvbGUiOiJtb2RlcmF0b3IiLCJpbml0aWFsX2xheW91dF9jbGFzc19saXN0IjoiIiwiZXhwIjoxNzYyNDM2Mzc3LCJzdWIiOiJ2aWRlbyIsImFjbCI6eyJwYXRocyI6eyIvc2Vzc2lvbi8qKiI6e319fSwianRpIjoiOTAwMGFlNmQtMDJlYy00NTU0LTljMjItZDIzNDg1NjgxYjc5IiwiaWF0IjoxNzYyMzQ5OTc2LCJhcHBsaWNhdGlvbl9pZCI6IjFjYzFjOGRlLTdlNTAtNDk3Yi1iMGIxLTdkOGUxYWU0NmQzOCJ9.P01ISnKxpAgJc8M-8KKcYYKVLQZ7mP1Hl74xN3kOk9yJipd6ygXe9flIZ3gs3mFK1dnVdNAFyf9SlkCqKBoATEAXXH6tRuEe5gw5821LHTvmsjRXjC6sXvm21pfgNrgPI_oUPikIeaadxDUsk9TTdAwx7k9-g0BeEe5bHTKDNdH47FzSfm5shesayKofx9H9PTv8AES9YkL21P-B3DYnAypt2bnIrjgtdAzVZ2B4nWuFGW7DdZbgPR8rUfC16rl5XThlZJMsaUM2PL8h1JKonYQaND67RUEZQiw9sXDEZzPDkCE6cq3guwc81SvLuRAfZEUZ2GHwIgfKBS-igJ5ftw",
 //       "apiKey": "1cc1c8de-7e50-497b-b0b1-7d8e1ae46d38",
 //       "captionsId": null

    // Replace with a generated Session ID
    const val SESSION_ID: String = "2_MX4xY2MxYzhkZS03ZTUwLTQ5N2ItYjBiMS03ZDhlMWFlNDZkMzh-fjE3NjIzNDk5NzY0ODF-ZXFIa3pQZ2l5SUxCeC84QStnWHJDK3Zkfn5-"

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    const val TOKEN: String = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6InNlc3Npb24uY29ubmVjdCIsInNlc3Npb25faWQiOiIyX01YNHhZMk14WXpoa1pTMDNaVFV3TFRRNU4ySXRZakJpTVMwM1pEaGxNV0ZsTkRaa016aC1makUzTmpJek5EazVOelkwT0RGLVpYRklhM3BRWjJsNVNVeENlQzg0UVN0bldISkRLM1prZm41LSIsInJvbGUiOiJtb2RlcmF0b3IiLCJpbml0aWFsX2xheW91dF9jbGFzc19saXN0IjoiIiwiZXhwIjoxNzYyNDM2Mzc3LCJzdWIiOiJ2aWRlbyIsImFjbCI6eyJwYXRocyI6eyIvc2Vzc2lvbi8qKiI6e319fSwianRpIjoiOTAwMGFlNmQtMDJlYy00NTU0LTljMjItZDIzNDg1NjgxYjc5IiwiaWF0IjoxNzYyMzQ5OTc2LCJhcHBsaWNhdGlvbl9pZCI6IjFjYzFjOGRlLTdlNTAtNDk3Yi1iMGIxLTdkOGUxYWU0NmQzOCJ9.P01ISnKxpAgJc8M-8KKcYYKVLQZ7mP1Hl74xN3kOk9yJipd6ygXe9flIZ3gs3mFK1dnVdNAFyf9SlkCqKBoATEAXXH6tRuEe5gw5821LHTvmsjRXjC6sXvm21pfgNrgPI_oUPikIeaadxDUsk9TTdAwx7k9-g0BeEe5bHTKDNdH47FzSfm5shesayKofx9H9PTv8AES9YkL21P-B3DYnAypt2bnIrjgtdAzVZ2B4nWuFGW7DdZbgPR8rUfC16rl5XThlZJMsaUM2PL8h1JKonYQaND67RUEZQiw9sXDEZzPDkCE6cq3guwc81SvLuRAfZEUZ2GHwIgfKBS-igJ5ftw"

    val isValid: Boolean
        // *** The code below is to validate this configuration file. You do not need to modify it  ***
        get() {
            if (TextUtils.isEmpty(API_KEY)
                || TextUtils.isEmpty(SESSION_ID)
                || TextUtils.isEmpty(TOKEN)
            ) {
                return false
            }

            return true
        }

    val description: String
        get() = ("""
     OpenTokConfig:
     API_KEY: $API_KEY
     SESSION_ID: $SESSION_ID
     TOKEN: $TOKEN
     
     """.trimIndent())
}
