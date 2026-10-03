package me.obvgreen.club.service;

import me.obvgreen.club.cache.ClubCache;

import java.util.UUID;

public class ChatToggleService {

    private final ClubCache cache;

    public ChatToggleService(ClubCache cache) {
        this.cache = cache;
    }

    public boolean isEnabled(UUID uuid) {
        Boolean enabled = cache.getChatToggle(uuid);
        return enabled != null && enabled;
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        cache.setChatToggle(uuid, enabled);
    }

    public boolean toggle(UUID uuid) {
        boolean newState = !isEnabled(uuid);
        setEnabled(uuid, newState);
        return newState;
    }
}
