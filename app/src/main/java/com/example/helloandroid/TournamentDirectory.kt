package com.example.helloandroid

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helloandroid.data.EventConnectionRow
import com.example.helloandroid.data.EventsConnectionResult
import com.example.helloandroid.data.EventsRepository

/** Tournament directory reached by the Home shortcut; the carousel continues to open details. */
@Composable
internal fun TournamentDirectoryScreen(
    onBack: () -> Unit,
    onTournamentSelected: (EventConnectionRow) -> Unit,
) {
    val repository = remember { EventsRepository() }
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var events by remember { mutableStateOf<List<EventConnectionRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(retryKey) {
        loading = true
        error = null
        when (val result = repository.getEvents()) {
            is EventsConnectionResult.Success -> events = result.events
            is EventsConnectionResult.Failure -> error = result.message
        }
        loading = false
    }

    val filtered = events
        .filter { it.matchesTournamentFilter(selectedFilter) }
        .filter { it.searchText().contains(query.trim(), ignoreCase = true) }
        .sortedWith(compareBy<EventConnectionRow> { it.tournamentStatusOrder() }.thenBy { it.name.lowercase() })

    Surface(Modifier.fillMaxSize(), color = TournamentCanvas) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painterResource(R.drawable.player_profile_depth),
                null,
                Modifier.fillMaxSize().graphicsLayer(alpha = 0.3f),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", color = TournamentInk, fontFamily = Satoshi, fontSize = 42.sp, modifier = Modifier.noRippleClick(onBack))
                    Spacer(Modifier.width(10.dp))
                    Text("ESPORTSPHERE", color = TournamentMuted, fontFamily = Satoshi, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                    Spacer(Modifier.weight(1f))
                    Text("•••", color = TournamentInk, fontFamily = Satoshi, fontSize = 20.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text("TOURNAMENTS", color = TournamentInk, fontFamily = Satoshi, fontSize = 31.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text("FOLLOW THE EVENTS SHAPING COMPETITIVE ESPORTS", color = TournamentMuted, fontFamily = Satoshi, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.45.sp)
                Spacer(Modifier.height(20.dp))
                TournamentSearchField(query) { query = it }
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "LIVE", "UPCOMING", "COMPLETED").forEach { filter ->
                        TournamentFilter(filter, filter == selectedFilter) { selectedFilter = filter }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("${filtered.size}  /  TOURNAMENTS FOUND", color = TournamentInk, fontFamily = Satoshi, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(13.dp))
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading tournaments…", color = TournamentMuted) }
                    error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Unable to load tournaments", color = TournamentInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp))
                            Text("Please check your connection and try again.", color = TournamentMuted, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { retryKey++ }, shape = ParallelogramShape(12.dp)) { Text("RETRY") }
                        }
                    }
                    filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (events.isEmpty()) "No tournaments available yet." else "No tournaments match your filters.", color = TournamentMuted, textAlign = TextAlign.Center)
                    }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filtered, key = EventConnectionRow::id) { event ->
                            TournamentDirectoryCard(event) { onTournamentSelected(event) }
                        }
                        item { Spacer(Modifier.height(90.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentSearchField(query: String, onQueryChanged: (String) -> Unit) = Surface(
    Modifier.fillMaxWidth(),
    shape = ParallelogramShape(14.dp),
    color = TournamentCard.copy(alpha = 0.9f),
    border = BorderStroke(1.dp, TournamentOutline.copy(alpha = 0.7f)),
) {
    Row(Modifier.padding(horizontal = 17.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("⌕", color = TournamentInk, fontSize = 23.sp)
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(color = TournamentInk, fontFamily = Satoshi),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search tournaments, games or organizers…", color = TournamentMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                inner()
            },
        )
    }
}

@Composable
private fun TournamentFilter(label: String, selected: Boolean, onClick: () -> Unit) = Surface(
    Modifier.noRippleClick(onClick),
    shape = ParallelogramShape(11.dp),
    color = if (selected) TournamentInk else TournamentCard.copy(alpha = 0.85f),
    border = BorderStroke(1.dp, if (selected) TournamentGold else TournamentOutline.copy(alpha = 0.6f)),
) {
    Text(label, color = if (selected) Color.White else TournamentInk, fontFamily = Satoshi, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 17.dp, vertical = 11.dp))
}

@Composable
private fun TournamentDirectoryCard(event: EventConnectionRow, onClick: () -> Unit) {
    val status = event.status?.uppercase()?.takeIf(String::isNotBlank) ?: "UPCOMING"
    val date = listOfNotNull(event.startDate?.shortTournamentDate(), event.endDate?.shortTournamentDate()).distinct().joinToString(" — ")
    Surface(
        Modifier.fillMaxWidth().height(116.dp).noRippleClick(onClick),
        shape = ParallelogramShape(17.dp),
        color = TournamentCard.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, TournamentOutline.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(92.dp).height(94.dp).clip(ParallelogramShape(13.dp)).background(TournamentInk)) {
                event.bannerUrl?.takeIf(String::isNotBlank)?.let { RemotePlayerImage(it, Modifier.fillMaxSize(), event.name) }
                    ?: event.logoUrl?.takeIf(String::isNotBlank)?.let { RemotePlayerImage(it, Modifier.fillMaxSize().padding(16.dp), event.name) }
                    ?: Text(event.name.take(2).uppercase(), color = TournamentGold, fontFamily = Satoshi, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(status, color = if (event.isLiveTournament()) Color(0xFFE33D48) else TournamentGold, fontFamily = Satoshi, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(event.name, color = TournamentInk, fontFamily = Satoshi, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                event.currentRound?.takeIf(String::isNotBlank)?.let { Text(it, color = TournamentMuted, fontFamily = Satoshi, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                val metadata = listOfNotNull(event.game, date.takeIf(String::isNotBlank), event.organizer).joinToString("  ·  ")
                if (metadata.isNotBlank()) Text(metadata, color = TournamentMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text("›", color = TournamentInk, fontFamily = Satoshi, fontSize = 35.sp)
        }
    }
}

private fun EventConnectionRow.searchText() = listOfNotNull(name, shortName, status, currentRound, game, organizer, location).joinToString(" ")
private fun EventConnectionRow.isLiveTournament() = status.orEmpty().contains("live", true) || status.orEmpty().contains("ongoing", true) || status.orEmpty().contains("active", true)
private fun EventConnectionRow.tournamentStatusOrder() = when {
    isLiveTournament() -> 0
    status.orEmpty().contains("upcoming", true) || status.orEmpty().contains("scheduled", true) -> 1
    else -> 2
}
private fun EventConnectionRow.matchesTournamentFilter(filter: String) = when (filter) {
    "LIVE" -> isLiveTournament()
    "UPCOMING" -> status.orEmpty().contains("upcoming", true) || status.orEmpty().contains("scheduled", true)
    "COMPLETED" -> status.orEmpty().contains("complete", true) || status.orEmpty().contains("finished", true) || status.orEmpty().contains("ended", true)
    else -> true
}
private fun String.shortTournamentDate() = substringBefore('T').takeIf(String::isNotBlank) ?: this

private val TournamentCanvas: Color @Composable get() = MaterialTheme.colorScheme.background
private val TournamentCard: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val TournamentOutline: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val TournamentInk: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val TournamentMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val TournamentGold = Color(0xFFD49A32)
