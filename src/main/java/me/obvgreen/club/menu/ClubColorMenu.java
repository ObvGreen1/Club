package me.obvgreen.club.menu;

import me.obvgreen.club.ClubPlugin;
import me.obvgreen.club.service.ClubService;
import me.obvgreen.club.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ClubColorMenu implements Listener {

    private final ClubPlugin plugin;
    private final ClubService clubService;

    public ClubColorMenu(ClubPlugin plugin, ClubService clubService) {
        this.plugin = plugin;
        this.clubService = clubService;
    }

    private String getTitle() {
        return ColorUtil.color(plugin.getConfig().getString("gui.club-color.title", "Select Club Color"));
    }

    public void open(Player player) {
        int size = plugin.getConfig().getInt("gui.club-color.size", 9);
        Inventory inv = Bukkit.createInventory(null, size, getTitle());

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("gui.club-color.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig == null) continue;

                int slot = itemConfig.getInt("slot");
                Material material = Material.matchMaterial(itemConfig.getString("material", "WOOL"));
                if (material == null) material = Material.WOOL;
                short data = (short) itemConfig.getInt("data", 0);
                String name = itemConfig.getString("name", "");

                inv.setItem(slot, createItem(material, data, name));
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, short data, String name) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String club = clubService.getPlayerClub(player.getUniqueId());
        if (club == null || !clubService.isOwner(player.getUniqueId(), club)) {
            player.closeInventory();
            return;
        }

        int clickedSlot = event.getSlot();
        String colorCode = null;

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("gui.club-color.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (itemsSection.getInt(key + ".slot") == clickedSlot) {
                    colorCode = itemsSection.getString(key + ".color-code");
                    break;
                }
            }
        }

        if (colorCode != null) {
            clubService.setClubColor(club, colorCode);
            player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.color-updated", "&aClub color updated!")));
            player.closeInventory();
        }
    }
}
