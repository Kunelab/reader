package com.maxreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxreader.app.ui.components.RsvpWordDisplay
import com.maxreader.app.ui.theme.*
import com.maxreader.app.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val rsvpState by viewModel.rsvpState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val bookData by viewModel.bookData.collectAsState()
    val tc = LocalThemeColors.current

    var showControls by remember { mutableStateOf(true) }
    var showChapterList by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showAddBookmark by remember { mutableStateOf(false) }
    var showJumpToWord by remember { mutableStateOf(false) }

    val bookmarks by viewModel.bookmarks.collectAsState()

    // Handle phone back button
    BackHandler {
        viewModel.pause()
        viewModel.saveCurrentProgress()
        onBack()
    }

    // Keep screen awake while playing
    val view = LocalView.current
    DisposableEffect(rsvpState.isPlaying) {
        view.keepScreenOn = rsvpState.isPlaying
        onDispose { view.keepScreenOn = false }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = tc.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            if (showControls) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = bookData?.title ?: "MaxReader",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (rsvpState.currentChapterTitle.isNotEmpty()) {
                                Text(
                                    text = rsvpState.currentChapterTitle,
                                    fontSize = 12.sp,
                                    color = tc.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.pause()
                            viewModel.saveCurrentProgress()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddBookmark = true }) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Add Bookmark")
                        }
                        IconButton(onClick = { showBookmarks = true }) {
                            Icon(Icons.Default.Bookmarks, contentDescription = "Bookmarks")
                        }
                        IconButton(onClick = { showChapterList = true }) {
                            Icon(Icons.Default.List, contentDescription = "Chapters")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = tc.surface,
                        titleContentColor = tc.textPrimary,
                        navigationIconContentColor = tc.textPrimary,
                        actionIconContentColor = tc.textPrimary
                    )
                )
            }

            // Main RSVP Area — tap to toggle play/pause
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        viewModel.togglePlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                RsvpWordDisplay(
                    word = rsvpState.currentWord,
                    contextWords = rsvpState.contextWords,
                    nextWords = rsvpState.nextWords,
                    fontSize = settings.fontSize,
                    showContext = settings.showContext,
                    fontFamily = settings.fontFamily,
                    letterSpacing = settings.letterSpacing,
                    contextLineSpacing = settings.contextLineSpacing,
                    contextMargin = settings.contextMarginHorizontal
                )
            }

            // Bottom controls
            if (showControls) {
                // Progress bar
                LinearProgressIndicator(
                    progress = rsvpState.progressPercent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(horizontal = 16.dp),
                    color = tc.accent,
                    trackColor = tc.surface,
                )

                // Compact control row — 3 equal columns
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left 1/3: WPM controls
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.setWpm(settings.wpm - 25) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "- WPM", tint = tc.textPrimary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${settings.wpm}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tc.accent
                        )
                        IconButton(onClick = { viewModel.setWpm(settings.wpm + 25) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "+ WPM", tint = tc.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Center 1/3: Playback controls
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.skipBackward() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Back", tint = tc.textPrimary, modifier = Modifier.size(20.dp))
                        }
                        FilledIconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = tc.accent
                            )
                        ) {
                            Icon(
                                imageVector = if (rsvpState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = tc.textPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.skipForward() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = tc.textPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Right 1/3: Word count (tap to jump)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showJumpToWord = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${rsvpState.wordIndex + 1}/${rsvpState.totalWords}",
                            fontSize = 11.sp,
                            color = tc.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Padding for system navigation bar
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }

    // Chapter list dialog
    if (showChapterList) {
        val chapters = bookData?.chapters ?: emptyList()
        // Number only content chapters
        var chapterNum = 0
        val numberedChapters = chapters.mapIndexed { index, ch ->
            if (ch.isContentChapter) {
                chapterNum++
                Triple(index, ch, chapterNum)
            } else {
                Triple(index, ch, 0)
            }
        }

        AlertDialog(
            onDismissRequest = { showChapterList = false },
            title = { Text("Chapters", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    numberedChapters.forEach { (index, chapter, num) ->
                        // Skip non-content chapters with very few words
                        if (!chapter.isContentChapter && chapter.words.size < 10) return@forEach

                        val isCurrentChapter = rsvpState.currentWord?.chapterIndex == index
                        val displayTitle = if (chapter.isContentChapter && num > 0) {
                            "$num. ${chapter.title}"
                        } else {
                            chapter.title
                        }

                        TextButton(
                            onClick = {
                                val firstWord = chapter.words.firstOrNull()
                                if (firstWord != null) {
                                    viewModel.seekTo(firstWord.globalIndex)
                                }
                                showChapterList = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayTitle,
                                color = if (isCurrentChapter) tc.accent else tc.textPrimary,
                                fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChapterList = false }) {
                    Text("Close", color = tc.accent)
                }
            },
            containerColor = tc.surface,
            titleContentColor = tc.textPrimary
        )
    }

    // Add bookmark dialog
    if (showAddBookmark) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBookmark = false },
            title = { Text("Add Bookmark", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Word ${rsvpState.wordIndex + 1} — ${rsvpState.currentChapterTitle}",
                        color = tc.textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text("Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tc.textPrimary,
                            unfocusedTextColor = tc.textPrimary,
                            focusedBorderColor = tc.accent,
                            unfocusedBorderColor = tc.textMuted,
                            cursorColor = tc.accent
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addBookmark(label.ifBlank { "Word ${rsvpState.wordIndex + 1}" })
                    showAddBookmark = false
                }) {
                    Text("Save", color = tc.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmark = false }) {
                    Text("Cancel", color = tc.textSecondary)
                }
            },
            containerColor = tc.surface,
            titleContentColor = tc.textPrimary
        )
    }

    // Bookmarks list dialog
    if (showBookmarks) {
        AlertDialog(
            onDismissRequest = { showBookmarks = false },
            title = { Text("Bookmarks", fontWeight = FontWeight.Bold) },
            text = {
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks yet", color = tc.textSecondary)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        bookmarks.sortedBy { it.wordIndex }.forEach { bm ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.seekTo(bm.wordIndex)
                                        showBookmarks = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = bm.label,
                                            color = tc.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = bm.chapterTitle,
                                            color = tc.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeBookmark(bm.wordIndex) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = tc.textMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarks = false }) {
                    Text("Close", color = tc.accent)
                }
            },
            containerColor = tc.surface,
            titleContentColor = tc.textPrimary
        )
    }

    // Jump to word dialog
    if (showJumpToWord) {
        var jumpText by remember { mutableStateOf("${rsvpState.wordIndex + 1}") }
        AlertDialog(
            onDismissRequest = { showJumpToWord = false },
            title = { Text("Jump to word", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter word number (1–${rsvpState.totalWords})",
                        color = tc.textSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jumpText,
                        onValueChange = { newText ->
                            jumpText = newText.filter { it.isDigit() }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tc.textPrimary,
                            unfocusedTextColor = tc.textPrimary,
                            focusedBorderColor = tc.accent,
                            unfocusedBorderColor = tc.textMuted,
                            cursorColor = tc.accent
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = jumpText.toIntOrNull()
                    if (target != null && target in 1..rsvpState.totalWords) {
                        viewModel.seekTo(target - 1)
                    }
                    showJumpToWord = false
                }) {
                    Text("Go", color = tc.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpToWord = false }) {
                    Text("Cancel", color = tc.textSecondary)
                }
            },
            containerColor = tc.surface,
            titleContentColor = tc.textPrimary
        )
    }
}
