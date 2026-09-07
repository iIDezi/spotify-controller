package com.spotifycontroller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

final class SpotifyPlaybackParser
{
	private SpotifyPlaybackParser()
	{
	}

	static SpotifyPlaybackState parse(String json)
	{
		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		JsonObject item = object(root, "item");
		if (item == null)
		{
			return SpotifyPlaybackState.idle();
		}

		String type = string(item, "type", "track");
		String title = string(item, "name", "Unknown title");
		String artist;
		String album;
		String artworkUrl;

		if ("episode".equals(type))
		{
			JsonObject show = object(item, "show");
			artist = show == null ? "Podcast" : string(show, "name", "Podcast");
			album = "Podcast episode";
			artworkUrl = imageUrl(array(item, "images"));
			if (artworkUrl.isEmpty() && show != null)
			{
				artworkUrl = imageUrl(array(show, "images"));
			}
		}
		else
		{
			artist = joinArtists(array(item, "artists"));
			JsonObject albumObject = object(item, "album");
			album = albumObject == null ? "" : string(albumObject, "name", "");
			artworkUrl = albumObject == null ? "" : imageUrl(array(albumObject, "images"));
		}

		JsonObject deviceObject = object(root, "device");
		String device = deviceObject == null ? "Active Spotify device" : string(deviceObject, "name", "Active Spotify device");
		boolean volumeAvailable = deviceObject != null && deviceObject.has("volume_percent") &&
			(!deviceObject.has("supports_volume") || bool(deviceObject, "supports_volume"));
		int volumePercent = deviceObject == null ? 0 : integer(deviceObject, "volume_percent");

		return SpotifyPlaybackState.of(
			bool(root, "is_playing"),
			type,
			title,
			artist,
			album,
			device,
			string(item, "uri", ""),
			artworkUrl,
			integer(root, "progress_ms"),
			integer(item, "duration_ms"),
			volumeAvailable,
			volumePercent,
			bool(root, "shuffle_state"),
			string(root, "repeat_state", "off"));
	}

	static String joinArtists(JsonArray artists)
	{
		if (artists == null || artists.size() == 0)
		{
			return "Unknown artist";
		}

		List<String> names = new ArrayList<>();
		for (JsonElement element : artists)
		{
			if (element != null && element.isJsonObject())
			{
				String name = string(element.getAsJsonObject(), "name", "");
				if (!name.isEmpty())
				{
					names.add(name);
				}
			}
		}

		return names.isEmpty() ? "Unknown artist" : String.join(", ", names);
	}

	static JsonObject object(JsonObject parent, String name)
	{
		JsonElement element = parent.get(name);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	static JsonArray array(JsonObject parent, String name)
	{
		JsonElement element = parent.get(name);
		return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
	}

	static String string(JsonObject parent, String name, String fallback)
	{
		JsonElement element = parent.get(name);
		return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
	}

	static String imageUrl(JsonArray images)
	{
		if (images == null || images.size() == 0)
		{
			return "";
		}

		String fallback = "";
		String best = "";
		int bestWidth = Integer.MAX_VALUE;
		String largest = "";
		int largestWidth = 0;
		for (JsonElement element : images)
		{
			if (element == null || !element.isJsonObject())
			{
				continue;
			}
			JsonObject image = element.getAsJsonObject();
			String url = string(image, "url", "");
			if (url.isEmpty())
			{
				continue;
			}
			if (fallback.isEmpty())
			{
				fallback = url;
			}
			int width = integer(image, "width");
			if (width > largestWidth)
			{
				largestWidth = width;
				largest = url;
			}
			if (width >= 128 && width < bestWidth)
			{
				bestWidth = width;
				best = url;
			}
		}
		return !best.isEmpty() ? best : (!largest.isEmpty() ? largest : fallback);
	}

	private static int integer(JsonObject parent, String name)
	{
		JsonElement element = parent.get(name);
		return element != null && element.isJsonPrimitive() ? element.getAsInt() : 0;
	}

	private static boolean bool(JsonObject parent, String name)
	{
		JsonElement element = parent.get(name);
		return element != null && element.isJsonPrimitive() && element.getAsBoolean();
	}
}
