package com.spotifycontroller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class SpotifyOverlay extends Overlay
{
	private static final Color PRIMARY_TEXT = new Color(238, 238, 238);
	private static final Color SECONDARY_TEXT = new Color(190, 190, 190);
	private static final Color DEFAULT_PANEL_BACKGROUND = new Color(14, 14, 14, 118);
	private static final Color PANEL_BORDER = new Color(180, 180, 180, 72);
	private static final Color BUTTON_BACKGROUND = new Color(12, 12, 12, 96);
	private static final Color BUTTON_BORDER = new Color(205, 205, 205, 88);
	private static final Color DEFAULT_PROGRESS_TRACK = new Color(118, 137, 148);
	private static final Color DEFAULT_PLAYBACK_PROGRESS = new Color(238, 238, 238);
	private static final Color DEFAULT_PLAYBACK_BUTTON = new Color(238, 238, 238);
	private static final int PROGRESS_TRACK_ALPHA = 145;
	private static final int PLAYBACK_PROGRESS_ALPHA = 225;

	private static final int BASE_WIDTH = 220;
	private static final int FULL_BASE_HEIGHT = 152;
	private static final int PROGRESS_ONLY_BASE_HEIGHT = 111;
	private static final int CONTROLS_ONLY_BASE_HEIGHT = 115;
	private static final int TEXT_ONLY_BASE_HEIGHT = 78;
	private static final int COMPACT_HEADER_BASE_HEIGHT = 29;
	private static final int COMPACT_NO_SOURCE_BASE_HEIGHT = 5;
	private static final int HEADER_BASELINE = 21;
	private static final int DETAILS_BASELINE = 47;
	private static final int DETAIL_BASELINE_OFFSET = 18;
	private static final int DETAIL_LINE_HEIGHT = 20;
	private static final int DETAIL_BOTTOM_PADDING = 11;
	private static final int PROGRESS_SECTION_HEIGHT = 33;
	private static final int CONTROLS_WITH_PROGRESS_SECTION_HEIGHT = 41;
	private static final int CONTROLS_ONLY_SECTION_HEIGHT = 37;
	private static final int LYRICS_VERTICAL_PADDING = 7;
	private static final int LYRICS_LINE_HEIGHT = 17;
	private static final double MINIMUM_SCALE = 0.5;
	private static final int MINIMUM_RENDERED_WIDTH = 50;
	private static final int CONTENT_GAP = 10;
	private static final int PADDING = 10;
	private static final int ARC = 9;
	private static final int ARTWORK_SIZE = 38;
	private static final int ARTWORK_GAP = 8;
	private static final int PROGRESS_HEIGHT = 4;
	private static final int PROGRESS_HIT_HEIGHT = 18;
	private static final int BUTTON_WIDTH = 48;
	private static final int BUTTON_HEIGHT = 27;
	private static final int BUTTON_GAP = 12;
	private static final int MARQUEE_START_PAUSE_MS = 1_500;
	private static final int MARQUEE_END_PAUSE_MS = 1_100;
	private static final double MARQUEE_PIXELS_PER_SECOND = 22.0;
	private static final int ARTIST_MARQUEE_START_PAUSE_MS = 2_000;
	private static final int ARTIST_MARQUEE_END_PAUSE_MS = 1_500;
	private static final double ARTIST_MARQUEE_PIXELS_PER_SECOND = 8.0;
	private static final float SOURCE_FONT_SIZE = 15f;
	private static final float TITLE_STATUS_FONT_SIZE = 16f;
	private static final float ARTIST_FONT_SIZE = 15f;
	private static final float TIME_FONT_SIZE = 13f;
	private static final float LYRICS_FONT_SIZE = 13f;

	private final SpotifyControllerPlugin plugin;
	private final SpotifyControllerConfig config;

	private volatile Rectangle previousBounds = new Rectangle();
	private volatile Rectangle playPauseBounds = new Rectangle();
	private volatile Rectangle nextBounds = new Rectangle();
	private volatile Rectangle progressBounds = new Rectangle();
	private volatile Control hoveredControl;
	private volatile boolean progressHovered;
	private volatile boolean seeking;
	private volatile int seekPreviewMs;
	private volatile boolean controlsVisible = true;
	private volatile boolean progressVisible = true;
	private volatile boolean sourceVisible = true;
	private volatile boolean titleVisible = true;
	private volatile boolean artistVisible = true;
	private volatile boolean artworkVisible = true;
	private volatile int lyricsLinesDrawn;
	private volatile int layoutBaseWidth = BASE_WIDTH;
	private String marqueeTitle = "";
	private long marqueeStartedAtMs;
	private String marqueeArtist = "";
	private long artistMarqueeStartedAtMs;

	@Inject
	private SpotifyOverlay(SpotifyControllerPlugin plugin, SpotifyControllerConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_LOW);
		setResizable(true);
		setMinimumSize(50);
		controlsVisible = config.showOverlayControls();
		progressVisible = config.showOverlayProgress();
		sourceVisible = config.showOverlaySource();
		titleVisible = config.showOverlayTitle();
		artistVisible = config.showOverlayArtist();
		artworkVisible = config.showOverlayArtwork();
		setPreferredSize(new Dimension(BASE_WIDTH, baseHeightFor(
			controlsVisible, progressVisible, 0,
			sourceVisible, titleVisible, artistVisible, artworkVisible)));
	}

	void setControlsVisible(boolean visible)
	{
		updateLayoutState(visible, progressVisible, lyricsLinesDrawn,
			sourceVisible, titleVisible, artistVisible, artworkVisible, layoutBaseWidth);
		if (!visible)
		{
			clearButtonBounds();
		}
	}

	void setProgressVisible(boolean visible)
	{
		updateLayoutState(controlsVisible, visible, lyricsLinesDrawn,
			sourceVisible, titleVisible, artistVisible, artworkVisible, layoutBaseWidth);
		if (!visible)
		{
			clearProgressBounds();
		}
	}

	private void updateLayoutState(
		boolean newControlsVisible,
		boolean newProgressVisible,
		int newLyricsLines,
		boolean newSourceVisible,
		boolean newTitleVisible,
		boolean newArtistVisible,
		boolean newArtworkVisible,
		int newBaseWidth)
	{
		int safeLyricsLines = Math.max(0,
			Math.min(SpotifyControllerConfig.MAX_OVERLAY_LYRIC_LINES, newLyricsLines));
		int safeBaseWidth = Math.max(MINIMUM_RENDERED_WIDTH, Math.min(BASE_WIDTH, newBaseWidth));
		if (controlsVisible == newControlsVisible &&
			progressVisible == newProgressVisible &&
			lyricsLinesDrawn == safeLyricsLines &&
			sourceVisible == newSourceVisible &&
			titleVisible == newTitleVisible &&
			artistVisible == newArtistVisible &&
			artworkVisible == newArtworkVisible &&
			layoutBaseWidth == safeBaseWidth)
		{
			return;
		}

		int currentBaseHeight = baseHeightFor(
			controlsVisible, progressVisible, lyricsLinesDrawn,
			sourceVisible, titleVisible, artistVisible, artworkVisible);
		Dimension currentSize = getPreferredSize();
		if (currentSize == null || currentSize.width <= 0 || currentSize.height <= 0)
		{
			currentSize = new Dimension(BASE_WIDTH, currentBaseHeight);
		}
		double scale = Math.min(
			(double) currentSize.width / layoutBaseWidth,
			(double) currentSize.height / currentBaseHeight);
		scale = Math.max(MINIMUM_SCALE, scale);

		controlsVisible = newControlsVisible;
		progressVisible = newProgressVisible;
		lyricsLinesDrawn = safeLyricsLines;
		sourceVisible = newSourceVisible;
		titleVisible = newTitleVisible;
		artistVisible = newArtistVisible;
		artworkVisible = newArtworkVisible;
		layoutBaseWidth = safeBaseWidth;
		int targetBaseHeight = baseHeightFor(
			controlsVisible, progressVisible, lyricsLinesDrawn,
			sourceVisible, titleVisible, artistVisible, artworkVisible);
		setPreferredSize(new Dimension(
			Math.max(minimumWidthFor(layoutBaseWidth),
				(int) Math.round(layoutBaseWidth * scale)),
			Math.max(minimumHeightFor(targetBaseHeight),
				(int) Math.round(targetBaseHeight * scale))));
	}

	static int baseHeightFor(boolean controlsVisible, boolean progressVisible)
	{
		return baseHeightFor(
			controlsVisible, progressVisible, 0, true, true, true, true);
	}

	static int baseHeightFor(boolean controlsVisible, boolean progressVisible, boolean lyricsVisible)
	{
		return baseHeightFor(controlsVisible, progressVisible, lyricsVisible ? 3 : 0);
	}

	static int baseHeightFor(boolean controlsVisible, boolean progressVisible, int lyricLines)
	{
		return baseHeightFor(
			controlsVisible, progressVisible, lyricLines, true, true, true, true);
	}

	static int baseHeightFor(
		boolean controlsVisible,
		boolean progressVisible,
		int lyricLines,
		boolean compactHeaderOnly)
	{
		return baseHeightFor(
			controlsVisible, progressVisible, lyricLines, compactHeaderOnly, true);
	}

	static int baseHeightFor(
		boolean controlsVisible,
		boolean progressVisible,
		int lyricLines,
		boolean compactHeaderOnly,
		boolean sourceVisible)
	{
		return baseHeightFor(
			controlsVisible,
			progressVisible,
			lyricLines,
			sourceVisible,
			!compactHeaderOnly,
			!compactHeaderOnly,
			!compactHeaderOnly);
	}

	static int baseHeightFor(
		boolean controlsVisible,
		boolean progressVisible,
		int lyricLines,
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		int safeLines = Math.max(0,
			Math.min(SpotifyControllerConfig.MAX_OVERLAY_LYRIC_LINES, lyricLines));
		int sectionHeight = detailBaseHeightFor(
			sourceVisible, titleVisible, artistVisible, artworkVisible);
		if (progressVisible)
		{
			sectionHeight += PROGRESS_SECTION_HEIGHT;
		}
		if (controlsVisible)
		{
			sectionHeight += progressVisible
				? CONTROLS_WITH_PROGRESS_SECTION_HEIGHT
				: CONTROLS_ONLY_SECTION_HEIGHT;
		}
		return sectionHeight +
			(safeLines == 0 ? 0 : LYRICS_VERTICAL_PADDING + (safeLines * LYRICS_LINE_HEIGHT));
	}

	static int detailBaseHeightFor(
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		int detailsStartY = detailsStartYFor(sourceVisible);
		int baseHeight = sourceVisible
			? COMPACT_HEADER_BASE_HEIGHT
			: COMPACT_NO_SOURCE_BASE_HEIGHT;
		int textLineCount = (titleVisible ? 1 : 0) + (artistVisible ? 1 : 0);
		if (textLineCount > 0)
		{
			int lastTextBaseline = detailsStartY + DETAIL_BASELINE_OFFSET +
				((textLineCount - 1) * DETAIL_LINE_HEIGHT);
			baseHeight = Math.max(baseHeight, lastTextBaseline + DETAIL_BOTTOM_PADDING);
		}
		if (artworkVisible)
		{
			baseHeight = Math.max(baseHeight,
				detailsStartY + ARTWORK_SIZE + DETAIL_BOTTOM_PADDING);
		}
		return baseHeight;
	}

	private static int detailsStartYFor(boolean sourceVisible)
	{
		return sourceVisible ? COMPACT_HEADER_BASE_HEIGHT : COMPACT_NO_SOURCE_BASE_HEIGHT;
	}

	static int titleBaselineFor(boolean sourceVisible)
	{
		return detailsStartYFor(sourceVisible) + DETAIL_BASELINE_OFFSET;
	}

	static int artistBaselineFor(boolean sourceVisible, boolean titleVisible)
	{
		return titleBaselineFor(sourceVisible) + (titleVisible ? DETAIL_LINE_HEIGHT : 0);
	}

	static int artworkYFor(boolean sourceVisible)
	{
		return detailsStartYFor(sourceVisible);
	}

	static int progressYFor(
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		return detailBaseHeightFor(sourceVisible, titleVisible, artistVisible, artworkVisible) + 3;
	}

	static int timeBaselineFor(
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		return detailBaseHeightFor(sourceVisible, titleVisible, artistVisible, artworkVisible) + 25;
	}

	static int buttonYFor(
		boolean progressVisible,
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		int y = detailBaseHeightFor(sourceVisible, titleVisible, artistVisible, artworkVisible);
		return progressVisible ? y + PROGRESS_SECTION_HEIGHT + 4 : y;
	}

	private int requestedBaseWidthFor(
		Graphics2D graphics,
		SpotifyPlaybackState state,
		int lyricLines,
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		Font sourceFont = FontManager.getRunescapeBoldFont().deriveFont(SOURCE_FONT_SIZE);
		Font titleFont = FontManager.getRunescapeFont().deriveFont(TITLE_STATUS_FONT_SIZE);
		Font artistFont = FontManager.getRunescapeSmallFont().deriveFont(ARTIST_FONT_SIZE);
		String playbackLabel = state.isPlaying() ? "Playing" : "Paused";
		return naturalBaseWidthFor(
			controlsVisible,
			progressVisible,
			lyricLines,
			sourceVisible,
			titleVisible,
			artistVisible,
			artworkVisible,
			graphics.getFontMetrics(sourceFont).stringWidth(sourceName()),
			graphics.getFontMetrics(titleFont).stringWidth(playbackLabel),
			UnicodeText.stringWidth(graphics, titleFont, state.getTitle()),
			UnicodeText.stringWidth(graphics, artistFont, state.getArtist()));
	}

	static int naturalBaseWidthFor(
		boolean controlsVisible,
		boolean progressVisible,
		int lyricLines,
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible,
		int sourceTextWidth,
		int statusTextWidth,
		int titleTextWidth,
		int artistTextWidth)
	{
		if (progressVisible || lyricLines > 0)
		{
			return BASE_WIDTH;
		}

		int width = controlsVisible
			? (PADDING * 2) + (BUTTON_WIDTH * 3) + (BUTTON_GAP * 2)
			: MINIMUM_RENDERED_WIDTH;
		boolean hasDetails = titleVisible || artistVisible || artworkVisible;
		int safeSourceWidth = Math.max(0, sourceTextWidth);
		int safeStatusWidth = Math.max(0, statusTextWidth);
		if (sourceVisible)
		{
			int sourceRowWidth = (PADDING * 2) + safeSourceWidth;
			if (!hasDetails)
			{
				sourceRowWidth += CONTENT_GAP + safeStatusWidth;
			}
			width = Math.max(width, sourceRowWidth);
		}

		int contentStartX = PADDING + (artworkVisible ? ARTWORK_SIZE + ARTWORK_GAP : 0);
		if (titleVisible)
		{
			int titleRowWidth = contentStartX + Math.max(20, titleTextWidth) + PADDING;
			if (sourceVisible)
			{
				titleRowWidth += CONTENT_GAP + safeStatusWidth;
			}
			width = Math.max(width, titleRowWidth);
		}
		if (artistVisible)
		{
			int artistRowWidth = contentStartX + Math.max(20, artistTextWidth) + PADDING;
			if (sourceVisible && !titleVisible)
			{
				artistRowWidth += CONTENT_GAP + safeStatusWidth;
			}
			width = Math.max(width, artistRowWidth);
		}
		if (artworkVisible && !titleVisible && !artistVisible)
		{
			int artworkRowWidth = (PADDING * 2) + ARTWORK_SIZE;
			if (sourceVisible)
			{
				artworkRowWidth += CONTENT_GAP + safeStatusWidth;
			}
			width = Math.max(width, artworkRowWidth);
		}
		return Math.max(MINIMUM_RENDERED_WIDTH, Math.min(BASE_WIDTH, width));
	}

	static int playbackLabelBaselineFor(boolean compactHeaderOnly)
	{
		return compactHeaderOnly ? HEADER_BASELINE : DETAILS_BASELINE;
	}

	static int lyricsFirstBaselineFor(
		boolean controlsVisible,
		boolean progressVisible,
		boolean compactHeaderOnly)
	{
		return lyricsFirstBaselineFor(controlsVisible, progressVisible, compactHeaderOnly, true);
	}

	static int lyricsFirstBaselineFor(
		boolean controlsVisible,
		boolean progressVisible,
		boolean compactHeaderOnly,
		boolean sourceVisible)
	{
		return baseHeightFor(
			controlsVisible, progressVisible, 0, compactHeaderOnly, sourceVisible) + 16;
	}

	static int lyricsFirstBaselineFor(
		boolean controlsVisible,
		boolean progressVisible,
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		return baseHeightFor(
			controlsVisible, progressVisible, 0,
			sourceVisible, titleVisible, artistVisible, artworkVisible) + 16;
	}

	private static int minimumHeightFor(int baseHeight)
	{
		return (int) Math.ceil(baseHeight * MINIMUM_SCALE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showGameOverlay())
		{
			clearControlBounds();
			return null;
		}

		SpotifyPlaybackState state = plugin.getPlaybackState();
		if (!state.isItemAvailable())
		{
			clearControlBounds();
			return null;
		}

		LyricsState lyrics = config.showOverlayLyrics()
			? plugin.getLyricsState()
			: LyricsState.empty();
		int lyricLineCount = lyrics.isAvailable()
			? Math.min(lyrics.getLineCount(), Math.max(1,
				Math.min(SpotifyControllerConfig.MAX_OVERLAY_LYRIC_LINES, config.overlayLyricsLines())))
			: 0;
		boolean requestedSourceVisible = config.showOverlaySource();
		boolean requestedTitleVisible = config.showOverlayTitle();
		boolean requestedArtistVisible = config.showOverlayArtist();
		boolean requestedArtworkVisible = config.showOverlayArtwork();
		int requestedBaseWidth = requestedBaseWidthFor(
			graphics,
			state,
			lyricLineCount,
			requestedSourceVisible,
			requestedTitleVisible,
			requestedArtistVisible,
			requestedArtworkVisible);
		updateLayoutState(
			controlsVisible,
			progressVisible,
			lyricLineCount,
			requestedSourceVisible,
			requestedTitleVisible,
			requestedArtistVisible,
			requestedArtworkVisible,
			requestedBaseWidth);
		int baseHeight = baseHeightFor(
			controlsVisible, progressVisible, lyricLineCount,
			sourceVisible, titleVisible, artistVisible, artworkVisible);
		Dimension overlaySize = getScaledOverlaySize(layoutBaseWidth, baseHeight);
		double scale = contentScaleFor(
			overlaySize.width, overlaySize.height, layoutBaseWidth, baseHeight);
		int layoutWidth = layoutWidthFor(
			overlaySize.width, overlaySize.height, layoutBaseWidth, baseHeight);
		double contentWidth = layoutWidth * scale;
		double contentHeight = baseHeight * scale;
		double contentOffsetX = (overlaySize.getWidth() - contentWidth) / 2.0;
		double contentOffsetY = (overlaySize.getHeight() - contentHeight) / 2.0;

		Graphics2D overlayGraphics = (Graphics2D) graphics.create();
		try
		{
			long nowMs = System.currentTimeMillis();
			overlayGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			overlayGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			RoundRectangle2D panel = new RoundRectangle2D.Float(0.5f, 0.5f,
				overlaySize.width - 1f, overlaySize.height - 1f, ARC, ARC);
			overlayGraphics.setColor(panelBackgroundColor());
			overlayGraphics.fill(panel);
			overlayGraphics.setColor(PANEL_BORDER);
			overlayGraphics.setStroke(new BasicStroke(1f));
			overlayGraphics.draw(panel);

			Graphics2D contentGraphics = (Graphics2D) overlayGraphics.create();
			try
			{
				contentGraphics.translate(contentOffsetX, contentOffsetY);
				contentGraphics.scale(scale, scale);
				paintContent(contentGraphics, state, lyrics, lyricLineCount, nowMs, layoutWidth,
					contentOffsetX, contentOffsetY, scale,
					sourceVisible, titleVisible, artistVisible, artworkVisible);
			}
			finally
			{
				contentGraphics.dispose();
			}
		}
		finally
		{
			overlayGraphics.dispose();
		}

		return overlaySize;
	}

	private void paintContent(
		Graphics2D graphics,
		SpotifyPlaybackState state,
		LyricsState lyrics,
		int lyricLineCount,
		long nowMs,
		int layoutWidth,
		double contentOffsetX,
		double contentOffsetY,
		double scale,
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		boolean hasDetails = titleVisible || artistVisible || artworkVisible;
		if (sourceVisible)
		{
			graphics.setFont(FontManager.getRunescapeBoldFont().deriveFont(SOURCE_FONT_SIZE));
			graphics.setColor(accentColor());
			String sourceName = sourceName();
			if (!hasDetails)
			{
				graphics.drawString(sourceName, PADDING, HEADER_BASELINE);
			}
			else
			{
				drawCentered(graphics, sourceName, HEADER_BASELINE, layoutWidth);
			}
		}

		BufferedImage artwork = artworkVisible ? plugin.getArtwork() : null;
		if (artwork != null)
		{
			paintArtwork(graphics, artwork, artworkYFor(sourceVisible));
		}
		int textX = artworkVisible ? PADDING + ARTWORK_SIZE + ARTWORK_GAP : PADDING;

		Font bodyFont = FontManager.getRunescapeFont().deriveFont(TITLE_STATUS_FONT_SIZE);
		graphics.setFont(bodyFont);
		FontMetrics bodyMetrics = graphics.getFontMetrics();
		String playbackLabel = state.isPlaying() ? "Playing" : "Paused";
		int labelWidth = sourceVisible ? bodyMetrics.stringWidth(playbackLabel) : 0;
		int labelX = layoutWidth - PADDING - labelWidth;
		int titleWidth = sourceVisible
			? Math.max(20, labelX - textX - 10)
			: Math.max(20, layoutWidth - PADDING - textX);
		if (titleVisible)
		{
			graphics.setColor(PRIMARY_TEXT);
			drawMarqueeTitle(graphics, state.getTitle(), bodyFont, textX, titleWidth, nowMs,
				titleBaselineFor(sourceVisible));
		}
		if (sourceVisible)
		{
			graphics.setFont(bodyFont);
			graphics.setColor(state.isPlaying() ? accentColor() : SECONDARY_TEXT);
			graphics.drawString(playbackLabel, labelX,
				hasDetails ? titleBaselineFor(true) : HEADER_BASELINE);
		}

		if (artistVisible)
		{
			Font artistFont = FontManager.getRunescapeSmallFont().deriveFont(ARTIST_FONT_SIZE);
			graphics.setFont(artistFont);
			graphics.setColor(SECONDARY_TEXT);
			int artistWidth = layoutWidth - PADDING - textX;
			if (sourceVisible && !titleVisible)
			{
				artistWidth = Math.max(20, artistWidth - labelWidth - 10);
			}
			drawMarqueeArtist(graphics, state.getArtist(), artistFont, textX,
				artistWidth, nowMs, artistBaselineFor(sourceVisible, titleVisible));
		}

		if (progressVisible)
		{
			int progressY = progressYFor(
				sourceVisible, titleVisible, artistVisible, artworkVisible);
			paintProgress(graphics, state, nowMs, layoutWidth, progressY,
				timeBaselineFor(sourceVisible, titleVisible, artistVisible, artworkVisible));
			updateAbsoluteProgressBounds(
				layoutWidth, progressY, contentOffsetX, contentOffsetY, scale);
		}
		else
		{
			clearProgressBounds();
		}
		if (!controlsVisible)
		{
			clearButtonBounds();
		}
		else
		{
			int controlsWidth = (BUTTON_WIDTH * 3) + (BUTTON_GAP * 2);
			int firstButtonX = (layoutWidth - controlsWidth) / 2;
			int buttonY = buttonYFor(
				progressVisible, sourceVisible, titleVisible, artistVisible, artworkVisible);
			Rectangle previousLocal = new Rectangle(firstButtonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
			Rectangle playPauseLocal = new Rectangle(firstButtonX + BUTTON_WIDTH + BUTTON_GAP,
				buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
			Rectangle nextLocal = new Rectangle(firstButtonX + ((BUTTON_WIDTH + BUTTON_GAP) * 2),
				buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);

			paintButton(graphics, previousLocal, Control.PREVIOUS, state.isPlaying());
			paintButton(graphics, playPauseLocal, Control.PLAY_PAUSE, state.isPlaying());
			paintButton(graphics, nextLocal, Control.NEXT, state.isPlaying());
			updateAbsoluteControlBounds(
				previousLocal,
				playPauseLocal,
				nextLocal,
				contentOffsetX,
				contentOffsetY,
				scale);
		}

		if (lyricLineCount > 0)
		{
			paintLyrics(graphics, lyrics, state.getEstimatedProgressMs(nowMs),
				state.getDurationMs(), layoutWidth, lyricLineCount,
				sourceVisible, titleVisible, artistVisible, artworkVisible);
		}
	}

	private void paintLyrics(
		Graphics2D graphics,
		LyricsState lyrics,
		int progressMs,
		int durationMs,
		int layoutWidth,
		int lyricLineCount,
		boolean sourceVisible,
		boolean titleVisible,
		boolean artistVisible,
		boolean artworkVisible)
	{
		LyricsState.Window window = lyrics.windowAt(progressMs, durationMs, lyricLineCount);
		int firstBaseline = lyricsFirstBaselineFor(
			controlsVisible, progressVisible,
			sourceVisible, titleVisible, artistVisible, artworkVisible);
		int availableWidth = Math.max(20, layoutWidth - (PADDING * 2));
		graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(LYRICS_FONT_SIZE));
		FontMetrics metrics = graphics.getFontMetrics();

		for (int index = 0; index < window.getLines().size(); index++)
		{
			drawLyricLine(graphics, window.getLines().get(index),
				firstBaseline + (index * LYRICS_LINE_HEIGHT), layoutWidth,
				availableWidth, metrics,
				index == window.getCurrentIndex() ? accentColor() : SECONDARY_TEXT);
		}
	}

	private static void drawLyricLine(
		Graphics2D graphics,
		String text,
		int baselineY,
		int layoutWidth,
		int availableWidth,
		FontMetrics metrics,
		Color color)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}
		graphics.setColor(color);
		drawCentered(graphics, fitText(text, metrics, availableWidth), baselineY, layoutWidth);
	}

	private static String fitText(String text, FontMetrics metrics, int availableWidth)
	{
		if (metrics.stringWidth(text) <= availableWidth)
		{
			return text;
		}
		String ellipsis = "…";
		int low = 0;
		int high = text.length();
		while (low < high)
		{
			int middle = (low + high + 1) >>> 1;
			if (metrics.stringWidth(text.substring(0, middle) + ellipsis) <= availableWidth)
			{
				low = middle;
			}
			else
			{
				high = middle - 1;
			}
		}
		return text.substring(0, low).trim() + ellipsis;
	}

	boolean handleMousePressed(MouseEvent event)
	{
		if (!config.showGameOverlay() || event.isAltDown() || !SwingUtilities.isLeftMouseButton(event))
		{
			return false;
		}

		SpotifyPlaybackState state = plugin.getPlaybackState();
		if (!state.isItemAvailable())
		{
			return false;
		}

		if (progressVisible && state.getDurationMs() > 0 && progressBounds.contains(event.getPoint()))
		{
			seeking = true;
			updateSeekPreview(event.getPoint(), state.getDurationMs());
			event.consume();
			return true;
		}

		Control control = controlAt(event.getPoint());
		if (control == null)
		{
			return false;
		}

		switch (control)
		{
			case PREVIOUS:
				plugin.previous();
				break;
			case PLAY_PAUSE:
				plugin.togglePlayPause(state.isPlaying());
				break;
			case NEXT:
				plugin.next();
				break;
			default:
				return false;
		}

		event.consume();
		return true;
	}

	boolean handleMouseDragged(MouseEvent event)
	{
		if (!seeking)
		{
			return false;
		}

		SpotifyPlaybackState state = plugin.getPlaybackState();
		if (!state.isItemAvailable() || state.getDurationMs() <= 0)
		{
			seeking = false;
			return false;
		}

		updateSeekPreview(event.getPoint(), state.getDurationMs());
		event.consume();
		return true;
	}

	boolean handleMouseReleased(MouseEvent event)
	{
		if (!seeking)
		{
			return false;
		}

		SpotifyPlaybackState state = plugin.getPlaybackState();
		if (state.isItemAvailable() && state.getDurationMs() > 0)
		{
			updateSeekPreview(event.getPoint(), state.getDurationMs());
			int requestedPositionMs = seekPreviewMs;
			seeking = false;
			plugin.seekTo(requestedPositionMs);
		}
		else
		{
			seeking = false;
		}

		progressHovered = progressBounds.contains(event.getPoint());
		event.consume();
		return true;
	}

	void handleMouseMoved(Point point)
	{
		hoveredControl = config.showGameOverlay() && controlsVisible ? controlAt(point) : null;
		progressHovered = config.showGameOverlay() && progressVisible && progressBounds.contains(point);
	}

	void clearHover()
	{
		hoveredControl = null;
		progressHovered = false;
	}

	private void paintButton(Graphics2D graphics, Rectangle bounds, Control control, boolean playing)
	{
		boolean highlighted = control == hoveredControl;
		RoundRectangle2D button = new RoundRectangle2D.Float(bounds.x + 0.5f, bounds.y + 0.5f,
			bounds.width - 1f, bounds.height - 1f, 6f, 6f);
		graphics.setColor(highlighted ? accentColor(42) : BUTTON_BACKGROUND);
		graphics.fill(button);
		graphics.setColor(highlighted ? accentColor() : BUTTON_BORDER);
		graphics.setStroke(new BasicStroke(1f));
		graphics.draw(button);

		graphics.setColor(playbackButtonColor());
		graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		int centerX = bounds.x + (bounds.width / 2);
		int centerY = bounds.y + (bounds.height / 2);
		switch (control)
		{
			case PREVIOUS:
				graphics.drawLine(centerX - 7, centerY - 6, centerX - 7, centerY + 6);
				fillTriangle(graphics, centerX + 6, centerY - 7, centerX + 6, centerY + 7,
					centerX - 5, centerY);
				break;
			case PLAY_PAUSE:
				if (playing)
				{
					graphics.fillRoundRect(centerX - 6, centerY - 7, 4, 14, 2, 2);
					graphics.fillRoundRect(centerX + 2, centerY - 7, 4, 14, 2, 2);
				}
				else
				{
					fillTriangle(graphics, centerX - 5, centerY - 7, centerX - 5, centerY + 7,
						centerX + 7, centerY);
				}
				break;
			case NEXT:
				fillTriangle(graphics, centerX - 6, centerY - 7, centerX - 6, centerY + 7,
					centerX + 5, centerY);
				graphics.drawLine(centerX + 7, centerY - 6, centerX + 7, centerY + 6);
				break;
			default:
				break;
		}
	}

	private void paintProgress(
		Graphics2D graphics,
		SpotifyPlaybackState state,
		long nowMs,
		int layoutWidth,
		int progressY,
		int timeBaseline)
	{
		int progressWidth = Math.max(1, layoutWidth - (PADDING * 2));
		int durationMs = state.getDurationMs();
		int progressMs = seeking
			? Math.max(0, Math.min(durationMs, seekPreviewMs))
			: state.getEstimatedProgressMs(nowMs);
		double fraction = durationMs <= 0 ? 0.0 : Math.min(1.0, (double) progressMs / durationMs);
		int filledWidth = (int) Math.round(progressWidth * fraction);

		graphics.setColor(progressTrackColor());
		graphics.fillRoundRect(PADDING, progressY, progressWidth, PROGRESS_HEIGHT,
			PROGRESS_HEIGHT, PROGRESS_HEIGHT);
		if (filledWidth > 0)
		{
			graphics.setColor(playbackProgressColor());
			graphics.fillRoundRect(PADDING, progressY, filledWidth, PROGRESS_HEIGHT,
				PROGRESS_HEIGHT, PROGRESS_HEIGHT);
		}

		if (durationMs > 0)
		{
			int markerX = PADDING + filledWidth;
			markerX = Math.max(PADDING, Math.min(PADDING + progressWidth, markerX));
			graphics.setColor(playbackProgressColor());
			graphics.fillOval(markerX - 4, progressY - 2, 8, 8);
		}

		graphics.setFont(FontManager.getRunescapeSmallFont().deriveFont(TIME_FONT_SIZE));
		graphics.setColor(SECONDARY_TEXT);
		String elapsed = formatTime(progressMs);
		String remaining = "-" + formatTime(Math.max(0, durationMs - progressMs));
		graphics.drawString(elapsed, PADDING, timeBaseline);
		graphics.drawString(remaining,
			layoutWidth - PADDING - graphics.getFontMetrics().stringWidth(remaining), timeBaseline);
	}

	private Color accentColor()
	{
		Color color = config.overlayAccentColor();
		return color == null ? new Color(29, 185, 84) : color;
	}

	private String sourceName()
	{
		return "Spotify";
	}

	private Color accentColor(int alpha)
	{
		Color color = accentColor();
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private Color progressTrackColor()
	{
		return withAlpha(config.overlayProgressTrackColor(), DEFAULT_PROGRESS_TRACK, PROGRESS_TRACK_ALPHA);
	}

	private Color playbackProgressColor()
	{
		return withAlpha(config.overlayPlaybackProgressColor(), DEFAULT_PLAYBACK_PROGRESS,
			PLAYBACK_PROGRESS_ALPHA);
	}

	private Color panelBackgroundColor()
	{
		Color color = config.overlayBackgroundColor();
		return color == null ? DEFAULT_PANEL_BACKGROUND : color;
	}

	private Color playbackButtonColor()
	{
		Color color = config.overlayPlaybackButtonColor();
		return color == null
			? DEFAULT_PLAYBACK_BUTTON
			: new Color(color.getRed(), color.getGreen(), color.getBlue());
	}

	private static Color withAlpha(Color color, Color fallback, int alpha)
	{
		Color selected = color == null ? fallback : color;
		return new Color(selected.getRed(), selected.getGreen(), selected.getBlue(), alpha);
	}

	private void drawMarqueeTitle(
		Graphics2D graphics,
		String title,
		Font font,
		int startX,
		int availableWidth,
		long nowMs,
		int baselineY)
	{
		String safeTitle = title == null ? "" : title;
		if (!safeTitle.equals(marqueeTitle))
		{
			marqueeTitle = safeTitle;
			marqueeStartedAtMs = nowMs;
		}

		int textWidth = UnicodeText.stringWidth(graphics, font, safeTitle);
		if (textWidth <= availableWidth)
		{
			UnicodeText.drawString(graphics, font, safeTitle, startX, baselineY);
			return;
		}

		int overflow = textWidth - availableWidth;
		long scrollDurationMs = Math.max(1L,
			(long) Math.ceil((overflow * 1_000.0) / MARQUEE_PIXELS_PER_SECOND));
		long cycleMs = MARQUEE_START_PAUSE_MS + scrollDurationMs + MARQUEE_END_PAUSE_MS;
		long phaseMs = Math.floorMod(nowMs - marqueeStartedAtMs, cycleMs);
		int offset;
		if (phaseMs < MARQUEE_START_PAUSE_MS)
		{
			offset = 0;
		}
		else if (phaseMs < MARQUEE_START_PAUSE_MS + scrollDurationMs)
		{
			offset = (int) Math.min(overflow,
				Math.round(((phaseMs - MARQUEE_START_PAUSE_MS) / 1_000.0) * MARQUEE_PIXELS_PER_SECOND));
		}
		else
		{
			offset = overflow;
		}

		Shape oldClip = graphics.getClip();
		graphics.clip(new Rectangle(startX, baselineY - 16, availableWidth, 20));
		UnicodeText.drawString(graphics, font, safeTitle, startX - offset, baselineY);
		graphics.setClip(oldClip);
	}

	private void drawMarqueeArtist(
		Graphics2D graphics,
		String artist,
		Font font,
		int startX,
		int availableWidth,
		long nowMs,
		int baselineY)
	{
		String safeArtist = artist == null ? "" : artist;
		if (!safeArtist.equals(marqueeArtist))
		{
			marqueeArtist = safeArtist;
			artistMarqueeStartedAtMs = nowMs;
		}

		int textWidth = UnicodeText.stringWidth(graphics, font, safeArtist);
		if (textWidth <= availableWidth)
		{
			UnicodeText.drawString(graphics, font, safeArtist, startX, baselineY);
			return;
		}

		int overflow = textWidth - availableWidth;
		long scrollDurationMs = Math.max(1L,
			(long) Math.ceil((overflow * 1_000.0) / ARTIST_MARQUEE_PIXELS_PER_SECOND));
		long cycleMs = ARTIST_MARQUEE_START_PAUSE_MS + scrollDurationMs + ARTIST_MARQUEE_END_PAUSE_MS;
		long phaseMs = Math.floorMod(nowMs - artistMarqueeStartedAtMs, cycleMs);
		int offset;
		if (phaseMs < ARTIST_MARQUEE_START_PAUSE_MS)
		{
			offset = 0;
		}
		else if (phaseMs < ARTIST_MARQUEE_START_PAUSE_MS + scrollDurationMs)
		{
			offset = (int) Math.min(overflow,
				Math.round(((phaseMs - ARTIST_MARQUEE_START_PAUSE_MS) / 1_000.0) *
					ARTIST_MARQUEE_PIXELS_PER_SECOND));
		}
		else
		{
			offset = overflow;
		}

		Shape oldClip = graphics.getClip();
		graphics.clip(new Rectangle(startX, baselineY - 16, availableWidth, 20));
		UnicodeText.drawString(graphics, font, safeArtist, startX - offset, baselineY);
		graphics.setClip(oldClip);
	}

	private void paintArtwork(Graphics2D graphics, BufferedImage artwork, int artworkY)
	{
		Shape oldClip = graphics.getClip();
		RoundRectangle2D artworkClip = new RoundRectangle2D.Float(
			PADDING, artworkY, ARTWORK_SIZE, ARTWORK_SIZE, 5f, 5f);
		graphics.clip(artworkClip);
		graphics.drawImage(artwork, PADDING, artworkY, ARTWORK_SIZE, ARTWORK_SIZE, null);
		graphics.setClip(oldClip);
		graphics.setColor(PANEL_BORDER);
		graphics.setStroke(new BasicStroke(1f));
		graphics.draw(artworkClip);
	}

	private static void fillTriangle(Graphics2D graphics, int x1, int y1, int x2, int y2, int x3, int y3)
	{
		Path2D triangle = new Path2D.Float();
		triangle.moveTo(x1, y1);
		triangle.lineTo(x2, y2);
		triangle.lineTo(x3, y3);
		triangle.closePath();
		graphics.fill(triangle);
	}

	private static void drawCentered(Graphics2D graphics, String text, int baselineY, int layoutWidth)
	{
		int x = (layoutWidth - graphics.getFontMetrics().stringWidth(text)) / 2;
		graphics.drawString(text, x, baselineY);
	}

	private static String formatTime(int milliseconds)
	{
		int totalSeconds = Math.max(0, milliseconds / 1_000);
		int seconds = totalSeconds % 60;
		int totalMinutes = totalSeconds / 60;
		if (totalMinutes >= 60)
		{
			return (totalMinutes / 60) + ":" + twoDigits(totalMinutes % 60) + ":" + twoDigits(seconds);
		}
		return totalMinutes + ":" + twoDigits(seconds);
	}

	private static String twoDigits(int value)
	{
		return value < 10 ? "0" + value : Integer.toString(value);
	}

	private Dimension getScaledOverlaySize(int baseWidth, int baseHeight)
	{
		Dimension preferredSize = getPreferredSize();
		if (preferredSize == null || preferredSize.width <= 0 || preferredSize.height <= 0)
		{
			return new Dimension(baseWidth, baseHeight);
		}
		return new Dimension(
			Math.max(minimumWidthFor(baseWidth), preferredSize.width),
			Math.max(minimumHeightFor(baseHeight),
				preferredSize.height));
	}

	static int layoutWidthFor(int overlayWidth, int overlayHeight, int baseHeight)
	{
		return layoutWidthFor(overlayWidth, overlayHeight, BASE_WIDTH, baseHeight);
	}

	static int layoutWidthFor(int overlayWidth, int overlayHeight, int baseWidth, int baseHeight)
	{
		double scale = contentScaleFor(overlayWidth, overlayHeight, baseWidth, baseHeight);
		return Math.max(baseWidth, (int) Math.floor(Math.max(1, overlayWidth) / scale));
	}

	private static double contentScaleFor(
		int overlayWidth,
		int overlayHeight,
		int baseWidth,
		int baseHeight)
	{
		double widthScale = Math.max(1, overlayWidth) / (double) Math.max(1, baseWidth);
		double heightScale = Math.max(1, overlayHeight) / (double) Math.max(1, baseHeight);
		return Math.max(0.01, Math.min(widthScale, heightScale));
	}

	private static int minimumWidthFor(int baseWidth)
	{
		return Math.max(MINIMUM_RENDERED_WIDTH,
			(int) Math.ceil(baseWidth * MINIMUM_SCALE));
	}

	private void updateAbsoluteControlBounds(
		Rectangle previous,
		Rectangle playPause,
		Rectangle next,
		double contentOffsetX,
		double contentOffsetY,
		double scale)
	{
		Point overlayLocation = getBounds().getLocation();
		previousBounds = scaledAndTranslated(previous, overlayLocation, contentOffsetX, contentOffsetY, scale);
		playPauseBounds = scaledAndTranslated(playPause, overlayLocation, contentOffsetX, contentOffsetY, scale);
		nextBounds = scaledAndTranslated(next, overlayLocation, contentOffsetX, contentOffsetY, scale);
	}

	private void updateAbsoluteProgressBounds(
		int layoutWidth,
		int progressY,
		double contentOffsetX,
		double contentOffsetY,
		double scale)
	{
		int hitY = progressY - ((PROGRESS_HIT_HEIGHT - PROGRESS_HEIGHT) / 2);
		int progressWidth = Math.max(1, layoutWidth - (PADDING * 2));
		Rectangle localBounds = new Rectangle(PADDING, hitY, progressWidth, PROGRESS_HIT_HEIGHT);
		progressBounds = scaledAndTranslated(
			localBounds,
			getBounds().getLocation(),
			contentOffsetX,
			contentOffsetY,
			scale);
	}

	private void updateSeekPreview(Point point, int durationMs)
	{
		Rectangle bounds = progressBounds;
		if (bounds.width <= 0)
		{
			seekPreviewMs = 0;
			return;
		}

		double fraction = (point.getX() - bounds.getMinX()) / bounds.getWidth();
		fraction = Math.max(0.0, Math.min(1.0, fraction));
		seekPreviewMs = (int) Math.round(durationMs * fraction);
	}

	private static Rectangle scaledAndTranslated(
		Rectangle rectangle,
		Point overlayLocation,
		double contentOffsetX,
		double contentOffsetY,
		double scale)
	{
		int left = overlayLocation.x + (int) Math.floor(contentOffsetX + (rectangle.x * scale));
		int top = overlayLocation.y + (int) Math.floor(contentOffsetY + (rectangle.y * scale));
		int right = overlayLocation.x +
			(int) Math.ceil(contentOffsetX + ((rectangle.x + rectangle.width) * scale));
		int bottom = overlayLocation.y +
			(int) Math.ceil(contentOffsetY + ((rectangle.y + rectangle.height) * scale));
		return new Rectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
	}

	private Control controlAt(Point point)
	{
		if (previousBounds.contains(point))
		{
			return Control.PREVIOUS;
		}
		if (playPauseBounds.contains(point))
		{
			return Control.PLAY_PAUSE;
		}
		if (nextBounds.contains(point))
		{
			return Control.NEXT;
		}
		return null;
	}

	private void clearControlBounds()
	{
		clearButtonBounds();
		clearProgressBounds();
	}

	private void clearProgressBounds()
	{
		progressBounds = new Rectangle();
		progressHovered = false;
		seeking = false;
		seekPreviewMs = 0;
	}

	private void clearButtonBounds()
	{
		previousBounds = new Rectangle();
		playPauseBounds = new Rectangle();
		nextBounds = new Rectangle();
		hoveredControl = null;
	}

	private enum Control
	{
		PREVIOUS,
		PLAY_PAUSE,
		NEXT
	}
}
