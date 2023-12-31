package com.waquwex.wordgame.Utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class PixelUtils {
    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float density = displayMetrics.density;
        return Math.round(px / density);
    }

    // Additional method for converting dp to pixels
    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float density = displayMetrics.density;
        return Math.round(dp * density);
    }
}