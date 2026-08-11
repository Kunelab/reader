package io.github.kunelab.reader.ui.screens

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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kunelab.reader.R
import io.github.kunelab.reader.settings.RsvpSettings
import io.github.kunelab.reader.ui.components.RsvpWordDisplay
import io.github.kunelab.reader.ui.theme.*
import io.github.kunelab.reader.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Deliberately no collect of the full rsvpState here. Reading it in this scope would
    // pull the whole screen -- top bar, controls, dialogs -- into a recomposition on
    // every word. Each consumer below collects the narrowest slice it needs instead.
    val settings by viewModel.settings.collectAsState()
    val bookData by viewModel.bookData.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val chapterTitle by viewModel.currentChapterTitle.collectAsState()
    val tc = LocalThemeColors.current

    var showControls by remember { mutableStateOf(true) }
    var showChapterList by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showAddBookmark by remember { mutableStateOf(false) }
    var showJumpToWord by remember { mutableStateOf(false) }

    val bookmarks by viewModel.bookmarks.collectAsState()

    // Handle phone back button
    BackHandler {
        viewModel.pause() // also persists the current position
        onBack()
    }

    // Keep screen awake while playing
    val view = LocalView.current
    DisposableEffect(isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose { view.keepScreenOn = false }
    }

    // Stop and persist when the app goes to the background, so a swipe-away or a
    // low-memory kill does not discard the reading position.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.pause() // also persists the current position
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                                text = bookData?.title ?: stringResource(R.string.app_name),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (chapterTitle.isNotEmpty()) {
                                Text(
                                    text = chapterTitle,
                                    fontSize = 12.sp,
                                    color = tc.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.pause() // also persists the current position
                            onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddBookmark = true }) {
                            Icon(
                                Icons.Default.BookmarkAdd,
                                contentDescription = stringResource(R.string.cd_add_bookmark)
                            )
                        }
                        IconButton(onClick = { showBookmarks = true }) {
                            Icon(
                                Icons.Default.Bookmarks,
                                contentDescription = stringResource(R.string.cd_bookmarks)
                            )
                        }
                        IconButton(onClick = { showChapterList = true }) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = stringResource(R.string.cd_chapters)
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_settings)
                            )
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

            // Main RSVP Area, tap to toggle play/pause
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        // Without these the primary control of the app is invisible to
                        // TalkBack: it is a bare clickable Box with no announced purpose.
                        onClickLabel = stringResource(R.string.cd_play_pause),
                        role = Role.Button
                    ) {
                        viewModel.togglePlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                RsvpStage(viewModel = viewModel, settings = settings)
            }

            // Bottom controls
            if (showControls) {
                ReaderProgressBar(viewModel = viewModel)

                // Compact control row, 3 equal columns
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
                        IconButton(
                            onClick = { viewModel.updateSettings { s -> s.copy(wpm = s.wpm - 25) } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease_wpm), tint = tc.textPrimary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${settings.wpm}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tc.accent
                        )
                        IconButton(
                            onClick = { viewModel.updateSettings { s -> s.copy(wpm = s.wpm + 25) } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase_wpm), tint = tc.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Center 1/3: Playback controls
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.skipBackward() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FastRewind, contentDescription = stringResource(R.string.cd_rewind), tint = tc.textPrimary, modifier = Modifier.size(20.dp))
                        }
                        FilledIconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = tc.accent
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_pause),
                                tint = tc.textPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.skipForward() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FastForward, contentDescription = stringResource(R.string.cd_forward), tint = tc.textPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Right 1/3: Word count (tap to jump)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showJumpToWord = true },
                        contentAlignment = Alignment.Center
                    ) {
                        WordCounter(viewModel = viewModel)
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
        val rsvpState by viewModel.rsvpState.collectAsState()
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
            title = { Text(stringResource(R.string.dialog_chapters_title), fontWeight = FontWeight.Bold) },
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
                            stringResource(R.string.chapter_numbered_title, num, chapter.title)
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
                    Text(stringResource(R.string.action_close), color = tc.accent)
                }
            },
            containerColor = tc.surface,
            titleContentColor = tc.textPrimary
        )
    }

    // Add bookmark dialog
    if (showAddBookmark) {
        val rsvpState by viewModel.rsvpState.collectAsState()
        var label by remember { mutableStateOf("") }
        val defaultLabel = stringResource(R.string.bookmark_default_label, rsvpState.wordIndex + 1)
        AlertDialog(
            onDismissRequest = { showAddBookmark = false },
            title = { Text(stringResource(R.string.dialog_add_bookmark_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            R.string.bookmark_position,
                            rsvpState.wordIndex + 1,
                            rsvpState.currentChapterTitle
                        ),
                        color = tc.textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text(stringResource(R.string.bookmark_label_hint)) },
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
                    viewModel.addBookmark(label.ifBlank { defaultLabel })
                    showAddBookmark = false
                }) {
                    Text(stringResource(R.string.action_save), color = tc.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmark = false }) {
                    Text(stringResource(R.string.action_cancel), color = tc.textSecondary)
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
            title = { Text(stringResource(R.string.dialog_bookmarks_title), fontWeight = FontWeight.Bold) },
            text = {
                if (bookmarks.isEmpty()) {
                    Text(stringResource(R.string.bookmarks_empty), color = tc.textSecondary)
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
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove_bookmark), tint = tc.textMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarks = false }) {
                    Text(stringResource(R.string.action_close), color = tc.accent)
                }
            },
            containerColor = tc.surface,
            titleContentColor = tc.textPrimary
        )
    }

    // Jump to word dialog
    if (showJumpToWord) {
        val rsvpState by viewModel.rsvpState.collectAsState()
        var jumpText by remember { mutableStateOf("${rsvpState.wordIndex + 1}") }
        AlertDialog(
            onDismissRequest = { showJumpToWord = false },
            title = { Text(stringResource(R.string.dialog_jump_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.jump_prompt, rsvpState.totalWords),
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
                    Text(stringResource(R.string.action_go), color = tc.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpToWord = false }) {
                    Text(stringResource(R.string.action_cancel), color = tc.textSecondary)
                }
            },
            containerColor = tc.surface,
            titleContentColor = tc.textPrimary
        )
    }
}

/**
 * The word itself. Kept in its own composable so that collecting the per-word state
 * invalidates only this subtree rather than the whole reader.
 */
@Composable
private fun RsvpStage(viewModel: ReaderViewModel, settings: RsvpSettings) {
    val rsvpState by viewModel.rsvpState.collectAsState()

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

/** Progress bar, isolated for the same reason as [RsvpStage]. */
@Composable
private fun ReaderProgressBar(viewModel: ReaderViewModel) {
    val tc = LocalThemeColors.current
    val rsvpState by viewModel.rsvpState.collectAsState()

    LinearProgressIndicator(
        progress = { rsvpState.progressPercent },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .padding(horizontal = 16.dp),
        color = tc.accent,
        trackColor = tc.surface,
    )
}

/** "current / total", isolated for the same reason as [RsvpStage]. */
@Composable
private fun WordCounter(viewModel: ReaderViewModel) {
    val tc = LocalThemeColors.current
    val rsvpState by viewModel.rsvpState.collectAsState()

    Text(
        text = stringResource(R.string.word_position, rsvpState.wordIndex + 1, rsvpState.totalWords),
        fontSize = 11.sp,
        color = tc.textSecondary
    )
}
