package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppManagerViewModel
import com.example.ui.components.AppDetailSheet
import com.example.ui.screens.AppsListScreen
import com.example.ui.theme.AppMatrixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppMatrixTheme {
                AppMatrixApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMatrixApp(viewModel: AppManagerViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Direct Activity Result uninstaller launcher with instant auto-refresh
    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.selectApp(null)
        viewModel.refreshApps()
    }

    // 1. Live BroadcastReceiver for real-time app additions, updates, and deletions
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                viewModel.refreshApps()
                val pkgName = intent?.data?.schemeSpecificPart
                if (pkgName != null && selectedApp?.packageName == pkgName &&
                    (intent.action == Intent.ACTION_PACKAGE_REMOVED || intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED)
                ) {
                    viewModel.selectApp(null)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if unregister fails
            }
        }
    }

    // 2. Auto-refresh when app resumes from background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    BackHandler(enabled = selectedApp != null) {
        viewModel.selectApp(null)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshApps() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppsListScreen(
                apps = apps,
                isLoading = isLoadingApps,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                selectedSort = selectedSort,
                iconProvider = { pkg -> viewModel.repository.getAppIconBitmap(pkg) },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onFilterSelect = { viewModel.setFilter(it) },
                onSortSelect = { viewModel.setSort(it) },
                onAppSelect = { viewModel.selectApp(it) },
                onExtractApk = { viewModel.extractApk(it) },
                onRefresh = { viewModel.refreshApps() }
            )
        }

        // App Detail Sheet
        selectedApp?.let { app ->
            AppDetailSheet(
                app = app,
                iconProvider = { pkg -> viewModel.repository.getAppIconBitmap(pkg) },
                onDismiss = { viewModel.selectApp(null) },
                onLaunch = { viewModel.launchApp(app) },
                onOpenSettings = { viewModel.openSystemSettings(app) },
                onExtractApk = { viewModel.extractApk(app) },
                onRequestUninstall = {
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:${app.packageName}")
                            putExtra(Intent.EXTRA_RETURN_RESULT, true)
                        }
                        uninstallLauncher.launch(intent)
                    } catch (e: Exception) {
                        viewModel.requestUninstall(app)
                    }
                }
            )
        }
    }
}
