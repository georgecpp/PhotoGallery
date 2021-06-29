package com.steelparrot.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class _PollService extends JobIntentService {
    private static final String TAG = "_PollService";
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1); // for AlarmManager.
    public static final String CHANNEL_ID = "channel_1";
    public static final int JOB_ID = 5;

    public static final String ACTION_SHOW_NOTIFICATION = "com.steelparrot.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.steelparrot.photogallery.PRIVATE";


    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public static Intent newIntent(Context context) {
        return new Intent(context,_PollService.class);
    }

  /*  @Override
    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O) {
            Notification notification = getNotificationNewPics(this);
            startForeground(1, notification);
           // enqueueWork(this, newIntent(this));
        }
    }*/


//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O) {
//            stopForeground(true);
//        }
//    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> items;

        if(query==null) {
            items = new FlickrFetch().fetchRecentPhotos();
        }
        else {
            items = new FlickrFetch().searchPhotos(query);
        }

        if(items.size()==0) {
            return;
        }

        String resultId = items.get(0).getId();
        if(resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        }
        else {
            Log.i(TAG,"Got a new result: " + resultId);
            createNotificationChannel();
            showNotification(this);
            //sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION),PERM_PRIVATE); // custom defined private permission.

            /*Notification notification = getNotificationNewPics(this);
            showBackgroundNotification(0,notification);*/

        }

        QueryPreferences.setLastResultId(this,resultId);
       /* boolean isOn =  QueryPreferences.isAlarmOn(this);
        setServiceAlarm(this,isOn);*/
    }

    private void showBackgroundNotification(int requestCode, Notification notification)
    {
        Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
        intent.putExtra(REQUEST_CODE, requestCode);
        intent.putExtra(NOTIFICATION,notification);
        sendOrderedBroadcast(intent,PERM_PRIVATE,null,null, Activity.RESULT_OK,null,null);
    }
    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context,_PollService.class,JOB_ID,intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static boolean isServiceAlarmOn(Context context) {
        Intent intent = _PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pendingIntent!=null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setServiceAlarm(Context context, boolean isOn) {
        Log.i(TAG,isOn? "Alarm Service On" : "Alarm Service Off");
        Intent intent = _PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pendingIntent);
        }
        else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        QueryPreferences.setAlarmOn(context,isOn);
    }

    private Notification getNotificationNewPics(Context context)
    {
        createNotificationChannel();
        Intent intent = PhotoGalleryActivity.newIntent(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(getResources().getString(R.string.new_pictures_title))
                .setContentText(getResources().getString(R.string.new_pictures_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        Notification notification = builder.build();
        return notification;
    }

    public void showNotification(Context context) {

        // preparing intent for pending intent when notification is tapped.
        Intent intent = PhotoGalleryActivity.newIntent(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(getResources().getString(R.string.new_pictures_title))
                .setContentText(getResources().getString(R.string.new_pictures_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        Notification notification = builder.build();

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(1,notification);
    }
}
