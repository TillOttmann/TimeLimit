package com.plugin.timelimit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;

class PlayerManager {
	
	// Warn-Zeitpunkte, falls die Spielzeit kurz davor ist, abzulaufen
	private final int FIRST_WARNING_TIME;
	private final int SECOND_WARNING_TIME;
	
	private Connection conn;
	private Player player;
	private String uuid;
	private int grade;
	private int timelimit;
	private int playTime = 0;
	private boolean status;
	boolean online;
	private List<String> permissionGroups;
	FileConfiguration config;
	
	@SuppressWarnings("unchecked")
	PlayerManager(Player player) {
		// Holt sich die Permissiongroups und die Warnzeiten aus der config.yml Datei
		config = TimeLimitMain.getFileConfig();
		permissionGroups = (List<String>) config.getList("PERMISSION_GROUPS");
		FIRST_WARNING_TIME = config.getInt("FIRST_WARNING_TIME");
		SECOND_WARNING_TIME = config.getInt("SECOND_WARNING_TIME");
		
		// Datenbankverbindung
		conn = TimeLimitMain.getDatabaseConnection();
		
		online = true;
		this.player = player;
		uuid = this.player.getUniqueId().toString();
		
		// Holt sich die Stufe des Spielers mithilfe der Permissiongroups
		grade = findGrade(this.player);

		// Holt sich die Spielerdaten aus der Datenbank
		getPlayTimeData();
		if (getPlayerData()) timeLimitCheckerLoop();
	}
	
	// Playtime und TimeLimit getter für das Scoreboard
	int getPlayTime() {
		return playTime / 60;
	}

	int getTimeLimit() {
		return ((status) ? timelimit : -1);
	}
	
	// Speichert die Spielzeit in der DB und stoppt den Bukkit-Runnable loop
	// durch das ändern der online-Variable
	void disconnectEvent() {
		savePlayTime();
		online = false;
	}
	
	void updateData(Integer timelimit, Boolean status) {
		if (timelimit != null && timelimit > 0) this.timelimit = timelimit;
		if (status != null) this.status = status;
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
					if (!warningSend1 && (timelimit - (playTime / 60)) <= FIRST_WARNING_TIME) {
						player.sendMessage(ChatColor.RED + "Es verbleiben weniger als 5 Minuten Spielzeit für heute!");
						warningSend1 = true;
						
					}
					
					if (!warningSend2 && (timelimit -(playTime / 60)) <= SECOND_WARNING_TIME) {
						player.sendMessage(ChatColor.RED + "Es verbleibt weniger als eine Minute Spielzeit für heute!");
						warningSend2 = true;
						
					}
					
					if (!((playTime / 60) < timelimit)) {
						player.kickPlayer("Deine Spielzeit von " + timelimit + " Minuten ist abgelaufen");
						this.cancel();
					}
					if (!online) {
						this.cancel();
					}
				}
				playTime += 5;
			}
		}.runTaskTimer(TimeLimitMain.getInstance(), 0, 100);
	}

	// Findet die Stufe des Spielers mithilfe der PermissionGroup herraus
	private int findGrade(Player player) {
	    for (int i = 0; i < permissionGroups.size(); i++) {
			if (player.hasPermission(permissionGroups.get(i))) {
				return Integer.valueOf(permissionGroups.get(i).replaceAll("[^0-9]", ""));
			}
	    }
	    if (player.hasPermission("group.EF")) return 11;
	    if (player.hasPermission("group.Q1")) return 12;
	    if (player.hasPermission("group.Q2")) return 13;
	    return 0;
	}
	
	private void getPlayTimeData() {
		try {
			PreparedStatement preparedStmt;
			
			String getPlayTimeData = "SELECT playtime FROM playTimeData WHERE uuid = ? AND date = CURDATE()";
			
			preparedStmt = conn.prepareStatement(getPlayTimeData);
			preparedStmt.setString(1, player.getUniqueId().toString());
			
			ResultSet playTimeRow = preparedStmt.executeQuery();
			
			if (playTimeRow.next()) {
				playTime = playTimeRow.getInt("playtime");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private boolean getPlayerData() {
		
		// Holt sich folgende Spielerdaten aus der Datenbank
		// playerData: grade, timelimit, status, modified
		// Falls die benötigten Einträge nicht existieren, werden sie hinzugefügt
		try {
			PreparedStatement preparedStmt;
			
			String getPlayerDataRow = "SELECT * FROM playerData WHERE uuid = ?";
			
			preparedStmt = conn.prepareStatement(getPlayerDataRow);
			preparedStmt.setString(1, player.getUniqueId().toString());
			
			ResultSet playerDataRow = preparedStmt.executeQuery();
			
			if (playerDataRow.next()) {
				
				int modified = playerDataRow.getInt("modified");
				
				timelimit = playerDataRow.getInt("timelimit");
				status = playerDataRow.getBoolean("status");
				
				// Checkt, ob mehr als eine Zeile vorhanden ist
				if (modified == 1) {
					return true;
				} else if (!playerDataRow.isLast()) {
					TimeLimitMain.sendConsoleMessage("error", "Mehr als ein Eintrag für '" + uuid + "' in playerData gefunden, bitte überprüfen!");
					return false;	
				}
				return true;
			}
			insertMissingPlayerData();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();	
		}
		return false;
	}
	
	// Speichert die Spielzeit in der DB in Sekunden. Die Spalte playTime ist die einzige, die
	// in Sekunden gespeichert wird, alle anderen Zeitspalten sind in Minuten
	private void savePlayTime() {
		
		try {
			PreparedStatement preparedStmt;
			
			String savePlayTimeData = "REPLACE INTO playTimeData (date, uuid, playTime)"
				+ "VALUES (CURDATE(), ?, ?)";
			
			preparedStmt = conn.prepareStatement(savePlayTimeData);
			preparedStmt.setString(1, uuid);
			preparedStmt.setInt(2, playTime);
			preparedStmt.execute();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void insertMissingPlayerData() {
		try {
			PreparedStatement preparedStmt;

			String insertMissingData = "INSERT INTO playerData (uuid, grade, timelimit, status, modified)"
					+ "VALUES (?, ?, "
					+ "(SELECT timelimit FROM presetData WHERE grade = ?), "
					+ "(SELECT status FROM presetData WHERE grade = ?), 0)";
			
			preparedStmt = conn.prepareStatement(insertMissingData);
			preparedStmt.setString(1, uuid);
			preparedStmt.setInt(2, grade);
			preparedStmt.setInt(3, grade);
			preparedStmt.setInt(4, grade);
			preparedStmt.execute();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}