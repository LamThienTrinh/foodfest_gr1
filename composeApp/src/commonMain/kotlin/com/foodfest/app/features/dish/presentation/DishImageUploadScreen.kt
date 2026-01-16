package com.foodfest.app.features.dish.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.foodfest.app.features.dish.data.Dish
import com.foodfest.app.features.dish.data.DishRepository
import com.foodfest.app.theme.AppColors
import com.foodfest.app.utils.readBytesFromPath
import com.foodfest.app.utils.resizeImage
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.toBase64(): String {
    return Base64.encode(this)
}

@Composable
fun DishImageUploadScreen(
    onBack: () -> Unit = {}
) {
    val repo = remember { DishRepository() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var dishes by remember { mutableStateOf<List<Dish>>(emptyList()) }
    var isLoadingDishes by remember { mutableStateOf(false) }
    var selectedDish by remember { mutableStateOf<Dish?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var showFilePicker by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }

    suspend fun loadPage(page: Int, keepMessage: Boolean = false) {
        isLoadingDishes = true
        if (!keepMessage) {
            message = null
        }
        selectedDish = null
        selectedImageBytes = null
        selectedFileName = null
        repo.getDishes(page = page)
            .onSuccess { resp ->
                dishes = resp.data
                currentPage = resp.page
                totalPages = max(1, (resp.total + resp.limit - 1) / resp.limit)
            }
            .onFailure { 
                if (!keepMessage) {
                    message = it.message 
                }
            }
        isLoadingDishes = false
    }

    LaunchedEffect(currentPage) {
        loadPage(currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Upload ảnh món ăn",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.Brown
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Chọn món và chọn ảnh từ thiết bị. Ảnh sẽ được upload lên Cloudinary và lưu vào DB.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.GrayPlaceholder
        )

        Spacer(Modifier.height(16.dp))

        if (isLoadingDishes) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Danh sách món (trang $currentPage/$totalPages)", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        if (currentPage > 1) currentPage -= 1
                    }, enabled = currentPage > 1) {
                        Text("Trang trước")
                    }
                    TextButton(onClick = {
                        if (currentPage < totalPages) currentPage += 1
                    }, enabled = currentPage < totalPages) {
                        Text("Trang sau")
                    }
                    TextButton(onClick = {
                        scope.launch { loadPage(currentPage) }
                    }) {
                        Text("Tải lại")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (dishes.isEmpty()) {
                Text(text = "Chưa có món ăn", color = AppColors.GrayPlaceholder)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dishes.forEach { dish ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDish = dish
                                    message = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDish?.id == dish.id) AppColors.Orange.copy(alpha = 0.15f) else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${dish.id}: ${dish.name}", fontWeight = FontWeight.SemiBold)
                                dish.imageUrl?.let {
                                    Text(text = "Ảnh hiện tại: $it", style = MaterialTheme.typography.bodySmall, color = AppColors.GrayPlaceholder)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // File Picker
        FilePicker(
            show = showFilePicker,
            fileExtensions = listOf("jpg", "jpeg", "png", "gif", "webp")
        ) { mpFile ->
            showFilePicker = false
            
            if (mpFile != null) {
                scope.launch {
                    try {
                        message = "Đang xử lý ảnh..."
                        
                        // Đọc bytes từ file - sử dụng readBytesFromPath để hỗ trợ content:// URI
                        val originalBytes = readBytesFromPath(mpFile.path)
                        val originalSizeKB = originalBytes.size / 1024
                        
                        // Resize ảnh để giảm dung lượng
                        val resizedBytes = resizeImage(
                            imageBytes = originalBytes,
                            maxWidth = 1024,
                            maxHeight = 1024,
                            quality = 85
                        )
                        val resizedSizeKB = resizedBytes.size / 1024
                        
                        selectedImageBytes = resizedBytes
                        
                        // Lấy tên file từ path
                        val fileName = mpFile.path.let { path ->
                            when {
                                path.contains("/") -> path.substringAfterLast("/")
                                path.contains("\\") -> path.substringAfterLast("\\")
                                else -> "image_${System.currentTimeMillis()}.jpg"
                            }
                        }
                        selectedFileName = fileName
                        
                        message = "✅ Đã chọn: $fileName\nGốc: ${originalSizeKB}KB → Resize: ${resizedSizeKB}KB"
                    } catch (e: Exception) {
                        message = "❌ Lỗi: ${e.message}\nPath: ${mpFile.path}"
                        selectedImageBytes = null
                        selectedFileName = null
                        e.printStackTrace()
                    }
                }
            }
        }

        // Selected file info
        if (selectedFileName != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Orange.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Ảnh đã chọn:", fontWeight = FontWeight.SemiBold, color = AppColors.Brown)
                    Text(selectedFileName ?: "", style = MaterialTheme.typography.bodyMedium)
                    selectedImageBytes?.let {
                        Text(
                            "Kích thước: ${it.size / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.GrayPlaceholder
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { showFilePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
        ) {
            Text(if (selectedFileName != null) "Chọn ảnh khác" else "Chọn ảnh từ thiết bị")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val dishId = selectedDish?.id
                val imageBytes = selectedImageBytes
                
                if (dishId == null) {
                    message = "Chọn một món trước"
                    return@Button
                }
                if (imageBytes == null) {
                    message = "Chọn ảnh trước khi upload"
                    return@Button
                }

                isUploading = true
                message = null
                scope.launch {
                    val base64 = imageBytes.toBase64()
                    val mimeType = when (selectedFileName?.substringAfterLast(".")?.lowercase()) {
                        "png" -> "image/png"
                        "jpg", "jpeg" -> "image/jpeg"
                        "gif" -> "image/gif"
                        "webp" -> "image/webp"
                        else -> "image/jpeg"
                    }
                    val base64String = "data:$mimeType;base64,$base64"
                    
                    repo.uploadDishImage(dishId, base64String)
                        .onSuccess { url ->
                            message = "✅ UPLOAD THÀNH CÔNG!\n\nẢnh đã được lưu cho món: ${selectedDish?.name}\n\nURL: $url"
                            selectedImageBytes = null
                            selectedFileName = null
                            selectedDish = null
                            loadPage(currentPage, keepMessage = true)
                        }
                        .onFailure { 
                            message = "❌ Upload thất bại: ${it.message}"
                        }
                    isUploading = false
                }
            },
            enabled = !isUploading && selectedImageBytes != null && selectedDish != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brown)
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Upload ảnh lên Cloudinary")
            }
        }

        Spacer(Modifier.height(8.dp))

        message?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (it.startsWith("✅")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = it,
                    color = if (it.startsWith("✅")) Color(0xFF2E7D32) else Color.Red,
                    textAlign = TextAlign.Start,
                    fontWeight = if (it.startsWith("✅")) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("Quay lại")
        }
    }
}
