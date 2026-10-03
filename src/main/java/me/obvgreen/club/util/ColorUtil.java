package me.obvgreen.club.util;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public static List<String> color(List<String> input) {
        return input.stream().map(ColorUtil::color).collect(Collectors.toList());
    }
}
