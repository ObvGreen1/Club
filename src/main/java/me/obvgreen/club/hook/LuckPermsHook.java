package me.obvgreen.club.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class LuckPermsHook {

    private LuckPermsHook() {
    }

    public static String getPrefix(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return "";
        }

        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(player.getUniqueId());

            if (user == null) {
                return "";
            }

            String prefix = user.getCachedData().getMetaData().getPrefix();

            if (prefix == null) {
                return "";
            }

            return ChatColor.translateAlternateColorCodes('&', prefix);
        } catch (Throwable throwable) {
            return "";
        }
    }
}
