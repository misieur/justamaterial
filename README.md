# Justamaterial
A Java library whose sole purpose is to inject new constants in the Bukkit Material class on Paper servers.
Justamaterial currently supports Minecraft from 1.21 to 1.21.11.

## How does it work
When you call Justamaterial from your plugin's PluginBootstrap, Justamaterial will dynamically attach a java agent to your JVM, when the Material class will be loaded,
Justamaterial will replace its bytecode with a new implementation generated using Byte Buddy from data it knows and the new constants that you want to add.
> [!WARNING]  
> Justamaterial doesn't take the existing class and add new constants to it, it would be too risky and unsupported.
> Justamaterial builds a whole new class from data it knows, which means if you use a fork of PaperMC that would modify the Material class,
> the library will not adapt its code to match your fork's modifications.

## How to use it
First, if you don't use PluginBootstrap in your paper plugin, this is mandatory, [follow this guide if you are not already using it](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#bootstrapper).

You first need to add the library to your dependencies. *I recommend you to have your own maven repository or make a fork of my project.*

##### build.gradle.kts
```kts
repositories {
    maven {
        url = uri("https://repo.misieur.me/repository")
        content {
            includeGroup("dev.misieur")
        }
    }
}

dependencies {
    implementation("dev.misieur:justamaterial:1.0-SNAPSHOT")
}
```
> [!NOTE]  
> Relocating justamaterial isn't supported and using any tool that would remove classes that it thinks are unused will break the library. *The `dev.misieur.justamaterial` package should be excluded from these tools.*
Then to inject new constants use this code:

```java
try {
    // Make sure Minecraft internal classes are loaded (mandatory)
    Objects.requireNonNull(Blocks.AIR);
    Objects.requireNonNull(Items.AIR);
    
    MaterialInjector.injectMaterials(
            context.getPluginSource().toAbsolutePath().toString(), // Path to the jar that contains the `dev.misieur.justamaterial` package
            new MaterialInfo(
                    "MY_PLUGIN_MY_ITEM", // UPPER_SNAKE_CASE replace ':' with '_'
                    null, // only if you now what you are doing
                    null, // only if you now what you are doing
                    NamespacedKey.fromString("my_plugin:my_item") // The item/block key (required for non 'minecraft' namespace)
            )
            // You can add more arguments or use an array of MaterialInfo
    );
} catch (Exception | LinkageError e) {
    e.printStackTrace();
    System.exit(0); // You most likely want to stop the server if there is an exception
}
```
