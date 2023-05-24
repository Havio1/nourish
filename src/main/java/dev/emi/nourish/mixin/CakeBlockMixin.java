package dev.emi.nourish.mixin;

import dev.emi.nourish.NourishHolder;
import dev.emi.nourish.groups.NourishGroup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CakeBlock.class)
public abstract class CakeBlockMixin extends Block {
	
	public CakeBlockMixin(Settings settings) {
		super(settings);
	}

	@Inject(at = @At("RETURN"), method = "tryEat")
	private static void tryEat(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<ActionResult> info) {
		if (info.getReturnValue() == ActionResult.SUCCESS) {
			for (NourishGroup group: NourishHolder.NOURISH.get(player).getProfile().groups) {
				Block block = state.getBlock();
				ItemStack stack = block.getPickStack(world,pos,state);
				if (stack.isIn(TagKey.of(Registries.ITEM.getKey(), group.identifier))) {
					NourishHolder.NOURISH.get(player).consume(group, 2 + 0.1F);
					NourishHolder.NOURISH.sync(player);
				}
			}
		}
	}
}