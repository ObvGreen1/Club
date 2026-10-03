package me.obvgreen.club.cache;

import me.obvgreen.club.model.ClubInvite;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClubCache {

    private final Map<String, ClubData> clubs = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerToClub = new ConcurrentHashMap<>();
    private final Map<UUID, ClubInvite> invites = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> chatToggles = new ConcurrentHashMap<>();

    public static class ClubData {
        public String name;
        public String normalizedName;
        public UUID ownerUuid;
        public boolean open;
        public String color;
        public final Set<UUID> members = ConcurrentHashMap.newKeySet();
        public final Set<UUID> admins = ConcurrentHashMap.newKeySet();

        public ClubData(String name, String normalizedName, UUID ownerUuid, boolean open, String color) {
            this.name = name;
            this.normalizedName = normalizedName;
            this.ownerUuid = ownerUuid;
            this.open = open;
            this.color = color;
        }
    }

    public void addClub(String name, String normalizedName, UUID ownerUuid, boolean open, String color) {
        ClubData clubData = new ClubData(name, normalizedName, ownerUuid, open, color);
        clubData.members.add(ownerUuid);
        clubs.put(name, clubData);
        clubs.put(normalizedName, clubData);
        playerToClub.put(ownerUuid, name);
    }

    public void removeClub(String name) {
        ClubData clubData = clubs.get(name);
        if (clubData != null) {
            for (UUID member : clubData.members) {
                playerToClub.remove(member);
            }
            clubs.remove(clubData.name);
            clubs.remove(clubData.normalizedName);
        }
    }

    public ClubData getClub(String name) {
        return clubs.get(name);
    }

    public boolean clubExists(String name) {
        return clubs.containsKey(name);
    }

    public String getRealClubName(String input) {
        ClubData clubData = clubs.get(input.toLowerCase());
        return clubData != null ? clubData.name : null;
    }

    public void addMember(String clubName, UUID playerUuid) {
        ClubData clubData = clubs.get(clubName);
        if (clubData != null) {
            clubData.members.add(playerUuid);
            playerToClub.put(playerUuid, clubName);
        }
    }

    public void removeMember(UUID playerUuid) {
        String clubName = playerToClub.get(playerUuid);
        if (clubName != null) {
            ClubData clubData = clubs.get(clubName);
            if (clubData != null) {
                clubData.members.remove(playerUuid);
                clubData.admins.remove(playerUuid);
            }
            playerToClub.remove(playerUuid);
        }
    }

    public void addAdmin(String clubName, UUID playerUuid) {
        ClubData clubData = clubs.get(clubName);
        if (clubData != null) {
            clubData.admins.add(playerUuid);
        }
    }

    public void removeAdmin(String clubName, UUID playerUuid) {
        ClubData clubData = clubs.get(clubName);
        if (clubData != null) {
            clubData.admins.remove(playerUuid);
        }
    }

    public Set<UUID> getMembers(String clubName) {
        ClubData clubData = clubs.get(clubName);
        return clubData != null ? new HashSet<>(clubData.members) : new HashSet<>();
    }

    public Set<UUID> getAdmins(String clubName) {
        ClubData clubData = clubs.get(clubName);
        return clubData != null ? new HashSet<>(clubData.admins) : new HashSet<>();
    }

    public String getPlayerClub(UUID uuid) {
        return playerToClub.get(uuid);
    }

    public boolean isInClub(UUID uuid) {
        return playerToClub.containsKey(uuid);
    }

    public UUID getOwner(String clubName) {
        ClubData clubData = clubs.get(clubName);
        return clubData != null ? clubData.ownerUuid : null;
    }

    public boolean isOwner(UUID uuid, String clubName) {
        ClubData clubData = clubs.get(clubName);
        return clubData != null && clubData.ownerUuid.equals(uuid);
    }

    public boolean isAdmin(UUID uuid, String clubName) {
        ClubData clubData = clubs.get(clubName);
        return clubData != null && clubData.admins.contains(uuid);
    }

    public boolean isOpen(String clubName) {
        ClubData clubData = clubs.get(clubName);
        return clubData != null && clubData.open;
    }

    public void setOpen(String clubName, boolean open) {
        ClubData clubData = clubs.get(clubName);
        if (clubData != null) {
            clubData.open = open;
        }
    }

    public String getClubColor(String clubName) {
        ClubData clubData = clubs.get(clubName);
        return clubData != null ? clubData.color : null;
    }

    public void setClubColor(String clubName, String color) {
        ClubData clubData = clubs.get(clubName);
        if (clubData != null) {
            clubData.color = color;
        }
    }

    public void addInvite(UUID playerUuid, ClubInvite invite) {
        invites.put(playerUuid, invite);
    }

    public ClubInvite getInvite(UUID uuid) {
        return invites.get(uuid);
    }

    public void removeInvite(UUID uuid) {
        invites.remove(uuid);
    }

    public void setChatToggle(UUID uuid, boolean enabled) {
        chatToggles.put(uuid, enabled);
    }

    public Boolean getChatToggle(UUID uuid) {
        return chatToggles.get(uuid);
    }

    public void removeChatToggle(UUID uuid) {
        chatToggles.remove(uuid);
    }

    public Collection<ClubData> getAllClubs() {
        Set<ClubData> uniqueClubs = new HashSet<>();
        for (ClubData clubData : clubs.values()) {
            uniqueClubs.add(clubData);
        }
        return uniqueClubs;
    }

    public Collection<ClubInvite> getAllInvites() {
        return new ArrayList<>(invites.values());
    }

    public Map<UUID, Boolean> getAllChatToggles() {
        return new HashMap<>(chatToggles);
    }

    public void clear() {
        clubs.clear();
        playerToClub.clear();
        invites.clear();
        chatToggles.clear();
    }
}
