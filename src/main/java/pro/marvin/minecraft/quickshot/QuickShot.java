package pro.marvin.minecraft.quickshot;

import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;

import java.util.*;

public class QuickShot extends JavaPlugin implements Listener {
	private HashMap<Integer, Boolean> gameStarted = new HashMap<>();
	private HashMap<Integer, Integer> countdown = new HashMap<>();
	private HashMap<Integer, Integer> arenaTimer = new HashMap<>();

	public List<Player> playersList = new ArrayList<>();
	public HashMap<String, Integer> playersMap = new HashMap<>();
	private HashMap<String, Integer> playersScore = new HashMap<>();
	private HashMap<String, Boolean> canDie = new HashMap<>();

	private HashMap<Integer, String> playerOnKillstreak = new HashMap<>();
	private HashMap<Integer, Integer> numberOfKillstreak = new HashMap<>();

	private static final int maxPlayersPerArena = 16;
	private static final String MAIN_WORLD = "world";

	public int timeLimit;
	public int maxArena;

	private ScoreboardManager manager;
	private Scoreboard board;
	private Objective[] objective = new Objective[51];

	@Override
	public void onEnable() {
		getCommand("qs").setExecutor(new QuickShotCommandExecutor(this));
		getServer().getPluginManager().registerEvents(this, this);
		manager = Bukkit.getScoreboardManager();
		board = manager.getNewScoreboard();
		countdown();
		arenaTimer();
		loadConfig();
		updateSigns();

		for (int i = 1; i < maxArena + 1; i++) {
			gameStarted.put(i, false);
			countdown.put(i, -1);
			arenaTimer.put(i, -1);
			playerOnKillstreak.put(i, "");
			numberOfKillstreak.put(i, 0);
		}
	}

	@Override
	public void onDisable() {
		for (Player p : playersList) {
			p.setGameMode(GameMode.ADVENTURE);
			p.getInventory().clear();
			p.teleport(qsSpawn());
			p.setExp(0);
			p.setLevel(0);
			p.setScoreboard(manager.getNewScoreboard());
		}
	}

	public void startGame(final Player player, final int mapId, boolean countdownStart) {
		if (!countdownStart) {
			if (getGameStarted(mapId)) {
				player.sendMessage("§7[QuickShot] §4Du kannst das Spiel nicht mehrfach starten!");
				return;
			}
			if (getPlayerInArena(mapId).size() < 2) {
				player.sendMessage("§7[QuickShot] §4Ohne genügend Spieler kann das Spiel nicht gestartet werden!");
				return;
			}
			countdown.put(mapId, 10);
			return;
		}
		countdown.put(mapId, -1);

		World w = getServer().getWorld("qs-w" + mapId);
		w.setTime(6000);
		w.setDifficulty(Difficulty.PEACEFUL);
		w.setStorm(false);
		setupScoreboard(mapId);
		for (Player p : getPlayerInArena(mapId)) {
			p.setGameMode(GameMode.ADVENTURE);
			p.setHealth(20);
			p.setFoodLevel(20);
			p.setLevel(0);
			preparePlayers(p, false);
		}

		for (Player p : getPlayerInArena(mapId)) {
			p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3, 1);
		}

