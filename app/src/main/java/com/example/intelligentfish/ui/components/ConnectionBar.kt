package com.example.intelligentfish.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.intelligentfish.data.net.WifiScanner
import com.example.intelligentfish.ui.theme.*

@Composable
fun ConnectionBar(
    connected: Boolean,
    isScanning: Boolean,
    wifiDevices: List<WifiScanner.WifiDevice>,
    onScan: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDirectConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (connected) StatusOnline else StatusOffline,
                    modifier = Modifier.size(10.dp)
                ) {}

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (connected) "已连接" else "未连接",
                    color = if (connected) StatusOnline else StatusOffline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = if (connected) onDisconnect else onScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connected) StatusOffline else BluePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    enabled = !isScanning
                ) {
                    Text(
                        if (connected) "断开" else if (isScanning) "扫描中..." else "扫描设备"
                    )
                }
            }

            // Direct connect button (user already connected to hotspot manually)
            if (!connected) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDirectConnect,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("已连上热点？点击直连设备")
                }
            }

            // Scan results
            if (!connected && wifiDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BluePrimary.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("发现设备：", style = MaterialTheme.typography.bodySmall, color = BluePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                wifiDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnectDevice(device.ssid) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\uD83D\uDCE1", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.ssid, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "信号: ${"\u2605".repeat(device.signalLevel)}${"\u2606".repeat(5 - device.signalLevel)}",
                                fontSize = 11.sp, color = Color.Gray
                            )
                        }
                        Text("连接", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (!connected && wifiDevices.isEmpty() && !isScanning) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "点击「扫描设备」搜索附近鱼缸热点",
                    color = Color.Gray, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
                )
            }
        }
    }
}
