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
import java.text.SimpleDateFormat
import java.util.*

data class FrameLog(
    val timestamp: String,
    val direction: String, // "RX" or "TX"
    val cmd: Byte,
    val rawHex: String,
    val payloadHex: String
)

@Composable
fun TestScreen(
    connected: Boolean,
    frameLogs: List<FrameLog>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendHex: (String) -> Unit,
    onSendEcho: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hexInput by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Connection bar
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (connected) StatusOnline else StatusOffline,
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (connected) "TCP Connected" else "Disconnected",
                    color = if (connected) StatusOnline else StatusOffline,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = if (connected) onDisconnect else onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connected) StatusOffline else BluePrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (connected) "Disconnect" else "Connect")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Frame log area
        Text("Frame Log", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
        Spacer(Modifier.height(4.dp))

        val listState = rememberLazyListState()
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            if (frameLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No frames yet", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                ) {
                    items(frameLogs) { log ->
                        FrameLogItem(log)
                        HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)
                    }
                }
                // Auto-scroll to bottom
                LaunchedEffect(frameLogs.size) {
                    if (frameLogs.isNotEmpty()) {
                        listState.animateScrollToItem(frameLogs.size - 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Send area
        Text("Send", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hexInput,
                onValueChange = { hexInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("AA 10 00 10 55", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    onSendHex(hexInput)
                    hexInput = ""
                },
                enabled = connected && hexInput.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Send")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Quick send buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSendEcho,
                enabled = connected,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("Echo (0x10)", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FrameLogItem(log: FrameLog) {
    val dirColor = if (log.direction == "RX") Color(0xFF4CAF50) else Color(0xFFFF9800)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(log.timestamp, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(60.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            log.direction,
            color = dirColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "CMD=0x%02X".format(log.cmd.toInt() and 0xFF),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            log.payloadHex,
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
