# Arcane Sound Meter – Decibel Level Detector

This project demonstrates how to build a **real-time sound level meter** in  
**Jetpack Compose (Material 3)** using the device’s microphone.

It uses `AudioRecord` to capture raw PCM audio, converts amplitude to  
**decibel (dB)** values, and visualizes the noise level with a dynamic sound bar  
and a warning indicator.

---

## Features

- **Microphone-based Amplitude Detection**
  - Captures raw audio using `AudioRecord`.
  - Computes RMS amplitude from PCM samples.
  - Converts amplitude to approximate **decibel (dB)** values.

- **Visual Sound Meter**
  - Animated colored meter bar that expands with noise level.
  - Color intensity changes:
    - Low: Teal  
    - Medium: Yellow  
    - High: Red

- **Noise Threshold Warning**
  - Alerts the user when sound exceeds the configured threshold (default 80 dB).
  - Displays a bright warning banner to notify high noise environments.

- **Smooth UI Animations**
  - Uses Compose animation for meter movement and color transitions.
  - Stylish dark-themed interface for a clean sound-lab aesthetic.

---
