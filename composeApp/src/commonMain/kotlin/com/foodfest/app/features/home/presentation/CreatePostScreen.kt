package com.foodfest.app.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.foodfest.app.theme.AppColors
import com.foodfest.app.components.AppImage
import com.foodfest.app.utils.readBytesFromPath
import com.foodfest.app.utils.resizeImage
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel = remember { CreatePostViewModel() },
    onBack: () -> Unit = {},
    onPostCreated: () -> Unit = {}
) {
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var showFilePicker by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var imageProcessingMessage by remember { mutableStateOf<String?>(null) }
    
    // File Picker
    FilePicker(
        show = showFilePicker,
        fileExtensions = listOf("jpg", "jpeg", "png", "gif", "webp")
    ) { mpFile ->
        showFilePicker = false
        
        if (mpFile != null) {
            scope.launch {
                try {
                    imageProcessingMessage = "Đang xử lý ảnh..."
                    
                    // Đọc bytes từ file
                    val originalBytes = readBytesFromPath(mpFile.path)
                    val originalSizeKB = originalBytes.size / 1024
                    
                    // Resize ảnh
                    val resizedBytes = resizeImage(
                        imageBytes = originalBytes,
                        maxWidth = 1024,
                        maxHeight = 1024,
                        quality = 85
                    )
                    val resizedSizeKB = resizedBytes.size / 1024
                    
                    selectedImageBytes = resizedBytes
                    
                    // Lấy tên file
                    val fileName = mpFile.path.let { path ->
                        when {
                            path.contains("/") -> path.substringAfterLast("/")
                            path.contains("\\") -> path.substringAfterLast("\\")
                            else -> "image_${Random.nextInt(100000, 999999)}.jpg"
                        }
                    }
                    selectedFileName = fileName
                    
                    imageProcessingMessage = "✅ Gốc: ${originalSizeKB}KB → ${resizedSizeKB}KB"
                } catch (e: Exception) {
                    imageProcessingMessage = "❌ Lỗi: ${e.message}"
                    selectedImageBytes = null
                    selectedFileName = null
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tạo bài viết",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Brown
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.Brown
                        )
                    }
                },
                actions = {
                    // Nút đăng bài
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.createPost(
                                    imageBytes = selectedImageBytes,
                                    fileName = selectedFileName
                                )
                            }
                        },
                        enabled = !state.isLoading && (state.content.isNotBlank() || selectedImageBytes != null)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Orange
                            )
                        } else {
                            Text(
                                text = "Đăng",
                                color = if (state.content.isNotBlank() || selectedImageBytes != null) 
                                    AppColors.Orange 
                                else 
                                    AppColors.GrayPlaceholder,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Thông báo lỗi/thành công
            state.message?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.contains("✅") || msg.contains("thành công", ignoreCase = true))
                            Color(0xFFE8F5E9)
                        else
                            Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.weight(1f),
                            color = if (msg.contains("✅") || msg.contains("thành công", ignoreCase = true))
                                Color(0xFF2E7D32)
                            else
                                Color(0xFFC62828)
                        )
                        IconButton(
                            onClick = { viewModel.clearMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = AppColors.GrayPlaceholder
                            )
                        }
                    }
                }
            }
            
            // Xử lý khi đăng bài thành công
            LaunchedEffect(state.isSuccess) {
                if (state.isSuccess) {
                    onPostCreated()
                }
            }
            
            // Nội dung bài viết
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Tiêu đề (tuỳ chọn)
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        placeholder = {
                            Text(
                                text = "Tiêu đề bài viết (tuỳ chọn)",
                                color = AppColors.GrayPlaceholder
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Orange,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = AppColors.Background,
                            unfocusedContainerColor = AppColors.Background
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Nội dung chính
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = { viewModel.updateContent(it) },
                        placeholder = {
                            Text(
                                text = "Bạn đang nghĩ gì về món ăn hôm nay? 🍳",
                                color = AppColors.GrayPlaceholder
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Orange,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = AppColors.Background,
                            unfocusedContainerColor = AppColors.Background
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 10
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Ảnh đã chọn
            if (selectedImageBytes != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ảnh đính kèm",
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Brown
                            )
                            IconButton(
                                onClick = {
                                    selectedImageBytes = null
                                    selectedFileName = null
                                    imageProcessingMessage = null
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Xoá ảnh",
                                    tint = Color.Red.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Hiển thị ảnh preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.Background),
                            contentAlignment = Alignment.Center
                        ) {
                            // Chuyển bytes sang base64 data URL để preview
                            val base64Image = remember(selectedImageBytes) {
                                selectedImageBytes?.let {
                                    "data:image/jpeg;base64,${Base64.encode(it)}"
                                }
                            }
                            
                            if (base64Image != null) {
                                AppImage(
                                    url = base64Image,
                                    contentDescription = "Preview ảnh",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        selectedFileName?.let { name ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.GrayPlaceholder
                            )
                        }
                        
                        imageProcessingMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (msg.startsWith("✅")) Color(0xFF2E7D32) else AppColors.GrayPlaceholder
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Nút chọn ảnh
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showFilePicker = true },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.Orange.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Thêm ảnh",
                            tint = AppColors.Orange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = if (selectedImageBytes != null) "Thay đổi ảnh" else "Thêm ảnh",
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Chọn ảnh từ thư viện",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.GrayPlaceholder
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Loại bài viết
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Loại bài viết",
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Brown
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PostTypeChip(
                            label = "Chia sẻ",
                            emoji = "📝",
                            isSelected = state.postType == "share",
                            onClick = { viewModel.updatePostType("share") },
                            modifier = Modifier.weight(1f)
                        )
                        PostTypeChip(
                            label = "Công thức",
                            emoji = "🍳",
                            isSelected = state.postType == "recipe",
                            onClick = { viewModel.updatePostType("recipe") },
                            modifier = Modifier.weight(1f)
                        )
                        PostTypeChip(
                            label = "Review",
                            emoji = "⭐",
                            isSelected = state.postType == "review",
                            onClick = { viewModel.updatePostType("review") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PostTypeChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, AppColors.Orange, RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(1.dp, AppColors.GrayPlaceholder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                }
            ),
        color = if (isSelected) AppColors.Orange.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AppColors.Orange else AppColors.TextPrimary
            )
        }
    }
}
