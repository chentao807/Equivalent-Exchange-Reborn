package net.multiplemonomials.eer.tileentity;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.Packet;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;
import net.multiplemonomials.eer.configuration.CommonConfiguration;
import net.multiplemonomials.eer.exchange.EnergyRegistry;
import net.multiplemonomials.eer.exchange.EnergyValue;
import net.multiplemonomials.eer.init.ModBlocks;
import net.multiplemonomials.eer.init.ModItems;
import net.multiplemonomials.eer.item.ItemAlchemicalFuel;
import net.multiplemonomials.eer.network.PacketHandler;
import net.multiplemonomials.eer.network.message.MessageTileCalcinator;
import net.multiplemonomials.eer.reference.Names;
import net.multiplemonomials.eer.util.EMCHelper;
import net.multiplemonomials.eer.util.ItemHelper;

import org.apache.commons.lang3.tuple.Pair;

public class TileEntityCalcinator extends TileEntityEE implements ISidedInventory
{
    public static final int INVENTORY_SIZE = 4;
    public static final int FUEL_INVENTORY_INDEX = 0;
    public static final int INPUT_INVENTORY_INDEX = 1;
    public static final int OUTPUT_LEFT_INVENTORY_INDEX = 2;
    public static final int OUTPUT_RIGHT_INVENTORY_INDEX = 3;
    
    public int burnTimeLeftInTicks;              // How much longer the fuel in Calcinator will cook
    public int totalBurnTimeInTicks;                // The fuel value for the currently burning fuel
    public int itemTimeLeftInTicks;				// How much longer the current item has to cook for
	public int itemTimeTotalInTicks;			//The total time that the current item must cook for
    
    //bitmasks for calculating output
	final private int bits0To5Bitmask = 0x3F; //0b111111
	final private int bits10To6Bitmask = 0x7C0; //0b11111000000
	final private int bits11To12Bitmask = 0x1800; //0b1100000000000
	final private int bits13To31Bitmask = 0x7FFFE000;//0b1111111111111111110000000000000
	
    public int itemSuckCoolDown = 0;
    
    /**
     * The items that will be outputted when the current stuff finshes burning
     */
    private Pair<ItemStack, ItemStack> itemsToOutput;
    
    /*
     * Modifiers for ItemsToOutput
     */
    
    /**
     * Get a NBT tag containing the items that will be output by this cycle
     * @return
     */
    public NBTTagCompound getItemsToOutputNBTTag()
    {
    	
    	NBTTagCompound pairTag = new NBTTagCompound();
    	
    	if(itemsToOutput != null)
    	{
    		if(itemsToOutput.getLeft() != null)
    		{
    			NBTTagCompound leftItemTag = new NBTTagCompound();
    	    	itemsToOutput.getLeft().writeToNBT(leftItemTag);
    	    	pairTag.setTag("leftItem", leftItemTag);
    		}
    		
    		if(itemsToOutput.getRight() != null)
    		{
    	    	NBTTagCompound rightItemTag = new NBTTagCompound();
    	    	itemsToOutput.getRight().writeToNBT(rightItemTag); 
    	    	pairTag.setTag("rightItem", rightItemTag);

    		}
    	}    	
    	return pairTag;

    }
    
    /**
     * Load the items to output from a NBT tag made by the above method
     *
     */
    public void setItemsToOutputFromNBT(NBTTagCompound pairTag)
    {
    	itemsToOutput = Pair.of(ItemStack.loadItemStackFromNBT(pairTag.getCompoundTag("leftItem")),
    			ItemStack.loadItemStackFromNBT(pairTag.getCompoundTag("rightItem")));
    }

    public TileEntityCalcinator()
    {
        super(INVENTORY_SIZE);
    }
    
    @Override
    public String getInventoryName()
    {
        return this.hasCustomName() ? this.getCustomName() : Names.Containers.CALCINATOR_NAME;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side)
    {
        return side == ForgeDirection.DOWN.ordinal() ? new int[]{FUEL_INVENTORY_INDEX, OUTPUT_LEFT_INVENTORY_INDEX, OUTPUT_RIGHT_INVENTORY_INDEX} : new int[]{INPUT_INVENTORY_INDEX, OUTPUT_LEFT_INVENTORY_INDEX, OUTPUT_RIGHT_INVENTORY_INDEX};
    }

