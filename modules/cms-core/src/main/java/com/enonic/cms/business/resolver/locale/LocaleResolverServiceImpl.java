/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.business.resolver.locale;

import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.enonic.cms.business.core.resource.ResourceService;
import com.enonic.cms.business.localization.resource.LocalizationResourceBundleUtils;
import com.enonic.cms.business.resolver.ForceResolverValueService;
import com.enonic.cms.business.resolver.ScriptResolverService;

import com.enonic.cms.domain.LanguageEntity;
import com.enonic.cms.domain.localization.LocaleParsingException;
import com.enonic.cms.domain.resolver.ForcedResolverValueLifetimeSettings;
import com.enonic.cms.domain.resolver.ResolverContext;
import com.enonic.cms.domain.resolver.ScriptResolverResult;
import com.enonic.cms.domain.resource.ResourceFile;
import com.enonic.cms.domain.resource.ResourceKey;
import com.enonic.cms.domain.structure.SiteEntity;
import com.enonic.cms.domain.structure.menuitem.MenuItemEntity;

/**
 * Created by rmy - Date: Apr 22, 2009
 */
public class LocaleResolverServiceImpl
    implements LocaleResolverService
{
    private ResourceService resourceService;

    private ScriptResolverService localeScriptResolver;

    private ForceResolverValueService forceResolverValueService;

    public final static String LOCALE_FORCED_BASE_NAME = "ForceLocale";

    public Locale getLocale( ResolverContext context )
    {
        if ( !context.getSite().isLocalizationEnabled() )
        {
            return null;
        }

        Locale forcedLocale = getForcedLocale( context );

        if ( forcedLocale != null )
        {
            return forcedLocale;
        }

        ResourceFile localeResolverScript = getLocaleResolverScript( context.getSite() );

        if ( localeResolverScript != null )
        {
            Locale scriptResolvedLocale = resolveLocaleFromScript( context, localeResolverScript );
            if ( scriptResolvedLocale != null )
            {
                return scriptResolvedLocale;
            }
        }

        Locale contextResolvedLocale = getContextResolvedLocale( context );

        return contextResolvedLocale;
    }

    private Locale getForcedLocale( ResolverContext context )
    {
        String forcedLocaleCacheKey = createForcedValueKey( context.getSite() );

        String forcedLocaleString = forceResolverValueService.getForcedResolverValue( context, forcedLocaleCacheKey );

        return StringUtils.isNotEmpty( forcedLocaleString ) ? createLocale( forcedLocaleString ) : null;
    }

    private ResourceFile getLocaleResolverScript( SiteEntity site )
    {
        ResourceKey localeResolverResourceKey = site.getLocaleResolver();

        if ( localeResolverResourceKey == null )
        {
            return null;
        }

        return getResolverScript( localeResolverResourceKey );
    }

    private Locale resolveLocaleFromScript( ResolverContext context, ResourceFile localeResolverScript )
    {
        ScriptResolverResult resolverResult = localeScriptResolver.resolveValue( context, localeResolverScript );

        return (Locale) resolverResult.getResolverReturnValues().get( LocaleXsltScriptResolver.LOCALE_RETURN_VALUE_KEY );
    }

    private Locale getContextResolvedLocale( ResolverContext context )
    {
        LanguageEntity contentLanguage = context.getLanguage();

        if ( contentLanguage != null && StringUtils.isNotEmpty( contentLanguage.getCode() ) )
        {
            return createLocale( contentLanguage.getCode() );
        }

        MenuItemEntity menuItem = context.getMenuItem();

        if ( menuItem != null && menuItem.getLanguage() != null )
        {
            return createLocale( menuItem.getLanguage().getCode() );
        }

        SiteEntity site = context.getSite();

        return site.getLanguage() != null ? createLocale( site.getLanguage().getCode() ) : null;
    }

    private Locale createLocale( String localeString )
    {
        try
        {
            return LocalizationResourceBundleUtils.parseLocaleString( localeString );
        }
        catch ( LocaleParsingException e )
        {
            return null;
        }
    }


    protected ResourceFile getResolverScript( ResourceKey resourceKey )
    {
        return resourceService.getResourceFile( resourceKey );
    }

    protected String createForcedValueKey( SiteEntity site )
    {
        return LOCALE_FORCED_BASE_NAME + site.getKey();
    }

    public void setForcedLocale( ResolverContext context, HttpServletResponse response, ForcedResolverValueLifetimeSettings lifeTimeSetting,
                                 String localeString )
    {

        String forcedValueKey = createForcedValueKey( context.getSite() );

        forceResolverValueService.clearForcedValue( context, response, forcedValueKey );
        forceResolverValueService.setForcedValue( context, response, forcedValueKey, lifeTimeSetting, localeString );
    }

    public void resetLocale( ResolverContext context, HttpServletResponse response )
    {
        forceResolverValueService.clearForcedValue( context, response, createForcedValueKey( context.getSite() ) );
    }

    public void setForceResolverValueService( ForceResolverValueService forceResolverValueService )
    {
        this.forceResolverValueService = forceResolverValueService;
    }

    public void setLocaleScriptResolver( ScriptResolverService localeScriptResolver )
    {
        this.localeScriptResolver = localeScriptResolver;
    }

    @Autowired
    public void setResourceService( ResourceService resourceService )
    {
        this.resourceService = resourceService;
    }

}
