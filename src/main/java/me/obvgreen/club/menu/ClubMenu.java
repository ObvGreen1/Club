package me.obvgreen.club.menu;

import me.obvgreen.club.ClubPlugin;
import me.obvgreen.club.config.MessageService;
import me.obvgreen.club.service.ClubService;
import me.obvgreen.club.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClubMenu implements Listener {

    private final ClubPlugin plugin;
    private final ClubService clubService;
    private final MessageService messages;
    private final ClubColorMenu colorMenu;

    public ClubMenu(ClubPlugin plugin, ClubService clubService, MessageService messages, ClubColorMenu colorMenu) {
        this.plugin = plugin;
        this.clubService = clubService;
        this.messages = messages;
        this.colorMenu = colorMenu;
    }

    private String getTitle() {
        return ColorUtil.color(plugin.getConfig().getString("gui.club-menu.title", "&6Club Management"));
    }

    public void open(Player player) {
        String club = clubService.getPlayerClub(player.getUniqueId());
        if (club == null) {
            messages.send(player, "not-in-club");
            return;
        }

        if (!clubService.isStaff(player.getUniqueId(), club)) {
            messages.send(player, "only-staff-menu");
            return;
        }

        int size = plugin.getConfig().getInt("gui.club-menu.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, getTitle());

        // Add club info item
        ItemStack infoItem = createInfoItem(club);
        int infoSlot = plugin.getConfig().getInt("gui.club-menu.slots.info", 13);
        inv.setItem(infoSlot, infoItem);

        // Add color item
        ItemStack colorItem = createColorItem();
        int colorSlot = plugin.getConfig().getInt("gui.club-menu.slots.color", 11);
        inv.setItem(colorSlot, colorItem);

        // Add toggle open/closed item
        ItemStack toggleItem = createToggleItem(club);
        int toggleSlot = plugin.getConfig().getInt("gui.club-menu.slots.toggle", 15);
        inv.setItem(toggleSlot, toggleItem);

        // Add members item
        ItemStack membersItem = createMembersItem(club);
        int membersSlot = plugin.getConfig().getInt("gui.club-menu.slots.members", 20);
        inv.setItem(membersSlot, membersItem);

        // Add disband item (owner only)
        if (clubService.isOwner(player.getUniqueId(), club)) {
            ItemStack disbandItem = createDisbandItem();
            int disbandSlot = plugin.getConfig().getInt("gui.club-menu.slots.disband", 24);
            inv.setItem(disbandSlot, disbandItem);
        }

        // Add leave item (non-owner only)
        if (!clubService.isOwner(player.getUniqueId(), club)) {
            ItemStack leaveItem = createLeaveItem();
            int leaveSlot = plugin.getConfig().getInt("gui.club-menu.slots.leave", 24);
            inv.setItem(leaveSlot, leaveItem);
        }

        player.openInventory(inv);
    }

    private ItemStack createInfoItem(String club) {
        Material material = Material.matchMaterial(plugin.getConfig().getString("gui.club-menu.items.info.material", "BOOK"));
        if (material == null) material = Material.BOOK;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("gui.club-menu.items.info.name", "&eClub Info")));

            UUID owner = clubService.getOwner(club);
            String ownerName = owner != null ? Bukkit.getOfflinePlayer(owner).getName() : "Unknown";
            int memberCount = clubService.getMembers(club).size();
            boolean isOpen = clubService.isOpen(club);

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Name: &f" + club));
            lore.add(ColorUtil.color("&7Owner: &f" + ownerName));
            lore.add(ColorUtil.color("&7Members: &f" + memberCount));
            lore.add(ColorUtil.color("&7Open: &f" + (isOpen ? "Yes" : "No")));
            lore.add("");
            lore.add(ColorUtil.color("&7Click to view club details"));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createColorItem() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("gui.club-menu.items.color.material", "WOOL"));
        if (material == null) material = Material.WOOL;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("gui.club-menu.items.color.name", "&dChange Color")));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Click to change club color"));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createToggleItem(String club) {
        Material material = Material.matchMaterial(plugin.getConfig().getString("gui.club-menu.items.toggle.material", "IRON_DOOR"));
        if (material == null) material = Material.IRON_DOOR;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean isOpen = clubService.isOpen(club);
            String name = isOpen ? 
                plugin.getConfig().getString("gui.club-menu.items.toggle.name-open", "&cClose Club") :
                plugin.getConfig().getString("gui.club-menu.items.toggle.name-closed", "&aOpen Club");

            meta.setDisplayName(ColorUtil.color(name));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Current: " + (isOpen ? "&aOpen" : "&cClosed")));
            lore.add("");
            lore.add(ColorUtil.color("&7Click to toggle"));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMembersItem(String club) {
        Material material = Material.matchMaterial(plugin.getConfig().getString("gui.club-menu.items.members.material", "SKULL_ITEM"));
        if (material == null) material = Material.SKULL_ITEM;

        ItemStack item = new ItemStack(material, 1, (short) 3);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("gui.club-menu.items.members.name", "&bManage Members")));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Click to manage members"));
            lore.add(ColorUtil.color("&7Use /club kick, admin, demote"));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDisbandItem() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("gui.club-menu.items.disband.material", "BARRIER"));
        if (material == null) material = Material.BARRIER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("gui.club-menu.items.disband.name", "&cDisband Club")));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&cWarning: This cannot be undone!"));
            lore.add("");
            lore.add(ColorUtil.color("&7Click to disband the club"));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLeaveItem() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("gui.club-menu.items.leave.material", "SIGN"));
        if (material == null) material = Material.SIGN;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("gui.club-menu.items.leave.name", "&cLeave Club")));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Click to leave the club"));

            meta.setLore(lore);
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

        String club = clubService.getPlayerClub(player.getUniqueId());
        if (club == null || !clubService.isStaff(player.getUniqueId(), club)) {
            player.closeInventory();
            return;
        }

        int clickedSlot = event.getSlot();
        int infoSlot = plugin.getConfig().getInt("gui.club-menu.slots.info", 13);
        int colorSlot = plugin.getConfig().getInt("gui.club-menu.slots.color", 11);
        int toggleSlot = plugin.getConfig().getInt("gui.club-menu.slots.toggle", 15);
        int membersSlot = plugin.getConfig().getInt("gui.club-menu.slots.members", 20);
        int disbandSlot = plugin.getConfig().getInt("gui.club-menu.slots.disband", 24);
        int leaveSlot = plugin.getConfig().getInt("gui.club-menu.slots.leave", 24);

        if (clickedSlot == infoSlot) {
            clubService.sendInfo(player, club);
            player.closeInventory();
        } else if (clickedSlot == colorSlot) {
            if (clubService.isOwner(player.getUniqueId(), club)) {
                player.closeInventory();
                colorMenu.open(player);
            } else {
                messages.send(player, "only-owner-color");
            }
        } else if (clickedSlot == toggleSlot) {
            if (clubService.isOwner(player.getUniqueId(), club)) {
                boolean isOpen = clubService.isOpen(club);
                clubService.setOpen(player, !isOpen);
                player.closeInventory();
            } else {
                messages.send(player, "only-owner-toggle");
            }
        } else if (clickedSlot == membersSlot) {
            clubService.sendList(player);
            player.closeInventory();
        } else if (clickedSlot == disbandSlot && clubService.isOwner(player.getUniqueId(), club)) {
            clubService.disband(player);
            player.closeInventory();
        } else if (clickedSlot == leaveSlot && !clubService.isOwner(player.getUniqueId(), club)) {
            clubService.leaveClub(player);
            player.closeInventory();
        }
    }
}
