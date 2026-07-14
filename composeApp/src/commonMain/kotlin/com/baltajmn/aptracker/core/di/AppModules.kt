package com.baltajmn.aptracker.core.di

import com.baltajmn.aptracker.core.data.createSupabaseClient
import com.baltajmn.aptracker.core.data.repository.SupabaseActivityRepository
import com.baltajmn.aptracker.core.data.repository.SupabaseRoomRepository
import com.baltajmn.aptracker.core.data.repository.SupabaseSlotRepository
import com.baltajmn.aptracker.core.domain.repository.ActivityRepository
import com.baltajmn.aptracker.core.domain.repository.RoomRepository
import com.baltajmn.aptracker.core.domain.repository.SlotRepository
import com.baltajmn.aptracker.core.push.createPushRegistrar
import com.baltajmn.aptracker.feature.auth.AuthViewModel
import com.baltajmn.aptracker.feature.rooms.AddRoomViewModel
import com.baltajmn.aptracker.feature.rooms.RoomDetailViewModel
import com.baltajmn.aptracker.feature.rooms.RoomsViewModel
import com.baltajmn.aptracker.feature.notifications.NotificationPrefsViewModel
import com.baltajmn.aptracker.feature.settings.SettingsViewModel
import com.baltajmn.aptracker.feature.slots.SlotDetailViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val dataModule = module {
    single { createSupabaseClient() }
    single<RoomRepository> { SupabaseRoomRepository(get()) }
    single<SlotRepository> { SupabaseSlotRepository(get()) }
    single<ActivityRepository> { SupabaseActivityRepository(get()) }
}

val authModule = module {
    single { createPushRegistrar(get()) }
    viewModel { AuthViewModel(get(), get()) }
}

val roomsModule = module {
    viewModelOf(::RoomsViewModel)
    viewModelOf(::AddRoomViewModel)
    viewModelOf(::RoomDetailViewModel)
}

val slotsModule = module {
    viewModelOf(::SlotDetailViewModel)
}

val notificationsModule = module {
    viewModelOf(::NotificationPrefsViewModel)
}

val settingsModule = module {
    viewModel { SettingsViewModel(get()) }
}

val appModules = listOf(dataModule, authModule, roomsModule, slotsModule, notificationsModule, settingsModule)
