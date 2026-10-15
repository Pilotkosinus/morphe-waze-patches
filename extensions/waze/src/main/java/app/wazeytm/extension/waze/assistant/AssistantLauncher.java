package app.wazeytm.extension.waze.assistant;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import app.wazeytm.extension.waze.settings.MorpheSettings;

import static app.wazeytm.extension.waze.settings.MorpheSettings.text;

/**
 * Starts the assistant that is configured on the device. Both ways show the assistant
 * on top of Waze (tested with Gemini in the Google app), Waze stays visible below it.
 * <ul>
 *   <li>{@link Intent#ACTION_VOICE_COMMAND}: the assistant opens and starts listening.
 *   Used if "Listen immediately" is enabled.</li>
 *   <li>{@link Activity#showAssist(Bundle)}: the same as long pressing home. The assistant opens,
 *   the user taps its microphone to speak.</li>
 * </ul>
 * The other way is used as fallback.
 */
public final class AssistantLauncher {
    private static final String TAG = "MorpheWazeAssistant";

    private AssistantLauncher() {
    }

    public static void launch(Context context) {
        Activity activity = findActivity(context);
        boolean listenImmediately = MorpheSettings.isEnabled(MorpheSettings.Feature.ASSISTANT_LISTEN_IMMEDIATELY);

        boolean started = listenImmediately
                ? startVoiceCommand(context, activity) || showAssist(activity)
                : showAssist(activity) || startVoiceCommand(context, activity);
        if (started) return;

        Toast.makeText(context, text(
                "Kein Sprachassistent gefunden. Bitte in den Android-Einstellungen unter „Standard-Apps“ einen digitalen Assistenten auswählen.",
                "No voice assistant found. Select a digital assistant app in the Android default apps settings."),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Opens the assistant in listening mode.
     */
    private static boolean startVoiceCommand(Context context, Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
            if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            (activity != null ? activity : context).startActivity(intent);
            Log.i(TAG, "Assistant started with ACTION_VOICE_COMMAND");
            return true;
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "No activity for ACTION_VOICE_COMMAND");
        } catch (Exception ex) {
            Log.e(TAG, "ACTION_VOICE_COMMAND failure", ex);
        }
        return false;
    }

    /**
     * Shows the assistant session of the system (like long pressing home).
     */
    private static boolean showAssist(Activity activity) {
        if (activity == null) return false;
        try {
            Bundle args = new Bundle();
            args.putString("source", "waze_morphe_button");
            if (activity.showAssist(args)) {
                Log.i(TAG, "Assistant session shown with showAssist");
                return true;
            }
            Log.w(TAG, "showAssist returned false (no assistant, disabled, or activity not in foreground)");
        } catch (Exception ex) {
            Log.e(TAG, "showAssist failure", ex);
        }
        return false;
    }

    static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
