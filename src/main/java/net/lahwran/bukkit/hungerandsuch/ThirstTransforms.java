/**
 * 
 */
package net.lahwran.bukkit.hungerandsuch;

import net.lahwran.bukkit.hungerandsuch.Main.TimeValue;

/**
 * @author lahwran
 *
 */
public class ThirstTransforms {
    public static final float buildup(float oldvalue, long tickssince)
    {
        return oldvalue + (((float)tickssince)/(144000f));
    }
    public static int healthdrop(float curvalue)
    {
        if(curvalue < 0) return 0;
        return (int)Math.round(Math.pow(curvalue, 3)*20.0f);
    }
    public static float buildup(TimeValue val)
    {
        long curtime = val.world.getFullTime();
        return buildup(val.value, curtime-val.feedtick);
    }
}
