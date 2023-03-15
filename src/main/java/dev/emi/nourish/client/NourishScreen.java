package dev.emi.nourish.client;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.nourish.NourishComponent;
import dev.emi.nourish.NourishHolder;
import dev.emi.nourish.NourishMain;
import dev.emi.nourish.groups.NourishGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static dev.emi.nourish.NourishMain.MOD_ID;

public class NourishScreen extends Screen {
	private static final Identifier GUI_TEX = new Identifier(MOD_ID, "textures/gui/gui.png");
	private static final Identifier NOURISHMENT_METER_TEX = new Identifier(MOD_ID, "textures/gui/gui_nourishment_meter_filled.png");
	private final int NOURISHMENT_METER_FILL_WIDTH = 91;
	private boolean returnToInv;
	private int maxNameLength = 0;
	private int w;
	private int h;
	private int x;
	private int y;

	int[] rgbValues;
	float alphaValue;

	private TexturedButtonWidget exitWidget;

	public NourishScreen() {
		super(Text.translatable(("nourish.gui.nourishment")).formatted(Formatting.BOLD));
	}

	public NourishScreen(boolean returnToInv) {
		this();
		this.returnToInv = returnToInv;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void init() {
		List<NourishGroup> groups = NourishHolder.NOURISH.get(client.player).getProfile().groups;
		for (NourishGroup group: groups) {
			int l = this.textRenderer.getWidth(Text.translatable(("nourish.group." + group.identifier.getPath()).toString()));
			if (l > maxNameLength) {
				maxNameLength = l;
			}
		}
		w = maxNameLength + 120;
		if(w < 166){
			w = 166;
		}
		h = 34 + groups.size() * 20;
		if (groups.size() > 0 && groups.get(groups.size() - 1).secondary) {
			h += 24;
		}
		x = (width - w) / 2 - 2;
		y = (height - h) / 2 - 2;

		exitWidget = new TexturedButtonWidget(x + 171, y + 12,16, 16, 64, 32, 16, GUI_TEX, (widget) -> {
			MinecraftClient.getInstance().setScreen(new InventoryScreen(client.player));
		});
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {

		//darken background when screen is on screen
		this.renderBackground(matrices);

		RenderSystem.setShaderTexture(0, GUI_TEX);
		//draws filled background
		DrawableHelper.drawTexture(matrices, x + 4, y + 4, 4 * w, 3 * h, w - 4, h - 4, 256 * w, 256 * h);

		//top border
		DrawableHelper.drawTexture(matrices, x + 4, y, 4 * w, 0, w - 4, 4, 256 * w, 256);
		//bottom border
		DrawableHelper.drawTexture(matrices, x + 4, y + h, 3 * w, 4, w - 4, 4, 256 * w, 256);
		//left border
		DrawableHelper.drawTexture(matrices, x, y + 4, 0, 4 * h, 4, h - 4, 256, 256 * h);
		//right border
		DrawableHelper.drawTexture(matrices, x + w, y + 4, 4, 3 * h, 4, h - 4, 256, 256 * h);

		this.drawTexture(matrices, x, y, 0, 0, 4, 4);
		this.drawTexture(matrices, x + w, y, 4, 0, 4, 4);
		this.drawTexture(matrices, x, y + h, 0, 4, 4, 4);
		this.drawTexture(matrices, x + w, y + h, 4, 4, 4, 4);
		int yo = 28;
		boolean secondary = false;

		for (NourishGroup group: NourishHolder.NOURISH.get(client.player).getProfile().groups) {
			rgbValues = group.getColor();
			alphaValue = 1.0f;
			if (group.secondary && !secondary) {
				secondary = true;
				Text t = Text.translatable("nourish.gui.secondary");
				int sw = this.textRenderer.getWidth(t.getString());
				this.textRenderer.draw(matrices, t.getString(), x + w / 2 - sw / 2, y + yo + 9, 4210752);
				yo += 20;
			}
			this.textRenderer.draw(matrices, Text.translatable("nourish.group." + group.identifier.getPath()).getString(), x + 10, y + yo + 9, 4210752);
			NourishComponent comp = NourishHolder.NOURISH.get(client.player);
			RenderSystem.setShaderTexture(0, GUI_TEX);

			//draw empty progress bar texture
			this.drawTexture(matrices, x + maxNameLength + 20, y + yo + 8, 0, 8, 91, 11);

			//changes shader, allowing for alpha-transparency and switches to meter texture
			//RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.enableBlend();
			RenderSystem.setShaderTexture(0, NOURISHMENT_METER_TEX);

			//draws uncolored meter texture
			this.drawTexture(matrices,x + maxNameLength + 20,y + yo + 11,0, 0, 0 + Math.round(NOURISHMENT_METER_FILL_WIDTH * comp.getValue(group) + 0.19f), 5);

			//for debugging rgbValues' values:
			//System.out.println(Arrays.toString(rgbValues) + " " + group.name + " from NourishScreen.java");

			//sets color and draws colored meter texture
			RenderSystem.setShaderColor(rgbValues[0] / 255f,rgbValues[1] / 255f,rgbValues[2] / 255f,alphaValue);
			this.drawTexture(matrices,x + maxNameLength + 20,y + yo + 11,0, 0, 0 + Math.round(NOURISHMENT_METER_FILL_WIDTH * comp.getValue(group) + 0.19f), 5);

			//revert changes to shader
			RenderSystem.disableBlend();
			RenderSystem.setShaderTexture(0, GUI_TEX);
			RenderSystem.setShaderColor(1.0f,1.0f,1.0f,1.0f);

			if (mouseX > x + maxNameLength + 18 && mouseY > y + yo + 8 && mouseX < x + maxNameLength + 112 && mouseY < y + yo + 21) {
				if (group.description) {
					List<Text> lines = Lists.newArrayList();
					lines.add(Text.translatable("nourish.group.description." + group.identifier.getPath()));
					this.renderTooltip(matrices, lines, mouseX, mouseY);
				}
			}
			yo += 20;
		}
		this.addDrawableChild(exitWidget);
		int tw = this.textRenderer.getWidth(this.title.getString());
		this.textRenderer.draw(matrices, this.title.getString(), (width - tw) / 2, y + 15.0F, 4210752);
		super.render(matrices, mouseX, mouseY, delta);
	}

	public boolean keyPressed(int int_1, int int_2, int int_3) {
		if (client.options.inventoryKey.matchesKey(int_1, int_2)) {
			if (returnToInv) {
				client.setScreen(new InventoryScreen(client.player));
			} else {
				this.onClose();
			}
			return true;
		} else {
			return super.keyPressed(int_1, int_2, int_3);
		}
	}

	public void onClose() {
		if (returnToInv) {
			client.setScreen(new InventoryScreen(client.player));
			return;
		}
		super.shouldCloseOnEsc();
	}

	public boolean isPauseScreen() {
		return false;
	}
}