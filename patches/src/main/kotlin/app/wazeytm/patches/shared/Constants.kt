package app.wazeytm.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_WAZE = Compatibility(
        name = "Waze",
        packageName = "com.waze",
        // Waze is distributed as split APK bundle.
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x33CCFF,
        targets = listOf(
            // Fingerprints only use unobfuscated names of com.waze.sdk.SdkConfiguration,
            // so newer versions are expected to work.
            AppTarget(
                version = null,
                isExperimental = true
            ),
            // Version the patch was developed and tested with.
            AppTarget(
                version = "5.25.0.1"
            )
        )
    )
}
