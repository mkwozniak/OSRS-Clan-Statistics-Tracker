package com.clanstatstracker;


import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClanTrackerMainPanel extends PluginPanel
{
    private final ClanStatisticsTracker plugin;
    private final JPanel memberListPanel;
    private final JPanel contentPanel;
    private final CardLayout cardLayout;
    private final List<JButton> memberButtons = new ArrayList<>();
    private final JScrollPane scrollPane;

    public ClanTrackerMainPanel(ClanStatisticsTracker plugin)
    {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create card layout for switching between main view and member view
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Create main view panel
        JPanel mainViewPanel = new JPanel(new BorderLayout());

        // Title label
        JLabel titleLabel = new JLabel("Clan Member Tracker", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainViewPanel.add(titleLabel, BorderLayout.NORTH);

        // Rebuild button
        JButton rebuildButton = new JButton("Rebuild All Hiscores");
        rebuildButton.setToolTipText("Fetch hiscore data for all clan members");
        rebuildButton.addActionListener(e -> plugin.rebuildAllHiscores());

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        buttonPanel.add(rebuildButton, BorderLayout.CENTER);
        mainViewPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Member list panel with scroll
        memberListPanel = new JPanel();
        memberListPanel.setLayout(new BoxLayout(memberListPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(memberListPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainViewPanel.add(scrollPane, BorderLayout.CENTER);

        // Add main view to content panel
        contentPanel.add(mainViewPanel, "MAIN");

        add(contentPanel, BorderLayout.CENTER);
    }

    public void updateMemberList(List<String> memberNames)
    {
        memberListPanel.removeAll();
        memberButtons.clear();

        // Sort members alphabetically
        List<String> sortedNames = new ArrayList<>(memberNames);
        sortedNames.sort(String.CASE_INSENSITIVE_ORDER);

        for (String name : sortedNames)
        {
            JButton memberButton = new JButton(name);
            memberButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            memberButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            memberButton.setFont(new Font("Arial", Font.PLAIN, 14));
            memberButton.setFocusPainted(false);

            memberButton.addActionListener(e -> {
                //plugin.checkMemberProgression(name);
                plugin.showMemberPanel(name);
            });

            memberButtons.add(memberButton);
            memberListPanel.add(memberButton);
            memberListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        // Add info label if no members
        if (sortedNames.isEmpty())
        {
            JLabel noMembersLabel = new JLabel("No clan members found");
            noMembersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            noMembersLabel.setForeground(Color.GRAY);
            memberListPanel.add(noMembersLabel);
        }

        memberListPanel.revalidate();
        memberListPanel.repaint();
    }

    public void showMemberPanel(ClanTrackerMemberPanel memberPanel)
    {
        // Remove existing member panel if any
        Component[] components = contentPanel.getComponents();
        for (Component comp : components)
        {
            if (comp instanceof ClanTrackerMemberPanel)
            {
                contentPanel.remove(comp);
            }
        }

        // Add new member panel
        contentPanel.add(memberPanel, "MEMBER");
        cardLayout.show(contentPanel, "MEMBER");
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    public void showMainPanel()
    {
        cardLayout.show(contentPanel, "MAIN");
    }
}