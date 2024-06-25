package com.example.examplemod;

import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GravityTorchBlock extends TorchBlock implements SimpleWaterloggedBlock, Fallable {

	private static final float px = 1f / 16f;
	private static final double diameter = 4.0;
	private static final double y_offset = 2.0;
	private static final double height = 10.0 + y_offset;
	private static final VoxelShape AABB = box(
			8.0 - diameter / 2.0, 0.0,    8.0 - diameter / 2.0,
			8.0 + diameter / 2.0, height, 8.0 + diameter / 2.0);


	public static final MapCodec<GravityTorchBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(PARTICLE_OPTIONS_FIELD.forGetter(block -> block.flameParticle), propertiesCodec())
			.apply(instance, GravityTorchBlock::new)
			);

	public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final ToIntFunction<BlockState> LIGHT_EMISSION = blockstate -> blockstate.getValue(LIT) ? 14 : 0;


	public static GravityTorchBlock create() {
		return new GravityTorchBlock(
				ParticleTypes.FLAME,
				BlockBehaviour.Properties.of()
				.noCollission()
				.instabreak()
				.lightLevel(LIGHT_EMISSION)
				.sound(SoundType.STONE)
				.pushReaction(PushReaction.DESTROY)
				);
	}

	public GravityTorchBlock(SimpleParticleType particle, BlockBehaviour.Properties bbp) {
		super(particle, bbp);
		registerDefaultState(stateDefinition.any().setValue(LIT, Boolean.TRUE).setValue(WATERLOGGED, Boolean.FALSE));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
		boolean flag = fluidstate.getType() == Fluids.WATER;
		return super.getStateForPlacement(pContext).setValue(WATERLOGGED, flag).setValue(LIT, !flag);
	}

	@Override
	protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		pLevel.scheduleTick(pPos, this, this.getDelayAfterPlace());
	}

	@Override
	protected BlockState updateShape(
			BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
		if (pState.getValue(WATERLOGGED)) {
			pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
		}
		// don't super() here, the torch code would break us. Instead schedule a tick which will handle the falling if needed 
		pLevel.scheduleTick(pPos, this, getDelayAfterPlace());
		return pState;
	}

	/**
	 * The delay between placement or neighbour changes and us trying to fall
	 */
	protected int getDelayAfterPlace() {
		return 2;
	}

	@Override
	protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
		if (!canSurvive(pState, pLevel, pPos) && pPos.getY() >= pLevel.getMinBuildHeight()) {
			FallingBlockEntity.fall(pLevel, pPos, pState);
		}
	}

	@Override
	public void onLand(Level pLevel, BlockPos pPos, BlockState pState, BlockState pReplaceableState, FallingBlockEntity pFallingBlock) {
		if (pState.getValue(WATERLOGGED) && pState.getValue(LIT)) {
			extinguish(null, pState, pLevel, pPos);
		}
	}
	
	@Override
	protected FluidState getFluidState(BlockState pState) {
		return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(LIT, WATERLOGGED);
	}

	@Override
	public boolean placeLiquid(LevelAccessor pLevel, BlockPos pPos, BlockState pState, FluidState pFluidState) {
		if (!pState.getValue(WATERLOGGED) && pFluidState.getType() == Fluids.WATER) {
			BlockState blockstate = pState.setValue(WATERLOGGED, true);
			if (pState.getValue(LIT)) {
				extinguish(null, blockstate, pLevel, pPos);
			} else {
				pLevel.setBlock(pPos, blockstate, 3);
			}

			pLevel.scheduleTick(pPos, pFluidState.getType(), pFluidState.getType().getTickDelay(pLevel));
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected ItemInteractionResult useItemOn(
			ItemStack pStack, BlockState blockstate, Level level, BlockPos blockpos, Player player, InteractionHand pHand, BlockHitResult pHitResult) {
		if (pStack.getItem() == Items.FLINT_AND_STEEL && player.getAbilities().mayBuild && !blockstate.getValue(LIT) && !blockstate.getValue(WATERLOGGED)) {
			level.playSound(player, blockpos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
			level.setBlock(blockpos, blockstate.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)), 11);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockpos);
			if (player != null) {
				pStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(pHand));
			}

			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		} else {
			return super.useItemOn(pStack, blockstate, level, blockpos, player, pHand, pHitResult);
		}
	}

	public static boolean isLit(BlockState pState) {
		return pState.hasProperty(LIT) && pState.getValue(LIT);
	}

	protected boolean canBeLit(BlockState pState) {
		return !pState.getValue(WATERLOGGED) && !pState.getValue(LIT);
	}

	@Override
	protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
		if (!pLevel.isClientSide && pProjectile.isOnFire() && canBeLit(pState)) {
			setLit(pLevel, pState, pHit.getBlockPos(), true);
		}
	}

	private static void setLit(LevelAccessor pLevel, BlockState pState, BlockPos pPos, boolean pLit) {
		pLevel.setBlock(pPos, pState.setValue(LIT, pLit), 11);
	}

	@Override
	protected void onExplosionHit(BlockState pState, Level pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
		if (pExplosion.canTriggerBlocks() && pState.getValue(LIT)) {
			extinguish(null, pState, pLevel, pPos);
		}

		super.onExplosionHit(pState, pLevel, pPos, pExplosion, pDropConsumer);
	}

	public static void extinguish(@Nullable Player pPlayer, BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
		setLit(pLevel, pState, pPos, false);
		pLevel.playSound(null, pPos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
		pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos);
	}

	@Override
	public MapCodec<? extends TorchBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState p_304673_, BlockGetter p_304919_, BlockPos p_304930_, CollisionContext p_304757_) {
		return AABB;
	}

	@Override
	public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
		if (pState.getValue(LIT)) {
			double d0 = (double)pPos.getX() + 0.5;
			double d1 = (double)pPos.getY() + 0.7 + y_offset * px;
			double d2 = (double)pPos.getZ() + 0.5;
	
			pLevel.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
			pLevel.addParticle(flameParticle, d0, d1, d2, 0.0, 0.0, 0.0);
		}
	}

}
