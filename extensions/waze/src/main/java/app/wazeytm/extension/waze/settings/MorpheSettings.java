package app.wazeytm.extension.waze.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Locale;

/**
 * Central configuration of all Morphe Waze features.
 * <p>
 * Values are stored in the shared preferences file {@link #PREFERENCES_NAME} of Waze,
 * so they survive restarts. All features are enabled by default, so every patch also works
 * without the settings patch.
 */
public final class MorpheSettings {
    private static final String TAG = "MorpheWazeSettings";
    public static final String PREFERENCES_NAME = "morphe_patch_settings";

    public enum Feature {
        PATCHED_YOUTUBE_MUSIC(
                "patched_youtube_music_support",
                "Patched YouTube Music Support",
                "Patched YouTube Music Support",
                "Gepatchtes YouTube Music in der Waze-Musiksteuerung anbieten (z. B. Morphe/ReVanced).",
                "Offer patched YouTube Music (e.g. Morphe/ReVanced) in the Waze music controls.",
                true
        ) {
            @Override
            public boolean isIncluded() {
                return PatchRegistry.patchedYouTubeMusicSupportIncluded();
            }
        },
        GOOGLE_ASSISTANT_BUTTON(
                "google_assistant_button",
                "Google Assistant Button",
                "Google Assistant Button",
                "Zeigt unter den Kartenbuttons einen Button, der den Sprachassistenten des Geräts über Waze öffnet.",
                "Shows a map button below the other map buttons that opens the device assistant on top of Waze.",
                false
        ) {
            @Override
            public boolean isIncluded() {
                return PatchRegistry.googleAssistantButtonIncluded();
            }
        },
        ASSISTANT_LISTEN_IMMEDIATELY(
                "google_assistant_listen_immediately",
                "Sofort zuhören",
                "Listen immediately",
                "Der Assistent hört nach dem Antippen direkt zu. Aus: Der Assistent öffnet sich nur, das Mikrofon muss extra angetippt werden.",
                "The assistant listens right after tapping the button. Off: the assistant only opens, tap its microphone to speak.",
                false
        ) {
            @Override
            public boolean isIncluded() {
                return PatchRegistry.googleAssistantButtonIncluded();
            }

            @Override
            public Feature getParent() {
                return GOOGLE_ASSISTANT_BUTTON;
            }
        },
        SPEED_CAMERA_SOUND_ALWAYS(
                "speed_camera_sound_always",
                "Blitzer-Warnton immer abspielen",
                "Always play speed camera sound",
                "Waze spielt den Warnton für Blitzer sonst nur, wenn du zu schnell fährst. Mit dieser Option kommt der Ton bei jedem Blitzer.",
                "Waze otherwise plays the speed camera sound only when you drive too fast. With this option the sound plays for every speed camera.",
                false
        ) {
            @Override
            public boolean isIncluded() {
                return PatchRegistry.speedCameraSoundAlwaysIncluded();
            }

            @Override
            void onChanged() {
                app.wazeytm.extension.waze.alerts.SpeedCameraSoundPatch.apply();
            }
        };

        public final String key;
        private final String titleDe;
        private final String titleEn;
        private final String descriptionDe;
        private final String descriptionEn;
        /**
         * The setting is read once when Waze starts, so a change needs a restart.
         */
        public final boolean requiresRestart;

        Feature(String key, String titleDe, String titleEn, String descriptionDe, String descriptionEn,
                boolean requiresRestart) {
            this.key = key;
            this.titleDe = titleDe;
            this.titleEn = titleEn;
            this.descriptionDe = descriptionDe;
            this.descriptionEn = descriptionEn;
            this.requiresRestart = requiresRestart;
        }

        /**
         * @return If the bytecode patch of this feature was applied to the APK.
         */
        public abstract boolean isIncluded();

        /**
         * Called after the user changed the switch. Features that apply a change right away override it.
         */
        void onChanged() {
        }

        /**
         * @return The feature this option belongs to, or null for a patch.
         * An option is shown below its parent and only works if the parent is enabled.
         */
        public Feature getParent() {
            return null;
        }

        public String getTitle() {
            return isGerman() ? titleDe : titleEn;
        }

        public String getDescription() {
            return isGerman() ? descriptionDe : descriptionEn;
        }
    }

    private MorpheSettings() {
    }

    /**
     * @return If the feature is patched into the APK and enabled by the user.
     */
    public static boolean isEnabled(Feature feature) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) return true;
        return preferences.getBoolean(feature.key, true);
    }

    public static void setEnabled(Feature feature, boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) return;
        preferences.edit().putBoolean(feature.key, enabled).apply();
        Log.i(TAG, feature.key + " = " + enabled);
        feature.onChanged();
    }

    public static void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences preferences = getPreferences();
        if (preferences != null) preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences preferences = getPreferences();
        if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static boolean isGerman() {
        return "de".equals(Locale.getDefault().getLanguage());
    }

    /**
     * @return German or English text depending on the device language.
     */
    public static String text(String german, String english) {
        return isGerman() ? german : english;
    }

    private static SharedPreferences getPreferences() {
        Context context = getContext();
        return context == null ? null : context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @SuppressLint("PrivateApi")
    public static Context getContext() {
        try {
            return (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null);
        } catch (Exception ex) {
            Log.e(TAG, "getContext failure", ex);
            return null;
        }
    }
}
