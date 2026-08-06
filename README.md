# MaxReader - RSVP EPUB Speed Reader

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

## Build

1. Open in Android Studio (Hedgehog or newer)
2. Sync Gradle
3. Run on device/emulator (API 26+)

## Architecture

```
app/
├── epub/           # EPUB parsing (epublib + Jsoup)
├── model/          # Data classes (BookData, RsvpWord, etc.)
├── rsvp/           # RSVP playback engine (coroutine-based)
├── settings/       # DataStore preferences
├── ui/
│   ├── components/ # RsvpWordDisplay, ContextDisplay
│   ├── screens/    # HomeScreen, ReaderScreen, SettingsScreen
│   └── theme/      # Dark theme colors
└── viewmodel/      # ReaderViewModel
```

## Controls

- **Tap screen**: Play / Pause
- **+/- buttons**: Adjust WPM by 25
- **⏪ / ⏩**: Skip 10 words back/forward
- **Settings**: All timing & display options
