package com.example.helloandroid

import android.os.Bundle
import android.os.Build
import android.net.Uri
import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.widget.Toast
import android.widget.ImageView
import android.util.Log
import org.json.JSONObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Outline as ComposeOutline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import com.google.firebase.messaging.FirebaseMessaging
import com.example.helloandroid.data.PlayerConnectionRow
import com.example.helloandroid.data.PlayersConnectionResult
import com.example.helloandroid.data.PlayersRepository
import com.example.helloandroid.data.HomeNewsResult
import com.example.helloandroid.data.NewsRepository
import com.example.helloandroid.data.HomeCarouselItem
import com.example.helloandroid.data.HomeCarouselRepository
import com.example.helloandroid.data.HomeCarouselResult
import com.example.helloandroid.data.EventConnectionRow

private enum class Screen { Splash, Onboarding, Home, Explore, Teams, Players, PlayerDetails, TournamentDirectory, Tournament, Ask, Watch, Profile, Settings, Account, Preferences, Notifications, Login, SignUp, Performance }
private const val AppVersion = "1.7.0"
internal val Satoshi = FontFamily(
    Font(R.font.satoshi_medium, FontWeight.Medium),
    Font(R.font.satoshi_bold, FontWeight.Bold),
    Font(R.font.satoshi_black, FontWeight.Black),
)

class MainActivity : ComponentActivity() {
    private var openSignInAfterRedirect by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openSignInAfterRedirect = intent.isEsportSphereAuthRedirect()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("EsportSphereFCM", "FCM token obtained on app start")
            FcmDeviceSync.synchronize(applicationContext, token)
        }.addOnFailureListener { error ->
            Log.w("EsportSphereFCM", "Unable to obtain FCM token", error)
        }
        setContent { EsportSphereApp(openSignInAfterRedirect = openSignInAfterRedirect) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.isEsportSphereAuthRedirect()) openSignInAfterRedirect = true
    }
}

private fun Intent?.isEsportSphereAuthRedirect(): Boolean =
    this?.action == Intent.ACTION_VIEW && data?.scheme == "esportsphere" && data?.host == "open"

@Composable
fun EsportSphereApp(openSignInAfterRedirect: Boolean = false) {
    val context = LocalContext.current
    val appPreferences = remember(context) { context.getSharedPreferences(AppPreferencesName, 0) }
    val appScope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.Splash) }
    var isLoggedIn by remember { mutableStateOf(appPreferences.getBoolean(PrefSignedIn, false)) }
    var avatarPreset by remember { mutableIntStateOf(appPreferences.getInt(PrefAvatarPreset, 0)) }
    var uploadedAvatarUri by remember { mutableStateOf(appPreferences.getString(PrefAvatarUri, null)) }
    var themeChoice by remember { mutableStateOf(appPreferences.readThemeChoice()) }
    var selectedPlayer by remember { mutableStateOf<PlayerConnectionRow?>(null) }
    var selectedEvent by remember { mutableStateOf<EventConnectionRow?>(null) }
    var lastBackPressTime by remember { mutableStateOf(0L) }
    var homeGreetingFinished by remember { mutableStateOf(false) }
    val carouselRepository = remember { HomeCarouselRepository() }
    var homeCarousel by remember { mutableStateOf<HomeCarouselResult?>(null) }
    BackHandler(enabled = screen != Screen.Splash && screen != Screen.Onboarding) {
        if (screen != Screen.Home) {
            screen = if (screen == Screen.PlayerDetails) Screen.Players else Screen.Home
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2_000) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(openSignInAfterRedirect) {
        if (openSignInAfterRedirect) {
            screen = Screen.Login
        } else {
            delay(1_650)
            screen = if (isLoggedIn || appPreferences.getBoolean(PrefOnboardingComplete, false)) Screen.Home else Screen.Onboarding
        }
    }
    LaunchedEffect(Unit) {
        val appContext = context.applicationContext
        carouselRepository.cachedActive(appContext).takeIf { it.isNotEmpty() }?.let {
            homeCarousel = HomeCarouselResult.Success(it)
        }
        val refreshed = withTimeoutOrNull(1_200) { carouselRepository.active(appContext) }
        if (refreshed != null) {
            homeCarousel = refreshed
        } else {
            // Continue syncing after the splash has yielded the screen; existing cached
            // slides remain visible until a complete replacement is ready.
            launch { homeCarousel = carouselRepository.active(appContext) }
        }
    }
    val useDarkTheme = when (themeChoice) {
        ThemeChoice.Light -> false
        ThemeChoice.Dark -> true
        ThemeChoice.Device -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (useDarkTheme) EsportSphereDarkColors else EsportSphereLightColors) {
        when (screen) {
            Screen.Splash -> SplashScreen()
            Screen.Onboarding -> OnboardingScreen { appPreferences.edit().putBoolean(PrefOnboardingComplete, true).apply(); screen = Screen.Home }
            else -> AppShell(
                screen = screen,
                isLoggedIn = isLoggedIn,
                avatarPreset = avatarPreset,
                uploadedAvatarUri = uploadedAvatarUri,
                navigate = { screen = it },
                onSignedIn = { result ->
                    isLoggedIn = true
                    appPreferences.edit().putBoolean(PrefSignedIn, true).putBoolean(PrefOnboardingComplete, true).putString(PrefAccessToken, result.accessToken).putString(PrefRefreshToken, result.refreshToken).putString(PrefAccountId, result.userId).putString(PrefAccountName, result.name).putString(PrefAccountEmail, result.email).putString(PrefAccountPhone, result.phone).apply()
                    appScope.launch {
                        findPersistentAvatarUri(context.applicationContext, result.userId)?.let { savedUri ->
                            uploadedAvatarUri = savedUri.toString()
                            appPreferences.edit().putString(PrefAvatarUri, savedUri.toString()).apply()
                        }
                    }
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> FcmDeviceSync.synchronize(context.applicationContext, token) }
                    screen = Screen.Home
                },
                onSignedOut = {
                    FcmDeviceSync.removeForCurrentUser(
                        context.applicationContext,
                        appPreferences.getString(PrefAccessToken, null),
                        appPreferences.getString(PrefAccountId, null),
                    )
                    isLoggedIn = false; uploadedAvatarUri = null; avatarPreset = 0
                    appPreferences.edit().putBoolean(PrefSignedIn, false).remove(PrefAvatarUri).remove(PrefAvatarPreset).remove(PrefAccessToken).remove(PrefRefreshToken).remove(PrefAccountId).remove(PrefAccountName).remove(PrefAccountEmail).remove(PrefAccountPhone).apply(); screen = Screen.Home
                },
                onAvatarChanged = { preset, uri -> avatarPreset = preset; uploadedAvatarUri = uri; appPreferences.edit().putInt(PrefAvatarPreset, preset).apply(); if (uri == null) appPreferences.edit().remove(PrefAvatarUri).apply() else appPreferences.edit().putString(PrefAvatarUri, uri).apply() },
                themeChoice = themeChoice,
                onThemeChoiceChanged = { themeChoice = it; appPreferences.edit().putString(PrefTheme, it.name).apply() },
                selectedPlayer = selectedPlayer,
                onPlayerSelected = { selectedPlayer = it },
                selectedEvent = selectedEvent,
                onEventSelected = { selectedEvent = it },
                showHomeGreeting = !homeGreetingFinished,
                onHomeGreetingFinished = { homeGreetingFinished = true },
                homeCarousel = homeCarousel,
            )
        }
    }
}

