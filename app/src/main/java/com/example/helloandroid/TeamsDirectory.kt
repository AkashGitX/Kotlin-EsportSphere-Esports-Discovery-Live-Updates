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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.helloandroid.data.TeamConnectionRow
import com.example.helloandroid.data.TeamsConnectionResult
import com.example.helloandroid.data.TeamsRepository
import kotlinx.coroutines.launch

/** The Teams sibling of Players: compact, cached and entirely database-backed. */
@Composable
internal fun TeamsScreen(onBack: () -> Unit) {
    val repository = remember { TeamsRepository() }
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var sortAscending by remember { mutableStateOf(true) }
    var teams by remember { mutableStateOf<List<TeamConnectionRow>>(emptyList()) }
    var totalCount by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var visibleCount by remember { mutableIntStateOf(10) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var prefetchedOffset by remember { mutableIntStateOf(-1) }

    suspend fun loadPage(reset: Boolean) {
        if (isLoading || (!reset && !hasMore)) return
        isLoading = true
        error = null
        val offset = if (reset) 0 else teams.size
        when (val page = repository.getTeams(limit = 25, offset = offset)) {
            is TeamsConnectionResult.Failure -> error = page.message
            is TeamsConnectionResult.Success -> {
                teams = (if (reset) page.teams else (teams + page.teams).distinctBy { it.id }).take(50)
                totalCount = page.totalCount?.coerceAtMost(50)
                hasMore = teams.size < 50 && page.teams.size == 25 && (totalCount == null || teams.size < totalCount!!)
            }
        }
        isLoading = false
    }

    LaunchedEffect(reloadKey) { loadPage(reset = true) }
    LaunchedEffect(query, selectedFilter, sortAscending) { visibleCount = 10 }
    LaunchedEffect(query, teams.size) { if (query.isNotBlank() && hasMore && !isLoading) loadPage(reset = false) }
    LaunchedEffect(teams.size, hasMore) {
        if (hasMore && teams.isNotEmpty() && teams.size < 50 && prefetchedOffset != teams.size) {
            prefetchedOffset = teams.size
            repository.prefetchTeams(offset = teams.size)
        }
    }
    LaunchedEffect(teams) {
        teams.take(16).mapNotNull { it.logoUrl?.takeIf(String::isNotBlank) }.forEach { url ->
            launch { PlayerImageMemoryCache.load(url) }
        }
    }

    val filtered = teams.filter { it.matchesTeamFilter(selectedFilter) }
        .filter { it.searchableText().contains(query.trim(), ignoreCase = true) }
        .let { rows -> if (sortAscending) rows.sortedBy { it.directoryName() } else rows.sortedByDescending { it.directoryName() } }
    val shownTeams = filtered.take(visibleCount)
    val directoryTotal = totalCount ?: teams.size

    Surface(Modifier.fillMaxSize(), color = TeamCanvas) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.player_profile_depth), null, Modifier.fillMaxSize().graphicsLayer(alpha = 0.32f), contentScale = ContentScale.Crop)
            Column(Modifier.fillMaxSize().navigationBarsPadding().padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", color = TeamInk, style = MaterialTheme.typography.displaySmall, modifier = Modifier.noRippleClick(onBack))
                    Spacer(Modifier.width(8.dp))
                    Text("ESPORTSPHERE", color = TeamMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                    Spacer(Modifier.weight(1f))
                    Text("•••", color = TeamInk, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
                Text("TEAMS", color = TeamInk, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(5.dp))
                Text("DISCOVER THE ORGANIZATIONS SHAPING THE COMPETITIVE SCENE", color = TeamMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(20.dp))
                TeamSearchField(query) { query = it }
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "INDIA", "SOUTH ASIA", "INTERNATIONAL", "VERIFIED").forEach { filter -> TeamFilterChip(filter, filter == selectedFilter) { selectedFilter = filter } }
                }
                Spacer(Modifier.height(17.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${directoryTotal}  /  TEAMS FOUND", color = TeamInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("SORT: ${if (sortAscending) "A–Z" else "Z–A"}  ▾", color = TeamMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.noRippleClick { sortAscending = !sortAscending })
                }
                Spacer(Modifier.height(13.dp))
                when {
                    isLoading && teams.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading teams…", color = TeamMuted) }
                    error != null && teams.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Unable to load teams", color = TeamInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(8.dp)); Text("Please check your connection and try again.", color = TeamMuted, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp)); Button(onClick = { reloadKey++ }, shape = ParallelogramShape(12.dp)) { Text("RETRY") }
                        }
                    }
                    shownTeams.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (teams.isEmpty()) "No teams available yet." else "No teams match your filters.", color = TeamMuted, textAlign = TextAlign.Center) }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        itemsIndexed(shownTeams, key = { _, team -> team.id }) { index, team ->
                            TeamDirectoryCard(index + 1, team)
                            if (index >= shownTeams.lastIndex - 2) LaunchedEffect(teams.size, visibleCount, index) {
                                if (visibleCount < filtered.size) visibleCount = minOf(visibleCount + 10, filtered.size)
                                else if (hasMore && !isLoading) loadPage(reset = false)
                            }
                        }
                        if (isLoading && teams.isNotEmpty()) item { Text("Loading more teams…", color = TeamMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamSearchField(query: String, onQueryChanged: (String) -> Unit) = Surface(
    Modifier.fillMaxWidth(), shape = ParallelogramShape(16.dp), color = TeamCard.copy(alpha = 0.88f), border = BorderStroke(1.dp, TeamOutline.copy(alpha = 0.7f)), shadowElevation = 2.dp,
) {
    Row(Modifier.padding(horizontal = 17.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("⌕", color = TeamInk, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        BasicTextField(value = query, onValueChange = onQueryChanged, singleLine = true, textStyle = TextStyle(color = TeamInk), modifier = Modifier.weight(1f), decorationBox = { inner -> if (query.isEmpty()) Text("Search teams, organizations or regions…", color = TeamMuted, maxLines = 1, overflow = TextOverflow.Ellipsis); inner() })
        Text("☷", color = TeamInk, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TeamFilterChip(label: String, selected: Boolean, onClick: () -> Unit) = Surface(
    Modifier.noRippleClick(onClick), shape = ParallelogramShape(12.dp), color = if (selected) TeamInk else TeamCard.copy(alpha = 0.82f), border = BorderStroke(1.dp, if (selected) TeamGold else TeamOutline.copy(alpha = 0.55f)),
) { Text(label, color = if (selected) Color.White else TeamInk, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) }

@Composable
private fun TeamDirectoryCard(rank: Int, team: TeamConnectionRow) {
    val name = team.directoryName()
    val shortName = team.shortName?.takeIf { !it.equals(name, true) }
    val location = listOf(team.country?.teamCountryFlag().orEmpty(), team.region.orEmpty()).filter { it.isNotBlank() }.joinToString("  |  ")
    val active = team.status?.equals("active", ignoreCase = true) != false
    Surface(Modifier.fillMaxWidth().height(82.dp), shape = ParallelogramShape(18.dp), color = TeamCard.copy(alpha = 0.84f), border = BorderStroke(1.dp, TeamOutline.copy(alpha = 0.55f)), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(76.dp).height(64.dp).clip(ParallelogramShape(13.dp))) {
                Canvas(Modifier.fillMaxSize()) { drawRect(brush = Brush.linearGradient(listOf(Color(0xFF171B24), Color(0xFF090B10)))) }
                if (!team.logoUrl.isNullOrBlank()) RemotePlayerImage(team.logoUrl, Modifier.fillMaxSize().padding(10.dp), name)
                else Text(name.take(2).uppercase(), color = TeamGold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
                Text("#${rank.toString().padStart(2, '0')}", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(7.dp).align(Alignment.TopStart))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(name.uppercase(), color = TeamInk, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                shortName?.let { Text(it.uppercase(), color = TeamMuted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, maxLines = 1) }
                if (location.isNotBlank()) Text(location, color = TeamMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Spacer(Modifier.width(5.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Text(if (team.isVerified) "◈ VERIFIED" else "◇ UNVERIFIED", color = if (team.isVerified) TeamGold else TeamMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Spacer(Modifier.height(6.dp)); Text("● ${if (active) "ACTIVE" else "INACTIVE"}", color = if (active) Color(0xFF1AAE5B) else TeamMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Spacer(Modifier.width(10.dp)); Box(Modifier.width(1.dp).height(45.dp).background(TeamGold.copy(alpha = 0.46f))); Spacer(Modifier.width(10.dp))
            Text("›", color = TeamInk, style = MaterialTheme.typography.displaySmall)
        }
    }
}

private fun TeamConnectionRow.directoryName(): String = displayName ?: name ?: shortName ?: "Team"
private fun TeamConnectionRow.searchableText(): String = listOfNotNull(name, shortName, displayName, country, region, status, profileSlug, organizationId).joinToString(" ")
private fun TeamConnectionRow.matchesTeamFilter(filter: String): Boolean = when (filter) {
    "ALL" -> true
    "INDIA" -> country.orEmpty().contains("india", true) || country.orEmpty().contains("ind", true)
    "SOUTH ASIA" -> region.orEmpty().contains("south asia", true)
    "INTERNATIONAL" -> !country.orEmpty().contains("india", true) && !country.orEmpty().contains("ind", true)
    "VERIFIED" -> isVerified
    else -> true
}

private fun String.teamCountryFlag(): String = when {
    contains("india", true) || equals("ind", true) -> "🇮🇳"
    contains("bangladesh", true) || equals("ban", true) -> "🇧🇩"
    contains("nepal", true) || equals("nep", true) -> "🇳🇵"
    else -> "◉"
}

private val TeamCanvas: Color @Composable get() = MaterialTheme.colorScheme.background
private val TeamCard: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val TeamOutline: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val TeamInk: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val TeamMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val TeamGold = Color(0xFFD49A32)
