package com.example.myapo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent  = getIntent();
        String errMsg  = intent != null ? intent.getStringExtra("error") : "Unknown error.";
        if (errMsg == null) errMsg = "Unknown error.";

        final String display = errMsg;
        new MaterialAlertDialogBuilder(this)
            .setTitle("App Error")
            .setMessage(display)
            .setCancelable(false)
            .setPositiveButton("Close App", (d, w) -> {
                Process.killProcess(Process.myPid());
                System.exit(1);
            })
            .setNegativeButton("Restart", (d, w) -> {
                Intent restart = new Intent(this, MainActivity.class);
                restart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(restart);
                finish();
            })
            .show();
    }
}
