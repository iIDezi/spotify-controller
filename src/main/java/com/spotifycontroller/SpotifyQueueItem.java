package com.spotifycontroller;

import java.util.Objects;

final class SpotifyQueueItem
{
	private final String uri;
	private final String type;
	private final String title;
	private final String artist;

	SpotifyQueueItem(String uri, String type, String title, String artist)
	{
		this.uri = Objects.requireNonNull(uri);
		this.type = Objects.requireNonNull(type);
		this.title = Objects.requireNonNull(title);
		this.artist = Objects.requireNonNull(artist);
	}

	String getUri()
	{
		return uri;
	}

	String getType()
	{
		return type;
	}

	String getTitle()
	{
		return title;
	}

	String getArtist()
	{
		return artist;
	}
}
