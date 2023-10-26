# PeriPage Printer Library

The **PeriPage Printer Library** is a utility library designed to facilitate the process of connecting and printing content on PeriPage portable printers via Bluetooth. This library provides easy-to-use functions for converting text and images into a format compatible with PeriPage printers and handling Bluetooth communication.

## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Examples](#examples)
- [Contributing](#contributing)
- [License](#license)

## Introduction

Portable thermal printers, such as the PeriPage printer, have become popular for various applications like label printing, note-taking, and image printing. This library simplifies the process of connecting to a PeriPage printer via Bluetooth and printing text or images. It also provides functions for converting text to a bitmap and printing feeds.

## Features

- **Bluetooth Connection:** Easily connect to a PeriPage printer via Bluetooth.
- **Text-to-Bitmap Conversion:** Convert multiline text into a bitmap with a specified font and size for printing.
- **Image Printing:** Print images on PeriPage printers by converting and sending bitmap data.
- **Feed Printing:** Print a specified number of feed lines, with options for blank or filled lines.

## Installation

To use the PeriPage Printer Library in your Android project, follow these steps:

1. Clone this repository or download the library files.
2. Include the library in your project by adding the necessary source files to your Android Studio project.

## Usage

### Initialization

To use the PeriPage Printer Library, you must first create an instance of the `BluetoothHelper` class, which handles Bluetooth communication with the printer. 

```kotlin
val bluetoothHelper = BluetoothHelper(appActivity, context)
```

### Bluetooth Permissions

The library handles Bluetooth permissions for you. It checks for permission based on your Android version and can request permission if needed. Use the following functions to check and request Bluetooth permissions:

- `checkAndRequestBluetoothPermission()`: Checks and requests Bluetooth permissions as needed.
- `onRequestPermissionsResult()`: Handles the result of the permission request.

### Connecting to the Printer

To connect to the PeriPage printer, use the `connectPrinter()` function. It checks if the printer is already connected and attempts to establish a Bluetooth connection if not.

```kotlin
if (!bluetoothHelper.isPrinterConnected()) {
    if (bluetoothHelper.connectPrinter()) {
        // Connection successful
    } else {
        // Connection failed
    }
} else {
    // Printer is already connected
}
```

### Printing Text

To print text, first convert it to a bitmap using the `textToMultilineBitmap()` function. Then, use the `printImage()` function to send the bitmap data to the printer for printing.

```kotlin
val text = "Hello, PeriPage Printer!"
val fontSize = 20F
val fontId = R.font.courier_new
val bitmap = bluetoothHelper.messageToBitmap(text, fontId, fontSize)
bluetoothHelper.printImage(bitmap)
```

### Printing Feeds

You can use the `printFeed()` function to print a specified number of feed lines. You can choose to print blank or filled lines.

```kotlin
val linesToFeed = 3
val printBlankLines = true
bluetoothHelper.printFeed(linesToFeed, printBlankLines)
```

### Disconnecting

To disconnect from the printer, use the `disconnectPrinter()` function.

```kotlin
bluetoothHelper.disconnectPrinter()
```

## Examples

Here are some usage examples for the PeriPage Printer Library:

```kotlin
// Initialize the BluetoothHelper
val bluetoothHelper = BluetoothHelper(appActivity, context)

// Check and request Bluetooth permissions if necessary
bluetoothHelper.checkAndRequestBluetoothPermission()

// Connect to the PeriPage printer
if (!bluetoothHelper.isPrinterConnected()) {
    if (bluetoothHelper.connectPrinter()) {
        // Connection successful
    } else {
        // Connection failed
    }
} else {
    // Printer is already connected
}

// Print text
val text = "Hello, PeriPage Printer!"
val fontSize = 20F
val fontId = R.font.courier_new
val bitmap = bluetoothHelper.messageToBitmap(text, fontId, fontSize)
bluetoothHelper.printImage(bitmap)

// Print feed lines
val linesToFeed = 3
val printBlankLines = true
bluetoothHelper.printFeed(linesToFeed, printBlankLines)

// Disconnect from the printer
bluetoothHelper.disconnectPrinter()
```

## Contributing

Contributions to this library are welcome. If you have suggestions, feature requests, or find any issues, please open a GitHub issue or submit a pull request.

## License

This PeriPage Printer Library is provided under the [MIT License](LICENSE). You are free to use and modify the code according to the terms of the license.