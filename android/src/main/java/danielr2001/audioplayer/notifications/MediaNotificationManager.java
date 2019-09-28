package danielr2001.audioplayer.notifications;

import danielr2001.audioplayer.audioplayers.ForegroundAudioPlayer;
import danielr2001.audioplayer.enums.NotificationDefaultActions;
import danielr2001.audioplayer.enums.NotificationCustomActions;
import danielr2001.audioplayer.interfaces.AsyncResponse;
import danielr2001.audioplayer.R;
import danielr2001.audioplayer.models.AudioObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Map;

public class MediaNotificationManager {
    public static final String PLAY_ACTION = "com.daniel.exoPlayer.action.play";
    public static final String PAUSE_ACTION = "com.daniel.exoPlayer.action.pause";
    public static final String PREVIOUS_ACTION = "com.daniel.exoPlayer.action.previous";
    public static final String NEXT_ACTION = "com.daniel.exoPlayer.action.next";
    public static final String CUSTOM1_ACTION = "com.daniel.exoPlayer.action.custom1";
    public static final String CUSTOM2_ACTION = "com.daniel.exoPlayer.action.custom2";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "Playback";

    private ForegroundAudioPlayer foregroundExoPlayer;
    private Context context;
    private Activity activity;

    private NotificationManager notificationManager;
    private MediaSessionCompat mediaSession;

    private Intent playIntent;
    private Intent pauseIntent;
    private Intent prevIntent;
    private Intent nextIntent;
    private Intent notificationIntent;
    private Intent customIntent1;
    private Intent customIntent2;

    private PendingIntent ppPlayIntent;
    private PendingIntent pPauseIntent;
    private PendingIntent pPrevIntent;
    private PendingIntent pNextIntent;
    private PendingIntent pNotificatioIntent;
    private PendingIntent pCustomIntent1;
    private PendingIntent pCustomIntent2;

    private AudioObject audioObject;
    private boolean isPlaying;
    private boolean isShowing;
    private boolean isInitialized;

