package com.example.intelligentfish.ui.control

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.intelligentfish.data.model.DeviceStatus
import com.example.intelligentfish.data.model.SensorData
import com.example.intelligentfish.data.model.WorkMode
import com.example.intelligentfish.ui.theme.*
import com.example.intelligentfish.viewmodel.FishTankViewModel

@Composable
fun ControlScreen(viewModel: FishTankViewModel) {
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentMode = if (sensorData.runMode == 1) WorkMode.MANUAL else WorkMode.AUTO

    fun checkConnected(action: () -> Unit) {
        if (connected) action()
        else android.widget.Toast.makeText(context, "请先连接设备", android.widget.Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 模式切换（无需连接，本地状态直接生效）
        item {
            ModeToggleCard(
                currentMode = currentMode,
                connected = true,
                onModeChange = { viewModel.switchMode(it) }
            )
        }

        // 继电器控制
        item {
            Text("设备控制", style = MaterialTheme.typography.titleMedium, color = BluePrimary)
        }

        item {
            RelayControlCard(
                icon = "🔥",
                name = "加热棒",
                isOn = deviceStatus.relayHeat,
                enabled = connected && currentMode == WorkMode.MANUAL,
                onToggle = { checkConnected { viewModel.controlRelay(0, !deviceStatus.relayHeat) } }
            )
        }
        item {
            RelayControlCard(
                icon = "💦",
                name = "加水阀",
                isOn = deviceStatus.relayFill,
                enabled = connected && currentMode == WorkMode.MANUAL,
                onToggle = { checkConnected { viewModel.controlRelay(1, !deviceStatus.relayFill) }
            }
            )
        }
        item {
            RelayControlCard(
                icon = "🚰",
                name = "排水阀",
                isOn = deviceStatus.relayDrain,
                enabled = connected && currentMode == WorkMode.MANUAL,
                onToggle = { checkConnected { viewModel.controlRelay(2, !deviceStatus.relayDrain) } }
            )
        }
        item {
            RelayControlCard(
                icon = "🫧",
                name = "增氧泵",
                isOn = deviceStatus.relayOxygen,
                enabled = connected && currentMode == WorkMode.MANUAL,
                onToggle = { checkConnected { viewModel.controlRelay(3, !deviceStatus.relayOxygen) } }
            )
        }

        // 喂食触发
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { checkConnected { viewModel.triggerFeed() } },
                enabled = connected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningYellow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "🐟 立即喂食",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimaryDark
                )
            }
        }

        // 报警开关
        item {
            AlarmToggleCard(
                enabled = deviceStatus.alarmEnable,
                connected = connected,
                onToggle = { checkConnected { viewModel.setAlarm(!deviceStatus.alarmEnable) } }
            )
        }
    }
}

@Composable
private fun ModeToggleCard(
    currentMode: WorkMode,
    connected: Boolean,
    onModeChange: (WorkMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("运行模式", style = MaterialTheme.typography.titleSmall, color = BluePrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeButton(
                    label = "🔄 自动模式",
                    selected = currentMode == WorkMode.AUTO,
                    enabled = connected,
                    onClick = { onModeChange(WorkMode.AUTO) },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    label = "🎛 手动模式",
                    selected = currentMode == WorkMode.MANUAL,
                    enabled = connected,
                    onClick = { onModeChange(WorkMode.MANUAL) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors(containerColor = BluePrimary)
        } else {
            ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        },
        border = if (!selected) BorderStroke(1.5.dp, BluePrimaryLight) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else BluePrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun RelayControlCard(
    icon: String,
    name: String,
    isOn: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onToggle,
                enabled = enabled,
                colors = if (isOn) {
                    ButtonDefaults.buttonColors(containerColor = StatusOnline)
                } else {
                    ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f))
                },
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    if (isOn) "已开启" else "已关闭",
                    color = if (isOn) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AlarmToggleCard(
    enabled: Boolean,
    connected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔔", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("报警功能", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            Switch(
                checked = enabled,
                enabled = connected,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StatusOnline,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray
                )
            )
        }
    }
}
