package com.plugin.timelimit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

class DatabaseConnection {
	
	private List<Integer> presetLimits;
	private List<Boolean> presetStates;
	private Connection connection;

	// Konstruktor, der die angegebenen Daten aus der config.yml benutzt, um die
	// Datenbankverbindung aufzubauen
	protected DatabaseConnection() {
		
		presetLimits = new ArrayList<Integer>();
		presetStates = new ArrayList<Boolean>();
		FileConfiguration config = TimeLimitMain.getFileConfig();

		try {
			String DBUrl = "jdbc:mysql://" + config.getString("DB_Url");
			String DBUser = config.getString("DB_User");
			String DBPw = config.getString("DB_Pw");
			connection = DriverManager.getConnection(DBUrl, DBUser, DBPw);

			TimeLimitMain.sendConsoleMessage("default", "Datenbankverbindung erfolgreich aufgebaut");

		} catch (SQLException e) {
			e.printStackTrace();
		}
		presetLimits.add(config.getInt("GRADE_" + 0 + "_LIMIT"));
		presetStates.add(config.getBoolean("GRADE_" + 0 + "_STATE"));
		for (int i = 5; i <= 13; i++) {
			presetLimits.add(config.getInt("GRADE_" + i + "_LIMIT"));
			presetStates.add(config.getBoolean("GRADE_" + i + "_STATE"));
		}
		configureDatabase();
	}

	// Gibt die Datenbankverbindung als Connection Objekt weiter
	protected Connection getDatabaseConnection() {
		return connection;
	}

	private void configureDatabase() {

		try {
			Statement stmt = connection.createStatement();

			// Erstellt die Datenbank timelimit, falls diese nicht existiert
			String createDatabase = "CREATE DATABASE IF NOT EXISTS timelimit";
			stmt.executeUpdate(createDatabase);

			if (stmt.getWarnings() != null) {
				TimeLimitMain.sendConsoleMessage("default", "Datenbank 'timelimit' existiert bereits, Erstellung wird übersprungen");

			} else {
				TimeLimitMain.sendConsoleMessage("warning", "Datenbank 'timelimit' erfolgreich erstellt");
			}

			// Wählt die Datenbank aus
			stmt.execute("USE timelimit");
			stmt.clearWarnings();

			// Erstellt die presetData Tabelle, diese enthält das voreingestellte Zeitlimit
			// jeder Stufe
			// Die Spalte 'status' gibt an, ob das limit aktiviert oder deaktiviert ist (1 =
			// aktiv, 0 = nicht aktiv)
			String createPresetDataTable = "CREATE TABLE IF NOT EXISTS presetData "
				+ "(grade TINYINT UNIQUE, timelimit SMALLINT DEFAULT 0, status BOOLEAN DEFAULT 0)";
			stmt.executeUpdate(createPresetDataTable);

			if (stmt.getWarnings() != null) {
				TimeLimitMain.sendConsoleMessage("default", "Tabelle 'presetData' existiert bereits, Erstellung wird übersprungen");

			} else {
				TimeLimitMain.sendConsoleMessage("warning", "Tabelle 'presetData' erfolgreich erstellt");
			}

			stmt.clearWarnings();

			// Erstellt die playTimeData Tabelle, hier wird die Spielzeit jedes Spielers in
			// Abhängigkeit des Datums angegeben
			String createPlayTimeDataTable = "CREATE TABLE IF NOT EXISTS playTimeData "
				+ "(id INT AUTO_INCREMENT PRIMARY KEY, date DATETIME, UUID VARCHAR(36), "
				+ "playtime MEDIUMINT DEFAULT 0, UNIQUE KEY unique_uuid_date (uuid, date))";
			stmt.executeUpdate(createPlayTimeDataTable);

			if (stmt.getWarnings() != null) {
				TimeLimitMain.sendConsoleMessage("default", "Tabelle 'playTimeData' existiert bereits, Erstellung wird übersprungen");

			} else {
				TimeLimitMain.sendConsoleMessage("warning", "Tabelle 'playTimeData' erfolgreich erstellt");
			}

			stmt.clearWarnings();

			// Erstellt die playerData Tabelle, in dieser werden die Stufe, das individuelle
			// Zeitlimit und der Status angegeben
			// Die Spalte 'modified' speichert ausserdem, ob der Spieler ein individuelles
			// Zeitlimit erhalten hat
			String createPlayerDataTable = "CREATE TABLE IF NOT EXISTS playerData "
				+ "(uuid VARCHAR(36) PRIMARY KEY, grade TINYINT, timelimit SMALLINT DEFAULT 0, status BOOLEAN DEFAULT 0, modified BOOLEAN DEFAULT 0)";
			stmt.executeUpdate(createPlayerDataTable);

			if (stmt.getWarnings() != null) {
				TimeLimitMain.sendConsoleMessage("default", "Tabelle 'playerData' existiert bereits, Erstellung wird übersprungen");

			} else {
				TimeLimitMain.sendConsoleMessage("warning", "Tabelle 'playerData' erfolgreich erstellt");
			}
			
			// Fügt Zeitlimit-Presets in die Tabelle ein, falls diese nicht existieren
			String insertOrUpdatePresets = "REPLACE INTO presetData (grade, timelimit, status) VALUES ";
			for (int i = 0; i <= (13 - 5 + 1); i++) {
				insertOrUpdatePresets += "(" + i  + "," + presetLimits.get(i)  + "," + presetStates.get(i) + "),";
			}
			int affectedRows = stmt.executeUpdate(insertOrUpdatePresets.substring(0, insertOrUpdatePresets.length() -1));
			
			if (affectedRows == 20) {
				TimeLimitMain.sendConsoleMessage("default", "Alle Limit-Presets erfolgreich aktualisiert");

			} else {
				TimeLimitMain.sendConsoleMessage("warning", "INGORE IF FIRST TIME SETUP: Nicht alle Limit-Presets konnten eingefügt werden");
			}

			// Schliesst das Statement
			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Schliesst die Datenbankverbindung, wird durch onDisable() in TimeLimitMain
	// aufgerufen
	protected void closeDatabaseConnection() {
		
		if (connection != null) {
			try {
				connection.close();
				TimeLimitMain.sendConsoleMessage("default", "Datenbankverbindung erfolgreich geschlossen");

			} catch (SQLException e) {
				e.printStackTrace();

			}
		}
	}
}