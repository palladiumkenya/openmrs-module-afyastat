const extras = require('./contact-summary-extras');
const { today, getNewestReport, isAlive, isReadyForNewPregnancy, isReadyForDelivery, getLastScreeningDate, getLastScreeningFormDateCreated, getField, getAgeInMonths } = extras;

//contact, reports, lineage are globally available for contact-summary
const thisContact = contact;
const thisLineage = lineage;
const allReports = reports;
const context = {
  alive: isAlive(thisContact),
  muted: false,
  show_pregnancy_form: isReadyForNewPregnancy(thisContact, allReports),
  show_delivery_form: isReadyForDelivery(thisContact, allReports),
  has_hts_initial: allReports.some((report) => report.form === 'hts_initial_form' || report.form === 'hts_retest_form'),
  has_hts_referral: allReports.some((report) => report.form === 'hts_referral'),
  has_kp_contact_form: allReports.some((report) => report.form === 'contact'),
  has_hts_linkage: allReports.some((report) => report.form === 'hts_linkage'),
  has_hts_contact_followup: allReports.some((report) => report.form === 'hts_client_tracing'),
};

console.log('trying console');
const mostRecentHtsForm = getNewestReport(allReports, ['hts_initial_form', 'hts_retest_form']);
const latestHtsForm = getNewestReport(allReports, ['hts_initial_form', 'hts_retest_form', 'hts_screening_form']);
const mostRecentHtsRetest = getNewestReport(allReports, ['hts_retest_form']);
const mostRecentHtsInitial = getNewestReport(allReports, ['hts_initial_form']);
const mostRecentHtsContactTracing = getNewestReport(allReports, ['hts_client_tracing']);
const mostRecentContactForm = getNewestReport(allReports, ['contact']);

let lastScreeningDate = null;
let lastScreenDateCreated = null;
let lastHtsService = null;

const mostRecentScreeningForm = getNewestReport(allReports, ['hts_screening_form']);
if (mostRecentScreeningForm) {
  lastScreeningDate = getLastScreeningDate(mostRecentScreeningForm);
  lastScreenDateCreated = getLastScreeningFormDateCreated(mostRecentScreeningForm);
  lastHtsService = getField(mostRecentScreeningForm, 'hts_service') || '';
}
const durationSinceLastScreen = lastScreeningDate ? today.diff(lastScreeningDate, 'days') : null;
const pocHtsScreening = lastScreenDateCreated ? today.diff(lastScreenDateCreated, 'days') : null;

context.screenedToday = durationSinceLastScreen === 0 ? true : false; // check if one had screening today
context.contactAgeInMonths = getAgeInMonths(thisContact);
context.contactAgeInYears = Math.floor(getAgeInMonths(thisContact)/12);


if (latestHtsForm !== null && durationSinceLastScreen !== null && durationSinceLastScreen > 0) { // handle retrospective data entry. set screenedToday to true if screening form has been entered today
  if ( pocHtsScreening === null || pocHtsScreening === 0) {
    context.screenedToday = true;
  }
}

context.lastHtsService = lastHtsService;
context.pocHtsScreening = pocHtsScreening === 0 ? true : false;


let latestfemaleKpTypeResponse = getField(mostRecentHtsForm, 'observation._160581_kpTypeMULTISELECT_99DCT') || '';
let latestmaleKpTypeResponse = getField(mostRecentHtsForm, 'observation._160581_kpTypeMaleMULTISELECT_99DCT') || '';
let latestKpTypeResponse = latestfemaleKpTypeResponse !== '' ? latestfemaleKpTypeResponse : latestmaleKpTypeResponse !== '' ? latestmaleKpTypeResponse : '';

context.hts_initial = {
  population_type:getField(mostRecentHtsForm, 'observation._164930_populationType_99DCT') || '',
  key_population_type:latestKpTypeResponse,
  priority_population_type:getField(mostRecentHtsForm, 'observation._160581_priorityTypeMULTISELECT_99DCT') || '',
  disability:getField(mostRecentHtsForm, 'observation._164951_disability_99DCT') || '',
  disability_type:getField(mostRecentHtsForm, 'observation._162558_disabilityTypeMULTISELECT_99DCT') || '',
  ever_tested:getField(mostRecentHtsForm, 'observation._164401_everTestedByProvider_99DCT') || '',
  previous_settings: getField(mostRecentHtsForm, 'observation.test_setting') || '',
  previous_hiv_self_test: getField(mostRecentHtsForm, 'observation._164952_HasClientdoneHIVselfTestInlast12months_99DCT') || '',
  duration_since_last_test: getField(mostRecentHtsForm, 'observation._159813_durationSinceLastTest_99DCT') || '',

};