@Composable
private fun SplashScreen() = Surface(Modifier.fillMaxSize(), color = Canvas) {
    val splashLogo = if (MaterialTheme.colorScheme.background == EsportSphereDarkColors.background) R.drawable.esportsphere_splash_white else R.drawable.esportsphere_splash_black
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(splashLogo), "EsportSphere", Modifier.size(220.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(18.dp))
        Text("EsportSphere", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("THE COMPETITIVE GAMING UNIVERSE", color = MutedInk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    val titles = listOf("All esports. One sphere.", "Never miss the action.", "Your esports command center.")
    val bodies = listOf(
        "Follow your favourite teams, leagues and games in one place.",
        "Track live scores, upcoming matches and tournament results.",
        "Ask Sphere for real esports answers, personalised to you.",
    )
    val symbols = listOf("◎", "◷", "✦")
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("EsportSphere", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Skip", color = MutedInk, modifier = Modifier.noRippleClick(onFinish))
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Surface(shape = CircleShape, color = SoftBlue, modifier = Modifier.size(180.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(symbols[page], color = Blue, style = MaterialTheme.typography.displayLarge) }
                    }
                    Spacer(Modifier.height(36.dp))
                    Text(titles[page], color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(bodies[page], color = MutedInk, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(titles.size) { index -> Surface(Modifier.size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp), shape = CircleShape, color = if (index == pagerState.currentPage) Blue else Color(0xFFE2E5EC)) {} }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { if (pagerState.currentPage == titles.lastIndex) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                shape = RoundedCornerShape(16.dp),
            ) { Text(if (pagerState.currentPage == titles.lastIndex) "Enter EsportSphere" else "Continue", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AppShell(
    screen: Screen,
    isLoggedIn: Boolean,
    avatarPreset: Int,
    uploadedAvatarUri: String?,
    navigate: (Screen) -> Unit,
    onSignedIn: (SupabaseAuthResult) -> Unit,
    onSignedOut: () -> Unit,
    onAvatarChanged: (Int, String?) -> Unit,
    themeChoice: ThemeChoice,
    onThemeChoiceChanged: (ThemeChoice) -> Unit,
    selectedPlayer: PlayerConnectionRow?,
    onPlayerSelected: (PlayerConnectionRow) -> Unit,
    selectedEvent: EventConnectionRow?,
    onEventSelected: (EventConnectionRow?) -> Unit,
    showHomeGreeting: Boolean,
    onHomeGreetingFinished: () -> Unit,
    homeCarousel: HomeCarouselResult?,
) {
    var showNavigation by remember { mutableStateOf(true) }
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    navigate = {
                        showNavigation = true
                        navigate(it)
                    },
                    onNavigationVisibilityChanged = { showNavigation = it },
                    isLoggedIn = isLoggedIn,
                    avatarPreset = avatarPreset,
                    uploadedAvatarUri = uploadedAvatarUri,
                    showGreeting = showHomeGreeting,
                    onGreetingFinished = onHomeGreetingFinished,
                    homeCarousel = homeCarousel,
                )
                Screen.Explore -> ExploreScreen()
                Screen.Teams -> TeamsScreen(onBack = { navigate(Screen.Home) })
                Screen.Players -> PlayersScreen(
                    onBack = { navigate(Screen.Home) },
                    onPlayerSelected = { player -> onPlayerSelected(player); navigate(Screen.PlayerDetails) },
                )
                Screen.PlayerDetails -> selectedPlayer?.let { player -> PlayerDetailsScreen(player) { navigate(Screen.Players) } }
                Screen.TournamentDirectory -> TournamentDirectoryScreen(
                    onBack = { navigate(Screen.Home) },
                    onTournamentSelected = { event ->
                        onEventSelected(event)
                        navigate(Screen.Tournament)
                    },
                )
                Screen.Tournament -> TournamentScreen(
                    tournament = null,
                    event = selectedEvent,
                    onBack = { navigate(Screen.Home) },
                )
                Screen.Ask -> AskSphereScreen()
                Screen.Watch -> WatchScreen()
                Screen.Profile -> ProfileScreen(navigate)
                    Screen.Settings -> SettingsScreen(
                    isLoggedIn = isLoggedIn,
                    avatarPreset = avatarPreset,
                    uploadedAvatarUri = uploadedAvatarUri,
                    onAvatarChanged = onAvatarChanged,
                    onBack = { navigate(Screen.Home) },
                    onSignIn = { navigate(Screen.Login) },
                        onLogOut = onSignedOut,
                        onOpenAccount = { navigate(Screen.Account) },
                        onOpenPreferences = { navigate(Screen.Preferences) },
                        onOpenNotifications = { navigate(Screen.Notifications) },
                    )
                    Screen.Account -> AccountScreen(onBack = { navigate(Screen.Settings) }, onSignedOut = onSignedOut)
                    Screen.Preferences -> PreferencesScreen(themeChoice, onThemeChoiceChanged, onBack = { navigate(Screen.Settings) })
                    Screen.Notifications -> NotificationsScreen(onBack = { navigate(Screen.Settings) })
                Screen.Login -> LoginScreen(onSignedIn = onSignedIn, onSignUp = { navigate(Screen.SignUp) }, onBack = { navigate(Screen.Home) })
                Screen.SignUp -> SignUpScreen(onSignedUp = onSignedIn, onBack = { navigate(Screen.Login) })
                Screen.Performance -> ComingSoonScreen()
                else -> Unit
            }
            AnimatedVisibility(
                visible = showNavigation && screen !in setOf(Screen.Login, Screen.SignUp, Screen.Settings, Screen.Account, Screen.Preferences, Screen.Notifications, Screen.Teams, Screen.Players, Screen.PlayerDetails, Screen.TournamentDirectory, Screen.Tournament),
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomNavigation(screen) {
                    showNavigation = true
                    navigate(it)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    navigate: (Screen) -> Unit,
    onNavigationVisibilityChanged: (Boolean) -> Unit,
    isLoggedIn: Boolean,
    avatarPreset: Int,
    uploadedAvatarUri: String?,
    showGreeting: Boolean,
    onGreetingFinished: () -> Unit,
    homeCarousel: HomeCarouselResult?,
): Unit {
    val context = LocalContext.current
    var showBrandName by remember(showGreeting) { mutableStateOf(!showGreeting) }
    val greeting = remember { timeOfDayGreeting() }
    fun showMessage(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    val newsRepository = remember { NewsRepository() }
    var featuredNews by remember { mutableStateOf<HomeNewsResult?>(null) }
    LaunchedEffect(Unit) {
        launch { featuredNews = newsRepository.featured() }
    }
    LaunchedEffect(showGreeting) {
        if (showGreeting) {
            delay(3_000)
            showBrandName = true
            onGreetingFinished()
        }
    }
    val feedScroll = rememberScrollState()
    LaunchedEffect(feedScroll) {
        var previousPosition = 0
        snapshotFlow { feedScroll.value }.collect { currentPosition ->
            when {
                currentPosition > previousPosition -> onNavigationVisibilityChanged(false)
                currentPosition < previousPosition -> onNavigationVisibilityChanged(true)
            }
            previousPosition = currentPosition
        }
    }
    Box(Modifier.fillMaxSize()) {
        HomeDepthBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(feedScroll).padding(start = 20.dp, end = 20.dp, bottom = 96.dp),
        ) {
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.esportsphere_symbol),
                contentDescription = "EsportSphere",
                modifier = Modifier.size(42.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(9.dp))
            Column {
                    AnimatedContent(
                        targetState = showBrandName,
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { -it / 2 } + fadeOut())
                        },
                        label = "home-heading",
                    ) { displayBrandName ->
                        Text(
                            if (displayBrandName) "ESPORTSPHERE" else greeting,
                            color = Ink,
                            fontSize = if (displayBrandName) 19.sp else 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                    }
                    Text("PLAY · COMPETE · BELONG", color = MutedInk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        if (isLoggedIn) {
            Box(Modifier.size(46.dp).noRippleClick { navigate(Screen.Settings) }) { AvatarPreview(avatarPreset, uploadedAvatarUri?.let(Uri::parse), Modifier.fillMaxSize()) }
        } else {
            Button(onClick = { navigate(Screen.Login) }, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(16.dp)) { Text("Sign in", fontWeight = FontWeight.Bold) }
        }
    }
    Spacer(Modifier.height(16.dp))
    HomeSearchBar { result -> showMessage("$result profile is coming next.") }
    (homeCarousel as? HomeCarouselResult.Success)?.items?.takeIf { it.isNotEmpty() }?.let { items ->
        Spacer(Modifier.height(22.dp))
        TournamentHero(items = items)
        Spacer(Modifier.height(22.dp))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Shortcut("♜", "Tournaments", Modifier.weight(1f)) { navigate(Screen.TournamentDirectory) }
        Shortcut("♧", "Teams", Modifier.weight(1f)) { navigate(Screen.Teams) }
        Shortcut("♙", "Players", Modifier.weight(1f)) { navigate(Screen.Players) }
        Shortcut("▤", "News", Modifier.weight(1f)) { navigate(Screen.Explore) }
    }
    Spacer(Modifier.height(30.dp))
    SectionTitle("Live Now  ●", "View All  ›") { showMessage("Live match directory is coming next.") }
    Spacer(Modifier.height(12.dp))
    LiveCard { showMessage("Watch integration will open YouTube or X later.") }
    Spacer(Modifier.height(30.dp))
    SectionTitle("Upcoming Match", "View All  ›") { showMessage("Match schedule is coming next.") }
    Spacer(Modifier.height(12.dp))
    UpcomingMatch { showMessage("GodLike vs Revenant · Today, 6:00 PM") }
    Spacer(Modifier.height(30.dp))
    SectionTitle("Featured Teams", "View All  ›") { navigate(Screen.Explore) }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TeamTile("GL", "GodLike", Modifier.weight(1f)) { showMessage("GodLike team page is coming next.") }
        TeamTile("RNT", "Revenant", Modifier.weight(1f)) { showMessage("Revenant team page is coming next.") }
        TeamTile("V", "Team Vitality", Modifier.weight(1f)) { showMessage("Team Vitality page is coming next.") }
        TeamTile("S8", "S8UL", Modifier.weight(1f)) { showMessage("S8UL team page is coming next.") }
    }
    Spacer(Modifier.height(30.dp))
    SectionTitle("FEATURED NEWS", "View All  ›") { navigate(Screen.Explore) }
    Spacer(Modifier.height(12.dp))
    FeaturedNewsCarousel(result = featuredNews)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeDepthBackground() {
    Image(
        painter = painterResource(R.drawable.player_profile_depth),
        contentDescription = null,
        modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.32f),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun HomeSearchBar(onResultClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val directory = remember {
        listOf(
            "Team • GodLike", "Team • Revenant", "Team • Team Vitality", "Team • S8UL",
            "Player • Jonathan", "Player • Mortal", "Player • Scout", "Player • Alpha Clasher",
        ).shuffled()
    }
    val results = if (query.isBlank()) directory.take(4) else directory.filter { it.contains(query, ignoreCase = true) }.take(4)
    Column {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Card,
            border = BorderStroke(1.dp, Outline),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", color = Blue, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Ink),
                    decorationBox = { field ->
                        if (query.isEmpty()) Text("Search teams, players and events", color = MutedInk)
                        field()
                    },
                )
                if (query.isNotEmpty()) Text("×", color = MutedInk, style = MaterialTheme.typography.titleLarge, modifier = Modifier.noRippleClick { query = "" })
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(if (query.isBlank()) "LOOKING FOR" else "MATCHES", color = MutedInk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        if (results.isEmpty()) {
            Text("No demo result found", color = MutedInk, style = MaterialTheme.typography.bodySmall)
        } else if (query.isBlank()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                results.forEach { result ->
                    Surface(
                        modifier = Modifier.width(84.dp).noRippleClick { onResultClick(result.removePrefix("Team • ").removePrefix("Player • ")) },
                        shape = RoundedCornerShape(12.dp),
                        color = SoftBlue,
                    ) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(22.dp), shape = CircleShape, color = Card) {
                                Image(painterResource(R.drawable.esportsphere_symbol), contentDescription = null, modifier = Modifier.padding(3.dp), contentScale = ContentScale.Fit)
                            }
                            Spacer(Modifier.width(5.dp))
                            Text(result.substringAfter("• "), color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                results.forEach { result -> SearchResultRow(result) { onResultClick(result.substringAfter("• ")) } }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: String, onClick: () -> Unit) {
    val name = result.substringAfter("• ")
    val kind = if (result.startsWith("Team")) "Organisation" else "Player"
    val badge = if (result.startsWith("Team")) name.take(2).uppercase() else name.take(1).uppercase()
    Surface(
        Modifier.fillMaxWidth().noRippleClick(onClick),
        shape = RoundedCornerShape(14.dp),
        color = Card,
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.7f)),
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(34.dp), shape = CircleShape, color = if (result.startsWith("Team")) Gold else Blue) {
                Box(contentAlignment = Alignment.Center) { Text(badge, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) }
            }
            Spacer(Modifier.width(11.dp))
            Text(name, color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(7.dp))
            Text("|", color = MutedInk)
            Spacer(Modifier.width(7.dp))
            Text(kind, color = MutedInk, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TournamentHero(items: List<HomeCarouselItem>) {
    val colours = listOf(Card, SoftBlue, MaterialTheme.colorScheme.tertiaryContainer)
    val pagerState = rememberPagerState(pageCount = { items.size })
    LaunchedEffect(items.size) {
        while (items.size > 1) {
            delay(5_000)
            pagerState.animateScrollToPage(
                page = (pagerState.currentPage + 1) % items.size,
                animationSpec = tween(durationMillis = 850),
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(285.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { activePage ->
        val pageOffset = ((pagerState.currentPage - activePage) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
        val item = items[activePage]
        val sourceBitmap = remember(item.localImagePath) { item.localImagePath?.let(BitmapFactory::decodeFile) }
        val foreground = Color.White
        Surface(
                Modifier.fillMaxSize()
                    .padding(vertical = 7.dp)
                    .graphicsLayer {
                        // Rotate around the entering/leaving edge for a restrained cube turn.
                        rotationY = pageOffset * 52f
                        transformOrigin = TransformOrigin(if (pageOffset > 0f) 0f else 1f, 0.5f)
                        cameraDistance = 7f * density
                        alpha = 1f - kotlin.math.abs(pageOffset) * 0.08f
                    },
            shape = RoundedCornerShape(22.dp),
            color = colours[activePage % colours.size],
            shadowElevation = 7.dp,
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.TopCenter)
                        .padding(start = 11.dp, top = 11.dp, end = 11.dp)
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp)),
                ) {
                    sourceBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } ?: Image(
                        painterResource(R.drawable.esportsphere_symbol),
                        contentDescription = "EsportSphere",
                        modifier = Modifier.align(Alignment.Center).size(142.dp),
                        contentScale = ContentScale.Fit,
                    )
                    // These fades are clipped with the image, including its lower corners.
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f), Color.Black.copy(alpha = 0.67f)))))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.94f)))))
                }
                // Facts and copy live in a soft grey rail, leaving the image unobstructed.
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Ink.copy(alpha = 0.74f),
                ) {
                    Text(
                        item.label ?: "FEATURED",
                        color = foreground,
                        fontFamily = Satoshi,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
                // The only text over the image is its headline; all supporting details live below.
                Text(
                    item.title,
                    color = Color.White,
                    fontFamily = Satoshi,
                    fontSize = item.title.carouselTitleSize(),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 145.dp, end = 20.dp),
                )
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Card.copy(alpha = 0.97f)),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 82.dp, bottom = 31.dp)) {
                    Text(
                        item.subtitle ?: "TOURNAMENT DETAILS",
                        color = Blue,
                        fontFamily = Satoshi,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.description?.let { description -> Spacer(Modifier.height(4.dp)); Text(description, color = MutedInk, fontFamily = Satoshi, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 19.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A five-dot atmospheric pager mark: solid at the center and fading
                    // completely away toward both edges, as in the Home visual system.
                    repeat(5) { index ->
                        val alpha = when (kotlin.math.abs(index - 2)) {
                            0 -> 1f
                            1 -> 0.48f
                            else -> 0f
                        }
                        Box(Modifier.size(5.dp).clip(CircleShape).background(Ink.copy(alpha = alpha)))
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun TournamentScreen(tournament: HomeCarouselItem?, event: EventConnectionRow?, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Overview") }
    val bannerBitmap = remember(tournament?.localImagePath) {
        tournament?.localImagePath?.let(BitmapFactory::decodeFile)
    }
    val title = event?.name ?: tournament?.title?.ifBlank { "TOURNAMENT" } ?: "TOURNAMENTS"
    val round = event?.currentRound ?: tournament?.subtitle?.takeIf(String::isNotBlank) ?: "Round information will be announced soon"
    val label = event?.status?.uppercase()?.takeIf(String::isNotBlank)
        ?: tournament?.label?.uppercase()?.takeIf(String::isNotBlank)
        ?: "TOURNAMENT"

    Box(Modifier.fillMaxSize()) {
        HomeDepthBackground()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // The banner is deliberately full width; tournament identity belongs on the image,
            // not inside a smaller home-style card.
            Box(Modifier.fillMaxWidth().height(410.dp)) {
                if (bannerBitmap != null) {
                    Image(
                        bitmap = bannerBitmap.asImageBitmap(),
                        contentDescription = "$title tournament banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else if (!event?.bannerUrl.isNullOrBlank()) {
                    RemotePlayerImage(event?.bannerUrl.orEmpty(), Modifier.fillMaxSize(), "$title tournament banner")
                } else {
                    Box(Modifier.fillMaxSize().background(Ink))
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.92f)),
                        ),
                    ),
                )
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "‹",
                        color = Color.White,
                        fontFamily = Satoshi,
                        fontSize = 42.sp,
                        modifier = Modifier.noRippleClick(onBack),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "TOURNAMENT",
                        color = Color.White,
                        fontFamily = Satoshi,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
                Text(
                    label,
                    color = Color.White,
                    fontFamily = Satoshi,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(22.dp),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                    Text(
                        "CURRENT ROUND · $round",
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = Satoshi,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        title,
                        color = Color.White,
                        fontFamily = Satoshi,
                        fontSize = title.carouselTitleSize(),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }

            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                TournamentInfoStrip(round = round, label = label)
                Spacer(Modifier.height(26.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(23.dp),
                ) {
                    listOf("Overview", "Matches", "Standings", "Teams").forEach { tab ->
                        TournamentTab(tab, selected = selectedTab == tab) { selectedTab = tab }
                    }
                }
                Spacer(Modifier.height(26.dp))
                when (selectedTab) {
                    "Overview" -> TournamentOverview(tournament, event, round)
                    "Matches" -> TournamentEmptyState("MATCH SCHEDULE", "Matches for this tournament will appear here as soon as the mapped event schedule is available.")
                    "Standings" -> TournamentEmptyState("STANDINGS", "Live standings will appear here as the tournament progresses.")
                    "Teams" -> TournamentEmptyState("PARTICIPATING TEAMS", "Registered teams will appear here once the event roster is available.")
                }
                Spacer(Modifier.height(42.dp))
            }
        }
    }
}

@Composable
private fun TournamentInfoStrip(round: String, label: String) = Surface(
    Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(0.dp),
    color = Card.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, Outline.copy(alpha = 0.7f)),
) {
    Row(Modifier.padding(vertical = 15.dp, horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        TournamentInfoValue("STATUS", label)
        Box(Modifier.width(1.dp).height(36.dp).background(Outline))
        TournamentInfoValue("ROUND", round, Modifier.weight(1f))
    }
}

@Composable
private fun TournamentInfoValue(label: String, value: String, modifier: Modifier = Modifier) = Column(
    modifier.padding(horizontal = 10.dp),
) {
    Text(label, color = MutedInk, fontFamily = Satoshi, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(4.dp))
    Text(value, color = Ink, fontFamily = Satoshi, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun TournamentTab(title: String, selected: Boolean, onClick: () -> Unit) = Column(
    modifier = Modifier.noRippleClick(onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(
        title.uppercase(),
        color = if (selected) Ink else MutedInk,
        fontFamily = Satoshi,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
    )
    Spacer(Modifier.height(8.dp))
    Box(Modifier.width(if (selected) 28.dp else 0.dp).height(2.dp).background(if (selected) Gold else Color.Transparent))
}

@Composable
private fun TournamentOverview(tournament: HomeCarouselItem?, event: EventConnectionRow?, round: String) {
    TournamentSectionLabel("ABOUT THE TOURNAMENT")
    Spacer(Modifier.height(12.dp))
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Card.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.7f)),
    ) {
        Text(
            event?.description?.takeIf(String::isNotBlank)
                ?: tournament?.description?.takeIf(String::isNotBlank)
                ?: "Tournament details, format and coverage will be updated here.",
            color = Ink,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(18.dp),
        )
    }
    Spacer(Modifier.height(28.dp))
    TournamentSectionLabel("TOURNAMENT STATUS")
    Spacer(Modifier.height(12.dp))
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Ink,
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(Gold))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("CURRENT ROUND", color = Color.White.copy(alpha = 0.68f), fontFamily = Satoshi, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Spacer(Modifier.height(4.dp))
                Text(round, color = Color.White, fontFamily = Satoshi, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2)
            }
        }
    }
}

@Composable
private fun TournamentSectionLabel(title: String) = Text(
    title,
    color = Ink,
    fontFamily = Satoshi,
    fontSize = 14.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = 1.3.sp,
)

@Composable
private fun TournamentEmptyState(title: String, message: String) {
    TournamentSectionLabel(title)
    Spacer(Modifier.height(12.dp))
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Card.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.7f)),
    ) {
        Text(message, color = MutedInk, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(20.dp))
    }
}

