package com.runepulse.telemetry;


import com.runepulse.telemetry.model.BaselineSnapshot;
import com.runepulse.telemetry.model.BossKcSnapshot;
import com.runepulse.telemetry.model.GearItem;
import com.runepulse.telemetry.model.GearSnapshot;
import com.runepulse.telemetry.model.XpSnapshot;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

public class TelemetryService
{
    private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
    private final Map<Skill, Integer> buffer = new EnumMap<>(Skill.class);
    private final Map<String, Integer> bossKc = new HashMap<>();
    private boolean bossKcDirty = false;
    private Instant lastSend = Instant.EPOCH;
    private boolean baselineSent = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Pattern BOSSKILL_MESSAGE_PATTERN =
        Pattern.compile("Your (.+) (?:kill|success) count is: ([0-9,]+)");

    @Inject
    private TelemetryUploader uploader;

    @Inject
    private TelemetryConfig config;

    @Inject
    private ItemManager itemManager;
    public void recordXpChange(Skill skill, int currentXp)
    {
        if (!config.trackXp())
        {
            return;
        }

        int previous = lastXp.getOrDefault(skill, currentXp);
        int delta = currentXp - previous;

        if (delta > 0)
        {
            buffer.merge(skill, delta, Integer::sum);
        }

        lastXp.put(skill, currentXp);
    }

    public void tick(Client client)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (client.getLocalPlayer() == null)
        {
            return;
        }

        if (!baselineSent && config.trackXp())
        {
            sendBaseline(client);
            baselineSent = true;
        }

        if (Instant.now().isBefore(lastSend.plusSeconds(300)))
        {
            return;
        }

        String username = client.getLocalPlayer().getName();
        if (username == null || username.isBlank())
        {
            return;
        }

        if (config.trackXp() && !buffer.isEmpty())
        {
            Map<String, Integer> xpGained = new HashMap<>();
            for (Map.Entry<Skill, Integer> entry : buffer.entrySet())
            {
                xpGained.put(entry.getKey().getName(), entry.getValue());
            }

            uploader.send(new XpSnapshot(
                username,
                Instant.now().getEpochSecond(),
                xpGained
            ));

            buffer.clear();
        }

        if (config.trackGear())
        {
            sendGear(client, username);
        }

        if (config.trackBossKc() && bossKcDirty)
        {
            uploader.sendBossKc(new BossKcSnapshot(
                username,
                Instant.now().getEpochSecond(),
                new HashMap<>(bossKc)
            ));
            bossKcDirty = false;
        }
        lastSend = Instant.now();
    }

    public void reset()
    {
        buffer.clear();
        lastXp.clear();
        lastSend = Instant.EPOCH;
        baselineSent = false;
    }

    public void pair(ConfigManager configManager)
    {
        executor.submit(() ->
        {
            String token = uploader.pair();
            if (token != null && !token.isBlank())
            {
                configManager.setConfiguration("runepulse", "apiToken", token);
                configManager.setConfiguration("runepulse", "paired", true);
            }
        });
    }

    public void syncPaired(ConfigManager configManager)
    {
        String token = configManager.getConfiguration("runepulse", "apiToken");
        boolean paired = token != null && !token.isBlank();
        configManager.setConfiguration("runepulse", "paired", paired);
    }

    public void updateVisibility(boolean isPublic)
    {
        executor.submit(() -> uploader.sendVisibility(isPublic));
    }

    private void sendBaseline(Client client)
    {
        String username = client.getLocalPlayer().getName();
        if (username == null || username.isBlank())
        {
            return;
        }

        Map<String, Integer> skills = new HashMap<>();
        for (Skill skill : Skill.values())
        {
            skills.put(skill.getName(), client.getSkillExperience(skill));
        }

        uploader.sendBaseline(new BaselineSnapshot(
            username,
            Instant.now().getEpochSecond(),
            skills
        ));
    }

    private void sendGear(Client client, String username)
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return;
        }

        Map<String, GearItem> slots = new HashMap<>();
        Map<String, Integer> slotMap = new HashMap<>();
        slotMap.put("HEAD", EquipmentInventorySlot.HEAD.getSlotIdx());
        slotMap.put("CAPE", EquipmentInventorySlot.CAPE.getSlotIdx());
        slotMap.put("AMULET", EquipmentInventorySlot.AMULET.getSlotIdx());
        slotMap.put("WEAPON", EquipmentInventorySlot.WEAPON.getSlotIdx());
        slotMap.put("BODY", EquipmentInventorySlot.BODY.getSlotIdx());
        slotMap.put("SHIELD", EquipmentInventorySlot.SHIELD.getSlotIdx());
        slotMap.put("LEGS", EquipmentInventorySlot.LEGS.getSlotIdx());
        slotMap.put("HANDS", EquipmentInventorySlot.GLOVES.getSlotIdx());
        slotMap.put("FEET", EquipmentInventorySlot.BOOTS.getSlotIdx());
        slotMap.put("RING", EquipmentInventorySlot.RING.getSlotIdx());
        slotMap.put("AMMO", 13);

        Item[] items = equipment.getItems();
        for (Map.Entry<String, Integer> entry : slotMap.entrySet())
        {
            int idx = entry.getValue();
            if (idx < 0 || idx >= items.length)
            {
                continue;
            }

            Item item = items[idx];
            if (item == null || item.getId() <= 0)
            {
                continue;
            }

            String name = itemManager.getItemComposition(item.getId()).getName();
            slots.put(entry.getKey(), new GearItem(item.getId(), name, item.getQuantity()));
        }

        uploader.sendGear(new GearSnapshot(
            username,
            Instant.now().getEpochSecond(),
            slots
        ));
    }

    public void onChatMessage(net.runelite.api.events.ChatMessage event)
    {
        if (!config.trackBossKc())
        {
            return;
        }

        String typeName = event.getType().name();
        if (!"GAMEMESSAGE".equals(typeName) && !"SPAM".equals(typeName))
        {
            return;
        }

        String msg = Text.removeTags(event.getMessage());
        Matcher m = BOSSKILL_MESSAGE_PATTERN.matcher(msg);
        if (m.find())
        {
            String bossName = Text.removeTags(m.group(1));
            String kcStr = m.group(2).replace(",", "");
            try
            {
                int kc = Integer.parseInt(kcStr);
                bossKc.put(bossName, kc);
                bossKcDirty = true;
            }
            catch (NumberFormatException ignored)
            {
            }
        }
    }
}


