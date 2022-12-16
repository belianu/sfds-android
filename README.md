# Signals From Deep Space
Wrapper for the use of sharedflow types in view of a communication system through kotlin coroutines for android.

Development in progress, updates coming soon.
Suggestions, opinions, pull requests, etc. are welcome.

## Raw and quick example in Android Jetpack Compose, better contextualized examples will follow shortly...

Generator in @Composable function or in a View Model
```
 SFDSComSysOperator.Generator().sendSome(
                            lineId = "signComponentLine",
                            signals = listOf(
                                //close the editor mode
                                Signal(
                                    target = "editorEnabled",
                                    message = false.toString()
                                ),
                                //get the bitmap from the draw controller
                                Signal(
                                    target = "drawController",
                                    action = "get"
                                )
                            )
                        )
```

Listener in @Composable function
```
SFDSComSysRec?.receiveAll(lineId = "signComponentLine") { signals: List<SFDSComSys.Signal<String, String, String>> ->

        signals.forEach { signal ->

            when (signal.target) {
                //change editor flag
                "editorEnabled" -> editorEnabled = signal.message?.toBoolean() ?: false
                "lastSing" -> {
                    //set the last sign retrieved
                    lastSign = signal.message ?: ""
                }
                "drawController" -> when (signal.action) {
                    //get the bitmap
                    "get" -> onGetBitmap(drawController.getDrawBoxBitmap())
                }
            }
        }
    }
```
