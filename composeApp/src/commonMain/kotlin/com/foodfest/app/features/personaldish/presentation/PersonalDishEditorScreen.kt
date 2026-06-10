package com.foodfest.app.features.personaldish.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.foodfest.app.components.FoodFestFormTextField
import com.foodfest.app.components.FoodFestImagePickerCard
import com.foodfest.app.components.FoodFestSelectableChip
import com.foodfest.app.theme.AppColors
import com.foodfest.app.utils.readBytesFromPath
import com.foodfest.app.utils.resizeImage
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun PersonalDishEditorScreen(
    viewModel: PersonalDishEditorViewModel = remember { PersonalDishEditorViewModel() },
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var imageProcessingMessage by remember { mutableStateOf<String?>(null) }
    val previewUrl = remember(selectedImageBytes, state.imageUrl) {
        selectedImageBytes?.let { bytes ->
            "data:image/jpeg;base64,${Base64.encode(bytes)}"
        } ?: state.imageUrl.trim().ifBlank { null }
    }

    LaunchedEffect(Unit) {
        viewModel.loadTags()
    }

    FilePicker(
        show = showImagePicker,
        fileExtensions = listOf("jpg", "jpeg", "png", "gif", "webp")
    ) { mpFile ->
        showImagePicker = false
        if (mpFile == null) return@FilePicker

        scope.launch {
            try {
                imageProcessingMessage = "Đang xử lý ảnh..."

                val originalBytes = readBytesFromPath(mpFile.path)
                val originalSizeKB = originalBytes.size / 1024
                val resizedBytes = resizeImage(
                    imageBytes = originalBytes,
                    maxWidth = 1024,
                    maxHeight = 1024,
                    quality = 85
                )
                val resizedSizeKB = resizedBytes.size / 1024
                selectedImageBytes = resizedBytes
                selectedFileName = mpFile.path.toDisplayFileName()
                imageProcessingMessage = "✅ Gốc: ${originalSizeKB}KB → ${resizedSizeKB}KB"
            } catch (e: Exception) {
                selectedImageBytes = null
                selectedFileName = null
                imageProcessingMessage = "Không đọc được ảnh: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tạo món của tôi",
                        color = AppColors.Brown,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isSaving) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.Brown
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.errorMessage?.let { message ->
                MessageCard(message = message, isError = true)
            }
            state.savingMessage?.let { message ->
                MessageCard(message = message, isError = false)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitle("Thông tin món")
                    FoodFestFormTextField(
                        value = state.dishName,
                        onValueChange = viewModel::updateDishName,
                        label = "Tên món *",
                        placeholder = "Ví dụ: Trứng chiên cà chua"
                    )
                    FoodFestImagePickerCard(
                        title = "Ảnh món",
                        previewUrl = previewUrl,
                        selectedFileName = selectedFileName,
                        helperText = imageProcessingMessage ?: "Chọn ảnh từ thiết bị hoặc dán URL bên dưới.",
                        onPickImage = { showImagePicker = true },
                        onClearImage = {
                            selectedImageBytes = null
                            selectedFileName = null
                            imageProcessingMessage = null
                            viewModel.updateImageUrl("")
                        },
                        enabled = !state.isSaving
                    )
                    FoodFestFormTextField(
                        value = state.imageUrl,
                        onValueChange = viewModel::updateImageUrl,
                        label = "Ảnh URL (tuỳ chọn)",
                        placeholder = "Dán link nếu không chọn file"
                    )
                    FoodFestFormTextField(
                        value = state.description,
                        onValueChange = viewModel::updateDescription,
                        label = "Mô tả",
                        placeholder = "Món này hợp bữa nào, vị ra sao...",
                        singleLine = false,
                        minHeight = 92.dp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitle("Công thức")
                    FoodFestFormTextField(
                        value = state.ingredients,
                        onValueChange = viewModel::updateIngredients,
                        label = "Nguyên liệu",
                        placeholder = "Mỗi dòng một nguyên liệu",
                        singleLine = false,
                        minHeight = 120.dp,
                        maxLines = 8
                    )
                    FoodFestFormTextField(
                        value = state.instructions,
                        onValueChange = viewModel::updateInstructions,
                        label = "Cách làm",
                        placeholder = "Các bước nấu món này",
                        singleLine = false,
                        minHeight = 140.dp,
                        maxLines = 10
                    )
                    FoodFestFormTextField(
                        value = state.note,
                        onValueChange = viewModel::updateNote,
                        label = "Ghi chú",
                        placeholder = "Mẹo nấu, khẩu vị gia đình...",
                        singleLine = false,
                        minHeight = 92.dp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitle("Thời gian & khẩu phần")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FoodFestFormTextField(
                            value = state.prepTime,
                            onValueChange = viewModel::updatePrepTime,
                            label = "Chuẩn bị",
                            placeholder = "phút",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        FoodFestFormTextField(
                            value = state.cookTime,
                            onValueChange = viewModel::updateCookTime,
                            label = "Nấu",
                            placeholder = "phút",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FoodFestFormTextField(
                        value = state.serving,
                        onValueChange = viewModel::updateServing,
                        label = "Khẩu phần",
                        placeholder = "Số người",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Tags")
                    Spacer(modifier = Modifier.height(10.dp))
                    when {
                        state.isLoadingTags -> CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppColors.Orange,
                            strokeWidth = 2.dp
                        )
                        state.tags.isEmpty() -> Text(
                            text = "Chưa tải được tags, bạn vẫn có thể lưu món.",
                            color = AppColors.GrayPlaceholder,
                            fontSize = 13.sp
                        )
                        else -> LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(state.tags, key = { it.id }) { tag ->
                                FoodFestSelectableChip(
                                    label = tag.name,
                                    selected = tag.id in state.selectedTagIds,
                                    onClick = { viewModel.toggleTag(tag.id) }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.createDish(selectedImageBytes, selectedFileName)) {
                            onSaved()
                        }
                    }
                },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Orange)
            ) {
                if (state.isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(state.savingMessage ?: "Đang lưu...", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(state.savingMessage ?: "Lưu món", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun String.toDisplayFileName(): String {
    return when {
        contains("/") -> substringAfterLast("/")
        contains("\\") -> substringAfterLast("\\")
        isNotBlank() -> this
        else -> "personal_dish_${Random.nextInt(100000, 999999)}.jpg"
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = AppColors.Brown,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            color = if (isError) Color(0xFFC62828) else Color(0xFF2E7D32),
            modifier = Modifier.padding(12.dp)
        )
    }
}
