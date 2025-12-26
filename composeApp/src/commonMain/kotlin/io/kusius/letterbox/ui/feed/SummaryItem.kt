package io.kusius.letterbox.ui.feed

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.kusius.letterbox.model.MailSummary
import io.kusius.letterbox.ui.theme.AppTheme
import letterbox.composeapp.generated.resources.Res
import letterbox.composeapp.generated.resources.mark_archive
import letterbox.composeapp.generated.resources.mark_read
import letterbox.composeapp.generated.resources.mark_unread
import letterbox.composeapp.generated.resources.person
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

@Composable
private fun RoundedIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(color = AppTheme.colorScheme().onPrimary)
                .fillMaxSize(),
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
internal fun SummaryItem(
    item: MailSummary,
    onAction: () -> Unit,
    onStartToEndSwipe: () -> Unit,
    onEndToStartSwipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localDensity = LocalDensity.current

    val state =
        rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.StartToEnd) {
                    onStartToEndSwipe()
                } else if (it == SwipeToDismissBoxValue.EndToStart) {
                    onEndToStartSwipe()
                }
                // reset item when toggling done status
                it != SwipeToDismissBoxValue.StartToEnd
            },
            positionalThreshold = {
                with(localDensity) { 112.dp.toPx() }
            },
        )
    val haptics = LocalHapticFeedback.current

    val fontWeight =
        if (item.isRead) {
            FontWeight.Normal
        } else {
            FontWeight.ExtraBold
        }

    // Provide haptic once user reaches threshold for swipe action to execute
    LaunchedEffect(state.targetValue) {
        if (state.targetValue != SwipeToDismissBoxValue.Settled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val direction = state.dismissDirection
            val backgroundContentPadding = 24.dp
            val targetScale =
                if (state.targetValue != SwipeToDismissBoxValue.Settled) 0.6f else 0.5f
            val animatedScale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), // or tween()
                label = "iconScaleAnimation",
            )
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colorScheme().tertiaryContainer),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().padding(backgroundContentPadding),
                        ) {
                            AsyncImage(
                                model =
                                    if (item.isRead) {
                                        Res.getUri("drawable/mark_unread.svg")
                                    } else {
                                        Res.getUri("drawable/mark_read.svg")
                                    },
                                contentDescription = null,
                                modifier = Modifier.scale(animatedScale),
                            )
                        }
                    }
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    Card(
                        modifier = Modifier.fillMaxSize().animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colorScheme().errorContainer),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxSize().padding(backgroundContentPadding),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.mark_archive),
                                tint = AppTheme.colorScheme().onErrorContainer,
                                contentDescription = null,
                                modifier = Modifier.scale(animatedScale),
                            )
                        }
                    }
                }

                SwipeToDismissBoxValue.Settled -> {}
            }
        },
        modifier = modifier.clickable(onClick = onAction),
    ) {
        Card(
            modifier = Modifier.height(100.dp).fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally,
                    ),
                modifier = Modifier.fillMaxSize(),
            ) {
                RoundedIcon(
                    painter = painterResource(Res.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = item.title,
                        style = AppTheme.typography().titleMedium,
                        fontWeight = fontWeight,
                    )
                    Text(
                        text = "From: ${item.sender}",
                        style = AppTheme.typography().titleSmall,
                        fontWeight = fontWeight,
                    )
                    Text(text = item.summary, style = AppTheme.typography().bodySmall)
                }
            }
        }
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    }
}

@Composable
@Preview()
private fun PreviewMainSummaryItem() {
    AppTheme {
        SummaryItem(
            item =
                MailSummary(
                    id = "Id",
                    title = "Re: Hello",
                    summary = "I would like to say hello to you",
                    sender = "gmkousis@gmail.com",
                    isRead = false,
                    receivedAtUnixMillis = Clock.System.now(),
                ),
            onAction = {},
            onEndToStartSwipe = {},
            onStartToEndSwipe = {},
        )
    }
}

@Composable
@Preview()
private fun PreviewMainSummaryItemRead() {
    AppTheme {
        SummaryItem(
            item =
                MailSummary(
                    id = "Id",
                    title = "Re: Hello",
                    summary = "I would like to say hello to you",
                    sender = "gmkousis@gmail.com",
                    isRead = true,
                    receivedAtUnixMillis = Clock.System.now(),
                ),
            onAction = {},
            onEndToStartSwipe = {},
            onStartToEndSwipe = {},
        )
    }
}
