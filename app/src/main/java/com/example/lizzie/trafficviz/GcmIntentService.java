package com.example.lizzie.trafficviz;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;

import org.json.JSONException;
import org.json.JSONObject;
//import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.*;


import java.io.IOException;
import java.lang.reflect.Array;
import java.security.Policy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.PubNubException;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.push.PNPushAddChannelResult;
import com.pubnub.api.models.consumer.push.PNPushRemoveChannelResult;


/**
 * Created by lizzie on 7/26/16.
 */

public class GcmIntentService extends IntentService {

    private class asyncTaskClass extends GcmIntentService {

    }

    String regId = "pikachu";
    String SENDER_ID = "209687934533";
    String maybe_sender_id = "AIzaSyCGbXUSDAyPeow9QR7JJHNGvp6frpWoy3M";
    String TAG = "hello, world";
    GoogleCloudMessaging gcm;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    Context context;
    String CHANNEL = "pikaChannel";
    String PROPERTY_REG_ID = "oi";

    int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceID instanceID = InstanceID.getInstance(this);
        String token;
        try {
            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        } catch (Exception e) {
            System.out.println(e);
        }
        Bundle extras = intent.getExtras();
        gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty() && GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            sendNotification("Received: " + extras.toString());
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("PubNub GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        int NOTIFICATION_ID = 1;
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

//which device should receive notifications?

    private void register() {
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);

            try {
                regId = getRegistrationId(context);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (regId.isEmpty()) {
                registerInBackground();
            } else {
                Toast.makeText(this, "Registration ID already exists: " + regId, Toast.LENGTH_SHORT).show(); //makeToast() ??
            }
        } else {
            Log.e(TAG, "No valid Google Play Services APK found.");
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog((Activity) context, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.e(TAG, "device not supported"); //finish() ?
            }
            return false;
        }
        return true;
    }

    private String getRegistrationId(Context context) throws Exception {
        final SharedPreferences prefs =
                getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            return "";
        }

        return registrationId;
    }

    public String getRegistrationToken() throws IOException {

        InstanceID instanceID = InstanceID.getInstance(this);
        String token = instanceID.getToken(GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

        return token;
    }

    private void registerInBackground() {
        AsyncTask<Integer, Void, String> opTask = new AsyncTask<Integer, Void, String>() {
            @Override
            protected String doInBackground(Integer... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    //regId = gcm.register(SENDER_ID);
                    regId = getRegistrationToken(); //register is deprecated :(
                    msg = "Device registered, registration ID: " + regId;

                    sendRegistrationId(regId);

                    storeRegistrationId(context, regId);
                    Log.i(TAG, msg);
                } catch (Exception ex) { //try
                    msg = "Error :" + ex.getMessage();
                    Log.e(TAG, msg);
                } //catch
                return msg;
            }
        };
        opTask.execute(null, null, null); //async task
    }

    //PubNub instance
    PNConfiguration pnConfiguration = new PNConfiguration()
            .setSubscribeKey("sub-c-e3dc294e-4568-11e6-bfbb-02ee2ddab7fe")
            .setPublishKey("pub-c-cfd86269-b02b-4004-97a6-92ca5f582fce");
    private final PubNub pb = new PubNub(pnConfiguration);

    private void sendRegistrationId(String regID) {
        pb.addPushNotificationsOnChannels().channels(Arrays.asList(CHANNEL)).deviceId(regID).async(new PNCallback<PNPushAddChannelResult>() {
            @Override
            public void onResponse(PNPushAddChannelResult result, PNStatus status) {
                // HANDLE ME HERE.
            }
        });
    }

    private void storeRegistrationId(Context context, String regId) throws Exception {
        final SharedPreferences prefs =
                getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.apply();
    }

    public void sendNotification2() throws PubNubException {
        pb.publish()
                .message("hi")
                .channel("hi2")
                .sync();
    }

    private void unregister() {
        AsyncTask<String, Void, Object> opTask = new AsyncTask<String, Void, Object>() {
            @Override
            protected Object doInBackground(String... params) {
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }

                    // Unregister from GCM
                    gcm.unregister();

                    // Remove Registration ID from memory
                    removeRegistrationId(context);

                    // Disable Push Notification
                    pb.removePushNotificationsFromChannels()
                            .channels(Arrays.asList(CHANNEL))
                            .deviceId(regId)
                            .async(new PNCallback<PNPushRemoveChannelResult>() {
                                @Override
                                public void onResponse(PNPushRemoveChannelResult result, PNStatus status) {
                                    // HANDLE RESULT HERE
                                }
                            });

                } catch (Exception e) {
                }
                return null;
            }
        }.execute(null, null, null);
    }

    private void removeRegistrationId(Context context) throws Exception {
        final SharedPreferences prefs =
                getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PROPERTY_REG_ID);
        editor.apply();
    }
}



