# Spotify Controller

Spotify Controller puts Spotify controls inside RuneLite, with a sidebar for
playback, search, queue management, and settings, plus an optional in-game
overlay.

## Features

- Previous, play/pause, next, seek, and shuffle controls.
- Spotify search and a draggable view of the upcoming queue.
- Optional album artwork and synced lyrics.
- A resizable in-game overlay with configurable text, controls, colors, and
  transparency.
- A three-tab RuneLite sidebar for playback, search, and settings.

## Spotify setup

1. Create an app in the Spotify developer dashboard.
2. Add `http://127.0.0.1:43821/callback` as a redirect URI.
3. Paste the app's Client ID into the plugin settings.
4. Enable Spotify access and select **Connect Spotify** in the sidebar.

Spotify Premium is required for the Web API's playback controls. The plugin
slows or pauses requests when Spotify reports a rate or Development Mode quota
limit.

## Lyrics

Lyrics are optional and off by default. When enabled, the plugin sends the
current track title, artist, album, duration, and your IP address to LRCLIB. One
lookup is made per track and recent results are cached.

## Privacy

Spotify and lyrics access are separate opt-in settings. Spotify Controller does
not send RuneScape account, player, chat, or game-state data, and it does not
record or upload audio.
