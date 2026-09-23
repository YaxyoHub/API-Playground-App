package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.localization.AppLanguage
import kotlinx.coroutines.flow.collectLatest

enum class NavigationTab(
    val icon: ImageVector,
    val testTag: String
) {
    REQUEST(Icons.Default.Send, "nav_request"),
    COLLECTIONS(Icons.Default.Folder, "nav_collections"),
    ENVIRONMENTS(Icons.Default.Tune, "nav_environments"),
    HISTORY(Icons.Default.History, "nav_history"),
    MONITOR(Icons.Default.Speed, "nav_monitor")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ApiPlaygroundViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(NavigationTab.REQUEST) }
    val snackbarHostState = remember { SnackbarHostState() }
    val collections by viewModel.collections.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val strings by viewModel.strings.collectAsState()

    var showLanguageDropdown by remember { mutableStateOf(false) }

    // Listen for toast messages from ViewModel
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("app_snackbar_host")
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = strings.appTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 0.3.sp
                        )
                    }
                },
                actions = {
                    // Language Switcher Button
                    Box {
                        Surface(
                            onClick = { showLanguageDropdown = true },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("language_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = CyanGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = currentLanguage.flag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLanguageDropdown,
                            onDismissRequest = { showLanguageDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            AppLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(lang.flag, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyanGlow)
                                            Text(
                                                lang.displayName,
                                                fontWeight = if (currentLanguage == lang) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setLanguage(lang)
                                        showLanguageDropdown = false
                                    },
                                    trailingIcon = if (currentLanguage == lang) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.testTag("lang_option_${lang.code}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Theme Toggle Button (Light Mode <-> Dark Mode)
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) strings.themeLight else strings.themeDark,
                            tint = if (isDarkMode) AmberWarning else CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 2.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .testTag("bottom_navigation_bar")
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val tabLabel = when (tab) {
                        NavigationTab.REQUEST -> strings.tabRequest
                        NavigationTab.COLLECTIONS -> strings.tabCollections
                        NavigationTab.ENVIRONMENTS -> strings.tabEnvironments
                        NavigationTab.HISTORY -> strings.tabHistory
                        NavigationTab.MONITOR -> strings.tabStatus
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tabLabel,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tabLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanGlow,
                            selectedTextColor = CyanGlow,
                            indicatorColor = CyanAccent.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                NavigationTab.REQUEST -> {
                    RequestBuilderScreen(
                        viewModel = viewModel,
                        collections = collections,
                        onNavigateToEnvironments = { currentTab = NavigationTab.ENVIRONMENTS },
                        strings = strings
                    )
                }
                NavigationTab.COLLECTIONS -> {
                    CollectionsScreen(
                        viewModel = viewModel,
                        onRequestSelected = { currentTab = NavigationTab.REQUEST },
                        strings = strings
                    )
                }
                NavigationTab.ENVIRONMENTS -> {
                    EnvironmentsScreen(
                        viewModel = viewModel,
                        strings = strings
                    )
                }
                NavigationTab.HISTORY -> {
                    HistoryCompareScreen(
                        viewModel = viewModel,
                        onRequestLoaded = { currentTab = NavigationTab.REQUEST },
                        strings = strings
                    )
                }
                NavigationTab.MONITOR -> {
                    HealthMonitorScreen(
                        viewModel = viewModel,
                        strings = strings
                    )
                }
            }
        }
    }
}
