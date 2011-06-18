/**
 * 
 */
package net.lahwran.bukkit.hungerandsuch;

/**
 * @author lahwran
 *
 */
public class ThirstTransforms {
    public static final float buildup(float oldvalue, long tickssince)
    {
        return oldvalue + (((float)tickssince)/72000f);
    }
    public static int healthdrop(float curvalue)
    {
        if(curvalue < 0) return 0;
        return (int)Math.round(Math.pow(curvalue, 3)*20.0f);
    }
}
