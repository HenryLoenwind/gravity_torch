package info.loenwind.gravitytorch;

import static net.neoforged.neoforge.common.ItemAbilities.FIRESTARTER_LIGHT;

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
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
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
import net.neoforged.neoforge.common.ItemAbility;

public final class GravityTorchBlock extends TorchBlock implements SimpleWaterloggedBlock, Fallable {

	private static final float px = 1f / 16f;
	private static final double diameter = 4.0;
	private static final double y_offset = 2.0;
	private static final double height = 10.0 + y_offset;
	private static final VoxelShape AABB = box( //
			8.0 - diameter / 2.0, 0.0, 8.0 - diameter / 2.0, //
			8.0 + diameter / 2.0, height, 8.0 + diameter / 2.0);

	private static final MapCodec<GravityTorchBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(PARTICLE_OPTIONS_FIELD.forGetter(block -> block.flameParticle), propertiesCodec()).apply(instance, GravityTorchBlock::new));

	static final BooleanProperty LIT = AbstractCandleBlock.LIT;
	private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	// Need to check waterlogged because this is what we get after falling into
	// water. We correct this instantly in onLand(), but the FallingEntity gets in
	// one chunk render before that, which means there would be a big blink...
	private static final ToIntFunction<BlockState> LIGHT_EMISSION = blockstate -> blockstate.getValue(LIT) && !blockstate.getValue(WATERLOGGED) ? 14 : 0;

	public static GravityTorchBlock create() {
		return new GravityTorchBlock(ParticleTypes.FLAME,
				BlockBehaviour.Properties.of().noCollission().instabreak().lightLevel(LIGHT_EMISSION).sound(SoundType.STONE).pushReaction(PushReaction.DESTROY));
	}

	public GravityTorchBlock(final SimpleParticleType particle, final BlockBehaviour.Properties bbp) {
		super(particle, bbp);
		registerDefaultState(stateDefinition.any().setValue(LIT, Boolean.TRUE).setValue(WATERLOGGED, Boolean.FALSE));
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext pContext) {
		final FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
		final boolean flag = fluidstate.getType() == Fluids.WATER;
		return super.getStateForPlacement(pContext).setValue(WATERLOGGED, flag).setValue(LIT, !flag);
	}

	@Override
	protected void onPlace(final BlockState pState, final Level pLevel, final BlockPos pPos, final BlockState pOldState, final boolean pIsMoving) {
		// schedule check for falling. Not really needed as we cannot be placed in a
		// position where we don't sit on something.
		pLevel.scheduleTick(pPos, this, getDelayAfterPlace());
	}

	@Override
	protected BlockState updateShape(final BlockState pState, final Direction pDirection, final BlockState pNeighborState, final LevelAccessor pLevel,
			final BlockPos pPos, final BlockPos pNeighborPos) {
		if (pState.getValue(WATERLOGGED)) {
			pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
		}
		// don't super() here, the torch code would break us. Instead schedule a tick
		// which will handle the falling if needed
		pLevel.scheduleTick(pPos, this, getDelayAfterPlace());
		return pState;
	}

	/**
	 * The delay between placement or neighbour changes and us trying to fall
	 */
	private int getDelayAfterPlace() {
		return 2;
	}

	@Override
	protected void tick(final BlockState pState, final ServerLevel pLevel, final BlockPos pPos, final RandomSource pRandom) {
		if (!canSurvive(pState, pLevel, pPos) && pPos.getY() >= pLevel.getMinBuildHeight()) {
			FallingBlockEntity.fall(pLevel, pPos, pState);
			if (pState.getValue(LIT)) {
				pLevel.setBlock(pPos, GravityTorch.BA_BLOCK.get().defaultBlockState(), UPDATE_ALL_IMMEDIATE);
			}
		}
	}

	@Override
	public void onLand(final Level pLevel, final BlockPos pPos, final BlockState pState, final BlockState pReplaceableState,
			final FallingBlockEntity pFallingBlock) {
		if (pState.getValue(WATERLOGGED) && pState.getValue(LIT)) {
			extinguish(null, pState, pLevel, pPos);
		}
	}

	@Override
	protected FluidState getFluidState(final BlockState pState) {
		return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(LIT, WATERLOGGED);
	}

	@Override
	public boolean placeLiquid(final LevelAccessor pLevel, final BlockPos pPos, final BlockState pState, final FluidState pFluidState) {
		if (!pState.getValue(WATERLOGGED) && pFluidState.getType() == Fluids.WATER) {
			final BlockState blockstate = pState.setValue(WATERLOGGED, true);
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
	public @org.jetbrains.annotations.Nullable BlockState getToolModifiedState(final BlockState blockstate, final UseOnContext context,
			final ItemAbility itemAbility, final boolean simulate) {
		return itemAbility == FIRESTARTER_LIGHT && canBeLit(blockstate) ? blockstate.setValue(BlockStateProperties.LIT, true) : null;
	}

	protected boolean canBeLit(final BlockState pState) {
		return !pState.getValue(WATERLOGGED) && !pState.getValue(LIT);
	}

	@Override
	protected void onProjectileHit(final Level pLevel, final BlockState pState, final BlockHitResult pHit, final Projectile pProjectile) {
		if (!pLevel.isClientSide && pProjectile.isOnFire() && canBeLit(pState)) {
			pLevel.setBlock(pHit.getBlockPos(), pState.setValue(LIT, true), 11);
		}
	}

	@Override
	protected void onExplosionHit(final BlockState pState, final Level pLevel, final BlockPos pPos, final Explosion pExplosion,
			final BiConsumer<ItemStack, BlockPos> pDropConsumer) {
		if (pExplosion.canTriggerBlocks() && pState.getValue(LIT)) {
			extinguish(null, pState, pLevel, pPos);
		}
		super.onExplosionHit(pState, pLevel, pPos, pExplosion, pDropConsumer);
	}

	public static void extinguish(@Nullable final Player pPlayer, final BlockState pState, final LevelAccessor pLevel, final BlockPos pPos) {
		pLevel.setBlock(pPos, pState.setValue(LIT, false), 11);
		pLevel.playSound(null, pPos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
		pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos);
	}

	@Override
	public MapCodec<? extends TorchBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(final BlockState p_304673_, final BlockGetter p_304919_, final BlockPos p_304930_, final CollisionContext p_304757_) {
		return AABB;
	}

	@Override
	public void animateTick(final BlockState pState, final Level pLevel, final BlockPos pPos, final RandomSource pRandom) {
		if (pState.getValue(LIT)) {
			final double d0 = pPos.getX() + 0.5;
			final double d1 = pPos.getY() + 0.7 + y_offset * px;
			final double d2 = pPos.getZ() + 0.5;

			pLevel.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
			pLevel.addParticle(flameParticle, d0, d1, d2, 0.0, 0.0, 0.0);
		}
	}

}
