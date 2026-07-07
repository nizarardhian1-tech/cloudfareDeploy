package com.example.myapo.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.google.android.material.snackbar.Snackbar;

public class UiHelper {

    public static void showMessage(Context context, String message) {
        if (context instanceof Activity) {
            View rootView = ((Activity) context).getWindow().getDecorView().getRootView();
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    public static void showLongMessage(Context context, String message) {
        if (context instanceof Activity) {
            View rootView = ((Activity) context).getWindow().getDecorView().getRootView();
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
        }
    }

    public static void showError(Context context, String message) {
        if (context instanceof Activity) {
            View rootView = ((Activity) context).getWindow().getDecorView().getRootView();
            Snackbar snack = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            snack.setBackgroundTint(0xFFBA1A1A);
            snack.setTextColor(0xFFFFFFFF);
            snack.show();
        }
    }

    public static void animateFadeIn(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }

    public static void animateSlideInUp(View view) {
        if (view == null) return;
        view.setTranslationY(60f);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }
}
