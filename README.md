# ESPZera Android Application

This Android application is designed to facilitate the collection and visualization of Channel State Information (CSI) data from an ESP32 device. It provides an interactive grid interface to define measurement areas, initiate data collection via UDP broadcast, receive CSI data, and store it locally in an SQLite database. It also includes a feature to view the collected CSI data from a `.db` file.

## Features

* **Interactive Grid Generation**: Dynamically create a measurement grid (e.g., 10x10, 3x5) by specifying width and height.

* **Point Selection**: Select a specific point (cell) on the grid for measurement.

* **UDP Communication (Bidirectional)**:

    * Sends "start" commands to an ESP32 via UDP broadcast (port 8888), along with the desired collection time.

    * Listens for incoming CSI data from the ESP32 via UDP (port 8889).

* **SQLite Database Integration**:

    * Automatically saves "Ambiente" (Environment - grid dimensions), "Ponto" (Point - selected cell coordinates), and "Leitura" (Reading - collection details) information.

    * Stores received CSI data payloads, associated with their respective "Leitura" records.

* **CSI Data Viewing**: Allows opening a `.db` file (e.g., the application's own database file) to view collected CSI data in a list.

## Setup and Installation (Android Studio)

To set up and run this application in Android Studio, follow these steps:

1.  **Clone the Repository**: If your project is hosted on GitHub (e.g., `Nogas/esp`), clone it:

    ```bash
    git clone [https://github.com/Nogas/esp.git](https://github.com/Nogas/esp.git)
    cd esp
    ```

    If you're starting from an existing Android Studio project, open it.

2.  **Open in Android Studio**: Open the project in Android Studio. Ensure Gradle syncs successfully.

3.  **Verify Package Name**: The code provided assumes the package name `com.example.espzera`. If your project's package name is different (e.g., `com.example.esp`), you **must** update the `package` declaration at the top of all `.java` files (`MainActivity.java`, `LeituraActivity.java`, `DatabaseHelper.java`, `Ambiente.java`, `Ponto.java`, `CsiData.java`, `Usuario.java`, `UsuarioAdapter.java`, `UsuarioManagerActivity.java`, `AddEditUserDialogFragment.java`, `CadastrosActivity.java`, `SplashActivity.java`, `ViewDataActivity.java`, `CsiDataAdapter.java`) to match your project's actual package name.

4.  **Add New Files**: Ensure the following files are correctly placed in your project structure:

    * **Java Classes** (in `app/src/main/java/com/example/espzera/`):

        * `Ambiente.java`
        * `Ponto.java`
        * `CsiData.java`
        * `LeituraActivity.java` (updated version)
        * `ViewDataActivity.java` (for viewing CSI data)
        * `CsiDataAdapter.java` (you'll need to create this for `RecyclerView` in `ViewDataActivity`)

    * **Layout XML Files** (in `app/src/main/res/layout/`):

        * `activity_leitura.xml`
        * `activity_view_data.xml` (for `ViewDataActivity`)

    * **Drawable XML Files** (in `app/src/main/res/drawable/`):

        * `rounded_edittext_background.xml`
        * `grid_cell_background.xml`

    * **Colors XML File** (in `app/src/main/res/values/`):

        * `colors.xml` (ensure it contains the Tailwind-inspired color definitions)

5.  **Update Existing Files**: Modify the following existing files with the latest provided code:

    * `MainActivity.java` (to launch `LeituraActivity`)
    * `DatabaseHelper.java` (to include CSI table and methods, and the `onUpgrade` fix)
    * `AndroidManifest.xml` (add `INTERNET` permission and declare `LeituraActivity` and `ViewDataActivity`)

6.  **Crucial: Database Version Increment and Reinstallation**: If you have previously run the app on your device/emulator *before* adding the `csi_data` table to `DatabaseHelper.java`, the SQLite database schema on your device will be outdated. To force the creation of the `csi_data` table:

    * Open `DatabaseHelper.java`.
    * Change the `DATABASE_VERSION` constant to a higher number (e.g., from `1` to `2`):

        ```java
        private static final int DATABASE_VERSION = 2; // Increment this number
        ```

    * **Completely uninstall the application** from your Android device or emulator.
    * Clean and rebuild your project in Android Studio (`Build` > `Clean Project`, then `Build` > `Rebuild Project`).
    * Run the application again. This will trigger the `onUpgrade` method in `DatabaseHelper`, which will drop and recreate all tables, including `csi_data`.

7.  **Network Permissions**: Ensure `android.permission.INTERNET` is present in your `AndroidManifest.xml`:

    ```xml
    <uses-permission android:name="android.permission.INTERNET" />
    ```

## Usage (Android Application)

1.  **Launch the App**: Run the application from Android Studio on your device or emulator.

2.  **Navigate to Readings**: From the main menu, click the "Leituras" (Readings) button. This will open the `LeituraActivity`.

3.  **Generate Grid**:

    * Enter the desired `Largura` (Width) and `Altura` (Height) for your measurement grid (e.g., 10 for 10x10, 3 for 3x5).
    * Click the "Gerar Grade" (Generate Grid) button. The grid will appear as empty squares.

4.  **Select a Point**: Click on any square in the generated grid. The selected square will highlight, and its coordinates will be displayed. The "Iniciar Coleta" (Start Collection) button will become enabled.

5.  **Set Collection Time**: Enter the `Tempo de Coleta (segundos)` (Collection Time in seconds). This value will be sent to the ESP32.

6.  **Start Collection**: Click the "Iniciar Coleta" button.

    * The app will first save the "Ambiente", "Ponto", and "Leitura" details to its local SQLite database.
    * It will then start an internal UDP listener (on port 8889) to receive CSI data from the ESP32.
    * Concurrently, it will send "start,(collection_time_seconds)" messages via UDP broadcast (to 255.255.255.255 on port 8888) once every second for 30 seconds.
    * The "Status da Coleta" will update to show the progress of UDP messages sent and reception of CSI data.

7.  **View Collected CSI Data**:

    * You'll need to implement a way to launch `ViewDataActivity` (e.g., a new button on `MainActivity` or `CadastrosActivity`).
    * In `ViewDataActivity`, click "Open DB" to select your app's database file (usually located in `/data/data/com.example.espzera/databases/esp_app.db` on a rooted device or emulator, or accessible via Device File Explorer in Android Studio).
    * The app will read the `csi_data` table and display the records in a `RecyclerView`.

## ESP32 Integration (Firmware)

For the Android app to function correctly, your ESP32 firmware needs to implement the following:

1.  **WiFi Connection**: Connect to the same WiFi network as your Android device.

2.  **UDP Listener (Command)**:

    * Listen for incoming UDP packets on **port 8888**.
    * Parse the received message (e.g., `"start,5"`).
    * Extract the command (`"start"`) and the `collection_time_seconds` (e.g., `5`).

3.  **CSI Data Collection**:

    * Upon receiving the "start" command, begin collecting CSI data for the specified duration (`collection_time_seconds`).
    * Ensure your ESP32 is configured to capture CSI data (e.g., using the ESP-IDF WiFi CSI API).

4.  **UDP Sender (CSI Data)**:

    * For each CSI data packet collected, send it via UDP to your Android device's IP address on **port 8889**.
    * **Important**: The Android app expects the CSI data to be a CSV string with specific fields, followed by the raw CSI payload. The expected format is:

        ```
        dataHora,cenario,type,seq,mac,rssi,rate,csiPayloadRaw
        ```

        Example:

        ```
        2024-06-18 10:30:00.123,my_scenario,rx_ctl,123,AA:BB:CC:DD:EE:FF,-50,54.0,BASE64_ENCODED_CSI_DATA_HERE
        ```

        * `dataHora`: Timestamp of the CSI capture on ESP32 (e.g., "YYYY-MM-DD HH:MM:SS.SSS")
        * `cenario`: A string describing the scenario (e.g., "walking", "empty_room")
        * `type`: Type of CSI packet (e.g., "rx_ctl", "tx_ctl")
        * `seq`: Sequence number of the CSI packet
        * `mac`: MAC address of the sender/receiver
        * `rssi`: RSSI value
        * `rate`: Data rate
        * `csiPayloadRaw`: The actual CSI data, preferably base64 encoded if it contains binary data, as a string.
