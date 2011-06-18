package net.lahwran.bukkit.hungerandsuch;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class Listener extends PlayerListener
{

    public final Main plugin;

    public Listener(final Main plugin)
    {
        this.plugin = plugin;
    }

    private static final String str(Object input)
    {
        if (input == null)
            return "null";
        else if (input instanceof Block)
        {
            Block block = (Block)input;
            return String.format("<Block %d:%d in %s at %d,%d,%d>",
                    block.getTypeId(),block.getData(),block.getWorld().getName(),
                    block.getX(), block.getY(), block.getZ());
        }
        else if (input instanceof ItemStack)
        {
           ItemStack itemstack = (ItemStack)input;
           return String.format("<ItemStack %d tall of type %d>", itemstack.getAmount(), itemstack.getTypeId());
        }
        else
            return input.toString();
    }

    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        World world = player.getWorld();
        long jointime = world.getFullTime();
        float hungervalue = 0.0f;
        if(plugin.hungers.containsKey(player.getName()))
        {
            hungervalue = plugin.hungers.get(player.getName());
        }
        plugin.lastfood.put(player, new Main.LastValue(jointime, world, hungervalue));
        
        float thirstvalue = 0.0f;
        if(plugin.thirsts.containsKey(player.getName()))
        {
            thirstvalue = plugin.thirsts.get(player.getName());
        }
        plugin.lastwater.put(player, new Main.LastValue(jointime, world, thirstvalue));
        plugin.syncToDisk();
        
    }

    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        
        plugin.syncToDisk();
        synchronized(plugin.lastfood)
        {
            if(plugin.lastfood.containsKey(player))
            {
                plugin.lastfood.remove(player);
            }
        }
        synchronized(plugin.lastwater)
        {
            if(plugin.lastwater.containsKey(player))
            {
                plugin.lastwater.remove(player);
            }
        }
    }

    public void onPlayerTeleport(PlayerTeleportEvent event)
    {
        World toworld = event.getTo().getWorld();
        World fromworld = event.getFrom().getWorld();
        if (fromworld != toworld)
        {
            long newtime = toworld.getFullTime();
            
            Player player = event.getPlayer();
            
            synchronized(plugin.lastfood)
            {
                Main.LastValue prevhunger = plugin.lastfood.get(player);
                float newhunger = 0.0f;
                if (prevhunger != null)
                {
                    
                    newhunger = HungerTransforms.buildup(prevhunger.value, prevhunger.world.getFullTime()-prevhunger.feedtick);
                }
                plugin.lastfood.put(player, new Main.LastValue(newtime, toworld, newhunger));
            }
            
            synchronized(plugin.lastwater)
            {
                Main.LastValue prevthirst = plugin.lastwater.get(player);
                float newthirst = 0.0f;
                if (prevthirst != null)
                {
                    newthirst = ThirstTransforms.buildup(prevthirst.value, prevthirst.world.getFullTime()-prevthirst.feedtick);
                }
                plugin.lastwater.put(player, new Main.LastValue(newtime, toworld, newthirst));
            }
            plugin.syncToDisk();
        }
    }

    public void onPlayerInteract(PlayerInteractEvent event)
    {
        System.out.println("--- Interact event ---");
        System.out.println("Item: "+str(event.getItem()));
        System.out.println("Block: "+str(event.getClickedBlock()));
        System.out.println("BlockFace: "+str(event.getBlockFace()));
        System.out.println("Action: "+str(event.getAction()));
        System.out.println("Material: "+str(event.getMaterial()));
        System.out.println("BlockInHand: "+str(event.isBlockInHand()));
        System.out.println("Cancelled: "+str(event.isCancelled()));
        if(event.hasItem() && (!event.isBlockInHand()) &&
           (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) &&
           plugin.fooditems.contains(event.getItem().getTypeId()))
        {
            System.out.println("you just tried to eat food! I just tried to stop you.");
            boolean eaten = false;
            event.getPlayer().getWorld().getFullTime();
            //if()
            
            event.setUseItemInHand(Result.DENY);
        }
    }
}
