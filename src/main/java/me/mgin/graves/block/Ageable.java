package me.mgin.graves.block;

import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

import com.mojang.authlib.GameProfile;

import me.mgin.graves.block.entity.GraveBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Degradable;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public interface Ageable<T extends Enum<T>> {
    int field_31056 = 4;

    Optional<BlockState> getDegradationResultState(BlockState state);
    //Optional<BlockEntity> getDegradationResultEntity(BlockEntity blockEntity);

    float getDegradationChanceMultiplier();

    default void tickDegradation(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        float f = 0.05688889F;
        if (random.nextFloat() < 0.05688889F) {
            this.tryDegrade(state, world, pos, random);
        }

    }

    T getDegradationLevel();

    default void tryDegrade(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int i = this.getDegradationLevel().ordinal();
        int j = 0;
        int k = 0;
        Iterator var8 = BlockPos.iterateOutwards(pos, 4, 4, 4).iterator();

        while(var8.hasNext()) {
            BlockPos blockPos = (BlockPos)var8.next();
            int l = blockPos.getManhattanDistance(pos);
            if (l > 4) {
                break;
            }

            if (!blockPos.equals(pos)) {
                BlockState blockState = world.getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (block instanceof Degradable) {
                    Enum<?> enum_ = ((Degradable)block).getDegradationLevel();
                    if (this.getDegradationLevel().getClass() == enum_.getClass()) {
                        int m = enum_.ordinal();
                        if (m < i) {
                            return;
                        }

                        if (m > i) {
                            ++k;
                        } else {
                            ++j;
                        }
                    }
                }
            }
        }
        float f = (float)(k + 1) / (float)(k + j + 1);
        float g = f * f * this.getDegradationChanceMultiplier();
        if (random.nextFloat() < g) {
            this.getDegradationResultState(state).ifPresent((statex) -> {
            	GraveBlockEntity entity = (GraveBlockEntity)world.getBlockEntity(pos);
            	GameProfile owner = entity.getGraveOwner();
            	String name = entity.getCustomNametag();
            	DefaultedList<ItemStack> items = entity.getItems();
            	int xp = entity.getXp();
                world.setBlockState(pos, statex);
                GraveBlockEntity newEntity = new GraveBlockEntity(pos, statex);
                newEntity.setGraveOwner(owner);
                newEntity.setItems(items);
                newEntity.setCustomNametag(name);
                newEntity.setXp(xp);
                world.addBlockEntity(newEntity);
            });
        }

    }
}
