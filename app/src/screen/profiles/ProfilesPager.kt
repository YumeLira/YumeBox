/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */


package com.github.yumelira.yumebox.screen.profiles
import com.github.yumelira.yumebox.presentation.theme.UiDp
import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.github.yumelira.yumebox.App
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.controller.OverrideService
import com.github.yumelira.yumebox.data.model.ProfileBinding
import com.github.yumelira.yumebox.data.store.ProfileBindingProvider
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.LocalNavigator
import com.github.yumelira.yumebox.presentation.icon.ShellIcons
import com.github.yumelira.yumebox.presentation.util.OverrideEditorStore
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.github.yumelira.yumebox.screen.home.HomeViewModel
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.ramcosta.composedestinations.generated.destinations.OverrideConfigPreviewRouteDestination
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.*

@SuppressLint("UseKtx")
@Composable
fun ProfilesPager(mainInnerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()

    val overrideConfigViewModel = koinViewModel<OverrideConfigViewModel>()
    val bindingProvider: ProfileBindingProvider = koinInject()
    val overrideService: OverrideService = koinInject()
    val userConfigs by overrideConfigViewModel.userConfigs.collectAsState()
    val context = LocalContext.current

    val showAddBottomSheet = remember { mutableStateOf(false) }
    var isDeleteDialogVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Profile?>(null) }
    val showSettingsDialog = remember { mutableStateOf(false) }
    val showShareDialog = remember { mutableStateOf(false) }
    var isEditOptionsDialogVisible by remember { mutableStateOf(false) }
    var showEditOptionsDialog by remember { mutableStateOf<Profile?>(null) }
    var openConfigPreviewAfterEditDialogDismiss by remember { mutableStateOf(false) }
    var profileToShare by remember { mutableStateOf<Profile?>(null) }
    var profileToEdit by remember { mutableStateOf<Profile?>(null) }
    var profileBinding by remember { mutableStateOf<ProfileBinding?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    var importUrlFromScheme by remember { mutableStateOf<String?>(null) }
    val pendingImportUrl by MainActivity.pendingImportUrl.collectAsState()
    val urlProfiles = remember(profiles) {
        profiles.filter { it.type == Profile.Type.Url }
    }

    var scannedUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingImportUrl) {
        if (pendingImportUrl != null) {
            importUrlFromScheme = pendingImportUrl
            profileToEdit = null
            showAddBottomSheet.value = true
            MainActivity.clearPendingImportUrl()
        }
    }

    LaunchedEffect(showSettingsDialog.value) {
        if (showSettingsDialog.value) {
            overrideConfigViewModel.refresh()
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.ProfilesPage.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = UiDp.dp12),
                        onClick = {
                            if (!isDownloading && urlProfiles.isNotEmpty()) {
                                isDownloading = true
                                scope.launch {
                                    urlProfiles.forEach { profile ->
                                        profilesViewModel.updateProfile(profile.uuid)
                                    }
                                    isDownloading = false
                                }
                            }
                        }
                    ) {
                        Icon(
                            ShellIcons.UpdateProfiles,
                            contentDescription = MLang.ProfilesPage.Action.UpdateAll
                        )
                    }

                    IconButton(
                        onClick = {
                            profileToEdit = null
                            showAddBottomSheet.value = true
                        }
                    ) {
                        Icon(
                            ShellIcons.AddProfile,
                            contentDescription = MLang.ProfilesPage.Action.AddProfile
                        )
                    }
                })
        },
    ) { innerPadding ->
        if (profiles.isEmpty()) {

            CenteredText(
                firstLine = MLang.ProfilesPage.Empty.NoProfiles,
                secondLine = MLang.ProfilesPage.Empty.Hint
            )
        } else {
            val lazyListState = rememberLazyListState()
            val reorderableLazyListState =
                rememberReorderableLazyListState(lazyListState) { from, to ->
                    profilesViewModel.reorderProfiles(from.index, to.index)
                }

            ScreenLazyColumn(
                lazyListState = lazyListState,
                scrollBehavior = scrollBehavior,
                innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
                topPadding = UiDp.dp20,
            ) {
                items(
                    items = profiles,
                    key = { it.uuid.toString() }
                ) { profile ->
                    ReorderableItem(
                        reorderableLazyListState,
                        key = profile.uuid.toString()
                    ) { isDragging ->
                        ProfileCard(
                            profile = profile,
                            workDir = App.instance.filesDir.resolve("imported"),
                            isDownloading = isDownloading,
                            modifier = Modifier
                                .longPressDraggableHandle()
                                .alpha(if (isDragging) 0.9f else 1f),
                            onExport = { profile ->
                                if (!isDownloading) {
                                    profileToShare = profile
                                    showShareDialog.value = true
                                }
                            },
                            onUpdate = { profile ->
                                if (!isDownloading) {
                                    isDownloading = true
                                    scope.launch {
                                        profilesViewModel.updateProfile(profile.uuid)
                                        isDownloading = false
                                    }
                                }
                            },
                            onDelete = { profile ->
                                if (!isDownloading) {
                                    showDeleteDialog = profile
                                    isDeleteDialogVisible = true
                                }
                            },
                            onEdit = { profile ->
                                if (!isDownloading) {
                                    showEditOptionsDialog = profile
                                    isEditOptionsDialogVisible = true
                                }
                            },
                            onToggleEnabled = { profile ->
                                if (!isDownloading) {
                                    scope.launch {
                                        if (profile.active && isRunning) {
                                            homeViewModel.stopProxy()
                                        }
                                        profilesViewModel.toggleProfileEnabled(profile.uuid)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    AddProfileSheet(
        show = showAddBottomSheet,
        profileToEdit = profileToEdit,
        importUrl = importUrlFromScheme ?: scannedUrl,
        onAddProfile = { name, source, type, interval, fileUri ->
            profilesViewModel.createProfile(type, name, source, interval, fileUri)
        },
        onUpdateProfile = { uuid, name, source, interval ->
            profilesViewModel.patchProfile(uuid, name, source, interval)
        },
        onDownloadComplete = {
            isDownloading = false
            showAddBottomSheet.value = false
            profilesViewModel.clearDownloadProgress()
        },
        profilesViewModel = profilesViewModel
    )

    showDeleteDialog?.let { profile ->
        DeleteConfirmDialog(
            show = isDeleteDialogVisible,
            profileName = profile.name,
            onConfirm = {
            isDeleteDialogVisible = false
            profilesViewModel.deleteProfile(profile.uuid)
        },
            onDismiss = { isDeleteDialogVisible = false },
            onDismissFinished = { showDeleteDialog = null },
        )
    }

    val currentProfileToEdit = profileToEdit
    if (currentProfileToEdit != null) {
        ProfileSettingsDialog(
            show = showSettingsDialog.value,
            profile = currentProfileToEdit,
            userConfigs = userConfigs,
            binding = profileBinding,
            onDismiss = {
                showSettingsDialog.value = false
            },
            onDismissFinished = {
                profileToEdit = null
                profileBinding = null
            },
            onSaveProfileMeta = { newName, newSource ->
                if (newName.isNotBlank() && newSource.isNotBlank()) {
                    profilesViewModel.patchProfile(
                        currentProfileToEdit.uuid,
                        newName,
                        newSource,
                        currentProfileToEdit.interval
                    )
                }
            },
            onSaveOverrideSettings = { selectedOverrideIds ->
                scope.launch {
                    val profileId = currentProfileToEdit.uuid.toString()
                    val normalizedOverrideIds = selectedOverrideIds.distinct()
                    val currentBinding = profileBinding ?: bindingProvider.getBinding(profileId)
                    val updatedBinding = currentBinding?.copy(
                        overrideIds = normalizedOverrideIds,
                    ) ?: ProfileBinding(
                        profileId = profileId,
                        overrideIds = normalizedOverrideIds,
                    )

                    bindingProvider.setBinding(updatedBinding)
                    profileBinding = bindingProvider.getBinding(profileId)

                    if (isRunning && homeViewModel.isCurrentProfile(currentProfileToEdit.uuid)) {
                        overrideService.applyOverride(profileId)
                    }
                }
            },
        )
    }

    profileToShare?.let { profile ->
        ShareOptionsDialog(show = showShareDialog.value, profile = profile, onDismiss = {
            showShareDialog.value = false
        }, onDismissFinished = {
            profileToShare = null
        }, onShareFile = { profile ->
            val file = importedConfigFile(profile)

            if (!file.exists()) {
                context.toast(MLang.ProfilesPage.ShareDialog.ImportedConfigMissing.format(file.absolutePath))
            } else {
                runCatching {
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/x-yaml"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent, MLang.ProfilesPage.ShareDialog.ShareFile
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                }.onFailure {
                    context.toast(it.message ?: "Share failed")
                }
            }
            showShareDialog.value = false
        }, onShareLink = { profile ->
            val context = App.instance
            val url = if (profile.type == Profile.Type.Url) profile.source else null
            url?.let {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, it)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(
                        intent, MLang.ProfilesPage.ShareDialog.ShareLink
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
            } ?: run {
                context.toast(MLang.ProfilesPage.ShareDialog.NoLink)
            }
            showShareDialog.value = false
        })
    }

    showEditOptionsDialog?.let { profile ->
        ProfileEditOptionsDialog(
            show = isEditOptionsDialogVisible,
            onOpenConfig = {
                openConfigPreviewAfterEditDialogDismiss = false
                isEditOptionsDialogVisible = false
                openProfileConfigPreview(
                    targetFile = importedConfigFile(profile),
                    missingMessage = MLang.ProfilesPage.SettingsDialog.ConfigMissing.format(importedConfigFile(profile).absolutePath),
                    editable = true,
                    onReadFailed = context::toast,
                ) { configContent, callback ->
                    OverrideEditorStore.setupConfigPreview(
                        title = profile.name,
                        content = configContent,
                        language = LanguageScope.Yaml,
                        callback = callback,
                    )
                    openConfigPreviewAfterEditDialogDismiss = true
                }
            },
            onEditSettings = {
                openConfigPreviewAfterEditDialogDismiss = false
                isEditOptionsDialogVisible = false
                profileToEdit = profile
                scope.launch {
                    profileBinding = bindingProvider.getBinding(profile.uuid.toString())
                }
                showSettingsDialog.value = true
            },
            onDismiss = {
                openConfigPreviewAfterEditDialogDismiss = false
                isEditOptionsDialogVisible = false
            },
            onDismissFinished = {
                showEditOptionsDialog = null
                if (openConfigPreviewAfterEditDialogDismiss) {
                    openConfigPreviewAfterEditDialogDismiss = false
                    navigator.navigate(OverrideConfigPreviewRouteDestination)
                }
            },
        )
    }
}
