package com.maxreader.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxreader.app.R
import com.maxreader.app.library.LibraryBook
import com.maxreader.app.library.OpenPersistableDocument
import com.maxreader.app.ui.theme.*
import com.maxreader.app.viewmodel.LoadingState
import com.maxreader.app.viewmodel.ReaderViewModel

@Composable
fun HomeScreen(
    viewModel: ReaderViewModel,
    onBookLoaded: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val loadingState by viewModel.loadingState.collectAsState()
    val libraryBooks by viewModel.libraryBooks.collectAsState()
    val tc = LocalThemeColors.current

    val filePicker = rememberLauncherForActivityResult(
        contract = OpenPersistableDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.openPickedBook(it) }
    }

    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Success) {
            onBookLoaded()
            viewModel.resetLoadingState()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = tc.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = tc.accent
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "MaxReader",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = tc.textPrimary
            )

            Text(
                text = stringResource(R.string.home_subtitle),
                fontSize = 14.sp,
                color = tc.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Open EPUB button
            Button(
                onClick = {
                    filePicker.launch(arrayOf("application/epub+zip", "application/octet-stream"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = tc.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = loadingState !is LoadingState.Loading
            ) {
                if (loadingState is LoadingState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.action_loading), color = tc.textPrimary, fontSize = 16.sp)
                } else {
                    Text(stringResource(R.string.action_open_epub), color = tc.textPrimary, fontSize = 16.sp)
                }
            }

            if (loadingState is LoadingState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (loadingState as LoadingState.Error).message,
                    color = tc.accent,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tc.textPrimary)
            ) {
                Text(stringResource(R.string.action_settings), fontSize = 15.sp)
            }

            // Library section
            if (libraryBooks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.header_recent_books).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = tc.accent,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(libraryBooks.sortedByDescending { it.lastOpenedTimestamp }) { book ->
                        LibraryBookItem(
                            book = book,
                            onClick = {
                                viewModel.loadBook(Uri.parse(book.uri))
                            },
                            onRemove = {
                                viewModel.removeLibraryBook(book.uri)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryBookItem(
    book: LibraryBook,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val tc = LocalThemeColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = tc.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    color = tc.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    color = tc.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = book.progressPercent,
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp),
                        color = tc.accent,
                        trackColor = tc.background
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.progress_percent,
                            (book.progressPercent * 100).toInt()
                        ),
                        color = tc.textSecondary,
                        fontSize = 11.sp
                    )
                }
                if (book.lastChapterTitle.isNotEmpty()) {
                    Text(
                        text = book.lastChapterTitle,
                        color = tc.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_remove_book),
                    tint = tc.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
