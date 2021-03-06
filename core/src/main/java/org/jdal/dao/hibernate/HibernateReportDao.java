/*
 * Copyright 2008-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdal.dao.hibernate;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.jdal.dao.ReportDao;
import org.jdal.reporting.Report;
import org.jdal.reporting.ReportType;

/**
 * @author Jose A. Corbacho
 *
 */
public class HibernateReportDao extends HibernateDao<Report, Long> implements ReportDao {

	public HibernateReportDao() {
		super(Report.class);
	}
	
	@SuppressWarnings("unchecked")
	public List<Report> getReportsByType(ReportType type) {
		return getSession().createCriteria(Report.class)
			.add(Restrictions.eq("type", type))
			.list();
	}

}
