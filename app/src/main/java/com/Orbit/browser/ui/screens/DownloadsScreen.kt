package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.data.db.Download
import com.orbit.browser.data.db.DownloadStatus
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.components.FrostedBackButton
import com.orbit.browser.ui.theme.LocalOBTheme

@Composable
fun DownloadsScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val theme        = LocalOBTheme.current
    val g            = theme.glass
    val isDark       = theme.isDark
    val a1           = theme.effectiveA1
    val dbDownloads  by viewModel.downloadsList.collectAsState()

    val localView = androidx.compose.ui.platform.LocalView.current
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()

    LaunchedEffect(activeTabId, dbDownloads.size) {
        if (activeTabId.isNotBlank() && localView.width > 0 && localView.height > 0) {
            val provider = com.orbit.browser.browser.preview.ComposePreviewProvider(localView, "Downloads")
            viewModel.previewManager.requestPreview(
                tabId = activeTabId,
                provider = provider,
                policy = com.orbit.browser.browser.preview.SchedulePolicy.Debounced(com.orbit.browser.browser.preview.PreviewTimingDefaults.COMPOSE_SETTLE_DELAY_MS)
            )
        }
    }

    val selectedIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedIds.isNotEmpty()

    androidx.activity.compose.BackHandler(enabled = isSelectionMode) {
        selectedIds.clear()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FrostedBackButton(
                    onClick = {
                        if (isSelectionMode) selectedIds.clear()
                        else viewModel.closeDownloads()
                    },
                    isDark = isDark,
                )

                Text(
                    text = if (isSelectionMode) "${selectedIds.size} Selected" else "Downloads",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = g.txColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                if (isSelectionMode) {
                    TextButton(onClick = { selectedIds.clear() }) {
                        Text("Cancel", fontWeight = FontWeight.Bold, color = a1)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 24.dp)
                            .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.65f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = { viewModel.openSearch() }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = g.txColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Downloads List or Empty State
            if (dbDownloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = g.tx2Color,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "No Downloads",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = g.txColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Files downloaded in Orbit will be listed here.",
                            fontSize = 12.sp,
                            color = g.tx2Color
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(dbDownloads, key = { it.id }) { dl ->
                        val isSelected = selectedIds.contains(dl.id)
                        RedesignedDownloadTile(
                            download = dl,
                            viewModel = viewModel,
                            theme = theme,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onToggleSelect = {
                                if (isSelected) selectedIds.remove(dl.id)
                                else selectedIds.add(dl.id)
                            },
                            onLongClick = {
                                if (!isSelected) selectedIds.add(dl.id)
                            }
                        )
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // FLOATING ISLAND ACTION BAR (Share | Rename | Delete)
        // ─────────────────────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            val selectedDownloads = dbDownloads.filter { selectedIds.contains(it.id) }

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp), blurRadius = 28.dp)
                    .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f))
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Share Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            shareFiles(context, selectedDownloads)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = a1,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = g.txColor)
                    }

                    // 2. Rename Action (Enabled when 1 item selected)
                    val canRename = selectedDownloads.size == 1
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .alpha(if (canRename) 1f else 0.4f)
                            .clickable(enabled = canRename) {
                                showRenameDialog = true
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = if (canRename) g.txColor else g.tx2Color,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Rename", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (canRename) g.txColor else g.tx2Color)
                    }

                    // 3. Delete Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            showDeleteDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        // Delete Options Dialog (Delete Files vs Remove from History)
        if (showDeleteDialog) {
            val selectedDownloads = dbDownloads.filter { selectedIds.contains(it.id) }
            DeleteOptionsDialog(
                itemCount = selectedDownloads.size,
                theme = theme,
                onDismiss = { showDeleteDialog = false },
                onConfirmDeleteFiles = {
                    selectedDownloads.forEach { dl -> viewModel.deleteDownload(dl, deleteFileFromDisk = true) }
                    selectedIds.clear()
                    showDeleteDialog = false
                },
                onConfirmRemoveHistory = {
                    selectedDownloads.forEach { dl -> viewModel.deleteDownload(dl, deleteFileFromDisk = false) }
                    selectedIds.clear()
                    showDeleteDialog = false
                }
            )
        }

        // Rename Dialog
        if (showRenameDialog) {
            val selectedDownload = dbDownloads.find { selectedIds.contains(it.id) }
            if (selectedDownload != null) {
                RenameDialog(
                    initialName = selectedDownload.filename,
                    theme = theme,
                    onDismiss = { showRenameDialog = false },
                    onConfirm = { newName ->
                        viewModel.renameDownload(selectedDownload, newName)
                        selectedIds.clear()
                        showRenameDialog = false
                    }
                )
            }
        }
    }
}

