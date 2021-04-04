import 'package:flutter_module/services/base_channel.dart';

/// Use native alert, toast and more UI related components
class SimpleMethodChannel extends BaseMethodChannel {
  @override
  String name = "ui";

  void showToast(String message) {
    channel.invokeMethod(
      'toast',
      {'message': message},
    );
  }

  void showAbout() {
    channel.invokeMethod('about');
  }

  void sendFeedback() {
    channel.invokeMethod('feedback');
  }

  void shareApp() {
    channel.invokeMethod('share');
  }
}
