package kuvaev.mainapp.eatit_server.Helper;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import kuvaev.mainapp.eatit_server.R;

public class NotificationHelper {
    private static final String ABD_CHANNEL_ID = "com.example.asus.androideatitserver.ABDULKARIM";
    private static final String ABD_CHANNEL_NAME = "Eat it";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  //only working this function if API is 26 or higher
            createChannel();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {

        NotificationChannel abdChannel = new NotificationChannel(ABD_CHANNEL_ID,
                ABD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);

        abdChannel.enableLights(false);
        abdChannel.enableVibration(true);
        abdChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(abdChannel);
    }

    public NotificationManager getManager() {

        if (manager == null)
            manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        return manager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public android.app.Notification.Builder getEatItChannelNotification(String title , String  body ,
                                                                        PendingIntent contentIntent ,
                                                                        Uri soundUri){
        return new android.app.Notification.Builder(getApplicationContext() , ABD_CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_start)
                .setSound(soundUri)
                .setAutoCancel(false);

    }

    //maybe get error in android oreo in line : setSmallIcon() becuz we pass argument Mipmap, must use drawable

    @RequiresApi(api = Build.VERSION_CODES.O)
    public android.app.Notification.Builder getEatItChannelNotification(String title , String  body ,
                                                                        Uri soundUri){
        return new android.app.Notification.Builder(getApplicationContext() , ABD_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_start)
                .setSound(soundUri)
                .setAutoCancel(false);

    }
}
