package com.clanstatstracker;

import lombok.Data;

@Data
public class SkillProgress
{
    private int levelsGained;
    private long experienceGained;

    public SkillProgress(int levelsGained, long experienceGained)
    {
        this.levelsGained = levelsGained;
        this.experienceGained = experienceGained;
    }

    public boolean hasProgression()
    {
        return levelsGained > 0 || experienceGained > 0;
    }

    public int getLevelsGained(){
        return levelsGained;
    }

    public long getExperienceGained(){
        return experienceGained;
    }
}