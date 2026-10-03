package me.obvgreen.club.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.obvgreen.club.ClubPlugin;
import me.obvgreen.club.service.ClubService;
import org.bukkit.OfflinePlayer;

public class ClubPlaceholderExpansion extends PlaceholderExpansion {

    private final ClubPlugin plugin;
    private final ClubService clubService;

    public ClubPlaceholderExpansion(ClubPlugin plugin, ClubService clubService) {
        this.plugin = plugin;
        this.clubService = clubService;
    }

    @Override
    public String getIdentifier() {
        return "club";
    }

    @Override
    public String getAuthor() {
        return "obvgreen";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }

        if (params.equalsIgnoreCase("current_club")) {
            String club = clubService.getPlayerClub(player.getUniqueId());

            if (club == null) {
                return plugin.getConfig().getString("placeholder.no-club", "");
            }

            String color = clubService.getClubColor(club);
            return me.obvgreen.club.util.ColorUtil.color("&7[" + color + club + "&7]&r ");
        }

        if (params.equalsIgnoreCase("online_members")) {
            String club = clubService.getPlayerClub(player.getUniqueId());

            if (club == null) {
                return plugin.getConfig().getString("placeholder.no-club", "");
            }

            return String.valueOf(clubService.getOnlineMembersCount(club));
        }

        return null;
    }
}