package io.github.tatooinoyo.star.badge.ui.state


sealed interface SyncState {
    // 1. 初始/空闲状态
    data object Idle : SyncState

    // 2. 发送端状态流 (Sender)
    sealed interface Sender : SyncState {
        // 生成了分享码，正在监听 UDP 广播等待接收端
        data class Ready(val shareCode: String) : Sender
        // 发现了接收端，正在建立 TCP 连接和握手
        data class Handshaking(val shareCode: String, val targetIp: String) : Sender
        // 握手成功，正在发送数据
        data class Sending(val shareCode: String) : Sender
        // 发送完成
        data class Success(val shareCode: String) : Sender
        // 发送过程出错
        data class Error(val shareCode: String, val message: String) : Sender
    }

    // 3. 接收端状态流 (Receiver)
    sealed interface Receiver : SyncState {
        // 正在广播 UDP 寻找发送端
        data object Searching : Receiver
        // 找到发送端，正在接收/解密数据
        data class Receiving(val senderIp: String) : Receiver
        // 接收并写入数据库成功
        data object Success : Receiver
        // 接收过程出错
        data class Error(val message: String) : Receiver
    }
}