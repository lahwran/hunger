/**
 * 
 */
package net.lahwran.bukkit.hungerandsuch;

import net.lahwran.bukkit.hungerandsuch.Main.TimeValue;

/**
 * @author lahwran
 *
 */
public class HungerTransforms {
    public static final float buildup(float oldvalue, long tickssince)
    {
        return oldvalue + (((float)tickssince)/(1008000f));
    }
    public static int healthdrop(float curvalue)
    {
        if(curvalue < 0) return 0;
        return (int)Math.round(Math.pow(curvalue, 2)*20.0f);
    }
    public static float buildup(TimeValue val)
    {
        long curtime = val.world.getFullTime();
        return buildup(val.value, curtime-val.feedtick);
    }
}
