import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_module/ui/pages/home.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  runApp(CPUSpeed());
}

class CPUSpeed extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'CPUSpeed',
      theme: ThemeData(
        primarySwatch: Colors.green,
        accentColor: Colors.pinkAccent,
      ),
      home: HomePage(),
    );
  }
}
