package app.wazeytm.extension.waze.assistant;

import android.content.Context;
import android.util.Log;

import androidx.compose.runtime.Composer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Adds the assistant button to the Waze map button pack (Jetpack Compose).
 * <p>
 * The call is injected right before the button column (portrait) or row (landscape) of
 * {@code ButtonPackPortrait}/{@code ButtonPackLandscape} is closed, so the button is laid out
 * below the audio and sound buttons with the same spacing.
 * <p>
 * Compose classes are not obfuscated in Waze, but Kotlin's function interfaces are.
 * Therefore {@code AndroidView(factory, modifier, update)} is called with reflection and the
 * lambdas are created with {@link Proxy} for whatever interface type the method declares.
 */
@SuppressWarnings("unused")
public final class AssistantButtonHook {
    private static final String TAG = "MorpheWazeAssistant";
    private static final int GROUP_KEY = 0x4D6F7270; // "Morp"

    private static Method androidViewMethod;
    private static Object modifier;
    private static Object factory;
    private static Object update;
    private static boolean failed;

    /**
     * Offset in px that Waze applies to every button of the button pack. Waze slides the buttons
     * off screen with it, for example while the map is moved or zoomed.
     */
    private static volatile int packOffsetX;
    private static volatile int packOffsetY;

    /**
     * Injection point. Called with the offset arguments of every {@code ButtonPackButton(...)} call.
     */
    public static void setButtonOffset(int x, int y) {
        packOffsetX = x;
        packOffsetY = y;
    }

    static int getPackOffsetX() {
        return packOffsetX;
    }

    static int getPackOffsetY() {
        return packOffsetY;
    }

    /**
     * Injection point inside the button pack composable.
     */
    public static void addButton(Composer composer) {
        // Always emit the group, so the slot table structure stays the same if the button fails.
        composer.startReplaceGroup(GROUP_KEY);
        try {
            if (failed) return;
            if (androidViewMethod == null) initialize();
            // AndroidView(factory, modifier, update, composer, $changed, $default)
            androidViewMethod.invoke(null, factory, modifier, update, composer, 0, 0);
            // The button pack recomposes on every animation frame of the offset.
            AssistantButtonView.applyPackOffset();
        } catch (Exception ex) {
            failed = true;
            Log.e(TAG, "Could not add assistant button", ex);
        } finally {
            composer.endReplaceGroup();
        }
    }

    private static void initialize() throws Exception {
        Class<?> kt = Class.forName("androidx.compose.ui.viewinterop.AndroidView_androidKt");
        Method found = null;
        for (Method method : kt.getMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (method.getName().equals("AndroidView") && types.length == 6
                    && types[1].getName().equals("androidx.compose.ui.Modifier")
                    && types[3].getName().equals("androidx.compose.runtime.Composer")) {
                found = method;
                break;
            }
        }
        if (found == null) throw new IllegalStateException("AndroidView(factory, modifier, update) not found");

        Class<?> functionType = found.getParameterTypes()[0];
        Class<?> updateType = found.getParameterTypes()[2];

        factory = createFunction(functionType, argument -> new AssistantButtonView((Context) argument));
        update = createFunction(updateType, argument -> null);
        modifier = Class.forName("androidx.compose.ui.Modifier").getField("Companion").get(null);
        androidViewMethod = found;
    }

    private interface Function {
        Object invoke(Object argument);
    }

    /**
     * Implements the obfuscated Kotlin Function1 interface.
     */
    private static Object createFunction(Class<?> type, Function function) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "MorpheAssistantButtonFunction";
                default:
                    return function.invoke(args != null && args.length > 0 ? args[0] : null);
            }
        });
    }
}
