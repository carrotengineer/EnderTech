package io.endertech.multiblock.controller;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.endertech.multiblock.IMultiblockPart;
import io.endertech.multiblock.MultiblockControllerBase;
import io.endertech.multiblock.MultiblockTileEntityBase;
import io.endertech.multiblock.MultiblockValidationException;
import io.endertech.multiblock.block.BlockTankPart;
import io.endertech.multiblock.rectangular.RectangularMultiblockControllerBase;
import io.endertech.multiblock.tile.TileTankPart;
import io.endertech.network.message.MessageTileUpdate;
import io.endertech.util.LogHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import java.util.*;

public class ControllerTank extends RectangularMultiblockControllerBase
{
    protected boolean active;
    private Set<TileTankPart> attachedControllers;
    private int random_number = 0;

    public ControllerTank(World world)
    {
        super(world);
        active = false;
        attachedControllers = new HashSet<TileTankPart>();
    }

    public void setRandomNumber(int newRandomNumber)
    {
        LogHelper.info("Setting random number from " + this.random_number + " to " + newRandomNumber);
        this.random_number = newRandomNumber;
    }

    public int getRandomNumber()
    {
        return this.random_number;
    }

    @Override
    public void onAttachedPartWithMultiblockNBT(IMultiblockPart part, NBTTagCompound nbt)
    {
        this.readFromNBT(nbt);
    }

    @Override
    public void onAttachedPartWithMultiblockMessage(IMultiblockPart part, IMessage message)
    {
        LogHelper.info("My random number before: " + this.random_number);
        this.decodeMessage(message);
        LogHelper.info("My random number after:" + this.random_number);
    }

    @Override
    protected void onBlockAdded(IMultiblockPart newPart)
    {
        if (newPart instanceof TileTankPart)
        {
            TileTankPart tankPart = (TileTankPart) newPart;
            if (BlockTankPart.isController(tankPart.getBlockMetadata()))
            {
                attachedControllers.add(tankPart);
            }
        }
    }

    @Override
    protected void onBlockRemoved(IMultiblockPart oldPart)
    {
        if (oldPart instanceof TileTankPart)
        {
            TileTankPart tankPart = (TileTankPart) oldPart;
            if (BlockTankPart.isController(tankPart.getBlockMetadata()))
            {
                attachedControllers.remove(tankPart);
            }
        }
    }

    @Override
    protected void isMachineWhole() throws MultiblockValidationException
    {
        if (attachedControllers.size() != 1)
        {
            throw new MultiblockValidationException("You must have 1 controller in the tank structure (currently " + attachedControllers.size() + ")");
        }

        super.isMachineWhole();
    }

    @Override
    protected void onMachineAssembled()
    {
        if (this.getRandomNumber() == 0) setRandomNumber(new Random().nextInt(1000000));
        LogHelper.info("Tank assembled with R: " + this.random_number + "!");
    }

    @Override
    protected void onMachineRestored()
    {
        LogHelper.info("Tank restored");
    }

    @Override
    protected void onMachinePaused()
    {
        LogHelper.info("Tank paused");
    }

    @Override
    protected void onMachineDisassembled()
    {
        LogHelper.info("Tank disassembled");
    }

    @Override
    protected int getMinimumNumberOfBlocksForAssembledMachine()
    {
        return getMinimumXSize() * getMinimumZSize() * getMinimumYSize() - 7;
    }

    @Override
    protected int getMaximumXSize()
    {
        return 5;
    }

    @Override
    protected int getMinimumXSize() { return 3; }

    @Override
    protected int getMaximumZSize()
    {
        return 5;
    }

    @Override
    protected int getMinimumZSize() { return 3; }

    @Override
    protected int getMaximumYSize()
    {
        return 10;
    }

    @Override
    protected int getMinimumYSize() { return 3; }

    @Override
    protected void onAssimilate(MultiblockControllerBase assimilated)
    {
        if (assimilated instanceof ControllerTank)
        {
            ControllerTank controller = (ControllerTank) assimilated;
            if (controller.getRandomNumber() != 0)
            {
                LogHelper.info("Setting new random number from assimilated controller");
                setRandomNumber(((ControllerTank) assimilated).getRandomNumber());
            } else
            {
                LogHelper.info("Not setting random number as new assimilated tank had 0");
            }
        }
    }

    @Override
    protected void onAssimilated(MultiblockControllerBase assimilator)
    {
        this.attachedControllers.clear();
    }

    @Override
    protected boolean updateServer()
    {
        return false;
    }

    @Override
    protected void updateClient() { }

    @Override
    public void writeToNBT(NBTTagCompound data)
    {
        data.setBoolean("tankActive", this.isActive());
        data.setInteger("randomNumber", this.random_number);

        LogHelper.info("Writing tank to NBT: " + this.toString());
    }

    public String toString()
    {
        return "R: " + this.getRandomNumber();
    }

    @Override
    public void readFromNBT(NBTTagCompound data)
    {
        if (data.hasKey("tankActive"))
        {
            setActive(data.getBoolean("tankActive"));
        }

        if (data.hasKey("randomNumber"))
        {
            this.setRandomNumber(data.getInteger("randomNumber"));
            LogHelper.info("Read random number from NBT: " + this.random_number);
        }
    }

    public static class MessageTankUpdate extends MessageTileUpdate
    {
        public int random_number;

        public MessageTankUpdate() { }

        public MessageTankUpdate(TileTankPart tileSaveDelegate)
        {
            super(tileSaveDelegate);
            this.random_number = tileSaveDelegate.getTankController().random_number;
            LogHelper.info("Packed random number in to message: " + this.random_number);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            super.fromBytes(buf);
            this.random_number = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            super.toBytes(buf);
            buf.writeInt(this.random_number);
        }
    }

    @Override
    public IMessage encodeMessage(MultiblockTileEntityBase saveDelegate)
    {
        if (saveDelegate instanceof TileTankPart)
        {
            return new MessageTankUpdate((TileTankPart) saveDelegate);
        }

        return null;
    }

    @Override
    public void decodeMessage(IMessage message)
    {
        if (message instanceof MessageTankUpdate)
        {
            this.random_number = ((MessageTankUpdate) message).random_number;
            LogHelper.info("Reading tank from message: " + this.toString());
        }
    }

    @Override
    public String getName()
    {
        // TODO: localise
        return "Ender Tank";
    }

    public boolean isActive()
    {
        return this.active;
    }

    public void setActive(boolean active)
    {
        if (active == this.active) { return; }
        this.active = active;

        for (IMultiblockPart part : connectedParts)
        {
            if (this.active) { part.onMachineActivated(); } else { part.onMachineDeactivated(); }
        }
    }

    @Override
    protected void isBlockGoodForInterior(World world, int x, int y, int z) throws MultiblockValidationException
    {
        if (world.isAirBlock(x, y, z)) { return; } // Air is OK
        else throw new MultiblockValidationException("Interior must be air");
    }

    @Override
    public List<String> getWailaBody()
    {
        String activeStatus = "Active: ";
        if (this.isActive())
        {
            activeStatus += EnumChatFormatting.GREEN + "yes" + EnumChatFormatting.RESET;
        } else
        {
            activeStatus += EnumChatFormatting.RED + "no" + EnumChatFormatting.RESET;
        }

        List<String> additions = new ArrayList<String>();
        additions.add(activeStatus);
        return additions;
    }
}
