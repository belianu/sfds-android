package dev.meres.sfds

data class Signal<A, B, C, D>(
    val sender: A? = null, val target: B? = null, val action: C? = null, val message: D? = null
) {

    data class Validated<A, B, C, D>(val sender: A, val target: B, val action: C, val message: D)

    fun validateAll(): Validated<A, B, C, D>? {
        return if (sender != null && target != null && action != null && message != null) {
            Validated(sender, target, action, message)
        } else null
    }

}