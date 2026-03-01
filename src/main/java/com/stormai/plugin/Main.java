package com.stormai.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Fire and Ice Mace Plugin Enabled");
        getCommand("firemace").setExecutor(new CommandHandler(this));
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new IceMaceListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Fire and Ice Mace Plugin Disabled");
    }
}