context.hts_retest_latest = {
  final_result:getField(mostRecentHtsRetest, 'observation._159427_finalResults_99DCT') || '',
  test_date:getField(mostRecentHtsRetest, 'encounter_date') || '',
};

context.hts_initial_latest = {
  final_result:getField(mostRecentHtsInitial, 'observation._159427_finalResults_99DCT') || '',
  referral_facility:getField(mostRecentHtsInitial, 'observation._160481_referralFacility_99DCT') || '',
};

context.hts_latest_consent = {
  consent:getField(mostRecentHtsForm, 'observation._1710_clientConsented_99DCT') || '',
  eligibility: getField(mostRecentHtsForm, 'observation._162699_eligibleForTesting_99DCT') || '',
};

context.hts_latest_form = {
  form_type:latestHtsForm ? latestHtsForm.form : '',
  //form_type:mostRecentHtsForm ? mostRecentHtsForm.form : '',
};

context.kpif_contact = {
  contact_date: getField(mostRecentContactForm, 'encounter_date') || '',
};
context.hts_client_trace_latest = {
  outcome:getField(mostRecentHtsContactTracing, 'observation._159811_outcome_99DCT') || '',
};

// get the name of the configured facility
let configuredFacilityName = '';
if( thisContact.parent && thisLineage[0]) {
  for (let i = 0; i < 4; i++) { // we use 4 since our hierarchy has at most 3 levels for clients
    const parentObj = thisLineage[i];
    if (parentObj !== null && 'contact_type' in parentObj && parentObj.contact_type === 'ahealth_facility') { // we are only interested in ahealth_facility contact type
      configuredFacilityName = parentObj.name;
      break;
    }
  }
}

context.thisFacilityName = configuredFacilityName;

const fields = [
  //{ appliesToType: 'person', label: 'patient_id', value: thisContact.patient_id, width: 4 },
  { appliesToType: 'bfacility_employee', label: 'Role', value: thisContact.role, width: 4 },
  { appliesToType: 'bfacility_employee', appliesIf: function () { return thisContact.parent && thisLineage[0]; }, label: 'client.facility', value: thisLineage, filter: 'lineage' },
  { appliesToType: 'patient_contact', label: 'contact.age', value: thisContact.date_of_birth, width: 4, filter: 'age' },
  { appliesToType: 'patient_contact', label: 'contact.sex', value: 'contact.sex.' + thisContact.sex, translate: true, width: 4 },
  { appliesToType: 'patient_contact', label: 'person.field.phone', value: thisContact.phone, width: 4 },
  { appliesToType: 'patient_contact', label: 'person.field.physical_address', value: thisContact.physical_address, width: 4 },
  { appliesToType: 'patient_contact', label: 'person.field.relation_to_index', value: thisContact.contact_relationship, width: 4 },
  { appliesToType: 'patient_contact', label: 'person.field.booking_date', value: thisContact.booking_date, width: 4 },
  { appliesToType: 'patient_contact', label: 'person.field.ipv_outcome', value: thisContact.ipv_outcome, width: 4 },
  { appliesToType: 'patient_contact', label: 'person.field.pns_approach', value: thisContact.pns_approach, width: 4 },
  { appliesToType: 'patient_contact', label: 'contact.index.relationship', value: thisLineage, filter: 'lineage' },
  { appliesToType: 'universal_client', label: 'contact.age', value: thisContact.patient_birthDate, filter: 'age', width: 4 },
  { appliesToType: 'universal_client', label: 'contact.sex', value: 'contact.sex.' + thisContact.patient_sex, translate: true, width: 4 },
  { appliesToType: 'universal_client', label: 'contact.phone', value: thisContact.patient_telephone, width: 4 },
  { appliesToType: 'universal_client', label: 'person.address.county', value: thisContact.patient_county, width: 4 },
  { appliesToType: 'universal_client', label: 'person.address.sub_county', value: thisContact.patient_subcounty, width: 4 },
  { appliesToType: 'universal_client', label: 'person.address.ward', value: thisContact.patient_ward, width: 4 },
  { appliesToType: 'universal_client', label: 'person.address.village', value: thisContact.patient_village, width: 4 },
  { appliesToType: 'universal_client', label: 'person.address.landmark', value: thisContact.patient_landmark, width: 8 },
  { appliesToType: 'universal_client', appliesIf: function () { return thisContact.relation_uuid; }, label: 'Related Client', value: `<a href='/#/contacts/${thisContact.relation ? thisContact.relation._id : thisContact.relation_uuid}'>${thisContact.relation_name}</a>`, width: 4, filter: 'safeHtml' },
  { appliesToType: 'universal_client', appliesIf: function () { return thisContact.relation_uuid; }, label: 'Relationship', value: thisContact.relation_type, width: 4 },
  { appliesToType: 'universal_client', appliesIf: function () { return thisContact.parent && thisLineage[0]; }, label: 'client.facility', value: thisLineage, filter: 'lineage' },
  { appliesToType: 'universal_client', appliesIf: function () { return thisContact.record_purpose && thisContact.record_purpose === 'linkage'; }, label: 'Service Alert', value: `<p style='color: red;font-weight: bold'>This client originated from the EMR and is due for linkage </p>`, width: 12, filter: 'safeHtml' },
  { appliesToType: 'universal_client', label: 'contact.notes', value: thisContact.notes, width: 12 },
  { appliesToType: 'patient_support_group', appliesIf: function () { return thisContact.parent && thisLineage[0]; }, label: 'client.facility', value: thisLineage, filter: 'lineage' }

];

