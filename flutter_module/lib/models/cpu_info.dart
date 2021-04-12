import 'dart:math';

class CPUInfo {
  late int maxFrequency;
  late int minFrequency;
  late int currMaxFrequency;
  late int currMinFrequency;
  /// This is used to calculate current percentage
  late int _diffFrequency;

  /// For example, 4 x 1.90 GHz | 4 x 2.46 GHZ
  late String cpuInfo;

  CPUInfo(Map<String, Object?>? json) {
    // Unpack data from map
    cpuInfo = (json?['info'] as String?) ?? 'Unknown';

    maxFrequency = (json?['max'] as int?) ?? 0;
    minFrequency = (json?['min'] as int?) ?? 0;

    currMaxFrequency = (json?['max_curr'] as int?) ?? 0;
    currMinFrequency = (json?['min_curr'] as int?) ?? 0;

    // NOTE: Make sure it is not 0 to prevent zero division
    _diffFrequency = max(maxFrequency - minFrequency, 1);
  }

  double calcCurrentPercent(bool max) {
    if (max) {
      return (currMaxFrequency - minFrequency) / _diffFrequency;
    } else {
      return (currMinFrequency - minFrequency) / _diffFrequency;
    }
  }
}
