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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults.iconToggleButtonColors
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import io.kusius.letterbox.model.MailSummary
import io.kusius.letterbox.ui.theme.AppTheme
import letterbox.composeapp.generated.resources.Res
import letterbox.composeapp.generated.resources.person
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

@Composable
private fun RoundedIcon(
    painter: Painter,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
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
    val threshold =
        with(LocalWindowInfo.current) {
            (containerSize.width * 0.7f).dp.value
        }

    val state =
        rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.StartToEnd) {
                    onStartToEndSwipe()
                } else if (it == SwipeToDismissBoxValue.EndToStart) {
                    onEndToStartSwipe()
                }
                // Do not dismiss when StartToEnd swiping
                it != SwipeToDismissBoxValue.StartToEnd
            },
            positionalThreshold = { threshold },
        )
    val haptics = LocalHapticFeedback.current
    val iconColor = AppTheme.colorScheme().onBackground
    val fontWeight =
        if (item.isRead) {
            FontWeight.Normal
        } else {
            FontWeight.ExtraBold
        }
    val iconColorFilter =
        ColorFilter.tint(
            iconColor,
            BlendMode.SrcAtop,
        )

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
                            IconToggleButton(
                                checked = false,
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
                                            .scale(animatedScale),
                                )
                            }
                        }
                    }
                }

                SwipeToDismissBoxValue.Settled -> {}
            }
        },
        modifier = modifier.clickable(onClick = onAction),
    ) {
        Card(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxSize(),
            ) {
                RoundedIcon(
                    painter = painterResource(Res.drawable.person),
                    contentDescription = null,
                    size = 35.dp,
                    modifier = Modifier.align(Alignment.Top).padding(8.dp),
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = item.sender,
                        style = AppTheme.typography().titleSmall,
                        fontWeight = fontWeight,
                    )
                    Text(
                        text = item.title,
                        style = AppTheme.typography().titleSmall,
                        fontWeight = fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.summary,
                        style = AppTheme.typography().bodySmall,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    }
}

@Composable
@Preview
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
                    senderEmail = "george@mail.com",
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
                    summary = "I would like to say hello to you I would like to say hello to youI would like to say hello to youI would like to say hello to youI would like to say hello to youI would like to say hello to youI would like to say hello to youI would like to say hello to you",
                    sender = "gmkousis@gmail.com",
                    isRead = true,
                    receivedAtUnixMillis = Clock.System.now(),
                    senderEmail = "george@mail.com"
                ),
            onAction = {},
            onEndToStartSwipe = {},
            onStartToEndSwipe = {},
        )
    }
}
