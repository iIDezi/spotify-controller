package com.spotifycontroller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpotifyOverlayLayoutTest
{
	@Test
	public void sizesOverlayForVisibleSections()
	{
		assertEquals(152, SpotifyOverlay.baseHeightFor(true, true));
		assertEquals(111, SpotifyOverlay.baseHeightFor(false, true));
		assertEquals(115, SpotifyOverlay.baseHeightFor(true, false));
		assertEquals(78, SpotifyOverlay.baseHeightFor(false, false));
		assertEquals(210, SpotifyOverlay.baseHeightFor(true, true, true));
		assertEquals(169, SpotifyOverlay.baseHeightFor(false, true, true));
		assertEquals(173, SpotifyOverlay.baseHeightFor(true, false, true));
		assertEquals(136, SpotifyOverlay.baseHeightFor(false, false, true));
		assertEquals(176, SpotifyOverlay.baseHeightFor(true, true, 1));
		assertEquals(244, SpotifyOverlay.baseHeightFor(true, true, 5));
		assertEquals(29, SpotifyOverlay.baseHeightFor(false, false, 0, true));
		assertEquals(104, SpotifyOverlay.baseHeightFor(false, false, 4, true));
		assertEquals(278, SpotifyOverlay.baseHeightFor(true, true, 7));
		assertEquals(155, SpotifyOverlay.baseHeightFor(false, false, 7, true, true));
		assertEquals(131, SpotifyOverlay.baseHeightFor(false, false, 7, true, false));
		assertEquals(21, SpotifyOverlay.playbackLabelBaselineFor(true));
		assertEquals(45, SpotifyOverlay.lyricsFirstBaselineFor(false, false, true));
		assertEquals(21, SpotifyOverlay.lyricsFirstBaselineFor(false, false, true, false));

		assertEquals(34, SpotifyOverlay.baseHeightFor(
			false, false, 0, false, true, false, false));
		assertEquals(34, SpotifyOverlay.baseHeightFor(
			false, false, 0, false, false, true, false));
		assertEquals(54, SpotifyOverlay.baseHeightFor(
			false, false, 0, false, false, false, true));
		assertEquals(146, SpotifyOverlay.baseHeightFor(
			false, false, 5, false, true, true, true));
		assertEquals(23, SpotifyOverlay.titleBaselineFor(false));
		assertEquals(23, SpotifyOverlay.artistBaselineFor(false, false));
		assertEquals(5, SpotifyOverlay.artworkYFor(false));
		assertEquals(70, SpotifyOverlay.lyricsFirstBaselineFor(
			false, false, false, true, true, true));
	}

	@Test
	public void usesExtraHorizontalSpaceWithoutStretchingTextGlyphs()
	{
		assertEquals(220, SpotifyOverlay.layoutWidthFor(220, 152, 152));
		assertEquals(220, SpotifyOverlay.layoutWidthFor(440, 304, 152));
		assertEquals(440, SpotifyOverlay.layoutWidthFor(440, 152, 152));
		assertEquals(880, SpotifyOverlay.layoutWidthFor(880, 152, 152));
		assertEquals(116, SpotifyOverlay.layoutWidthFor(232, 108, 116, 54));
	}

	@Test
	public void sizesCompactArtworkLayoutsToTheirNaturalWidth()
	{
		assertEquals(58, SpotifyOverlay.naturalBaseWidthFor(
			false, false, 0, false, false, false, true,
			0, 45, 0, 0));
		assertEquals(113, SpotifyOverlay.naturalBaseWidthFor(
			false, false, 0, true, false, false, true,
			52, 45, 0, 0));
		assertEquals(116, SpotifyOverlay.naturalBaseWidthFor(
			false, false, 0, false, true, false, true,
			0, 45, 50, 0));
		assertEquals(100, SpotifyOverlay.naturalBaseWidthFor(
			false, false, 0, false, false, true, true,
			0, 45, 0, 34));
		assertEquals(188, SpotifyOverlay.naturalBaseWidthFor(
			true, false, 0, false, false, false, false,
			0, 45, 0, 0));
		assertEquals(220, SpotifyOverlay.naturalBaseWidthFor(
			false, true, 0, false, false, false, false,
			0, 45, 0, 0));
		assertEquals(220, SpotifyOverlay.naturalBaseWidthFor(
			false, false, 0, false, true, false, true,
			0, 45, 500, 0));
	}

}
