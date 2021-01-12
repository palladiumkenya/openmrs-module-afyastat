package org.openmrs.module.afyastat.api;

import org.openmrs.BaseOpenmrsData;

import java.util.Date;
import java.util.UUID;

public class ContactTrace extends BaseOpenmrsData {
	
	private Integer id;
	
	private String uuid;
	
	private PatientContact patientContact;
	
	private String contactType;
	
	private String status;
	
	private String reasonUncontacted;
	
	private String uniquePatientNo;
	
	private String facilityLinkedTo;
	
	private String healthWorkerHandedTo;
	
	private String remarks;
	
	private Date date;
	
	private Date appointmentDate;
	
	public ContactTrace() {
		prePersist();
	}
	
	public ContactTrace(String uuid, String contactType, String status, String uniquePatientNo, String facilityLinkedTo,
	    String healthWorkerHandedTo, String remarks, Date traceDate, Date appointmentDate) {
		this.uuid = uuid;
		this.contactType = contactType;
		this.status = status;
		this.reasonUncontacted = reasonUncontacted;
		this.uniquePatientNo = uniquePatientNo;
		this.facilityLinkedTo = facilityLinkedTo;
		this.healthWorkerHandedTo = healthWorkerHandedTo;
		this.remarks = remarks;
		this.date = traceDate;
		this.appointmentDate = appointmentDate;
	}
	
	public void prePersist() {
		
		if (null == getUuid())
			setUuid(UUID.randomUUID().toString());
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public PatientContact getPatientContact() {
		return patientContact;
	}
	
	public void setPatientContact(PatientContact patientContact) {
		this.patientContact = patientContact;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getContactType() {
		return contactType;
	}
	
	public void setContactType(String contactType) {
		this.contactType = contactType;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getReasonUncontacted() {
		return reasonUncontacted;
	}
	
	public void setReasonUncontacted(String reasonUncontacted) {
		this.reasonUncontacted = reasonUncontacted;
	}
	
	public String getUniquePatientNo() {
		return uniquePatientNo;
	}
	
	public void setUniquePatientNo(String uniquePatientNo) {
		this.uniquePatientNo = uniquePatientNo;
	}
	
	public String getFacilityLinkedTo() {
		return facilityLinkedTo;
	}
	
	public void setFacilityLinkedTo(String facilityLinkedTo) {
		this.facilityLinkedTo = facilityLinkedTo;
	}
	
	public String getHealthWorkerHandedTo() {
		return healthWorkerHandedTo;
	}
	
	public void setHealthWorkerHandedTo(String healthWorkerHandedTo) {
		this.healthWorkerHandedTo = healthWorkerHandedTo;
	}
	
	public String getRemarks() {
		return remarks;
	}
	
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public Date getAppointmentDate() {
		return appointmentDate;
	}
	
	public void setAppointmentDate(Date appointmentDate) {
		this.appointmentDate = appointmentDate;
	}
}
