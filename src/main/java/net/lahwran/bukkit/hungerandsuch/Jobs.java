/**
 * 
 */
package net.lahwran.bukkit.hungerandsuch;

import java.util.Map.Entry;

import net.lahwran.bukkit.hungerandsuch.Main.LastValue;

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
            synchronized(plugin.lastfood)
            {
                for(Entry<Player, LastValue> e:plugin.lastfood.entrySet())
                {
                    Player p = e.getKey();
                    LastValue v = e.getValue();
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
            
        }
    }
}
