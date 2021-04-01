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
  final String smallIconFileName;
  final String? title;
  final String? subTitle;
  final String? largeIconUrl;
  final bool isLocal;
  final NotificationDefaultActions notificationDefaultActions;
  final NotificationActionCallbackMode notificationActionCallbackMode;
  final NotificationCustomActions notificationCustomActions;
  // TODO add background color!
  // TODO notification importance!
  // TODO set timeout!

  AudioNotification({
    required this.smallIconFileName,
    this.title,
    this.subTitle = "",
    this.largeIconUrl = "",
    this.isLocal = false,
    this.notificationDefaultActions = NotificationDefaultActions.ALL,
    this.notificationActionCallbackMode =
        NotificationActionCallbackMode.DEFAULT,
    this.notificationCustomActions = NotificationCustomActions.DISABLED,
  });
}
