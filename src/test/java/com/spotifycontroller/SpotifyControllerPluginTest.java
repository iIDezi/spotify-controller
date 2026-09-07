package com.spotifycontroller;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SpotifyControllerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SpotifyControllerPlugin.class);
		RuneLite.main(args);
	}
}
