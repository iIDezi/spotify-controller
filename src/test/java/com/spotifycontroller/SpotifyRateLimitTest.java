package com.spotifycontroller;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpotifyRateLimitTest
{
	@Test
	public void developmentQuotaStopsAutomaticRetriesUntilManualRefresh() throws Exception
	{
		AtomicInteger requestCount = new AtomicInteger();
		CompletableFuture<Void> secondRequest = new CompletableFuture<>();
		String quotaJson = "{\"error\":{\"status\":429,\"message\":\"Too many requests\",\"reason\":\"QUOTA_EXCEEDED\"}}";
		OkHttpClient httpClient = new OkHttpClient.Builder()
			.addInterceptor(chain ->
			{
				Request request = chain.request();
				if (requestCount.incrementAndGet() == 2)
				{
					secondRequest.complete(null);
				}
				return new Response.Builder()
					.request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(429)
					.message("Too Many Requests")
					.body(ResponseBody.create(null, quotaJson.getBytes(StandardCharsets.UTF_8)))
					.build();
			})
			.build();
		SpotifyControllerConfig config = config();
		SpotifyApiClient client = new SpotifyApiClient(config, null, httpClient, new Gson());
		CompletableFuture<String> quotaStatus = new CompletableFuture<>();
		client.configureSessionForTesting(
			"test-token", System.currentTimeMillis() + 60_000L, listener(quotaStatus));

		try
		{
			client.retryNow();
			String status = quotaStatus.get(5, TimeUnit.SECONDS);
			assertTrue(status.contains("Development Mode quota exhausted"));

			client.resetPlaybackRequestTimeForTesting();
			client.refreshNow();
			assertEquals(1, requestCount.get());

			client.retryNow();
			secondRequest.get(5, TimeUnit.SECONDS);
			assertEquals(2, requestCount.get());
		}
		finally
		{
			client.stop();
		}
	}

	private static SpotifyControllerConfig config()
	{
		return new SpotifyControllerConfig()
		{
			@Override
			public boolean enableSpotifyAccess()
			{
				return true;
			}

			@Override
			public int refreshSeconds()
			{
				return 10;
			}
		};
	}

	private static SpotifyListener listener(CompletableFuture<String> quotaStatus)
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
			}

			@Override
			public void onStatus(String message)
			{
				if (message.contains("Development Mode quota exhausted"))
				{
					quotaStatus.complete(message);
				}
			}

			@Override
			public void onConnectionChanged(boolean connected)
			{
			}
		};
	}

}
