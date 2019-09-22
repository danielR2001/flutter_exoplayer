import 'package:flutter/material.dart';

enum NotificationDefaultActions {
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
  CUSTOM1,
  CUSTOM2,
}

enum NotificationActionCallbackMode {
  CUSTOM,
  DEFAULT,
}

enum NotificationCustomActions {
  DISABLED,
  ONE,
  TWO,
}

class AudioNotification {
  String _smallIconFileName;
  String _title;
  String _subTitle;
  String _largeIconUrl;
  bool _isLocal;
  NotificationDefaultActions _notificationDefaultActions;
  NotificationActionCallbackMode _notificationActionCallbackMode;
  NotificationCustomActions _notificationCustomActions;
  //! TODO add background color!
  //! TODO notification importance!
  //! TODO set timeout!

  AudioNotification({
    @required String smallIconFileName,
    String title,
    String subTitle,
    String largeIconUrl,
    bool isLocal = false,
    NotificationDefaultActions notificationDefaultActions = NotificationDefaultActions.ALL,
    NotificationActionCallbackMode notificationActionCallbackMode =
        NotificationActionCallbackMode.DEFAULT,
    NotificationCustomActions notificationCustomActions =
        NotificationCustomActions.DISABLED,
  }) {
    isLocal ??= false;
    notificationDefaultActions ??= NotificationDefaultActions.ALL;
    notificationActionCallbackMode ??= NotificationActionCallbackMode.DEFAULT;

    this._smallIconFileName = smallIconFileName;
    this._title = title;
    this._subTitle = subTitle;
    this._largeIconUrl = largeIconUrl;
    this._isLocal = isLocal;
    this._notificationDefaultActions = notificationDefaultActions;
    this._notificationActionCallbackMode = notificationActionCallbackMode;
    this._notificationCustomActions = notificationCustomActions;
  }

  String get smallIconFileName => _smallIconFileName;

  String get title => _title;

  String get subTitle => _subTitle;

  String get largeIconUrl => _largeIconUrl;

  bool get isLocal => _isLocal;

  NotificationDefaultActions get notificationDefaultActions => _notificationDefaultActions;

  NotificationActionCallbackMode get notificationActionCallbackMode =>
      _notificationActionCallbackMode;

  NotificationCustomActions get notificationCustomActions =>
      _notificationCustomActions;
}
