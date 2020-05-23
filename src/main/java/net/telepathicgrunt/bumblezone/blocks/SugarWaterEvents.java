package net.telepathicgrunt.bumblezone.blocks;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.telepathicgrunt.bumblezone.Bumblezone;
import net.telepathicgrunt.bumblezone.items.BzItems;


@Mod.EventBusSubscriber(modid = Bumblezone.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SugarWaterEvents
{
	public static Block	SUGAR_WATER_BLOCK;

	/**
	 * Call this in FMLCommonSetupEvent as we cannot create static 
	 * instance of the block before it is actually made and registered.
	 */
	public static void setup()
	{
		SUGAR_WATER_BLOCK = BzBlocks.SUGAR_WATER_BLOCK.get().getDefaultState().getBlock();
	}

	/**
	 * Allow player to harvest sugar water with bottles
	 */
	@SubscribeEvent
	public static void getSugarWater(PlayerInteractEvent.RightClickItem event)
	{
		PlayerEntity playerEntity = (PlayerEntity) event.getEntity();
		World world = event.getWorld();
		RayTraceResult raytraceresult = rayTrace(world, playerEntity, RayTraceContext.FluidMode.SOURCE_ONLY);

		if (raytraceresult.getType() == RayTraceResult.Type.BLOCK && 
			world.getBlockState(((BlockRayTraceResult)raytraceresult).getPos()) == BzBlocks.SUGAR_WATER_BLOCK.get().getDefaultState())
		{
			Hand playerHand = event.getHand();
			ItemStack itemstack = playerEntity.getHeldItem(playerHand);

			if (itemstack.getItem() == Items.GLASS_BOTTLE)
			{
				world.playSound(playerEntity, playerEntity.getPosX(), playerEntity.getPosY(), playerEntity.getPosZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
				itemstack.shrink(1); // remove current honey bottle

				if (itemstack.isEmpty())
				{
					playerEntity.setHeldItem(playerHand, new ItemStack(BzItems.SUGAR_WATER_BOTTLE.get())); // places sugar water bottle in hand
				}
				else if (!playerEntity.inventory.addItemStackToInventory(new ItemStack(BzItems.SUGAR_WATER_BOTTLE.get()))) // places sugar water bottle in inventory
				{
					playerEntity.dropItem(new ItemStack(BzItems.SUGAR_WATER_BOTTLE.get()), false); // drops sugar water bottle if inventory is full
				}

				event.setCanceled(true);
			}
		}
	}


	//*borrowed* from the Item class lol
	protected static RayTraceResult rayTrace(World world, PlayerEntity player, RayTraceContext.FluidMode fluidMode)
	{
		float pitch = player.rotationPitch;
		float yaw = player.rotationYaw;
		Vec3d eyePos = player.getEyePosition(1.0F);
		float f2 = MathHelper.cos(-yaw * ((float) Math.PI / 180F) - (float) Math.PI);
		float f3 = MathHelper.sin(-yaw * ((float) Math.PI / 180F) - (float) Math.PI);
		float f4 = -MathHelper.cos(-pitch * ((float) Math.PI / 180F));
		float yTargetNormalized = MathHelper.sin(-pitch * ((float) Math.PI / 180F));
		float xTargetNormalized = f3 * f4;
		float zTargetNormalized = f2 * f4;
		double targetDistance = player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue();
		Vec3d targetPos = eyePos.add(
			(double) xTargetNormalized * targetDistance, 
			(double) yTargetNormalized * targetDistance, 
			(double) zTargetNormalized * targetDistance);
		return world.rayTraceBlocks(new RayTraceContext(eyePos, targetPos, RayTraceContext.BlockMode.OUTLINE, fluidMode, player));
	}
}
