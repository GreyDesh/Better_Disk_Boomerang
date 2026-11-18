package greydesh.tactical.discs.init;

import greydesh.tactical.discs.Discs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static final TagKey<Item> DISCS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Discs.MODID, "discs"));
}
