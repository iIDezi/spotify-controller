package com.spotifycontroller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

final class SpotifyQueueParser
{
	private static final int MAXIMUM_ITEMS = 10;

	private SpotifyQueueParser()
	{
	}

	static SpotifyQueueState parse(String json)
	{
		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		JsonArray queue = SpotifyPlaybackParser.array(root, "queue");
		if (queue == null)
		{
			return SpotifyQueueState.empty();
		}

		List<SpotifyQueueItem> items = new ArrayList<>();
		for (JsonElement element : queue)
		{
			if (items.size() >= MAXIMUM_ITEMS)
			{
				break;
			}
			if (element == null || !element.isJsonObject())
			{
				continue;
			}

			JsonObject item = element.getAsJsonObject();
			String uri = SpotifyPlaybackParser.string(item, "uri", "");
			if (uri.isEmpty())
			{
				continue;
			}

			String type = SpotifyPlaybackParser.string(item, "type", "track");
			String artist;
			if ("episode".equals(type))
			{
				JsonObject show = SpotifyPlaybackParser.object(item, "show");
				artist = show == null
					? "Podcast"
					: SpotifyPlaybackParser.string(show, "name", "Podcast");
			}
			else
			{
				artist = SpotifyPlaybackParser.joinArtists(
					SpotifyPlaybackParser.array(item, "artists"));
			}

			items.add(new SpotifyQueueItem(
				uri,
				type,
				SpotifyPlaybackParser.string(item, "name", "Unknown title"),
				artist));
		}
		return new SpotifyQueueState(items);
	}
}
