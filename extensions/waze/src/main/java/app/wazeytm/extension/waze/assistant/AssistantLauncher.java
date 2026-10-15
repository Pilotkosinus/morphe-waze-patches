package app.wazeytm.extension.waze.assistant;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import static app.wazeytm.extension.waze.settings.MorpheSettings.text;

/**
 * Starts the assistant that is configured on the device.
 * <ol>
 *   <li>{@link Activity#showAssist(Bundle)}: Asks the system to show the current assistant
 *   (the same as long pressing home or "Hey Google"). The assistant shows its session on top of
 *   Waze, Waze stays the foreground app.</li>
 *   <li>{@link Intent#ACTION_VOICE_COMMAND}: Fallback, opens the voice command activity of the
 *   assistant app. Depending on the assistant this can cover Waze.</li>
 * </ol>
 */
public final class AssistantLauncher {
    private static final String TAG = "MorpheWazeAssistant";

    private AssistantLauncher() {
    }

    public static void launch(Context context) {
        Activity activity = findActivity(context);

        if (activity != null) {
            try {
                Bundle args = new Bundle();
                args.putString("source", "waze_morphe_button");
                if (activity.showAssist(args)) {
                    Log.i(TAG, "Assistant session shown with showAssist");
                    return;
                }
                Log.w(TAG, "showAssist returned false (no assistant, disabled, or activity not in foreground)");
            } catch (Exception ex) {
                Log.e(TAG, "showAssist failure", ex);
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
            if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            (activity != null ? activity : context).startActivity(intent);
            Log.i(TAG, "Assistant started with ACTION_VOICE_COMMAND");
            return;
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "No activity for ACTION_VOICE_COMMAND");
        } catch (Exception ex) {
            Log.e(TAG, "ACTION_VOICE_COMMAND failure", ex);
        }

        Toast.makeText(context, text(
                "Kein Sprachassistent gefunden. Bitte in den Android-Einstellungen unter „Standard-Apps“ einen digitalen Assistenten auswählen.",
                "No voice assistant found. Select a digital assistant app in the Android default apps settings."),
                Toast.LENGTH_LONG).show();
    }

    static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
