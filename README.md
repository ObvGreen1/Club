# Club

> [!IMPORTANT]
> **This plugin is no longer supported.** This 1.8 branch is frozen and will
> receive no further bug fixes, features or Spigot API updates. A newer rewrite
> is being prepared and will target **26.2** instead of 1.8. Expect a fresh
> release soon — do not plan new features against this version.

Club system plugin for Spigot 1.8.8+ (Bukkit). Players create clubs, invite
friends, promote admins, and talk in a private club chat. Data lives in SQLite,
so no external database server is needed.

## Features

- Clubs with owner / admin / member roles
- Invite system with configurable expiry (`/club accept`, `/club accept <player>`)
- Open or closed clubs, public join via `/club join <name>`
- Per-club colour used in chat, menus and PlaceholderAPI
- Private club chat with `/club chat` toggle or the `/cc` shortcut
- GUI menu (`/club menu`) and a colour picker (`/club color`)
- SQLite storage with an in-memory cache flushed every 30 minutes and on shutdown
- Optional PlaceholderAPI and LuckPerms integration

## Requirements

| Dependency   | Required | Notes                          |
|--------------|----------|--------------------------------|
| Spigot/Bukkit| yes      | 1.8.8-R0.1 only (frozen branch)|
| SQLite JDBC  | bundled  | `libs/sqlite-jdbc-3.53.0.0.jar`|
| PlaceholderAPI| no      | `libs/PlaceholderAPI-2.11.3.jar` |
| LuckPerms    | no       | `libs/LuckPerms-Bukkit-5.4.158.jar` |

Placeholders and LuckPerms prefixes are only used when those plugins are
present. The plugin still enables without them.

Builds target Java 8 and the 1.8.8 Spigot API. The upcoming release moves to
26.2 and will require a newer JDK.

## Build

The Maven build depends on three vendored jars under `libs/` that are declared
with `system` scope in [pom.xml](pom.xml). They are committed on purpose — there
is no Maven repository to fetch them from, so a fresh clone still builds.

```bash
mvn clean package
```

Output: `target/Club-1.0.jar`. Drop it into `plugins/` and restart the server.

## Commands

Root command: `/club` (alias `/clubs`). Club chat toggle: `/cc`.

| Command | Permission | Description |
|---|---|---|
| `/club create <name>` | — | Create a club |
| `/club invite <player>` | — | Invite a player to your club |
| `/club accept <player>` | — | Accept that player's invitation |
| `/club join <name>` | — | Join an open club |
| `/club open` | owner | Open the club to public joins |
| `/club close` | owner | Close the club |
| `/club kick <player>` | owner / admin | Kick a member |
| `/club leave` | — | Leave your club |
| `/club disband` | owner | Disband the club |
| `/club admin <player>` | owner | Promote a member to admin |
| `/club demote <player>` | owner | Demote an admin |
| `/club rename <name>` | owner | Rename the club |
| `/club color` | owner | Open the colour picker GUI |
| `/club menu` | — | Open the club management GUI |
| `/club chat [message...]` | — | Toggle club chat, or send one message |
| `/club list` | — | List your club members |
| `/club info <club>` | — | Show another club's info |
| `/club reload` | `club.reload` or `club.admin` | Reload config and restart services |
| `/club` | — | Show the help menu |

`/club reload` works from the console; every other subcommand is player-only.
Player subcommands share a 15-second cooldown. There is no `/club help`
subcommand — bare `/club` prints the help list.

Permissions declared in [plugin.yml](src/main/resources/plugin.yml):

```yaml
club.create:  # default: false — declared but NOT checked in code
club.reload:  # default: op — checked for /club reload
club.admin:   # default: op — checked for /club reload
```

Owner, admin and staff restrictions are enforced in Java against the club
roster, not by permission nodes. To stop players creating clubs, either patch
`ClubCommand` to check `club.create`, or remove the `create` case.

## Configuration

[config.yml](src/main/resources/config.yml) is copied to
`plugins/ClubPlugin/config.yml` on first start. Only values that differ from
the compiled-in defaults are listed — everything else falls back to a default
inside the Java code.

| Key | Default | Meaning |
|---|---|---|
| `default-club-color` | `&b` | Colour used when a club has none |
| `database.file` | `clubs.db` | SQLite file inside the plugin folder |
| `placeholder.no-club` | `""` | Value `%club_current_club%` returns for clubless players |
| `club.name-min-length` | `3` | Minimum club name length |
| `club.name-max-length` | `16` | Maximum club name length |
| `club.name-regex` | `^[A-Za-z0-9_]+$` | Allowed club name characters |
| `club.invite-expire-seconds` | `300` | Invite lifetime |
| `chat.use-luckperms-prefix` | `true` | Pull the sender prefix from LuckPerms |
| `chat.format` | see config | `%club%`, `%prefix%`, `%player%`, `%message%` |
| `messages.prefix` | `&8[&3Club&8] &r` | Prepended to every message |
| `gui.club-color.*` | see config | Colour picker layout, slots, materials |

Colours use `&`-codes and are translated by
[ColorUtil](src/main/java/me/obvgreen/club/util/ColorUtil.java).

## PlaceholderAPI

Registered automatically when PlaceholderAPI is installed. Identifier: `club`.

| Placeholder | Returns |
|---|---|
| `%club_current_club%` | `[ColoredClubName]` or `placeholder.no-club` |
| `%club_online_members%` | Number of online members, or `placeholder.no-club` |

## Database

SQLite file `plugins/ClubPlugin/clubs.db`. Clubs, members and roles are cached
in memory; the cache is written back on a 30-minute timer, on `/club reload`,
and on plugin disable. Member cap is 40 per club; players over the cap are
removed automatically on startup.

## Project layout

```
src/main/java/me/obvgreen/club/
  ClubPlugin.java          bootstrap, reload, shutdown
  cache/                   in-memory club data
  command/                 /club and /cc executors
  config/                  MessageService
  database/                SQLite access
  hook/                    PlaceholderAPI + LuckPerms
  listener/                club chat routing
  menu/                    club menu and colour picker GUIs
  model/                   Club, ClubInvite
  service/                 club + chat-toggle logic
  util/                    colour translation
```

## License

[MIT](LICENSE) — Copyright (c) 2025 obvgreen.