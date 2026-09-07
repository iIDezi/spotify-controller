package com.spotifycontroller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpotifyPlaybackParserTest
{
	@Test
	public void parsesTrackPlayback()
	{
		String json = "{\"is_playing\":true,\"progress_ms\":61000," +
			"\"shuffle_state\":true,\"repeat_state\":\"context\"," +
			"\"device\":{\"name\":\"Desktop\",\"volume_percent\":64,\"supports_volume\":true}," +
			"\"item\":{\"type\":\"track\",\"name\":\"Song\"," +
			"\"uri\":\"spotify:track:abc\"," +
			"\"duration_ms\":180000,\"artists\":[{\"name\":\"Artist One\"},{\"name\":\"Artist Two\"}]," +
			"\"album\":{\"name\":\"Album\",\"images\":[{\"url\":\"large.jpg\",\"width\":640}," +
			"{\"url\":\"medium.jpg\",\"width\":300},{\"url\":\"small.jpg\",\"width\":64}]}}}";

		SpotifyPlaybackState state = SpotifyPlaybackParser.parse(json);

		assertTrue(state.isItemAvailable());
		assertTrue(state.isPlaying());
		assertEquals("Song", state.getTitle());
		assertEquals("Artist One, Artist Two", state.getArtist());
		assertEquals("Album", state.getAlbum());
		assertEquals("Desktop", state.getDevice());
		assertEquals("spotify:track:abc", state.getUri());
		assertEquals("medium.jpg", state.getArtworkUrl());
		assertEquals(61000, state.getProgressMs());
		assertEquals(180000, state.getDurationMs());
		assertTrue(state.isVolumeAvailable());
		assertEquals(64, state.getVolumePercent());
		assertTrue(state.isShuffleEnabled());
		assertEquals("context", state.getRepeatMode());
	}

	@Test
	public void parsesEpisodeArtwork()
	{
		String json = "{\"is_playing\":false,\"item\":{\"type\":\"episode\",\"name\":\"Episode\"," +
			"\"uri\":\"spotify:episode:def\",\"images\":[{\"url\":\"episode.jpg\",\"width\":300}]," +
			"\"show\":{\"name\":\"Show\"}}}";

		SpotifyPlaybackState state = SpotifyPlaybackParser.parse(json);

		assertEquals("spotify:episode:def", state.getUri());
		assertEquals("episode.jpg", state.getArtworkUrl());
	}

	@Test
	public void parsesEpisodePlayback()
	{
		String json = "{\"is_playing\":false,\"progress_ms\":1000,\"item\":{\"type\":\"episode\"," +
			"\"name\":\"Episode 5\",\"duration_ms\":3000000,\"show\":{\"name\":\"My Podcast\"}}}";

		SpotifyPlaybackState state = SpotifyPlaybackParser.parse(json);

		assertTrue(state.isItemAvailable());
		assertFalse(state.isPlaying());
		assertEquals("Episode 5", state.getTitle());
		assertEquals("My Podcast", state.getArtist());
		assertEquals("Podcast episode", state.getAlbum());
	}

	@Test
	public void parsesNoActiveItem()
	{
		SpotifyPlaybackState state = SpotifyPlaybackParser.parse("{\"is_playing\":false,\"item\":null}");

		assertFalse(state.isItemAvailable());
		assertEquals("Nothing playing", state.getTitle());
		assertFalse(state.isShuffleEnabled());
		assertEquals("off", state.getRepeatMode());
	}
}
