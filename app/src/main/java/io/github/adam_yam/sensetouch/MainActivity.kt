package io.github.adam_yam.sensetouch

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            val now = LaunchEngine.busy.get()
            if (working != now) { working = now; revision++ }
            refreshHandler.postDelayed(this, 500)
        }
    }
    private var fingerprints by mutableStateOf<List<RegisteredFingerprint>>(emptyList())
    private var loadingFingerprints by mutableStateOf(false)
    private var fingerprintError by mutableStateOf<String?>(null)
    private var manualSelection by mutableStateOf(false)
    private var selectedId by mutableStateOf<Int?>(null)
    private var queryGeneration = 0
    private val queryHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private fun refreshFingerprints() {
        if (loadingFingerprints) return
        val generation = ++queryGeneration
        loadingFingerprints = true
        fingerprintError = null
        val previousId = selectedId
        val previousManualSelection = manualSelection
        val timeout = Runnable {
            if (generation == queryGeneration && loadingFingerprints) {
                queryGeneration++
                loadingFingerprints = false
                fingerprints = emptyList()
                manualSelection = previousManualSelection
                selectedId = previousId.takeIf { previousManualSelection }
                fingerprintError = "지문 목록 조회 시간이 초과되었습니다. 다시 시도해 주세요."
            }
        }
        queryHandler.postDelayed(timeout, 5000)
        val app = applicationContext
        Thread {
            val result = runCatching { FingerprintCatalog.read(app) }
            runOnUiThread {
                if (generation != queryGeneration || isDestroyed) return@runOnUiThread
                queryHandler.removeCallbacks(timeout)
                loadingFingerprints = false
                manualSelection = false
                result.onSuccess {
                    fingerprints = it
                    selectedId = previousId?.takeIf { id -> it.any { entry -> entry.id == id } } ?: it.firstOrNull()?.id
                }.onFailure {
                    fingerprints = emptyList()
                    manualSelection = previousManualSelection
                    selectedId = previousId.takeIf { previousManualSelection }
                    val cause = (it as? java.lang.reflect.InvocationTargetException)?.targetException ?: it
                    fingerprintError = "이 기기에서 자동 조회를 완료하지 못했습니다.\n${cause.message ?: "One UI가 조회를 제한할 수 있습니다."} (${cause.javaClass.simpleName})"
                }
            }
        }.start()
    }
    private var revision by mutableIntStateOf(0)
    private var message by mutableStateOf("")
    private var working by mutableStateOf(false)
    private var requesting by mutableStateOf(false)
    private val binderReceived = Shizuku.OnBinderReceivedListener { runOnUiThread { revision++ } }
    private val binderDead = Shizuku.OnBinderDeadListener { runOnUiThread { requesting = false; revision++ } }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == 101) runOnUiThread {
            requesting = false
            if (result == PackageManager.PERMISSION_GRANTED) runTask { LaunchEngine.grant(this, it) }
            else { message = "Shizuku 권한이 거부되었습니다. Shizuku의 승인된 앱에서 SenseTouch 권한을 허용해 주세요."; revision++ }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        setContent {
            SenseTouchTheme(window) { Screen(revision) }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.removeCallbacks(refresh)
        refreshHandler.post(refresh)
        revision++
        refreshFingerprints()
        working = LaunchEngine.busy.get()
        if (LaunchEngine.pending(this) && !working && LaunchEngine.allowed(this)) {
            runTask { LaunchEngine.recover(this, it) }
        }
    }
    override fun onPause() {
        refreshHandler.removeCallbacks(refresh)
        super.onPause()
    }
    override fun onDestroy() {
        queryGeneration++
        queryHandler.removeCallbacksAndMessages(null)
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        super.onDestroy()
    }
    private fun runTask(task: ((String) -> Unit) -> Unit) {
        working = true
        val owner = java.lang.ref.WeakReference(this)
        task { result ->
            owner.get()?.takeUnless { it.isDestroyed }?.let {
                it.working = LaunchEngine.busy.get()
                it.message = result
                it.revision++
            }
        }
    }
    private fun open(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure { message = "화면을 열 수 없습니다. ${it.javaClass.simpleName}" }
    }
    private fun setup() {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            packageManager.getLaunchIntentForPackage(DeviceProfile.SHIZUKU)?.let(::open)
                ?: open(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")))
            return
        }
        if (runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false))
            runTask { LaunchEngine.grant(this, it) }
        else {
            requesting = true
            runCatching { Shizuku.requestPermission(101) }.onFailure {
                requesting = false; message = "권한 요청 실패: ${it.javaClass.simpleName}"
            }
        }
    }
    @Composable
    private fun Screen(tick: Int) {
        val choices = if (manualSelection) (1..4).map { RegisteredFingerprint(it, "지문 $it") } else fingerprints
        val selected = choices.firstOrNull { it.id == selectedId }
        var about by remember { mutableStateOf(false) }
        var consent by remember { mutableStateOf(false) }
        val installed = remember(tick) { runCatching { packageManager.getPackageInfo(DeviceProfile.SHIZUKU, 0) }.isSuccess }
        val connected = remember(tick) { runCatching { Shizuku.pingBinder() }.getOrDefault(false) }
        val shizukuAllowed = remember(tick) { connected && runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false) }
        val granted = remember(tick) { LaunchEngine.allowed(this) }
        val supported = remember(tick) { DeviceProfile.supported(this) }
        val pending = remember(tick) { LaunchEngine.pending(this) }
        val canLaunch = supported && granted && !pending && !working && !loadingFingerprints && selected != null
        val colors = MaterialTheme.colorScheme
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SenseTouch", fontSize = 30.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { about = true }) { Text("앱 정보", fontSize = 14.sp) }
                    }
                    Text("지문 인식률 향상", fontSize = 15.sp, color = colors.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("연결 및 권한")
                    GroupCard {
                        StatusRow("Shizuku", when { !installed -> "미설치"; !connected -> "미실행"; !shizukuAllowed -> "권한 필요"; else -> "연결됨" }, connected && shizukuAllowed)
                        InsetDivider()
                        StatusRow("시스템 설정 권한", if (granted) "허용됨" else "설정 필요", granted)
                        if (!granted) {
                            InsetDivider()
                            LinkRow(when { requesting -> "권한 응답 대기 중"; !installed -> "Shizuku 설치 안내"; !connected -> "Shizuku 열기"; else -> "권한 설정하기" }, !working && !requesting, ::setup)
                        }
                    }
                    if (granted && !connected) Footnote("초기 설정이 완료되어 바로 사용할 수 있습니다.")
                }
                if (!supported) NoticeCard("지원하지 않는 기기", "삼성 지문 인식률 향상 화면을 찾지 못했습니다.", error = true)
                if (pending) GroupCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("설정 복원이 필요해요", fontWeight = FontWeight.SemiBold, color = colors.error)
                        Footnote("기존 Assistant 설정을 복원한 뒤 다시 실행할 수 있습니다.")
                    }
                    InsetDivider()
                    LinkRow("복원 재시도", granted && !working) { runTask { LaunchEngine.recover(this@MainActivity, it) } }
                    InsetDivider()
                    LinkRow("Assistant 설정 열기") { open(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (manualSelection) "지문 번호 선택" else "등록된 지문", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = ::refreshFingerprints, enabled = !working && !loadingFingerprints) { Text(if (loadingFingerprints) "조회 중…" else "새로고침", fontSize = 14.sp) }
                    }
                    when {
                        loadingFingerprints && choices.isEmpty() -> GroupCard {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("지문을 불러오는 중", color = colors.onSurfaceVariant, fontSize = 15.sp)
                            }
                        }
                        manualSelection -> Footnote("삼성 설정에 등록된 지문 번호를 직접 선택해 주세요.")
                        fingerprintError != null -> GroupCard {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("자동 조회를 완료하지 못했어요", fontWeight = FontWeight.SemiBold)
                                Footnote(fingerprintError!!)
                            }
                            InsetDivider()
                            LinkRow("번호로 직접 선택", !working) { manualSelection = true; selectedId = 1 }
                        }
                        fingerprints.isEmpty() -> NoticeCard("등록된 지문이 없어요", "기기 보안 설정에서 지문을 먼저 등록해 주세요.")
                        else -> Footnote("인식률을 높일 지문을 선택해 주세요.")
                    }
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (row in choices.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (finger in row) key(finger.id) {
                                FingerprintTile(finger.name, selectedId == finger.id, !working && !loadingFingerprints, Modifier.weight(1f)) { selectedId = finger.id }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                GroupCard { LinkRow("기기 보안 설정") { open(Intent(Settings.ACTION_SECURITY_SETTINGS)) } }
                if (message.isNotBlank()) GroupCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("안내", fontWeight = FontWeight.SemiBold)
                        Text(message, fontSize = 14.sp, lineHeight = 21.sp, color = colors.onSurfaceVariant)
                    }
                    InsetDivider()
                    LinkRow("확인") { message = "" }
                }
                Spacer(Modifier.height(4.dp))
            }
            Surface(color = colors.background) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction(if (working) "처리 중…" else "지문 인식률 향상", canLaunch) { consent = true }
                    Text(selected?.name?.let { "$it 선택됨" } ?: "지문을 선택해 주세요", fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
        if (consent) IosDialog(onDismiss = { consent = false }) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("지문 인식률 향상", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text("삼성 지문 인식률 향상 화면을 엽니다. Assistant 설정은 잠시 변경한 뒤 복원하며, 시스템 인증은 기기에서 직접 진행해 주세요.", fontSize = 14.sp, lineHeight = 21.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = colors.onSurfaceVariant)
                Text("지원 여부와 효과는 기기·OS에 따라 다릅니다.", fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = colors.onSurfaceVariant)
            }
            HorizontalDivider(color = colors.outlineVariant, thickness = 0.5.dp)
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { consent = false }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("취소", fontSize = 16.sp) }
                TextButton(onClick = {
                    consent = false
                    selected?.let { finger -> runTask { LaunchEngine.launch(this@MainActivity, finger.id, !manualSelection, it) } }
                }, enabled = canLaunch, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("계속", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (about) IosDialog(onDismiss = { about = false }) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { about = false }) { Text("완료", fontWeight = FontWeight.SemiBold) } }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.primary, modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_fingerprint), null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                    }
                    Text("SenseTouch", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("${BuildConfig.VERSION_NAME} · Adam", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                Text("삼성 Galaxy의 지문 인식률 향상 기능을 간편하게 사용할 수 있는 앱입니다.", fontSize = 14.sp, lineHeight = 21.sp, color = colors.onSurfaceVariant)
                HorizontalDivider(color = colors.outlineVariant, thickness = 0.5.dp)
                Text(DeviceProfile.details(), fontSize = 14.sp, lineHeight = 22.sp)
                PrimaryAction("GitHub 바로가기", icon = R.drawable.ic_github) { open(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Adam-yam"))) }
                Footnote("인터넷 권한 없이 동작합니다. 지문 이름과 ID는 표시·선택에만 사용하며 저장하거나 전송하지 않습니다. 지문 이미지와 생체 템플릿은 읽지 않습니다. 외부 링크는 브라우저에서 열립니다.")
                Footnote("GPL-3.0-only\nFingerprintAccuracyEnhancer 및 Root Activity Launcher 기반. Shizuku API (MIT), HiddenApiBypass 및 AndroidX (Apache-2.0).")
            }
        }
    }
}
