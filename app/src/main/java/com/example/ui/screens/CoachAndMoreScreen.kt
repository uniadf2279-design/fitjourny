package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.LocalFitStrings
import com.example.core.ThemePreference
import com.example.data.model.CoachRequest
import com.example.data.model.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachAndMoreScreen(
    profile: UserProfile,
    coachRequests: List<CoachRequest>,
    isAdmin: Boolean,
    language: AppLanguage,
    theme: ThemePreference,
    onSubmitCoachRequest: (goalSummary: String, contactMethod: String, notes: String) -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit,
    onSelectTheme: (ThemePreference) -> Unit,
    onEditHealthProfile: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalFitStrings.current
    var goalSummary by remember { mutableStateOf("") }
    var contactMethod by remember { mutableStateOf("in_app_note") }
    var notes by remember { mutableStateOf("") }
    var submittedBanner by remember { mutableStateOf(false) }

    val contactMethods = listOf(
        "in_app_note" to (if (strings.isArabic) "ملاحظة داخل التطبيق" else "In-App Note"),
        "email" to (if (strings.isArabic) "البريد الإلكتروني" else "Email"),
        "phone_callback" to (if (strings.isArabic) "اتصال هاتفي" else "Phone Callback")
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Coach / Captain Request Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_coach_banner),
                            contentDescription = strings.coachTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.coachTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(strings.coachSubtitle, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = strings.coachDisclaimer,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = goalSummary,
                            onValueChange = { goalSummary = it.take(280) },
                            label = { Text(strings.goalSummaryLabel) },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("coach_goal_input")
                        )

                        Text(strings.contactMethodLabel, style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            contactMethods.forEach { (key, label) ->
                                FilterChip(
                                    selected = contactMethod == key,
                                    onClick = { contactMethod = key },
                                    label = { Text(label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it.take(550) },
                            label = { Text(strings.notesLabel) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (submittedBanner) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (strings.isArabic) "تم إرسال طلبك بنجاح لمراجعة الإدارة." else "Request submitted for Admin review.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (goalSummary.isNotBlank()) {
                                    onSubmitCoachRequest(goalSummary, contactMethod, notes)
                                    goalSummary = ""
                                    notes = ""
                                    submittedBanner = true
                                }
                            },
                            enabled = goalSummary.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_coach_request_button")
                        ) {
                            Text(strings.submitCoachRequestBtn)
                        }
                    }
                }
            }
        }

        if (coachRequests.isNotEmpty()) {
            item {
                Text(
                    text = strings.myCoachRequestsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                )
            }
            items(coachRequests, key = { it.id }) { req ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(req.goalSummary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = req.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (req.adminNotes.isNotBlank()) {
                            Text(
                                text = "Admin: ${req.adminNotes}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Settings & Controls
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = language == AppLanguage.ARABIC,
                            onClick = { onToggleLanguage(AppLanguage.ARABIC) },
                            label = { Text("العربية (RTL)") }
                        )
                        FilterChip(
                            selected = language == AppLanguage.ENGLISH,
                            onClick = { onToggleLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English (LTR)") }
                        )
                    }

                    Text(strings.themeLabel, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = theme == ThemePreference.SYSTEM,
                            onClick = { onSelectTheme(ThemePreference.SYSTEM) },
                            label = { Text(strings.themeSystem) }
                        )
                        FilterChip(
                            selected = theme == ThemePreference.LIGHT,
                            onClick = { onSelectTheme(ThemePreference.LIGHT) },
                            label = { Text(strings.themeLight) }
                        )
                        FilterChip(
                            selected = theme == ThemePreference.DARK,
                            onClick = { onSelectTheme(ThemePreference.DARK) },
                            label = { Text(strings.themeDark) }
                        )
                    }

                    OutlinedButton(
                        onClick = onEditHealthProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_health_profile_button")
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.editProfileBtn)
                    }

                    if (isAdmin) {
                        Button(
                            onClick = onOpenAdminConsole,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_admin_console_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.adminTitle)
                        }
                    }

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.signOut)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
