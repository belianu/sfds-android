package dev.meres.sfds

import kotlinx.coroutines.flow.MutableSharedFlow

class TransmissionChannel<A, B, C, D>(
    val id: String,
    val owners: List<Communicator> = listOf(),
    val users: List<Communicator> = listOf(),
    val channel: MutableSharedFlow<List<Signal<A, B, C, D>>> = MutableSharedFlow(),
)

/*
class ClearChannel<A, B, C, D>(
    id: String,
    channel: MutableSharedFlow<List<Signal<A, B, C, D>>> = MutableSharedFlow()
) : TransmissionChannel<A, B, C, D>(id = id, channel = channel)

class SecuredChannel<A, B, C, D>(
    id: String,
    channel: MutableSharedFlow<List<Signal<A, B, C, D>>> = MutableSharedFlow()
) : TransmissionChannel<A, B, C, D>(id = id, channel = channel)
*/



