package com.clanstatstracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ClanStatisticsTrackerPlugin
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClanStatisticsTracker.class);
		RuneLite.main(args);
	}
}