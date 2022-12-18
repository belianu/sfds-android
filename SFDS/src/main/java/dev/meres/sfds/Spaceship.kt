package dev.meres.sfds

data class Spaceship(
    val name: String,
    val communicationSystem: SFDS<String,String,String,String>
)
