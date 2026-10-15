package app.wazeytm.extension.waze.settings;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adds the "Morphe patch settings" entry to the Waze main settings page.
 * <p>
 * Waze builds its settings as a tree of obfuscated node classes. The patch resolves the
 * button node class and its click listener interface from the "about" entry at patch time
 * and writes their names into {@link #getItemClassName()} and {@link #getClickInterfaceName()}.
 */
@SuppressWarnings("unused")
public final class SettingsMenuHook {
    private static final String TAG = "MorpheWazeSettings";

    private static final String MAIN_SETTINGS_PAGE_ID = "settings_main";
    private static final String ENTRY_ID = "morphe_patch_settings";
    private static final String ENTRY_ANALYTICS = "MORPHE_PATCH_SETTINGS";
    /**
     * The Waze settings end with a divider and the version text.
     * The entry is inserted before them.
     */
    private static final String VERSION_ENTRY_ID = "version";

    /**
     * Replaced by the patch with the type of the settings button node, e.g. "com.waze.settings.tree.d.h".
     */
    private static String getItemClassName() {
        return "";
    }

    /**
     * Replaced by the patch with the type of the click listener interface of the button node.
     */
    private static String getClickInterfaceName() {
        return "";
    }

    /**
     * Injection point. Called at the start of the settings page constructor.
     *
     * @return The list of child nodes to use for the page.
     */
    public static List<?> onSettingsPageCreated(String pageId, List<?> children) {
        try {
            if (!MAIN_SETTINGS_PAGE_ID.equals(pageId) || children == null) return children;

            Object entry = createEntry();
            if (entry == null) return children;

            List<Object> result = new ArrayList<>(children);
            int index = findVersionIndex(result);
            // Keep the divider in front of the version text below the new entry.
            if (index > 0) index--;
            if (index < 0) index = result.size();
            result.add(index, entry);
            return result;
        } catch (Exception ex) {
            Log.e(TAG, "onSettingsPageCreated failure", ex);
            return children;
        }
    }

    private static Object createEntry() throws Exception {
        Context context = MorpheSettings.getContext();
        if (context == null) return null;

        Class<?> itemClass = Class.forName(getItemClassName());
        Class<?> clickInterface = Class.forName(getClickInterfaceName());

        Object clickListener = Proxy.newProxyInstance(
                clickInterface.getClassLoader(),
                new Class<?>[]{clickInterface},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "toString":
                            return "MorpheSettingsClickListener";
                        default:
                            openSettings();
                            return null;
                    }
                });

        int titleId = context.getResources().getIdentifier("morphe_settings_title", "string", context.getPackageName());
        int iconId = context.getResources().getIdentifier("morphe_settings_icon", "drawable", context.getPackageName());
        if (titleId == 0) {
            Log.e(TAG, "Missing string resource morphe_settings_title");
            return null;
        }

        Constructor<?> constructor = itemClass.getConstructor(
                String.class, int.class, String.class, int.class, clickInterface);
        return constructor.newInstance(ENTRY_ID, titleId, ENTRY_ANALYTICS, iconId, clickListener);
    }

    private static void openSettings() {
        Context context = MorpheSettings.getContext();
        if (context != null) MorpheSettingsActivity.start(context);
    }

    /**
     * @return Index of the node with the id "version", or -1.
     */
    private static int findVersionIndex(List<Object> nodes) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (hasStringFieldValue(nodes.get(i), VERSION_ENTRY_ID)) return i;
        }
        return -1;
    }

    private static boolean hasStringFieldValue(Object node, String value) {
        if (node == null) return false;
        for (Class<?> type = node.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != String.class) continue;
                try {
                    field.setAccessible(true);
                    if (Objects.equals(value, field.get(node))) return true;
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }
}
