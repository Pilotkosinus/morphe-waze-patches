package app.wazeytm.extension.waze.settings;

/**
 * Tells at runtime which bytecode patches were applied.
 * <p>
 * Every method returns false here. Each patch replaces the body of its own method
 * with {@code return true} when it is applied, so a feature that was not patched
 * into the APK is never offered as a working switch.
 * <p>
 * To register a new patch: add a method here, add a {@link MorpheSettings.Feature}
 * that references it, and make the patch return true from the method.
 */
@SuppressWarnings("unused")
public final class PatchRegistry {
    private PatchRegistry() {
    }

    public static boolean settingsIncluded() {
        return false;
    }

    public static boolean patchedYouTubeMusicSupportIncluded() {
        return false;
    }

    public static boolean googleAssistantButtonIncluded() {
        return false;
    }

    public static boolean speedCameraSoundAlwaysIncluded() {
        return false;
    }
}
