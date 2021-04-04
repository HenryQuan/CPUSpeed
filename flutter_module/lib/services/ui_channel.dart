import 'package:flutter_module/services/base_channel.dart';

/// Use native alert, toast and more UI related components
class UIChannel extends BaseMethodChannel {
  @override
  String name = "ui";

  void showToast(String message) {
    channel.invokeMethod(
      'toast',
      {'message': message},
    );
  }
}
