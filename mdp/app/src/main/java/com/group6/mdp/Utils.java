package com.group6.mdp;

import android.content.Context;
import android.widget.Toast;

public final class Utils {
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
