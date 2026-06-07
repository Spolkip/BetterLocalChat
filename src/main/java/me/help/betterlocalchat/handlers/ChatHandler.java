package me.help.betterlocalchat.handlers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.help.betterlocalchat.BetterLocalChat;
import net.md_5.bungee.api.chat.ClickEvent;

import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

public class ChatHandler implements Listener {

    private final BetterLocalChat plugin;
    private final HashMap<UUID, ChatModes> chatModes = new HashMap<>();
    private final HashMap<UUID, Boolean> globalChatVisibility = new HashMap<>();
    private final HashMap<UUID, Boolean> canSeeAllLocalChats = new HashMap<>();
    private final HashMap<UUID, Boolean> adminSpyActive = new HashMap<>();
    private final HashMap<UUID, Long> globalCooldowns = new HashMap<>();

    private int localChatRadius;
    private boolean handleGlobalChatPrefix;
    private boolean returnToGlobalOnLeaveStaffChat;
    private List<String> disabledLocalChatWorlds;
    private List<String> disabledGlobalChatWorlds;

    // Configurable extras
    private int globalCooldownSeconds;
    private String hoverTextTemplate;
    private String clickCommandTemplate;
    private Sound mentionSound;

    // Quick Prefixes & Discord Webhooks
    private final NamespacedKey soundKey;
    private String prefixGlobal, prefixStaff, prefixDev, prefixGlobalPlus;
    private String webhookGlobal, webhookStaff, webhookDev, webhookGlobalPlus;

    // Raw templates for dynamic PAPI checks
    private String localTemplate, globalTemplate, staffTemplate, devTemplate, globalPlusTemplate;

    // High-speed cached prefixes (if no PAPI placeholders are used)
    private String cachedLocal, cachedGlobal, cachedStaff, cachedDev, cachedGlobalPlus;

    public ChatHandler(BetterLocalChat plugin) {
        this.plugin = plugin;
        this.soundKey = new NamespacedKey(plugin, "mention_sound");
    }

