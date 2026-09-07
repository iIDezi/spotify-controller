package com.spotifycontroller;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpotifySearchTest
{
	@Test
	public void parsesTrackDetails()
	{
		String json = "{\"tracks\":{\"items\":[{\"uri\":\"spotify:track:123\"," +
			"\"name\":\"Red Shoes\",\"artists\":[{\"name\":\"Sunglasses Kid\"}," +
			"{\"name\":\"Guest\"}],\"album\":{\"name\":\"Night Drive\"," +
			"\"images\":[{\"url\":\"https://i.scdn.co/large.jpg\",\"width\":640}," +
			"{\"url\":\"https://i.scdn.co/thumb.jpg\",\"width\":300}," +
			"{\"url\":\"https://i.scdn.co/tiny.jpg\",\"width\":64}]}}]}}";

		List<SpotifySearchResult> results = SpotifySearchParser.parse(json);

		assertEquals(1, results.size());
		assertEquals("spotify:track:123", results.get(0).getUri());
		assertEquals("Red Shoes", results.get(0).getTitle());
		assertEquals("Sunglasses Kid, Guest", results.get(0).getArtist());
		assertEquals("Night Drive", results.get(0).getAlbum());
		assertEquals("https://i.scdn.co/thumb.jpg", results.get(0).getArtworkUrl());
	}

	@Test
	public void limitsResultsAndSkipsNonTrackUris()
	{
		StringBuilder json = new StringBuilder("{\"tracks\":{\"items\":[");
		json.append("{\"uri\":\"spotify:episode:skip\",\"name\":\"Skip\"}");
		for (int index = 0; index < 12; index++)
		{
			json.append(",{\"uri\":\"spotify:track:")
				.append(index)
				.append("\",\"name\":\"Song ")
				.append(index)
				.append("\",\"artists\":[{\"name\":\"Artist\"}]}");
		}
		json.append("]}}");

		List<SpotifySearchResult> results = SpotifySearchParser.parse(json.toString());

		assertEquals(10, results.size());
		assertEquals("spotify:track:0", results.get(0).getUri());
		assertEquals("spotify:track:9", results.get(9).getUri());
	}

	@Test
	public void returnsEmptyResultsWhenTracksAreMissing()
	{
		assertTrue(SpotifySearchParser.parse("{}").isEmpty());
	}

	@Test
	public void encodesSearchPathAndRequestsTenTracks()
	{
		String path = SpotifyApiClient.searchPath(" Red Shoes artist:Sunglasses Kid ");

		assertTrue(path.startsWith("/search?"));
		assertTrue(path.contains("q=Red%20Shoes%20artist%3ASunglasses%20Kid"));
		assertTrue(path.contains("type=track"));
		assertTrue(path.contains("limit=10"));
	}
}
