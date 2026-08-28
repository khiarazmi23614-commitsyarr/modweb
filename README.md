# ModWeb — Embedded Chromium Browser for Minecraft

Fabric client mod for Minecraft 1.20.1 that embeds a real Chromium browser through MCEF.

## Features

- **F9** opens/closes the browser.
- **Esc** closes the browser.
- Minecraft world continues running because the screen does not pause the game.
- Real Chromium rendering via MCEF: modern HTML/CSS/JavaScript and websites such as Google and YouTube can be loaded.
- Address bar accepts URLs and Google searches.
- Chrome-inspired dark title bar with minimize/maximize/close controls.
- Browser window can be dragged.
- Position, browser resolution, keybind and mute preference are stored in `config/modweb.json`.
- Browser texture is only drawn while the browser screen is active.

## Important: MCEF dependency

This project intentionally does **not** bundle Chromium/CEF native binaries into the mod JAR. It depends on MCEF, which supplies the embedded Chromium runtime and downloads the required native files. The MCEF project documents that its runtime supports Windows 10/11, macOS 11+ and GNU/Linux glibc 2.31+ and does not support Android.

Install the matching **MCEF Fabric** release alongside ModWeb.

## Build

```bash
gradle build
```

The GitHub Actions workflow also builds the project automatically and uploads the resulting JAR as an artifact.

## Configuration

After first launch, edit `config/modweb.json`:

```json
{
  "keybind": "F9",
  "width": 1100,
  "height": 700,
  "x": 40,
  "y": 40,
  "muteAudio": false
}
```

`muteAudio` is reserved for the browser audio integration layer; MCEF/CEF audio is otherwise allowed to play through the normal game audio output.
