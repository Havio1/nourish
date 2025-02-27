package dev.emi.nourish.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.nourish.NourishComponent;
import dev.emi.nourish.NourishHolder;
import dev.emi.nourish.PlayerNourishComponent;
import dev.emi.nourish.client.NourishScreen;
import dev.emi.nourish.effects.NourishEffect;
import dev.emi.nourish.effects.NourishEffect.NourishAttribute;
import dev.emi.nourish.effects.NourishEffectCondition;
import dev.emi.nourish.effects.NourishStatusEffectInstance;
import dev.emi.nourish.groups.NourishGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler>{
	private static final Identifier GUI_TEX = new Identifier("nourish", "textures/gui/gui.png");
	private TexturedButtonWidget nourishWidget;

	@Shadow @Final
	private RecipeBookWidget recipeBook;

	@Shadow private float mouseX;

	@Shadow private float mouseY;

	public InventoryScreenMixin(PlayerScreenHandler container, PlayerInventory inventory, Text text) {
		super(container, inventory, text);
	}

	@Inject(at = @At("TAIL"), method = "init")
	public void init(CallbackInfo info) {
		nourishWidget = new TexturedButtonWidget(this.x + this.backgroundWidth - 9 - 35, this.y + 61, 20, 18, 0, 19, 18, GUI_TEX, (widget) -> {
			MinecraftClient.getInstance().setScreen(new NourishScreen(true));
		});
		this.addDrawableChild(nourishWidget);
	}

	@Inject(at = @At("TAIL"), method = "render")
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (this.client.player.currentScreenHandler.getCursorStack().isEmpty() && (this.focusedSlot == null || !this.focusedSlot.hasStack())) {
			if (!recipeBook.isOpen()) {
				if (mouseX < this.x - 8 && mouseX > this.x - 122 && mouseY > this.y && mouseY < this.height - this.y) {
					Collection<StatusEffectInstance> collection = this.client.player.getStatusEffects();
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					int effectHeight = 33;
					if (collection.size() > 5) {
						effectHeight = 132 / (collection.size() - 1);
					}
					int e = (mouseY - this.y) / effectHeight;
					int r = (mouseY - this.y) % effectHeight;
					List<StatusEffectInstance> effects = Ordering.natural().sortedCopy(collection);
					if (e < effects.size() && r > 2 && r < 29) {
						StatusEffectInstance effect = effects.get(e);
						if (effect instanceof NourishStatusEffectInstance) {
							NourishHolder.NOURISH.maybeGet(this.client.player).ifPresent(comp -> {
								List<NourishEffect> nourishEffects = Lists.newArrayList();
								for (NourishEffect eff: comp.getProfile().effects) {
									if (eff.test(comp)) {
										for (Pair<Identifier, Integer> status : eff.status_effects) {
											if (Registries.STATUS_EFFECT.get(status.getLeft()) == effect.getEffectType() && status.getRight() == effect.getAmplifier()) {
												nourishEffects.add(eff);
											}
										}
									}
								}
								if (nourishEffects.size() > 0) {
									List<Text> lines = Lists.newArrayList();
									boolean first = true;
									for (NourishEffect eff: nourishEffects) {
										if (!first) {
											lines.add(Text.literal(""));
										}
										first = false;
										addCause(lines, eff);
									}
									this.renderTooltip(matrices, lines, mouseX, mouseY);
								}
							});
						}
					}
				} else if (nourishWidget.isHovered()) {
					this.renderTooltip(matrices, getAttributesTooltip(), mouseX, mouseY);
				}
			}
		}
	}

	@Unique
	private List<Text> getAttributesTooltip() {
		List<Text> list = new ArrayList<Text>();
		NourishComponent comp = NourishHolder.NOURISH.get(client.player);
		boolean first = true;
		for (NourishEffect eff: comp.getProfile().effects) {
			if (eff.test(comp)) {
				if (eff.attributes.size() > 0) {
					if (!first) {
						list.add(Text.literal(""));
					}
					first = false;
					addCause(list, eff);
				}
				for (NourishAttribute attr : eff.attributes) {
					EntityAttribute attribute = Registries.ATTRIBUTE.get(attr.id);
					EntityAttributeModifier modifier = new EntityAttributeModifier(PlayerNourishComponent.ATTRIBUTE_UUID,
						"nourish", attr.amount, attr.operation);
					double v = modifier.getValue();
					if (modifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_BASE
							&& modifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
						if (attribute.equals(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)) {
							v *= 10.0D;
						}
					} else {
						v *= 100.0D;
					}
					Text text = Text.translatable(attribute.getTranslationKey());
					if (v > 0) {
						list.add(Text.translatable("attribute.modifier.plus." + modifier.getOperation().getId(),
							ItemStack.MODIFIER_FORMAT.format(v), text).formatted(Formatting.BLUE));
					} else {
						list.add(Text.translatable("attribute.modifier.take." + modifier.getOperation().getId(),
							ItemStack.MODIFIER_FORMAT.format(-v), text).formatted(Formatting.RED));
					}
				}
			}
		}
		return list;
	}

	@Unique
	private void addCause(List<Text> lines, NourishEffect eff) {
		lines.add(Text.translatable("nourish.effect.caused"));
		for (NourishEffectCondition condition: eff.conditions) {
			List<String> groups = Lists.newArrayList();
			for (NourishGroup group: condition.groups) {
				groups.add(Text.translatable("nourish.group." + group.name).getString());
			}
			Text text = Text.literal(String.join(", ", groups));
			String root = "nourish.effect.cause.multiple";
			if (groups.size() == 1) {
				root = "nourish.effect.cause.single";
			}
			if (condition.above != -1.0F) {
				if (condition.below != 2.0F) {
					lines.add(Text.translatable(root + ".above_and_below", text,
						(int) (condition.above * 100), (int) (condition.below * 100)));
				} else {
					lines.add(Text.translatable(root + ".above", text, (int) (condition.above * 100)));
				}
			} else {
				lines.add(Text.translatable(root + ".below", text, (int) (condition.below * 100)));
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "handledScreenTick")
	public void tick(CallbackInfo info) {
		if (this.client.interactionManager.hasCreativeInventory()) {
			//this.client.setScreen(new CreativeInventoryScreen(this.client.player);
		} else {
			nourishWidget.setPosition(this.x + this.backgroundWidth - 9 - 35, this.y + 61);// :tiny_potato:
		}
	}
}