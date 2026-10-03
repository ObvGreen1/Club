package me.obvgreen.club.service;

import me.obvgreen.club.ClubPlugin;
import me.obvgreen.club.cache.ClubCache;
import me.obvgreen.club.config.MessageService;
import me.obvgreen.club.database.DatabaseManager;
import me.obvgreen.club.hook.LuckPermsHook;
import me.obvgreen.club.model.ClubInvite;
import me.obvgreen.club.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ClubService {

    private final ClubPlugin plugin;
    private final DatabaseManager databaseManager;
    private final MessageService messages;
    private final ChatToggleService chatToggleService;
    private final ClubCache cache;

    public ClubService(ClubPlugin plugin, DatabaseManager databaseManager, MessageService messages, ChatToggleService chatToggleService) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.messages = messages;
        this.chatToggleService = chatToggleService;
        this.cache = new ClubCache();
        loadCacheFromDatabase();
    }

    private String normalizeClubName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private void loadCacheFromDatabase() {
        try {
            // Load clubs
            String clubSql = "SELECT name, normalized_name, owner_uuid, open, color FROM clubs";
            try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(clubSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String normalizedName = rs.getString("normalized_name");
                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                    boolean open = rs.getInt("open") == 1;
                    String color = rs.getString("color");
                    cache.addClub(name, normalizedName, ownerUuid, open, color);
                }
            }

            // Load members
            String memberSql = "SELECT club_name, player_uuid FROM club_members";
            try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(memberSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String clubName = rs.getString("club_name");
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    ClubCache.ClubData clubData = cache.getClub(clubName);
                    if (clubData != null && !clubData.members.contains(playerUuid)) {
                        cache.addMember(clubName, playerUuid);
                    }
                }
            }

            // Load admins
            String adminSql = "SELECT club_name, player_uuid FROM club_admins";
            try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(adminSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String clubName = rs.getString("club_name");
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    cache.addAdmin(clubName, playerUuid);
                }
            }

            // Load invites
            String inviteSql = "SELECT player_uuid, club_name, expires_at, inviter_uuid FROM club_invites";
            try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(inviteSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    String clubName = rs.getString("club_name");
                    long expiresAt = rs.getLong("expires_at");
                    UUID inviterUuid = rs.getString("inviter_uuid") != null ? UUID.fromString(rs.getString("inviter_uuid")) : null;
                    cache.addInvite(playerUuid, new ClubInvite(playerUuid, clubName, expiresAt, inviterUuid));
                }
            }

            // Load chat toggles
            String toggleSql = "SELECT player_uuid, enabled FROM club_chat_toggles";
            try (PreparedStatement ps = databaseManager.getConnection().prepareStatement(toggleSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    boolean enabled = rs.getInt("enabled") == 1;
                    cache.setChatToggle(playerUuid, enabled);
                }
            }

            plugin.getLogger().info("Loaded club data from database into cache.");
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to load cache from database: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void saveCacheToDatabase() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Connection connection = databaseManager.getConnection();
                connection.setAutoCommit(false);

                try {
                    // Clear existing data
                    try (Statement stmt = connection.createStatement()) {
                        stmt.executeUpdate("DELETE FROM club_admins");
                        stmt.executeUpdate("DELETE FROM club_members");
                        stmt.executeUpdate("DELETE FROM club_invites");
                        stmt.executeUpdate("DELETE FROM club_chat_toggles");
                        stmt.executeUpdate("DELETE FROM clubs");
                    }

                    // Save clubs
                    String insertClub = "INSERT INTO clubs(name, normalized_name, owner_uuid, open, color) VALUES(?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertClub)) {
                        for (ClubCache.ClubData clubData : cache.getAllClubs()) {
                            ps.setString(1, clubData.name);
                            ps.setString(2, clubData.normalizedName);
                            ps.setString(3, clubData.ownerUuid.toString());
                            ps.setInt(4, clubData.open ? 1 : 0);
                            ps.setString(5, clubData.color);
                            ps.executeUpdate();
                        }
                    }

                    // Save members
                    String insertMember = "INSERT INTO club_members(club_name, player_uuid) VALUES(?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertMember)) {
                        for (ClubCache.ClubData clubData : cache.getAllClubs()) {
                            for (UUID member : clubData.members) {
                                ps.setString(1, clubData.name);
                                ps.setString(2, member.toString());
                                ps.executeUpdate();
                            }
                        }
                    }

                    // Save admins
                    String insertAdmin = "INSERT INTO club_admins(club_name, player_uuid) VALUES(?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertAdmin)) {
                        for (ClubCache.ClubData clubData : cache.getAllClubs()) {
                            for (UUID admin : clubData.admins) {
                                ps.setString(1, clubData.name);
                                ps.setString(2, admin.toString());
                                ps.executeUpdate();
                            }
                        }
                    }

                    // Save invites
                    String insertInvite = "INSERT INTO club_invites(player_uuid, club_name, expires_at, inviter_uuid) VALUES(?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertInvite)) {
                        for (ClubInvite invite : cache.getAllInvites()) {
                            ps.setString(1, invite.getPlayerUuid().toString());
                            ps.setString(2, invite.getClubName());
                            ps.setLong(3, invite.getExpiresAt());
                            ps.setString(4, invite.getInviterUuid() != null ? invite.getInviterUuid().toString() : null);
                            ps.executeUpdate();
                        }
                    }

                    // Save chat toggles
                    String insertToggle = "INSERT INTO club_chat_toggles(player_uuid, enabled) VALUES(?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertToggle)) {
                        for (Map.Entry<UUID, Boolean> entry : cache.getAllChatToggles().entrySet()) {
                            ps.setString(1, entry.getKey().toString());
                            ps.setInt(2, entry.getValue() ? 1 : 0);
                            ps.executeUpdate();
                        }
                    }

                    connection.commit();
                    plugin.getLogger().info("Saved cache data to database.");
                } catch (SQLException ex) {
                    connection.rollback();
                    plugin.getLogger().severe("Failed to save cache to database: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to save cache to database: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    public ClubCache getCache() {
        return cache;
    }

    public void setClubColor(String clubName, String color) {
        cache.setClubColor(clubName, color);
    }

    public String getClubColor(String clubName) {
        String color = cache.getClubColor(clubName);
        if (color != null && !color.isEmpty()) {
            return color;
        }
        // Fallback to the configurable default color
        return plugin.getConfig().getString("default-club-color", "&b");
    }


    private Map<String, String> replacements(String... values) {
        Map<String, String> replacements = new HashMap<>();

        for (int i = 0; i + 1 < values.length; i += 2) {
            replacements.put(values[i], values[i + 1]);
        }

        return replacements;
    }

    private String getOfflineName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
    }

    private List<UUID> sortOnlineFirst(Collection<UUID> uuids) {
        return uuids.stream()
                .sorted((first, second) -> {
                    OfflinePlayer firstPlayer = Bukkit.getOfflinePlayer(first);
                    OfflinePlayer secondPlayer = Bukkit.getOfflinePlayer(second);

                    boolean firstOnline = firstPlayer.isOnline();
                    boolean secondOnline = secondPlayer.isOnline();

                    if (firstOnline != secondOnline) {
                        return firstOnline ? -1 : 1;
                    }

                    String firstName = firstPlayer.getName() != null ? firstPlayer.getName() : first.toString();
                    String secondName = secondPlayer.getName() != null ? secondPlayer.getName() : second.toString();

                    return firstName.compareToIgnoreCase(secondName);
                })
                .collect(Collectors.toList());
    }

    private String formatPlayer(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
        boolean online = offlinePlayer.isOnline();

        return ColorUtil.color("&b" + name + " " + (online ? "&a●" : "&c●"));
    }

    private String formatPlayerList(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return ColorUtil.color("&7None");
        }

        return sortOnlineFirst(uuids).stream()
                .map(this::formatPlayer)
                .collect(Collectors.joining(ColorUtil.color("&7, ")));
    }

    public boolean isInClub(UUID uuid) {
        return cache.isInClub(uuid);
    }

    public String getPlayerClub(UUID uuid) {
        return cache.getPlayerClub(uuid);
    }

    public boolean clubExists(String clubName) {
        return cache.clubExists(normalizeClubName(clubName));
    }

    public String getRealClubName(String input) {
        return cache.getRealClubName(input);
    }

    public boolean createClub(Player player, String clubName) {
        if (!player.hasPermission("club.create")) {
            messages.send(player, "no-permission");
            return false;
        }

        if (isInClub(player.getUniqueId())) {
            messages.send(player, "already-in-club");
            return false;
        }

        int min = plugin.getConfig().getInt("club.name-min-length", 3);
        int max = plugin.getConfig().getInt("club.name-max-length", 16);
        String regex = plugin.getConfig().getString("club.name-regex", "^[A-Za-z0-9_]+$");

        if (clubName.length() < min || clubName.length() > max || !clubName.matches(regex)) {
            messages.send(player, "invalid-club-name");
            return false;
        }

        if (clubExists(clubName)) {
            messages.send(player, "club-name-exists");
            return false;
        }

        String defaultColor = plugin.getConfig().getString("default-club-color", "&b");
        cache.addClub(clubName, normalizeClubName(clubName), player.getUniqueId(), false, defaultColor);
        messages.send(player, "club-created", Collections.singletonMap("club", clubName));
        return true;
    }

    public UUID getOwner(String clubName) {
        return cache.getOwner(clubName);
    }

    public boolean isOwner(UUID uuid, String clubName) {
        return cache.isOwner(uuid, clubName);
    }

    public boolean isAdmin(UUID uuid, String clubName) {
        return cache.isAdmin(uuid, clubName);
    }

    public boolean isStaff(UUID uuid, String clubName) {
        return isOwner(uuid, clubName) || isAdmin(uuid, clubName);
    }

    public List<UUID> getAdmins(String clubName) {
        return new ArrayList<>(cache.getAdmins(clubName));
    }

    public void removeAdmin(UUID uuid, String clubName) {
        cache.removeAdmin(clubName, uuid);
    }

    public void invite(Player sender, Player target) {
        String club = getPlayerClub(sender.getUniqueId());

        if (club == null) {
            messages.send(sender, "not-in-club");
            return;
        }

        if (!isStaff(sender.getUniqueId(), club)) {
            messages.send(sender, "only-staff-invite");
            return;
        }

        if (isInClub(target.getUniqueId())) {
            messages.send(sender, "target-already-in-club");
            return;
        }

        long expiresAt = System.currentTimeMillis() + (plugin.getConfig().getInt("club.invite-expire-seconds", 300) * 1000L);
        cache.addInvite(target.getUniqueId(), new ClubInvite(target.getUniqueId(), club, expiresAt, sender.getUniqueId()));

        messages.send(sender, "invited-player", Collections.singletonMap("player", target.getName()));

        // Send invite message with clickable accept button
        messages.send(target, "invite-received", Collections.singletonMap("club", club));

        String clickCommand = "/club accept " + sender.getName();
        net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent("");
        net.md_5.bungee.api.chat.TextComponent clickButton = new net.md_5.bungee.api.chat.TextComponent("[CLICK TO ACCEPT]");
        clickButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        clickButton.setBold(true);
        clickButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, clickCommand));
        clickButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.TextComponent[] {
            new net.md_5.bungee.api.chat.TextComponent("Click to accept invite from " + sender.getName())
        }));

        message.addExtra(clickButton);
        target.spigot().sendMessage(message);

        broadcastToClub(club, "club-invite-broadcast", replacements(
                "sender", sender.getName(),
                "player", target.getName(),
                "club", club
        ));
    }

    public ClubInvite getInvite(UUID uuid) {
        return cache.getInvite(uuid);
    }

    public void deleteInvite(UUID uuid) {
        cache.removeInvite(uuid);
    }

    public void acceptInvite(Player player, Player inviter) {
        if (isInClub(player.getUniqueId())) {
            messages.send(player, "already-in-club");
            return;
        }

        ClubInvite invite = getInvite(player.getUniqueId());

        if (invite == null) {
            messages.send(player, "no-invite");
            return;
        }

        if (invite.isExpired()) {
            deleteInvite(player.getUniqueId());
            messages.send(player, "invite-expired");
            return;
        }

        // Check if the inviter matches the invite
        if (!invite.getInviterUuid().equals(inviter.getUniqueId())) {
            messages.send(player, "invalid-inviter");
            return;
        }

        String club = getRealClubName(invite.getClubName());

        if (club == null) {
            deleteInvite(player.getUniqueId());
            messages.send(player, "club-not-found");
            return;
        }

        if (getTotalMemberCount(club) >= 40) {
            deleteInvite(player.getUniqueId());
            messages.send(player, "club-full");
            return;
        }

        cache.addMember(club, player.getUniqueId());
        deleteInvite(player.getUniqueId());

        messages.send(player, "joined-club", Collections.singletonMap("club", club));

        broadcastToClub(club, "club-join-broadcast", replacements(
                "player", player.getName(),
                "club", club
        ));
    }

    public boolean isOpen(String clubName) {
        return cache.isOpen(clubName);
    }

    public void joinOpenClub(Player player, String clubInput) {
        if (isInClub(player.getUniqueId())) {
            messages.send(player, "already-in-club");
            return;
        }

        String club = getRealClubName(clubInput);

        if (club == null) {
            messages.send(player, "club-not-found");
            return;
        }

        if (!isOpen(club)) {
            messages.send(player, "club-not-open");
            return;
        }

        if (getTotalMemberCount(club) >= 40) {
            messages.send(player, "club-full");
            return;
        }

        cache.addMember(club, player.getUniqueId());
        deleteInvite(player.getUniqueId());

        messages.send(player, "joined-club", Collections.singletonMap("club", club));

        broadcastToClub(club, "club-join-broadcast", replacements(
                "player", player.getName(),
                "club", club
        ));
    }

    public void setOpen(Player player, boolean open) {
        String club = getPlayerClub(player.getUniqueId());

        if (club == null) {
            messages.send(player, "not-in-club");
            return;
        }

        if (!isOwner(player.getUniqueId(), club)) {
            messages.send(player, open ? "only-owner-open" : "only-owner-close");
            return;
        }

        cache.setOpen(club, open);
        messages.send(player, open ? "club-opened" : "club-closed");

        broadcastToClub(club, open ? "club-open-broadcast" : "club-close-broadcast", replacements(
                "sender", player.getName(),
                "player", player.getName(),
                "club", club
        ));
    }

    public void leaveClub(Player player) {
        String club = getPlayerClub(player.getUniqueId());

        if (club == null) {
            messages.send(player, "not-in-club");
            return;
        }

        if (isOwner(player.getUniqueId(), club)) {
            messages.send(player, "owner-must-disband");
            return;
        }

        removeAdmin(player.getUniqueId(), club);
        removeMember(player.getUniqueId());

        messages.send(player, "left-club", Collections.singletonMap("club", club));

        broadcastToClub(club, "club-leave-broadcast", replacements(
                "player", player.getName(),
                "club", club
        ));
    }

    public void broadcastToClub(String club, String messagePath, Map<String, String> replacements) {
        Set<UUID> recipients = new HashSet<>(getMembers(club));

        for (UUID uuid : recipients) {
            Player member = Bukkit.getPlayer(uuid);

            if (member != null && member.isOnline()) {
                messages.send(member, messagePath, replacements);
            }
        }
    }

    public void removeMember(UUID uuid) {
        String club = getPlayerClub(uuid);

        if (club != null) {
            removeAdmin(uuid, club);
        }

        cache.removeMember(uuid);
    }

    public List<UUID> getMembers(String clubName) {
        return new ArrayList<>(cache.getMembers(clubName));
    }

    public int getTotalMemberCount(String clubName) {
        return cache.getMembers(clubName).size();
    }

    public int getOnlineMembersCount(String clubName) {
        List<UUID> members = getMembers(clubName);
        int onlineCount = 0;

        for (UUID uuid : members) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.isOnline()) {
                onlineCount++;
            }
        }

        return onlineCount;
    }

    public void disband(Player player) {
        String club = getPlayerClub(player.getUniqueId());

        if (club == null) {
            messages.send(player, "not-in-club");
            return;
        }

        if (!isOwner(player.getUniqueId(), club)) {
            messages.send(player, "only-owner-disband");
            return;
        }

        List<UUID> members = getMembers(club);
        cache.removeClub(club);

        for (UUID uuid : members) {
            Player member = Bukkit.getPlayer(uuid);

            if (member != null && member.isOnline()) {
                messages.send(member, "disbanded-member");
            }
        }

        messages.send(player, "disbanded-self", Collections.singletonMap("club", club));
    }

    public void kick(Player sender, String targetName) {
        String club = getPlayerClub(sender.getUniqueId());

        if (club == null) {
            messages.send(sender, "not-in-club");
            return;
        }

        if (!isStaff(sender.getUniqueId(), club)) {
            messages.send(sender, "only-staff-kick");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = target.getUniqueId();

        String targetClub = getPlayerClub(targetUuid);

        if (targetClub == null || !targetClub.equalsIgnoreCase(club)) {
            messages.send(sender, "target-not-in-club");
            return;
        }

        if (isOwner(targetUuid, club)) {
            messages.send(sender, "cannot-kick-owner");
            return;
        }

        if (!isOwner(sender.getUniqueId(), club) && isAdmin(targetUuid, club)) {
            messages.send(sender, "cannot-kick-admin");
            return;
        }

        String realTargetName = target.getName() != null ? target.getName() : targetName;

        removeAdmin(targetUuid, club);
        removeMember(targetUuid);

        messages.send(sender, "kicked-player", replacements(
                "player", realTargetName,
                "club", club
        ));

        Player onlineTarget = Bukkit.getPlayer(targetUuid);

        if (onlineTarget != null && onlineTarget.isOnline()) {
            messages.send(onlineTarget, "kicked-notify", Collections.singletonMap("club", club));
        }

        broadcastToClub(club, "club-kick-broadcast", replacements(
                "sender", sender.getName(),
                "player", realTargetName,
                "club", club
        ));
    }

    public void promoteAdmin(Player sender, String targetName) {
        String club = getPlayerClub(sender.getUniqueId());

        if (club == null) {
            messages.send(sender, "not-in-club");
            return;
        }

        if (!isOwner(sender.getUniqueId(), club)) {
            messages.send(sender, "only-owner-admin");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = target.getUniqueId();

        String targetClub = getPlayerClub(targetUuid);

        if (targetClub == null || !targetClub.equalsIgnoreCase(club)) {
            messages.send(sender, "target-not-in-club");
            return;
        }

        if (isOwner(targetUuid, club)) {
            messages.send(sender, "owner-already-highest");
            return;
        }

        if (isAdmin(targetUuid, club)) {
            messages.send(sender, "already-admin");
            return;
        }

        cache.addAdmin(club, targetUuid);

        String realTargetName = target.getName() != null ? target.getName() : targetName;

        messages.send(sender, "admin-promoted", replacements(
                "player", realTargetName,
                "club", club
        ));

        Player onlineTarget = Bukkit.getPlayer(targetUuid);

        if (onlineTarget != null && onlineTarget.isOnline()) {
            messages.send(onlineTarget, "admin-promoted-notify", Collections.singletonMap("club", club));
        }

        broadcastToClub(club, "club-admin-promote-broadcast", replacements(
                "sender", sender.getName(),
                "player", realTargetName,
                "club", club
        ));
    }

    public void demoteAdmin(Player sender, String targetName) {
        String club = getPlayerClub(sender.getUniqueId());

        if (club == null) {
            messages.send(sender, "not-in-club");
            return;
        }

        if (!isOwner(sender.getUniqueId(), club)) {
            messages.send(sender, "only-owner-demote");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = target.getUniqueId();

        String targetClub = getPlayerClub(targetUuid);

        if (targetClub == null || !targetClub.equalsIgnoreCase(club)) {
            messages.send(sender, "target-not-in-club");
            return;
        }

        if (isOwner(targetUuid, club)) {
            messages.send(sender, "cannot-demote-owner");
            return;
        }

        if (!isAdmin(targetUuid, club)) {
            messages.send(sender, "not-admin");
            return;
        }

        removeAdmin(targetUuid, club);

        String realTargetName = target.getName() != null ? target.getName() : targetName;

        messages.send(sender, "admin-demoted", replacements(
                "player", realTargetName,
                "club", club
        ));

        Player onlineTarget = Bukkit.getPlayer(targetUuid);

        if (onlineTarget != null && onlineTarget.isOnline()) {
            messages.send(onlineTarget, "admin-demoted-notify", Collections.singletonMap("club", club));
        }

        broadcastToClub(club, "club-admin-demote-broadcast", replacements(
                "sender", sender.getName(),
                "player", realTargetName,
                "club", club
        ));
    }

    public void toggleChat(Player player) {
        String club = getPlayerClub(player.getUniqueId());

        if (club == null) {
            messages.send(player, "not-in-club");
            return;
        }

        boolean enabled = chatToggleService.toggle(player.getUniqueId());
        messages.send(player, enabled ? "club-chat-enabled" : "club-chat-disabled");
    }

    public void sendClubChat(Player sender, String message) {
        String club = getPlayerClub(sender.getUniqueId());

        if (club == null) {
            return;
        }

        boolean useLuckPermsPrefix = plugin.getConfig().getBoolean("chat.use-luckperms-prefix", true);
        String prefix = useLuckPermsPrefix ? LuckPermsHook.getPrefix(sender) : "";

        String format = plugin.getConfig().getString(
                "chat.format",
                "&bClub &7» %prefix%%player%&7: &f%message%"
        );

        String finalMessage = ColorUtil.color(format
                .replace("%club%", club)
                .replace("%prefix%", prefix)
                .replace("%player%", sender.getName())
                .replace("%message%", message));

        for (UUID memberUuid : getMembers(club)) {
            Player member = Bukkit.getPlayer(memberUuid);

            if (member != null && member.isOnline()) {
                member.sendMessage(finalMessage);
            }
        }
    }

    public void sendInfo(Player player, String clubInput) {
        String club = getRealClubName(clubInput);

        if (club == null) {
            messages.send(player, "club-not-found");
            return;
        }

        sendClubDisplay(player, club, true);
    }

    public List<String> getClubNames() {
        List<String> names = new ArrayList<>();
        for (ClubCache.ClubData clubData : cache.getAllClubs()) {
            names.add(clubData.name);
        }
        Collections.sort(names);
        return names;
    }

    public void sendList(Player player) {
        String club = getPlayerClub(player.getUniqueId());

        if (club == null) {
            messages.send(player, "not-in-club");
            return;
        }

        sendClubDisplay(player, club, false);
    }

    private void sendClubDisplay(Player player, String club, boolean infoMode) {
        UUID ownerUuid = getOwner(club);

        String ownerName = "Unknown";

        if (ownerUuid != null) {
            ownerName = formatPlayer(ownerUuid);
        }

        List<UUID> admins = getAdmins(club);

        List<UUID> normalMembers = getMembers(club).stream()
                .filter(uuid -> ownerUuid == null || !uuid.equals(ownerUuid))
                .filter(uuid -> !admins.contains(uuid))
                .collect(Collectors.toList());

        String open = isOpen(club) ? "Yes" : "No";
        String prefix = infoMode ? "info" : "list";

        player.sendMessage(messages.format(prefix + "-header", Collections.singletonMap("club", club)));
        player.sendMessage(messages.format(prefix + "-owner", Collections.singletonMap("owner", ownerName)));

        if (infoMode) {
            player.sendMessage(messages.format("info-open", Collections.singletonMap("open", open)));
        }

        Map<String, String> adminReplacements = new HashMap<>();
        adminReplacements.put("count", String.valueOf(admins.size()));
        adminReplacements.put("admins", formatPlayerList(admins));
        player.sendMessage(messages.format(prefix + "-admins", adminReplacements));

        Map<String, String> memberReplacements = new HashMap<>();
        memberReplacements.put("count", String.valueOf(normalMembers.size()));
        memberReplacements.put("members", formatPlayerList(normalMembers));
        player.sendMessage(messages.format(prefix + "-members", memberReplacements));
    }

    public void renameClub(Player player, String newName) {
        String currentClub = getPlayerClub(player.getUniqueId());

        if (currentClub == null) {
            messages.send(player, "not-in-club");
            return;
        }

        if (!isOwner(player.getUniqueId(), currentClub)) {
            messages.send(player, "only-owner-rename");
            return;
        }

        // Validate the new name with the same rules as club create
        int min = plugin.getConfig().getInt("club.name-min-length", 3);
        int max = plugin.getConfig().getInt("club.name-max-length", 16);
        String regex = plugin.getConfig().getString("club.name-regex", "^[A-Za-z0-9_]+$");

        if (newName.length() < min || newName.length() > max || !newName.matches(regex)) {
            messages.send(player, "invalid-club-name");
            return;
        }

        if (clubExists(newName)) {
            messages.send(player, "club-name-exists");
            return;
        }

        // Update cache - need to handle the rename
        ClubCache.ClubData clubData = cache.getClub(currentClub);
        if (clubData != null) {
            // Store the data we need before removing
            Set<UUID> members = new HashSet<>(clubData.members);
            Set<UUID> admins = new HashSet<>(clubData.admins);
            List<ClubInvite> invitesToUpdate = new ArrayList<>();
            
            for (ClubInvite invite : cache.getAllInvites()) {
                if (invite.getClubName().equals(currentClub)) {
                    invitesToUpdate.add(invite);
                }
            }
            
            // Remove old mappings
            cache.removeClub(currentClub);
            
            // Add new mappings
            clubData.name = newName;
            clubData.normalizedName = normalizeClubName(newName);
            cache.addClub(newName, clubData.normalizedName, clubData.ownerUuid, clubData.open, clubData.color);
            
            // Update member mappings
            for (UUID member : members) {
                cache.addMember(newName, member);
            }
            
            // Update admin mappings
            for (UUID admin : admins) {
                cache.addAdmin(newName, admin);
            }
            
            // Update invites
            for (ClubInvite invite : invitesToUpdate) {
                cache.addInvite(invite.getPlayerUuid(), new ClubInvite(invite.getPlayerUuid(), newName, invite.getExpiresAt(), invite.getInviterUuid()));
            }
        }

        messages.send(player, "club-renamed", replacements(
                "old-name", currentClub,
                "new-name", newName
        ));

        broadcastToClub(newName, "club-rename-broadcast", replacements(
                "sender", player.getName(),
                "old-name", currentClub,
                "new-name", newName
        ));
    }

    public void enforceMemberLimit() {
        for (ClubCache.ClubData clubData : cache.getAllClubs()) {
            int memberCount = clubData.members.size();
            if (memberCount > 40) {
                String clubName = clubData.name;
                UUID ownerUuid = clubData.ownerUuid;
                Set<UUID> admins = new HashSet<>(clubData.admins);
                
                // Get members to remove (exclude owner and admins first, then regular members)
                List<UUID> membersToRemove = new ArrayList<>();
                List<UUID> regularMembers = new ArrayList<>();
                
                for (UUID member : clubData.members) {
                    if (!member.equals(ownerUuid) && !admins.contains(member)) {
                        regularMembers.add(member);
                    }
                }
                
                // Sort regular members to remove the most recent ones (or any deterministic order)
                // We'll remove from the end of the list
                int excess = memberCount - 40;
                int toRemove = Math.min(excess, regularMembers.size());
                
                for (int i = regularMembers.size() - 1; i >= regularMembers.size() - toRemove; i--) {
                    membersToRemove.add(regularMembers.get(i));
                }
                
                // If still need to remove more (unlikely since owner + admins are usually few), remove admins
                if (membersToRemove.size() < excess) {
                    List<UUID> adminList = new ArrayList<>(admins);
                    int remainingToRemove = excess - membersToRemove.size();
                    for (int i = adminList.size() - 1; i >= adminList.size() - remainingToRemove && i >= 0; i--) {
                        membersToRemove.add(adminList.get(i));
                    }
                }
                
                // Remove the excess members
                for (UUID uuid : membersToRemove) {
                    removeAdmin(uuid, clubName);
                    removeMember(uuid);
                    
                    String playerName = getOfflineName(uuid);
                    plugin.getLogger().info("Removed " + playerName + " from club " + clubName + " due to member limit (40 max).");
                    
                    // Announce to the club
                    broadcastToClub(clubName, "member-limit-remove", replacements(
                            "player", playerName,
                            "club", clubName
                    ));
                }
                
                plugin.getLogger().info("Enforced member limit for club " + clubName + ": removed " + membersToRemove.size() + " members.");
            }
        }
    }
}
