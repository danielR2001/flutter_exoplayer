class AudioObject {
  int smallIcon;
  String title;
  String subTitle;
  String largeIconUrl;
  bool isLocal;

  AudioObject(int smallIcon, String title, String subTitle, String largeIconUrl,
      bool isLocal) {
    this.smallIcon = smallIcon;
    this.title = title;
    this.subTitle = subTitle;
    this.largeIconUrl = largeIconUrl;
    this.isLocal = isLocal;
  }

  int getSmallIcon() {
    return smallIcon;
  }

  String getTitle() {
    return title;
  }

  String getSubTitle() {
    return subTitle;
  }

  String getLargeIconUrl() {
    return largeIconUrl;
  }

  bool getIsLocal() {
    return isLocal;
  }
}
