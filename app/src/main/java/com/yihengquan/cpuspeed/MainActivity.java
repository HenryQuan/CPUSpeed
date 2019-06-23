package com.yihengquan.cpuspeed;

import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.ads.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private int maxFreqInfo = 0;
    private int minFreqInfo = 0;
    private int freqDiff;
    private int core = Runtime.getRuntime().availableProcessors();

    private int currMaxFreq = 0;
    private int currMinFreq = 0;

    private final String appVersion = "1.0.4";

    // banner
    private AdView banner;
    private Menu menu;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater m = getMenuInflater();
        m.inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    protected void onDestroy() {
        if (banner != null) banner.destroy();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        // Show toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (!showWelcomeDialog()) {
            showWhatsNewDialog();
        }

        if (!findBinary("su")) {
            Toast.makeText(this, "Device is not rooted", Toast.LENGTH_LONG).show();
        } else {
            // Make sure cpu folders have the right permission
            try {
                Process p = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                os.writeBytes("chmod 755 /sys/devices/system/cpu/cpu*");
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String output = getOutputFromShell("su -c cat /sys/devices/system/cpu/cpu*/cpufreq/*m*_freq");

            if(output != null && !output.isEmpty()) {
                // Update freq when seek bar changed
                final SeekBar maxFreq = findViewById(R.id.maxFreq);
                final SeekBar minFreq = findViewById(R.id.minFreq);

                // Catch errors...
                try {
                    String[] shell = output.split("\n");
                    //System.out.println(output);
                    // Store cpu info
                    HashMap<String, Integer> speedInfo = new HashMap<>();

                    // Find max values
                    for (int i = 0; i < this.core; i++) {
                        // First and third values
                        String maxInfoStr = shell[i * 4];
                        int maxInfo = Integer.parseInt(maxInfoStr);
                        int maxCurr = Integer.parseInt(shell[i * 4 + 2]);
                        int minCurr = Integer.parseInt(shell[i * 4 + 3]);

                        if (maxInfo > maxFreqInfo) maxFreqInfo = maxInfo;
                        if (maxCurr > currMaxFreq) currMaxFreq = maxCurr;
                        if (minCurr > currMinFreq) currMinFreq = minCurr;

                        // Store max info
                        Integer count = speedInfo.get(maxInfoStr);
                        if (count == null) count = 0;
                        speedInfo.put(maxInfoStr, count + 1);
                    }

                    // Min and curr min are current, only max values could differ
                    minFreqInfo = Integer.parseInt(shell[1]);
                    freqDiff = maxFreqInfo - minFreqInfo;

                    Toast.makeText(this, String.format(Locale.ENGLISH,"%d MHz - %d MHz", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show();

                    TextView minValue = findViewById(R.id.minFreqValue);
                    minValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMinFreq));
                    TextView maxValue = findViewById(R.id.maxFreqValue);
                    maxValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMaxFreq));

                    // Set cpuInfo text
                    String infoStr = "";
                    for (String key : speedInfo.keySet()) {
                        int count = speedInfo.get(key);
                        float speed = Float.parseFloat(key) / 1000000;
                        infoStr += String.format("%d x %.2f GHz\n", count, speed);
                    }
                    System.out.println(infoStr);

                    final TextView info = findViewById(R.id.cpuInfo);
                    info.setText(infoStr);

                    // Update progress (remember to x100 first)
                    maxFreq.setProgress((currMaxFreq - minFreqInfo) * 100 / freqDiff);
                    minFreq.setProgress((currMinFreq - minFreqInfo)  * 100 / freqDiff);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final TextView maxValue = findViewById(R.id.maxFreqValue);
                final TextView minValue = findViewById(R.id.minFreqValue);

                maxFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        currMaxFreq = minFreqInfo + freqDiff * progress / 100;
                        // Max has to be greater than or equal to min
                        if (currMaxFreq < currMinFreq) {
                            // Also move minFreq
                            minFreq.setProgress(progress);
                            minValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMaxFreq));
                        }
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
                        currMinFreq = minFreqInfo + freqDiff * progress / 100;
                        // Min has to be less than or equal to max
                        if (currMinFreq > currMaxFreq) {
                            // Move maxFreq
                            maxFreq.setProgress(progress);
                            maxValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMaxFreq));
                        }
                        minValue.setText(String.format(Locale.ENGLISH,"%d MHz", currMinFreq));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) { }
                });
            } else {
                Toast.makeText(this, "Please contact me for more info", Toast.LENGTH_LONG).show();
            }
        }

        // Set ads state
        boolean showAds = getAds();
        MenuItem ads = this.menu.findItem(R.id.menu_ads);
        ads.setChecked(showAds);

        // Setup banner
        if (showAds) {
            banner = new AdView(this, "889645368038871_889649694705105", AdSize.BANNER_HEIGHT_50);
            banner.loadAd();
            LinearLayout adBanner = findViewById(R.id.ads_banner);
            adBanner.addView(banner);
        }
    }

    /**
     * Show github page when pressed
     * @param item
     */
    public void showAbout(MenuItem item) {
        new AlertDialog.Builder(this)
            .setTitle("CPUSpeed")
            .setMessage("It aims to help you set CPUSpeed easily for rooted android devices. Please visit my Github repository for more info and support me on Patreon.")

            // Specifying a listener allows you to take an action before dismissing the dialog.
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton("Github", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openLink("https://github.com/HenryQuan/CPUSpeed");
                }
            })
            .setCancelable(true)
            .show();
    }

    /**
     * Share app with a popup
     * @param item
     */
    public void shareApp(MenuItem item) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,"https://play.google.com/store/apps/details?id=com.yihengquan.cpuspeed");
        startActivity(Intent.createChooser(share, "Share CPUSpeed"));
    }

    /**
     * Show or hide ads
     * @param item
     */
    public void toggleAds(MenuItem item) {
        MenuItem ads = findViewById(R.id.menu_ads);
        boolean newState = !ads.isChecked();
        ads.setChecked(newState);
        this.setAds(newState);
    }

    /**
     * Get current state of ads
     * @return whether show or hide ads
     */
    private boolean getAds() {
        SharedPreferences pref = getSharedPreferences("CPUSpeed", MODE_PRIVATE);
        boolean ADS = pref.getBoolean("ADS", true);
        return ADS;
    }

    /**
     * Set state for ads
     * @param state true of false
     */
    private void setAds(final Boolean state) {
        SharedPreferences pref = getSharedPreferences("CPUSpeed", MODE_PRIVATE);
        final SharedPreferences.Editor editor = pref.edit();
        if (!state) {
            final MenuItem ads = this.menu.findItem(R.id.menu_ads);
            new AlertDialog.Builder(this)
                .setTitle("Support CPUSpeed")
                .setPositiveButton("I want to support developer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // update state
                        editor.putBoolean("ADS", true);
                        editor.apply();
                        ads.setChecked(true);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // update state
                        editor.putBoolean("ADS", state);
                        editor.apply();
                    }
                })
                .setCancelable(false)
                .show();
        } else {
            // update state
            editor.putBoolean("ADS", state);
            editor.apply();
        }
    }


    /**
     * Email me with feed back
     * @param item
     */
    public void emailMe(MenuItem item) {
        this.openLink(String.format("mailto:development.henryquan@gmail.com?subject=[CPUSpeed %s] ", appVersion));
    }

    /**
     * Visit my patreon page
     * @param item
     */
    public void gotoPatreon(MenuItem item) {
        this.openLink("https://www.patreon.com/henryquan");
    }

    /**
     * Open certain links
     * @param url
     */
    private void openLink(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show a simple dialog to say hello and some warnings
     */
    private boolean showWelcomeDialog() {
        SharedPreferences pref = getSharedPreferences("CPUSpeed", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        boolean first = pref.getBoolean("Welcome", true);

        if (first) {
            new AlertDialog.Builder(this)
                .setTitle("CPUSpeed")
                .setMessage("Thank you for downloading this app.\n\nPlease note that if you underclock your device, it might freeze or even shutdown in the worst case. If you overclock your device, it might become warm and battery will run out quickly.")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
            // Set to false
            editor.putBoolean("Welcome", false);
            editor.apply();
        }

        return first;
    }

    /**
     * Show what's new in this version
     */
    private void showWhatsNewDialog() {
        SharedPreferences pref = getSharedPreferences("CPUSpeed", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        boolean whatsnew = pref.getBoolean(appVersion, true);

        if (whatsnew) {
            new AlertDialog.Builder(this)
                .setTitle(String.format("Version %s", appVersion))
                .setMessage("- Added feedback button\n- Added ads\n\nThank you for 1000 downloads!")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
            // Set to false
            editor.putBoolean(appVersion, false);
            editor.apply();
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
        if (maxFreqInfo == 0 || minFreqInfo == 0) {
            Toast.makeText(this, String.format(Locale.ENGLISH,"Error: unknown clock speed", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show();
        } else {
            setCPUSpeed(currMaxFreq, currMinFreq, core);
        }
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
        int i;
        for (i = 0; i < core; i++) {
            // Max
            String path = String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq", i);
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s", path, maxSpeed, path, path));

            // Min
            path = String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_min_freq", i);
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s", path, minSpeed, path, path));
        }


        for (i = 0; i < core; i++) {
            String path = "/sys/module/msm_performance/parameters/cpu_max_freq";
            // Max
            maxSpeed = 3000000;
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho '%d:%d' > %s\nchmod 444 %s", path, i, maxSpeed, path, path));
        }

        for (i = 0; i < core; i++) {
            String path = "/sys/module/msm_performance/parameters/cpu_min_freq";
            // Min
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho '%d:%d' > %s\nchmod 444 %s", path, i, minSpeed, path, path));
        }

        // System.out.println(commands.toString());

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
