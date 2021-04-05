import 'package:flutter_module/services/base_channel.dart';

// Call all CPU related native methods
class CPUMethodChannel extends BaseMethodChannel {
  @override
  String name = "cpu";

  Future<void> setup() async {
    channel.invokeMethod('setup');
  }
}
