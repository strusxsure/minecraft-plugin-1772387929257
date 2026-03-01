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

public class IceMaceListener implements Listener {

    private final Main plugin;
    private final Map<UUID, BukkitTask> activeEffects = new HashMap<>();
    private final Map<UUID, Set<Location>> slipperyFloors = new HashMap<>();
    private final Map<UUID, Set<Location>> fallingBlocks = new HashMap<>();
    private final Map<UUID, Set<Location>> snowBlindnessAreas = new HashMap<>();
    private final Map<UUID, Integer> freezeStacks = new HashMap<>();

    public IceMaceListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!isIceMace(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (event.getClickedBlock() != null) {
            handleBlockInteraction(player, event.getClickedBlock().getLocation());
        } else {
            activateIceMace(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isIceMace(player.getInventory().getItemInMainHand())) {
            return;
        }

        Location loc = player.getLocation();
        if (isNearSlipperyFloor(player) || isNearFallingBlocks(player) || isNearSnowBlindnessArea(player)) {
            applyIceEffects(player);
        }
    }

    private void activateIceMace(Player player) {
        if (activeEffects.containsKey(player.getUniqueId())) {
            player.sendMessage("§bIce Mace is already active!");
            return;
        }

        player.sendMessage("§bIce Mace activated!");
        BukkitTask task = new BukkitRunnable() {
            int duration = 0;
            final int maxDuration = 200; // 10 seconds

            @Override
            public void run() {
                if (duration >= maxDuration || !player.isOnline() || !isIceMace(player.getInventory().getItemInMainHand())) {
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

        // Apply freeze debuff to nearby entities
        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.getLocation().distance(loc) <= radius && entity != player) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 2));
                // Stack freeze effect
                if (entity instanceof Player) {
                    Player p = (Player) entity;
                    freezeStacks.put(p.getUniqueId(), freezeStacks.getOrDefault(p.getUniqueId(), 0) + 1);
                    if (freezeStacks.get(p.getUniqueId()) >= 3) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                    }
                }
            }
        }

        // Create slippery floor around player
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location floorBlock = loc.clone().add(x, -1, z);
                if (floorBlock.getBlock().getType() == Material.AIR) {
                    floorBlock.getBlock().setType(Material.ICE);
                    addSlipperyFloor(player, floorBlock);
                }
            }
        }

        // Spawn falling blocks around player
        for (int i = 0; i < 3; i++) {
            Location fallingBlockLoc = loc.clone().add(
                (Math.random() - 0.5) * 10,
                10,
                (Math.random() - 0.5) * 10
            );
            if (fallingBlockLoc.getWorld() != null) {
                fallingBlockLoc.getWorld().spawnFallingBlock(fallingBlockLoc, Material.PACKED_ICE.createBlockData());
                addFallingBlock(player, fallingBlockLoc);
            }
        }

        // Create snow blindness area
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location snowBlock = loc.clone().add(x, 0, z);
                if (snowBlock.getBlock().getType() == Material.AIR) {
                    snowBlock.getBlock().setType(Material.SNOW);
                    addSnowBlindnessArea(player, snowBlock);
                }
            }
        }
    }

    private void handleBlockInteraction(Player player, Location loc) {
        // Create slippery floor on block interaction
        Location below = loc.clone().subtract(0, 1, 0);
        if (below.getBlock().getType() == Material.AIR) {
            below.getBlock().setType(Material.ICE);
            addSlipperyFloor(player, below);
        }
    }

    private void applyIceEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
    }

    private boolean isNearSlipperyFloor(Player player) {
        Set<Location> floors = slipperyFloors.get(player.getUniqueId());
        if (floors == null) return false;
        for (Location floor : floors) {
            if (floor.distance(player.getLocation()) < 2) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearFallingBlocks(Player player) {
        Set<Location> blocks = fallingBlocks.get(player.getUniqueId());
        if (blocks == null) return false;
        for (Location block : blocks) {
            if (block.distance(player.getLocation()) < 4) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearSnowBlindnessArea(Player player) {
        Set<Location> areas = snowBlindnessAreas.get(player.getUniqueId());
        if (areas == null) return false;
        for (Location area : areas) {
            if (area.distance(player.getLocation()) < 3) {
                return true;
            }
        }
        return false;
    }

    private void addSlipperyFloor(Player player, Location loc) {
        slipperyFloors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (slipperyFloors.containsKey(player.getUniqueId())) {
                slipperyFloors.get(player.getUniqueId()).remove(loc);
                if (loc.getBlock().getType() == Material.ICE) {
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }, 200L);
    }

    private void addFallingBlock(Player player, Location loc) {
        fallingBlocks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (fallingBlocks.containsKey(player.getUniqueId())) {
                fallingBlocks.get(player.getUniqueId()).remove(loc);
            }
        }, 100L);
    }

    private void addSnowBlindnessArea(Player player, Location loc) {
        snowBlindnessAreas.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loc);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (snowBlindnessAreas.containsKey(player.getUniqueId())) {
                snowBlindnessAreas.get(player.getUniqueId()).remove(loc);
                if (loc.getBlock().getType() == Material.SNOW) {
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }, 200L);
    }

    private boolean isIceMace(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType() != org.bukkit.Material.IRON_PICKAXE) {
            return false;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§bIce Mace");
    }
}