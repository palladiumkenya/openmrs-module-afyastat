package org.openmrs.module.afyastat.htmltojson;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.afyastat.metadata.AfyaStatMetadata;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.kenyacore.form.FormDescriptor;
import org.openmrs.module.kenyacore.form.FormManager;
import org.openmrs.module.kenyacore.form.FormUtils;
import org.openmrs.ui.framework.resource.ResourceFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class HtmlFormUtil {
	
	public static ObjectNode getFormSchemaJson(String formUuid, ResourceFactory resourceFactory) throws IOException {
		Form form = Context.getFormService().getFormByUuid(formUuid);
		ObjectNode questions = JsonNodeFactory.instance.objectNode();
		ArrayNode questionsList = JsonNodeFactory.instance.arrayNode();
		
		String pathToResourceDir = getGeneratedHtmlResourcePath();
		if (StringUtils.isBlank(pathToResourceDir)) {
			System.out.println("Please set the resources path for forms migration");
			return null;
		}
		
		if (form != null) {
			
			HtmlForm htmlForm = null;
			try {
				htmlForm = FormUtils.getHtmlForm(form, resourceFactory);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
			if (htmlForm != null) {
				String formHtml;
				String generateFileName = generateFileNameFromHtmlForm(form.getName()) + ".html";
				;
				generateFileName = pathToResourceDir + generateFileName;
				
				System.out.println("Generated file name: " + generateFileName);
				File file = new File(generateFileName);
				String content = FileUtils.readFileToString(file, "UTF-8");
				
				//formHtml = htmlForm.getXmlData();
				formHtml = content;
				Document doc = Jsoup.parse(formHtml);
				
				System.out.println("Form name: " + htmlForm.getName());
				Element htmlform = doc.select("htmlform").first();
				
				Elements elementTags = htmlform.select("obs,obsgroup");
				
				for (Element element : elementTags) {
					
					// check the tag name
					if (element.normalName().equals("obs")) {
						//System.out.println("Encountered obs element " + element.attr("conceptId"));
						boolean isGrouped = false;
						Elements parents = element.parents();
						for (Element parent : parents) {
							
							if (parent.normalName().equals("obsgroup")) {
								isGrouped = true;
								System.out.println("Parent tag: " + parent.tagName());
							}
						}
						
						if (isGrouped) {
							System.out.println("Skipping a grouped obs tag");
							continue;
						}
						HtmlFormDataPoint dataPoint = HtmlObsTagExtractor.extractObsTag(element);
						if (dataPoint == null) {
							System.out.println("Encountered invalid concept UUID in the form schema. Ignoring the tag ");
							continue;
						}
						HtmlFormObsRenderer renderer = new HtmlFormObsRenderer(dataPoint);
						ObjectNode obsJson = renderer.render();
						if (obsJson != null) {
							questionsList.add(obsJson);
						}
					} else if (element.normalName().equals("obsgroup")) {
						
						System.out.println("Encountered obsgroup element ");
						HtmlFormObsGroupRenderer obsGroupRenderer = new HtmlFormObsGroupRenderer(element);
						ObjectNode groupJson = obsGroupRenderer.render();
						if (groupJson != null) {
							questionsList.add(groupJson);
						}
					}
					
				}
			}
		}
		questions.put("questions", questionsList);
		return questions;
	}
	
	public static ArrayNode getAllForms(FormManager formManager, ResourceFactory resourceFactory) {
		
		List<FormDescriptor> formList = new ArrayList<FormDescriptor>(formManager.getAllFormDescriptors());
		
		/**
		 * The whitelist contains uuid of forms to be processed. TODO: expose the whitelist in a
		 * configurable object probably through a UI
		 */
		List<String> whiteList = Arrays.asList("e958f902-64df-4819-afd4-7fb061f59308",
		    "37f6bd8d-586a-4169-95fa-5781f987fe62", "59ed8e62-7f1f-40ae-a2e3-eabe350277ce",
		    "0038a296-62f8-4099-80e5-c9ea7590c157", "22c68f86-bbf0-49ba-b2d1-23fa7ccf0259",
		    "e4b506c1-7379-42b6-a374-284469cba8da", "83fb6ab2-faec-4d87-a714-93e77a28a201");
		
		/**
		 * The blacklist contains uuids of forms to be skipped during processing. TODO: expose this
		 * through a UI
		 */
		List<String> blackList = Arrays.asList("04295648-7606-11e8-adc0-fa7ae01bbebc");
		String formHtml;
		ArrayNode generatedFormList = JsonNodeFactory.instance.arrayNode();
		
		for (FormDescriptor formDescriptor : formList) {
			String targetUuid = formDescriptor.getTargetUuid();
			Form form = Context.getFormService().getFormByUuid(targetUuid);
			
			if (form != null) {
				
				HtmlForm htmlForm = null;
				try {
					htmlForm = FormUtils.getHtmlForm(form, resourceFactory);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				
				if (htmlForm != null /*&& whiteList.contains(targetUuid)*/) {
					System.out.println("Html Form: " + htmlForm.getUuid() + " , Name: " + htmlForm.getName());
					
					ObjectNode generatedFormEtl = JsonNodeFactory.instance.objectNode();
					
					HtmlFormToJsonSchemaModel schema = new HtmlFormToJsonSchemaModel(targetUuid, htmlForm.getName());
					
					formHtml = htmlForm.getXmlData();
					generatedFormEtl.put("uuid", targetUuid);
					generatedFormEtl.put("formName", htmlForm.getName());
					
					Document doc = Jsoup.parse(formHtml);
					for (Element element : doc.select("repeat")) {
						element.remove();
					}
					
					for (Element element : doc.select("obsgroup")) {
						element.remove();
					}
					
					Element htmlform = doc.select("htmlform").first();
					Elements obsTags = htmlform.select("obs");
					Set<HtmlFormDataPoint> dataPoints = new HashSet<HtmlFormDataPoint>();
					for (Element obsTag : obsTags) {
						String conceptUUId = obsTag.attr("conceptId");
						
						Concept concept = getConceptByUuidOrId(conceptUUId);
						
						if (concept == null) {
							System.out.println("Concept UUID Invalid: " + conceptUUId);
							continue;
						}
						HtmlFormDataPoint dataPoint = new HtmlFormDataPoint();
						String requiredField = obsTag.attr("required");
						if (StringUtils.isNotBlank(requiredField) && requiredField.equals("true")) {
							dataPoint.setRequiredField(true);
						}
						
						String cUuid = concept.getUuid();
						//String dataType = getConceptDatatype(concept);
						dataPoint.setConceptUUID(cUuid);
						
						dataPoint.setConceptId(concept.getConceptId());
						dataPoint.setConceptName(concept.getName().getName());
						dataPoint.setDataType("obs");
						dataPoints.add(dataPoint);
						
					}
					schema.setDataPoints(dataPoints);
					generatedFormEtl.put("dataPoints", String.valueOf(dataPoints.size()));
					generatedFormList.add(generatedFormEtl);
				}
			}
		}
		
		return generatedFormList;
	}
	
	public static boolean writeFormsToFileSystem(FormManager formManager, ResourceFactory resourceFactory) {
		
		String pathToResourceDir = getGeneratedHtmlResourcePath();
		if (StringUtils.isBlank(pathToResourceDir)) {
			System.out.println("Please set the resources path for forms migration");
			return false;
		}
		
		List<FormDescriptor> formList = new ArrayList<FormDescriptor>(formManager.getAllFormDescriptors());
		
		for (FormDescriptor formDescriptor : formList) {
			String targetUuid = formDescriptor.getTargetUuid();
			Form form = Context.getFormService().getFormByUuid(targetUuid);
			String formHtml;
			String generateFileName;
			
			if (form != null) {
				
				HtmlForm htmlForm = null;
				try {
					htmlForm = FormUtils.getHtmlForm(form, resourceFactory);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				
				if (htmlForm != null) {
					formHtml = htmlForm.getXmlData();
					generateFileName = generateFileNameFromHtmlForm(form.getName()) + ".html";
					generateFileName = pathToResourceDir + generateFileName;
					createFile(generateFileName, formHtml);
				}
			}
			
		}
		
		return true;
	}
	
	public static void createFile(String fileName, String formContent) {
		try {
			
			FileWriter myWriter = new FileWriter(fileName);
			BufferedWriter output = new BufferedWriter(myWriter);
			
			// Writes the string to the file
			output.write(formContent);
			output.flush();
			output.close();
			System.out.println("Successfully wrote to the file.");
		}
		catch (IOException e) {
			System.out.println("An error occurred while writing to " + fileName);
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets concept by ID or UUID
	 * 
	 * @param conceptIdentifier
	 * @return
	 */
	public static Concept getConceptByUuidOrId(String conceptIdentifier) {
		Concept concept = null;
		if (Context.getConceptService().getConceptByUuid(conceptIdentifier) != null) {
			concept = Context.getConceptService().getConceptByUuid(conceptIdentifier);
		} else {
			concept = Context.getConceptService().getConcept(conceptIdentifier);
		}
		return concept;
	}
	
	/**
	 * Gets concept datatype
	 * 
	 * @param concept
	 * @return String equivalent of the datatype
	 */
	public static String getConceptDatatype(Concept concept) {
		if (concept == null)
			return null;
		
		if (concept.getDatatype().isCoded()) {
			return "Coded";
		} else if (concept.getDatatype().isBoolean()) {
			return "Boolean";
		} else if (concept.getDatatype().isText()) {
			return "Text";
		} else if (concept.getDatatype().isDateTime() || concept.getDatatype().isDate()) {
			return "Datetime";
		} else if (concept.getDatatype().isNumeric()) {
			return "Numeric";
		}
		return null;
	}
	
	/**
	 * Return concept UUID for a concept
	 * 
	 * @param concept
	 * @return
	 */
	public static String getConceptUuid(Concept concept) {
		return concept.getUuid();
	}
	
	public static String generateFileNameFromHtmlForm(String formName) {
		if (StringUtils.isBlank(formName)) {
			return null;
		}
		
		formName = formName.replaceAll("\\s+", "_");
		formName = formName.replaceAll("[-+.^:,]", "_");
		formName = formName.replaceAll("'", "_");
		formName = formName.replaceAll("\\(", "_");
		formName = formName.replaceAll("\\)", "_");
		;
		formName.toLowerCase();
		return formName;
	}
	
	public static String getGeneratedHtmlResourcePath() {
		GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(
		    AfyaStatMetadata.GENERATED_HTML_RESOURCE_PATH);
		if (globalPropertyObject == null) {
			return null;
		}
		
		if (globalPropertyObject.getValue() != null) {
			try {
				String pathValue = globalPropertyObject.getValue().toString();
				return pathValue;
			}
			catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	
}
