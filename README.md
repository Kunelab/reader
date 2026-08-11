# KuneLab Reader - RSVP EPUB Speed Reader

**English** · [Français](README.fr.md) · [Español](README.es.md) · [简体中文](README.zh-CN.md)

A fast, focused Android EPUB reader using **RSVP** (Rapid Serial Visual Presentation) with an ORP (Optimal Recognition Point) centered bold letter.

## Features

- **RSVP reading**: Words flash one at a time at your chosen speed
- **ORP highlighting**: The optimal recognition point character is bold & red, centered on screen
- **Last word display**: Previous word shown at the top for glance-back
- **Context sentence**: The last N words (configurable) shown below for paragraph context
- **Adjustable WPM**: 50–1500 words per minute, adjustable via slider or +/- buttons
- **Punctuation pauses**: Separate configurable pause (in ms) for:
  - Comma, semicolon, colon
  - Period, exclamation, question mark
  - Paragraph breaks
- **Dark theme**: Eye-friendly dark UI
- **EPUB support**: Opens standard `.epub` files via Android file picker
- **Multilingual UI**: English, French, Spanish and Simplified Chinese

## Build

1. Open in Android Studio (Hedgehog or newer)
2. Sync Gradle
3. Run on device/emulator (API 26+)

## Architecture

```
app/
├── epub/           # EPUB parsing (Jsoup + XmlPullParser)
├── model/          # Data classes (BookData, RsvpWord, etc.)
├── rsvp/           # RSVP playback engine (coroutine-based)
├── settings/       # DataStore preferences
├── ui/
│   ├── components/ # RsvpWordDisplay
│   ├── screens/    # HomeScreen, ReaderScreen, SettingsScreen
│   └── theme/      # Dark, AMOLED, Sepia and Light palettes
└── viewmodel/      # ReaderViewModel
```

## Controls

- **Tap screen**: Play / Pause
- **+/- buttons**: Adjust WPM by 25
- **⏪ / ⏩**: Skip 10 words back/forward
- **Settings**: All timing & display options

## Languages

The UI is translated into English, French, Spanish and Simplified Chinese. The app follows the system language; on Android 13+ you can also give it its own language from _Settings → Apps → KuneLab Reader → Language_.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full text.

Unofficial translations are provided for convenience in [LICENSE.fr](LICENSE.fr), [LICENSE.es](LICENSE.es) and [LICENSE.zh-CN](LICENSE.zh-CN). **Only the English version is legally binding.**

```
Copyright 2026 Maxime Pinard

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
