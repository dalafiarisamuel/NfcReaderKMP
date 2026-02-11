## NfcReaderKMP

This project demonstrates a simple NFC reader implementation for both Android and iOS using Kotlin Multiplatform. The reader is implemented as a reusable Composable component.

### Features

- **Cross-Platform NFC Reading**: Read NFC tags on both Android and iOS with a single codebase.
- **Customizable UI**: Configure the text displayed on the scanning screen using `NfcConfig`.
- **State-Driven**: Uses a `StateFlow` to represent the scanning state (`Initial`, `Success`, `Error`).
- **Tag Information**: Extracts the tag's serial number, payload, and a list of supported technologies.

### How to Use

1.  **Add the `nfcreader` module** to your project.
2.  **In your Composable function**, create an instance of the `NfcReadManagerState`:

    ```kotlin
    val nfcManager = rememberNfcReadManagerState(
        config = NfcConfig(
            readyToScanMessage = "Please tap your card",
            cancelMessage = "Close",
            bringTagCloserMessage = "Move your card closer to the back of the phone"
        )
    )
    ```

3.  **Collect the state** from the `nfcManager`:

    ```kotlin
    val result by nfcManager.nfcResult.collectAsState()
    ```

4.  **Handle the different states** in your UI:

    ```kotlin
    when (val state = result) {
        is NfcReadResult.Success -> {
            // Display the tag data
            Text("Serial Number: ${state.data.serialNumber}")
        }
        is NfcReadResult.Error -> {
            // Show an error message
            Text("Error: ${state.message}")
        }
        NfcReadResult.Initial -> {
            // Show a prompt to start scanning
            Text("Ready to scan tags")
        }
    }
    ```

5.  **Start scanning** when needed:

    ```kotlin
    Button(onClick = { nfcManager.startScanning() }) {
        Text("Start Scanning")
    }
    ```

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
