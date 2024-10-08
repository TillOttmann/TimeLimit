package com.plugin.timelimit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

class CommandManager implements CommandExecutor, TabCompleter {
	private Player player;
	private Connection conn;
	private String target;
	private int timeLimit;
	private boolean status;
	private String subCommand;
	
	// Registriert die Class als Executor und Tabcompleter
	// Holt sich die Datenbankverbindung aus der Main Class
	protected CommandManager() {
		conn = TimeLimitMain.getDatabaseConnection();
		TimeLimitMain.setCommandExecutorAndTabCompleter(this);
	}
	
	// Überprüft das target
	private boolean setTarget(String target) {
		if (target.length() <= 16) {
			this.target = target;
			return true;
		}
		player.sendMessage("Invalides Ziel");
		return false;
	}
	
	// Überprüft das timeLimit
	private boolean setTimeLimit(String timeLimit) {
		try {
			this.timeLimit = Integer.parseInt(timeLimit);
			// 32767 ist das maximum von SMALLINT(6) (signed), Werte darüber
			// führen zu einer SQLException
			if (this.timeLimit >= 0 && this.timeLimit <= 32767) return true;
		
		// Ignoriert die NumberFormatException (wird geworfen wenn das Zeitlimit keine Zahl ist)
		// Gibt jedoch eine Fehlermeldung wieder
		} catch (NumberFormatException ignore) { }
		
		player.sendMessage("Invalider Wert");
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		
		List<String> completions = new ArrayList<>();
		
		// Befehle
		if (args.length == 1) {
			List<String> subCommands = Arrays.asList("set", "get", "disable", "enable", "show", "hide");
			StringUtil.copyPartialMatches(args[0], subCommands, completions);
		}
		
		// Online-Spieler
		if (args.length == 2) {
	        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
	            completions.add(onlinePlayer.getName());
	        }
		}
		
