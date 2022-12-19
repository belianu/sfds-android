package dev.meres.sfds

import kotlinx.coroutines.flow.MutableSharedFlow

class Channel<A, B, C, D>(
    val id: String,
    val owner: Communicator.Owner? = null,
    val registeredUsers: MutableList<Communicator.User> = mutableListOf(),
    val registrationCap: Int = 0,
    val channel: MutableSharedFlow<List<Signal<A, B, C, D>>> = MutableSharedFlow(),
)