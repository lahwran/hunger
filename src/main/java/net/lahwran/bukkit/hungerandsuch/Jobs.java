/**
 * 
 */
package net.lahwran.bukkit.hungerandsuch;

import java.util.Map.Entry;

import net.lahwran.bukkit.hungerandsuch.Main.TimeValue;

import org.bukkit.entity.Player;

/**
 * @author lahwran
 *
 */
public class Jobs {
    public static class Job {
        Main plugin;
        public Job(Main plugin)
        {
            this.plugin=plugin;
        }
    }
    public static class SyncDisk extends Job implements Runnable
    {
        public SyncDisk(Main plugin){ super(plugin); }
        public void run(){ plugin.syncToDisk(); }
    }
    public static class InformPlayersHunger extends Job implements Runnable
    {
        public InformPlayersHunger(Main plugin){ super(plugin); }
        public void run()
        {
            synchronized(plugin.lasteaten)
            {
                for(Entry<Player, TimeValue> e:plugin.lasteaten.entrySet())
                {
                    Player p = e.getKey();
                    if (plugin.goldendeaths.contains(p.getName()))
                        continue;
                    TimeValue v = e.getValue();
                    Main.notifyhunger(p,v);
                }
            }
        }
    }
    public static class InformPlayersThirst extends Job implements Runnable
    {
        public InformPlayersThirst(Main plugin){ super(plugin); }
        public void run()
        {
            synchronized(plugin.lastdrink)
            {
                for(Entry<Player, TimeValue> e:plugin.lastdrink.entrySet())
                {
                    Player p = e.getKey();
                    if (plugin.goldendeaths.contains(p.getName()))
                        continue;
                    TimeValue v = e.getValue();
                    Main.notifythirst(p, v);
                }
            }
        }
    }
    public static class Hurter extends Job implements Runnable
    {

        //these two are used in the hurter job to track when health subtract has changed
        int tickrate;
        public Hurter(Main plugin, int tickrate)
        {
            super(plugin);
            this.tickrate=tickrate;
        }
        public void run()
        {
            synchronized(plugin.lasthungers){
            synchronized(plugin.lasteaten){
                for(Entry<Player, TimeValue> e:plugin.lasteaten.entrySet())
                {
                    Player p = e.getKey();
                    if (plugin.goldendeaths.contains(p.getName()))
                        continue;
                    TimeValue v = e.getValue();
                    Integer lasthealthdrop = plugin.lasthungers.get(p.getName());
                    long tickssince = v.world.getFullTime() - v.feedtick;
                    float curvalue = HungerTransforms.buildup(v.value, tickssince);
                    int curhealthdrop = HungerTransforms.healthdrop(curvalue);
                    if(lasthealthdrop == null)
                    {
                        lasthealthdrop = 0;
                    }
                    if (lasthealthdrop < curhealthdrop)
                    {
                        p.damage(curhealthdrop-lasthealthdrop);
                        p.sendMessage("§bThe hunger hurts! find food!");
                    }
                    System.out.println("Hunger for player "+p.getDisplayName()+": "+lasthealthdrop+", "+curhealthdrop+", "+curvalue);
                    plugin.lasthungers.put(p.getName(), curhealthdrop);
                }
            }}
            synchronized(plugin.lastthirsts){
            synchronized(plugin.lastdrink){
                for(Entry<Player, TimeValue> e:plugin.lastdrink.entrySet())
                {
                    Player p = e.getKey();
                    if (plugin.goldendeaths.contains(p.getName()))
                        continue;
                    TimeValue v = e.getValue();
                    Integer lasthealthdrop = plugin.lastthirsts.get(p.getName());
                    long tickssince = v.world.getFullTime() - v.feedtick;
                    float curvalue = ThirstTransforms.buildup(v.value, tickssince);
                    int curhealthdrop = ThirstTransforms.healthdrop(curvalue);
                    if(lasthealthdrop == null)
                    {
                        lasthealthdrop = 0;
                    }
                    if (lasthealthdrop < curhealthdrop)
                    {
                        p.damage(curhealthdrop-lasthealthdrop);
                        p.sendMessage("§bThe thirst hurts! find water!");
                    }
                    System.out.println("Thirst for player "+p.getDisplayName()+": "+lasthealthdrop+", "+curhealthdrop+", "+curvalue);
                    plugin.lastthirsts.put(p.getName(), curhealthdrop);
                }
            }}
        }
    }
    public static class Healer extends Job implements Runnable
    {
        public Healer(Main plugin){ super(plugin); }
        public void run()
        {
            synchronized(plugin.lastdrink){
            synchronized(plugin.lasteaten){
                for(Entry<Player, TimeValue> e:plugin.lasteaten.entrySet())
                {
                    Player player = e.getKey();
                    TimeValue hungerv = e.getValue();
                    TimeValue thirstv = plugin.lastdrink.get(player);
                    float hunger = HungerTransforms.buildup(hungerv);
                    float thirst = ThirstTransforms.buildup(thirstv);
                    
                    boolean toohungry = hunger >= 0.01190476;
                    boolean toothirsty = thirst >= 0.083;
                    int oldhealth = player.getHealth();
                    
                    if (plugin.goldendeaths.contains(player.getName()) || 
                            (!toohungry && !toothirsty))
                    {
                        if (oldhealth >= 20) continue;
                        player.setHealth(oldhealth+1);
                        player.sendMessage("§bYou heal!");
                    }
                    else if (oldhealth < 20)
                    {
                        StringBuilder message = new StringBuilder("§bYou are hurt, but are ");
                        if(toohungry) message.append("too hungry ");
                        if(toohungry && toothirsty) message.append("and ");
                        if(toothirsty) message.append("too thirsty ");
                        message.append("to heal! Find food!");
                    }
                }
            }}
           
        }
    }
}
