package com.plugin.timelimit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;

class PlayerManager {
	
	// Warn-Zeitpunkte, falls die Spielzeit kurz davor ist, abzulaufen
	private final int FIRST_WARNING_TIME = 5;
	private final int SECOND_WARNING_TIME = 1;
	
	private Connection conn;
	private Player player;
	private String name;
	private int grade;
	private int timeLimit;
	private int playTime;
	private boolean status;
	protected boolean online;
	// Alle PermissionGroups der Stufenzuweisung
	private final String[] permissionGroups = new String[] { "group.6a", "group.6b", "group.6c", "group.6d", "group.7a", "group.7b",
			"group.7c", "group.7d", "group.8a", "group.8b", "group.8c", "group.8d", "group.9a", "group.9b", "group.9d", "group.9d",
			"group.10a", "group.10b", "group.10d" };

	protected PlayerManager(Player player) {
		
		conn = TimeLimitMain.getDatabaseConnection();
		online = true;
		this.player = player;
		name = this.player.getName();
		// Holt sich die Stufe des Spielers mithilfe der Permissiongroups
		grade = findGrade(this.player);

		// Holt sich die Spielerdaten aus der Datenbank
		getPlayerData();
		timeLimitCheckerLoop();
	}
	
	// Playtime und TimeLimit getter für das Scoreboard
	protected int getPlayTime() {
		return playTime / 60;
	}

	protected int getTimeLimit() {
		return ((status) ? timeLimit : -1);
	}
	
	private void getPlayerData() {
		
		// Holt sich folgende Spielerdaten aus der Datenbank
		// playerData: grade, timelimit, status, modified
		// playTimeData: playTime (des derzeitigen Datums)
		// Falls die benötigten Einträge nicht existieren, werden sie hinzugefügt
		try {
			PreparedStatement preparedStmt;

			String insertPlayerDataIfNotExists = "INSERT INTO playerData (username, grade, timelimit, status, modified) "
				+ "SELECT ?, ?, " + "(SELECT timelimit FROM presetData WHERE grade = ?), "
				+ "(SELECT status FROM presetData WHERE grade = ?), " + "0 "
				+ "WHERE NOT EXISTS (SELECT 1 FROM playerData WHERE username = ?)";

			preparedStmt = conn.prepareStatement(insertPlayerDataIfNotExists);
			preparedStmt.setString(1, name);
			preparedStmt.setInt(2, grade);
			preparedStmt.setInt(3, grade);
			preparedStmt.setInt(4, grade);
			preparedStmt.setString(5, name);
			preparedStmt.execute();

			String insertPlayTimeDataIfNotExists = "INSERT INTO playTimeData (date, username, playTime) " + "SELECT CURDATE(), ?, 0 "
				+ "WHERE NOT EXISTS (SELECT 1 FROM playTimeData WHERE username = ? AND date = CURDATE())";

			preparedStmt = conn.prepareStatement(insertPlayTimeDataIfNotExists);
			preparedStmt.setString(1, name);
			preparedStmt.setString(2, name);
			preparedStmt.execute();

			String getPlayerData = "SELECT pd.grade, pd.timelimit, pd.status, pd.modified, "
				+ "(SELECT playTime FROM playTimeData WHERE username = ? AND date = CURDATE()) AS playTime " + "FROM playerData pd "
				+ "WHERE pd.username = ?";

			preparedStmt = conn.prepareStatement(getPlayerData);
			preparedStmt.setString(1, name);
			preparedStmt.setString(2, name);

			ResultSet result = preparedStmt.executeQuery();

			if (result.next()) {
				
				int gradeTemp = result.getInt("grade");
				int modifiedTemp = result.getInt("modified");
				
				if (!result.isLast()) {
					TimeLimitMain.sendConsoleMessage("error", "Mehr als ein Eintrag für '" 
							+ name + "' in playTimeData (für den heutigen Tag) bzw playerData gefunden, bitte überprüfen!");
					
				} else if (gradeTemp != grade && modifiedTemp == 0) {
					String updateGradeAndLimitPreset = "UPDATE playerData SET timelimit = "
						+ "(SELECT timelimit FROM presetData WHERE grade = ?), " + "grade = ? WHERE username = ?";

					preparedStmt = conn.prepareStatement(updateGradeAndLimitPreset);
					preparedStmt.setInt(1, grade);
					preparedStmt.setInt(2, grade);
					preparedStmt.setString(3, name);
					preparedStmt.execute();

				}
				
				timeLimit = result.getInt("timelimit");
				status = result.getBoolean("status");
				playTime = result.getInt("playTime");

			} else {
				TimeLimitMain.sendConsoleMessage("error", "Daten von '" + name + "' konnten nicht abgerufen werden!");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	// Speichert die Spielzeit in der DB und stoppt den Bukkit-Runnable loop
	// durch das ändern der online-Variable
	protected void disconnectEvent() {
		savePlayTime();
		online = false;
	}
	
	// Speichert die Spielzeit in der DB in Sekunden. Die Spalte playTime ist die einzige, die
	// in Sekunden gespeichert wird, alle anderen Zeitspalten sind in Minuten
	private void savePlayTime() {
		
		try {
			PreparedStatement preparedStmt;
			
			String savePlayTimeData = "INSERT INTO playTimeData (date, username, playTime)"
				+ "VALUES (CURDATE(), ?, ?)"
				+ "ON DUPLICATE KEY UPDATE"
				+ "    playTime = VALUES(playTime)";
			
			preparedStmt = conn.prepareStatement(savePlayTimeData);
			preparedStmt.setString(1, name);
			preparedStmt.setInt(2, playTime);
			preparedStmt.execute();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private void timeLimitCheckerLoop() {
		
		// Bukkit loop: Überprüft alle 100 Ticks (20 Ticks = 1 sec, 100 Ticks = 5 sec), ob der Spieler
		// das Zeitlimit überschritten hat
		// Falls entweder das Zeitlimit überschritten ist oder der Spieler den Server verlässt (onPlayerQuit())
		// wird der loop abgebrochen
		new BukkitRunnable() {
			boolean warningSend1 = false;
			boolean warningSend2 = false;
			@Override
			public void run() {
				if (status) {
					if (!warningSend1 && (timeLimit - (playTime / 60)) <= FIRST_WARNING_TIME) {
						player.sendMessage(ChatColor.RED + "Es verbleiben weniger als 5 Minuten Spielzeit für heute!");
						warningSend1 = true;
						
					}
					
					if (!warningSend2 && (timeLimit -(playTime / 60)) <= SECOND_WARNING_TIME) {
						player.sendMessage(ChatColor.RED + "Es verbleibt weniger als eine Minute Spielzeit für heute!");
						warningSend2 = true;
						
					}
					
					if (!((playTime / 60) < timeLimit) || !online) {
						player.kickPlayer("Deine Spielzeit von " + timeLimit + " Minuten ist abgelaufen");
						this.cancel();

					}
				}
				playTime += 5;
			}
		}.runTaskTimer(TimeLimitMain.getInstance(), 0, 100);
	}

	// Findet die Stufe des Spielers mithilfe der PermissionGroup herraus
	private int findGrade(Player player) {
	    for (int i = 0; i < permissionGroups.length; i++) {
			if (player.hasPermission(this.permissionGroups[i])) {
				return Integer.valueOf(permissionGroups[i].replaceAll("[^0-9]", ""));
			}
	    }
	    if (player.hasPermission("group.EF")) return 11;
	    if (player.hasPermission("group.Q1")) return 12;
	    if (player.hasPermission("group.Q2")) return 13;
	    return 0;
	}

}