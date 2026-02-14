## NfcReaderKMP

This project demonstrates a simple NFC reader implementation for both Android and iOS using Kotlin Multiplatform. The NFC reader is implemented as a reusable Composable component.

### Features

- **Cross-Platform NFC Reading**: Read NFC tags on both Android and iOS with a single codebase.
- **Customizable UI**: Configure titles, subtitles, and button text using `NfcConfig`.
- **Dismissal Control**: Configure whether the scan sheet can be dismissed by clicking outside or pressing the back button.
- **State-Driven**: Uses a `StateFlow` to represent the scanning state (`Initial`, `Success`, `Error`).
- **Tag Information**: Extracts the tag's serial number, payload, and a list of supported technologies.

### How to Use

1.  **In your Composable function**, create an instance of the `NfcReadManagerState`:

    ```kotlin
    val nfcManager = rememberNfcReadManagerState(
        config = NfcConfig(
            titleMessage = "Ready to Scan",
            subtitleMessage = "Hold your tag near the back of the device.",
            buttonText = "Cancel",
            shouldDismissBottomSheetOnClickOutside = false,
            shouldDismissBottomSheetOnBackPress = false
        )
    )
    ```

2. **Collect the state** from the `nfcManager`:

    ```kotlin
    val result by nfcManager.nfcResult.collectAsState()
    ```

3. **Handle the different states** in your UI:

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

4. **Start scanning** when needed:

    ```kotlin
    Button(onClick = { nfcManager.startScanning() }) {
        Text("Start Scanning")
    }
    ```

### Build and Run

- **Android**: `./gradlew :composeApp:assembleDebug`
- **iOS**: Open the `iosApp` directory in Xcode and run.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
