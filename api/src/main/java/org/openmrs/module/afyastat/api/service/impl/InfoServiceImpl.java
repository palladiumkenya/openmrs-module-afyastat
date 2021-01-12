/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.afyastat.api.service.impl;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Person;
import org.openmrs.Role;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.afyastat.api.db.ArchiveInfoDao;
import org.openmrs.module.afyastat.api.db.ErrorInfoDao;
import org.openmrs.module.afyastat.api.db.NotificationInfoDao;
import org.openmrs.module.afyastat.api.db.InfoSourceDao;
import org.openmrs.module.afyastat.api.db.QueueInfoDao;
import org.openmrs.module.afyastat.api.db.ErrorMessageInfoDao;
import org.openmrs.module.afyastat.api.service.AfyaStatDataService;
import org.openmrs.module.afyastat.api.service.RegistrationInfoService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.model.RegistrationInfo;
import org.openmrs.module.afyastat.model.AfyaDataSource;
import org.openmrs.module.afyastat.model.NotificationInfo;
import org.openmrs.module.afyastat.model.ArchiveInfo;
import org.openmrs.module.afyastat.model.ErrorInformation;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.ErrorMessagesInfo;
import org.openmrs.module.afyastat.model.FormInfoStatus;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.util.HandlerUtil;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 */
public class InfoServiceImpl extends BaseOpenmrsService implements AfyaStatDataService {
	
	private ErrorInfoDao errorDataDao;
	
	private QueueInfoDao queueDataDao;
	
	private ArchiveInfoDao archiveDataDao;
	
	private InfoSourceDao dataSourceDao;
	
	private NotificationInfoDao notificationDataDao;
	
	private ErrorMessageInfoDao errorMessageDao;
	
	public QueueInfoDao getQueueDataDao() {
		return queueDataDao;
	}
	
	public void setQueueDataDao(final QueueInfoDao queueDataDao) {
		this.queueDataDao = queueDataDao;
	}
	
	public ErrorInfoDao getErrorDataDao() {
		return errorDataDao;
	}
	
	public void setErrorDataDao(final ErrorInfoDao errorDataDao) {
		this.errorDataDao = errorDataDao;
	}
	
	public ArchiveInfoDao getArchiveDataDao() {
		return archiveDataDao;
	}
	
	public void setArchiveDataDao(final ArchiveInfoDao archiveDataDao) {
		this.archiveDataDao = archiveDataDao;
	}
	
	public InfoSourceDao getDataSourceDao() {
		return dataSourceDao;
	}
	
	public void setDataSourceDao(final InfoSourceDao dataSourceDao) {
		this.dataSourceDao = dataSourceDao;
	}
	
	public NotificationInfoDao getNotificationDataDao() {
		return notificationDataDao;
	}
	
	public void setNotificationDataDao(final NotificationInfoDao notificationDataDao) {
		this.notificationDataDao = notificationDataDao;
	}
	
	public ErrorMessageInfoDao getErrorMessageDao() {
		return errorMessageDao;
	}
	
	public void setErrorMessageDao(final ErrorMessageInfoDao errorMessageDao) {
		this.errorMessageDao = errorMessageDao;
	}
	
	/**
	 * Return the data with the given id.
	 * 
	 * @param id the form data id.
	 * @return the form data with the matching id.
	 * @should return form data with matching id.
	 * @should return null when no form data with matching id.
	 */
	@Override
	public AfyaStatQueueData getQueueData(final Integer id) {
		return getQueueDataDao().getData(id);
	}
	
	/**
	 * Return the data with the given uuid.
	 * 
	 * @param uuid the form data uuid.
	 * @return the form data with the matching uuid.
	 * @should return form data with matching uuid.
	 * @should return null when no form data with matching uuid.
	 */
	@Override
	public AfyaStatQueueData getQueueDataByUuid(final String uuid) {
		return getQueueDataDao().getDataByUuid(uuid);
	}
	
	/**
	 * Return all saved form data.
	 * 
	 * @return all saved form data.
	 * @should return empty list when no form data are saved in the database.
	 * @should return all saved form data.
	 */
	@Override
	public List<AfyaStatQueueData> getAllQueueData() {
		return getQueueDataDao().getAllData();
	}
	
