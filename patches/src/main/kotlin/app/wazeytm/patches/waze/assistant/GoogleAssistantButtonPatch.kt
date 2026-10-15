package app.wazeytm.patches.waze.assistant

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.wazeytm.patches.shared.Constants.COMPATIBILITY_WAZE
import app.wazeytm.patches.shared.EXTENSION_PATH
import app.wazeytm.patches.shared.markPatchIncluded
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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
            val method = fingerprint.method

            // Waze slides its buttons off screen by passing an x/y offset to every
            // ButtonPackButton(button, onClick, offsetX, offsetY, ...) call.
            // Pass the same offset to the extension, so the assistant button moves with them.
            val buttonCalls = method.instructions.withIndex().filter { (_, instruction) ->
                instruction.opcode == Opcode.INVOKE_STATIC_RANGE &&
                    ((instruction as ReferenceInstruction).reference as MethodReference).isButtonPackButton(method.definingClass)
            }
            if (buttonCalls.isEmpty()) throw IllegalStateException("ButtonPackButton call not found in ${method.name}")

            // Add the button as last child of the button column/row.
            // Inserted first: endNode comes after all button calls, so their indices stay valid.
            val endNode = fingerprint.instructionMatches[1]
            val composerRegister = when (val instruction = endNode.instruction) {
                is FiveRegisterInstruction -> instruction.registerC
                is RegisterRangeInstruction -> instruction.startRegister
                else -> throw IllegalStateException("Unexpected endNode instruction: $instruction")
            }
            method.addInstruction(
                endNode.index,
                "invoke-static/range { v$composerRegister .. v$composerRegister }, " +
                    "$ASSISTANT_HOOK_CLASS->addButton(Landroidx/compose/runtime/Composer;)V"
            )

            buttonCalls.reversed().forEach { (index, instruction) ->
                val offsetRegister = (instruction as RegisterRangeInstruction).startRegister + 2
                method.addInstruction(
                    index,
                    "invoke-static/range { v$offsetRegister .. v${offsetRegister + 1} }, " +
                        "$ASSISTANT_HOOK_CLASS->setButtonOffset(II)V"
                )
            }
        }
    }
}

/**
 * `static void ButtonPackButton(button, onClick, int offsetX, int offsetY, onLongClick, Modifier, Composer, int)`
 * of the same class.
 */
private fun MethodReference.isButtonPackButton(definingClass: String): Boolean {
    val types = parameterTypes.map { it.toString() }
    return this.definingClass == definingClass && returnType == "V" && types.size == 8 &&
        types[2] == "I" && types[3] == "I" &&
        types[5] == "Landroidx/compose/ui/Modifier;" &&
        types[6] == "Landroidx/compose/runtime/Composer;" &&
        types[7] == "I"
}
