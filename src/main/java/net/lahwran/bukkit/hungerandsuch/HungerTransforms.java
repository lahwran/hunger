/**
 * 
 */
package net.lahwran.bukkit.hungerandsuch;

/**
 * @author lahwran
 *
 */
public class HungerTransforms {
    public static final float buildup(float oldvalue, long tickssince)
    {
        return oldvalue + (((float)tickssince)/504000f);
    }
    public static int healthdrop(float curvalue)
    {
        if(curvalue < 0) return 0;
        return (int)Math.round(Math.pow(curvalue, 2)*20.0f);
    }
}