/** Keeps every carousel title on one line by scaling it to the available title area. */
private fun String.carouselTitleSize() =
    (252f / (trim().replace(Regex("\\s+"), " ").length.coerceAtLeast(1) * 0.57f))
        .coerceIn(10f, 27f)
        .sp

@Composable
private fun Shortcut(icon: String, label: String, modifier: Modifier, action: () -> Unit) = Surface(modifier.height(112.dp).noRippleClick(action), shape = RoundedCornerShape(18.dp), color = Card) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(icon, color = Ink, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(8.dp)); Text(label, color = Ink, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) } }

@Composable
private fun LiveCard(action: () -> Unit) = Surface(Modifier.fillMaxWidth().height(240.dp).noRippleClick(action), shape = RoundedCornerShape(20.dp), color = Color(0xFF11141D)) { Column(Modifier.padding(26.dp)) { Surface(shape = RoundedCornerShape(9.dp), color = Color(0xFFFF3A42)) { Text(" ● LIVE ", color = Color.White, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 5.dp)) }; Spacer(Modifier.height(38.dp)); Text("FFML South Asia", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Text("Grand Finals", color = Color.White, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.weight(1f)); Text("●  Free Fire    |    ◉  12.4K watching", color = Color.White) } }

@Composable
private fun UpcomingMatch(action: () -> Unit) = Surface(Modifier.fillMaxWidth().height(138.dp).noRippleClick(action), shape = RoundedCornerShape(20.dp), color = Card, border = BorderStroke(1.dp, Outline)) { Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) { TeamMark("GL", "GodLike"); Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Today, 6:00 PM", color = MutedInk, style = MaterialTheme.typography.labelMedium); Text("VS", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Surface(shape = RoundedCornerShape(12.dp), color = Card) { Text("BO6", modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp), color = MutedInk) } }; TeamMark("RNT", "Revenant") } }

@Composable private fun TeamMark(mark: String, name: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) { Surface(Modifier.size(50.dp), shape = CircleShape, color = if (mark == "GL") Gold else Color(0xFFE94343)) { Box(contentAlignment = Alignment.Center) { Text(mark, color = Color.White, fontWeight = FontWeight.ExtraBold) } }; Spacer(Modifier.height(8.dp)); Text(name, color = Ink, fontWeight = FontWeight.Bold) }

@Composable
private fun TeamTile(mark: String, name: String, modifier: Modifier, action: () -> Unit) = Surface(modifier.height(124.dp).noRippleClick(action), shape = RoundedCornerShape(17.dp), color = Card) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Surface(Modifier.size(48.dp), shape = CircleShape, color = if (mark == "V") Color(0xFFEDDE20) else Ink) { Box(contentAlignment = Alignment.Center) { Text(mark, color = Color.White, fontWeight = FontWeight.ExtraBold) } }; Spacer(Modifier.height(8.dp)); Text(name, color = Ink, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) } }

@Composable
private fun MatchCard(event: String, first: String, versus: String, second: String, time: String) = Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Outline), color = Card) {
    Column(Modifier.padding(16.dp)) { Text(event.uppercase(), color = Blue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(9.dp)); Text("$first  $versus  $second", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(6.dp)); Text(time, color = MutedInk, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun PlayersScreen(onBack: () -> Unit, onPlayerSelected: (PlayerConnectionRow) -> Unit) {
    val repository = remember { PlayersRepository() }
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var sortAscending by remember { mutableStateOf(true) }
    var players by remember { mutableStateOf<List<PlayerConnectionRow>>(emptyList()) }
    var totalCount by remember { mutableStateOf<Int?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var visibleCount by remember { mutableIntStateOf(10) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var prefetchedOffset by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    suspend fun loadPage(reset: Boolean) {
        if (isLoading || (!reset && !hasMore)) return
        isLoading = true
        loadError = null
        val offset = if (reset) 0 else players.size
        when (val page = repository.getPlayers(limit = 25, offset = offset)) {
            is PlayersConnectionResult.Failure -> loadError = page.message
            is PlayersConnectionResult.Success -> {
                val merged = if (reset) page.players else (players + page.players).distinctBy { it.id }
                players = merged.take(50)
                totalCount = page.totalCount?.coerceAtMost(50)
                hasMore = players.size < 50 && page.players.size == 25 && (totalCount == null || players.size < totalCount!!)
            }
        }
        isLoading = false
    }

    LaunchedEffect(reloadKey) { loadPage(reset = true) }
    LaunchedEffect(query, selectedFilter, sortAscending) { visibleCount = 10 }
    LaunchedEffect(query, players.size) {
        // A search should cover every player in the capped 50-player directory,
        // even when a matching player belongs to the second 25-row page.
        if (query.isNotBlank() && hasMore && !isLoading) loadPage(reset = false)
    }
    LaunchedEffect(players.size, hasMore) {
        // Warm the next page while this one is visible. The repository runs the
        // request off the main thread and the scroll path can then use its cache.
        if (hasMore && players.isNotEmpty() && players.size < 50 && prefetchedOffset != players.size) {
            prefetchedOffset = players.size
            repository.prefetchPlayers(limit = 25, offset = players.size)
        }
    }
    LaunchedEffect(players) {
        // Decode the first viewport plus nearby rows while the directory itself is
        // already visible. Each URL is stored once and reused by cards and profiles.
        players.take(16).mapNotNull { it.avatarUrl?.takeIf(String::isNotBlank) }.forEach { url ->
            launch { PlayerImageMemoryCache.load(url) }
        }
    }

    val filteredPlayers = players
        .filter { it.matchesPlayerFilter(selectedFilter) }
        .filter { it.searchableText().contains(query.trim(), ignoreCase = true) }
        .let { rows -> if (sortAscending) rows.sortedBy { it.directoryName() } else rows.sortedByDescending { it.directoryName() } }
    val displayedPlayers = filteredPlayers.take(visibleCount)
    val directoryTotal = totalCount ?: players.size

    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 18.dp),
        ) {
            Text("‹", color = Ink, style = MaterialTheme.typography.displaySmall, modifier = Modifier.noRippleClick(onBack))
            Text("Players", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("DISCOVER PLAYERS SHAPING THE GAME", color = MutedInk, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Card,
                border = BorderStroke(1.dp, Outline),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕", color = MutedInk, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Ink),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search players", color = MutedInk)
                            inner()
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "India", "International", "Rusher", "Support", "IGL").forEach { filter ->
                    PlayerFilterChip(filter, selected = filter == selectedFilter) { selectedFilter = filter }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val upper = minOf(visibleCount, filteredPlayers.size)
                Text(if (filteredPlayers.isEmpty()) "0 players" else "1 – $upper of $directoryTotal players", color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Sort: ${if (sortAscending) "A–Z" else "Z–A"}  ▾",
                    color = MutedInk,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.noRippleClick { sortAscending = !sortAscending },
                )
            }
            Spacer(Modifier.height(12.dp))
            when {
                isLoading && players.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading players…", color = MutedInk)
                }
                loadError != null && players.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unable to load players", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Please check your connection and try again.", color = MutedInk, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { reloadKey++ }, shape = RoundedCornerShape(14.dp)) { Text("Retry") }
                    }
                }
                displayedPlayers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (players.isEmpty()) "No players available yet." else "No players match your filters.",
                        color = MutedInk,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(displayedPlayers, key = { _, player -> player.id }) { index, player ->
                            PlayerDirectoryCard(rank = index + 1, player = player, onClick = { onPlayerSelected(player) })
                            if (index >= displayedPlayers.lastIndex - 2) {
                                LaunchedEffect(players.size, visibleCount, index) {
                                    if (visibleCount < filteredPlayers.size) {
                                        visibleCount = minOf(visibleCount + 10, filteredPlayers.size)
                                    } else if (hasMore && !isLoading) {
                                        loadPage(reset = false)
                                    }
                                }
                            }
                        }
                        if (isLoading && players.isNotEmpty()) item { Text("Loading more players…", color = MutedInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)) }
                    }
                }
            }
        }
    }
}

private fun timeOfDayGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "GOOD MORNING"
    in 12..16 -> "GOOD AFTERNOON"
    in 17..20 -> "GOOD EVENING"
    else -> "GOOD NIGHT"
}


