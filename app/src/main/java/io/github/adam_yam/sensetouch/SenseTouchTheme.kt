package io.github.adam_yam.sensetouch

import android.view.Window
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat

private val Accent = Color(0xFF3B82F6)

@Composable
internal fun SenseTouchTheme(window: Window, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme(
        primary = Accent, onPrimary = Color.White,
        primaryContainer = Color(0xFF132544), onPrimaryContainer = Color(0xFFB8D4FF),
        background = Color(0xFF000000), onBackground = Color(0xFFF5F5F7),
        surface = Color(0xFF1C1C1E), onSurface = Color(0xFFF5F5F7),
        onSurfaceVariant = Color(0xFFA1A1A9), outlineVariant = Color(0xFF363638),
        error = Color(0xFFFF6961)
    ) else lightColorScheme(
        primary = Accent, onPrimary = Color.White,
        primaryContainer = Color(0xFFEFF5FF), onPrimaryContainer = Color(0xFF1D4ED8),
        background = Color(0xFFF2F2F7), onBackground = Color(0xFF1C1C1E),
        surface = Color.White, onSurface = Color(0xFF1C1C1E),
        onSurfaceVariant = Color(0xFF6C6C74), outlineVariant = Color(0xFFE5E5EA),
        error = Color(0xFFBC3029)
    )
    SideEffect {
        window.statusBarColor = colors.background.toArgb()
        window.navigationBarColor = colors.background.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = colors, typography = Typography(
        bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    )) { Surface(Modifier.fillMaxSize(), color = colors.background, content = content) }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, modifier = Modifier.padding(start = 16.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun Footnote(text: String) {
    Text(text, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) { Column(content = content) }
}

@Composable
internal fun InsetDivider() {
    HorizontalDivider(Modifier.padding(start = 18.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun StatusRow(label: String, value: String, ready: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun LinkRow(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(0.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Icon(painterResource(R.drawable.ic_chevron), null, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun NoticeCard(title: String, text: String, error: Boolean = false) {
    GroupCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Footnote(text)
        }
    }
}

@Composable
internal fun FingerprintTile(name: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier.clip(RoundedCornerShape(20.dp)).selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(20.dp), color = if (selected) colors.primaryContainer else colors.surface,
        border = if (selected) BorderStroke(1.5.dp, colors.primary) else null) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(painterResource(R.drawable.ic_fingerprint), null, tint = if (enabled) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(34.dp))
                if (selected) Surface(shape = RoundedCornerShape(50), color = colors.primary, modifier = Modifier.size(20.dp)) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.padding(3.dp))
                } else Spacer(Modifier.size(20.dp))
            }
            Text(name, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun PrimaryAction(label: String, enabled: Boolean = true, icon: Int? = null, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun IosDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) { Column(content = content) }
    }
}