private fun getFileMetadata(filename: String, mimeType: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when {
        ext in listOf("pdf") || mimeType.contains("pdf") -> Pair(Icons.Default.PictureAsPdf, Color(0xFFEF4444))
        ext in listOf("mp4", "mkv", "avi", "mov") || mimeType.contains("video") -> Pair(Icons.Default.Movie, Color(0xFFF97316))
        ext in listOf("png", "jpg", "jpeg", "webp", "gif") || mimeType.contains("image") -> Pair(Icons.Default.Image, Color(0xFF0EA5E9))
        ext in listOf("zip", "rar", "7z", "tar", "gz") || mimeType.contains("zip") -> Pair(Icons.Default.FolderZip, Color(0xFF1A6FFF))
        ext in listOf("apk") -> Pair(Icons.Default.Android, Color(0xFF22C55E))
        ext in listOf("fig") -> Pair(Icons.Default.Palette, Color(0xFFA855F7))
        else -> Pair(Icons.Default.InsertDriveFile, Color(0xFF6B7280))
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    val gb = mb / 1024f
    return when {
        gb >= 1f -> String.format("%.1f GB", gb)
        mb >= 1f -> String.format("%.1f MB", mb)
        kb >= 1f -> String.format("%.0f KB", kb)
        else -> "$bytes Bytes"
    }
}

private fun formatEta(seconds: Long): String {
    if (seconds <= 0) return "Calculating time left…"
    val mins = seconds / 60
    val secs = seconds % 60
    return when {
        mins >= 60 -> String.format("%d hrs %d mins left", mins / 60, mins % 60)
        mins > 0 -> String.format("%d mins %d secs left", mins, secs)
        else -> String.format("%d secs left", secs)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RedesignedDownloadTile(
    download: Download,
    viewModel: BrowserViewModel,
    theme: com.orbit.browser.ui.theme.OBThemeConfig,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val g = theme.glass
    val isDark = theme.isDark
    val a1 = theme.effectiveA1
    val context = androidx.compose.ui.platform.LocalContext.current
    val (fileIcon, fileColor) = getFileMetadata(download.filename, download.mimeType)

    val cleanSiteAddress = remember(download.url) {
        if (download.url.isBlank()) ""
        else try {
            val uri = android.net.Uri.parse(download.url)
            uri.host?.removePrefix("www.") ?: download.url
        } catch (_: Exception) {
            download.url
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) a1.copy(alpha = 0.15f)
                else if (isDark) Color.White.copy(alpha = 0.05f)
                else Color.White.copy(alpha = 0.70f)
            )
            .border(
                1.dp,
                if (isSelected) a1.copy(alpha = 0.6f)
                else if (isDark) Color.White.copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.85f),
                RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else if (download.status == DownloadStatus.Completed) {
                        openDownloadedFile(context, download)
                    }
                }
            )
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) a1 else g.tx2Color.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 6.dp)
                )
            }
        if (download.status == DownloadStatus.Downloading || download.status == DownloadStatus.Pending || download.status == DownloadStatus.Paused) {
            // ─────────────────────────────────────────────────────────────
            // ACTIVE / PAUSED DOWNLOADING ITEM TILE
            // ─────────────────────────────────────────────────────────────
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DownloadThumbnailItem(
                        filePath = download.filePath,
                        filename = download.filename,
                        mimeType = download.mimeType,
                        size = 40.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // 1. Filename at top
                        Text(
                            text = download.filename,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = g.txColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        // 2. Estimated Time Left below filename
                        val etaText = when (download.status) {
                            DownloadStatus.Paused -> "Download Paused"
                            DownloadStatus.Pending -> "Starting download…"
                            else -> if (download.etaSeconds > 0) formatEta(download.etaSeconds) else "Calculating time left…"
                        }
                        Text(
                            text = etaText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (download.status == DownloadStatus.Paused) Color(0xFFF97316) else fileColor
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 3. Progress Bar with theme gradient
                val progressFraction = if (download.sizeBytes > 0) (download.downloadedBytes.toFloat() / download.sizeBytes.toFloat()).coerceIn(0f, 1f) else 0f
                val percent = (progressFraction * 100).toInt()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.horizontalGradient(listOf(fileColor, a1)))
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 4. Downloaded bytes from Total bytes on left, Percentage on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val downloadedStr = formatFileSize(download.downloadedBytes)
                    val totalStr = if (download.sizeBytes > 0) formatFileSize(download.sizeBytes) else "Unknown"
                    Text(
                        text = "$downloadedStr / $totalStr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = g.tx2Color
                    )
                    Text(
                        text = "$percent%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = g.txColor
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 5. Action controls: "Pause | Cancel" or "Resume | Cancel" separated by "|"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (download.status == DownloadStatus.Paused) {
                        Text(
                            text = "Resume",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = a1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.resumeDownload(context, download) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Pause",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = g.txColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.pauseDownload(context, download) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = " | ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = g.tx2Color.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Text(
                        text = "Cancel",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.cancelDownload(context, download) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        } else if (download.status == DownloadStatus.Cancelled || download.status == DownloadStatus.Failed) {
            // ─────────────────────────────────────────────────────────────
            // CANCELLED / FAILED DOWNLOAD ITEM TILE
            // ─────────────────────────────────────────────────────────────
            val isCancelled = download.status == DownloadStatus.Cancelled
            val statusLabel = if (isCancelled) "Download Cancelled" else "Download Failed"
            val statusColor = if (isCancelled) g.tx3Color else Color(0xFFEF4444)

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DownloadThumbnailItem(
                        filePath = download.filePath,
                        filename = download.filename,
                        mimeType = download.mimeType,
                        size = 42.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // 1. Filename with strikethrough (cross with dash) and grey text
                        Text(
                            text = download.filename,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = g.tx3Color,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        // 2. Mentioned Cancelled or Failed
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cleanSiteAddress,
                        fontSize = 10.5.sp,
                        color = g.tx3Color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Retry option
                    Text(
                        text = "Retry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = a1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.startDownload(context, download.url, "", "", download.mimeType, download.sizeBytes)
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        } else {
            // ─────────────────────────────────────────────────────────────
            // COMPLETED / FINISHED DOWNLOADED ITEM TILE
            // ─────────────────────────────────────────────────────────────
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DownloadThumbnailItem(
                        filePath = download.filePath,
                        filename = download.filename,
                        mimeType = download.mimeType,
                        size = 42.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // 1. Item Name
                        Text(
                            text = download.filename,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = g.txColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        // 2. Site Address below name
                        Text(
                            text = cleanSiteAddress,
                            fontSize = 11.sp,
                            color = g.tx2Color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // File size on right bottom corner of tile
                    Text(
                        text = formatFileSize(download.sizeBytes.coerceAtLeast(download.downloadedBytes)),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = g.txColor
                    )
                }
            }
        }
    }
}
}

@Composable
fun DownloadThumbnailItem(
    filePath: String,
    filename: String,
    mimeType: String,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val (fallbackIcon, fileColor) = getFileMetadata(filename, mimeType)
    var bitmapThumbnail by remember(filePath) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(filePath) {
        if (filePath.isNotBlank()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val file = java.io.File(filePath)
                    if (file.exists() && file.length() > 0) {
                        val ext = filename.substringAfterLast('.', "").lowercase()
                        if (ext in listOf("png", "jpg", "jpeg", "webp", "gif") || mimeType.contains("image")) {
                            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                            bitmapThumbnail = android.graphics.BitmapFactory.decodeFile(filePath, opts)
                        } else if (ext in listOf("mp4", "mkv", "avi", "mov", "webm") || mimeType.contains("video")) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                bitmapThumbnail = android.media.ThumbnailUtils.createVideoThumbnail(
                                    file,
                                    android.util.Size(120, 120),
                                    null
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                bitmapThumbnail = android.media.ThumbnailUtils.createVideoThumbnail(
                                    filePath,
                                    android.provider.MediaStore.Images.Thumbnails.MINI_KIND
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(fileColor.copy(alpha = 0.15f))
            .border(1.dp, fileColor.copy(alpha = 0.30f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmapThumbnail
        if (bmp != null) {
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = filename,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = fileColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

private fun shareFiles(context: android.content.Context, downloads: List<Download>) {
    if (downloads.isEmpty()) return
    try {
        val uris = ArrayList<android.net.Uri>()
        downloads.forEach { dl ->
            if (dl.filePath.isNotBlank()) {
                val file = java.io.File(dl.filePath)
                if (file.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    uris.add(uri)
                }
            }
        }

        if (uris.isEmpty()) {
            android.widget.Toast.makeText(context, "File does not exist on storage", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = if (uris.size == 1) {
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = downloads.firstOrNull()?.mimeType?.ifBlank { "*/*" } ?: "*/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uris.first())
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val chooser = android.content.Intent.createChooser(shareIntent, "Share Files").apply {
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Failed to share file: ${e.localizedMessage ?: "Error"}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun DeleteOptionsDialog(
    itemCount: Int,
    theme: com.orbit.browser.ui.theme.OBThemeConfig,
    onDismiss: () -> Unit,
    onConfirmDeleteFiles: () -> Unit,
    onConfirmRemoveHistory: () -> Unit
) {
    val g = theme.glass
    val isDark = theme.isDark
    val a1 = theme.effectiveA1

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        title = {
            Text(
                text = "Delete $itemCount ${if (itemCount == 1) "item" else "items"}?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = g.txColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose how you want to remove the selected downloads:",
                    fontSize = 12.5.sp,
                    color = g.tx2Color
                )

                // Option 1: Delete Files
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.10f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                        .clickable { onConfirmDeleteFiles() }
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Delete files",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "Deletes files from phone storage & history",
                                fontSize = 11.sp,
                                color = g.tx2Color
                            )
                        }
                    }
                }

                // Option 2: Remove from History
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(a1.copy(alpha = 0.10f))
                        .border(1.dp, a1.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                        .clickable { onConfirmRemoveHistory() }
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = a1,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Remove from history",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = g.txColor
                            )
                            Text(
                                text = "Removes from history, keeps files on disk",
                                fontSize = 11.sp,
                                color = g.tx2Color
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = g.tx2Color)
            }
        }
    )
}

@Composable
private fun RenameDialog(
    initialName: String,
    theme: com.orbit.browser.ui.theme.OBThemeConfig,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    val g = theme.glass
    val isDark = theme.isDark
    val a1 = theme.effectiveA1

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        title = {
            Text("Rename Download", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = g.txColor)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Filename") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = a1,
                    unfocusedBorderColor = g.tx2Color.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text.trim())
                }
            ) {
                Text("Rename", fontWeight = FontWeight.Bold, color = a1)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = g.tx2Color)
            }
        }
    )
}

private fun openDownloadedFile(context: android.content.Context, download: Download) {
    try {
        val file = java.io.File(download.filePath)
        if (!file.exists()) {
            android.widget.Toast.makeText(context, "File does not exist on storage", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = if (download.mimeType.isNotBlank() && download.mimeType != "application/octet-stream") {
            download.mimeType
        } else {
            val ext = download.filename.substringAfterLast('.', "").lowercase()
            android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Open file with"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "No app found to open this file", android.widget.Toast.LENGTH_SHORT).show()
    }
}
