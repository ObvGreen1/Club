package me.obvgreen.club.listener;

import me.obvgreen.club.service.ChatToggleService;
import me.obvgreen.club.service.ClubService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ClubChatListener implements Listener {

    private final ClubService clubService;
    private final ChatToggleService chatToggleService;

    public ClubChatListener(ClubService clubService, ChatToggleService chatToggleService) {
        this.clubService = clubService;
        this.chatToggleService = chatToggleService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!chatToggleService.isEnabled(player.getUniqueId())) {
            return;
        }

        if (!clubService.isInClub(player.getUniqueId())) {
            chatToggleService.setEnabled(player.getUniqueId(), false);
            return;
        }

        event.setCancelled(true);
        clubService.sendClubChat(player, event.getMessage());
    }
}
