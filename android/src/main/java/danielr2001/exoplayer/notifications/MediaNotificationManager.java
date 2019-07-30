package danielr2001.exoplayer.notifications;

import danielr2001.exoplayer.audioplayers.ForegroundExoPlayer;
import danielr2001.exoplayer.enums.NotificationMode;
import danielr2001.exoplayer.interfaces.AsyncResponse;
import danielr2001.exoplayer.R;

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

public class MediaNotificationManager {
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
    private NotificationCompat.Builder builder;

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

    public MediaNotificationManager(ForegroundExoPlayer foregroundExoPlayer, Context context){
        this.context = context;
        this.foregroundExoPlayer = foregroundExoPlayer;

        initIntents();
    }

    private void initIntents() {
        //notificationIntent = new Intent(this.context, MainActivity.class);
        //pendingIntent = PendingIntent.getActivity(this.context, 0, notificationIntent, 0);

        playIntent = new Intent(this.context, ForegroundExoPlayer.class);
        playIntent.setAction(PLAY_ACTION);
        pplayIntent = PendingIntent.getService(this.context, 1, playIntent, 0);

        pauseIntent = new Intent(this.context, ForegroundExoPlayer.class);
        pauseIntent.setAction(PAUSE_ACTION);
        ppauseIntent = PendingIntent.getService(this.context, 1, pauseIntent, 0);

        prevIntent = new Intent(this.context, ForegroundExoPlayer.class);
        prevIntent.setAction(PREVIOUS_ACTION);
        pprevIntent = PendingIntent.getService(this.context, 1, prevIntent, 0);

        nextIntent = new Intent(this.context, ForegroundExoPlayer.class);
        nextIntent.setAction(NEXT_ACTION);
        pnextIntent = PendingIntent.getService(this.context, 1, nextIntent, 0);

    }

    public void makeNotification(AudioObject audioObject, boolean isPlaying){
        this.audioObject = audioObject;
        this.isPlaying = isPlaying;
        if(audioObject.getLargeIcomUrl() != null){
            loadImageFromUrl(audioObject.getLargeIcomUrl(), audioObject.getIsLocal());
        }else{
            showNotification();
        }
    }

    public void makeNotification(boolean isPlaying){
        this.isPlaying = isPlaying;
        showNotification();
        
    }

    private void showNotification(){
        Notification notification;
        int icon = this.context.getResources().getIdentifier(audioObject.getSmallIconFileName(), "drawable", this.context.getPackageName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaSession = new MediaSessionCompat(this.context, "playback");
            CreateNotificationChannel();
            builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                    .setSmallIcon(icon)
                    //.setOngoing(this.isPlaying)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(false)
                    .setColorized(true);
                    //.setContentIntent(pendingIntent);

            if(audioObject.getTitle() != null){
                builder.setContentTitle(audioObject.getTitle());
            }
            if(audioObject.getSubTitle() != null){
                builder.setContentText(audioObject.getSubTitle());
            }
            if(audioObject.getLargeIcon() != null){
                builder.setLargeIcon(audioObject.getLargeIcon());
            }
            initNotificationActions();
            initNotificationStyle();

            notification = builder.build();

        } else {
            notificationManager = (android.app.NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                    .setSmallIcon(icon)
                    //.setOngoing(this.isPlaying)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(false)
                    .setSound(null);
                   // .setContentIntent(pendingIntent);
                   //! TODO content intent => current activity
            if(audioObject.getTitle() != null){
                builder.setContentTitle(audioObject.getTitle());
            }
            if(audioObject.getSubTitle() != null){
                builder.setContentText(audioObject.getSubTitle());
            }
            if(audioObject.getLargeIcon() != null){
                builder.setLargeIcon(audioObject.getLargeIcon());
            }   
            initNotificationActions();   

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

    private void loadImageFromUrl(String imageUrl, boolean isLocal) {
        try {
          new LoadImageFromUrl(imageUrl, isLocal, new AsyncResponse() {
            @Override
            public void processFinish(Bitmap bitmap) {
              if (bitmap != null) {
                audioObject.setLargeIcon(bitmap);
                showNotification();
              } else {
                Log.e("ExoPlayerPlugin", "Failed loading image!");
              }
            }
          }).execute();
        } catch (Exception e) {
          Log.e("ExoPlayerPlugin", "Failed loading image!");
        }
      }

    private void initNotificationActions(){
        if(audioObject.getNotificationMode() != NotificationMode.NONE){
            if(audioObject.getNotificationMode() == NotificationMode.BOTH){
                builder.addAction(R.drawable.ic_previous, "Previous", pprevIntent);
                if(isPlaying){
                    builder.addAction(R.drawable.ic_pause, "Pause", ppauseIntent);
                }else{
                    builder.addAction(R.drawable.ic_play, "Play", pplayIntent);
                }
                builder.addAction(R.drawable.ic_next, "Next", pnextIntent);
            }else{
                if(audioObject.getNotificationMode() == NotificationMode.PREVIOUS){
                    builder.addAction(R.drawable.ic_previous, "Previous", pprevIntent);
                }
                if(isPlaying){
                    builder.addAction(R.drawable.ic_pause, "Pause", ppauseIntent);
                }else{
                    builder.addAction(R.drawable.ic_play, "Play", pplayIntent);
                }
                if(audioObject.getNotificationMode() == NotificationMode.NEXT){
                    builder.addAction(R.drawable.ic_next, "Next", pnextIntent);
                }
            }
        }else{
            if(isPlaying){
                builder.addAction(R.drawable.ic_pause, "Pause", ppauseIntent);
            }else{
                builder.addAction(R.drawable.ic_play, "Play", pplayIntent);
            }
        }
    }

    private void initNotificationStyle(){
        if(audioObject.getNotificationMode() == NotificationMode.NEXT || audioObject.getNotificationMode() == NotificationMode.PREVIOUS){
            builder.setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
            .setShowActionsInCompactView(0, 1).setMediaSession(mediaSession.getSessionToken()));
        }else if(audioObject.getNotificationMode() == NotificationMode.BOTH){
            builder.setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
            .setShowActionsInCompactView(0, 1, 2).setMediaSession(mediaSession.getSessionToken()));
        }else{
            builder.setStyle(new androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
            .setShowActionsInCompactView(0).setMediaSession(mediaSession.getSessionToken()));
        }
    }
}
