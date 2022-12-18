package dev.meres.sfds

import android.util.Log
import dev.meres.sfds.conts.ConnectionType
import dev.meres.sfds.conts.TransmissionBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

//Signals From Deep Space
class SFDS<A, B, C, D>(
    val deepSpaceCarrier: CoroutineScope,
    val connectionType: ConnectionType = ConnectionType.BROADCAST,
    val transmissionBehavior: TransmissionBehavior = TransmissionBehavior.SHARED,
) {

    //region variables
    private val communicators: MutableList<Communicator> = mutableListOf()
    private val addressBook: HashMap<String, TransmissionChannel<A, B, C, D>> = hashMapOf()
    //endregion variables


    init {

        when (transmissionBehavior) {

            TransmissionBehavior.SHARED -> {
                newSharedChannel<A, B, C, D>().also {
                    addressBook[it.id] = it
                }
            }

            TransmissionBehavior.FULL_DUPLEX -> {

            }

            TransmissionBehavior.CLIENT_SERVER -> {

            }

        }
    }


    inner class Generator {

        val sessions: HashMap<String, Job> = hashMapOf()

        fun sendOne(sessionId: String = "",channelId: String = "", signal: Signal<A, B, C, D>) {
            //clear the job if exist
            sessions[sessionId]?.cancel().also { printDuplicateSession(sessionId) }
            //start the new session of the job
            sessions[sessionId] =deepSpaceCarrier.launch {
                channelFinder(channelId)?.channel?.emit(listOf(signal))
            }
        }

        fun sendSome(sessionId: String = "",channelId: String = "", signals: List<Signal<A, B, C, D>>) {
            if (signals.isNotEmpty()) {
                //clear the job if exist
                sessions[sessionId]?.cancel().also { printDuplicateSession(sessionId) }
                //start the new session of the job
                sessions[sessionId] =deepSpaceCarrier.launch {
                    channelFinder(channelId)?.channel?.emit(signals)
                }
            } else {
                Log.w("SFDS", "List of signals to send is empty")
            }
        }
    }


    inner class Receiver {

        val sessions: HashMap<String, Job> = hashMapOf()

        fun receiveOne(sessionId: String = "",channelId: String = "", signalIndex: Int = 0, analyzeSignal: (Signal<A, B, C, D>) -> Unit) {
            //get the transmission Line
            val transmissionChannel: TransmissionChannel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            transmissionChannel?.let { activeLine ->
                //clear the job if exist
                sessions[sessionId]?.cancel().also { printDuplicateSession(sessionId) }
                //start the new session of the job
                sessions[sessionId] =deepSpaceCarrier.launch {
                    activeLine.channel.collect { signals ->
                        signalAnalyzer(signals, signalIndex, analyzeSignal)
                    }
                }
            }
        }

        fun receiveAll(sessionId: String = "",channelId: String = "", analyzeSignals: (List<Signal<A, B, C, D>>) -> Unit) {
            //get the transmission Line
            val transmissionChannel: TransmissionChannel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            transmissionChannel?.let { activeLine ->
                //clear the job if exist
                sessions[sessionId]?.cancel().also { printDuplicateSession(sessionId) }
                //start the new session of the job
                sessions[sessionId] =deepSpaceCarrier.launch {
                    activeLine.channel.collect { signals ->
                        analyzeSignals(signals)
                    }
                }
            }
        }

        fun receiveLatestOne(
            sessionId: String = "",
            channelId: String = "",
            signalIndex: Int = 0,
            analyzeSignal: (Signal<A, B, C, D>) -> Unit
        ) {

            //get the transmission Line
            val transmissionChannel: TransmissionChannel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            transmissionChannel?.let { activeLine ->
                //clear the job if exist
                sessions[sessionId]?.cancel().also { printDuplicateSession(sessionId) }
                //start the new session of the job
                sessions[sessionId] =deepSpaceCarrier.launch {
                    activeLine.channel.collectLatest { signals ->
                        signalAnalyzer(signals, signalIndex, analyzeSignal)
                    }
                }
            }
        }

        fun receiveLatestAll(
            sessionId: String = "",channelId: String = "", analyzeSignals: (List<Signal<A, B, C, D>>) -> Unit) {

            //get the transmission Line
            val transmissionChannel: TransmissionChannel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            transmissionChannel?.let { activeLine ->
                //clear the job if exist
                sessions[sessionId]?.cancel().also { printDuplicateSession(sessionId) }
                //start the new session of the job
                sessions[sessionId] =deepSpaceCarrier.launch {
                    activeLine.channel.collectLatest { signals ->
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

    private fun <A, B, C, D> newSharedChannel() = TransmissionChannel<A, B, C, D>(
        id = UUID.randomUUID().toString(),
        owners = emptyList(),
        users = emptyList(),
    )

    private fun printDuplicateSession(sessionId: String){
        Log.i("SFDS","A pre-existent session of id : $sessionId has been deleted to create this new one.")
    }
}