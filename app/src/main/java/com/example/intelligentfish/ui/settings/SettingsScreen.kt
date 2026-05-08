package com.example.intelligentfish.ui.settings

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
import com.example.intelligentfish.data.model.ThresholdSet
import com.example.intelligentfish.ui.theme.*
import com.example.intelligentfish.viewmodel.FishTankViewModel

@Composable
fun SettingsScreen(viewModel: FishTankViewModel) {
    val thresholdSet by viewModel.thresholdSet.collectAsStateWithLifecycle()
    val feedIntervalSeconds by viewModel.feedIntervalSeconds.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text("阈值设置", style = MaterialTheme.typography.titleMedium, color = BluePrimary)
        }

        // 温度阈值
        item {
            ThresholdCard(
                icon = "🌡",
                title = "水温阈值",
                unit = "°C",
                minValue = thresholdSet.tempLower,
                maxValue = thresholdSet.tempUpper,
                onMinMinus = { viewModel.adjustTempLower(-1f) },
                onMinPlus = { viewModel.adjustTempLower(1f) },
                onMaxMinus = { viewModel.adjustTempUpper(-1f) },
                onMaxPlus = { viewModel.adjustTempUpper(1f) },
                showRange = true,
                formatValue = { "%.1f".format(it) }
            )
        }

        // pH阈值
        item {
            ThresholdCard(
                icon = "🧪",
                title = "pH值阈值",
                unit = "",
                minValue = thresholdSet.phLower,
                maxValue = thresholdSet.phUpper,
                onMinMinus = { viewModel.adjustPhLower(-0.5f) },
                onMinPlus = { viewModel.adjustPhLower(0.5f) },
                onMaxMinus = { viewModel.adjustPhUpper(-0.5f) },
                onMaxPlus = { viewModel.adjustPhUpper(0.5f) },
                showRange = true,
                formatValue = { "%.1f".format(it) }
            )
        }

        // 水位阈值
        item {
            ThresholdCardInt(
                icon = "💧",
                title = "水位阈值",
                unit = "%",
                minValue = thresholdSet.waterLevelMin,
                maxValue = thresholdSet.waterLevelMax,
                onMinMinus = { viewModel.adjustWaterLevelMin(-5) },
                onMinPlus = { viewModel.adjustWaterLevelMin(5) },
                onMaxMinus = { viewModel.adjustWaterLevelMax(-5) },
                onMaxPlus = { viewModel.adjustWaterLevelMax(5) }
            )
        }

        // 溶氧量阈值
        item {
            ThresholdCardInt(
                icon = "🌬",
                title = "溶氧量下限",
                unit = "%",
                maxValue = thresholdSet.oxygenLevelMax,
                onMaxMinus = { viewModel.adjustOxygenLevelMax(-1) },
                onMaxPlus = { viewModel.adjustOxygenLevelMax(1) },
                showRange = false
            )
        }

        // 喂食间隔
        item {
            FeedIntervalCard(
                intervalSeconds = feedIntervalSeconds,
                onMinus = { viewModel.setFeedInterval(feedIntervalSeconds - 30) },
                onPlus = { viewModel.setFeedInterval(feedIntervalSeconds + 30) }
            )
        }

        // 同步按钮
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { viewModel.syncThresholds() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("📤 同步到设备", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ThresholdCard(
    icon: String,
    title: String,
    unit: String,
    minValue: Float = 0f,
    maxValue: Float = 0f,
    onMinMinus: () -> Unit = {},
    onMinPlus: () -> Unit = {},
    onMaxMinus: () -> Unit = {},
    onMaxPlus: () -> Unit = {},
    showRange: Boolean = true,
    formatValue: (Float) -> String = { "%.0f".format(it) }
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (showRange) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("最低", color = Color.Gray, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdjustButton("-", onMinMinus)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "${formatValue(minValue)} $unit",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        AdjustButton("+", onMinPlus)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (showRange) "最高" else "上限", color = Color.Gray, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdjustButton("-", onMaxMinus)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${formatValue(maxValue)} $unit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AdjustButton("+", onMaxPlus)
                }
            }
        }
    }
}

@Composable
private fun ThresholdCardInt(
    icon: String,
    title: String,
    unit: String,
    minValue: Int = 0,
    maxValue: Int = 0,
    onMinMinus: () -> Unit = {},
    onMinPlus: () -> Unit = {},
    onMaxMinus: () -> Unit = {},
    onMaxPlus: () -> Unit = {},
    showRange: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (showRange) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("最低", color = Color.Gray, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdjustButton("-", onMinMinus)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "$minValue $unit",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        AdjustButton("+", onMinPlus)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (showRange) "最高" else "上限", color = Color.Gray, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdjustButton("-", onMaxMinus)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "$maxValue $unit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AdjustButton("+", onMaxPlus)
                }
            }
        }
    }
}

@Composable
private fun FeedIntervalCard(
    intervalSeconds: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val h = intervalSeconds / 3600
    val m = (intervalSeconds % 3600) / 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🐟", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("喂食间隔", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (m > 0) "每 ${h}h${m}min" else "每 ${h}h",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdjustButton("-", onMinus)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "$intervalSeconds",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Text(" 秒", fontSize = 14.sp, color = BluePrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    AdjustButton("+", onPlus)
                }
            }
        }
    }
}

@Composable
private fun AdjustButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimaryLight.copy(alpha = 0.15f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
