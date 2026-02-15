package dev.misieur.justamaterial;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Materials {

    private static final LinkedList<MaterialInfo> MATERIALS = new LinkedList<>();
    @SuppressWarnings({"FieldCanBeLocal", "unused"}) // Used in Material through reflection
    private static Map<String, Triple<Integer, String, NamespacedKey>> DATA;
    private static final Gson GSON = new Gson();

    public static void loadVanillaMaterials() {
        String fileName = "vanilla_materials/" + Version.get() + ".json";
        try (InputStream inputStream = Materials.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException(fileName + " not found in classpath");
            }
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            MaterialInfo[] materials = GSON.fromJson(reader, MaterialInfo[].class);
            MATERIALS.addAll(Arrays.asList(materials));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addMaterials(MaterialInfo @NotNull ... materials) {
        MATERIALS.addAll(Arrays.asList(materials));
    }

    @Contract(pure = true)
    public static List<MaterialInfo> getMaterials() {
        return MATERIALS;
    }

    public static void build() {
        DATA = MATERIALS.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MaterialInfo::name,
                        info -> Triple.of(info.id(), info.data(), info.customKey())
                ));
    }

}
