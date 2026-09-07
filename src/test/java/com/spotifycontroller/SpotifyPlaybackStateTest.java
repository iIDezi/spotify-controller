package com.spotifycontroller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpotifyPlaybackStateTest
{
	@Test
	public void changesPlayingStateWithoutLosingPlaybackDetails()
	{
		SpotifyPlaybackState paused = SpotifyPlaybackState.of(
			false,
			"track",
			"A long song title",
			"An artist",
			"An album",
			"Hidden device",
			"spotify:track:abc",
			"https://i.scdn.co/art.jpg",
			61_000,
			180_000,
			true,
			72);

		SpotifyPlaybackState playing = paused.withPlaying(true);

		assertTrue(playing.isPlaying());
		assertEquals(paused.getTitle(), playing.getTitle());
		assertEquals(paused.getArtist(), playing.getArtist());
		assertEquals(paused.getAlbum(), playing.getAlbum());
		assertEquals(paused.getDevice(), playing.getDevice());
		assertEquals(paused.getUri(), playing.getUri());
		assertEquals(paused.getArtworkUrl(), playing.getArtworkUrl());
		assertEquals(paused.getDurationMs(), playing.getDurationMs());
		assertTrue(playing.isVolumeAvailable());
		assertEquals(72, playing.getVolumePercent());
		assertTrue(playing.getProgressMs() >= paused.getProgressMs());

		SpotifyPlaybackState pausedAgain = playing.withPlaying(false);
		assertFalse(pausedAgain.isPlaying());
		assertTrue(pausedAgain.getProgressMs() >= playing.getProgressMs());
	}

	@Test
	public void changesProgressWithoutLosingPlaybackDetails()
	{
		SpotifyPlaybackState state = SpotifyPlaybackState.of(
			true,
			"track",
			"Song",
			"Artist",
			"Album",
			"Hidden device",
			15_000,
			180_000);

		SpotifyPlaybackState sought = state.withProgress(90_000);

		assertTrue(sought.isPlaying());
		assertEquals(state.getTitle(), sought.getTitle());
		assertEquals(state.getArtist(), sought.getArtist());
		assertEquals(90_000, sought.getProgressMs());
		assertEquals(180_000, sought.getDurationMs());
		assertEquals(0, state.withProgress(-1).getProgressMs());
		assertEquals(180_000, state.withProgress(999_999).getProgressMs());
	}

	@Test
	public void changesVolumeWithoutLosingPlaybackDetails()
	{
		SpotifyPlaybackState state = SpotifyPlaybackState.of(
			true, "track", "Song", "Artist", "Album", "Hidden device", 15_000, 180_000);

		SpotifyPlaybackState changed = state.withVolume(45);

		assertTrue(changed.isVolumeAvailable());
		assertEquals(45, changed.getVolumePercent());
		assertEquals(state.getTitle(), changed.getTitle());
		assertEquals(state.getDurationMs(), changed.getDurationMs());
		assertEquals(100, state.withVolume(500).getVolumePercent());
	}

	@Test
	public void changesPlaybackModesWithoutLosingPlaybackDetails()
	{
		SpotifyPlaybackState state = SpotifyPlaybackState.of(
			true,
			"track",
			"Song",
			"Artist",
			"Album",
			"Hidden device",
			"spotify:track:abc",
			"https://i.scdn.co/art.jpg",
			15_000,
			180_000,
			true,
			72,
			false,
			"off");

		SpotifyPlaybackState shuffled = state.withShuffleEnabled(true);
		SpotifyPlaybackState repeatTrack = shuffled.withRepeatMode("track");

		assertTrue(shuffled.isShuffleEnabled());
		assertEquals("track", repeatTrack.getRepeatMode());
		assertTrue(repeatTrack.isShuffleEnabled());
		assertEquals(state.getTitle(), repeatTrack.getTitle());
		assertEquals(state.getDurationMs(), repeatTrack.getDurationMs());
		assertEquals("off", repeatTrack.withRepeatMode("unsupported").getRepeatMode());
	}
}