	/**
	 * Save form data into the database.
	 * 
	 * @param formData the form data.
	 * @return saved form data.
	 * @should save form data into the database.
	 */
	@Override
	public AfyaStatQueueData saveQueueData(final AfyaStatQueueData formData) {
		return getQueueDataDao().saveData(formData);
	}
	
	/**
	 * Delete form data from the database.
	 * 
	 * @param formData the form data
	 * @should remove form data from the database
	 */
	@Override
	public void purgeQueueData(final AfyaStatQueueData formData) {
		getQueueDataDao().purgeData(formData);
	}
	
	/**
	 * Get the total number of the queue data in the database with partial matching search term on
	 * the payload.
	 * 
	 * @param search the search term.
	 * @return the total number of the queue data in the database.
	 */
	@Override
	public Number countQueueData(final String search) {
		return queueDataDao.countData(search);
	}
	
	/**
	 * Get queue data with matching search term for a particular page.
	 * 
	 * @param search the search term.
	 * @param pageNumber the page number.
	 * @param pageSize the size of the page.
	 * @return list of all queue data with matching search term for a particular page.
	 */
	@Override
	public List<AfyaStatQueueData> getPagedQueueData(final String search, final Integer pageNumber, final Integer pageSize) {
		return queueDataDao.getPagedData(search, pageNumber, pageSize);
	}
	
	/**
	 * Return the error data with the given id.
	 * 
	 * @param id the error data id.
	 * @return the error data with the matching id.
	 * @should return error data with matching id.
	 * @should return null when no error data with matching id.
	 */
	@Override
	public ErrorInformation getErrorData(final Integer id) {
		return getErrorDataDao().getData(id);
	}
	
	/**
	 * Return the error data with the given uuid.
	 * 
	 * @param uuid the error data uuid.
	 * @return the error data with the matching uuid.
	 * @should return error data with matching uuid.
	 * @should return null when no error data with matching uuid.
	 */
	@Override
	public ErrorInformation getErrorDataByUuid(final String uuid) {
		return getErrorDataDao().getDataByUuid(uuid);
	}
	
	/**
	 * Return the registration error data with the given patientUuid.
	 * 
	 * @param patientUuid the error data uuid.
	 * @return the registration error data with the matching patientUuid.
	 * @should return registration error data with matching patientUuid.
	 * @should return null when no registration error data with matching patientUuid.
	 */
	@Override
	public ErrorInformation getRegistrationErrorDataByPatientUuid(String patientUuid) {
		
		List<ErrorInformation> errors = getErrorDataDao().getPagedData(patientUuid, null, null);
		for (ErrorInformation errorData : errors) {
			if (StringUtils.equals("json-registration", errorData.getDiscriminator())) {
				return errorData;
			}
		}
		return null;
	}
	
	/**
	 * Return all saved error data.
	 * 
	 * @return all saved error data.
	 * @should return empty list when no error data are saved in the database.
	 * @should return all saved error data.
	 */
	@Override
	public List<ErrorInformation> getAllErrorData() {
		return getErrorDataDao().getAllData();
	}
	
	/**
	 * Save error data into the database.
	 * 
	 * @param errorData the error data.
	 * @return saved error data.
	 * @should save error data into the database.
	 */
	@Override
	public ErrorInformation saveErrorData(final ErrorInformation errorData) {
		return getErrorDataDao().saveData(errorData);
	}
	
	/**
	 * Delete error data from the database.
	 * 
	 * @param errorData the error data
	 * @should remove error data from the database
	 */
	@Override
	public void purgeErrorData(final ErrorInformation errorData) {
		getErrorDataDao().purgeData(errorData);
	}
	
	/**
	 * Get the total number of the error data in the database with partial matching search term on
	 * the payload.
	 * 
	 * @param search the search term.
	 * @return the total number of the error data in the database.
	 */
	@Override
	public Number countErrorData(final String search) {
		return errorDataDao.countData(search);
	}
	
