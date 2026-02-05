package com.runepulse.telemetry;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.ChatMessage;

@PluginDescriptor(
    name = "RunePulse",
    description = "RuneLite telemetry for XP, gear, and boss KC"
)
public class TelemetryPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private TelemetryService service;

    @Inject
    private ConfigManager configManager;

    @Override
    protected void startUp()
    {
        service.syncPaired(configManager);
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        service.recordXpChange(event.getSkill(), event.getXp());
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        service.tick(client);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            service.reset();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        service.onChatMessage(event);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!"runepulse".equals(event.getGroup()))
        {
            return;
        }

        if ("pairNow".equals(event.getKey()) && configManager.getConfig(TelemetryConfig.class).pairNow())
        {
            service.pair(configManager);
            configManager.setConfiguration("runepulse", "pairNow", false);
        }

        if ("publicProfile".equals(event.getKey()))
        {
            service.updateVisibility(configManager.getConfig(TelemetryConfig.class).publicProfile());
        }

        if ("paired".equals(event.getKey()))
        {
            service.syncPaired(configManager);
        }
    }

    @Provides
    TelemetryConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TelemetryConfig.class);
    }
}

