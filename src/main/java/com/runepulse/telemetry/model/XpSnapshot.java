package com.runepulse.telemetry.model;

import java.util.Map;

public class XpSnapshot
{
    private final String username;
    private final long timestamp;
    private final Map<String, Integer> xpGained;

    public XpSnapshot(String username, long timestamp, Map<String, Integer> xpGained)
    {
        this.username = username;
        this.timestamp = timestamp;
        this.xpGained = xpGained;
    }
}


