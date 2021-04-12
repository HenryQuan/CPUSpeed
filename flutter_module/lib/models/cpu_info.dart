import 'dart:math';

class CPUInfo {
  late int maxFrequency;
  late int minFrequency;
  late int currMaxFrequency;
  late int currMinFrequency;
  late double maxPercentage;
  late double minPercentage;

  /// This is used to calculate current percentage
  late int _diffFrequency;

  /// For example, 4 x 1.90 GHz | 4 x 2.46 GHZ
  late String cpuInfo;
  bool hasData = false;

  CPUInfo(Map<String, Object?>? json) {
    hasData = json != null;

    // Unpack data from map
    cpuInfo = (json?['info'] as String?) ?? 'Unknown';

    maxFrequency = (json?['max'] as int?) ?? 0;
    minFrequency = (json?['min'] as int?) ?? 0;

    currMaxFrequency = (json?['max_curr'] as int?) ?? 0;
    currMinFrequency = (json?['min_curr'] as int?) ?? 0;

    // NOTE: Make sure it is not 0 to prevent zero division
    _diffFrequency = max(maxFrequency - minFrequency, 1);

    maxPercentage = calcCurrentPercent(true);
    minPercentage = calcCurrentPercent(false);
  }

  double calcCurrentPercent(bool max) {
    if (max) {
      return (currMaxFrequency - minFrequency) / _diffFrequency;
    } else {
      return (currMinFrequency - minFrequency) / _diffFrequency;
    }
  }

  int calcFrequency(double percent) {
    final offset = (_diffFrequency * percent).toInt();
    return offset + minFrequency;
  }

  /// Make sure max is always more than min and min is always less than max
  void onChangeSlider(double value, {required bool max}) {
    if (max) {
      maxPercentage = value;
      currMaxFrequency = calcFrequency(value);
      if (currMaxFrequency < currMinFrequency) {
        currMinFrequency = currMaxFrequency;
        minPercentage = maxPercentage;
      }
    } else {
      minPercentage = value;
      currMinFrequency = calcFrequency(value);
      if (currMinFrequency > currMaxFrequency) {
        currMaxFrequency = currMinFrequency;
        maxPercentage = minPercentage;
      }
    }
  }
}
