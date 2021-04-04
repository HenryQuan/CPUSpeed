import 'package:flutter/material.dart';

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

  double min = 0;
  double max = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('CPUSpeed'),
        actions: [
          PopupMenuButton<String>(
            onSelected: onSelectPopupMenu,
            itemBuilder: (BuildContext context) {
              return options.map((String choice) {
                return PopupMenuItem<String>(
                  value: choice,
                  child: Text(choice),
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
                    Text('8888888 MHz'),
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
                    Text('8888888 MHz'),
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

  void onSelectPopupMenu(String value) {}
}
