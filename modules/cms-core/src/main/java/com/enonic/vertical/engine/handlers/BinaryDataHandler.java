/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import org.joda.time.DateTime;

import com.enonic.vertical.engine.XDG;

import com.enonic.cms.framework.blob.BlobRecord;

import com.enonic.cms.business.core.content.binary.access.BinaryAccessResolver;

import com.enonic.cms.domain.content.ContentAccessType;
import com.enonic.cms.domain.content.ContentEntity;
import com.enonic.cms.domain.content.ContentKey;
import com.enonic.cms.domain.content.ContentVersionEntity;
import com.enonic.cms.domain.content.binary.BinaryData;
import com.enonic.cms.domain.content.binary.BinaryDataEntity;
import com.enonic.cms.domain.content.binary.BinaryDataKey;
import com.enonic.cms.domain.content.binary.ContentBinaryDataEntity;
import com.enonic.cms.domain.security.user.User;
import com.enonic.cms.domain.security.user.UserEntity;

public class BinaryDataHandler
    extends BaseHandler
{

    private BinaryAccessResolver binaryAccessResolver;

    public void setBinaryAccessResolver( BinaryAccessResolver value )
    {
        this.binaryAccessResolver = value;
    }

    public BinaryData getBinaryData( int binaryKey )
    {
        ContentBinaryDataEntity entity = contentBinaryDataDao.findByBinaryKey( binaryKey );
        if ( entity == null )
        {
            return null;
        }

        return getBinaryData( entity, false, -1 );
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
            BlobRecord blob = this.binaryDataDao.getBlob( contentBinaryData.getBinaryData() );
            binaryData.data = blob.getAsBytes();
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

    public int[] getBinaryDataKeysByVersion( int versionKey )
    {
        StringBuffer sql =
            XDG.generateSelectSQL( db.tContentBinaryData, db.tContentBinaryData.cbd_bda_lKey, false, db.tContentBinaryData.cbd_cov_lKey );
        return getCommonHandler().getIntArray( sql.toString(), versionKey );
    }

}
