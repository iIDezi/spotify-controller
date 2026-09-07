package com.spotifycontroller;

import java.awt.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpotifyThemeConfigTest
{
	@Test
	public void defaultsBothIndependentAccentsToSpotifyGreen()
	{
		SpotifyControllerConfig config = new SpotifyControllerConfig() { };
		Color spotifyGreen = new Color(29, 185, 84);
		assertEquals(spotifyGreen, config.overlayAccentColor());
		assertEquals(spotifyGreen, config.sidebarAccentColor());
		assertEquals(new Color(118, 137, 148), config.overlayProgressTrackColor());
		assertEquals(new Color(238, 238, 238), config.overlayPlaybackProgressColor());
		assertEquals(new Color(14, 14, 14, 118), config.overlayBackgroundColor());
		assertEquals(new Color(238, 238, 238), config.overlayPlaybackButtonColor());
		assertTrue(config.showOverlaySource());
		assertTrue(config.showOverlayTitle());
		assertTrue(config.showOverlayArtist());
		assertTrue(config.showOverlayArtwork());
		assertFalse(config.showOverlayLyrics());
		assertEquals(3, config.overlayLyricsLines());
		assertEquals(7, SpotifyControllerConfig.MAX_OVERLAY_LYRIC_LINES);
		assertFalse(SpotifyControllerPlugin.hasVisibleOverlaySection(
			false, false, false, false, false, false, false));
		assertTrue(SpotifyControllerPlugin.hasVisibleOverlaySection(
			true, false, false, false, false, false, false));
		assertTrue(SpotifyControllerPlugin.hasVisibleOverlaySection(
			false, false, false, false, false, false, true));
	}

	@Test
	public void buildsSidebarWithIndependentThemeControls()
	{
		SpotifyPanel panel = new SpotifyPanel(null, new SpotifyControllerConfig() { });
		panel.refreshTheme();
		panel.stop();
	}
}
