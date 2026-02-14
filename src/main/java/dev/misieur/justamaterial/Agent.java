package dev.misieur.justamaterial;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

public final class Agent {

    public static Instrumentation inst;

    public static void install() {
        inst = ByteBuddyAgent.install();
        if (isClassLoaded("org.bukkit.Material", inst)) {
            throw new IllegalStateException("Material class is already loaded!");
        }
        inst.addTransformer(new MaterialTransformer(), true);
        // We create a new class that can be accessed by Material which contains our class loader, that will allow it to access our classes from the Material class
        try {
            DynamicType.Builder<Object> builder = new ByteBuddy()
                    .subclass(Object.class)
                    .name("dev.misieur.justamaterial.GeneratedForBukkit")
                    .defineField(
                            "CLASSLOADER",
                            ClassLoader.class,
                            Visibility.PUBLIC,
                            Ownership.STATIC
                    );
            try (DynamicType.Unloaded<Object> unloaded = builder.make()) {
                Class<?> dynamicClass = unloaded.load(NamespacedKey.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                        .getLoaded();
                Field field = dynamicClass.getDeclaredField("CLASSLOADER");
                field.setAccessible(true);
                field.set(null, (ClassLoader) Materials.class.getClassLoader());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isClassLoaded(String className, @NotNull Instrumentation inst) {
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }
}
