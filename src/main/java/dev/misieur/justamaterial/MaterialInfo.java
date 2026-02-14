package dev.misieur.justamaterial;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A record class that represents a value from the {@link org.bukkit.Material} class
 *
 * @param name The name of the material (e.g. PAPER)
 * @param id The id of the material (-1 for non legacy materials)
 * @param data The reference of the data class ({@link org.bukkit.material.MaterialData} if null)
 * @param customKey The key of the material item/block (only for non 'minecraft' namespace keys)
 */
public record MaterialInfo(@NotNull String name, @Nullable Integer id, @Nullable String data, @Nullable NamespacedKey customKey) {
}
