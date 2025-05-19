package me.help.betterlocalchat.handlers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.help.betterlocalchat.BetterLocalChat;
import me.help.betterlocalchat.handlers.ChatModes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ChatHandler implements Listener {

    private final BetterLocalChat plugin;
    private final HashMap<UUID, ChatModes> chatModes = new HashMap<>();
    private final HashMap<UUID, Boolean> globalChatVisibility = new HashMap<>();
    private final HashMap<UUID, Boolean> canSeeAllLocalChats = new HashMap<>();
    private int localChatRadius;
    private String localChatPrefix;
    private String globalChatPrefix;
    private String staffChatPrefix;
    private boolean handleGlobalChatPrefix;
    private boolean returnToGlobalOnLeaveStaffChat;
    private List<String> disabledLocalChatWorlds;
    private List<String> disabledGlobalChatWorlds;
    private String developerChatPrefix;

    private String GlobalPlusChatPrefix;

    public ChatHandler(BetterLocalChat plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        localChatRadius = plugin.getConfig().getInt("local-chat-radius", 100);
        localChatPrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("local-chat-prefix", "&7[Local]&r "));
        globalChatPrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("global-chat-prefix", "&7[Global]&r "));
        staffChatPrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("staff-chat-prefix", "&c[Staff]&r "));
        GlobalPlusChatPrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("globalplus-chat-prefix", "&c[GeneralPlus]&r "));
        handleGlobalChatPrefix = plugin.getConfig().getBoolean("handle-global-chat-prefix", false);
        developerChatPrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("developer-chat-prefix", "&b[Dev]&r "));
        returnToGlobalOnLeaveStaffChat = plugin.getConfig().getBoolean("return-to-global-on-leave-staffchat", true);
        disabledLocalChatWorlds = plugin.getConfig().getStringList("disabled-local-chat-worlds");
        disabledGlobalChatWorlds = plugin.getConfig().getStringList("disabled-global-chat-worlds");
    }


    private String translateHexColors(String message) {
        // Pattern to match hex colors in the format &#RRGGBB
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);

        // Replace all hex color codes with the corresponding Minecraft color format
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexColor = matcher.group(1); // Extract the hex color (e.g., FFFFFF)

            // Convert hex color to the Minecraft §x format
            StringBuilder colorCode = new StringBuilder("§x");
            for (char c : hexColor.toCharArray()) {
                colorCode.append("§").append(c); // Append each character prefixed by §
            }

            matcher.appendReplacement(buffer, colorCode.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public ChatModes getChatMode(Player player) {
        return chatModes.getOrDefault(player.getUniqueId(), ChatModes.LOCAL);
    }

    public boolean isInDeveloperChat(Player player) {
        return getChatMode(player) == ChatModes.DEVELOPER;
    }
    public boolean isInGeneralPlusChat(Player player) {
        return getChatMode(player) == ChatModes.GLOBALPLUS;
    }

    public void setChatMode(Player player, ChatModes mode) {
        chatModes.put(player.getUniqueId(), mode);
    }




    public boolean canSeeAllLocalChats(Player player) {
        return canSeeAllLocalChats.getOrDefault(player.getUniqueId(), false);
    }

    public boolean isReturnToGlobalOnLeaveStaffChat() {
        return returnToGlobalOnLeaveStaffChat;
    }
    public boolean isInGlobalChat(Player player) {
        return getChatMode(player) == ChatModes.GLOBAL;
    }

    public boolean isInLocalChat(Player player) {
        return getChatMode(player) == ChatModes.LOCAL;
    }

    public boolean isInStaffChat(Player player) {
        return getChatMode(player) == ChatModes.STAFF;
    }


    public void toggleGlobalChatVisibility(Player player) {
        boolean isVisible = globalChatVisibility.getOrDefault(player.getUniqueId(), true);
        globalChatVisibility.put(player.getUniqueId(), !isVisible);
        player.sendMessage(ChatColor.BLUE + "Global chat visibility " +
                (!isVisible ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
    }

    public boolean isHandlingGlobalPrefix() {
        return handleGlobalChatPrefix;
    }

    public void setHandleGlobalChatPrefix(boolean handle) {
        handleGlobalChatPrefix = handle;
        plugin.getConfig().set("handle-global-chat-prefix", handle);
        plugin.saveConfig();
    }

    public void toggleSeeAllLocalChats(Player player) {
        boolean canSee = canSeeAllLocalChats.getOrDefault(player.getUniqueId(), false);
        canSeeAllLocalChats.put(player.getUniqueId(), !canSee);
        player.sendMessage((!canSee ? ChatColor.GREEN : ChatColor.RED) + "You can now " +
                (!canSee ? "see" : "no longer see") + " all local chats.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        World playerWorld = player.getWorld();

        // ✅ Apply PlaceholderAPI to the message
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        // ✅ Translate hex colors in the message
        message = translateHexColors(message);

        // ✅ Translate regular color codes
        message = ChatColor.translateAlternateColorCodes('&', message);

        // ✅ Apply PlaceholderAPI and hex translation to chat prefixes
        localChatPrefix = PlaceholderAPI.setPlaceholders(player, localChatPrefix);
        globalChatPrefix = PlaceholderAPI.setPlaceholders(player, globalChatPrefix);
        staffChatPrefix = PlaceholderAPI.setPlaceholders(player, staffChatPrefix);
        developerChatPrefix = PlaceholderAPI.setPlaceholders(player, developerChatPrefix);
        GlobalPlusChatPrefix = PlaceholderAPI.setPlaceholders(player, GlobalPlusChatPrefix);

        // Ensure prefixes are also translated for hex colors
        localChatPrefix = translateHexColors(localChatPrefix);
        globalChatPrefix = translateHexColors(globalChatPrefix);
        staffChatPrefix = translateHexColors(staffChatPrefix);
        developerChatPrefix = translateHexColors(developerChatPrefix);
        GlobalPlusChatPrefix = translateHexColors(GlobalPlusChatPrefix);

        localChatPrefix = ChatColor.translateAlternateColorCodes('&', localChatPrefix);
        globalChatPrefix = ChatColor.translateAlternateColorCodes('&', globalChatPrefix);
        staffChatPrefix = ChatColor.translateAlternateColorCodes('&', staffChatPrefix);
        developerChatPrefix = ChatColor.translateAlternateColorCodes('&', developerChatPrefix);
        GlobalPlusChatPrefix = ChatColor.translateAlternateColorCodes('&', GlobalPlusChatPrefix);

        // 🔹 Staff Chat Handling
        if (getChatMode(player) == ChatModes.STAFF) {
            event.setCancelled(true);
            String staffMessage = staffChatPrefix + ChatColor.WHITE + player.getName() + ": " + message;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("chat.staff")) {
                    onlinePlayer.sendMessage(staffMessage);
                }
            }
            return;
        }

        // 🔹 Global Chat Handling
        if (getChatMode(player) == ChatModes.GLOBAL) {
            if (handleGlobalChatPrefix) {
                event.setFormat(globalChatPrefix + ChatColor.WHITE + "%s: %s");
            }
            event.getRecipients().removeIf(recipient -> !globalChatVisibility.getOrDefault(recipient.getUniqueId(), true));
            return;
        }

        // 🔹 Local Chat Handling
        if (disabledLocalChatWorlds.contains(playerWorld.getName())) {
            player.sendMessage(ChatColor.RED + "Local chat is disabled in this world.");
            event.setCancelled(true);
            return;
        }
        // 🔹 Developer Chat Handling
        if (getChatMode(player) == ChatModes.DEVELOPER) {
            event.setCancelled(true);

            // Ensure the developer chat prefix is translated correctly
            String devChatPrefixFormatted = PlaceholderAPI.setPlaceholders(player, developerChatPrefix);
            devChatPrefixFormatted = translateHexColors(devChatPrefixFormatted);
            devChatPrefixFormatted = ChatColor.translateAlternateColorCodes('&', devChatPrefixFormatted);

            String devMessage = devChatPrefixFormatted + ChatColor.WHITE + player.getName() + ": " + message;

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("chat.developer")) { // Only send to devs
                    onlinePlayer.sendMessage(devMessage);
                }
            }
            return;
        }

        // 🔹 GeneralPlus Chat Handling
        if (getChatMode(player) == ChatModes.GLOBALPLUS) {
            event.setCancelled(true);

            // Ensure the paid chat prefix is translated correctly
            String globalChatPrefixFormatted = PlaceholderAPI.setPlaceholders(player, GlobalPlusChatPrefix);
            globalChatPrefixFormatted = translateHexColors(globalChatPrefixFormatted);
            globalChatPrefixFormatted = ChatColor.translateAlternateColorCodes('&', globalChatPrefixFormatted);

            String paidMessage = globalChatPrefixFormatted + ChatColor.WHITE + player.getName() + ": " + message;

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("chat.paid")) { // Only send to paid users
                    onlinePlayer.sendMessage(paidMessage);
                }
            }
            return;
        }

        event.setCancelled(true);
        Location playerLoc = player.getLocation();
        String formattedMessage = localChatPrefix + ChatColor.WHITE + player.getName() + ": " + message;

        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (canSeeAllLocalChats(nearbyPlayer) && nearbyPlayer.hasPermission("chat.seealllocal")) {
                nearbyPlayer.sendMessage(formattedMessage);
            } else if (nearbyPlayer.getWorld().equals(playerWorld) &&
                    nearbyPlayer.getLocation().distance(playerLoc) <= localChatRadius) {
                nearbyPlayer.sendMessage(formattedMessage);
            }

        }

    }
}