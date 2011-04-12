/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.category.CategoryKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.enonic.esl.sql.model.Column;
import com.enonic.esl.sql.model.Table;
import com.enonic.esl.util.ArrayUtil;
import com.enonic.esl.util.StringUtil;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.engine.XDG;
import com.enonic.vertical.engine.dbmodel.ContentPubKeyView;
import com.enonic.vertical.engine.dbmodel.ContentPubKeysView;
import com.enonic.vertical.engine.dbmodel.ContentPublishedView;
import com.enonic.vertical.engine.dbmodel.ContentVersionView;
import com.enonic.vertical.engine.dbmodel.ContentView;
import com.enonic.vertical.engine.filters.ContentFilter;
import com.enonic.vertical.engine.processors.ElementProcessor;
import com.enonic.vertical.engine.processors.ProcessElementException;
import com.enonic.vertical.engine.processors.VersionKeyContentMapProcessor;
import com.enonic.vertical.event.ContentHandlerListener;
import com.enonic.vertical.event.VerticalEventMulticaster;

import com.enonic.cms.framework.util.TIntArrayList;
import com.enonic.cms.framework.util.TIntObjectHashMap;

import com.enonic.cms.store.dao.ContentTypeDao;

import com.enonic.cms.domain.CalendarUtil;
import com.enonic.cms.domain.LanguageEntity;
import com.enonic.cms.domain.LanguageKey;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.security.user.User;

