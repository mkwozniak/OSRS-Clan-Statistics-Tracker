package com.clanstatstracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldListLoad;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanMember;
import net.runelite.client.game.SpriteManager;
import net.runelite.api.SpriteID;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Clan Statistics Tracker"
)
public class ClanStatisticsTracker extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ClanStatisticsTrackerConfig config;

	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private SpriteManager spriteManager;
	private NavigationButton navButton;

	private ClanPanel clanPanel;
	ClanSettings clanSettings;

	private boolean loadedNavigation = false;
	private boolean loadedClan = false;

	@Override
	protected void startUp() throws Exception {
		log.info("Plugin started!");
		clanPanel = new ClanPanel();
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Plugin stopped!");
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}

		if(!loadedNavigation)
		{
			BufferedImage icon = spriteManager.getSprite(SpriteID.TAB_CLAN_CHAT, 0);

			navButton = NavigationButton.builder()
					.tooltip("Clan Tracker")
					.icon(icon) // TODO: provide your icon BufferedImage
					.priority(5)
					.panel(clanPanel)
					.build();

			clientToolbar.addNavigation(navButton);
			loadedNavigation = true;
			loadedClan = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if(!loadedClan)
		{
			/*
			ClanChannel clan = client.getClanChannel();
			if (clan == null) return;

			clanPanel.clearMembers();
			for (ClanChannelMember member : clan.getMembers())
			{
				clanPanel.addMember(member.getName().toString());
			}
			*/

			clanSettings = client.getClanSettings();

			if(clanSettings == null)
			{
				System.out.println("Could not load clan settings");
				return;
			}

			int membs = 0;
			for (ClanMember member : clanSettings.getMembers())
			{
				String name = member.getName(); // Member’s RSN
				clanPanel.addMember(name);
				membs++;
				System.out.println("Loaded Clan Member : " + name);
			}

			System.out.println("Members " + membs);
			if(membs != 0)
			{
				loadedClan = true;
			}
		}
	}

	@Provides
	ClanStatisticsTrackerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ClanStatisticsTrackerConfig.class);
	}
}
