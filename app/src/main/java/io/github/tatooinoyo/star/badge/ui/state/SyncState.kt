package io.github.tatooinoyo.star.badge.ui.state

sealed interface SyncState {
    data object Idle : SyncState

    sealed interface Sender : SyncState {
        data class Ready(val shareCode: String) : Sender
        data class Handshaking(val shareCode: String, val targetIp: String) : Sender
        data class Sending(val shareCode: String) : Sender
        data class Success(val shareCode: String) : Sender
        data class Error(val shareCode: String, val message: String) : Sender
    }

    sealed interface Receiver : SyncState {
        data object Searching : Receiver
        data class Receiving(val senderIp: String) : Receiver
        data class Confirming(val badgeCount: Int) : Receiver
        data object Success : Receiver
        data class Error(val message: String) : Receiver
    }
}
