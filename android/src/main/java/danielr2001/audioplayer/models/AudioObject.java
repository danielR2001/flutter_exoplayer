package danielr2001.audioplayer.models;

import danielr2001.audioplayer.enums.NotificationDefaultActions;
import danielr2001.audioplayer.enums.NotificationCustomActions;
import danielr2001.audioplayer.enums.NotificationActionCallbackMode;

import android.graphics.Bitmap;

public class AudioObject {
    private String url;
    private String smallIconFileName;
    private String title;
    private String subTitle;
    private String largeIconUrl;
    private boolean isLocal;
    private NotificationDefaultActions notificationDefaultActions;
    private NotificationActionCallbackMode notificationActionCallbackMode;
    private NotificationCustomActions notificationCustomActions;

    private Bitmap largeIcon;
    
    //for foreground player
    public AudioObject(String url, String smallIconFileName, String title, String subTitle, String largeIconUrl, boolean isLocal, NotificationDefaultActions notificationDefaultActions, NotificationActionCallbackMode notificationActionCallbackMode, NotificationCustomActions notificationCustomActions){
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

    //for background player
    public AudioObject(String url){
        this.url = url;
    }

    public String getSmallIconFileName(){
        return smallIconFileName;
    }

    public String getTitle(){
        return title;
    }

    public String getSubTitle(){
        return subTitle;
    }

    public String getLargeIconUrl(){
        return largeIconUrl;
    }

    public Bitmap getLargeIcon(){
        return largeIcon;
    }

    public String getUrl(){
        return url;
    }

    public boolean getIsLocal(){
        return isLocal;
    }

    public NotificationDefaultActions getNotificationActionMode(){
        return notificationDefaultActions;
    }

    public NotificationActionCallbackMode getNotificationActionCallbackMode(){
        return notificationActionCallbackMode;
    }

    public NotificationCustomActions getNotificationCustomActions(){
        return notificationCustomActions;
    }

    public void setLargeIcon(Bitmap bitmap){
        this.largeIcon = bitmap;
    }
}