package com.example.intelligentfish.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.intelligentfish.ui.theme.*

data class FrameLog(
    val timestamp: String,
    val direction: String, // "RX" or "TX"
    val hexData: String    // Full hex string
)

@Composable
fun TestScreen(
    connected: Boolean,
    frameLogs: List<FrameLog>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendHex: (String) -> Unit,
    onSendEcho: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hexInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // ==================== Top: Connection Bar ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Protocol badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BluePrimary.copy(alpha = 0.1f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        "TCP",
                        color = BluePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // IP:Port
                Text(
                    "192.168.4.1:8080",
                    color = Color(0xFF333333),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )

                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (connected) StatusOnline else StatusOffline,
                            shape = RoundedCornerShape(50)
                        )
                )

                Spacer(Modifier.width(8.dp))

                // Connect/Disconnect button
                Button(
                    onClick = if (connected) onDisconnect else onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connected) StatusOffline else BluePrimary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (connected) "断开" else "连接",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFFE0E0E0))

        // ==================== Middle: Receive Log Area ====================
        val listState = rememberLazyListState()

        if (frameLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📡", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击「连接」开始通信",
                        color = Color(0xFF999999),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (connected) "已连接，等待数据..." else "未连接",
                        color = if (connected) StatusOnline else Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
                    .padding(horizontal = 6.dp)
            ) {
                items(frameLogs) { log ->
                    LogItem(log)
                }
            }
            LaunchedEffect(frameLogs.size) {
                if (frameLogs.isNotEmpty()) {
                    listState.animateScrollToItem(frameLogs.size - 1)
                }
            }
        }

        HorizontalDivider(color = Color(0xFFE0E0E0))

        // ==================== Bottom: Send Area ====================
        Surface(
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Quick action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickButton(
                        text = "Echo",
                        onClick = onSendEcho,
                        enabled = connected,
                        modifier = Modifier.weight(1f),
                        bgColor = BluePrimary
                    )
                    QuickButton(
                        text = "清空",
                        onClick = onClearLogs,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        bgColor = Color(0xFFE0E0E0),
                        textColor = Color(0xFF666666)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // HEX input + send
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("AA 10 00 10 55", fontSize = 13.sp, color = Color(0xFFBBBBBB))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color(0xFFDDDDDD)
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSendHex(hexInput)
                            hexInput = ""
                        },
                        enabled = connected && hexInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary,
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text("发送", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: FrameLog) {
    val isRx = log.direction == "RX"
    val dirColor = if (isRx) Color(0xFF2E7D32) else Color(0xFFE65100)
    val dirArrow = if (isRx) "收←" else "发→"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            log.timestamp,
            color = Color(0xFFAAAAAA),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(52.dp)
        )

        Text(
            dirArrow,
            color = dirColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )

        Text(
            log.hexData,
            color = Color(0xFF333333),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    bgColor: Color = BluePrimary,
    textColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            disabledContainerColor = Color(0xFFEEEEEE)
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(text, fontSize = 12.sp, color = if (enabled) textColor else Color(0xFF999999))
    }
}
