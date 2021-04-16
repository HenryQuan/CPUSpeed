import 'package:flutter/material.dart';

class SliderRow extends StatelessWidget {
  const SliderRow({
    Key? key,
    required this.title,
    required this.freq,
    required this.percent,
    required this.onChange,
  }) : super(key: key);

  final String title;
  final int freq;
  final double percent;
  final void Function(double) onChange;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(title),
        Expanded(
          child: Slider(
            value: percent,
            onChanged: onChange,
          ),
        ),
        Text('$freq KHz'),
      ],
    );
  }
}
