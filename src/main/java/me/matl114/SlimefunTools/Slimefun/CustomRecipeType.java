package me.matl114.SlimefunTools.Slimefun;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;

public class CustomRecipeType extends RecipeType {
    @Getter
    private final HashMap<ItemStack[], ItemStack> recipes = new LinkedHashMap();
    private final List<BiConsumer<ItemStack[], ItemStack>> registerCallback = new ArrayList();
    private final List<BiConsumer<ItemStack[], ItemStack>> unregisterCallback = new ArrayList();
    public CustomRecipeType(NamespacedKey key, ItemStack item) {
        super(key, item);
    }

    public void register(ItemStack[] recipe, ItemStack result) {
        super.register(recipe, result);
        this.registerCallback.forEach((c) -> {
            c.accept(recipe, result);
        });
        this.recipes.put(recipe, result);
    }

    @Override
    public void unregister(ItemStack[] recipe, ItemStack result) {
        super.unregister(recipe, result);
        this.unregisterCallback.forEach((c) -> {
            c.accept(recipe, result);
        });
        this.recipes.remove(recipe);
    }
    public void unregisterRecipeType(){
        for(Map.Entry<ItemStack[], ItemStack> entry : this.recipes.entrySet()){
            super.unregister(entry.getKey(), entry.getValue());
            this.unregisterCallback.forEach((c) -> {
                c.accept(entry.getKey(), entry.getValue());
            });
        }
        this.recipes.clear();
        this.unregisterCallback.clear();
        this.registerCallback.clear();
    }

    public CustomRecipeType relatedTo(BiConsumer<ItemStack[], ItemStack> regCallback, BiConsumer<ItemStack[],ItemStack> unregCallback) {
        this.recipes.forEach(regCallback);
        this.registerCallback.add(regCallback);
        this.unregisterCallback.add(unregCallback);
        return this;
    }


}
