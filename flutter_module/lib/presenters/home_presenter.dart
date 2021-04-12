import 'package:flutter/material.dart';
import 'package:flutter_module/core/app_settings.dart';
import 'package:flutter_module/core/constants.dart';
import 'package:flutter_module/models/cpu_info.dart';
import 'package:flutter_module/services/cpu_channel.dart';
import 'package:flutter_module/services/simple_channel.dart';

class HomePresenter extends ChangeNotifier {
  final options = [
    'Feedback',
    'Share',
    'About',
  ];

  final _simpleChannel = SimpleMethodChannel();
  final _cpuChannel = CPUMethodChannel();

  late CPUInfo _info;
  double get minPercent => _info.minPercentage;
  double get maxPercent => _info.maxPercentage;
  int get minFreq => _info.minFrequency;
  int get maxFreq => _info.maxFrequency;
  int get currMinFreq => _info.currMinFrequency;
  int get currMaxFreq => _info.currMaxFrequency;

  // If min and max freq are unknown, the user shouldn't change sliders at all
  bool get canChange => _info.hasData;
  String get cpuInfo => _info.cpuInfo;

  final BuildContext _context;
  HomePresenter(this._context);

  // region CPU
  void loadCPUInfo() async {
    await _cpuChannel.setup();
    final json = await _cpuChannel.getCPUInfo();
    _info = CPUInfo(json);
    notifyListeners();
  }

  void onUpdateSpeed() {
    _cpuChannel.setSpeed(currMinFreq, currMaxFreq);
  }
  // endregion

  // region Slider
  void onChangeMaxSlider(double value) {
    if (canChange) {
      _info.onChangeSlider(value, max: true);
      notifyListeners();
    }
  }

  void onChangeMinSlider(double value) {
    if (canChange) {
      _info.onChangeSlider(value, max: false);
      notifyListeners();
    }
  }
  // endregion

  // region Dialog & Menu
  void onSelectPopupMenu(String value) {
    switch (value) {
      case 'Feedback':
        _simpleChannel.sendFeedback();
        break;
      case 'Share':
        _simpleChannel.shareApp();
        break;
      case 'About':
        _showAboutDialog();
        break;
    }
  }

  Future<void> showDialogIfNeeded() async {
    final settings = AppSetting.instance;
    if (settings.isFirstLaunch) {
      // Show app introduction if not yet
      settings.isFirstLaunch = false;
      showDialog(
        context: _context,
        builder: (context) {
          return AlertDialog(
            title: Text('CPUSpeed'),
            content: Text(APP_INTRODUCTION),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: Text('Close'),
              ),
            ],
          );
        },
      );
    } else if (settings.shouldShowWhatsNew) {
      // Show what's new if haven't
      settings.shouldShowWhatsNew = false;
      showDialog(
        context: _context,
        builder: (context) {
          return AlertDialog(
            title: Text('Version $APP_VERSION'),
            content: Text(WHATS_NEW),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: Text('Close'),
              ),
            ],
          );
        },
      );
    }
  }

  void _showAboutDialog() {
    showDialog(
      context: _context,
      builder: (context) {
        return AlertDialog(
          title: Text('CPUSpeed'),
          content: Text(APP_ABOUT),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (BuildContext context) => LicensePage(
                      applicationVersion: APP_VERSION,
                      applicationLegalese: APP_SHORT_DESCRIPTION,
                    ),
                  ),
                );
              },
              child: Text('LICENSES'),
            ),
            TextButton(
              onPressed: () => _simpleChannel.openUrl(PRIVACY_POLICY_URL),
              child: Text('PRIVACY POLICY'),
            ),
            TextButton(
              onPressed: () => _simpleChannel.openUrl(GITHUB_REPO_URL),
              child: Text('GITHUB'),
            ),
          ],
        );
      },
    );
  }
  // endregion
}