@Composable
private fun PlayerFilterChip(label: String, selected: Boolean, onClick: () -> Unit) = Surface(
    Modifier.noRippleClick(onClick),
    shape = RoundedCornerShape(13.dp),
    color = if (selected) Ink else Card,
    border = BorderStroke(1.dp, if (selected) Gold else Outline.copy(alpha = 0.55f)),
) { Text(label, color = if (selected) Color.White else Ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)) }

private class SlantedPlayerShape(private val slant: Dp = 16.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): ComposeOutline {
        val inset = with(density) { slant.toPx() }
        return ComposeOutline.Generic(Path().apply {
            moveTo(inset, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - inset, size.height)
            lineTo(0f, size.height)
            close()
        })
    }
}

/** Shared editorial geometry for player, team and event detail surfaces. */
internal class ParallelogramShape(
    private val slant: Dp = 18.dp,
    private val reverse: Boolean = false,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): ComposeOutline {
        val inset = with(density) { slant.toPx().coerceAtMost(size.width / 3f) }
        return ComposeOutline.Generic(Path().apply {
            if (reverse) {
                moveTo(0f, 0f)
                lineTo(size.width - inset, 0f)
                lineTo(size.width, size.height)
                lineTo(inset, size.height)
            } else {
                moveTo(inset, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width - inset, size.height)
                lineTo(0f, size.height)
            }
            close()
        })
    }
}

@Composable
private fun PlayerDirectoryCard(rank: Int, player: PlayerConnectionRow, onClick: () -> Unit) {
    val name = player.directoryName()
    val secondary = listOfNotNull(player.displayName, player.realName).firstOrNull { it != name }
    val location = listOf(player.country?.countryFlag().orEmpty(), player.region.orEmpty())
        .filter { it.isNotBlank() }
        .joinToString("  |  ")
    val teamName = player.teamName ?: "Free agent"
    val cardShape = SlantedPlayerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(cardShape)
            .noRippleClick(onClick),
    ) {
        PlayerCardWaveBackdrop()
        Row(
            Modifier.fillMaxSize().padding(start = 11.dp, end = 9.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("#${rank.toString().padStart(2, '0')}", color = MutedInk, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(32.dp))
            Box(Modifier.size(width = 48.dp, height = 58.dp).clip(SlantedPlayerShape(12.dp))) {
                if (!player.avatarUrl.isNullOrBlank()) {
                    RemotePlayerImage(player.avatarUrl, Modifier.fillMaxSize(), name)
                } else {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(brush = Brush.linearGradient(listOf(player.avatarColor(), player.avatarColor().copy(alpha = 0.72f), Color(0xFF10131B))))
                        drawCircle(Color.White.copy(alpha = 0.07f), radius = size.minDimension * 0.44f, center = Offset(size.width * 0.68f, size.height * 0.3f))
                        var stripe = -size.height
                        while (stripe < size.width + size.height) {
                            drawLine(Color.White.copy(alpha = 0.045f), Offset(stripe, size.height), Offset(stripe + size.height, 0f), 1.dp.toPx())
                            stripe += 12.dp.toPx()
                        }
                    }
                    Text(name.take(1).uppercase(), color = Color.White.copy(alpha = 0.94f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(name, color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                secondary?.let { Text(it, color = MutedInk, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                if (location.isNotBlank()) Text(location, color = MutedInk, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Spacer(Modifier.width(4.dp))
            Box(Modifier.width(1.dp).height(40.dp).background(Outline.copy(alpha = 0.72f)))
            Spacer(Modifier.width(7.dp))
            if (player.teamName.isNullOrBlank()) {
                Text("No Team", color = MutedInk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(122.dp))
            } else Row(Modifier.width(122.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(26.dp), shape = CircleShape, color = SoftBlue) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(teamName.take(2).uppercase(), color = Blue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    teamName,
                    color = Ink.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.width(1.dp))
            Text("›", color = MutedInk, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(7.dp))
        }
    }
}

@Composable
internal fun RemotePlayerImage(url: String, modifier: Modifier, description: String) {
    var bitmap by remember(url) { mutableStateOf(PlayerImageMemoryCache.peek(url)) }
    LaunchedEffect(url) {
        bitmap = PlayerImageMemoryCache.load(url)
    }
    Box(modifier.background(Color(0xFF171C27))) {
        bitmap?.let { Image(bitmap = it, contentDescription = description, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

/** Shared URL-image cache: directory cards and the opened profile reuse one bitmap. */
internal object PlayerImageMemoryCache {
    private val images = ConcurrentHashMap<String, androidx.compose.ui.graphics.ImageBitmap>()

    fun peek(url: String): androidx.compose.ui.graphics.ImageBitmap? = images[url]

    suspend fun load(url: String): androidx.compose.ui.graphics.ImageBitmap? {
        images[url]?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                connection.getInputStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
        if (loaded != null) images.putIfAbsent(url, loaded)
        return images[url] ?: loaded
    }
}

@Composable
private fun PlayerCardWaveBackdrop() {
    val card = Card
    val softBlue = SoftBlue
    Canvas(Modifier.fillMaxSize()) {
    drawRect(brush = Brush.linearGradient(listOf(card.copy(alpha = 0.96f), Color.White.copy(alpha = 0.92f), card.copy(alpha = 0.9f))))
    val lowerWave = Path().apply {
        moveTo(0f, size.height * 0.73f)
        cubicTo(size.width * 0.26f, size.height * 0.48f, size.width * 0.58f, size.height * 1.05f, size.width, size.height * 0.60f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(lowerWave, brush = Brush.linearGradient(listOf(softBlue.copy(alpha = 0.14f), Color.White.copy(alpha = 0.58f), softBlue.copy(alpha = 0.10f))))
    val highlightWave = Path().apply {
        moveTo(0f, size.height * 0.20f)
        cubicTo(size.width * 0.32f, size.height * 0.48f, size.width * 0.62f, -size.height * 0.12f, size.width, size.height * 0.22f)
        lineTo(size.width, size.height * 0.34f)
        cubicTo(size.width * 0.63f, 0f, size.width * 0.32f, size.height * 0.64f, 0f, size.height * 0.31f)
        close()
    }
    drawPath(highlightWave, color = Color.White.copy(alpha = 0.44f))
    }
}

@Composable
private fun PlayerDetailsScreen(player: PlayerConnectionRow, onBack: () -> Unit) {
    val profile = PlayerProfileUi.from(player)
    var selectedTab by remember { mutableStateOf(PlayerProfileTab.Overview) }

    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Box(Modifier.fillMaxSize()) {
            PlayerProfileDepthBackground()
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 26.dp),
            ) {
                PlayerProfileTopBar(onBack)
                PlayerHero(profile)
                Column(Modifier.padding(horizontal = 18.dp)) {
                    PlayerIdentityStrip(profile)
                    Spacer(Modifier.height(14.dp))
                    PlayerPerformanceBar(profile)
                    Spacer(Modifier.height(24.dp))
                    PlayerProfileTabs(selectedTab) { selectedTab = it }
                    Spacer(Modifier.height(20.dp))
                    when (selectedTab) {
                        PlayerProfileTab.Overview -> PlayerOverview(profile)
                        PlayerProfileTab.Matches -> PlayerMatches(profile)
                        PlayerProfileTab.Achievements -> PlayerAchievements(profile)
                        PlayerProfileTab.Socials -> PlayerSocials(profile)
                    }
                    Spacer(Modifier.height(26.dp))
                }
                EsportSphereProfileFooter()
            }
        }
    }
}

private enum class PlayerProfileTab(val label: String) {
    Overview("OVERVIEW"), Matches("MATCH HISTORY"), Achievements("ACHIEVEMENTS"), Socials("SOCIALS")
}

/** UI model deliberately separated from the small directory row, ready for profile_details data later. */
private data class PlayerProfileUi(
    val id: String,
    val ign: String,
    val realName: String,
    val country: String,
    val region: String,
    val avatarUrl: String?,
    val teamName: String?,
    val role: String,
    val status: String,
    val matches: String,
    val averageKills: String,
    val booyahRate: String,
    val totalDamage: String,
    val bio: String,
    val quote: String,
    val career: List<PlayerCareerUi>,
    val achievements: List<PlayerAchievementUi>,
    val socials: List<PlayerSocialUi>,
) {
    companion object {
        fun from(player: PlayerConnectionRow): PlayerProfileUi {
            val ign = player.directoryName()
            val team = player.teamName
            return PlayerProfileUi(
                id = player.id,
                ign = ign,
                realName = player.realName ?: player.displayName ?: "EsportSphere Player",
                country = player.country ?: "Country unavailable",
                region = player.region ?: "Region unavailable",
                avatarUrl = player.avatarUrl,
                teamName = team,
                role = player.role ?: "PLAYER",
                status = "ACTIVE",
                matches = "1,248",
                averageKills = "3.8",
                booyahRate = "18.4%",
                totalDamage = "5.2M",
                bio = player.bio ?: "$ign is an EsportSphere competitor focused on discipline, strategic growth and consistent performance.",
                quote = "It’s not just about winning,\nit’s about building something bigger.",
                career = listOf(
                    PlayerCareerUi("2024", team ?: "EsportSphere", player.role ?: "PLAYER"),
                    PlayerCareerUi("2023", "EsportSphere", "COMPETITOR"),
                    PlayerCareerUi("2022", "Competitive Free Fire", "DEVELOPMENT")
                ),
                achievements = listOf(
                    PlayerAchievementUi("FFMAI 2025", "MAIN EVENT QUALIFIER", "TOP 12", true),
                    PlayerAchievementUi("APL INFERNO LEAGUE", "FINALISTS", "TOP 2", false),
                    PlayerAchievementUi("THUNDERSTRIKE ARENA", "COMPETITOR", "TOP 8", false)
                ),
                socials = listOf(
                    PlayerSocialUi("YouTube", ign),
                    PlayerSocialUi("Instagram", "@${ign.lowercase()}"),
                    PlayerSocialUi("X", "@${ign.lowercase()}ffs"),
                    PlayerSocialUi("Discord", "${ign.lowercase()}.exe")
                )
            )
        }
    }
}

private data class PlayerCareerUi(val year: String, val teamName: String, val role: String)
private data class PlayerAchievementUi(val title: String, val subtitle: String, val result: String, val isGold: Boolean)
private data class PlayerSocialUi(val network: String, val handle: String)
private data class PlayerMatchUi(val map: String, val placement: String, val kills: String, val damage: String)

@Composable
private fun PlayerProfileDepthBackground() {
    Image(
        painter = painterResource(R.drawable.player_profile_depth),
        contentDescription = null,
        modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.62f),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun PlayerProfileTopBar(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, top = 18.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("‹", color = Ink, fontSize = 42.sp, lineHeight = 42.sp, modifier = Modifier.noRippleClick(onBack))
        Spacer(Modifier.width(10.dp))
        Text("PLAYERS", color = Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Text("•••", color = Ink, style = MaterialTheme.typography.titleLarge, letterSpacing = 2.sp)
    }
}

@Composable
private fun PlayerHero(profile: PlayerProfileUi) {
    val compact = LocalConfiguration.current.screenWidthDp.dp < 600.dp
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
    if (compact) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerHeroImage(profile, Modifier.width(132.dp).height(258.dp))
                Spacer(Modifier.width(16.dp))
                PlayerHeroInformation(profile, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerHeroImage(profile, Modifier.width(230.dp).height(370.dp))
            Spacer(Modifier.width(34.dp))
            PlayerHeroInformation(profile, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
    }
}
}

@Composable
private fun PlayerHeroImage(profile: PlayerProfileUi, modifier: Modifier) {
    val shape = ParallelogramShape(26.dp)
    Box(modifier.clip(shape)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(brush = Brush.linearGradient(listOf(profileColor(profile.id), Color(0xFF0D1119))))
            drawCircle(Color(0xFFFFD36A).copy(alpha = 0.18f), radius = size.minDimension * 0.72f, center = Offset(size.width * 0.95f, size.height * 0.18f))
        }
        if (!profile.avatarUrl.isNullOrBlank()) RemotePlayerImage(profile.avatarUrl, Modifier.fillMaxSize(), profile.ign)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.43f)))))
        Text("#07", color = Gold, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light, modifier = Modifier.padding(16.dp).align(Alignment.TopStart))
        Text(
            profile.ign.uppercase(), color = Color.White.copy(alpha = 0.50f), style = MaterialTheme.typography.labelSmall,
            letterSpacing = 3.sp, modifier = Modifier.align(Alignment.CenterStart).graphicsLayer(rotationZ = -90f),
        )
        Text("PLAY\nCOMPETE\nEVOLVE", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall, lineHeight = 18.sp, letterSpacing = 2.sp, modifier = Modifier.padding(16.dp).align(Alignment.BottomStart))
    }
}

@Composable
private fun PlayerHeroInformation(profile: PlayerProfileUi, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        Text("DISCIPLINE BUILDS", color = MutedInk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("FREEDOM", color = MutedInk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Spacer(Modifier.height(11.dp))
        Text(profile.ign.uppercase(), color = Ink, fontSize = 35.sp, lineHeight = 37.sp, fontWeight = FontWeight.Black, maxLines = 2)
        Spacer(Modifier.height(3.dp))
        Text(profile.realName.uppercase(), color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, maxLines = 2)
        Spacer(Modifier.height(13.dp))
        Text("${profile.country.countryFlag()}  ${profile.country.uppercase()}  |  ${profile.region.uppercase()}", color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 2)
        Spacer(Modifier.height(17.dp))
        Text("Same Game.\nDifferent Mindset.", color = MutedInk, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.width(44.dp).height(2.dp).background(Gold))
    }
}

@Composable
private fun PlayerIdentityStrip(profile: PlayerProfileUi) {
    val shape = ParallelogramShape(14.dp)
    Surface(Modifier.fillMaxWidth(), shape = shape, color = Card.copy(alpha = 0.76f), border = BorderStroke(1.dp, Outline.copy(alpha = 0.58f)), shadowElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            IdentityCell("◈", profile.teamName ?: "NO TEAM", "TEAM", Modifier.weight(1.35f))
            IdentityDivider()
            IdentityCell("◇", profile.role.uppercase(), "ROLE", Modifier.weight(0.8f))
            IdentityDivider()
            IdentityCell("●", profile.status, "STATUS", Modifier.weight(0.8f), isStatus = true)
        }
    }
}

@Composable
private fun IdentityCell(icon: String, value: String, label: String, modifier: Modifier, isStatus: Boolean = false) = Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Text(icon, color = if (isStatus) Gold else Ink, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.width(8.dp))
    Column {
        Text(value, color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = MutedInk, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
    }
}

@Composable
private fun IdentityDivider() = Box(Modifier.width(1.dp).height(32.dp).background(Outline.copy(alpha = 0.8f)).padding(horizontal = 2.dp))

@Composable
private fun PlayerPerformanceBar(profile: PlayerProfileUi) {
    val shape = ParallelogramShape(18.dp)
    Surface(Modifier.fillMaxWidth(), shape = shape, color = Color(0xFF141925), shadowElevation = 7.dp) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            PerformanceCell(profile.matches, "MATCHES", Modifier.weight(1f))
            PerformanceDivider()
            PerformanceCell(profile.averageKills, "AVG KILLS", Modifier.weight(1f))
            PerformanceDivider()
            PerformanceCell(profile.booyahRate, "BOOYAH RATE", Modifier.weight(1f))
            PerformanceDivider()
            PerformanceCell(profile.totalDamage, "TOTAL DAMAGE", Modifier.weight(1f))
        }
    }
}

@Composable
private fun PerformanceCell(value: String, label: String, modifier: Modifier) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
    Spacer(Modifier.height(2.dp))
    Text(label, color = Color.White.copy(alpha = 0.64f), style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp, textAlign = TextAlign.Center, maxLines = 1)
}

@Composable
private fun PerformanceDivider() = Box(Modifier.width(1.dp).height(35.dp).background(Gold.copy(alpha = 0.40f)))

@Composable
private fun PlayerProfileTabs(selected: PlayerProfileTab, onSelected: (PlayerProfileTab) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        PlayerProfileTab.entries.forEach { tab ->
            Column(Modifier.noRippleClick { onSelected(tab) }) {
                Text(tab.label, color = if (tab == selected) Ink else MutedInk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(if (tab == selected) 38.dp else 0.dp).height(2.dp).background(if (tab == selected) Gold else Color.Transparent))
            }
        }
    }
}

@Composable
private fun PlayerOverview(profile: PlayerProfileUi) {
    val wideLayout = LocalConfiguration.current.screenWidthDp.dp >= 650.dp
    Box(Modifier.fillMaxWidth()) {
    if (wideLayout) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AboutPanel(profile, Modifier.weight(1f))
            CareerTimeline(profile, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AchievementPanel(profile, Modifier.weight(1f))
            SocialPanel(profile, Modifier.weight(1f))
        }
    } else {
        Column {
            AboutPanel(profile)
            Spacer(Modifier.height(16.dp))
            CareerTimeline(profile)
            Spacer(Modifier.height(16.dp))
            AchievementPanel(profile)
            Spacer(Modifier.height(16.dp))
            SocialPanel(profile)
        }
    }
}
}

@Composable
private fun AngularPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = ParallelogramShape(15.dp), color = Card.copy(alpha = 0.79f), border = BorderStroke(1.dp, Outline.copy(alpha = 0.52f)), shadowElevation = 2.dp) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun SlantedSectionHeader(title: String) = Surface(shape = ParallelogramShape(14.dp), color = Color(0xFF151923)) {
    Text(title, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 17.dp, vertical = 8.dp))
}

