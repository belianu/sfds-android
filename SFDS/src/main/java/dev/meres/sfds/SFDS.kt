package dev.meres.sfds

import android.util.Log
import dev.meres.sfds.const.ConnectionType
import dev.meres.sfds.const.TransmissionBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

//Signals From Deep Space
class SFDS<A, B, C, D>(
    val deepSpaceCarrier: CoroutineScope,
    var connectionType: ConnectionType = ConnectionType.BROADCAST,
    var transmissionBehavior: TransmissionBehavior = TransmissionBehavior.SHARED,
    var communicators: MutableList<Communicator> = mutableListOf(),
) {

    //region variables
    private val addressBook: HashMap<String, Channel<A, B, C, D>> = hashMapOf()
    var sharedTransmission: Boolean = false
    //endregion variables


    init {

        when (connectionType) {

            ConnectionType.BROADCAST -> {
                //link all the users

                when (transmissionBehavior) {

                    TransmissionBehavior.SHARED -> {
                        sharedTransmission = true

                        //create one channel and register all the user in the channel
                        UUID.randomUUID().toString().also { channelId ->
                            addressBook[channelId] = Channel(
                                id = channelId,
                                owner = null,
                                registeredUsers = communicators.map { communicator ->
                                    Communicator.User(
                                        id = communicator.id
                                    )
                                }.toMutableList()
                            )
                        }

                    }

                    TransmissionBehavior.FULL_DUPLEX -> {
                        sharedTransmission = false

                        //create one channel 1 to all for each communicator
                        communicators.forEach { communicator ->
                            //create list of users for this channel
                            val users = communicators.filter { item -> item.id != communicator.id }
                            //create the channel
                            UUID.randomUUID().toString().also { channelId ->
                                addressBook[channelId] = Channel(
                                    id = channelId,
                                    owner = Communicator.Owner(id = communicator.id),
                                    registeredUsers = users.map { communicator -> Communicator.User(id = communicator.id) }
                                        .toMutableList()
                                )
                            }
                        }

                    }

                    TransmissionBehavior.CLIENT_SERVER -> {
                        sharedTransmission = false

                        //create one channel for the owner 1 to all
                        val owner = communicators.filterIsInstance(Communicator.Owner::class.java).also {
                            Log.w(
                                "SFDS",
                                "Communicators list for Client-Server Broadcast transmission contains multiple owners. Only one owner is admitted for this type of transmission.\nOWNERS : ${it.joinToString()}"
                            )
                        }.first()

                        val users = communicators.filter { item -> item.id != owner.id }

                        //create the channel
                        UUID.randomUUID().toString().also { channelId ->
                            addressBook[channelId] = Channel(
                                id = channelId,
                                owner = owner,
                                registeredUsers = users.map { communicator -> Communicator.User(id = communicator.id) }
                                    .toMutableList()
                            )
                        }
                    }
                }
            }

            ConnectionType.MULTICAST -> {

                when (transmissionBehavior) {

                    TransmissionBehavior.SHARED -> {
                        //no owners
                        sharedTransmission = true


                    }

                    TransmissionBehavior.FULL_DUPLEX -> {

                    }

                    TransmissionBehavior.CLIENT_SERVER -> {

                    }

                }
            }

            ConnectionType.UNICAST -> {

                when (transmissionBehavior) {

                    TransmissionBehavior.SHARED -> {
                        //no owners
                        sharedTransmission = true
                    }

                    TransmissionBehavior.FULL_DUPLEX -> {

                    }

                    TransmissionBehavior.CLIENT_SERVER -> {

                    }

                }
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
            val channel: Channel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            channel?.let { activeLine ->
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
            val channel: Channel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            channel?.let { activeLine ->
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
            val channel: Channel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            channel?.let { activeLine ->
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
            val channel: Channel<A, B, C, D>? = channelFinder(channelId)

            //check the transmission Line
            channel?.let { activeLine ->
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

    private fun channelFinder(channelId: String): Channel<A, B, C, D>? {
        //get the line
        val channel: Channel<A, B, C, D>? = if (channelId.isNotBlank()) {
            addressBook[channelId]
        } else {
            addressBook.entries.firstOrNull()?.value
        }

        if (channel == null) {
            Log.e("SFDS", "Channel $channelId not present in the list of transmission lines.")
        }

        return channel
    }


    fun SFDS<A, B, C, D>.sharedBroadcast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.BROADCAST
        transmissionBehavior = TransmissionBehavior.SHARED
    }

    fun SFDS<A, B, C, D>.sharedMulticast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.MULTICAST
        transmissionBehavior = TransmissionBehavior.SHARED
    }

    fun SFDS<A, B, C, D>.sharedUnicast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.UNICAST
        transmissionBehavior = TransmissionBehavior.SHARED
    }

    fun SFDS<A, B, C, D>.fullduplexBroadcast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.BROADCAST
        transmissionBehavior = TransmissionBehavior.FULL_DUPLEX
    }

    fun SFDS<A, B, C, D>.fullduplexMulticast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.MULTICAST
        transmissionBehavior = TransmissionBehavior.FULL_DUPLEX
    }

    fun SFDS<A, B, C, D>.fullduplexUnicast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.UNICAST
        transmissionBehavior = TransmissionBehavior.FULL_DUPLEX
    }

    fun SFDS<A, B, C, D>.clientserverBroadcast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.BROADCAST
        transmissionBehavior = TransmissionBehavior.CLIENT_SERVER
    }

    fun SFDS<A, B, C, D>.clientserverMulticast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.MULTICAST
        transmissionBehavior = TransmissionBehavior.CLIENT_SERVER
    }

    fun SFDS<A, B, C, D>.clientserverUnicast(): SFDS<A, B, C, D> = this@SFDS.apply {
        connectionType = ConnectionType.UNICAST
        transmissionBehavior = TransmissionBehavior.CLIENT_SERVER
    }

    private fun <A, B, C, D> newSharedChannel() = Channel<A, B, C, D>(
        id = UUID.randomUUID().toString(),
    )

    private fun printDuplicateSession(sessionId: String){
        Log.i("SFDS","A pre-existent session of id : $sessionId has been deleted to create this new one.")
    }
}