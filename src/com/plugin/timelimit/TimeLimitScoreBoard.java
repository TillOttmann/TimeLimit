package com.plugin.timelimit;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
class TimeLimitScoreBoard {
	private FastBoard board;
	private Player player;
	private boolean status;
	
	protected TimeLimitScoreBoard(Player player) {
		 this.player = player;
		 enableBoard();
		 status = true;
	}
	
	// Löscht oder erstellt das Scoreboard
	protected void changeSBStatus(boolean status) {
		if (status) {
			if (!this.status) {
				enableBoard();
			}
		} else {
			if (this.status) {
				disableBoard();
			}
		}
		this.status = status;
	}
	
	// Erstellt das Board
	private void enableBoard() {
		board = new FastBoard(player);
		board.updateTitle(ChatColor.GREEN + "Spielzeit");
		loop();
	}
	
	// Löscht das board und stoppt den Bukkit-Runnable loop durch die variable 'online'
	protected void disableBoard() {
		this.status = false;
		board.delete();
	}
	private void loop() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!status) {
					this.cancel();
				} else {
					updateScoreBoard();
				}
			}
		}.runTaskTimer(TimeLimitMain.getInstance(), 0, (20 * 5));
	}
	
	// Aktualisiert das Scoreboard
	private void updateScoreBoard() {
		int playTime = PlayerListener.getPlayTime(player);
		int timelimit = PlayerListener.getTimeLimit(player);
		board.updateLines(
				"Heute: " + playTime + " Min",
				"Übrig: "
				+ ((((timelimit - playTime) <= 5) && timelimit >= 0) ? ChatColor.RED : "")
				+ ((timelimit >= 0) ? (timelimit - playTime) : "∞" )
				+ " Min");
	}
}
