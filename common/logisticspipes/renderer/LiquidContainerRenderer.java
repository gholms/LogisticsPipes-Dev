package logisticspipes.renderer;

import logisticspipes.items.LogisticsLiquidContainer;
import logisticspipes.proxy.SimpleServiceLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.liquids.LiquidStack;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LiquidContainerRenderer implements IItemRenderer {

	private final EntityItem dummyEntityItem = new EntityItem(null);
	private boolean useThis = true;
	private RenderItem renderItem;

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return useThis;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return false;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		Minecraft mc = FMLClientHandler.instance().getClient();
		if (item.getItem() instanceof LogisticsLiquidContainer) {
			LiquidStack liquid = SimpleServiceLocator.logisticsLiquidManager.getLiquidFromContainer(item);
			if (liquid == null) {
				doRenderItem(item, mc, type);
				return;
			}
			ItemStack liquidItem = liquid.asItemStack();
			GL11.glPushMatrix();
			if(type == ItemRenderType.INVENTORY) {
				GL11.glScaled(0.45, 0.75, 1);
				GL11.glTranslated(10, 2.5, 0);
			} else {
				GL11.glScaled(0.45, 0.75, 0.45);
				GL11.glTranslated(0, 0.09, 0);
			}
			doRenderItem(liquidItem, mc, type);
			GL11.glPopMatrix();
			doRenderItem(item, mc, type);
		}
	}

	public void doRenderItem(ItemStack itemstack, Minecraft mc, ItemRenderType type) {
		useThis = false;
		if (renderItem == null) {
			renderItem = new RenderItem() {
				public boolean shouldBob() {
					return false;
				};

				public boolean shouldSpreadItems() {
					return false;
				};
			};
			renderItem.setRenderManager(RenderManager.instance);
		}
		if(type == ItemRenderType.INVENTORY) {
			renderItem.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.renderEngine, itemstack, 0, 0);
		} else {
			GL11.glPushMatrix();
			dummyEntityItem.setEntityItemStack(itemstack);
            GL11.glScalef(2F, 2F, 2F);
			renderItem.doRenderItem(dummyEntityItem, 0, 0, 0, 0, 0);
			GL11.glPopMatrix();
		}
		useThis = true;
	}
}
