# Permission nodes

The following nodes can be used:

Node |  Definition
------------- | -------------
pvparena.* | Gives access to all commands
pvparena.admin | Allows you to create and administrate arenas (default: op)
pvparena.create| Allows you to create and administrate your arenas (default: op)
pvparena.override| Allows you to override some shortcuts checks (default: op)
pvparena.telepass| Allows you to teleport while in an arena (default: op)
pvparena.user | Allows you to use the arena (default: everyone)

PVP Arena uses the SuperPerms interface, i.e. default bukkit permissions interface.


### Specific class permissions

If you set `explicitClassNeeded` to true in the arena config you will have to add permissions for each class, e.g.:

Node |  Definition
------------- | -------------
pvparena.class.ranger | Give Ranger class permission
pvparena.class.tank |  Give Tank class permission

The class name must be lowercase.


### Specific arena permissions

If you set `explicitArenaNeeded` to true in the arena config you will have to add permissions for each arena, e.g.:

Node |  Definition
------------- | -------------
pvparena.join.ctf | Give ctf join permission
pvparena.join.spleef | Give spleef join permission

The arena name must be lowercase.


### Specific command permissions

You can allow regular, non-op players to use specific commands. You can deny these permissions to prevent the use of any command. E.g.:

Node |  Definition
------------- | -------------
pvparena.cmds.playerjoin | Allows player to run [/pvparena playerjoin](commands/playerjoin.md)
pvparena.cmds.spawn | Allows player to modify [spawns](commands/spawn.md) of the arenas
-pvparena.cmds.remove | Prohibit [deletion](commands/remove.md) of the arenas
