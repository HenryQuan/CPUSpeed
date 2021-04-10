import 'package:flutter/material.dart';
import 'package:flutter_module/core/app_settings.dart';
import 'package:flutter_module/core/color.dart';
import 'package:flutter_module/core/constants.dart';
import 'package:flutter_module/services/cpu_channel.dart';
import 'package:flutter_module/services/simple_channel.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final options = [
    'Feedback',
    'Share',
    'About',
  ];

  final _simpleChannel = SimpleMethodChannel();
  final _cpuChannel = CPUMethodChannel();

  double minPercent = 0;
  double maxPercent = 0;
  int minFreq = 0;
  int maxFreq = 0;
  int currMinFreq = 0;
  int currMaxFreq = 0;

  // If min and max freq are unknown, the user shouldn't change it at all
  bool canChange = false;
  String cpuInfo = "";

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance?.addPostFrameCallback((timeStamp) async {
      await _showDialogIfNeeded();
      _loadCPUInfo();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('CPUSpeed'),
        actions: [
          PopupMenuButton<String>(
            onSelected: _onSelectPopupMenu,
            itemBuilder: (BuildContext context) {
              return options.map((String choice) {
                return PopupMenuItem<String>(
                  value: choice,
                  child: Container(
                    // Make the popup wider to feel more native
                    width: 140,
                    child: Text(choice),
                  ),
                );
              }).toList(growable: false);
            },
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('Max'),
                      Expanded(
                        child: Slider(
                          value: maxPercent,
                          onChanged: _onChangeMaxSlider,
                        ),
                      ),
                      Text('$currMaxFreq KHz'),
                    ],
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('Min'),
                      Expanded(
                        child: Slider(
                          value: minPercent,
                          onChanged: _onChangeMinSlider,
                        ),
                      ),
                      Text('$currMinFreq KHz'),
                    ],
                  ),
                  Padding(
                    padding: const EdgeInsets.only(top: 16.0),
                    child: Text(cpuInfo),
                  ),
                ],
              ),
            ),
            FloatingActionButton(
              child: Icon(Icons.save),
              onPressed: _onUpdateSpeed,
            ),
          ],
        ),
      ),
    );
  }

  void _onChangeMinSlider(double value) {
    if (canChange) {
      // Update frequency and make sure max frequency is always more than min
      currMinFreq = _calcCurrFreq(value);
      if (currMinFreq > currMaxFreq) {
        currMaxFreq = currMinFreq;
        maxPercent = minPercent;
      }
      setState(() {
        minPercent = value;
      });
    }
  }

  void _onChangeMaxSlider(double value) {
    if (canChange) {
      // Make sure min if always less than max
      currMaxFreq = _calcCurrFreq(value);
      if (currMaxFreq < currMinFreq) {
        currMinFreq = currMaxFreq;
        minPercent = maxPercent;
      }
      setState(() {
        maxPercent = value;
      });
    }
  }

  int _calcCurrFreq(double percent) {
    final offset = ((maxFreq - minFreq) * percent).toInt();
    return offset + minFreq;
  }

  double _calcCurrPercent(bool max) {
    if (max) {
      return currMaxFreq / maxFreq;
    } else {
      return currMinFreq / maxFreq;
    }
  }

  Future<void> _showDialogIfNeeded() async {
    final settings = AppSetting.instance;
    if (settings.isFirstLaunch) {
      // Show app introduction if not yet
      settings.isFirstLaunch = false;
      showDialog(
        context: context,
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
        context: context,
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

  void _loadCPUInfo() async {
    await _cpuChannel.setup();
    final json = await _cpuChannel.getCPUInfo();
    if (json != null) {
      setState(() {
        canChange = true;
        cpuInfo = json['info'] as String;

        maxFreq = json['max'] as int;
        minFreq = json['min'] as int;

        currMaxFreq = json['max_curr'] as int;
        currMinFreq = json['min_curr'] as int;

        maxPercent = _calcCurrPercent(true);
        minPercent = _calcCurrPercent(false);
      });
    } else {
      setState(() {
        cpuInfo = "Unknown";
      });
    }
  }

  void _onUpdateSpeed() {
    _cpuChannel.setSpeed(currMinFreq, currMaxFreq);
  }

  void _onSelectPopupMenu(String value) {
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

  void _showAboutDialog() {
    showDialog(
      context: context,
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
}
