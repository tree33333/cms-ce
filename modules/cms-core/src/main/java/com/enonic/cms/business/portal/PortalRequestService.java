/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.business.portal;

import com.enonic.cms.domain.portal.PortalRequest;
import com.enonic.cms.domain.portal.PortalResponse;

/**
 * Apr 17, 2009
 */
public interface PortalRequestService
{
    PortalResponse processRequest( PortalRequest request );
}
