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
import com.foodfest.app.features.family.presentation.FamilyHomeScreen
import com.foodfest.app.features.family.presentation.FamilyMembersScreen
import com.foodfest.app.features.family.presentation.FamilyWeeklyMenuScreen
import com.foodfest.app.features.family.presentation.FamilyDayMenuScreen
import com.foodfest.app.features.family.presentation.FamilySavedMealsScreen
import com.foodfest.app.features.family.presentation.FamilyPantryScreen
import com.foodfest.app.features.family.presentation.FamilyShoppingListScreen
import com.foodfest.app.features.family.presentation.FamilyNotesScreen
import com.foodfest.app.features.family.presentation.FamilyRecentVotesScreen
import com.foodfest.app.features.notification.data.AppNotification
import com.foodfest.app.features.notification.presentation.NotificationScreen
import com.foodfest.app.core.cache.SharedDishNameCache
import com.foodfest.app.features.personaldish.presentation.MyDishesScreen
import com.foodfest.app.features.profile.presentation.MyPostsScreen
import com.foodfest.app.features.profile.presentation.UserProfileScreen
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
    MyPosts,
    UserProfile,
    SavedPosts,
    CreatePost,
    FamilyHome,
    FamilyMembers,
    FamilyWeeklyMenu,
    FamilySavedMeals,
    FamilyDayMenu,
    FamilyPantry,
    FamilyShoppingList,
    FamilyNotes,
    FamilyRecentVotes,
    Notifications
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
        var selectedUserProfileId by remember { mutableStateOf<Int?>(null) }
        var selectedFamilyId by remember { mutableStateOf<Int?>(null) }
        var selectedFamilyMenuId by remember { mutableStateOf<Int?>(null) }
        var selectedFamilyMenuDate by remember { mutableStateOf<String?>(null) }
        var selectedFamilyMealType by remember { mutableStateOf<String?>(null) }
        var selectedFamilyShoppingListId by remember { mutableStateOf<Int?>(null) }
        var openFamilyVoteOnEntry by remember { mutableStateOf(false) }
        var previousScreenBeforeUserProfile by remember { mutableStateOf(Screen.Main) }
        var previousScreenBeforeSavedMeals by remember { mutableStateOf(Screen.FamilyHome) }
        var previousTabBeforeSavedMeals by remember { mutableStateOf(MainTab.Family) }
        
        val authRepository = remember { AuthRepository() }
        val scope = rememberCoroutineScope()

        fun clearSessionState() {
            scope.launch {
                SharedDishNameCache.clear()
            }
            TokenManager.clearAll()
            authToken = null
            currentUser = null
        }

        fun parseTrailingId(value: String): Int? {
            return value.substringAfterLast("/", missingDelimiterValue = "").toIntOrNull()
        }

        fun openFamilyHome(familyId: Int? = null) {
            selectedFamilyId = familyId
            currentTab = MainTab.Family
            currentScreen = Screen.FamilyHome
        }

        fun handleNotificationNavigation(notification: AppNotification) {
            val actionUrl = notification.actionUrl.orEmpty()
            when {
                actionUrl.startsWith("foodfest://family/pantry/") -> {
                    selectedFamilyId = parseTrailingId(actionUrl)
                    currentScreen = Screen.FamilyPantry
                }
                actionUrl.startsWith("foodfest://families/invites/") -> {
                    openFamilyHome()
                }
                actionUrl.startsWith("foodfest://family/") -> {
                    openFamilyHome(parseTrailingId(actionUrl))
                }
                notification.type.startsWith("family") ||
                    notification.type.startsWith("pantry") ||
                    notification.relatedEntityType?.startsWith("family") == true -> {
                    openFamilyHome()
                }
            }
        }
        
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
                            clearSessionState()
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
                                clearSessionState()
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
                            onNavigateToMyPosts = {
                                currentScreen = Screen.MyPosts
                            },
                            onNavigateToSavedPosts = {
                                currentScreen = Screen.SavedPosts
                            },
                            onNavigateToFamilyHome = {
                                currentScreen = Screen.FamilyHome
                            },
                            onNavigateToFamilyWeeklyMenu = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyWeeklyMenu
                            },
                            onNavigateToFamilySavedMeals = { familyId ->
                                selectedFamilyId = familyId
                                selectedFamilyMenuId = null
                                selectedFamilyMenuDate = null
                                selectedFamilyMealType = null
                                previousScreenBeforeSavedMeals = Screen.Main
                                previousTabBeforeSavedMeals = MainTab.Family
                                currentScreen = Screen.FamilySavedMeals
                            },
                            onNavigateToFamilyMembers = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyMembers
                            },
                            onNavigateToFamilyPantry = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyPantry
                            },
                            onNavigateToFamilyShoppingList = { familyId, shoppingListId ->
                                selectedFamilyId = familyId
                                selectedFamilyShoppingListId = shoppingListId
                                currentScreen = Screen.FamilyShoppingList
                            },
                            onNavigateToFamilyNotes = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyNotes
                            },
                            onNavigateToFamilyVotes = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyRecentVotes
                            },
                            onNavigateToCreatePost = {
                                currentScreen = Screen.CreatePost
                            },
                            onNavigateToUserProfile = { userId ->
                                selectedUserProfileId = userId
                                previousScreenBeforeUserProfile = Screen.Main
                                currentScreen = Screen.UserProfile
                            },
                            onNavigateToNotifications = {
                                currentScreen = Screen.Notifications
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

                    Screen.MyPosts -> {
                        MyPostsScreen(
                            userId = currentUser?.id,
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            }
                        )
                    }

                    Screen.UserProfile -> {
                        UserProfileScreen(
                            userId = selectedUserProfileId,
                            currentUserId = currentUser?.id,
                            onBack = {
                                currentScreen = previousScreenBeforeUserProfile
                            }
                        )
                    }
                    
                    Screen.SavedPosts -> {
                        SavedPostsScreen(
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            },
                            onNavigateToUserProfile = { userId ->
                                selectedUserProfileId = userId
                                previousScreenBeforeUserProfile = Screen.SavedPosts
                                currentScreen = Screen.UserProfile
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

                    // Family Home entry point.
                    Screen.FamilyHome -> {
                        FamilyHomeScreen(
                            initialFamilyId = selectedFamilyId,
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            },
                            onNavigateToMembers = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyMembers
                            },
                            onNavigateToWeeklyMenu = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyWeeklyMenu
                            },
                            onNavigateToSavedMeals = { familyId ->
                                selectedFamilyId = familyId
                                selectedFamilyMenuId = null
                                selectedFamilyMenuDate = null
                                selectedFamilyMealType = null
                                previousScreenBeforeSavedMeals = Screen.FamilyHome
                                currentScreen = Screen.FamilySavedMeals
                            },
                            onNavigateToPantry = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyPantry
                            },
                            onNavigateToNotes = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyNotes
                            },
                            onNavigateToVotes = { familyId ->
                                selectedFamilyId = familyId
                                currentScreen = Screen.FamilyRecentVotes
                            }
                        )
                    }

                    // Family Members management screen.
                    Screen.FamilyMembers -> {
                        val familyId = selectedFamilyId ?: 0
                        FamilyMembersScreen(
                            familyId = familyId,
                            currentUserId = currentUser?.id,
                            onBack = {
                                currentScreen = Screen.FamilyHome
                            }
                        )
                    }

                    Screen.FamilyWeeklyMenu -> {
                        val familyId = selectedFamilyId ?: 0
                        FamilyWeeklyMenuScreen(
                            familyId = familyId,
                            onBack = {
                                currentScreen = Screen.FamilyHome
                            },
                            onOpenDayMenu = { menuId, menuDate, mealType ->
                                selectedFamilyMenuId = menuId
                                selectedFamilyMenuDate = menuDate
                                selectedFamilyMealType = mealType
                                currentScreen = Screen.FamilyDayMenu
                            },
                            onOpenSavedMeals = { menuId, menuDate, mealType ->
                                selectedFamilyMenuId = menuId
                                selectedFamilyMenuDate = menuDate
                                selectedFamilyMealType = mealType
                                previousScreenBeforeSavedMeals = Screen.FamilyWeeklyMenu
                                currentScreen = Screen.FamilySavedMeals
                            },
                            onOpenShoppingList = { shoppingListId ->
                                selectedFamilyShoppingListId = shoppingListId
                                currentScreen = Screen.FamilyShoppingList
                            }
                        )
                    }

                    Screen.FamilySavedMeals -> {
                        val familyId = selectedFamilyId ?: 0
                        val menuId = selectedFamilyMenuId
                        val menuDate = selectedFamilyMenuDate
                        val mealType = selectedFamilyMealType
                        FamilySavedMealsScreen(
                            familyId = familyId,
                            menuId = menuId,
                            menuDate = menuDate,
                            mealType = mealType,
                            onBack = {
                                if (previousScreenBeforeSavedMeals == Screen.Main) {
                                    currentScreen = Screen.Main
                                    currentTab = previousTabBeforeSavedMeals
                                } else {
                                    currentScreen = previousScreenBeforeSavedMeals
                                }
                            },
                            onAppliedToMenu = { appliedMenuId, appliedDate, appliedMealType ->
                                selectedFamilyMenuId = appliedMenuId
                                selectedFamilyMenuDate = appliedDate
                                selectedFamilyMealType = appliedMealType
                                currentScreen = Screen.FamilyDayMenu
                            }
                        )
                    }

                    Screen.FamilyDayMenu -> {
                        val familyId = selectedFamilyId ?: 0
                        val menuId = selectedFamilyMenuId ?: 0
                        val menuDate = selectedFamilyMenuDate.orEmpty()
                        val mealType = selectedFamilyMealType.orEmpty()
                        FamilyDayMenuScreen(
                            familyId = familyId,
                            menuId = menuId,
                            menuDate = menuDate,
                            mealType = mealType,
                            openVoteOnEntry = openFamilyVoteOnEntry,
                            onOpenVoteEntryHandled = {
                                openFamilyVoteOnEntry = false
                            },
                            onBack = {
                                currentScreen = Screen.FamilyWeeklyMenu
                            }
                        )
                    }

                    Screen.FamilyPantry -> {
                        val familyId = selectedFamilyId ?: 0
                        FamilyPantryScreen(
                            familyId = familyId,
                            onBack = {
                                currentScreen = Screen.FamilyHome
                            }
                        )
                    }

                    Screen.FamilyShoppingList -> {
                        val familyId = selectedFamilyId ?: 0
                        val shoppingListId = selectedFamilyShoppingListId ?: 0
                        FamilyShoppingListScreen(
                            familyId = familyId,
                            shoppingListId = shoppingListId,
                            currentUserId = currentUser?.id,
                            onBack = {
                                currentScreen = Screen.FamilyWeeklyMenu
                            }
                        )
                    }

                    Screen.FamilyNotes -> {
                        val familyId = selectedFamilyId ?: 0
                        FamilyNotesScreen(
                            familyId = familyId,
                            onBack = {
                                currentScreen = Screen.FamilyHome
                            }
                        )
                    }

                    Screen.FamilyRecentVotes -> {
                        val familyId = selectedFamilyId ?: 0
                        FamilyRecentVotesScreen(
                            familyId = familyId,
                            onBack = {
                                currentScreen = Screen.FamilyHome
                            },
                            onOpenVoteMeal = { menuId, menuDate, mealType ->
                                selectedFamilyMenuId = menuId
                                selectedFamilyMenuDate = menuDate
                                selectedFamilyMealType = mealType
                                openFamilyVoteOnEntry = true
                                currentScreen = Screen.FamilyDayMenu
                            }
                        )
                    }

                    Screen.Notifications -> {
                        NotificationScreen(
                            onBack = {
                                currentScreen = Screen.Main
                                currentTab = MainTab.Profile
                            },
                            onNotificationClick = { notification ->
                                handleNotificationNavigation(notification)
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
    onNavigateToMyPosts: () -> Unit,
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToFamilyHome: () -> Unit,
    onNavigateToFamilyWeeklyMenu: (Int) -> Unit,
    onNavigateToFamilyMembers: (Int) -> Unit,
    onNavigateToFamilySavedMeals: (Int) -> Unit,
    onNavigateToFamilyPantry: (Int) -> Unit,
    onNavigateToFamilyShoppingList: (Int, Int) -> Unit,
    onNavigateToFamilyNotes: (Int) -> Unit,
    onNavigateToFamilyVotes: (Int) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToUserProfile: (Int) -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                MainTab.Home -> HomeScreen(
                    onNavigateToCreatePost = onNavigateToCreatePost,
                    currentUserId = user?.id,
                    onNavigateToUserProfile = onNavigateToUserProfile
                )
                MainTab.Dish -> DishListScreen()
                MainTab.BlindBox -> BlindBoxScreen(
                    onNavigateToDishDetail = onNavigateToDishDetail
                )
                MainTab.Family -> FamilyHomeScreen(
                    onBack = {
                        onSelectTab(MainTab.Home)
                    },
                    onNavigateToMembers = { familyId ->
                        onNavigateToFamilyMembers(familyId)
                    },
                    onNavigateToWeeklyMenu = { familyId ->
                        onNavigateToFamilyWeeklyMenu(familyId)
                    },
                    onNavigateToSavedMeals = { familyId ->
                        onNavigateToFamilySavedMeals(familyId)
                    },
                    onNavigateToPantry = { familyId ->
                        onNavigateToFamilyPantry(familyId)
                    },
                    onNavigateToNotes = { familyId ->
                        onNavigateToFamilyNotes(familyId)
                    },
                    onNavigateToVotes = { familyId ->
                        onNavigateToFamilyVotes(familyId)
                    }
                )
                MainTab.Profile -> ProfileScreen(
                    user = user,
                    onLogout = onLogout,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToDishUpload = onNavigateToDishUpload,
                    onNavigateToFavorites = onNavigateToFavorites,
                    onNavigateToMyDishes = onNavigateToMyDishes,
                    onNavigateToMyPosts = onNavigateToMyPosts,
                    onNavigateToSavedPosts = onNavigateToSavedPosts,
                    onNavigateToFamilyHome = onNavigateToFamilyHome,
                    onNavigateToNotifications = onNavigateToNotifications
                )
            }
        }
        BottomNavBar(current = currentTab, onSelected = onSelectTab)
    }
}
