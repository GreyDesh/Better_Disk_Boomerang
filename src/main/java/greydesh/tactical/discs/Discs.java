package greydesh.tactical.discs;

import com.mojang.logging.LogUtils;
import greydesh.tactical.discs.client.ClientEvents;
import greydesh.tactical.discs.entity.DiscEntity;
import greydesh.tactical.discs.init.ModEntities;
import greydesh.tactical.discs.init.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.slf4j.Logger;

@Mod(Discs.MODID)
public class Discs {
    public static final String MODID = "discs";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Discs(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // Register mod deferred registers
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);

        // Creative tab contents
        modEventBus.addListener(this::addCreative);

        // Register client events
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientEvents::registerRenderers);
        }

        modEventBus.addListener(Config::onLoad);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Looting event listener for discs
        NeoForge.EVENT_BUS.addListener(Discs::onLivingDrops);
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getDirectEntity() instanceof DiscEntity disc) {
            int lootingLevel = disc.getLootingLevel();
            if (lootingLevel > 0) {
                var random = disc.level().getRandom();
                for (ItemEntity drop : event.getDrops()) {
                    ItemStack dropStack = drop.getItem();
                    if (!dropStack.isEmpty()) {
                        int bonus = random.nextInt(lootingLevel + 1);
                        if (bonus > 0) {
                            dropStack.grow(bonus);
                        }
                    }
                }
            }
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Tactical Discs mod initialized successfully.");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.COPPER_DISC.get());
            event.accept(ModItems.IRON_DISC.get());
            event.accept(ModItems.GOLDEN_DISC.get());
            event.accept(ModItems.DIAMOND_DISC.get());
            event.accept(ModItems.NETHERITE_DISC.get());
        }
    }
}
