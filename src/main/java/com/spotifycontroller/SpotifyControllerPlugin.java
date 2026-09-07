package com.spotifycontroller;

import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Spotify Controller",
	description = "Control Spotify from RuneLite with playback controls, search, queue, artwork, and lyrics",
	tags = {"spotify", "music", "media", "now playing"}
)
public class SpotifyControllerPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(SpotifyControllerPlugin.class);
	static final String PLUGIN_VERSION = "1.25.0";

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SpotifyControllerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SpotifyApiClient spotify;

	@Inject
	private LyricsClient lyricsClient;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SpotifyOverlay spotifyOverlay;

	@Inject
	private MouseManager mouseManager;

	private SpotifyPanel panel;
	private NavigationButton navigationButton;
	private volatile boolean spotifyConfigured;
	private volatile SpotifyPlaybackState playbackState = SpotifyPlaybackState.idle();
	private volatile SpotifyPlaybackState spotifyPlaybackState = SpotifyPlaybackState.idle();
	private volatile SpotifyQueueState queueState = SpotifyQueueState.empty();
	private volatile SpotifyQueueState spotifyQueueState = SpotifyQueueState.empty();
	private volatile BufferedImage artwork;
	private volatile String requestedArtworkUrl = "";
	private volatile LyricsState lyricsState = LyricsState.empty();
	private volatile String requestedLyricsKey = "";
	private volatile Boolean optimisticPlaying;
	private volatile long optimisticPlayingUntilMs;
	private volatile Integer optimisticSeekPositionMs;
	private volatile long optimisticSeekStartedAtMs;
	private volatile long optimisticSeekUntilMs;
	private volatile Integer optimisticVolumePercent;
	private volatile long optimisticVolumeUntilMs;
	private volatile Boolean optimisticShuffleEnabled;
	private volatile long optimisticShuffleUntilMs;
	private volatile String optimisticRepeatMode;
	private volatile long optimisticRepeatUntilMs;
	private final MouseAdapter overlayMouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			spotifyOverlay.handleMousePressed(event);
			return event;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent event)
		{
			spotifyOverlay.handleMouseReleased(event);
			return event;
		}

		@Override
		public MouseEvent mouseDragged(MouseEvent event)
		{
			spotifyOverlay.handleMouseDragged(event);
			return event;
		}

		@Override
		public MouseEvent mouseMoved(MouseEvent event)
		{
			spotifyOverlay.handleMouseMoved(event.getPoint());
			return event;
		}

		@Override
		public MouseEvent mouseExited(MouseEvent event)
		{
			spotifyOverlay.clearHover();
			return event;
		}
	};
	private final SpotifyListener spotifySourceListener = new SourceListener();

	@Override
	protected void startUp()
	{
		synchronizeOverlayConfiguration("showGameOverlay");
		spotifyConfigured = isSpotifyConfigured();
		playbackState = SpotifyPlaybackState.idle();
		spotifyPlaybackState = SpotifyPlaybackState.idle();
		queueState = SpotifyQueueState.empty();
		spotifyQueueState = SpotifyQueueState.empty();
		artwork = null;
		requestedArtworkUrl = "";
		lyricsState = LyricsState.empty();
		requestedLyricsKey = "";
		lyricsClient.reset();
		panel = new SpotifyPanel(this, config);
		panel.setConfigurationState(config.enableSpotifyAccess(), hasClientId());
		panel.setSystemMediaMode(false);
		panel.setOverlayVisible(config.showGameOverlay());
		panel.setOverlaySourceVisible(config.showOverlaySource());
		panel.setOverlayTitleVisible(config.showOverlayTitle());
		panel.setOverlayArtistVisible(config.showOverlayArtist());
		panel.setOverlayControlsVisible(config.showOverlayControls());
		spotifyOverlay.setControlsVisible(config.showOverlayControls());
		panel.setOverlayProgressVisible(config.showOverlayProgress());
		spotifyOverlay.setProgressVisible(config.showOverlayProgress());
		panel.setOverlayArtworkVisible(config.showOverlayArtwork());
		panel.setOverlayLyricsVisible(config.showOverlayLyrics());
		panel.setOverlayLyricsLines(config.overlayLyricsLines());
		navigationButton = NavigationButton.builder()
			.tooltip("Spotify Controller")
			.icon(createMusicIcon())
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(spotifyOverlay);
		mouseManager.registerMouseListener(overlayMouseListener);
		startPlaybackSources();
		log.debug("Spotify Controller started");
	}

	@Override
	protected void shutDown()
	{
		spotify.stop();
		mouseManager.unregisterMouseListener(overlayMouseListener);
		overlayManager.remove(spotifyOverlay);
		playbackState = SpotifyPlaybackState.idle();
		spotifyPlaybackState = SpotifyPlaybackState.idle();
		queueState = SpotifyQueueState.empty();
		spotifyQueueState = SpotifyQueueState.empty();
		artwork = null;
		requestedArtworkUrl = "";
		lyricsState = LyricsState.empty();
		requestedLyricsKey = "";
		lyricsClient.reset();
		optimisticPlaying = null;
		optimisticPlayingUntilMs = 0;
		optimisticSeekPositionMs = null;
		optimisticSeekStartedAtMs = 0;
		optimisticSeekUntilMs = 0;
		optimisticVolumePercent = null;
		optimisticVolumeUntilMs = 0;
		optimisticShuffleEnabled = null;
		optimisticShuffleUntilMs = 0;
		optimisticRepeatMode = null;
		optimisticRepeatUntilMs = 0;
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		if (panel != null)
		{
			panel.stop();
			panel = null;
		}
		log.debug("Spotify Controller stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SpotifyControllerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		synchronizeOverlayConfiguration(event.getKey());
		boolean requestedSpotifyConfiguration = isSpotifyConfigured();
		if (requestedSpotifyConfiguration != spotifyConfigured)
		{
			updateSpotifyConfiguration(requestedSpotifyConfiguration);
		}
		else if (spotifyConfigured)
		{
			spotify.configurationChanged(event.getKey());
		}
		spotifyOverlay.setControlsVisible(config.showOverlayControls());
		spotifyOverlay.setProgressVisible(config.showOverlayProgress());
		updateLyricsFor(playbackState);
		SpotifyPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				currentPanel.setConfigurationState(config.enableSpotifyAccess(), hasClientId());
				currentPanel.setSystemMediaMode(false);
				currentPanel.setOverlayVisible(config.showGameOverlay());
				currentPanel.setOverlaySourceVisible(config.showOverlaySource());
				currentPanel.setOverlayTitleVisible(config.showOverlayTitle());
				currentPanel.setOverlayArtistVisible(config.showOverlayArtist());
				currentPanel.setOverlayControlsVisible(config.showOverlayControls());
				currentPanel.setOverlayProgressVisible(config.showOverlayProgress());
				currentPanel.setOverlayArtworkVisible(config.showOverlayArtwork());
				currentPanel.setOverlayLyricsVisible(config.showOverlayLyrics());
				currentPanel.setOverlayLyricsLines(config.overlayLyricsLines());
				currentPanel.refreshTheme();
			});
		}
	}

	void connectSpotify()
	{
		spotify.beginAuthorization();
	}

	void disconnectSpotify()
	{
		spotify.disconnect();
	}

	void refreshSpotify()
	{
		if (spotifyConfigured)
		{
			spotify.retryNow();
		}
	}

	void searchTracks(String query)
	{
		spotify.searchTracks(
			query,
			results ->
			{
				SpotifyPanel currentPanel = panel;
				if (currentPanel != null)
				{
					SwingUtilities.invokeLater(() -> currentPanel.setSearchResults(query, results));
				}
			},
			error ->
			{
				SpotifyPanel currentPanel = panel;
				if (currentPanel != null)
				{
					SwingUtilities.invokeLater(() -> currentPanel.setSearchError(error));
				}
			});
	}

	void playSearchResult(SpotifySearchResult result)
	{
		spotify.playSearchResult(
			result.getUri(),
			() ->
			{
				SpotifyPanel currentPanel = panel;
				if (currentPanel != null)
				{
					SwingUtilities.invokeLater(() -> currentPanel.setSearchPlaybackStarted(result));
				}
			},
			error ->
			{
				SpotifyPanel currentPanel = panel;
				if (currentPanel != null)
				{
					SwingUtilities.invokeLater(() -> currentPanel.setSearchError(error));
				}
			});
	}

	void loadSearchArtwork(SpotifySearchResult result)
	{
		String artworkUrl = result.getArtworkUrl();
		if (artworkUrl.isEmpty())
		{
			return;
		}
		spotify.loadArtwork(artworkUrl, image ->
		{
			SpotifyPanel currentPanel = panel;
			if (currentPanel != null)
			{
				SwingUtilities.invokeLater(() -> currentPanel.setSearchArtwork(
					result.getUri(), artworkUrl, image));
			}
		});
	}

	void setGameOverlayVisible(boolean visible)
	{
		if (visible)
		{
			if (!hasAnyOverlaySectionEnabled())
			{
				configManager.setConfiguration(
					SpotifyControllerConfig.GROUP, "showOverlaySource", true);
			}
		}
		else
		{
			setAllOverlaySections(false);
		}
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "showGameOverlay", visible);
	}

	void setGameOverlaySourceVisible(boolean visible)
	{
		setOverlaySectionConfiguration("showOverlaySource", visible);
	}

	void setGameOverlayTitleVisible(boolean visible)
	{
		setOverlaySectionConfiguration("showOverlayTitle", visible);
	}

	void setGameOverlayArtistVisible(boolean visible)
	{
		setOverlaySectionConfiguration("showOverlayArtist", visible);
	}

	void setGameOverlayControlsVisible(boolean visible)
	{
		spotifyOverlay.setControlsVisible(visible);
		setOverlaySectionConfiguration("showOverlayControls", visible);
	}

	void setGameOverlayProgressVisible(boolean visible)
	{
		spotifyOverlay.setProgressVisible(visible);
		setOverlaySectionConfiguration("showOverlayProgress", visible);
	}

	void setGameOverlayArtworkVisible(boolean visible)
	{
		setOverlaySectionConfiguration("showOverlayArtwork", visible);
	}

	void setGameOverlayLyricsVisible(boolean visible)
	{
		setOverlaySectionConfiguration("showOverlayLyrics", visible);
	}

	void setGameOverlayLyricsLines(int lines)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "overlayLyricsLines",
			Math.max(1, Math.min(SpotifyControllerConfig.MAX_OVERLAY_LYRIC_LINES, lines)));
	}

	private void setOverlaySectionConfiguration(String key, boolean visible)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, key, visible);
		configManager.setConfiguration(
			SpotifyControllerConfig.GROUP, "showGameOverlay", hasAnyOverlaySectionEnabled());
	}

	private void synchronizeOverlayConfiguration(String changedKey)
	{
		if ("showGameOverlay".equals(changedKey))
		{
			if (config.showGameOverlay())
			{
				if (!hasAnyOverlaySectionEnabled())
				{
					configManager.setConfiguration(
						SpotifyControllerConfig.GROUP, "showOverlaySource", true);
				}
			}
			else
			{
				setAllOverlaySections(false);
			}
		}
		else if (isOverlaySectionKey(changedKey))
		{
			configManager.setConfiguration(
				SpotifyControllerConfig.GROUP, "showGameOverlay", hasAnyOverlaySectionEnabled());
		}
	}

	private void setAllOverlaySections(boolean visible)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "showOverlaySource", visible);
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "showOverlayTitle", visible);
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "showOverlayArtist", visible);
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "showOverlayControls", visible);
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "showOverlayProgress", visible);
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "showOverlayArtwork", visible);
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "showOverlayLyrics", visible);
	}

	private boolean hasAnyOverlaySectionEnabled()
	{
		return hasVisibleOverlaySection(
			config.showOverlaySource(),
			config.showOverlayTitle(),
			config.showOverlayArtist(),
			config.showOverlayControls(),
			config.showOverlayProgress(),
			config.showOverlayArtwork(),
			config.showOverlayLyrics());
	}

	static boolean hasVisibleOverlaySection(
		boolean source,
		boolean title,
		boolean artist,
		boolean controls,
		boolean progress,
		boolean artwork,
		boolean lyrics)
	{
		return source || title || artist || controls || progress || artwork || lyrics;
	}

	private static boolean isOverlaySectionKey(String key)
	{
		return "showOverlaySource".equals(key) ||
			"showOverlayTitle".equals(key) ||
			"showOverlayArtist".equals(key) ||
			"showOverlayControls".equals(key) ||
			"showOverlayProgress".equals(key) ||
			"showOverlayArtwork".equals(key) ||
			"showOverlayLyrics".equals(key);
	}

	void setOverlayAccentColor(Color color)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "overlayAccentColor", color);
	}

	void setSidebarAccentColor(Color color)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "sidebarAccentColor", color);
	}

	void setOverlayProgressTrackColor(Color color)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "overlayProgressTrackColor", color);
	}

	void setOverlayPlaybackProgressColor(Color color)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "overlayPlaybackProgressColor", color);
	}

	void setOverlayBackgroundColor(Color color)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "overlayBackgroundColor", color);
	}

	void setOverlayPlaybackButtonColor(Color color)
	{
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, "overlayPlaybackButtonColor", color);
	}

	void previous()
	{
		spotify.previous();
	}

	void next()
	{
		spotify.next();
	}

	void togglePlayPause(boolean currentlyPlaying)
	{
		boolean newPlaying = !currentlyPlaying;
		SpotifyPlaybackState currentState = playbackState;
		if (currentState.isItemAvailable())
		{
			optimisticPlayingUntilMs = System.currentTimeMillis() + 1_500L;
			optimisticPlaying = newPlaying;
			SpotifyPlaybackState optimisticState = currentState.withPlaying(newPlaying);
			spotifyPlaybackState = optimisticState;
			publishPlaybackState(optimisticState);
		}
		spotify.setPlaying(newPlaying);
	}

	void seekTo(int positionMs)
	{
		SpotifyPlaybackState currentState = playbackState;
		if (!currentState.isItemAvailable() || currentState.getDurationMs() <= 0)
		{
			return;
		}

		int clampedPositionMs = Math.max(0, Math.min(currentState.getDurationMs(), positionMs));
		long nowMs = System.currentTimeMillis();
		optimisticSeekPositionMs = clampedPositionMs;
		optimisticSeekStartedAtMs = nowMs;
		optimisticSeekUntilMs = nowMs + 2_000L;
		SpotifyPlaybackState optimisticState = currentState.withProgress(clampedPositionMs);
		spotifyPlaybackState = optimisticState;
		publishPlaybackState(optimisticState);
		spotify.seekTo(clampedPositionMs);
	}

	void setVolume(int volumePercent)
	{
		SpotifyPlaybackState currentState = playbackState;
		if (!currentState.isItemAvailable() || !currentState.isVolumeAvailable())
		{
			return;
		}

		int clampedVolume = Math.max(0, Math.min(100, volumePercent));
		optimisticVolumePercent = clampedVolume;
		optimisticVolumeUntilMs = System.currentTimeMillis() + 1_500L;
		SpotifyPlaybackState optimisticState = currentState.withVolume(clampedVolume);
		spotifyPlaybackState = optimisticState;
		spotify.setVolume(clampedVolume);
		publishPlaybackState(optimisticState);
	}

	void toggleShuffle(boolean currentlyEnabled)
	{
		SpotifyPlaybackState currentState = playbackState;
		if (!currentState.isItemAvailable())
		{
			return;
		}

		boolean enabled = !currentlyEnabled;
		optimisticShuffleEnabled = enabled;
		optimisticShuffleUntilMs = System.currentTimeMillis() + 2_000L;
		SpotifyPlaybackState optimisticState = currentState.withShuffleEnabled(enabled);
		spotifyPlaybackState = optimisticState;
		publishPlaybackState(optimisticState);
		spotify.setShuffle(enabled);
	}

	void cycleRepeatMode(String currentMode)
	{
		SpotifyPlaybackState currentState = playbackState;
		if (!currentState.isItemAvailable())
		{
			return;
		}

		String repeatMode = nextRepeatMode(currentMode);
		optimisticRepeatMode = repeatMode;
		optimisticRepeatUntilMs = System.currentTimeMillis() + 2_000L;
		SpotifyPlaybackState optimisticState = currentState.withRepeatMode(repeatMode);
		spotifyPlaybackState = optimisticState;
		publishPlaybackState(optimisticState);
		spotify.setRepeatMode(repeatMode);
	}

	static String nextRepeatMode(String currentMode)
	{
		if ("context".equals(currentMode))
		{
			return "track";
		}
		if ("track".equals(currentMode))
		{
			return "off";
		}
		return "context";
	}

	void playFromQueue(List<SpotifyQueueItem> visibleQueue, int selectedIndex)
	{
		SpotifyPlaybackState currentState = playbackState;
		int volumePercent = currentState.isVolumeAvailable()
			? currentState.getVolumePercent()
			: -1;
		spotify.playFromQueue(
			visibleQueue,
			selectedIndex,
			currentState.isPlaying(),
			volumePercent);
	}

	void reorderQueue(List<SpotifyQueueItem> reorderedItems)
	{
		queueState = new SpotifyQueueState(reorderedItems);
		spotifyQueueState = queueState;
		spotify.replaceQueue(playbackState, reorderedItems);
	}

	private void handleSourcePlaybackState(SpotifyPlaybackState state)
	{
		spotifyPlaybackState = state;
		acceptSelectedPlaybackState(state);
	}

	private void handleQueueTransitionChanged(boolean active)
	{
		if (active && spotifyPlaybackState.isPlaying())
		{
			SpotifyPlaybackState pausedState = spotifyPlaybackState.withPlaying(false);
			spotifyPlaybackState = pausedState;
			publishPlaybackState(pausedState);
		}
	}

	private void acceptSelectedPlaybackState(SpotifyPlaybackState state)
	{
		Boolean expectedPlaying = optimisticPlaying;
		long nowMs = System.currentTimeMillis();
		Integer expectedVolume = optimisticVolumePercent;
		if (expectedVolume != null)
		{
			if (nowMs < optimisticVolumeUntilMs &&
				(!state.isVolumeAvailable() || state.getVolumePercent() != expectedVolume))
			{
				state = state.withVolume(expectedVolume);
			}
			else
			{
				optimisticVolumePercent = null;
				optimisticVolumeUntilMs = 0;
			}
		}
		Boolean expectedShuffle = optimisticShuffleEnabled;
		if (expectedShuffle != null)
		{
			if (nowMs < optimisticShuffleUntilMs && state.isItemAvailable() &&
				state.isShuffleEnabled() != expectedShuffle)
			{
				state = state.withShuffleEnabled(expectedShuffle);
			}
			else
			{
				optimisticShuffleEnabled = null;
				optimisticShuffleUntilMs = 0;
			}
		}
		String expectedRepeatMode = optimisticRepeatMode;
		if (expectedRepeatMode != null)
		{
			if (nowMs < optimisticRepeatUntilMs && state.isItemAvailable() &&
				!expectedRepeatMode.equals(state.getRepeatMode()))
			{
				state = state.withRepeatMode(expectedRepeatMode);
			}
			else
			{
				optimisticRepeatMode = null;
				optimisticRepeatUntilMs = 0;
			}
		}
		boolean stalePlayingState = expectedPlaying != null && nowMs < optimisticPlayingUntilMs &&
			state.isItemAvailable() && state.isPlaying() != expectedPlaying;

		Integer expectedSeekPositionMs = optimisticSeekPositionMs;
		boolean sameItemAsCurrent = state.isItemAvailable() &&
			state.getTitle().equals(playbackState.getTitle()) &&
			state.getArtist().equals(playbackState.getArtist());
		int expectedProgressMs = expectedSeekPositionMs == null
			? 0
			: expectedSeekPositionMs + (state.isPlaying()
				? (int) Math.max(0L, nowMs - optimisticSeekStartedAtMs)
				: 0);
		boolean staleSeekState = expectedSeekPositionMs != null && nowMs < optimisticSeekUntilMs &&
			sameItemAsCurrent && Math.abs(state.getProgressMs() - expectedProgressMs) > 2_000;

		if (stalePlayingState || staleSeekState)
		{
			return;
		}
		if (expectedPlaying != null &&
			(nowMs >= optimisticPlayingUntilMs || !state.isItemAvailable() || state.isPlaying() == expectedPlaying))
		{
			optimisticPlaying = null;
			optimisticPlayingUntilMs = 0;
		}
		if (expectedSeekPositionMs != null)
		{
			optimisticSeekPositionMs = null;
			optimisticSeekStartedAtMs = 0;
			optimisticSeekUntilMs = 0;
		}
		publishPlaybackState(state);
	}

	private void publishPlaybackState(SpotifyPlaybackState state)
	{
		playbackState = state;
		SpotifyPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(() -> currentPanel.setPlaybackState(state));
		}
		loadArtworkFor(state);
		updateLyricsFor(state);
	}

	private void updateLyricsFor(SpotifyPlaybackState state)
	{
		if (!config.showOverlayLyrics() || !LyricsClient.canLookup(state))
		{
			requestedLyricsKey = "";
			lyricsState = LyricsState.empty();
			return;
		}

		String key = LyricsClient.keyFor(state);
		if (key.equals(requestedLyricsKey))
		{
			return;
		}
		requestedLyricsKey = key;
		lyricsState = LyricsState.empty();
		lyricsClient.requestLyrics(state, lyrics ->
		{
			if (config.showOverlayLyrics() && key.equals(requestedLyricsKey))
			{
				lyricsState = lyrics;
			}
		});
	}

	private void loadArtworkFor(SpotifyPlaybackState state)
	{
		String artworkUrl = state.getArtworkUrl();
		if (artworkUrl.isEmpty())
		{
			requestedArtworkUrl = "";
			artwork = null;
			SpotifyPanel currentPanel = panel;
			if (currentPanel != null)
			{
				SwingUtilities.invokeLater(() -> currentPanel.setArtwork(null));
			}
			return;
		}
		if (artworkUrl.equals(requestedArtworkUrl))
		{
			return;
		}

		requestedArtworkUrl = artworkUrl;
		artwork = null;
		SpotifyPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(() -> currentPanel.setArtwork(null));
		}
		spotify.loadArtwork(artworkUrl, image -> publishArtwork(artworkUrl, image));
	}

	private void publishArtwork(String artworkUrl, BufferedImage image)
	{
		SpotifyPanel targetPanel = panel;
		if (artworkUrl.equals(playbackState.getArtworkUrl()))
		{
			artwork = image;
			if (targetPanel != null)
			{
				SwingUtilities.invokeLater(() -> targetPanel.setArtwork(image));
			}
		}
	}

	SpotifyPlaybackState getPlaybackState()
	{
		return playbackState;
	}

	LyricsState getLyricsState()
	{
		return lyricsState;
	}

	BufferedImage getArtwork()
	{
		return artwork;
	}

	boolean isSystemMediaMode()
	{
		return false;
	}

	private void handleSourceStatus(String message)
	{
		log.debug("Spotify status: {}", message);
		SpotifyPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(() -> currentPanel.setSpotifyApiStatus(message));
		}
	}

	private void handleSourceQueueState(SpotifyQueueState state)
	{
		spotifyQueueState = state;
		queueState = state;
		SpotifyPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(() -> currentPanel.setQueueState(state));
		}
	}

	private void handleSourceConnectionChanged(boolean connected)
	{
		if (!connected)
		{
			spotifyPlaybackState = SpotifyPlaybackState.idle();
			spotifyQueueState = SpotifyQueueState.empty();
			acceptSelectedPlaybackState(spotifyPlaybackState);
		}
		SpotifyPanel currentPanel = panel;
		if (currentPanel != null)
		{
			SwingUtilities.invokeLater(() -> currentPanel.setConnected(connected));
		}
	}

	@Provides
	SpotifyControllerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpotifyControllerConfig.class);
	}

	private boolean hasClientId()
	{
		return config.spotifyClientId() != null && !config.spotifyClientId().trim().isEmpty();
	}

	private boolean isSpotifyConfigured()
	{
		return config.enableSpotifyAccess() && hasClientId();
	}

	private void startPlaybackSources()
	{
		if (spotifyConfigured)
		{
			spotify.start(spotifySourceListener);
		}
	}

	private void updateSpotifyConfiguration(boolean configured)
	{
		spotifyConfigured = configured;
		if (configured)
		{
			spotifyPlaybackState = SpotifyPlaybackState.idle();
			spotifyQueueState = SpotifyQueueState.empty();
			spotify.start(spotifySourceListener);
		}
		else
		{
			spotify.stop();
			spotifyPlaybackState = SpotifyPlaybackState.idle();
			spotifyQueueState = SpotifyQueueState.empty();
			SpotifyPanel currentPanel = panel;
			if (currentPanel != null)
			{
				SwingUtilities.invokeLater(() -> currentPanel.setConnected(false));
			}
		}
		acceptSelectedPlaybackState(spotifyPlaybackState);
	}

	private final class SourceListener implements SpotifyListener
	{
		@Override
		public void onPlaybackState(SpotifyPlaybackState state)
		{
			handleSourcePlaybackState(state);
		}

		@Override
		public void onQueueState(SpotifyQueueState state)
		{
			handleSourceQueueState(state);
		}

		@Override
		public void onQueueTransitionChanged(boolean active)
		{
			handleQueueTransitionChanged(active);
		}

		@Override
		public void onStatus(String message)
		{
			handleSourceStatus(message);
		}

		@Override
		public void onConnectionChanged(boolean connected)
		{
			handleSourceConnectionChanged(connected);
		}
	}

	private static BufferedImage createMusicIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(29, 185, 84));
		graphics.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(6, 3, 6, 11);
		graphics.drawLine(12, 2, 12, 9);
		graphics.drawLine(6, 3, 12, 2);
		graphics.drawLine(6, 6, 12, 5);
		graphics.fillOval(2, 9, 6, 5);
		graphics.fillOval(8, 7, 6, 5);
		graphics.dispose();
		return image;
	}
}
