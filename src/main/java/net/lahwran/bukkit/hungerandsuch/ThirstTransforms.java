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
}
