package me.obvgreen.club.model;

import java.util.UUID;

public class ClubInvite {

    private final UUID playerUuid;
    private final String clubName;
    private final long expiresAt;
    private final UUID inviterUuid;

    public ClubInvite(UUID playerUuid, String clubName, long expiresAt, UUID inviterUuid) {
        this.playerUuid = playerUuid;
        this.clubName = clubName;
        this.expiresAt = expiresAt;
        this.inviterUuid = inviterUuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getClubName() {
        return clubName;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public UUID getInviterUuid() {
        return inviterUuid;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
