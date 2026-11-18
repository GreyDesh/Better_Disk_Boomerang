package greydesh.tactical.discs.init;

import greydesh.tactical.discs.Discs;
import greydesh.tactical.discs.item.DiscItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Discs.MODID);

    // Melee damage is 2 less than thrown damage:
    // Copper: thrown 3, melee 1
    // Iron: thrown 4, melee 2
    // Golden: thrown 3, melee 1
    // Diamond: thrown 5, melee 3
    // Netherite: thrown 6, melee 4

    public static final DeferredItem<DiscItem> COPPER_DISC = ITEMS.register(
            "copper_disc",
            () -> new DiscItem(
                    new Item.Properties().stacksTo(1).durability(190).attributes(DiscItem.createAttributes(1.0F, 2.0F)),
                    3.0F,
                    0.90F,
                    14
            )
    );

    public static final DeferredItem<DiscItem> IRON_DISC = ITEMS.register(
            "iron_disc",
            () -> new DiscItem(
                    new Item.Properties().stacksTo(1).durability(250).attributes(DiscItem.createAttributes(2.0F, 2.0F)),
                    4.0F,
                    0.90F,
                    14
            )
    );

    public static final DeferredItem<DiscItem> GOLDEN_DISC = ITEMS.register(
            "golden_disc",
            () -> new DiscItem(
                    new Item.Properties().stacksTo(1).durability(32).attributes(DiscItem.createAttributes(1.0F, 2.0F)),
                    3.0F,
                    0.90F,
                    22
            )
    );

    public static final DeferredItem<DiscItem> DIAMOND_DISC = ITEMS.register(
            "diamond_disc",
            () -> new DiscItem(
                    new Item.Properties().stacksTo(1).durability(1561).rarity(Rarity.RARE).attributes(DiscItem.createAttributes(3.0F, 2.0F)),
                    5.0F,
                    0.90F,
                    10
            )
    );

    public static final DeferredItem<DiscItem> NETHERITE_DISC = ITEMS.register(
            "netherite_disc",
            () -> new DiscItem(
                    new Item.Properties().stacksTo(1).durability(2031).fireResistant().rarity(Rarity.EPIC).attributes(DiscItem.createAttributes(4.0F, 2.0F)),
                    6.0F,
                    0.90F,
                    15
            )
    );
}
