package com.spotifycontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SpotifyQueueState
{
	private static final SpotifyQueueState EMPTY = new SpotifyQueueState(Collections.emptyList());

	private final List<SpotifyQueueItem> items;

	SpotifyQueueState(List<SpotifyQueueItem> items)
	{
		this.items = Collections.unmodifiableList(new ArrayList<>(items));
	}

	static SpotifyQueueState empty()
	{
		return EMPTY;
	}

	List<SpotifyQueueItem> getItems()
	{
		return items;
	}
}
