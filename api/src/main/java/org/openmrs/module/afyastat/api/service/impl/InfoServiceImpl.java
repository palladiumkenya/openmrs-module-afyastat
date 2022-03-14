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

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.afyastat.api.db.AfyaDataSourceDao;
import org.openmrs.module.afyastat.api.db.AfyaStatQueueDataDao;
import org.openmrs.module.afyastat.api.db.ArchiveInfoDao;
import org.openmrs.module.afyastat.api.db.ErrorInfoDao;
import org.openmrs.module.afyastat.api.db.ErrorMessagesInfoDao;
import org.openmrs.module.afyastat.api.db.NotificationInfoDao;
import org.openmrs.module.afyastat.api.service.InfoService;
import org.openmrs.module.afyastat.api.service.RegistrationInfoService;
import org.openmrs.module.afyastat.exception.StreamProcessorException;
import org.openmrs.module.afyastat.metadata.AfyaStatMetadata;
import org.openmrs.module.afyastat.model.AfyaDataSource;
import org.openmrs.module.afyastat.model.AfyaStatQueueData;
import org.openmrs.module.afyastat.model.ArchiveInfo;
import org.openmrs.module.afyastat.model.ErrorInfo;
import org.openmrs.module.afyastat.model.ErrorMessagesInfo;
import org.openmrs.module.afyastat.model.FormInfoStatus;
import org.openmrs.module.afyastat.model.RegistrationInfo;
import org.openmrs.module.afyastat.model.handler.QueueInfoHandler;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.util.HandlerUtil;

/**
 */
public class InfoServiceImpl extends BaseOpenmrsService implements InfoService {
	
	private ErrorInfoDao errorInfoDao;
	
	private AfyaStatQueueDataDao afyaStatQueueDataDao;
	
	private ArchiveInfoDao archiveInfoDao;
	
	private AfyaDataSourceDao afyaDataSourceDao;
	
	private NotificationInfoDao notificationInfoDao;
	
	private ErrorMessagesInfoDao errorMessagesInfoDao;
	
	public AfyaStatQueueDataDao getAfyaStatQueueDataDao() {
		return afyaStatQueueDataDao;
	}
	
	public void setAfyaStatQueueDataDao(final AfyaStatQueueDataDao afyaStatQueueDataDao) {
		this.afyaStatQueueDataDao = afyaStatQueueDataDao;
	}
	
	public ErrorInfoDao getErrorInfoDao() {
		return errorInfoDao;
	}
	
	public void setErrorInfoDao(final ErrorInfoDao errorDataDao) {
		this.errorInfoDao = errorDataDao;
	}
	
	public ArchiveInfoDao getArchiveInfoDao() {
		return archiveInfoDao;
	}
	
	public void setArchiveInfoDao(final ArchiveInfoDao archiveInfoDao) {
		this.archiveInfoDao = archiveInfoDao;
	}
	
	public AfyaDataSourceDao getAfyaDataSourceDao() {
		return afyaDataSourceDao;
	}
	
	public void setAfyaDataSourceDao(final AfyaDataSourceDao afyaDataSourceDao) {
		this.afyaDataSourceDao = afyaDataSourceDao;
	}
	
	public NotificationInfoDao getNotificationInfoDao() {
		return notificationInfoDao;
	}
	
	public void setNotificationInfoDao(final NotificationInfoDao notificationDataDao) {
		this.notificationInfoDao = notificationDataDao;
	}
	
	public ErrorMessagesInfoDao getErrorMessagesInfoDao() {
		return errorMessagesInfoDao;
	}
	
	public void setErrorMessagesInfoDao(final ErrorMessagesInfoDao errorMessagesInfoDao) {
		this.errorMessagesInfoDao = errorMessagesInfoDao;
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
		return getAfyaStatQueueDataDao().getData(id);
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
		return getAfyaStatQueueDataDao().getDataByUuid(uuid);
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
		return getAfyaStatQueueDataDao().getAllData();
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
		return getAfyaStatQueueDataDao().saveData(formData);
	}
	
