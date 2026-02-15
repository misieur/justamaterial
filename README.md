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