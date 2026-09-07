package com.spotifycontroller;

import java.util.Objects;

public final class SpotifyPlaybackState
{
	private final boolean itemAvailable;
	private final boolean playing;
	private final String itemType;
	private final String title;
	private final String artist;
	private final String album;
	private final String device;
	private final String uri;
	private final String artworkUrl;
	private final int progressMs;
	private final int durationMs;
	private final boolean volumeAvailable;
	private final int volumePercent;
	private final boolean shuffleEnabled;
	private final String repeatMode;
	private final long receivedAtMs;

	private SpotifyPlaybackState(
		boolean itemAvailable,
		boolean playing,
		String itemType,
		String title,
		String artist,
		String album,
		String device,
		String uri,
		String artworkUrl,
		int progressMs,
		int durationMs,
		boolean volumeAvailable,
		int volumePercent,
		boolean shuffleEnabled,
		String repeatMode,
		long receivedAtMs)
	{
		this.itemAvailable = itemAvailable;
		this.playing = playing;
		this.itemType = Objects.requireNonNull(itemType);
		this.title = Objects.requireNonNull(title);
		this.artist = Objects.requireNonNull(artist);
		this.album = Objects.requireNonNull(album);
		this.device = Objects.requireNonNull(device);
		this.uri = Objects.requireNonNull(uri);
		this.artworkUrl = Objects.requireNonNull(artworkUrl);
		this.progressMs = Math.max(0, progressMs);
		this.durationMs = Math.max(0, durationMs);
		this.volumeAvailable = volumeAvailable;
		this.volumePercent = Math.max(0, Math.min(100, volumePercent));
		this.shuffleEnabled = shuffleEnabled;
		this.repeatMode = normalizeRepeatMode(repeatMode);
		this.receivedAtMs = receivedAtMs;
	}

	public static SpotifyPlaybackState idle()
	{
		return new SpotifyPlaybackState(false, false, "", "Nothing playing", "", "", "No active device", "", "", 0, 0,
			false, 0, false, "off",
			System.currentTimeMillis());
	}

	public static SpotifyPlaybackState of(
		boolean playing,
		String itemType,
		String title,
		String artist,
		String album,
		String device,
		int progressMs,
		int durationMs)
	{
		return of(playing, itemType, title, artist, album, device, "", "", progressMs, durationMs);
	}

	public static SpotifyPlaybackState of(
		boolean playing,
		String itemType,
		String title,
		String artist,
		String album,
		String device,
		String uri,
		String artworkUrl,
		int progressMs,
		int durationMs)
	{
		return of(playing, itemType, title, artist, album, device, uri, artworkUrl, progressMs, durationMs, false, 0);
	}

	public static SpotifyPlaybackState of(
		boolean playing,
		String itemType,
		String title,
		String artist,
		String album,
		String device,
		String uri,
		String artworkUrl,
		int progressMs,
		int durationMs,
		boolean volumeAvailable,
		int volumePercent)
	{
		return of(playing, itemType, title, artist, album, device, uri, artworkUrl,
			progressMs, durationMs, volumeAvailable, volumePercent, false, "off");
	}

	public static SpotifyPlaybackState of(
		boolean playing,
		String itemType,
		String title,
		String artist,
		String album,
		String device,
		String uri,
		String artworkUrl,
		int progressMs,
		int durationMs,
		boolean volumeAvailable,
		int volumePercent,
		boolean shuffleEnabled,
		String repeatMode)
	{
		return new SpotifyPlaybackState(true, playing, safe(itemType), safe(title), safe(artist), safe(album), safe(device),
			safe(uri), safe(artworkUrl), progressMs, durationMs, volumeAvailable, volumePercent,
			shuffleEnabled, repeatMode,
			System.currentTimeMillis());
	}

	private static String safe(String value)
	{
		return value == null ? "" : value;
	}

	private static String normalizeRepeatMode(String repeatMode)
	{
		if ("context".equals(repeatMode) || "track".equals(repeatMode))
		{
			return repeatMode;
		}
		return "off";
	}

	public boolean isItemAvailable()
	{
		return itemAvailable;
	}

	public boolean isPlaying()
	{
		return playing;
	}

	public String getItemType()
	{
		return itemType;
	}

	public String getTitle()
	{
		return title;
	}

	public String getArtist()
	{
		return artist;
	}

	public String getAlbum()
	{
		return album;
	}

	public String getDevice()
	{
		return device;
	}

	public String getUri()
	{
		return uri;
	}

	public String getArtworkUrl()
	{
		return artworkUrl;
	}

	public int getProgressMs()
	{
		return progressMs;
	}

	public int getDurationMs()
	{
		return durationMs;
	}

	public boolean isVolumeAvailable()
	{
		return volumeAvailable;
	}

	public int getVolumePercent()
	{
		return volumePercent;
	}

	public boolean isShuffleEnabled()
	{
		return shuffleEnabled;
	}

	public String getRepeatMode()
	{
		return repeatMode;
	}

	public int getEstimatedProgressMs(long nowMs)
	{
		long elapsed = playing ? Math.max(0L, nowMs - receivedAtMs) : 0L;
		return (int) Math.min(durationMs, progressMs + elapsed);
	}

	SpotifyPlaybackState withPlaying(boolean newPlaying)
	{
		long nowMs = System.currentTimeMillis();
		return new SpotifyPlaybackState(
			itemAvailable,
			newPlaying,
			itemType,
			title,
			artist,
			album,
			device,
			uri,
			artworkUrl,
			getEstimatedProgressMs(nowMs),
			durationMs,
			volumeAvailable,
			volumePercent,
			shuffleEnabled,
			repeatMode,
			nowMs);
	}

	SpotifyPlaybackState withProgress(int newProgressMs)
	{
		return new SpotifyPlaybackState(
			itemAvailable,
			playing,
			itemType,
			title,
			artist,
			album,
			device,
			uri,
			artworkUrl,
			Math.max(0, Math.min(durationMs, newProgressMs)),
			durationMs,
			volumeAvailable,
			volumePercent,
			shuffleEnabled,
			repeatMode,
			System.currentTimeMillis());
	}

	SpotifyPlaybackState withVolume(int newVolumePercent)
	{
		long nowMs = System.currentTimeMillis();
		return new SpotifyPlaybackState(
			itemAvailable,
			playing,
			itemType,
			title,
			artist,
			album,
			device,
			uri,
			artworkUrl,
			getEstimatedProgressMs(nowMs),
			durationMs,
			true,
			newVolumePercent,
			shuffleEnabled,
			repeatMode,
			nowMs);
	}

	SpotifyPlaybackState withShuffleEnabled(boolean enabled)
	{
		long nowMs = System.currentTimeMillis();
		return new SpotifyPlaybackState(
			itemAvailable,
			playing,
			itemType,
			title,
			artist,
			album,
			device,
			uri,
			artworkUrl,
			getEstimatedProgressMs(nowMs),
			durationMs,
			volumeAvailable,
			volumePercent,
			enabled,
			repeatMode,
			nowMs);
	}

	SpotifyPlaybackState withRepeatMode(String mode)
	{
		long nowMs = System.currentTimeMillis();
		return new SpotifyPlaybackState(
			itemAvailable,
			playing,
			itemType,
			title,
			artist,
			album,
			device,
			uri,
			artworkUrl,
			getEstimatedProgressMs(nowMs),
			durationMs,
			volumeAvailable,
			volumePercent,
			shuffleEnabled,
			mode,
			nowMs);
	}
}
