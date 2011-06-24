
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

    public static class TimeValue {
        public final long feedtick;
        public final float value;
        public final World world;
        public TimeValue(long feedtick, World world, float value)
        {
            this.feedtick = feedtick;
            this.value = value;
            this.world = world;
        }
    }

    //these two contain players who are currently online
    public HashMap<Player,TimeValue> lasteaten = new HashMap<Player,TimeValue>();
    public HashMap<Player,TimeValue> lastdrink = new HashMap<Player,TimeValue>();
    
    //these two contain every player we know about
    public HashMap<String,Float> allhungers = new HashMap<String,Float>();
    public HashMap<String,Float> allthirsts = new HashMap<String,Float>();
    
    public HashMap<String,Long> soupcooldowns = new HashMap<String,Long>();
    public HashMap<String,Long> drinkcooldowns = new HashMap<String,Long>();

    //used in hurter
    public HashMap<String,Integer> lasthungers = new HashMap<String,Integer>();
    public HashMap<String,Integer> lastthirsts = new HashMap<String,Integer>();

    public HashSet<String> goldendeaths = new HashSet<String>();

    private File hungerfile;

    private File thirstfile;

    private File goldenapplesfile;

    public Main()
    {
        fooditems.add(260); //apple - special behavior: should recharge both health and hunger partially
        fooditems.add(282); //mushroom soup + bowl - special behavior: should heal health some too, should leave bowl - 5 minute cooldown between uses
        fooditems.add(297); //bread
        fooditems.add(322); //golden apple - special behavior: should recharge both health and hunger fully, possibly slow hunger?
        fooditems.add(357); //cookie
        
        fooditems.add(349); //raw fish
        fooditems.add(350); //cooked fish
        fooditems.add(319); //raw pork
        fooditems.add(320); //cooked pork
    }

    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_INTERACT, listener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, listener, Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLAYER_TELEPORT, listener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, listener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_RESPAWN, listener, Priority.Monitor, this);
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
                if(split.length != 3) continue;
                allhungers.put(split[0], Float.parseFloat(split[1]));
                lasthungers.put(split[0], Integer.parseInt(split[2]));
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
                if(split.length != 3) continue;
                allthirsts.put(split[0], Float.parseFloat(split[1]));
                lastthirsts.put(split[0], Integer.parseInt(split[2]));
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
        
        goldenapplesfile = new File(this.getDataFolder(),"goldenapples");
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(goldenapplesfile)));
            String curline;
            while((curline = reader.readLine()) != null)
            {
                if(curline.length() <= 0) continue;
                goldendeaths.add(curline);
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
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Jobs.InformPlayersHunger(this), 12000, 12000);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Jobs.InformPlayersThirst(this), 6000, 6000);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Jobs.Hurter(this, 200), 200, 200);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Jobs.Healer(this), 2000, 6000);
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
        boolean thirstchanged = syncThirst();
        synchronized(allthirsts)
        {
            if(thirstchanged)
            {
                try
                {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thirstfile)));
                    for(Entry<String,Float> e:allthirsts.entrySet())
                    {
                        writer.append(e.getKey());
                        writer.append(" ");
                        writer.append(Float.toString(e.getValue()));
                        writer.append(" ");
                        Integer drop = lastthirsts.get(e.getKey());
                        if(drop == null)
                        {
                            writer.append(""+ThirstTransforms.healthdrop(e.getValue()));
                        }
                        else
                        {
                            writer.append(drop.toString());
                        }
                        writer.append("\n");
                    }
                    writer.close();
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
        synchronized(allhungers)
        {
            if(hungerchanged)
            {
                try
                {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hungerfile)));
                    for(Entry<String,Float> e:allhungers.entrySet())
                    {
                        writer.append(e.getKey());
                        writer.append(" ");
                        writer.append(Float.toString(e.getValue()));
                        writer.append(" ");
                        Integer drop = lasthungers.get(e.getKey());
                        if(drop == null)
                        {
                            writer.append(""+HungerTransforms.healthdrop(e.getValue()));
                        }
                        else
                        {
                            writer.append(drop.toString());
                        }
                        writer.append("\n");
                    }
                    writer.close();
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
        synchronized(goldendeaths)
        {
            try
            {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(goldenapplesfile)));
                boolean notfirst = false;
                for(String p:goldendeaths)
                {
                    if(notfirst) writer.append("\n");
                    writer.append(p);
                    notfirst=true;
                }
                writer.close();
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

    public boolean syncThirst()
    {
        boolean changed = false;
        synchronized(allthirsts)
        {
            synchronized(lastdrink)
            {
                for(Entry<Player,TimeValue> e:lastdrink.entrySet())
                {
                    changed = true;
                    TimeValue val = e.getValue();
                    float newval = ThirstTransforms.buildup(val.value, val.world.getFullTime()-val.feedtick);
                    allthirsts.put(e.getKey().getName(), newval);
                }
            }
        }
        return changed;
    }

    public boolean syncHunger()
    {
        boolean changed = false;
        synchronized(allhungers)
        {
            synchronized(lasteaten)
            {
                for(Entry<Player,TimeValue> e:lasteaten.entrySet())
                {
                    changed = true;
                    TimeValue val = e.getValue();
                    float newval = HungerTransforms.buildup(val.value, val.world.getFullTime()-val.feedtick);
                    allhungers.put(e.getKey().getName(), newval);
                }
            }
        }
        return changed;
    }

    public static String hungerbar(float hunger, int length)
    {
        float fillamt = 1f - ((float)Math.pow(hunger, 0.5f));
        return bar(fillamt, length, 0.154846f, 0.4f, 0.7f, false);
    }

    public static String thirstbar(float thirst, int length)
    {
        float fillamt = 1f - ((float)Math.pow(thirst, 0.6f));
        return bar(fillamt, length, 0.1f, 0.3402f,0.6f, true);
    }

    public static String bar(float fillamt, int length, float fbad, float fmed, float fgood, boolean blue)
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
        String aqua = "§b";
        String darkblue = "§1";
        
        StringBuilder bar = new StringBuilder(white);
        bar.append("[");
        int fullchars = (int)Math.round(fillamt * length);
        int bad = (int)Math.round(fbad * length);
        int med = (int)Math.round(fmed * length);
        int good = (int)Math.round(fgood * length);
        for(int i=0; i<length; i++)
        {
            if (i==fullchars) bar.append(grey);
            else if(i<fullchars)
            {
                if (i==bad) bar.append(red);
                else if (i==med && blue) bar.append(darkblue);
                else if (i==med) bar.append(yellow);
                else if (i==good && blue) bar.append(aqua);
                else if (i==good) bar.append(green);
                else if (i==0) bar.append(darkred);
            }
            bar.append(i<fullchars ? full : empty);
        }
        bar.append(white);
        bar.append("]");
        return bar.toString();
    }

    public static String hungerstatus(float hunger)
    {
        hunger *= 21.0f;
        String result = "You are ";
        final int barlen = 15;
        if (hunger < 0.25)
        {
            if(hunger < 0.075)
            {
                result += "completely ";
            }
            else if(hunger < 0.2)
            {
                //result += "feeling ";
            }
            else
            {
                result += "slightly ";
            }
            result += "full. ";
            result += bar(1.0f - (hunger * 4f), barlen, -1f, -1f, 0f, false);
        }
        else if (hunger < 1.50)
        {
            if(hunger < 0.4)
            {
                result += "slightly ";
            }
            else if(hunger < 1.0)
            {
                //result += "feeling ";
            }
            else
            {
                result += "very ";
            }
            result += "hungry. ";
            result += bar(1.0f - ((hunger-0.25f) * 0.8f), barlen, -1f, -1f, 0f, false);
        }
        else if (hunger < 4.0)
        {
            if(hunger < 1.8)
            {
                result += "getting ";
            }
            else if(hunger < 2.1)
            {
                //result += "feeling ";
            }
            else if(hunger < 3.0)
            {
                result += "very ";
            }
            else
            {
                result += "unbearably ";
            }
            result += "weak. ";
            result += bar(1.0f - ((hunger-1.5f) * 0.4f), barlen, -1f, 0f, 0.4f, false);
        }
        else if (hunger < 9.0)
        {
            if (hunger > 6.5)
            {
                result += "unbearably ";
            }
            result += "famished. ";
            result += bar(1.0f - ((hunger-4.0f) * 0.2f), barlen, 0f, 0.5f, -1f, false);
        }
        else if (hunger < 17.0)
        {
            if(hunger > 14.0)
            {
                result += "deathly ";
            }
            result += "starving. ";
            result += bar(1.0f - ((hunger-9f) * 0.125f), barlen, 0.375f, -1f, -1f, false);
        }
        else
        {
            result += "dying of hunger. ";
            result += bar(1.0f - ((hunger-17f) * 0.25f), barlen, -1f, -1f, -1f, false);
        }
        return result;
    }
    public static String thirststatus(float thirst)
    {
        float barval = 0.0f;
        thirst *= 3.0f;
        String result = "You are ";
        final int barlen = 15;
        if (thirst < 0.25)
        {
            result += "quenched. ";
            result += bar(1.0f - (thirst * 4f), barlen, -1f, -1f, 0f, true);
        }
        else if (thirst < 0.65) 
        {
            result += "thirsty. ";
            result += bar(1.0f - ((thirst-0.25f) * 2.5f), barlen, -1f, 0f, -1f, true);
        }
        else if (thirst < 0.9)
        {
            result += "dehydrated. ";
            result += bar(1.0f - ((thirst-0.65f) * 4f), barlen, 0f, -1f, -1f, true);
        }
        else if (thirst < 1.8)
        {
            result += "parched. ";
            result += bar(1.0f - ((thirst-0.9f) / 0.9f), barlen, -1f, -1f, -1f, true);
        }
        else
        {
            result += "dying of thirst. ";
            result += bar(1.0f - ((thirst-1.8f) / 1.2f), barlen, -1f, -1f, -1f, true);
        }
        return result;
    }
    public static void notifythirst(Player p, TimeValue v)
    {
        long tickssince = v.world.getFullTime() - v.feedtick;
        float thirst = ThirstTransforms.buildup(v.value, tickssince);
        p.sendMessage("§b["+strtime(v.world.getTime())+"] "+Main.thirststatus(thirst));
    }
    public static void notifyhunger(Player p, TimeValue v)
    {
        long tickssince = v.world.getFullTime() - v.feedtick;
        float hunger = HungerTransforms.buildup(v.value, tickssince);
        p.sendMessage("§b["+strtime(v.world.getTime())+"] "+Main.hungerstatus(hunger));
    }
    public static String strtime(long time) {
        int hours = (int) ((time / 1000 + 8) % 24);
        int minutes = (int) (60 * (time % 1000) / 1000);
        return String.format("%d:%02d %s",
                (hours % 12) == 0 ? 12 : hours % 12, minutes,
                hours < 12 ? "am" : "pm");
    }
}
