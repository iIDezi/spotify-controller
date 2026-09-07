package com.spotifycontroller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.LinkBrowser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SpotifyApiClient
{
	private static final Logger log = LoggerFactory.getLogger(SpotifyApiClient.class);
	private static final String ACCOUNTS_BASE = "https://accounts.spotify.com";
	private static final String API_BASE = "https://api.spotify.com/v1";
	private static final String REDIRECT_URI = "http://127.0.0.1:43821/callback";
	private static final int CALLBACK_PORT = 43821;
	private static final String SCOPES = "user-read-playback-state user-modify-playback-state";
	private static final String REFRESH_TOKEN_KEY = "spotifyRefreshToken";
	private static final String AUTHORIZED_CLIENT_ID_KEY = "spotifyAuthorizedClientId";
	private static final int MAXIMUM_ARTWORK_BYTES = 5 * 1024 * 1024;
	private static final int QUEUE_MUTE_SETTLE_DELAY_MS = 100;
	private static final int QUEUE_INITIAL_PAUSE_SETTLE_DELAY_MS = 75;
	private static final int QUEUE_FINAL_SKIP_SETTLE_DELAY_MS = 100;
	private static final int QUEUE_FINAL_PAUSE_SETTLE_DELAY_MS = 100;
	private static final int QUEUE_RESUME_DELAY_MS = 50;
	private static final int MINIMUM_BACKGROUND_REFRESH_SECONDS = 10;
	private static final long QUEUE_REFRESH_INTERVAL_MS = 30_000L;
	private static final long FALLBACK_PREFERENCE_MS = 60_000L;
	private static final MediaType JSON = MediaType.parse("application/json");

	private final SpotifyControllerConfig config;
	private final ConfigManager configManager;
	private final OkHttpClient httpClient;
	private final Gson gson;
	private final SecureRandom secureRandom = new SecureRandom();
	private final Object tokenLock = new Object();
	private final Object artworkLock = new Object();
	private final List<Consumer<String>> tokenWaiters = new ArrayList<>();
	private final Map<String, BufferedImage> artworkCache = new LinkedHashMap<String, BufferedImage>(16, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest)
		{
			return size() > 12;
		}
	};
	private final Map<String, List<Consumer<BufferedImage>>> artworkWaiters = new HashMap<>();
	private final AtomicBoolean authorizationPending = new AtomicBoolean();
	private final AtomicBoolean playbackRequestInFlight = new AtomicBoolean();
	private final AtomicBoolean queueRequestInFlight = new AtomicBoolean();
	private final AtomicBoolean queueAdvanceInFlight = new AtomicBoolean();
	private final AtomicInteger queueAdvanceRequestsRemaining = new AtomicInteger();
	private final AtomicReference<QueueAdvanceFailure> queueAdvanceFailure = new AtomicReference<>();
	private final AtomicLong credentialGeneration = new AtomicLong();

	private volatile SpotifyListener listener;
	private volatile boolean running;
	private volatile String accessToken;
	private volatile String refreshToken;
	private volatile long accessTokenExpiresAtMs;
	private volatile long nextApiRequestAtMs;
	private volatile long lastPlaybackRequestAtMs;
	private volatile long lastQueueRequestAtMs;
	private volatile long fallbackPreferredUntilMs;
	private volatile String lastQueuePlaybackUri = "";
	private volatile boolean developmentQuotaBlocked;
	private volatile int queueRestoreVolumePercent = -1;
	private volatile boolean queueMuted;
	private volatile boolean queuePaused;
	private volatile boolean queueResumeAfterSelection;
	private boolean tokenRefreshInFlight;
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> pollFuture;
	private volatile ServerSocket callbackServer;

	@Inject
	SpotifyApiClient(
		SpotifyControllerConfig config,
		ConfigManager configManager,
		OkHttpClient httpClient,
		Gson gson)
	{
		this.config = config;
		this.configManager = configManager;
		this.httpClient = httpClient;
		this.gson = gson;
	}

	void configureSessionForTesting(String token, long expiresAtMs, SpotifyListener testListener)
	{
		running = true;
		accessToken = token;
		accessTokenExpiresAtMs = expiresAtMs;
		listener = testListener;
	}

	void setExecutorForTesting(ScheduledExecutorService testExecutor)
	{
		executor = testExecutor;
	}

	void resetPlaybackRequestTimeForTesting()
	{
		lastPlaybackRequestAtMs = 0L;
	}

	void start(SpotifyListener listener)
	{
		this.listener = listener;
		running = true;
		developmentQuotaBlocked = false;
		nextApiRequestAtMs = 0;
		lastPlaybackRequestAtMs = 0;
		lastQueueRequestAtMs = 0;
		fallbackPreferredUntilMs = 0;
		lastQueuePlaybackUri = "";
		refreshToken = configManager.getConfiguration(SpotifyControllerConfig.GROUP, REFRESH_TOKEN_KEY);

		String authorizedClientId = configManager.getConfiguration(SpotifyControllerConfig.GROUP, AUTHORIZED_CLIENT_ID_KEY);
		if (refreshToken != null && authorizedClientId != null && !authorizedClientId.equals(clientId()))
		{
			clearTokens();
			notifyStatus("Client ID changed. Connect Spotify again.");
		}

		executor = Executors.newSingleThreadScheduledExecutor(runnable ->
		{
			Thread thread = new Thread(runnable, "spotify-controller");
			thread.setDaemon(true);
			return thread;
		});
		pollFuture = executor.scheduleWithFixedDelay(this::scheduledPoll, 0, 1, TimeUnit.SECONDS);

		if (!config.enableSpotifyAccess())
		{
			notifyStatus("Enable Spotify access in the plugin settings.");
		}
		else if (clientId().isEmpty())
		{
			notifyStatus("Add your Spotify Client ID in the plugin settings.");
		}
		else if (refreshToken == null || refreshToken.isEmpty())
		{
			notifyStatus("Connect your Spotify account.");
		}
		else
		{
			notifyStatus("Reconnecting to Spotify…");
		}
	}

	void stop()
	{
		running = false;
		SpotifyListener currentListener = listener;
		listener = null;
		if (pollFuture != null)
		{
			pollFuture.cancel(true);
			pollFuture = null;
		}
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
		closeCallbackServer();
		authorizationPending.set(false);
		playbackRequestInFlight.set(false);
		queueRequestInFlight.set(false);
		queueAdvanceInFlight.set(false);
		synchronized (artworkLock)
		{
			artworkWaiters.clear();
			artworkCache.clear();
		}
		synchronized (tokenLock)
		{
			tokenWaiters.clear();
			tokenRefreshInFlight = false;
		}
		if (currentListener != null)
		{
			currentListener.onConnectionChanged(false);
		}
	}

	void configurationChanged(String key)
	{
		if ("spotifyClientId".equals(key))
		{
			String authorizedClientId = configManager.getConfiguration(SpotifyControllerConfig.GROUP, AUTHORIZED_CLIENT_ID_KEY);
			if (authorizedClientId != null && !authorizedClientId.equals(clientId()))
			{
				disconnect();
				notifyStatus("Client ID changed. Connect Spotify again.");
			}
		}

		if ("enableSpotifyAccess".equals(key))
		{
			if (!config.enableSpotifyAccess())
			{
				accessToken = null;
				accessTokenExpiresAtMs = 0;
				closeCallbackServer();
				notifyConnection(false);
				notifyStatus("Spotify access is disabled.");
			}
			else
			{
				lastPlaybackRequestAtMs = 0;
				notifyStatus(refreshToken == null ? "Connect your Spotify account." : "Reconnecting to Spotify…");
			}
		}
	}

	void beginAuthorization()
	{
		if (!config.enableSpotifyAccess())
		{
			notifyStatus("Enable Spotify access in the plugin settings first.");
			return;
		}
		if (clientId().isEmpty())
		{
			notifyStatus("Add your Spotify Client ID in the plugin settings first.");
			return;
		}
		if (!authorizationPending.compareAndSet(false, true))
		{
			notifyStatus("A Spotify sign-in is already waiting in your browser.");
			return;
		}

		ScheduledExecutorService currentExecutor = executor;
		if (currentExecutor == null || currentExecutor.isShutdown())
		{
			authorizationPending.set(false);
			return;
		}
		currentExecutor.execute(this::runAuthorizationFlow);
	}

	void disconnect()
	{
		queueAdvanceInFlight.set(false);
		developmentQuotaBlocked = false;
		nextApiRequestAtMs = 0;
		lastQueueRequestAtMs = 0;
		fallbackPreferredUntilMs = 0;
		lastQueuePlaybackUri = "";
		clearTokens();
		closeCallbackServer();
		authorizationPending.set(false);
		notifyConnection(false);
		notifyPlayback(SpotifyPlaybackState.idle());
		notifyQueue(SpotifyQueueState.empty());
		notifyStatus("Disconnected from Spotify.");
	}

	void refreshNow()
	{
		long intervalMs = Math.max(MINIMUM_BACKGROUND_REFRESH_SECONDS, config.refreshSeconds()) * 1000L;
		if (System.currentTimeMillis() - lastPlaybackRequestAtMs >= intervalMs)
		{
			requestPlayback();
		}
	}

	void retryNow()
	{
		developmentQuotaBlocked = false;
		nextApiRequestAtMs = 0;
		lastPlaybackRequestAtMs = 0;
		requestPlayback();
	}

	void previous()
	{
		control("POST", "/me/player/previous", "Previous track");
	}

	void next()
	{
		control("POST", "/me/player/next", "Next track");
	}

	void setPlaying(boolean playing)
	{
		control("PUT", playing ? "/me/player/play" : "/me/player/pause", playing ? "Resume" : "Pause");
	}

	void seekTo(int positionMs)
	{
		control("PUT", "/me/player/seek?position_ms=" + Math.max(0, positionMs), "Seek");
	}

	void setVolume(int volumePercent)
	{
		int clampedVolume = Math.max(0, Math.min(100, volumePercent));
		control("PUT", "/me/player/volume?volume_percent=" + clampedVolume, "Volume");
	}

	void setShuffle(boolean enabled)
	{
		control("PUT", "/me/player/shuffle?state=" + enabled,
			enabled ? "Enable shuffle" : "Disable shuffle");
	}

	void setRepeatMode(String repeatMode)
	{
		String safeMode = "track".equals(repeatMode) || "context".equals(repeatMode)
			? repeatMode
			: "off";
		control("PUT", "/me/player/repeat?state=" + safeMode, "Change repeat mode");
	}

	void searchTracks(
		String query,
		Consumer<List<SpotifySearchResult>> onSuccess,
		Consumer<String> onFailure)
	{
		String safeQuery = query == null ? "" : query.trim();
		String unavailable = userRequestUnavailableMessage();
		if (safeQuery.isEmpty())
		{
			onFailure.accept("Enter a song or artist first.");
			return;
		}
		if (unavailable != null)
		{
			onFailure.accept(unavailable);
			return;
		}

		notifyStatus("Searching Spotify…");
		executeApi("GET", searchPath(safeQuery), true, (code, body, headers) ->
		{
			if (code >= 200 && code < 300)
			{
				try
				{
					onSuccess.accept(SpotifySearchParser.parse(body));
					notifyStatus("Spotify search complete.");
				}
				catch (RuntimeException ex)
				{
					log.debug("Unable to parse Spotify search response", ex);
					onFailure.accept("Spotify returned unreadable search results.");
					notifyStatus("Spotify returned unreadable search results.");
				}
				return;
			}
			handleApiError(code, body, headers);
			onFailure.accept(userRequestErrorMessage(code, body, headers));
		});
	}

	void playSearchResult(String uri, Runnable onSuccess, Consumer<String> onFailure)
	{
		if (uri == null || !uri.startsWith("spotify:track:"))
		{
			onFailure.accept("This search result cannot be played.");
			return;
		}
		String unavailable = userRequestUnavailableMessage();
		if (unavailable != null)
		{
			onFailure.accept(unavailable);
			return;
		}

		Map<String, Object> requestBody = new HashMap<>();
		List<String> uris = new ArrayList<>();
		uris.add(uri);
		requestBody.put("uris", uris);
		notifyStatus("Starting selected song…");
		executeApi("PUT", "/me/player/play", gson.toJson(requestBody), true, (code, body, headers) ->
		{
			if (code >= 200 && code < 300)
			{
				finishSuccessfulControl("Play selected song");
				onSuccess.run();
				return;
			}
			handleApiError(code, body, headers);
			onFailure.accept(userRequestErrorMessage(code, body, headers));
		});
	}

	static String searchPath(String query)
	{
		HttpUrl url = HttpUrl.parse(API_BASE + "/search").newBuilder()
			.addQueryParameter("q", query == null ? "" : query.trim())
			.addQueryParameter("type", "track")
			.addQueryParameter("limit", "10")
			.build();
		return url.toString().substring(API_BASE.length());
	}

	private String userRequestUnavailableMessage()
	{
		if (!running)
		{
			return "Spotify is not enabled. Check the plugin settings.";
		}
		if (!config.enableSpotifyAccess())
		{
			return "Spotify access is disabled in the plugin settings.";
		}
		if (clientId().isEmpty())
		{
			return "Add your Spotify Client ID in the plugin settings.";
		}
		if (developmentQuotaBlocked)
		{
			return "Spotify API quota is paused. Use Refresh once after the cooldown.";
		}
		long waitMs = nextApiRequestAtMs - System.currentTimeMillis();
		if (waitMs > 0)
		{
			long waitSeconds = Math.max(1, (waitMs + 999L) / 1000L);
			return "Spotify rate limit active. Try again in " + waitSeconds + " seconds.";
		}
		return null;
	}

	private String userRequestErrorMessage(int code, String body, Headers headers)
	{
		if (code == 0)
		{
			return "Connect your Spotify account first.";
		}
		if (code == -1)
		{
			return "Could not reach Spotify. Check your connection and try again.";
		}
		if (code == 403)
		{
			return "Spotify denied the request. Premium and app access are required.";
		}
		if (code == 404)
		{
			return "No active Spotify device. Open Spotify and start playback first.";
		}
		if (code == 429)
		{
			if (isDevelopmentQuotaExceeded(body))
			{
				return "Spotify Development Mode quota is exhausted. Try again later, then use Refresh once.";
			}
			int retrySeconds = parsePositiveInt(headers.get("Retry-After"), 60);
			return "Spotify rate limit reached. Try again in " + retrySeconds + " seconds.";
		}
		return "Spotify error " + code + ": " + errorMessage(body);
	}

	void playFromQueue(
		List<SpotifyQueueItem> items,
		int selectedIndex,
		boolean resumeAfterSelection,
		int volumePercent)
	{
		int advanceCount = queueAdvanceCount(items, selectedIndex);
		if (advanceCount == 0)
		{
			return;
		}
		if (!config.enableSpotifyAccess())
		{
			notifyStatus("Spotify access is disabled.");
			return;
		}
		if (!queueAdvanceInFlight.compareAndSet(false, true))
		{
			notifyStatus("A queued-song selection is already in progress.");
			return;
		}

		queueResumeAfterSelection = resumeAfterSelection;
		queueRestoreVolumePercent = volumePercent < 0
			? -1
			: Math.max(0, Math.min(100, volumePercent));
		queueMuted = false;
		queuePaused = false;
		queueAdvanceRequestsRemaining.set(0);
		queueAdvanceFailure.set(null);
		notifyQueueTransition(true);
		notifyStatus("Opening queued song…");
		if (queueRestoreVolumePercent > 0)
		{
			muteBeforeQueueAdvance(advanceCount);
		}
		else
		{
			pauseBeforeQueueAdvance(advanceCount);
		}
	}

	static int queueAdvanceCount(List<SpotifyQueueItem> items, int selectedIndex)
	{
		if (items == null || selectedIndex < 0 || selectedIndex >= items.size())
		{
			return 0;
		}
		return selectedIndex + 1;
	}

	private void muteBeforeQueueAdvance(int advanceCount)
	{
		executeApi("PUT", "/me/player/volume?volume_percent=0", true, (code, body, headers) ->
		{
			if (code >= 200 && code < 300)
			{
				queueMuted = true;
				scheduleQueueAdvance(
					() -> pauseBeforeQueueAdvance(advanceCount),
					QUEUE_MUTE_SETTLE_DELAY_MS);
				return;
			}
			if (code == 403)
			{
				log.debug("Spotify device does not allow temporary queue-transition muting");
				queueRestoreVolumePercent = -1;
				pauseBeforeQueueAdvance(advanceCount);
				return;
			}
			failQueueAdvance(code, body, headers);
		});
	}

	private void pauseBeforeQueueAdvance(int advanceCount)
	{
		executeApi("PUT", "/me/player/pause", true, (code, body, headers) ->
		{
			if (code < 200 || code >= 300)
			{
				failQueueAdvance(code, body, headers);
				return;
			}
			queuePaused = true;
			scheduleQueueAdvance(
				() -> advanceExistingQueue(advanceCount),
				QUEUE_INITIAL_PAUSE_SETTLE_DELAY_MS);
		});
	}

	private void advanceExistingQueue(int advanceCount)
	{
		if (!running)
		{
			queueAdvanceInFlight.set(false);
			return;
		}

		queueAdvanceRequestsRemaining.set(advanceCount);
		for (int requestIndex = 0; requestIndex < advanceCount; requestIndex++)
		{
			executeApi("POST", "/me/player/next", true, (code, body, headers) ->
			{
				if (code < 200 || code >= 300)
				{
					queueAdvanceFailure.compareAndSet(null, new QueueAdvanceFailure(code, body, headers));
				}
				if (queueAdvanceRequestsRemaining.decrementAndGet() != 0)
				{
					return;
				}

				QueueAdvanceFailure failure = queueAdvanceFailure.get();
				if (failure != null)
				{
					failQueueAdvance(failure.code, failure.body, failure.headers);
					return;
				}
				scheduleQueueAdvance(this::pauseSelectedQueueItem, QUEUE_FINAL_SKIP_SETTLE_DELAY_MS);
			});
		}
	}

	private void pauseSelectedQueueItem()
	{
		executeApi("PUT", "/me/player/pause", true, (code, body, headers) ->
		{
			if (code >= 200 && code < 300)
			{
				queuePaused = true;
				scheduleQueueAdvance(this::restoreQueueVolume, QUEUE_FINAL_PAUSE_SETTLE_DELAY_MS);
			}
			else
			{
				failQueueAdvance(code, body, headers);
			}
		});
	}

	private void restoreQueueVolume()
	{
		if (!queueMuted || queueRestoreVolumePercent < 0)
		{
			finishOrResumeQueueAdvance();
			return;
		}

		int volumePercent = queueRestoreVolumePercent;
		executeApi("PUT", "/me/player/volume?volume_percent=" + volumePercent, true,
			(code, body, headers) ->
			{
				if (code >= 200 && code < 300)
				{
					queueMuted = false;
					finishOrResumeQueueAdvance();
				}
				else
				{
					failQueueAdvance(code, body, headers);
				}
			});
	}

	private void finishOrResumeQueueAdvance()
	{
		if (queueResumeAfterSelection)
		{
			scheduleQueueAdvance(this::resumeAfterQueueAdvance, QUEUE_RESUME_DELAY_MS);
		}
		else
		{
			finishQueueAdvance();
		}
	}

	private void resumeAfterQueueAdvance()
	{
		executeApi("PUT", "/me/player/play", true, (code, body, headers) ->
		{
			if (code >= 200 && code < 300)
			{
				queuePaused = false;
				finishQueueAdvance();
			}
			else
			{
				failQueueAdvance(code, body, headers);
			}
		});
	}

	private void scheduleQueueAdvance(Runnable action, int delayMs)
	{
		ScheduledExecutorService currentExecutor = executor;
		if (currentExecutor == null || currentExecutor.isShutdown())
		{
			failQueueAdvance(-1, "", Headers.of());
			return;
		}
		currentExecutor.schedule(action, delayMs, TimeUnit.MILLISECONDS);
	}

	private void finishQueueAdvance()
	{
		clearQueueAdvanceState();
		queueAdvanceInFlight.set(false);
		finishSuccessfulControl("Open queued song");
		notifyQueueTransition(false);
	}

	private void failQueueAdvance(int code, String body, Headers headers)
	{
		int volumePercent = queueRestoreVolumePercent;
		boolean restoreVolume = queueMuted && volumePercent >= 0;
		boolean resumePlayback = queueResumeAfterSelection && queuePaused;
		queueMuted = false;
		queuePaused = false;

		Runnable finishFailure = () ->
		{
			clearQueueAdvanceState();
			queueAdvanceInFlight.set(false);
			notifyQueueTransition(false);
			handleApiError(code, body, headers);
		};
		Runnable resumeThenFinish = () ->
		{
			if (!resumePlayback)
			{
				finishFailure.run();
				return;
			}
			executeApi("PUT", "/me/player/play", true,
				(resumeCode, resumeBody, resumeHeaders) -> finishFailure.run());
		};

		if (!restoreVolume)
		{
			resumeThenFinish.run();
			return;
		}
		executeApi("PUT", "/me/player/volume?volume_percent=" + volumePercent, true,
			(restoreCode, restoreBody, restoreHeaders) -> resumeThenFinish.run());
	}

	private void clearQueueAdvanceState()
	{
		queueRestoreVolumePercent = -1;
		queueMuted = false;
		queuePaused = false;
		queueResumeAfterSelection = false;
		queueAdvanceRequestsRemaining.set(0);
		queueAdvanceFailure.set(null);
	}

	void replaceQueue(SpotifyPlaybackState current, List<SpotifyQueueItem> items)
	{
		List<String> uris = new ArrayList<>();
		boolean includeCurrent = current.isItemAvailable() && !current.getUri().isEmpty();
		if (includeCurrent)
		{
			uris.add(current.getUri());
		}
		for (SpotifyQueueItem item : items)
		{
			if (!item.getUri().isEmpty())
			{
				uris.add(item.getUri());
			}
		}
		Integer positionMs = includeCurrent
			? current.getEstimatedProgressMs(System.currentTimeMillis())
			: null;
		startUriList(uris, positionMs, "Queue reorder", current.isShuffleEnabled());
	}

	void loadArtwork(String url, Consumer<BufferedImage> callback)
	{
		HttpUrl artworkUrl = HttpUrl.parse(url);
		if (artworkUrl == null || !"https".equalsIgnoreCase(artworkUrl.scheme()))
		{
			callback.accept(null);
			return;
		}

		synchronized (artworkLock)
		{
			BufferedImage cached = artworkCache.get(url);
			if (cached != null)
			{
				callback.accept(cached);
				return;
			}
			List<Consumer<BufferedImage>> waiters = artworkWaiters.get(url);
			if (waiters != null)
			{
				waiters.add(callback);
				return;
			}
			waiters = new ArrayList<>();
			waiters.add(callback);
			artworkWaiters.put(url, waiters);
		}

		Request request = new Request.Builder().url(artworkUrl).get().build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException ex)
			{
				log.debug("Spotify artwork request failed", ex);
				finishArtwork(url, null);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				BufferedImage image = null;
				try (Response closeable = response)
				{
					long contentLength = closeable.body() == null ? 0 : closeable.body().contentLength();
					if (closeable.isSuccessful() && closeable.body() != null &&
						(contentLength < 0 || contentLength <= MAXIMUM_ARTWORK_BYTES))
					{
						image = ImageIO.read(closeable.body().byteStream());
					}
				}
				catch (IOException | RuntimeException ex)
				{
					log.debug("Unable to read Spotify artwork", ex);
				}
				finally
				{
					finishArtwork(url, image);
				}
			}
		});
	}

	private void finishArtwork(String url, BufferedImage image)
	{
		List<Consumer<BufferedImage>> waiters;
		synchronized (artworkLock)
		{
			waiters = artworkWaiters.remove(url);
			if (image != null)
			{
				artworkCache.put(url, image);
			}
		}
		if (waiters != null)
		{
			for (Consumer<BufferedImage> waiter : waiters)
			{
				waiter.accept(image);
			}
		}
	}

	private void scheduledPoll()
	{
		if (!running || !config.enableSpotifyAccess() || clientId().isEmpty())
		{
			return;
		}

		long intervalMs = Math.max(MINIMUM_BACKGROUND_REFRESH_SECONDS, config.refreshSeconds()) * 1000L;
		if (System.currentTimeMillis() - lastPlaybackRequestAtMs >= intervalMs)
		{
			requestPlayback();
		}
	}

	private void requestPlayback()
	{
		if (!running || queueAdvanceInFlight.get() || !config.enableSpotifyAccess() ||
			developmentQuotaBlocked ||
			System.currentTimeMillis() < nextApiRequestAtMs ||
			!playbackRequestInFlight.compareAndSet(false, true))
		{
			return;
		}
		lastPlaybackRequestAtMs = System.currentTimeMillis();
		if (System.currentTimeMillis() < fallbackPreferredUntilMs)
		{
			requestCurrentlyPlayingFallback();
			return;
		}

		executeApi("GET", "/me/player?additional_types=track%2Cepisode", true, (code, body, headers) ->
		{
			if (!running || queueAdvanceInFlight.get())
			{
				playbackRequestInFlight.set(false);
				return;
			}
			if (code == 204)
			{
				requestCurrentlyPlayingFallback();
				return;
			}
			playbackRequestInFlight.set(false);
			if (code >= 200 && code < 300)
			{
				try
				{
					SpotifyPlaybackState state = SpotifyPlaybackParser.parse(body);
					fallbackPreferredUntilMs = 0;
					notifyConnection(true);
					notifyPlayback(state);
					requestQueueIfNeeded(state);
					if (!state.isItemAvailable())
					{
						notifyStatus("Connected — no active playback.");
					}
					else
					{
						notifyStatus(state.isPlaying() ? "Playing." : "Paused.");
					}
				}
				catch (RuntimeException ex)
				{
					log.debug("Unable to parse Spotify playback response", ex);
					notifyStatus("Spotify returned an unreadable playback response.");
				}
			}
			else
			{
				handleApiError(code, body, headers);
			}
		});
	}

	private void requestCurrentlyPlayingFallback()
	{
		executeApi("GET", "/me/player/currently-playing?additional_types=track%2Cepisode", true,
			(code, body, headers) ->
			{
				playbackRequestInFlight.set(false);
				if (!running || queueAdvanceInFlight.get())
				{
					return;
				}
				if (code == 204)
				{
					fallbackPreferredUntilMs = 0;
					lastQueuePlaybackUri = "";
					lastQueueRequestAtMs = 0;
					notifyConnection(true);
					notifyPlayback(SpotifyPlaybackState.idle());
					notifyQueue(SpotifyQueueState.empty());
					notifyStatus("Connected — no active Spotify playback.");
				}
				else if (code >= 200 && code < 300)
				{
					try
					{
						SpotifyPlaybackState state = SpotifyPlaybackParser.parse(body);
						fallbackPreferredUntilMs = System.currentTimeMillis() + FALLBACK_PREFERENCE_MS;
						notifyConnection(true);
						notifyPlayback(state);
						requestQueueIfNeeded(state);
						notifyStatus(state.isPlaying()
							? "Playing through Spotify API fallback."
							: "Paused through Spotify API fallback.");
					}
					catch (RuntimeException ex)
					{
						log.debug("Unable to parse Spotify currently-playing response", ex);
						notifyStatus("Spotify returned an unreadable currently-playing response.");
					}
				}
				else
				{
					fallbackPreferredUntilMs = 0;
					handleApiError(code, body, headers);
				}
			});
	}

	private void requestQueueIfNeeded(SpotifyPlaybackState state)
	{
		if (state == null || !state.isItemAvailable())
		{
			lastQueuePlaybackUri = "";
			return;
		}

		String playbackUri = state.getUri();
		long nowMs = System.currentTimeMillis();
		boolean trackChanged = !playbackUri.equals(lastQueuePlaybackUri);
		lastQueuePlaybackUri = playbackUri;
		if (trackChanged || nowMs - lastQueueRequestAtMs >= QUEUE_REFRESH_INTERVAL_MS)
		{
			requestQueue();
		}
	}

	private void requestQueue()
	{
		if (!running || queueAdvanceInFlight.get() || !config.enableSpotifyAccess() ||
			developmentQuotaBlocked ||
			System.currentTimeMillis() < nextApiRequestAtMs ||
			!queueRequestInFlight.compareAndSet(false, true))
		{
			return;
		}
		lastQueueRequestAtMs = System.currentTimeMillis();

		executeApi("GET", "/me/player/queue", true, (code, body, headers) ->
		{
			queueRequestInFlight.set(false);
			if (!running || queueAdvanceInFlight.get())
			{
				return;
			}
			if (code == 204)
			{
				notifyQueue(SpotifyQueueState.empty());
			}
			else if (code >= 200 && code < 300)
			{
				try
				{
					notifyQueue(SpotifyQueueParser.parse(body));
				}
				catch (RuntimeException ex)
				{
					log.debug("Unable to parse Spotify queue response", ex);
				}
			}
			else
			{
				log.debug("Spotify queue request returned HTTP {}", code);
				handleApiError(code, body, headers);
			}
		});
	}

	private void startUriList(List<String> uris, Integer positionMs, String action, boolean preserveShuffle)
	{
		if (uris.isEmpty())
		{
			return;
		}
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("uris", uris);
		if (positionMs != null)
		{
			requestBody.put("position_ms", Math.max(0, positionMs));
		}
		String requestJson = gson.toJson(requestBody);
		if (preserveShuffle)
		{
			playUriListWithShufflePreserved(requestJson, action);
		}
		else
		{
			control("PUT", "/me/player/play", requestJson, action);
		}
	}

	private void playUriListWithShufflePreserved(String requestBody, String action)
	{
		if (!config.enableSpotifyAccess())
		{
			notifyStatus("Spotify access is disabled.");
			return;
		}
		notifyStatus(action + " requested…");
		executeApi("PUT", "/me/player/shuffle?state=false", true, (disableCode, disableBody, disableHeaders) ->
		{
			if (disableCode < 200 || disableCode >= 300)
			{
				handleApiError(disableCode, disableBody, disableHeaders);
				return;
			}
			executeApi("PUT", "/me/player/play", requestBody, true, (playCode, playBody, playHeaders) ->
				executeApi("PUT", "/me/player/shuffle?state=true", true,
					(restoreCode, restoreBody, restoreHeaders) ->
					{
						if (playCode < 200 || playCode >= 300)
						{
							handleApiError(playCode, playBody, playHeaders);
							return;
						}
						if (restoreCode < 200 || restoreCode >= 300)
						{
							handleApiError(restoreCode, restoreBody, restoreHeaders);
							return;
						}
						finishSuccessfulControl(action);
					}));
		});
	}

	private void control(String method, String path, String action)
	{
		control(method, path, "", action);
	}

	private void control(String method, String path, String requestBody, String action)
	{
		if (!config.enableSpotifyAccess())
		{
			notifyStatus("Spotify access is disabled.");
			return;
		}
		notifyStatus(action + " requested…");
		executeApi(method, path, requestBody, true, (code, body, headers) ->
		{
			if (code >= 200 && code < 300)
			{
				finishSuccessfulControl(action);
			}
			else
			{
				handleApiError(code, body, headers);
			}
		});
	}

	private void finishSuccessfulControl(String action)
	{
		nextApiRequestAtMs = 0;
		lastPlaybackRequestAtMs = 0;
		lastQueueRequestAtMs = 0;
		notifyStatus(action + " sent.");
		ScheduledExecutorService currentExecutor = executor;
		if (currentExecutor != null && !currentExecutor.isShutdown())
		{
			currentExecutor.execute(this::requestPlayback);
			currentExecutor.schedule(this::requestPlayback, 300, TimeUnit.MILLISECONDS);
		}
	}

	private void executeApi(String method, String path, boolean retryUnauthorized, ApiResponseHandler handler)
	{
		executeApi(method, path, "", retryUnauthorized, handler);
	}

	private void executeApi(
		String method,
		String path,
		String requestBody,
		boolean retryUnauthorized,
		ApiResponseHandler handler)
	{
		withAccessToken(token ->
		{
			if (token == null)
			{
				handler.handle(0, "", Headers.of());
				return;
			}

			Request.Builder builder = new Request.Builder()
				.url(API_BASE + path)
				.header("Authorization", "Bearer " + token)
				.header("Accept", "application/json");
			if ("GET".equals(method))
			{
				builder.get();
			}
			else
			{
				builder.method(method, RequestBody.create(JSON, requestBody));
			}

			httpClient.newCall(builder.build()).enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException ex)
				{
					log.debug("Spotify API request failed: {} {}", method, path, ex);
					handler.handle(-1, "", Headers.of());
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException
				{
					try (Response closeable = response)
					{
						String responseBody = closeable.body() == null ? "" : closeable.body().string();
						if (closeable.code() == 401 && retryUnauthorized)
						{
							invalidateAccessToken();
							executeApi(method, path, requestBody, false, handler);
							return;
						}
						handler.handle(closeable.code(), responseBody, closeable.headers());
					}
				}
			});
		});
	}

	private void withAccessToken(Consumer<String> action)
	{
		String usableToken = null;
		boolean startRefresh = false;
		boolean unavailable = false;
		synchronized (tokenLock)
		{
			if (accessToken != null && System.currentTimeMillis() < accessTokenExpiresAtMs)
			{
				usableToken = accessToken;
			}
			else if (refreshToken == null || refreshToken.isEmpty())
			{
				unavailable = true;
			}
			else
			{
				tokenWaiters.add(action);
				if (!tokenRefreshInFlight)
				{
					tokenRefreshInFlight = true;
					startRefresh = true;
				}
			}
		}

		if (usableToken != null)
		{
			action.accept(usableToken);
		}
		else if (unavailable)
		{
			notifyConnection(false);
			notifyStatus("Connect your Spotify account.");
			action.accept(null);
		}
		else if (startRefresh)
		{
			refreshAccessToken();
		}
	}

	private void refreshAccessToken()
	{
		long expectedGeneration = credentialGeneration.get();
		String currentRefreshToken = refreshToken;
		if (currentRefreshToken == null || currentRefreshToken.isEmpty())
		{
			finishTokenRefresh(null);
			return;
		}

		FormBody body = new FormBody.Builder()
			.add("client_id", clientId())
			.add("grant_type", "refresh_token")
			.add("refresh_token", currentRefreshToken)
			.build();
		Request request = new Request.Builder()
			.url(ACCOUNTS_BASE + "/api/token")
			.post(body)
			.header("Accept", "application/json")
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException ex)
			{
				if (credentialGeneration.get() != expectedGeneration)
				{
					finishTokenRefresh(null);
					return;
				}
				log.debug("Spotify token refresh failed", ex);
				notifyStatus("Could not reach Spotify. Will retry automatically.");
				finishTokenRefresh(null);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response closeable = response)
				{
					if (credentialGeneration.get() != expectedGeneration || !config.enableSpotifyAccess())
					{
						finishTokenRefresh(null);
						return;
					}
					String responseBody = closeable.body() == null ? "" : closeable.body().string();
					if (!closeable.isSuccessful())
					{
						log.debug("Spotify token refresh returned HTTP {}", closeable.code());
						if (closeable.code() == 400 || closeable.code() == 401)
						{
							clearTokens();
							notifyConnection(false);
							notifyStatus("Spotify authorization expired. Connect again.");
						}
						else
						{
							notifyStatus("Spotify authorization refresh failed: " + errorMessage(responseBody));
						}
						finishTokenRefresh(null);
						return;
					}

					try
					{
						installTokens(responseBody);
						notifyConnection(true);
						finishTokenRefresh(accessToken);
					}
					catch (RuntimeException ex)
					{
						log.debug("Unable to parse Spotify token response", ex);
						notifyStatus("Spotify returned an unreadable authorization response.");
						finishTokenRefresh(null);
					}
				}
			}
		});
	}

	private void finishTokenRefresh(String token)
	{
		List<Consumer<String>> waiters;
		synchronized (tokenLock)
		{
			tokenRefreshInFlight = false;
			waiters = new ArrayList<>(tokenWaiters);
			tokenWaiters.clear();
		}
		for (Consumer<String> waiter : waiters)
		{
			waiter.accept(token);
		}
	}

	private void runAuthorizationFlow()
	{
		long expectedGeneration = credentialGeneration.get();
		String verifier = randomUrlSafe(64);
		String state = randomUrlSafe(32);
		String challenge;
		try
		{
			challenge = codeChallenge(verifier);
		}
		catch (NoSuchAlgorithmException ex)
		{
			log.debug("SHA-256 is unavailable", ex);
			notifyStatus("This Java installation does not support Spotify login.");
			authorizationPending.set(false);
			return;
		}

		try (ServerSocket server = new ServerSocket())
		{
			callbackServer = server;
			server.setReuseAddress(true);
			server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), CALLBACK_PORT), 1);
			server.setSoTimeout(180_000);

			HttpUrl authorizationUrl = HttpUrl.parse(ACCOUNTS_BASE + "/authorize").newBuilder()
				.addQueryParameter("client_id", clientId())
				.addQueryParameter("response_type", "code")
				.addQueryParameter("redirect_uri", REDIRECT_URI)
				.addQueryParameter("scope", SCOPES)
				.addQueryParameter("state", state)
				.addQueryParameter("code_challenge_method", "S256")
				.addQueryParameter("code_challenge", challenge)
				.build();

			notifyStatus("Complete Spotify sign-in in your browser.");
			LinkBrowser.browse(authorizationUrl.toString());

			try (Socket socket = server.accept())
			{
				socket.setSoTimeout(5_000);
				CallbackParameters callback = readCallback(socket);
				if (callback.error != null)
				{
					writeBrowserResponse(socket, 400, "Spotify connection was not approved. You can close this tab.");
					notifyStatus("Spotify connection was not approved.");
				}
				else if (!state.equals(callback.state) || callback.code == null || callback.code.isEmpty())
				{
					writeBrowserResponse(socket, 400, "Spotify sign-in could not be verified. You can close this tab.");
					notifyStatus("Spotify sign-in could not be verified. Try Connect again.");
				}
				else
				{
					writeBrowserResponse(socket, 200, "Spotify sign-in returned to RuneLite. You can close this tab.");
					exchangeAuthorizationCode(callback.code, verifier, expectedGeneration);
				}
			}
		}
		catch (SocketTimeoutException ex)
		{
			notifyStatus("Spotify sign-in timed out. Press Connect to try again.");
		}
		catch (IOException | RuntimeException ex)
		{
			if (running)
			{
				log.debug("Spotify authorization callback failed", ex);
				notifyStatus("Could not open callback port 43821. Close any app using it and try again.");
			}
		}
		finally
		{
			callbackServer = null;
			authorizationPending.set(false);
		}
	}

	private void exchangeAuthorizationCode(String code, String verifier, long expectedGeneration)
	{
		FormBody body = new FormBody.Builder()
			.add("client_id", clientId())
			.add("grant_type", "authorization_code")
			.add("code", code)
			.add("redirect_uri", REDIRECT_URI)
			.add("code_verifier", verifier)
			.build();
		Request request = new Request.Builder()
			.url(ACCOUNTS_BASE + "/api/token")
			.post(body)
			.header("Accept", "application/json")
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException ex)
			{
				if (credentialGeneration.get() != expectedGeneration)
				{
					return;
				}
				log.debug("Spotify authorization-code exchange failed", ex);
				notifyStatus("Could not reach Spotify to finish connecting.");
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response closeable = response)
				{
					if (credentialGeneration.get() != expectedGeneration || !config.enableSpotifyAccess())
					{
						return;
					}
					String responseBody = closeable.body() == null ? "" : closeable.body().string();
					if (!closeable.isSuccessful())
					{
						log.debug("Spotify authorization-code exchange returned HTTP {}", closeable.code());
						notifyStatus("Spotify connection failed: " + errorMessage(responseBody));
						return;
					}

					try
					{
						installTokens(responseBody);
						notifyConnection(true);
						notifyStatus("Spotify connected.");
						lastPlaybackRequestAtMs = 0;
						requestPlayback();
					}
					catch (RuntimeException ex)
					{
						log.debug("Unable to parse Spotify authorization response", ex);
						notifyStatus("Spotify returned an unreadable authorization response.");
					}
				}
			}
		});
	}

	private void installTokens(String json)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);
		String newAccessToken = requiredString(root, "access_token");
		int expiresIn = root.has("expires_in") ? root.get("expires_in").getAsInt() : 3600;
		String newRefreshToken = optionalString(root, "refresh_token");

		synchronized (tokenLock)
		{
			accessToken = newAccessToken;
			accessTokenExpiresAtMs = System.currentTimeMillis() + Math.max(1, expiresIn - 30) * 1000L;
			if (newRefreshToken != null && !newRefreshToken.isEmpty())
			{
				refreshToken = newRefreshToken;
				configManager.setConfiguration(SpotifyControllerConfig.GROUP, REFRESH_TOKEN_KEY, newRefreshToken);
			}
		}
		configManager.setConfiguration(SpotifyControllerConfig.GROUP, AUTHORIZED_CLIENT_ID_KEY, clientId());
	}

	private void handleApiError(int code, String body, Headers headers)
	{
		if (code == 0)
		{
			notifyStatus("Connect your Spotify account.");
		}
		else if (code == -1)
		{
			notifyStatus("Could not reach Spotify. Will retry automatically.");
		}
		else if (code == 403)
		{
			notifyStatus("Spotify denied playback control. Premium and app access are required.");
		}
		else if (code == 404)
		{
			notifyStatus("No active Spotify device. Start playback in Spotify first.");
		}
		else if (code == 429)
		{
			if (isDevelopmentQuotaExceeded(body))
			{
				developmentQuotaBlocked = true;
				nextApiRequestAtMs = Long.MAX_VALUE;
				notifyStatus("Spotify Development Mode quota exhausted. Automatic API requests are paused; click Refresh once later.");
			}
			else
			{
				int retrySeconds = parsePositiveInt(headers.get("Retry-After"), 60);
				nextApiRequestAtMs = System.currentTimeMillis() + retrySeconds * 1000L;
				notifyStatus("Spotify rate limit reached. Retrying in " + retrySeconds + " seconds.");
			}
		}
		else
		{
			notifyStatus("Spotify error " + code + ": " + errorMessage(body));
		}
	}

	private boolean isDevelopmentQuotaExceeded(String json)
	{
		if (json == null || json.isEmpty())
		{
			return false;
		}
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonElement error = root == null ? null : root.get("error");
			if (error == null || !error.isJsonObject())
			{
				return false;
			}
			return "QUOTA_EXCEEDED".equals(optionalString(error.getAsJsonObject(), "reason"));
		}
		catch (RuntimeException ex)
		{
			return false;
		}
	}

	private void invalidateAccessToken()
	{
		synchronized (tokenLock)
		{
			accessToken = null;
			accessTokenExpiresAtMs = 0;
		}
	}

	private void clearTokens()
	{
		credentialGeneration.incrementAndGet();
		synchronized (tokenLock)
		{
			accessToken = null;
			refreshToken = null;
			accessTokenExpiresAtMs = 0;
		}
		configManager.unsetConfiguration(SpotifyControllerConfig.GROUP, REFRESH_TOKEN_KEY);
		configManager.unsetConfiguration(SpotifyControllerConfig.GROUP, AUTHORIZED_CLIENT_ID_KEY);
	}

	private void closeCallbackServer()
	{
		ServerSocket server = callbackServer;
		if (server != null)
		{
			try
			{
				server.close();
			}
			catch (IOException ex)
			{
				log.debug("Unable to close Spotify callback server", ex);
			}
		}
	}

	private String clientId()
	{
		String value = config.spotifyClientId();
		return value == null ? "" : value.trim();
	}

	private String randomUrlSafe(int bytes)
	{
		byte[] random = new byte[bytes];
		secureRandom.nextBytes(random);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
	}

	static String codeChallenge(String verifier) throws NoSuchAlgorithmException
	{
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
	}

	private static CallbackParameters readCallback(Socket socket) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
		String requestLine = reader.readLine();
		if (requestLine == null || !requestLine.startsWith("GET "))
		{
			return new CallbackParameters(null, null, "invalid_request");
		}
		String[] parts = requestLine.split(" ", 3);
		if (parts.length < 2)
		{
			return new CallbackParameters(null, null, "invalid_request");
		}

		String line;
		while ((line = reader.readLine()) != null && !line.isEmpty())
		{
			// Consume request headers before responding.
		}

		URI uri = URI.create(parts[1]);
		if (!"/callback".equals(uri.getPath()))
		{
			return new CallbackParameters(null, null, "invalid_path");
		}
		Map<String, String> query = parseQuery(uri.getRawQuery());
		return new CallbackParameters(query.get("code"), query.get("state"), query.get("error"));
	}

	private static Map<String, String> parseQuery(String rawQuery)
	{
		Map<String, String> values = new HashMap<>();
		if (rawQuery == null || rawQuery.isEmpty())
		{
			return values;
		}
		for (String pair : rawQuery.split("&"))
		{
			String[] parts = pair.split("=", 2);
			String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
			String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
			values.put(key, value);
		}
		return values;
	}

	private static void writeBrowserResponse(Socket socket, int status, String message) throws IOException
	{
		String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>RuneLite Media Controller</title>" +
			"<style>body{background:#121212;color:#fff;font:18px sans-serif;display:grid;place-items:center;height:100vh;margin:0}" +
			"main{max-width:560px;text-align:center;padding:32px}.logo{color:#1db954;font-size:48px}</style></head>" +
			"<body><main><div class=\"logo\">●</div><h1>RuneLite Media Controller</h1><p>" + message +
			"</p></main></body></html>";
		byte[] content = html.getBytes(StandardCharsets.UTF_8);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
		writer.write("HTTP/1.1 " + status + (status == 200 ? " OK" : " Bad Request") + "\r\n");
		writer.write("Content-Type: text/html; charset=utf-8\r\n");
		writer.write("Content-Length: " + content.length + "\r\n");
		writer.write("Connection: close\r\n\r\n");
		writer.flush();
		socket.getOutputStream().write(content);
		socket.getOutputStream().flush();
	}

	private String errorMessage(String json)
	{
		if (json == null || json.isEmpty())
		{
			return "request failed";
		}
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonElement error = root.get("error");
			String message = null;
			if (error != null && error.isJsonPrimitive())
			{
				message = error.getAsString();
			}
			else if (error != null && error.isJsonObject())
			{
				message = optionalString(error.getAsJsonObject(), "message");
			}
			if (message == null)
			{
				message = optionalString(root, "error_description");
			}
			return message == null || message.isEmpty() ? "request failed" : limit(message, 160);
		}
		catch (RuntimeException ex)
		{
			return "request failed";
		}
	}

	private static String requiredString(JsonObject object, String key)
	{
		String value = optionalString(object, key);
		if (value == null || value.isEmpty())
		{
			throw new IllegalArgumentException("Missing " + key);
		}
		return value;
	}

	private static String optionalString(JsonObject object, String key)
	{
		JsonElement value = object == null ? null : object.get(key);
		return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
	}

	private static int parsePositiveInt(String value, int fallback)
	{
		try
		{
			return value == null ? fallback : Math.max(1, Integer.parseInt(value));
		}
		catch (NumberFormatException ex)
		{
			return fallback;
		}
	}

	private static String limit(String value, int maxLength)
	{
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private void notifyPlayback(SpotifyPlaybackState state)
	{
		SpotifyListener current = listener;
		if (running && current != null)
		{
			current.onPlaybackState(state);
		}
	}

	private void notifyQueue(SpotifyQueueState state)
	{
		SpotifyListener current = listener;
		if (running && current != null)
		{
			current.onQueueState(state);
		}
	}

	private void notifyQueueTransition(boolean active)
	{
		SpotifyListener current = listener;
		if (running && current != null)
		{
			current.onQueueTransitionChanged(active);
		}
	}

	private void notifyStatus(String message)
	{
		SpotifyListener current = listener;
		if (running && current != null)
		{
			current.onStatus(message);
		}
	}

	private void notifyConnection(boolean connected)
	{
		SpotifyListener current = listener;
		if (running && current != null)
		{
			current.onConnectionChanged(connected);
		}
	}

	@FunctionalInterface
	private interface ApiResponseHandler
	{
		void handle(int code, String body, Headers headers);
	}

	private static final class QueueAdvanceFailure
	{
		private final int code;
		private final String body;
		private final Headers headers;

		private QueueAdvanceFailure(int code, String body, Headers headers)
		{
			this.code = code;
			this.body = body;
			this.headers = headers;
		}
	}

	private static final class CallbackParameters
	{
		private final String code;
		private final String state;
		private final String error;

		private CallbackParameters(String code, String state, String error)
		{
			this.code = code;
			this.state = state;
			this.error = error;
		}
	}
}
