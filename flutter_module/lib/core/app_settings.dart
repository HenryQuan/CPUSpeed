import 'package:flutter_module/core/constants.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppSetting {
  static const _firstLaunchKey = 'cpuspeed:firstlaunch';
  static const _versionKey = 'cpuspeed:$APP_VERSION';

  AppSetting._init();

  Future<void> setup() async {
    _pref = await SharedPreferences.getInstance();
  }

  // Singleton
  static AppSetting instance = AppSetting._init();
  late final SharedPreferences _pref;

  // First Launch
  bool? _isFirstLaunch;

  bool get isFirstLaunch {
    return _isFirstLaunch ?? _pref.getBool(_firstLaunchKey) ?? true;
  }

  set isFirstLaunch(bool value) {
    _isFirstLaunch = value;
    _pref.setBool(_firstLaunchKey, value);
  }

  // What's New
  bool? _shouldShowWhatsNew;

  bool get shouldShowWhatsNew {
    return _shouldShowWhatsNew ?? _pref.getBool(_versionKey) ?? true;
  }

  set shouldShowWhatsNew(bool value) {
    _shouldShowWhatsNew = value;
    _pref.setBool(_versionKey, value);
  }
}
