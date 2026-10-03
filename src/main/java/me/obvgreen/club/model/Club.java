package me.obvgreen.club.model;

import java.util.UUID;

public class Club {

    private final String name;
    private final UUID owner;
    private final boolean open;

    public Club(String name, UUID owner, boolean open) {
        this.name = name;
        this.owner = owner;
        this.open = open;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isOpen() {
        return open;
    }
}
