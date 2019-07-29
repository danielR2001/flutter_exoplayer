package danielr2001.exoplayer;

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

    MediaNotificationManager(ForegroundExoPlayer foregroundExoPlayer, Context context){
        this.context = context;
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
        Resources res = this.context.getResources();
        int icon = res.getIdentifier("ic_launcher", "drawable", "dr.library.exoplayer_example");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaSession = new MediaSessionCompat(this.context, "playback");
            CreateNotificationChannel();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(false);
                    //.setContentIntent(pendingIntent);

            if(audioObject.getNotificationMode() != NotificationMode.NONE){
                if(audioObject.getNotificationMode() == NotificationMode.BOTH){
                    builder.addAction(R.drawable.ic_previous, "", pprevIntent);
                    if(isPlaying){
                        builder.addAction(R.drawable.ic_pause, "", ppauseIntent);
                        builder.setOngoing(true);
                    }else{
                        builder.addAction(R.drawable.ic_play, "", pplayIntent);
                    }
                    builder.addAction(R.drawable.ic_next, "", pnextIntent);
                }else{
                    if(audioObject.getNotificationMode() == NotificationMode.PREVIOUS){
                        builder.addAction(R.drawable.ic_previous, "", pprevIntent);
                    }
                    if(isPlaying){
                        builder.addAction(R.drawable.ic_pause, "", ppauseIntent);
                        builder.setOngoing(true);
                    }else{
                        builder.addAction(R.drawable.ic_play, "", pplayIntent);
                    }
                    if(audioObject.getNotificationMode() == NotificationMode.NEXT){
                        builder.addAction(R.drawable.ic_next, "", pnextIntent);
                    }
                }
            }else{
                if(isPlaying){
                    builder.addAction(R.drawable.ic_pause, "", ppauseIntent);
                    builder.setOngoing(true);
                }else{
                    builder.addAction(R.drawable.ic_play, "", pplayIntent);
                }
            }

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

            if(audioObject.getTitle() != null){
                builder.setContentTitle(audioObject.getTitle());
            }
            if(audioObject.getSubTitle() != null){
                builder.setContentText(audioObject.getSubTitle());
            }
            if(audioObject.getLargeIcon() != null){
                builder.setLargeIcon(audioObject.getLargeIcon());
            }
            notification = builder.build();

        } else {
            notificationManager = (android.app.NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context, CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(false)
                    .setSound(null);
                   // .setContentIntent(pendingIntent);
                if(audioObject.getNotificationMode() != NotificationMode.NONE){
                    if(audioObject.getNotificationMode() == NotificationMode.BOTH){
                        builder.addAction(R.drawable.ic_previous, "Previous", pprevIntent);
                        if(isPlaying){
                            builder.addAction(R.drawable.ic_pause, "Pause", ppauseIntent);
                            builder.setOngoing(true);
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
                            builder.setOngoing(true);
                        }else{
                            builder.addAction(R.drawable.ic_play, "Play", pplayIntent);
                        }
                        if(audioObject.getNotificationMode() == NotificationMode.NEXT){
                            builder.addAction(R.drawable.ic_next, "Next", pnextIntent);
                        }
                    }
                }else{
                    if(isPlaying){
                        builder.addAction(R.drawable.ic_pause, "", ppauseIntent);
                        builder.setOngoing(true);
                    }else{
                        builder.addAction(R.drawable.ic_play, "", pplayIntent);
                    }
                }
                if(audioObject.getTitle() != null){
                    builder.setContentTitle(audioObject.getTitle());
                }
                if(audioObject.getSubTitle() != null){
                    builder.setContentText(audioObject.getSubTitle());
                }
                if(audioObject.getLargeIcon() != null){
                    builder.setLargeIcon(audioObject.getLargeIcon());
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

    private void loadImageFromUrl(String imageUrl, boolean isLocal) {
        try {
          new LoadImageFromUrl(imageUrl, isLocal, new AsyncResponse() {
            @Override
            public void processFinish(Bitmap bitmap) {
              if (bitmap != null) {
                  //TODO use bitmap!
              } else {
                Log.e("LoadImageFromUrl", "Failed loading image!");
              }
            }
          }).execute();
        } catch (Exception e) {
          Log.e("LoadImageFromUrl", "Failed loading image!");
        }
      }
}
