package logisticspipes.transport;

import java.util.BitSet;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.pipe.PipeLiquidUpdate;
import logisticspipes.pipes.basic.liquid.LogisticsLiquidSection;
import logisticspipes.proxy.MainProxy;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import logistics_bc.lp_BuildCraftCore;
import buildcraft.api.core.SafeTimeTracker;
import logistics_bc.core.utils.Utils;

public class PipeLiquidTransportLogistics extends PipeTransportLogistics implements IFluidHandler {

	public LogisticsLiquidSection[] sideTanks = new LogisticsLiquidSection[ForgeDirection.VALID_DIRECTIONS.length];
	public LogisticsLiquidSection internalTank = new LogisticsLiquidSection(getInnerCapacity());
	
	public FluidStack[] renderCache = new FluidStack[7];
	
	public PipeLiquidTransportLogistics() {
		for(ForgeDirection dir:ForgeDirection.VALID_DIRECTIONS) {
			sideTanks[dir.ordinal()] = new LogisticsLiquidSection(getSideCapacity());
		}
	}
	
	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		if(from.ordinal() < ForgeDirection.VALID_DIRECTIONS.length) {
			return sideTanks[from.ordinal()].fill(resource, doFill);
		} else {
			return 0;//internalTank.fill(resource, doFill);
		}
	}

	@Override
	public int fill(int tankIndex, FluidStack resource, boolean doFill) {
		return fill(ForgeDirection.getOrientation(tankIndex), resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		if(from.ordinal() < ForgeDirection.VALID_DIRECTIONS.length) {
			return sideTanks[from.ordinal()].drain(maxDrain, doDrain);
		} else {
			return null;//internalTank.drain(maxDrain, doDrain);
		}
	}

	@Override
	public FluidStack drain(int tankIndex, int maxDrain, boolean doDrain) {
		return drain(ForgeDirection.getOrientation(tankIndex), maxDrain, doDrain);
	}

	@Override
	public IFluidTank[] getTanks(ForgeDirection direction) {
		if(direction.ordinal() < ForgeDirection.VALID_DIRECTIONS.length) {
			return new IFluidTank[]{sideTanks[direction.ordinal()]};
		} else {
			return null;//new IFluidTank[]{internalTank};
		}
	}


	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);

		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			if (nbttagcompound.hasKey("tank[" + direction.ordinal() + "]")) {
				sideTanks[direction.ordinal()].readFromNBT(nbttagcompound.getCompoundTag("tank[" + direction.ordinal() + "]"));
			}
		}
		if (nbttagcompound.hasKey("tank[middle]")) {
			internalTank.readFromNBT(nbttagcompound.getCompoundTag("tank[middle]"));
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);

		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			NBTTagCompound subTag = new NBTTagCompound();
			sideTanks[direction.ordinal()].writeToNBT(subTag);
			nbttagcompound.setTag("tank[" + direction.ordinal() + "]", subTag);
		}
		NBTTagCompound subTag = new NBTTagCompound();
		internalTank.writeToNBT(subTag);
		nbttagcompound.setTag("tank[middle]", subTag);
	}
	
	@Override
	public IFluidTank getTank(ForgeDirection direction, FluidStack type) {
		return null;
	}
	
	/*
	@Override
	public boolean allowsConnect(PipeTransport with) {
		return super.allowsConnect(with) || with instanceof LogisitcsLiquidConnectionTransport;
	}
	*/

	public int getInnerCapacity() {
		return 10000;
	}

	public int getSideCapacity() {
		return 5000;
	}

	@Override
	public void onNeighborBlockChange(int blockId) {
		super.onNeighborBlockChange(blockId);

		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			if (!Utils.checkPipesConnections(container.getTile(orientations[direction.ordinal()]), container)) {
				if(MainProxy.isServer(container.worldObj)) {
					FluidStack stack = sideTanks[direction.ordinal()].getFluid();
					if(stack != null) {
						sideTanks[direction.ordinal()].setLiquid(null);
						internalTank.fill(stack, true);
					}
				}
				if(renderCache[direction.ordinal()] != null) {
					renderCache[direction.ordinal()].amount = 1;
				}
			}
		}
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		updateLiquid();
	}
	
	/*
	 * BuildCraft Liquid Sync Code
	 */
	private final SafeTimeTracker tracker = new SafeTimeTracker();
	private long clientSyncCounter = lp_BuildCraftCore.longUpdateFactor - 10;
	public byte initClient = 0;
	
	private static final ForgeDirection[] orientations = ForgeDirection.values();

	private void updateLiquid() {
		if(MainProxy.isClient(container.worldObj)) return;
		if (tracker.markTimeIfDelay(container.worldObj, lp_BuildCraftCore.updateFactor)) {

			boolean init = false;
			if (++clientSyncCounter > lp_BuildCraftCore.longUpdateFactor) {
				clientSyncCounter = 0;
				init = true;
			}
			if(clientSyncCounter < 0) clientSyncCounter = 0;
			ModernPacket packet = computeLiquidUpdate(init, true);
			if (packet != null) {
				MainProxy.sendPacketToAllWatchingChunk(container.xCoord, container.zCoord, MainProxy.getDimensionForWorld(container.worldObj), packet);
			}
		}
	}

	/**
	 * Computes the PacketLiquidUpdate packet for transmission to a client
	 * 
	 * @param initPacket
	 *            everything is sent, no delta stuff ( first packet )
	 * @param persistChange
	 *            The render cache change is persisted
	 * @return PacketLiquidUpdate liquid update packet
	 */
	private ModernPacket computeLiquidUpdate(boolean initPacket, boolean persistChange) {

		boolean changed = false;
		BitSet delta = new BitSet(21);

		if (initClient > 0) {
			initClient--;
			if (initClient == 1) {
				changed = true;
				delta.set(0, 21);
			}
		}

		FluidStack[] renderCache = this.renderCache.clone();

		for (ForgeDirection dir : orientations) {
			FluidStack current;
			if(dir != ForgeDirection.UNKNOWN) {
				current = sideTanks[dir.ordinal()].getFluid();
			} else {
				current = internalTank.getFluid();
			}
			FluidStack prev = renderCache[dir.ordinal()];

			if (prev == null && current == null) {
				continue;
			}

			if (prev == null && current != null) {
				changed = true;
				renderCache[dir.ordinal()] = current.copy();
				delta.set(dir.ordinal() * 3 + 0);
				delta.set(dir.ordinal() * 3 + 1);
				delta.set(dir.ordinal() * 3 + 2);
				continue;
			}

			if (prev != null && current == null) {
				changed = true;
				renderCache[dir.ordinal()] = null;
				delta.set(dir.ordinal() * 3 + 0);
				delta.set(dir.ordinal() * 3 + 1);
				delta.set(dir.ordinal() * 3 + 2);
				continue;
			}

			if (prev.fluidID != current.fluidID || initPacket) {
				changed = true;
				renderCache[dir.ordinal()]=new FluidStack(current.fluidID,renderCache[dir.ordinal()].amount);
				//TODO check: @GUIpsp Possibly instanciating multiple times, might be slow
				delta.set(dir.ordinal() * 3 + 0);
			}

			if (prev.itemMeta != current.itemMeta || initPacket) {
				changed = true;
				renderCache[dir.ordinal()]=new FluidStack(current.fluidID,renderCache[dir.ordinal()].amount,current.itemMeta);
				delta.set(dir.ordinal() * 3 + 1);
			}

			int displayQty = (prev.amount * 4 + current.amount) / 5;
			if (displayQty == 0 && current.amount > 0 || initPacket) {
				displayQty = current.amount;
			}
			if(dir != ForgeDirection.UNKNOWN) {
				displayQty = Math.min(getSideCapacity(), displayQty);
			} else {
				displayQty = Math.min(getInnerCapacity(), displayQty);
			}

			if (prev.amount != displayQty || initPacket) {
				changed = true;
				renderCache[dir.ordinal()].amount = displayQty;
				delta.set(dir.ordinal() * 3 + 2);
			}
		}

		if (persistChange) {
			this.renderCache = renderCache;
		}

		if (changed || initPacket) {
			return PacketHandler.getPacket(PipeLiquidUpdate.class).setRenderCache(renderCache).setDelta(delta).setPosX(container.xCoord).setPosY(container.yCoord).setPosZ(container.zCoord).setChunkDataPacket(initPacket);
		}

		return null;

	}

	@Override
	protected boolean isItemExitable(ItemStack stack) {
		return true;
	}
	
}