	/**
	 * Get error data with matching search term for a particular page.
	 * 
	 * @param search the search term.
	 * @param pageNumber the page number.
	 * @param pageSize the size of the page.
	 * @return list of all error data with matching search term for a particular page.
	 */
	@Override
	public List<ErrorInformation> getPagedErrorData(final String search, final Integer pageNumber, final Integer pageSize) {
		return errorDataDao.getPagedData(search, pageNumber, pageSize);
	}
	
	/**
	 * Return the archive data with the given id.
	 * 
	 * @param id the archive data id.
	 * @return the archive data with the matching id.
	 * @should return archive data with matching id.
	 * @should return null when no archive data with matching id.
	 */
	@Override
	public ArchiveInfo getArchiveData(final Integer id) {
		return getArchiveDataDao().getData(id);
	}
	
	/**
	 * Return the archive data with the given uuid.
	 * 
	 * @param uuid the archive data uuid.
	 * @return the archive data with the matching uuid.
	 * @should return archive data with matching uuid.
	 * @should return null when no archive data with matching uuid.
	 */
	@Override
	public ArchiveInfo getArchiveDataByUuid(final String uuid) {
		return getArchiveDataDao().getDataByUuid(uuid);
	}
	
	@Override
	public List<ArchiveInfo> getArchiveDataByFormDataUuid(final String formDataUuid) {
		return getArchiveDataDao().getAllDataByFormDataUuid(formDataUuid);
	}
	
	@Override
	public List<ErrorInformation> getErrorDataByFormDataUuid(final String formDataUuid) {
		return getErrorDataDao().getAllDataByFormDataUuid(formDataUuid);
	}
	
	@Override
	public List<AfyaStatQueueData> getQueueDataByFormDataUuid(final String formDataUuid) {
		return getQueueDataDao().getAllDataByFormDataUuid(formDataUuid);
	}
	
	/**
	 * Return all saved archive data.
	 * 
	 * @return all saved archive data.
	 * @should return empty list when no archive data are saved in the database.
	 * @should return all saved archive data.
	 */
	@Override
	public List<ArchiveInfo> getAllArchiveData() {
		return getArchiveDataDao().getAllData();
	}
	
	/**
	 * Save archive data into the database.
	 * 
	 * @param archiveData the archive data.
	 * @return saved archive data.
	 * @should save archive data into the database.
	 */
	@Override
	public ArchiveInfo saveArchiveData(final ArchiveInfo archiveData) {
		return getArchiveDataDao().saveData(archiveData);
	}
	
	/**
	 * Delete archive data from the database.
	 * 
	 * @param archiveData the archive data
	 * @should remove archive data from the database
	 */
	@Override
	public void purgeArchiveData(final ArchiveInfo archiveData) {
		getArchiveDataDao().purgeData(archiveData);
	}
	
	/**
	 * Get the total number of the archive data in the database with partial matching search term on
	 * the payload.
	 * 
	 * @param search the search term.
	 * @return the total number of the archive data in the database.
	 */
	@Override
	public Number countArchiveData(final String search) {
		return archiveDataDao.countData(search);
	}
	
	/**
	 * Get archive data with matching search term for a particular page.
	 * 
	 * @param search the search term.
	 * @param pageNumber the page number.
	 * @param pageSize the size of the page.
	 * @return list of all archive data with matching search term for a particular page.
	 */
	@Override
	public List<ArchiveInfo> getPagedArchiveData(final String search, final Integer pageNumber, final Integer pageSize) {
		return archiveDataDao.getPagedData(search, pageNumber, pageSize);
	}
	
	/**
	 * Return the data source with the given id.
	 * 
	 * @param id the data source id.
	 * @return the data source with the matching id.
	 * @should return data source with matching id.
	 * @should return null when no data source with matching id.
	 */
	@Override
	public AfyaDataSource getDataSource(final Integer id) {
		return getDataSourceDao().getById(id);
	}
	
	/**
	 * Return the data source with the given uuid.
	 * 
	 * @param uuid the data source uuid.
	 * @return the data source with the matching uuid.
	 * @should return data source with matching uuid.
	 * @should return null when no data source with matching uuid.
	 */
	@Override
	public AfyaDataSource getDataSourceByUuid(final String uuid) {
		return getDataSourceDao().getDataSourceByUuid(uuid);
	}
	
