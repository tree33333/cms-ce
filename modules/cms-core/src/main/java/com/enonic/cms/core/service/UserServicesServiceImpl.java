/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.service;

import java.util.List;

import javax.inject.Inject;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enonic.esl.util.UncheckedCastUtil;

import com.enonic.cms.core.security.user.User;
import com.enonic.cms.store.dao.UserDao;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class UserServicesServiceImpl
    implements UserServicesService
{
    @Inject
    private UserDao userDao;

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createLogEntries( User user, String xmlData )
    {
        // TODO: Implement method using Hibernate
        throw new IllegalStateException("Method not implemented");
    }

    public String getContent( User user, int key, boolean publishOnly )
    {
        // TODO: Implement method using Hibernate
        throw new IllegalStateException("Method not implemented");
    }

    public String getContentTypeByCategory( int cKey )
    {
        // TODO: Implement method using Hibernate
        throw new IllegalStateException("Method not implemented");
    }

    public String getContentTypeByContent( int contentKey )
    {
        // TODO: Implement method using Hibernate
        throw new IllegalStateException("Method not implemented");
    }

    public User getAnonymousUser()
    {
        return this.userDao.findBuiltInAnonymousUser();
    }

    public String getMenuItem( User user, int mikey )
    {
        // TODO: Implement method using Hibernate
        throw new IllegalStateException("Method not implemented");
    }

    public int getCurrentVersionKey( int contentKey )
    {
        // TODO: Implement method using Hibernate
        throw new IllegalStateException("Method not implemented");
    }

    @Override
    public List<User> browseAccount( String nameExpression, String orderBy, boolean orderAscending )
    {
        return UncheckedCastUtil.castList( userDao.browseAccount( nameExpression, orderBy, orderAscending ), User.class );
    }
}
