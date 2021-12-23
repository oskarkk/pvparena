# Squads

## Description

This mod allow players to make squads. Instead of respawning anywhere / at the team spawn, you spawn at a random squad member.

Squads are an addition to teams or even a replacement (in case of a FFA arena).

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Usage

Place signs, similar to the Class Signs:

```
[Squad name]
[ignored]
free
free
```

You can add another (one) sign below for display of more player names

## Config settings ( in your arena config file )

- modules.squads.ingameSquadSwitch \- allow switching squads ingame

## Commands

- `/pa [arena] !sq` \- show the arena squads
- `/pa [arena] !sq add [name] [limit]` \- add squad with player limit (set to 0 to remove limit)
- `/pa [arena] !sq remove [name]` \- remove squad [name]
- `/pa [arena] !sq set [name] [limit]` \- set player limit for squad

## Warnings

\-

## Dependencies

\-
