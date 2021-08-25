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
  "_id": "3d3c14f6-33d8-48b7-a184-c0461ab7881a",
  "_rev": "1-73db7c87311d2ff7ffcf086036ff650b",
  "type": "clinic",
  "contact_type": "clinic",
  "parent": {
    "_id": "a16f03f1-5c2f-4e23-9c6c-3a69cd6ea81a",
    "parent": {
      "_id": "5fae8830-5921-4cff-a507-8a18fd881c02"
    }
  },
  "role": "person",
  "petient_idNumbers": "",
  "petient_typeOfIdentifier": "patient_nationalIdnumber",
  "patient_caseId": "",
  "patient_nationalIdnumber": "9898989",
  "id_delimiter": "",
  "n_demographics": "",
  "patient_familyName": "test1",
  "patient_firstName": "test2",
  "patient_middleName": "test3",
  "name": "test1 test3 test2",
  "patient_name": "test1 test3 test2",
  "patient_sex": "male",
  "patient_dobKnown": "no",
  "patient_ageYears": "31",
  "patient_ageMonths": "7",
  "ephemeral_months": "1",
  "ephemeral_years": "1989",
  "dob_approx": "1989-01-03T00:00:00.000+03:00",
  "dob_raw": "1989-01-03T00:00:00.000+03:00",
  "dob_iso": "1989-01-02",
  "age_in_years": "31",
  "age_in_months": "379",
  "patient_birthDate": "1989-01-02",
  "head_of_household": "",
  "marital_status": "_1060_livingWithPartner_99DCT",
  "occupation": "_159466_driver_99DCT",
  "education": "_159943_primary_99DCT",
  "deceased": "no",
  "demographics_delimiter": "",
  "n_address": "",
  "patient_nationality": "Kenya",
  "patient_telephone": "+254700989989",
  "patient_alternatePhone": "+254799898878",
  "patient_postalAddress": "test4",
  "patient_emailAddress": "test5",
  "patient_county": "Kisii",
  "patient_subcounty": "citutu_chache",
  "patient_town": "",
  "patient_ward": "test6",
  "patient_location": "test7",
  "patient_sublocation": "test8",
  "patient_village": "test9",
  "patient_landmark": "test10",
  "patient_residence": "test11",
  "patient_nearesthealthcentre": "test12",
  "address_delimiter": "",
  "n_next_of_kin_details": "",
  "patient_nextofkin": "test13",
  "patient_nextofkinRelationship": "_974_uncle_99DCT",
  "patient_nextOfKinPhonenumber": "+254700909000",
  "patient_nextOfKinPostaladdress": "test14",
  "relationship_delimiter": "",
  "meta": {
    "created_by": "pnavigator",
    "created_by_person_uuid": "511cbc41-ff7f-4649-8bee-98c433496882",
    "created_by_place_uuid": "a16f03f1-5c2f-4e23-9c6c-3a69cd6ea81a"
  },
  "reported_date": 1596486112393
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
This is used to sync contact tracing data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as encounter api above


Peer Calendar API
------------
This is used to sync peer calendar data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as encounter api above

Demographic Update API
------------
This is used to update registration data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as Demographic api above

Queue data, Archive data, Error data
------------
Data from medic is being queued on openmrs.medic_queue_data table (**Queue data**) where it is then processed by Timed Scheduler.
This process can be Successful, or it can Fail (**Error data**) failed data is stored on openmrs.medic_error_data despite the outcome the data is archived as successful or as error (**Archive data**) on  openmrs.medic_archive_data  table so that queued data does not grow.
