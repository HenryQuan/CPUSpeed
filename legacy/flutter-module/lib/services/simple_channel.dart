import 'package:flutter_module/services/base_channel.dart';

/// Handle basic native requests (open a link, show a toast and more)
class SimpleMethodChannel extends BaseMethodChannel {
  @override
  String name = "ui";

  void showToast(String message) {
    channel.invokeMethod(
      'toast',
      {'message': message},
    );
  }

  void openUrl(String url) {
    channel.invokeMethod('openUrl', {'url': url});
  }

  void sendFeedback() {
    channel.invokeMethod('feedback');
  }

  void shareApp() {
    channel.invokeMethod('share');
  }
}