		gameStarted.put(mapId, true);
		arenaTimer.put(mapId, timeLimit);
	}

	public void stopGame(Player player, final int mapId, boolean message) {
		if (!getGameStarted(mapId)) {
			if (message) {
				player.sendMessage("§7[QuickShot] §4Das Spiel ist bereits beendet.");
			}
			return;
		}
		arenaTimer.put(mapId, -1);

		int highScore = 0;
		Player bestPlayer = null;
		for (Player p : getPlayerInArena(mapId)) {
			if (playersScore.get(p.getName()) > highScore) {
				highScore = playersScore.get(p.getName());
				bestPlayer = p;
			}
		}

		for (Player p : getPlayerInArena(mapId)) {
			assert bestPlayer != null;
			p.sendMessage("§7[QuickShot] §6Spieler §7" + bestPlayer.getDisplayName() + " §6hat gewonnen!");
			p.getInventory().clear();

			Firework fw = (Firework) p.getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK);
			FireworkMeta fwm = fw.getFireworkMeta();

			Color c = Color.GREEN;
			FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(c).with(Type.STAR).trail(true).build();
			fwm.addEffect(effect);
			fwm.setPower(0);
			fw.setFireworkMeta(fwm);

			getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
				for (Player p1 : getPlayerInArena(mapId)) {
					p1.getInventory().clear();
				}
			}, 20L);
		}

		playerOnKillstreak.put(mapId, "");
		numberOfKillstreak.put(mapId, 0);

		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
			for (Player p : getPlayerInArena(mapId)) {
				p.setGameMode(GameMode.ADVENTURE);
				p.getInventory().clear();
				p.setExp(0);
				p.setLevel(0);
				p.teleport(qsSpawn());

				p.setScoreboard(manager.getNewScoreboard());
				playersList.remove(p);
				playersMap.remove(p.getName());
				playersScore.remove(p.getName());
				canDie.remove(p.getName());
			}
			objective[mapId].unregister();
			gameStarted.put(mapId, false);
		}, 10 * 20L);
	}

	public void playerJoinedArena(Player p, int mapId) {
		if (playersList.contains(p)) {
			p.sendMessage("§7[QuickShot] §6Du bist bereits in einer Arena! Du kannst nicht mehrfach joinen!");
			return;
		}
		if (getPlayerInArena(mapId).size() > maxPlayersPerArena - 1 && !p.hasPermission("quickshot.premium")) {
			p.sendMessage("§7[QuickShot] §6Diese Arena ist bereits voll. Kaufe dir Premium, damit du doch noch die Arena joinen kannst!");
			return;
		} else if (getPlayerInArena(mapId).size() > maxPlayersPerArena - 1 && p.hasPermission("quickshot.premium")) {
			if (!kickRandomPlayer(mapId, 0)) {
				p.sendMessage("§7[QuickShot] §6Ich konnte nur Premium-Spieler finden und niemanden herauswerfen. Versuche es doch einfach noch einmal...");
				return;
			}
		}

		playersList.add(p);
		playersMap.put(p.getName(), mapId);
		playersScore.put(p.getName(), 0);
		canDie.put(p.getName(), true);

		p.getInventory().clear();
		p.setGameMode(GameMode.ADVENTURE);
		p.removePotionEffect(PotionEffectType.SPEED);
		p.teleport(randomSpawn(mapId));

		for (Player player : getPlayerInArena(mapId)) {
			player.sendMessage("§7[QuickShot] " + p.getDisplayName() + " §6ist der Arena beigetreten.");
		}

		if (getPlayerInArena(mapId).size() < 2) {
			p.sendMessage("§7[QuickShot] §6Der Countdown beginnt, sobald mindestens zwei Spieler in der Arena sind.");
			return;
		}

		if (countdown.get(mapId) != -1) {
			return;
		}

		countdown.put(mapId, 60);
	}

	private void setupScoreboard(int map) {
		objective[map] = board.registerNewObjective("kills", "dummy");
		objective[map].setDisplaySlot(DisplaySlot.SIDEBAR);
		objective[map].setDisplayName("Bestenliste");

		for (Player p : getPlayerInArena(map)) {
			Score score = objective[map].getScore(Bukkit.getPlayer(p.getName()));
			score.setScore(0);
			p.setScoreboard(board);
		}
	}

	public void loadConfig() {
		saveDefaultConfig();
		reloadConfig();
		timeLimit = getConfig().getInt("timeLimit");
		maxArena = getConfig().getInt("maxArena");
	}

	private void preparePlayers(final Player p, final boolean respawn) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
			p.teleport(randomSpawn(getPlayersArena(p)));
			p.getInventory().clear();
			p.setHealth(20);
			p.getInventory().setHeldItemSlot(0);

			if (!getGameStarted(getPlayersArena(p))) {
				return;
			}

			p.getInventory().addItem(new ItemStack(Material.BOW, 1));
			p.getInventory().addItem(new ItemStack(Material.WOOD_SWORD, 1));
			givePlayerAmmo(p);
			p.getInventory().setHeldItemSlot(0);

			if (respawn) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 80, 10));
			}
		}, 2);
	}

	private void givePlayerAmmo(final Player p) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> p.getInventory().addItem(new ItemStack(Material.ARROW, 1)), 10);

	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (getPlayerInGame(e.getPlayer()) && e.getPlayer().getItemInHand().getType() == Material.BOW && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && getGameStarted(getPlayersArena(e.getPlayer()))) {
			e.setCancelled(false);
		} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && isSign(e.getClickedBlock())) {
			Sign theSign = (Sign) e.getClickedBlock().getState();
			if (theSign.getLine(0).equals(ChatColor.GREEN + "[QuickShot]")) {
				for (int i = 1; i < maxArena + 1; i++) {
					if (theSign.getLine(3).startsWith(ChatColor.BLUE + "A" + i + ": ")) {
						e.getPlayer().performCommand("qs join " + i);
					}
				}

				if (theSign.getLine(2).equals(ChatColor.DARK_GREEN + "Zur Lobby")) {
					if (getPlayerInGame(e.getPlayer())) {
						playersList.remove(e.getPlayer());
						playersScore.remove(e.getPlayer().getName());
						playersMap.remove(e.getPlayer().getName());
					}
					e.getPlayer().performCommand("qs lobby");

					for (Player player : getPlayerInArena(getPlayersArena(e.getPlayer()))) {
						player.sendMessage("§7[QuickShot] §6" + e.getPlayer().getDisplayName() + " hat die Arena verlassen.");
					}
				}
			}
		} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock().getType().equals(Material.WOOD_BUTTON)) {
			e.setCancelled(false);
		} else if (getPlayerInGame(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			if ((e.getCause().equals(DamageCause.FALL) || e.getCause().equals(DamageCause.SUICIDE) || e.getCause().equals(DamageCause.DROWNING)) && getPlayerInGame((Player) e.getEntity())) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onHit(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player && getPlayerInGame((Player) e.getEntity())) {
			Player p = (Player) e.getEntity();

			if (p.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && getPlayerInGame(p)) {
				e.setCancelled(true);
				return;
			}

			if (!getGameStarted(getPlayersArena(p)) && (arenaTimer.get(getPlayersArena(p)) == -1)) {
				e.setCancelled(true);
				return;
			}

			if (e.getDamager() instanceof Arrow) {
				Arrow a = (Arrow) e.getDamager();
				if (a.getShooter() instanceof Player) {
					final Player shooter = (Player) a.getShooter();
					final Player target = p;
					e.setCancelled(true);

					if (canDie.get(target.getName()) && !shooter.equals(target)) {
						canDie.put(target.getName(), false);
						addFrag(shooter, target);
						preparePlayers(target, true);
						target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 5, 1);

						getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
							if (getPlayerInGame(shooter)) {
								givePlayerAmmo(shooter);
							}
						}, 2);

						getServer().getScheduler().scheduleSyncDelayedTask(this, () -> canDie.put(target.getName(), true), 20L);
					}
				}
			} else if (e.getEntity() instanceof Player) {
				if (e.getCause().equals(DamageCause.ENTITY_ATTACK)) {
					final Player shooter = (Player) e.getDamager();
					final Player target = (Player) e.getEntity();

					if (canDie.get(target.getName()) && (target.getHealth() - e.getDamage() <= 0)) {
						e.setCancelled(true);
						canDie.put(target.getName(), false);
						addFrag(shooter, target);
						preparePlayers(target, true);
						target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 5, 1);

						getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
							if (getPlayerInGame(shooter)) {
								givePlayerAmmo(shooter);
							}
						}, 2);

						getServer().getScheduler().scheduleSyncDelayedTask(this, () -> canDie.put(target.getName(), true), 20L);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		playerLeave(p);
	}

	public void playerLeave(Player p) {
		final int oldArena = getPlayersArena(p);
		boolean playerWasInGame = false;
		if (oldArena != 0) {
			playerWasInGame = true;
		}

		if (playerWasInGame) {
			for (Player player : getPlayerInArena(getPlayersArena(p))) {
				player.sendMessage("§7[QuickShot] " + p.getDisplayName() + " §6hat die Arena verlassen.");
			}

			p.teleport(qsSpawn());
			playersList.remove(p);
			playersMap.remove(p.getName());
			playersScore.remove(p.getName());
			canDie.remove(p.getName());
			p.getInventory().clear();

			p.setExp(0);
			p.setLevel(0);

			p.setScoreboard(manager.getNewScoreboard());

			if (getPlayerInArena(oldArena).size() < 2 && getGameStarted(oldArena)) {
				for (Player player : getPlayerInArena(oldArena)) {
					player.sendMessage("§7[QuickShot] §6Das Spiel wurde beendet, da nur noch ein Spieler online ist. Es werden mindestens zwei Spieler benötigt!");
				}
				stopGame(p, oldArena, false);
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (getPlayerInGame(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void playerDropItem(PlayerDropItemEvent e) {
		if (getPlayerInGame(e.getPlayer()) && getGameStarted(getPlayersArena(e.getPlayer()))) e.setCancelled(true);
	}

	@EventHandler
	public void playerPickUpItem(PlayerPickupItemEvent e) {
		if (getPlayerInGame(e.getPlayer()) && getGameStarted(getPlayersArena(e.getPlayer()))) e.setCancelled(true);
	}

	private void updateSigns() {
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			World w = Bukkit.getWorld(MAIN_WORLD);
			for (int i = 1; i < maxArena + 1; i++) {
				List<Double> sign = getConfig().getDoubleList("maps." + i + ".sign");
				Block b = w.getBlockAt(new Location(w, sign.get(0), sign.get(1), sign.get(2)));

				if (b.getTypeId() == Material.SIGN_POST.getId() || b.getTypeId() == Material.WALL_SIGN.getId()) {
					Sign mySign = (Sign) b.getState();

					String status = getArenaStatus(i);
					String players = ChatColor.RED + "" + getPlayerInArena(i).size() + " / " + maxPlayersPerArena;

					mySign.setLine(1, status);
					mySign.setLine(2, players);
					mySign.update();
				}
			}
		}, 0, 20L);
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		if (event.getLine(0).equalsIgnoreCase("QuickShot")) {
			for (int i = 1; i < maxArena + 1; i++) {
				if (event.getLine(2).equalsIgnoreCase("qs join " + i) && event.getPlayer().isOp()) {
					event.setLine(0, ChatColor.GREEN + "[QuickShot]");
					event.setLine(2, "Join " + ChatColor.DARK_BLUE + "Arena " + i);
					event.setLine(3, ChatColor.BLUE + "A" + i + ": " + event.getLine(3));
					List<Double> listPosition = Arrays.asList(event.getBlock().getLocation().getX(), event.getBlock().getLocation().getY(), event.getBlock().getLocation().getZ());
					getConfig().set("maps." + i + ".sign", listPosition);
					saveConfig();
					loadConfig();
				}
			}
		}

		if (event.getLine(0).equalsIgnoreCase("QuickShot") && event.getLine(2).equalsIgnoreCase("qs lobby") && event.getPlayer().isOp()) {
			event.setLine(0, ChatColor.GREEN + "[QuickShot]");
			event.setLine(2, ChatColor.DARK_GREEN + "Zur Lobby");
		}
	}

	private void countdown() {
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			for (int i = 1; i < maxArena + 1; i++) {
				if (countdown.get(i) != -1) {
					if (countdown.get(i) != 0) {
						if (getPlayerInArena(i).size() < 2) {
							for (Player players : getPlayerInArena(i)) {
								players.sendMessage("§7[QuickShot] §6Der Start wird abgebrochen, da zu wenig Spieler in der Arena sind.");
							}
							countdown.put(i, -1);
							return;
						}

						if (countdown.get(i) < 4) {
							for (Player p : getPlayerInArena(i)) {
								p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 3, 1);
							}
						}
						for (Player p : getPlayerInArena(i)) {
							p.setLevel(countdown.get(i));
						}
						countdown.put(i, countdown.get(i) - 1);
					} else {
						countdown.put(i, countdown.get(i) - 1);
						if (getPlayerInArena(i).size() < 2) {
							for (Player players : getPlayerInArena(i)) {
								players.sendMessage("§7[QuickShot] §6Der Start wird abgebrochen, da zu wenig Spieler in der Arena sind.");
							}
							countdown.put(i, -1);
							return;
						}
						startGame(null, i, true);
					}
				}
			}
		}, 0, 20L);
	}

	private void arenaTimer() {
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			for (int i = 1; i < maxArena + 1; i++) {
				if (arenaTimer.get(i) != -1) {
					if (arenaTimer.get(i) != 0) {
						if (arenaTimer.get(i) < 6) {
							for (Player p : getPlayerInArena(i)) {
								p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 3, 1);
							}
						}
						for (Player p : getPlayerInArena(i)) {
							p.setLevel(arenaTimer.get(i));
						}
						arenaTimer.put(i, arenaTimer.get(i) - 1);
					} else {
						arenaTimer.put(i, arenaTimer.get(i) - 1);
						for (Player p : getPlayerInArena(i)) {
							p.setLevel(0);
						}
						stopGame(null, i, false);
					}
				}
			}
		}, 0, 20L);
	}

	private void addFrag(Player p, Player target) {
		String message = "§7[QuickShot] §6" + p.getDisplayName() + " §7tötete §6" + target.getDisplayName();
		int arena = getPlayersArena(p);
		int score = playersScore.get(p.getName()) + 1;
		playersScore.put(p.getName(), score);

		Score newScore = objective[arena].getScore(Bukkit.getPlayer(p.getName()));
		newScore.setScore(score);
		for (Player players : getPlayerInArena(arena)) {
			players.setScoreboard(board);
			players.sendMessage(message);
		}

		if (playerOnKillstreak.get(arena).equals(p.getDisplayName())) {
			numberOfKillstreak.put(arena, numberOfKillstreak.get(arena) + 1);

			if (numberOfKillstreak.get(arena) == 2) {
				for (Player player : getPlayerInArena(arena)) {
					player.sendMessage("§7[QuickShot] §6" + p.getDisplayName() + "§7: §6Double Kill!");
				}
			}
			if (numberOfKillstreak.get(arena) == 3) {
				for (Player player : getPlayerInArena(arena)) {
					player.sendMessage("§7[QuickShot] §6" + p.getDisplayName() + "§7: §6Triple Kill!");
				}
			}
			if (numberOfKillstreak.get(arena) > 3) {
				for (Player player : getPlayerInArena(arena)) {
					player.sendMessage("§7[QuickShot] §6" + p.getDisplayName() + "§7: §6Stoppt ihn (" + numberOfKillstreak.get(getPlayersArena(p)) + " Kills)!");
				}
			}
		} else {
			playerOnKillstreak.put(getPlayersArena(p), p.getDisplayName());
			numberOfKillstreak.put(getPlayersArena(p), 1);
		}

		p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3, 1);
	}

	private boolean isSign(Block theBlock) {
		return theBlock.getType() == Material.SIGN || theBlock.getType() == Material.SIGN_POST || theBlock.getType() == Material.WALL_SIGN;
	}

	private boolean kickRandomPlayer(int mapId, int tries) {
		if (tries > 3) {
			return false;
		}
		List<Player> myPlayers = getPlayerInArena(mapId);
		Random r = new Random();
		int random = r.nextInt(maxPlayersPerArena);
		if (!myPlayers.get(random).hasPermission("quickshot.premium")) {
			myPlayers.get(random).performCommand("qs leave");
			myPlayers.get(random).sendMessage("§7[QuickShot] §6Du musstest einem Premium/YouTuber/Mod oder Admin Platz machen...");
			myPlayers.get(random).sendMessage("§7[QuickShot] §6Kaufe dir Premium, damit das nicht mehr passieren kann!");
			return true;
		} else {
			kickRandomPlayer(mapId, tries + 1);
			return false;
		}
	}

	private Location randomSpawn(int mapId) {
		Random r = new Random();
		int random = r.nextInt(6) + 1;
		List<Double> spawns = this.getConfig().getDoubleList("maps." + mapId + "." + random);
		return new Location(getServer().getWorld("qs-w" + mapId), spawns.get(0), spawns.get(1), spawns.get(2));
	}

	public Location qsSpawn() {
		List<Double> spawn = getConfig().getDoubleList("spawn");
		return new Location(getServer().getWorld(MAIN_WORLD), spawn.get(0), spawn.get(1), spawn.get(2));
	}

	public boolean getGameStarted(int mapId) {
		return gameStarted.get(mapId);
	}

	private List<Player> getPlayerInArena(int map) {
		List<Player> myPlayers = new ArrayList<>();
		for (Player p : playersList) {
			if (playersMap.get(p.getName()).equals(map)) {
				myPlayers.add(p);
			}
		}
		return myPlayers;
	}

	public int getPlayersArena(Player p) {
		if (playersMap.containsKey(p.getName())) {
			return playersMap.get(p.getName());
		}
		return 0;
	}

	public boolean getPlayerInGame(Player p) {
		return playersList.contains(p);
	}

	private String getArenaStatus(int mapId) {
		if (getGameStarted(mapId)) {
			return ChatColor.RED + "In-Game";
		}
		if (countdown.get(mapId) == -1) {
			return ChatColor.GREEN + "Wartend";
		}
		if (countdown.get(mapId) != -1) {
			return ChatColor.DARK_GREEN + "Countdown";
		}
		return "unbekannt";
	}

	public int getMaxArena() {
		return maxArena;
	}
}