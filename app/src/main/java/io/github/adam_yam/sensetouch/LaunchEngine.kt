package io.github.adam_yam.sensetouch

import android.app.SearchManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object LaunchEngine {
    private val worker = Executors.newSingleThreadExecutor()
    val busy = AtomicBoolean(false)
    private fun prefs(c: Context) = c.getSharedPreferences("assistant_recovery", Context.MODE_PRIVATE)
    fun pending(c: Context) = prefs(c).getBoolean("pending", false)
    fun allowed(c: Context) = c.checkSelfPermission(DeviceProfile.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    private fun read(c: Context) = Settings.Secure.getString(c.contentResolver, DeviceProfile.ASSISTANT)
    private fun write(c: Context, value: String?) {
        check(Settings.Secure.putString(c.contentResolver, DeviceProfile.ASSISTANT, value)) { "설정 저장이 거부되었습니다." }
        check(read(c) == value) { "설정 저장 결과를 확인할 수 없습니다." }
    }
    private fun recovery(c: Context) = AssistantRecovery(object : RecoveryStore {
        override val pending get() = pending(c)
        override val original get() = prefs(c).getString("original", null)
        override fun save(value: String?) = prefs(c).edit().putString("original", value).putBoolean("pending", true).commit()
        override fun clear() = prefs(c).edit().clear().commit()
    }, DeviceProfile.update.flattenToString(), { read(c) }, { write(c, it) })
    private fun restore(c: Context) {
        if (!pending(c)) return
        check(allowed(c)) { "설정 복원을 위해 권한을 다시 허용해 주세요." }
        recovery(c).restore()
    }
    fun recover(c: Context, done: (String) -> Unit) = execute(c, done) {
        restore(it)
        "기존 Assistant 설정을 확인했습니다."
    }
    fun grant(c: Context, done: (String) -> Unit) = execute(c, done) {
        check(Shizuku.pingBinder()) { "Shizuku가 실행되지 않았습니다." }
        check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { "Shizuku 권한을 허용해 주세요." }
        HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/IPackageManager")
        val stub = Class.forName("android.content.pm.IPackageManager\$Stub")
        val manager = stub.getMethod("asInterface", IBinder::class.java).invoke(null,
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")))
        Class.forName("android.content.pm.IPackageManager").getMethod("grantRuntimePermission",
            String::class.java, String::class.java, Int::class.javaPrimitiveType)
            .invoke(manager, it.packageName, DeviceProfile.WRITE_SECURE_SETTINGS, Process.myUid() / 100000)
        check(allowed(it)) { "시스템 설정 권한 부여를 확인하지 못했습니다." }
        restore(it)
        "권한 설정이 완료되었습니다."
    }
    fun launch(c: Context, id: Int, registered: Boolean = false, done: (String) -> Unit) = execute(c, done) {
        if (registered) {
            check(FingerprintCatalog.read(it).any { finger -> finger.id == id }) {
                "선택한 지문이 삭제되었거나 변경되었습니다. 목록을 새로고침해 주세요."
            }
        } else check(id in 1..4) { "지원하지 않는 수동 지문 번호입니다." }
        check(DeviceProfile.supported(it)) { "이 기기에는 지원되는 삼성 지문 인식률 향상 화면이 없습니다." }
        check(allowed(it)) { "먼저 시스템 설정 권한을 허용해 주세요." }
        restore(it)
        var error: Throwable? = null
        try {
            recovery(it).begin()
            HiddenApiBypass.addHiddenApiExemptions("Landroid/app/SearchManager;")
            HiddenApiBypass.invoke(SearchManager::class.java, it.getSystemService(Context.SEARCH_SERVICE),
                "launchAssist", Bundle().apply { putInt(DeviceProfile.SELECTED_ID, id) })
            Thread.sleep(DeviceProfile.RESTORE_DELAY_MS)
        } catch (e: Throwable) { error = e }
        finally {
            try { restore(it) } catch (e: Throwable) {
                throw IllegalStateException("Assistant 설정 복원 대기 중입니다. 권한을 확인한 뒤 복원 재시도를 눌러 주세요. ${e.message}", error ?: e)
            }
        }
        error?.let { throw it }
        "지문 인식률 향상 화면을 요청했습니다. 화면이 열리지 않으면 이 OS에서 실행이 제한된 것일 수 있습니다."
    }
    private fun execute(c: Context, done: (String) -> Unit, action: (Context) -> String) {
        if (!busy.compareAndSet(false, true)) { done("이전 작업을 처리하고 있습니다."); return }
        val app = c.applicationContext
        worker.execute {
            val message = try { action(app) } catch (e: Throwable) {
                val root = generateSequence(e) { it.cause?.takeUnless { cause -> cause === it } }.last()
                "${e.message ?: "실행에 실패했습니다."}\n오류: ${root.javaClass.simpleName}"
            } finally { busy.set(false) }
            android.os.Handler(android.os.Looper.getMainLooper()).post { done(message) }
        }
    }
}
