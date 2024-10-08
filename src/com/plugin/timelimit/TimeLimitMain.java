package com.plugin.timelimit;

import java.sql.Connection;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class TimeLimitMain extends JavaPlugin {
	private static TimeLimitMain instance;
	private static Connection conn;
	private DatabaseConnection DBInstance;

	// Wird aufgerufen, wenn das Plugin aktiviert wird
	@Override
	public void onEnable() {
		instance = this;
		// Default config.yml wird erstellt falls nicht vorhanden
		instance.saveDefaultConfig();
		// DatabaseConnection-Object, wird gespeichert um später die Datenbankverbindung bei onDisable()
		// wieder zu schließen. Die eigentliche Verbindung wird in conn gespeichert
		DBInstance = new DatabaseConnection();
		conn = DBInstance.getDatabaseConnection();
		
		getServer().getPluginManager().registerEvents(new PlayerListener(), instance);
		new CommandManager();
	}

	// Wird aufgerufen, wenn das Plugin deaktiviert wird
	@Override
	public void onDisable() {
		DBInstance.closeDatabaseConnection();
	}
	
	// Instance-Getter (Einzige derzeitige verwendung: Async-Runnables in CommandManager)
	protected static TimeLimitMain getInstance() {
		return instance;
	}
	
	// Getter-Funktion: Erlaubt den Zugriff auf die config.yml datei
	protected static FileConfiguration getFileConfig() {
		return instance.getConfig();
	}

	// Getter-Funktion für die Datenbank, alle Klassen verwenden eine geteilte Verbindung
	protected static Connection getDatabaseConnection() {
		return conn;
	}
	
	// Wird in dem Contructor von CommandManager aufgerufen, welche sowohl als
	// Executor als auch TabCompleter verwendet wird
	protected static void setCommandExecutorAndTabCompleter(CommandManager cmdInstance) {
		instance.getCommand("timelimit").setExecutor(cmdInstance);
		instance.getCommand("timelimit").setTabCompleter(cmdInstance);
	}
	
	// Sendet eine Nachricht an die Server-Console
	protected static void sendConsoleMessage(String type,String message) {
		org.bukkit.ChatColor color;
		if (type.equals("error")) {
			color = org.bukkit.ChatColor.RED;
		} else 	if (type.equals("warning")) {
			color = org.bukkit.ChatColor.YELLOW;
		} else {
			color = org.bukkit.ChatColor.GREEN;
		}
		Bukkit.getServer().getConsoleSender().sendMessage(color + "[TimeLimit] " + message);
	}
}
