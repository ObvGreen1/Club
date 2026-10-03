package me.obvgreen.club.command;

import me.obvgreen.club.ClubPlugin;
import me.obvgreen.club.config.MessageService;
import me.obvgreen.club.service.ClubService;
import me.obvgreen.club.menu.ClubColorMenu;
import me.obvgreen.club.menu.ClubMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;



public class ClubCommand implements CommandExecutor, TabCompleter {

    private final ClubPlugin plugin;
    private final ClubService clubService;
    private final MessageService messages;
    private final ClubColorMenu colorMenu;
    private final ClubMenu clubMenu;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_SECONDS = 15;


    public ClubCommand(ClubPlugin plugin, ClubService clubService, MessageService messages, ClubColorMenu colorMenu, ClubMenu clubMenu) {
        this.plugin = plugin;
        this.clubService = clubService;
        this.messages = messages;
        this.colorMenu = colorMenu;
        this.clubMenu = clubMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("club.reload") && !sender.hasPermission("club.admin")) {
                messages.send(sender, "no-permission");
                return true;
            }

            plugin.reloadPlugin();

            if (plugin.getMessageService() != null) {
                plugin.getMessageService().send(sender, "reloaded");
            } else {
                sender.sendMessage("§cClubPlugin failed to reload. Check console.");
            }

            return true;
        }

        if (!(sender instanceof Player)) {
            messages.send(sender, "players-only");
            return true;
        }

        Player player = (Player) sender;

        // Check cooldown
        UUID playerUUID = player.getUniqueId();
        if (cooldowns.containsKey(playerUUID)) {
            long lastUsed = cooldowns.get(playerUUID);
            long timePassed = (System.currentTimeMillis() - lastUsed) / 1000;
            if (timePassed < COOLDOWN_SECONDS) {
                long timeLeft = COOLDOWN_SECONDS - timePassed;
                player.sendMessage("§cPlease wait §e" + timeLeft + "§c seconds before using this command again.");
                return true;
            }
        }

        if (args.length == 0) {
            messages.sendList(player, "help");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                if (args.length < 2) {
                    messages.send(player, "create-usage");
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.createClub(player, args[1]);
                return true;

            case "invite":
                if (args.length < 2) {
                    messages.send(player, "invite-usage");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);

                if (target == null) {
                    messages.send(player, "player-not-found", Collections.singletonMap("player", args[1]));
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.invite(player, target);
                return true;

            case "color":
                String currentClub = clubService.getPlayerClub(player.getUniqueId());
                if (currentClub == null) {
                    messages.send(player, "not-in-club");
                    return true;
                }
                if (!clubService.isOwner(player.getUniqueId(), currentClub)) {
                    messages.send(player, "only-owner-admin"); // or make a custom "only-owner-color" message
                    return true;
                }
                colorMenu.open(player);
                return true;

            case "accept":
                if (args.length < 2) {
                    messages.send(player, "accept-usage");
                    return true;
                }

                Player inviter = Bukkit.getPlayerExact(args[1]);
                if (inviter == null) {
                    messages.send(player, "player-not-found", Collections.singletonMap("player", args[1]));
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.acceptInvite(player, inviter);
                return true;

            case "join":
                if (args.length < 2) {
                    messages.send(player, "join-usage");
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.joinOpenClub(player, args[1]);
                return true;

            case "open":
                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.setOpen(player, true);
                return true;

            case "close":
                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.setOpen(player, false);
                return true;

            case "leave":
                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.leaveClub(player);
                return true;

            case "disband":
                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.disband(player);
                return true;

            case "chat":
            case "clubchat":
                if (args.length > 1) {
                    String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    clubService.sendClubChat(player, message);
                } else {
                    cooldowns.put(playerUUID, System.currentTimeMillis());
                    clubService.toggleChat(player);
                }
                return true;

            case "kick":
                if (args.length < 2) {
                    messages.send(player, "kick-usage");
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.kick(player, args[1]);
                return true;

            case "admin":
                if (args.length < 2) {
                    messages.send(player, "admin-usage");
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.promoteAdmin(player, args[1]);
                return true;

            case "demote":
                if (args.length < 2) {
                    messages.send(player, "demote-usage");
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.demoteAdmin(player, args[1]);
                return true;

            case "list":
                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.sendList(player);
                return true;

            case "info":
                if (args.length < 2) {
                    messages.send(player, "info-usage");
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.sendInfo(player, args[1]);
                return true;

            case "menu":
                clubMenu.open(player);
                return true;

            case "rename":
                if (args.length < 2) {
                    messages.send(player, "rename-usage");
                    return true;
                }

                cooldowns.put(playerUUID, System.currentTimeMillis());
                clubService.renameClub(player, args[1]);
                return true;

            default:
                messages.send(player, "unknown-subcommand");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], Arrays.asList(
                    "create",
                    "invite",
                    "accept",
                    "join",
                    "open",
                    "close",
                    "leave",
                    "disband",
                    "chat",
                    "kick",
                    "admin",
                    "demote",
                    "list",
                    "info",
                    "reload",
                    "color",
                    "menu",
                    "rename"
            ));
        }

        if (args.length == 2 && (
                args[0].equalsIgnoreCase("invite")
                        || args[0].equalsIgnoreCase("kick")
                        || args[0].equalsIgnoreCase("admin")
                        || args[0].equalsIgnoreCase("demote")
                        || args[0].equalsIgnoreCase("accept")
        )) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return partial(args[1], names);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return partial(args[1], clubService.getClubNames());
        }

        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> options) {
        List<String> result = new ArrayList<>();

        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(option);
            }
        }

        return result;
    }
}
