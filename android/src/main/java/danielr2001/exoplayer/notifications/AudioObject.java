package danielr2001.exoplayer.notifications;

import danielr2001.exoplayer.enums.NotificationMode;

import android.graphics.Bitmap;

public class AudioObject {
    private String url;
    private int smallIcon;
    private String title;
    private String subTitle;
    private String largeIcomUrl;
    private boolean isLocal;
    private NotificationMode notificationMode;

    private Bitmap largeIcon;
 
    public AudioObject(String url, int smallIcon, String title, String subTitle, String largeIcomUrl, boolean isLocal, NotificationMode notificationMode){
        this.url = url;
        this.smallIcon = smallIcon;
        this.title = title;
        this.subTitle = subTitle;
        this.largeIcomUrl = largeIcomUrl;
        this.isLocal = isLocal;
        this.notificationMode = notificationMode;
    }

    public int getSmallIcon(){
        return smallIcon;
    }

    public String getTitle(){
        return title;
    }

    public String getSubTitle(){
        return subTitle;
    }

    public Bitmap getLargeIcon(){
        return largeIcon;
    }

    public void setLargeIcon(Bitmap bitmap){
        this.largeIcon = bitmap;
    }

    public String getUrl(){
        return url;
    }

    public NotificationMode getNotificationMode(){
        return notificationMode;
    }
}