package com.spotifycontroller;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpotifyQueueParserTest
{
	@Test
	public void parsesAndLimitsQueueToTenItems()
	{
		StringBuilder json = new StringBuilder("{\"queue\":[");
		for (int index = 0; index < 12; index++)
		{
			if (index > 0)
			{
				json.append(',');
			}
			json.append("{\"type\":\"track\",\"uri\":\"spotify:track:")
				.append(index)
				.append("\",\"name\":\"Song ")
				.append(index)
				.append("\",\"artists\":[{\"name\":\"Artist\"}]}");
		}
		json.append("]}");

		List<SpotifyQueueItem> items = SpotifyQueueParser.parse(json.toString()).getItems();

		assertEquals(10, items.size());
		assertEquals("Song 0", items.get(0).getTitle());
		assertEquals("spotify:track:9", items.get(9).getUri());
	}

	@Test
	public void parsesEpisodesAndSkipsItemsWithoutUris()
	{
		String json = "{\"queue\":[{\"type\":\"episode\",\"uri\":\"spotify:episode:1\"," +
			"\"name\":\"Episode\",\"show\":{\"name\":\"Podcast\"}},{\"name\":\"Unavailable\"}]}";

		List<SpotifyQueueItem> items = SpotifyQueueParser.parse(json).getItems();

		assertEquals(1, items.size());
		assertEquals("Podcast", items.get(0).getArtist());
		assertEquals("episode", items.get(0).getType());
	}
}
