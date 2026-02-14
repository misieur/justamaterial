package dev.misieur.justamaterial;

public class MaterialInjector {

    public static String jarFilePath;

    public static void injectMaterials(String jarFilePath, MaterialInfo... materials) {
        MaterialInjector.jarFilePath = jarFilePath;
        Materials.loadVanillaMaterials();
        Materials.addMaterials(materials);
        Materials.build();
        Agent.install();
    }

}
