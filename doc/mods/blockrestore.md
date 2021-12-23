# BlockRestore

## About

This mod restores blocks (not entities) of BATTLE [region](../regions.md) after the match.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.

## Config settings

- hard \- the mod will restore EVERY block of your battle region, regardless of a known changed state (default: false)
- offset \- the time in TICKS (1/20 second) that the scheduler waits for the next block set to be replaced (default: 1)
- restoreblocks \- restore blocks (default: true)
- restorecontainers \- restore containers (chests, furnaces, brewing stands, etc) content (default: false) 
- restoreinteractions \- restore player interactions with blocks like opened doors or toggled levers (default: false) 

## Commands

- `/pa [arena] !br hard` \- toggle the hard setting
- `/pa [arena] !br restoreblocks` \- toggle the restoreblocks setting
- `/pa [arena] !br restorecontainers` \- toggle the restorecontainers setting
- `/pa [arena] !br restoreinteractions` \- toggle the restoreinteractions setting
- `/pa [arena] !br clearinv` \- clear saved chest locations
- `/pa [arena] !br offset X` \- set the restore offset in TICKS! 

<br>

> **🚩 Tips:**  
> - If you add new chests to your map, don't forget to register them with `/pa [arena] !br clearinv`.
> - BlockRestore is designed for block destruction, chest and block usage only. If you need advanced restoring 
>   (especially entities), please prefer [WorldEdit](./worldedit.md) mod. 

<br>

> ⚙ **Technical precisions:**  
> - BlockRestore is fully asynchronous and may take some time restore the battlefield (few seconds in most cases). 
    This one is not reachable during the process.
> - Chest restoring lags badly for the first time, because it searches the BATTLE region(s) for chests and saves location 
>   of each of them.
> - Due to API limitations on servers that are **not based on PaperMC**, destruction of linked blocks (like wall torches
>   when the support block is broken) is limited to doors and non solid blocks on the top of the support block, along 
>   with directional blocks on the other faces.



