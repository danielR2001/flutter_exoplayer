import 'package:flutter/material.dart';

enum NotificationMode {
  NONE, //0
  NEXT, //1
  PREVIOUS, //2
  BOTH, //3
}

class AudioNotification {
  String smallIconFileName;
  String title;
  String subTitle;
  String largeIconUrl;
  bool isLocal;
  NotificationMode notificationMode;
  //! TODO add background color customization!
  //! TODO notification importance!
  //! TODO set timeout!

  AudioNotification({
    @required String smallIconFileName,
    String title,
    String subTitle,
    String largeIconUrl,
    bool isLocal = false,
    NotificationMode notificationMode = NotificationMode.BOTH,
  }) {
    isLocal ??= false;
    notificationMode ??= NotificationMode.BOTH;

    this.smallIconFileName = smallIconFileName;
    this.title = title;
    this.subTitle = subTitle;
    this.largeIconUrl = largeIconUrl;
    this.isLocal = isLocal;
    this.notificationMode = notificationMode;
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

  NotificationMode getNotificationMode() {
    return notificationMode;
  }
}
