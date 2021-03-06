/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.net.URL;
import com.enonic.vertical.VerticalProperties;
import com.enonic.vertical.adminweb.access.AdminConsoleLoginAccessResolver;
import com.enonic.vertical.utilities.VSSpringUtility;

import com.enonic.cms.framework.time.TimeService;

import com.enonic.cms.core.service.AdminService;
import com.enonic.cms.core.service.KeyService;
import com.enonic.cms.core.service.PresentationService;
import com.enonic.cms.store.dao.CategoryDao;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.ContentHandlerDao;
import com.enonic.cms.store.dao.ContentIndexDao;
import com.enonic.cms.store.dao.ContentTypeDao;
import com.enonic.cms.store.dao.ContentVersionDao;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.LanguageDao;
import com.enonic.cms.store.dao.MenuItemDao;
import com.enonic.cms.store.dao.PageTemplateDao;
import com.enonic.cms.store.dao.PortletDao;
import com.enonic.cms.store.dao.SiteDao;
import com.enonic.cms.store.dao.UnitDao;
import com.enonic.cms.store.dao.UserDao;
import com.enonic.cms.store.dao.UserStoreDao;
import com.enonic.cms.upgrade.UpgradeService;

import com.enonic.cms.business.SitePropertiesService;
import com.enonic.cms.business.core.content.ContentParserService;
import com.enonic.cms.business.core.content.ContentService;
import com.enonic.cms.business.core.content.binary.BinaryService;
import com.enonic.cms.business.core.content.imports.ImportJobFactory;
import com.enonic.cms.business.core.content.imports.ImportService;
import com.enonic.cms.business.core.resource.ResourceService;
import com.enonic.cms.business.core.resource.access.ResourceAccessResolver;
import com.enonic.cms.business.core.security.SecurityService;
import com.enonic.cms.business.core.security.userstore.MemberOfResolver;
import com.enonic.cms.business.core.security.userstore.UserStoreService;
import com.enonic.cms.business.core.security.userstore.connector.synchronize.SynchronizeUserStoreJobFactory;
import com.enonic.cms.business.core.structure.SiteService;
import com.enonic.cms.business.core.structure.menuitem.MenuItemService;
import com.enonic.cms.business.country.CountryService;
import com.enonic.cms.business.locale.LocaleService;
import com.enonic.cms.business.log.LogService;
import com.enonic.cms.business.mail.SendMailService;
import com.enonic.cms.business.portal.cache.SiteCachesService;
import com.enonic.cms.business.portal.rendering.PageRendererFactory;
import com.enonic.cms.business.preview.PreviewService;
import com.enonic.cms.business.resolver.deviceclass.DeviceClassResolverService;
import com.enonic.cms.business.resolver.locale.LocaleResolverService;
import com.enonic.cms.business.timezone.TimeZoneService;

