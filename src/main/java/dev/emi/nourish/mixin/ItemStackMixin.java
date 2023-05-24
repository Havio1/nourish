package dev.emi.nourish.mixin;

import dev.emi.nourish.NourishHolder;
import dev.emi.nourish.NourishMain;
import dev.emi.nourish.groups.NourishGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow
	public abstract Item getItem();

	@Inject(at = @At("RETURN"), method = "getTooltip")
	public void getTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> info) {
		if (NourishMain.debugTooltip && getItem().isFood()) {
			info.getReturnValue().add(Text.literal("NourishFood"));
		}
		if (player == null) return;
		ItemStack stack = (ItemStack) (Object) this;
		Identifier id = Registries.ITEM.getId(stack.getItem());
		List<ItemStack> items = new ArrayList<ItemStack>();
		List<String> groups = new ArrayList<String>();
		if (id.toString().equals("sandwichable:sandwich")) {
			DefaultedList<ItemStack> foods = DefaultedList.ofSize(128, ItemStack.EMPTY);
			Inventories.readNbt(stack.getSubNbt("BlockEntityTag"), foods);
			items.addAll(items);
		} else {
			items.add(stack);
		}
		MinecraftClient client = MinecraftClient.getInstance();
		for (NourishGroup group: NourishHolder.NOURISH.get(client.player).getProfile().groups) {
			for (ItemStack food: items) {
				if (food.isIn(TagKey.of(Registries.ITEM.getKey(), group.identifier))) {
					groups.add(Text.translatable("nourish.group." + group.name).getString());
					break;
				}
			}
		}
		if (groups.size() > 0) {
			info.getReturnValue().add(Text.literal(String.join(", ", groups)).formatted(Formatting.GOLD));
		}
	}
}