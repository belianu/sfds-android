package dev.meres.sfds

interface Generator<A, B, C> {

    fun sendOne(sessionId: String = "", lineId: String = "", signal: SFDSComSys.Signal<A, B, C>) {}

    fun sendSome(sessionId: String = "", lineId: String = "", signals: List<SFDSComSys.Signal<A, B, C>>) {}
}