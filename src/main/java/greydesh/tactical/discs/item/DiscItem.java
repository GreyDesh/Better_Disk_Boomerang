package greydesh.tactical.discs.item;

import greydesh.tactical.discs.entity.DiscEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

public class DiscItem extends Item {
    private final float flightDamage;
    private final float flightSpeed;
    private final int enchantmentValue;

    public DiscItem(Properties properties, float flightDamage, float flightSpeed, int enchantmentValue) {
        super(properties);
        this.flightDamage = flightDamage;
        this.flightSpeed = flightSpeed;
        this.enchantmentValue = enchantmentValue;
    }

    public DiscItem(Properties properties, float flightDamage, float flightSpeed) {
        this(properties, flightDamage, flightSpeed, 15);
    }

    public static ItemAttributeModifiers createAttributes(float meleeDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_ID,
                                meleeDamage - 1.0F,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Item.BASE_ATTACK_SPEED_ID,
                                attackSpeed - 4.0F,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    public float getDamage() {
        return flightDamage;
    }

    public float getFlightSpeed() {
        return flightSpeed;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.8F,
                1.4F / (level.getRandom().nextFloat() * 0.3F + 0.8F)
        );

        if (!level.isClientSide) {
            ItemStack entityStack = itemstack.copyWithCount(1);
            int originSlot = (hand == InteractionHand.OFF_HAND) ? Inventory.SLOT_OFFHAND : player.getInventory().selected;
            DiscEntity discEntity = new DiscEntity(level, player, entityStack, hand, originSlot);
            discEntity.setDamage(this.flightDamage);
            discEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, this.flightSpeed, 0.0F);
            level.addFreshEntity(discEntity);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(CommonComponents.EMPTY);
        tooltipComponents.add(Component.translatable("item.discs.when_thrown").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("attribute.modifier.equals.0",
                (int) this.flightDamage,
                Component.translatable("attribute.discs.thrown_damage")).withStyle(ChatFormatting.DARK_GREEN));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
