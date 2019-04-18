package com.yihengquan.cpuspeed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
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
        String input = ((EditText)findViewById(R.id.speedText)).getText().toString();
        int core = Runtime.getRuntime().availableProcessors();
        Toast.makeText(this, "Hello World\n" + input + core, Toast.LENGTH_SHORT).show();
    }

    private void setCPUSpeed(int core) {
        // Get a list for commands
        ArrayList<String> commands;
        for (int i = 0; i < core; i++) {
            commands.add()
        }

        // Try to get root
        try {

            Process p = Runtime.getRuntime().exec("/system/bin/sh -");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String tmpCmd : commands) {
                os.writeBytes(tmpCmd + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
