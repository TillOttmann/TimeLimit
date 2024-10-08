package com.plugin.timelimit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.configuration.file.FileConfiguration;

class DatabaseConnection {

	private Connection connection;

	// Konstruktor, der die angegebenen Daten aus der config.yml benutzt, um die
	// Datenbankverbindung aufzubauen
	protected DatabaseConnection() {

		FileConfiguration config = TimeLimitMain.getFileConfig();

		try {
			String DBUrl = "jdbc:mysql://" + config.getString("DB_Url");
			String DBUser = config.getString("DB_User");
			String DBPw = config.getString("DB_Pw");
			connection = DriverManager.getConnection(DBUrl, DBUser, DBPw);

			TimeLimitMain.sendConsoleMessage("default", "Datenbankverbindung erfolgreich aufgebaut");
			configureDatabase();

		} catch (SQLException e) {
			e.printStackTrace();
		}

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
				+ "(grade TINYINT UNIQUE, timeLimit SMALLINT DEFAULT 0, status BOOLEAN DEFAULT 0)";
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
				+ "(date DATETIME, username VARCHAR(16), playTime MEDIUMINT DEFAULT 0, UNIQUE KEY uniqueUsernameDate (date, username))";
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
				+ "(username VARCHAR(16), grade TINYINT, timeLimit SMALLINT DEFAULT 0, status BOOLEAN DEFAULT 0, modified BOOLEAN DEFAULT 0)";
			stmt.executeUpdate(createPlayerDataTable);

			if (stmt.getWarnings() != null) {
				TimeLimitMain.sendConsoleMessage("default", "Tabelle 'playerData' existiert bereits, Erstellung wird übersprungen");

			} else {
				TimeLimitMain.sendConsoleMessage("warning", "Tabelle 'playerData' erfolgreich erstellt");
			}
			
			// Fügt Zeitlimit-Presets in die Tabelle ein, falls diese nicht existieren
			String insertNewPresets = "INSERT INTO presetData (grade, timeLimit, status) " + "SELECT * FROM ( "
				+ "SELECT 0 AS grade, 120 AS timeLimit, 1 AS status UNION ALL " + "SELECT 5, 60, 1 UNION ALL "
				+ "SELECT 6, 60, 1 UNION ALL " + "SELECT 7, 60, 1 UNION ALL " + "SELECT 8, 60, 1 UNION ALL "
				+ "SELECT 9, 60, 1 UNION ALL " + "SELECT 10, 60, 1 UNION ALL " + "SELECT 11, 60, 1 UNION ALL "
				+ "SELECT 12, 60, 1 UNION ALL " + "SELECT 13, 60, 1 " + ") AS NewGrades " + "WHERE NOT EXISTS ( " + "SELECT 1 "
				+ "FROM presetData AS PD " + "WHERE PD.grade = NewGrades.grade " + ");";

			int affectedRows = stmt.executeUpdate(insertNewPresets);

			if (affectedRows == 0) {
				TimeLimitMain.sendConsoleMessage("default", "Alle Limit-Presets vorhanden, Erstellung wird übersprungen");

			} else {
				TimeLimitMain.sendConsoleMessage("warning", "Fehlende Limit-Presets wurden hinzugefügt");
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