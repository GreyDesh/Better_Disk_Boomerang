package greydesh.tactical.discs.init;

import greydesh.tactical.discs.Discs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> LUMBERJACK =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Discs.MODID, "lumberjack"));

    public static final ResourceKey<Enchantment> BOUNCING =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Discs.MODID, "bouncing"));
}
