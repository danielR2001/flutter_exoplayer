package danielr2001.audioplayer.models;

import danielr2001.audioplayer.enums.NotificationActionMode;
import danielr2001.audioplayer.enums.NotificationActionCallbackMode;

import android.graphics.Bitmap;

public class AudioObject {
    private String url;
    private String smallIconFileName;
    private String title;
    private String subTitle;
    private String largeIconUrl;
    private boolean isLocal;
    private NotificationActionMode notificationActionMode;
    private NotificationActionCallbackMode notificationActionCallbackMode;

    private Bitmap largeIcon;
    
    //for foreground player
    public AudioObject(String url, String smallIconFileName, String title, String subTitle, String largeIconUrl, boolean isLocal, NotificationActionMode notificationActionMode, NotificationActionCallbackMode notificationActionCallbackMode){
        this.url = url;
        this.smallIconFileName = smallIconFileName;
        this.title = title;
        this.subTitle = subTitle;
        this.largeIconUrl = largeIconUrl;
        this.isLocal = isLocal;
        this.notificationActionMode = notificationActionMode;
        this.notificationActionCallbackMode = notificationActionCallbackMode;
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

    public NotificationActionMode getNotificationActionMode(){
        return notificationActionMode;
    }

    public NotificationActionCallbackMode getNotificationActionCallbackMode(){
        return notificationActionCallbackMode;
    }

    public void setLargeIcon(Bitmap bitmap){
        this.largeIcon = bitmap;
    }
}