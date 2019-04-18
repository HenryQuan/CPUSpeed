package com.yihengquan.cpuspeed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void setSpeed(View view) {
        EditText inputBox = findViewById(R.id.speedText);
        String input = inputBox.getText().toString();
        // Hide virtual keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(inputBox.getWindowToken(), 0);

        try {
            // If input is a number
            int speed = Integer.parseInt(input);
            int core = Runtime.getRuntime().availableProcessors();
            setCPUSpeed(speed, core);
        } catch (NumberFormatException e) {
            // Not valid input
            Toast.makeText(this, "Input is not valid", Toast.LENGTH_SHORT).show();
        }
    }

    private void setCPUSpeed(int speed, int core) {
        // Get a list for commands
        ArrayList<String> commands = new ArrayList<>();
        for (int i = 0; i < core; i++) {
            String curr = String.format("su -c 'echo %d > /sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq'", speed, i);
            commands.add(curr);
        }

        // Try to get root and run the script
        try {
            String[] c = commands.toArray(new String[0]);
            Process p = Runtime.getRuntime().exec("/system/bin/sh -");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String cmd : c) {
                os.writeBytes(cmd + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
