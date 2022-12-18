package dev.meres.sfds

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

//Signals From Deep Space
class SFDS<A, B, C, D>(
    val deepSpaceCarrier: CoroutineScope, channels: List<TransmissionChannel<A, B, C, D>> = emptyList()
) {

    private val addressBook: HashMap<String, TransmissionChannel<A, B, C, D>> = hashMapOf()

    data class TransmissionChannel<A, B, C, D>(
        val id: String, val line: MutableSharedFlow<List<Signal<A, B, C, D>>> = MutableSharedFlow()
    )

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

    init {
        //check if is present a custom line list
        if (channels.isEmpty()) {
            val randomChannelId: String = UUID.randomUUID().toString()
            addressBook[randomChannelId] = TransmissionChannel(id = randomChannelId)
        } else {
            channels.forEach {
                addressBook[it.id] = it
            }
        }
    }


    inner class Generator {

        fun sendOne(channelId: String = "", signal: Signal<A, B, C, D>) {
            deepSpaceCarrier.launch {
                channelFinder(channelId)?.line?.emit(listOf(signal))
            }
        }

        fun sendSome(channelId: String = "", signals: List<Signal<A, B, C, D>>) {
            if (signals.isNotEmpty()) {
                deepSpaceCarrier.launch {
                    channelFinder(channelId)?.line?.emit(signals)
                }
            } else {
                Log.w("SFDS", "List of signals to send is empty")
            }
        }
    }


    inner class Receiver {

        fun receiveOne(channelId: String = "", signalIndex: Int = 0, analyzeSignal: (Signal<A, B, C, D>) -> Unit) {
            //get the transmission Line
            val transmissionChannel: TransmissionChannel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            transmissionChannel?.let { activeLine ->
                deepSpaceCarrier.launch {
                    activeLine.line.collect { signals ->
                        signalAnalyzer(signals, signalIndex, analyzeSignal)
                    }
                }
            }
        }

        fun receiveAll(channelId: String = "", analyzeSignals: (List<Signal<A, B, C, D>>) -> Unit) {
            //get the transmission Line
            val transmissionChannel: TransmissionChannel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            transmissionChannel?.let { activeLine ->
                deepSpaceCarrier.launch {
                    activeLine.line.collect { signals ->
                        analyzeSignals(signals)
                    }
                }
            }
        }

        fun receiveLatestOne(
            channelId: String = "",
            signalIndex: Int = 0,
            analyzeSignal: (Signal<A, B, C, D>) -> Unit
        ) {

            //get the transmission Line
            val transmissionChannel: TransmissionChannel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            transmissionChannel?.let { activeLine ->
                deepSpaceCarrier.launch {
                    activeLine.line.collectLatest { signals ->
                        signalAnalyzer(signals, signalIndex, analyzeSignal)
                    }
                }
            }
        }

        fun receiveLatestAll(channelId: String = "", analyzeSignals: (List<Signal<A, B, C, D>>) -> Unit) {

            //get the transmission Line
            val transmissionChannel: TransmissionChannel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            transmissionChannel?.let { activeLine ->
                deepSpaceCarrier.launch {
                    activeLine.line.collectLatest { signals ->
                        analyzeSignals(signals)
                    }
                }
            }
        }

        private fun signalAnalyzer(
            signals: List<Signal<A, B, C, D>>,
            signalIndex: Int,
            analysis: (Signal<A, B, C, D>) -> Unit
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

    private fun channelFinder(channelId: String): TransmissionChannel<A, B, C, D>? {
        //get the line
        val channel: TransmissionChannel<A, B, C, D>? = if (channelId.isNotBlank()) {
            addressBook[channelId]
        } else {
            addressBook.entries.firstOrNull()?.value
        }

        if (channel == null) {
            Log.e("SFDS", "Channel $channelId not present in the list of transmission lines.")
        }

        return channel
    }
}