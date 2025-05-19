package me.help.betterlocalchat;

import me.help.betterlocalchat.handlers.ChatHandler;
import me.help.betterlocalchat.handlers.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterLocalChat extends JavaPlugin {

    private ChatHandler chatHandler;

    @Override
    public void onEnable() {
        getLogger().info("BetterLocalChat is starting...");

        try {
            // Initialize ChatHandler
            chatHandler = new ChatHandler(this);
            getLogger().info("ChatHandler initialized successfully.");

            // Load configuration
            loadConfig();
            getLogger().info("Configuration loaded successfully.");

            // Register event listeners
            Bukkit.getPluginManager().registerEvents(chatHandler, this);
            getLogger().info("Event listeners registered successfully.");

            // Register commands
            CommandHandler commandHandler = new CommandHandler(this, chatHandler);
            getLogger().info("CommandHandler initialized successfully.");

            registerCommands(commandHandler);
            getLogger().info("Commands registered successfully.");

            // PlaceholderAPI check
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                getLogger().info("PlaceholderAPI detected! Prefixes will be processed with placeholders.");
            } else {
                getLogger().warning("PlaceholderAPI not found! Prefixes will not use placeholders.");
            }

            getLogger().info("BetterLocalChat has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("An error occurred while enabling BetterLocalChat!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this); // Disable plugin on error
        }
    }

    private void registerCommands(CommandHandler commandHandler) {
        getCommand("global").setExecutor(commandHandler);
        getCommand("local").setExecutor(commandHandler);
        getCommand("betterlocalchat").setExecutor(commandHandler);
        getCommand("toggleglobalchat").setExecutor(commandHandler);
        getCommand("toggleglobalprefix").setExecutor(commandHandler);
        getCommand("staffchat").setExecutor(commandHandler);
        getCommand("togglelocalview").setExecutor(commandHandler);
        getCommand("devchat").setExecutor(commandHandler);
        getCommand("globalplus").setExecutor(commandHandler);
    }


    public void loadConfig() {

        saveDefaultConfig();
        reloadConfig();  // Reload the config from disk
        chatHandler.loadConfig();  // Reload settings in the ChatHandler
        getLogger().info("Chat Plugin Config Loaded!");
    }
}