# Phone Call Detection

This application provides a Video Chat tutorial for Android. Upon deploying this sample application, you should be
able to have two-way audio and video communication using OpenTok. You will be notified about incoming and outgoing
phone calls (happening during the video call).

Main features:
* Create 2-way OpenTok video call
* Detect incoming and outgoing native phone calls (while being on video call)

# Configure the app 
Open the `OpenTokConfig` file and configure the `API_KEY`, `SESSION_ID`, and `TOKEN` variables. You can obtain these values from your [TokBox account](https://tokbox.com/account/#/).

# Listen fo call state changes
This is the code responsible for listening for the call status:

```java
TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

//...

private PhoneStateListener phoneStateListener = new PhoneStateListener() {
    @Override
    public void onCallStateChanged(int state, String incomingNumber) {

        super.onCallStateChanged(state, incomingNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE: //Initial state
                Log.d("onCallStateChanged", "CALL_STATE_IDLE");
                break;

            case TelephonyManager.CALL_STATE_RINGING: // Incoming call Ringing
                Log.d("onCallStateChanged", "CALL_STATE_RINGING");
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK: // Outgoing Call | Accepted incoming call
                Log.d("onCallStateChanged", "CALL_STATE_OFFHOOK");
                break;

            default:
                Log.d("onCallStateChanged", "Unknown Phone State !");
                break;
        }
    }
};
```

## Further Reading

* Review [other sample projects](../)
* Read more about [OpenTok Android SDK](https://tokbox.com/developer/sdks/android/)
