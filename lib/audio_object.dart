import 'package:flutter/material.dart';

enum NotificationMode {
  NONE,
  NEXT,
  PREVIOUS,
  BOTH,
}

class AudioObject {
  int smallIcon;
  String title;
  String subTitle;
  String largeIconUrl;
  bool isLocal;
  NotificationMode notificationMode;

  AudioObject({
    @required int smallIcon,
    String title,
    String subTitle,
    String largeIconUrl,
    bool isLocal = false,
    NotificationMode notificationMode = NotificationMode.BOTH,
  }) {
    isLocal ??= false;
    notificationMode ??= NotificationMode.BOTH;

    this.smallIcon = smallIcon;
    this.title = title;
    this.subTitle = subTitle;
    this.largeIconUrl = largeIconUrl;
    this.isLocal = isLocal;
    this.notificationMode = notificationMode;
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

  NotificationMode getNotificationMode() {
    return notificationMode;
  }
}
