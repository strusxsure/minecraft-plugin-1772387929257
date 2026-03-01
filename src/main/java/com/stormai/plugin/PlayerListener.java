package com.stormai.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final Map<UUID, BukkitTask> activeEffects = new HashMap<>();
    private final Map<UUID, Set<Location>> lavaTraps = new HashMap<>();
    private final Map<UUID, Set<Location>> flameWalls = new HashMap<>();
    private final Map<UUID, Location> spawnZones = new HashMap<>();

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!isFireMace(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (event.getClickedBlock() != null) {
            handleBlockInteraction(player, event.getClickedBlock().getLocation());
        } else {
            activateFireMace(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isFireMace(player.getInventory().getItemInMainHand())) {
            return;
        }

        Location loc = player.getLocation();
        if (isNearLavaTrap(player) || isNearFlameWall(player) || isNearSpawnZone(player)) {
            applyHeatDamage(player);
        }
    }

    private void activateFireMace(Player player) {
        if (activeEffects.containsKey(player.getUniqueId())) {
            player.sendMessage("§cFire Mace is already active!");
            return;
        }

        player.sendMessage("§6Fire Mace activated!");
        BukkitTask task = new BukkitRunnable() {
            int duration = 0;
            final int maxDuration = 200; // 10 seconds

            @Override
            public void run() {
                if (duration >= maxDuration || !player.isOnline() || !isFireMace(player.getInventory().getItemInMainHand())) {
                    cancel();
                    activeEffects.remove(player.getUniqueId());
                    return;
                }

                duration++;
                applyAreaEffects(player);
            }
        }.runTaskTimer(plugin, 0L, 10L);

        activeEffects.put(player.getUniqueId(), task);
    }

    private void applyAreaEffects(Player player) {
        Location loc = player.getLocation();
        int radius = 5;

        // Heat damage over time
        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(loc) <= radius && entity != player) {
                entity.damage(1.0, player);
                entity.setFireTicks(60);
            }
        }

        // Create lava trap at player's feet
        Location below = loc.clone().subtract(0, 1, 0);
        if (below.getBlock().getType() == Material.AIR) {
            below.getBlock().setType(Material.LAVA);
            addLavaTrap(player, below);
        }

        // Spawn fire mob at random location
        Location spawnLoc = loc.clone().add(
            (Math.random() - 0.5) * 10,
            5,
            (Math.random() - 0.5) * 10
        );
        if (spawnLoc.getWorld() != null) {
            spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.BLAZE);
            spawnZones.put(player.getUniqueId(), spawnLoc);
        }

        // Create flame wall in front of player
        Location wallStart = loc.clone().add(player.getLocation().getDirection().multiply(3));
        for (int i = 0; i < 5; i++) {
            Location wallBlock = wallStart.clone().add(0, i, 0);
            if (wallBlock.getBlock().getType() == Material.AIR) {
                wallBlock.getBlock().setType(Material.CAMPFIRE);
                addFlameWall(player, wallBlock);
            }
        }
    }

    private void handleBlockInteraction(Player player, Location loc) {
        // Create lava trap on block interaction
        Location below = loc.clone().subtract(0, 1, 0);
        if (below.getBlock().getType() == Material.AIR) {
            below.getBlock().setType(Material.LAVA);
            addLavaTrap(player, below);
        }
    }

    private void applyHeatDamage(Player player) {
        player.damage(0.5, player);
        player.setFireTicks(20);
    }

    private boolean isNearLavaTrap(Player player) {
        Set<Location> traps = lavaTraps.get(player.getUniqueId());
        if (traps == null) return false;
        for (Location trap : traps) {
            if (trap.distance(player.getLocation()) < 3) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearFlameWall(Player player) {
        Set<Location> walls = flameWalls.get(player.getUniqueId());
        if (walls == null) return false;
        for (Location wall : walls) {
            if (wall.distance(player.getLocation()) < 4) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearSpawnZone(Player player) {
        Location zone = spawnZones.get(player.getUniqueId());
        return zone != null && zone.distance(player.getLocation()) < 8;
    }

    private void addLavaTrap(Player player, Location loc) {
        lavaTraps.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (lavaTraps.containsKey(player.getUniqueId())) {
                lavaTraps.get(player.getUniqueId()).remove(loc);
                if (loc.getBlock().getType() == Material.LAVA) {
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }, 200L);
    }

    private void addFlameWall(Player player, Location loc) {
        flameWalls.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (flameWalls.containsKey(player.getUniqueId())) {
                flameWalls.get(player.getUniqueId()).remove(loc);
                if (loc.getBlock().getType() == Material.CAMPFIRE) {
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }, 200L);
    }

    private boolean isFireMace(ItemStack item) {
        if (item == null || item.getType() != org.bukkit.Material.IRON_PICKAXE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§cFire Mace");
    }
}