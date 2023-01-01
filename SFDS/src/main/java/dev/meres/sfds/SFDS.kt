package dev.meres.sfds

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

//Signals From Deep Space
class SFDSComSys<A, B, C>(
    val deepSpaceCarrier: CoroutineScope, lines: List<TransmissionLine<A, B, C>> = emptyList()
) : Generator<A, B, C>, Receiver<A, B, C> {

    //region variables
    private val addressBook: HashMap<String, TransmissionLine<A, B, C>> = hashMapOf()
    private var sessionsBook: HashMap<String, Job?> = hashMapOf()
    //endregion variables

    data class TransmissionLine<A, B, C>(
        val id: String, val line: MutableSharedFlow<List<Signal<A, B, C>>> = MutableSharedFlow()
    )

    data class Signal<A, B, C>(
        val target: A? = null, val action: B? = null, val message: C? = null
    )

    init {
        //check if is present a custom line list
        if (lines.isEmpty()) {
            val randomLineId: String = UUID.randomUUID().toString()
            addressBook[randomLineId] = TransmissionLine(id = randomLineId)
        } else {
            lines.forEach {
                addressBook[it.id] = it
            }
        }
    }


    //region overrides
    override fun sendOne(sessionId: String, lineId: String, signal: Signal<A, B, C>) {

        checkAndRemoveDuplicateSessions(sessionId)

        if (sessionsBook.containsKey(sessionId).not()) {
            super.sendOne(sessionId, lineId, signal)
            //start the new session of the job
            sessionsBook[sessionId] = deepSpaceCarrier.launch {
                lineFinder(lineId)?.line?.emit(listOf(signal))
            }
        }
    }

    override fun sendSome(sessionId: String, lineId: String, signals: List<Signal<A, B, C>>) {
        super.sendSome(sessionId, lineId, signals)

        checkAndRemoveDuplicateSessions(sessionId)

        if (sessionsBook.containsKey(sessionId).not()) {
            if (signals.isNotEmpty()) {
                //start the new session of the job
                sessionsBook[sessionId] = deepSpaceCarrier.launch {
                    lineFinder(lineId)?.line?.emit(signals)
                }
            } else {
                Log.w("SFDS", "List of signals to send is empty")
            }
        }
    }

    override fun receiveOne(
        sessionId: String,
        lineId: String,
        signalIndex: Int,
        analyzeSignal: (Signal<A, B, C>) -> Unit
    ) {
        super.receiveOne(sessionId, lineId, signalIndex, analyzeSignal)

        checkAndRemoveDuplicateSessions(sessionId)

        if (sessionsBook.containsKey(sessionId).not()) {
            //get the transmission Line
            val transmissionLine: TransmissionLine<A, B, C>? = lineFinder(lineId)

            //check the transmission Line
            transmissionLine?.let { activeLine ->
                //start the new session of the job
                sessionsBook[sessionId] = deepSpaceCarrier.launch {
                    activeLine.line.collect { signals ->
                        signalAnalyzer(signals, signalIndex, analyzeSignal)
                    }
                }
            }
        }
    }

    override fun receiveAll(sessionId: String, lineId: String, analyzeSignals: (List<Signal<A, B, C>>) -> Unit) {
        super.receiveAll(sessionId, lineId, analyzeSignals)

        checkAndRemoveDuplicateSessions(sessionId)

        if (sessionsBook.containsKey(sessionId).not()) {
            //get the transmission Line
            val transmissionLine: TransmissionLine<A, B, C>? = lineFinder(lineId)
            //check the transmission Line
            transmissionLine?.let { activeLine ->
                //start the new session of the job
                sessionsBook[sessionId] = deepSpaceCarrier.launch {
                    activeLine.line.collect { signals ->
                        analyzeSignals(signals)
                    }
                }
            }
        }
    }

    override fun receiveLatestOne(
        sessionId: String,
        lineId: String,
        signalIndex: Int,
        analyzeSignal: (Signal<A, B, C>) -> Unit
    ) {
        super.receiveLatestOne(sessionId, lineId, signalIndex, analyzeSignal)

        checkAndRemoveDuplicateSessions(sessionId)
        if (sessionsBook.containsKey(sessionId).not()) {
            //get the transmission Line
            val transmissionLine: TransmissionLine<A, B, C>? = lineFinder(lineId)
            //check the transmission Line
            transmissionLine?.let { activeLine ->
                //start the new session of the job
                sessionsBook[sessionId] = deepSpaceCarrier.launch {
                    activeLine.line.collectLatest { signals ->
                        signalAnalyzer(signals, signalIndex, analyzeSignal)
                    }
                }
            }
        }
    }

    override fun receiveLatestAll(sessionId: String, lineId: String, analyzeSignals: (List<Signal<A, B, C>>) -> Unit) {
        super.receiveLatestAll(sessionId, lineId, analyzeSignals)

        checkAndRemoveDuplicateSessions(sessionId)

        if (sessionsBook.containsKey(sessionId).not()) {
            //get the transmission Line
            val transmissionLine: TransmissionLine<A, B, C>? = lineFinder(lineId)
            //check the transmission Line
            transmissionLine?.let { activeLine ->
                //start the new session of the job
                sessionsBook[sessionId] = deepSpaceCarrier.launch {
                    activeLine.line.collectLatest { signals ->
                        analyzeSignals(signals)
                    }
                }
            }
        }
    }
    //endregion overrides


    //region private methods
    private fun signalAnalyzer(
        signals: List<Signal<A, B, C>>,
        signalIndex: Int,
        analysis: (Signal<A, B, C>) -> Unit
    ) {
        //check the index presence in the list of signals
        if (signalIndex in 0 until signals.count()) {
            //analyze the signal
            analysis(signals[signalIndex])
        } else {
            Log.e(
                "SFDS",
                "Index $signalIndex is out of the indexes present in the retrieved list of signals : ${signals.count()}"
            )
        }
    }

    private fun lineFinder(lineId: String): TransmissionLine<A, B, C>? {
        //get the line
        val line: SFDSComSys.TransmissionLine<A, B, C>? = if (lineId.isNotBlank()) {
            addressBook[lineId]
        } else {
            addressBook.entries.firstOrNull()?.value
        }

        if (line == null) {
            Log.e("SFDS", "Line $lineId not present in the list of transmission lines.")
        }

        return line
    }

    private fun checkAndRemoveDuplicateSessions(sessionId: String) {
        if (sessionsBook.containsKey(sessionId)) {
            sessionsBook[sessionId]?.cancel()
            sessionsBook.remove(sessionId).also { printDuplicateSession(sessionId) }
        }
    }

    private fun printDuplicateSession(sessionId: String) {
        Log.i("SFDS", "A pre-existent session of id : $sessionId has been deleted to create this new one.")
    }
    //endregion private methods
}