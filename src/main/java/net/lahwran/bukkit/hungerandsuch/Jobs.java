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
                    TimeValue v = e.getValue();
                    long tickssince = v.world.getFullTime() - v.feedtick;
                    float hunger = HungerTransforms.buildup(v.value, tickssince);
                    p.sendMessage(Main.hungerbar(hunger, 40));
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
                    TimeValue v = e.getValue();
                    long tickssince = v.world.getFullTime() - v.feedtick;
                    float thirst = ThirstTransforms.buildup(v.value, tickssince);
                    p.sendMessage(Main.thirstbar(thirst, 40));
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
                    }
                    System.out.println("Thirst for player "+p.getDisplayName()+": "+lasthealthdrop+", "+curhealthdrop+", "+curvalue);
                    plugin.lastthirsts.put(p.getName(), curhealthdrop);
                }
            }}
        }
    }
}
