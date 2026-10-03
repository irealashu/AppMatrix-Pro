package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppModel
import com.example.ui.AppFilter
import com.example.ui.AppSortOption
import com.example.ui.components.AppCardItem

@Composable
fun AppsListScreen(
    apps: List<AppModel>,
    isLoading: Boolean,
    searchQuery: String,
    selectedFilter: AppFilter,
    selectedSort: AppSortOption,
    iconProvider: (String) -> ImageBitmap?,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (AppFilter) -> Unit,
    onSortSelect: (AppSortOption) -> Unit,
    onAppSelect: (AppModel) -> Unit,
    onExtractApk: (AppModel) -> Unit,
    onRefresh: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar with Sort Menu
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by app name or package...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("btn_sort_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort Options",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { if (selectedSort == AppSortOption.NAME_ASC) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                text = { Text("Name (A - Z)") },
                                onClick = { onSortSelect(AppSortOption.NAME_ASC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { if (selectedSort == AppSortOption.NAME_DESC) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                text = { Text("Name (Z - A)") },
                                onClick = { onSortSelect(AppSortOption.NAME_DESC); showSortMenu = false }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { if (selectedSort == AppSortOption.SIZE_DESC) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                text = { Text("APK Size (Largest first)") },
                                onClick = { onSortSelect(AppSortOption.SIZE_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { if (selectedSort == AppSortOption.SIZE_ASC) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                text = { Text("APK Size (Smallest first)") },
                                onClick = { onSortSelect(AppSortOption.SIZE_ASC); showSortMenu = false }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { if (selectedSort == AppSortOption.INSTALL_DATE_DESC) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                text = { Text("Install Date (Newest first)") },
                                onClick = { onSortSelect(AppSortOption.INSTALL_DATE_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { if (selectedSort == AppSortOption.INSTALL_DATE_ASC) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                text = { Text("Install Date (Oldest first)") },
                                onClick = { onSortSelect(AppSortOption.INSTALL_DATE_ASC); showSortMenu = false }
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_apps_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == AppFilter.ALL,
                    onClick = { onFilterSelect(AppFilter.ALL) },
                    label = { Text("All") },
                    leadingIcon = {
                        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("filter_all")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == AppFilter.USER,
                    onClick = { onFilterSelect(AppFilter.USER) },
                    label = { Text("User Apps") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("filter_user")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == AppFilter.SYSTEM,
                    onClick = { onFilterSelect(AppFilter.SYSTEM) },
                    label = { Text("System Apps") },
                    leadingIcon = {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("filter_system")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App Counts Pill
        if (!isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${apps.size} applications found",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )

                Text(
                    text = when (selectedSort) {
                        AppSortOption.NAME_ASC -> "Sorted by: Name (A-Z)"
                        AppSortOption.NAME_DESC -> "Sorted by: Name (Z-A)"
                        AppSortOption.SIZE_DESC -> "Sorted by: Size (Largest)"
                        AppSortOption.SIZE_ASC -> "Sorted by: Size (Smallest)"
                        AppSortOption.INSTALL_DATE_DESC -> "Sorted by: Date (Newest)"
                        AppSortOption.INSTALL_DATE_ASC -> "Sorted by: Date (Oldest)"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Apps List or Loading State
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading installed applications...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No applications match your criteria",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try clearing the search or changing the filter",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("apps_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    AppCardItem(
                        app = app,
                        iconProvider = iconProvider,
                        onClick = { onAppSelect(app) }
                    )
                }
            }
        }
    }
}
