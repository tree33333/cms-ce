/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.business.image.filter.effect;

import java.awt.Color;
import java.awt.image.RGBImageFilter;

public final class SepiaFilter
    extends RGBImageFilter
{
    private final int depth;

    public SepiaFilter( int depth )
    {
        this.depth = depth < 0 ? 0 : depth;
        this.canFilterIndexColorModel = true;
    }

    public int filterRGB( int x, int y, int rgb )
    {
        Color c = new Color( rgb );
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();

        int gry = ( r + g + b ) / 3;
        r = g = b = gry;

        r = r + ( depth * 2 );
        g = g + depth;
        if ( r > 255 )
        {
            r = 255;
        }
        if ( g > 255 )
        {
            g = 255;
        }

        c = new Color( r, g, b );
        return c.getRGB();
    }
}
