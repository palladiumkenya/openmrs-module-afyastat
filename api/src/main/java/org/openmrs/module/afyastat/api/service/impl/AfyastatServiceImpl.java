/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.afyastat.api.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.afyastat.api.AfyastatService;
import org.openmrs.module.afyastat.api.db.hibernate.HibernateAfyaStatDAO;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;

public class AfyastatServiceImpl extends BaseOpenmrsService implements AfyastatService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private HibernateAfyaStatDAO queueDataDao;
	
	public void setQueueDataDao(HibernateAfyaStatDAO queueDataDao) {
		this.queueDataDao = queueDataDao;
	}
	
	public HibernateAfyaStatDAO getQueueDataDao() {
		return queueDataDao;
	}
	
	@Override
	public AfyaStatQueueData saveQueData(AfyaStatQueueData afyaStatQueueData) {
		return queueDataDao.saveQueData(afyaStatQueueData);
	}
	
	@Override
	public void onStartup() {
		
	}
	
	@Override
	public void onShutdown() {
		
	}
	
}
