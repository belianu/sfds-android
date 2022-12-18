package dev.meres.signalsfromdeepspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.meres.sfds.SFDS
import dev.meres.sfds.Spaceship
import dev.meres.signalsfromdeepspace.ui.theme.SignalsFromDeepSpaceTheme
import java.sql.Time
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SignalsFromDeepSpaceTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val coroutineScope = rememberCoroutineScope()

                    val genericCommunicationSystem: SFDS<String, String, String, String> = SFDS(coroutineScope)

                    val enterprise: Spaceship =
                        Spaceship(name = "Enterprise", communicationSystem = genericCommunicationSystem)
                    val discovery: Spaceship =
                        Spaceship(name = "Discovery", communicationSystem = genericCommunicationSystem)


                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {

                        EnterpriseCommunicationMonitor(
                            enterprise,
                            modifier = Modifier.weight(0.5f).fillMaxSize().background(Color.DarkGray)
                        )

                        DiscoveryCommunicationMonitor(
                            discovery,
                            modifier = Modifier.weight(0.5f).fillMaxSize().background(Color.Gray)
                        )

                    }

                    enterprise.communicationSystem.Generator().sendOne(
                        signal =
                        SFDS.Signal(
                            sender = "Enterprise - Martin",
                            target = "Discovery - Bob",
                            action = "uppercase",
                            message = "data to analyze"
                        )
                    )
                }
            }
        }
    }
}


@Composable
fun DiscoveryCommunicationMonitor(spaceship: Spaceship, modifier: Modifier) {

    var messageHistory by remember { mutableStateOf("Discovery Message History\n\n") }


    val receiver = spaceship.communicationSystem.Receiver()
    val generator = spaceship.communicationSystem.Generator()

    receiver.receiveOne {

        it.validateAll()?.let { signal ->

            if (signal.target.contains("Discovery")) {

                messageHistory += "\n"

                when (signal.target.split("-")[1].trim()) {

                    "Bob" -> {
                        messageHistory +=
                                SimpleDateFormat("hh:mm:ss.SSS",Locale.getDefault()).format(Date.from(Instant.now())) +
                                "\nBob has received a message from ${signal.sender}\n" +
                                "Content: ${it.message}\n" +
                                "Action required : ${it.action}"

                        if (it.action == "uppercase") {

                            val analyzedMessage: String = signal.message.uppercase()

                            generator.sendOne(
                                signal = SFDS.Signal(
                                    sender = "Discovery - Bob",
                                    target = "Enterprise - Nicolas",
                                    action = "split",
                                    message = analyzedMessage
                                )
                            )
                        }
                    }

                    "Jack" -> {
                        messageHistory += "\n" +
                                SimpleDateFormat("hh:mm:ss.SSS",Locale.getDefault()).format(Date.from(Instant.now())) +
                                "\nJack has received a message from ${signal.sender}\n" +
                                "Content:\n${it.message}\n" +
                                "Action required : ${it.action}"

                        if (signal.action == "invert") {

                            val inverted = signal.message.split(",").reversed()

                            generator.sendOne(
                                signal = SFDS.Signal(
                                    sender = "Discovery - Jack",
                                    target = "Enterprise - Martin",
                                    action = "print:red",
                                    message = inverted.joinToString(",")
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    Text(messageHistory, modifier = modifier)
}


@Composable
fun EnterpriseCommunicationMonitor(spaceship: Spaceship, modifier: Modifier) {

    var messageHistory by remember { mutableStateOf("Enterprise Message History\n\n") }

    var messageColor by remember { mutableStateOf(Color.Black) }


    LaunchedEffect(null) {

        val receiver = spaceship.communicationSystem.Receiver()
        val generator = spaceship.communicationSystem.Generator()

        receiver.receiveOne {

            it.validateAll()?.let { signal ->

                if (signal.target.contains("Enterprise")) {
                    when (signal.target.split("-")[1].trim()) {

                        "Martin" -> {
                            messageHistory += "\n\n" +
                                    SimpleDateFormat("hh:mm:ss.SSS",Locale.getDefault()).format(Date.from(Instant.now())) +
                                    "\nMartin has received a message from ${signal.sender}\n" +
                                    "Content:\n${it.message}\n" +
                                    "Action required : ${it.action}"

                            if (signal.action.contains("print")) {

                                when (signal.action.split(":")[1]) {

                                    "red" -> {
                                        messageColor = Color.Red
                                    }

                                }

                                messageHistory += "\n" + signal.message

                            }
                        }

                        "Nicolas" -> {
                            messageHistory +=
                                    SimpleDateFormat("hh:mm:ss.SSS",Locale.getDefault()).format(Date.from(Instant.now())) +
                                    "\nNicolas has received a message from ${signal.sender}\n" +
                                    "Content:\n${it.message}\n" +
                                    "Action required : ${it.action}"

                            if (signal.action == "split") {
                                val splitted = signal.message.split(" ")
                                    .map { part -> part.trim() }

                                generator.sendOne(
                                    signal = SFDS.Signal(
                                        sender = "Enterprise - Nicolas",
                                        target = "Discovery - Jack",
                                        action = "invert",
                                        message = splitted.joinToString(separator = ",")
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Text(messageHistory, modifier = modifier, color = messageColor)

}