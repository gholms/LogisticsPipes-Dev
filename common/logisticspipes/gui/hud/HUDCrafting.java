package logisticspipes.gui.hud;

import java.util.ArrayList;
import java.util.List;

import logisticspipes.logic.BaseLogicCrafting;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.utils.ItemIdentifierStack;
import logisticspipes.utils.gui.BasicGuiHelper;
import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

public class HUDCrafting extends BasicHUDGui {
	
	private final PipeItemsCraftingLogistics pipe;
	
	public HUDCrafting(PipeItemsCraftingLogistics pipe) {
		this.pipe = pipe;
	}
	
	@Override
	public void renderHeadUpDisplay(double d, boolean day, Minecraft mc) {
		if(day) {
        	GL11.glColor4b((byte)64, (byte)64, (byte)64, (byte)64);
        } else {
        	GL11.glColor4b((byte)127, (byte)127, (byte)127, (byte)64);	
        }
		BasicGuiHelper.drawGuiBackGround(mc, -50, -50, 50, 50, 0);
		if(day) {
        	GL11.glColor4b((byte)64, (byte)64, (byte)64, (byte)127);
        } else {
        	GL11.glColor4b((byte)127, (byte)127, (byte)127, (byte)127);	
        }

		GL11.glTranslatef(0.0F, 0.0F, -0.005F);
		GL11.glScalef(1.5F, 1.5F, 0.0001F);
		String message = "Result:";
		mc.fontRenderer.drawString(message , -28, -25, 0);
		GL11.glScalef(0.8F, 0.8F, -1F);
		List<ItemIdentifierStack> list = new ArrayList<ItemIdentifierStack>();
		if(((BaseLogicCrafting)pipe.logic).getCraftedItem() != null) {
			list.add(ItemIdentifierStack.GetFromStack(((BaseLogicCrafting)pipe.logic).getCraftedItem()));
		}
		BasicGuiHelper.renderItemIdentifierStackListIntoGui(list, null, 0, 13, -37, 4, 12, 18, 18, mc, true, true);
	}

	@Override
	public boolean display() {
		return pipe.canRegisterSign() || pipe.hasOrder();
	}


	@Override
	public boolean cursorOnWindow(int x, int y) {
		return -50 < x && x < 50 && -50 < y && y < 50;
	}

	@Override
	public void handleCursor(int x, int y) {
		super.handleCursor(x, y);
	}
}