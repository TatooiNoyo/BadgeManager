package io.github.tatooinoyo.star.badge.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.ui.icons.BadgeIcons
import io.github.tatooinoyo.star.badge.ui.theme.BackButtonShape
import io.github.tatooinoyo.star.badge.ui.theme.BadgeTokens
import io.github.tatooinoyo.star.badge.ui.theme.BorderDefault
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrange
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrangeLight
import io.github.tatooinoyo.star.badge.ui.theme.CapsuleTabShape
import io.github.tatooinoyo.star.badge.ui.theme.IconContainerShape
import io.github.tatooinoyo.star.badge.ui.theme.MenuButtonShape
import io.github.tatooinoyo.star.badge.ui.theme.NotchShape
import io.github.tatooinoyo.star.badge.ui.theme.TextPrimary
import io.github.tatooinoyo.star.badge.ui.theme.TextSecondary

@Composable
fun BadgeContentCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDefault),
    ) {
        content()
    }
}

@Composable
fun CapsuleTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CapsuleTabShape)
            .background(
                if (selected) BrandOrange else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun BadgeMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(MenuButtonShape)
            .background(BadgeTokens.menuButtonBackground)
            .border(1.dp, BorderDefault, MenuButtonShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(BadgeIcons.Menu),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TextPrimary,
        )
    }
}

@Composable
fun BadgeBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(BackButtonShape)
            .background(BadgeTokens.backButtonBackground)
            .border(1.dp, BorderDefault, BackButtonShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(BadgeIcons.ArrowLeft),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TextPrimary,
        )
    }
}

@Composable
fun PanelNotch(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(4.dp)
            .clip(NotchShape)
            .background(BorderDefault),
    )
}

@Composable
fun BadgeIconContainer(
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(IconContainerShape)
            .background(BadgeTokens.iconContainerBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(BadgeIcons.IdCard),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = BrandOrange,
        )
    }
}

@Composable
fun ServerTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(BadgeTokens.serverTagBackground)
            .border(1.dp, BorderDefault, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = BadgeTokens.serverTagForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CategoryTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(BadgeTokens.categoryTagBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = BadgeTokens.categoryTagForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun FilterChipStyled(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) BadgeTokens.filterChipSelectedBackground
                else BadgeTokens.filterChipUnselectedBackground,
            )
            .border(
                1.dp,
                if (selected) BadgeTokens.filterChipSelectedBorder
                else BadgeTokens.filterChipUnselectedBorder,
                MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (selected) {
            Icon(
                painter = painterResource(BadgeIcons.Check),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = BadgeTokens.filterChipSelectedForeground,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) BadgeTokens.filterChipSelectedForeground
            else BadgeTokens.filterChipUnselectedForeground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun DragHandle(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            painter = painterResource(BadgeIcons.DragCol),
            contentDescription = null,
            modifier = Modifier.size(width = 4.dp, height = 22.dp),
            tint = TextSecondary.copy(alpha = 0.55f),
        )
        Icon(
            painter = painterResource(BadgeIcons.DragCol),
            contentDescription = null,
            modifier = Modifier.size(width = 4.dp, height = 22.dp),
            tint = TextSecondary.copy(alpha = 0.55f),
        )
    }
}

@Composable
fun PrimaryOrangeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (enabled) BrandOrange else BrandOrange.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun OutlinedOrangeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(Color.White)
            .border(1.dp, BrandOrange, MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = BrandOrange,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = BrandOrange,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun LabeledInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(Color.White)
            .border(1.dp, BorderDefault, MaterialTheme.shapes.small)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
        trailing?.invoke()
    }
}

@Composable
fun SecondaryScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        BadgeBackButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
