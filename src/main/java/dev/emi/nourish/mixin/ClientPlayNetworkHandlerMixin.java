package dev.emi.nourish.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.nourish.effects.NourishStatusEffectInstance;
import dev.emi.nourish.wrapper.EntityPotionEffectS2CPacketWrapper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;

import java.util.Optional;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

	@Redirect(method = "onEntityStatusEffect", at = @At(value = "NEW", args = "class=net/minecraft/entity/effect/StatusEffectInstance"))
	public StatusEffectInstance onEntityPotionEffect(StatusEffect type, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon, StatusEffectInstance hiddenEffect, Optional factorCalculationData, EntityStatusEffectS2CPacket packet) {
		if (((EntityPotionEffectS2CPacketWrapper) packet).getNourishFlag()) {
			return new NourishStatusEffectInstance(type, duration, amplifier);
		}
		return new StatusEffectInstance(type, duration, amplifier, ambient, showParticles, showIcon);
	}
}