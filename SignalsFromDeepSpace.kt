import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

//Signals From Deep Space
class SFDSComSys<A, B, C>(
    val deepSpaceCarrier: CoroutineScope, lines: List<TransmissionLine<A, B, C>> = emptyList()
) {

    private val addressBook: HashMap<String, TransmissionLine<A, B, C>> = hashMapOf()

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


    inner class Generator {

        fun sendOne(lineId: String = "", signal: Signal<A, B, C>) {
            deepSpaceCarrier.launch {
                lineFinder(lineId)?.line?.emit(listOf(signal))
            }
        }

        fun sendSome(lineId: String = "", signals: List<Signal<A, B, C>>) {
            if (signals.isNotEmpty()) {
                deepSpaceCarrier.launch {
                    lineFinder(lineId)?.line?.emit(signals)
                }
            }else{
                Log.w("SFDS","List of signals to send is empty")
            }
        }
    }


    inner class Receiver {

        fun receiveOne(lineId: String = "", signalIndex: Int = 0, analyzeSignal: (Signal<A, B, C>) -> Unit) {
            //get the transmission Line
            val transmissionLine: TransmissionLine<A, B, C>? = lineFinder(lineId)

            //check the transmission Line
            transmissionLine?.let { activeLine ->
                deepSpaceCarrier.launch {
                    activeLine.line.collectLatest { signals ->
                        signalAnalyzer(signals, signalIndex, analyzeSignal)
                    }
                }
            }
        }

        fun receiveAll(lineId: String = "", analyzeSignals: (List<Signal<A, B, C>>) -> Unit) {
            //get the transmission Line
            val transmissionLine: TransmissionLine<A, B, C>? = lineFinder(lineId)

            //check the transmission Line
            transmissionLine?.let { activeLine ->
                deepSpaceCarrier.launch {
                    activeLine.line.collect { signals ->
                        analyzeSignals(signals)
                    }
                }
            }
        }

        fun receiveLatestOne(lineId: String = "", signalIndex: Int = 0, analyzeSignal: (Signal<A, B, C>) -> Unit) {

            //get the transmission Line
            val transmissionLine: TransmissionLine<A, B, C>? = lineFinder(lineId)

            //check the transmission Line
            transmissionLine?.let { activeLine ->
                deepSpaceCarrier.launch {
                    activeLine.line.collectLatest { signals ->
                        signalAnalyzer(signals, signalIndex, analyzeSignal)
                    }
                }
            }
        }

        fun receiveLatestAll(lineId: String = "", analyzeSignals: (List<Signal<A, B, C>>) -> Unit) {

            //get the transmission Line
            val transmissionLine: TransmissionLine<A, B, C>? = lineFinder(lineId)

            //check the transmission Line
            transmissionLine?.let { activeLine ->
                deepSpaceCarrier.launch {
                    activeLine.line.collectLatest { signals ->
                        analyzeSignals(signals)
                    }
                }
            }
        }

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
    }

    private fun lineFinder(lineId: String): TransmissionLine<A, B, C>? {
        //get the line
        val line: TransmissionLine<A, B, C>? = if (lineId.isNotBlank()) {
            addressBook[lineId]
        } else {
            addressBook.entries.firstOrNull()?.value
        }

        if (line == null) {
            Log.e("SFDS", "Line $lineId not present in the list of transmission lines.")
        }

        return line
    }
}
