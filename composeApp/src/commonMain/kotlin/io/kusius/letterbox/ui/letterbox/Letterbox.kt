package io.kusius.letterbox.ui.letterbox

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButtonDefaults.iconToggleButtonColors
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import io.kusius.letterbox.model.Mail
import io.kusius.letterbox.model.MailPart
import io.kusius.letterbox.model.MailPartBody
import io.kusius.letterbox.model.MailSummary.Companion.randomMailSummary
import io.kusius.letterbox.model.MimeTypes
import io.kusius.letterbox.ui.common.MailItem
import io.kusius.letterbox.ui.common.cardstack.lazy.LazyCardStack
import io.kusius.letterbox.ui.common.cardstack.lazy.itemsIndexed
import io.kusius.letterbox.ui.common.cardstack.lazy.rememberLazyCardStackState
import io.kusius.letterbox.ui.common.swipeablecard.CardSwipeDirection
import io.kusius.letterbox.ui.common.swipeablecard.SwipeableCard
import io.kusius.letterbox.ui.theme.AppTheme
import kotlinx.serialization.Serializable
import letterbox.composeapp.generated.resources.Res
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object LetterboxRoute

@Composable
fun LetterboxScreenRoot(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    viewModel: LetterboxViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState.value) {
        is LetterboxUiState.Data -> {
            LetterboxScreen(
                data = state.data,
                modifier = modifier.fillMaxSize(),
                onAction = viewModel::onAction,
            )
        }

        is LetterboxUiState.Loading -> {
            Text(text = "Loading")
        }

        is LetterboxUiState.Error -> {
            Text(text = "Error! ${state.error}")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LetterboxScreen(
    data: List<Mail>,
    onAction: (LetterboxAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val platformConfig = rememberPlatformConfig()
    val state = rememberLazyCardStackState(visibleCards = platformConfig.visibleCards(), stackDepth = 10)
    var directionState by remember { mutableStateOf<CardSwipeDirection?>(null) }
    val iconExpandedScale = 0.6f
    val iconCollapsedScale = 0.5f
    val iconColor = AppTheme.colorScheme().onBackground
    val iconColorFilter =
        ColorFilter.tint(
            iconColor,
            BlendMode.SrcAtop,
        )

    val haptics = LocalHapticFeedback.current
    // Provide haptic once user reaches threshold for swipe action to execute
    LaunchedEffect(directionState) {
        if (directionState != null) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val animatedScaleLeft by animateFloatAsState(
        targetValue = if (directionState == CardSwipeDirection.Left) iconExpandedScale else iconCollapsedScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), // or tween()
        label = "iconScaleAnimation",
    )

    val animatedScaleRight by animateFloatAsState(
        targetValue = if (directionState == CardSwipeDirection.Right) iconExpandedScale else iconCollapsedScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), // or tween()
        label = "iconScaleAnimation",
    )
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val maxW = maxWidth - 4.dp
        val maxH = maxHeight - 4.dp
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconToggleButton(
                    checked = directionState == CardSwipeDirection.Left,
                    onCheckedChange = {},
                    colors =
                        iconToggleButtonColors(
                            checkedContainerColor = AppTheme.colorScheme().errorContainer,
                            checkedContentColor = AppTheme.colorScheme().onErrorContainer,
                        ),
                ) {
                    AsyncImage(
                        model = Res.getUri("drawable/mark_archive.svg"),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .graphicsLayer { colorFilter = iconColorFilter }
                                .clickable {
                                    if (state.index in data.indices) {
                                        onAction(LetterboxAction.Archive(mail = data[state.index]))
                                        state.moveNext()
                                    }
                                }.scale(animatedScaleLeft),
                    )
                }
                Text("Unread mails : ${data.size}")

                IconToggleButton(
                    checked = directionState == CardSwipeDirection.Right,
                    onCheckedChange = {},
                    colors =
                        iconToggleButtonColors(
                            checkedContainerColor = AppTheme.colorScheme().tertiaryContainer,
                            checkedContentColor = AppTheme.colorScheme().onTertiaryContainer,
                        ),
                ) {
                    AsyncImage(
                        model = Res.getUri("drawable/mark_read.svg"),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .graphicsLayer { colorFilter = iconColorFilter }
                                .clickable(enabled = data.isNotEmpty()) {
                                    if (state.index in data.indices) {
                                        onAction(LetterboxAction.MarkRead(mail = data[state.index]))
                                        state.moveNext()
                                    }
                                }.scale(animatedScaleRight),
                    )
                }
            }

            LazyCardStack(
                state = state,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = data,
                    key = { _, item -> item.summary.id },
                    placeholder = { _, item ->
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            AnimatedContent(
                                scrollable = false,
                                draggable = false,
                                expandable = false,
                                expanded = false,
                                modifier = Modifier.width(300.dp).height(400.dp),
                                item = item,
                                onBack = {},
                                onMaximize = {},
                                onAboutToSwipe = { _, _ -> },
                                onSwiped = { _, _ -> },
                            ) {
                                // Empty content for placeholder
                            }
                        }
                    },
                ) { index, item ->
                    val expanded = expandedStates[item.summary.id] ?: false

                    val transition = updateTransition(expanded, label = "size_transition")
                    val width by transition.animateDp(transitionSpec = { tween(250) }) { expandedState ->
                        if (expandedState) maxW else 300.dp
                    }
                    val height by transition.animateDp(transitionSpec = { tween(250) }) { expandedState ->
                        if (expandedState) maxH else 400.dp
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AnimatedContent(
                            expandable = platformConfig.isExpandable() && (index == 0),
                            draggable = platformConfig.isDraggableCards() && (index == 0),
                            scrollable = (index == 0),
                            expanded = expanded,
                            modifier = Modifier.width(width).height(height),
                            item = item,
                            onBack = {
                                expandedStates[item.summary.id] = false
                            },
                            onMaximize = {
                                expandedStates[item.summary.id] = true
                            },
                            onAboutToSwipe = { _, direction ->
                                directionState = direction
                            },
                            onSwiped = { mail, direction ->
                                val action =
                                    when (direction) {
                                        CardSwipeDirection.Left -> {
                                            LetterboxAction.Archive(mail)
                                        }

                                        CardSwipeDirection.Right -> {
                                            LetterboxAction.MarkRead(mail)
                                        }

                                        CardSwipeDirection.Down -> {
                                            expandedStates[item.summary.id] = false
                                            null
                                        }
                                    }
                                action?.let {
                                    directionState = null
                                    onAction(it)
                                    state.moveNext()
                                }
                            },
                        ) {
                            MailItem(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .background(AppTheme.colorScheme().secondaryContainer)
                .padding(8.dp),
    ) {
        Text(
            title,
            style = AppTheme.typography().titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AnimatedContent(
    draggable: Boolean = true,
    expandable: Boolean = true,
    scrollable: Boolean = true,
    expanded: Boolean = false,
    item: Mail,
    onSwiped: (Mail, CardSwipeDirection) -> Unit,
    onAboutToSwipe: (Mail, CardSwipeDirection?) -> Unit,
    onBack: () -> Unit,
    onMaximize: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (expanded) {
        BackHandler {
            onBack()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    SwipeableCard(
        headerDraggable = expanded,
        scrollable = scrollable,
        draggable = draggable && !expanded,
        item = item,
        key = { item.summary.id },
        onSwiped = onSwiped,
        header = {
            Header(
                item.summary.title,
                modifier =
                    Modifier
                        .clickable(
                            enabled = !expanded && expandable,
                            indication = null,
                            interactionSource = interactionSource,
                        ) {
                            if (!expanded) onMaximize()
                        },
            )
        },
        modifier = modifier,
        onAboutToSwipe = { item, direction ->
            onAboutToSwipe(item, direction)
        },
    ) {
        Box(modifier = Modifier) {
            content()
            if (!expanded) {
                // Blocks input so that main content does not respond to clicks, taps etc.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {}
                            .clickable(
                                enabled = expandable,
                                indication = null,
                                interactionSource = interactionSource,
                            ) {
                                onMaximize()
                            }.background(Color.Transparent),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LetterboxScreenPreview() {
    AppTheme {
        LetterboxScreen(
            data =
                buildList {
                    for (i in 1..10) {
                        add(
                            Mail(
                                summary = randomMailSummary(i),
                                mailPart =
                                    MailPart(
                                        MimeTypes.Text.PLAIN,
                                        MailPartBody(data = "Mail $i", size = 0, attachmentId = null),
                                        emptyList(),
                                    ),
                            ),
                        )
                    }
                },
            onAction = {},
            modifier = Modifier,
        )
    }
}