@Composable
private fun AboutPanel(profile: PlayerProfileUi, modifier: Modifier = Modifier) = AngularPanel(modifier.fillMaxWidth()) {
    SlantedSectionHeader("ABOUT")
    Spacer(Modifier.height(17.dp))
    Text(profile.bio, color = Ink, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
    Spacer(Modifier.height(20.dp))
    Text("“", color = Gold, fontSize = 48.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black)
    Text(profile.quote, color = Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, lineHeight = 21.sp)
    Spacer(Modifier.height(8.dp))
    Text("— ${profile.ign.uppercase()}", color = MutedInk, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
}

@Composable
private fun CareerTimeline(profile: PlayerProfileUi, modifier: Modifier = Modifier) = AngularPanel(modifier.fillMaxWidth()) {
    SlantedSectionHeader("CAREER TIMELINE")
    Spacer(Modifier.height(18.dp))
    profile.career.forEachIndexed { index, entry ->
        Row(verticalAlignment = Alignment.Top) {
            Text(entry.year, color = MutedInk, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(44.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (index == 0) Gold else Ink.copy(alpha = 0.55f)))
                if (index != profile.career.lastIndex) Box(Modifier.width(1.dp).height(34.dp).background(Outline))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.padding(bottom = if (index == profile.career.lastIndex) 0.dp else 12.dp)) {
                Text(entry.teamName.uppercase(), color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.role, color = MutedInk, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun AchievementPanel(profile: PlayerProfileUi, modifier: Modifier = Modifier) = AngularPanel(modifier.fillMaxWidth()) {
    SlantedSectionHeader("ACHIEVEMENTS")
    Spacer(Modifier.height(14.dp))
    profile.achievements.forEachIndexed { index, achievement ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (achievement.isGold) "♜" else "◈", color = if (achievement.isGold) Gold else MutedInk, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(34.dp))
            Column(Modifier.weight(1f)) {
                Text(achievement.title, color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(achievement.subtitle, color = MutedInk, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Text(achievement.result, color = if (achievement.isGold) Gold else Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        }
        if (index != profile.achievements.lastIndex) Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(Outline.copy(alpha = 0.65f)))
    }
}

@Composable
private fun SocialPanel(profile: PlayerProfileUi, modifier: Modifier = Modifier) = AngularPanel(modifier.fillMaxWidth()) {
    SlantedSectionHeader("SOCIALS")
    Spacer(Modifier.height(14.dp))
    profile.socials.forEachIndexed { index, social ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(social.network.take(1), color = Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.width(34.dp))
            Text(social.network, color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(88.dp))
            Text(social.handle, color = MutedInk, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (index != profile.socials.lastIndex) Box(Modifier.fillMaxWidth().padding(vertical = 11.dp).height(1.dp).background(Outline.copy(alpha = 0.65f)))
    }
}

@Composable
private fun PlayerStatsDetail(profile: PlayerProfileUi) = AngularPanel(Modifier.fillMaxWidth()) {
    SlantedSectionHeader("PERFORMANCE")
    Spacer(Modifier.height(18.dp))
    listOf("Matches" to profile.matches, "Average kills" to profile.averageKills, "Booyah rate" to profile.booyahRate, "Total damage" to profile.totalDamage).forEach { (label, value) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), color = MutedInk, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
            Text(value, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PlayerMatches(profile: PlayerProfileUi) {
    val matches = listOf(PlayerMatchUi("BERMUDA", "#1", "8", "3,421"), PlayerMatchUi("PURGATORY", "#3", "6", "2,914"), PlayerMatchUi("NEXTERRA", "#5", "4", "1,982"))
    AngularPanel(Modifier.fillMaxWidth()) {
        SlantedSectionHeader("RECENT MATCHES")
        Spacer(Modifier.height(15.dp))
        matches.forEachIndexed { index, match ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(match.map, color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold); Text("BR · SQUAD", color = MutedInk, style = MaterialTheme.typography.labelSmall) }
                Text(match.placement, color = Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, modifier = Modifier.width(38.dp))
                Text("${match.kills} KILLS", color = MutedInk, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(58.dp))
                Text(match.damage, color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
            }
            if (index != matches.lastIndex) Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(Outline.copy(alpha = 0.65f)))
        }
    }
}

@Composable
private fun PlayerAchievements(profile: PlayerProfileUi) = AchievementPanel(profile, Modifier.fillMaxWidth())

@Composable
private fun PlayerSocials(profile: PlayerProfileUi) = SocialPanel(profile, Modifier.fillMaxWidth())

@Composable
private fun EsportSphereProfileFooter() {
    Surface(Modifier.fillMaxWidth(), shape = ParallelogramShape(28.dp, reverse = true), color = Color(0xFF121721)) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.esportsphere_symbol), contentDescription = "EsportSphere", modifier = Modifier.size(34.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("ESPORTSPHERE", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("PLAYERS SHAPE TOMORROW", color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            }
            Text("PEOPLE\nPLAY\nPROGRESS", color = Gold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, lineHeight = 16.sp, letterSpacing = 1.sp)
        }
    }
}

private fun profileColor(id: String): Color = listOf(Color(0xFF2D435E), Color(0xFF5C3037), Color(0xFF3D315A), Color(0xFF264B46))[Math.floorMod(id.hashCode(), 4)]

private fun PlayerConnectionRow.directoryName(): String = ign ?: displayName ?: realName ?: "Player"
private fun PlayerConnectionRow.searchableText(): String = listOfNotNull(id, ign, displayName, realName, country, region, role, teamName).joinToString(" ")
private fun PlayerConnectionRow.matchesPlayerFilter(filter: String): Boolean = when (filter) {
    "All" -> true
    "India" -> country.orEmpty().contains("india", true) || country.orEmpty().contains("ind", true)
    "International" -> country?.let { !it.contains("india", true) && !it.contains("ind", true) } ?: false
    else -> role.orEmpty().contains(filter, true)
}
private fun String.countryFlag(): String = when {
    contains("india", true) || contains("ind", true) -> "🇮🇳"
    contains("bangladesh", true) || contains("ban", true) -> "🇧🇩"
    contains("nepal", true) || contains("nep", true) -> "🇳🇵"
    else -> ""
}
private fun PlayerConnectionRow.avatarColor(): Color = listOf(Color(0xFF243C68), Color(0xFF6D2E45), Color(0xFF463368), Color(0xFF314C3A))[Math.floorMod(id.hashCode(), 4)]

@Composable
private fun AskSphereScreen() {
    var question by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 112.dp),
    ) {
        Text("Ask Sphere", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("Your esports AI assistant", color = MutedInk)
        Spacer(Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = SoftBlue, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("✦  Sphere is ready", color = Blue, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(8.dp)); Text("Ask about teams, player records, tournament history, match results, or what to watch next.", color = Ink) } }
        Spacer(Modifier.height(18.dp))
        listOf("Who is the best Free Fire team?", "How much has Team Falcons won?", "What matches are live tonight?").forEach { suggestion -> Text("•  $suggestion", color = Blue, modifier = Modifier.padding(vertical = 8.dp).noRippleClick { question = suggestion }) }
        Spacer(Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Outline), color = Card, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { BasicTextField(value = question, onValueChange = { question = it }, textStyle = androidx.compose.ui.text.TextStyle(color = Ink), modifier = Modifier.weight(1f), decorationBox = { inner -> if (question.isEmpty()) Text("Ask anything about esports…", color = MutedInk); inner() }); Text("🎙", modifier = Modifier.noRippleClick { }) } }
    }
}


@Composable
private fun ProfileScreen(navigate: (Screen) -> Unit) = Column(Modifier.fillMaxSize().padding(24.dp)) {
    Text("Profile", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(20.dp)); Surface(shape = RoundedCornerShape(20.dp), color = Card, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(48.dp), shape = CircleShape, color = Blue) {}; Spacer(Modifier.width(14.dp)); Column { Text("Esport Player", color = Ink, fontWeight = FontWeight.Bold); Text("Level up your esports profile", color = MutedInk, style = MaterialTheme.typography.bodySmall) } } }
    Spacer(Modifier.height(20.dp)); SettingsRow("Your performance") { navigate(Screen.Performance) }; SettingsRow("Notifications") {}; SettingsRow("Settings") {}
}

@Composable
private fun SettingsScreen(
    isLoggedIn: Boolean,
    avatarPreset: Int,
    uploadedAvatarUri: String?,
    onAvatarChanged: (Int, String?) -> Unit,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onLogOut: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val accountPreferences = remember(context) { context.getSharedPreferences(AppPreferencesName, 0) }
    val accountName = accountPreferences.getString(PrefAccountName, "Esport Player").orEmpty()
    val accountEmail = accountPreferences.getString(PrefAccountEmail, "").orEmpty()
    val accountId = accountPreferences.getString(PrefAccountId, null)
    val scope = rememberCoroutineScope()
    fun showMessage(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !accountId.isNullOrBlank()) {
            scope.launch {
                val savedUri = persistAvatarInSharedMedia(context.applicationContext, uri, accountId)
                if (savedUri != null) onAvatarChanged(avatarPreset, savedUri.toString())
                else showMessage("Unable to save this profile photo. Please try another image.")
            }
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Text("‹", color = Ink, style = MaterialTheme.typography.displaySmall, modifier = Modifier.noRippleClick(onBack))
        Text("Settings", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Manage your EsportSphere experience", color = MutedInk, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(22.dp))
        Surface(Modifier.fillMaxWidth().noRippleClick { if (isLoggedIn) onOpenAccount() else onSignIn() }, shape = RoundedCornerShape(22.dp), color = Card) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarPreview(avatarPreset, uploadedAvatarUri?.let(Uri::parse), Modifier.size(74.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isLoggedIn) accountName.ifBlank { "Esport Player" } else "Join EsportSphere", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(if (isLoggedIn) accountEmail else "Sign in to personalise your experience", color = MutedInk)
                    Text(if (isLoggedIn) "“Play. Compete. Belong.”" else "Tap here to sign in", color = MutedInk)
                }
                Text("›", color = MutedInk, style = MaterialTheme.typography.headlineLarge)
            }
        }
        Spacer(Modifier.height(28.dp))
        if (isLoggedIn) {
            SettingsHeading("PROFILE PHOTO")
            Text("Choose an avatar or save a circular photo on this device.", color = MutedInk, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { index ->
                    Surface(
                        Modifier.size(62.dp).noRippleClick { onAvatarChanged(index, null) },
                        shape = CircleShape,
                        color = avatarColour(index),
                        border = BorderStroke(if (avatarPreset == index && uploadedAvatarUri == null) 3.dp else 1.dp, if (avatarPreset == index && uploadedAvatarUri == null) Blue else Color(0xFFE2E4EA)),
                    ) { Box(contentAlignment = Alignment.Center) { Text(listOf("P", "GG", "XP")[index], color = Color.White, fontWeight = FontWeight.ExtraBold) } }
                }
                Surface(Modifier.size(62.dp).noRippleClick { imagePicker.launch("image/*") }, shape = CircleShape, color = SoftBlue, border = BorderStroke(1.dp, Blue)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Blue, style = MaterialTheme.typography.headlineMedium) } }
            }
            Spacer(Modifier.height(28.dp))
        } else {
            Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(16.dp)) { Text("Sign in", fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(28.dp))
        }
        SettingsHeading("ACCOUNT")
        SettingsCard("◯", "Account", if (isLoggedIn) "Manage your name, email, phone and password" else "Sign in to manage your account", action = { if (isLoggedIn) onOpenAccount() else onSignIn() }) { Text("›", color = MutedInk, style = MaterialTheme.typography.headlineLarge) }
        Spacer(Modifier.height(24.dp))
        SettingsHeading("APP")
        SettingsCard("☾", "Preferences", "Theme, language and your esports follows", action = onOpenPreferences) { Text("›", color = MutedInk, style = MaterialTheme.typography.headlineLarge) }
        SettingsCard("♧", "Notifications", "Manage match, news and team notifications", action = onOpenNotifications) { Text("›", color = MutedInk, style = MaterialTheme.typography.headlineLarge) }
        Spacer(Modifier.height(24.dp))
        SettingsHeading("PRIVACY")
        SettingsCard("◌", "Privacy & Security", "Control your privacy and account security", action = { showMessage("Privacy controls are coming next.") }) { Text("›", color = MutedInk, style = MaterialTheme.typography.headlineLarge) }
        Spacer(Modifier.height(24.dp))
        SettingsHeading("DATA")
        SettingsCard("◫", "Data & App", "Storage, cache and app data", action = { showMessage("Data & App controls are coming next.") }) { Text("›", color = MutedInk, style = MaterialTheme.typography.headlineLarge) }
        Spacer(Modifier.height(24.dp))
        SettingsHeading("SUPPORT")
        SettingsCard("?", "Help & About", "Get support, give feedback or raise a ticket", action = { showMessage("Help Centre is coming next.") }) { Text("›", color = MutedInk, style = MaterialTheme.typography.headlineLarge) }
        Spacer(Modifier.height(22.dp))
        if (isLoggedIn) Surface(Modifier.fillMaxWidth().height(52.dp).noRippleClick(onLogOut), shape = RoundedCornerShape(16.dp), color = Color(0xFFFFEFEF)) { Box(contentAlignment = Alignment.Center) { Text("⇥  Log Out", color = Color(0xFFE94444), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) } }
        Spacer(Modifier.height(22.dp))
        Text(
            "EsportSphere • Version $AppVersion",
            color = MutedInk,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AccountScreen(onBack: () -> Unit, onSignedOut: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences(AppPreferencesName, 0) }
    var name by remember { mutableStateOf(preferences.getString(PrefAccountName, "Esport Player").orEmpty()) }
    var email by remember { mutableStateOf(preferences.getString(PrefAccountEmail, "").orEmpty()) }
    var phone by remember { mutableStateOf(preferences.getString(PrefAccountPhone, "").orEmpty()) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val originalEmail = preferences.getString(PrefAccountEmail, "").orEmpty()
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(22.dp)) {
            Text("‹", color = Ink, style = MaterialTheme.typography.displaySmall, modifier = Modifier.noRippleClick(onBack))
            Text("Account", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Manage your EsportSphere account details", color = MutedInk, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(26.dp))
            SettingsHeadingForPage("PROFILE DETAILS", MutedInk)
            Spacer(Modifier.height(10.dp))
            Text("Full name", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); AuthField("Your name", name) { name = it }
            Spacer(Modifier.height(16.dp)); Text("Email address", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); AuthField("name@example.com", email) { email = it }
            Spacer(Modifier.height(16.dp)); Text("Mobile number", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); PhoneNumberField(phone) { phone = it }
            Spacer(Modifier.height(26.dp))
            SettingsHeadingForPage("SECURITY", MutedInk)
            Spacer(Modifier.height(10.dp))
            Text("New password", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); PasswordField(password, passwordVisible, { password = it }, { passwordVisible = !passwordVisible }, "New password (optional)")
            Spacer(Modifier.height(6.dp)); Text("Leave blank to keep your current password. New passwords need at least 6 characters.", color = MutedInk, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = {
                    val token = preferences.getString(PrefAccessToken, null)
                    val userId = preferences.getString(PrefAccountId, null)
                    if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                        Toast.makeText(context, "Please sign in again before updating your account.", Toast.LENGTH_LONG).show()
                    } else {
                        scope.launch {
                            isSaving = true
                            val emailToUpdate = email.takeIf { it != originalEmail }
                            val result = supabaseUpdateUser(token, name.trim(), phone.trim(), emailToUpdate, password.takeIf { it.isNotBlank() })
                            if (result.success) {
                                val profileResult = supabaseUpsertProfile(token, userId, name.trim(), phone.trim())
                                if (profileResult.success) {
                                    preferences.edit().putString(PrefAccountName, name.trim()).putString(PrefAccountPhone, phone.trim()).apply()
                                    if (emailToUpdate == null) preferences.edit().putString(PrefAccountEmail, email.trim()).apply()
                                    password = ""
                                    Toast.makeText(context, if (emailToUpdate == null) "Account details have been updated." else "Account details saved. Confirm the email change from your inbox.", Toast.LENGTH_LONG).show()
                                } else Toast.makeText(context, "Auth updated, but profile database update failed: ${profileResult.message}", Toast.LENGTH_LONG).show()
                            } else Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            isSaving = false
                        }
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank() && phone.length == 10 && (password.isBlank() || password.length >= 6) && !isSaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                shape = RoundedCornerShape(16.dp),
            ) { Text(if (isSaving) "Saving…" else "Save account details", fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(30.dp))
            SettingsHeadingForPage("DANGER ZONE", Color(0xFFE94444))
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth().noRippleClick {
                val token = preferences.getString(PrefAccessToken, null)
                if (token.isNullOrBlank() || isDeleting) Toast.makeText(context, "Please sign in again before deleting your account.", Toast.LENGTH_LONG).show()
                else scope.launch {
                    isDeleting = true
                    val result = supabaseDeleteAccount(token)
                    isDeleting = false
                    if (result.success) { Toast.makeText(context, "Account deleted.", Toast.LENGTH_LONG).show(); onSignedOut() }
                    else Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }, shape = RoundedCornerShape(16.dp), color = Color(0xFFFFEFEF)) {
                Column(Modifier.padding(18.dp)) {
                    Text(if (isDeleting) "Deleting account…" else "Delete account", color = Color(0xFFE94444), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Permanently remove your account and its data", color = MutedInk, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun AvatarPreview(selectedAvatar: Int, uploadedAvatar: Uri?, modifier: Modifier) {
    if (uploadedAvatar != null) {
        AndroidView(
            factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
            update = { it.setImageURI(uploadedAvatar) },
            modifier = modifier.graphicsLayer { shape = CircleShape; clip = true },
        )
    } else {
        Surface(modifier, shape = CircleShape, color = avatarColour(selectedAvatar)) { Box(contentAlignment = Alignment.Center) { Text(listOf("P", "GG", "XP")[selectedAvatar], color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) } }
    }
}

private enum class ThemeChoice { Light, Dark, Device }

internal const val AppPreferencesName = "esportsphere_preferences"
private const val PrefSignedIn = "signed_in"
private const val PrefOnboardingComplete = "onboarding_complete"
private const val PrefTheme = "theme_choice"
private const val PrefAvatarPreset = "avatar_preset"
private const val PrefAvatarUri = "avatar_uri"
internal const val PrefAccessToken = "supabase_access_token"
private const val PrefRefreshToken = "supabase_refresh_token"
internal const val PrefAccountId = "account_id"
private const val PrefAccountName = "account_name"
private const val PrefAccountEmail = "account_email"
private const val PrefAccountPhone = "account_phone"
private const val PrefFollowingTeams = "following_teams"
private const val PrefFollowingPlayers = "following_players"
private const val PrefFollowingEvents = "following_events"
internal const val PrefMatchReminders = "match_reminders"
internal const val PrefTournamentUpdates = "tournament_updates"
internal const val PrefTeamPlayerUpdates = "team_player_updates"
internal const val PrefNewsNotifications = "news_notifications"
internal const val PrefFcmToken = "fcm_registration_token"

private fun android.content.SharedPreferences.readThemeChoice(): ThemeChoice = runCatching {
    ThemeChoice.valueOf(getString(PrefTheme, ThemeChoice.Device.name) ?: ThemeChoice.Device.name)
}.getOrDefault(ThemeChoice.Device)

private fun android.content.SharedPreferences.readStringList(key: String, default: List<String>): List<String> =
    getString(key, default.joinToString("|"))?.split("|")?.filter(String::isNotBlank) ?: default

private fun android.content.SharedPreferences.saveStringList(key: String, values: List<String>) {
    edit().putString(key, values.joinToString("|")).apply()
}

private data class SupabaseAuthResult(
    val success: Boolean,
    val message: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

internal const val SupabaseProjectUrl = "https://lbvlqrkqzhzvnmyfvrht.supabase.co"
internal const val SupabasePublishableKey = "sb_publishable_ZjIEmJf--6B_wIBvCFMprg_mfqqNy7Z"

private suspend fun supabaseSignIn(email: String, password: String): SupabaseAuthResult =
    supabaseAuth("token?grant_type=password", JSONObject().put("email", email).put("password", password))

private suspend fun supabaseSignUp(name: String, phone: String, email: String, password: String): SupabaseAuthResult =
    supabaseAuth(
        "signup",
        JSONObject()
            .put("email", email)
            .put("password", password)
            .put("email_redirect_to", "esportsphere://open")
            .put("data", JSONObject().put("full_name", name).put("phone", phone)),
        detectExistingEmail = true,
    )

private suspend fun supabaseAuth(
    endpoint: String,
    body: JSONObject,
    detectExistingEmail: Boolean = false,
): SupabaseAuthResult = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL("$SupabaseProjectUrl/auth/v1/$endpoint").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", SupabasePublishableKey)
            setRequestProperty("Content-Type", "application/json")
        }
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val responseText = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        val response = JSONObject(responseText.ifBlank { "{}" })
        if (connection.responseCode !in 200..299) {
            val errorMessage = response.optString("msg", response.optString("message", "Authentication failed."))
            SupabaseAuthResult(
                success = false,
                message = if (detectExistingEmail && errorMessage.contains("already registered", ignoreCase = true)) {
                    "An account with this email already exists. Please sign in."
                } else {
                    errorMessage
                },
            )
        } else {
            val user = response.optJSONObject("user") ?: JSONObject()
            // With email confirmation enabled, GoTrue returns an obfuscated user for an
            // already-confirmed email. Its identities list is explicitly empty.
            val identities = user.optJSONArray("identities")
            if (detectExistingEmail && identities != null && identities.length() == 0) {
                return@withContext SupabaseAuthResult(
                    success = false,
                    message = "An account with this email already exists. Please sign in.",
                )
            }
            val metadata = user.optJSONObject("user_metadata") ?: JSONObject()
            SupabaseAuthResult(
                success = true,
                message = "Success",
                accessToken = response.optString("access_token").takeIf { it.isNotBlank() },
                refreshToken = response.optString("refresh_token").takeIf { it.isNotBlank() },
                userId = user.optString("id").takeIf { it.isNotBlank() },
                name = metadata.optString("full_name").takeIf { it.isNotBlank() },
                email = user.optString("email").takeIf { it.isNotBlank() },
                phone = metadata.optString("phone", user.optString("phone")).takeIf { it.isNotBlank() },
            )
        }
    }.getOrElse { SupabaseAuthResult(false, "Could not reach Supabase. Check your connection and try again.") }
}

private suspend fun supabaseUpdateUser(accessToken: String, name: String, phone: String, email: String?, password: String?): SupabaseAuthResult = withContext(Dispatchers.IO) {
    runCatching {
        val body = JSONObject().put("data", JSONObject().put("full_name", name).put("phone", phone))
        email?.takeIf { it.isNotBlank() }?.let { body.put("email", it) }
        password?.takeIf { it.isNotBlank() }?.let { body.put("password", it) }
        val connection = (URL("$SupabaseProjectUrl/auth/v1/user").openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", SupabasePublishableKey)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json")
        }
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val responseText = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        val response = JSONObject(responseText.ifBlank { "{}" })
        if (connection.responseCode in 200..299) SupabaseAuthResult(true, "Account details updated.")
        else SupabaseAuthResult(false, response.optString("msg", response.optString("message", "Could not update account details.")))
    }.getOrElse { SupabaseAuthResult(false, "Could not reach Supabase. Check your connection and try again.") }
}

private suspend fun supabaseUpsertProfile(accessToken: String, userId: String, name: String, phone: String): SupabaseAuthResult = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL("$SupabaseProjectUrl/rest/v1/profiles?on_conflict=id").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", SupabasePublishableKey)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
        }
        val body = JSONObject().put("id", userId).put("full_name", name).put("phone", phone)
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val responseText = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode in 200..299) SupabaseAuthResult(true, "Profile saved.")
        else {
            val response = JSONObject(responseText.ifBlank { "{}" })
            SupabaseAuthResult(false, response.optString("message", "Could not save profile to the database."))
        }
    }.getOrElse { SupabaseAuthResult(false, "Could not reach Supabase. Check your connection and try again.") }
}

private suspend fun supabaseDeleteAccount(accessToken: String): SupabaseAuthResult = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL("$SupabaseProjectUrl/functions/v1/delete-account").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", SupabasePublishableKey)
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        connection.outputStream.use { }
        val responseText = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode in 200..299) SupabaseAuthResult(true, "Account deleted.")
        else if (connection.responseCode == 404) SupabaseAuthResult(false, "Delete function not found. Deploy an Edge Function named exactly delete-account in this Supabase project.")
        else {
            val response = JSONObject(responseText.ifBlank { "{}" })
            SupabaseAuthResult(false, response.optString("error", response.optString("message", "Could not delete the account.")))
        }
    }.getOrElse { SupabaseAuthResult(false, "Could not reach the account deletion service.") }
}