	/**
	 * Return all saved data source.
	 * 
	 * @return all saved data source .
	 * @should return empty list when no data source are saved in the database.
	 * @should return all saved data source.
	 */
	@Override
	public List<AfyaDataSource> getAllDataSource() {
		return getDataSourceDao().getAll();
	}
	
	/**
	 * Save data source into the database.
	 * 
	 * @param dataSource the data source.
	 * @return saved data source.
	 * @should save data source into the database.
	 */
	@Override
	public AfyaDataSource saveDataSource(final AfyaDataSource dataSource) {
		return getDataSourceDao().saveOrUpdate(dataSource);
	}
	
	/**
	 * Delete data source from the database.
	 * 
	 * @param dataSource the data source
	 * @should remove data source from the database
	 */
	@Override
	public void purgeDataSource(final AfyaDataSource dataSource) {
		getDataSourceDao().delete(dataSource);
	}
	
	/**
	 * Get the total number of the data source in the database with partial matching search term on
	 * the payload.
	 * 
	 * @param search the search term.
	 * @return the total number of the data source in the database.
	 */
	@Override
	public Number countDataSource(final String search) {
		return dataSourceDao.countDataSource(search);
	}
	
	/**
	 * Get data source with matching search term for a particular page.
	 * 
	 * @param search the search term.
	 * @param pageNumber the page number.
	 * @param pageSize the size of the page.
	 * @return list of all data source with matching search term for a particular page.
	 */
	@Override
	public List<AfyaDataSource> getPagedDataSource(final String search, final Integer pageNumber, final Integer pageSize) {
		return dataSourceDao.getPagedDataSources(search, pageNumber, pageSize);
	}
	
	/**
	 * Return the notification data with the given id.
	 * 
	 * @param id the notification data id.
	 * @return the notification data with the matching id.
	 * @should return notification data with matching id.
	 * @should return null when no notification data with matching id.
	 */
	@Override
	public NotificationInfo getNotificationData(final Integer id) {
		return getNotificationDataDao().getData(id);
	}
	
	/**
	 * Return the notification data with the given uuid.
	 * 
	 * @param uuid the notification data uuid.
	 * @return the notification data with the matching uuid.
	 * @should return notification data with matching uuid.
	 * @should return null when no notification data with matching uuid.
	 */
	@Override
	public NotificationInfo getNotificationDataByUuid(final String uuid) {
		return getNotificationDataDao().getDataByUuid(uuid);
	}
	
	/**
	 * Return all saved notification data.
	 * 
	 * @return all saved notification data.
	 * @should return empty list when no notification data are saved in the database.
	 * @should return all saved notification data.
	 */
	@Override
	public List<NotificationInfo> getAllNotificationData() {
		return getNotificationDataDao().getAllData();
	}
	
	/**
	 * Return paged notification data for a particular person with matching search term for a
	 * particular page.
	 * 
	 * @param search the search term.
	 * @param pageNumber the page number.
	 * @param pageSize the size of the page.
	 * @return all saved notification data.
	 * @should return empty list when no notification data are saved in the database.
	 * @should return all saved notification data.
	 */
	@Override
	public List<NotificationInfo> getNotificationDataByReceiver(final Person person, final String search,
	        final Integer pageNumber, final Integer pageSize, final String status, final Date syncDate) {
		return getNotificationDataDao().getNotificationsByReceiver(person, search, pageNumber, pageSize, status, syncDate);
	}
	
	/**
	 * Return paged notification data from a particular person with matching search term for a
	 * particular page.
	 * 
	 * @param search the search term.
	 * @param pageNumber the page number.
	 * @param pageSize the size of the page.
	 * @return all saved notification data.
	 * @should return empty list when no notification data are saved in the database.
	 * @should return all saved notification data.
	 */
	@Override
	public List<NotificationInfo> getNotificationDataBySender(final Person person, final String search,
	        final Integer pageNumber, final Integer pageSize, final String status, final Date syncDate) {
		return getNotificationDataDao().getNotificationsBySender(person, search, pageNumber, pageSize, status, syncDate);
	}
	
