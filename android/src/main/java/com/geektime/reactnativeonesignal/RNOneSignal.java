package com.geektime.reactnativeonesignal;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.onesignal.OneSignal;

import org.json.JSONObject;
import org.json.JSONException;


/**
 * Created by Avishay on 1/31/16.
 */
public class RNOneSignal extends ReactContextBaseJavaModule implements Application.ActivityLifecycleCallbacks {
    public static final String NOTIFICATION_OPENED_INTENT_FILTER = "GTNotificatinOpened";

    private ReactContext mReactContext;
    private Activity mActivity;

    public RNOneSignal(ReactApplicationContext reactContext, Activity activity) {
        super(reactContext);
        mActivity = activity;
        mReactContext = reactContext;

        OneSignal.startInit(mActivity)
                .setNotificationOpenedHandler(new NotificationOpenedHandler(reactContext))
                .init();
        OneSignal.enableNotificationsWhenActive(true);

        activity.getApplication().registerActivityLifecycleCallbacks(this);

        registerNotificationsReceiveNotification();
    }

    public RNOneSignal(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    private void sendEvent(String eventName, Object params) {
        mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @ReactMethod
    public void sendTag(String key, String value) {
        OneSignal.sendTag(key, value);
    }

    @ReactMethod
    public void sendTags(ReadableMap tags) {
        OneSignal.sendTags(RNUtils.readableMapToJson(tags));
    }

    @ReactMethod
    public void getTags(final Callback callback) {
        OneSignal.getTags(new OneSignal.GetTagsHandler() {
            @Override
            public void tagsAvailable(JSONObject tags) {
                callback.invoke(RNUtils.jsonToWritableMap(tags));
            }
        });
    }

    @ReactMethod
    public void configure() {
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            public void idsAvailable(String userId, String registrationId) {
                final WritableMap params = Arguments.createMap();

                params.putString("userId", userId);
                params.putString("pushToken", registrationId);

                sendEvent("idsAvailable", params);
            }
        });
    }

    @ReactMethod
    public void deleteTag(String key) {
        OneSignal.deleteTag(key);
    }

    @ReactMethod
    public void enableVibrate(Boolean enable) {
        OneSignal.enableVibrate(enable);
    }

    @ReactMethod
    public void enableSound(Boolean enable) {
        OneSignal.enableSound(enable);
    }

    @ReactMethod
    public void enableNotificationsWhenActive(Boolean enable) {
        OneSignal.enableNotificationsWhenActive(enable);
    }

    @ReactMethod
    public void enableInAppAlertNotification(Boolean enable) {
        OneSignal.enableInAppAlertNotification(enable);
    }

    @ReactMethod
    public void setSubscription(Boolean enable) {
        OneSignal.setSubscription(enable);
    }

    @ReactMethod
    public void promptLocation() {
        OneSignal.promptLocation();
    }

    @ReactMethod
    public void postNotification(String contents, String data, String player_id) {
        try {
          OneSignal.postNotification(new JSONObject("{'contents': " + contents + ", 'data': {'p2p_notification': " + data +"}, 'include_player_ids': ['" + player_id + "']}"),
             new OneSignal.PostNotificationResponseHandler() {
               @Override
               public void onSuccess(JSONObject response) {
                 Log.i("OneSignal", "postNotification Success: " + response.toString());
               }

               @Override
               public void onFailure(JSONObject response) {
                 Log.e("OneSignal", "postNotification Failure: " + response.toString());
               }
             });
        } catch (JSONException e) {
          e.printStackTrace();
        }
    }

    private void registerNotificationsReceiveNotification() {
        IntentFilter intentFilter = new IntentFilter(NOTIFICATION_OPENED_INTENT_FILTER);

        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyNotification(intent.getExtras());
            }
        }, intentFilter);
    }

    private void notifyNotification(Bundle bundle) {
        final WritableMap params = Arguments.createMap();
        params.putString("message", bundle.getString("message"));
        params.putString("additionalData", bundle.getString("additionalData"));
        params.putBoolean("isActive", bundle.getBoolean("isActive"));

        sendEvent("remoteNotificationOpened", params);
    }

    @Override
    public String getName() {
        return "RNOneSignal";
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity.equals(mActivity)) {
            OneSignal.removeNotificationOpenedHandler();
        }
    }
}
