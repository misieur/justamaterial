package dev.misieur.justamaterialplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"UnstableApiUsage", "removal", "deprecation"})
public final class Plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // This code will generate the json files in src/main/resources/vanilla_materials
//        for (Material material : Material.values()) {
//            JsonObject materialObject = new JsonObject();
//            try {
//                materialObject.addProperty("id", material.getId());
//            } catch (Exception ignored) {
//            }
//            Class<?> data = material.data;
//            //noinspection removal
//            if (data != MaterialData.class) {
//                materialObject.addProperty("data", data.getName());
//            }
//            materialObject.addProperty("name", material.name());
//            materials.add(materialObject);
//        }
//        File file = new File(getDataFolder(), ".json");
//        file.getParentFile().mkdirs();
//        try (FileWriter writer = new FileWriter(file)) {
//            new Gson().toJson(materials, writer);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        String i = "";
        i += test(Material.COBBLESTONE);
        i += test(Material.PAPER);
        i += test(Material.ACACIA_BUTTON);
        i += test(Material.FIRE);
        i += test(Material.END_PORTAL);
        i += test(Material.LEGACY_ANVIL);
        i += test(Material.DIAMOND_CHESTPLATE);
        getLogger().info(i);
        // This will basically give us a string so we can compare on each version
        // if it gives us the same result with or without the Material class being replaced
        Bukkit.shutdown();
    }

    private static @NotNull String test(@NotNull Material material) {
        String i = ":";
        int count = 0;
        try {
            i += "`" + count++ + "`";
            i = add(i, material.name());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        } // 0
        try {
            i += "`" + count++ + "`";
            i = add(i, material.asBlockType());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.asItemType());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getCompostChance());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isCompostable());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getItemTranslationKey());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getBlockTranslationKey());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getTranslationKey());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getCreativeCategory());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getDefaultAttributeModifiers());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getEquipmentSlot());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        } // 10
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getCraftingRemainingItem());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getSlipperiness());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getBlastResistance());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getHardness());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isInteractable());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isItem());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.hasGravity());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isOccluding());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isFuel());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isBurnable());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        } // 20
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isFlammable());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isTransparent());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isAir());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isSolid());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isRecord());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isEdible());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isBlock());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getData());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.createBlockData());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getMaxDurability());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        } // 30
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getMaxStackSize());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getKey());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isLegacy());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getId());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.isCollidable());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.getItemRarity());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count++ + "`";
            i = add(i, material.translationKey());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        }
        try {
            i += "`" + count + "`";
            i = add(i, material.isEmpty());
        } catch (Exception | NoSuchMethodError | IllegalAccessError e) {
            i += e;
        } // 38
        return i + ":";
    }

    private static @NotNull String add(@NotNull String string, @Nullable Object object) {
        if (object == null) {
            return string + "null";
        } else {
            return string + object.toString().replaceAll("@[0-9a-f]{1,8}", ""); // Removes hash codes
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
