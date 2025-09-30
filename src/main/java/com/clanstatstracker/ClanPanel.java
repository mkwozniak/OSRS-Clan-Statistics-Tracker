package com.clanstatstracker;

import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class ClanPanel extends PluginPanel
{
    private final JPanel memberListPanel = new JPanel();

    public ClanPanel()
    {
        super(false); // false = don’t automatically add scrollbars

        setLayout(new BorderLayout());

        JLabel title = new JLabel("Clan Members");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        memberListPanel.setLayout(new BoxLayout(memberListPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(memberListPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addMember(String memberName)
    {
        //JLabel label = new JLabel(memberName);
        JButton btn = new JButton(memberName);
        memberListPanel.add(btn);
        revalidate();
        repaint();
    }

    public void clearMembers()
    {
        memberListPanel.removeAll();
        revalidate();
        repaint();
    }
}
