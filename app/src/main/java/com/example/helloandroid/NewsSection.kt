package com.example.helloandroid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.helloandroid.data.HomeNewsItem
import com.example.helloandroid.data.HomeNewsResult
import com.example.helloandroid.data.NewsRepository
import kotlinx.coroutines.launch

/** Database-driven News destination. Filters belong here, not on Home. */
@Composable
internal fun ExploreScreen() {
    val repository = remember { NewsRepository() }
    var selectedFeed by remember { mutableStateOf(NewsFeed.Trending) }
    var trending by remember { mutableStateOf<HomeNewsResult?>(null) }
    var featured by remember { mutableStateOf<HomeNewsResult?>(null) }
    var latest by remember { mutableStateOf<HomeNewsResult?>(null) }
    var selectedStory by remember { mutableStateOf<HomeNewsItem?>(null) }
    var imageStory by remember { mutableStateOf<HomeNewsItem?>(null) }
    var articleTextScale by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        launch { trending = repository.trending() }
        launch { featured = repository.featured() }
        launch { latest = repository.latest() }
    }
    val result = when (selectedFeed) {
        NewsFeed.Trending -> trending
        NewsFeed.Featured -> featured
        NewsFeed.Latest -> latest
    }

    Surface(Modifier.fillMaxSize(), color = NewsCanvas) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.player_profile_depth), null, Modifier.fillMaxSize().graphicsLayer(alpha = 0.34f), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 22.dp, top = 28.dp, end = 22.dp, bottom = 112.dp)) {
                Text("Explore", color = NewsInk, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text("Trending esports news", color = NewsMuted, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(22.dp))
                NewsFeedTabs(selectedFeed) { selectedFeed = it }
                Spacer(Modifier.height(16.dp))
                NewsFeedContent(
                    feed = selectedFeed,
                    result = result,
                    onStoryClick = { selectedStory = it },
                    onImageClick = { imageStory = it },
                )
            }
        }
    }
    selectedStory?.let { story ->
        NewsStoryDialog(
            story = story,
            textScale = articleTextScale,
            onDecreaseText = { articleTextScale = (articleTextScale - 0.1f).coerceAtLeast(0.9f) },
            onIncreaseText = { articleTextScale = (articleTextScale + 0.1f).coerceAtMost(1.4f) },
            onViewImage = { imageStory = story },
            onDismiss = { selectedStory = null },
        )
    }
    imageStory?.let { story -> NewsImageDialog(story) { imageStory = null } }
}

private enum class NewsFeed(val label: String) { Trending("TRENDING"), Featured("FEATURED"), Latest("LATEST") }

@Composable
private fun NewsFeedTabs(selected: NewsFeed, onSelected: (NewsFeed) -> Unit) = Row(
    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp),
) {
    NewsFeed.entries.forEach { feed ->
        Surface(
            Modifier.noRippleClick { onSelected(feed) }, shape = RoundedCornerShape(12.dp),
            color = if (feed == selected) NewsInk else NewsCard,
            border = BorderStroke(1.dp, if (feed == selected) NewsGold else NewsOutline.copy(alpha = 0.62f)),
        ) { Text(feed.label, color = if (feed == selected) Color.White else NewsInk, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)) }
    }
}

@Composable
private fun NewsFeedContent(
    feed: NewsFeed,
    result: HomeNewsResult?,
    onStoryClick: (HomeNewsItem) -> Unit,
    onImageClick: (HomeNewsItem) -> Unit,
) = when (result) {
    null -> NewsStatePanel("Loading ${feed.label.lowercase()} stories…")
    is HomeNewsResult.Failure -> NewsStatePanel("News is unavailable right now.")
    is HomeNewsResult.Success -> if (result.stories.isEmpty()) NewsStatePanel("No ${feed.label.lowercase()} stories have been published yet.")
    else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        result.stories.forEach { story ->
            NewsDirectoryCard(story, onReadArticle = { onStoryClick(story) }, onViewImage = { onImageClick(story) })
        }
    }
}

@Composable
private fun NewsStatePanel(message: String) = Surface(
    Modifier.fillMaxWidth().height(128.dp), shape = RoundedCornerShape(16.dp), color = NewsCard.copy(alpha = 0.78f), border = BorderStroke(1.dp, NewsOutline.copy(alpha = 0.52f)),
) { Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) { Text(message, color = NewsMuted, textAlign = TextAlign.Center) } }