@Composable
private fun PreferencesScreen(theme: ThemeChoice, onThemeChanged: (ThemeChoice) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences(AppPreferencesName, 0) }
    var followingTeams by remember { mutableStateOf(preferences.readStringList(PrefFollowingTeams, listOf("GodLike", "Revenant"))) }
    var followingPlayers by remember { mutableStateOf(preferences.readStringList(PrefFollowingPlayers, listOf("Jonathan", "Mortal"))) }
    var followingEvents by remember { mutableStateOf(preferences.readStringList(PrefFollowingEvents, listOf("FFML South Asia"))) }
    val canvas = Canvas
    val primary = Ink
    val secondary = MutedInk
    val card = Card
    Surface(Modifier.fillMaxSize(), color = canvas) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(22.dp)) {
            Text("‹", color = primary, style = MaterialTheme.typography.displaySmall, modifier = Modifier.noRippleClick(onBack))
            Text("Preferences", color = primary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Make EsportSphere feel like yours", color = secondary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(28.dp))
            SettingsHeadingForPage("APPEARANCE", secondary)
            Surface(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(20.dp), color = card) {
                Column(Modifier.padding(18.dp)) {
                    Text("Theme", color = primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Choose how EsportSphere looks", color = secondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(14.dp))
                    ThemeOption("Light", "Always use light mode", theme == ThemeChoice.Light, primary, secondary) { onThemeChanged(ThemeChoice.Light) }
                    ThemeOption("Dark", "Always use dark mode", theme == ThemeChoice.Dark, primary, secondary) { onThemeChanged(ThemeChoice.Dark) }
                    ThemeOption("Device default", "Follow your phone’s setting", theme == ThemeChoice.Device, primary, secondary) { onThemeChanged(ThemeChoice.Device) }
                }
            }
            Spacer(Modifier.height(24.dp))
            SettingsHeadingForPage("LANGUAGE", secondary)
            Surface(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(20.dp), color = card) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text("English", color = primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("Selected", color = Blue, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(24.dp))
            SettingsHeadingForPage("YOUR ESPORTS PREFERENCES", secondary)
            PreferenceList("Teams", followingTeams, primary, secondary, card) { followingTeams = it; preferences.saveStringList(PrefFollowingTeams, it) }
            PreferenceList("Players", followingPlayers, primary, secondary, card) { followingPlayers = it; preferences.saveStringList(PrefFollowingPlayers, it) }
            PreferenceList("Events", followingEvents, primary, secondary, card) { followingEvents = it; preferences.saveStringList(PrefFollowingEvents, it) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ThemeOption(title: String, subtitle: String, selected: Boolean, primary: Color, secondary: Color, action: () -> Unit) = Row(Modifier.fillMaxWidth().noRippleClick(action).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(20.dp), shape = CircleShape, color = if (selected) Blue else Color.Transparent, border = BorderStroke(2.dp, if (selected) Blue else secondary)) { if (selected) Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall) } }; Spacer(Modifier.width(12.dp)); Column { Text(title, color = primary, fontWeight = FontWeight.Bold); Text(subtitle, color = secondary, style = MaterialTheme.typography.bodySmall) } }

@Composable
private fun PreferenceList(title: String, entries: List<String>, primary: Color, secondary: Color, card: Color, update: (List<String>) -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(20.dp), color = card) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (entries.isEmpty()) Text("No $title followed yet.", color = secondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            entries.forEach { entry ->
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(entry, color = primary, modifier = Modifier.weight(1f)); Text("Following", color = Blue, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.width(10.dp)); Text("Remove", color = Color(0xFFE94444), style = MaterialTheme.typography.labelMedium, modifier = Modifier.noRippleClick { update(entries - entry) }) }
            }
            Text("+ Add $title", color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp).noRippleClick { update(entries + "New $title") })
        }
    }
}

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences(AppPreferencesName, 0) }
    var matchReminders by remember { mutableStateOf(preferences.getBoolean(PrefMatchReminders, true)) }
    var tournamentUpdates by remember { mutableStateOf(preferences.getBoolean(PrefTournamentUpdates, true)) }
    var teamPlayerUpdates by remember { mutableStateOf(preferences.getBoolean(PrefTeamPlayerUpdates, true)) }
    var news by remember { mutableStateOf(preferences.getBoolean(PrefNewsNotifications, false)) }
    var pendingTopic by remember { mutableStateOf<NotificationTopic?>(null) }
    fun save(topic: NotificationTopic, enabled: Boolean) {
        preferences.edit().putBoolean(topic.preferenceKey, enabled).apply()
        when (topic) {
            NotificationTopic.MatchReminders -> matchReminders = enabled
            NotificationTopic.TournamentUpdates -> tournamentUpdates = enabled
            NotificationTopic.TeamPlayerUpdates -> teamPlayerUpdates = enabled
            NotificationTopic.News -> news = enabled
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingTopic?.let { save(it, granted) }
        pendingTopic = null
        if (!granted) Toast.makeText(context, "Allow notifications in Android settings to receive EsportSphere updates.", Toast.LENGTH_LONG).show()
    }
    fun update(topic: NotificationTopic, enabled: Boolean) {
        if (!enabled) {
            save(topic, false)
        } else if (EsportSphereNotifications.hasPermission(context)) {
            save(topic, true)
        } else {
            pendingTopic = topic
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(Unit) { EsportSphereNotifications.createChannels(context) }
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(22.dp)) {
            Text("‹", color = Ink, style = MaterialTheme.typography.displaySmall, modifier = Modifier.noRippleClick(onBack))
            Text("Notifications", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Choose the updates you want to receive", color = MutedInk, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(28.dp))
            NotificationToggle("Match reminders", "Upcoming matches and live game alerts", matchReminders) { update(NotificationTopic.MatchReminders, it) }
            NotificationToggle("Tournament updates", "Registration, standings and tournament news", tournamentUpdates) { update(NotificationTopic.TournamentUpdates, it) }
            NotificationToggle("Team / player updates", "Updates from teams and players you follow", teamPlayerUpdates) { update(NotificationTopic.TeamPlayerUpdates, it) }
            NotificationToggle("News", "Top esports stories and highlights", news) { update(NotificationTopic.News, it) }
        }
    }
}

@Composable
private fun NotificationToggle(title: String, subtitle: String, checked: Boolean, update: (Boolean) -> Unit) = Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(20.dp), color = Card) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MutedInk, style = MaterialTheme.typography.bodySmall) }; Switch(checked = checked, onCheckedChange = update) } }

