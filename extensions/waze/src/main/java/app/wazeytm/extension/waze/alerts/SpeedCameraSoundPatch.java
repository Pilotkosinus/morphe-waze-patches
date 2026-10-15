package app.wazeytm.extension.waze.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Method;

import app.wazeytm.extension.waze.settings.MorpheSettings;

/**
 * Waze shows speed camera alerts on screen, but plays the alert sound only when driving
 * above the speed limit. This is controlled by the configuration value
 * {@code CONFIG_VALUE_ALERTS_PLAY_SPEED_CAMERA_SOUND_BELOW_SPEED_LIMIT} (default false),
 * which the native alerting code of Waze reads.
 * <p>
 * If enabled, this class sets the value to true. Waze can overwrite configuration values
 * when it syncs with its server, so the value is applied again after every sync.
 * The original value is remembered and restored when the feature is turned off.
 */
@SuppressWarnings("unused")
public final class SpeedCameraSoundPatch {
    private static final String TAG = "MorpheWazeSpeedCam";

    private static final String CONFIG_FIELD = "CONFIG_VALUE_ALERTS_PLAY_SPEED_CAMERA_SOUND_BELOW_SPEED_LIMIT";
    /**
     * Value of the configuration before it was changed by this patch.
     */
    private static final String ORIGINAL_VALUE_KEY = "speed_camera_sound_below_limit_original";

    private SpeedCameraSoundPatch() {
    }

    /**
     * Injection point. Called when Waze finished syncing its configuration.
     */
    public static void onConfigSynced() {
        apply();
    }

    /**
     * Applies the current setting. Also called when the switch in the settings is changed.
     */
    public static synchronized void apply() {
        try {
            Context context = MorpheSettings.getContext();
            if (context == null) return;
            SharedPreferences preferences = context.getSharedPreferences(
                    MorpheSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);

            Object config = Class.forName("com.waze.config.ConfigValues").getField(CONFIG_FIELD).get(null);
            Class<?> managerClass = Class.forName("com.waze.ConfigManager");
            Object manager = managerClass.getMethod("getInstance").invoke(null);
            Method getter = findMethod(managerClass, "getConfigValueBool", config.getClass(), 1);
            Method setter = findMethod(managerClass, "setConfigValueBool", config.getClass(), 2);

            boolean current = (Boolean) getter.invoke(manager, config);
            boolean enabled = MorpheSettings.isEnabled(MorpheSettings.Feature.SPEED_CAMERA_SOUND_ALWAYS);

            if (enabled) {
                if (!current) {
                    if (!preferences.contains(ORIGINAL_VALUE_KEY)) {
                        preferences.edit().putBoolean(ORIGINAL_VALUE_KEY, false).apply();
                    }
                    setter.invoke(manager, config, true);
                    Log.i(TAG, "Speed camera sound below the speed limit: on (was off)");
                } else {
                    Log.i(TAG, "Speed camera sound below the speed limit: already on");
                }
            } else if (preferences.contains(ORIGINAL_VALUE_KEY)) {
                boolean original = preferences.getBoolean(ORIGINAL_VALUE_KEY, false);
                setter.invoke(manager, config, original);
                preferences.edit().remove(ORIGINAL_VALUE_KEY).apply();
                Log.i(TAG, "Speed camera sound below the speed limit restored to " + original);
            }
        } catch (Exception ex) {
            Log.e(TAG, "apply failure", ex);
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?> configType, int parameterCount) {
        for (Method method : type.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getName().equals(name) && parameters.length == parameterCount
                    && parameters[0].isAssignableFrom(configType)) {
                return method;
            }
        }
        throw new IllegalStateException("Method not found: " + name);
    }
}
