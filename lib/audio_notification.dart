import 'package:flutter/material.dart';

enum NotificationActionMode {
  NONE, //0
  NEXT, //1
  PREVIOUS, //2
  ALL, //3
}

enum NotificationActionName{
  PREVIOUS,
  NEXT,
  PLAY,
  PAUSE,
}

enum NotificationActionCallbackMode{
  CUSTOM,
  DEFAULT,
}

class AudioNotification {
  String smallIconFileName;
  String title;
  String subTitle;
  String largeIconUrl;
  bool isLocal;
  NotificationActionMode notificationActionMode;
  NotificationActionCallbackMode notificationActionCallbackMode;
  //! TODO add background color!
  //! TODO notification importance!
  //! TODO set timeout!

  AudioNotification({
    @required String smallIconFileName,
    String title,
    String subTitle,
    String largeIconUrl,
    bool isLocal = false,
    NotificationActionMode notificationActionMode = NotificationActionMode.ALL,
    NotificationActionCallbackMode notificationActionCallbackMode = NotificationActionCallbackMode.DEFAULT,
  }) {
    isLocal ??= false;
    notificationActionMode ??= NotificationActionMode.ALL;
    notificationActionCallbackMode ??= NotificationActionCallbackMode.DEFAULT;

    this.smallIconFileName = smallIconFileName;
    this.title = title;
    this.subTitle = subTitle;
    this.largeIconUrl = largeIconUrl;
    this.isLocal = isLocal;
    this.notificationActionMode = notificationActionMode;
    this.notificationActionCallbackMode = notificationActionCallbackMode;
  }

  String getSmallIconFileName() {
    return smallIconFileName;
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

  NotificationActionMode getNotificationMode() {
    return notificationActionMode;
  }

  NotificationActionCallbackMode getNotificationActionCallbackMode(){
    return notificationActionCallbackMode;
  }
}
