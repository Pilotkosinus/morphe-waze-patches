package app.wazeytm.patches.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext

internal const val EXTENSION_PATH = "extensions/waze.mpe"

private const val PATCH_REGISTRY_CLASS =
    "Lapp/wazeytm/extension/waze/settings/PatchRegistry;"

/**
 * Makes `PatchRegistry.<methodName>()` of the extension return true,
 * so the Morphe settings know that this patch was applied.
 */
context(context: BytecodePatchContext)
internal fun markPatchIncluded(methodName: String) {
    Fingerprint(
        definingClass = PATCH_REGISTRY_CLASS,
        name = methodName,
        returnType = "Z",
        parameters = listOf()
    ).method.addInstructions(
        0,
        """
            const/4 v0, 0x1
            return v0
        """
    )
}

/**
 * Replaces the body of a `static String` extension method with a constant.
 */
context(context: BytecodePatchContext)
internal fun returnString(definingClass: String, methodName: String, value: String) {
    Fingerprint(
        definingClass = definingClass,
        name = methodName,
        returnType = "Ljava/lang/String;",
        parameters = listOf()
    ).method.addInstructions(
        0,
        """
            const-string v0, "$value"
            return-object v0
        """
    )
}
