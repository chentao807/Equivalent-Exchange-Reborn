package net.multiplemonomials.eer.block;

import net.multiplemonomials.eer.init.ModItems;
import net.multiplemonomials.eer.reference.GuiIds;
import net.multiplemonomials.eer.reference.Names;
import net.multiplemonomials.eer.reference.RenderIds;
import net.multiplemonomials.eer.tileentity.TileEntityAlchemicalChest;
import net.multiplemonomials.eer.tileentity.TileEntityAlchemicalChestLarge;
import net.multiplemonomials.eer.tileentity.TileEntityAlchemicalChestMedium;
import net.multiplemonomials.eer.tileentity.TileEntityAlchemicalChestSmall;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.multiplemonomials.eer.EquivalentExchangeReborn;

import java.util.List;

public class BlockAlchemicalChest extends BlockEE implements ITileEntityProvider
{
    public BlockAlchemicalChest()
    {
        super(Material.wood);
        this.setHardness(2.5f);
        this.setBlockName(Names.Blocks.ALCHEMICAL_CHEST);
        this.setBlockBounds(0.0625f, 0.0f, 0.0625f, 0.9375f, 0.875f, 0.9375f);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metaData)
    {
        if (metaData == 0)
        {
            return new TileEntityAlchemicalChestSmall();
        }
        else if (metaData == 1)
        {
            return new TileEntityAlchemicalChestMedium();
        }
        else if (metaData == 2)
        {
            return new TileEntityAlchemicalChestLarge();
        }

        return null;
    }

    @Override
    public int damageDropped(int metaData)
    {
        return metaData;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public int getRenderType()
    {
        return RenderIds.alchemicalChest;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9)
    {
        if (player.isSneaking() || world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN))
        {
            return false;
        }
        else if(player.getHeldItem() != null && player.getHeldItem().getItem() == ModItems.alchemicalUpgrade)
        {
        	if(world.getBlockMetadata(x, y, z) == player.getHeldItem().getItemDamage() - 1)
        	{
        		TileEntityAlchemicalChest alchemicalChest = (TileEntityAlchemicalChest)(world.getTileEntity(x, y, z));
        		alchemicalChest.upgradeToNextLevel();
        		
        		if(!player.capabilities.isCreativeMode)
        		{
        			player.getHeldItem().stackSize -= 1;
        		}
        		
        		world.markBlockForUpdate(x, y, z);
        		
        	}
        	
        	return true;
        }
        else
        {
            if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileEntityAlchemicalChest)
            {
                player.openGui(EquivalentExchangeReborn.instance, GuiIds.ALCHEMICAL_CHEST, world, x, y, z);
            }

            return true;
        }
    }

    @Override
    public boolean onBlockEventReceived(World world, int x, int y, int z, int eventId, int eventData)
    {
        super.onBlockEventReceived(world, x, y, z, eventId, eventData);
        TileEntity tileentity = world.getTileEntity(x, y, z);
        return tileentity != null ? tileentity.receiveClientEvent(eventId, eventData) : false;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs creativeTabs, List list)
    {
        for (int meta = 0; meta < 3; meta++)
        {
            list.add(new ItemStack(item, 1, meta));
        }
    }
}
