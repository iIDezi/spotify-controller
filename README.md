# Media Controller

A standalone RuneLite Plugin Hub candidate for viewing and controlling Spotify from RuneLite. The broader name also covers the separate local Windows build, while this Hub-safe edition intentionally uses Spotify's Web API only.

## Features

- In-game overlay with track title, artist, playback state, progress, seek, previous, play/pause, and next controls. **Source** hides or shows the Spotify label and Playing/Paused state together; Song title and Artist remain independently toggleable through RuneLite configuration or the sidebar Settings tab.
- Optional album or episode artwork in the in-game overlay, controlled by the **Album artwork** setting and the matching toggle in the sidebar Settings tab.
- Optional synced lyrics in a compact one-to-seven-line window, controlled by **Overlay lyrics** and **Lyric lines** in RuneLite or the matching sidebar controls. The default is three lines. In a lyrics-only layout, Source and Playing/Paused share a compact top row when enabled; hiding Source removes that row and moves lyrics into the released space.
- Separate RuneLite color pickers for the overlay accent, sidebar accent, progress track, played progress and marker, overlay background (including alpha), and playback-button icons.
- Three-tab RuneLite sidebar: Playback for current media and queue, Search for finding and starting songs with album-cover thumbnails, and Settings for every overlay visibility control.
- Green music-note navigation icon in the RuneLite sidebar.
- Emoji-aware playback text that retains RuneLite's fonts for supported characters and uses a compatible system fallback for emoji in titles, artists, queue rows, and Search results.
- OAuth authorization-code flow with PKCE. The refresh token is stored in RuneLite's plugin configuration and is never written to the repository.
- Spotify network access is disabled by default and has a third-party-service warning in the RuneLite settings.
- Lyrics access is separately disabled by default and has RuneLite's required third-party-server warning.
- The overlay recalculates both height and width from the sections currently enabled. Source-off, title-only, artist-only, artwork-only, progress-only, controls-only, and lyrics combinations remove unused rows and columns while preserving the user's chosen scale. Artwork with Source, title, or artist uses the measured visible text width; progress, lyrics, and long text retain the normal full width.

## Spotify setup

1. Create a Spotify developer application.
2. Add `http://127.0.0.1:43821/callback` as a redirect URI.
3. Copy the application's Client ID into **Spotify Client ID**.
4. Enable **Spotify access**, then open the plugin sidebar and select **Connect Spotify**.

Spotify's Web API playback-control endpoints require Spotify Premium.

The connection card displays the latest Spotify API status. If the full playback-state endpoint temporarily returns no content, the plugin checks Spotify's currently-playing endpoint before treating playback as inactive.

Spotify API playback checks use a minimum ten-second interval. The queue refreshes when the playing item changes and no more than once every thirty seconds during steady playback. If Spotify reports that the Development Mode quota is exhausted, automatic API requests pause instead of repeatedly retrying; use the sidebar **Refresh** button once after allowing the quota time to recover.

## Song search

Open the sidebar's **Search** tab, enter a song, artist, album, or Spotify search expression, then press Enter or select **Search**. The plugin makes one user-initiated catalog request and displays up to ten matching tracks with an album-cover thumbnail, artist, and album. A music-note placeholder appears while artwork loads or when no cover is available. Select a row and choose **Play selected**, or double-click the row, to start that track on the active Spotify device.

Artwork URLs come from the same Spotify search response, so thumbnails do not require another Spotify Web API catalog request. The plugin downloads the returned images over HTTPS and reuses its bounded in-memory artwork cache.

Starting a search result sends Spotify a one-track URI list. Spotify replaces the current playback context with that selection, so the prior album, playlist, or queue may no longer remain as the active context. Search never runs in the background, does not restore Smart Mix, and respects the same rate-limit and Development Mode quota pause as the playback controls.

## Lyrics

Enable **Overlay lyrics** in RuneLite's plugin configuration or in the sidebar **Settings** tab. Set **Lyric lines** from one to seven with RuneLite's native setting or the sidebar minus/plus buttons. The current line uses the overlay accent; surrounding lines are muted. Long lines are shortened with an ellipsis so they cannot widen the overlay.

The master **In-game overlay** switch and its content switches stay synchronized. Turning the master off turns Source, Song title, Artist, Playback Controls, Playback Progress, Album artwork, and Lyrics off. Turning on any content switch enables the master automatically, while turning off the final content switch disables it. If the master is enabled while every section is off, Source is enabled to avoid showing an empty panel.

Spotify's Web API does not provide lyrics. When this separate option is enabled, the plugin sends the current title, artist, album, and duration to LRCLIB over HTTPS and receives synced or plain lyrics. It performs one lookup per track and caches recent results. No lyrics request is made while the option is off, and the overlay adds no empty lyric area when a match is unavailable.

## Shuffle and queue behavior

The sidebar shuffle button now has only two states: **Off ↔ Shuffle**. Off uses a muted Shuffle symbol and neutral border; Shuffle uses the selected sidebar accent. The plugin does not request recommendations, search for replacement tracks, append songs, or perform automatic queue cleanup when Shuffle changes.

The sidebar displays up to ten upcoming items. Double-clicking an item advances through Spotify's existing queue, while dragging items deliberately rebuilds the visible queue order. These existing manual queue controls are independent of Shuffle.


## Privacy

Spotify access and lyrics access are independent opt-in settings. A submitted search sends the text you entered to Spotify and downloads the returned album-cover images from Spotify's HTTPS image hosts for display. Lyrics lookup sends the current track title, artist, album, duration, and your network IP address to LRCLIB. The plugin does not send RuneScape account, player, chat, or game-state data and does not record or upload audio.


