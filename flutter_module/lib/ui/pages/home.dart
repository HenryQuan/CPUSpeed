import 'package:flutter/material.dart';
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
  final double frequency = 10000000;

  double min = 0;
  double max = 0;

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
                onPressed: () {},
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
                        onChanged: (value) {
                          setState(() {
                            max = value;
                          });
                        },
                      ),
                    ),
                    Text('${(frequency * max).round()} MHz'),
                  ],
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text('Min'),
                    Expanded(
                      child: Slider(
                        value: min,
                        onChanged: (value) {
                          setState(() {
                            min = value;
                          });
                        },
                      ),
                    ),
                    Text('${(frequency * min).round()} MHz'),
                  ],
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 16.0),
                  child: Text('4 x 1.90 GHz'),
                ),
                Text('4 x 2.46 GHz'),
              ],
            ),
          ],
        ),
      ),
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
        _simpleChannel.showAbout();
        // _showAboutDialog();
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
              onPressed: () {},
              child: Text('PRIVACY POLICY'),
            ),
            TextButton(
              onPressed: () {},
              child: Text('GITHUB'),
            ),
          ],
        );
      },
    );
  }
}