public final class ContentHandler
    extends BaseHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( ContentHandler.class.getName() );

    private final static String CTY_TABLE = "tContentType";

    private final static String HAN_TABLE = "tContentHandler";

    private final static String CON_IS_CHILD = "SELECT rco_con_lParent,rco_con_lChild FROM TRELATEDCONTENT WHERE rco_con_lChild = ?";

    private static final Map<String, String> orderByMap;

    static
    {
        orderByMap = new HashMap<String, String>();

        orderByMap.put( "category", "cat_lKey" );
        orderByMap.put( "owneruid", "usr_sOwnerUID" );
        orderByMap.put( "ownername", "usr_sOwnerName" );
        orderByMap.put( "created", "con_dteCreated" );
        orderByMap.put( "@created", "con_dteCreated" );
        orderByMap.put( "publishfrom", "con_dtePublishFrom" );
        orderByMap.put( "@publishfrom", "con_dtePublishFrom" );
        orderByMap.put( "publishto", "con_dtePublishTo" );
        orderByMap.put( "@publishto", "con_dtePublishTo" );
        orderByMap.put( "languagekey", "con_lan_lKey" );
        orderByMap.put( "title", "cov_sTitle" );
        orderByMap.put( "priority", "con_lPriority" );
        orderByMap.put( "@priority", "con_lPriority" );
        orderByMap.put( "timestamp", "cov_dteTimeStamp" );
        orderByMap.put( "@timestamp", "cov_dteTimeStamp" );
    }

    private VerticalEventMulticaster multicaster = new VerticalEventMulticaster();

    @Autowired
    private ContentTypeDao contentTypeDao;

    public CategoryKey getCategoryKey( int contentKey )
    {
        ContentEntity entity = contentDao.findByKey( new ContentKey( contentKey ) );
        if ( ( entity != null ) && !entity.isDeleted() )
        {
            return entity.getCategory().getKey();
        }
        else
        {
            return null;
        }
    }

    public int[] getCategoryKeys( int[] contentKeys )
    {
        if ( contentKeys == null || contentKeys.length == 0 )
        {
            return new int[0];
        }
        CommonHandler commonHandler = getCommonHandler();
        StringBuffer sql =
            XDG.generateSelectWhereInSQL( db.tContent, db.tContent.con_cat_lKey, true, db.tContent.con_lKey, contentKeys.length );
        return commonHandler.getIntArray( sql.toString(), contentKeys );
    }


    public Document getContentTypesDocument( int[] contentTypeKeys )
    {
        Document doc = XMLTool.createDocument( "contenttypes" );
        if ( contentTypeKeys.length > 0 )
        {
            for ( int contentTypeKey : contentTypeKeys )
            {
                Element contentTypeElem = XMLTool.createElement( doc, doc.getDocumentElement(), "contenttype" );
                contentTypeElem.setAttribute( "key", Integer.toString( contentTypeKey ) );
                XMLTool.createElement( doc, contentTypeElem, "name", getContentHandler().getContentTypeName( contentTypeKey ) );
            }
        }
        return doc;
    }

    public synchronized void addListener( ContentHandlerListener chl )
    {
        multicaster.add( chl );
    }

    public Document getContent( User user, int contentKey, boolean publishedOnly, int parentLevel, int childrenLevel,
                                int parentChildrenLevel, boolean relatedTitlesOnly, boolean includeStatistics, ContentFilter contentFilter )
    {
        return getContents( user, new int[]{contentKey}, publishedOnly, false, parentLevel, childrenLevel, parentChildrenLevel,
                            relatedTitlesOnly, includeStatistics, contentFilter );
    }

    public Document getContents( User user, int[] contentKeys, boolean publishedOnly, boolean titlesOnly, int parentLevel,
                                 int childrenLevel, int parentChildrenLevel, boolean relatedTitlesOnly, boolean includeStatistics,
                                 ContentFilter contentFilter )
    {
        ContentView contentView = ContentView.getInstance();
        if ( contentKeys == null || contentKeys.length == 0 )
        {
            return XMLTool.createDocument( "contents" );
        }

        Table contentTable;
        if ( publishedOnly && titlesOnly )
        {
            contentTable = ContentPubKeysView.getInstance();
        }
        else if ( publishedOnly )
        {
            contentTable = ContentPublishedView.getInstance();
        }
        else
        {
            contentTable = contentView;
        }

        Column[] selectColumns = null;
        if ( titlesOnly )
        {
            selectColumns = getTitlesOnlyColumns();
        }

        StringBuffer sql = XDG.generateSelectSQL( contentTable, selectColumns, false, null );
        XDG.appendWhereInSQL( sql, contentView.con_lKey, contentKeys.length );

        List<Integer> paramValues = ArrayUtil.toArrayList( contentKeys );

        String sqlString = sql.toString();
        if ( user != null )
        {
            int[] categoryKeys = getCategoryKeys( contentKeys );
            sqlString = getSecurityHandler().appendContentSQL( user, categoryKeys, sqlString );
        }

        return getContents( user, null, null, 0, Integer.MAX_VALUE, sqlString, paramValues, false, publishedOnly, titlesOnly, parentLevel,
                            childrenLevel, parentChildrenLevel, relatedTitlesOnly, true, includeStatistics, true, null, false,
                            contentFilter );
    }

    public Document getContentType( int contentTypeKey, boolean includeContentCount )
    {
        return getContentTypes( new int[]{contentTypeKey}, includeContentCount );
    }

    public int getContentTypeKey( int contentKey )
    {
        ContentEntity entity = contentDao.findByKey( new ContentKey( contentKey ) );
        return entity != null ? entity.getCategory().getContentType().getKey() : -1;
    }

    public String getContentTypeName( int contentTypeKey )
    {
        ContentTypeEntity entity = contentTypeDao.findByKey( contentTypeKey );
        return entity != null ? entity.getName() : null;
    }

    public Document getContentTypes( int[] contentTypeKeys, boolean includeContentCount )
    {
        List<ContentTypeEntity> list;

        if ( contentTypeKeys != null && contentTypeKeys.length > 0 )
        {
            list = new ArrayList<ContentTypeEntity>();

            for ( int key : contentTypeKeys )
            {
                ContentTypeEntity entity = contentTypeDao.findByKey( key );
                if ( entity != null )
                {
                    list.add( entity );
                }
            }
        }
        else
        {
            list = contentTypeDao.getAll();
        }

        return createContentTypesDoc( list, includeContentCount );
    }

    public Document getContents( User user, Set<Integer> referencedKeys, Element contentsElem, int fromIdx, int count, String sql,
                                 List<Integer> paramValues, boolean useOneParamValueEachQuery, boolean publishedOnly, boolean titlesOnly,
                                 int parentLevel, int childrenLevel, int parentChildrenLevel, boolean relatedTitlesOnly,
                                 boolean includeTotalCount, boolean includeStatistics, boolean includeSectionNames,
                                 ElementProcessor elementProcessor, boolean allowDuplicateContents, ContentFilter contentFilter )
    {

        Connection con = null;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        PreparedStatement childPreparedStmt = null;
        ResultSet childResultSet = null;

        Document doc;
        Element root;

        doc = XMLTool.createDocument( "contents" );
        if ( contentsElem == null )
        {
            root = doc.getDocumentElement();
        }
        else
        {
            doc = contentsElem.getOwnerDocument();
            root = contentsElem;
        }

        Set<Integer> contentKeys;
        if ( referencedKeys == null )
        {
            contentKeys = new HashSet<Integer>();
        }
        else
        {
            contentKeys = referencedKeys;
        }

        try
        {
            con = getConnection();
            childPreparedStmt = con.prepareStatement( CON_IS_CHILD );
            preparedStmt = con.prepareStatement( sql );
            int paramIndex = -1;
            if ( paramValues != null )
            {
                if ( useOneParamValueEachQuery )
                {
                    if ( paramValues.size() > 0 )
                    {
                        preparedStmt.setObject( 1, paramValues.get( 0 ) );
                        paramIndex = 1;
                    }
                }
                else
                {
                    int i = 1;
                    for ( Iterator<Integer> iter = paramValues.iterator(); iter.hasNext(); i++ )
                    {
                        Object paramValue = iter.next();

                        preparedStmt.setObject( i, paramValue );
                    }
                }
            }
            resultSet = preparedStmt.executeQuery();

            TIntArrayList parentKeys = new TIntArrayList();
            TIntArrayList childrenKeys = new TIntArrayList();
            boolean moreResults = resultSet.next();

            int i;
            if ( !useOneParamValueEachQuery )
            {
                i = fromIdx;
                // Skip rows:
                try
                {
                    if ( fromIdx > 0 )
                    {
                        resultSet.relative( fromIdx );
                    }
                }
                catch ( SQLException e )
                {
                    System.err.println( e.getErrorCode() );
                    // ResultSet is not scrollable
                    i = 0;
                }
            }
            else
            {
                i = 0;
            }

            // related content keys elememt map
            TIntObjectHashMap contentKeyRCKElemMap = new TIntObjectHashMap();
            TIntObjectHashMap versionKeyRCKElemMap = new TIntObjectHashMap();

            // content binaries processor
            VersionKeyContentMapProcessor versionKeyContentMapProcessor = new VersionKeyContentMapProcessor( this );

            SectionHandler sectionHandler = getSectionHandler();
            while ( ( i < fromIdx + count ) && ( moreResults || ( paramIndex > 0 && paramIndex < paramValues.size() ) ) )
            {

                // if we have a result, get data
                if ( i >= fromIdx && moreResults )
                {
                    // pre-fetch content data
                    Document contentdata;
                    if ( titlesOnly )
                    {
                        contentdata = null;
                    }
                    else
                    {
                        InputStream contentDataIn = resultSet.getBinaryStream( "cov_xmlContentData" );
                        contentdata = XMLTool.domparse( contentDataIn );
                    }

                    Integer contentKey = resultSet.getInt( "con_lKey" );
                    if ( ( !allowDuplicateContents && contentKeys.contains( contentKey ) ) ||
                        ( contentFilter != null && !contentFilter.filter( baseEngine, resultSet ) ) )
                    {
                        moreResults = resultSet.next();
                        continue;
                    }
                    contentKeys.add( contentKey );

                    int versionKey = getCurrentVersionKey( contentKey );

                    Element elem = XMLTool.createElement( doc, root, "content" );
                    elem.setAttribute( "key", contentKey.toString() );
                    String unitKey = resultSet.getString( "cat_uni_lKey" );
                    if ( !resultSet.wasNull() )
                    {
                        elem.setAttribute( "unitkey", unitKey );
                    }
                    elem.setAttribute( "contenttypekey", resultSet.getString( "cat_cty_lKey" ) );
                    elem.setAttribute( "versionkey", String.valueOf( versionKey ) );

                    LanguageKey languageKey = new LanguageKey( resultSet.getInt( "con_lan_lKey" ) );
                    elem.setAttribute( "languagekey", String.valueOf( languageKey ) );
                    elem.setAttribute( "languagecode", getLanguageCode( languageKey ) );

                    elem.setAttribute( "priority", resultSet.getString( "con_lPriority" ) );
                    Timestamp publishfrom = resultSet.getTimestamp( "con_dtePublishFrom" );
                    if ( !resultSet.wasNull() )
                    {
                        elem.setAttribute( "publishfrom", CalendarUtil.formatTimestamp( publishfrom ) );
                    }
                    Timestamp publishto = resultSet.getTimestamp( "con_dtePublishTo" );
                    if ( !resultSet.wasNull() )
                    {
                        elem.setAttribute( "publishto", CalendarUtil.formatTimestamp( publishto ) );
                    }
                    Timestamp created = resultSet.getTimestamp( "con_dteCreated" );
                    elem.setAttribute( "created", CalendarUtil.formatTimestamp( created ) );
                    Timestamp timestamp = resultSet.getTimestamp( "cov_dteTimestamp" );
                    elem.setAttribute( "timestamp", CalendarUtil.formatTimestamp( timestamp ) );

                    // content status
                    //  0: draft
                    //  1: not published
                    //  2: publish date undecided
                    //  3: archived
                    //  4: publish waiting
                    //  5: published
                    //  6: publish expired
                    elem.setAttribute( "status", resultSet.getString( "cov_lStatus" ) );
                    elem.setAttribute( "state", resultSet.getString( "cov_lState" ) );

                    if ( !titlesOnly )
                    {
                        // owner info
                        Element ownerElem = XMLTool.createElement( doc, elem, "owner", resultSet.getString( "usr_sOwnerName" ) );
                        ownerElem.setAttribute( "key", resultSet.getString( "usr_hOwner" ) );
                        ownerElem.setAttribute( "uid", resultSet.getString( "usr_sOwnerUID" ) );
                        ownerElem.setAttribute( "deleted", String.valueOf( resultSet.getBoolean( "usr_bOwnerDeleted" ) ) );

                        // modifier info
                        Element modifierElem = XMLTool.createElement( doc, elem, "modifier", resultSet.getString( "usr_sModifierName" ) );
                        modifierElem.setAttribute( "key", resultSet.getString( "usr_hModifier" ) );
                        modifierElem.setAttribute( "uid", resultSet.getString( "usr_sModifierUID" ) );
                        modifierElem.setAttribute( "deleted", String.valueOf( resultSet.getBoolean( "usr_bModDeleted" ) ) );
                    }

                    XMLTool.createElement( doc, elem, "title", resultSet.getString( "cov_sTitle" ) );

                    if ( !titlesOnly )
                    {
                        // add previous pre-fetched content data
                        Node contentDataRoot = doc.importNode( contentdata.getDocumentElement(), true );
                        elem.appendChild( contentDataRoot );

                        // is the content a child?
                        childPreparedStmt.setInt( 1, contentKey );
                        childResultSet = childPreparedStmt.executeQuery();
                        if ( childResultSet.next() )
                        {
                            elem.setAttribute( "child", "true" );
                        }
                        close( childResultSet );
                        childResultSet = null;

                        Element e = XMLTool.createElement( doc, elem, "categoryname", resultSet.getString( "cat_sName" ) );
                        e.setAttribute( "key", resultSet.getString( "cat_lKey" ) );
                    }

                    if ( includeSectionNames )
                    {
                        sectionHandler.appendSectionNames( contentKey, elem );
                    }

                    // get the content's children and parent keys according to children and parent levels
                    Element relatedcontentkeysElem = XMLTool.createElement( doc, elem, "relatedcontentkeys" );
                    contentKeyRCKElemMap.put( contentKey, relatedcontentkeysElem );
                    versionKeyRCKElemMap.put( versionKey, relatedcontentkeysElem );

                    if ( includeStatistics )
                    {
                        getLogEntries( elem, contentKey );
                    }

                    // Post processing
                    try
                    {
                        if ( elementProcessor != null )
                        {
                            elementProcessor.process( elem );
                        }

                        // Always add version info
                        versionKeyContentMapProcessor.process( elem );
                    }
                    catch ( ProcessElementException pee )
                    {
                        String message = "Failed to process element: %t";
                        LOG.warn( StringUtil.expandString( message, null, pee ), pee );
                    }

                    // increase index
                    i++;
                }
                else if ( i < fromIdx )
                {
                    i++;
                }

                // when we're using one param each query, do another query
                if ( paramValues != null && useOneParamValueEachQuery && paramValues.size() > 0 )
                {
                    close( resultSet );
                    resultSet = null;
                    if ( paramIndex < paramValues.size() )
                    {
                        preparedStmt.setObject( 1, paramValues.get( paramIndex ) );
                        paramIndex++;
                        resultSet = preparedStmt.executeQuery();
                        moreResults = resultSet.next();

                        // advance to the next query/ies if this one's empty
                        while ( !moreResults && paramIndex < paramValues.size() )
                        {
                            preparedStmt.setObject( 1, paramValues.get( paramIndex ) );
                            paramIndex++;
                            resultSet = preparedStmt.executeQuery();
                            moreResults = resultSet.next();
                        }
                    }
                    else
                    {
                        moreResults = false;
                    }
                }
                else if ( moreResults )
                {
                    moreResults = resultSet.next();
                }
            }

            if ( resultSet != null )
            {
                resultSet.close();
                resultSet = null;
            }
            preparedStmt.close();
            preparedStmt = null;

            // add binaries elements
            getContentBinaries( versionKeyContentMapProcessor.getVersionKeyContentMap() );

            if ( parentLevel > 0 )
            {
                getParentContentKeys( parentKeys, contentKeyRCKElemMap, publishedOnly, true );
            }
            if ( childrenLevel > 0 )
            {
                getChildrenContentKeys( childrenKeys, versionKeyRCKElemMap, publishedOnly );
            }

            Element relatedcontentsElem;
            if ( "relatedcontents".equals( root.getTagName() ) )
            {
                relatedcontentsElem = root;
            }
            else
            {
                relatedcontentsElem = XMLTool.createElement( doc, root, "relatedcontents" );
            }

            Table contentTable;
            ContentView contentView = ContentView.getInstance();
            if ( publishedOnly && relatedTitlesOnly )
            {
                contentTable = ContentPubKeysView.getInstance();
            }
            else if ( publishedOnly )
            {
                contentTable = ContentPublishedView.getInstance();
            }
            else
            {
                contentTable = contentView;
            }

            if ( parentLevel > 0 && parentKeys.size() > 0 )
            {
                Column[] selectColumns = null;
                if ( relatedTitlesOnly )
                {
                    selectColumns = getTitlesOnlyColumns();
                }

                int[] parentContentKeys = parentKeys.toArray();
                StringBuffer sqlString = XDG.generateSelectSQL( contentTable, selectColumns, false, null );
                XDG.appendWhereInSQL( sqlString, contentView.cov_lKey, parentContentKeys );

                int[] categoryKeys = getCategoryKeys( parentContentKeys );
                String tempSql = sqlString.toString();
                tempSql = getSecurityHandler().appendContentSQL( user, categoryKeys, tempSql );

                parentChildrenLevel = Math.min( parentChildrenLevel, 3 );
                getContents( user, contentKeys, relatedcontentsElem, 0, Integer.MAX_VALUE, tempSql, null, false, publishedOnly,
                             relatedTitlesOnly, parentLevel - 1, parentChildrenLevel, parentChildrenLevel, relatedTitlesOnly, true, false,
                             // includeStatistics
                             true, // includeSectionNames
                             elementProcessor, false, null );
            }

            if ( childrenLevel > 0 && childrenKeys.size() > 0 )
            {
                Column[] selectColumns = null;
                if ( relatedTitlesOnly )
                {
                    selectColumns = getTitlesOnlyColumns();
                }

                StringBuffer sqlString = XDG.generateSelectSQL( contentTable, selectColumns, false, null );
                sqlString.append( " WHERE con_lKey IN (" );
                int[] childrenContentKeys = childrenKeys.toArray();
                int[] categoryKeys = getCategoryKeys( childrenContentKeys );
                for ( int childrenContentKey : childrenContentKeys )
                {
                    sqlString.append( childrenContentKey );
                    sqlString.append( ',' );
                }
                sqlString.replace( sqlString.length() - 1, sqlString.length(), ")" );
                String tempSql = sqlString.toString();
                tempSql = getSecurityHandler().appendContentSQL( user, categoryKeys, tempSql );

                getContents( user, contentKeys, relatedcontentsElem, 0, Integer.MAX_VALUE, tempSql, null, false, publishedOnly,
                             relatedTitlesOnly, 0, childrenLevel - 1, 0, relatedTitlesOnly, true, false, // includeStatistics
                             true, // includeSectionNames
                             elementProcessor, false, null );
            }

            if ( contentsElem == null && includeTotalCount )
            {
                StringBuffer countSql = new StringBuffer( sql );
                int orderByIndex = sql.indexOf( "ORDER BY" );
                int lastParenthesisIndex = sql.lastIndexOf( ")" );
                if ( orderByIndex > 0 )
                {
                    countSql.delete( orderByIndex, sql.length() );
                }

                // append parenthesis if neccessary
                if ( orderByIndex != -1 && lastParenthesisIndex > orderByIndex )
                {
                    countSql.append( ")" );
                }

                countSql.replace( "SELECT ".length(), sql.indexOf( " FROM" ), "count(distinct con_lKey) AS con_lCount" );
                if ( contentFilter != null )
                {
                    contentFilter.appendWhereClause( baseEngine, countSql );
                }
                preparedStmt = con.prepareStatement( countSql.toString() );
                if ( paramValues != null && useOneParamValueEachQuery )
                {
                    int totalCount = 0;
                    for ( i = 0; i < paramValues.size(); i++ )
                    {
                        preparedStmt.setObject( 1, paramValues.get( i ) );
                        resultSet = preparedStmt.executeQuery();
                        if ( resultSet.next() )
                        {
                            totalCount += resultSet.getInt( "con_lCount" );
                        }
                        resultSet.close();
                    }
                    root.setAttribute( "totalcount", String.valueOf( totalCount ) );
                }
                else
                {
                    if ( paramValues != null )
                    {
                        for ( i = 0; i < paramValues.size(); i++ )
                        {
                            preparedStmt.setObject( i + 1, paramValues.get( i ) );
                        }
                    }
                    resultSet = preparedStmt.executeQuery();
                    if ( resultSet.next() )
                    {
                        root.setAttribute( "totalcount", resultSet.getString( "con_lCount" ) );
                    }
                }
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get contents; %t";
            LOG.error( StringUtil.expandString( message, (Object) null, sqle ), sqle );
            if ( contentsElem != null )
            {
                XMLTool.removeChildNodes( contentsElem, false );
            }
            else
            {
                doc = XMLTool.createDocument( "contents" );
            }
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );

            close( childResultSet );
            close( childPreparedStmt );

            close( con );
        }

        return doc;
    }

    private void getChildrenContentKeys( TIntArrayList childrenKeys, TIntObjectHashMap rckElemMap, boolean publishedOnly )
    {

        if ( rckElemMap.size() == 0 )
        {
            return;
        }

        Column[] selectColumns = {db.tRelatedContent.rco_con_lChild, db.tRelatedContent.rco_con_lParent};
        StringBuffer sql = XDG.generateSelectSQL( db.tRelatedContent, selectColumns, false, null );
        if ( publishedOnly )
        {
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lChild, ContentPubKeyView.getInstance(), db.tContent.con_lKey );
        }
        else
        {
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lChild, db.tContent, db.tContent.con_lKey );
            XDG.appendWhereSQL( sql, db.tContent.con_bDeleted, XDG.OPERATOR_EQUAL, 0 );
        }
        XDG.appendWhereInSQL( sql, db.tRelatedContent.rco_con_lParent, rckElemMap.keys() );

        Object[][] keys = getCommonHandler().getObjectArray( sql.toString(), (int[]) null );
        for ( Object[] key : keys )
        {
            int childContentKey = ( (Number) key[0] ).intValue();
            int parentVersionKey = ( (Number) key[1] ).intValue();
            Element relatedcontentkeysElem = (Element) rckElemMap.get( parentVersionKey );
            Document doc = relatedcontentkeysElem.getOwnerDocument();

            Element relatedcontentkey = XMLTool.createElement( doc, relatedcontentkeysElem, "relatedcontentkey" );
            if ( !childrenKeys.contains( childContentKey ) )
            {
                childrenKeys.add( childContentKey );
            }
            relatedcontentkey.setAttribute( "key", String.valueOf( childContentKey ) );
            relatedcontentkey.setAttribute( "level", "1" );
        }
    }

    private void getParentContentKeys( TIntArrayList parentKeys, TIntObjectHashMap rckElemMap, boolean publishedOnly, boolean currentOnly )
    {

        if ( rckElemMap.size() == 0 )
        {
            return;
        }

        StringBuffer sql;
        if ( publishedOnly )
        {
            ContentPubKeysView view = ContentPubKeysView.getInstance();
            Column[] selectColumns = {view.con_lKey, db.tRelatedContent.rco_con_lParent, db.tRelatedContent.rco_con_lChild};
            sql = XDG.generateSelectSQL( db.tRelatedContent, selectColumns, false, null );
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lParent, view, view.cov_lKey );
            XDG.appendWhereInSQL( sql, db.tRelatedContent.rco_con_lChild, rckElemMap.keys() );
        }
        else if ( currentOnly )
        {
            ContentVersionView versionView = ContentVersionView.getInstance();
            Column[] selectColumns = {versionView.con_lKey, db.tRelatedContent.rco_con_lParent, db.tRelatedContent.rco_con_lChild};
            sql = XDG.generateSelectSQL( db.tRelatedContent, selectColumns, false, null );
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lParent, versionView, versionView.cov_lKey );
            XDG.appendWhereInSQL( sql, db.tRelatedContent.rco_con_lChild, rckElemMap.keys() );
            XDG.appendWhereSQL( sql, versionView.cov_bCurrent, XDG.OPERATOR_EQUAL, 1 );
        }
        else
        {
            Column[] selectColumns =
                {db.tContentVersion.cov_con_lKey, db.tRelatedContent.rco_con_lParent, db.tRelatedContent.rco_con_lChild};
            sql = XDG.generateSelectSQL( db.tRelatedContent, selectColumns, false, null );
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lParent, db.tContentVersion, db.tContentVersion.cov_lKey );
            XDG.appendJoinSQL( sql, db.tContentVersion.cov_con_lKey, db.tContent, db.tContent.con_lKey );
            XDG.appendWhereInSQL( sql, db.tRelatedContent.rco_con_lChild, rckElemMap.keys() );
            XDG.appendWhereSQL( sql, db.tContent.con_bDeleted, XDG.OPERATOR_EQUAL, 0 );
        }

        Object[][] keys = getCommonHandler().getObjectArray( sql.toString(), (int[]) null );
        for ( int i = 0; i < keys.length; i++ )
        {
            int parentContentKey = ( (Number) keys[i][0] ).intValue();
            int parentVersionKey = ( (Number) keys[i][1] ).intValue();
            int childContentKey = ( (Number) keys[i][2] ).intValue();
            Element relatedcontentkeysElem = (Element) rckElemMap.get( childContentKey );
            Document doc = relatedcontentkeysElem.getOwnerDocument();

            Element relatedcontentkey = XMLTool.createElement( doc, relatedcontentkeysElem, "relatedcontentkey" );
            if ( parentKeys.contains( parentVersionKey ) == false )
            {
                parentKeys.add( parentVersionKey );
            }
            relatedcontentkey.setAttribute( "key", String.valueOf( parentContentKey ) );
            relatedcontentkey.setAttribute( "versionkey", String.valueOf( parentVersionKey ) );
            relatedcontentkey.setAttribute( "level", "-1" );
        }
    }

    private Document createContentTypesDoc( List<ContentTypeEntity> list, boolean includeContentCount )
    {
        Document doc = XMLTool.createDocument( "contenttypes" );
        Element root = doc.getDocumentElement();

        if ( list == null )
        {
            return doc;
        }

        for ( ContentTypeEntity entity : list )
        {
            Element elem = XMLTool.createElement( doc, root, "contenttype" );
            elem.setAttribute( "key", String.valueOf( entity.getKey() ) );
            elem.setAttribute( "contenthandlerkey", String.valueOf( entity.getHandler().getKey() ) );
            elem.setAttribute( "handler", entity.getHandler().getClassName() );

            if ( entity.getDefaultCssKey() != null )
            {
                elem.setAttribute( "csskey", entity.getDefaultCssKey().toString() );
                elem.setAttribute( "csskeyexists", resourceDao.getResourceFile( entity.getDefaultCssKey() ) != null ? "true" : "false" );
            }

            if ( includeContentCount )
            {
                int count = getContentCountByContentType( entity.getKey() );
                elem.setAttribute( "contentcount", String.valueOf( count ) );
            }

            XMLTool.createElement( doc, elem, "name", entity.getName() );
            if ( entity.getDescription() != null )
            {
                XMLTool.createElement( doc, elem, "description", entity.getDescription() );
            }

            if ( entity.getData() != null )
            {
                Document modDoc = entity.getData().getAsDOMDocument();
                Element mdElem = modDoc.getDocumentElement();

                if ( mdElem.getTagName().equals( "module" ) )
                {
                    Element tempElem = XMLTool.createElement( doc, elem, "moduledata" );
                    tempElem.appendChild( doc.importNode( mdElem, true ) );
                }
                else
                {
                    elem.appendChild( doc.importNode( mdElem, true ) );
                }
            }
            else
            {
                XMLTool.createElement( doc, elem, "moduledata" );
            }

            XMLTool.createElement( doc, elem, "timestamp", CalendarUtil.formatTimestamp( entity.getTimestamp(), true ) );
        }

        return doc;
    }

    public int getContentCountByContentType( int contentTypeKey )
    {
        ContentView contentView = ContentView.getInstance();
        StringBuffer countSQL =
            XDG.generateSelectSQL( contentView, contentView.con_lKey.getCountColumn(), false, contentView.cat_cty_lKey );
        return getCommonHandler().getInt( countSQL.toString(), contentTypeKey );
    }

    private void getLogEntries( Element contentElem, int contentKey )
    {

        LogHandler logHandler = getLogHandler();
        int readCount = logHandler.getReadCount( com.enonic.cms.core.log.Table.CONTENT.asInteger(), contentKey );
        Element elem = XMLTool.createElement( contentElem.getOwnerDocument(), contentElem, "logentries" );
        if ( readCount > 0 )
        {
            elem.setAttribute( "totalread", String.valueOf( readCount ) );
        }
        else
        {
            elem.setAttribute( "totalread", "0" );
        }
    }

    public StringBuffer getCategoryPathString( int contentKey )
    {

        CategoryKey categoryKey;

        ContentEntity content = contentDao.findByKey( new ContentKey( contentKey ) );
        if ( content != null )
        {
            categoryKey = content.getCategory().getKey();
            return getCategoryHandler().getPathString( categoryKey );
        }
        else
        {
            return new StringBuffer();
        }

    }

    public int getCurrentVersionKey( int contentKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tContent, db.tContent.con_cov_lKey, false, db.tContent.con_lKey );
        return getCommonHandler().getInt( sql.toString(), contentKey );
    }

    public void getContentBinaries( TIntObjectHashMap versionKeyContentMap )
    {
        if ( versionKeyContentMap.size() == 0 )
        {
            return;
        }

        Table cbd = db.tContentBinaryData;
        Table bd = db.tBinaryData;
        Column[] selectColumns =
            {db.tBinaryData.bda_lKey, db.tBinaryData.bda_sFileName, db.tBinaryData.bda_lFileSize, db.tContentBinaryData.cbd_sLabel,
                db.tContentBinaryData.cbd_cov_lKey};
        StringBuffer sql = XDG.generateFKJoinSQL( cbd, bd, selectColumns, null );
        XDG.appendWhereInSQL( sql, db.tContentBinaryData.cbd_cov_lKey, versionKeyContentMap.keys().length );

        Object[][] data = getCommonHandler().getObjectArray( sql.toString(), versionKeyContentMap.keys() );

        for ( int binaryCounter = 0; binaryCounter < data.length; binaryCounter++ )
        {
            int binaryKey = ( (Number) data[binaryCounter][0] ).intValue();
            String fileName = StringUtil.stripControlChars( (String) data[binaryCounter][1] );
            int fileSize = ( (Number) data[binaryCounter][2] ).intValue();
            String label = (String) data[binaryCounter][3];
            int versionKey = ( (Number) data[binaryCounter][4] ).intValue();

            Element contentElem = (Element) versionKeyContentMap.get( versionKey );
            Document doc = contentElem.getOwnerDocument();
            Element binariesElem = XMLTool.createElementIfNotPresent( doc, contentElem, "binaries" );
            Element binaryElem = XMLTool.createElement( doc, binariesElem, "binary" );

            binaryElem.setAttribute( "key", String.valueOf( binaryKey ) );
            binaryElem.setAttribute( "filename", fileName );
            binaryElem.setAttribute( "filesize", String.valueOf( fileSize ) );
            if ( label != null )
            {
                binaryElem.setAttribute( "label", label );
            }
        }
    }

    public Column[] getTitlesOnlyColumns()
    {
        ContentView contentView = ContentView.getInstance();
        return new Column[]{contentView.con_lKey, contentView.cov_lKey, contentView.cat_uni_lKey, contentView.cat_cty_lKey,
            contentView.con_lan_lKey, contentView.con_dteCreated, contentView.cov_lStatus, contentView.cov_lState,
            contentView.con_dtePublishFrom, contentView.con_dtePublishTo, contentView.cov_sTitle, contentView.con_lPriority,
            contentView.cov_dteTimestamp,};
    }

    private String getLanguageCode( LanguageKey languageKey )
    {
        LanguageEntity entity = languageDao.findByKey( languageKey );
        return entity != null ? entity.getCode() : null;
    }
}
