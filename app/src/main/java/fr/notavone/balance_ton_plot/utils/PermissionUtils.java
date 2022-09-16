package fr.notavone.balance_ton_plot.utils;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.logging.Logger;

public class PermissionUtils implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int PERMISSION_UTILS_REQUEST_CODE = 69;

    private final Logger logger = Logger.getLogger(PermissionUtils.class.getName());
    private final Activity activity;

    public PermissionUtils(Activity activity) {
        this.activity = activity;
    }

    public boolean hasPermission(String... permission) {
        for (String perm : permission) {
            if (activity.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void requestPermission(String... permission) {
        activity.requestPermissions(permission, PERMISSION_UTILS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_UTILS_REQUEST_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    logger.info("Permission " + permissions[i] + " granted");
                } else {
                    logger.info("Permission " + permissions[i] + " denied");
                }
            }
        }
    }
}
