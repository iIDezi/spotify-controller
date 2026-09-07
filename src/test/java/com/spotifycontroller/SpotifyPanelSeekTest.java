package com.spotifycontroller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpotifyPanelSeekTest
{
	@Test
	public void mapsSidebarSeekPositionAndClampsEdges()
	{
		assertEquals(0, SpotifyPanel.seekPositionForX(-10, 200, 180_000));
		assertEquals(90_000, SpotifyPanel.seekPositionForX(100, 200, 180_000));
		assertEquals(180_000, SpotifyPanel.seekPositionForX(250, 200, 180_000));
	}

	@Test
	public void returnsZeroWithoutUsableTrackOrDuration()
	{
		assertEquals(0, SpotifyPanel.seekPositionForX(100, 0, 180_000));
		assertEquals(0, SpotifyPanel.seekPositionForX(100, 200, 0));
	}
}
