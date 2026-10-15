package app.wazeytm.patches.waze.alerts

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.wazeytm.patches.shared.Constants.COMPATIBILITY_WAZE
import app.wazeytm.patches.shared.EXTENSION_PATH
import app.wazeytm.patches.shared.markPatchIncluded

private const val EXTENSION_CLASS =
    "Lapp/wazeytm/extension/waze/alerts/SpeedCameraSoundPatch;"

private const val CONFIG_MANAGER_CLASS = "Lcom/waze/ConfigManager;"

/**
 * `void ConfigManager.onConfigSyncedFromServer()`: called after the configuration
 * from the Waze server was applied.
 */
internal object ConfigSyncedFromServerFingerprint : Fingerprint(
    definingClass = CONFIG_MANAGER_CLASS,
    name = "onConfigSyncedFromServer",
    returnType = "V",
    parameters = listOf()
)

/**
 * `void ConfigManager.onConfigSyncedToServer()`: called when the configuration sync is complete.
 */
internal object ConfigSyncedToServerFingerprint : Fingerprint(
    definingClass = CONFIG_MANAGER_CLASS,
    name = "onConfigSyncedToServer",
    returnType = "V",
    parameters = listOf()
)

/**
 * `void NativeManager.flushOnAppStartedEvents()`: called on every app start right after
 * the native core (including its configuration) was initialized.
 */
internal object AppStartedFingerprint : Fingerprint(
    definingClass = "Lcom/waze/NativeManager;",
    name = "flushOnAppStartedEvents",
    returnType = "V",
    parameters = listOf()
)

@Suppress("unused")
val speedCameraSoundPatch = bytecodePatch(
    name = "Always Play Speed Camera Sound",
    description = "Waze shows speed camera alerts on screen, but plays the alert sound only when you drive " +
        "above the speed limit. This patch plays the sound for every speed camera. " +
        "Can be turned off in the Morphe patch settings.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WAZE)

    extendWith(EXTENSION_PATH)

    execute {
        markPatchIncluded("speedCameraSoundAlwaysIncluded")

        // Apply the setting on app start, and again after every sync with the Waze server,
        // because the server can change configuration values.
        listOf(AppStartedFingerprint, ConfigSyncedFromServerFingerprint, ConfigSyncedToServerFingerprint).forEach {
            it.method.addInstruction(
                0,
                "invoke-static { }, $EXTENSION_CLASS->onConfigSynced()V"
            )
        }
    }
}
