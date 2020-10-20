package danielr2001.audioplayer.models;

import android.graphics.Bitmap;

import danielr2001.audioplayer.enums.NotificationActionCallbackMode;
import danielr2001.audioplayer.enums.NotificationCustomActions;
import danielr2001.audioplayer.enums.NotificationDefaultActions;

public class AudioObject {
    private String url;
    private String smallIconFileName;
    private String title;
    private String subTitle;
    private String largeIconUrl;
    private boolean isLocal;
    private long duration;
    private NotificationDefaultActions notificationDefaultActions;
    private NotificationActionCallbackMode notificationActionCallbackMode;
    private NotificationCustomActions notificationCustomActions;

    private Bitmap largeIcon;

    // clone
    public AudioObject(AudioObject audioObject) {
        this.url = audioObject.url;
        this.smallIconFileName = audioObject.smallIconFileName;
        this.title = audioObject.title;
        this.subTitle = audioObject.subTitle;
        this.largeIconUrl = audioObject.largeIconUrl;
        this.isLocal = audioObject.isLocal;
        this.duration = audioObject.duration;
        this.notificationDefaultActions = audioObject.notificationDefaultActions;
        this.notificationActionCallbackMode = audioObject.notificationActionCallbackMode;
        this.notificationCustomActions = audioObject.notificationCustomActions;
    }

    // for foreground player
    public AudioObject(String url, String smallIconFileName, String title, String subTitle, String largeIconUrl,
                       boolean isLocal, NotificationDefaultActions notificationDefaultActions,
                       NotificationActionCallbackMode notificationActionCallbackMode,
                       NotificationCustomActions notificationCustomActions) {
        this.url = url;
        this.smallIconFileName = smallIconFileName;
        this.title = title;
        this.subTitle = subTitle;
        this.largeIconUrl = largeIconUrl;
        this.isLocal = isLocal;
        this.notificationDefaultActions = notificationDefaultActions;
        this.notificationActionCallbackMode = notificationActionCallbackMode;
        this.notificationCustomActions = notificationCustomActions;
    }

    // for background player
    public AudioObject(String url) {
        this.url = url;
    }

    // for notification change
    public AudioObject(String smallIconFileName, String title, String subTitle, String largeIconUrl,
                       NotificationDefaultActions notificationDefaultActions,
                       NotificationActionCallbackMode notificationActionCallbackMode,
                       NotificationCustomActions notificationCustomActions) {
        this.smallIconFileName = smallIconFileName;
        this.title = title;
        this.subTitle = subTitle;
        this.largeIconUrl = largeIconUrl;
        this.notificationDefaultActions = notificationDefaultActions;
        this.notificationActionCallbackMode = notificationActionCallbackMode;
        this.notificationCustomActions = notificationCustomActions;
    }

    public String getSmallIconFileName() {
        return smallIconFileName;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getLargeIconUrl() {
        return largeIconUrl;
    }

    public Bitmap getLargeIcon() {
        return largeIcon;
    }

    public String getUrl() {
        return url;
    }

    public boolean getIsLocal() {
        return isLocal;
    }

    public long getDuration() {
        return duration;
    }

    public NotificationDefaultActions getNotificationActionMode() {
        return notificationDefaultActions;
    }

    public NotificationActionCallbackMode getNotificationActionCallbackMode() {
        return notificationActionCallbackMode;
    }

    public NotificationCustomActions getNotificationCustomActions() {
        return notificationCustomActions;
    }

    public void setLargeIcon(Bitmap bitmap) {
        this.largeIcon = bitmap;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setIsLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}