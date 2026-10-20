package com.homi.oslauncher

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

class SystemPanelActivity : ComponentActivity() {
    private val audio by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
        val startBrightness = runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS).toFloat() / 255f
        }.getOrDefault(0.5f)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF54D8FF),
                background = Color(0xFF080D18),
                surface = Color(0xFF151E30)
            )) {
                SystemPanel(
                    initialVolume = startVolume,
                    initialBrightness = startBrightness,
                    onVolume = { value ->
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC,
                            (value * maxVolume).toInt().coerceIn(0, maxVolume), 0)
                    },
                    onBrightness = { value ->
                        if (!Settings.System.canWrite(this)) {
                            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = android.net.Uri.parse("package:$packageName")
                            })
                        } else {
                            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS,
                                (value * 255).toInt().coerceIn(1, 255))
                        }
                    },
                    onWifiSettings = { openSettings(Settings.ACTION_WIFI_SETTINGS) },
                    onBluetoothSettings = { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) },
                    onNotificationPermission = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onBack = { finish() },
                    notificationJson = getSharedPreferences("homi_notifications", MODE_PRIVATE)
                        .getString("items", "[]").orEmpty()
                )
            }
        }
    }

    private fun openSettings(action: String) {
        runCatching { startActivity(Intent(action)) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}

private data class PanelNotification(
    val key: String, val packageName: String, val title: String, val text: String
)

@Composable
private fun SystemPanel(
    initialVolume: Float,
    initialBrightness: Float,
    onVolume: (Float) -> Unit,
    onBrightness: (Float) -> Unit,
    onWifiSettings: () -> Unit,
    onBluetoothSettings: () -> Unit,
    onNotificationPermission: () -> Unit,
    onBack: () -> Unit,
    notificationJson: String
) {
    var volume by remember { mutableFloatStateOf(initialVolume.coerceIn(0f, 1f)) }
    var brightness by remember { mutableFloatStateOf(initialBrightness.coerceIn(0f, 1f)) }
    val notifications = remember(notificationJson) {
        mutableStateListOf<PanelNotification>().apply {
            runCatching {
                val array = JSONArray(notificationJson)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(PanelNotification(item.optString("key"), item.optString("package"),
                        item.optString("title"), item.optString("text")))
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF080D18)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("HOMI CONTROL", color = Color(0xFF54D8FF), fontSize = 13.sp)
                        Text("لوحة النظام", color = Color.White, fontSize = 28.sp)
                        Text("تحكم سريع بخدمات أندرويد", color = Color(0xFF9BAAC3), fontSize = 13.sp)
                    }
                    Button(onClick = onBack) { Text("إغلاق") }
                }
            }
            item {
                PanelCard(title = "السطوع", subtitle = "يتطلب السماح بتعديل إعدادات النظام") {
                    Slider(value = brightness, onValueChange = {
                        brightness = it
                        onBrightness(it)
                    })
                    Text("${(brightness * 100).toInt()}%", color = Color(0xFFB8C7DF))
                }
            }
            item {
                PanelCard(title = "صوت الوسائط", subtitle = "مستوى صوت تشغيل الوسائط") {
                    Slider(value = volume, onValueChange = {
                        volume = it
                        onVolume(it)
                    })
                    Text("${(volume * 100).toInt()}%", color = Color(0xFFB8C7DF))
                }
            }
            item {
                PanelCard(title = "الاتصالات", subtitle = "تفتح لوحات أندرويد الرسمية") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onWifiSettings, modifier = Modifier.weight(1f)) { Text("Wi-Fi") }
                        Button(onClick = onBluetoothSettings, modifier = Modifier.weight(1f)) { Text("Bluetooth") }
                    }
                }
            }
            item {
                PanelCard(title = "الإشعارات", subtitle = "فعّل وصول الإشعارات لعرض المحتوى الحقيقي") {
                    Button(onClick = onNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("منح صلاحية الإشعارات")
                    }
                }
            }
            item { Text("الإشعارات الأخيرة (${notifications.size})", color = Color.White, fontSize = 20.sp) }
            if (notifications.isEmpty()) {
                item {
                    Text("لا توجد إشعارات محفوظة بعد. فعّل صلاحية الوصول ثم أعد فتح لوحة النظام.",
                        color = Color(0xFF9BAAC3))
                }
            } else {
                items(notifications, key = { it.key }) { item ->
                    PanelCard(title = item.title.ifBlank { item.packageName }, subtitle = item.packageName) {
                        if (item.text.isNotBlank()) Text(item.text, color = Color(0xFFD7E2F3))
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text("ملاحظة: أندرويد لا يسمح للتطبيقات العادية بتبديل Wi-Fi أو Bluetooth مباشرة في الإصدارات الحديثة؛ لذلك تُفتح إعدادات النظام.",
                    color = Color(0xFF7F90AA), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PanelCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = Color(0xFF151E30)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White, fontSize = 17.sp)
            Text(subtitle, color = Color(0xFF9BAAC3), fontSize = 12.sp)
            content()
        }
    }
}
