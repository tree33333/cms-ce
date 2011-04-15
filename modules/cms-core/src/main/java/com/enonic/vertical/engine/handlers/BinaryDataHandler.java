/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import javax.inject.Inject;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.binary.BinaryDataEntity;
import com.enonic.cms.core.content.binary.BinaryDataKey;
import com.enonic.cms.core.content.binary.ContentBinaryDataEntity;
import org.joda.time.DateTime;

import com.enonic.cms.framework.blob.BlobStoreObject;

import com.enonic.cms.core.content.binary.access.BinaryAccessResolver;

import com.enonic.cms.core.content.ContentAccessType;
import com.enonic.cms.core.content.ContentVersionEntity;
import com.enonic.cms.core.content.binary.BinaryData;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;

public class BinaryDataHandler
    extends BaseHandler
{

    @Inject
    private BinaryAccessResolver binaryAccessResolver;

    public void setBinaryAccessResolver( BinaryAccessResolver value )
    {
        this.binaryAccessResolver = value;
    }

    private BinaryData getBinaryData( ContentBinaryDataEntity contentBinaryData, boolean anonAccess, long timestamp )
    {
        BinaryData binaryData = new BinaryData();
        binaryData.key = contentBinaryData.getBinaryData().getKey();
        binaryData.contentKey = contentBinaryData.getContentVersion().getContent().getKey().toInt();
        binaryData.setSafeFileName( contentBinaryData.getBinaryData().getName() );
        binaryData.timestamp = contentBinaryData.getBinaryData().getCreatedAt();
        binaryData.anonymousAccess = anonAccess;

        if ( binaryData.timestamp.getTime() > timestamp )
        {
            BlobStoreObject blob = this.binaryDataDao.getBlob( contentBinaryData.getBinaryData() );
            binaryData.data = blob.getData();
        }

        return binaryData;
    }

    public int getBinaryDataKey( int contentKey, String label )
    {
        ContentEntity content = contentDao.findByKey( new ContentKey( contentKey ) );
        if ( content != null )
        {
            BinaryDataEntity binaryData = content.getMainVersion().getSingleBinaryData( label );
            if ( binaryData != null )
            {
                return binaryData.getKey();
            }
        }
        return -1;
    }

    public BinaryData getBinaryData( User oldUser, int binaryKey, long timestamp )
    {

        UserEntity user = securityService.getUser( oldUser );
        BinaryDataEntity binaryData = binaryDataDao.findByKey( new BinaryDataKey( binaryKey ) );
        if ( !binaryAccessResolver.hasReadAndIsAccessibleOnline( binaryData, user, new DateTime() ) )
        {
            return null;
        }

        ContentBinaryDataEntity contentBinaryData = contentBinaryDataDao.findByBinaryKey( binaryData.getKey() );
        ContentVersionEntity contentVersion = contentBinaryData.getContentVersion();
        ContentEntity content = contentVersion.getContent();

        // fast check if anonymous have read
        UserEntity anonymousUser = securityService.getUser( securityService.getAnonymousUserKey() );
        boolean anonAccess = content.hasAccessRightSet( anonymousUser.getUserGroup(), ContentAccessType.READ );
        return getBinaryData( contentBinaryData, anonAccess, timestamp );
    }
}
