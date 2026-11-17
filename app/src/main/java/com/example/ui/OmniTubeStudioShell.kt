package com.example.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.signOutFromStudio
import com.example.data.model.StudioCatalog
import com.example.data.model.StudioConfig
import com.example.ui.components.AutoPilotScheduleManagerSheet
import com.example.ui.components.DrawerTabShortcut
import com.example.ui.components.GeminiSidebarDrawerContent
import com.example.ui.components.TelemetryPill
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardTabScreen
import com.example.ui.screens.IntelYppTabScreen
import com.example.ui.screens.SetupGuideTabScreen
import com.example.ui.screens.VideoEngineTabScreen
import com.example.ui.theme.BroadcastCrimson
import com.example.ui.theme.ObsidianFloor
import com.example.ui.theme.PolicyRed
import com.example.ui.theme.RackSurface
import com.example.ui.theme.RackSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.YppEmerald
import com.example.ui.viewmodel.StreamUiState
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val MAX_DRAWER_VIDEO_BYTES = 18 * 1024 * 1024

private const val ROUTE_COMMAND = "command"
private const val ROUTE_STUDIO = "studio"
private const val ROUTE_INTEL = "intel"
private const val ROUTE_SETUP = "setup"

private data class StudioTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val studioTabs = listOf(
    StudioTab(
        route = ROUTE_COMMAND,
        label = "Command",
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "tab_command"
    ),
    StudioTab(
        route = ROUTE_STUDIO,
        label = "Engine",
        selectedIcon = Icons.Rounded.Movie,
        unselectedIcon = Icons.Outlined.Movie,
        testTag = "tab_engine"
    ),
    StudioTab(
        route = ROUTE_INTEL,
        label = "YPP & Intel",
        selectedIcon = Icons.Rounded.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        testTag = "tab_intel"
    ),
    StudioTab(
        route = ROUTE_SETUP,
        label = "Setup Guide",
        selectedIcon = Icons.Rounded.School,
        unselectedIcon = Icons.Outlined.School,
        testTag = "tab_setup"
    )
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun OmniTubeAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: StudioViewModel = viewModel(
                key = user.uid,
                factory = StudioViewModel.provideFactory(
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "Creator",
                    userEmail = user.email ?: ""
                )
            )
            OmniTubeStudioShell(
                userEmail = user.email ?: "",
                viewModel = viewModel
            )
        }
    }
}

