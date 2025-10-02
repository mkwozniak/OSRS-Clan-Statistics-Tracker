package com.clanstatstracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.client.RuneLite;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.SessionClose;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@PluginDescriptor(
		name = "Clan Tracker",
		description = "Track clan member stats and progression",
		tags = {"clan", "tracker", "stats"}
)
public class ClanStatisticsTracker extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private WorldService worldService;

	@Inject
	private SpriteManager spriteManager;

	private ClanTrackerMainPanel mainPanel;
	private NavigationButton navButton;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private File dataDirectory;
	private Map<String, MemberData> memberDataMap = new ConcurrentHashMap<>();
	private Map<String, Integer> sessionMessageCounts = new ConcurrentHashMap<>();

	@Inject
	private HiscoreClient hiscoreClient;

	@Override
	protected void startUp() throws Exception
	{
		System.out.println("Clan Tracker started!");

		// Initialize data directory
		dataDirectory = new File(RuneLite.RUNELITE_DIR, "clantracker");
		if (!dataDirectory.exists())
		{
			dataDirectory.mkdirs();
		}

		// Initialize hiscore client
		//hiscoreClient = new HiscoreClient(worldService.getWorlds());

		// Load existing member data
		loadAllMemberData();

		// Create main panel
		mainPanel = new ClanTrackerMainPanel(this);

		// Create navigation button
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon-ct.png");
		//BufferedImage icon = spriteManager.getSprite(SpriteID.TAB_CLAN_CHAT, 0);
		navButton = NavigationButton.builder()
				.tooltip("Clan Tracker")
				.icon(icon)
				.priority(10)
				.panel(mainPanel)
				.build();

		clientToolbar.addNavigation(navButton);

		// Update panel with current clan members
		updateClanMembers();
	}

	@Override
	protected void shutDown() throws Exception
	{
		System.out.println("Clan Tracker stopped!");
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		System.out.println("Client Shutdown: Clan Tracker stopped!");
		clientToolbar.removeNavigation(navButton);

		// Save all session data before shutdown
		saveAllSessionData();
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		updateClanMembers();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.CLAN_CHAT ||
				event.getType() == ChatMessageType.CLAN_MESSAGE ||
				event.getType() == ChatMessageType.CLAN_GIM_CHAT ||
				event.getType() == ChatMessageType.CLAN_GIM_MESSAGE)
		{
			String sender = event.getName();
			if (sender != null && !sender.isEmpty())
			{
				// Clean the name (remove clan rank icons)
				sender = sender.replaceAll("<.*?>", "").trim();
				sessionMessageCounts.merge(sender, 1, Integer::sum);
			}
			else {
				System.out.println("Non User Message was sent to clan chat: " + event.getMessage());
				for (String member : memberDataMap.keySet())
				{
					if(event.getMessage().contains(member))
					{
						memberDataMap.get(member).addNotification(event.getMessage());
						break;
					}
				}
			}
		}
	}

	public void updateClanMembers()
	{
		SwingUtilities.invokeLater(() -> {
			ClanSettings clanSettings = client.getClanSettings();

			if(clanSettings == null)
			{
				System.out.println("Could not load clan settings");
				return;
			}

			int membs = 0;
			List<String> memberNames = new ArrayList<>();
			for (ClanMember member : clanSettings.getMembers())
			{
				String name = member.getName();
				memberNames.add(name);

				// Ensure member data exists
				memberDataMap.computeIfAbsent(name, k -> {
					MemberData data = loadMemberData(name);
					if (data == null)
					{
						data = new MemberData(name);
					}
					return data;
				});

				membs++;
				System.out.println("Loaded Clan Member : " + name);
			}

			System.out.println("Members " + membs);
			if(membs != 0)
			{
				mainPanel.updateMemberList(memberNames);
			}
		});
	}

	public void rebuildAllHiscores()
	{
		//ClanChannel clanChannel = client.getClanChannel();
		ClanSettings clanSettings = client.getClanSettings();
		if (clanSettings == null)
		{
			JOptionPane.showMessageDialog(mainPanel,
					"You must be in a clan to rebuild hiscore data!",
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(mainPanel,
				"This will fetch hiscore data for all clan members.\nThis may take several minutes. Continue?",
				"Confirm Rebuild",
				JOptionPane.YES_NO_OPTION);

		if (confirm != JOptionPane.YES_OPTION)
		{
			return;
		}

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (ClanMember member : clanSettings.getMembers())
		{
			String name = member.getName();
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try
				{
					fetchAndSaveHiscoreData(name);
					System.out.println("Fetched hiscore data for: {}" + name);
				}
				catch (Exception e)
				{
					System.out.println("Failed to fetch hiscore data for {}: {}" + name + ":" + e.getMessage());
				}
			});
			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
				.thenRun(() -> {
					SwingUtilities.invokeLater(() -> {
						JOptionPane.showMessageDialog(mainPanel,
								"Hiscore rebuild complete!",
								"Success",
								JOptionPane.INFORMATION_MESSAGE);
					});
				});
	}

	private void fetchAndSaveHiscoreData(String playerName) throws IOException
	{
		HiscoreResult result = hiscoreClient.lookup(playerName);

		MemberData data = memberDataMap.computeIfAbsent(playerName, MemberData::new);

		// Store current stats as baseline if this is first fetch
		if (data.getLastSnapshot() == null)
		{
			data.setLastSnapshot(new HiscoreSnapshot(result));
		}
		else
		{
			// Update with new snapshot
			HiscoreSnapshot newSnapshot = new HiscoreSnapshot(result);
			data.updateWithNewSnapshot(newSnapshot);
		}

		saveMemberData(data);
	}

	public void updatePlayerHiscoreData(String playerName){
		CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
			try
			{
				fetchAndSaveHiscoreData(playerName);
				System.out.println("Fetched hiscore data for: {}" + playerName);
			}
			catch (Exception e)
			{
				System.out.println("Failed to fetch hiscore data for {}: {}" + playerName + ":" + e.getMessage());
				JOptionPane.showMessageDialog(mainPanel,
						"Hiscore Fetch failed. They may not qualify or an error occured.",
						"Failure",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		CompletableFuture.completedFuture(future)
				.thenRun(() -> {
					SwingUtilities.invokeLater(() -> {
						JOptionPane.showMessageDialog(mainPanel,
								"Hiscore Fetch complete!",
								"Success",
								JOptionPane.INFORMATION_MESSAGE);
					});
				});
	}

	public void checkMemberProgression(String playerName)
	{
		CompletableFuture.runAsync(() -> {
			try
			{
				HiscoreResult result = hiscoreClient.lookup(playerName);
				MemberData data = memberDataMap.get(playerName);

				if (data != null && data.getLastSnapshot() != null)
				{
					//HiscoreSnapshot newSnapshot = new HiscoreSnapshot(result);
					//data.updateWithNewSnapshot(newSnapshot);
					//saveMemberData(data);

					SwingUtilities.invokeLater(() -> {
						showMemberPanel(playerName);
					});
				}
				else
				{
					SwingUtilities.invokeLater(() -> {
						JOptionPane.showMessageDialog(mainPanel,
								"No baseline data for this member. Please rebuild hiscores first.",
								"No Data",
								JOptionPane.WARNING_MESSAGE);
					});
				}
			}
			catch (Exception e)
			{
				System.out.println("Failed to check progression for {}: {}" + playerName + ":" + e.getMessage());
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(mainPanel,
							"Failed to fetch data: " + e.getMessage(),
							"Error",
							JOptionPane.ERROR_MESSAGE);
				});
			}
		});
	}

	public void showMemberPanel(String playerName)
	{
		MemberData data = memberDataMap.get(playerName);
		if (data == null)
		{
			JOptionPane.showMessageDialog(mainPanel,
					"No data available for this member.",
					"No Data",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		int currentSessionMessages = sessionMessageCounts.getOrDefault(playerName, 0);
		ClanTrackerMemberPanel memberPanel = new ClanTrackerMemberPanel(this, data, currentSessionMessages);
		mainPanel.showMemberPanel(memberPanel);
	}

	public void deleteSessionData(String playerName, int sessionIndex)
	{
		MemberData data = memberDataMap.get(playerName);
		if (data != null)
		{
			data.removeSession(sessionIndex);
			saveMemberData(data);
		}
	}

	private void saveAllSessionData()
	{
		String sessionDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		System.out.println("save1");
		for (Map.Entry<String, Integer> entry : sessionMessageCounts.entrySet())
		{
			System.out.println("save2");
			String playerName = entry.getKey();
			int messageCount = entry.getValue();

			if (messageCount > 0)
			{
				System.out.println("save3");
				MemberData data = memberDataMap.get(playerName);
				if (data != null)
				{
					System.out.println("save4");
					data.addSession(sessionDate, messageCount);
					saveMemberData(data);
					System.out.println("Saving Message Session for player " + playerName);
				}
			}
		}
	}

	private void saveMemberData(MemberData data)
	{
		File file = new File(dataDirectory, sanitizeFilename(data.getPlayerName()) + ".json");
		try (FileWriter writer = new FileWriter(file))
		{
			gson.toJson(data, writer);
		}
		catch (IOException e)
		{
			System.out.println("Failed to save member data for {}: {}" + data.getPlayerName() + ":" + e.getMessage());
		}
	}

	private MemberData loadMemberData(String playerName)
	{
		File file = new File(dataDirectory, sanitizeFilename(playerName) + ".json");
		if (!file.exists())
		{
			return null;
		}

		try (FileReader reader = new FileReader(file))
		{
			return gson.fromJson(reader, MemberData.class);
		}
		catch (IOException e)
		{
			System.out.println("Failed to load member data for {}: {}" + playerName + ":" + e.getMessage());
			return null;
		}
	}

	private void loadAllMemberData()
	{
		File[] files = dataDirectory.listFiles((dir, name) -> name.endsWith(".json"));
		if (files != null)
		{
			for (File file : files)
			{
				try (FileReader reader = new FileReader(file))
				{
					MemberData data = gson.fromJson(reader, MemberData.class);
					if (data != null)
					{
						memberDataMap.put(data.getPlayerName(), data);
					}
				}
				catch (IOException e)
				{
					System.out.println("Failed to load file {}: {}" + file.getName() + ":" + e.getMessage());
				}
			}
		}
	}

	private String sanitizeFilename(String name)
	{
		return name.replaceAll("[^a-zA-Z0-9_-]", "_");
	}

	@Provides
	ClanStatisticsTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanStatisticsTrackerConfig.class);
	}
}