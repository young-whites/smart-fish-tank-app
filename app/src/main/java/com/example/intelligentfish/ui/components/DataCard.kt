package com.example.intelligentfish.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DataCard(
    icon: String,
    label: String,
    value: String,
    unit: String,
    progress: Float,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors), shape)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // 图标
            Text(
                text = icon,
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 数值
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            // 单位 + 标签
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            // 迷你进度条
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )
        }
    }
}
