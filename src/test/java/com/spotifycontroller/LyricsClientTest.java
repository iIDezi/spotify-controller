package com.spotifycontroller;

import com.google.gson.Gson;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LyricsClientTest
{
	@Test
	public void coalescesAndCachesOneLookupPerTrack() throws Exception
	{
		AtomicInteger requests = new AtomicInteger();
		AtomicReference<HttpUrl> requestedUrl = new AtomicReference<>();
		OkHttpClient httpClient = new OkHttpClient.Builder()
			.addInterceptor(chain ->
			{
				requests.incrementAndGet();
				requestedUrl.set(chain.request().url());
				return new Response.Builder()
					.request(chain.request())
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(ResponseBody.create(MediaType.parse("application/json"),
						"{\"syncedLyrics\":\"[00:00.00]Hello\"}"))
					.build();
			})
			.build();
		LyricsClient client = new LyricsClient(httpClient, new Gson());
		SpotifyPlaybackState state = SpotifyPlaybackState.of(
			true, "track", "Song", "Artist", "Album", "Device", "", "", 0, 180_000);

		CountDownLatch callbacks = new CountDownLatch(2);
		client.requestLyrics(state, lyrics -> callbacks.countDown());
		client.requestLyrics(state, lyrics -> callbacks.countDown());
		assertTrue("Lyrics callbacks did not finish", callbacks.await(5, TimeUnit.SECONDS));
		assertEquals(1, requests.get());
		assertEquals("Song", requestedUrl.get().queryParameter("track_name"));
		assertEquals("Artist", requestedUrl.get().queryParameter("artist_name"));
		assertEquals("Album", requestedUrl.get().queryParameter("album_name"));
		assertEquals("180", requestedUrl.get().queryParameter("duration"));

		AtomicInteger cachedCallbacks = new AtomicInteger();
		client.requestLyrics(state, lyrics -> cachedCallbacks.incrementAndGet());
		assertEquals(1, cachedCallbacks.get());
		assertEquals(1, requests.get());
		client.reset();
		httpClient.dispatcher().executorService().shutdownNow();
		httpClient.connectionPool().evictAll();
	}
}
