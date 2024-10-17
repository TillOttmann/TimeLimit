package com.plugin.timelimit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;

class PlayerListener implements Listener {

	// Hier werden alle online Spieler nach Name und PlayerManager Object
	// gespeichert
	static HashMap<String, PlayerManager> playerManagers = new HashMap<String, PlayerManager>();
	static HashMap<String, TimeLimitScoreBoard> playerBoards = new HashMap<String, TimeLimitScoreBoard>();
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {

		// Erstellt das PlayerManager Object und fügt es zur HashMap hinzu
		playerManagers.putIfAbsent(e.getPlayer().getName(), new PlayerManager(e.getPlayer()));
		playerBoards.putIfAbsent(e.getPlayer().getName(), new TimeLimitScoreBoard(e.getPlayer()));
		

	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		
		// Deaktiviert alle Bukkit-Runnable loops und entfernt dann die Objekte aus den Hashmaps
		playerBoards.get(e.getPlayer().getName()).disableBoard();
		playerManagers.get(e.getPlayer().getName()).disconnectEvent();
		playerManagers.remove(e.getPlayer().getName());
		playerBoards.remove(e.getPlayer().getName());
		
	}
	
	// (De)Aktiviert das TimeLimit-Scoreboard
	// Das Scoreboard wird be deaktivierung gelöscht und der Bukkit-Runnable loop wird gestoppt,
	// es wird nicht nur versteckt, wie der Befehl 'hide/show' zu vermuten lässt
	static void changeScoreBoardStatus(Player player, boolean status) {
		playerBoards.get(player.getName()).changeSBStatus(status);
		
	}
	
	// Spielzeit-Getter, wird durch updateScoreboard() in TimeLimitScoreBoard aufgerufen
	static int getPlayTime(Player player) {
		return playerManagers.get(player.getName()).getPlayTime();
	}
	
	// Zeitlimit-Getter, wird durch den Constructor von TimeLimitScoreBoard aufgerufen
	static int getTimeLimit(Player player) {
		return playerManagers.get(player.getName()).getTimeLimit();
	}
	
	static void restartPlayer(Player player) {
		playerBoards.get(player.getName()).disableBoard();
		playerManagers.get(player.getName()).disconnectEvent();
		playerManagers.remove(player.getName());
		playerBoards.remove(player.getName());
		
		playerManagers.putIfAbsent(player.getName(), new PlayerManager(player));
		playerBoards.putIfAbsent(player.getName(), new TimeLimitScoreBoard(player));
	}
}
