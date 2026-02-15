package dev.misieur.justamaterial;

import com.google.common.collect.Multimap;
import io.papermc.paper.datacomponent.DataComponentType;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarFile;

@SuppressWarnings("UnstableApiUsage")
public final class MaterialTransformer implements ClassFileTransformer {

    private static final String TARGET = "org/bukkit/Material";

    @Override
    public byte[] transform(Module module, ClassLoader loader, String name,
                            Class<?> cls, ProtectionDomain pd, byte[] bytes) {

        if (!TARGET.equals(name)) return null;
        TypePool typePool = TypePool.Default.of(loader);
        TypeDescription typeDescription = typePool.describe(name.replace('/', '.')).resolve();

        try {
            bytes = getMaterialClassBytes(typeDescription);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return bytes;
    }

    @SuppressWarnings("removal")
    private static byte @NotNull [] getMaterialClassBytes(TypeDescription typeDescription) throws IOException {
        ClassFileLocator locator = new ClassFileLocator.ForJarFile(new JarFile(MaterialInjector.jarFilePath));
        TypePool typePool = TypePool.Default.of(MaterialTransformer.class.getClassLoader());
        Function<String, TypeDescription> resolver = (name) -> typePool.describe("dev.misieur.justamaterial.MaterialAdvices$" + name).resolve();
        DynamicType.Builder<? extends Enum<?>> builder;
        try {
            builder = new ByteBuddy()
                    .makeEnumeration(Materials.getMaterials().stream().map(MaterialInfo::name).toArray(String[]::new))
                    .name("org.bukkit.Material")
                    .implement(Keyed.class, org.bukkit.Translatable.class, net.kyori.adventure.translation.Translatable.class)
                    // public static final String LEGACY_PREFIX = "LEGACY_";
                    .defineField("LEGACY_PREFIX", String.class,
                            Visibility.PUBLIC, Ownership.STATIC, FieldManifestation.FINAL)
                    .value("LEGACY_")
                    // private static final Map<String, Material> BY_NAME;
                    .defineField("BY_NAME", new TypeReference<Map<String, ?>>() {
                            }.getType(),
                            Visibility.PRIVATE, Ownership.STATIC, FieldManifestation.FINAL)
                    // private static Map<String, Triple<Integer, String, NamespacedKey>> DATA_CACHE;
                    .defineField("DATA_CACHE", new TypeReference<Map<String, Triple<Integer, String, NamespacedKey>>>() {
                            }.getType(),
                            Visibility.PRIVATE, Ownership.STATIC)
                    // static {}
                    .invokable(ElementMatchers.isTypeInitializer())
                    .intercept(Advice.to(resolver.apply("staticInitializer"), locator).wrap(StubMethod.INSTANCE))

                    // public static Material getMaterial(@NotNull final String name)
                    .defineMethod("getMaterial", typeDescription, Visibility.PUBLIC, Ownership.STATIC)
                    .withParameter(String.class)
                    .intercept(
                            MethodCall.invoke(ElementMatchers.named("getMaterial").and(ElementMatchers.takesArguments(String.class, boolean.class)))
                                    .withArgument(0)
                                    .with(false)
                    )
                    // public static Material getMaterial(@NotNull String name, boolean legacyName)
                    .defineMethod("getMaterial", typeDescription, Visibility.PUBLIC, Ownership.STATIC)
                    .withParameter(String.class)
                    .withParameter(boolean.class)
                    .intercept(Advice.to(resolver.apply("getMaterial"), locator).wrap(StubMethod.INSTANCE))
                    // public static Material matchMaterial(@NotNull final String name)
                    .defineMethod("matchMaterial", typeDescription, Visibility.PUBLIC, Ownership.STATIC)
                    .withParameter(String.class)
                    .intercept(MethodCall.invoke(ElementMatchers.named("matchMaterial").and(ElementMatchers.takesArguments(String.class, boolean.class))).withArgument(0).with(false))
                    // public static Material matchMaterial(@NotNull final String name, boolean legacyName)
                    .defineMethod("matchMaterial", typeDescription, Visibility.PUBLIC, Ownership.STATIC)
                    .withParameter(String.class)
                    .withParameter(boolean.class)
                    .intercept(Advice.to(resolver.apply("getMaterial"), locator).wrap(StubMethod.INSTANCE))

                    // private final int id;
                    .defineField("id", int.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                    // public final Class<?> data;
                    .defineField("data", new TypeReference<Class<?>>() {
                    }.getType(), Visibility.PUBLIC, FieldManifestation.FINAL)
                    // private final boolean legacy;
                    .defineField("legacy", boolean.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                    // private final Constructor<? extends MaterialData> ctor;
                    .defineField("ctor", new TypeReference<Constructor<? extends org.bukkit.material.MaterialData>>() {
                    }.getType())
                    // private int maxStack;
                    .defineField("maxStack", int.class, Visibility.PRIVATE)
                    // private final NamespacedKey key;
                    .defineField("key", NamespacedKey.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                    .constructor(ElementMatchers.any())
                    .intercept(
                            Advice.to(resolver.apply("constructor"), locator)
                                    .wrap(
                                            MethodCall.invoke(Enum.class.getDeclaredConstructor(String.class, int.class))
                                                    .onSuper()
                                                    .withArgument(0, 1)
                                    )
                    )
                    // @Override annotation is not needed
                    // public @NotNull String translationKey()
                    .defineMethod("translationKey", String.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getTranslationKey"), locator).wrap(StubMethod.INSTANCE))
                    // public io.papermc.paper.inventory.ItemRarity getItemRarity()
                    .defineMethod("getItemRarity", io.papermc.paper.inventory.ItemRarity.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getItemRarity"), locator).wrap(StubMethod.INSTANCE))
                    // public Multimap<Attribute, AttributeModifier> getItemAttributes(@NotNull EquipmentSlot equipmentSlot)
                    .defineMethod("getItemAttributes", new TypeReference<Multimap<Attribute, AttributeModifier>>() {
                    }.getType(), Visibility.PUBLIC)
                    .withParameter(EquipmentSlot.class)
                    .intercept(Advice.to(resolver.apply("getDefaultAttributeModifiers"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isCollidable()
                    .defineMethod("isCollidable", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isCollidable"), locator).wrap(StubMethod.INSTANCE))
                    // public int getId()
                    .defineMethod("getId", int.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getId"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isLegacy()
                    .defineMethod("isLegacy", boolean.class, Visibility.PUBLIC)
                    .intercept(FieldAccessor.ofField("legacy"))
                    // public NamespacedKey getKey()
                    .defineMethod("getKey", NamespacedKey.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getKey"), locator).wrap(StubMethod.INSTANCE))
                    // public int getMaxStackSize()
                    .defineMethod("getMaxStackSize", int.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getMaxStackSize"), locator).wrap(StubMethod.INSTANCE))
                    // public short getMaxDurability()
                    .defineMethod("getMaxDurability", short.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getMaxDurability"), locator).wrap(StubMethod.INSTANCE))
                    // public BlockData createBlockData()
                    .defineMethod("createBlockData", BlockData.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("createBlockData"), locator).wrap(StubMethod.INSTANCE))
                    // public BlockData createBlockData(@Nullable Consumer<? super BlockData> consumer)
                    .defineMethod("createBlockData", BlockData.class, Visibility.PUBLIC)
                    .withParameter(new TypeReference<Consumer<? super BlockData>>() {
                    }.getType())
                    .intercept(Advice.to(resolver.apply("createBlockData_consumer"), locator).wrap(StubMethod.INSTANCE))
                    // public BlockData createBlockData(@Nullable String data) throws IllegalArgumentException
                    .defineMethod("createBlockData", BlockData.class, Visibility.PUBLIC)
                    .withParameter(String.class)
                    .intercept(Advice.to(resolver.apply("createBlockData_data"), locator).wrap(StubMethod.INSTANCE))
                    // public Class<? extends MaterialData> getData()
                    .defineMethod("getData", new TypeReference<Class<? extends org.bukkit.material.MaterialData>>() {
                    }.getType(), Visibility.PUBLIC)
                    .intercept(
                            Advice.to(resolver.apply("getData"), locator).wrap(StubMethod.INSTANCE)
                    )
                    // public MaterialData getNewData(final byte raw)
                    .defineMethod("getNewData", org.bukkit.material.MaterialData.class, Visibility.PUBLIC)
                    .withParameter(byte.class)
                    .intercept(Advice.to(resolver.apply("getNewData"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isBlock()
                    .defineMethod("isBlock", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isBlock"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isEdible()
                    .defineMethod("isEdible", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isEdible"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isRecord()
                    .defineMethod("isRecord", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isRecord"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isSolid()
                    .defineMethod("isSolid", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isSolid"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isAir()
                    .defineMethod("isAir", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isAir"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isTransparent()
                    .defineMethod("isTransparent", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isTransparent"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isFlammable()
                    .defineMethod("isFlammable", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isFlammable"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isBurnable()
                    .defineMethod("isBurnable", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isBurnable"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isFuel()
                    .defineMethod("isFuel", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isFuel"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isOccluding()
                    .defineMethod("isOccluding", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isOccluding"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean hasGravity()
                    .defineMethod("hasGravity", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("hasGravity"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isItem()
                    .defineMethod("isItem", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isItem"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isInteractable()
                    .defineMethod("isInteractable", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isInteractable"), locator).wrap(StubMethod.INSTANCE))
                    // public float getHardness()
                    .defineMethod("getHardness", float.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getHardness"), locator).wrap(StubMethod.INSTANCE))
                    // public float getBlastResistance()
                    .defineMethod("getBlastResistance", float.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getBlastResistance"), locator).wrap(StubMethod.INSTANCE))
                    // public float getSlipperiness()
                    .defineMethod("getSlipperiness", float.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getSlipperiness"), locator).wrap(StubMethod.INSTANCE))
                    // public Material getCraftingRemainingItem()
                    .defineMethod("getCraftingRemainingItem", typeDescription, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getCraftingRemainingItem"), locator).wrap(StubMethod.INSTANCE))
                    // public @NotNull @Unmodifiable Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers()
                    .defineMethod("getDefaultAttributeModifiers", new TypeReference<Multimap<Attribute, AttributeModifier>>() {
                    }.getType(), Visibility.PUBLIC)
                    .intercept(MethodCall.invoke(ElementMatchers.named("getDefaultAttributeModifiers").and(ElementMatchers.takesArguments(EquipmentSlot.class))).with((EquipmentSlot) null))
                    // public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot)
                    .defineMethod("getDefaultAttributeModifiers", new TypeReference<Multimap<Attribute, AttributeModifier>>() {
                    }.getType(), Visibility.PUBLIC)
                    .withParameter(EquipmentSlot.class)
                    .intercept(Advice.to(resolver.apply("getDefaultAttributeModifiers"), locator).wrap(StubMethod.INSTANCE))
                    // public CreativeCategory getCreativeCategory()
                    .defineMethod("getCreativeCategory", CreativeCategory.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getCreativeCategory"), locator).wrap(StubMethod.INSTANCE))
                    // public String getTranslationKey()
                    .defineMethod("getTranslationKey", String.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getTranslationKey"), locator).wrap(StubMethod.INSTANCE))
                    // public String getBlockTranslationKey()
                    .defineMethod("getBlockTranslationKey", String.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getBlockTranslationKey"), locator).wrap(StubMethod.INSTANCE))
                    // public String getItemTranslationKey()
                    .defineMethod("getItemTranslationKey", String.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getItemTranslationKey"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean isCompostable()
                    .defineMethod("isCompostable", boolean.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("isCompostable"), locator).wrap(StubMethod.INSTANCE))
                    // public float getCompostChance()
                    .defineMethod("getCompostChance", float.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getCompostChance"), locator).wrap(StubMethod.INSTANCE))
                    // public ItemType asItemType()
                    .defineMethod("asItemType", ItemType.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("asItemType"), locator).wrap(StubMethod.INSTANCE))
                    // public BlockType asBlockType()
                    .defineMethod("asBlockType", BlockType.class, Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("asBlockType"), locator).wrap(StubMethod.INSTANCE))
                    // public @Nullable <T> T getDefaultData(final io.papermc.paper.datacomponent.DataComponentType.@NotNull Valued<T> type)
                    .defineMethod("getDefaultData", TypeDescription.Generic.Builder.typeVariable("T").build(), Visibility.PUBLIC)
                    .withParameter(TypeDescription.Generic.Builder
                            .parameterizedType(TypeDescription.ForLoadedType.of(DataComponentType.Valued.class), TypeDescription.Generic.Builder.typeVariable("T").build())
                            .build())
                    .typeVariable("T")
                    .intercept(Advice.to(resolver.apply("getDefaultData"), locator).wrap(StubMethod.INSTANCE))
                    // public boolean hasDefaultData(final io.papermc.paper.datacomponent.@NotNull DataComponentType type)
                    .defineMethod("hasDefaultData", boolean.class, Visibility.PUBLIC)
                    .withParameter(DataComponentType.class)
                    .intercept(Advice.to(resolver.apply("hasDefaultData"), locator).wrap(StubMethod.INSTANCE))
                    // public java.util.@Unmodifiable @NotNull Set<io.papermc.paper.datacomponent.DataComponentType> getDefaultDataTypes()
                    .defineMethod("getDefaultDataTypes", new TypeReference<Set<DataComponentType>>() {
                    }.getType(), Visibility.PUBLIC)
                    .intercept(Advice.to(resolver.apply("getDefaultDataTypes"), locator).wrap(StubMethod.INSTANCE));
            switch (Version.get()) {
                case V1_21, V1_21_4, V1_21_5, V1_21_6 -> builder = builder
                        .defineMethod("isEnabledByFeature", boolean.class, Visibility.PUBLIC)
                        .withParameter(org.bukkit.World.class)
                        .intercept(Advice.to(resolver.apply("isEnabledByFeature"), locator).wrap(StubMethod.INSTANCE));
            }
            switch (Version.get()) {
                case V1_21, V1_21_4 -> builder = builder
                        // public boolean isEmpty()
                        .defineMethod("isEmpty", boolean.class, Visibility.PUBLIC)
                        .intercept(Advice.to(resolver.apply("isEmpty"), locator).wrap(StubMethod.INSTANCE))
                        // public EquipmentSlot getEquipmentSlot()
                        .defineMethod("getEquipmentSlot", EquipmentSlot.class, Visibility.PUBLIC)
                        .intercept(Advice.to(resolver.apply("getEquipmentSlot_old"), locator).wrap(StubMethod.INSTANCE));
                default -> builder = builder
                        // public boolean isEmpty()
                        .defineMethod("isEmpty", boolean.class, Visibility.PUBLIC)
                        .intercept(MethodCall.invoke(ElementMatchers.named("isAir")))
                        // public EquipmentSlot getEquipmentSlot()
                        .defineMethod("getEquipmentSlot", EquipmentSlot.class, Visibility.PUBLIC)
                        .intercept(Advice.to(resolver.apply("getEquipmentSlot"), locator).wrap(StubMethod.INSTANCE));
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        try (DynamicType.Unloaded<?> unloaded = builder.make()) {
            return unloaded.getBytes();
        }
    }

    @SuppressWarnings("unused")
    abstract static class TypeReference<T> {
        public Type getType() {
            return ((ParameterizedType) getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
        }
    }


}
