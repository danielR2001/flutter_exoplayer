package dr.library.exoplayer;

import android.graphics.Bitmap;

public class AudioObject {
    private int smallIcon;
    private String title;
    private String subTitle;
    private Bitmap largeIcon;
    private String url;

    AudioObject(int smallIcon, String title, String subTitle, Bitmap largeIcon, String url){
        this.smallIcon = smallIcon;
        this.title = title;
        this.subTitle = subTitle;
        this.largeIcon = largeIcon;
        this.url = url;
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

    public String getUrl(){
        return url;
    }
}