package com.spotifycontroller;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LyricsParserTest
{
	private final Gson gson = new Gson();

	@Test
	public void followsSyncedLyricsWithThreeLineWindow()
	{
		String json = "{\"syncedLyrics\":\"[00:01.00]First\\n[00:05.25]Second\\n[00:09.500]Third\\n[00:12.0]Fourth\"}";
		LyricsState lyrics = LyricsParser.parse(json, gson);

		assertTrue(lyrics.isAvailable());
		assertTrue(lyrics.isSynced());
		assertEquals(4, lyrics.getLineCount());
		LyricsState.Window window = lyrics.windowAt(9_750, 15_000);
		assertEquals("Second", window.getPrevious());
		assertEquals("Third", window.getCurrent());
		assertEquals("Fourth", window.getNext());
	}

	@Test
	public void usesPlainLyricsWhenSyncedLyricsAreUnavailable()
	{
		String json = "{\"syncedLyrics\":null,\"plainLyrics\":\"One\\nTwo\\nThree\\nFour\"}";
		LyricsState lyrics = LyricsParser.parse(json, gson);

		assertFalse(lyrics.isSynced());
		LyricsState.Window window = lyrics.windowAt(5_100, 10_000);
		assertEquals("Two", window.getPrevious());
		assertEquals("Three", window.getCurrent());
		assertEquals("Four", window.getNext());
	}

	@Test
	public void ignoresMetadataAndSupportsMultipleTimestamps()
	{
		String json = "{\"syncedLyrics\":\"[ar:Artist]\\n[00:01.0][00:02.00]Echo\"}";
		LyricsState lyrics = LyricsParser.parse(json, gson);

		assertEquals(2, lyrics.getLineCount());
		assertEquals("Echo", lyrics.windowAt(2_100, 3_000).getCurrent());
	}

	@Test
	public void rejectsMalformedOrEmptyResponses()
	{
		assertFalse(LyricsParser.parse("not json", gson).isAvailable());
		assertFalse(LyricsParser.parse("{\"plainLyrics\":\"  \\n\"}", gson).isAvailable());
	}

	@Test
	public void buildsStableLookupKeysWithoutRequiringAnAlbum()
	{
		SpotifyPlaybackState state = SpotifyPlaybackState.of(
			true, "track", " Song ", " ARTIST ", "", "Device", "", "", 0, 181_400);
		assertTrue(LyricsClient.canLookup(state));
		assertEquals("song\nartist\n\n181", LyricsClient.keyFor(state));
	}

	@Test
	public void expandsAndShrinksTheWindowAroundTheCurrentLine()
	{
		String json = "{\"plainLyrics\":\"One\\nTwo\\nThree\\nFour\\nFive\\nSix\\nSeven\"}";
		LyricsState lyrics = LyricsParser.parse(json, gson);

		LyricsState.Window oneLine = lyrics.windowAt(5_000, 10_000, 1);
		assertEquals(1, oneLine.getLines().size());
		assertEquals("Four", oneLine.getCurrent());

		LyricsState.Window sevenLines = lyrics.windowAt(5_000, 10_000, 7);
		assertEquals(7, sevenLines.getLines().size());
		assertEquals(3, sevenLines.getCurrentIndex());
		assertEquals("One", sevenLines.getLines().get(0));
		assertEquals("Seven", sevenLines.getLines().get(6));
	}
}
