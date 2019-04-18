package com.yihengquan.cpuspeed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Try to get root
        try {
            String[] commands = {"su -c 'dumpstate > /sdcard/log1.txt'"};
            Process p = Runtime.getRuntime().exec("/system/bin/sh -");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String tmpCmd : commands) {
                os.writeBytes(tmpCmd+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSpeed(View view) {
        String input = ((EditText)findViewById(R.id.speedText)).getText().toString();

        Toast.makeText(this, "Hello World\n" + input, Toast.LENGTH_SHORT).show();
    }
}
