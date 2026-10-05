package io.github.adam_yam.sensetouch

import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

internal data class RegisteredFingerprint(val id: Int, val name: String)

internal object FingerprintCatalog {
    private val reading = java.util.concurrent.atomic.AtomicBoolean(false)
    fun read(context: Context): List<RegisteredFingerprint> {
        check(reading.compareAndSet(false, true)) { "이전 지문 조회가 아직 진행 중입니다. 잠시 후 다시 시도해 주세요." }
        return try { readCurrent(context) } finally { reading.set(false) }
    }

    @Suppress("DEPRECATION")
    private fun readCurrent(context: Context): List<RegisteredFingerprint> {
        val manager = context.getSystemService(FingerprintManager::class.java)
            ?: error("이 기기에서 지문 서비스를 찾지 못했습니다.")
        check(manager.isHardwareDetected) { "지문 센서를 사용할 수 없습니다." }
        HiddenApiBypass.addHiddenApiExemptions(
            "Landroid/hardware/fingerprint/FingerprintManager;",
            "Landroid/hardware/fingerprint/Fingerprint;",
            "Landroid/hardware/biometrics/BiometricAuthenticator\$Identifier;"
        )
        val raw = FingerprintManager::class.java.getMethod("getEnrolledFingerprints").invoke(manager)
            as? List<*> ?: error("시스템이 지문 목록을 반환하지 않았습니다.")
        val entries = raw.map { item ->
            checkNotNull(item) { "지문 목록에 잘못된 항목이 있습니다." }
            val id = (item.javaClass.getMethod("getBiometricId").invoke(item) as Number).toInt()
            val name = item.javaClass.getMethod("getName").invoke(item)?.toString().orEmpty()
            RegisteredFingerprint(id, name)
        }
        check(entries.isNotEmpty() || !manager.hasEnrolledFingerprints()) {
            "등록된 지문이 있지만 시스템이 목록 조회를 허용하지 않았습니다."
        }
        return validate(entries)
    }

    fun validate(entries: List<RegisteredFingerprint>): List<RegisteredFingerprint> {
        check(entries.all { it.id > 0 }) { "지원하지 않는 지문 ID가 반환되었습니다." }
        check(entries.map { it.id }.distinct().size == entries.size) { "중복된 지문 ID가 반환되었습니다." }
        return entries.mapIndexed { index, entry ->
            if (entry.name.isBlank()) entry.copy(name = "등록 지문 ${index + 1}") else entry
        }
    }
}
