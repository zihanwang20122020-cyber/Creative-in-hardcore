# Creative-in-hardcore
Hardcore Creative is a Fabric server-side mod that gives the recorded world owner controlled creative access without making every operator, LAN guest, command block, console source, or server player a mod
  administrator.

  It is designed for singleplayer worlds, LAN worlds, and dedicated Fabric servers. The same jar works in all supported environments. On dedicated servers, vanilla clients can connect without installing the mod.

  ## What It Does

  Hardcore Creative records a per-world owner UUID and uses that player as the trusted administrator for creative access. The owner can grant, revoke, enable, or disable creative access for selected players.

  Authorized players can use their own creative access, but they cannot grant access to other players unless they are the recorded world owner.

  ## Features

  - Works in singleplayer, LAN worlds, and dedicated Fabric servers.
  - Server-side only for multiplayer servers.
  - Compatible with vanilla clients on dedicated servers.
  - Works in survival, creative, adventure, and hardcore worlds.
  - Uses one jar for all supported environments.
  - Gives the recorded world owner real server-side permissions through the mod.
  - Allows authorized players to use creative mode.
  - Supports the vanilla F3+F4 game mode switcher for authorized players.
  - Stores owner, authorization, and creative restore state per world/server.
  - Prevents non-owner players from granting creative access to others.
  - Prevents command blocks from administering the mod.
  - Keeps console and RCON from becoming implicit mod administrators.

  ## Ownership System

  In singleplayer and LAN, the Minecraft singleplayer owner becomes the mod owner.

  On a dedicated server, if the world has no saved owner yet, the first player who initializes the world becomes the recorded owner. After that, administration is locked to that UUID.

  This means operators do not automatically become mod administrators. Only the recorded owner controls who gets creative access.

  ## Commands

  Use `/hccreative` or `/hardcorecreative`.

  - `/hccreative status [player]`
  - `/hccreative grant <player>`
  - `/hccreative revoke <player>`
  - `/hccreative enable <player>`
  - `/hccreative disable <player>`
  - `/hccreative setmode creative`
  - `/hccreative setmode survival`
  - `/hccreative list`
  - `/hccreative reload`

  ## Installation

  ### Singleplayer

  Install the jar in your client `mods` folder.

  ### LAN

  Install the jar in the LAN host’s `mods` folder.

  ### Dedicated Server

  Install the jar in the server `mods` folder.

  Players joining the dedicated server do not need to install the mod.

  ## Requirements

  - Minecraft `26+`
  - Fabric Loader `0.19+`
  - Fabric API `0.154.0+26+`
  - Java `25+`

  ## Configuration

  The config file is created at:

  ```text
  config/hardcorecreative.properties

  Main options:

  - allowCreativeAccess=true
  - autoAuthorizeSingleplayerOwner=true
  - autoRestoreCreativeOnLogin=true
  - forceSurvivalOnRevoke=true
  - verboseLogging=false

  ## Compatibility

  Hardcore Creative is made for Fabric and Minecraft 26.2.

  It is intended for:

  - singleplayer
  - LAN worlds
  - dedicated Fabric servers

  For dedicated servers, mark the mod as server-side only.
