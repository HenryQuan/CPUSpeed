import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/services.dart';
import 'package:flutter_module/core/color.dart';
import 'package:flutter_module/ui/pages/home.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  runApp(CPUSpeed());
}

class CPUSpeed extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    _watchBrightnessChange();

    return MaterialApp(
      title: 'CPUSpeed',
      theme: ThemeData(
        primarySwatch: pink,
        primaryColor: green,
        appBarTheme: AppBarTheme(
          brightness: Brightness.dark,
        ),
      ),
      darkTheme: ThemeData(
        primarySwatch: pink,
        accentColor: pink,
        brightness: Brightness.dark,
      ),
      home: HomePage(),
    );
  }

  void _watchBrightnessChange() {
    final window = WidgetsBinding.instance?.window;
    // This listens to platform change
    window?.onPlatformBrightnessChanged = () {
      final useDark = window.platformBrightness == Brightness.dark;
      // Setup navigation bar colour
      SystemChrome.setSystemUIOverlayStyle(
        SystemUiOverlayStyle(
          systemNavigationBarColor:
              useDark ? Colors.grey[900] : Colors.grey[50],
          systemNavigationBarIconBrightness:
              useDark ? Brightness.light : Brightness.dark,
        ),
      );
    };
  }
}
