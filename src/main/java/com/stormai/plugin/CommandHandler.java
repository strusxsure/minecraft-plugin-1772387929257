package com.stormai.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CommandHandler implements CommandExecutor {

    private final Main plugin;

    public CommandHandler(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("firemace.give") && !player.hasPermission("icemace.give")) {
            player.sendMessage("§cYou don't have permission to use this command");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /firemace or /icemace");
            return true;
        }

        String maceType = args[0].toLowerCase();
        ItemStack mace;

        switch (maceType) {
            case "fire":
                mace = createFireMace();
                player.sendMessage("§6You have received the Fire Mace!");
                break;
            case "ice":
                mace = createIceMace();
                player.sendMessage("§bYou have received the Ice Mace!");
                break;
            default:
                player.sendMessage("§cUnknown mace type. Use 'fire' or 'ice'.");
                return true;
        }

        player.getInventory().addItem(mace);
        return true;
    }

    private ItemStack createFireMace() {
        ItemStack mace = new ItemStack(org.bukkit.Material.IRON_PICKAXE);
        org.bukkit.inventory.meta.ItemMeta meta = mace.getItemMeta();
        meta.setDisplayName("§cFire Mace");
        meta.setCustomModelData(1001);
        mace.setItemMeta(meta);
        return mace;
    }

    private ItemStack createIceMace() {
        ItemStack mace = new ItemStack(org.bukkit.Material.IRON_PICKAXE);
        org.bukkit.inventory.meta.ItemMeta meta = mace.getItemMeta();
        meta.setDisplayName("§bIce Mace");
        meta.setCustomModelData(1002);
        mace.setItemMeta(meta);
        return mace;
    }
}