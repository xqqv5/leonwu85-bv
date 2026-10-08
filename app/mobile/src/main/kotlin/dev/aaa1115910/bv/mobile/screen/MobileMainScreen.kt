package dev.aaa1115910.bv.mobile.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.VerticalDragType
import com.origeek.imageViewer.previewer.rememberPreviewerState
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.repositories.MessageRepository
import dev.aaa1115910.biliapi.repositories.UserRepository as BiliUserRepository
import dev.aaa1115910.bv.mobile.activities.InboxActivity
import dev.aaa1115910.bv.mobile.activities.LoginActivity
import dev.aaa1115910.bv.mobile.activities.SettingsActivity
import dev.aaa1115910.bv.mobile.component.ImagePreviewerActions
import dev.aaa1115910.bv.component.rememberVlcUpgradePrompt
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.mobile.component.LibVLCDownloaderDialog
import dev.aaa1115910.bv.mobile.settings.MobileBottomNavItem
import dev.aaa1115910.bv.mobile.settings.MobilePrefs
import dev.aaa1115910.bv.player.impl.vlc.VlcNativeLibs
import dev.aaa1115910.bv.mobile.component.update.MobileAutoUpdateDialog
import dev.aaa1115910.bv.mobile.screen.home.DynamicScreen
import dev.aaa1115910.bv.mobile.screen.home.HomeScreen
import dev.aaa1115910.bv.mobile.screen.home.MineScreen
import dev.aaa1115910.bv.mobile.screen.home.SearchScreen
import dev.aaa1115910.bv.update.AutoUpdateChecker
import dev.aaa1115910.bv.update.AutoUpdateInfo
import dev.aaa1115910.bv.mobile.util.saveImageToGallery
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.UserSwitchViewModel
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MobileMainScreen(
    modifier: Modifier = Modifier,
    popularViewModel: PopularViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel(),
    userSwitchViewModel: UserSwitchViewModel = koinViewModel(),
    biliUserRepository: BiliUserRepository = koinInject(),
    messageRepository: MessageRepository = koinInject()
) {
    val logger = KotlinLogging.logger("MobileMainScreen")
    val state = rememberMobileMainScreenState(
        popularViewModel = popularViewModel,
        userViewModel = userViewModel,
        userSwitchViewModel = userSwitchViewModel
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val navSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    val bottomNavigationItems by MobilePrefs.bottomNavigationItemsFlow.collectAsState(
        initial = MobilePrefs.bottomNavigationItems
    )
    val bottomNavItems = remember(bottomNavigationItems) {
        bottomNavigationItems.map(MobileMainScreenNav::fromBottomNavItem)
    }
    val mineInBottomBar = navSuiteType == NavigationSuiteType.NavigationBar &&
        MobileMainScreenNav.Mine in bottomNavItems
    val onOpenMineShortcut: (() -> Unit)? = if (
        navSuiteType == NavigationSuiteType.NavigationBar &&
        MobileMainScreenNav.Home !in bottomNavItems && MobileMainScreenNav.Mine !in bottomNavItems
    ) {
        { state.navigate(MobileMainScreenNav.Mine) }
    } else {
        null
    }

    val pictures = remember { mutableStateListOf<Picture>() }
    var savingPreviewImage by remember { mutableStateOf(false) }
    var dynamicUnreadCount by remember { mutableStateOf(0) }
    var messageUnreadCount by remember { mutableStateOf(0) }
    var autoUpdateInfo by remember { mutableStateOf<AutoUpdateInfo?>(null) }
    var showVlcUpgrade by rememberVlcUpgradePrompt(
        usingVlc = MobilePrefs.playerType == PlayerType.VLC,
        selectedVersion = MobilePrefs.vlcSelectedVersion,
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = { pictures.size },
        getKey = { pictures[it].key }
    )

    fun refreshDynamicUnreadCount() {
        if (!userViewModel.isLogin) {
            dynamicUnreadCount = 0
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                biliUserRepository.getDynamicUnreadCount()
            }.onSuccess { count ->
                withContext(Dispatchers.Main) {
                    dynamicUnreadCount = count
                }
            }.onFailure {
                logger.warn(it) { "Load dynamic unread count failed" }
            }
        }
    }

    fun refreshMessageUnreadCount() {
        if (!userViewModel.isLogin) {
            messageUnreadCount = 0
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                messageRepository.getUnreadCount()
            }.onSuccess { count ->
                withContext(Dispatchers.Main) {
                    messageUnreadCount = count
                }
            }.onFailure {
                logger.warn(it) { "Load message unread count failed" }
            }
        }
    }

    LaunchedEffect(userViewModel.isLogin) {
        refreshDynamicUnreadCount()
        refreshMessageUnreadCount()
    }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                AutoUpdateChecker.checkOnceDaily()
            }
        }.onSuccess { updateInfo ->
            autoUpdateInfo = updateInfo
        }.onFailure {
            logger.warn(it) { "Auto update check failed" }
        }
    }

    LaunchedEffect(state.currentNavItem) {
        if (state.currentNavItem == MobileMainScreenNav.Dynamic) {
            dynamicUnreadCount = 0
        }
    }

    LaunchedEffect(bottomNavItems, navSuiteType) {
        val configurableNavItems = MobileBottomNavItem.entries
            .map(MobileMainScreenNav::fromBottomNavItem)
        if (navSuiteType == NavigationSuiteType.NavigationBar &&
            state.currentNavItem in configurableNavItems && state.currentNavItem !in bottomNavItems
        ) {
            val fallbackNavItem = bottomNavItems.firstOrNull { it != MobileMainScreenNav.Setting }
                ?: MobileMainScreenNav.Home
            state.navigate(fallbackNavItem)
        }
    }

    DisposableEffect(lifecycleOwner, userViewModel.isLogin) {
        var leaveFromThisPage = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> leaveFromThisPage = true
                Lifecycle.Event.ON_RESUME -> {
                    refreshDynamicUnreadCount()
                    refreshMessageUnreadCount()
                    if (leaveFromThisPage) {
                        userViewModel.updateUserInfo(forceUpdate = true)
                    }
                    leaveFromThisPage = false
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onShowPreviewer: (newPictures: List<Picture>, afterSetPictures: () -> Unit) -> Unit =
        { newPictures, afterSetPictures ->
            pictures.swapList(newPictures)
            logger.fInfo { "update image previewer pictures list: $newPictures" }
            afterSetPictures()
        }

    val verticalNavOrder = listOf(
        MobileMainScreenNav.Home,
        MobileMainScreenNav.Search,
        MobileMainScreenNav.Dynamic,
        MobileMainScreenNav.History,
        MobileMainScreenNav.Favorite,
        MobileMainScreenNav.Mine,
        MobileMainScreenNav.Setting
    ).map { it.name }
    val horizontalNavOrder = (
        MobileBottomNavItem.entries.map(MobileMainScreenNav::fromBottomNavItem) +
            listOf(MobileMainScreenNav.Search)
        ).map { it.name }

    val compareNavIndex: (String?, String?) -> Boolean = { a, b ->
        if (navSuiteType == NavigationSuiteType.NavigationBar) {
            horizontalNavOrder.indexOf(a) < horizontalNavOrder.indexOf(b)
        } else {
            verticalNavOrder.indexOf(a) < verticalNavOrder.indexOf(b)
        }
    }

    val navEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        {
            val coefficient = 10
            if (navSuiteType == NavigationSuiteType.NavigationBar) {
                if (compareNavIndex(
                        targetState.destination.route,
                        initialState.destination.route
                    )
                ) {
                    fadeIn() + slideInHorizontally { -it / coefficient }
                } else {
                    fadeIn() + slideInHorizontally { it / coefficient }
                }
            } else {
                if (compareNavIndex(
                        targetState.destination.route,
                        initialState.destination.route
                    )
                ) {
                    fadeIn() + slideInVertically { -it / coefficient }
                } else {
                    fadeIn() + slideInVertically { it / coefficient }
                }
            }
        }

    val navExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        {
            val coefficient = 10
            if (navSuiteType == NavigationSuiteType.NavigationBar) {
                if (compareNavIndex(
                        targetState.destination.route,
                        initialState.destination.route
                    )
                ) {
                    fadeOut() + slideOutHorizontally { it / coefficient }
                } else {
                    fadeOut() + slideOutHorizontally { -it / coefficient }
                }
            } else {
                if (compareNavIndex(
                        targetState.destination.route,
                        initialState.destination.route
                    )
                ) {
                    fadeOut() + slideOutVertically { it / coefficient }
                } else {
                    fadeOut() + slideOutVertically { -it / coefficient }
                }
            }
        }

    BackHandler(previewerState.canClose || previewerState.animating) {
        if (previewerState.canClose) scope.launch {
            previewerState.closeTransform()
        }
    }

    val navHostContent: @Composable () -> Unit = {
        NavHost(
            navController = state.navController,
            startDestination = MobileMainScreenNav.Home.name,
            enterTransition = navEnterTransition,
            exitTransition = navExitTransition
        ) {
            composable(MobileMainScreenNav.Home.name) {
                HomeScreen(
                    rcmdGridState = state.rcmdGridState,
                    popularGridState = state.popularGridState,
                    windowSize = state.windowSizeClass.widthSizeClass,
                    messageUnreadCount = messageUnreadCount,
                    onOpenSearch = { state.navigate(MobileMainScreenNav.Search) },
                    onOpenMine = { state.navigate(MobileMainScreenNav.Mine) },
                    onOpenInbox = {
                        messageUnreadCount = 0
                        InboxActivity.actionStart(context)
                    }
                )
            }

            composable(MobileMainScreenNav.Dynamic.name) {
                BackHandler(previewerState.canClose || previewerState.animating) {
                    if (previewerState.canClose) scope.launch {
                        previewerState.closeTransform()
                    }
                }

                DynamicScreen(
                    dynamicGridState = state.dynamicGridState,
                    previewerState = previewerState,
                    onShowPreviewer = onShowPreviewer,
                    onOpenMine = onOpenMineShortcut,
                    // dynamicViewModel = dynamicViewModel
                )
            }

            composable(MobileMainScreenNav.Search.name) {
                SearchScreen()
            }
            composable(MobileMainScreenNav.History.name) {
                if (userViewModel.isLogin) {
                    HistoryScreen(
                        windowSize = state.windowSizeClass,
                        historyViewModel = koinViewModel(
                            key = "history-${userSwitchViewModel.currentUser.id}"
                        ),
                        showBackButton = false,
                        onOpenMine = onOpenMineShortcut,
                        onBack = { state.navigate(bottomNavItems.first()) }
                    )
                } else {
                    LoginRequiredContent(title = "历史", onOpenMine = onOpenMineShortcut)
                }
            }
            composable(MobileMainScreenNav.Favorite.name) {
                if (userViewModel.isLogin) {
                    FavoriteScreen(
                        windowSize = state.windowSizeClass,
                        favoriteViewModel = koinViewModel(
                            key = "favorite-${userSwitchViewModel.currentUser.id}"
                        ),
                        showBackButton = false,
                        onOpenMine = onOpenMineShortcut,
                        onBack = { state.navigate(bottomNavItems.first()) }
                    )
                } else {
                    LoginRequiredContent(title = "收藏", onOpenMine = onOpenMineShortcut)
                }
            }
            composable(MobileMainScreenNav.Mine.name) {
                MineScreen(
                    windowSize = state.windowSizeClass.widthSizeClass,
                    userViewModel = userViewModel,
                    userSwitchViewModel = userSwitchViewModel,
                    showBackButton = !mineInBottomBar,
                    onBack = {
                        if (!state.navController.popBackStack()) {
                            state.navigate(MobileMainScreenNav.Home)
                        }
                    }
                )
            }
        }
    }

    Box(
        modifier = modifier,
    ) {
        if (state.currentNavItem == MobileMainScreenNav.Mine && !mineInBottomBar) {
            navHostContent()
        } else {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    NavigationSuit(
                        mobileMainScreenState = state,
                        navigationSuiteType = navSuiteType,
                        bottomNavItems = bottomNavItems,
                        avatar = userViewModel.face,
                        dynamicUnreadCount = dynamicUnreadCount,
                        onNavigate = { navItem ->
                            if (navItem == MobileMainScreenNav.Dynamic) {
                                dynamicUnreadCount = 0
                            }
                            state.navigate(navItem)
                        },
                        onOpenMine = { state.navigate(MobileMainScreenNav.Mine) }
                    )
                }
            ) {
                navHostContent()
            }
        }
    }

    ImagePreviewer(
        modifier = Modifier
            .fillMaxSize(),
        state = previewerState,
        imageLoader = { index ->
            val imageRequest = ImageRequest.Builder(LocalContext.current)
                .data(pictures[index].url)
                .size(Size.ORIGINAL)
                .build()
            rememberAsyncImagePainter(imageRequest)
        },
        previewerLayer = {
            foreground = { page ->
                ImagePreviewerActions(
                    saving = savingPreviewImage,
                    onClose = {
                        if (previewerState.canClose) {
                            scope.launch {
                                previewerState.closeTransform()
                            }
                        }
                    },
                    onSave = {
                        val picture = pictures.getOrNull(page)
                        if (picture == null) {
                            "图片不存在".toast(context)
                            return@ImagePreviewerActions
                        }
                        if (savingPreviewImage) return@ImagePreviewerActions
                        scope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                savingPreviewImage = true
                            }
                            runCatching {
                                saveImageToGallery(context, picture.url)
                            }.onSuccess {
                                withContext(Dispatchers.Main) {
                                    "图片已保存到相册".toast(context)
                                }
                            }.onFailure {
                                logger.warn(it) { "Save dynamic preview image failed" }
                                withContext(Dispatchers.Main) {
                                    "保存失败：${it.localizedMessage ?: "未知错误"}".toast(context)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                savingPreviewImage = false
                            }
                        }
                    }
                )
            }
        }
    )

    if (showVlcUpgrade) {
        LibVLCDownloaderDialog(
            onDismissRequest = { showVlcUpgrade = false },
            onDownloadComplete = {
                MobilePrefs.vlcSelectedVersion = VlcNativeLibs.defaultVersion
                showVlcUpgrade = false
                "LibVLC ${VlcNativeLibs.defaultVersion} 组件已安装".toast(context)
            },
            onDownloadFailed = { error ->
                showVlcUpgrade = false
                "升级失败，可在播放器设置中重试：$error".toast(context)
            },
        )
    } else {
        MobileAutoUpdateDialog(
            updateInfo = autoUpdateInfo,
            onDismiss = { autoUpdateInfo = null }
        )
    }
}

