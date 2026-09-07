# Changelog

## 1.24.2

- Added section-aware horizontal sizing to remove the unused right-side space from compact overlay layouts.
- Artwork-only now wraps the cover at a 58-pixel base width.
- Artwork with Source, song title, or artist now measures the visible text and sizes the panel to that content.
- Progress, lyrics, and long text still use the normal 220-pixel width, while automatic width changes preserve the user's current overlay scale.
- Initialized layout state from saved overlay settings so a previously wide panel cannot affect the first compact render after startup.

## 1.24.1

- Replaced fixed vertical overlay coordinates with section-aware layout sizing.
- Disabling **Source** now removes its full header space and shifts every remaining section upward.
- Song-title-only and artist-only overlays now collapse to a single compact text row.
- Album-artwork-only overlays now wrap tightly around the artwork.
- Progress, controls, and lyrics are positioned immediately after whichever details remain visible.
- Dynamic resizing preserves the user's current RuneLite overlay scale.

## 1.24.0

- Added a **Source** overlay toggle to both RuneLite configuration and the sidebar Settings tab.
- Source hides or shows the Spotify/Windows Media label together with Playing/Paused.
- Made the master **In-game overlay** switch disable every content section when turned off.
- Turning on any content section now enables the master overlay automatically; turning off the last section disables it.
- Enabling an otherwise empty overlay turns Source on so a blank overlay cannot be displayed.
- Lyrics-only layouts now move lyrics into the released top space when Source is hidden.
- Increased the adjustable lyric limit from five to seven lines.

## 1.23.1

- Removed the empty title-and-artist area when every overlay section except lyrics is disabled.
- In the compact lyrics-only layout, the Spotify source label and Playing/Paused state now share the top row.
- Lyrics begin immediately below the compact header, and the overlay automatically shrinks while preserving the user's scale.
- The normal overlay layout remains unchanged whenever title, artist, artwork, progress, or playback controls are visible.

## 1.23.0

- Added separate **Song title** and **Artist** visibility settings for the in-game overlay.
- Added matching Song title and Artist toggles to the sidebar Settings tab.
- Both details remain visible by default, and changing either control is saved through RuneLite configuration.

## 1.22.2

- Fixed emoji and other unsupported Unicode characters appearing as square replacement glyphs in playback titles.
- Added emoji-capable font fallback to the in-game title and artist, sidebar playback text, queue rows, and Search results while retaining RuneLite fonts for supported characters.
- Made width measurement and truncation Unicode-aware so an emoji surrogate pair cannot be cut in half.

## 1.22.1

- Added album-cover thumbnails beside every Spotify Search result.
- Uses the cover-art URLs already included in Spotify's search response rather than making another Spotify API request.
- Reuses the existing bounded artwork cache and shows a music-note placeholder while artwork is loading or unavailable.
- Increased search-row height slightly and moved song, artist, and album text beside the thumbnail.

## 1.22.0

- Added a dedicated **Search** sidebar tab, separate from Playback and Settings.
- Searches Spotify's track catalog only when the user submits a query and shows up to ten results.
- Displays each result's song, artist, and album, with Enter-button, Search-button, double-click, and **Play selected** interactions.
- Starts the selected Spotify track on the active Spotify device and refreshes playback and queue state afterward.
- Reuses the existing Spotify authorization scopes and respects both ordinary rate-limit cooldowns and Development Mode quota pauses.
- Does not restore Smart Mix or add any automatic search, recommendation, or queue-cleanup behavior.

## 1.21.0

- Added an opt-in **Overlay lyrics** setting backed by LRCLIB's anonymous HTTPS API.
- Shows only a compact window around the current lyric and highlights the current line with the overlay accent color.
- Added an adjustable **Lyric lines** limit from one to five, defaulting to three.
- Added matching Lyrics and plus/minus line-count controls to the sidebar Settings tab.
- Uses synced lyrics when available and falls back to evenly timed plain lyrics.
- Fetches lyrics once per track, caches recent results, and never adds requests to the Spotify polling loop.
- Keeps the lyric panel hidden when lyrics are disabled or unavailable.

## 1.20.3

