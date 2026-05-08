package com.example.intelligentfish.ui.dashboard

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
import com.example.intelligentfish.ui.components.ConnectionBar
import com.example.intelligentfish.ui.components.DataCard
import com.example.intelligentfish.ui.theme.*
import com.example.intelligentfish.viewmodel.FishTankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FishTankViewModel) {
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
    val thresholdSet by viewModel.thresholdSet.collectAsStateWithLifecycle()
    val wifiDevices by viewModel.wifiDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 连接栏
        item {
            ConnectionBar(
                connected = connected,
                isScanning = isScanning,
                wifiDevices = wifiDevices,
                onScan = { viewModel.scanWifiDevices() },
                onConnectDevice = { ssid -> viewModel.connectViaHotspot(ssid) },
                onDirectConnect = { viewModel.directConnect() },
                onDisconnect = { viewModel.disconnect() }
            )
        }

        // 模式指示卡片
        item {
            val mode = if (sensorData.runMode == 1) WorkMode.MANUAL else WorkMode.AUTO
            ModeIndicatorCard(mode)
        }

        // 四宫格数据卡片
        item {
            SensorGrid(sensorData = sensorData)
        }

        // 设备状态行
        item {
            DeviceStatusRow(deviceStatus)
        }

        // 喂食倒计时
        item {
            FeedCountdownCard(
                countdown = sensorData.feedCountdown,
                onQuickFeed = { viewModel.triggerFeed() }
            )
        }
    }
}

@Composable
private fun ModeIndicatorCard(mode: WorkMode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mode == WorkMode.AUTO) BluePrimary else CyanAccent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (mode == WorkMode.AUTO) "🔄" else "🎛",
                fontSize = 28.sp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "当前模式",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                )
                Text(
                    text = mode.label + "模式",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SensorGrid(sensorData: SensorData) {
    val data = sensorData

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DataCard(
                icon = "🌡",
                label = "水温",
                value = "%.1f".format(data.waterTemp),
                unit = "°C",
                progress = ((data.waterTemp - 15f) / 35f).coerceIn(0f, 1f),
                gradientColors = listOf(TempGradientStart, TempGradientEnd),
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )
            DataCard(
                icon = "🧪",
                label = "pH值",
                value = "%.1f".format(data.phValue),
                unit = "pH",
                progress = (data.phValue / 14f).coerceIn(0f, 1f),
                gradientColors = listOf(PhGradientStart, PhGradientEnd),
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DataCard(
                icon = "💧",
                label = "水位",
                value = "${data.waterLevel}",
                unit = "%",
                progress = (data.waterLevel / 100f).coerceIn(0f, 1f),
                gradientColors = listOf(WaterGradientStart, WaterGradientEnd),
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )
            DataCard(
                icon = "🌬",
                label = "溶氧量",
                value = "${data.oxygenLevel}",
                unit = "%",
                progress = (data.oxygenLevel / 20f).coerceIn(0f, 1f),
                gradientColors = listOf(AirGradientStart, AirGradientEnd),
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )
        }
    }
}

@Composable
private fun DeviceStatusRow(status: DeviceStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "设备状态",
                style = MaterialTheme.typography.titleSmall,
                color = BluePrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RelayChip("加热", "🔥", status.relayHeat)
                RelayChip("加水", "💦", status.relayFill)
                RelayChip("排水", "🚰", status.relayDrain)
                RelayChip("增氧", "🫧", status.relayOxygen)
            }
        }
    }
}

@Composable
private fun RelayChip(name: String, icon: String, isOn: Boolean) {
    val bgColor = if (isOn) StatusOnline.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f)
    val textColor = if (isOn) StatusOnline else Color.Gray

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun FeedCountdownCard(countdown: Int, onQuickFeed: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🐟 下次喂食",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BluePrimary,
                )
                Text(
                    text = if (countdown > 0) {
                        val h = countdown / 3600
                        val min = (countdown % 3600) / 60
                        val sec = countdown % 60
                        buildString {
                            if (h > 0) append("${h}时")
                            if (min > 0) append("${min}分")
                            append("${sec}秒")
                        }
                    } else "待定",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimaryDark,
                )
            }

            Button(
                onClick = onQuickFeed,
                colors = ButtonDefaults.buttonColors(containerColor = WarningYellow),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("🐟 快速喂食", color = BluePrimaryDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}
