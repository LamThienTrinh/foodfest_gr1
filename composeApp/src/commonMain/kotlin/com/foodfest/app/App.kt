package com.foodfest.app

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.foodfest.app.features.auth.data.AuthResponse
import com.foodfest.app.features.auth.data.User
import com.foodfest.app.features.auth.data.AuthRepository
import com.foodfest.app.features.auth.presentation.AuthScreen
import com.foodfest.app.features.auth.presentation.RegisterScreen
import com.foodfest.app.features.profile.presentation.ProfileScreen
import com.foodfest.app.features.profile.presentation.EditProfileScreen
import com.foodfest.app.features.dish.presentation.DishImageUploadScreen
import com.foodfest.app.features.dish.presentation.DishListScreen
import com.foodfest.app.features.dish.presentation.DishDetailScreen
import com.foodfest.app.features.home.presentation.HomeScreen
import com.foodfest.app.features.blindbox.presentation.BlindBoxScreen
import com.foodfest.app.features.favorite.presentation.FavoriteDishesScreen
import com.foodfest.app.features.personaldish.presentation.MyDishesScreen
import com.foodfest.app.features.savedposts.presentation.SavedPostsScreen
import com.foodfest.app.features.home.presentation.CreatePostScreen
import com.foodfest.app.core.storage.TokenManager
import com.foodfest.app.navigation.BottomNavBar
import com.foodfest.app.navigation.MainTab
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

// Navigation states
enum class Screen {
    Auth,
    Register,
    Main,
    EditProfile,
    DishImageUpload,
    DishDetail,
    Favorites,
    MyDishes,
    SavedPosts,
    CreatePost
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.Auth) }
        var authToken by remember { mutableStateOf<String?>(null) }
        var currentUser by remember { mutableStateOf<User?>(null) }
        var isCheckingAuth by remember { mutableStateOf(true) }
        var currentTab by remember { mutableStateOf(MainTab.Home) }
        var selectedDishId by remember { mutableStateOf<Int?>(null) }
        
        val authRepository = remember { AuthRepository() }
        val scope = rememberCoroutineScope()
        
        //  Kiểm tra token khi app khởi động
        LaunchedEffect(Unit) {
            val savedToken = TokenManager.getToken()
            if (savedToken != null) {
                println(" Tìm thấy token đã lưu, đang verify...")
                // Verify token bằng cách gọi API /me
                scope.launch {
                    authRepository.getProfile(savedToken)
                        .onSuccess { user ->
                            println(" Token hợp lệ, tự động đăng nhập")
                            authToken = savedToken
                            currentUser = user
                            currentScreen = Screen.Main
                            currentTab = MainTab.Profile
                        }
                        .onFailure {
                            println(" Token không hợp lệ hoặc đã hết hạn")
                            TokenManager.clearAll()
                        }
                    isCheckingAuth = false
                }
            } else {
                println(" Chưa có token, hiển thị màn hình đăng nhập")
                isCheckingAuth = false
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Hiển thị loading khi đang kiểm tra auth
            if (isCheckingAuth) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Surface
            }
            
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.Auth -> {
                        AuthScreen(
                            onLoginSuccess = { token, user ->
                                authToken = token
                                currentUser = user
                                currentScreen = Screen.Main
                                currentTab = MainTab.Home
                            },
                            onNavigateToRegister = {
                                currentScreen = Screen.Register
                            }
                        )
                    }
                    
                    Screen.Register -> {
                        RegisterScreen(
                            onRegisterSuccess = { token, user ->
                                authToken = token
                                currentUser = user
                                currentScreen = Screen.Main
                                currentTab = MainTab.Home
                            },
                            onNavigateToLogin = {
                                currentScreen = Screen.Auth
                            }
                        )
                    }

                    Screen.Main -> {
                        MainScreen(
                            currentTab = currentTab,
                            onSelectTab = { currentTab = it },
                            user = currentUser,
                            onLogout = {
                                TokenManager.clearAll()
                                authToken = null
                                currentUser = null
                                currentScreen = Screen.Auth
                                currentTab = MainTab.Home
                            },
                            onNavigateToEditProfile = {
                                currentScreen = Screen.EditProfile
                            },
                            onNavigateToDishUpload = {
                                currentScreen = Screen.DishImageUpload
                            },
                            onNavigateToDishDetail = { dishId ->
                                selectedDishId = dishId
                                currentScreen = Screen.DishDetail
                            },
                            onNavigateToFavorites = {
                                currentScreen = Screen.Favorites
                            },
                            onNavigateToMyDishes = {
                                currentScreen = Screen.MyDishes
                            },
                            onNavigateToSavedPosts = {
                                currentScreen = Screen.SavedPosts
                            },
                            onNavigateToCreatePost = {
                                currentScreen = Screen.CreatePost
                            }
                        )
                    }
                    
                    Screen.EditProfile -> {
                        EditProfileScreen(
                            user = currentUser,
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            },
                            onProfileUpdated = { updatedUser ->
                                currentUser = updatedUser
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            }
                        )
                    }
                    
                    Screen.DishImageUpload -> {
                        DishImageUploadScreen(
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            }
                        )
                    }
                    
                    Screen.DishDetail -> {
                        selectedDishId?.let { dishId ->
                            DishDetailScreen(
                                dishId = dishId,
                                onBack = {
                                    currentScreen = Screen.Main
                                    currentTab = MainTab.BlindBox
                                }
                            )
                        }
                    }
                    
                    Screen.Favorites -> {
                        FavoriteDishesScreen(
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            }
                        )
                    }
                    
                    Screen.MyDishes -> {
                        MyDishesScreen(
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            }
                        )
                    }
                    
                    Screen.SavedPosts -> {
                        SavedPostsScreen(
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            }
                        )
                    }
                    
                    Screen.CreatePost -> {
                        CreatePostScreen(
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Home
                            },
                            onPostCreated = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Home
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    user: User?,
    onLogout: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToDishUpload: () -> Unit,
    onNavigateToDishDetail: (Int) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToMyDishes: () -> Unit,
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToCreatePost: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                MainTab.Home -> HomeScreen(
                    onNavigateToCreatePost = onNavigateToCreatePost
                )
                MainTab.Dish -> DishListScreen()
                MainTab.BlindBox -> BlindBoxScreen(
                    onNavigateToDishDetail = onNavigateToDishDetail
                )
                MainTab.Profile -> ProfileScreen(
                    user = user,
                    onLogout = onLogout,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToDishUpload = onNavigateToDishUpload,
                    onNavigateToFavorites = onNavigateToFavorites,
                    onNavigateToMyDishes = onNavigateToMyDishes,
                    onNavigateToSavedPosts = onNavigateToSavedPosts
                )
            }
        }
        BottomNavBar(current = currentTab, onSelected = onSelectTab)
    }
}