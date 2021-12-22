# Spawn Command

## Description

Set an arena spawn to your current position, including orientation !

## Usage

| Command                                                 | Definition                  |
|---------------------------------------------------------|-----------------------------|
| /pa [arena] spawn set (teamName) [spawnName] (class)    | Define a spawn for an arena |
| /pa [arena] spawn remove (teamName) [spawnName] (class) | Remove a spawn for an arena |

For multi-spawn, you can set everything as name, as long as name **start with** the spawn type.  
The spawn will be chosen randomly.

Example with type "exit":
- `/pa ctf spawn set exit` - sets exit spawn of the arena "ctf"

Example with type "spawn": 
- `/pa ctf spawn set red spawn` - sets the red team's spawn of the arena "ctf"
- `/pa ctf spawn remove red spawn2` - removes the second red team's spawn of the arena "ctf"
- `/pa free spawn set spawnEAST` - sets "spawnEAST" of the arena "free"
- `/pa ctf spawn set red spawn Pyro` - sets spawn only for Pyro class of red team

Example with type "lounge":
- `/pa ctf spawn set lounge red` - sets the red team's lounge spawn of the arena "ctf"
- `/pa free spawn set lounge` - sets lounge  of the arena "free"
- `/pa free spawn removed lounge` - removes lounge  of the arena "free"

## Details

There are two syntax according to the gamemode (free or teams) of your arena goal: 
- If you're using a "free" arena, you can define unlimited spawns using syntax `/pa myArena spawn set team spawnX` where X should
 be anything (word, digit, letter, etc).
- If your arena works with teams, you have to use `/pa myArena spawn set team spawn` where "team" is the name of one of your 
team.
- you can set spawn only for some class with `/pa myArena set team spawn class` where "class" is the name of one of your
  arena class.

If you get a message "spawn unknown", this is probably because you did not install/activate a [goal](../goals.md) or 
a [module](../modules.md). 
Be sure you have installed and activated stuff you want to add, for instance the "Flags" goal, or the "StandardSpectate" 
module...

## Spawn Offset

You can define unique offsets for each spawn name, in order to not be placed on the block center but rather one edge:

- `/pa [arena] spawn [spawnname] offset X Y Z`

For example 0.5 0 0.5 as X Y Z would work setting you on an edge. 
You might want to keep F3 at hand to see if you actually have to add or subtract to get to the right edge.
