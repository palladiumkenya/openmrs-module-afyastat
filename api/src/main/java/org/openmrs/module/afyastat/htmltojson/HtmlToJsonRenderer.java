package org.openmrs.module.afyastat.htmltojson;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HtmlToJsonRenderer {
	
	private Set<Integer> conceptIds = new HashSet<Integer>();
	
	/**
	 * Generates a complete DDL statement for a form. It has split query sections into: header,
	 * body, tail and standard fields.
	 * 
	 * @param schema
	 * @return
	 */
	
	public String generateQuery(HtmlFormToJsonSchemaModel schema) {
		if (schema == null)
			return null;
		StringBuilder builder = new StringBuilder();
		Set<HtmlFormDataPoint> dataPoints = schema.getDataPoints();
		/*String generatedTableName = QueryTemplate.DATABASE_NAME.concat(".").concat(schema.getGeneratedTableName());
		builder.append(getQueryHeader(generatedTableName, dataPoints))
		        .append(buildQueryBody(dataPoints))
		        .append(buildQueryTail(dataPoints))
		        .append(constructFromStatement())
		        .append(constructJoin());*/
		
		// prepare substitution objects
		/*Map<String, List<String>> subs = new HashMap<String, List<String>>();
		subs.put("formUUIDs", Arrays.asList(schema.getFormUUID()));
		subs.put("conceptList", getConceptsAsString());*/
		
		String substituedString = "";//doSubsitution(builder.toString(), subs);
		return substituedString;
	}
	
	/**
	 * Refer to DDLTemplate for an equivalent method
	 * 
	 * @param tableName
	 * @param dataPoints
	 * @return
	 */
	private String getQueryHeader(String tableName, Set<HtmlFormDataPoint> dataPoints) {
		if (tableName == null)
			return null;
		StringBuilder header = new StringBuilder();
		header.append("insert into :tableName ( \n".replace(":tableName", tableName));
		header.append(getStandardDeclarationFields());
		/*for (FormDataPoint dataPoint : dataPoints) {
		    header.append("\t").append(dataPoint.getGeneratedName().toLowerCase()).append(", \n");
		}*/
		header.append("\tvoided\n").append(")\n");
		return header.toString();
		
	}
	
	/**
	 * Refer to DDLTemplate for an equivalent method
	 * 
	 * @param dataPoints
	 * @return
	 */
	private String buildQueryBody(Set<HtmlFormDataPoint> dataPoints) {
		if (dataPoints == null || dataPoints.size() == 0)
			return null;
		// iterate through data points
		StringBuilder builder = new StringBuilder();
		builder.append(getStandardFromFields());
		/*for (HtmlFormDataPoint dataPoint : dataPoints) {
		    builder.append("\t")
		            .append(getTypeFromDataPoint(dataPoint))
		            .append(", \n");
		    conceptIds.add(dataPoint.getConceptId());
		}*/
		return builder.toString();
		
	}
	
	/**
	 * Refer to DDLTemplate for an equivalent method
	 * 
	 * @param dataPoint
	 * @return
	 */
	private String getTypeFromDataPoint(HtmlFormDataPoint dataPoint) {
		if (dataPoint == null || null == dataPoint.getDataType() || "".equals(dataPoint.getDataType()))
			return null;
		String pointDataType = dataPoint.getDataType();
		StringBuilder builder = new StringBuilder();
		if (pointDataType.equals("Coded")) {
			builder.append("max(if(o.concept_id=:conceptId,o.value_coded,null))").append(" as ")
			        .append(dataPoint.getGeneratedName().toLowerCase());
		} else if (pointDataType.equals("Boolean")) {
			builder.append("max(if(o.concept_id=:conceptId,o.value_coded,null))").append(" as ")
			        .append(dataPoint.getGeneratedName().toLowerCase());
		} else if (pointDataType.equals("Datetime")) {
			builder.append("max(if(o.concept_id=:conceptId,o.value_datetime,null))").append(" as ")
			        .append(dataPoint.getGeneratedName().toLowerCase());
		} else if (pointDataType.equals("Text")) {
			builder.append("max(if(o.concept_id=:conceptId,o.value_text,null))").append(" as ")
			        .append(dataPoint.getGeneratedName().toLowerCase());
		} else if (pointDataType.equals("Numeric")) {
			builder.append("max(if(o.concept_id=:conceptId,o.value_numeric,null))").append(" as ")
			        .append(dataPoint.getGeneratedName().toLowerCase());
		}
		
		return builder.toString().replace(":conceptId", String.valueOf(dataPoint.getConceptId()));
	}
	
	/**
	 * Refer to DDLTemplate for an equivalent method
	 * 
	 * @param dataPoints
	 * @return
	 */
	private String buildQueryTail(Set<HtmlFormDataPoint> dataPoints) {
		if (dataPoints == null || dataPoints.size() == 0)
			return null;
		return "\te.voided\n";
	}
	
	/**
	 * Refer to DDLTemplate for an equivalent method
	 * 
	 * @return
	 */
	private String getStandardFromFields() {
		StringBuilder builder = new StringBuilder();
		builder.append("select \n").append("\te.patient_id").append(", \n").append("\te.visit_id").append(", \n")
		        .append("\tdate(e.encounter_datetime)").append(" as visit_date, \n").append("\te.location_id")
		        .append(", \n").append("\te.encounter_id").append(" as encounter_id, \n").append("\te.creator")
		        .append(", \n").append("\te.date_created").append(" as date_created, \n");
		
		return builder.toString();
		
	}
	
	private String getStandardDeclarationFields() {
		StringBuilder builder = new StringBuilder();
		builder.append("\tpatient_id").append(", \n").append("\tvisit_id").append(", \n").append("\tvisit_date")
		        .append(", \n").append("\tlocation_id").append(", \n").append("\tencounter_id").append(", \n")
		        .append("\tcreator").append(", \n").append("\tdate_created").append(", \n");
		
		return builder.toString();
		
	}
	
	private String constructFromStatement() {
		StringBuilder builder = new StringBuilder();
		builder.append("from encounter e ");
		return builder.toString();
	}
	
	private String constructJoin() {
		StringBuilder builder = new StringBuilder();
		builder.append("inner join\n");
		builder.append("(\n");
		builder.append("\tselect form_id from form where uuid in(':formUUIDs')");
		builder.append(") f on f.form_id=e.form_id \n");
		builder.append("left outer join obs o on o.encounter_id=e.encounter_id");
		builder.append(" and o.concept_id in (:conceptList)\n");
		builder.append("where e.voided=0\n");
		builder.append("group by e.patient_id, e.encounter_id, visit_date");
		return builder.toString();
	}
	
	private String doSubsitution(String queryString, Map<String, List<String>> parameters) {
		
		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			String k = entry.getKey();
			List<String> v = entry.getValue();
			// build string from values
			String values = StringUtils.join(v, ",");
			String toReplace = ":".concat(k);
			queryString = queryString.replace(toReplace, values);
		}
		
		return queryString;
		
	}
	
	private List<String> getConceptsAsString() {
		List<String> cs = new ArrayList<String>();
		for (Integer c : conceptIds) {
			cs.add(String.valueOf(c));
		}
		return cs;
	}
	
	public Set<Integer> getConceptIds() {
		return conceptIds;
	}
	
	public void setConceptIds(Set<Integer> conceptIds) {
		this.conceptIds = conceptIds;
	}
}
