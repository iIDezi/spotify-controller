package com.spotifycontroller;

import java.util.Objects;

final class SpotifySearchResult
{
	private final String uri;
	private final String title;
	private final String artist;
	private final String album;
	private final String artworkUrl;

	SpotifySearchResult(String uri, String title, String artist, String album, String artworkUrl)
	{
		this.uri = Objects.requireNonNull(uri);
		this.title = Objects.requireNonNull(title);
		this.artist = Objects.requireNonNull(artist);
		this.album = Objects.requireNonNull(album);
		this.artworkUrl = Objects.requireNonNull(artworkUrl);
	}

	String getUri()
	{
		return uri;
	}

	String getTitle()
	{
		return title;
	}

	String getArtist()
	{
		return artist;
	}

	String getAlbum()
	{
		return album;
	}

	String getArtworkUrl()
	{
		return artworkUrl;
	}
}
