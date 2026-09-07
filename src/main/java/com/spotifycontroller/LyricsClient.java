package com.spotifycontroller;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LyricsClient
{
	private static final Logger log = LoggerFactory.getLogger(LyricsClient.class);
	private static final String API_URL = "https://lrclib.net/api/get";
	private static final long MAXIMUM_RESPONSE_BYTES = 1_048_576L;

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final Object lock = new Object();
	private final Map<String, LyricsState> cache = new LinkedHashMap<String, LyricsState>(32, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, LyricsState> eldest)
		{
			return size() > 32;
		}
	};
	private final Map<String, List<Consumer<LyricsState>>> waiters = new HashMap<>();
	private long generation;

	@Inject
	LyricsClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	void requestLyrics(SpotifyPlaybackState state, Consumer<LyricsState> callback)
	{
		String key = keyFor(state);
		if (key.isEmpty())
		{
			callback.accept(LyricsState.empty());
			return;
		}

		long requestGeneration;
		synchronized (lock)
		{
			if (cache.containsKey(key))
			{
				callback.accept(cache.get(key));
				return;
			}
			List<Consumer<LyricsState>> existingWaiters = waiters.get(key);
			if (existingWaiters != null)
			{
				existingWaiters.add(callback);
				return;
			}
			List<Consumer<LyricsState>> newWaiters = new ArrayList<>();
			newWaiters.add(callback);
			waiters.put(key, newWaiters);
			requestGeneration = generation;
		}

		HttpUrl.Builder url = HttpUrl.parse(API_URL).newBuilder()
			.addQueryParameter("track_name", state.getTitle())
			.addQueryParameter("artist_name", state.getArtist());
		if (!state.getAlbum().trim().isEmpty())
		{
			url.addQueryParameter("album_name", state.getAlbum());
		}
		if (state.getDurationMs() > 0)
		{
			url.addQueryParameter("duration", Integer.toString(Math.max(1,
				Math.round(state.getDurationMs() / 1_000f))));
		}

		Request request = new Request.Builder()
			.url(url.build())
			.header("Accept", "application/json")
			.header("User-Agent", "RuneLite-Spotify-Controller/" + SpotifyControllerPlugin.PLUGIN_VERSION)
			.get()
			.build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				log.debug("Lyrics request failed", exception);
				finish(key, LyricsState.empty(), false, requestGeneration);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response closeableResponse = response)
				{
					if (response.code() == 404)
					{
						finish(key, LyricsState.empty(), true, requestGeneration);
						return;
					}
					ResponseBody body = response.body();
					if (!response.isSuccessful() || body == null ||
						(body.contentLength() > MAXIMUM_RESPONSE_BYTES && body.contentLength() >= 0))
					{
						finish(key, LyricsState.empty(), false, requestGeneration);
						return;
					}
					String json = readBody(body);
					if (json == null)
					{
						finish(key, LyricsState.empty(), false, requestGeneration);
						return;
					}
					finish(key, LyricsParser.parse(json, gson), true, requestGeneration);
				}
				catch (IOException exception)
				{
					log.debug("Unable to read lyrics response", exception);
					finish(key, LyricsState.empty(), false, requestGeneration);
				}
			}
		});
	}

	private static String readBody(ResponseBody body) throws IOException
	{
		try (InputStream input = body.byteStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream())
		{
			byte[] buffer = new byte[8_192];
			int total = 0;
			int read;
			while ((read = input.read(buffer)) != -1)
			{
				total += read;
				if (total > MAXIMUM_RESPONSE_BYTES)
				{
					return null;
				}
				output.write(buffer, 0, read);
			}
			return new String(output.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	void reset()
	{
		synchronized (lock)
		{
			generation++;
			cache.clear();
			waiters.clear();
		}
	}

	static boolean canLookup(SpotifyPlaybackState state)
	{
		return state != null && state.isItemAvailable() &&
			!state.getTitle().trim().isEmpty() && !state.getArtist().trim().isEmpty();
	}

	static String keyFor(SpotifyPlaybackState state)
	{
		if (!canLookup(state))
		{
			return "";
		}
		return normalize(state.getTitle()) + '\n' + normalize(state.getArtist()) + '\n' +
			normalize(state.getAlbum()) + '\n' + Math.max(0, Math.round(state.getDurationMs() / 1_000f));
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private void finish(
		String key,
		LyricsState lyrics,
		boolean cacheResult,
		long requestGeneration)
	{
		List<Consumer<LyricsState>> callbacks;
		synchronized (lock)
		{
			if (requestGeneration != generation)
			{
				return;
			}
			callbacks = waiters.remove(key);
			if (cacheResult)
			{
				cache.put(key, lyrics);
			}
		}
		if (callbacks != null)
		{
			for (Consumer<LyricsState> callback : callbacks)
			{
				callback.accept(lyrics);
			}
		}
	}
}
