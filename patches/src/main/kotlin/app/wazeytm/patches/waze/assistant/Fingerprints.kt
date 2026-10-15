package app.wazeytm.patches.waze.assistant

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

private const val COMPOSER_CLASS = "Landroidx/compose/runtime/Composer;"

/**
 * Composable that lays out the map buttons (audio, sound, ...) of the main screen.
 * The Compose trace strings stay in release builds and contain the source name.
 *
 * The first `Composer.endNode()` closes the button column (portrait) or row (landscape).
 */
internal abstract class ButtonPackFingerprint(composableName: String) : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("com.waze.main_screen.button_pack.$composableName (", StringComparisonType.STARTS_WITH),
        methodCall(
            definingClass = COMPOSER_CLASS,
            name = "endNode"
        )
    )
)

internal object ButtonPackPortraitFingerprint : ButtonPackFingerprint("ButtonPackPortrait")

internal object ButtonPackLandscapeFingerprint : ButtonPackFingerprint("ButtonPackLandscape")