@Composable private fun SettingsHeadingForPage(text: String, color: Color) = Text(text, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)

@Composable private fun SettingsHeading(text: String) = Text(text, color = MutedInk, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
@Composable private fun SettingsCard(icon: String, title: String, subtitle: String, action: () -> Unit, trailing: @Composable () -> Unit) = Surface(Modifier.fillMaxWidth().padding(top = 10.dp).noRippleClick(action), shape = RoundedCornerShape(20.dp), color = Card) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, color = Ink, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.width(18.dp)); Column(Modifier.weight(1f)) { Text(title, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MutedInk, style = MaterialTheme.typography.bodySmall) }; trailing() } }

@Composable
private fun LoginScreen(onSignedIn: (SupabaseAuthResult) -> Unit, onSignUp: () -> Unit, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().padding(28.dp)) {
            Text("‹", color = Ink, style = MaterialTheme.typography.displaySmall, modifier = Modifier.noRippleClick(onBack))
            Spacer(Modifier.height(26.dp))
            Image(painterResource(R.drawable.esportsphere_symbol), "EsportSphere", Modifier.size(86.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(20.dp))
            Text("Sign in to EsportSphere", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("Save teams, personalise your feed, and keep your esports world in sync.", color = MutedInk, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(30.dp))
            AuthField("Email address", email) { email = it }
            Spacer(Modifier.height(14.dp))
            AuthField("Password", password) { password = it }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                scope.launch {
                    isLoading = true
                    val result = supabaseSignIn(email.trim(), password)
                    isLoading = false
                    if (result.success) {
                        // Authentication remains email/password only. The required phone number is
                        // retained in user metadata and synced to the public profile after login.
                        val profileResult = if (
                            result.accessToken != null && result.userId != null &&
                            result.name != null && result.phone != null
                        ) {
                            supabaseUpsertProfile(result.accessToken, result.userId, result.name, result.phone)
                        } else {
                            SupabaseAuthResult(true, "No profile details to sync.")
                        }
                        if (!profileResult.success) {
                            Toast.makeText(context, "Signed in, but profile sync failed: ${profileResult.message}", Toast.LENGTH_LONG).show()
                        }
                        onSignedIn(result)
                    } else Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }, enabled = email.isNotBlank() && password.isNotBlank() && !isLoading, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(16.dp)) { Text(if (isLoading) "Signing in…" else "Sign in", fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(14.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = SoftBlue) { Text("Sign in securely with your EsportSphere email and password.", color = Ink, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp)) }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Text("New here? ", color = MutedInk); Text("Create an account", color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.noRippleClick(onSignUp)) }
        }
    }
}