private fun navigateToStudioTab(navController: NavHostController, route: String) {
    if (navController.currentDestination == null) return
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OmniTubeStudioShell(
    userEmail: String,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snapshotState by viewModel.snapshotState.collectAsStateWithLifecycle()
    val transient by viewModel.transient.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showScheduleSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val drawerPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        viewModel.setReferenceBitmap(bmp)
                        navigateToStudioTab(navController, ROUTE_STUDIO)
                        scope.launch {
                            drawerState.close()
                            snackbarHostState.showSnackbar("Photo attached to AI Video & Thumbnail Engine.")
                        }
                    } else {
                        Toast.makeText(context, "Could not decode selected photo.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: "Photo upload failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val drawerVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    Toast.makeText(context, "Could not read selected video.", Toast.LENGTH_SHORT).show()
                } else if (bytes.size > MAX_DRAWER_VIDEO_BYTES) {
                    Toast.makeText(
                        context,
                        "Video exceeds 18MB inline limit. Pick a shorter clip.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val mime = context.contentResolver.getType(uri) ?: "video/mp4"
                    viewModel.analyzePickedVideoBytes("Sidebar Clip Upload", bytes, mime)
                    navigateToStudioTab(navController, ROUTE_INTEL)
                    scope.launch {
                        drawerState.close()
                        snackbarHostState.showSnackbar("Video uploaded for Gemini YPP & retention audit.")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: "Video upload failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(transient.statusBanner) {
        val msg = transient.statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    val readyBundle = (snapshotState as? StreamUiState.Ready)?.data
    val studioConfig = readyBundle?.config ?: StudioConfig()
    val isArmed = studioConfig.isMasterAutopilotOn
    val dailyFreq = studioConfig.dailyVideoFrequency
    val activeTrack = StudioCatalog.funkTracks.firstOrNull {
        it.id == studioConfig.activeFunkTrackId
    } ?: StudioCatalog.funkTracks.first()

    val drawerTabs = remember {
        studioTabs.map {
            DrawerTabShortcut(it.route, it.label, it.selectedIcon, it.unselectedIcon)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = RackSurface,
                drawerContentColor = StudioTextPrimary,
                windowInsets = WindowInsets.safeDrawing,
                modifier = Modifier.widthIn(max = 328.dp)
            ) {
                GeminiSidebarDrawerContent(
                    userEmail = userEmail,
                    config = studioConfig,
                    activeTrackTitle = "${activeTrack.title} (${activeTrack.bpm} BPM)",
                    queuedCount = readyBundle?.batches?.size ?: 0,
                    hasReferencePhoto = transient.referenceBitmap != null,
                    currentRoute = currentDestination?.route,
                    tabs = drawerTabs,
                    onClose = { scope.launch { drawerState.close() } },
                    onUploadPhoto = {
                        drawerPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUploadVideo = {
                        drawerVideoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onManageSchedules = {
                        scope.launch { drawerState.close() }
                        showScheduleSheet = true
                    },
                    onToggleMaster = viewModel::updateMasterSwitch,
                    onSelectTab = { route ->
                        navigateToStudioTab(navController, route)
                        scope.launch { drawerState.close() }
                    },
                    onSignOut = {
                        scope.launch { drawerState.close() }
                        signOutFromStudio(credentialManager, scope)
                    }
                )
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianFloor)
        ) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = ObsidianFloor,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("open_sidebar_drawer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Open OmniTube Gemini Sidebar",
                                    tint = StudioTextPrimary
                                )
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "OmniTube Gemini Studio",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = StudioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            Row(
                                modifier = Modifier.padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TelemetryPill(
                                    text = "$dailyFreq/DAY",
                                    color = TelemetryCyan
                                )
                                TelemetryPill(
                                    text = if (isArmed) "ON" else "OFF",
                                    color = if (isArmed) YppEmerald else BroadcastCrimson
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = RackSurface,
                            titleContentColor = StudioTextPrimary
                        )
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = RackSurface,
                            contentColor = StudioTextPrimary
                        ) {
                            studioTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToStudioTab(navController, tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(tab.testTag),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BroadcastCrimson,
                                        selectedTextColor = StudioTextPrimary,
                                        indicatorColor = RackSurfaceElevated,
                                        unselectedIconColor = StudioTextSecondary,
                                        unselectedTextColor = StudioTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = RackSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            studioTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToStudioTab(navController, tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = BroadcastCrimson,
                                        selectedTextColor = StudioTextPrimary,
                                        indicatorColor = RackSurfaceElevated,
                                        unselectedIconColor = StudioTextSecondary,
                                        unselectedTextColor = StudioTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxSize()
                        ) {
                            when (val state = snapshotState) {
                                is StreamUiState.Loading -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            color = TelemetryCyan,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Hydrating Broadcast Control Room...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StudioTextSecondary
                                        )
                                    }
                                }
                                is StreamUiState.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Studio Sync Error",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = PolicyRed
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StudioTextSecondary
                                        )
                                    }
                                }
                                is StreamUiState.Ready -> {
                                    NavHost(
                                        navController = navController,
                                        startDestination = ROUTE_COMMAND
                                    ) {
                                        composable(ROUTE_COMMAND) {
                                            DashboardTabScreen(
                                                bundle = state.data,
                                                transient = transient,
                                                viewModel = viewModel
                                            )
                                        }
                                        composable(ROUTE_STUDIO) {
                                            VideoEngineTabScreen(
                                                bundle = state.data,
                                                transient = transient,
                                                viewModel = viewModel
                                            )
                                        }
                                        composable(ROUTE_INTEL) {
                                            IntelYppTabScreen(
                                                bundle = state.data,
                                                transient = transient,
                                                viewModel = viewModel
                                            )
                                        }
                                        composable(ROUTE_SETUP) {
                                            SetupGuideTabScreen(
                                                bundle = state.data,
                                                transient = transient,
                                                userEmail = userEmail,
                                                viewModel = viewModel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            containerColor = RackSurface,
            contentColor = StudioTextPrimary
        ) {
            AutoPilotScheduleManagerSheet(
                config = studioConfig,
                queuedCount = readyBundle?.batches?.size ?: 0,
                onToggleMaster = viewModel::updateMasterSwitch,
                onFrequencyChange = viewModel::updateDailyFrequency,
                onContentTypeChange = viewModel::updateContentType,
                onFormatModeChange = viewModel::updateFormatMode,
                onDispatchBatch = {
                    viewModel.generateDailyBatchFromAi()
                    showScheduleSheet = false
                    navigateToStudioTab(navController, ROUTE_COMMAND)
                },
                onOpenQueueTab = {
                    showScheduleSheet = false
                    navigateToStudioTab(navController, ROUTE_COMMAND)
                }
            )
        }
    }
}