if (thisContact.short_name) {
  fields.unshift({ appliesToType: 'person', label: 'contact.short_name', value: thisContact.short_name, width: 4 });
}

const cards = [
  {
    label: 'HIV Testing profile',
    appliesToType: 'universal_client',
    appliesIf: function () {
      return true;
    },
    fields: function () {
      const fields = [];
      let lastTested;
      let lastTestResult;
      let resultTranslated;
      let linkageDate;
      let linkageFacility;
      let linkageCCCNumber;
      let consentForPnsQuestion;
      let consentForPns;


      const testReport = getNewestReport(allReports, ['hts_initial_form', 'hts_retest_form']);
      if (testReport) {
        lastTested = getField(testReport, 'encounter_date');
        lastTestResult = getField(testReport, 'observation._159427_finalResults_99DCT');
        consentForPnsQuestion = getField(testReport, 'observation._160592_consentForPns_99DCT');
        if (lastTestResult) {
          if (lastTestResult === '_703_positive_99DCT') {
            resultTranslated = 'Positive';
          } else if (lastTestResult === '_664_negative_99DCT') {
            resultTranslated = 'Negative';
          } else if (lastTestResult === '_1138_inconclusive_99DCT') {
            resultTranslated = 'Inconclusive';
          }
        }

        if (consentForPnsQuestion) {
          if (consentForPnsQuestion === '_1065_yes_99DCT') {
            consentForPns = 'Yes';
          } else if (consentForPnsQuestion === '_1066_no_99DCT') {
            consentForPns = 'No';
          }
        }
      }

      if (testReport) {
        fields.push(
          {
            label: 'client.last_tested',
            value: lastTested ? lastTested : 'contact.profile.value.unknown',
            filter: lastTested ? 'simpleDate' : '',
            translate: lastTested ? false : true,
            width: 4
          },
          {
            label: 'client.last_test_result',
            value: resultTranslated,
            width: 4
          },
          {
            label: 'Consented for PNS',
            value: consentForPns,
            width: 4
          }
        );
        if (resultTranslated === 'Positive') {
          const linkageReport = getNewestReport(allReports, ['hts_linkage']);
          linkageDate = getField(linkageReport, 'encounter_date');
          linkageFacility = getField(linkageReport, 'observation._162724_facilityLinkedTo_99DCT');
          linkageCCCNumber = getField(linkageReport, 'observation._162053_cccNumber_99DCT');

          fields.push(
            {
              label: 'client.date_linked',
              value: linkageDate ? linkageDate : `<p style='color: red;font-weight: bold;'>NOT YET LINKED</p>`,
              filter: linkageDate ? 'simpleDate' : '',
              translate: linkageDate ? false : true,
              width: 4
            },
            {
              label: 'client.facility_linked',
              value: linkageFacility,
              width: 4
            },
            {
              label: 'client.ccc_number',
              value: linkageCCCNumber,
              width: 4
            }
          );
        }
      }
      return fields;
    }
  },
  {
    label: 'Contacts registered in EMR',
    appliesToType: 'universal_client',
    appliesIf: function () {
      return thisContact.transitioned_contacts && thisContact.transitioned_contacts.length > 0;
    },
    fields: function () {
      const fields = [];
      const contactList = thisContact.transitioned_contacts;
      fields.push(
        {
          label: 'Name',
          value: '',
          width: 4
        },
        {
          label: 'Relationship',
          value: '',
          width: 8
        }
      );
      for (const c of contactList) {
        fields.push(
          {
            label: '',
            value: `<a href='/#/contacts/${c.contact_uuid}'>${c.contact_name}</a>`,
            width: 4,
            filter: 'safeHtml'
          },
          {
            label: '',
            value: c.contact_relation_type,
            width: 8
          }
        );
      }
      return fields;
    }
  }
];

module.exports = {
  context: context,
  cards: cards,
  fields: fields
};
