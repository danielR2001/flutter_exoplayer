package dr.library.exoplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;

public class MediaNotificationManager {
    public static final String SERVICE_STARTED = "com.daniel.exoPlayer.action.serviceStarted";
    public static final String PLAY_ACTION = "com.daniel.exoPlayer.action.play";
    public static final String PAUSE_ACTION = "com.daniel.exoPlayer.action.pause";
    public static final String PREVIOUS_ACTION = "com.daniel.exoPlayer.action.previous";
    public static final String NEXT_ACTION = "com.daniel.exoPlayer.action.next";
    private static final int notificationId = 1;
    private static final String CHANNEL_ID = "Playback";

    private ForegroundExoPlayer foregroundExoPlayer;
    private Context context;

    private MediaSessionCompat mediaSession;
    private NotificationManager notificationManager;

    private Intent playIntent;
    private Intent pauseIntent;
    private Intent prevIntent;
    private Intent nextIntent;
    private Intent notificationIntent;
    private Intent deleteIntent;

    private PendingIntent pplayIntent;
    private PendingIntent ppauseIntent;
    private PendingIntent pprevIntent;
    private PendingIntent pnextIntent;
    private PendingIntent pendingIntent;

    MediaNotificationManager(ForegroundExoPlayer foregroundExoPlayer){
        this.context = foregroundExoPlayer.getApplicationContext();
        this.foregroundExoPlayer = foregroundExoPlayer;

        initIntents();
    }

    private void initIntents() {
        //notificationIntent = new Intent(this.context, MainActivity.class);
        //pendingIntent = PendingIntent.getActivity(this.context, 0, notificationIntent, 0);

        playIntent = new Intent(this.context, ForegroundExoPlayer.class);
        playIntent.setAction(PLAY_ACTION);
        pplayIntent = PendingIntent.getBroadcast(this.context, 1, playIntent, 0);

        pauseIntent = new Intent(this.context, ForegroundExoPlayer.class);
        pauseIntent.setAction(PAUSE_ACTION);
        ppauseIntent = PendingIntent.getBroadcast(this.context, 1, pauseIntent, 0);

        prevIntent = new Intent(this.context, ForegroundExoPlayer.class);
        prevIntent.setAction(PREVIOUS_ACTION);
        pprevIntent = PendingIntent.getBroadcast(this.context, 1, prevIntent, 0);

        nextIntent = new Intent(this.context, ForegroundExoPlayer.class);
        nextIntent.setAction(NEXT_ACTION);
        pnextIntent = PendingIntent.getBroadcast(this.context, 1, nextIntent, 0);

    }

    public void makeNotification(AudioObject audioObject, boolean isPlaying){
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaSession = new MediaSessionCompat(this.context, "playback");
            CreateNotificationChannel();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                    .setContentTitle(audioObject.getTitle())
                    .setContentText(audioObject.getSubTitle())
                    .setSmallIcon(audioObject.getSmallIcon())
                    .setLargeIcon(audioObject.getLargeIcon())
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_previous, "", pprevIntent)
                    .addAction(R.drawable.ic_next, "", pnextIntent)
                    .setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                            .setShowActionsInCompactView(0, 1, 2).setMediaSession(mediaSession.getSessionToken()));
                    //.setContentIntent(pendingIntent);
            // .build();
            if(isPlaying){
                builder.addAction(R.drawable.ic_pause, "", ppauseIntent);
                builder.setOngoing(true);
            }else{
                builder.addAction(R.drawable.ic_play, "", pplayIntent);
            }
            notification = builder.build();

        } else {
            notificationManager = (android.app.NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                    .setContentTitle(audioObject.getTitle())
                    .setContentText(audioObject.getSubTitle())
                    .setSmallIcon(audioObject.getSmallIcon())
                    .setLargeIcon(audioObject.getLargeIcon())
                    .setWhen(System.currentTimeMillis())
                    .setSound(null)
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_previous, "", pprevIntent)
                    .addAction(R.drawable.ic_next, "", pnextIntent);
                   // .setContentIntent(pendingIntent);

            if(isPlaying){
                builder.addAction(R.drawable.ic_pause, "", ppauseIntent);
                builder.setOngoing(true);
            }else{
                builder.addAction(R.drawable.ic_play, "", pplayIntent);
            }
            notification = builder.build();
        }
        notificationManager.notify(notificationId, notification);
        foregroundExoPlayer.startForeground(notificationId,notification);
    }


    private void CreateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Playback",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setSound(null, null);
            notificationChannel.setShowBadge(false);
            notificationManager = (android.app.NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
