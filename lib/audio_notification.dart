import 'package:flutter/material.dart';

enum NotificationActionMode {
  NONE,
  NEXT,
  PREVIOUS,
  ALL,
}

enum NotificationActionName {
  PREVIOUS,
  NEXT,
  PLAY,
  PAUSE,
}

enum NotificationActionCallbackMode {
  CUSTOM,
  DEFAULT,
}

class AudioNotification {
  String _smallIconFileName;
  String _title;
  String _subTitle;
  String _largeIconUrl;
  bool _isLocal;
  NotificationActionMode _notificationActionMode;
  NotificationActionCallbackMode _notificationActionCallbackMode;
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
    NotificationActionCallbackMode notificationActionCallbackMode =
        NotificationActionCallbackMode.DEFAULT,
  }) {
    isLocal ??= false;
    notificationActionMode ??= NotificationActionMode.ALL;
    notificationActionCallbackMode ??= NotificationActionCallbackMode.DEFAULT;

    this._smallIconFileName = smallIconFileName;
    this._title = title;
    this._subTitle = subTitle;
    this._largeIconUrl = largeIconUrl;
    this._isLocal = isLocal;
    this._notificationActionMode = notificationActionMode;
    this._notificationActionCallbackMode = notificationActionCallbackMode;
  }

  String get smallIconFileName => _smallIconFileName;

  String get title => _title;

  String get subTitle => _subTitle;

  String get largeIconUrl => _largeIconUrl;

  bool get isLocal => _isLocal;

  NotificationActionMode get notificationMode => _notificationActionMode;

  NotificationActionCallbackMode get notificationActionCallbackMode =>
      _notificationActionCallbackMode;
}