@Composable
private fun LoginRequiredContent(title: String, onOpenMine: (() -> Unit)? = null) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "请先登录后查看$title",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { context.startActivity(Intent(context, LoginActivity::class.java)) }) {
            Text(text = "登录")
        }
        if (onOpenMine != null) {
            TextButton(onClick = onOpenMine) {
                Text(text = "我的与设置")
            }
        }
    }
}

@Composable
private fun NavigationSuit(
    modifier: Modifier = Modifier,
    mobileMainScreenState: MobileMainScreenState,
    navigationSuiteType: NavigationSuiteType,
    bottomNavItems: List<MobileMainScreenNav>,
    avatar: String,
    dynamicUnreadCount: Int,
    onNavigate: (MobileMainScreenNav) -> Unit,
    onOpenMine: () -> Unit,
) {
    when (navigationSuiteType) {
        NavigationSuiteType.NavigationBar -> {
            Box(modifier = modifier) {
                NavigationSuite(
                    layoutType = NavigationSuiteType.NavigationBar,
                    colors = NavigationSuiteDefaults.colors(
                        shortNavigationBarContainerColor = Color.Transparent,
                        shortNavigationBarContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationBarContainerColor = Color.Transparent,
                        navigationBarContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    bottomNavItems.forEach { navItem ->
                        item(
                            icon = {
                                NavigationIcon(
                                    navItem = navItem,
                                    dynamicUnreadCount = dynamicUnreadCount
                                )
                            },
                            label = { Text(navItem.displayName) },
                            selected = mobileMainScreenState.currentNavItem == navItem,
                            onClick = { onNavigate(navItem) }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                )
            }
        }

        NavigationSuiteType.NavigationRail -> {
            NavigationRail(
                modifier = modifier,
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
            ) {
                NavigationRailItem(
                    icon = {
                        if (avatar.isBlank()) {
                            Icon(Icons.Rounded.Person, contentDescription = "User Avatar")
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            ) {
                                AsyncImage(
                                    modifier = Modifier
                                        .size(36.dp),
                                    model = avatar,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    },
                    selected = false,
                    onClick = onOpenMine
                )
                NavigationRailItem(
                    icon = {
                        Icon(
                            imageVector = MobileMainScreenNav.Search.icon,
                            contentDescription = MobileMainScreenNav.Search.displayName
                        )
                    },
                    selected = mobileMainScreenState.currentNavItem == MobileMainScreenNav.Search,
                    onClick = { onNavigate(MobileMainScreenNav.Search) }
                )
                Spacer(Modifier.weight(1f))
                listOf(
                    MobileMainScreenNav.Home,
                    MobileMainScreenNav.Dynamic,
                    MobileMainScreenNav.Setting,
                ).forEach { navItem ->
                    NavigationRailItem(
                        icon = {
                            NavigationIcon(
                                navItem = navItem,
                                dynamicUnreadCount = dynamicUnreadCount
                            )
                        },
                        label = { Text(navItem.displayName) },
                        selected = mobileMainScreenState.currentNavItem == navItem,
                        onClick = { onNavigate(navItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationIcon(
    navItem: MobileMainScreenNav,
    dynamicUnreadCount: Int
) {
    if (navItem == MobileMainScreenNav.Dynamic && dynamicUnreadCount > 0) {
        val badgeText = if (dynamicUnreadCount > 99) "99+" else dynamicUnreadCount.toString()
        val density = LocalDensity.current
        val fixedBadgeDensity = remember(density.density) {
            Density(density = density.density, fontScale = 1f)
        }
        BadgedBox(
            badge = {
                CompositionLocalProvider(LocalDensity provides fixedBadgeDensity) {
                    Badge(
                        modifier = if (dynamicUnreadCount < 10) {
                            Modifier.size(16.dp)
                        } else {
                            Modifier
                        }
                    ) {
                        Text(text = badgeText)
                    }
                }
            }
        ) {
            Icon(navItem.icon, contentDescription = navItem.displayName)
        }
    } else {
        Icon(navItem.icon, contentDescription = navItem.displayName)
    }
}

data class MobileMainScreenState(
    val context: Context,
    val scope: CoroutineScope,
    val windowSizeClass: WindowSizeClass,
    val rcmdGridState: LazyGridState,
    val popularGridState: LazyGridState,
    val dynamicGridState: LazyStaggeredGridState,
    val navController: NavHostController,
    val currentBackStackEntry: NavBackStackEntry?,
    val currentNavItem: MobileMainScreenNav,
    private val homeViewModel: PopularViewModel,
    private val userViewModel: UserViewModel,
    private val userSwitchViewModel: UserSwitchViewModel,
) {
    companion object {
        val logger = KotlinLogging.logger {}
    }

    var activeSearch by mutableStateOf(false)

    fun navigate(navItem: MobileMainScreenNav) {
        logger.fInfo { "Navigate to ${navItem.name}" }

        val navigateToRoute: () -> Unit = {
            val route = navItem.name
            navController.navigate(route) {
                launchSingleTop = true
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                    saveState = true
                }
                restoreState = true
            }
        }

        val notCurrentNavItem = currentNavItem != navItem

        when (navItem) {
            MobileMainScreenNav.Home -> {
                if (notCurrentNavItem) {
                    navigateToRoute()
                } else {
                    scope.launch { rcmdGridState.animateScrollToItem(0) }
                    scope.launch { popularGridState.animateScrollToItem(0) }
                }
            }

            MobileMainScreenNav.Search, MobileMainScreenNav.History, MobileMainScreenNav.Favorite -> {
                if (notCurrentNavItem) {
                    navigateToRoute()
                }
            }

            MobileMainScreenNav.Setting -> {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }

            MobileMainScreenNav.Dynamic -> {
                if (notCurrentNavItem) {
                    navigateToRoute()
                } else {
                    scope.launch { dynamicGridState.animateScrollToItem(0) }
                }
            }

            MobileMainScreenNav.Mine -> {
                if (notCurrentNavItem) {
                    navigateToRoute()
                }
            }
        }

        @SuppressLint("RestrictedApi")
        val breadcrumb = navController
            .currentBackStack
            .value
            .map { it.destination }
            .filterNot { it is NavGraph }
            .joinToString(" > ") { it.route ?: "null" }
        logger.fInfo { "Navigation Stack: > $breadcrumb" }
    }

}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberMobileMainScreenState(
    context: Context = LocalContext.current,
    scope: CoroutineScope = rememberCoroutineScope(),
    windowSizeClass: WindowSizeClass = calculateWindowSizeClass(context as Activity),
    rcmdGridState: LazyGridState = rememberLazyGridState(),
    popularGridState: LazyGridState = rememberLazyGridState(),
    dynamicGridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    navController: NavHostController = rememberNavController(),
    popularViewModel: PopularViewModel,//= koinNavViewModel(),
    userViewModel: UserViewModel,//= koinNavViewModel(),
    userSwitchViewModel: UserSwitchViewModel //= koinNavViewModel()
): MobileMainScreenState {
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavItem by remember {
        derivedStateOf {
            MobileMainScreenNav.fromName(currentBackStackEntry?.destination?.route ?: "")
        }
    }

    LaunchedEffect(Unit) {
        userViewModel.updateUserInfo()
    }

    DisposableEffect(lifecycleOwner) {
        var leaveFromThisPage = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                leaveFromThisPage = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (leaveFromThisPage) {
                    scope.launch(Dispatchers.IO) {
                        userSwitchViewModel.updateUserDbList()
                    }
                }
                leaveFromThisPage = false
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return remember(
        context,
        scope,
        windowSizeClass,
        rcmdGridState,
        popularGridState,
        dynamicGridState,
        navController,
        currentNavItem
    ) {
        MobileMainScreenState(
            context,
            scope,
            windowSizeClass,
            rcmdGridState,
            popularGridState,
            dynamicGridState,
            navController,
            currentBackStackEntry,
            currentNavItem,
            popularViewModel,
            userViewModel,
            userSwitchViewModel
        )
    }
}

enum class MobileMainScreenNav(val displayName: String, val icon: ImageVector) {
    Home("首页", Icons.Rounded.Home),
    Search("搜索", Icons.Rounded.Search),
    Dynamic("动态", Icons.Rounded.FiberNew),
    History("历史", Icons.Rounded.History),
    Favorite("收藏", Icons.Rounded.Favorite),
    Mine("我的", Icons.Rounded.Person),
    Setting("设置", Icons.Rounded.Settings), ;

    companion object {
        fun fromName(name: String) = entries.firstOrNull { it.name == name } ?: Home

        fun fromBottomNavItem(item: MobileBottomNavItem): MobileMainScreenNav = when (item) {
            MobileBottomNavItem.Home -> Home
            MobileBottomNavItem.Dynamic -> Dynamic
            MobileBottomNavItem.History -> History
            MobileBottomNavItem.Favorite -> Favorite
            MobileBottomNavItem.Mine -> Mine
            MobileBottomNavItem.Setting -> Setting
        }
    }
}
