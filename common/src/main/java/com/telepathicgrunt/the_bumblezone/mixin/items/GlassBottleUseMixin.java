package com.telepathicgrunt.the_bumblezone.mixin.items;

import com.llamalad7.mixinextras.sugar.Local;
import com.telepathicgrunt.the_bumblezone.items.GlassBottleBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BottleItem.class, priority = 1200)
public class GlassBottleUseMixin {

    /**
     * Using Glass Bottle to get Sugar Water Bottle
     */
    @Inject(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BottleItem;turnBottleIntoItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", ordinal = 1),
            cancellable = true)
    private void bumblezone$bottleFluidInteractSugarWater(Level world,
                                                          Player user,
                                                          InteractionHand hand,
                                                          CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir,
                                                          @Local(ordinal = 0) BlockPos blockPos)
    {
        ItemStack returnStack = GlassBottleBehavior.useBottleOnSugarWater(((BottleItem)(Object)this), world, user, hand, blockPos);
        if (!returnStack.isEmpty()) {
            cir.setReturnValue(InteractionResultHolder.success(returnStack));
        }
    }

    /**
     * Using Glass Bottle to get honey could anger bees
     */
    @Inject(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;", ordinal = 0, shift = At.Shift.BEFORE),
            cancellable = true)
    private void bumblezone$bottleFluidInteractHoneyLikeFluid(Level world,
                                                              Player user,
                                                              InteractionHand hand,
                                                              CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir,
                                                              @Local(ordinal = 0) BlockPos blockPos)
    {
        if (GlassBottleBehavior.useBottleOnBzHoneyLikeFluid(world, user, hand, blockPos)) {
            cir.setReturnValue(InteractionResultHolder.success(user.getItemInHand(hand)));
        }
    }
}