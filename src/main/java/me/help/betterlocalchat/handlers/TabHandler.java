package me.help.betterlocalchat.handlers;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabHandler implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Handle tab completion for /betterlocalchat or /blc
        if (command.getName().equalsIgnoreCase("betterlocalchat") || command.getName().equalsIgnoreCase("blc")) {
            if (args.length == 1) {
                if (sender.hasPermission("chat.reload") && "reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
                if ("help".startsWith(args[0].toLowerCase())) {
                    completions.add("help");
                }
            }
        }

        return completions;
    }
}