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

        if (findBinary("su")) {
            // Empty screen for non-rooted devices
            Toast.makeText(this, "Device is not rooted", Toast.LENGTH_LONG).show();
        } else {
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

                String[] shell = output.toString().split("\n");
                minFreqInfo = Integer.parseInt(shell[0]);
                maxFreqInfo = Integer.parseInt(shell[1]);
                Toast.makeText(this, String.format(Locale.ENGLISH,"Max: %d\nMin: %d", maxFreqInfo, minFreqInfo), Toast.LENGTH_SHORT).show();

                // Update freq when seek bar changed
                SeekBar maxFreq = findViewById(R.id.maxFreq);
                maxFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                        Toast.makeText(getApplicationContext(), "Hello" ,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) { }
                });

                SeekBar minFreq = findViewById(R.id.minFreq);
                minFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                        Toast.makeText(getApplicationContext(), "Hello",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) { }
                });
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    /**
     * Find binary from https://stackoverflow.com/questions/19288463/how-to-check-if-android-phone-is-rooted#19289543
     * @param binaryName
     * @return
     */
    private static boolean findBinary(String binaryName) {
        boolean found = false;
        String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String where : places) {
            if ( new File( where + binaryName ).exists() ) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Set CPU frequency
     * @param speed
     * @param core
     */
    private void setCPUSpeed(int speed, int core) {
        // Get a list for commands
        ArrayList<String> commands = new ArrayList<>();
        for (int i = 0; i < core; i++) {
            String path = String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq", i);
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s", path, speed, path, path));
        }

        try {
            // Try to get root and run the script
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
