package dev.meres.sfds

abstract class Communicator(open val id: String) {
    data class Owner(override val id: String) : Communicator(id)
    data class User(override val id: String) : Communicator(id)
}
