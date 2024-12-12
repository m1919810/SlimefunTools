package me.matl114.SlimefunTools.implement;

import com.google.common.base.Charsets;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import me.matl114.matlib.Utils.ConfigLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;

public class Configs {
    public static void load(Plugin plugin) {
        Configs.plugin=plugin;
        init();
        //final File scAddonFile = new File(plugin.getDataFolder(), "language.yml");
        //copyFile(scAddonFile, "language");
        CONFIG=ConfigLoader.loadExternalConfig("config");
        ConfigLoader.copyFolder("item-base");
    }
    public static Plugin plugin;
    public static Config CONFIG;
    public static Config SERVER_CONFIG;
    public static void init() {
        SERVER_CONFIG=new Config(plugin);
    }
}
