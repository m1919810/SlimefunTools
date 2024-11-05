package me.matl114.SlimefunTools.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Files;

public class SerializeUtils {
    public static ItemStack deserializeFromString(String string) {
        try{
            YamlConfiguration itemConfig= new YamlConfiguration();
            itemConfig.loadFromString(string);
            return itemConfig.getItemStack("item");
        }catch (Throwable e){
            throw new RuntimeException("Unable to deserialize item: "+e.getMessage());
        }
    }
    public static String serializeToString(ItemStack item){
        YamlConfiguration itemConfig= new YamlConfiguration();
        itemConfig.set("item", item);
        return itemConfig.saveToString();
    }
}
