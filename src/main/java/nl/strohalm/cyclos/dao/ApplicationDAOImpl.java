/*
   This file is part of Cyclos.

   Cyclos is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   Cyclos is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Cyclos; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package nl.strohalm.cyclos.dao;

import java.sql.SQLException;

import nl.strohalm.cyclos.entities.Application;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * Implementation for application dao
 * @author luis
 */
public class ApplicationDAOImpl extends BaseDAOImpl<Application> implements ApplicationDAO {

    public ApplicationDAOImpl() {
        super(Application.class);
    }

    public Application read() {
        return getHibernateTemplate().execute(new HibernateCallback<Application>() {
            public Application doInHibernate(final Session session) throws HibernateException, SQLException {
                return (Application) session.createCriteria(Application.class).uniqueResult();
            }
        });
    }
}