public abstract class AbstractAdminwebServlet
    extends HttpServlet
{

    protected VerticalProperties verticalProperties;

    // Daos:

    @Autowired
    protected CategoryDao categoryDao;

    protected ContentDao contentDao;

    protected ContentIndexDao contentIndexDao;

    @Autowired
    protected ContentHandlerDao contentHandlerDao;

    @Autowired
    protected ContentTypeDao contentTypeDao;

    @Autowired
    protected ContentVersionDao contentVersionDao;

    @Autowired
    protected GroupDao groupDao;

    protected LanguageDao languageDao;

    @Autowired
    protected MenuItemDao menuItemDao;

    @Autowired
    protected PageTemplateDao pageTemplateDao;

    @Autowired
    protected PortletDao portletDao;

    @Autowired
    protected SiteDao siteDao;

    @Autowired
    protected UnitDao unitDao;

    @Autowired
    protected UserDao userDao;

    protected UserStoreDao userStoreDao;

    // Services:

    protected AdminService adminService;

    @Autowired
    protected BinaryService binaryService;

    protected ContentService contentService;

    @Autowired
    protected ContentParserService contentParserService;

    @Autowired
    protected CountryService countryService;

    @Autowired
    protected DeviceClassResolverService deviceClassResolverService;

    @Autowired
    protected ImportService importService;

    protected KeyService keyService;

    @Autowired
    protected LocaleService localeService;

    @Autowired
    protected LocaleResolverService localeResolverService;

    @Autowired
    protected LogService logService;

    @Autowired
    protected MenuItemService menuItemService;

    protected PresentationService presentation;

    protected ResourceService resourceService;

    protected SecurityService securityService;

    @Autowired
    protected SendMailService sendMailService;

    protected SiteService siteService;

    @Autowired
    protected SiteCachesService siteCachesService;

    protected SitePropertiesService sitePropertiesService;

    @Autowired
    protected TimeService timeService;

    @Autowired
    protected PreviewService previewService;

    @Autowired
    protected TimeZoneService timeZoneService;

    protected UpgradeService upgradeService;

    @Autowired
    protected UserStoreService userStoreService;

    // Factories:

    @Autowired
    protected ImportJobFactory importJobFactory;

    protected PageRendererFactory pageRendererFactory;

    @Autowired
    protected SynchronizeUserStoreJobFactory synchronizeUserStoreJobFactory;

    // Resolvers:

    @Autowired
    protected AdminConsoleLoginAccessResolver adminConsoleLoginAccessResolver;

    @Autowired
    protected MemberOfResolver memberOfResolver;

    protected ResourceAccessResolver resourceAccessResolver;

    public void init( ServletConfig servletConfig )
        throws ServletException
    {
        super.init( servletConfig );

        final ServletContext context = getServletContext();
        AdminStore.initialize( context, "/WEB-INF/stylesheets" );

        VSSpringUtility.autoWireObject( this, servletConfig.getServletContext() );
    }

    public void setSitePropertiesService( SitePropertiesService value )
    {
        this.sitePropertiesService = value;
    }

    @Autowired
    public void setContentDao( ContentDao contentDao )
    {
        this.contentDao = contentDao;
    }

    @Autowired
    public void setContentIndexDao( ContentIndexDao contentIndexDao )
    {
        this.contentIndexDao = contentIndexDao;
    }

    @Autowired
    public void setLanguageDao( LanguageDao languageDao )
    {
        this.languageDao = languageDao;
    }

    public void setVerticalProperties( VerticalProperties value )
    {
        this.verticalProperties = value;
    }

    public void setAdminService( AdminService value )
    {
        this.adminService = value;
    }

    public void setKeyService( KeyService value )
    {
        this.keyService = value;
    }

    public void setResourceAccessResolver( ResourceAccessResolver resourceAccessResolver )
    {
        this.resourceAccessResolver = resourceAccessResolver;
    }

    public void setPresentationService( PresentationService value )
    {
        this.presentation = value;
    }

    public void setSiteService( SiteService value )
    {
        this.siteService = value;
    }

    public void setContentService( ContentService value )
    {
        this.contentService = value;
    }

    public void setResourceService( ResourceService value )
    {
        this.resourceService = value;
    }

    public void setUpgradeService( UpgradeService value )
    {
        this.upgradeService = value;
    }

    public void setPageRendererFactory( PageRendererFactory value )
    {
        this.pageRendererFactory = value;
    }

    public void setSecurityService( SecurityService value )
    {
        this.securityService = value;
    }

    public void setUserStoreDao( UserStoreDao userStoreDao )
    {
        this.userStoreDao = userStoreDao;
    }

    protected AdminService lookupAdminBean()
    {
        return adminService;
    }

    protected KeyService lookupKeyBean()
        throws VerticalAdminException
    {
        return keyService;
    }


    protected boolean isRequestForAdminPath( String path, HttpServletRequest request )
    {
        if ( path == null )
        {
            throw new NullPointerException( path );
        }
        if ( !path.startsWith( "/" ) )
        {
            throw new IllegalArgumentException( "Expected a path that starts with a forward slash" );
        }

        return request.getRequestURI().endsWith( path );
    }

    protected void redirectClientToAdminPath( String adminPath, HttpServletRequest request, HttpServletResponse response )
        throws VerticalAdminException
    {
        redirectClientToAdminPath( adminPath, (MultiValueMap) null, request, response );
    }

    protected void redirectClientToAdminPath( String adminPath, ExtendedMap formItems, HttpServletRequest request,
                                              HttpServletResponse response )
        throws VerticalAdminException
    {
        MultiValueMap mv = null;
        if ( formItems != null )
        {
            mv = new MultiValueMap( formItems );
        }
        redirectClientToAdminPath( adminPath, mv, request, response );
    }

    protected void redirectClientToAdminPath( String adminPath, String parameterName, String parameterValue, HttpServletRequest request,
                                              HttpServletResponse response )
        throws VerticalAdminException
    {
        MultiValueMap queryParams = new MultiValueMap();
        queryParams.put( parameterName, parameterValue );

        redirectClientToAdminPath( adminPath, queryParams, request, response );
    }

    protected void redirectClientToAdminPath( String adminPath, MultiValueMap queryParams, HttpServletRequest request,
                                              HttpServletResponse response )
        throws VerticalAdminException
    {
        AdminHelper.redirectClientToAdminPath( request, response, adminPath, queryParams );
    }

    protected void redirectClientToReferer( HttpServletRequest request, HttpServletResponse response )
    {
        AdminHelper.redirectClientToReferer( request, response );
    }

    protected void redirectClientToURL( URL url, HttpServletResponse response )
    {
        AdminHelper.redirectToURL( url, response );
    }

    protected void redirectClientToAbsoluteUrl( String url, HttpServletResponse response )
    {
        AdminHelper.redirectClientToAbsoluteUrl( url, response );
    }


    protected void forwardRequest( String adminPath, HttpServletRequest request, HttpServletResponse response )
        throws VerticalAdminException
    {
        int length = adminPath != null ? adminPath.length() : 0;
        length += "/admin".length();

        StringBuffer newUrl = new StringBuffer( length );
        newUrl.append( "/admin" );
        newUrl.append( adminPath );

        try
        {
            RequestDispatcher dispatcher = request.getRequestDispatcher( newUrl.toString() );
            dispatcher.forward( request, response );
        }
        catch ( IOException ioe )
        {
            String message = "Failed to forward request to \"%0\": %t";
            VerticalAdminLogger.errorAdmin( this.getClass(), 0, message, adminPath, ioe );
        }
        catch ( ServletException se )
        {
            String message = "Failed to forward request to \"%0\": %t";
            VerticalAdminLogger.errorAdmin( this.getClass(), 0, message, adminPath, se );
        }
    }

}
