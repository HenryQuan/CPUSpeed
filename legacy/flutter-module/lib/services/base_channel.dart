import 'package:flutter/services.dart';

abstract class BaseMethodChannel {
  abstract String name;
  static const _channel = 'cpuspeed.flutter/';
  late final channel = MethodChannel('$_channel$name');
}
