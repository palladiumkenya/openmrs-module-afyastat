openmrs-module-afyastat
afyastat API
==========================

Description
-----------
This module handles data exchange between CHT powered Afyastat and KenyaEMR.
These are REST services used to receive data from afyastat server.
Afyastat is built on top of CHT Web application which is a single page application built with Angular and NgRx frameworks.
This is a very basic module which can be used as a starting point in creating a new module.

Building from Source
--------------------
You will need to have Java 1.6+ and Maven 2.x+ installed.  Use the command 'mvn package' to 
compile and package the module.  The .omod file will be in the omod/target folder.

Alternatively you can add the snippet provided in the [Creating Modules](https://wiki.openmrs.org/x/cAEr) page to your 
omod/pom.xml and use the mvn command:

    mvn package -P deploy-web -D deploy.path="../../openmrs-1.8.x/webapp/src/main/webapp"

It will allow you to deploy any changes to your web 
resources such as jsp or js files without re-installing the module. The deploy path says 
where OpenMRS is deployed.

Installation
------------
1. Build the module to produce the .omod file.
2. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file.

If uploads are not allowed from the web (changable via a runtime property), you can drop the omod
into the ~/.OpenMRS/modules folder.  (Where ~/.OpenMRS is assumed to be the Application 
Data Directory that the running openmrs is currently using.)  After putting the file in there 
simply restart OpenMRS/tomcat and the module will be loaded and started.

Accreditation
-------------
* Data exchange processing logic adapted from the OpenMRS muzima core module, and used under MPL 2.0 (https://github.com/muzima/openmrs-module-muzimacore)
```
Contact Tracing API
------------
This is used to sync contact tracing data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as encounter api above


Peer Calendar API
------------
This is used to sync peer calendar data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as encounter api above


afyastat API
==========================

Description
-----------
These are REST services used to receive data from afyastat server.
Afyastat is built on top of CHT Web application which is a single page application built with Angular and NgRx frameworks.


End points and what they process:
--------------------
•	Demographics API (medicregistration)
•	Encounter API (medicformsdata)
•	Contact tracing API (mediccontacttracedata)
•	Peer Calendar API (medicpeercalendar )
•	Demographic update API (medicdemographicupdates)


Demographic API
------------
This is used to sync registration data from afyastat to kenyaEMR. This API takes and process payload from CouchDb in the browser which is is a JSON-based NoSQL and insert the same to openmrs.person table.
Example of Demographic API syntax
```

@RequestMapping(
    method = {RequestMethod.POST},
    value = {"/medicregistration"}
)
```

Example of JSON payload.


```
{
  "_id": "a191a0d7-66c6-41c9-adbd-c672a67827c2",
  "_rev": "1-940690c286f5a57e577d8973aa90efaf",
  "type": "contact",
  "contact_type": "universal_client",
  "parent": {
    "_id": "c1143892-ccfb-4fa5-95f0-e6fe7ab1b774"
  },
  "role": "client",
  "record_originator": "cht",
  "new_registration": "yes",
  "n_demographics": "",
  "patient_familyName": "Xxxxx",
  "patient_firstName": "Zzzzz",
  "patient_middleName": "Yyyyy",
  "name": "Xxxxx Yyyyy Zzzzz",
  "patient_name": "Xxxxx Yyyyy Zzzzz",
  "patient_idNumbers": "",
  "patient_identifierType_nationalId_49af6cdc-7968-4abb-bf46-de10d7f4859f": "1234567890",
  "patient_identifierType_clientID_9a260a8c-b94d-11eb-8529-0242ac130003": "1234567890",
  "id_delimiter": "",
  "patient_sex": "male",
  "patient_dobKnown": "_1066_No_99DCT",
  "patient_ageYears": "23",
  "ephemeral_years": "1997",
  "month_today": "08",
  "day_today": "30",
  "adjusted_yob": "1997",
  "dob_approx": "1997-06-16T00:00:00.000+03:00",
  "dob_raw": "1997-06-16T00:00:00.000+03:00",
  "dob_iso": "1997-06-15",
  "age_in_years": "0",
  "age_in_months": "290",
  "patient_birthDate": "1997-06-15",
  "calculated_age": "24",
  "head_of_household": "",
  "patient_marital_status": "_5555_marriedMonogomous_99DCT",
  "patient_occupation": "_1540_employee_99DCT",
  "patient_education_level": "_159785_collegeUniversityPolytechnic_99DCT",
  "demographics_delimiter": "",
  "n_address": "",
  "patient_nationality": "Kenya",
  "patient_telephone": "+254712345678",
  "patient_alternatePhone": "+254712345679",
  "patient_postalAddress": "Test",
  "patient_emailAddress": "test@gmailcom",
  "patient_county": "Nyeri",
  "patient_subcounty": "Kieni",
  "patient_town": "",
  "patient_ward": "Gatarakwa",
  "patient_location": "Test_Location",
  "patient_sublocation": "Test_Sub-location",
  "patient_village": "Test_Residence",
  "patient_landmark": "Test_Land_Mark",
  "patient_residence": "Test",
  "patient_nearesthealthcentre": "Test_facility",
  "address_delimiter": "",
  "n_next_of_kin_details": "",
  "patient_nextofkin": "Test Kin",
  "patient_nextofkinRelation": "Partner",
  "patient_nextofkinRelationship": "Partner",
  "patient_nextOfKinPhonenumber": "+254700000000",
  "patient_nextOfKinPostaladdress": "Test Post",
  "relationship_delimiter": "",
  "relationship_to_existing_contact": "",
  "need_linkage_to_existing_client": "_1066_No_99DCT",
  "relation_uuid": "",
  "relation_name": "",
  "relation_type": "",
  "meta": {
    "created_by": "clinician",
    "created_by_person_uuid": "fe5213a8-79d1-4d11-b14e-1fc950c1fd4a",
    "created_by_place_uuid": "c1143892-ccfb-4fa5-95f0-e6fe7ab1b774"
  },
  "reported_date": 1630286917891
}
```


Encounter API 
------------
This is used to sync forms data from afyastat to kenyaEMR. openmrs.obs table.
Example of Encounter API syntax

```
@RequestMapping(
    method = {RequestMethod.POST},
    value = {"/medicformsdata"}
)
```

Payload example:

```
{
  "_id": "32b02999-6c9f-4fea-b6e3-1f3638e108a2",
  "_rev": "1-1fb76b89398079dee47b75345e169c31",
  "form": "hts_initial_form",
  "type": "data_record",
  "content_type": "xml",
  "reported_date": 1623873660150,
  "contact": {
    "_id": "fe5213a8-79d1-4d11-b14e-1fc950c1fd4a",
    "parent": {
      "_id": "c1143892-ccfb-4fa5-95f0-e6fe7ab1b774"
    }
  },
  "from": "+254700800000",
  "hidden_fields": [
    "meta"
  ],
  "fields": {
    "inputs": {
      "meta": {
        "location": {
          "lat": "",
          "long": "",
          "error": "",
          "message": ""
        },
        "deprecatedID": ""
      },
      "user": {
        "contact_id": "fe5213a8-79d1-4d11-b14e-1fc950c1fd4a",
        "facility_id": "c1143892-ccfb-4fa5-95f0-e6fe7ab1b774",
        "name": "clinician"
      },
      "source": "contact",
      "source_id": "",
      "contact": {
        "_id": "65794100-c130-4203-9c29-06f4cd2528c5",
        "name": "Test  tested",
        "kemr_uuid": "",
        "patient_sex": "male",
        "contact": {
          "_id": "",
          "name": ""
        },
        "parent": {
          "parent": {
            "contact": {
              "name": "",
              "phone": ""
            }
          }
        }
      }
    },
    "eligibility": "",
    "source": "contact",
    "source_id": "",
    "place_id": "65794100-c130-4203-9c29-06f4cd2528c5",
    "place_name": "Test  Tested",
    "head": "",
    "form_uuid": "402dc5d7-46da-42d4-b2be-f43ea4ad87b0",
    "encounter_type_uuid": "9c0a7a57-62ff-4f75-babe-5835b0e921b7",
    "sex": "male",
    "ageInYears": "34",
    "this_facility_name": "HomaBay HC",
    "encounter_date": "2021-06-03",
    "observation": {
      "_164930_populationType_99DCT": "_164929_keyPopulation_99DCT",
      "_164929_kpTypeMaleMULTISELECT_99DCT": "_105_pwid_99DCT",
      "_164951_disability_99DCT": "_1066_no_99DCT",
      "_164401_everTestedByProvider_99DCT": "_1065_yes_99DCT",
      "_159813_durationSinceLastTest_99DCT": "",
      "_164952_HasClientdoneHIVselfTestInlast12months_99DCT": "_1066_no_99DCT",
      "_165215_htsSetting_99DCT": "_1537_facility_99DCT",
      "_163556_htsApproach_99DCT": "_164953_CITC_99DCT",
      "facility_testing": "_164953_nonPatient_99DCT",
      "_164956_htsStrategyUsed_99DCT": "_164953_nonPatient_99DCT",
      "facility_sdp": "_160538_pmtctAnc_99DCT",
      "_160540_htsEntryPoint_99DCT": "_160538_pmtctAnc_99DCT",
      "_1710_clientConsented_99DCT": "1",
      "_164959_clientTestedAs_99DCT": "_164957_individual_99DCT",
      "_164410_firstTest_99DCT": {
        "_164962_KitOneName_99DCT": "_164960_determine_99DCT",
        "_164964_KitOneLotNumber_99DCT": "erere",
        "_162502_KitOneExpiry_99DCT": "2021-07-02",
        "_1040_testOneResults_99DCT": "_703_positive_99DCT"
      },
      "_164410_secondTest_99DCT": {
        "_164962_KitTwoName_99DCT": "_164961_firstResponse_99DCT",
        "_164964_KitTwoLotNumber_99DCT": "sdsds",
        "_162501_KitTwoExpiry_99DCT": "2021-07-02",
        "_1326_testTwoResults_99DCT": "_703_positive_99DCT"
      },
      "final_result": "Positive",
      "_159427_finalResults_99DCT": "_703_positive_99DCT",
      "final_result_note": "",
      "_164848_resultGiven_99DCT": "_1065_yes_99DCT",
      "_1659_tbScreening_99DCT": "_1660_noSigns_99DCT",
      "_1887_referralReason_99DCT": "_162082_confirmatoryTest_99DCT",
      "_160481_referralFacility_99DCT": "_163266_thisFacility_99DCT",
      "same_facility_referral": "HomaBay HC",
      "_162724_facilityReferred_99DCT": "HomaBay HC",
      "_160592_consentForPns_99DCT": "_1066_no_99DCT",
      "_163042_remarks_99DCT": ""
    },
    "group_review": {
      "n_submit": "",
      "n_household_details_title": "",
      "n_household_details": "",
      "n_assessment_findings": "",
      "n_drinking_water_source": "",
      "n_uncovered_well_or_spring": "",
      "n_hts_strategy": "",
      "n_entry_point": "",
      "n_test_1_results": "",
      "n_test_result_1_positive": "",
      "n_test_2_results": "",
      "n_test_result_2_positive": "",
      "n_final_results": "",
      "n_final_test_positive": "",
      "n_tb_screening_summary": "",
      "n_no_signs": "",
      "n_pns_summary": "",
      "pns_consent_no": "",
      "r_followup_positive": "",
      "r_followup_note_positive": ""
    },
    "audit_trail": {
      "created_by": "clinician"
    },
    "meta": {
      "instanceID": "uuid:c353f7b8-8c4c-4d2a-b5e4-b62bf1bf05a7"
    }
  },
  "geolocation": {
    "latitude": -1.2812287999999998,
    "longitude": 36.75914240000001,
    "altitude": null,
    "accuracy": 318824,
    "altitudeAccuracy": null,
    "heading": null,
    "speed": null
  },
  "_attachments": {
    "content": {
      "content_type": "application/xml",
      "revpos": 1,
      "digest": "md5-ovPRByOPaHNZb913Hqli0A==",
      "length": 6543,
      "stub": true
    }
  }
}

```
Contact Tracing API
------------
This is used to sync contact tracing data from afyastat to kenyaEMR. 
Example of payload:
```
{
  "_id": "cb8ad8ce-de3b-4238-8e49-ad020723b897",
  "_rev": "1-85b82313b31ff086b845e22f4a2dae6d",
  "form": "hts_client_tracing",
  "type": "data_record",
  "content_type": "xml",
  "reported_date": 1630287329901,
  "contact": {
    "_id": "fe5213a8-79d1-4d11-b14e-1fc950c1fd4a",
    "parent": {
      "_id": "c1143892-ccfb-4fa5-95f0-e6fe7ab1b774"
    }
  },
  "from": "+254700800000",
  "hidden_fields": [
    "meta"
  ],
  "fields": {
    "inputs": {
      "meta": {
        "location": {
          "lat": "",
          "long": "",
          "error": "",
          "message": ""
        },
        "deprecatedID": ""
      },
      "source": "contact",
      "source_id": "",
      "contact": {
        "_id": "a191a0d7-66c6-41c9-adbd-c672a67827c2",
        "name": "Xxxxx Yyyyy Zzzzz",
        "kemr_uuid": "",
        "patient_telephone": "+254712345678",
        "parent": {
          "parent": ""
        }
      }
    },
    "source": "contact",
    "source_id": "",
    "place_id": "a191a0d7-66c6-41c9-adbd-c672a67827c2",
    "place_name": "Xxxxx Yyyyy Zzzzz",
    "head": "",
    "form_uuid": "15ed03d2-c972-11e9-a32f-2a2ae2dbcce4",
    "encounter_type_uuid": "9c0a7a57-62ff-4f75-babe-5835b0e921b7",
    "encounter_date": "2021-08-30",
    "observation": {
      "_164966_tracingType_99DCT": "_164965_physical_99DCT",
      "_159811_outcome_99DCT": "_1118_notReached_99DCT",
      "_1779_physicalWhyNot_99DCT": "_1706_notFoundAtHome_99DCT",
      "_5622_remarks_99DCT": ""
    },
    "group_review": {
      "n_submit": "",
      "n_household_details_title": "",
      "n_household_details": "",
      "n_assessment_findings": "",
      "tracking": "",
      "physical": "",
      "n_test_1_results": "",
      "voluntaryExit": "",
      "notcontactedphysicaly": ""
    },
    "meta": {
      "instanceID": "uuid:e16fce0c-4779-4218-8f7c-36fbf6fffd0b"
    }
  },
  "geolocation": {
    "latitude": -3.967015,
    "longitude": 39.7446353,
    "altitude": null,
    "accuracy": 1185,
    "altitudeAccuracy": null,
    "heading": null,
    "speed": null
  },
  "_attachments": {
    "content": {
      "content_type": "application/xml",
      "revpos": 1,
      "digest": "md5-C3XZdcCQATT7AcksfNZe+A==",
      "length": 2376,
      "stub": true
    }
  }
}

```
Peer Calendar API
------------
This is used to sync peer calendar data from afyastat to kenyaEMR. 
Short example of peer calendar payload:
```
{
  "_id": "74afd9a9-72f7-45b5-aaa4-effe46205f0d",
  "_rev": "1-74fb3d52864c3514e6a6f561d93181bb",
  "form": "peer_calendar",
  "type": "data_record",
  "content_type": "xml",
  "reported_date": 1600419278309,
  "contact": {
    "_id": "c9012ccd-fd48-4e43-8c20-7a10b0e3146e",
    "parent": {
      "_id": "799e3035-c409-4060-a95a-ee7ff4f2bfe4",
      "parent": {
        "_id": "a16f03f1-5c2f-4e23-9c6c-3a69cd6ea81a",
        "parent": {
          "_id": "5fae8830-5921-4cff-a507-8a18fd881c02"
        }
      }
    }
  },
  "from": "+254712345678",
  "hidden_fields": [
    "meta"
  ],
  "fields": {
    "inputs": {
      "meta": {
        "location": {
          "lat": "",
          "long": "",
          "error": "",
          "message": ""
        },
        "deprecatedID": ""
      },
      "source": "contact",
      "source_id": "",
      "contact": {
        "_id": "767a53e3-1432-4575-9fd2-de5f4c573fb6",
        "name": "Musa Songs Jakadala",
        "contact": {
          "_id": "",
          "name": ""
        },
        "parent": {
          "parent": {
            "contact": {
              "name": "",
              "phone": ""
            }
          }
        }
      }
    },
    "source": "contact",
    "source_id": "",
    "place_id": "767a53e3-1432-4575-9fd2-de5f4c573fb6",
    "place_name": "Musa Songs Jakadala",
    "head": "",
    "form_uuid": "7492cffe-5874-4144-a1e6-c9e455472a35",
    "encounter_type_uuid": "c4f9db39-2c18-49a6-bf9b-b243d673c64d",
    "encounter_date": "2020-09-18",
    "observation": {
      "_165006_hotspotName_99DCT": "name of",
      "_165005_typology_99DCT": "_165012_InjectingDen_99DCT",
      "_165356_selectMonth_99DCT": "2020-02-01",
      "_165007_sexActPerWeek_99DCT": "34",
      "_165299_condomsPerMonth_99DCT": "136",
      "_165008_averageAnalPerWeek_99DCT": "3",
      "_165300_lubricantRequiredPerMonth_99DCT": "12",
      "_165009_injectionPerDay_99DCT": "3",
      "_165308_needlesPerMonth_99DCT": "90",
      "_165301_durationOfSexWork_99DCT": "3",
      "_123160_everExperiencedViolence_99DCT": "_1065_yes_99DCT",
      "_165302_servicesReceivedMULTISELECT_99DCT": "_159777_condoms_99DCT",
      "_1732_weekToFillServicesMULTISELECT_99DCT": "_165304_firstWeek_99DCT",
      "_165304_weekOne_99DCT": {
        "_165058_noOfNeedlesDistributed_99DCT": "4",
        "_165055_noOfCondomsDistributed_99DCT": "4",
        "_165056_noFemaleCondoms_99DCT": "4",
        "_165057_noOfLubesDistributed_99DCT": "4",
        "_165147_healthEducationMULTISELECT_99DCT": "_165148_PrEPmessaging_99DCT",
        "_165222_noOfHIVKitsDistributed_99DCT": "4",
        "_1774_receivedClinicalService_99DCT": "_1065_yes_99DCT",
        "_123160_violenceReported_99DCT": "_1065_yes_99DCT"
      },
      "_160632_remarks_99DCT": "test"
    },
    "group_review": {
      "n_submit": "",
      "n_household_details_title": "",
      "n_household_details": "",
      "n_assessment_findings": "",
      "pepfarSite": "",
      "sexAct": "",
      "sex_act": "",
      "condomsPerMonth": "",
      "condoms_permonth": "",
      "anal": "",
      "analPerWeek": "",
      "lubricant": "",
      "lubricantPerMonth": "",
      "injection": "",
      "injectionPerDay": "",
      "needles": "",
      "needlesPerMonth": "",
      "duration": "",
      "durationSexWork": "",
      "violence": "",
      "yes": "",
      "experienceViolence": "",
      "services": "",
      "condoms": "",
      "weeks": ""
    },
    "meta": {
      "instanceID": "uuid:a0a18b23-9811-4cea-acb1-011bc299c495"
    }
  },
  "geolocation": {
    "latitude": -1.3034531999999999,
    "longitude": 36.7927116,
    "altitude": null,
    "accuracy": 2568,
    "altitudeAccuracy": null,
    "heading": null,
    "speed": null
  },
  "_attachments": {
    "content": {
      "content_type": "application/xml",
      "revpos": 1,
      "digest": "md5-I73tgMQgYYcyHLtzlWaxWQ==",
      "length": 4730,
      "stub": true
    }
  }
}
```

Demographic Update API
------------
This is used to update registration data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as Demographic api above

Queue data, Archive data, Error data
------------
Data from medic is being queued on openmrs.medic_queue_data table (**Queue data**) where it is then processed by Timed Scheduler.
This process can be Successful, or it can Fail (**Error data**) failed data is stored on openmrs.medic_error_data despite the outcome the data is archived as successful or as error (**Archive data**) on  openmrs.medic_archive_data  table so that queued data does not grow.

![Flowchat](https://user-images.githubusercontent.com/15907903/131275178-36e1f52c-56a9-4eb4-b39b-c31c015e309e.JPG)


