package com.goodix.ble.libuihelper.misc;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.goodix.ble.libcomx.logger.Logger;
import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libuihelper.logger.Log;

import java.util.HashSet;

public class PermissionTask extends Task {

    private PermissionFragment fragment;
    private FragmentActivity hostActivity;
    private final int REQUEST_CODE;
    private HashSet<String> permissionSet = new HashSet<>();
    private int timeout = 120_000;


    public PermissionTask(FragmentActivity hostActivity) {
        this.hostActivity = hostActivity;
        this.REQUEST_CODE = hashCode() & 0xFFFF;
    }

    public PermissionTask addPermission(String permission) {
        this.permissionSet.add(permission);
        return this;
    }

    public PermissionTask setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public boolean isGranted() {
        if (hostActivity != null) {
            for (String p : permissionSet) {
                boolean granted = ContextCompat.checkSelfPermission(hostActivity, p) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * check if the permission has not been granted to the given package without instantiating a task.
     */
    public static boolean isGranted(Context ctx, String permission) {
        if (ctx != null) {
            return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    @Override
    protected int doWork() {
        if (hostActivity == null) {
            finishedWithError("hostActivity is null.");
            return 0;
        }

        if (permissionSet.isEmpty()) {
            finishedWithDone();
            return 0;
        }

        if (fragment == null) {
            fragment = new PermissionFragment();
            fragment.task = this;
        }
        hostActivity.getSupportFragmentManager().beginTransaction()
                .add(fragment, "PermissionFragment")
                .commit();

        return timeout;
    }

    @Override
    protected void onCleanup() {
        super.onCleanup();
        if (hostActivity != null && fragment != null) {
            if (fragment.isAdded()) {
                hostActivity.getSupportFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
            }
        }
    }

    public static class PermissionFragment extends Fragment {

        PermissionTask task;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);

            if (task == null) {
                FragmentManager fm = getFragmentManager();
                if (fm != null) {
                    fm.beginTransaction().remove(this).commit();
                }
                Log.e(PermissionTask.class.getSimpleName(), "task is null.");
                return;
            }

            Logger.v(task.logger, task.getName(), "PermissionFragment#onAttach()");

            String[] permissions = new String[task.permissionSet.size()];
            int i = 0;
            for (String p : task.permissionSet) {
                permissions[i++] = p;
            }
            requestPermissions(permissions, task.REQUEST_CODE);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (task != null) {
                Logger.v(task.logger, task.getName(), "PermissionFragment#onDetach()");
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (task == null || requestCode != task.REQUEST_CODE || !task.isStarted()) {
                return;
            }

            Logger.v(task.logger, task.getName(), "PermissionFragment#onRequestPermissionsResult()");

            HashSet<String> requested = new HashSet<>(task.permissionSet);
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    requested.remove(permissions[i]);
                }
            }
            if (requested.isEmpty()) {
                task.finishedWithDone();
            } else {
                StringBuilder sb = new StringBuilder(32 + requested.size() * 64);
                sb.append("Permissions not granted: ");
                for (String s : requested) {
                    sb.append(s).append(", ");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.deleteCharAt(sb.length() - 1);
                task.finishedWithError(sb.toString());
            }
        }
    }
}
