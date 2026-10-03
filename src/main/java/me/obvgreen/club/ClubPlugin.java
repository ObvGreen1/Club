package me.obvgreen.club;

import me.obvgreen.club.command.CCCommand;
import me.obvgreen.club.command.ClubCommand;
import me.obvgreen.club.config.MessageService;
import me.obvgreen.club.database.DatabaseManager;
import me.obvgreen.club.menu.ClubColorMenu;
import me.obvgreen.club.menu.ClubMenu;
import me.obvgreen.club.hook.ClubPlaceholderExpansion;
import me.obvgreen.club.listener.ClubChatListener;
import me.obvgreen.club.service.ChatToggleService;
import me.obvgreen.club.service.ClubService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClubPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private MessageService messageService;
    private ClubService clubService;
    private ChatToggleService chatToggleService;
    private ClubColorMenu colorMenu;
    private ClubMenu clubMenu;

    private ClubPlaceholderExpansion placeholderExpansion;

    private boolean shuttingDown = false;

    @Override
    public void onEnable() {
        shuttingDown = false;

        try {
            startPlugin();
            getLogger().info("ClubPlugin enabled.");
        } catch (Throwable throwable) {
            getLogger().severe("ClubPlugin failed to enable safely.");
            throwable.printStackTrace();

            safeShutdown();

            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        safeShutdown();
        getLogger().info("ClubPlugin disabled.");
    }

    public void reloadPlugin() {
        if (shuttingDown) {
            return;
        }

        getLogger().info("Reloading ClubPlugin...");

        safeShutdown();

        reloadConfig();

        try {
            startPlugin();
            getLogger().info("ClubPlugin reloaded.");
        } catch (Throwable throwable) {
            getLogger().severe("ClubPlugin failed to reload.");
            throwable.printStackTrace();

            safeShutdown();

            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void startPlugin() {
        saveDefaultConfig();
        this.messageService = new MessageService(this);
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.init();
        this.clubService = new ClubService(this, databaseManager, messageService, null);
        this.chatToggleService = new ChatToggleService(clubService.getCache());

        this.colorMenu = new ClubColorMenu(this, clubService);
        this.clubMenu = new ClubMenu(this, clubService, messageService, colorMenu);

        registerCommands();
        registerListeners();
        registerPlaceholderAPI();

        // Enforce member limit for existing clubs
        clubService.enforceMemberLimit();

        // Schedule periodic cache save every 30 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            clubService.saveCacheToDatabase();
        }, 30 * 60 * 20L, 30 * 60 * 20L); // 30 minutes in ticks
    }

    private void registerCommands() {
        PluginCommand clubCommandPlugin = getCommand("club");

        if (clubCommandPlugin == null) {
            throw new IllegalStateException(
                    "Command 'club' is missing from plugin.yml. " +
                            "Make sure src/main/resources/plugin.yml contains commands.club."
            );
        }

        ClubCommand clubCommand = new ClubCommand(this, clubService, messageService, colorMenu, clubMenu);
        clubCommandPlugin.setExecutor(clubCommand);
        clubCommandPlugin.setTabCompleter(clubCommand);

        PluginCommand ccCommandPlugin = getCommand("cc");

        if (ccCommandPlugin == null) {
            throw new IllegalStateException(
                    "Command 'cc' is missing from plugin.yml. " +
                            "Make sure src/main/resources/plugin.yml contains commands.cc."
            );
        }

        ccCommandPlugin.setExecutor(new CCCommand(clubService, messageService));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(
                new ClubChatListener(clubService, chatToggleService),
                this
        );
        Bukkit.getPluginManager().registerEvents(colorMenu, this);
        Bukkit.getPluginManager().registerEvents(clubMenu, this);
    }

    private void registerPlaceholderAPI() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        try {
            this.placeholderExpansion = new ClubPlaceholderExpansion(this, clubService);
            this.placeholderExpansion.register();

            getLogger().info("Registered PlaceholderAPI expansion.");
        } catch (Throwable throwable) {
            getLogger().warning("Could not register PlaceholderAPI expansion: " + throwable.getMessage());
        }
    }

    private void safeShutdown() {
        try {
            HandlerList.unregisterAll(this);
        } catch (Throwable ignored) {
        }

        try {
            Bukkit.getScheduler().cancelTasks(this);
        } catch (Throwable ignored) {
        }

        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
            } catch (Throwable ignored) {
            }

            placeholderExpansion = null;
        }

        // Save cache to database before closing
        if (clubService != null) {
            try {
                clubService.saveCacheToDatabase();
            } catch (Throwable ignored) {
            }
        }

        if (databaseManager != null) {
            try {
                databaseManager.close();
            } catch (Throwable ignored) {
            }

            databaseManager = null;
        }

        clubService = null;
        chatToggleService = null;
        messageService = null;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public ClubService getClubService() {
        return clubService;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
