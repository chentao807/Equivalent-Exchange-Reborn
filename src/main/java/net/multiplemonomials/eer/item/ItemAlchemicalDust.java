package net.multiplemonomials.eer.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.multiplemonomials.eer.init.ModItems;
import net.multiplemonomials.eer.reference.Colors;
import net.multiplemonomials.eer.reference.Names;
import net.multiplemonomials.eer.reference.Reference;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemAlchemicalDust extends ItemEE
{
    public ItemAlchemicalDust()
    {
        super();
        this.setMaxStackSize(64);
        this.setHasSubtypes(true);
        this.setUnlocalizedName(Names.Items.ALCHEMICAL_DUST);
    }

    public static List<ItemStack> getAlchemicalDusts()
    {
        List<ItemStack> alchemicalDustStacks = new ArrayList<ItemStack>();

        for (int meta = 0; meta < Names.Items.ALCHEMICAL_DUST_SUBTYPES.length; meta++)
        {
            alchemicalDustStacks.add(new ItemStack(ModItems.alchemicalDust, 1, meta));
        }

        return alchemicalDustStacks;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack itemStack, int renderPass)
    {
        if (itemStack.getItemDamage() == 0)
        {
            return Integer.parseInt(Colors.DUST_ASH, 16);
        }
        else if (itemStack.getItemDamage() == 1)
        {
            return Integer.parseInt(Colors.DUST_VERDANT, 16);
        }
        else if (itemStack.getItemDamage() == 2)
        {
            return Integer.parseInt(Colors.DUST_AZURE, 16);
        }
        else if (itemStack.getItemDamage() == 3)
        {
            return Integer.parseInt(Colors.DUST_MINIUM, 16);
        }

        return Integer.parseInt(Colors.PURE_WHITE, 16);
    }

    @Override
    public String getUnlocalizedName()
    {
        return String.format("item.%s%s", Reference.RESOURCE_PREFIX, Names.Items.ALCHEMICAL_DUST);
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack)
    {
        return String.format("item.%s%s.%s", Reference.RESOURCE_PREFIX, Names.Items.ALCHEMICAL_DUST, Names.Items.ALCHEMICAL_DUST_SUBTYPES[MathHelper.clamp_int(itemStack.getItemDamage(), 0, Names.Items.ALCHEMICAL_DUST_SUBTYPES.length - 1)]);
    }
    
    

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs creativeTab, List list)
    {
        for (int meta = 0; meta < Names.Items.ALCHEMICAL_DUST_SUBTYPES.length; ++meta)
        {
            list.add(new ItemStack(this, 1, meta));
        }
    }
  
}
