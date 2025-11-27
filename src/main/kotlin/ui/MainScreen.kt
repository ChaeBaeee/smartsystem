package com.smartstudy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartstudy.models.UserProfile
import com.smartstudy.services.AuthService
import com.smartstudy.ui.theme.SmartStudyTheme
import kotlinx.coroutines.launch

private val ShellGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFF7F9FD),
        Color(0xFFE8F1FF),
        Color(0xFFF7F9FD)
    )
)

private data class NavDestination(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

enum class Screen {
    DASHBOARD,
    SCHEDULE,
    TIMER,
    TRACKING,
    SUBJECTS,
    GRADES,
    ALERTS
}

@Composable
fun MainScreen() {
    val authService = remember { AuthService() }
    var currentUser by remember { mutableStateOf(authService.getCurrentUser()) }
    var isLoggedIn by remember { mutableStateOf(authService.isLoggedIn() && currentUser != null) }

    if (!isLoggedIn || currentUser == null) {
        LoginScreen(authService) { user ->
            currentUser = user
            isLoggedIn = true
        }
        return
    }

    val navItems = remember {
        listOf(
            NavDestination("Dashboard", Icons.Filled.Home, Screen.DASHBOARD),
            NavDestination("Study Schedule", Icons.Filled.DateRange, Screen.SCHEDULE),
            NavDestination("Focus Timer", Icons.Filled.AccessTime, Screen.TIMER),
            NavDestination("Time Tracking", Icons.Filled.History, Screen.TRACKING),
            NavDestination("Subjects & Topics", Icons.Filled.MenuBook, Screen.SUBJECTS),
            NavDestination("Grades", Icons.Filled.Assessment, Screen.GRADES),
            NavDestination("Alerts", Icons.Filled.Warning, Screen.ALERTS)
        )
    }
    var selected by remember { mutableStateOf(navItems.first()) }

    SmartStudyTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShellGradient)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Sidebar(
                    modifier = Modifier.fillMaxHeight(),
                    items = navItems,
                    selected = selected,
                    onSelect = { selected = it },
                    onLogout = {
                        authService.logout()
                        currentUser = null
                        isLoggedIn = false
                    }
                )
                MainContent(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    selected = selected.screen,
                    user = currentUser!!,
                    onNavigate = { screen ->
                        navItems.find { it.screen == screen }?.let { selected = it }
                    },
                    onLogout = {
                        authService.logout()
                        currentUser = null
                        isLoggedIn = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Sidebar(
    modifier: Modifier = Modifier,
    items: List<NavDestination>,
    selected: NavDestination,
    onSelect: (NavDestination) -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Surface(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight(),
        color = Color.White,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Smart Study",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3) // Blue color for visibility
                )
                Text(
                    text = "Academic Progress",
                    style = MaterialTheme.typography.caption,
                    color = Color(0xFF666666) // Dark gray for better visibility
                )
            }

            items.forEach { item ->
                SidebarItem(
                    destination = item,
                    selected = item == selected,
                    onClick = { onSelect(item) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFE74C3C)
                )
            ) {
                Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log Out", color = Color.White)
            }
        }
    }
}

@Composable
private fun SidebarItem(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colors.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    
    val textColor = if (selected) {
        MaterialTheme.colors.primary
    } else {
        Color(0xFF1A1A1A) // Dark gray for better visibility
    }
    
    val iconColor = if (selected) {
        MaterialTheme.colors.primary
    } else {
        Color(0xFF4A4A4A) // Medium gray for better visibility
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = iconColor
            )
            Text(
                text = destination.label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    selected: Screen,
    user: UserProfile,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    var showProfileMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Welcome message removed - only shown in Dashboard
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(4.dp)
                                .clickable { showProfileMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.avatarInitials.takeIf { it.isNotBlank() } ?: user.name.split(" ")
                                    .take(2).joinToString("") { it.first().uppercaseChar().toString() },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            DropdownMenuItem(onClick = {
                                showProfileMenu = false
                                onLogout()
                            }) {
                                Text("Sign out")
                            }
                        }
                    }
                }
            }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = 4.dp,
                color = MaterialTheme.colors.surface
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                    when (selected) {
                        Screen.DASHBOARD -> DashboardScreen(onNavigate = onNavigate)
                        Screen.SCHEDULE -> ScheduleScreen()
                        Screen.TIMER -> StudyTimerScreen()
                        Screen.TRACKING -> TimeTrackingScreen()
                        Screen.SUBJECTS -> SubjectsScreen(
                    onNavigateToTimer = { onNavigate(Screen.TIMER) }
                )
                        Screen.GRADES -> GradesScreen()
                        Screen.ALERTS -> AlertsScreen()
                    }
                }
            }
        }
    }
}
