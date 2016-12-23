/*
 * Copyright (C) 2013-2016 XHawk87
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.xhawk87.Coinage.moneybags;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import me.xhawk87.Coinage.Coinage;
import me.xhawk87.Coinage.utils.FileUpdater;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author XHawk87
 */
public class MoneyBag implements InventoryHolder {

    private String id;
    private Coinage plugin;
    private Inventory inventory;
    private File file;
    private FileUpdater fileUpdater;

    public MoneyBag(Coinage plugin, String id, int size, String title) {
        this(plugin, id);
        inventory = plugin.getServer().createInventory(this, size, title);
    }

    public MoneyBag(Coinage plugin, String id) {
        this.plugin = plugin;
        this.id = id;
        this.file = new File(new File(plugin.getDataFolder(), "moneybags"), id + ".yml");
        this.fileUpdater = new FileUpdater(file);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void load() {
        new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (file) {
                    final FileConfiguration data = new YamlConfiguration();
                    try {
                        data.load(file);
                    } catch (IOException | InvalidConfigurationException ex) {
                        plugin.getLogger().log(Level.SEVERE, "Could not load " + file.getPath(), ex);
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            onLoad(data);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void save() {
        FileConfiguration data = new YamlConfiguration();
        data.set("size", inventory.getSize());
        data.set("title", inventory.getTitle());
        ItemStack[] contents = inventory.getContents();
        ConfigurationSection contentsData = data.createSection("contents");
        for (int i = 0; i < contents.length; i++) {
            ItemStack coin = contents[i];
            if (coin != null && coin.getTypeId() != 0) {
                contentsData.set(Integer.toString(i), coin);
            }
        }
        fileUpdater.save(plugin, data.saveToString());
    }

    public void onLoad(FileConfiguration data) {
        int size = data.getInt("size");
        String title = data.getString("title");
        inventory = plugin.getServer().createInventory(this, size, title);
        ConfigurationSection contentsData = data.getConfigurationSection("contents");
        for (String key : contentsData.getKeys(false)) {
            int slot = Integer.parseInt(key);
            ItemStack coin = contentsData.getItemStack(key);
            inventory.setItem(slot, coin);
        }
    }

    public void checkCoins(Inventory out) {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack coin = contents[i];
            if (coin == null || coin.getTypeId() == 0) {
                continue;
            }
            if (plugin.getDenominationOfCoin(coin) == null) {
                inventory.clear(i);
                out.addItem(coin);
            }
        }

        // Update player inventory immediately
        if (out.getHolder() instanceof Player) {
            Player player = (Player) out.getHolder();
            player.updateInventory();
        }

        save();
    }

    public String getId() {
        return id;
    }

    public static Recipe createMoneyBagType(String title, int size, int itemId, int itemData, String[] shape, Map<Character, ItemStack> ingredients) {
        ItemStack result = new ItemStack(itemId, 1, (short) itemData);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(title);
        String lore = MoneyBag.encodeLore("moneybag," + size) + ChatColor.LIGHT_PURPLE + "Right-click while holding to open";
        List<String> loreStrings = new ArrayList<>();
        loreStrings.add(lore);
        meta.setLore(loreStrings);
        meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 1, true);
        result.setItemMeta(meta);
        ShapedRecipe recipe = new ShapedRecipe(result);
        recipe.shape(shape);
        for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            ItemStack material = entry.getValue();
            recipe.setIngredient(entry.getKey(), material.getType(), material.getDurability());
        }
        return recipe;
    }

    public static Recipe createMoneyBagType(String title, int size, int itemId, int itemData, List<ItemStack> ingredients) {
        ItemStack result = new ItemStack(itemId, 1, (short) itemData);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(title);
        String lore = MoneyBag.encodeLore("moneybag," + size) + ChatColor.LIGHT_PURPLE + "Right-click while holding to open";
        List<String> loreStrings = new ArrayList<>();
        loreStrings.add(lore);
        meta.setLore(loreStrings);
        meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 1, true);
        result.setItemMeta(meta);
        ShapelessRecipe recipe = new ShapelessRecipe(result);
        for (ItemStack material : ingredients) {
            recipe.addIngredient(material.getType(), material.getDurability());
        }
        return recipe;
    }

