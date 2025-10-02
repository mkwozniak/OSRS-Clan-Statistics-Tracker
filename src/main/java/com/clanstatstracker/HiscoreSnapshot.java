package com.clanstatstracker;

import lombok.Data;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

import java.util.HashMap;
import java.util.Map;

@Data
public class HiscoreSnapshot
{
    private Map<String, SkillData> skills;
    private Map<String, Integer> bossKills;

    public HiscoreSnapshot()
    {
        skills = new HashMap<>();
        bossKills = new HashMap<>();
    }

    public HiscoreSnapshot(HiscoreResult result)
    {
        this();

        // Store skill data
        for (HiscoreSkill hiscoreSkill : HiscoreSkill.values())
        {
            if (hiscoreSkill.getType() == HiscoreSkillType.SKILL)
            {
                net.runelite.client.hiscore.Skill skillData = result.getSkill(hiscoreSkill);
                if (skillData != null)
                {
                    String skillName = hiscoreSkill.getName();
                    skills.put(skillName, new SkillData(
                            skillData.getLevel(),
                            skillData.getExperience()
                    ));
                }
            }
            else if (hiscoreSkill.getType() == HiscoreSkillType.BOSS)
            {
                net.runelite.client.hiscore.Skill bossData = result.getSkill(hiscoreSkill);
                if (bossData != null && bossData.getRank() != -1)
                {
                    String bossName = hiscoreSkill.getName();
                    bossKills.put(bossName, bossData.getLevel());
                }
            }
        }
    }

    public SkillData getSkillData(String skillName)
    {
        return skills.get(skillName);
    }

    public Integer getBossKills(String bossName)
    {
        return bossKills.getOrDefault(bossName, 0);
    }

    public Map<String, Integer> getBossKillsData(){
        return bossKills;
    }

    @Data
    public static class SkillData
    {
        private int level;
        private long experience;

        public SkillData(int level, long experience)
        {
            this.level = level;
            this.experience = experience;
        }

        public int getLevel(){
            return level;
        }

        public long getExperience(){
            return experience;
        }
    }
}