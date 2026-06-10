package com.foodfest.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodfest.app.theme.AppColors

/**
 * Reusable image picker surface. The caller owns file picking and upload state.
 */
@Composable
fun FoodFestImagePickerCard(
    title: String,
    previewUrl: String?,
    selectedFileName: String?,
    helperText: String?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Orange.copy(alpha = 0.08f))
                    .height(52.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(14.dp))
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = AppColors.Orange
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = title,
                        color = AppColors.Brown,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (previewUrl != null) {
                    IconButton(onClick = onClearImage, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa ảnh",
                            tint = AppColors.Error
                        )
                    }
                }
            }

            if (previewUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    AppImage(
                        url = previewUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                selectedFileName?.let { fileName ->
                    Text(
                        text = fileName,
                        color = AppColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!helperText.isNullOrBlank()) {
                    Text(
                        text = helperText,
                        color = if (helperText.startsWith("✅")) Color(0xFF2E7D32) else AppColors.GrayPlaceholder,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            TextButton(
                onClick = onPickImage,
                enabled = enabled,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (previewUrl == null) "Chọn ảnh từ thiết bị" else "Chọn ảnh khác",
                    color = if (enabled) AppColors.Orange else AppColors.GrayPlaceholder,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
