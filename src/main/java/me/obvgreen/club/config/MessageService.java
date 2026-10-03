package me.obvgreen.club.config;

import me.obvgreen.club.ClubPlugin;
import me.obvgreen.club.util.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class MessageService {

    private final ClubPlugin plugin;

    public MessageService(ClubPlugin plugin) {
        this.plugin = plugin;
    }

    public String get(String path) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&3Club&8] &r");
        String value = plugin.getConfig().getString("messages." + path, "&cMissing message: " + path);
        return ColorUtil.color(prefix + value);
    }

    public List<String> getList(String path) {
        return ColorUtil.color(plugin.getConfig().getStringList("messages." + path));
    }

    public String format(String path, Map<String, String> replacements) {
        String msg = get(path);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return msg;
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        sender.sendMessage(format(path, replacements));
    }

    public void sendList(CommandSender sender, String path) {
        for (String line : getList(path)) {
            sender.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "&8[&3Club&8] &r")) + line);
        }
    }
}
