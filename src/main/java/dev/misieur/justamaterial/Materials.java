package dev.misieur.justamaterial;

import com.google.gson.Gson;
import io.papermc.paper.ServerBuildInfo;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Materials {

    private static final LinkedList<MaterialInfo> MATERIALS = new LinkedList<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Used in Material through reflection
    private static final Map<String, Triple<Integer, String, NamespacedKey>> DATA = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static void loadVanillaMaterials() {
        String fileName = switch (ServerBuildInfo.buildInfo().minecraftVersionId()) {
            case "1.21.11" -> "vanilla_materials/1.21.11.json";
            default ->
                    throw new IllegalStateException("Version " + ServerBuildInfo.buildInfo().minecraftVersionId() + " is not supported");
        };
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

    public static List<MaterialInfo> getMaterials() {
        return MATERIALS;
    }

    public static void build() {
        DATA.clear();
        for (MaterialInfo info : MATERIALS) {
            DATA.put(info.name(), Triple.of(info.id(), info.data(), info.customKey()));
        }
    }

}
