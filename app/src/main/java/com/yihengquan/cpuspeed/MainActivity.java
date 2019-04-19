package com.yihengquan.cpuspeed;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private int maxFreqInfo;
    private int minFreqInfo;
    private int freqDiff;
    private int core = Runtime.getRuntime().availableProcessors();

    private int currMaxFreq;
    private int currMinFreq;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Prevent restarting activity
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main);
        } else {
            setContentView(R.layout.activity_main);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!findBinary("su")) {
            Toast.makeText(this, "Device is not rooted", Toast.LENGTH_LONG).show();
        } else {
            // Get cpuinfo and current freq in one go
            String output = getOutputFromShell("cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq && cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq " +
                    "&& cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq && cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
            String[] shell = output.split("\n");
            minFreqInfo = Integer.parseInt(shell[0]);
            maxFreqInfo = Integer.parseInt(shell[1]);

            currMinFreq = Integer.parseInt(shell[2]);
            currMaxFreq = Integer.parseInt(shell[3]);
            freqDiff = maxFreqInfo - minFreqInfo;
            Toast.makeText(this, String.format(Locale.ENGLISH,"%d MHz - %d MHz", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show();

            TextView minValue = findViewById(R.id.minFreqValue);
            minValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMinFreq));
            TextView maxValue = findViewById(R.id.maxFreqValue);
            maxValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMaxFreq));

            // Update freq when seek bar changed
            SeekBar maxFreq = findViewById(R.id.maxFreq);
            SeekBar minFreq = findViewById(R.id.minFreq);

            // Update progress (remember to x100 first)
            maxFreq.setProgress((currMaxFreq - minFreqInfo) * 100 / freqDiff);
            minFreq.setProgress((currMinFreq - minFreqInfo)  * 100 / freqDiff);

            maxFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    TextView maxValue = findViewById(R.id.maxFreqValue);
                    currMaxFreq = minFreqInfo + freqDiff * progress / 100;
                    // Max has to be greater than or equal to min
                    if (currMaxFreq < currMinFreq) currMaxFreq = currMinFreq;
                    maxValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMaxFreq));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });

            minFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    TextView minValue = findViewById(R.id.minFreqValue);
                    currMinFreq = minFreqInfo + freqDiff * progress / 100;
                    minValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMinFreq));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }
    }

    /**
     * Get output by running command
     * @param command
     * @return
     */
    private static String getOutputFromShell(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
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

            return output.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return  "";
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
     * Button clicked
     * @param view
     */
    public void setSpeed(View view) {
        setCPUSpeed(currMaxFreq, currMinFreq, core);
    }

    /**
     * Set CPU frequency
     * @param maxSpeed
     * @param minSpeed
     * @param core
     */
    private void setCPUSpeed(int maxSpeed, int minSpeed, int core) {
        // Get a list for commands
        ArrayList<String> commands = new ArrayList<>();
        for (int i = 0; i < core; i++) {
            // Min
            String path = String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_min_freq", i);
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s", path, minSpeed, path, path));

            // Max
            path = String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq", i);
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s", path, maxSpeed, path, path));
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

            Toast.makeText(this, String.format(Locale.ENGLISH,"Success", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, String.format(Locale.ENGLISH,"Something went wrong", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show();
        }
    }
}
