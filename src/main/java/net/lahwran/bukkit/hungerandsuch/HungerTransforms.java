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
}
