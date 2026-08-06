# NA

A simple TPA plugin for your Minecraft server — players can request teleports,
accept or deny them, and toggle a few handy options. Based on the open-source
[DonutTpa v13](https://modrinth.com/plugin/tpa-donut) by SqidgeonStudios, with a
couple of bugfixes on top.

---

## What it does

Players can ask to teleport to each other, accept or deny requests, and even
toggle things like auto-accept. It has chat messages, actionbar messages,
titles, sounds, and a couple of handy GUIs.

## Commands

| Command | What it does |
| --- | --- |
| `/tpa <player>` | Ask to teleport to a player |
| `/tpahere <player>` | Ask a player to teleport to you |
| `/tpaccept` | Accept a pending request |
| `/tpadeny` | Deny a pending request |
| `/tpacancel` | Cancel a request you sent |
| `/tpatoggle` | Turn incoming TPA requests on/off |
| `/tpaheretoggle` | Turn incoming TPA-here requests on/off |
| `/tpaauto` | Auto-accept all requests (`/tpauto` works too) |
| `/tpaguitoggle` | Turn the request GUIs on/off |
| `/tpareload` | Reload the plugin (admin) |

## Configuration

Everything lives in the plugin's data folder (`plugins/NA/`).
The code lives under the `github.io.Frenxys` package:

- `tpa-config.yml` — main settings
- `tpa-messages.yml` — all the messages (chat, actionbar, titles)
- `tpa-sounds.yml` — sounds
- `tpa-request-cooldown.yml` — request cooldown
- `tpa-world-nick.yml` — world nicknames
- `tpa-gui/` — the GUI layouts

Player settings are stored in an SQLite database (`data.db`).

## Building

You need **JDK 25** on your machine. Then, from the project folder:

```bash
./gradlew build
```

You'll find the plugin at `build/libs/NA-1.0.jar`. Drop it in your server's
`plugins/` folder and restart.

Or open the project in IntelliJ IDEA and run the `build` task from the Gradle
tool window — same result.

The jar runs on Minecraft 1.21+ all the way up to 26.2 (it compiles with JDK 25
but targets Java 21 bytecode). Want to build for a different Minecraft version?
Just change `paperVersion` in `gradle.properties` and rebuild.

## Credits

Made by **Enea** ([github.com/Frenxys](https://github.com/Frenxys)).

The plugin is based on the open-source **DonutTpa v13** by
[SqidgeonStudios](https://modrinth.com/plugin/tpa-donut) — their credits are
kept in `plugin.yml`.
