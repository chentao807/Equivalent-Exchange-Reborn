package net.multiplemonomials.eer.handler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.multiplemonomials.eer.exchange.EnergyValue;
import net.multiplemonomials.eer.exchange.WrappedStack;
import net.multiplemonomials.eer.reference.Reference;
import net.multiplemonomials.eer.util.LogHelper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

public class ValueFilesHandler
{
    public static final String CATEGORY_EMC_VALUES = "emcvalues";
    private static File valueFileFolder;
    
    //stores EMC values loaded from the server
    private static Map<WrappedStack, EnergyValue> serverValueMap;

    private static File getValueFileFolder()
    {
        if (valueFileFolder == null)
        {
            valueFileFolder = new File(Reference.BASE_CONFIGURATION_FILE_PATH + "emc");
            if (!valueFileFolder.exists())
            {
                valueFileFolder.mkdirs();
            }
        }
        return valueFileFolder;
    }

    private static File getValueFile(String modid)
    {
        return new File(getValueFileFolder().getPath() + File.separator + modid + ".emc");
    }

    public static void addFileValue(String modid, ItemStack itemStack, EnergyValue emcValue)
    {
        File emcFile = getValueFile(modid);
        Configuration emcConfiguration = new Configuration(emcFile);

        try
        {
            emcConfiguration.load();

            UniqueIdentifier itemID = GameRegistry.findUniqueIdentifierFor(itemStack.getItem());
            
            emcConfiguration.get(CATEGORY_EMC_VALUES, itemID.modId + "|" + itemID.name + "|" + itemStack.getItemDamage(), emcValue.getValue()).set(emcValue.getValue());
        }
        catch (Exception e)
        {
            LogHelper.error(String.format("Unable to save EMC Value(%s) for (%s)!", emcValue.getValue(), itemStack.getDisplayName()));
        }
        finally
        {
            emcConfiguration.save();
        }
    }

    public static EnergyValue getFileValue(String modid, ItemStack itemStack)
    {
        File emcFile = getValueFile(modid);
        Configuration emcConfiguration = new Configuration(emcFile);

        EnergyValue emcValue = null;

        try
        {
            emcConfiguration.load();
            
            UniqueIdentifier itemID = GameRegistry.findUniqueIdentifierFor(itemStack.getItem());

            Float value = (float)emcConfiguration.get(CATEGORY_EMC_VALUES, itemID.modId + "|" + itemID.name + "|" + itemStack.getItemDamage(), 0.0F).getDouble(0.0F);
            emcValue = new EnergyValue(value);
        }
        catch (Exception e)
        {
            LogHelper.error(String.format("Unable to load EMC Value for (%s)!",  itemStack.getDisplayName()));
        }
        finally
        {
            emcConfiguration.save();
        }
        return emcValue;
    }

    public static Map<WrappedStack, EnergyValue> getFileValues(File emcFile)
    {
        Map<WrappedStack, EnergyValue> valueMap = new HashMap<WrappedStack, EnergyValue>();

        try
        {
            Configuration emcConfiguration = new Configuration(emcFile);

            ConfigCategory category = emcConfiguration.getCategory(CATEGORY_EMC_VALUES);

            for(Map.Entry<String, Property> entry : category.entrySet())
            {
                WrappedStack wrappedStack = null;
                String[] stackStrings = entry.getKey().split("\\|");
                if (stackStrings.length >= 3)
                {
                    Item item = GameRegistry.findItem(stackStrings[0], stackStrings[1]);
                    int metadata = Integer.parseInt(stackStrings[2]);
                    
                    if(item == null) //item was removed, or some other parsing issue
                    {
                    	
                    	LogHelper.error("Unable to load item with mod ID " + stackStrings[0] + " and unlocalized name " + stackStrings[1]);
                    	continue;
                    
                    }
                    else if(item instanceof ItemBlock) //item is a block
                    {
                    	Block block = ((ItemBlock) item).field_150939_a;
                    	
                    	wrappedStack = new WrappedStack(block);
                    	
                    }
                    else //an actual item
                    {
                        wrappedStack = new WrappedStack(new ItemStack(item, 1, metadata));
                    }
                }

                float value = (float)entry.getValue().getDouble(0.0F);
                EnergyValue emcValue = new EnergyValue(value);

                if (wrappedStack != null && emcValue.getValue() != 0.0F)
                {
                    valueMap.put(wrappedStack, emcValue);
                }
            }
        }
        catch (Exception e)
        {
            LogHelper.error("Unable to load EMC Value file!");
        }
        return valueMap;
    }

    public static Map<WrappedStack, EnergyValue> getFileValues(String modid)
    {
        File emcFile = getValueFile(modid);
        return getFileValues(emcFile);
    }
    
    static Map<WrappedStack, EnergyValue> getAllLocalFileValues()
    {
    	Map<WrappedStack, EnergyValue> valueMap = new HashMap<WrappedStack, EnergyValue>();


        for (File file : getValueFileFolder().listFiles())
        {
            if (file.getName().endsWith(".emc"))
            {
                valueMap.putAll(getFileValues(file));
            }
        }
        return valueMap;
    }
    
    public static void addValueFileFromServer(File valueFile)
    {
    	if(serverValueMap == null)
    	{
    		serverValueMap = new HashMap<WrappedStack, EnergyValue>();
    	}
    	serverValueMap.putAll(getFileValues(valueFile));
    	
    }
    
    public static Map<WrappedStack, EnergyValue> getAllFileValues()
    {
    	//if values have been added from the server, get them.
    	//else, use the local ones
    	if(serverValueMap != null)
    	{
    		return serverValueMap;
    	}
    	
    	return getAllLocalFileValues();
    }
}
