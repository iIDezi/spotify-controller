package com.spotifycontroller;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpotifyQueuePlaybackTest
{
	@Test
	public void advancesThroughTheExistingQueueToTheSelectedRow()
	{
		List<SpotifyQueueItem> displayedQueue = java.util.Arrays.asList(
			item("track-a"),
			item("track-b"),
			item("track-c"),
			item("track-d"));

		assertEquals(1, SpotifyApiClient.queueAdvanceCount(displayedQueue, 0));
		assertEquals(3, SpotifyApiClient.queueAdvanceCount(displayedQueue, 2));
		assertEquals(4, SpotifyApiClient.queueAdvanceCount(displayedQueue, 3));
	}

	@Test
	public void ignoresInvalidQueueSelections()
	{
		List<SpotifyQueueItem> displayedQueue = Collections.singletonList(item("track-a"));

		assertEquals(0, SpotifyApiClient.queueAdvanceCount(displayedQueue, -1));
		assertEquals(0, SpotifyApiClient.queueAdvanceCount(displayedQueue, 2));
		assertEquals(0, SpotifyApiClient.queueAdvanceCount(null, 0));
	}

	@Test
	public void mutesAndPausesBeforeAdvancingThenRestoresAudio() throws Exception
	{
		List<String> requests = Collections.synchronizedList(new ArrayList<>());
		OkHttpClient httpClient = new OkHttpClient.Builder()
			.addInterceptor(chain ->
			{
				Request request = chain.request();
				String query = request.url().encodedQuery();
				requests.add(request.method() + " " + request.url().encodedPath() +
					(query == null ? "" : "?" + query));
				return new Response.Builder()
					.request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(204)
					.message("No Content")
					.body(ResponseBody.create(null, new byte[0]))
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
		};
		SpotifyApiClient client = new SpotifyApiClient(config, null, httpClient, new Gson());
		CountDownLatch completed = new CountDownLatch(1);
		client.configureSessionForTesting(
			"test-token", System.currentTimeMillis() + 60_000L, listener(completed));
		client.setExecutorForTesting(Executors.newSingleThreadScheduledExecutor());

		try
		{
			List<SpotifyQueueItem> queue = new ArrayList<>();
			for (int index = 0; index < 10; index++)
			{
				queue.add(item("track-" + index));
			}
			long startedAtMs = System.currentTimeMillis();
			client.playFromQueue(queue, 9, true, 72);
			assertTrue("Queue transition did not finish", completed.await(5, TimeUnit.SECONDS));
			assertTrue("Ten-song queue transition was too slow",
				System.currentTimeMillis() - startedAtMs < 2_000L);

			List<String> expected = new ArrayList<>(Arrays.asList(
				"PUT /v1/me/player/volume?volume_percent=0",
				"PUT /v1/me/player/pause"));
			for (int index = 0; index < 10; index++)
			{
				expected.add("POST /v1/me/player/next");
			}
			expected.add("PUT /v1/me/player/pause");
			expected.add("PUT /v1/me/player/volume?volume_percent=72");
			expected.add("PUT /v1/me/player/play");
			assertEquals(expected, new ArrayList<>(requests).subList(0, expected.size()));
		}
		finally
		{
			client.stop();
		}
	}

	private static SpotifyListener listener(CountDownLatch completed)
	{
		return new SpotifyListener()
		{
			@Override
			public void onPlaybackState(SpotifyPlaybackState state)
			{
			}

			@Override
			public void onQueueState(SpotifyQueueState state)
			{
			}

			@Override
			public void onQueueTransitionChanged(boolean active)
			{
				if (!active)
				{
					completed.countDown();
				}
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

	private static SpotifyQueueItem item(String id)
	{
		return new SpotifyQueueItem("spotify:track:" + id, "track", id, "Artist");
	}
}
