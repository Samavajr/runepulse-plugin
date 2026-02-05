package com.runepulse.telemetry.model;

import java.util.Map;

public class GearSnapshot
{
    private final String username;
    private final long timestamp;
    private final Map<String, GearItem> slots;

    public GearSnapshot(String username, long timestamp, Map<String, GearItem> slots)
    {
        this.username = username;
        this.timestamp = timestamp;
        this.slots = slots;
    }
}

