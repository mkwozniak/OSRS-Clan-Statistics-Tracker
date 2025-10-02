package com.clanstatstracker;

import lombok.Data;
import net.runelite.api.Skill;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
public class ProgressionData
{
    private Map<String, SkillProgress> skillProgress;
    private Map<String, Integer> bossKillsGained;

    public ProgressionData()
    {
        skillProgress = new HashMap<>();
        bossKillsGained = new HashMap<>();
    }

    public ProgressionData(HiscoreSnapshot oldSnapshot, HiscoreSnapshot newSnapshot)
    {
        this();

        // Calculate skill progression
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }

            String skillName = skill.getName();
            HiscoreSnapshot.SkillData oldData = oldSnapshot.getSkillData(skillName);
            HiscoreSnapshot.SkillData newData = newSnapshot.getSkillData(skillName);

            if (oldData != null && newData != null)
            {
                int levelDiff = newData.getLevel() - oldData.getLevel();
                long xpDiff = newData.getExperience() - oldData.getExperience();

                if (levelDiff > 0 || xpDiff > 0)
                {
                    skillProgress.put(skillName, new SkillProgress(levelDiff, xpDiff));
                }
            }
        }

        // Calculate boss kill progression
        Set<String> allBosses = newSnapshot.getBossKillsData().keySet();
        for (String bossName : allBosses)
        {
            int oldKills = oldSnapshot.getBossKills(bossName);
            int newKills = newSnapshot.getBossKills(bossName);

            int killsGained = newKills - oldKills;
            if (killsGained > 0)
            {
                bossKillsGained.put(bossName, killsGained);
            }
        }
    }

    public boolean hasAnyProgression()
    {
        return !skillProgress.isEmpty() || !bossKillsGained.isEmpty();
    }

    public boolean hasSkillProgression()
    {
        return !skillProgress.isEmpty();
    }

    public boolean hasBossProgression()
    {
        return !bossKillsGained.isEmpty();
    }

    public SkillProgress getSkillProgress(Skill skill)
    {
        return skillProgress.get(skill.getName());
    }

    public Set<String> getBossNames()
    {
        return bossKillsGained.keySet();
    }

    public int getBossKillsGained(String bossName)
    {
        return bossKillsGained.getOrDefault(bossName, 0);
    }
}
