
package net.lahwran.bukkit.hungerandsuch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin{
    public final Listener listener = new Listener(this);

    public final HashSet<Integer> fooditems = new HashSet<Integer>();

    public static class LastValue {
        public final long feedtick;
        public final float value;
        public final World world;
        public LastValue(long feedtick, World world, float value)
        {
            this.feedtick = feedtick;
            this.value = value;
            this.world = world;
        }
    }

    //these two contain players who are currently online
    public final HashMap<Player,LastValue> lastfood = new HashMap<Player,LastValue>();
    public final HashMap<Player,LastValue> lastwater = new HashMap<Player,LastValue>();
    
    //these two contain every player we know about
    public final HashMap<String,Float> hungers = new HashMap<String,Float>();
    public final HashMap<String,Float> thirsts = new HashMap<String,Float>();

    private File hungerfile;

    private File thirstfile;

    public Main()
    {
        fooditems.add(260); //apple
        fooditems.add(282); //mushroom soup + bowl - special behavior: should heal health some too, should leave bowl
        fooditems.add(297); //bread
        fooditems.add(322); //golden apple - special behavior: should recharge both health and hunger
        fooditems.add(357); //cookie
        
        fooditems.add(349); //raw fish
        fooditems.add(350); //cooked fish
        fooditems.add(319); //raw pork
        fooditems.add(320); //cooked pork
    }

    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_INTERACT, listener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, listener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_TELEPORT, listener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, listener, Priority.Normal, this);
        File datadir = this.getDataFolder();
        if (!datadir.exists())
            datadir.mkdirs();
        hungerfile = new File(this.getDataFolder(),"hunger");
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(hungerfile)));
            String curline;
            while((curline = reader.readLine()) != null)
            {
                String[] split = curline.split(" ");
                if(split.length != 2) continue;
                hungers.put(split[0], Float.parseFloat(split[1]));
            }
        }
        catch (FileNotFoundException e){}
        catch (NumberFormatException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        thirstfile = new File(this.getDataFolder(),"thirst");
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(thirstfile)));
            String curline;
            while((curline = reader.readLine()) != null)
            {
                String[] split = curline.split(" ");
                if(split.length != 2) continue;
                thirsts.put(split[0], Float.parseFloat(split[1]));
            }
        }
        catch (FileNotFoundException e){}
        catch (NumberFormatException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Jobs.SyncDisk(this), 600, 600);
        System.out.println("HungerAndSuch enabled");
    }
    public void onDisable()
    {
        getServer().getScheduler().cancelTasks(this);
        syncToDisk();
        System.out.println("HungerAndSuch disabled");
    }

    public synchronized void syncToDisk()
    {
        boolean thirstchanged = false;
        synchronized(thirsts)
        {
            if(thirstchanged)
            {
                try
                {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thirstfile)));
                    for(Entry<String,Float> e:thirsts.entrySet())
                    {
                        writer.append(e.getKey());
                        writer.append(" ");
                        writer.append(Float.toString(e.getValue()));
                        writer.append("\n");
                    }
                }
                catch (FileNotFoundException e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        boolean hungerchanged = syncHunger();
        synchronized(hungers)
        {
            if(hungerchanged)
            {
                try
                {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hungerfile)));
                    for(Entry<String,Float> e:hungers.entrySet())
                    {
                        writer.append(e.getKey());
                        writer.append(" ");
                        writer.append(Float.toString(e.getValue()));
                        writer.append("\n");
                    }
                }
                catch (FileNotFoundException e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean syncThirst()
    {
        boolean changed = false;
        synchronized(thirsts)
        {
            synchronized(lastwater)
            {
                for(Entry<Player,LastValue> e:lastwater.entrySet())
                {
                    changed = true;
                    LastValue val = e.getValue();
                    float newval = ThirstTransforms.buildup(val.value, val.world.getFullTime()-val.feedtick);
                    thirsts.put(e.getKey().getName(), newval);
                }
            }
        }
        return changed;
    }

    public boolean syncHunger()
    {
        boolean changed = false;
        synchronized(hungers)
        {
            synchronized(lastfood)
            {
                for(Entry<Player,LastValue> e:lastfood.entrySet())
                {
                    changed = true;
                    LastValue val = e.getValue();
                    float newval = HungerTransforms.buildup(val.value, val.world.getFullTime()-val.feedtick);
                    hungers.put(e.getKey().getName(), newval);
                }
            }
        }
        return changed;
    }

    public static String hungerbar(float hunger, int length)
    {
        float fillamt = 1f - ((float)Math.pow(hunger, 0.5f));
        return bar(fillamt, length, 0.154846f, 0.4f, 0.7f);
    }

    public static String thirstbar(float thirst, int length)
    {
        float fillamt = 1f - ((float)Math.pow(thirst, 0.6f));
        return bar(fillamt, length, 0.1f, 0.3402f,0.6f);
    }

    public static String bar(float fillamt, int length, float fbad, float fmed, float fgood)
    {
        String full = "=";
        String empty = "-";
        
        String red = "§c";
        String yellow = "§e";
        String green = "§a";
        String white = "§f";
        String darkred = "§4";
        String black = "§0";
        String grey = "§7";
        
        StringBuilder bar = new StringBuilder(white);
        bar.append("[");
        bar.append(black);
        int fullchars = (int)Math.round(fillamt * length);
        int bad = (int)Math.round(fbad * length);
        int med = (int)Math.round(fmed * length);
        int good = (int)Math.round(fgood * length);
        for(int i=0; i<length; i++)
        {
            if (i==fullchars) bar.append(grey);
            else if (i==1) bar.append(darkred);
            else if (i==bad) bar.append(red);
            else if (i==med) bar.append(yellow);
            else if (i==good) bar.append(green);
            bar.append(i<fullchars ? full : empty);
        }
        bar.append(white);
        bar.append("]");
        return bar.toString();
    }
}
