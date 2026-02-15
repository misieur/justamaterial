package dev.misieur.justamaterial;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.meta.When;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class MaterialAdvices {

    public static class constructor {
        @SuppressWarnings({"removal", "unchecked", "UnusedAssignment", "ParameterCanBeLocal"})
        @Advice.OnMethodExit
        static void exit(@Advice.This Material material,
                         @Advice.FieldValue(value = "id", readOnly = false) int id,
                         @Advice.FieldValue(value = "ctor", readOnly = false) @Nonnull(when = When.NEVER) Constructor<? extends org.bukkit.material.MaterialData> ctor,
                         @Advice.FieldValue(value = "maxStack", readOnly = false) int maxStack,
                         @Advice.FieldValue(value = "data", readOnly = false) @Nonnull(when = When.NEVER) Class<?> data,
                         @Advice.FieldValue(value = "legacy", readOnly = false) boolean legacy,
                         @Advice.FieldValue(value = "key", readOnly = false) @Nonnull(when = When.NEVER) NamespacedKey key,
                         @Advice.FieldValue("LEGACY_PREFIX") @NotNull String LEGACY_PREFIX,
                         @Advice.FieldValue(value = "DATA_CACHE", readOnly = false) @Nonnull(when = When.MAYBE) Map<String, Triple<Integer, String, NamespacedKey>> DATA_CACHE) {
            if (DATA_CACHE == null) {
                try {
                    Field dataField = Class.forName("dev.misieur.justamaterial.Materials", true, GeneratedForBukkit.CLASSLOADER).getDeclaredField("DATA");
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
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        public static void exit(@Advice.FieldValue(value = "BY_NAME", readOnly = false) @NotNull Map<String, Material> BY_NAME) {
            Map<String, Material> byName = HashMap.newHashMap(Material.values().length);
            for (Material material : Material.values()) {
                byName.put(material.name(), material);
            }
            BY_NAME = Map.copyOf(byName);
        }
    }

    public static class createBlockData {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) @NotNull BlockData returned,
                                @Advice.This @NotNull Material material) {
            returned = Bukkit.createBlockData(material);
        }
    }

    public static class createBlockData_consumer {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) @NotNull BlockData returned,
                                @Advice.This @NotNull Material material,
                                @Advice.Argument(0) @NotNull Consumer<? super BlockData> consumer) {
            returned = Bukkit.createBlockData(material, consumer);
        }
    }

    public static class createBlockData_data {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) @NotNull BlockData returned,
                                @Advice.This @NotNull Material material,
                                @Advice.Argument(0) @NotNull String data) {
            returned = Bukkit.createBlockData(material, data);
        }
    }

    @SuppressWarnings("deprecation")
    public static class getMaterial {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) @Nullable Material returned,
                                @Advice.FieldValue(value = "BY_NAME") @NotNull Map<String, Material> BY_NAME,
                                @Advice.FieldValue(value = "LEGACY_PREFIX") @NotNull String LEGACY_PREFIX,
                                @Advice.Argument(value = 0, readOnly = false) @NotNull String name,
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
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) Material returned,
                                @Advice.Argument(0) @Nonnull(when = When.MAYBE) String name,
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
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) org.bukkit.material.MaterialData returned,
                         @Advice.This @NotNull Material material,
                         @Advice.Argument(0) byte raw,
                         @Advice.FieldValue("ctor") @NotNull Constructor<? extends org.bukkit.material.MaterialData> ctor) {
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
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            BlockType blockType = material.asBlockType();
            if (blockType != null) {
                returned = blockType.hasCollision();
                return;
            }
            throw new IllegalArgumentException(material + " isn't a block type");
        }
    }

    public static class getId {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Contract(pure = true)
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) int returned,
                         @Advice.FieldValue("legacy") boolean legacy,
                         @Advice.FieldValue("id") int id) {
            Preconditions.checkArgument(legacy, "Cannot get ID of Modern Material");
            returned = id;
        }
    }

    public static class getKey {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Contract(pure = true)
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) NamespacedKey returned,
                         @Advice.FieldValue("legacy") boolean legacy,
                         @Advice.FieldValue("key") NamespacedKey key) {
            Preconditions.checkArgument(!legacy, "Cannot get key of Legacy Material");
            returned = key;
        }
    }

    public static class getDefaultAttributeModifiers {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) Multimap<Attribute, AttributeModifier> returned,
                         @Advice.This @NotNull Material material,
                         @Advice.Argument(0) @Nullable EquipmentSlot slot) {
            ItemType type = material.asItemType();
            Preconditions.checkArgument(type != null, "The Material is not an item!");
            returned = slot != null ? type.getDefaultAttributeModifiers(slot) : type.getDefaultAttributeModifiers();
        }
    }

    @SuppressWarnings("removal")
    public static class getItemRarity {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) io.papermc.paper.inventory.ItemRarity returned,
                         @Advice.This @NotNull Material material) {
            returned = new org.bukkit.inventory.ItemStack(material).getRarity();
        }
    }

    public static class getTranslationKey {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) String returned,
                         @Advice.This @NotNull Material material) {
            if (material.isItem()) {
                returned = java.util.Objects.requireNonNull(material.asItemType()).translationKey();
            } else {
                returned = java.util.Objects.requireNonNull(material.asBlockType()).translationKey();
            }
        }
    }

    public static class getMaxDurability {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) short returned,
                         @Advice.This @NotNull Material material) {
            ItemType type = material.asItemType();
            returned = type == null ? 0 : type.getMaxDurability();
        }
    }

    public static class isBlock {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            returned = material.asBlockType() != null;
        }
    }

    public static class isEdible {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            ItemType type = material.asItemType();
            returned = type != null && type.isEdible();
        }
    }

    public static class isRecord {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            ItemType type = material.asItemType();
            returned = type != null && type.isRecord();
        }
    }

    public static class isSolid {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isSolid();
        }
    }

    public static class isAir {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isAir();
        }
    }

    @SuppressWarnings({"removal"})
    public static class isTransparent {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            // This is unreadable I agree but by doing this I can avoid creating a MaterialAdvices$1 class that wouldn't be found on the bukkit classloader
            returned = material.isBlock() && (material == Material.ACACIA_BUTTON || material == Material.ACACIA_SAPLING || material == Material.ACTIVATOR_RAIL || material == Material.AIR || material == Material.ALLIUM || material == Material.ATTACHED_MELON_STEM || material == Material.ATTACHED_PUMPKIN_STEM || material == Material.AZURE_BLUET || material == Material.BARRIER || material == Material.BEETROOTS || material == Material.BIRCH_BUTTON || material == Material.BIRCH_SAPLING || material == Material.BLACK_CARPET || material == Material.BLUE_CARPET || material == Material.BLUE_ORCHID || material == Material.BROWN_CARPET || material == Material.BROWN_MUSHROOM || material == Material.CARROTS || material == Material.CAVE_AIR || material == Material.CHORUS_FLOWER || material == Material.CHORUS_PLANT || material == Material.COCOA || material == Material.COMPARATOR || material == Material.CREEPER_HEAD || material == Material.CREEPER_WALL_HEAD || material == Material.CYAN_CARPET || material == Material.DANDELION || material == Material.DARK_OAK_BUTTON || material == Material.DARK_OAK_SAPLING || material == Material.DEAD_BUSH || material == Material.DETECTOR_RAIL || material == Material.DRAGON_HEAD || material == Material.DRAGON_WALL_HEAD || material == Material.END_GATEWAY || material == Material.END_PORTAL || material == Material.END_ROD || material == Material.FERN || material == Material.FIRE || material == Material.FLOWER_POT || material == Material.GRAY_CARPET || material == Material.GREEN_CARPET || material == Material.JUNGLE_BUTTON || material == Material.JUNGLE_SAPLING || material == Material.LADDER || material == Material.LARGE_FERN || material == Material.LEVER || material == Material.LIGHT_BLUE_CARPET || material == Material.LIGHT_GRAY_CARPET || material == Material.LILAC || material == Material.LILY_PAD || material == Material.LIME_CARPET || material == Material.MAGENTA_CARPET || material == Material.MELON_STEM || material == Material.NETHER_PORTAL || material == Material.NETHER_WART || material == Material.OAK_BUTTON || material == Material.OAK_SAPLING || material == Material.ORANGE_CARPET || material == Material.ORANGE_TULIP || material == Material.OXEYE_DAISY || material == Material.PEONY || material == Material.PINK_CARPET || material == Material.PINK_TULIP || material == Material.PLAYER_HEAD || material == Material.PLAYER_WALL_HEAD || material == Material.POPPY || material == Material.POTATOES || material == Material.POTTED_ACACIA_SAPLING || material == Material.POTTED_ALLIUM || material == Material.POTTED_AZALEA_BUSH || material == Material.POTTED_AZURE_BLUET || material == Material.POTTED_BIRCH_SAPLING || material == Material.POTTED_BLUE_ORCHID || material == Material.POTTED_BROWN_MUSHROOM || material == Material.POTTED_CACTUS || material == Material.POTTED_DANDELION || material == Material.POTTED_DARK_OAK_SAPLING || material == Material.POTTED_DEAD_BUSH || material == Material.POTTED_FERN || material == Material.POTTED_FLOWERING_AZALEA_BUSH || material == Material.POTTED_JUNGLE_SAPLING || material == Material.POTTED_OAK_SAPLING || material == Material.POTTED_ORANGE_TULIP || material == Material.POTTED_OXEYE_DAISY || material == Material.POTTED_PINK_TULIP || material == Material.POTTED_POPPY || material == Material.POTTED_RED_MUSHROOM || material == Material.POTTED_RED_TULIP || material == Material.POTTED_SPRUCE_SAPLING || material == Material.POTTED_WHITE_TULIP || material == Material.POWERED_RAIL || material == Material.PUMPKIN_STEM || material == Material.PURPLE_CARPET || material == Material.RAIL || material == Material.REDSTONE_TORCH || material == Material.REDSTONE_WALL_TORCH || material == Material.REDSTONE_WIRE || material == Material.RED_CARPET || material == Material.RED_MUSHROOM || material == Material.RED_TULIP || material == Material.REPEATER || material == Material.ROSE_BUSH || material == Material.SHORT_GRASS || material == Material.SKELETON_SKULL || material == Material.SKELETON_WALL_SKULL || material == Material.SNOW || material == Material.SPRUCE_BUTTON || material == Material.SPRUCE_SAPLING || material == Material.STONE_BUTTON || material == Material.STRUCTURE_VOID || material == Material.SUGAR_CANE || material == Material.SUNFLOWER || material == Material.TALL_GRASS || material == Material.TORCH || material == Material.TRIPWIRE || material == Material.TRIPWIRE_HOOK || material == Material.VINE || material == Material.VOID_AIR || material == Material.WALL_TORCH || material == Material.WHEAT || material == Material.WHITE_CARPET || material == Material.WHITE_TULIP || material == Material.WITHER_SKELETON_SKULL || material == Material.WITHER_SKELETON_WALL_SKULL || material == Material.YELLOW_CARPET || material == Material.ZOMBIE_HEAD || material == Material.ZOMBIE_WALL_HEAD || material == Material.LEGACY_AIR || material == Material.LEGACY_SAPLING || material == Material.LEGACY_POWERED_RAIL || material == Material.LEGACY_DETECTOR_RAIL || material == Material.LEGACY_LONG_GRASS || material == Material.LEGACY_DEAD_BUSH || material == Material.LEGACY_YELLOW_FLOWER || material == Material.LEGACY_RED_ROSE || material == Material.LEGACY_BROWN_MUSHROOM || material == Material.LEGACY_RED_MUSHROOM || material == Material.LEGACY_TORCH || material == Material.LEGACY_FIRE || material == Material.LEGACY_REDSTONE_WIRE || material == Material.LEGACY_CROPS || material == Material.LEGACY_LADDER || material == Material.LEGACY_RAILS || material == Material.LEGACY_LEVER || material == Material.LEGACY_REDSTONE_TORCH_OFF || material == Material.LEGACY_REDSTONE_TORCH_ON || material == Material.LEGACY_STONE_BUTTON || material == Material.LEGACY_SNOW || material == Material.LEGACY_SUGAR_CANE_BLOCK || material == Material.LEGACY_PORTAL || material == Material.LEGACY_DIODE_BLOCK_OFF || material == Material.LEGACY_DIODE_BLOCK_ON || material == Material.LEGACY_PUMPKIN_STEM || material == Material.LEGACY_MELON_STEM || material == Material.LEGACY_VINE || material == Material.LEGACY_WATER_LILY || material == Material.LEGACY_NETHER_WARTS || material == Material.LEGACY_ENDER_PORTAL || material == Material.LEGACY_COCOA || material == Material.LEGACY_TRIPWIRE_HOOK || material == Material.LEGACY_TRIPWIRE || material == Material.LEGACY_FLOWER_POT || material == Material.LEGACY_CARROT || material == Material.LEGACY_POTATO || material == Material.LEGACY_WOOD_BUTTON || material == Material.LEGACY_SKULL || material == Material.LEGACY_REDSTONE_COMPARATOR_OFF || material == Material.LEGACY_REDSTONE_COMPARATOR_ON || material == Material.LEGACY_ACTIVATOR_RAIL || material == Material.LEGACY_CARPET || material == Material.LEGACY_DOUBLE_PLANT || material == Material.LEGACY_END_ROD || material == Material.LEGACY_CHORUS_PLANT || material == Material.LEGACY_CHORUS_FLOWER || material == Material.LEGACY_BEETROOT_BLOCK || material == Material.LEGACY_END_GATEWAY || material == Material.LEGACY_STRUCTURE_VOID);
        }
    }

    public static class isFlammable {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isFlammable();
        }
    }

    public static class isBurnable {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isBurnable();
        }
    }

    public static class isFuel {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            ItemType type = material.asItemType();
            returned = type != null && type.isFuel();
        }
    }

    public static class isOccluding {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isOccluding();
        }
    }

    public static class hasGravity {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.hasGravity();
        }
    }

    public static class isItem {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            returned = material.asItemType() != null;
        }
    }

    @SuppressWarnings("deprecation")
    public static class isInteractable {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            returned = type != null && type.isInteractable();
        }
    }

    public static class getHardness {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) float returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            Preconditions.checkArgument(type != null, "The Material is not a block!");
            returned = type.getHardness();
        }
    }

    public static class getBlastResistance {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) float returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            Preconditions.checkArgument(type != null, "The Material is not a block!");
            returned = type.getBlastResistance();
        }
    }

    public static class getSlipperiness {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) float returned,
                         @Advice.This @NotNull Material material) {
            BlockType type = material.asBlockType();
            Preconditions.checkArgument(type != null, "The Material is not a block!");
            returned = type.getSlipperiness();
        }
    }

    @SuppressWarnings("deprecation")
    public static class getCraftingRemainingItem {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) Material returned,
                         @Advice.This @NotNull Material material) {
            ItemType type = material.asItemType();
            Preconditions.checkArgument(type != null, "The Material is not an item!");
            returned = type.getCraftingRemainingItem() == null ? null : type.getCraftingRemainingItem().asMaterial();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class getEquipmentSlot {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) EquipmentSlot returned,
                         @Advice.This @NotNull Material material) {
            ItemType type = material.asItemType();
            Preconditions.checkArgument(type != null, "The Material is not an item!");
            Equippable equippable = type.getDefaultData(DataComponentTypes.EQUIPPABLE);
            returned = equippable == null ? EquipmentSlot.HAND : equippable.slot();
        }
    }

    @SuppressWarnings("removal")
    public static class getCreativeCategory {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) CreativeCategory returned,
                         @Advice.This @NotNull Material material) {
            ItemType type = material.asItemType();
            returned = type == null ? null : type.getCreativeCategory();
        }
    }

    public static class getBlockTranslationKey {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) String returned,
                         @Advice.This @NotNull Material material) {
            returned = material.isBlock() ? material.translationKey() : null;
        }
    }

    public static class getItemTranslationKey {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) String returned,
                         @Advice.This @NotNull Material material) {
            returned = material.isItem() ? material.translationKey() : null;
        }
    }

    public static class isCompostable {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            ItemType itemType = material.asItemType();
            returned = itemType != null && itemType.isCompostable();
        }
    }

    public static class getCompostChance {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) float returned,
                         @Advice.This @NotNull Material material) {
            ItemType type = material.asItemType();
            Preconditions.checkArgument(type != null, "The Material is not an item!");
            returned = type.getCompostChance();
        }
    }

    @SuppressWarnings({"removal", "deprecation"})
    public static class asItemType {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) ItemType returned,
                         @Advice.This @NotNull Material material) {
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
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) BlockType returned,
                         @Advice.This @NotNull Material material) {
            if (material.isLegacy()) {
                NamespacedKey key = Bukkit.getUnsafe().fromLegacy(new org.bukkit.material.MaterialData(material), true).getKey();
                returned = Registry.BLOCK.get(key);
            } else {
                returned = Registry.BLOCK.get(material.getKey());
            }
        }
    }

    public static class getData {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment", "removal"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) Class<? extends org.bukkit.material.MaterialData> returned,
                         @Advice.FieldValue("ctor") @NotNull Constructor<? extends org.bukkit.material.MaterialData> ctor,
                         @Advice.This @NotNull Material material) {
            Preconditions.checkArgument(material.isLegacy(), "Cannot get data class of Modern Material");
            returned = ctor.getDeclaringClass();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class getDefaultData {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static <T> void exit(@Advice.Return(readOnly = false) T returned,
                             @Advice.This @NotNull Material material,
                             @Advice.Argument(0) io.papermc.paper.datacomponent.DataComponentType.Valued<T> type) {
            Preconditions.checkArgument(material.asItemType() != null);
            returned = material.asItemType().getDefaultData(type);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class hasDefaultData {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material,
                         @Advice.Argument(0) DataComponentType type) {
            Preconditions.checkArgument(material.asItemType() != null);
            returned = material.asItemType().hasDefaultData(type);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class getDefaultDataTypes {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) Set<DataComponentType> returned,
                         @Advice.This @NotNull Material material) {
            Preconditions.checkArgument(material.asItemType() != null);
            returned = material.asItemType().getDefaultDataTypes();
        }
    }

    @SuppressWarnings("removal")
    public static class getMaxStackSize {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) int returned,
                         @Advice.FieldValue(value = "maxStack", readOnly = false) int maxStack,
                         @Advice.This Material material) {
            if (material == Material.LEGACY_AIR) {
                returned = 0;
                return;
            }
            if (maxStack == -1) { // Not calculated yet
                ItemType itemType = material.asItemType();
                returned = maxStack = itemType != null ? itemType.getMaxStackSize() : 64;
                return;
            }
            returned = maxStack;
        }
    }

    /**
     * Removed in 1.21.9
     */
    public static class isEnabledByFeature {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment", "removal", "UnstableApiUsage"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material,
                         @Advice.Argument(0) @NotNull World world) {
            ItemType itemType = material.asItemType();
            if (itemType != null)
                returned = Bukkit.getDataPackManager().isEnabledByFeature(itemType, world);
            else if (material.asBlockType() != null)
                returned = Bukkit.getDataPackManager().isEnabledByFeature(material.asBlockType(), world);
        }
    }

    /**
     * Becomes {@link Material#isAir()} in 1.21.5
     */
    public static class isEmpty {
        @Contract(pure = true)
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) boolean returned,
                         @Advice.This @NotNull Material material) {
            returned = (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR);
        }
    }

    /**
     * {@link getEquipmentSlot} in 1.21.5
     */
    public static class getEquipmentSlot_old {
        @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment"})
        @Advice.OnMethodExit
        static void exit(@Advice.Return(readOnly = false) EquipmentSlot returned,
                         @Advice.This @NotNull Material material) {
            Preconditions.checkArgument(material.isItem(), "The Material is not an item!");
            if (material == Material.CARVED_PUMPKIN || material == Material.CHAINMAIL_HELMET || material == Material.CREEPER_HEAD || material == Material.DIAMOND_HELMET || material == Material.DRAGON_HEAD || material == Material.GOLDEN_HELMET || material == Material.IRON_HELMET || material == Material.LEATHER_HELMET || material == Material.NETHERITE_HELMET || material == Material.PLAYER_HEAD || material == Material.PIGLIN_HEAD || material == Material.SKELETON_SKULL || material == Material.TURTLE_HELMET || material == Material.WITHER_SKELETON_SKULL || material == Material.ZOMBIE_HEAD)
                returned = EquipmentSlot.HEAD;
            else if (material == Material.CHAINMAIL_CHESTPLATE || material == Material.DIAMOND_CHESTPLATE || material == Material.ELYTRA || material == Material.GOLDEN_CHESTPLATE || material == Material.IRON_CHESTPLATE || material == Material.LEATHER_CHESTPLATE || material == Material.NETHERITE_CHESTPLATE)
                returned = EquipmentSlot.CHEST;
            else if (material == Material.CHAINMAIL_LEGGINGS || material == Material.DIAMOND_LEGGINGS || material == Material.GOLDEN_LEGGINGS || material == Material.IRON_LEGGINGS || material == Material.LEATHER_LEGGINGS || material == Material.NETHERITE_LEGGINGS)
                returned = EquipmentSlot.LEGS;
            else if (material == Material.CHAINMAIL_BOOTS || material == Material.DIAMOND_BOOTS || material == Material.GOLDEN_BOOTS || material == Material.IRON_BOOTS || material == Material.LEATHER_BOOTS || material == Material.NETHERITE_BOOTS)
                returned = EquipmentSlot.FEET;
            else if (material == Material.SHIELD)
                returned = EquipmentSlot.OFF_HAND;
            else if (material == Material.BLACK_CARPET || material == Material.BLUE_CARPET || material == Material.BROWN_CARPET || material == Material.CYAN_CARPET || material == Material.DIAMOND_HORSE_ARMOR || material == Material.GOLDEN_HORSE_ARMOR || material == Material.GRAY_CARPET || material == Material.GREEN_CARPET || material == Material.IRON_HORSE_ARMOR || material == Material.LEATHER_HORSE_ARMOR || material == Material.LIGHT_BLUE_CARPET || material == Material.LIGHT_GRAY_CARPET || material == Material.LIME_CARPET || material == Material.MAGENTA_CARPET || material == Material.ORANGE_CARPET || material == Material.PINK_CARPET || material == Material.PURPLE_CARPET || material == Material.RED_CARPET || material == Material.WHITE_CARPET || material == Material.WOLF_ARMOR || material == Material.YELLOW_CARPET)
                returned = EquipmentSlot.BODY;
            else
                returned = EquipmentSlot.HAND;
        }
    }


}
