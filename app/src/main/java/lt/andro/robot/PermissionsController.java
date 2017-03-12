package lt.andro.robot;

public interface PermissionsController {
    boolean hasPermissionGranted(String permission);

    void requestPermission(String permission);
}