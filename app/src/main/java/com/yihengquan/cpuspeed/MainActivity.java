package com.yihengquan.cpuspeed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private int maxFreqInfo;
    private int minFreqInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get max and min freq from cpuinfo
        try {
            Process p = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq && cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try{ Thread.sleep(1000, 0); } catch(Exception e) {e.printStackTrace();}
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            p.waitFor();

            Toast.makeText(this, output.toString(), Toast.LENGTH_SHORT).show();

            SeekBar maxFreq = findViewById(R.id.maxFreq);
            maxFreq.setMax(2900000);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
            String path = String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq", i);
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s", path, speed, path, path));
        }

        // Try to get root and run the script
        try {
            String[] c = commands.toArray(new String[0]);
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String cmd : c) {
                os.writeBytes(cmd + "\n");
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
