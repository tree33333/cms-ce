/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.resource;

import javax.inject.Inject;

import com.google.common.collect.Multimap;

import com.enonic.cms.store.dao.ResourceDao;
import com.enonic.cms.store.dao.ResourceUsageDao;

public class ResourceServiceImpl
    implements ResourceService
{
    @Inject
    private ResourceUsageDao resourceUsageDao;

    @Inject
    private ResourceDao resourceDao;

    public ResourceFile getResourceFile( ResourceKey resourceKey )
    {
        return resourceDao.getResourceFile( resourceKey );
    }

    public ResourceBase getResource( ResourceKey resourceKey )
    {
        ResourceBase resource = resourceDao.getResourceFile( resourceKey );
        if ( resource == null )
        {
            resource = resourceDao.getResourceFolder( resourceKey );
        }
        return resource;
    }

    public Multimap<ResourceKey, ResourceReferencer> getUsedBy( ResourceKey resourceKey )
    {
        return resourceUsageDao.getUsedBy( resourceKey );
    }
}