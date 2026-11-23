package com.simhelp.phonecontrol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.TelecomManager;

import java.lang.reflect.Method;

final class DialerRoleCompat {
    private static final String ROLE_MANAGER_CLASS = "android.app.role.RoleManager";
    private static final String ROLE_DIALER = "android.app.role.DIALER";

    private DialerRoleCompat() {}

    static boolean canRequestDefaultDialer(Context context) {
        TelecomManager telecom = context.getSystemService(TelecomManager.class);
        if (telecom != null) return true;
        return hasRoleApi() && isRoleAvailable(context);
    }

    static boolean isDefaultDialer(Context context) {
        if (hasRoleApi()) {
            Boolean held = isRoleHeld(context);
            if (held != null) return held;
        }
        TelecomManager telecom = context.getSystemService(TelecomManager.class);
        if (telecom == null) return false;
        String current = telecom.getDefaultDialerPackage();
        return context.getPackageName().equals(current);
    }

    static boolean requestDefaultDialer(Activity activity, int requestCode) {
        if (hasRoleApi() && isRoleAvailable(activity)) {
            Intent roleIntent = createRoleRequestIntent(activity);
            if (roleIntent != null) {
                activity.startActivityForResult(roleIntent, requestCode);
                return true;
            }
        }
        TelecomManager telecom = activity.getSystemService(TelecomManager.class);
        if (telecom == null) return false;
        Intent legacyIntent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
        legacyIntent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                activity.getPackageName());
        activity.startActivityForResult(legacyIntent, requestCode);
        return true;
    }

    private static boolean hasRoleApi() {
        return Build.VERSION.SDK_INT >= 29;
    }

    private static boolean isRoleAvailable(Context context) {
        try {
            Class<?> roleManagerClass = Class.forName(ROLE_MANAGER_CLASS);
            Object roleManager = context.getSystemService(roleManagerClass);
            if (roleManager == null) return false;
            Method isRoleAvailable = roleManagerClass.getMethod("isRoleAvailable", String.class);
            Object value = isRoleAvailable.invoke(roleManager, ROLE_DIALER);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException error) {
            BridgeLog.error("dialer_role_available_check_failed", error);
            return false;
        }
    }

    private static Boolean isRoleHeld(Context context) {
        try {
            Class<?> roleManagerClass = Class.forName(ROLE_MANAGER_CLASS);
            Object roleManager = context.getSystemService(roleManagerClass);
            if (roleManager == null) return null;
            Method isRoleHeld = roleManagerClass.getMethod("isRoleHeld", String.class);
            Object value = isRoleHeld.invoke(roleManager, ROLE_DIALER);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException error) {
            BridgeLog.error("dialer_role_held_check_failed", error);
            return null;
        }
    }

    private static Intent createRoleRequestIntent(Context context) {
        try {
            Class<?> roleManagerClass = Class.forName(ROLE_MANAGER_CLASS);
            Object roleManager = context.getSystemService(roleManagerClass);
            if (roleManager == null) return null;
            Method createRequestRoleIntent = roleManagerClass.getMethod("createRequestRoleIntent",
                    String.class);
            Object value = createRequestRoleIntent.invoke(roleManager, ROLE_DIALER);
            return value instanceof Intent ? (Intent) value : null;
        } catch (ReflectiveOperationException error) {
            BridgeLog.error("dialer_role_request_intent_failed", error);
            return null;
        }
    }
}
