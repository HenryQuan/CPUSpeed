// Test if presenter is working properly
import 'package:flutter_module/models/cpu_info.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('CPUInfo -', () {
    const mockData1 = {
      'info': '4 x 1.90 GHz\n4 x 2.46 GhZ',
      'min': 300000,
      'max': 2457600,
      'max_curr': 2457600,
      'min_curr': 1237843,
    };

    const mockData2 = {
      'info': '4 x 1.70 GHz\n4 x 2.80 GhZ',
      'min': 300000,
      'max': 2457600,
      'max_curr': 2457600,
      'min_curr': 300000,
    };

    const mockData3 = {
      'info': '4 x 1.90 GHz\n4 x 2.46 GhZ',
      'min': 300000,
      'max': 2457600,
      'max_curr': 2000000,
      'min_curr': 1234230,
    };

    test('default value test', () {
      final info = CPUInfo(null);
      expect(info.hasData, false);
      expect(info.maxPercentage == 0, true);
      expect(info.minPercentage == 0, true);
    });

    test('percentage test', () {
      final info1 = CPUInfo(mockData1);
      expect(info1.calcFrequency(0), 300000);
      expect(info1.calcFrequency(1), 2457600);
      expect(info1.calcFrequency(0.25), (2457600 - 300000) * 0.25 + 300000);

      final info2 = CPUInfo(mockData2);
      expect(info2.calcCurrentPercent(true), 1);
      expect(info2.calcCurrentPercent(false), 0);

      final info3 = CPUInfo(mockData3);
      expect(info3.calcFrequency(0.34), (2457600 - 300000) * 0.34 + 300000);
      expect(info3.calcFrequency(0.78), (2457600 - 300000) * 0.78 + 300000);
      // NOTE: they are double so just make sure it is close enough
      expect(info3.calcCurrentPercent(true) - 0.78, lessThan(0.1));
      expect(info3.calcCurrentPercent(false) - 0.43, lessThan(0.1));
    });

    test('slider changes', () {
      final info3 = CPUInfo(mockData3);
      info3.onChangeSlider(0.1, max: true);
      expect(info3.maxPercentage == 0.1, true);
      expect(info3.minPercentage == 0.1, true);

      info3.onChangeSlider(0.8, max: false);
      expect(info3.maxPercentage == info3.minPercentage, true);

      info3.onChangeSlider(0.82, max: true);
      expect(info3.maxPercentage == 0.82, true);
      expect(info3.minPercentage == 0.8, true);

      info3.onChangeSlider(0.2, max: false);
      expect(info3.maxPercentage == 0.82, true);
      expect(info3.minPercentage == 0.2, true);
    });
  });
}
