package com.spotifycontroller;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpotifyPlaybackFallbackTest
{
	@Test
	public void usesCurrentlyPlayingWhenPlaybackStateReturnsNoContent() throws Exception
	{
		List<String> requests = Collections.synchronizedList(new ArrayList<>());
		CompletableFuture<Void> queueRequested = new CompletableFuture<>();
		String playbackJson = "{" +
			"\"is_playing\":true," +
			"\"progress_ms\":12000," +
			"\"item\":{" +
			"\"type\":\"track\"," +
			"\"name\":\"Recovered Song\"," +
			"\"uri\":\"spotify:track:recovered\"," +
			"\"duration_ms\":180000," +
			"\"artists\":[{\"name\":\"Recovered Artist\"}]," +
			"\"album\":{\"name\":\"Recovered Album\",\"images\":[]}" +
			"}}";
		OkHttpClient httpClient = new OkHttpClient.Builder()
			.addInterceptor(chain ->
			{
				Request request = chain.request();
				String path = request.url().encodedPath();
				requests.add(path);
				int code = 204;
				byte[] body = new byte[0];
				if (path.endsWith("/currently-playing"))
				{
					code = 200;
					body = playbackJson.getBytes(StandardCharsets.UTF_8);
				}
				else if (path.endsWith("/queue"))
				{
					queueRequested.complete(null);
				}
				return new Response.Builder()
					.request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(code)
					.message(code == 200 ? "OK" : "No Content")
					.body(ResponseBody.create(null, body))
					.build();
			})
			.build();
		SpotifyControllerConfig config = new SpotifyControllerConfig()
		{
			@Override
			public boolean enableSpotifyAccess()
			{
				return true;
			}

			@Override
			public int refreshSeconds()
			{
				return 5;
			}
		};
		SpotifyApiClient client = new SpotifyApiClient(config, null, httpClient, new Gson());
		CompletableFuture<SpotifyPlaybackState> playbackReceived = new CompletableFuture<>();
		client.configureSessionForTesting(
			"test-token", System.currentTimeMillis() + 60_000L, listener(playbackReceived));

		try
		{
			client.refreshNow();
			CompletableFuture.allOf(playbackReceived, queueRequested).get(5, TimeUnit.SECONDS);
			SpotifyPlaybackState state = playbackReceived.getNow(SpotifyPlaybackState.idle());
			assertEquals("Recovered Song", state.getTitle());
			assertTrue(state.isPlaying());
			assertEquals("spotify:track:recovered", state.getUri());
			assertEquals("/v1/me/player", requests.get(0));
			assertEquals("/v1/me/player/currently-playing", requests.get(1));
			assertTrue(requests.contains("/v1/me/player/queue"));
		}
		finally
		{
			client.stop();
		}
	}

	private static SpotifyListener listener(CompletableFuture<SpotifyPlaybackState> playbackReceived)
	{
		return new SpotifyListener()
		{
			@Override
			public void onPlaybackState(SpotifyPlaybackState state)
			{
				if (state.isItemAvailable())
				{
					playbackReceived.complete(state);
				}
			}

			@Override
			public void onQueueState(SpotifyQueueState state)
			{
			}

			@Override
			public void onQueueTransitionChanged(boolean active)
			{
			}

			@Override
			public void onStatus(String message)
			{
			}

			@Override
			public void onConnectionChanged(boolean connected)
			{
			}
		};
	}

}
