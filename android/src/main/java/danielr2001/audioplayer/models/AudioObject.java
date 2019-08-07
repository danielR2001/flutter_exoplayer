package danielr2001.audioplayer.models;

import danielr2001.audioplayer.enums.NotificationMode;

import android.graphics.Bitmap;

public class AudioObject {
    private String url;
    private String smallIconFileName;
    private String title;
    private String subTitle;
    private String largeIconUrl;
    private boolean isLocal;
    private NotificationMode notificationMode;

    private Bitmap largeIcon;
    
    //for foreground player
    public AudioObject(String url, String smallIconFileName, String title, String subTitle, String largeIconUrl, boolean isLocal, NotificationMode notificationMode){
        this.url = url;
        this.smallIconFileName = smallIconFileName;
        this.title = title;
        this.subTitle = subTitle;
        this.largeIconUrl = largeIconUrl;
        this.isLocal = isLocal;
        this.notificationMode = notificationMode;
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

    public NotificationMode getNotificationMode(){
        return notificationMode;
    }

    public void setLargeIcon(Bitmap bitmap){
        this.largeIcon = bitmap;
    }
}