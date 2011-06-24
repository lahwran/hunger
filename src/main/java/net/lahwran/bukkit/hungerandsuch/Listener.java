package net.lahwran.bukkit.hungerandsuch;

import java.util.HashSet;

import net.lahwran.bukkit.hungerandsuch.Main.TimeValue;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class Listener extends PlayerListener implements CommandExecutor
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
        synchronized(plugin.allhungers)
        {
            if(plugin.allhungers.containsKey(player.getName()))
            {
                hungervalue = plugin.allhungers.get(player.getName());
            }
        }
        TimeValue val = new Main.TimeValue(jointime, world, hungervalue);
        synchronized(plugin.lasteaten)
        {
            plugin.lasteaten.put(player, val);
            Main.notifyhunger(player, val);
        }
        
        float thirstvalue = 0.0f;
        synchronized(plugin.allthirsts)
        {
            if(plugin.allthirsts.containsKey(player.getName()))
            {
                thirstvalue = plugin.allthirsts.get(player.getName());
            }
        }
        val = new Main.TimeValue(jointime, world, thirstvalue);
        synchronized(plugin.lastdrink)
        {
            plugin.lastdrink.put(player, val);
            Main.notifythirst(player, val);
        }
        plugin.syncToDisk();
        
    }

    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        
        plugin.syncToDisk();
        synchronized(plugin.lasteaten)
        {
            if(plugin.lasteaten.containsKey(player))
            {
                plugin.lasteaten.remove(player);
            }
        }
        synchronized(plugin.lastdrink)
        {
            if(plugin.lastdrink.containsKey(player))
            {
                plugin.lastdrink.remove(player);
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
            
            synchronized(plugin.lasteaten)
            {
                Main.TimeValue prevhunger = plugin.lasteaten.get(player);
                float newhunger = 0.0f;
                if (prevhunger != null)
                {
                    
                    newhunger = HungerTransforms.buildup(prevhunger.value, prevhunger.world.getFullTime()-prevhunger.feedtick);
                }
                plugin.lasteaten.put(player, new Main.TimeValue(newtime, toworld, newhunger));
            }
            
            synchronized(plugin.lastdrink)
            {
                Main.TimeValue prevthirst = plugin.lastdrink.get(player);
                float newthirst = 0.0f;
                if (prevthirst != null)
                {
                    newthirst = ThirstTransforms.buildup(prevthirst.value, prevthirst.world.getFullTime()-prevthirst.feedtick);
                }
                plugin.lastdrink.put(player, new Main.TimeValue(newtime, toworld, newthirst));
            }
            plugin.syncToDisk();
        }
    }

    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        Player player = event.getPlayer();
        World world = event.getRespawnLocation().getWorld();
        long curtime = world.getFullTime();
        synchronized(plugin.lasteaten){
        synchronized(plugin.lastdrink){
            TimeValue value = new TimeValue(curtime, world, 0.0f);
            plugin.lasteaten.put(player, value);
            plugin.lastdrink.put(player, value);
            Main.notifyhunger(player, value);
            Main.notifythirst(player, value);
        }}
        synchronized(plugin.goldendeaths)
        {
            plugin.goldendeaths.remove(player.getName());
        }
    }

    public void heal(Player player, int healamt)
    {
        player.setHealth(Math.min(player.getHealth()+healamt, 20));
        
    }

    public TimeValue feed(Player player, int feedamt)
    {
        feedamt *= 2;
        synchronized(plugin.lasteaten)
        {
            TimeValue last = plugin.lasteaten.get(player);
            World world = player.getWorld();
            long curtime = world.getFullTime();
            long tickssince = (curtime-feedamt)-last.feedtick;
            float newval = Math.max(HungerTransforms.buildup(last.value, tickssince), 0.0f);
            TimeValue newtimeval = new TimeValue(curtime,world,newval);
            plugin.lasteaten.put(player, newtimeval);
            return newtimeval;
        }
    }

    public TimeValue hydrate(Player player, int hydrateamount) //lame name
    {
        hydrateamount *= 2;
        synchronized(plugin.lastdrink)
        {
            TimeValue last = plugin.lastdrink.get(player);
            World world = player.getWorld();
            long curtime = world.getFullTime();
            long tickssince = (curtime-hydrateamount)-last.feedtick;
            float newval = Math.max(ThirstTransforms.buildup(last.value, tickssince), 0.0f);
            TimeValue newtimeval = new TimeValue(curtime,world,newval);
            plugin.lastdrink.put(player, newtimeval);
            return newtimeval;
        }
    }
    
    public void subtract(Player player, ItemStack item, int amount)
    {
        if(item.getAmount() - amount <= 0)
            player.getInventory().removeItem(item);
        else
            item.setAmount(item.getAmount() - amount);
    }
    public void subtract(Player player, ItemStack item)
    {
        subtract(player, item, 1);
    }

    public void onPlayerInteract(PlayerInteractEvent event)
    {
        /*System.out.println("--- Interact event ---");
        System.out.println("Item: "+str(event.getItem()));
        System.out.println("Block: "+str(event.getClickedBlock()));
        System.out.println("BlockFace: "+str(event.getBlockFace()));
        System.out.println("Action: "+str(event.getAction()));
        System.out.println("Material: "+str(event.getMaterial()));
        System.out.println("BlockInHand: "+str(event.isBlockInHand()));
        System.out.println("Cancelled: "+str(event.isCancelled()));*/
        Player player = event.getPlayer();
        World world = player.getWorld();
        long curtime = world.getFullTime();
        
        HashSet<Byte> transparent = new HashSet<Byte>();
        transparent.add((Byte)(byte)0);
        Block targetblock = player.getTargetBlock(transparent, 2);
        int targettype = -1;
        if (targetblock != null)
            targettype = targetblock.getTypeId();
        
        
        if(event.hasItem() && (!event.isBlockInHand()) &&
           (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) &&
           plugin.fooditems.contains(event.getItem().getTypeId()))
        {
            int itemid = event.getItem().getTypeId();
            ItemStack item = event.getItem();
            if(itemid == 282)
            {
                //mushroom soup 
                Long lastsoup = plugin.soupcooldowns.get(player.getName());
                if(lastsoup == null || lastsoup < curtime-2000)
                {
                    
                    item.setTypeId(281);
                    heal(player, 4);
                    player.sendMessage("You are feeling less hungry!");
                    Main.notifyhunger(player, feed(player, 3600));
                    plugin.soupcooldowns.put(player.getName(), curtime);
                }
                else
                {
                    int seconds = (int) ((2000 - (curtime-lastsoup))/20);
                    String sseconds = ""+(seconds % 60)+" seconds.";
                    int minutes = (seconds / 60);
                    String sminutes = "";
                    if(minutes > 0)
                        sminutes = ""+minutes+" minutes, ";
                    player.sendMessage(String.format("§bYou have eaten soup too recently for this to affect you. Wait %s%s", sminutes, sseconds));
                }
            }
            else if(itemid == 260)
            {
                heal(player, 8);
                player.sendMessage("That was a good apple!");
                Main.notifyhunger(player, feed(player, 96000));
                subtract(player, item);
            }
            else if(itemid == 322)
            {
                heal(player, 20);
                player.sendMessage("Your health is restored and §byou are free from hunger till death!");
                synchronized(plugin.lasteaten)
                {
                    float newval = -20000000000f; //plenty of zeroes, yes?
                    TimeValue newtimeval = new TimeValue(curtime,world,newval);
                    plugin.lasteaten.put(player, newtimeval);
                }
                synchronized(plugin.goldendeaths)
                {
                    plugin.goldendeaths.add(player.getName());
                }
                subtract(player, item);
            }
            else if(itemid == 297)
            {
                //bread
                player.sendMessage("You feel nourished.");
                Main.notifyhunger(player, feed(player, 18000));
                subtract(player, item);
            }
            else if(itemid == 357)
            {
                //cookie
                player.sendMessage("That was a yummy cookie!");
                Main.notifyhunger(player, feed(player, 4000));
                subtract(player, item);
            }
            else if(itemid == 349)
            {
                //raw fish
                player.sendMessage("That raw fish was gross. Maybe you should have cooked it in a furnace!");
                Main.notifyhunger(player, feed(player, 8000));
                subtract(player, item);
            }
            else if(itemid == 350)
            {
                //cooked fish
                player.sendMessage("The fish tastes bland but is filling.");
                Main.notifyhunger(player, feed(player, 17000));
                subtract(player, item);
            }
            else if(itemid == 319)
            {
                //raw ham
                player.sendMessage("That raw ham was gross. Maybe you should have cooked it in a furnace!");
                Main.notifyhunger(player, feed(player, 8000));
                subtract(player, item);
            }
            else if(itemid == 320)
            {
                //cooked ham
                player.sendMessage("The ham is sweet and filling!.");
                Main.notifyhunger(player, feed(player, 20000));
                subtract(player, item);
            }
            event.setUseItemInHand(Result.DENY);
        }
        else if(targettype == 8 || targettype == 9)
        {
            Long lastdrink = plugin.drinkcooldowns.get(player.getName());
            if(lastdrink == null || lastdrink < curtime - 10)
            {
                int watercount = 0;
                Location l = targetblock.getLocation();
                int startx = l.getBlockX();
                int starty = l.getBlockY();
                int startz = l.getBlockZ();
                for(int x=-2; x<=2; x++)
                for(int z=-2; z<=2; z++)
                {
                    int b_id = world.getBlockAt(startx + x, starty, startz+z).getTypeId();
                    if(b_id == 8 || b_id == 9)
                        watercount++;
                }
                if (event.hasItem() && !event.isBlockInHand() && 
                  (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) &&
                   event.getItem().getTypeId() == 281)
                {
                    //we have a bowl
                    if(watercount > 3)
                    {
                        TimeValue t = hydrate(player, 1500);
                        player.sendMessage("You drink from bowl. "+Main.thirststatus(t.value));
                    }
                    else
                    {
                        player.sendMessage("There is not enough water here to drink from.");
                    }
                }
                else if (!event.hasItem()&& 
                        (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR))
                {
                    if(watercount > 7)
                    {
                        TimeValue t = hydrate(player, 500);
                        player.sendMessage("You drink with hands. "+Main.thirststatus(t.value));
                    }
                    else
                    {
                        player.sendMessage("There is not enough water here to drink with your hands. Try drinking with a bowl.");
                    }
                }
                else if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR)
                {
                    player.sendMessage("You can't drink by punching!");
                }
                else
                {
                    player.sendMessage("You can't drink from pools with that item. Try your hands or a bowl.");
                }
                plugin.drinkcooldowns.put(player.getName(), curtime);
            }
            else
            {
                player.sendMessage("§bYou can't drink that fast!");
            }
        }
        if(event.hasItem() && event.getAction() == Action.RIGHT_CLICK_AIR && 
                event.getItem().getTypeId() == 326)
        {
            ItemStack item = event.getItem();
            item.setTypeId(325);
            TimeValue t = hydrate(player, 20000);
            player.sendMessage("You drink from bucket. "+Main.thirststatus(t.value));
        }
        //if(event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getTypeId() == )
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage("You're not a player, the commands aren't smart enough to help you at the moment");
        }
        Player player = (Player)sender;
        Main.notifyhunger(player, plugin.lasteaten.get(player));
        Main.notifythirst(player, plugin.lastdrink.get(player));
        return true;
    }
}
