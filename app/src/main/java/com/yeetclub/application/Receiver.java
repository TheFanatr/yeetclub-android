package com.yeetclub.application;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.parse.ParseAnalytics;
import com.parse.ParsePushBroadcastReceiver;
import com.yeetclub.android.MainActivity;
import com.yeetclub.android.R;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by @santafebound on 2015-11-07.
 */
public class Receiver extends ParsePushBroadcastReceiver {

    @Override
    protected Bitmap getLargeIcon(Context context, Intent intent) {
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
    }

    @Override
    protected void onPushOpen(Context context, Intent intent) {

        ParseAnalytics.trackAppOpenedInBackground(intent);

        String uriString = null;
        try {
            JSONObject pushData = new JSONObject(intent.getStringExtra("com.parse.Data"));
            uriString = pushData.optString("uri");
        } catch (JSONException e) {
            Log.v(getClass().toString(), "Unexpected JSONException when receiving push data: ", e);
        }
        Class<? extends Activity> cls = getActivity(context, intent);
        Intent activityIntent;
        if (uriString != null && !uriString.isEmpty()) {
            activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            /*Log.w(getClass().toString(), uriString);*/
        } else {
            activityIntent = new Intent(context, MainActivity.class);
            //TODO Pass "fragment1" as string extra if we come here from "pushRant" notification; for now, link to TabFragment2 (NotificationsAdapter)
            /*if (pushName.equals("pushRant")) {
                activityIntent.putExtra("fragment", "fragment1");
            } else {
                activityIntent.putExtra("fragment", "fragment2");
            }*/
            activityIntent.putExtra("fragment", "fragment2");
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
        activityIntent.putExtras(intent.getExtras());
        if (Build.VERSION.SDK_INT >= 16) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(cls);
            stackBuilder.addNextIntent(activityIntent);
            stackBuilder.startActivities();
        } else {
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(activityIntent);
        }

    }
}