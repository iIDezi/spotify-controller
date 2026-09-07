package com.spotifycontroller;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(SpotifyControllerConfig.GROUP)
public interface SpotifyControllerConfig extends Config
{
	String GROUP = "spotify-controller";
	int MAX_OVERLAY_LYRIC_LINES = 7;

	@ConfigItem(
		keyName = "enableSpotifyAccess",
		name = "Enable Spotify access",
		description = "Connect to Spotify's Web API when a Client ID is entered",
		position = 0,
		warning = "This feature sends your IP address, Spotify authorization data, and playback-control requests to Spotify, a third-party service not controlled or verified by RuneLite developers"
	)
	default boolean enableSpotifyAccess()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spotifyClientId",
		name = "Spotify Client ID",
		description = "Paste the Spotify app Client ID, not the Client Secret",
		position = 1
	)
	default String spotifyClientId()
	{
		return "";
	}

	@Range(min = 10, max = 30)
	@ConfigItem(
		keyName = "refreshSeconds",
		name = "Refresh interval",
		description = "Seconds between Spotify Web API checks (minimum 10 seconds to protect the API quota)",
		position = 2
	)
	default int refreshSeconds()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "showGameOverlay",
		name = "Show game overlay",
		description = "Display the current Spotify song over the game screen",
		position = 3
	)
	default boolean showGameOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlaySource",
		name = "Source",
		description = "Show the Spotify source and Playing/Paused state in the in-game overlay",
		position = 4
	)
	default boolean showOverlaySource()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayTitle",
		name = "Song title",
		description = "Show the current song or episode title in the in-game overlay",
		position = 5
	)
	default boolean showOverlayTitle()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayArtist",
		name = "Artist",
		description = "Show the current artist or podcast name in the in-game overlay",
		position = 6
	)
	default boolean showOverlayArtist()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayControls",
		name = "Playback Controls",
		description = "Show previous, play/pause, and next buttons in the in-game overlay",
		position = 7
	)
	default boolean showOverlayControls()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayProgress",
		name = "Playback Progress",
		description = "Show the seek bar and elapsed/remaining time in the in-game overlay",
		position = 8
	)
	default boolean showOverlayProgress()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayArtwork",
		name = "Album artwork",
		description = "Show album or episode artwork in the in-game overlay",
		position = 9
	)
	default boolean showOverlayArtwork()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayLyrics",
		name = "Overlay lyrics",
		description = "Show up to seven lyric lines in the in-game overlay; sends track metadata to LRCLIB",
		position = 10,
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean showOverlayLyrics()
	{
		return false;
	}

	@Range(min = 1, max = MAX_OVERLAY_LYRIC_LINES)
	@ConfigItem(
		keyName = "overlayLyricsLines",
		name = "Lyric lines",
		description = "Maximum number of lyric lines shown in the in-game overlay",
		position = 11
	)
	default int overlayLyricsLines()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "overlayAccentColor",
		name = "In-game overlay accent",
		description = "Accent color used by the in-game Spotify overlay",
		position = 12
	)
	default Color overlayAccentColor()
	{
		return new Color(29, 185, 84);
	}

	@ConfigItem(
		keyName = "sidebarAccentColor",
		name = "Sidebar UI accent",
		description = "Accent color used by the Spotify sidebar UI",
		position = 13
	)
	default Color sidebarAccentColor()
	{
		return new Color(29, 185, 84);
	}

	@ConfigItem(
		keyName = "overlayProgressTrackColor",
		name = "Progress bar track",
		description = "Background track color used by the in-game overlay progress bar",
		position = 14
	)
	default Color overlayProgressTrackColor()
	{
		return new Color(118, 137, 148);
	}

	@ConfigItem(
		keyName = "overlayPlaybackProgressColor",
		name = "Playback progress",
		description = "Played portion and position-marker color used by the in-game overlay progress bar",
		position = 15
	)
	default Color overlayPlaybackProgressColor()
	{
		return new Color(238, 238, 238);
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayBackgroundColor",
		name = "Overlay background",
		description = "Background color and transparency used by the in-game Spotify overlay",
		position = 16
	)
	default Color overlayBackgroundColor()
	{
		return new Color(14, 14, 14, 118);
	}

	@ConfigItem(
		keyName = "overlayPlaybackButtonColor",
		name = "Playback button icons",
		description = "Icon color used by the previous, play/pause, and next buttons in the in-game overlay",
		position = 17
	)
	default Color overlayPlaybackButtonColor()
	{
		return new Color(238, 238, 238);
	}

}
