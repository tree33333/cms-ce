/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.server.service.portal.mvc.controller;

import com.enonic.cms.domain.SitePath;

/**
 * Feb 17, 2010
 */
public class AttachmentRequestException
    extends RuntimeException
{
    private SitePath sitePath;

    public AttachmentRequestException( SitePath sitePath, String referer, Exception e )
    {
        super( buildMessage( sitePath, referer, e ), e );
        this.sitePath = sitePath;
    }

    private static String buildMessage( SitePath sitePath, String referer, Exception e )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "Failed to serve attachment request [" );
        buf.append( sitePath.asString() );
        buf.append( "] " );
        if ( referer != null )
        {
            buf.append( "referer was [" ).append( referer ).append( "] " );
        }
        buf.append( ": " );
        buf.append( e.getMessage() );
        return buf.toString();
    }

}
