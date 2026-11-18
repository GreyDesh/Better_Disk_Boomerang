package greydesh.tactical.discs.init;

import greydesh.tactical.discs.Discs;
import greydesh.tactical.discs.entity.DiscEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Discs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DiscEntity>> DISC =
            ENTITY_TYPES.register("disc", () -> EntityType.Builder.<DiscEntity>of(DiscEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.2F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("disc"));
}
