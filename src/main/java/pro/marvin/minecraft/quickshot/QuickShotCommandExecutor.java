package pro.marvin.minecraft.quickshot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class QuickShotCommandExecutor implements CommandExecutor {
	private QuickShot plugin;

	public QuickShotCommandExecutor(QuickShot qc) {
		plugin = qc;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("qs")) {

			if (args.length == 0) {
				if (sender.isOp()) {
					sender.sendMessage("§8--------------------=[ §6QuickShot §8]=--------------------");
					sender.sendMessage("§7[QuickShot] §6Befehle:");
					sender.sendMessage("§6     - qs [start/stop] [map]");
					sender.sendMessage("§6     - qs setLobby");
					sender.sendMessage("§6     - qs setSpawn [map] [spawn]");
					sender.sendMessage("§6     - qs setTimeLimit [int > 0]");
					sender.sendMessage("§6     - qs setMaxArena [int > 0]");
					sender.sendMessage("(c) 2013-2017: Marvin Menzerath");
				}
				return true;
			}

			if (args.length == 1 && sender instanceof Player) {
				if (args[0].equalsIgnoreCase("lobby")) {
					Player player = (Player) sender;
					if (plugin.getPlayerInGame(player)) {
						return true;
					}
					player.teleport(plugin.qsSpawn());
					return true;
				}

				if (args[0].equalsIgnoreCase("leave")) {
					Player player = (Player) sender;
					plugin.playerLeave(player);
					return true;
				}

				if (args[0].equalsIgnoreCase("setlobby")) {
					Player player = (Player) sender;
					List<Double> listPosition = Arrays.asList(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
					plugin.getConfig().set("spawn", listPosition);
					plugin.saveConfig();
					plugin.loadConfig();
					sender.sendMessage("§7[QuickShot] §6QS-Spawn gesetzt.");
					return true;
				}

				if (args[0].equalsIgnoreCase("start")) {
					if (sender.hasPermission("quickshot.premium")) {
						int map = plugin.getPlayersArena((Player) sender);
						plugin.startGame((Player) sender, map, false);
					} else {
						sender.sendMessage("§7[QuickShot] §4Nur OPs, YouTuber und Premiums und können die Arena starten!");
					}
					return true;
				}
			}

			if (args.length == 2) {
				if (args[0].equalsIgnoreCase("join") && sender instanceof Player) {
					Player p = (Player) sender;

					int map;
					try {
						map = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage("§7[QuickShot] §4Dies ist keine gültige Arena!");
						return true;
					}

					if (map > plugin.getMaxArena()) {
						sender.sendMessage("§7[QuickShot] §4Diese Arena wurde nicht eingerichtet oder ist zur Zeit nicht aktiv!");
						return true;
					}

					if (plugin.getGameStarted(map)) {
						p.sendMessage("§7[QuickShot] §4Das Spiel läuft bereits. Joinen ist nicht möglich!");
						return true;
					}

					plugin.playerJoinedArena(p, map);
					return true;
				}

				if (sender.isOp() && sender instanceof Player) {
					int map;
					try {
						map = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage("§7[QuickShot] §4Dies ist keine gültige Arena!");
						return true;
					}

					if (map == 0) {
						sender.sendMessage("§7[QuickShot] §4Dies ist keine gültige Arena!");
					}

					if (args[0].equalsIgnoreCase("stop")) {
						plugin.stopGame((Player) sender, map, true);
						return true;
					} else if (args[0].equalsIgnoreCase("start")) {
						plugin.startGame((Player) sender, map, false);
						return true;
					}
				} else {
					sender.sendMessage("§7[QuickShot] §4Nur OPs und können diese Befehle ausführen!");
					return true;
				}

				if (args[0].equalsIgnoreCase("setTimeLimit")) {
					if (!sender.isOp()) {
						sender.sendMessage("§7[QuickShot] §4Nur OPs können diese Befehle ausführen!");
						return true;
					}

					int time;
					try {
						time = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage("§7[QuickShot] §4Dies ist keine gültige Zahl!");
						return true;
					}
					plugin.getConfig().set("timeLimit", time);
					plugin.saveConfig();
					plugin.timeLimit = time;
					sender.sendMessage("§7[QuickShot] §6TimeLimit auf " + time + " gesetzt.");
					return true;
				}
				if (args[0].equalsIgnoreCase("setMaxArena")) {
					if (!sender.isOp()) {
						sender.sendMessage("§7[QuickShot] §4Nur OPs können diese Befehle ausführen!");
						return true;
					}

					int max;
					try {
						max = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage("§7[QuickShot] §4Dies ist keine gültige Zahl!");
						return true;
					}
					plugin.getConfig().set("maxArena", max);
					plugin.saveConfig();
					plugin.maxArena = max;
					sender.sendMessage("§7[QuickShot] §6MaxArena auf " + max + " gesetzt.");
					return true;
				}
				return true;
			}

			if (args.length == 3) {
				if (sender instanceof Player) {
					if (args[0].equalsIgnoreCase("setspawn")) {
						if (sender.isOp()) {
							int arg1;
							int arg2;
							try {
								arg1 = Integer.parseInt(args[1]);
								arg2 = Integer.parseInt(args[2]);
							} catch (NumberFormatException e) {
								sender.sendMessage("§7[QuickShot] §4Die Parameter dürfen nur Zahlen sein.");
								return true;
							}
							if (arg1 < 4 && arg1 > 0 && arg2 < 7 && arg2 > 0) {
								Player player = (Player) sender;
								List<Double> listPosition = Arrays.asList(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
								plugin.getConfig().set("maps." + arg1 + "." + arg2, listPosition);
								plugin.saveConfig();
								plugin.loadConfig();
								sender.sendMessage("§7[QuickShot] §6Spawn " + arg2 + " auf Map " + arg1 + " gesetzt.");
								return true;
							} else {
								sender.sendMessage("§7[QuickShot] §4Die Parameter müssen im Bereich von 1-3 (Maps) und 1-6 (Spawns) liegen.");
								return true;
							}
						} else {
							sender.sendMessage("§7[QuickShot] §4Nur OPs können diese Befehle ausführen!");
							return true;
						}
					}
				} else {
					sender.sendMessage("§7[QuickShot] §4Nur Spieler können diese Befehle ausführen!");
					return true;
				}
			}
		}
		return false;
	}
}