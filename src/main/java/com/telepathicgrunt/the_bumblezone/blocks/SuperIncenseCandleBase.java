package com.telepathicgrunt.the_bumblezone.blocks;

import com.telepathicgrunt.the_bumblezone.blocks.blockentities.SuperIncenseCandleBlockEntity;
import com.telepathicgrunt.the_bumblezone.modinit.BzBlockEntities;
import com.telepathicgrunt.the_bumblezone.modinit.BzItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;


public class SuperIncenseCandleBase extends BaseEntityBlock implements SimpleWaterloggedBlock, SuperCandle {
    public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape AABB = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);

    public SuperIncenseCandleBase() {
        super(Properties.of(Material.DECORATION, MaterialColor.SAND).noOcclusion().strength(0.1F).sound(SoundType.CANDLE).lightLevel((blockState) -> blockState.getValue(LIT) ? 15 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE).setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AABB;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        placeWickIfPossible(level, pos, false);
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        boolean flag = fluidstate.getType() == Fluids.WATER;
        return super.getStateForPlacement(context).setValue(WATERLOGGED, flag);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        placeWickIfPossible(world, pos, false);
        super.neighborChanged(blockstate, world, pos, block, fromPos, notify);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.getValue(WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
            BlockState blockstate = state.setValue(WATERLOGGED, Boolean.TRUE);
            if (state.getValue(LIT)) {
                SuperCandleWick.extinguish(null, level.getBlockState(pos.above()), level, pos.above());
                level.setBlock(pos, blockstate.setValue(LIT, false), 3);
            }
            else {
                level.setBlock(pos, blockstate, 3);
            }

            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        level.scheduleTick(pos, state.getBlock(), 0);
    }

    @Override
    public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource random) {
        super.tick(blockState, serverLevel, blockPos, random);
        serverLevel.sendBlockUpdated(blockPos, blockState, blockState, 8);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (player.getAbilities().mayBuild) {
            if (player.getItemInHand(interactionHand).isEmpty() && blockState.getValue(LIT)) {
                SuperCandleWick.extinguish(player, level.getBlockState(blockPos.above()), level, blockPos.above());
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            // Make item tag. Also needs dispenser behavior
            else if (player.getItemInHand(interactionHand).is(Items.FLINT_AND_STEEL) &&
                    !blockState.getValue(LIT))
            {
                SuperCandleWick.setLit(level, level.getBlockState(blockPos.above()), blockPos.above(), true);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && projectile.isOnFire() && canBeLit(level, state, hit.getBlockPos())) {
            SuperCandleWick.setLit(level, level.getBlockState(hit.getBlockPos().above()), hit.getBlockPos().above(), true);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player != null && player.getAbilities().instabuild) {
            super.playerWillDestroy(level, pos, state, player);
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SuperIncenseCandleBlockEntity superIncenseCandleBlockEntity) {
            ItemStack itemStack = BzItems.SUPER_INCENSE_CANDLE.get().getDefaultInstance();
            superIncenseCandleBlockEntity.saveToItem(itemStack);
            ItemEntity itementity = new ItemEntity(level, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, itemStack);
            itementity.setDefaultPickUpDelay();
            level.addFreshEntity(itementity);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BzBlockEntities.SUPER_INCENSE_CANDLE.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, BzBlockEntities.SUPER_INCENSE_CANDLE.get(), level.isClientSide ? (a, b, c, d) -> {} : SuperIncenseCandleBlockEntity::serverTick);
    }

    public static int getItemColor(ItemStack itemStack) {
        if (itemStack.hasTag()) {
            CompoundTag tag = itemStack.getTag();
            if (tag != null && tag.contains("BlockEntityTag")) {
                CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
                if (blockEntityTag.contains(SuperIncenseCandleBlockEntity.COLOR_TAG)) {
                    return blockEntityTag.getInt(SuperIncenseCandleBlockEntity.COLOR_TAG);
                }
            }
        }
        return 16777215;
    }

    public static int getBlockColor(BlockAndTintGetter world, BlockPos pos, int tintIndex) {
        if (world != null) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SuperIncenseCandleBlockEntity superIncenseCandleBlockEntity) {
                int currentColor = superIncenseCandleBlockEntity.getColor();

                if (tintIndex == 1) {
                    // Change tint of lit top
                    int red = Math.max((currentColor >> 16 & 255), 40);
                    int green = Math.max((currentColor >> 8 & 255), 10);
                    int blue = Math.max((currentColor & 255), 5);
                    currentColor = (Math.min(red + 60, 255) << 16) + (Math.min(green + 30, 255) << 8) + Math.min(blue + 25, 255);
                    return currentColor;
                }
                else {
                    return currentColor;
                }
            }
        }
        return tintIndex;
    }

    @Override
    public void animateTick(BlockState blockState, Level world, BlockPos position, RandomSource random) {
        if (blockState.hasProperty(LIT) && blockState.getValue(LIT)) {
            BlockEntity blockEntity = world.getBlockEntity(position);
            if (blockEntity instanceof SuperIncenseCandleBlockEntity superIncenseCandleBlockEntity && superIncenseCandleBlockEntity.getMobEffect() != null) {
                int color = superIncenseCandleBlockEntity.getColor();
                double red = (double)(color >> 16 & 255) / 255.0D;
                double green = (double)(color >> 8 & 255) / 255.0D;
                double blue = (double)(color & 255) / 255.0D;

                //number of particles in this tick
                for (int i = 0; i < random.nextInt(3); ++i) {
                    this.spawnEffectParticles(world, random, position, blockState, red, green, blue);
                }
            }
        }
        super.animateTick(blockState, world, position, random);
    }

    private void spawnEffectParticles(Level world, RandomSource random, BlockPos position, BlockState blockState, double red, double green, double blue) {
        world.addParticle(ParticleTypes.AMBIENT_ENTITY_EFFECT,
                position.getX() + 0.4d + (random.nextDouble() * 0.2d),
                position.getY() + 0.4d + (random.nextDouble() * 0.2d),
                position.getZ() + 0.4d + (random.nextDouble() * 0.2d),
                red,
                green,
                blue);
    }
}
