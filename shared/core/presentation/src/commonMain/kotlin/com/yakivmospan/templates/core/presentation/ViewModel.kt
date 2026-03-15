package com.yakivmospan.templates.core.presentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class ViewModel<Event, State, SideEffect>(initialState: State) : ViewModelEventReceiver<Event>, androidx.lifecycle.ViewModel() {
    private val _state: MutableStateFlow<State> = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state

    private val _sideEffect: MutableSharedFlow<SideEffect> = MutableSharedFlow(extraBufferCapacity = 1)
    val sideEffects: SharedFlow<SideEffect> = _sideEffect.asSharedFlow()

    abstract override fun onEvent(event: Event)

    protected fun updateState(update: (State) -> State) {
        _state.value = update(_state.value)
    }

    protected fun updateState(update: State): Unit {
        _state.value = update
    }

    protected fun emitSideEffect(effect: SideEffect) = _sideEffect.tryEmit(effect)
}

interface ViewModelEventReceiver<Event> {
    fun onEvent(event: Event)
}