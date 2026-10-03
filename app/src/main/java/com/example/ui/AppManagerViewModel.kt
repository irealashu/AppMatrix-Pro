package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppManagerRepository
import com.example.data.AppMatrixDatabase
import com.example.model.AppModel
import com.example.model.AppNoteEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppFilter {
    ALL, USER, SYSTEM
}

enum class AppSortOption {
    NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC, INSTALL_DATE_DESC, INSTALL_DATE_ASC
}

class AppManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppMatrixDatabase::class.java,
        "app_matrix.db"
    ).fallbackToDestructiveMigration().build()

    val repository = AppManagerRepository(application, db)

    private val _rawApps = MutableStateFlow<List<AppModel>>(emptyList())
    private val _appNotesMap = MutableStateFlow<Map<String, AppNoteEntity>>(emptyMap())

    private val prefs = application.getSharedPreferences("app_matrix_preferences", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(
        runCatching {
            AppFilter.valueOf(prefs.getString("pref_selected_filter", AppFilter.ALL.name) ?: AppFilter.ALL.name)
        }.getOrDefault(AppFilter.ALL)
    )
    val selectedFilter: StateFlow<AppFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(
        runCatching {
            AppSortOption.valueOf(prefs.getString("pref_selected_sort", AppSortOption.NAME_ASC.name) ?: AppSortOption.NAME_ASC.name)
        }.getOrDefault(AppSortOption.NAME_ASC)
    )
    val selectedSort: StateFlow<AppSortOption> = _selectedSort.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _selectedApp = MutableStateFlow<AppModel?>(null)
    val selectedApp: StateFlow<AppModel?> = _selectedApp.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val apps: StateFlow<List<AppModel>> = combine(
        _rawApps,
        _appNotesMap,
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { raw, notes, query, filter, sort ->
        val merged = raw.map { app ->
            val noteEntity = notes[app.packageName]
            if (noteEntity != null) {
                app.copy(customNote = noteEntity.note)
            } else app
        }

        val trimmedQuery = query.trim()
        val tokens = if (trimmedQuery.isEmpty()) emptyList() else trimmedQuery.split(Regex("\\s+"))

        val filtered = merged.filter { app ->
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystemApp
                AppFilter.SYSTEM -> app.isSystemApp
            }
            if (!matchesFilter) return@filter false

            if (tokens.isEmpty()) return@filter true

            val appTypeKeyword = if (app.isSystemApp) "system sys" else "user thirdparty installed"
            val sdkString = "sdk ${app.targetSdkVersion} api ${app.targetSdkVersion} target ${app.targetSdkVersion} min ${app.minSdkVersion}"
            val versionString = "v${app.versionName} ${app.versionName} code ${app.versionCode}"

            tokens.all { token ->
                app.appName.contains(token, ignoreCase = true) ||
                app.packageName.contains(token, ignoreCase = true) ||
                app.versionName.contains(token, ignoreCase = true) ||
                versionString.contains(token, ignoreCase = true) ||
                sdkString.contains(token, ignoreCase = true) ||
                appTypeKeyword.contains(token, ignoreCase = true) ||
                app.customNote.contains(token, ignoreCase = true) ||
                app.category.name.contains(token, ignoreCase = true)
            }
        }

        when (sort) {
            AppSortOption.NAME_ASC -> filtered.sortedBy { it.appName.lowercase() }
            AppSortOption.NAME_DESC -> filtered.sortedByDescending { it.appName.lowercase() }
            AppSortOption.SIZE_DESC -> filtered.sortedByDescending { it.apkSizeBytes }
            AppSortOption.SIZE_ASC -> filtered.sortedBy { it.apkSizeBytes }
            AppSortOption.INSTALL_DATE_DESC -> filtered.sortedByDescending { it.installTimeMs }
            AppSortOption.INSTALL_DATE_ASC -> filtered.sortedBy { it.installTimeMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadNotes()
        loadApps()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            repository.getAppNotesFlow().collect { notesMap ->
                _appNotesMap.value = notesMap
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val loaded = repository.loadInstalledApps()
            _rawApps.value = loaded
            _isLoadingApps.value = false
            _isRefreshing.value = false
        }
    }

    fun refreshApps() {
        _isRefreshing.value = true
        loadApps()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter) {
        _selectedFilter.value = filter
        prefs.edit().putString("pref_selected_filter", filter.name).apply()
    }

    fun setSort(sort: AppSortOption) {
        _selectedSort.value = sort
        prefs.edit().putString("pref_selected_sort", sort.name).apply()
    }

    fun selectApp(app: AppModel?) {
        _selectedApp.value = app
    }

    fun extractApk(app: AppModel) {
        viewModelScope.launch {
            _userMessage.value = "Extracting APK for ${app.appName}..."
            val resultPath = repository.extractApk(app.packageName)
            if (resultPath != null) {
                _userMessage.value = "APK saved to: $resultPath"
            } else {
                _userMessage.value = "Failed to extract APK for ${app.appName}."
            }
        }
    }

    fun saveAppNote(packageName: String, note: String, tags: String) {
        viewModelScope.launch {
            repository.saveAppNote(packageName, note, tags)
            _userMessage.value = "Saved note for $packageName"
        }
    }

    fun launchApp(app: AppModel) {
        val success = repository.launchApp(app.packageName)
        if (!success) {
            _userMessage.value = "Cannot launch ${app.appName} directly (no launcher Activity)"
        }
    }

    fun openSystemSettings(app: AppModel) {
        repository.openAppDetailsSettings(app.packageName)
    }

    fun requestUninstall(app: AppModel) {
        repository.requestUninstallApp(app.packageName)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