	/**
	 * Return count for the paged notification data for a particular person with matching search
	 * term for a particular page.
	 * 
	 * @param person the person.
	 * @param search the search term.
	 * @return all saved notification data.
	 * @should return empty list when no notification data are saved in the database.
	 * @should return all saved notification data.
	 */
	@Override
	public Number countNotificationDataByReceiver(final Person person, final String search, final String status) {
		return getNotificationDataDao().countNotificationsByReceiver(person, search, status);
	}
	
	/**
	 * Return count for the paged notification data from a particular person with matching search
	 * term for a particular page.
	 * 
	 * @param person the person.
	 * @param search the search term.
	 * @return all saved notification data.
	 * @should return empty list when no notification data are saved in the database.
	 * @should return all saved notification data.
	 */
	@Override
	public Number countNotificationDataBySender(final Person person, final String search, final String status) {
		return getNotificationDataDao().countNotificationsBySender(person, search, status);
	}
	
	@Override
	public List<NotificationInfo> getNotificationDataByRole(final Role role, final String search, final Integer pageNumber,
	        final Integer pageSize, final String status) {
		return getNotificationDataDao().getNotificationsByRole(role, search, pageNumber, pageSize, status);
	}
	
	@Override
	public Number countNotificationDataByRole(final Role role, final String search, final String status) {
		return getNotificationDataDao().countNotificationsByRole(role, search, status);
	}
	
	/**
	 * Save notification data into the database.
	 * 
	 * @param notificationData the notification data.
	 * @return saved notification data.
	 * @should save notification data into the database.
	 */
	@Override
	public NotificationInfo saveNotificationData(final NotificationInfo notificationData) {
		return getNotificationDataDao().saveOrUpdate(notificationData);
	}
	
	/**
	 * Delete notification data from the database.
	 * 
	 * @param notificationData the notification data
	 * @should remove notification data from the database
	 */
	@Override
	public void purgeNotificationData(final NotificationInfo notificationData) {
		getNotificationDataDao().purgeData(notificationData);
	}
	
	/**
	 * Void a single notification data.
	 * 
	 * @param notificationData the notification data to be voided.
	 * @return the voided notification data.
	 */
	@Override
	public NotificationInfo voidNotificationData(final NotificationInfo notificationData, final String reason) {
		notificationData.setVoided(Boolean.TRUE);
		notificationData.setVoidedBy(Context.getAuthenticatedUser());
		notificationData.setDateVoided(new Date());
		notificationData.setVoidReason(reason);
		return saveNotificationData(notificationData);
	}
	
	@Override
	public ErrorMessagesInfo getErrorMessage(Integer id) {
		return getErrorMessageDao().getById(id);
	}
	
	@Override
	public ErrorMessagesInfo getErrorMessageByUuid(String uuid) {
		return getErrorMessageDao().getDataByUuid(uuid);
	}
	
	@Override
	public List<ErrorMessagesInfo> getAllErrorMessage() {
		return getErrorMessageDao().getAll();
	}
	
	@Override
	public ErrorMessagesInfo saveErrorMessage(ErrorMessagesInfo errormessage) {
		return getErrorMessageDao().saveData(errormessage);
	}
	
	@Override
	public void purgeErrorMessage(ErrorMessagesInfo errormessage) {
		getErrorMessageDao().purgeData(errormessage);
	}
	
	@Override
	public Number countErrorMessage(String search) {
		return getErrorMessageDao().countData(search);
	}
	
	@Override
	public List<ErrorMessagesInfo> getPagedErrorMessage(String search, Integer pageNumber, Integer pageSize) {
		return null;
	}
	
	@Override
	public List<ErrorMessagesInfo> validateData(String uuid, String formData) {
		List<ErrorMessagesInfo> errorMessages = new ArrayList<ErrorMessagesInfo>();
		ErrorInformation errorData = getErrorDataByUuid(uuid);
		errorDataDao.detachDataFromHibernateSession(errorData);
		errorData.setPayload(formData);
		
		AfyaStatQueueData queueData = new AfyaStatQueueData(errorData);
		
		List<QueueInfoHandler> queueDataHandlers = HandlerUtil.getHandlersForType(QueueInfoHandler.class,
		    AfyaStatQueueData.class);
		for (QueueInfoHandler queueDataHandler : queueDataHandlers) {
			
			try {
				if (queueDataHandler.accept(queueData)) {
					queueDataHandler.validate(queueData);
				}
			}
			catch (Exception ex) {
				errorMessages = createErrorMessageList((StreamProcessorException) ex);
			}
		}
		
		return errorMessages;
	}
	
