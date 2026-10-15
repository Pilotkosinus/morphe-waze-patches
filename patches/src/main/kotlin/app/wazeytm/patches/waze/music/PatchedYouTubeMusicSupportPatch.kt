package app.wazeytm.patches.waze.music

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.wazeytm.patches.shared.Constants.COMPATIBILITY_WAZE
import app.wazeytm.patches.shared.EXTENSION_PATH
import app.wazeytm.patches.shared.markPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

internal const val EXTENSION_CLASS =
    "Lapp/wazeytm/extension/waze/PatchedYouTubeMusicSupportPatch;"

private const val DEFAULT_PACKAGE_NAMES = "app.morphe.android.apps.youtube.music"

@Suppress("unused")
val patchedYouTubeMusicSupportPatch = bytecodePatch(
    name = "Waze – Patched YouTube Music Support",
    description = "Adds a repackaged YouTube Music app (for example patched with Morphe) " +
        "to the audio partners of Waze, so it can be selected and controlled from the Waze music player. " +
        "The original YouTube Music app keeps working.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WAZE)

    extendWith(EXTENSION_PATH)

    val packageNames by stringOption(
        key = "packageNames",
        default = DEFAULT_PACKAGE_NAMES,
        title = "Package names",
        description = "Comma separated package names of patched YouTube Music apps. " +
            "Installed repackaged YouTube Music apps are also detected automatically.",
        required = true
    ) { value ->
        value != null && value.split(",").all { name ->
            name.trim().matches(Regex("^[a-zA-Z][\\w]*(\\.[a-zA-Z][\\w]*)+$"))
        }
    }

    execute {
        markPatchIncluded("patchedYouTubeMusicSupportIncluded")

        // Clone the server side YouTube Music partner entry for patched package names.
        UpdateAppListFingerprint.let {
            val moveResult = it.instructionMatches[1]
            val configRegister = moveResult.getInstruction<OneRegisterInstruction>().registerA

            it.method.addInstruction(
                moveResult.index + 1,
                "invoke-static { v$configRegister }, $EXTENSION_CLASS->" +
                    "onSdkConfigurationLoaded(Lcom/waze/sdk/SdkConfiguration;)V"
            )
        }

        // Partner icons are only available on the server for the original package name.
        GetCustomizedAppIconUrlFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS->getIconPackageName(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
            """
        )

        ConfiguredPackageNamesFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "${packageNames!!.replace(" ", "")}"
                return-object v0
            """
        )
    }
}