    public void loadConfig() {
        localChatRadius = plugin.getConfig().getInt("local-chat-radius", 100);
        handleGlobalChatPrefix = plugin.getConfig().getBoolean("handle-global-chat-prefix", false);
        returnToGlobalOnLeaveStaffChat = plugin.getConfig().getBoolean("return-to-global-on-leave-staffchat", true);
        disabledLocalChatWorlds = plugin.getConfig().getStringList("disabled-local-chat-worlds");
        disabledGlobalChatWorlds = plugin.getConfig().getStringList("disabled-global-chat-worlds");

        globalCooldownSeconds = plugin.getConfig().getInt("global-chat-cooldown", 3);
        hoverTextTemplate = translateAllColors(plugin.getConfig().getString("hover-text", "&6Click to message %player%!"));
        clickCommandTemplate = plugin.getConfig().getString("click-command", "/msg %player% ");

        prefixGlobal = plugin.getConfig().getString("quick-prefixes.global", "!");
        prefixStaff = plugin.getConfig().getString("quick-prefixes.staff", "@");
        prefixDev = plugin.getConfig().getString("quick-prefixes.developer", "#");
        prefixGlobalPlus = plugin.getConfig().getString("quick-prefixes.globalplus", "$");

        webhookGlobal = plugin.getConfig().getString("discord-webhooks.global", "");
        webhookStaff = plugin.getConfig().getString("discord-webhooks.staff", "");
        webhookDev = plugin.getConfig().getString("discord-webhooks.developer", "");
        webhookGlobalPlus = plugin.getConfig().getString("discord-webhooks.globalplus", "");

        try {
            mentionSound = Sound.valueOf(plugin.getConfig().getString("mention-sound", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        } catch (IllegalArgumentException e) {
            mentionSound = null;
        }

        localTemplate = plugin.getConfig().getString("local-chat-prefix", "&7[Local]&r ");
        globalTemplate = plugin.getConfig().getString("global-chat-prefix", "&7[Global]&r ");
        staffTemplate = plugin.getConfig().getString("staff-chat-prefix", "&c[Staff]&r ");
        devTemplate = plugin.getConfig().getString("developer-chat-prefix", "&b[Dev]&r ");
        globalPlusTemplate = plugin.getConfig().getString("globalplus-chat-prefix", "&b[GlobalPlus]&r ");

        cachedLocal = localTemplate.contains("%") ? null : translateAllColors(localTemplate);
        cachedGlobal = globalTemplate.contains("%") ? null : translateAllColors(globalTemplate);
        cachedStaff = staffTemplate.contains("%") ? null : translateAllColors(staffTemplate);
        cachedDev = devTemplate.contains("%") ? null : translateAllColors(devTemplate);
        cachedGlobalPlus = globalPlusTemplate.contains("%") ? null : translateAllColors(globalPlusTemplate);
    }

    private String translateAllColors(String message) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            StringBuilder colorCode = new StringBuilder("§x");
            for (char c : matcher.group(1).toCharArray()) {
                colorCode.append("§").append(c);
            }
            matcher.appendReplacement(buffer, colorCode.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private String getFormattedPrefix(Player player, String template, String cached) {
        if (cached != null) return cached;
        String formatted = template;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }
        return translateAllColors(formatted);
    }

    // --- State Management ---
    public ChatModes getChatMode(Player player) { return chatModes.getOrDefault(player.getUniqueId(), ChatModes.LOCAL); }
    public void setChatMode(Player player, ChatModes mode) { chatModes.put(player.getUniqueId(), mode); }
    public boolean isReturnToGlobalOnLeaveStaffChat() { return returnToGlobalOnLeaveStaffChat; }
    public boolean isHandlingGlobalPrefix() { return handleGlobalChatPrefix; }
    public void setHandleGlobalChatPrefix(boolean handle) {
        handleGlobalChatPrefix = handle;
        plugin.getConfig().set("handle-global-chat-prefix", handle);
        plugin.saveConfig();
    }

    // --- Toggles ---
    public void toggleGlobalChatVisibility(Player player) {
        boolean isVisible = globalChatVisibility.getOrDefault(player.getUniqueId(), true);
        globalChatVisibility.put(player.getUniqueId(), !isVisible);
        player.sendMessage(ChatColor.BLUE + "Global chat visibility " + (!isVisible ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
    }

    public void toggleSeeAllLocalChats(Player player) {
        boolean canSee = canSeeAllLocalChats.getOrDefault(player.getUniqueId(), false);
        canSeeAllLocalChats.put(player.getUniqueId(), !canSee);
        player.sendMessage((!canSee ? ChatColor.GREEN : ChatColor.RED) + "You can now " + (!canSee ? "see" : "no longer see") + " all local chats.");
    }

    public void toggleAdminSpy(Player player) {
        boolean active = adminSpyActive.getOrDefault(player.getUniqueId(), false);
        adminSpyActive.put(player.getUniqueId(), !active);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Social Spy mode has been " + (!active ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
    }

    private boolean hasAdminSpy(Player player) {
        return adminSpyActive.getOrDefault(player.getUniqueId(), false) && player.hasPermission("chat.spy");
    }

    // --- Async File Logger ---
    private void logToChannelFile(String channel, String sender, String message) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) dataFolder.mkdirs();
                File logDir = new File(dataFolder, "logs");
                if (!logDir.exists()) logDir.mkdirs();
                File logFile = new File(logDir, channel.toLowerCase() + ".log");
                try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                    String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    writer.println("[" + time + "] " + sender + ": " + message);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not write to log: " + e.getMessage());
            }
        });
    }

    // --- Discord Webhook Sender ---
    private void sendDiscordWebhook(String webhookUrl, String sender, String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "BetterLocalChat");
                connection.setDoOutput(true);

                String cleanMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
                String jsonPayload = "{\"username\": \"" + sender + "\", \"content\": \"" + cleanMessage + "\"}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                connection.getInputStream().close();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord Webhook: " + e.getMessage());
            }
        });
    }

    // --- Interactive Chat Builder (With Proximity Fading) ---
    private void dispatchInteractiveMessage(Player sender, String prefix, String rawMsg, Collection<? extends Player> recipients, boolean isLocal) {
        TextComponent prefixComp = new TextComponent(prefix);

        TextComponent nameComp = new TextComponent(sender.getName());
        String hoverText = hoverTextTemplate.replace("%player%", sender.getName());
        String clickCmd = clickCommandTemplate.replace("%player%", sender.getName());

        nameComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(hoverText)));
        nameComp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, clickCmd));

        TextComponent separator = new TextComponent(": ");
        separator.setColor(net.md_5.bungee.api.ChatColor.WHITE);

        for (Player recipient : recipients) {
            String distanceColorMsg = rawMsg;

            if (isLocal && sender.getWorld().equals(recipient.getWorld())) {
                double distance = sender.getLocation().distance(recipient.getLocation());
                double percentage = distance / localChatRadius;

                if (percentage <= 0.33) {
                    distanceColorMsg = "§f" + rawMsg;
                } else if (percentage <= 0.66) {
                    distanceColorMsg = "§7" + rawMsg;
                } else {
                    distanceColorMsg = "§8" + rawMsg;
                }
            } else if (isLocal) {
                distanceColorMsg = "§7" + rawMsg;
            }

            TextComponent messageComp = new TextComponent(distanceColorMsg);

            TextComponent masterLayout = new TextComponent();
            masterLayout.addExtra(prefixComp);
            masterLayout.addExtra(nameComp);
            masterLayout.addExtra(separator);
            masterLayout.addExtra(messageComp);

            recipient.spigot().sendMessage(masterLayout);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        UUID uuid = player.getUniqueId();

        ChatModes activeMode = getChatMode(player);

        // 1. Quick Channel Prefixes
        if (prefixGlobal != null && !prefixGlobal.isEmpty() && message.startsWith(prefixGlobal) && message.length() > prefixGlobal.length()) {
            activeMode = ChatModes.GLOBAL;
            message = message.substring(prefixGlobal.length()).trim();
        } else if (prefixStaff != null && !prefixStaff.isEmpty() && message.startsWith(prefixStaff) && message.length() > prefixStaff.length()) {
            activeMode = ChatModes.STAFF;
            message = message.substring(prefixStaff.length()).trim();
        } else if (prefixDev != null && !prefixDev.isEmpty() && message.startsWith(prefixDev) && message.length() > prefixDev.length()) {
            activeMode = ChatModes.DEVELOPER;
            message = message.substring(prefixDev.length()).trim();
        } else if (prefixGlobalPlus != null && !prefixGlobalPlus.isEmpty() && message.startsWith(prefixGlobalPlus) && message.length() > prefixGlobalPlus.length()) {
            activeMode = ChatModes.GLOBALPLUS;
            message = message.substring(prefixGlobalPlus.length()).trim();
        }

        // Apply Message Translation
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        message = translateAllColors(message);
        String discordCleanMessage = ChatColor.stripColor(message);

        // Permissions Check
        if (activeMode == ChatModes.LOCAL && !player.hasPermission("chat.local")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use local chat.");
            event.setCancelled(true);
            return;
        }
        if (activeMode == ChatModes.GLOBAL && !player.hasPermission("chat.global")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use global chat.");
            event.setCancelled(true);
            return;
        }

        // Cooldown Check
        if (activeMode == ChatModes.GLOBAL && !player.hasPermission("chat.cooldown.bypass")) {
            long now = System.currentTimeMillis();
            if (globalCooldowns.containsKey(uuid) && globalCooldowns.get(uuid) > now) {
                long timeLeft = (globalCooldowns.get(uuid) - now) / 1000;
                player.sendMessage(ChatColor.RED + "Please wait " + timeLeft + "s before chatting globally!");
                event.setCancelled(true);
                return;
            }
            globalCooldowns.put(uuid, now + (globalCooldownSeconds * 1000L));
        }

        // Custom Mention System
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (message.toLowerCase().contains(onlinePlayer.getName().toLowerCase())) {
                Sound soundToPlay = mentionSound;

                String customSound = onlinePlayer.getPersistentDataContainer().get(soundKey, PersistentDataType.STRING);
                if (customSound != null) {
                    try {
                        soundToPlay = Sound.valueOf(customSound);
                    } catch (IllegalArgumentException ignored) {}
                }

                if (soundToPlay != null) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), soundToPlay,
                            (float) plugin.getConfig().getDouble("mention-volume", 1.0),
                            (float) plugin.getConfig().getDouble("mention-pitch", 1.0));
                }
            }
        }

        event.setCancelled(true);
        Set<Player> recipients = new HashSet<>();
        String prefix = "";

        // Route Message
        switch (activeMode) {
            case STAFF:
                prefix = getFormattedPrefix(player, staffTemplate, cachedStaff);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("chat.staff") || hasAdminSpy(p)) recipients.add(p);
                }
                logToChannelFile("staff", player.getName(), message);
                sendDiscordWebhook(webhookStaff, player.getName(), discordCleanMessage);
                break;

            case DEVELOPER:
                prefix = getFormattedPrefix(player, devTemplate, cachedDev);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("chat.developer") || hasAdminSpy(p)) recipients.add(p);
                }
                logToChannelFile("developer", player.getName(), message);
                sendDiscordWebhook(webhookDev, player.getName(), discordCleanMessage);
                break;

            case GLOBALPLUS:
                prefix = getFormattedPrefix(player, globalPlusTemplate, cachedGlobalPlus);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("chat.paid") || hasAdminSpy(p)) recipients.add(p);
                }
                logToChannelFile("globalplus", player.getName(), message);
                sendDiscordWebhook(webhookGlobalPlus, player.getName(), discordCleanMessage);
                break;

            case GLOBAL:
                if (disabledGlobalChatWorlds.contains(player.getWorld().getName())) {
                    player.sendMessage(ChatColor.RED + "Global chat is disabled in this world.");
                    return;
                }
                prefix = getFormattedPrefix(player, globalTemplate, cachedGlobal);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (globalChatVisibility.getOrDefault(p.getUniqueId(), true)) recipients.add(p);
                }
                logToChannelFile("global", player.getName(), message);
                sendDiscordWebhook(webhookGlobal, player.getName(), discordCleanMessage);
                break;

            case LOCAL:
                if (disabledLocalChatWorlds.contains(player.getWorld().getName())) {
                    player.sendMessage(ChatColor.RED + "Local chat is disabled in this world.");
                    return;
                }
                prefix = getFormattedPrefix(player, localTemplate, cachedLocal);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("chat.seealllocal") && canSeeAllLocalChats.getOrDefault(p.getUniqueId(), false)) {
                        recipients.add(p);
                    } else if (hasAdminSpy(p)) {
                        recipients.add(p);
                    } else if (p.getWorld().equals(player.getWorld()) && p.getLocation().distance(player.getLocation()) <= localChatRadius) {
                        recipients.add(p);
                    }
                }
                logToChannelFile("local", player.getName(), message);
                break;
        }

        dispatchInteractiveMessage(player, prefix, message, recipients, activeMode == ChatModes.LOCAL);
    }
}