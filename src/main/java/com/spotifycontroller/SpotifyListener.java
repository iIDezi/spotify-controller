package com.spotifycontroller;

interface SpotifyListener
{
	void onPlaybackState(SpotifyPlaybackState state);

	void onQueueState(SpotifyQueueState state);

	void onQueueTransitionChanged(boolean active);

	void onStatus(String message);

	void onConnectionChanged(boolean connected);
}
