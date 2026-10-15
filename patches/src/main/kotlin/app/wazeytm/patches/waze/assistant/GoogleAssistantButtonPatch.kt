package app.wazeytm.patches.waze.assistant

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.wazeytm.patches.shared.Constants.COMPATIBILITY_WAZE
import app.wazeytm.patches.shared.EXTENSION_PATH
import app.wazeytm.patches.shared.markPatchIncluded
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction

private const val ASSISTANT_HOOK_CLASS =
    "Lapp/wazeytm/extension/waze/assistant/AssistantButtonHook;"

@Suppress("unused")
val googleAssistantButtonPatch = bytecodePatch(
    name = "Google Assistant Button",
    description = "Adds a button below the audio and sound buttons of the map. " +
        "It opens the voice assistant of the device on top of Waze, so the map stays visible.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WAZE)

    extendWith(EXTENSION_PATH)

    execute {
        markPatchIncluded("googleAssistantButtonIncluded")

        listOf(ButtonPackPortraitFingerprint, ButtonPackLandscapeFingerprint).forEach { fingerprint ->
            val endNode = fingerprint.instructionMatches[1]
            val composerRegister = when (val instruction = endNode.instruction) {
                is FiveRegisterInstruction -> instruction.registerC
                is RegisterRangeInstruction -> instruction.startRegister
                else -> throw IllegalStateException("Unexpected endNode instruction: $instruction")
            }

            // Add the button as last child of the button column/row.
            fingerprint.method.addInstruction(
                endNode.index,
                "invoke-static/range { v$composerRegister .. v$composerRegister }, " +
                    "$ASSISTANT_HOOK_CLASS->addButton(Landroidx/compose/runtime/Composer;)V"
            )
        }
    }
}
