package me.help.betterlocalchat.handlers;

import me.help.betterlocalchat.BetterLocalChat;
import org.bukkit.ChatColor;
import me.help.betterlocalchat.handlers.ChatHandler;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        World playerWorld = player.getWorld();
        String cmd = command.getName().toLowerCase(); // Normalize command name

        if (cmd.equals("global") || cmd.equals("g")) {
            if (chatHandler.isInGlobalChat(player)) {
                player.sendMessage(ChatColor.RED + "You are already in Global Chat.");
                return true;
            }
            if (plugin.getConfig().getStringList("disabled-global-chat-worlds").contains(playerWorld.getName())) {
                player.sendMessage(ChatColor.RED + "Global chat is disabled in this world.");
                return true;
            }
            chatHandler.setChatMode(player, ChatModes.GLOBAL);
            player.sendMessage(ChatColor.GREEN + "You are now in Global Chat.");
            return true;
        }

        if (cmd.equals("local") || cmd.equals("l")) {
            if (chatHandler.isInLocalChat(player)) {
                player.sendMessage(ChatColor.RED + "You are already in Local Chat.");
                return true;
            }
            if (plugin.getConfig().getStringList("disabled-local-chat-worlds").contains(playerWorld.getName())) {
                player.sendMessage(ChatColor.RED + "Local chat is disabled in this world.");
                return true;
            }
            chatHandler.setChatMode(player, ChatModes.LOCAL);
            player.sendMessage(ChatColor.YELLOW + "You are now in Local Chat.");
            return true;
        }


        if (cmd.equals("betterlocalchat") || cmd.equals("blc")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (player.hasPermission("chat.reload")) {
                    plugin.loadConfig();
                    player.sendMessage(ChatColor.GREEN + "Local chat configuration reloaded!");
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to reload the chat!");
                }
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
                player.sendMessage(ChatColor.AQUA + "==== BetterLocalChat Commands ====");
                player.sendMessage(ChatColor.GOLD + "/global (/g) - " + ChatColor.YELLOW + "Switch to global chat");
                player.sendMessage(ChatColor.GOLD + "/local (/l) - " + ChatColor.YELLOW + "Switch to local chat");
                player.sendMessage(ChatColor.GOLD + "/betterlocalchat reload - " + ChatColor.YELLOW + "Reloads the plugin configuration");
                player.sendMessage(ChatColor.GOLD + "/toggleglobalchat (/tgc) - " + ChatColor.YELLOW + "Toggle global chat visibility");
                player.sendMessage(ChatColor.GOLD + "/toggleglobalprefix (/tgp) - " + ChatColor.YELLOW + "Toggle handling of global chat prefix");
                player.sendMessage(ChatColor.GOLD + "/staffchat (/sc) - " + ChatColor.YELLOW + "Switch to staff chat");
                player.sendMessage(ChatColor.GOLD + "/togglelocalview - " + ChatColor.YELLOW + "Toggle the ability to see all local chats");
                player.sendMessage(ChatColor.AQUA + "=================================");
                return true;
            }

            player.sendMessage(ChatColor.RED + "Usage: /betterlocalchat <reload/help>");
            return true;
        }


        if (cmd.equals("toggleglobalchat")) {
            if (player.hasPermission("chat.togglevisibility")) {
                chatHandler.toggleGlobalChatVisibility(player);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to toggle global chat visibility!");
            }
            return true;
        }

        if (cmd.equals("toggleglobalprefix")) {
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

        if (cmd.equals("staffchat") || cmd.equals("sc")) {
            if (!player.hasPermission("chat.staff")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
                return true;
            }

            if (chatHandler.isInStaffChat(player)) {
                if (chatHandler.isReturnToGlobalOnLeaveStaffChat()) {
                    chatHandler.setChatMode(player, ChatModes.GLOBAL);
                    player.sendMessage(ChatColor.GREEN + "You have left Staff Chat and switched to Global Chat.");
                } else {
                    chatHandler.setChatMode(player, ChatModes.LOCAL);
                    player.sendMessage(ChatColor.YELLOW + "You have left Staff Chat.");
                }
            } else {
                chatHandler.setChatMode(player, ChatModes.STAFF);
                player.sendMessage(ChatColor.GOLD + "You are now in Staff Chat.");
            }
            return true;
        }

        if (cmd.equals("togglelocalview")) {
            if (!player.hasPermission("chat.seealllocal")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to see all local chats.");
                return true;
            }

            chatHandler.toggleSeeAllLocalChats(player);
            return true;
        }
        if (cmd.equals("devchat") || cmd.equals("dc")) {
            if (!player.hasPermission("chat.developer")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use Developer Chat.");
                return true;
            }

            if (chatHandler.isInDeveloperChat(player)) {
                if (chatHandler.isReturnToGlobalOnLeaveStaffChat()) {
                    chatHandler.setChatMode(player, ChatModes.GLOBAL);
                    player.sendMessage(ChatColor.GREEN + "You have left Developer Chat.");
                } else {
                    chatHandler.setChatMode(player, ChatModes.LOCAL);
                    player.sendMessage(ChatColor.YELLOW + "You have left Developer Chat.");
                }
            } else {
                chatHandler.setChatMode(player, ChatModes.DEVELOPER);
                player.sendMessage(ChatColor.AQUA + "You are now in Developer Chat.");
            }
            return true;
        }
        if (cmd.equals("globalplus") || cmd.equals("gp")) {
            if (!player.hasPermission("chat.globalplus")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use GlobalPlus Chat.");
                return true;
            }
            if (chatHandler.getChatMode(player) == ChatModes.GLOBALPLUS) {
                if (chatHandler.isReturnToGlobalOnLeaveStaffChat()) {
                    chatHandler.setChatMode(player, ChatModes.GLOBAL);
                    player.sendMessage(ChatColor.GREEN + "You have left GlobalPlus Chat.");
                } else {
                    chatHandler.setChatMode(player, ChatModes.LOCAL);
                    player.sendMessage(ChatColor.YELLOW + "You have left GlobalPlus Chat.");
                }
            } else {
                chatHandler.setChatMode(player, ChatModes.GLOBALPLUS);
                player.sendMessage(ChatColor.GOLD + "You are now in GlobalPlus Chat.");
            }
            return true;
        }


        return false; // Returning false lets Bukkit show the "Usage" message
    }
}