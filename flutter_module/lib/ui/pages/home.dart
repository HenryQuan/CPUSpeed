import 'package:flutter/material.dart';
import 'package:flutter_module/controllers/home_controller.dart';
import 'package:flutter_module/core/app_settings.dart';
import 'package:flutter_module/core/constants.dart';
import 'package:flutter_module/models/cpu_info.dart';
import 'package:flutter_module/services/cpu_channel.dart';
import 'package:flutter_module/services/simple_channel.dart';
import 'package:flutter_module/ui/widgets/slider_row.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late final controller = HomeController();

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
              return controller.options.map((String choice) {
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
                  SliderRow(
                    title: 'Max',
                    freq: controller.currMaxFreq,
                    percent: controller.maxPercent,
                    onChange: _onChangeMaxSlider,
                  ),
                  SliderRow(
                    title: 'Min',
                    freq: controller.currMinFreq,
                    percent: controller.minPercent,
                    onChange: _onChangeMinSlider,
                  ),
                  Padding(
                    padding: const EdgeInsets.only(top: 16.0),
                    child: Text(controller.cpuInfo),
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
}
