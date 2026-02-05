package com.runepulse.telemetry.model;

import java.util.Map;

public class BossKcSnapshot
{
    private final String username;
    private final long timestamp;
    private final Map<String, Integer> bossKc;

    public BossKcSnapshot(String username, long timestamp, Map<String, Integer> bossKc)
    {
        this.username = username;
        this.timestamp = timestamp;
        this.bossKc = bossKc;
    }
}

