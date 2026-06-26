package com.example.myapo.utils;

import android.content.Context;
import android.widget.Toast;

public class UiHelper {

    // Tampilkan Toast pendek
    public static void showMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    // Tampilkan Toast panjang
    public static void showLongMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // Tampilkan Toast error dengan icon (opsional)
    public static void showError(Context context, String message) {
        Toast.makeText(context, "⚠️ " + message, Toast.LENGTH_LONG).show();
    }
}