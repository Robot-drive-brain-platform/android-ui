package lt.andro.robot;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import hugo.weaving.DebugLog;

public class PermissionsControllerImpl implements PermissionsController {
    private static final int PERMISSIONS_CONTROLLER_REQUEST = 23452;
    private Activity activity;
    private MainActivityView view;

    public PermissionsControllerImpl(Activity activity, MainActivityView view) {
        this.activity = activity;
        this.view = view;
    }

    @DebugLog
    @Override
    public boolean hasPermissionGranted(@NonNull String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @DebugLog
    @Override
    public void requestPermission(String permission) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {

            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            showPermissionExplanationMessage();

        } else {

            // No explanation needed, we can request the permission.

            requestPermissionGrant(permission);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    @DebugLog
    private void showPermissionExplanationMessage() {
        view.showMessage("Permission is required.");
    }

    @DebugLog
    private void requestPermissionGrant(String permission) {
        ActivityCompat.requestPermissions(activity, new String[]{permission}, PERMISSIONS_CONTROLLER_REQUEST);
    }

    @Override
    public String toString() {
        return "PermissionsControllerImpl{" +
                "activity=" + activity +
                '}';
    }
}