	/**
	 * Delete form data from the database.
	 * 
	 * @param formData the form data
	 * @should remove form data from the database
	 */
	@Override
	public void purgeQueueData(final AfyaStatQueueData formData) {
		getAfyaStatQueueDataDao().purgeData(formData);
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
		return afyaStatQueueDataDao.countData(search);
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
		return afyaStatQueueDataDao.getPagedData(search, pageNumber, pageSize);
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
	public ErrorInfo getErrorData(final Integer id) {
		return getErrorInfoDao().getData(id);
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
	public ErrorInfo getErrorDataByUuid(final String uuid) {
		return getErrorInfoDao().getDataByUuid(uuid);
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
	public ErrorInfo getRegistrationErrorDataByPatientUuid(String patientUuid) {
		
		List<ErrorInfo> errors = getErrorInfoDao().getPagedData(patientUuid, null, null);
		for (ErrorInfo errorData : errors) {
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
	public List<ErrorInfo> getAllErrorData() {
		return getErrorInfoDao().getAllData();
	}
	
	/**
	 * Save error data into the database.
	 * 
	 * @param errorInfo the error data.
	 * @return saved error data.
	 * @should save error data into the database.
	 */
	@Override
	public ErrorInfo saveErrorData(final ErrorInfo errorInfo) {
		return getErrorInfoDao().saveData(errorInfo);
	}
	
	/**
	 * Delete error data from the database.
	 * 
	 * @param errorInfo the error data
	 * @should remove error data from the database
	 */
	@Override
	public void purgeErrorData(final ErrorInfo errorInfo) {
		getErrorInfoDao().purgeData(errorInfo);
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
		return errorInfoDao.countData(search);
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
	public List<ErrorInfo> getPagedErrorData(final String search, final Integer pageNumber, final Integer pageSize) {
		return errorInfoDao.getPagedData(search, pageNumber, pageSize);
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
		return getArchiveInfoDao().getData(id);
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
		return getArchiveInfoDao().getDataByUuid(uuid);
	}
	
	@Override
	public List<ArchiveInfo> getArchiveDataByFormDataUuid(final String formDataUuid) {
		return getArchiveInfoDao().getAllDataByFormDataUuid(formDataUuid);
	}
	
	@Override
	public ArchiveInfo getArchiveDataByFormDataUuidDateFormFilledAndPatientUuid(final String formDataUuid,
	        final Long dateFormFilled, String patientUuid) {
		return getArchiveInfoDao().getDataByFormDataUuidDateFormFilledAndPatientUuid(formDataUuid, dateFormFilled,
		    patientUuid);
	}
	
	@Override
	public List<ErrorInfo> getErrorDataByFormDataUuid(final String formDataUuid) {
		return getErrorInfoDao().getAllDataByFormDataUuid(formDataUuid);
	}
	
	@Override
	public ErrorInfo getErrorDataByFormDataUuiDateFormFilledAndPatientUuid(final String formDataUuid,
	        final Long dateFormFilled, String patientUuid) {
		return getErrorInfoDao()
		        .getDataByFormDataUuidDateFormFilledAndPatientUuid(formDataUuid, dateFormFilled, patientUuid);
	}
	
	@Override
	public List<AfyaStatQueueData> getQueueDataByFormDataUuid(final String formDataUuid) {
		return getAfyaStatQueueDataDao().getAllDataByFormDataUuid(formDataUuid);
	}
	
	@Override
	public AfyaStatQueueData getQueueDataByFormDataUuidDateFormFilledAndPatientUuid(final String formDataUuid,
	        final Long dateFormFiled, String patientUuid) {
		return getAfyaStatQueueDataDao().getDataByFormDataUuidDateFormFilledAndPatientUuid(formDataUuid, dateFormFiled,
		    patientUuid);
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
		return getArchiveInfoDao().getAllData();
	}
	
	/**
	 * Save archive data into the database.
	 * 
	 * @param archiveInfo the archive data.
	 * @return saved archive data.
	 * @should save archive data into the database.
	 */
	@Override
	public ArchiveInfo saveArchiveData(final ArchiveInfo archiveInfo) {
		return getArchiveInfoDao().saveData(archiveInfo);
	}
	
	/**
	 * Delete archive data from the database.
	 * 
	 * @param archiveInfo the archive data
	 * @should remove archive data from the database
	 */
	@Override
	public void purgeArchiveData(final ArchiveInfo archiveInfo) {
		getArchiveInfoDao().purgeData(archiveInfo);
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
		return archiveInfoDao.countData(search);
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
		return archiveInfoDao.getPagedData(search, pageNumber, pageSize);
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
		return getAfyaDataSourceDao().getById(id);
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
		return getAfyaDataSourceDao().getDataSourceByUuid(uuid);
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
		return getAfyaDataSourceDao().getAll();
	}
	
	/**
	 * Save data source into the database.
	 * 
	 * @param afyaDataSource the data source.
	 * @return saved data source.
	 * @should save data source into the database.
	 */
	@Override
	public AfyaDataSource saveDataSource(final AfyaDataSource afyaDataSource) {
		return getAfyaDataSourceDao().saveOrUpdate(afyaDataSource);
	}
	
	/**
	 * Delete data source from the database.
	 * 
	 * @param dataSource the data source
	 * @should remove data source from the database
	 */
	@Override
	public void purgeDataSource(final AfyaDataSource dataSource) {
		getAfyaDataSourceDao().delete(dataSource);
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
		return afyaDataSourceDao.countDataSource(search);
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
		return afyaDataSourceDao.getPagedDataSources(search, pageNumber, pageSize);
	}
	
	@Override
	public ErrorMessagesInfo getErrorMessage(Integer id) {
		return getErrorMessagesInfoDao().getById(id);
	}
	
	@Override
	public ErrorMessagesInfo getErrorMessageByUuid(String uuid) {
		return getErrorMessagesInfoDao().getDataByUuid(uuid);
	}
	
	@Override
	public List<ErrorMessagesInfo> getAllErrorMessage() {
		return getErrorMessagesInfoDao().getAll();
	}
	
	@Override
	public ErrorMessagesInfo saveErrorMessage(ErrorMessagesInfo errorMessagesInfo) {
		return getErrorMessagesInfoDao().saveData(errorMessagesInfo);
	}
	
	@Override
	public void purgeErrorMessage(ErrorMessagesInfo errorMessagesInfo) {
		getErrorMessagesInfoDao().purgeData(errorMessagesInfo);
	}
	
	@Override
	public Number countErrorMessage(String search) {
		return getErrorMessagesInfoDao().countData(search);
	}
	
	@Override
	public List<ErrorMessagesInfo> getPagedErrorMessage(String search, Integer pageNumber, Integer pageSize) {
		return null;
	}
	
	@Override
	public List<ErrorMessagesInfo> validateData(String uuid, String formData) {
		List<ErrorMessagesInfo> errorMessages = new ArrayList<ErrorMessagesInfo>();
		ErrorInfo errorInfo = getErrorDataByUuid(uuid);
		errorInfoDao.detachDataFromHibernateSession(errorInfo);
		errorInfo.setPayload(formData);
		
		AfyaStatQueueData queueData = new AfyaStatQueueData(errorInfo);
		
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
		List<ErrorMessagesInfo> errorMessagesInfos = new ArrayList<ErrorMessagesInfo>();
		for (Exception exception : ex.getAllException()) {
			ErrorMessagesInfo error = new ErrorMessagesInfo();
			error.setMessage(exception.getMessage());
			errorMessagesInfos.add(error);
		}
		return errorMessagesInfos;
	}
	
	@Override
	public List<AfyaStatQueueData> mergeDuplicatePatient(@NotNull final String errorDataUuid,
	        @NotNull final String existingPatientUuid, @NotNull final String payload) {
		List<AfyaStatQueueData> requeued = new ArrayList<AfyaStatQueueData>();
		ErrorInfo errorInfo = this.getErrorDataByUuid(errorDataUuid);
		errorInfo.setPayload(payload);
		String submittedPatientUuid = errorInfo.getPatientUuid();
		
		//errorInfo.setDiscriminator("json-demographics-update");
		errorInfo.setVoided(true);
		errorInfo.setVoidReason("Merged with an existing registration");
		
		errorInfo = this.saveErrorData(errorInfo);
		
		registerTemporaryUuid(submittedPatientUuid, existingPatientUuid);
		AfyaStatQueueData afyaStatQueueData = new AfyaStatQueueData(errorInfo);
		this.purgeErrorData(errorInfo);
		
		// add CHT ref to the existing patient if at all it doesn't exist
		Patient existingPatient = Context.getPatientService().getPatientByUuid(existingPatientUuid);
		addChtReferenceToExistingPatient(existingPatient, submittedPatientUuid);
		
		// Fetch all ErrorData associated with the patient UUID (the one determined to be of a duplicate patient).
		int countOfErrors = this.countErrorData(submittedPatientUuid).intValue();
		List<ErrorInfo> allToRequeue = this.getPagedErrorData(submittedPatientUuid, 1, countOfErrors);
		for (ErrorInfo errorData1 : allToRequeue) {
			afyaStatQueueData = new AfyaStatQueueData(errorData1);
			afyaStatQueueData = this.saveQueueData(afyaStatQueueData);
			this.purgeErrorData(errorData1);
			requeued.add(afyaStatQueueData);
		}
		return requeued;
	}
	
	/**
	 * Add CHT reference to a client
	 * 
	 * @param existingPatient
	 * @param submittedPatientUuid
	 */
	private void addChtReferenceToExistingPatient(Patient existingPatient, String submittedPatientUuid) {
		PatientIdentifierType chtPit = Context.getPatientService().getPatientIdentifierTypeByUuid(
		    AfyaStatMetadata._PatientIdentifierType.CHT_RECORD_UUID);
		// check if patient already has CHT ref and prefer it over the new one
		
		for (PatientIdentifier identifier : existingPatient.getActiveIdentifiers()) {
			if (identifier.getIdentifierType().equals(chtPit)) { // the patient already has a CHT ref. Do nothing
				return;
			}
		}
		PatientIdentifier chtRef = new PatientIdentifier();
		chtRef.setIdentifierType(chtPit);
		chtRef.setIdentifier(submittedPatientUuid);
		chtRef.setLocation(Context.getService(KenyaEmrService.class).getDefaultLocation());
		chtRef.setPatient(existingPatient);
		Context.getPatientService().savePatientIdentifier(chtRef);
		
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
	
	@Override
	public void reQueueErrors(String errorList) {
		if (Context.isAuthenticated()) {
			
			if (errorList.equals("all")) {
				List<ErrorInfo> errors = getAllErrorData();
				
				for (ErrorInfo errorData : errors) {
					AfyaStatQueueData queueData = new AfyaStatQueueData(errorData);
					saveQueueData(queueData);
					purgeErrorData(errorData);
				}
			} else {
				String[] uuidList = errorList.split(",");
				for (String uuid : uuidList) {
					ErrorInfo errorData = getErrorDataByUuid(uuid);
					AfyaStatQueueData queueData = new AfyaStatQueueData(errorData);
					saveQueueData(queueData);
					purgeErrorData(errorData);
				}
			}
		}
	}
	
	@Override
	public void purgeErrors(String errorList) {
		if (Context.isAuthenticated()) {
			
			if (errorList.equals("all")) {
				List<ErrorInfo> errors = getAllErrorData();
				
				for (ErrorInfo errorData : errors) {
					purgeErrorData(errorData);
				}
			} else {
				String[] uuidList = errorList.split(",");
				for (String uuid : uuidList) {
					ErrorInfo errorData = getErrorDataByUuid(uuid);
					purgeErrorData(errorData);
				}
			}
		}
	}
	
	@Override
	public void createAsNewRegistration(String queueUuid) {
		if (Context.isAuthenticated()) {
			ErrorInfo errorData = getErrorDataByUuid(queueUuid);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = null;
			try {
				jsonNode = objectMapper.readTree(errorData.getPayload());
			}
			catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			
			String submittedPatientUuid = errorData.getPatientUuid();
			
			ObjectNode objectNode = (ObjectNode) jsonNode;
			objectNode.put("skipPatientMatching", "true");
			
			AfyaStatQueueData queueData = new AfyaStatQueueData(errorData);
			queueData.setPayload(objectNode.toString());
			
			errorData.setVoided(true);
			errorData.setVoidReason("To be created as new registration");
			
			errorData = saveErrorData(errorData);
			saveQueueData(queueData);
			purgeErrorData(errorData);
			
			// Fetch all ErrorData associated with the patient UUID (the one determined to be of a duplicate patient).
			int countOfErrors = countErrorData(submittedPatientUuid).intValue();
			if (countOfErrors > 0) {
				List<ErrorInfo> allToRequeue = getPagedErrorData(submittedPatientUuid, 1, countOfErrors);
				for (ErrorInfo errorData1 : allToRequeue) {
					AfyaStatQueueData afyaStatQueueData = new AfyaStatQueueData(errorData1);
					afyaStatQueueData = saveQueueData(afyaStatQueueData);
					saveQueueData(afyaStatQueueData);
					purgeErrorData(errorData1);
				}
			}
		}
	}
}
