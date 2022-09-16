package fr.notavone.balance_ton_plot.utils;

import android.view.View;

public class UiChangeListener implements View.OnSystemUiVisibilityChangeListener {
    private final View view;

    private static final int UI_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    public UiChangeListener(View view) {
        this.view = view;
        onSystemUiVisibilityChange(view.getSystemUiVisibility());
    }

    @Override
    public void onSystemUiVisibilityChange(int i) {
        view.setSystemUiVisibility(UI_FLAGS);
    }
}
