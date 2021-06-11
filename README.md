# QuickShot
Minecraft (Bukkit/Spigot) Mini-Game: Defend yourself with a Bow and eliminate everyone else!

## How To Play
...

## Setup
Follow these steps to setup and configure "QuickShot" on your server:

1. Download the plugin: Grab the current release from [here](https://github.com/MarvinMenzerath/spigot-quickshot/releases) and put it in your plugins-folder.
2. Download Multiverse: Grab the current release from [here](https://dev.bukkit.org/projects/multiverse-core) and put it in your plugins-folder.
3. Upload the maps: Upload the maps you want to play on and name them `qs-wX`, where "X" has to be a unique number. This will be the id of the arena. Example: `qs-w1`
This can be changed in the config-file (see `gameWorldPrefix`).
4. Now start your server and import the worlds. Example: `/mvimport qs-w1 normal`  After you did this for every arena, edit the Multiverse-world-config and change the following parameters (if you want to):
  * `allowweather: false`
  * `difficulty: PEACEFUL`
  * `animals:
      spawn: false`
  * `monsters:
      spawn: false`
  * `hunger: false`
5. Reload the Multiverse-Config: `/mvreload`
6. Set a DTA-Spawn: `/qs setlobby` You will spawn there if you type `/qs lobby`.
7. Configure the first arena:
  1. Teleport there: `/mvtp qs-w1`
  2. Now set every spawn (exactly 6!): Go to the place you want the players to spawn and type in `/qs setspawn [MAP] [SPAWN]`. Example: `/qs setspawn 1 1`
  3. Enable the arena: `/qs enable 1`.
  4. Place a sign to join the arena (every new line represents a line of the sign):
    1. `QuickShot`
    2. `__EMPTY__`
    3. `qs join [ARENA-ID]`
    4. `[MAP-NAME]`

## Commands

### User
...

### Admin / OP
...

## Permissions
...
