package ca.thederpygolems.oldenchanting;

import java.util.Random;
import ca.thederpygolems.oldenchanting.versions.PrepareItemEnchant;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Arnah
 * @since Nov 27, 2015
 */
public class OldEnchanting extends JavaPlugin implements Listener, CommandExecutor{
	
	private final Random rand = new Random();
	
	private boolean lapis, hideEnchant, oldEnchantCosts, randomizeEnchants;
	private PrepareItemEnchant event;
	private String version;
	private float versionVal;
	private Class<?> craftInventoryView;
	
	public void onEnable(){
		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(this, this);
		getCommand("oldenchanting").setExecutor(this);
		load(false);
		try{
			version = getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			versionVal = Float.parseFloat(version.substring(1, 5).replace("_", "."));
		}catch(ArrayIndexOutOfBoundsException ignored){
		}
		if(!setupVersionInterface()){
			getLogger().severe("Failed to setup OldEnchanting for version: " + version + ". Please report this.");
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}
	
	public boolean setupVersionInterface(){
		if(version == null) return false;
		try{
			craftInventoryView = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftInventoryView");
		}catch(Exception ex){
			return false;
		}
		try{
			Class<?> clazz = Class.forName("ca.thederpygolems.oldenchanting.versions.PrepareItemEnchant" + version.replace("v", "_"));
			event = (PrepareItemEnchant) clazz.getConstructor(OldEnchanting.class).newInstance(this);
			return true;
		}catch(Exception e){
			try{
				Class<?> clazz;
				if(versionVal <= 1.10){
					clazz = Class.forName("ca.thederpygolems.oldenchanting.versions.PrepareItemEnchant_Fallback_Old");
				}else{
					clazz = Class.forName("ca.thederpygolems.oldenchanting.versions.PrepareItemEnchant_Fallback");
				}
				event = (PrepareItemEnchant) clazz.getConstructor(OldEnchanting.class).newInstance(this);
				return true;
			}catch(Exception ex){
				return false;
			}
		}
	}
	
	@EventHandler
	public void prepareItemEnchant(PrepareItemEnchantEvent e){
		if(randomizeEnchants) event.randomizeSeed(e);
		if(oldEnchantCosts) event.oldEnchantCosts(e);
		if(hideEnchant) event.hideEnchants(e);
	}
	
	@EventHandler
	public void enchantItem(EnchantItemEvent e){
		if(lapis || oldEnchantCosts){
			getServer().getScheduler().scheduleSyncDelayedTask(this, ()->{// Fix up removing 1,2,3 levels depending on tier, and restock.
				if(oldEnchantCosts) e.getEnchanter().setLevel(e.getEnchanter().getLevel() - (e.getExpLevelCost() - (64 - e.getInventory().getItem(1).getAmount())));
				if(lapis) e.getInventory().setItem(1, new ItemStack(Material.INK_SACK, 64, (short) 4));
			}, 1);
		}
	}
	
	@EventHandler
	public void lapisClickEvent(InventoryClickEvent e){// Prevent them from stealing lapis
		if(!lapis) return;
		if(e.getClickedInventory() != null && e.getClickedInventory().getType().equals(InventoryType.ENCHANTING)){
			if(e.getRawSlot() == 1){
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void inventoryOpen(InventoryOpenEvent e){// Give lapis
		if(!lapis) return;
		if(e.getInventory() != null && e.getInventory().getType().equals(InventoryType.ENCHANTING)){
			e.getInventory().setItem(1, new ItemStack(Material.INK_SACK, 64, (short) 4));
		}
	}
	
	@EventHandler
	public void inventoryClose(InventoryCloseEvent e){// Remove lapis so it doesn't drop on the ground.
		if(!lapis) return;
		if(e.getInventory() != null && e.getInventory().getType().equals(InventoryType.ENCHANTING)){
			e.getInventory().setItem(1, null);
		}
	}
	
	public boolean onCommand(CommandSender sender, Command arg1, String label, String[] args){
		if(args == null || args.length <= 0) return false;
		if(args[0].equalsIgnoreCase("reload")){
			load(true);
			sender.sendMessage(ChatColor.GREEN + "OldEnchanting config reloaded.");
		}
		return true;
	}
	
	public void load(boolean reload){
		if(reload) reloadConfig();
		lapis = getConfig().getBoolean("lapis");
		hideEnchant = getConfig().getBoolean("hideEnchant");
		oldEnchantCosts = getConfig().getBoolean("oldEnchantCosts");
		randomizeEnchants = getConfig().getBoolean("randomizeEnchants");
	}
	
	public Random getRand(){
		return rand;
	}
	
	public Class<?> getCraftInventoryView(){
		return craftInventoryView;
	}
}