- Distinguished Spotify Development Mode quota exhaustion from the ordinary rolling rate limit.
- Pauses automatic Spotify API requests after a `QUOTA_EXCEEDED` response instead of retrying every ten seconds.
- The sidebar Refresh button now performs one deliberate API retry after quota recovery.
- Reduced background playback polling to a minimum ten-second interval.
- Refreshes the queue when the playing item changes and at most once every thirty seconds instead of on every playback check.
- Reuses the currently-playing fallback for one minute after it succeeds, avoiding repeated failed playback-state requests.
- Keeps a playback fallback request marked in flight until it completes, preventing overlapping recovery requests.

## 1.20.2

- Added a currently-playing fallback when Spotify's full playback-state request returns no content.
- Requests the queue after fallback playback is recovered.
- Displays the latest Spotify API status in the sidebar connection card instead of hiding playback and queue failures.
- Reports queue request errors through the same visible status.
- Clarified that settings require the Spotify Client ID, not the Client Secret.

## 1.20.0

- Removed Smart Mix, its third shuffle state, recommendation/search requests, and sparkle icon.
- Restored the sidebar Shuffle control to the ordinary two-state Off/On behavior.
- Removed Smart Mix queue additions and the automatic queue-rebuild cleanup path.
- Restored queue parsing and display state to the original ten-item limit.
- Kept the existing manual double-click selection and drag-to-reorder queue controls unchanged.

## 1.19.4

- Smart Mix now records the exact tracks it successfully adds.
- Turning Smart Mix off removes those tracks and rebuilds the remaining queue from the current song.
- Preserves the estimated current position and restores the paused state when playback was paused.
- Retains up to fifty returned queue entries internally for cleanup while the sidebar continues to show ten.
- If queue restoration fails, Smart Mix remains selected so turning it off can be retried.
- Added queue-filtering, URI-list restoration, and expanded queue-retention tests.

## 1.19.3

- Replaced the Smart Mix shuffle-arrow symbol with a dedicated three-sparkle logo.
- Ordinary Shuffle retains the crossed-arrow logo, so the two active modes are visibly distinct without hovering.
- Added a rendering regression test that confirms Shuffle and Smart Mix produce different icons.

## 1.19.2

- Fixed Smart Mix immediately disappearing when Spotify rejected the deprecated Recommendations request.
- Smart Mix now falls back to Spotify track search using the current artist or title.
- Smart Mix remains selected with its distinct magic-shuffle logo after the request finishes.
- If neither source returns tracks, the tooltip reports that recommendations are unavailable and the next click turns Smart Mix off.

## 1.19.1

- Fixed Shuffle remaining active when Spotify rejected a Smart Mix request.
- A failed Smart Mix attempt now returns the control to Off instead of repeatedly falling back to Shuffle.
- Made Off visually distinct with a muted standard Shuffle icon and inactive border.
- Added a separate accented magic-shuffle logo with diamond sparkles for Smart Mix.

## 1.19.0

- Changed the sidebar shuffle button to cycle through Off, Shuffle, Smart Mix, and back to Off.
- Added an API-based Smart Mix that seeds recommendations from the current song and queues up to five non-duplicate tracks.
- Added a sparkle indicator and loading state for Smart Mix.
- Handles Spotify developer applications where the deprecated Recommendations endpoint is unavailable.
- Added parser, state-cycle, duplicate-filtering, and track-URI tests.

## 1.18.0

- Split Spotify Controller into its own RuneLite project and development client.
- Added optional album or episode artwork to the in-game overlay.
- Added an Album Artwork toggle to the sidebar Settings tab and RuneLite configuration.
- Replaced the sidebar navigation logo with a green music-note icon.
- Preserved separate overlay, sidebar, progress, background, and playback-button colors.
- Removed the PowerShell/native Windows-media fallback from the Plugin Hub candidate.
- Removed the Player Indicators Plus code, configuration, and launcher from this project.
- Removed the Lombok build dependency.
# Spotify Controller 1.25.0

- Kept the Spotify Controller name for the Spotify-only Plugin Hub build.
- Removed the beta notice, Discord feedback button, and Discord link.
- Kept the Plugin Hub edition Spotify-only; Windows media-session support remains in the separate local build.
- Updated Plugin Hub metadata and authorship to iiDezi.
