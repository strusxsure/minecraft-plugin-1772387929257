package com.stormai.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class LightningMaceListener implements Listener {

    private final Main plugin;
    private final Map<UUID, BukkitTask> activeEffects = new HashMap<>();
    private final Map<UUID, Set<Location>> electrifiedFloors = new HashMap<>();
    private final Map<UUID, Set<Location>> lightningPuzzles = new HashMap<>();
    private final Map<UUID, List<Location>> puzzleSwitches = new HashMap<>();
    private final Map<Location, Boolean> switchStates = new HashMap<>();
    private final Map<UUID, Long> lastStormTime = new HashMap<>();
    private final Map<UUID, Set<LivingEntity>> chargedMobs = new HashMap<>();

    public LightningMaceListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!isLightningMace(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (event.getClickedBlock() != null) {
            handleBlockInteraction(player, event.getClickedBlock().getLocation());
        } else {
            activateLightningMace(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isLightningMace(player.getInventory().getItemInMainHand())) {
            return;
        }

        Location loc = player.getLocation();
        if (isNearElectrifiedFloor(player) || isNearLightningPuzzle(player)) {
            applyShockDamage(player);
        }
    }

    private void activateLightningMace(Player player) {
        if (activeEffects.containsKey(player.getUniqueId())) {
            player.sendMessage("§eLightning Mace is already active!");
            return;
        }

        player.sendMessage("§eLightning Mace activated!");
        BukkitTask task = new BukkitRunnable() {
            int duration = 0;
            final int maxDuration = 200; // 10 seconds

            @Override
            public void run() {
                if (duration >= maxDuration || !player.isOnline() || !isLightningMace(player.getInventory().getItemInMainHand())) {
                    cancel();
                    activeEffects.remove(player.getUniqueId());
                    return;
                }

                duration++;
                applyAreaEffects(player);
                checkStormMode(player);
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second

        activeEffects.put(player.getUniqueId(), task);
    }

    private void applyAreaEffects(Player player) {
        Location loc = player.getLocation();
        int radius = 8;

        // Random lightning strikes
        if (Math.random() < 0.3) { // 30% chance per second
            Location strikeLoc = loc.clone().add(
                (Math.random() - 0.5) * radius * 2,
                (Math.random() - 0.5) * 5,
                (Math.random() - 0.5) * radius * 2
            );
            strikeLightning(player, strikeLoc);
        }

        // Electrified floor around player
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location floorBlock = loc.clone().add(x, -1, z);
                if (floorBlock.getBlock().getType() == Material.AIR) {
                    floorBlock.getBlock().setType(Material.LIGHTNING_ROD);
                    addElectrifiedFloor(player, floorBlock);
                }
            }
        }

        // Charged mobs
        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(loc) <= radius && entity != player) {
                if (entity.getType() != EntityType.PLAYER) {
                    chargeMob(player, entity);
                }
            }
        }
    }

    private void checkStormMode(Player player) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastStormTime.get(player.getUniqueId());

        if (lastTime == null || (currentTime - lastTime) >= 120000) { // Every 2 minutes
            lastStormTime.put(player.getUniqueId(), currentTime);
            activateStormMode(player);
        }
    }

    private void activateStormMode(Player player) {
        player.sendMessage("§bStorm Mode activated! Lightning intensifies...");
        Location loc = player.getLocation();
        int radius = 15;

        // Increase lightning frequency
        for (int i = 0; i < 5; i++) {
            Location strikeLoc = loc.clone().add(
                (Math.random() - 0.5) * radius * 2,
                (Math.random() - 0.5) * 5,
                (Math.random() - 0.5) * radius * 2
            );
            strikeLightning(player, strikeLoc);
        }

        // Reduce visibility
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));
        }

        // Increase mob spawn
        for (int i = 0; i < 10; i++) {
            Location spawnLoc = loc.clone().add(
                (Math.random() - 0.5) * 20,
                10,
                (Math.random() - 0.5) * 20
            );
            if (spawnLoc.getWorld() != null) {
                spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            }
        }
    }

    private void handleBlockInteraction(Player player, Location loc) {
        // Create electrified floor on block interaction
        Location below = loc.clone().subtract(0, 1, 0);
        if (below.getBlock().getType() == Material.AIR) {
            below.getBlock().setType(Material.LIGHTNING_ROD);
            addElectrifiedFloor(player, below);
        }

        // Check if it's a switch for lightning puzzle
        if (below.getBlock().getType() == Material.STONE_BUTTON || below.getBlock().getType() == Material.OAK_BUTTON) {
            handlePuzzleSwitch(player, below);
        }
    }

    private void handlePuzzleSwitch(Player player, Location switchLoc) {
        List<Location> switches = puzzleSwitches.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        if (!switches.contains(switchLoc)) {
            switches.add(switchLoc);
        }

        // Toggle switch state
        boolean currentState = switchStates.getOrDefault(switchLoc, false);
        switchStates.put(switchLoc, !currentState);

        // Check if puzzle is complete
        if (switches.size() == 4) { // Assuming 4-switch puzzle
            boolean allCorrect = true;
            for (Location s : switches) {
                if (!switchStates.getOrDefault(s, false)) {
                    allCorrect = false;
                    break;
                }
            }

            if (allCorrect) {
                player.sendMessage("§aPuzzle solved! Gate opened.");
                // Open gate logic here
            } else {
                player.sendMessage("§cWrong sequence! Lightning strike!");
                strikeLightning(player, player.getLocation());
            }
        }
    }

    private void applyShockDamage(Player player) {
        player.damage(1.0, player);
        player.setFireTicks(20);
    }

    private boolean isNearElectrifiedFloor(Player player) {
        Set<Location> floors = electrifiedFloors.get(player.getUniqueId());
        if (floors == null) return false;
        for (Location floor : floors) {
            if (floor.distance(player.getLocation()) < 2) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearLightningPuzzle(Player player) {
        Set<Location> puzzles = lightningPuzzles.get(player.getUniqueId());
        if (puzzles == null) return false;
        for (Location puzzle : puzzles) {
            if (puzzle.distance(player.getLocation()) < 4) {
                return true;
            }
        }
        return false;
    }

    private void addElectrifiedFloor(Player player, Location loc) {
        electrifiedFloors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (electrifiedFloors.containsKey(player.getUniqueId())) {
                electrifiedFloors.get(player.getUniqueId()).remove(loc);
                if (loc.getBlock().getType() == Material.LIGHTNING_ROD) {
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }, 600L); // 30 seconds
    }

    private void addLightningPuzzle(Player player, Location loc) {
        lightningPuzzles.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loc);
    }

    private void strikeLightning(Player player, Location loc) {
        if (loc.getWorld() != null) {
            loc.getWorld().strikeLightning(loc);
        }
    }

    private void chargeMob(Player player, LivingEntity entity) {
        if (chargedMobs.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(entity)) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 1));
            player.sendMessage("§eMob charged with lightning energy!");
        }
    }

    private boolean isLightningMace(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_PICKAXE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§eLightning Mace");
    }
}