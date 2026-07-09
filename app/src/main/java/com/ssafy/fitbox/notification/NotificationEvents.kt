package com.ssafy.fitbox.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationEvents {
    private val _arrived = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val arrived = _arrived.asSharedFlow()

    fun notifyArrived() {
        _arrived.tryEmit(Unit)
    }
}