@Composable
private fun NewsDirectoryCard(
    story: HomeNewsItem,
    onReadArticle: () -> Unit,
    onViewImage: () -> Unit,
) = Surface(
    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = NewsCard.copy(alpha = 0.94f), border = BorderStroke(1.dp, NewsOutline.copy(alpha = 0.55f)), shadowElevation = 1.dp,
) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(128.dp).clip(RoundedCornerShape(22.dp)).noRippleClick(onViewImage)) {
            NewsStoryArtwork(story, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f).noRippleClick(onReadArticle)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(story.category?.uppercase() ?: "NEWS", color = NewsBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                story.createdAt?.let { Text(newsTimeLabel(it), color = NewsMuted, style = MaterialTheme.typography.labelSmall) }
            }
            Spacer(Modifier.height(5.dp))
            Text(story.title, color = NewsInk, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Text(story.summary ?: "Open this story to read the latest esports update.", color = NewsMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp)); Text("Tap to read  •  Tap image to view", color = NewsBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/** Home has only the real featured-news carousel, never category controls or fallback stories. */
@Composable
internal fun FeaturedNewsCarousel(result: HomeNewsResult?) {
    var selectedStory by remember { mutableStateOf<HomeNewsItem?>(null) }
    var imageStory by remember { mutableStateOf<HomeNewsItem?>(null) }
    var articleTextScale by remember { mutableStateOf(1f) }

    when (result) {
        null -> NewsStatePanel("Loading featured stories…")
        is HomeNewsResult.Failure -> NewsStatePanel("Featured news is unavailable right now.")
        is HomeNewsResult.Success -> if (result.stories.isEmpty()) NewsStatePanel("No featured stories have been published yet.")
        else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            result.stories.forEach { story ->
                Surface(
                    Modifier.width(268.dp).height(178.dp).noRippleClick { selectedStory = story }, shape = ParallelogramShape(18.dp), color = NewsCard, border = BorderStroke(1.dp, NewsOutline.copy(alpha = 0.60f)), shadowElevation = 3.dp,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        NewsStoryArtwork(story, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF10141D).copy(alpha = 0.95f)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(17.dp)) {
                            Text(story.category?.uppercase() ?: "FEATURED", color = NewsGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(5.dp)); Text(story.title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
    selectedStory?.let { story ->
        NewsStoryDialog(
            story = story,
            textScale = articleTextScale,
            onDecreaseText = { articleTextScale = (articleTextScale - 0.1f).coerceAtLeast(0.9f) },
            onIncreaseText = { articleTextScale = (articleTextScale + 0.1f).coerceAtMost(1.4f) },
            onViewImage = { imageStory = story },
            onDismiss = { selectedStory = null },
        )
    }
    imageStory?.let { story -> NewsImageDialog(story) { imageStory = null } }
}

@Composable
private fun NewsStoryArtwork(story: HomeNewsItem, modifier: Modifier) = Box(modifier) {
    if (!story.imageUrl.isNullOrBlank()) RemotePlayerImage(story.imageUrl, Modifier.fillMaxSize(), story.title)
    else Canvas(Modifier.fillMaxSize()) {
        drawRect(brush = Brush.linearGradient(listOf(Color(0xFF1D3153), Color(0xFF11151E))))
        drawCircle(NewsGold.copy(alpha = 0.21f), radius = size.minDimension * 0.70f, center = androidx.compose.ui.geometry.Offset(size.width, 0f))
    }
}

@Composable
private fun NewsStoryDialog(
    story: HomeNewsItem,
    textScale: Float,
    onDecreaseText: () -> Unit,
    onIncreaseText: () -> Unit,
    onViewImage: () -> Unit,
    onDismiss: () -> Unit,
) = Dialog(onDismissRequest = onDismiss) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = NewsCard) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(19.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(story.category?.uppercase() ?: "NEWS", color = NewsGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("A−", color = NewsBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(7.dp).noRippleClick(onDecreaseText))
                    Text("A+", color = NewsBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(7.dp).noRippleClick(onIncreaseText))
                    Text("×", color = NewsMuted, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 6.dp).noRippleClick(onDismiss))
                }
            }
            Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(20.dp)).noRippleClick(onViewImage)) { NewsStoryArtwork(story, Modifier.fillMaxSize()) }
            Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(story.author?.let { "By $it" }, story.createdAt?.let(::newsTimeLabel)).joinToString("  •  "),
                    color = NewsMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("A−  A+", color = NewsBlue, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(14.dp)); Text(story.title, color = NewsInk, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp * textScale), fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(14.dp))
            Text("ARTICLE", color = NewsGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Spacer(Modifier.height(7.dp))
            Text(
                story.content ?: story.summary ?: "The full article will be available here when it is published.",
                color = NewsInk,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp * textScale, lineHeight = 25.sp * textScale),
            )
            Spacer(Modifier.height(15.dp))
            Text("Tap the image to open it in the gallery viewer.", color = NewsBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NewsImageDialog(story: HomeNewsItem, onDismiss: () -> Unit) = Dialog(onDismissRequest = onDismiss) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += panChange
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.Black) {
        Box(Modifier.fillMaxWidth()) {
            NewsStoryArtwork(story, Modifier.fillMaxWidth().height(440.dp).transformable(transformState).graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y))
            Text("×", color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 2.dp).noRippleClick(onDismiss))
        }
    }
}

private fun newsTimeLabel(value: String): String = value.substringBefore('T').takeIf { it.isNotBlank() } ?: value

private val NewsCanvas: Color @Composable get() = MaterialTheme.colorScheme.background
private val NewsCard: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val NewsOutline: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val NewsInk: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val NewsMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val NewsBlue: Color @Composable get() = MaterialTheme.colorScheme.primary
private val NewsGold = Color(0xFFD49A32)
