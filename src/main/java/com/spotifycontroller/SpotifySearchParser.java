package com.spotifycontroller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SpotifySearchParser
{
	private static final int MAXIMUM_RESULTS = 10;

	private SpotifySearchParser()
	{
	}

	static List<SpotifySearchResult> parse(String json)
	{
		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		JsonObject tracks = SpotifyPlaybackParser.object(root, "tracks");
		JsonArray items = tracks == null ? null : SpotifyPlaybackParser.array(tracks, "items");
		if (items == null)
		{
			return Collections.emptyList();
		}

		List<SpotifySearchResult> results = new ArrayList<>();
		for (JsonElement element : items)
		{
			if (results.size() >= MAXIMUM_RESULTS)
			{
				break;
			}
			if (element == null || !element.isJsonObject())
			{
				continue;
			}

			JsonObject track = element.getAsJsonObject();
			String uri = SpotifyPlaybackParser.string(track, "uri", "");
			if (!uri.startsWith("spotify:track:"))
			{
				continue;
			}

			JsonObject album = SpotifyPlaybackParser.object(track, "album");
			results.add(new SpotifySearchResult(
				uri,
				SpotifyPlaybackParser.string(track, "name", "Unknown title"),
				SpotifyPlaybackParser.joinArtists(SpotifyPlaybackParser.array(track, "artists")),
				album == null ? "" : SpotifyPlaybackParser.string(album, "name", ""),
				album == null
					? ""
					: SpotifyPlaybackParser.imageUrl(SpotifyPlaybackParser.array(album, "images"))));
		}
		return results;
	}
}
