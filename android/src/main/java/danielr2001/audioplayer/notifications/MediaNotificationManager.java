package danielr2001.audioplayer.notifications;

import danielr2001.audioplayer.audioplayers.ForegroundAudioPlayer;
import danielr2001.audioplayer.enums.NotificationMode;
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
    private static final int notificationId = 1;
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
    private Intent deleteIntent;

    private PendingIntent pplayIntent;
    private PendingIntent ppauseIntent;
    private PendingIntent pprevIntent;
    private PendingIntent pnextIntent;
    private PendingIntent pendingIntent;

    private AudioObject audioObject;
    private boolean isPlaying;

    public MediaNotificationManager(ForegroundAudioPlayer foregroundExoPlayer, Context context, MediaSessionCompat mediaSession, Activity activity) {
        this.context = context;
        this.foregroundExoPlayer = foregroundExoPlayer;
        this.mediaSession = mediaSession;
        this.activity = activity;

        initIntents();
    }

    private void initIntents() {
        notificationIntent = new Intent(this.context, activity.getClass());
        pendingIntent = PendingIntent.getActivity(this.context, 0,
        notificationIntent, 0);

        playIntent = new Intent(this.context, ForegroundAudioPlayer.class);
        playIntent.setAction(PLAY_ACTION);
        pplayIntent = PendingIntent.getService(this.context, 1, playIntent, 0);

        pauseIntent = new Intent(this.context, ForegroundAudioPlayer.class);
        pauseIntent.setAction(PAUSE_ACTION);
        ppauseIntent = PendingIntent.getService(this.context, 1, pauseIntent, 0);

        prevIntent = new Intent(this.context, ForegroundAudioPlayer.class);
        prevIntent.setAction(PREVIOUS_ACTION);
        pprevIntent = PendingIntent.getService(this.context, 1, prevIntent, 0);

        nextIntent = new Intent(this.context, ForegroundAudioPlayer.class);
        nextIntent.setAction(NEXT_ACTION);
        pnextIntent = PendingIntent.getService(this.context, 1, nextIntent, 0);

    }

    //make new notification
    public void makeNotification(AudioObject audioObject, boolean isPlaying) {
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
                .setContentIntent(pendingIntent);

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

        notificationManager.notify(notificationId, notification);
        if(this.isPlaying){
            foregroundExoPlayer.startForeground(notificationId, notification);
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
        if (audioObject.getNotificationMode() == NotificationMode.PREVIOUS || audioObject.getNotificationMode() == NotificationMode.BOTH) {
            builder.addAction(R.drawable.ic_previous, "Previous", pprevIntent);
        }

        if (this.isPlaying) {
            builder.addAction(R.drawable.ic_pause, "Pause", ppauseIntent);
        } else {
            builder.addAction(R.drawable.ic_play, "Play", pplayIntent);
        }

        if (audioObject.getNotificationMode() == NotificationMode.NEXT || audioObject.getNotificationMode() == NotificationMode.BOTH) {
            builder.addAction(R.drawable.ic_next, "Next", pnextIntent);
        }
        return builder;
    }

    private NotificationCompat.Builder initNotificationStyle(NotificationCompat.Builder builder) {
        if (audioObject.getNotificationMode() == NotificationMode.NEXT
                || audioObject.getNotificationMode() == NotificationMode.PREVIOUS) {
            builder.setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
                    .setShowActionsInCompactView(0, 1)
                    .setMediaSession(mediaSession.getSessionToken()));
        } else if (audioObject.getNotificationMode() == NotificationMode.BOTH) {
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
