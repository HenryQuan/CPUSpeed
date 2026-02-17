import 'package:flutter_module/services/base_channel.dart';

// Call all CPU related native methods
class CPUMethodChannel extends BaseMethodChannel {
  @override
  String name = "cpu";

  Future<void> setup() async {
    channel.invokeMethod('setup');
  }

  Future<Map<String, Object>?> getCPUInfo() async {
    return Map.from(await channel.invokeMethod('info'));
  }

  Future<void> setSpeed(int min, int max) async {
    channel.invokeMethod('setSpeed', {
      'max': max,
      'min': min,
    });
  }
}