@Composable
private fun SignUpScreen(onSignedUp: (SupabaseAuthResult) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState())) {
            Text("‹", color = Ink, style = MaterialTheme.typography.displaySmall, modifier = Modifier.noRippleClick(onBack))
            Spacer(Modifier.height(22.dp))
            Text("Create your account", color = Ink, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text("Join EsportSphere and shape your competitive world.", color = MutedInk, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(30.dp))
            Text("Full name *", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); AuthField("What should we call you?", name) { name = it }
            Spacer(Modifier.height(16.dp)); Text("Mobile number *", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); PhoneNumberField(phone) { phone = it }
            Spacer(Modifier.height(16.dp)); Text("Email address *", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); AuthField("name@example.com", email) { email = it }
            Spacer(Modifier.height(16.dp)); Text("Password *", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); PasswordField(password, passwordVisible, { password = it }, { passwordVisible = !passwordVisible })
            Spacer(Modifier.height(6.dp)); Text("Use at least 6 characters", color = MutedInk, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(28.dp))
            Button(onClick = {
                scope.launch {
                    isLoading = true
                    val result = supabaseSignUp(name.trim(), phone.trim(), email.trim(), password)
                    isLoading = false
                    if (!result.success) {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        if (result.message == "An account with this email already exists. Please sign in.") {
                            onBack()
                        }
                    }
                    else if (result.accessToken != null && result.userId != null) {
                        val profileResult = supabaseUpsertProfile(result.accessToken, result.userId, name.trim(), phone.trim())
                        if (profileResult.success) onSignedUp(result)
                        else Toast.makeText(context, "Account created, but profile database setup failed: ${profileResult.message}", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(context, "Account created. Check your email to confirm it, then sign in.", Toast.LENGTH_LONG).show()
                        onBack()
                    }
                }
            }, enabled = name.isNotBlank() && phone.length == 10 && email.isNotBlank() && password.length >= 6 && !isLoading, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(16.dp)) { Text(if (isLoading) "Creating account…" else "Create account", fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(14.dp)); Text("Name, a 10-digit mobile number, email address and a password of at least 6 characters are required. You will sign in with your email; your number is saved to your profile.", color = MutedInk, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AuthField(label: String, value: String, onValueChange: (String) -> Unit) = Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Outline), color = Card) { BasicTextField(value = value, onValueChange = onValueChange, singleLine = true, cursorBrush = SolidColor(Blue), textStyle = androidx.compose.ui.text.TextStyle(color = Ink), modifier = Modifier.padding(16.dp).fillMaxWidth(), decorationBox = { inner -> if (value.isEmpty()) Text(label, color = MutedInk, maxLines = 1); inner() }) }

@Composable
private fun PhoneNumberField(value: String, onValueChange: (String) -> Unit) {
    val displayValue = value.chunked(5).joinToString(" ")
    var fieldValue by remember { mutableStateOf(TextFieldValue(displayValue, TextRange(displayValue.length))) }
    LaunchedEffect(displayValue) {
        // Keep externally restored values in sync without overriding the cursor while typing.
        if (fieldValue.text != displayValue) fieldValue = TextFieldValue(displayValue, TextRange(displayValue.length))
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Outline),
        color = Card,
    ) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { entered ->
                val digits = entered.text.filter(Char::isDigit).take(10)
                val digitsBeforeCursor = entered.text.take(entered.selection.start).count(Char::isDigit).coerceAtMost(digits.length)
                val formatted = digits.chunked(5).joinToString(" ")
                // The formatted space is not a digit. Map the logical digit position back
                // to its visible position so the cursor stays after the character typed.
                val cursor = formatted.indexOfNthDigitEnd(digitsBeforeCursor)
                fieldValue = TextFieldValue(formatted, TextRange(cursor))
                onValueChange(digits)
            },
            singleLine = true,
            cursorBrush = SolidColor(Blue),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = androidx.compose.ui.text.TextStyle(color = Ink),
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("12345 67890", color = MutedInk)
                inner()
            },
        )
    }
}

private const val AvatarMediaFolder = "Pictures/EsportSphere"

private fun avatarDisplayName(userId: String, mimeType: String): String =
    "esportsphere_avatar_${userId}.${if (mimeType == "image/png") "png" else "jpg"}"

/**
 * Uses shared media rather than cache or app-private files. The photo remains under
 * Pictures/EsportSphere if the app is uninstalled; Android still lets the user delete it.
 */
private suspend fun persistAvatarInSharedMedia(context: Context, source: Uri, userId: String): Uri? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
    runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(source)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val displayName = avatarDisplayName(userId, mimeType)
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        resolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(displayName),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) resolver.delete(Uri.withAppendedPath(collection, cursor.getLong(0).toString()), null, null)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, AvatarMediaFolder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val destination = requireNotNull(resolver.insert(collection, values))
        try {
            requireNotNull(resolver.openInputStream(source)).use { input ->
                requireNotNull(resolver.openOutputStream(destination)).use { output -> input.copyTo(output) }
            }
            resolver.update(destination, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            destination
        } catch (error: Exception) {
            resolver.delete(destination, null, null)
            throw error
        }
    }.onFailure { Log.w("EsportSphereAvatar", "Unable to persist profile image", it) }.getOrNull()
}

private suspend fun findPersistentAvatarUri(context: Context, userId: String?): Uri? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || userId.isNullOrBlank()) return@withContext null
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val names = listOf(avatarDisplayName(userId, "image/jpeg"), avatarDisplayName(userId, "image/png"))
    context.contentResolver.query(
        collection,
        arrayOf(MediaStore.Images.Media._ID),
        "${MediaStore.MediaColumns.DISPLAY_NAME} IN (?, ?)",
        names.toTypedArray(),
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) Uri.withAppendedPath(collection, cursor.getLong(0).toString()) else null
    }
}

private fun String.indexOfNthDigitEnd(digitCount: Int): Int {
    if (digitCount <= 0) return 0
    var seen = 0
    forEachIndexed { index, character ->
        if (character.isDigit() && ++seen == digitCount) return index + 1
    }
    return length
}

@Composable
private fun PasswordField(value: String, visible: Boolean, onValueChange: (String) -> Unit, onVisibilityChange: () -> Unit, hint: String = "Create a password") = Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Outline), color = Card) {
    Row(Modifier.padding(start = 16.dp, top = 4.dp, end = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            cursorBrush = SolidColor(Blue),
            textStyle = androidx.compose.ui.text.TextStyle(color = Ink),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = MutedInk); inner() },
        )
        Text(if (visible) "🙈" else "👁", modifier = Modifier.noRippleClick(onVisibilityChange).padding(8.dp))
    }
}
@Composable
private fun avatarColour(index: Int) = listOf(Color(0xFF121927), Blue, Color(0xFFB56D45))[index]

@Composable
private fun SettingsRow(label: String, action: () -> Unit) = Row(Modifier.fillMaxWidth().noRippleClick(action).padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Ink, style = MaterialTheme.typography.titleMedium); Text("›", color = MutedInk, style = MaterialTheme.typography.headlineSmall) }

@Composable
private fun ComingSoonScreen() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("◌", color = Blue, style = MaterialTheme.typography.displayLarge); Spacer(Modifier.height(16.dp)); Text("Performance", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(8.dp)); Text("Coming soon", color = MutedInk, textAlign = TextAlign.Center) } }

@Composable
private fun WatchScreen() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("▶", color = Blue, style = MaterialTheme.typography.displayLarge); Spacer(Modifier.height(16.dp)); Text("Watch", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(8.dp)); Text("YouTube and X handoff will be connected here.", color = MutedInk, textAlign = TextAlign.Center) } }

@Composable
private fun BottomNavigation(current: Screen, navigate: (Screen) -> Unit) = Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Card.copy(alpha = 0.92f),
        tonalElevation = 8.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Outline.copy(alpha = 0.8f)),
    ) {
    Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        NavItem("Home", "⌂", current == Screen.Home, Modifier.weight(1f)) { navigate(Screen.Home) }
        NavItem("Explore", "⌕", current == Screen.Explore, Modifier.weight(1f)) { navigate(Screen.Explore) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Surface(Modifier.size(58.dp), shape = CircleShape, color = Ink, shadowElevation = 10.dp) { Image(painterResource(R.drawable.esportsphere_symbol), "Ask Sphere", Modifier.fillMaxSize().noRippleClick { navigate(Screen.Ask) }, contentScale = ContentScale.Fit) } }
        NavItem("Watch", "▻", current == Screen.Watch, Modifier.weight(1f)) { navigate(Screen.Watch) }
        NavItem("Performance", "▥", current == Screen.Performance, Modifier.weight(1f)) { navigate(Screen.Performance) }
    }
    }
}

@Composable
private fun NavItem(label: String, icon: String, selected: Boolean, modifier: Modifier, action: () -> Unit) = Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.noRippleClick(action).padding(4.dp)) { Text(icon, color = if (selected) Blue else MutedInk, style = MaterialTheme.typography.titleLarge); Text(label, color = if (selected) Blue else MutedInk, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center) }

@Composable
internal fun Modifier.noRippleClick(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

@Composable private fun SectionTitle(title: String, action: String, onAction: () -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text(action, color = Blue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.noRippleClick(onAction)) }

private val Canvas: Color
    @Composable get() = MaterialTheme.colorScheme.background
private val Card: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val Outline: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val Ink: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground
private val MutedInk: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val Blue: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val SoftBlue: Color
    @Composable get() = MaterialTheme.colorScheme.secondaryContainer
private val Gold = Color(0xFFD49A32)
private val EsportSphereLightColors = lightColorScheme(
    primary = Color(0xFF3D55D9),
    onPrimary = Color.White,
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF161721),
    surfaceVariant = Color(0xFFF3F3F7),
    onSurfaceVariant = Color(0xFF707382),
    outlineVariant = Color(0xFFE0E1E8),
    secondaryContainer = Color(0xFFEAF0FF),
    tertiaryContainer = Color(0xFFF3EAE2),
)
private val EsportSphereDarkColors = darkColorScheme(
    primary = Color(0xFFB7C4FF),
    onPrimary = Color(0xFF10236D),
    background = Color(0xFF111218),
    onBackground = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF20212A),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outlineVariant = Color(0xFF454650),
    secondaryContainer = Color(0xFF26345A),
    tertiaryContainer = Color(0xFF47352B),
)
private const val DemoEmail = "demo@esportsphere.app"
private const val DemoPassword = "Sphere@123"

@Preview(showBackground = true) @Composable fun PreviewHome() {
    AppShell(
        screen = Screen.Home,
        isLoggedIn = false,
        avatarPreset = 0,
        uploadedAvatarUri = null,
        navigate = {},
        onSignedIn = {},
        onSignedOut = {},
        onAvatarChanged = { _, _ -> },
        themeChoice = ThemeChoice.Light,
        onThemeChoiceChanged = {},
        selectedPlayer = null,
        onPlayerSelected = {},
        selectedEvent = null,
        onEventSelected = {},
        showHomeGreeting = false,
        onHomeGreetingFinished = {},
        homeCarousel = null,
    )
}
