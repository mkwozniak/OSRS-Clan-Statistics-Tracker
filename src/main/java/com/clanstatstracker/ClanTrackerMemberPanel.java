package com.clanstatstracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Slf4j
public class ClanTrackerMemberPanel extends JPanel
{
    private final ClanStatisticsTracker plugin;
    private final MemberData memberData;
    private final int currentSessionMessages;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

    public ClanTrackerMemberPanel(ClanStatisticsTracker plugin, MemberData memberData, int currentSessionMessages)
    {
        this.plugin = plugin;
        this.memberData = memberData;
        this.currentSessionMessages = currentSessionMessages;

        //setLayout(new BorderLayout());
        //setBorder(new EmptyBorder(0, 0, 0, 0));

        // Create main content panel with scroll
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Back button
        JButton backButton = new JButton("← Back to Member List");
        backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        backButton.addActionListener(e -> {
            Container parent = this.getParent();
            if (parent instanceof JPanel)
            {
                CardLayout layout = (CardLayout) parent.getLayout();
                layout.show(parent, "MAIN");
            }
        });
        add(backButton, BorderLayout.NORTH);

        // Fetch button
        JButton fetchButton = new JButton("Update From Hiscores");
        fetchButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        fetchButton.addActionListener(e -> {
            plugin.updatePlayerHiscoreData(memberData.getPlayerName());
        });
        add(fetchButton, BorderLayout.WEST);

        // Progression button
        JButton progressButton = new JButton("Check Progress");
        progressButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressButton.addActionListener(e -> {
            plugin.checkMemberProgression(memberData.getPlayerName());
            invalidate();
            repaint();
        });
        add(progressButton, BorderLayout.EAST);

        // Member name title
        JLabel nameLabel = new JLabel(memberData.getPlayerName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        //nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setBorder(new EmptyBorder(10, 0, 15, 0));
        contentPanel.add(nameLabel);

        // Add progression section
        addProgressionSection(contentPanel);

        // Add separator
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(createSeparator());
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Add messaging section
        addMessagingSection(contentPanel);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.WEST);
    }

    private void addProgressionSection(JPanel parent)
    {
        JLabel sectionLabel = new JLabel("Progression Since Last Check");
        sectionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        //sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        parent.add(sectionLabel);

        ProgressionData progression = memberData.getProgression();

        if (progression == null || !progression.hasAnyProgression())
        {
            JLabel noProgressLabel = new JLabel("No progression data available");
            noProgressLabel.setForeground(Color.GRAY);
            noProgressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            parent.add(noProgressLabel);
            return;
        }

        // Create a panel for progression data
        JPanel progressionPanel = new JPanel();
        progressionPanel.setLayout(new BoxLayout(progressionPanel, BoxLayout.Y_AXIS));
        progressionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressionPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Skills section
        if (progression.hasSkillProgression())
        {
            JLabel skillsHeader = new JLabel("Skills:");
            skillsHeader.setFont(new Font("Arial", Font.BOLD, 12));
            skillsHeader.setBorder(new EmptyBorder(5, 5, 5, 5));
            progressionPanel.add(skillsHeader);

            for (Skill skill : Skill.values())
            {
                if (skill == Skill.OVERALL)
                {
                    continue;
                }

                SkillProgress skillProg = progression.getSkillProgress(skill);
                if (skillProg != null && skillProg.hasProgression())
                {
                    JPanel skillPanel = createSkillProgressPanel(skill, skillProg);
                    progressionPanel.add(skillPanel);
                }
            }
        }

        // Boss kills section
        if (progression.hasBossProgression())
        {
            progressionPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JLabel bossHeader = new JLabel("Bosses:");
            bossHeader.setFont(new Font("Arial", Font.BOLD, 12));
            bossHeader.setBorder(new EmptyBorder(5, 5, 5, 5));
            progressionPanel.add(bossHeader);

            for (String bossName : progression.getBossNames())
            {
                int killsGained = progression.getBossKillsGained(bossName);
                if (killsGained > 0)
                {
                    JPanel bossPanel = createBossProgressPanel(bossName, killsGained);
                    progressionPanel.add(bossPanel);
                }
            }
        }

        parent.add(progressionPanel);
    }

