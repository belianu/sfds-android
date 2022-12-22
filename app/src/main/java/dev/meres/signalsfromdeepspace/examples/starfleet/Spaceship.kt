package dev.meres.signalsfromdeepspace.examples.starfleet

import dev.meres.sfds.SFDS

data class Spaceship(
    val name: String,
    val communicationSystem: SFDS<String, String, String, String>
)
