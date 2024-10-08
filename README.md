
# TimeLimit Plugin for Spigot

This Spigot plugin allows the management of players playtime (screentime). It was created for the minecraft server of my school (Goetheschule Essen) to manage how long students can play.

## Features

- Customizable timelimits for every player
- Automatic assignment of timelimits based on grade
- Disabling and enabling of timelimits without removing the limit itself
- Toggleable scoreboard with information on playtime and playtime left
- Warnings when playtime is about to run out
- Kicking players when playtime has run out

## Installation

1) Download the latest release (.jar) from [releases](https://github.com/TillOttmann/TimeLimit/releases)
2) Put the .jar inside the plugins folder
3) Run the server once OR create the config.yml file manually (server_dir/plugins/TimeLimit/config.yml)
4) Add your database connection inside config.yml

```yml
DB_Url: '[URL]:[PORT]'
DB_User: '[USER]'
DB_Pw: '[PASSWORD]'
```
5) You are done! Just start the server, the database will be created automatically
    
## Usage

Set the timelimit of any player, target beeing the player name and value the timelimit in minutes
```
/timelimit set <target> <value>
```
Get the timelimit of any player
```
/timelimit get <target>
```
Get the timelimit of any player
```
/timelimit get <target>
```
Disable/enable the timelimit of any player
```
/timelimit disable <target>
/timelimit enable <target>
```
Show/hide the scoreboard
```
/timelimit show
/timelimit hide
```
## Used Resources

 - [Spigot API using BuildTools](https://www.spigotmc.org/wiki/buildtools/)
 - [FastBoard made by MRMicky-FR (Scoreboard API)](https://github.com/MrMicky-FR/FastBoard)

