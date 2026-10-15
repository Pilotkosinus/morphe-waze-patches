package app.wazeytm.patches.waze.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * Method that builds the Waze main settings tree.
 *
 * Matches the "about" button node:
 * `new SettingsButton("about", R.string.ABOUT_SETTING_MENU_ITEM, "ABOUT_SETTINGS", R.drawable.setting_icon_info, listener)`
 *
 * The constructor reference gives the (obfuscated) button node class and its click listener interface.
 */
internal object AboutSettingsItemFingerprint : Fingerprint(
    filters = listOf(
        string("ABOUT_SETTINGS"),
        methodCall(
            name = "<init>",
            parameters = listOf("Ljava/lang/String;", "I", "Ljava/lang/String;", "I", "L")
        )
    )
)

/**
 * Same method, creation of the main settings page:
 * `new SettingsPage("settings_main", "SETTINGS_MAIN_SETTINGS", title, icon, children, flags)`
 */
internal object MainSettingsPageFingerprint : Fingerprint(
    filters = listOf(
        string("SETTINGS_MAIN_SETTINGS"),
        methodCall(
            name = "<init>",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "L", "L", "Ljava/util/List;", "I")
        )
    )
)

/**
 * `AppCompatDelegate.setDefaultNightMode(int)` (obfuscated). Waze applies its own day/night
 * setting with it. The settings page reads the mode from this class.
 */
internal object SetDefaultNightModeFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        string("setDefaultNightMode() called with an unknown mode")
    )
)
