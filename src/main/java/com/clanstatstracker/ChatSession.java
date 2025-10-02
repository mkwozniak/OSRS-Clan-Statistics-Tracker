package com.clanstatstracker;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatSession
{
    private String date;
    private int messageCount;
    private List<String> notifications;

    public ChatSession(String date, int messageCount, List<String> notifs)
    {
        this.date = date;
        this.messageCount = messageCount;
        this.notifications = notifs;
    }

    public String getDate(){
        return date;
    }

    public int getMessageCount(){
        return messageCount;
    }

    public List<String> getNotifications(){
        return notifications;
    }
}
