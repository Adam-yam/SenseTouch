package io.github.adam_yam.sensetouch

internal interface RecoveryStore {
    val pending: Boolean
    val original: String?
    fun save(value: String?): Boolean
    fun clear(): Boolean
}

internal class AssistantRecovery(
    private val store: RecoveryStore,
    private val temporary: String,
    private val read: () -> String?,
    private val write: (String?) -> Unit
) {
    fun begin() {
        check(!store.pending) { "기존 복원을 먼저 완료해 주세요." }
        val original = read()
        check(original != temporary) { "기본 디지털 어시스턴트를 직접 설정해 주세요." }
        check(store.save(original)) { "원래 설정을 저장하지 못해 실행을 중단했습니다." }
        write(temporary)
    }
    fun restore() {
        if (!store.pending) return
        if (read() == temporary) write(store.original)
        check(store.clear()) { "복원 기록을 정리하지 못했습니다." }
    }
}
