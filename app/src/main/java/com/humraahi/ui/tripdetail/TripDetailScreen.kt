package com.humraahi.ui.tripdetail

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.humraahi.navigation.Routes
import com.humraahi.ui.chat.ChatScreen
import com.humraahi.ui.itinerary.ItineraryScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: TripDetailViewModel = viewModel(
        factory = TripDetailViewModel.factory(application, tripId)
    )
    val joinState by viewModel.joinState.collectAsState()
    val trip by viewModel.trip.collectAsState()
    val memberError by viewModel.memberError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMembers by remember { mutableStateOf(false) }
    val tabs = listOf("Chat", "Itinerary")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val inviteMember = {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(
                Intent.EXTRA_TEXT,
                "Join my trip on Humraahi!\n${Routes.tripInviteUrl(tripId)}"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    LaunchedEffect(memberError) {
        memberError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMemberError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Trip Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMembers = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Trip members")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = joinState) {
            JoinTripState.Joining -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            JoinTripState.Ready -> {
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    TabRow(selectedTabIndex = pagerState.currentPage) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> ChatScreen(tripId = tripId)
                            1 -> ItineraryScreen()
                        }
                    }
                }

                if (showMembers) {
                    TripMembersDialog(
                        trip = trip,
                        currentUserId = viewModel.currentUserId,
                        onDismiss = { showMembers = false },
                        onInvite = inviteMember,
                        onRemoveMember = viewModel::removeMember
                    )
                }
            }
            is JoinTripState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = viewModel::retryJoin,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
