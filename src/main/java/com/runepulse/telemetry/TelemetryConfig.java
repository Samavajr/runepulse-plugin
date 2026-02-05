package com.runepulse.telemetry;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("runepulse")
public interface TelemetryConfig extends Config
{
    @ConfigItem(
        keyName = "paired",
        name = "Paired",
        description = "Shows whether this client is paired"
    )
    default boolean paired()
    {
        return false;
    }

    @ConfigItem(
        keyName = "pairNow",
        name = "Pair Now",
        description = "Generate and store an API token automatically"
    )
    default boolean pairNow()
    {
        return false;
    }

    @ConfigItem(
        keyName = "trackXp",
        name = "Track XP",
        description = "Send XP baselines and XP gains"
    )
    default boolean trackXp()
    {
        return true;
    }

    @ConfigItem(
        keyName = "trackGear",
        name = "Track Gear",
        description = "Send equipped gear snapshots"
    )
    default boolean trackGear()
    {
        return false;
    }

    @ConfigItem(
        keyName = "trackBossKc",
        name = "Track Boss KC",
        description = "Send boss kill counts"
    )
    default boolean trackBossKc()
    {
        return false;
    }

    @ConfigItem(
        keyName = "publicProfile",
        name = "Public Profile",
        description = "Allow your profile to be publicly viewable"
    )
    default boolean publicProfile()
    {
        return true;
    }
}

