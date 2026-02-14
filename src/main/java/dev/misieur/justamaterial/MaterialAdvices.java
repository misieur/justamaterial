package dev.misieur.justamaterial;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class MaterialAdvices {

    @SuppressWarnings({"removal", "unchecked"})
    public static class constructor {
        @Advice.OnMethodExit
        static void exit(@Advice.This Material material,
                         @Advice.FieldValue(value = "id", readOnly = false) int id,
                         @Advice.FieldValue(value = "ctor", readOnly = false) Constructor<? extends org.bukkit.material.MaterialData> ctor,
                         @Advice.FieldValue(value = "maxStack", readOnly = false) int maxStack,
                         @Advice.FieldValue(value = "data", readOnly = false) Class<?> data,
                         @Advice.FieldValue(value = "legacy", readOnly = false) boolean legacy,
                         @Advice.FieldValue(value = "key", readOnly = false) NamespacedKey key,
                         @Advice.FieldValue("LEGACY_PREFIX") String LEGACY_PREFIX,
                         @Advice.FieldValue(value = "DATA_CACHE", readOnly = false) Map<String, Triple<Integer, String, NamespacedKey>> DATA_CACHE) {
            if (DATA_CACHE == null) {
                try {
                    Field classLoaderField = Class.forName("dev.misieur.justamaterial.GeneratedForBukkit").getDeclaredField("CLASSLOADER");
                    classLoaderField.setAccessible(true);
                    Field dataField = Class.forName("dev.misieur.justamaterial.Materials", true, (ClassLoader) classLoaderField.get(null)).getDeclaredField("DATA");
                    dataField.setAccessible(true);
                    DATA_CACHE = (Map<String, Triple<Integer, String, NamespacedKey>>) dataField.get(null);
                } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            Triple<Integer, String, NamespacedKey> triple = DATA_CACHE.get(material.name());
            if (triple == null) throw new RuntimeException("Material " + material.name() + " not found!");

            id = triple.getLeft() != null ? triple.getLeft() : -1;
            legacy = material.name().startsWith(LEGACY_PREFIX);
            key = triple.getRight() != null ? triple.getRight() : NamespacedKey.minecraft(material.name().toLowerCase(Locale.ROOT));

            String className = triple.getMiddle();
            if (className == null) data = org.bukkit.material.MaterialData.class;
            else {
                try {
                    data = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                if (org.bukkit.material.MaterialData.class.isAssignableFrom(data)) {
                    ctor = (Constructor<? extends org.bukkit.material.MaterialData>) data.getConstructor(Material.class, byte.class);
                } else {
                    ctor = null;
                }
            } catch (NoSuchMethodException | SecurityException ex) {
                throw new AssertionError(ex);
            }
            maxStack = -1; // Will be calculated using the item when Material#getMaxStackSize() is called
        }
    }

    public static class staticInitializer {
        @Advice.OnMethodExit
        public static void exit(@Advice.FieldValue(value = "BY_NAME", readOnly = false) Map<String, Material> BY_NAME) {
            Map<String, Material> byName = HashMap.newHashMap(Material.values().length);
            for (Material material : Material.values()) {
                byName.put(material.name(), material);
            }
            BY_NAME = Map.copyOf(byName);
        }
    }

    public static class createBlockData {
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) BlockData returned,
                                @Advice.This Material material) {
            returned = Bukkit.createBlockData(material);
        }
    }

    public static class createBlockData_consumer {
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) BlockData returned,
                                @Advice.This Material material,
                                @Advice.Argument(0) Consumer<? super BlockData> consumer) {
            returned = Bukkit.createBlockData(material, consumer);
        }
    }

    public static class createBlockData_data {
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) BlockData returned,
                                @Advice.This Material material,
                                @Advice.Argument(0) String data) {
            returned = Bukkit.createBlockData(material, data);
        }
    }

    @SuppressWarnings("deprecation")
    public static class getMaterial {
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) Material returned,
                                @Advice.FieldValue(value = "BY_NAME") Map<String, Material> BY_NAME,
                                @Advice.FieldValue(value = "LEGACY_PREFIX") String LEGACY_PREFIX,
                                @Advice.Argument(value = 0, readOnly = false) String name,
                                @Advice.Argument(1) boolean legacyName) {
            if (legacyName) {
                if (!name.startsWith(LEGACY_PREFIX)) {
                    name = LEGACY_PREFIX + name;
                }

                Material match = BY_NAME.get(name);
                returned = Bukkit.getUnsafe().fromLegacy(match);
                return;
            }

            returned = BY_NAME.get(name);
        }
    }

    public static class matchMaterial {
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) Material returned,
                                @Advice.Argument(0) String name,
                                @Advice.Argument(1) boolean legacyName) {
            Preconditions.checkArgument(name != null, "Name cannot be null");

            String filtered = name;
            if (filtered.startsWith(NamespacedKey.MINECRAFT + ":")) {
                filtered = filtered.substring((NamespacedKey.MINECRAFT + ":").length());
            }

            filtered = filtered.toUpperCase(Locale.ROOT);
            filtered = filtered.replaceAll("\\s+", "_").replaceAll(":+", "_").replaceAll("\\W", "");
            returned = Material.getMaterial(filtered, legacyName);
        }
    }

    @SuppressWarnings("removal")
    public static class getNewData {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) org.bukkit.material.MaterialData returned,
                         @Advice.This Material material,
                         @Advice.Argument(0) byte raw,
                         @Advice.FieldValue("ctor") Constructor<? extends org.bukkit.material.MaterialData> ctor) {
            Preconditions.checkArgument(material.isLegacy(), "Cannot get new data of Modern Material");
            try {
                returned = ctor.newInstance(material, raw);
            } catch (InstantiationException ex) {
                final Throwable t = ex.getCause();
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw new AssertionError(t);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    public static class isCollidable {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            if (material.isBlock()) {
                returned = material.asBlockType().hasCollision();
                return;
            }
            throw new IllegalArgumentException(material + " isn't a block type");
        }
    }

    public static class getDefaultAttributeModifiers {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) Multimap<Attribute, AttributeModifier> returned,
                         @Advice.This Material material,
                         @Nullable @Advice.Argument(0) EquipmentSlot slot) {
            ItemType type = material.asItemType();
            Preconditions.checkArgument(type != null, "The Material is not an item!");
            returned = slot != null ? type.getDefaultAttributeModifiers(slot) : type.getDefaultAttributeModifiers();
        }
    }

    @SuppressWarnings("removal")
    public static class getItemRarity {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) io.papermc.paper.inventory.ItemRarity returned,
                         @Advice.This Material material) {
            returned = new org.bukkit.inventory.ItemStack(material).getRarity();
        }
    }

    public static class getTranslationKey {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) String returned,
                         @Advice.This Material material) {
            if (material.isItem()) {
                returned = java.util.Objects.requireNonNull(material.asItemType()).translationKey();
            } else {
                returned = java.util.Objects.requireNonNull(material.asBlockType()).translationKey();
            }
        }
    }

    public static class getMaxDurability {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) short returned,
                         @Advice.This Material material) {
            ItemType type = material.asItemType();
            returned = type == null ? 0 : type.getMaxDurability();
        }
    }

    public static class isBlock {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            returned = material.asBlockType() != null;
        }
    }

    public static class isEdible {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            ItemType type = material.asItemType();
            returned = type != null && type.isEdible();
        }
    }

    public static class isRecord {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            ItemType type = material.asItemType();
            returned = type != null && type.isRecord();
        }
    }

    public static class isSolid {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isSolid();
        }
    }

    public static class isAir {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isAir();
        }
    }

    @SuppressWarnings("removal")
    public static class isTransparent {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            if (!material.isBlock()) {
                returned = false;
                return;
            }
            returned = switch (material) {
                case ACACIA_BUTTON, ACACIA_SAPLING, ACTIVATOR_RAIL, AIR, ALLIUM, ATTACHED_MELON_STEM,
                     ATTACHED_PUMPKIN_STEM,
                     AZURE_BLUET, BARRIER, BEETROOTS, BIRCH_BUTTON, BIRCH_SAPLING, BLACK_CARPET, BLUE_CARPET,
                     BLUE_ORCHID,
                     BROWN_CARPET, BROWN_MUSHROOM, CARROTS, CAVE_AIR, CHORUS_FLOWER, CHORUS_PLANT, COCOA, COMPARATOR,
                     CREEPER_HEAD, CREEPER_WALL_HEAD, CYAN_CARPET, DANDELION, DARK_OAK_BUTTON, DARK_OAK_SAPLING,
                     DEAD_BUSH,
                     DETECTOR_RAIL, DRAGON_HEAD, DRAGON_WALL_HEAD, END_GATEWAY, END_PORTAL, END_ROD, FERN, FIRE,
                     FLOWER_POT,
                     GRAY_CARPET, GREEN_CARPET, JUNGLE_BUTTON, JUNGLE_SAPLING, LADDER, LARGE_FERN, LEVER,
                     LIGHT_BLUE_CARPET,
                     LIGHT_GRAY_CARPET, LILAC, LILY_PAD, LIME_CARPET, MAGENTA_CARPET, MELON_STEM, NETHER_PORTAL,
                     NETHER_WART, OAK_BUTTON, OAK_SAPLING, ORANGE_CARPET, ORANGE_TULIP, OXEYE_DAISY, PEONY, PINK_CARPET,
                     PINK_TULIP, PLAYER_HEAD, PLAYER_WALL_HEAD, POPPY, POTATOES, POTTED_ACACIA_SAPLING, POTTED_ALLIUM,
                     POTTED_AZALEA_BUSH, POTTED_AZURE_BLUET, POTTED_BIRCH_SAPLING, POTTED_BLUE_ORCHID,
                     POTTED_BROWN_MUSHROOM, POTTED_CACTUS, POTTED_DANDELION, POTTED_DARK_OAK_SAPLING, POTTED_DEAD_BUSH,
                     POTTED_FERN, POTTED_FLOWERING_AZALEA_BUSH, POTTED_JUNGLE_SAPLING, POTTED_OAK_SAPLING,
                     POTTED_ORANGE_TULIP, POTTED_OXEYE_DAISY, POTTED_PINK_TULIP, POTTED_POPPY, POTTED_RED_MUSHROOM,
                     POTTED_RED_TULIP, POTTED_SPRUCE_SAPLING, POTTED_WHITE_TULIP, POWERED_RAIL, PUMPKIN_STEM,
                     PURPLE_CARPET,
                     RAIL, REDSTONE_TORCH, REDSTONE_WALL_TORCH, REDSTONE_WIRE, RED_CARPET, RED_MUSHROOM, RED_TULIP,
                     REPEATER, ROSE_BUSH, SHORT_GRASS, SKELETON_SKULL, SKELETON_WALL_SKULL, SNOW, SPRUCE_BUTTON,
                     SPRUCE_SAPLING, STONE_BUTTON, STRUCTURE_VOID, SUGAR_CANE, SUNFLOWER, TALL_GRASS, TORCH, TRIPWIRE,
                     TRIPWIRE_HOOK, VINE, VOID_AIR, WALL_TORCH, WHEAT, WHITE_CARPET, WHITE_TULIP, WITHER_SKELETON_SKULL,
                     WITHER_SKELETON_WALL_SKULL, YELLOW_CARPET, ZOMBIE_HEAD, ZOMBIE_WALL_HEAD, LEGACY_AIR,
                     LEGACY_SAPLING,
                     LEGACY_POWERED_RAIL, LEGACY_DETECTOR_RAIL, LEGACY_LONG_GRASS, LEGACY_DEAD_BUSH,
                     LEGACY_YELLOW_FLOWER,
                     LEGACY_RED_ROSE, LEGACY_BROWN_MUSHROOM, LEGACY_RED_MUSHROOM, LEGACY_TORCH, LEGACY_FIRE,
                     LEGACY_REDSTONE_WIRE, LEGACY_CROPS, LEGACY_LADDER, LEGACY_RAILS, LEGACY_LEVER,
                     LEGACY_REDSTONE_TORCH_OFF, LEGACY_REDSTONE_TORCH_ON, LEGACY_STONE_BUTTON, LEGACY_SNOW,
                     LEGACY_SUGAR_CANE_BLOCK, LEGACY_PORTAL, LEGACY_DIODE_BLOCK_OFF, LEGACY_DIODE_BLOCK_ON,
                     LEGACY_PUMPKIN_STEM, LEGACY_MELON_STEM, LEGACY_VINE, LEGACY_WATER_LILY, LEGACY_NETHER_WARTS,
                     LEGACY_ENDER_PORTAL, LEGACY_COCOA, LEGACY_TRIPWIRE_HOOK, LEGACY_TRIPWIRE, LEGACY_FLOWER_POT,
                     LEGACY_CARROT, LEGACY_POTATO, LEGACY_WOOD_BUTTON, LEGACY_SKULL, LEGACY_REDSTONE_COMPARATOR_OFF,
                     LEGACY_REDSTONE_COMPARATOR_ON, LEGACY_ACTIVATOR_RAIL, LEGACY_CARPET, LEGACY_DOUBLE_PLANT,
                     LEGACY_END_ROD, LEGACY_CHORUS_PLANT, LEGACY_CHORUS_FLOWER, LEGACY_BEETROOT_BLOCK,
                     LEGACY_END_GATEWAY,
                     LEGACY_STRUCTURE_VOID -> true;
                default -> false;
            };
        }
    }

    public static class isFlammable {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isFlammable();
        }
    }

    public static class isBurnable {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isBurnable();
        }
    }

    public static class isFuel {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            ItemType type = material.asItemType();
            returned = type != null && type.isFuel();
        }
    }

    public static class isOccluding {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isOccluding();
        }
    }

    public static class hasGravity {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.hasGravity();
        }
    }

    public static class isItem {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            returned = material.asItemType() != null;
        }
    }

    @SuppressWarnings("deprecation")
    public static class isInteractable {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isInteractable();
        }
    }

    public static class getHardness {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) float returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            Preconditions.checkArgument(type != null, "The Material is not a block!");
            returned = type.getHardness();
        }
    }

    public static class getBlastResistance {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) float returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            Preconditions.checkArgument(type != null, "The Material is not a block!");
            returned = type.getBlastResistance();
        }
    }

    public static class getSlipperiness {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) float returned,
                         @Advice.This Material material) {
            BlockType type = material.asBlockType();
            Preconditions.checkArgument(type != null, "The Material is not a block!");
            returned = type.getSlipperiness();
        }
    }

    @SuppressWarnings("deprecation")
    public static class getCraftingRemainingItem {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) Material returned,
                         @Advice.This Material material) {
            ItemType type = material.asItemType();
            Preconditions.checkArgument(type != null, "The Material is not an item!");
            returned = type.getCraftingRemainingItem() == null ? null : type.getCraftingRemainingItem().asMaterial();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class getEquipmentSlot {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) EquipmentSlot returned,
                         @Advice.This Material material) {
            ItemType type = material.asItemType();
            Preconditions.checkArgument(type != null, "The Material is not an item!");
            Equippable equippable = type.getDefaultData(DataComponentTypes.EQUIPPABLE);
            returned = equippable == null ? EquipmentSlot.HAND : equippable.slot();
        }
    }

    @SuppressWarnings("removal")
    public static class getCreativeCategory {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) CreativeCategory returned,
                         @Advice.This Material material) {
            ItemType type = material.asItemType();
            returned = type == null ? null : type.getCreativeCategory();
        }
    }

    public static class getBlockTranslationKey {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) String returned,
                         @Advice.This Material material) {
            returned = material.isBlock() ? material.translationKey() : null;
        }
    }

    public static class getItemTranslationKey {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) String returned,
                         @Advice.This Material material) {
            returned = material.isItem() ? material.translationKey() : null;
        }
    }

    public static class isCompostable {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material) {
            returned = material.isItem() && material.asItemType().isCompostable();
        }
    }

    public static class getCompostChance {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) float returned,
                         @Advice.This Material material) {
            ItemType type = material.asItemType();
            Preconditions.checkArgument(type != null, "The Material is not an item!");
            returned = type.getCompostChance();
        }
    }

    @SuppressWarnings({"removal", "deprecation"})
    public static class asItemType {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) ItemType returned,
                         @Advice.This Material material) {
            if (material.isLegacy()) {
                NamespacedKey key = Bukkit.getUnsafe().fromLegacy(new org.bukkit.material.MaterialData(material), true).getKey();
                returned = Registry.ITEM.get(key);
            } else {
                returned = Registry.ITEM.get(material.getKey());
            }
        }
    }

    @SuppressWarnings({"removal", "deprecation"})
    public static class asBlockType {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) BlockType returned,
                         @Advice.This Material material) {
            if (material.isLegacy()) {
                NamespacedKey key = Bukkit.getUnsafe().fromLegacy(new org.bukkit.material.MaterialData(material), true).getKey();
                returned = Registry.BLOCK.get(key);
            } else {
                returned = Registry.BLOCK.get(material.getKey());
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class getDefaultData {
        @Advice.OnMethodExit
        static <T> void exit(@Advice.Return(readOnly = false) T returned,
                             @Advice.This Material material,
                             @Advice.Argument(0) io.papermc.paper.datacomponent.DataComponentType.Valued<T> type) {
            Preconditions.checkArgument(material.asItemType() != null);
            returned = material.asItemType().getDefaultData(type);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class hasDefaultData {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This Material material,
                         @Advice.Argument(0) DataComponentType type) {
            Preconditions.checkArgument(material.asItemType() != null);
            returned = material.asItemType().hasDefaultData(type);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class getDefaultDataTypes {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) Set<DataComponentType> returned,
                         @Advice.This Material material) {
            Preconditions.checkArgument(material.asItemType() != null);
            returned = material.asItemType().getDefaultDataTypes();
        }
    }

    public static class getMaxStackSize {
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) int returned,
                         @Advice.FieldValue(value = "maxStack", readOnly = false) int maxStack,
                         @Advice.This Material material) {
            if (maxStack == -1) { // Not calculated yet
                ItemType itemType = material.asItemType();
                returned = maxStack = itemType != null ? itemType.getMaxStackSize() : 64;
            }
            returned = maxStack;
        }
    }


}
