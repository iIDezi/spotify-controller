package com.spotifycontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class LyricsState
{
	private static final LyricsState EMPTY = new LyricsState(Collections.emptyList(), false);

	private final List<Line> lines;
	private final boolean synced;

	LyricsState(List<Line> lines, boolean synced)
	{
		this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
		this.synced = synced;
	}

	static LyricsState empty()
	{
		return EMPTY;
	}

	boolean isAvailable()
	{
		return !lines.isEmpty();
	}

	boolean isSynced()
	{
		return synced;
	}

	int getLineCount()
	{
		return lines.size();
	}

	Window windowAt(int progressMs, int durationMs)
	{
		return windowAt(progressMs, durationMs, 3);
	}

	Window windowAt(int progressMs, int durationMs, int maximumLines)
	{
		if (lines.isEmpty())
		{
			return Window.empty();
		}

		int currentIndex = synced
			? syncedLineIndex(Math.max(0, progressMs))
			: plainLineIndex(Math.max(0, progressMs), Math.max(0, durationMs));
		int windowSize = Math.min(lines.size(), Math.max(1,
			Math.min(SpotifyControllerConfig.MAX_OVERLAY_LYRIC_LINES, maximumLines)));
		int start = currentIndex - (windowSize / 2);
		start = Math.max(0, Math.min(lines.size() - windowSize, start));
		List<String> visibleLines = new ArrayList<>(windowSize);
		for (int index = start; index < start + windowSize; index++)
		{
			visibleLines.add(lines.get(index).getText());
		}
		return new Window(visibleLines, currentIndex - start);
	}

	private int syncedLineIndex(long progressMs)
	{
		int low = 0;
		int high = lines.size() - 1;
		int result = 0;
		while (low <= high)
		{
			int middle = (low + high) >>> 1;
			if (lines.get(middle).getTimestampMs() <= progressMs)
			{
				result = middle;
				low = middle + 1;
			}
			else
			{
				high = middle - 1;
			}
		}
		return result;
	}

	private int plainLineIndex(int progressMs, int durationMs)
	{
		if (durationMs <= 0)
		{
			return 0;
		}
		long scaledIndex = ((long) progressMs * lines.size()) / durationMs;
		return (int) Math.min(lines.size() - 1L, scaledIndex);
	}

	static final class Line
	{
		private final long timestampMs;
		private final String text;

		Line(long timestampMs, String text)
		{
			this.timestampMs = Math.max(0L, timestampMs);
			this.text = text == null ? "" : text;
		}

		long getTimestampMs()
		{
			return timestampMs;
		}

		String getText()
		{
			return text;
		}
	}

	static final class Window
	{
		private static final Window EMPTY = new Window(Collections.emptyList(), 0);

		private final List<String> lines;
		private final int currentIndex;

		Window(List<String> lines, int currentIndex)
		{
			this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
			this.currentIndex = Math.max(0, Math.min(Math.max(0, lines.size() - 1), currentIndex));
		}

		static Window empty()
		{
			return EMPTY;
		}

		String getPrevious()
		{
			return currentIndex > 0 ? lines.get(currentIndex - 1) : "";
		}

		String getCurrent()
		{
			return lines.isEmpty() ? "" : lines.get(currentIndex);
		}

		String getNext()
		{
			return currentIndex + 1 < lines.size() ? lines.get(currentIndex + 1) : "";
		}

		List<String> getLines()
		{
			return lines;
		}

		int getCurrentIndex()
		{
			return currentIndex;
		}
	}
}
