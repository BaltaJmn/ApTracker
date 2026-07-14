package com.baltajmn.aptracker.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.baltajmn.aptracker.core.domain.model.Slot
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPrefsScreen(navController: NavController) {
    val viewModel = koinViewModel<NotificationPrefsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifications") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.slots.isEmpty() -> Text(
                    "No slots configured. Add rooms and slots first.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.slots, key = { it.id }) { slot ->
                        SlotNotificationCard(
                            slot = slot,
                            onUpdate = viewModel::updateSlotNotifications
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotNotificationCard(slot: Slot, onUpdate: (Slot) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(slot.slotName, style = MaterialTheme.typography.titleMedium)
                    slot.gameName?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = slot.notifyEnabled,
                    onCheckedChange = { onUpdate(slot.copy(notifyEnabled = it)) }
                )
            }

            if (slot.notifyEnabled) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                NotificationToggleRow(
                    label = "Progression items",
                    description = "Items required to unlock other checks",
                    checked = slot.notifyProgression,
                    onCheckedChange = { onUpdate(slot.copy(notifyProgression = it)) }
                )
                NotificationToggleRow(
                    label = "Useful items",
                    description = "Helpful but not required items",
                    checked = slot.notifyUseful,
                    onCheckedChange = { onUpdate(slot.copy(notifyUseful = it)) }
                )
                NotificationToggleRow(
                    label = "Filler items",
                    description = "Non-essential items",
                    checked = slot.notifyFiller,
                    onCheckedChange = { onUpdate(slot.copy(notifyFiller = it)) }
                )
                NotificationToggleRow(
                    label = "Suppress local finds",
                    description = "Don't notify when you find your own items",
                    checked = slot.suppressLocal,
                    onCheckedChange = { onUpdate(slot.copy(suppressLocal = it)) }
                )
                NotificationToggleRow(
                    label = "Suppress other slots",
                    description = "Don't notify for items from other players",
                    checked = slot.suppressOthers,
                    onCheckedChange = { onUpdate(slot.copy(suppressOthers = it)) }
                )
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
