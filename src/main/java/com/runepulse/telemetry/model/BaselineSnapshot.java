package com.runepulse.telemetry.model;

import java.util.Map;

public class BaselineSnapshot
{
    private final String username;
    private final long timestamp;
    private final Map<String, Integer> skills;

    public BaselineSnapshot(String username, long timestamp, Map<String, Integer> skills)
    {
        this.username = username;
        this.timestamp = timestamp;
        this.skills = skills;
    }
}


