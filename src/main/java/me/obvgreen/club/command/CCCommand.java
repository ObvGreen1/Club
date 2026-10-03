package me.obvgreen.club.command;

import me.obvgreen.club.config.MessageService;
import me.obvgreen.club.service.ClubService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CCCommand implements CommandExecutor {

    private final ClubService clubService;
    private final MessageService messages;

    public CCCommand(ClubService clubService, MessageService messages) {
        this.clubService = clubService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.send(sender, "players-only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            String message = String.join(" ", args);
            clubService.sendClubChat(player, message);
        } else {
            clubService.toggleChat(player);
        }
        return true;
    }
}
