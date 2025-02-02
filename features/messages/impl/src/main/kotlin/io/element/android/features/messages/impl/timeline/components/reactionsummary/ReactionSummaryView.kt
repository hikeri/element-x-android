/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.features.messages.impl.timeline.components.reactionsummary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.features.messages.impl.timeline.model.AggregatedReaction
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.preview.DayNightPreviews
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.theme.ElementTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionSummaryView(
    state: ReactionSummaryState,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    fun onDismiss() {
        state.eventSink(ReactionSummaryEvents.Clear)
    }

    if (state.target != null) {
        ModalBottomSheet(
            onDismissRequest = ::onDismiss,
            sheetState = sheetState,
            modifier = modifier
        ) {
            SheetContent(summary = state.target)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SheetContent(
    summary: ReactionSummaryState.Summary,
    modifier: Modifier = Modifier,
) {
    val animationScope = rememberCoroutineScope()
    var selectedReactionKey: String by rememberSaveable { mutableStateOf(summary.selectedKey) }
    val selectedReactionIndex: Int by remember {
        derivedStateOf {
            summary.reactions.indexOfFirst { it.key == selectedReactionKey }
        }
    }
    val pagerState = rememberPagerState(initialPage = selectedReactionIndex)
    val reactionListState = rememberLazyListState()

    LaunchedEffect(pagerState.currentPage) {
        selectedReactionKey = summary.reactions[pagerState.currentPage].key
        val visibleInfo =  reactionListState.layoutInfo.visibleItemsInfo
        if (selectedReactionIndex <= visibleInfo.first().index || selectedReactionIndex >= visibleInfo.last().index) {
            reactionListState.animateScrollToItem(selectedReactionIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        LazyRow(state = reactionListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            items(summary.reactions) { reaction ->
                AggregatedReactionButton(
                    reaction = reaction,
                    isHighlighted = selectedReactionKey == reaction.key,
                    onClick = {
                        selectedReactionKey = reaction.key
                        animationScope.launch {
                            pagerState.animateScrollToPage(selectedReactionIndex)
                        }
                    }
                )
            }
        }
        HorizontalPager(state = pagerState, pageCount = summary.reactions.size) { page ->
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(summary.reactions[page].senders) { sender ->

                    val user = sender.user ?: MatrixUser(userId = sender.senderId)

                    SenderRow(
                        avatarData = user.getAvatarData(AvatarSize.UserListItem),
                        name = user.displayName ?: user.userId.value,
                        userId = user.userId.value,
                        sentTime = sender.sentTime
                    )
                }
            }
        }
    }
}

@Composable
fun AggregatedReactionButton(
    reaction: AggregatedReaction,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val buttonColor = if (isHighlighted) {
        ElementTheme.colors.bgActionPrimaryRest
    } else {
        Color.Transparent
    }

    val textColor = if (isHighlighted) {
        MaterialTheme.colorScheme.inversePrimary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(buttonColor, RoundedCornerShape(corner = CornerSize(percent = 50)))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        color = buttonColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier,
        ) {
            Text(
                text = reaction.displayKey,
                style = ElementTheme.typography.fontBodyMdRegular.copy(
                    fontSize = 20.sp,
                    lineHeight = 25.sp
                ),
            )
            if (reaction.count > 1) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = reaction.count.toString(),
                    color = textColor,
                    style = ElementTheme.typography.fontBodyMdRegular.copy(
                        fontSize = 20.sp,
                        lineHeight = 25.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SenderRow(
    avatarData: AvatarData,
    name: String,
    userId: String,
    sentTime: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(avatarData)
        Column(
            modifier = Modifier.padding(start = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .weight(1f),
                    text = name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                    style = ElementTheme.typography.fontBodyMdRegular,
                )
                Text(
                    text = sentTime,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodySmRegular,
                )
            }
            Text(
                text = userId,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ElementTheme.typography.fontBodySmRegular,
            )
        }
    }
}

@DayNightPreviews
@Composable
internal fun SheetContentPreview(
    @PreviewParameter(ReactionSummaryStateProvider::class) state: ReactionSummaryState
) = ElementPreview {
    SheetContent(summary = state.target as ReactionSummaryState.Summary)
}
