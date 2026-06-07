package me.help.betterlocalchat.handlers;

import me.help.betterlocalchat.BetterLocalChat;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class CommandHandler implements CommandExecutor {

    private final BetterLocalChat plugin;
    private final ChatHandler chatHandler;

    public CommandHandler(BetterLocalChat plugin, ChatHandler chatHandler) {
        this.plugin = plugin;
        this.chatHandler = chatHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        String cmd = command.getName().toLowerCase();

        // --- 1. Global Chat ---
        if (cmd.equals("global") || cmd.equals("g")) {
            if (!player.hasPermission("chat.global")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to join global chat.");
                return true;
            }
            if (chatHandler.getChatMode(player) == ChatModes.GLOBAL) {
                player.sendMessage(ChatColor.RED + "You are already in Global Chat.");
                return true;
            }
            chatHandler.setChatMode(player, ChatModes.GLOBAL);
            player.sendMessage(ChatColor.GREEN + "You are now in Global Chat.");
            return true;
        }

        // --- 2. Local Chat ---
        if (cmd.equals("local") || cmd.equals("l")) {
            if (!player.hasPermission("chat.local")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to join local chat.");
                return true;
            }
            if (chatHandler.getChatMode(player) == ChatModes.LOCAL) {
                player.sendMessage(ChatColor.RED + "You are already in Local Chat.");
                return true;
            }
            chatHandler.setChatMode(player, ChatModes.LOCAL);
            player.sendMessage(ChatColor.YELLOW + "You are now in Local Chat.");
            return true;
        }

        // --- 3. Channels List ---
        if (cmd.equals("channels") || cmd.equals("chlist")) {
            player.sendMessage(ChatColor.AQUA + "==== Available Chat Channels ====");

            if (player.hasPermission("chat.local")) {
                player.sendMessage(ChatColor.GOLD + "Local Chat " + ChatColor.GRAY + "(/l) - " + ChatColor.YELLOW + "Talk to nearby players");
            }
            if (player.hasPermission("chat.global")) {
                player.sendMessage(ChatColor.GOLD + "Global Chat " + ChatColor.GRAY + "(/g) - " + ChatColor.YELLOW + "Talk to the whole server");
            }
            if (player.hasPermission("chat.paid")) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "GlobalPlus Chat " + ChatColor.GRAY + "(/gp) - " + ChatColor.YELLOW + "Premium chat channel");
            }
            if (player.hasPermission("chat.staff")) {
                player.sendMessage(ChatColor.RED + "Staff Chat " + ChatColor.GRAY + "(/sc) - " + ChatColor.YELLOW + "Private channel for staff");
            }
            if (player.hasPermission("chat.developer")) {
                player.sendMessage(ChatColor.BLUE + "Developer Chat " + ChatColor.GRAY + "(/dc) - " + ChatColor.YELLOW + "Private channel for developers");
            }

            player.sendMessage(ChatColor.AQUA + "=================================");
            return true;
        }

        // --- 4. Main Plugin Commands ---
        if (cmd.equals("betterlocalchat") || cmd.equals("blc")) {

            // Custom Mention Sound logic
            if (args.length == 2 && args[0].equalsIgnoreCase("mentionsound")) {
                if (player.hasPermission("chat.custommention")) {
                    try {
                        Sound sound = Sound.valueOf(args[1].toUpperCase());
                        NamespacedKey key = new NamespacedKey(plugin, "mention_sound");
                        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, sound.name());

                        player.sendMessage(ChatColor.GREEN + "Your mention sound has been updated to " + sound.name() + "!");
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid sound! Try: ENTITY_EXPERIENCE_ORB_PICKUP");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to set a custom mention sound.");
                }
                return true;
            }

            // Reload Logic
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (player.hasPermission("chat.reload")) {
                    plugin.loadConfig();
                    player.sendMessage(ChatColor.GREEN + "Local chat configuration reloaded!");
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to reload the chat.");
                }
                return true;
            }

            // Help Logic
            if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
                player.sendMessage(ChatColor.AQUA + "==== BetterLocalChat Commands ====");
                player.sendMessage(ChatColor.GOLD + "/channels - " + ChatColor.YELLOW + "List your available channels");
                player.sendMessage(ChatColor.GOLD + "/global (/g) - " + ChatColor.YELLOW + "Switch to global chat");
                player.sendMessage(ChatColor.GOLD + "/local (/l) - " + ChatColor.YELLOW + "Switch to local chat");
                player.sendMessage(ChatColor.GOLD + "/blc mentionsound <sound> - " + ChatColor.YELLOW + "Change your ping sound");
                player.sendMessage(ChatColor.GOLD + "/chatspy - " + ChatColor.YELLOW + "Toggle Admin Spy mode");
                player.sendMessage(ChatColor.GOLD + "/blc reload - " + ChatColor.YELLOW + "Reload config");
                player.sendMessage(ChatColor.AQUA + "=================================");
                return true;
            }

            player.sendMessage(ChatColor.RED + "Usage: /betterlocalchat <reload/help/mentionsound>");
            return true;
        }

        // --- 5. Social Spy ---
        if (cmd.equals("chatspy") || cmd.equals("spy")) {
            if (player.hasPermission("chat.spy")) {
                chatHandler.toggleAdminSpy(player);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use Chat Spy.");
            }
            return true;
        }

        // --- 6. Staff Chat ---
        if (cmd.equals("staffchat") || cmd.equals("sc")) {
            if (!player.hasPermission("chat.staff")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use Staff Chat.");
                return true;
            }
            handleToggle(player, ChatModes.STAFF, ChatColor.RED + "You are now in Staff Chat.");
            return true;
        }

        // --- 7. Developer Chat ---
        if (cmd.equals("devchat") || cmd.equals("dc")) {
            if (!player.hasPermission("chat.developer")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use Developer Chat.");
                return true;
            }
            handleToggle(player, ChatModes.DEVELOPER, ChatColor.AQUA + "You are now in Developer Chat.");
            return true;
        }

        // --- 8. GlobalPlus Chat ---
        if (cmd.equals("globalplus") || cmd.equals("gp")) {
            if (!player.hasPermission("chat.paid")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use GlobalPlus Chat.");
                return true;
            }
            handleToggle(player, ChatModes.GLOBALPLUS, ChatColor.LIGHT_PURPLE + "You are now in GlobalPlus Chat.");
            return true;
        }

        // --- 9. Toggle Visibility / View Modes ---
        if (cmd.equals("toggleglobalchat") || cmd.equals("tgc")) {
            if (player.hasPermission("chat.togglevisibility")) {
                chatHandler.toggleGlobalChatVisibility(player);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to toggle global chat visibility.");
            }
            return true;
        }

        if (cmd.equals("toggleglobalprefix") || cmd.equals("tgp")) {
            if (player.hasPermission("chat.toggleglobalprefix")) {
                boolean currentlyHandling = chatHandler.isHandlingGlobalPrefix();
                chatHandler.setHandleGlobalChatPrefix(!currentlyHandling);
                player.sendMessage(ChatColor.BLUE + "Global chat prefix handling is now " +
                        (currentlyHandling ? ChatColor.RED + "disabled" : ChatColor.GREEN + "enabled") + ".");
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to toggle global chat prefix.");
            }
            return true;
        }

        if (cmd.equals("togglelocalview")) {
            if (player.hasPermission("chat.seealllocal")) {
                chatHandler.toggleSeeAllLocalChats(player);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to see all local chats.");
            }
            return true;
        }

        return false;
    }

    private void handleToggle(Player player, ChatModes targetMode, String joinMessage) {
        if (chatHandler.getChatMode(player) == targetMode) {
            ChatModes revertTo = chatHandler.isReturnToGlobalOnLeaveStaffChat() ? ChatModes.GLOBAL : ChatModes.LOCAL;
            chatHandler.setChatMode(player, revertTo);
            player.sendMessage(ChatColor.GREEN + "You left the channel and returned to " + revertTo.name() + " chat.");
        } else {
            chatHandler.setChatMode(player, targetMode);
            player.sendMessage(joinMessage);
        }
    }
}