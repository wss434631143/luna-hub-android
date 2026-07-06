package com.lunahub.android.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lunahub.android.domain.model.CameraMedia
import com.lunahub.android.domain.model.MediaType

@Composable
fun LunaPage(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = LunaSpacing.PageHorizontal)
            .padding(top = LunaSpacing.PageTop),
    ) {
        LunaTopBar(title = title, subtitle = subtitle)
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
fun LunaTopBar(title: String, subtitle: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LunaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LunaSpacing.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(LunaSpacing.CardPadding), content = content)
    }
}

@Composable
fun LunaPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(LunaSpacing.ButtonHeight),
        shape = RoundedCornerShape(LunaSpacing.SmallRadius),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(text)
    }
}

@Composable
fun LunaSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(LunaSpacing.ButtonHeight),
        shape = RoundedCornerShape(LunaSpacing.SmallRadius),
    ) {
        Text(text)
    }
}

@Composable
fun LunaIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
fun LunaSectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (action != null && onAction != null) {
            Text(
                text = action,
                modifier = Modifier.clickable(onClick = onAction),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun LunaEmptyState(title: String, message: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    LunaCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Image, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                LunaPrimaryButton(text = actionText, onClick = onAction, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun LunaLoadingState(text: String = "正在加载") {
    LunaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun LunaErrorState(message: String, onRetry: () -> Unit) {
    LunaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(10.dp))
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            LunaIconButton(Icons.Outlined.Refresh, "重试", onRetry)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LunaMediaGridItem(
    media: CameraMedia,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brush = if (media.mediaType == MediaType.Photo) {
        Brush.linearGradient(listOf(Color(0xFFDCE8F7), Color(0xFFBFCDE0)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFE4E1F8), Color(0xFFC7C0E9)))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(brush)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .aspectRatio(0.78f),
    ) {
        Icon(
            imageVector = if (media.mediaType == MediaType.Video) Icons.Outlined.Videocam else Icons.Outlined.Image,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(34.dp),
            tint = Color.White.copy(alpha = 0.9f),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.26f))
                .padding(8.dp),
        ) {
            Text(media.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(if (media.mediaType == MediaType.Video) "视频" else "照片", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
