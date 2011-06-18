/**
 * 
 */
package net.lahwran.bukkit.hungerandsuch;

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
