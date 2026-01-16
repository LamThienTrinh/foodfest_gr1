package com.foodfest.app.features.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodfest.app.features.auth.data.AuthRepository
import com.foodfest.app.features.auth.data.User
import com.foodfest.app.features.profile.presentation.components.*
import com.foodfest.app.core.storage.TokenManager
import com.foodfest.app.theme.AppColors
import foodfest.composeapp.generated.resources.Res
import foodfest.composeapp.generated.resources.default_avatar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: User?,
    onBack: () -> Unit,
    onProfileUpdated: (User) -> Unit
) {
    val authRepo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Form states
    var fullName by remember { mutableStateOf(user?.fullName ?: "") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // UI states
    var isUpdatingName by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    
    // Validation
    var nameError by remember { mutableStateOf<String?>(null) }
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    fun validateName(): Boolean {
        return when {
            fullName.isBlank() -> {
                nameError = "Tên không được để trống"
                false
            }
            fullName.length < 2 -> {
                nameError = "Tên phải có ít nhất 2 ký tự"
                false
            }
            else -> {
                nameError = null
                true
            }
        }
    }
    
    fun validatePasswords(): Boolean {
        var valid = true
        
        if (currentPassword.isBlank()) {
            currentPasswordError = "Vui lòng nhập mật khẩu hiện tại"
            valid = false
        } else {
            currentPasswordError = null
        }
        
        if (newPassword.length < 6) {
            newPasswordError = "Mật khẩu mới phải có ít nhất 6 ký tự"
            valid = false
        } else {
            newPasswordError = null
        }
        
        if (newPassword != confirmPassword) {
            confirmPasswordError = "Mật khẩu xác nhận không khớp"
            valid = false
        } else {
            confirmPasswordError = null
        }
        
        return valid
    }
    
    fun updateName() {
        if (!validateName()) return
        
        scope.launch {
            isUpdatingName = true
            val token = TokenManager.getToken()
            if (token != null) {
                authRepo.updateProfile(token, fullName)
                    .onSuccess { updatedUser ->
                        onProfileUpdated(updatedUser)
                        snackbarHostState.showSnackbar("Cập nhật thành công!")
                    }
                    .onFailure { error ->
                        snackbarHostState.showSnackbar(error.message ?: "Cập nhật thất bại")
                    }
            }
            isUpdatingName = false
        }
    }
    
    fun changePassword() {
        if (!validatePasswords()) return
        
        scope.launch {
            isChangingPassword = true
            val token = TokenManager.getToken()
            if (token != null) {
                authRepo.changePassword(token, currentPassword, newPassword)
                    .onSuccess {
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        showChangePassword = false
                        snackbarHostState.showSnackbar("Đổi mật khẩu thành công!")
                    }
                    .onFailure { error ->
                        currentPasswordError = error.message
                    }
            }
            isChangingPassword = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Chỉnh sửa hồ sơ",
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Avatar section
            AvatarSection(
                avatarUrl = user?.avatarUrl,
                onChangeAvatar = { /* TODO: Implement avatar change */ }
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Profile info section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Thông tin cá nhân",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Brown
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Username (readonly)
                    FormTextField(
                        value = user?.username ?: "",
                        onValueChange = {},
                        label = "Tên đăng nhập",
                        enabled = false,
                        leadingIcon = Icons.Default.Person
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Full name
                    FormTextField(
                        value = fullName,
                        onValueChange = { 
                            fullName = it
                            nameError = null
                        },
                        label = "Họ và tên",
                        isError = nameError != null,
                        errorMessage = nameError
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    PrimaryButton(
                        text = "Lưu thay đổi",
                        onClick = { updateName() },
                        isLoading = isUpdatingName,
                        enabled = fullName != user?.fullName
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Change password section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                            "Đổi mật khẩu",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Brown
                        )
                        
                        TextButton(onClick = { showChangePassword = !showChangePassword }) {
                            Text(
                                if (showChangePassword) "Ẩn" else "Mở rộng",
                                color = AppColors.Orange
                            )
                        }
                    }
                    
                    if (showChangePassword) {
                        Spacer(Modifier.height(16.dp))
                        
                        PasswordTextField(
                            value = currentPassword,
                            onValueChange = { 
                                currentPassword = it
                                currentPasswordError = null
                            },
                            label = "Mật khẩu hiện tại",
                            isError = currentPasswordError != null,
                            errorMessage = currentPasswordError
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        PasswordTextField(
                            value = newPassword,
                            onValueChange = { 
                                newPassword = it
                                newPasswordError = null
                            },
                            label = "Mật khẩu mới",
                            isError = newPasswordError != null,
                            errorMessage = newPasswordError
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        PasswordTextField(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                confirmPasswordError = null
                            },
                            label = "Xác nhận mật khẩu mới",
                            isError = confirmPasswordError != null,
                            errorMessage = confirmPasswordError
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        PrimaryButton(
                            text = "Đổi mật khẩu",
                            onClick = { changePassword() },
                            isLoading = isChangingPassword
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AvatarSection(
    avatarUrl: String?,
    onChangeAvatar: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable { onChangeAvatar() }
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, AppColors.Orange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.default_avatar),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(116.dp).clip(CircleShape)
                )
            }
            
            // Camera icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(AppColors.Orange)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Đổi ảnh",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
