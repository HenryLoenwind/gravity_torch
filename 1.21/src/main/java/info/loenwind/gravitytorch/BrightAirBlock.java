package info.loenwind.gravitytorch;

import static info.loenwind.gravitytorch.GravityTorchBlock.LIT;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = GravityTorch.MODID)
public class BrightAirBlock extends AirBlock {
	private static final int LIGHT_LEVEL = 12;
	public static final MapCodec<AirBlock> CODEC = simpleCodec(BrightAirBlock::new);

	@Override
	public MapCodec<AirBlock> codec() {
		return CODEC;
	}

	public BrightAirBlock(final Properties p_48756_) {
		super(p_48756_);
	}

	public static BrightAirBlock create() {
		return new BrightAirBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air().lightLevel(state -> LIGHT_LEVEL));
	}

	@Override
	protected void onPlace(final BlockState pState, final Level pLevel, final BlockPos pPos, final BlockState pOldState, final boolean pIsMoving) {
		if (!pLevel.isClientSide) {
			pLevel.scheduleTick(pPos, this, 3);
		}
	}

	@Override
	protected void tick(final BlockState pState, final ServerLevel pLevel, final BlockPos pPos, final RandomSource pRandom) {
		final List<FallingBlockEntity> list = Lists.newArrayList();
		pLevel.getEntities(EntityType.FALLING_BLOCK, AABB.encapsulatingFullBlocks(pPos, pPos), fbe -> fbe.getBlockState().getBlock() instanceof GravityTorchBlock,
				list, 1);
		if (list.isEmpty()) {
			pLevel.removeBlock(pPos, false);
		} else {
			pLevel.scheduleTick(pPos, this, 3);
		}
	}

	@Override
	public void animateTick(final BlockState pState, final Level pLevel, final BlockPos pPos, final RandomSource pRandom) {
		pLevel.addParticle(ParticleTypes.SMOKE, pPos.getX() + 0.5, pPos.getY() + pRandom.nextFloat(), pPos.getZ() + 0.5, 0.0, -0.1 * pRandom.nextFloat(), 0.0);
	}

	@SubscribeEvent
	public static void onEntityTick(final EntityTickEvent.Post event) {
		if (event.getEntity() instanceof final FallingBlockEntity fbe && fbe.isAlive() && fbe.getBlockState().getBlock() instanceof GravityTorchBlock
				&& fbe.getBlockState().getValue(LIT) && fbe.level().isEmptyBlock(fbe.blockPosition())
				&& fbe.level().getLightEmission(fbe.blockPosition()) < LIGHT_LEVEL) {
			fbe.level().setBlock(fbe.blockPosition(), GravityTorch.BA_BLOCK.get().defaultBlockState(), UPDATE_ALL_IMMEDIATE);
		}
	}

}