    public static Recipe loadMoneyBagType(ConfigurationSection data) {
        String title = data.getString("title");
        int size = data.getInt("size", -1);
        int itemId = data.getInt("item-id", -1);
        int itemData = data.getInt("item-data", -1);
        if (title == null || size == -1 || itemId == -1 || itemData == -1) {
            return null;
        }
        ItemStack result = new ItemStack(itemId, 1, (short) itemData);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(title);
        String lore = MoneyBag.encodeLore("moneybag," + size) + ChatColor.LIGHT_PURPLE + "Right-click while holding to open";
        List<String> loreStrings = new ArrayList<>();
        loreStrings.add(lore);
        meta.setLore(loreStrings);
        meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 1, true);
        result.setItemMeta(meta);
        if (data.isConfigurationSection("shaped-recipe")) {
            ConfigurationSection shapedSection = data.getConfigurationSection("shaped-recipe");
            ShapedRecipe shapedRecipe = new ShapedRecipe(result);
            List<String> lines = shapedSection.getStringList("shape");
            shapedRecipe.shape(lines.toArray(new String[lines.size()]));
            ConfigurationSection materials = shapedSection.getConfigurationSection("materials");
            for (String key : materials.getKeys(false)) {
                String[] materialParts = materials.getString(key).split(":");
                int materialId = Integer.parseInt(materialParts[0]);
                int materialData = 0;
                if (materialParts.length == 2) {
                    materialData = Integer.parseInt(materialParts[1]);
                }
                Material material = Material.getMaterial(materialId);
                shapedRecipe.setIngredient(key.charAt(0), material, materialData);
            }
            return shapedRecipe;
        } else if (data.isConfigurationSection("shapeless-recipe")) {
            ConfigurationSection shapelessSection = data.getConfigurationSection("shapeless-recipe");
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(result);
            List<String> materials = shapelessSection.getStringList("materials");
            for (String materialString : materials) {
                String[] materialParts = materialString.split(":");
                int materialId = Integer.parseInt(materialParts[0]);
                int materialData = 0;
                if (materialParts.length == 2) {
                    materialData = Integer.parseInt(materialParts[1]);
                }
                Material material = Material.getMaterial(materialId);
                shapelessRecipe.addIngredient(material, materialData);
            }
            return shapelessRecipe;
        } else {
            return null;
        }
    }

    public static void saveMoneyBagType(ConfigurationSection data, Recipe recipe) {
        ItemStack result = recipe.getResult();
        ItemMeta meta = result.getItemMeta();
        List<String> loreStrings = meta.getLore();
        String lore = loreStrings.get(0);
        String sizeString = MoneyBag.decodeLore(lore).split(",")[1];
        int size = Integer.parseInt(sizeString);

        data.set("title", meta.getDisplayName());
        data.set("size", size);
        data.set("item-id", result.getTypeId());
        data.set("item-data", result.getDurability());

        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
            ConfigurationSection shapedSection = data.createSection("shaped-recipe");
            shapedSection.set("shape", Arrays.asList(shapedRecipe.getShape()));

            ConfigurationSection materials = shapedSection.createSection("materials");
            for (Map.Entry<Character, ItemStack> entry : shapedRecipe.getIngredientMap().entrySet()) {
                String key = entry.getKey().toString();
                ItemStack material = entry.getValue();
                int materialId = material.getTypeId();
                int materialData = material.getDurability();
                materials.set(key, materialId + ":" + materialData);
            }
        } else if (recipe instanceof ShapelessRecipe) {
            ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
            ConfigurationSection shapelessSection = data.createSection("shapeless-recipe");
            List<String> materialList = new ArrayList<>();
            for (ItemStack material : shapelessRecipe.getIngredientList()) {
                int materialId = material.getTypeId();
                int materialData = material.getDurability();
                materialList.add(materialId + ":" + materialData);
            }
            shapelessSection.set("materials", materialList);
        }
    }

    public static String decodeLore(String lore) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lore.length(); i++) {
            char c = lore.charAt(i);
            if (c == ChatColor.COLOR_CHAR) {
                i++;
                char d = lore.charAt(i);
                if (d == ';') {
                    return sb.toString();
                } else {
                    sb.append(d);
                }
            }
        }
        return "";
    }

    public static String encodeLore(String data) {
        StringBuilder sb = new StringBuilder();
        for (char c : data.toCharArray()) {
            sb.append(ChatColor.COLOR_CHAR);
            sb.append(c);
        }
        sb.append(ChatColor.COLOR_CHAR);
        sb.append(';');
        return sb.toString();
    }
}