		completions.sort(String.CASE_INSENSITIVE_ORDER);
		return completions;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender instanceof Player && label.equalsIgnoreCase("timelimit")) {
		
			player = (Player) sender;
			if (args.length > 0) {
				subCommand = (args[0] != null && !args[0].isEmpty()) ? args[0] : "none";
				
				
				// Überpüft, ob die target und timelimit parameter existieren, falls ja,
				// überprüft diese
				if ((args.length > 1) && !setTarget((args[1] != null) ? args[1] : "")) return false;
				if ((args.length > 2) && !setTimeLimit((args[2] != null) ? args[2] : "")) return false;;
				
				// Die Datenbankoperationen laufen Async, um den Server nicht zu verlangsamen
				if (subCommand.equalsIgnoreCase("set") && args.length == 3) {
					
					//Setzt das Zeitlimit eines Spielers
					if (player.hasPermission("timelimit.set")) {
						Bukkit.getScheduler().runTaskAsynchronously(TimeLimitMain.getInstance(), () -> {
							updateTimeLimitDB();
						});
					} else {
						player.sendMessage("Dir fehlt die Berechtigung 'timelimit.set', um diesen Befehl auszuführen!");
					}
					return true;
					
				} else if (subCommand.equalsIgnoreCase("get") && args.length == 2) {

					// Holt sich das Zeitlimit eines Spielers aus der DB
					if (player.hasPermission("timelimit.get") || (target.equalsIgnoreCase(player.getName()))) {
						Bukkit.getScheduler().runTaskAsynchronously(TimeLimitMain.getInstance(), () -> {
							getTimeLimitDB();
						});
					} else {
						player.sendMessage("Dir fehlt die Berechtigung 'timelimit.get', um diesen Befehl auszuführen!");
					}
					return true;
					
				} else if ((subCommand.equalsIgnoreCase("disable") || subCommand.equalsIgnoreCase("enable")) && args.length == 2) {
					
					// (De)Aktiviert das Zeitlimit einens Spielers. Das Limit selber bleibt hierbei unberührt
					if (player.hasPermission("timelimit.status")) {
						status = (subCommand.equalsIgnoreCase("disable")) ? false : true;
						Bukkit.getScheduler().runTaskAsynchronously(TimeLimitMain.getInstance(), () -> {
							changeTimeLimitStatusDB();
						});
					} else {
						player.sendMessage("Dir fehlt die Berechtigung 'timelimit.status', um diesen Befehl auszuführen!");
					}
					return true;

				} else if ((subCommand.equalsIgnoreCase("show") || subCommand.equalsIgnoreCase("hide")) && args.length == 1) {
					
					// Erstellt bzw löscht das Scoreboard eines Spielers
					PlayerListener.changeScoreBoardStatus(player,(subCommand.equalsIgnoreCase("show")) ? true : false);
					return true;
				}
			}

		} else {
			TimeLimitMain.sendConsoleMessage("warning", "Bitte diesen Befehl nur als Spieler ausführen!");
		}
		return false;		
	}
	
	private void updateTimeLimitDB() {
		
		try {
			PreparedStatement preparedStmt;
			
			String updateTimeLimit = "UPDATE playerData SET timeLimit = ?, modified = 1 WHERE username = ?";
			
			preparedStmt = conn.prepareStatement(updateTimeLimit);
			preparedStmt.setInt(1, timeLimit);
			preparedStmt.setString(2, target);
			
			int affectedRows = preparedStmt.executeUpdate();		

			if (affectedRows > 1) {
				TimeLimitMain.sendConsoleMessage("error", "Mehr als ein Eintrag in 'playerData' für '"
						+ target + "' gefunden, bitte überprüfen!");
				player.sendMessage("Error, bitte Console überprüfen!");
				
			} else {

				if (affectedRows == 0) {

					String insertMissingPlayerData = "INSERT INTO playerData "
							+ "(username, grade, timeLimit, status, modified) "
							+ "VALUES (?, 0, ?, 1, 1)";
					
					preparedStmt = conn.prepareStatement(insertMissingPlayerData);
					preparedStmt.setString(1, target);
					preparedStmt.setInt(2, timeLimit);
					affectedRows = preparedStmt.executeUpdate();
				}
				
				if (affectedRows != 1) {
					TimeLimitMain.sendConsoleMessage("warning", 
							"Eintrag für '" + target + "' konnte nicht erstellt werden");
					
					player.sendMessage("Eintrag für '" + target + "' konnte nicht erstellt werden");
					
				} else {
					TimeLimitMain.sendConsoleMessage("default", 
							"Zeitlimit für " + target + " erfolgreich auf "
							+ timeLimit + " Minuten gesetzt");
				
					player.sendMessage("Zeitlimit für " + target + " erfolgreich auf "
							+ timeLimit + " Minuten gesetzt");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void getTimeLimitDB() {
		
		try {
			PreparedStatement preparedStmt;
			
			String getTimeLimit = "SELECT timeLimit, status FROM playerData WHERE username = ?";
			
			preparedStmt = conn.prepareStatement(getTimeLimit);
			preparedStmt.setString(1, target);
			
			ResultSet result = preparedStmt.executeQuery();
			
			if (result.next()) {
				int timeLimitTemp = result.getInt("timeLimit");
				String statusString = (result.getBoolean("status")) ? "aktiviert" : "deaktiviert";
				
				if (!result.isLast()) {
					TimeLimitMain.sendConsoleMessage("error", "Mehr als ein Eintrag für '" 
							+ target + "' in playerData gefunden, bitte überprüfen!");
					player.sendMessage("Error, bitte Console überprüfen!");
					
				} else {
					player.sendMessage("Das Zeitlimit von " + target 
					+ " beträgt " + timeLimitTemp + " Minuten (" + statusString + ")");
				}
			} else {
				player.sendMessage("Das Zeitlimit von " + target + " wurde nicht gefunden");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void changeTimeLimitStatusDB() {
		
		try {
			PreparedStatement preparedStmt;
			
			String changeStatus = "UPDATE playerData SET status = ?, modified = 1 WHERE username = ?";
			
			preparedStmt = conn.prepareStatement(changeStatus);
			preparedStmt.setBoolean(1, status);
			preparedStmt.setString(2, target);
			
			int affectedRows = preparedStmt.executeUpdate();		

			if (affectedRows > 1) {
				TimeLimitMain.sendConsoleMessage("error", "Mehr als ein Eintrag in 'playerData' für '"
						+ target + "' gefunden, bitte überprüfen!");
				player.sendMessage("Error, bitte Console überprüfen!");
				
			} else {
				if (affectedRows == 0) {

					String insertMissingPlayerData = "INSERT INTO playerData "
							+ "(username, grade, timeLimit, status, modified) "
							+ "VALUES (?, 0, 0, ?, 1)";
					
					preparedStmt = conn.prepareStatement(insertMissingPlayerData);
					preparedStmt.setString(1, target);
					preparedStmt.setBoolean(2, status);
					affectedRows = preparedStmt.executeUpdate();
				}
				
				if (affectedRows != 1) {
					TimeLimitMain.sendConsoleMessage("warning", 
							"Eintrag für '" + target + "' konnte nicht erstellt werden");
					
					player.sendMessage("Eintrag für '" + target + "' konnte nicht erstellt werden");
				}
				
				String action = (status) ? "aktiviert" : "deaktiviert";
				
				TimeLimitMain.sendConsoleMessage("default", 
						"Zeitlimit für " + target + " erfolgreich " + action);
			
				player.sendMessage("Zeitlimit für " + target + " erfolgreich " + action);
			
				}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}