import 'package:flutter/material.dart';
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

  double min = 0;
  double max = 0;
  int minFreq = 0;
  int maxFreq = 0;
  String cpuInfo = "";

  @override
  void initState() {
    super.initState();

    _cpuChannel.setup().then((_) {
      _cpuChannel.getCPUInfo().then((json) {
        if (json != null) {
          setState(() {
            cpuInfo = json['info'] as String;
            minFreq = json['min'] as int;
            maxFreq = json['max'] as int;
          });
        }
      });
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
        child: Stack(
          children: [
            Align(
              alignment: Alignment.bottomCenter,
              child: FloatingActionButton(
                child: Icon(Icons.save),
                onPressed: _onUpdateSpeed,
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text('Max'),
                    Expanded(
                      child: Slider(
                        value: max,
                        onChanged: _onChangeMaxSlider,
                      ),
                    ),
                    Text('${(maxFreq * max).round()} MHz'),
                  ],
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text('Min'),
                    Expanded(
                      child: Slider(
                        value: min,
                        onChanged: _onChangeMinSlider,
                      ),
                    ),
                    Text('${(maxFreq * min).round()} MHz'),
                  ],
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 16.0),
                  child: Text(cpuInfo),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _onChangeMinSlider(double value) {
    setState(() {
      min = value;
    });
  }

  void _onChangeMaxSlider(double value) {
    setState(() {
      max = value;
    });
  }

  void _onUpdateSpeed() {
    _cpuChannel.setSpeed(
      (min * maxFreq).toInt(),
      (max * maxFreq).toInt(),
    );
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
          content: Text(
            'It aims to help you set CPUSpeed easily for rooted android devices. Please visit my Github repository for more info.',
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (BuildContext context) => LicensePage(
                      applicationVersion: APP_VERSION,
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
