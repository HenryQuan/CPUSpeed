import 'package:flutter/material.dart';
import 'package:flutter_module/presenters/home_presenter.dart';
import 'package:flutter_module/ui/widgets/slider_row.dart';
import 'package:provider/provider.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late final _presenter = HomePresenter(context);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance?.addPostFrameCallback((timeStamp) async {
      await _presenter.showDialogIfNeeded();
      _presenter.loadCPUInfo();
    });
  }

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider.value(
      value: _presenter,
      builder: (context, widget) {
        return Scaffold(
          appBar: AppBar(
            title: Text('CPUSpeed'),
            actions: [
              PopupMenuButton<String>(
                onSelected: _presenter.onSelectPopupMenu,
                itemBuilder: (BuildContext context) {
                  return _presenter.options.map((String choice) {
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
                  child: Consumer<HomePresenter>(
                    builder: (context, presenter, widget) {
                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          SliderRow(
                            title: 'Max',
                            freq: presenter.currMaxFreq,
                            percent: presenter.maxPercent,
                            onChange: presenter.onChangeMaxSlider,
                          ),
                          SliderRow(
                            title: 'Min',
                            freq: presenter.currMinFreq,
                            percent: presenter.minPercent,
                            onChange: presenter.onChangeMinSlider,
                          ),
                          Padding(
                            padding: const EdgeInsets.only(top: 16.0),
                            child: Text(presenter.cpuInfo),
                          ),
                        ],
                      );
                    },
                  ),
                ),
                FloatingActionButton(
                  child: Icon(Icons.save),
                  onPressed: _presenter.onUpdateSpeed,
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
