package dev.meres.sfds

interface Receiver<A, B, C> {

    fun receiveOne(
        sessionId: String = "",
        lineId: String = "",
        signalIndex: Int = 0,
        analyzeSignal: (SFDSComSys.Signal<A, B, C>) -> Unit
    ) {
    }

    fun receiveAll(
        sessionId: String = "",
        lineId: String = "",
        analyzeSignals: (List<SFDSComSys.Signal<A, B, C>>) -> Unit
    ) {
    }

    fun receiveLatestOne(
        sessionId: String = "",
        lineId: String = "",
        signalIndex: Int = 0,
        analyzeSignal: (SFDSComSys.Signal<A, B, C>) -> Unit
    ) {
    }

    fun receiveLatestAll(
        sessionId: String = "",
        lineId: String = "",
        analyzeSignals: (List<SFDSComSys.Signal<A, B, C>>) -> Unit
    ) {}
}