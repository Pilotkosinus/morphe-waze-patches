package app.wazeytm.patches.waze.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.wazeytm.patches.shared.Constants.COMPATIBILITY_WAZE
import app.wazeytm.patches.shared.EXTENSION_PATH
import app.wazeytm.patches.shared.markPatchIncluded
import app.wazeytm.patches.shared.returnString
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val SETTINGS_HOOK_CLASS =
    "Lapp/wazeytm/extension/waze/settings/SettingsMenuHook;"

private const val SETTINGS_ACTIVITY_TYPE =
    "Lapp/wazeytm/extension/waze/settings/MorpheSettingsActivity;"

internal const val SETTINGS_ACTIVITY_CLASS =
    "app.wazeytm.extension.waze.settings.MorpheSettingsActivity"

private const val TITLE_EN = "Morphe patch settings"
private const val TITLE_DE = "Morphe Patch Einstellungen"

/**
 * Settings icon in the style of the Waze settings icons (40dp colored circle, white glyph).
 */
private val SETTINGS_ICON = """
    <vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="40dp"
        android:height="40dp"
        android:viewportWidth="40"
        android:viewportHeight="40">
        <path
            android:fillColor="#7B5CF5"
            android:pathData="M20,0a20,20 0,1 1,0 40a20,20 0,1 1,0 -40z" />
        <group
            android:translateX="11"
            android:translateY="11"
            android:scaleX="0.75"
            android:scaleY="0.75">
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M20.5,11H19V7c0,-1.1 -0.9,-2 -2,-2h-4V3.5C13,2.12 11.88,1 10.5,1S8,2.12 8,3.5V5H4c-1.1,0 -1.99,0.9 -1.99,2v3.8H3.5c1.49,0 2.7,1.21 2.7,2.7s-1.21,2.7 -2.7,2.7H2V20c0,1.1 0.9,2 2,2h3.8v-1.5c0,-1.49 1.21,-2.7 2.7,-2.7 1.49,0 2.7,1.21 2.7,2.7V22H17c1.1,0 2,-0.9 2,-2v-4h1.5c1.38,0 2.5,-1.12 2.5,-2.5S21.88,11 20.5,11z" />
        </group>
    </vector>
""".trimIndent()

private val settingsResourcePatch = resourcePatch {
    execute {
        // Declare the settings activity of the extension.
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SETTINGS_ACTIVITY_CLASS)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:theme", "@android:style/Theme.Material.Light.NoActionBar")
            activity.setAttribute("android:configChanges", "orientation|screenSize|screenLayout|keyboardHidden")
            application.appendChild(activity)
        }

        // Title of the settings entry. Waze looks the resource up in its own language
        // pack first and falls back to the Android string resource if there is none.
        fun addString(path: String, value: String) {
            val file = get(path)
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n")
            }
            document(path).use { document ->
                val string = document.createElement("string")
                string.setAttribute("name", "morphe_settings_title")
                string.textContent = value
                document.documentElement.appendChild(string)
            }
        }
        addString("res/values/strings.xml", TITLE_EN)
        addString("res/values-de/strings.xml", TITLE_DE)

        get("res/drawable/morphe_settings_icon.xml").apply {
            parentFile.mkdirs()
            writeText(SETTINGS_ICON)
        }
    }
}

@Suppress("unused")
val morpheSettingsPatch = bytecodePatch(
    name = "Morphe Patch Settings",
    description = "Adds \"$TITLE_DE\" at the bottom of the Waze settings. " +
        "It shows all applied Morphe patches with a switch to turn each of them on or off.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WAZE)

    dependsOn(settingsResourcePatch)

    extendWith(EXTENSION_PATH)

    execute {
        markPatchIncluded("settingsIncluded")

        // Settings button node class and its click listener interface, from the "about" entry.
        val buttonConstructor = AboutSettingsItemFingerprint.instructionMatches[1]
            .getInstruction<ReferenceInstruction>().reference as MethodReference
        val buttonClass = buttonConstructor.definingClass
        val clickInterface = buttonConstructor.parameterTypes[4].toString()

        returnString(SETTINGS_HOOK_CLASS, "getItemClassName", buttonClass.toJavaClassName())
        returnString(SETTINGS_HOOK_CLASS, "getClickInterfaceName", clickInterface.toJavaClassName())

        // Day/night mode of Waze for the settings page. Optional, the page follows the system otherwise.
        SetDefaultNightModeFingerprint.originalMethodOrNull?.let {
            returnString(SETTINGS_ACTIVITY_TYPE, "getNightModeClassName", it.definingClass.toJavaClassName())
        }

        // Settings page constructor. The main page uses a synthetic constructor with default
        // arguments that delegates to the real one. Hook the real constructor, so every
        // creation of the page passes through the hook.
        val pageConstructorReference = MainSettingsPageFingerprint.instructionMatches[1]
            .getInstruction<ReferenceInstruction>().reference as MethodReference

        var pageConstructor = navigate(pageConstructorReference).stop()
        delegatedConstructor(pageConstructor)?.let { pageConstructor = navigate(it).stop() }

        val listParameterIndex = pageConstructor.parameterTypes.indexOfFirst { it.toString() == "Ljava/util/List;" }
        if (listParameterIndex < 0) throw IllegalStateException("Settings page constructor has no List parameter")

        // p0 is "this", parameters start at p1. The id is the first parameter.
        val listRegister = "p${listParameterIndex + 1}"
        pageConstructor.addInstructions(
            0,
            """
                invoke-static { p1, $listRegister }, $SETTINGS_HOOK_CLASS->onSettingsPageCreated(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;
                move-result-object $listRegister
            """
        )
    }
}

/**
 * @return The `this(...)` constructor that [constructor] delegates to, if it has a List parameter.
 */
private fun delegatedConstructor(constructor: MutableMethod): MethodReference? {
    if (!AccessFlags.SYNTHETIC.isSet(constructor.accessFlags)) return null
    return constructor.instructions.firstNotNullOfOrNull { instruction ->
        if (instruction.opcode != Opcode.INVOKE_DIRECT && instruction.opcode != Opcode.INVOKE_DIRECT_RANGE) {
            return@firstNotNullOfOrNull null
        }
        val reference = (instruction as ReferenceInstruction).reference as MethodReference
        reference.takeIf {
            it.name == "<init>" && it.definingClass == constructor.definingClass &&
                it.parameterTypes.any { type -> type.toString() == "Ljava/util/List;" }
        }
    }
}

private fun String.toJavaClassName() = removePrefix("L").removeSuffix(";").replace('/', '.')