    public void setIsShowing(boolean isShowing) {
        this.isShowing = isShowing;
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void setIsInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public MediaNotificationManager(ForegroundAudioPlayer foregroundExoPlayer, Context context, MediaSessionCompat mediaSession, Activity activity) {
        this.context = context;
        this.foregroundExoPlayer = foregroundExoPlayer;
        this.mediaSession = mediaSession;
        this.activity = activity;
        isInitialized = false;

        initIntents();
    }

    private void initIntents() {
        notificationIntent = new Intent(this.context, activity.getClass());
        pNotificatioIntent = PendingIntent.getActivity(this.context, 0,
        notificationIntent, 0);

        playIntent = new Intent(this.context, ForegroundAudioPlayer.class);
        playIntent.setAction(PLAY_ACTION);
        ppPlayIntent = PendingIntent.getService(this.context, 1, playIntent, 0);

        pauseIntent = new Intent(this.context, ForegroundAudioPlayer.class);
        pauseIntent.setAction(PAUSE_ACTION);
        pPauseIntent = PendingIntent.getService(this.context, 1, pauseIntent, 0);

        prevIntent = new Intent(this.context, ForegroundAudioPlayer.class);
        prevIntent.setAction(PREVIOUS_ACTION);
        pPrevIntent = PendingIntent.getService(this.context, 1, prevIntent, 0);

        nextIntent = new Intent(this.context, ForegroundAudioPlayer.class);
        nextIntent.setAction(NEXT_ACTION);
        pNextIntent = PendingIntent.getService(this.context, 1, nextIntent, 0);

        customIntent1 = new Intent(this.context, ForegroundAudioPlayer.class);
        customIntent1.setAction(CUSTOM1_ACTION);
        pCustomIntent1 = PendingIntent.getService(this.context, 1, customIntent1, 0);

        customIntent2 = new Intent(this.context, ForegroundAudioPlayer.class);
        customIntent2.setAction(CUSTOM2_ACTION);
        pCustomIntent2 = PendingIntent.getService(this.context, 1, customIntent2, 0);

    }

    //make new notification
    public void makeNotification(AudioObject audioObject, boolean isPlaying) {
        isInitialized = true;
        isShowing = true;
        this.audioObject = audioObject;
        this.isPlaying = isPlaying;
        if (audioObject.getLargeIconUrl() != null) {
            loadImageFromUrl(audioObject.getLargeIconUrl(), audioObject.getIsLocal());
        } else {
            showNotification();
        }
    }
    
    //update current notification
    public void makeNotification(boolean isPlaying) {
        this.isPlaying = isPlaying;
        showNotification();

    }

    private void showNotification() {
        Notification notification;
        int icon = this.context.getResources().getIdentifier(audioObject.getSmallIconFileName(), "drawable",
                this.context.getPackageName());

                notificationManager = initNotificationManager();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(false)
                .setColorized(true)
                .setSound(null)
                .setContentIntent(pNotificatioIntent);

        if (audioObject.getTitle() != null) {
            builder.setContentTitle(audioObject.getTitle());
        }
        if (audioObject.getSubTitle() != null) {
            builder.setContentText(audioObject.getSubTitle());
        }
        if (audioObject.getLargeIcon() != null) {
            builder.setLargeIcon(audioObject.getLargeIcon());
        }
        if(!this.isPlaying){
            builder.setTimeoutAfter(900000);
        }
        builder = initNotificationActions(builder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = initNotificationStyle(builder);
        }

        notification = builder.build();

        notificationManager.notify(NOTIFICATION_ID, notification);
        if(this.isPlaying){
            foregroundExoPlayer.startForeground(NOTIFICATION_ID, notification);
        }else{
            foregroundExoPlayer.stopForeground(false);
        }
    }

    private NotificationManager initNotificationManager() {
        NotificationManager notificationManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Playback",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setSound(null, null);
            notificationChannel.setShowBadge(false);

            notificationManager = (android.app.NotificationManager) this.context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        } else {
            notificationManager = (android.app.NotificationManager) this.context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    private NotificationCompat.Builder initNotificationActions(NotificationCompat.Builder builder) {
        int customIcon1 = this.context.getResources().getIdentifier("ic_custom1", "drawable", //! TODO maybe change to custom file name
        this.context.getPackageName());
        int customIcon2 = this.context.getResources().getIdentifier("ic_custom2", "drawable",
        this.context.getPackageName());

        if(audioObject.getNotificationCustomActions() == NotificationCustomActions.ONE || audioObject.getNotificationCustomActions() == NotificationCustomActions.TWO){
            builder.addAction(customIcon1, "Custom1", pCustomIntent1);
        }
        if (audioObject.getNotificationActionMode() == NotificationDefaultActions.PREVIOUS || audioObject.getNotificationActionMode() == NotificationDefaultActions.ALL) {
            builder.addAction(R.drawable.ic_previous, "Previous", pPrevIntent);
        }

        if (this.isPlaying) {
            builder.addAction(R.drawable.ic_pause, "Pause", pPauseIntent);
        } else {
            builder.addAction(R.drawable.ic_play, "Play", ppPlayIntent);
        }

        if (audioObject.getNotificationActionMode() == NotificationDefaultActions.NEXT || audioObject.getNotificationActionMode() == NotificationDefaultActions.ALL) {
            builder.addAction(R.drawable.ic_next, "Next", pNextIntent);
        }
        if(audioObject.getNotificationCustomActions() == NotificationCustomActions.TWO){
            builder.addAction(customIcon2, "Custom2", pCustomIntent2);
        }
        return builder;
    }

    private NotificationCompat.Builder initNotificationStyle(NotificationCompat.Builder builder) {
        if (audioObject.getNotificationActionMode() == NotificationDefaultActions.NEXT
                || audioObject.getNotificationActionMode() == NotificationDefaultActions.PREVIOUS) {
            builder.setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                    .setShowActionsInCompactView(0, 1)
                    .setMediaSession(mediaSession.getSessionToken()));
        } else if (audioObject.getNotificationActionMode() == NotificationDefaultActions.ALL) {
            builder.setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.getSessionToken()));
        } else {
            builder.setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                    .setShowActionsInCompactView(0)
                    .setMediaSession(mediaSession.getSessionToken()));
        }
        return builder;
    }

    private void loadImageFromUrl(String imageUrl, boolean isLocal) {
        try {
            new LoadImageFromUrl(imageUrl, isLocal, new AsyncResponse() {
                @Override
                public void processFinish(Map<String,Bitmap> bitmapMap) {
                    if (bitmapMap != null) {
                        if(bitmapMap.get(audioObject.getLargeIconUrl()) != null){
                            audioObject.setLargeIcon(bitmapMap.get(audioObject.getLargeIconUrl()));
                            showNotification();
                        }else{
                            Log.e("ExoPlayerPlugin", "canceled showing notification!");
                        }
                    } else {
                        showNotification();
                        Log.e("ExoPlayerPlugin", "Failed loading image!");
                    }
                }
            }).execute();
        } catch (Exception e) {
            Log.e("ExoPlayerPlugin", "Failed loading image!");
        }
    }
}
