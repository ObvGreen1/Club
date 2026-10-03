package me.obvgreen.club.database;

import me.obvgreen.club.ClubPlugin;

import java.io.File;
import java.sql.*;

public class DatabaseManager {

    private final ClubPlugin plugin;
    private Connection connection;

    public DatabaseManager(ClubPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File folder = plugin.getDataFolder();

            if (!folder.exists()) {
                folder.mkdirs();
            }

            String fileName = plugin.getConfig().getString("database.file", "clubs.db");
            File dbFile = new File(folder, fileName);

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA foreign_keys = ON;");
                statement.executeUpdate("PRAGMA journal_mode = WAL;");
                statement.executeUpdate("PRAGMA synchronous = NORMAL;");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS clubs (" +
                        "name TEXT PRIMARY KEY," +
                        "normalized_name TEXT UNIQUE," +
                        "owner_uuid TEXT NOT NULL," +
                        "open INTEGER NOT NULL DEFAULT 0" +
                        ");");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS club_members (" +
                        "club_name TEXT NOT NULL," +
                        "player_uuid TEXT NOT NULL UNIQUE," +
                        "PRIMARY KEY (club_name, player_uuid)" +
                        ");");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS club_admins (" +
                        "club_name TEXT NOT NULL," +
                        "player_uuid TEXT NOT NULL," +
                        "PRIMARY KEY (club_name, player_uuid)" +
                        ");");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS club_invites (" +
                        "player_uuid TEXT PRIMARY KEY," +
                        "club_name TEXT NOT NULL," +
                        "expires_at INTEGER NOT NULL" +
                        ");");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS club_chat_toggles (" +
                        "player_uuid TEXT PRIMARY KEY," +
                        "enabled INTEGER NOT NULL DEFAULT 0" +
                        ");");
            }

            migrateNormalizedNames();
            migrateClubAdmins();
            migrateColors();
            migrateInviterUuid();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize SQLite database", ex);
        }
    }

    private void migrateColors() {
        try {
            if (!columnExists("clubs", "color")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE clubs ADD COLUMN color TEXT DEFAULT '&r';");
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not migrate club colors table: " + ex.getMessage());
        }
    }

    private void migrateNormalizedNames() {
        try {
            if (!columnExists("clubs", "normalized_name")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE clubs ADD COLUMN normalized_name TEXT;");
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE clubs SET normalized_name = LOWER(name) WHERE normalized_name IS NULL"
            )) {
                ps.executeUpdate();
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_clubs_normalized_name ON clubs(normalized_name);");
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not migrate normalized club names: " + ex.getMessage());
        }
    }

    private void migrateClubAdmins() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS club_admins (" +
                    "club_name TEXT NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "PRIMARY KEY (club_name, player_uuid)" +
                    ");");
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not migrate club admins table: " + ex.getMessage());
        }
    }

    private void migrateInviterUuid() {
        try {
            if (!columnExists("club_invites", "inviter_uuid")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE club_invites ADD COLUMN inviter_uuid TEXT;");
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not migrate inviter uuid column: " + ex.getMessage());
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                init();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to access SQLite connection", ex);
        }

        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }

            connection = null;
        }
    }
}