    @Override
    public boolean canInsertItem(int slotIndex, ItemStack itemStack, int side)
    {
        return isItemValidForSlot(slotIndex, itemStack);
    }

    @Override
    public boolean canExtractItem(int slotIndex, ItemStack itemStack, int side)
    {
        return slotIndex == OUTPUT_LEFT_INVENTORY_INDEX || slotIndex == OUTPUT_RIGHT_INVENTORY_INDEX;
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex)
    {
        // FIXME sendDustPileData();
        return inventory[slotIndex];
    }
    
    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound)
    {
        super.writeToNBT(nbtTagCompound);
        
        // Write the ItemStacks in the inventory to NBT
        NBTTagList tagList = new NBTTagList();
        for (int currentIndex = 0; currentIndex < inventory.length; ++currentIndex)
        {
            if (inventory[currentIndex] != null)
            {
                NBTTagCompound tagCompound = new NBTTagCompound();
                tagCompound.setByte("Slot", (byte) currentIndex);
                inventory[currentIndex].writeToNBT(tagCompound);
                tagList.appendTag(tagCompound);
            }
        }
        
        nbtTagCompound.setInteger("burnTimeLeftInTicks", burnTimeLeftInTicks);
        nbtTagCompound.setInteger("totalBurnTimeInTicks", totalBurnTimeInTicks);
        nbtTagCompound.setInteger("itemTimeLeftInTicks", itemTimeLeftInTicks);
        nbtTagCompound.setInteger("itemTimeTotalInTicks", itemTimeTotalInTicks);
        
        nbtTagCompound.setTag("itemsToOutput", getItemsToOutputNBTTag());
        
        nbtTagCompound.setTag("Items", tagList);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound)
    {
        super.readFromNBT(nbtTagCompound);

        // Read in the ItemStacks in the inventory from NBT
        NBTTagList tagList = nbtTagCompound.getTagList("Items", 10);
        inventory = new ItemStack[this.getSizeInventory()];
        for (int i = 0; i < tagList.tagCount(); ++i)
        {
            NBTTagCompound tagCompound = tagList.getCompoundTagAt(i);
            byte slotIndex = tagCompound.getByte("Slot");
            if (slotIndex >= 0 && slotIndex < inventory.length)
            {
                inventory[slotIndex] = ItemStack.loadItemStackFromNBT(tagCompound);
            }
        }
        
        burnTimeLeftInTicks = nbtTagCompound.getInteger("burnTimeLeftInTicks");
        totalBurnTimeInTicks = nbtTagCompound.getInteger("totalBurnTimeInTicks");
        itemTimeLeftInTicks = nbtTagCompound.getInteger("itemTimeLeftInTicks");
        itemTimeTotalInTicks = nbtTagCompound.getInteger("itemTimeTotalInTicks");
        
        setItemsToOutputFromNBT(nbtTagCompound.getCompoundTag("itemsToOutput"));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemStack)
    {
    	//accept only alchemical fuels in the fuel slot
    	if(slot == FUEL_INVENTORY_INDEX && itemStack.getItem() instanceof ItemAlchemicalFuel)
    	{
    		return true;
    	}
        return false;
    }

    @Override
    public boolean receiveClientEvent(int eventId, int eventData)
    {
        if (eventId == 1)
        {
            this.state = (byte) eventData;
            // NAME UPDATE
            // this.worldObj.updateAllLightTypes(this.xCoord, this.yCoord, this.zCoord);
            this.worldObj.func_147451_t(this.xCoord, this.yCoord, this.zCoord);
            return true;
        }
        else if (eventId == 2)
        {
            inventory[OUTPUT_LEFT_INVENTORY_INDEX].stackSize = (byte) eventData;
            return true;
        }
        else if (eventId == 3)
        {
        	inventory[OUTPUT_LEFT_INVENTORY_INDEX].setItemDamage(eventData);
            return true;
        }
        else if (eventId == 4)
        {
        	inventory[OUTPUT_RIGHT_INVENTORY_INDEX].stackSize = (byte) eventData;
            return true;
        }
        else if (eventId == 5)
        {
        	inventory[OUTPUT_RIGHT_INVENTORY_INDEX].setItemDamage(eventData);
        	return true;
        }
        else
        {
            return super.receiveClientEvent(eventId, eventData);
        }
    }

    @Override
    public Packet getDescriptionPacket()
    {
        return PacketHandler.INSTANCE.getPacketFrom(new MessageTileCalcinator(this));
    }
    
    /**
     * 
     * @return true if there is fuel in the fuel slot and an item with an emc value in the output slot
     */
    private boolean canBurn()
    {
    	if(inventory[INPUT_INVENTORY_INDEX] == null && itemsToOutput == null)
    	{
    		return false;
    	}
    	
    	return (burnTimeLeftInTicks > 0 || EMCHelper.isConsideredFuel(inventory[FUEL_INVENTORY_INDEX])) && (EnergyRegistry.getInstance().hasEnergyValue(inventory[INPUT_INVENTORY_INDEX]) || itemsToOutput != null);
    	
    }
    
    /**
     * @param itemStack
     * @return how many ticks the supplied item can burn for (can be 0)
     */
    private int getBurnTimeForItemStack(ItemStack itemStack)
    {
    	if(EnergyRegistry.getInstance().hasEnergyValue(itemStack) && EMCHelper.isConsideredFuel(itemStack))
    	{
	    	EnergyValue stackEmc = EnergyRegistry.getInstance().getEnergyValue(inventory[FUEL_INVENTORY_INDEX]);
			float stackEmcValue = stackEmc.getValue();
			return MathHelper.ceiling_double_int((stackEmcValue) * CommonConfiguration.FURNACE_TICKS_PER_FUEL_EMC);
    	}
    	
    	return 0;
	}
    
    /**
     * @param itemStack
     * @return how many ticks the supplied item must be cooked for (can be 0)
     */
    private int getItemCookTimeForItemStack(ItemStack itemStack)
    {
    	if(EnergyRegistry.getInstance().hasEnergyValue(itemStack))
    	{
	    	EnergyValue stackEmc = EnergyRegistry.getInstance().getEnergyValue(inventory[INPUT_INVENTORY_INDEX]);
			float stackEmcValue = stackEmc.getValue();
			return MathHelper.ceiling_double_int((stackEmcValue) * CommonConfiguration.FURNACE_TICKS_PER_ITEM_EMC);
    	}
    	
    	return 0;
	}

	/**
     * Takes one item and starts producing its outputs
     * 
     * @return true if a new cycle was started, false if the output slots couldn't be filled with the items that would be produced
     */
    private boolean startBurningAnew()
    {
    	//input has no EMC value = no go
    	if(!(EnergyRegistry.getInstance().hasEnergyValue(inventory[INPUT_INVENTORY_INDEX])))
    	{
    		return false;
    	}
    	
    	EnergyValue stackEmc = EnergyRegistry.getInstance().getEnergyValue(inventory[INPUT_INVENTORY_INDEX]);
		float stackEmcValue = stackEmc.getValue();
		ItemStack[] outputs = calculateOutputItemsForEmc(MathHelper.floor_float(stackEmcValue));
		
		
		//items won't fit in output slots = no go
		if(inventory[OUTPUT_LEFT_INVENTORY_INDEX] != null)
		{
			if(inventory[OUTPUT_LEFT_INVENTORY_INDEX].getItemDamage() != outputs[0].getItemDamage() || inventory[OUTPUT_LEFT_INVENTORY_INDEX].getItem() != outputs[0].getItem())
			{
				return false;
			}
			else if(outputs[0].stackSize + inventory[OUTPUT_LEFT_INVENTORY_INDEX].stackSize > ItemHelper.maxStackSize(outputs[0]))
			{
				//spill over into the other item stack
				if(outputs[1] == null || (outputs[1].stackSize < ItemHelper.maxStackSize(outputs[0]) && ItemHelper.similar(outputs[1], outputs[0])))
				{
					if(outputs[1] == null)
					{
						outputs[1] = new ItemStack(outputs[0].getItem(), 0, outputs[0].getItemDamage());
					}
					outputs[1].stackSize = outputs[1].stackSize + (outputs[0].stackSize - ItemHelper.maxStackSize(outputs[0]));
					outputs[0].stackSize = ItemHelper.maxStackSize(outputs[0]);
					if(outputs[1].stackSize > ItemHelper.maxStackSize(outputs[1]))
					{
						return false;
					}
				}
				
				return false;
			}
			
		}
				
		if(inventory[OUTPUT_RIGHT_INVENTORY_INDEX] != null && outputs[1] != null)
		{
			if(inventory[OUTPUT_RIGHT_INVENTORY_INDEX].getItemDamage() != outputs[1].getItemDamage() || inventory[OUTPUT_RIGHT_INVENTORY_INDEX].getItem() != outputs[1].getItem())
			{
				return false;
			}
			else if(outputs[1].stackSize + inventory[OUTPUT_RIGHT_INVENTORY_INDEX].stackSize > ItemHelper.maxStackSize(outputs[1]))
			{
				return false;
			}
			
		}		
		
		//TODO: Increase the number of output slots and output all four produced dusts instead of dropping the lowest two
		itemsToOutput = Pair.<ItemStack, ItemStack>of(outputs[0], outputs[1]);
		
		itemTimeLeftInTicks = itemTimeTotalInTicks = getItemCookTimeForItemStack(inventory[INPUT_INVENTORY_INDEX]);
		
		//take one item
		if(inventory[INPUT_INVENTORY_INDEX].stackSize == 1)
		{
			inventory[INPUT_INVENTORY_INDEX] = null;
		}
		else
		{
			--inventory[INPUT_INVENTORY_INDEX].stackSize;
		}
		
		return true;
    }
    
    /**
     * 
     * @param emcValue the emc value of the item
     * @return an array of dusts which have the same EMC as the provided value
     */
    private ItemStack[] calculateOutputItemsForEmc(int emcValue)
    {
    	//Bitwise algorithm contributed by Randall Smith
    	int miniumDustQuantity = (emcValue & bits13To31Bitmask) / 8192;
    	int azureDustQuantity = (emcValue & bits11To12Bitmask) / 2048;
    	int verdantDustQuantity = (emcValue & bits10To6Bitmask) / 64;
    	int ashQuantity = emcValue & bits0To5Bitmask;
    	
    	ItemStack[] outputItems = new ItemStack[4];
    	
    	int counter = 0;
    	
    	if(miniumDustQuantity > 0)
    	{
    		outputItems[counter++] = new ItemStack(ModItems.alchemicalDust, miniumDustQuantity, 3);
    	}
    	
       	if(azureDustQuantity > 0)
    	{
    		outputItems[counter++] = new ItemStack(ModItems.alchemicalDust, azureDustQuantity, 2);
    	}
       	
       	if(verdantDustQuantity > 0)
    	{
    		outputItems[counter++] = new ItemStack(ModItems.alchemicalDust, verdantDustQuantity, 1);
    	}
       	
       	if(ashQuantity > 0)
    	{
    		outputItems[counter++] = new ItemStack(ModItems.alchemicalDust, ashQuantity, 0);
    	}
    	
    	return outputItems;
    }
    
    /**
     * Called to output the items to the inventory output slots
     */
    private void outputBurnedItems()
    {
    	if(itemsToOutput == null)
    	{
    		return;
    	}
    	
    	if(itemsToOutput.getLeft() != null)
    	{
	    	if(inventory[OUTPUT_LEFT_INVENTORY_INDEX] == null || !(inventory[OUTPUT_LEFT_INVENTORY_INDEX].getItem() == itemsToOutput.getLeft().getItem() && inventory[OUTPUT_LEFT_INVENTORY_INDEX].getItemDamage() == itemsToOutput.getLeft().getItemDamage()))
	    	{
	    		setInventorySlotContents(OUTPUT_LEFT_INVENTORY_INDEX, itemsToOutput.getLeft());
	    	}
	    	else
	    	{
	    		inventory[OUTPUT_LEFT_INVENTORY_INDEX].stackSize += itemsToOutput.getLeft().stackSize;
	    	}
	    	
	    	worldObj.addBlockEvent(xCoord, yCoord, zCoord, ModBlocks.calcinator, 2, itemsToOutput.getLeft().stackSize);
	    	
	    	worldObj.addBlockEvent(xCoord, yCoord, zCoord, ModBlocks.calcinator, 3, itemsToOutput.getLeft().getItemDamage());

	    	
	    	
    	}
    	
    	if(itemsToOutput.getRight() != null)
    	{
	    	if(inventory[OUTPUT_RIGHT_INVENTORY_INDEX] == null || (inventory[OUTPUT_RIGHT_INVENTORY_INDEX].getItem() == itemsToOutput.getRight().getItem() && inventory[OUTPUT_RIGHT_INVENTORY_INDEX].getItemDamage() == itemsToOutput.getRight().getItemDamage()))
	    	{
	    		setInventorySlotContents(OUTPUT_RIGHT_INVENTORY_INDEX, itemsToOutput.getRight());
	    	}
	    	else
	    	{
	    		inventory[OUTPUT_RIGHT_INVENTORY_INDEX].stackSize += itemsToOutput.getRight().stackSize;
	    	}
	    	
	    	worldObj.addBlockEvent(xCoord, yCoord, zCoord, ModBlocks.calcinator, 4, itemsToOutput.getRight().stackSize);
	    	
	    	worldObj.addBlockEvent(xCoord, yCoord, zCoord, ModBlocks.calcinator, 5, itemsToOutput.getRight().getItemDamage());

    	}
    	
    	itemsToOutput = null;
    }
    
    //4 things that could happen here:
    //not burning -> burning
    //burning -> burning
    //burning -> not burning
    //not burning -> not burning
    @Override
    public void updateEntity()
    {
    	//is burning
    	if(state == 1)
    	{
    		if(!canBurn())
    		{
    			changeState(0);
    			return;
    		}
    		
    		if(--itemTimeLeftInTicks <= 0)
    		{
    			outputBurnedItems();
    			//no boolean -> byte conversion, grumble grumble
    			changeState(startBurningAnew() ? 1 : 0);
    		}
    		
    		if(--burnTimeLeftInTicks <= 0)
    		{
    			//this unit of fuel has run out.
    			changeState(takeFuel() ? 1 : 0);
    		}
    		
    	}
    	else if(canBurn())
    	{
    		if(itemsToOutput != null)
    		{
    			changeState(1);
    		}
    		else if(startBurningAnew())
    		{
    			changeState(1);
    		}
    	}
    	
    	return;
    	
    	
    }
    
    /**
     * Sets state, and updates the lighting if the state has changed
     */
    void changeState(int newState)
    {
    	if(newState != state)
    	{
    		worldObj.addBlockEvent(xCoord, yCoord, zCoord, ModBlocks.calcinator, 1, newState);
    		state = (byte)newState;
    	}
    }

    /**
     * Removes one fuel from the fuel slot and sets burnTimeLeftInTicks and totalBurnTimeInTicks accordingly
     * 
     * @return whether fuel was taken
     */
	private boolean takeFuel() 
	{
		
		if(inventory[FUEL_INVENTORY_INDEX] == null)
		{
			return false;
		}
		
		int fuelBurnTime = getBurnTimeForItemStack(inventory[FUEL_INVENTORY_INDEX]);
		if(fuelBurnTime <= 0)
		{
			return false;
		}
		
		//cleared to take one
		
		totalBurnTimeInTicks = burnTimeLeftInTicks = fuelBurnTime;
		
		if(inventory[FUEL_INVENTORY_INDEX].stackSize == 1)
		{
			inventory[FUEL_INVENTORY_INDEX] = null;
		}
		else
		{
			--inventory[FUEL_INVENTORY_INDEX].stackSize;
		}
		
		return true;
	}
	
	public int getCookTimeRemainingScaled(int par1)
	{
	         return MathHelper.ceiling_float_int(((itemTimeTotalInTicks - itemTimeLeftInTicks) * par1)  / (float)itemTimeTotalInTicks);
	}
	
	public int getBurnTimeRemainingScaled(int par1)
	{
	         return MathHelper.ceiling_float_int((burnTimeLeftInTicks / (float)totalBurnTimeInTicks) * par1);
	}

}
