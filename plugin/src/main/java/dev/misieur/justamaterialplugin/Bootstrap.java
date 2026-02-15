package dev.misieur.justamaterialplugin;

import dev.misieur.justamaterial.MaterialInjector;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import net.kyori.adventure.text.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class Bootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        try {
            Objects.requireNonNull(Blocks.AIR);
            Objects.requireNonNull(Items.AIR);
            context.getLogger().info(Component.text("Injecting Material"));
            MaterialInjector.injectMaterials(context.getPluginSource().toAbsolutePath().toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
