# Compose Architecture (MVVM)

## Layer structure per feature
```
feature/
  rooms/
    RoomsScreen.kt         # @Composable, collects state from ViewModel
    RoomsViewModel.kt      # StateFlow<UiState>, handles intents
    RoomsUiState.kt        # data class with loading/success/error states
```

## ViewModel pattern
```kotlin
class RoomsViewModel(private val getRooms: GetRoomsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(RoomsUiState())
    val state: StateFlow<RoomsUiState> = _state.asStateFlow()

    fun loadRooms() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getRooms().fold(
                onSuccess = { rooms -> _state.update { it.copy(rooms = rooms, isLoading = false) } },
                onFailure = { e -> _state.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }
}
```

## State collection in Compose
```kotlin
@Composable
fun RoomsScreen(viewModel: RoomsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ...
}
```

## Koin injection
```kotlin
// Module definition (core/di/)
val roomsModule = module {
    viewModel { RoomsViewModel(get()) }
    factory { GetRoomsUseCase(get()) }
    single<RoomRepository> { SupabaseRoomRepository(get()) }
}
```

## Navigation with type-safe routes
```kotlin
// core/navigation/Routes.kt
@Serializable object RoomList
@Serializable data class RoomDetail(val roomId: String)

// Usage
navController.navigate(RoomDetail(roomId = id))
```