    private JPanel createSkillProgressPanel(Skill skill, SkillProgress progress)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(3, 0, 3, 0));
        panel.setMaximumSize(new Dimension(128, 16));

        JLabel nameLabel = new JLabel(skill.getName());
        panel.add(nameLabel, BorderLayout.WEST);

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        if (progress.getLevelsGained() > 0)
        {
            JLabel levelLabel = new JLabel("+" + progress.getLevelsGained() + " lvl");
            levelLabel.setForeground(new Color(0, 150, 0));
            levelLabel.setFont(new Font("Arial", Font.BOLD, 11));
            progressPanel.add(levelLabel);
        }

        if (progress.getExperienceGained() > 0)
        {
            JLabel xpLabel = new JLabel("+" + NUMBER_FORMAT.format(progress.getExperienceGained()) + " xp");
            xpLabel.setForeground(new Color(0, 100, 200));
            xpLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            progressPanel.add(xpLabel);
        }

        panel.add(progressPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createBossProgressPanel(String bossName, int kills)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(3, 10, 3, 10));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel nameLabel = new JLabel(bossName);
        panel.add(nameLabel, BorderLayout.WEST);

        JLabel killsLabel = new JLabel("+" + NUMBER_FORMAT.format(kills) + " kills");
        killsLabel.setForeground(new Color(150, 0, 150));
        killsLabel.setFont(new Font("Arial", Font.BOLD, 11));
        panel.add(killsLabel, BorderLayout.EAST);

        return panel;
    }

    private void addMessagingSection(JPanel parent)
    {
        JLabel sectionLabel = new JLabel("Clan Chat Activity");
        sectionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        parent.add(sectionLabel);

        // Current session
        JPanel currentSessionPanel = new JPanel(new BorderLayout());
        currentSessionPanel.setBorder(BorderFactory.createTitledBorder("Current Session"));
        currentSessionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentSessionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel currentLabel = new JLabel("Messages sent: " + currentSessionMessages);
        currentLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        currentSessionPanel.add(currentLabel, BorderLayout.WEST);

        for(String notif : memberData.getNotifications())
        {
            JTextArea lbl = new JTextArea(notif);
            lbl.setLineWrap(true);
            lbl.setBorder(new EmptyBorder(5, 1, 5, 1));
            currentSessionPanel.add(lbl, BorderLayout.SOUTH);
        }

        parent.add(currentSessionPanel);
        parent.add(Box.createRigidArea(new Dimension(0, 10)));

        // Previous sessions
        List<ChatSession> sessions = memberData.getChatSessions();
        if (sessions.isEmpty())
        {
            JLabel noSessionsLabel = new JLabel("No previous sessions recorded");
            noSessionsLabel.setForeground(Color.GRAY);
            noSessionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            noSessionsLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
            parent.add(noSessionsLabel);
        }
        else
        {
            JLabel historyLabel = new JLabel("Session History:");
            historyLabel.setFont(new Font("Arial", Font.BOLD, 12));
            historyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            historyLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
            parent.add(historyLabel);

            for (int i = sessions.size() - 1; i >= 0; i--)
            {
                ChatSession session = sessions.get(i);
                JPanel sessionPanel = createSessionPanel(session, i);
                parent.add(sessionPanel);
                parent.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }
    }

    private JPanel createSessionPanel(ChatSession session, int index)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel dateLabel = new JLabel(session.getDate());
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        dateLabel.setForeground(Color.GRAY);

        JLabel messageLabel = new JLabel("Messages: " + session.getMessageCount());
        messageLabel.setFont(new Font("Arial", Font.BOLD, 12));

        infoPanel.add(dateLabel);
        infoPanel.add(messageLabel);

        for(String notif : session.getNotifications())
        {
            JTextArea lbl = new JTextArea(notif);
            lbl.setLineWrap(true);
            lbl.setBorder(new EmptyBorder(5, 5, 5, 5));
            infoPanel.add(lbl, BorderLayout.SOUTH);
        }

        JButton deleteButton = new JButton("Delete");
        deleteButton.setFont(new Font("Arial", Font.PLAIN, 10));
        deleteButton.setPreferredSize(new Dimension(70, 40));

        final int sessionIndex = index;
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete this session data?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION)
            {
                plugin.deleteSessionData(memberData.getPlayerName(), sessionIndex);

                // Refresh the panel
                Container parent = this.getParent();
                if (parent != null)
                {
                    int currentMessages = this.currentSessionMessages;
                    ClanTrackerMemberPanel newPanel = new ClanTrackerMemberPanel(
                            plugin,
                            memberData,
                            currentMessages
                    );

                    parent.remove(ClanTrackerMemberPanel.this);
                    parent.add(newPanel, "MEMBER");
                    ((CardLayout) parent.getLayout()).show(parent, "MEMBER");
                    parent.revalidate();
                    parent.repaint();
                }
            }
        });

        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(deleteButton, BorderLayout.EAST);

        return panel;
    }

    private JSeparator createSeparator()
    {
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        return separator;
    }
}