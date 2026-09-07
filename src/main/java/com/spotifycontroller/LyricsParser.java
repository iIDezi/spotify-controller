package com.spotifycontroller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LyricsParser
{
	private static final int MAXIMUM_LINES = 2_000;
	private static final Pattern TIMESTAMP = Pattern.compile(
		"\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]");

	private LyricsParser()
	{
	}

	static LyricsState parse(String json, Gson gson)
	{
		if (json == null || json.trim().isEmpty())
		{
			return LyricsState.empty();
		}

		try
		{
			JsonObject response = gson.fromJson(json, JsonObject.class);
			if (response == null)
			{
				return LyricsState.empty();
			}

			String syncedLyrics = stringValue(response.get("syncedLyrics"));
			List<LyricsState.Line> syncedLines = parseSynced(syncedLyrics);
			if (!syncedLines.isEmpty())
			{
				return new LyricsState(syncedLines, true);
			}

			String plainLyrics = stringValue(response.get("plainLyrics"));
			List<LyricsState.Line> plainLines = parsePlain(plainLyrics);
			return plainLines.isEmpty()
				? LyricsState.empty()
				: new LyricsState(plainLines, false);
		}
		catch (RuntimeException exception)
		{
			return LyricsState.empty();
		}
	}

	private static List<LyricsState.Line> parseSynced(String lyrics)
	{
		List<LyricsState.Line> lines = new ArrayList<>();
		if (lyrics == null || lyrics.trim().isEmpty())
		{
			return lines;
		}

		for (String rawLine : lyrics.split("\\r?\\n"))
		{
			Matcher matcher = TIMESTAMP.matcher(rawLine);
			List<Long> timestamps = new ArrayList<>();
			int textStart = -1;
			while (matcher.find())
			{
				int seconds = Integer.parseInt(matcher.group(2));
				if (seconds > 59)
				{
					continue;
				}
				long minutes = Long.parseLong(matcher.group(1));
				long fractionMs = fractionMilliseconds(matcher.group(3));
				timestamps.add(((minutes * 60L) + seconds) * 1_000L + fractionMs);
				textStart = matcher.end();
			}

			String text = textStart < 0 ? "" : rawLine.substring(textStart).trim();
			if (text.isEmpty())
			{
				continue;
			}
			for (Long timestamp : timestamps)
			{
				lines.add(new LyricsState.Line(timestamp, text));
				if (lines.size() >= MAXIMUM_LINES)
				{
					break;
				}
			}
			if (lines.size() >= MAXIMUM_LINES)
			{
				break;
			}
		}
		lines.sort(Comparator.comparingLong(LyricsState.Line::getTimestampMs));
		return lines;
	}

	private static List<LyricsState.Line> parsePlain(String lyrics)
	{
		List<LyricsState.Line> lines = new ArrayList<>();
		if (lyrics == null || lyrics.trim().isEmpty())
		{
			return lines;
		}

		for (String rawLine : lyrics.split("\\r?\\n"))
		{
			String text = rawLine.trim();
			if (!text.isEmpty())
			{
				lines.add(new LyricsState.Line(lines.size(), text));
				if (lines.size() >= MAXIMUM_LINES)
				{
					break;
				}
			}
		}
		return lines;
	}

	private static String stringValue(JsonElement value)
	{
		return value == null || value.isJsonNull() ? "" : value.getAsString();
	}

	private static long fractionMilliseconds(String fraction)
	{
		if (fraction == null || fraction.isEmpty())
		{
			return 0L;
		}
		long value = Long.parseLong(fraction);
		if (fraction.length() == 1)
		{
			return value * 100L;
		}
		if (fraction.length() == 2)
		{
			return value * 10L;
		}
		return value;
	}
}
