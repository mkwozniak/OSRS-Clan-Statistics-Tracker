package com.clanstatstracker;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class MemberData
{
    private String playerName;
    private HiscoreSnapshot lastSnapshot;
    private ProgressionData progression;
    private List<ChatSession> chatSessions;
    private List<String> notifications;

    public MemberData(String playerName)
    {
        this.playerName = playerName;
        this.chatSessions = new ArrayList<>();
        this.progression = new ProgressionData();
        this.notifications = new ArrayList<>();
    }

    public void updateWithNewSnapshot(HiscoreSnapshot newSnapshot)
    {
        if (lastSnapshot != null)
        {
            progression = new ProgressionData(lastSnapshot, newSnapshot);
        }
        lastSnapshot = newSnapshot;
    }

    public void addSession(String date, int messageCount)
    {
        chatSessions.add(new ChatSession(date, messageCount, notifications));
    }

    public void addNotification(String notif)
    {
        notifications.add(notif);
    }

    public List<String> getNotifications(){
        return notifications;
    }

    public void removeSession(int index)
    {
        if (index >= 0 && index < chatSessions.size())
        {
            chatSessions.remove(index);
        }
    }

    public HiscoreSnapshot getLastSnapshot(){
        return lastSnapshot;
    }

    public void setLastSnapshot(HiscoreSnapshot snapshot){
        lastSnapshot = snapshot;
    }

    public String getPlayerName(){
        return playerName;
    }

    public List<ChatSession> getChatSessions(){
        return chatSessions;
    }

    public ProgressionData getProgression(){
        return progression;
    }
}