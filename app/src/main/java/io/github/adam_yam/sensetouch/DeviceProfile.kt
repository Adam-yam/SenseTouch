package io.github.adam_yam.sensetouch

import android.content.ComponentName
import android.content.Context
import android.os.Build

object DeviceProfile {
    const val SHIZUKU = "moe.shizuku.privileged.api"
    const val WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS"
    const val ASSISTANT = "assistant"
    const val SELECTED_ID = "selected_id"
    const val RESTORE_DELAY_MS = 700L
    val update = ComponentName("com.samsung.android.biometrics.app.setting",
        "com.samsung.android.biometrics.app.setting.fingerprint.enroll.FingerprintUpdateActivity")
    fun supported(context: Context): Boolean = Build.MANUFACTURER.equals("samsung", true) &&
        runCatching { context.packageManager.getActivityInfo(update, 0).let { it.enabled && it.applicationInfo.enabled } }.getOrDefault(false)
    fun details(): String {
        val oneUi = runCatching { Build.VERSION::class.java.getField("SEM_PLATFORM_INT").getInt(null) - 90000 }
            .getOrNull()?.takeIf { it >= 0 }?.let { "${it / 10000}.${it % 10000 / 100}" } ?: "확인 불가"
        return "${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} · One UI $oneUi"
    }
}
