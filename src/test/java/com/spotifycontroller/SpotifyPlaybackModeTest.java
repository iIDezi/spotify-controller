package com.spotifycontroller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpotifyPlaybackModeTest
{
	@Test
	public void cyclesSpotifyRepeatModes()
	{
		assertEquals("context", SpotifyControllerPlugin.nextRepeatMode("off"));
		assertEquals("track", SpotifyControllerPlugin.nextRepeatMode("context"));
		assertEquals("off", SpotifyControllerPlugin.nextRepeatMode("track"));
		assertEquals("context", SpotifyControllerPlugin.nextRepeatMode("unknown"));
	}

	@Test
	public void onlyShowsModeIconsForPlayingSpotify()
	{
		SpotifyPlaybackState playing = SpotifyPlaybackState.of(
			true, "track", "Song", "Artist", "Album", "Hidden device", 1_000, 180_000);
		SpotifyPlaybackState paused = playing.withPlaying(false);

		assertTrue(SpotifyPanel.shouldShowSpotifyModeControls(false, playing));
		assertFalse(SpotifyPanel.shouldShowSpotifyModeControls(true, playing));
		assertFalse(SpotifyPanel.shouldShowSpotifyModeControls(false, paused));
		assertFalse(SpotifyPanel.shouldShowSpotifyModeControls(false, SpotifyPlaybackState.idle()));
	}
}