	@Override
	public List<String> getDiscriminatorTypes() {
		List<String> discriminatorTypes = new ArrayList<String>();
		List<QueueInfoHandler> queueDataHandlers = HandlerUtil.getHandlersForType(QueueInfoHandler.class,
		    AfyaStatQueueData.class);
		for (QueueInfoHandler queueDataHandler : queueDataHandlers) {
			String discriminator = queueDataHandler.getDiscriminator();
			// collect all discriminator value and return it to the web interface
			discriminatorTypes.add(discriminator);
		}
		return discriminatorTypes;
	}
	
	private List<ErrorMessagesInfo> createErrorMessageList(StreamProcessorException ex) {
		List<ErrorMessagesInfo> errorMessages = new ArrayList<ErrorMessagesInfo>();
		for (Exception exception : ex.getAllException()) {
			ErrorMessagesInfo error = new ErrorMessagesInfo();
			error.setMessage(exception.getMessage());
			errorMessages.add(error);
		}
		return errorMessages;
	}
	
	@Override
	public List<AfyaStatQueueData> mergeDuplicatePatient(@NotNull final String errorDataUuid,
	        @NotNull final String existingPatientUuid, @NotNull final String payload) {
		List<AfyaStatQueueData> requeued = new ArrayList<AfyaStatQueueData>();
		ErrorInformation errorData = this.getErrorDataByUuid(errorDataUuid);
		errorData.setPayload(payload);
		String submittedPatientUuid = errorData.getPatientUuid();
		
		errorData.setDiscriminator("json-demographics-update");
		
		errorData = this.saveErrorData(errorData);
		
		registerTemporaryUuid(submittedPatientUuid, existingPatientUuid);
		AfyaStatQueueData queueData = new AfyaStatQueueData(errorData);
		queueData = this.saveQueueData(queueData);
		this.purgeErrorData(errorData);
		requeued.add(queueData);
		
		// Fetch all ErrorData associated with the patient UUID (the one determined to be of a duplicate patient).
		int countOfErrors = this.countErrorData(submittedPatientUuid).intValue();
		List<ErrorInformation> allToRequeue = this.getPagedErrorData(submittedPatientUuid, 1, countOfErrors);
		for (ErrorInformation errorData1 : allToRequeue) {
			queueData = new AfyaStatQueueData(errorData1);
			queueData = this.saveQueueData(queueData);
			this.purgeErrorData(errorData1);
			requeued.add(queueData);
		}
		return requeued;
	}
	
	private void registerTemporaryUuid(final String temporaryUuid, final String permanentUuid) {
		RegistrationInfoService registrationDataService = Context.getService(RegistrationInfoService.class);
		RegistrationInfo registrationData = registrationDataService.getRegistrationDataByTemporaryUuid(temporaryUuid);
		if (registrationData == null) {
			registrationData = new RegistrationInfo();
			registrationData.setTemporaryUuid(temporaryUuid);
			registrationData.setAssignedUuid(permanentUuid);
			registrationDataService.saveRegistrationData(registrationData);
		}
	}
	
	public FormInfoStatus getFormDataStatusByFormDataUuid(String formDataUuid) {
		FormInfoStatus formDataStatus = new FormInfoStatus(formDataUuid);
		if (getArchiveDataByFormDataUuid(formDataUuid).size() > 0) {
			formDataStatus.setStatus("archived");
		} else if (getErrorDataByFormDataUuid(formDataUuid).size() > 0) {
			formDataStatus.setStatus("errored");
		} else if (getQueueDataByFormDataUuid(formDataUuid).size() > 0) {
			formDataStatus.setStatus("queued");
		} else {
			formDataStatus.setStatus("unknown");
		}
		return formDataStatus;
	}
}
