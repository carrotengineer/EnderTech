package io.endertech.multiblock.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.endertech.EnderTech;
import io.endertech.multiblock.IMultiblockPart;
import io.endertech.multiblock.MultiblockControllerBase;
import io.endertech.multiblock.controller.ControllerTank;
import io.endertech.multiblock.tile.TileTankPart;
import io.endertech.multiblock.tile.TileTankPartBase;
import io.endertech.multiblock.tile.TileTankValve;
import io.endertech.reference.Strings;
import io.endertech.util.*;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.util.ForgeDirection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockTankPart extends BlockContainer implements IOutlineDrawer
{
    public static final int FRAME_METADATA_BASE = 0; // Frame metadata
    public static final int FRAME_CORNER = 1;
    public static final int FRAME_CENTER = 2;
    public static final int FRAME_VERTICAL = 3;
    public static final int FRAME_EASTWEST = 4;
    public static final int FRAME_NORTHSOUTH = 5;

    public static final int VALVE_BASE = 6;
    public static final int VALVE_IDLE = 7;
    public static final int VALVE_ACTIVE = 8;

    private static final String TEXTURE_BASE = "endertech:enderTankPart";

    public static ItemStack itemBlockTankFrame;
    public static ItemStack itemBlockTankValve;

    private static String[] _subBlocks = new String[] {"frameDefault", "frameCorner", "frameCenter", "frameVertical", "frameEastWest", "frameNorthSouth", "valve"};

    private IIcon[] _icons = new IIcon[_subBlocks.length];

    public BlockTankPart()
    {
        super(Material.iron);
        setHardness(10.0f);
        setResistance(20.0f);
        this.setCreativeTab(EnderTech.tabET);
        this.setBlockName(Strings.Blocks.TANK_PART_NAME);
        this.setBlockTextureName(TEXTURE_BASE);
    }

    public void init()
    {
        TileTankPart.init();
        TileTankValve.init();

        itemBlockTankFrame = new ItemStack(this, 1, FRAME_METADATA_BASE);
        itemBlockTankValve = new ItemStack(this, 1, VALVE_BASE);
    }

    public static boolean isFrame(int metadata) { return metadata >= FRAME_METADATA_BASE && metadata <= FRAME_NORTHSOUTH; }

    public static boolean isValve(int metadata) { return metadata >= VALVE_BASE && metadata <= VALVE_ACTIVE; }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata)
    {
        if (metadata >= FRAME_METADATA_BASE && metadata <= FRAME_NORTHSOUTH) return new TileTankPart();
        if (metadata >= VALVE_BASE && metadata <= VALVE_ACTIVE) return new TileTankValve();

        throw new IllegalArgumentException("Unrecognized metadata");
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int faceHit, float par7, float par8, float par9)
    {
        if (player.isSneaking())
        {
            return false;
        }

        if (!world.isRemote)
        {
            ItemStack currentEquippedItem = player.getCurrentEquippedItem();

            if (currentEquippedItem == null)
            {
                TileEntity te = world.getTileEntity(x, y, z);
                if (te instanceof IMultiblockPart)
                {
                    MultiblockControllerBase controller = ((IMultiblockPart) te).getMultiblockController();
                    if (controller != null)
                    {
                        player.addChatComponentMessage(new ChatComponentText(controller.toString()));

                        Exception e = controller.getLastValidationException();
                        if (e != null)
                        {
                            player.addChatComponentMessage(new ChatComponentText("Last reason for not being able to assemble:"));
                            player.addChatComponentMessage(new ChatComponentText(e.getMessage()));
                        }
                    } else
                    {
                        player.addChatComponentMessage(new ChatComponentText("Block is not connected to a reactor. This could be due to lag, or a bug. If the problem persists, try breaking and re-placing the block."));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isOpaqueCube()
    {

        return true;
    }

    @Override
    public boolean renderAsNormalBlock()
    {

        return true;
    }

    public ItemStack getTankFrameItemStack()
    {
        return new ItemStack(this, 1, FRAME_METADATA_BASE);
    }

    public ItemStack getTankValveItemStack() { return new ItemStack(this, 1, VALVE_BASE); }

    @Override
    public void getSubBlocks(Item item, CreativeTabs par2CreativeTabs, List par3List)
    {
        par3List.add(getTankFrameItemStack());
        par3List.add(getTankValveItemStack());
    }

    @Override
    public boolean drawOutline(DrawBlockHighlightEvent event)
    {
        BlockCoord target = new BlockCoord(event.target.blockX, event.target.blockY, event.target.blockZ);
        World world = event.player.worldObj;

        TileEntity tile = world.getTileEntity(target.x, target.y, target.z);
        if (tile == null)
        {
            RenderHelper.renderBlockOutline(event.context, event.player, target, RGBA.Blue.setAlpha(0.6f), 2.0f, event.partialTicks);
            return true;
        }

        if (tile instanceof TileTankPartBase)
        {
            return ((TileTankPartBase) tile).drawOutline(event);
        }

        return false;
    }

    @Override
    public int damageDropped(int meta)
    {
        if (isFrame(meta)) return FRAME_METADATA_BASE;
        else if (isValve(meta)) return VALVE_BASE;

        return FRAME_METADATA_BASE;
    }

    public boolean canPlaceBlockAt(World world, int x, int y, int z)
    {
        Set<Integer> randomNumbers = new HashSet<Integer>();
        for (ForgeDirection neighbour : ForgeDirection.VALID_DIRECTIONS)
        {
            TileEntity tile = world.getTileEntity(x + neighbour.offsetX, y + neighbour.offsetY, z + neighbour.offsetZ);
            if (tile != null && tile instanceof TileTankPart)
            {
                ControllerTank controller = ((TileTankPart) tile).getTankController();
                if (controller != null)
                {
                    int randomNumber = controller.getRandomNumber();
                    if (randomNumber > 0)
                    {
                        if (!randomNumbers.contains(randomNumber))
                        {
                            randomNumbers.add(randomNumber);
                        }

                        if (randomNumbers.size() > 1)
                        {
                            LogHelper.info("Someone tried to connect two previously formed tanks - stopping them.");
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public IIcon getIcon(int side, int metadata)
    {
        // Casing block
        switch (metadata)
        {
            case FRAME_METADATA_BASE:
            case FRAME_CORNER:
            case FRAME_CENTER:
                return _icons[metadata];

            case FRAME_VERTICAL:
                // Vertical block
                if (side == 0 || side == 1)
                {
                    return _icons[FRAME_METADATA_BASE];
                } else
                {
                    return _icons[metadata];
                }
            case FRAME_EASTWEST:
                // X-aligned block (e/w)
                if (side == 4 || side == 5)
                {
                    return _icons[FRAME_METADATA_BASE];
                } else
                {
                    return _icons[metadata];
                }
            case FRAME_NORTHSOUTH:
                // Z-aligned block (n/s)
                if (side == 2 || side == 3)
                {
                    return _icons[FRAME_METADATA_BASE];
                } else if (side == 4 || side == 5)
                {
                    // I hate everything
                    return _icons[FRAME_EASTWEST];
                } else
                {
                    return _icons[metadata];
                }
            case VALVE_BASE:
                return _icons[VALVE_BASE];

            default:
                if (side == 0 || side == 1)
                {
                    return _icons[FRAME_METADATA_BASE];
                } else
                {
                    metadata = Math.max(0, Math.min(5, metadata));
                    return _icons[metadata];
                }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister)
    {
        this.blockIcon = iconRegister.registerIcon(TEXTURE_BASE);

        for (int i = 0; i < _subBlocks.length; ++i)
        {
            _icons[i] = iconRegister.registerIcon(TEXTURE_BASE + "." + _subBlocks[i]);
        }
    }
}
