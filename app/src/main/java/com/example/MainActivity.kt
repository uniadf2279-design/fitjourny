package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.AppLanguage
import com.example.core.ArabicFitStrings
import com.example.core.EnglishFitStrings
import com.example.core.LocalFitStrings
import com.example.core.PreferencesRepository
import com.example.core.ThemePreference
import com.example.core.UiPreferences
import com.example.core.UiState
import com.example.core.configureAppCheck
import com.example.data.repository.AiPlanRepository
import com.example.data.repository.FitnessRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.CoachAndMoreScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExerciseLibraryScreen
import com.example.ui.screens.NutritionLibraryScreen
import com.example.ui.screens.PlansAndReportsScreen
import com.example.ui.screens.ProfileParqScreen
import com.example.ui.theme.FitJourneyTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.FitJourneyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before any UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            FitJourneyRoot()
        }
    }
}

@Composable
fun FitJourneyRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
    val uiPrefs by prefsRepo.uiPreferences.collectAsStateWithLifecycle(initialValue = UiPreferences())

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            val cm = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = cm,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val isArabic = uiPrefs.language == AppLanguage.ARABIC
    val strings = if (isArabic) ArabicFitStrings else EnglishFitStrings
    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalFitStrings provides strings,
        LocalLayoutDirection provides layoutDir
    ) {
        FitJourneyTheme(
            themePreference = uiPrefs.theme,
            isArabic = isArabic
        ) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    language = uiPrefs.language,
                    theme = uiPrefs.theme,
                    onToggleLanguage = { lang ->
                        scope.launch { prefsRepo.setLanguage(lang) }
                    },
                    onCycleTheme = { th ->
                        scope.launch { prefsRepo.setTheme(th) }
                    },
                    onAuthSuccess = { currentUser = Firebase.auth.currentUser }
                )
            } else {
                key(user.uid) {
                    AuthenticatedFitJourneyApp(
                        user = user,
                        prefsRepo = prefsRepo,
                        uiPrefs = uiPrefs
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedFitJourneyApp(
    user: FirebaseUser,
    prefsRepo: PreferencesRepository,
    uiPrefs: UiPreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val fitViewModel: FitJourneyViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val repo = FitnessRepository(app)
                FitJourneyViewModel(
                    repository = repo,
                    aiRepository = AiPlanRepository(),
                    preferencesRepository = prefsRepo,
                    currentUserId = user.uid,
                    currentUserDisplayName = user.displayName ?: "Athlete",
                    currentUserEmail = user.email,
                    isEmailVerified = user.isEmailVerified
                )
            }
        }
    )

    val strings = LocalFitStrings.current
    val profileState by fitViewModel.userProfileState.collectAsStateWithLifecycle()
    val workoutLogs by fitViewModel.workoutLogs.collectAsStateWithLifecycle()
    val measurements by fitViewModel.measurements.collectAsStateWithLifecycle()
    val plans by fitViewModel.plans.collectAsStateWithLifecycle()
    val exercises by fitViewModel.exercises.collectAsStateWithLifecycle()
    val meals by fitViewModel.meals.collectAsStateWithLifecycle()
    val planTemplates by fitViewModel.planTemplates.collectAsStateWithLifecycle()
    val coachRequests by fitViewModel.coachRequests.collectAsStateWithLifecycle()
    val isAdmin by fitViewModel.isAdmin.collectAsStateWithLifecycle()
    val aiState by fitViewModel.aiState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    var editingProfile by remember { mutableStateOf(false) }
    var showAdminConsole by remember { mutableStateOf(false) }

    when (val state = profileState) {
        is UiState.Loading -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        is UiState.Error -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { signOut(credentialManager, scope) }) {
                        Text(strings.signOut)
                    }
                }
            }
        }

        is UiState.Success -> {
            val profile = state.data
            if (profile == null || editingProfile) {
                if (editingProfile) {
                    BackHandler { editingProfile = false }
                }
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        TopAppBar(
                            title = { Text(strings.appName) },
                            actions = {
                                IconButton(
                                    onClick = {
                                        val nextLang = if (uiPrefs.language == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
                                        fitViewModel.setLanguage(nextLang, profile)
                                    }
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = strings.languageLabel)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    ProfileParqScreen(
                        existingProfile = profile,
                        defaultDisplayName = user.displayName ?: "Athlete",
                        currentLanguageCode = uiPrefs.language.code,
                        currentThemeCode = uiPrefs.theme.code,
                        onSave = { updated ->
                            fitViewModel.saveProfile(updated, isNew = (profile == null)) { res ->
                                if (res.isSuccess) editingProfile = false
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            } else if (showAdminConsole && isAdmin) {
                val adminViewModel: AdminViewModel = viewModel(
                    key = "admin_${user.uid}",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            AdminViewModel(FitnessRepository(app))
                        }
                    }
                )
                AdminConsoleScreen(
                    adminViewModel = adminViewModel,
                    onBack = { showAdminConsole = false }
                )
            } else {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        TopAppBar(
                            title = { Text(strings.appName) },
                            actions = {
                                IconButton(
                                    onClick = {
                                        val nextLang = if (uiPrefs.language == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
                                        fitViewModel.setLanguage(nextLang, profile)
                                    },
                                    modifier = Modifier.testTag("topbar_language_toggle")
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = strings.languageLabel)
                                }
                                IconButton(
                                    onClick = {
                                        val nextTheme = if (uiPrefs.theme == ThemePreference.DARK) ThemePreference.LIGHT else ThemePreference.DARK
                                        fitViewModel.setTheme(nextTheme, profile)
                                    },
                                    modifier = Modifier.testTag("topbar_theme_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (uiPrefs.theme == ThemePreference.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = strings.themeLabel
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            val navItems = listOf(
                                Triple(0, strings.navDashboard, Icons.Default.Dashboard),
                                Triple(1, strings.navExercises, Icons.Default.FitnessCenter),
                                Triple(2, strings.navNutrition, Icons.Default.Restaurant),
                                Triple(3, strings.navPlans, Icons.Default.CalendarMonth),
                                Triple(4, strings.navMore, Icons.Default.MoreHoriz)
                            )
                            navItems.forEach { (idx, label, icon) ->
                                NavigationBarItem(
                                    selected = currentTab == idx,
                                    onClick = { currentTab = idx },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_tab_$idx")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        0 -> DashboardScreen(
                            profile = profile,
                            workoutLogs = workoutLogs,
                            measurements = measurements,
                            plans = plans,
                            meals = meals,
                            onLogWorkout = { t, m, c, n -> fitViewModel.logWorkout(t, m, c, n) },
                            onDeleteWorkoutLog = { id -> fitViewModel.deleteWorkoutLog(id) },
                            onLogMeasurement = { w, wa, ch, bf, n -> fitViewModel.logMeasurement(w, wa, ch, bf, n) },
                            onNavigateToPlans = { currentTab = 3 },
                            onNavigateToNutrition = { currentTab = 2 },
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> ExerciseLibraryScreen(
                            exercises = exercises,
                            modifier = Modifier.padding(innerPadding)
                        )
                        2 -> NutritionLibraryScreen(
                            meals = meals,
                            modifier = Modifier.padding(innerPadding)
                        )
                        3 -> PlansAndReportsScreen(
                            profile = profile,
                            planTemplates = planTemplates,
                            plans = plans,
                            workoutLogs = workoutLogs,
                            measurements = measurements,
                            aiState = aiState,
                            onGeneratePlan = { weeks ->
                                fitViewModel.generateOrAdaptPlan(profile, weeks, uiPrefs.language.code)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        else -> CoachAndMoreScreen(
                            profile = profile,
                            coachRequests = coachRequests,
                            isAdmin = isAdmin,
                            language = uiPrefs.language,
                            theme = uiPrefs.theme,
                            onSubmitCoachRequest = { goal, method, notes ->
                                fitViewModel.submitCoachRequest(profile.displayName, goal, method, notes)
                            },
                            onToggleLanguage = { lang -> fitViewModel.setLanguage(lang, profile) },
                            onSelectTheme = { th -> fitViewModel.setTheme(th, profile) },
                            onEditHealthProfile = { editingProfile = true },
                            onOpenAdminConsole = { showAdminConsole = true },
                            onSignOut = { signOut(credentialManager, scope) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
