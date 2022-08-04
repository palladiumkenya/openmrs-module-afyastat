const { URL } = require('url');
const level = require('level');
const { v4: uuidv4 } = require('uuid');
const PouchDB = require('pouchdb-core');

PouchDB.plugin(require('pouchdb-adapter-http'));
PouchDB.plugin(require('pouchdb-mapreduce'));

const CONTACT_TYPES = {
  case: 'universal_client', // trace_case
  parent_health_facility: 'ahealth_facility',
  contact: 'patient_contact' // trace_contact
};

const CHANGES_LIMIT = 100;
const SLEEP_DELAY = 1000;
const EXCLUDED_KEYS = ['_id', '_rev', 'needs_sign_off', 'place_id', 'patient_id', 'parent', 'type', 'contact_type'];

//if (!process.env.COUCH_URL) {
  //throw new Error('COUCH_URL env var not set.');
//}

const cache = level('cache');


let COUCH_URL;
let COUCH_USER_URL;
try {
  COUCH_URL = new URL('http://medic:cb6f4d4b-73cc-4c42-97cb-0db5a631190a@localhost:5988/medic');
  COUCH_USER_URL = new URL('http://medic:cb6f4d4b-73cc-4c42-97cb-0db5a631190a@localhost:5988/medic');
  COUCH_USER_URL.pathname = '_users';
  console.log('User URL: ' + COUCH_USER_URL);

  if (COUCH_URL.pathname !== '/medic') {
    COUCH_URL.pathname = '/medic';
  }
} catch (err) {
  console.error(`You need to define a valid COUCH_URL`);
  console.error(err.stack);
  console.error('Exiting');
  process.exit(-1);
}

const couchdb = new PouchDB(COUCH_URL.href);
const couchdbUsers = new PouchDB(COUCH_USER_URL.href);

const getUTCTimestamp = () => new Date(Date.now() + (new Date().getTimezoneOffset() * 60000)).getTime();

const getChangesAndLastSeq = async (db, seqNumber) => {
  const changes = await db.changes({ limit: CHANGES_LIMIT, since: seqNumber });
  return {
    changeSet: changes.results.filter(change => !change.deleted),
    seqNumber: changes.last_seq
  };
};

const getCase = async (db, type, keyField, key) => {
  let response;
  let result;

  const options = {
    include_docs: true,
  };

  if (keyField === '_id') {
    options.keys = [key];
    response = await db.allDocs(options);
  } else {
    options.key = ["universal_client","kemr_uuid:" + key];
    response = await db.query('medic-client/contacts_by_type_freetext', options);
  }

  response.rows.forEach(row => {

    if (row.doc && row.doc.contact_type === type) {
      result = row.doc;
    }
  });

  return result;
};

const getPlacesByType = async (db, placeType) => {
  const options = {
    include_docs: true,
    key: [`${placeType}`],
  };

  const docs = await db.query('medic-client/contacts_by_type', options);

  const result = [];
  docs.rows.map(row => {
    if (row.doc) {
      result.push(row.doc);
    }
  });
  return result;
};

const getDocsFromChangeSet = async (db, changeSet) => {
  const options = {
    include_docs: true,
    keys: changeSet.map(change => change.id)
  };
  const docs = await db.allDocs(options);
  const result = [];
  docs.rows.map(row => {
    if (row.doc) {
      result.push(row.doc);
    }
  });
  return result;
};

const getParentPlaceForUser = async (db, username) => {

  const foundUser = await db.get(`org.couchdb.user:${username}`);
  if (foundUser && foundUser.facility_id) {
      return foundUser.facility_id;
  }
    return null;
};
const getDocsMap = async (db, docs) => {
  const options = {
    include_docs: false,
    keys: docs.map((doc) => doc._id)
  };

  const result = {};
  const response = await db.allDocs(options);

  response.rows.forEach((row) => {
    if (row.doc) {
      result[row.doc._id] = row.doc;
    }
  });

  return result;
};

const getSeqNumber = async cache => {
  let seqNumber;
  try {
    seqNumber = await cache.get('seqNumber');
  } catch (err) {
    seqNumber = '0';
  }
  return seqNumber;
};

const updateSeqNumber = async (cache, seqNumber) => {
  try {
    await cache.put('seqNumber', seqNumber);
  } catch (err) {
    console.error('Error updating the most recent Sequence Number');
    console.error(err.stack);
    throw err;
  }
};

const createNewClientDocument = item => {
  const newClient = {
    _id: uuidv4(),
    kemr_uuid: item._id,
    type: 'contact',
    contact_type: 'universal_client',
    record_originator: 'kenyaemr',
    record_purpose: item.record_purpose,
    reported_date: item.reported_date
  };
  for (const key of Object.keys(item.fields)) {
    if (!EXCLUDED_KEYS.includes(key) && !!item.fields[key]) {
      newClient[key] = item.fields[key];
    }
  }

  if (!newClient.client_name) {
    newClient.client_name = [newClient.patient_firstName, newClient.patient_familyName, newClient.patient_middleName].join(' ').replace(/\s+/, ' ');
  }

  newClient['contacts'] = item.contacts;
  return newClient;
};

const extractContactDetails = (item, retainReference) => {
  const contact = {
    _id: uuidv4(),
    type: 'person',//contact
    contact_type: 'person',//trace_contact
    record_originator: 'kenyaemr'//add discriminator
  };
  for (const key of Object.keys(item)) {
    if (![...EXCLUDED_KEYS, 'contacts'].includes(key) && !!item[key]) {
      contact[key] = item[key];
    }
    if (retainReference) {
      contact._id = item._id;
    }
  }
  return contact;
};

// adapted from medic-conf
const minifyLineage = lineage => {
  if (!lineage || !lineage._id) {
    return undefined;
  }

  const result = {
    _id: lineage._id,
    parent: minifyLineage(lineage.parent),
  };

  return JSON.parse(JSON.stringify(result));
};

const logPouchDBResults = results => {
  for (const result of results) {
    if (result.ok) {
      console.info(`Doc: ${result.id} saved successfully`);
    } else {
      console.error(`An error occurred while saving Doc: ${result.id}. Error: ${result.error}. Status: ${result.status}`);
    }
  }
};

const moveClientsToHealthFacility = async (db, newCases) => {
  const parentHealthFacility = (await getPlacesByType(couchdb, CONTACT_TYPES.parent_health_facility))[0];

  newCases.forEach(async item => {
    const cases = {};
    cases.existingCase = await getCase(db, CONTACT_TYPES.case, 'kemr_uuid', item._id);// originally in CHT
    cases.transitionedContact = await getCase(db, CONTACT_TYPES.contact, '_id', item.fields.cht_ref_uuid);

    const docsToCreate = [];
    /*
     When we transition patient contact to fully registered contact (universal_client), we'll change it's contact type
     to universal_client from patient_contact. When looking them up after the transition, we still need to rely
     on KenyaEMRs='s cht_ref_uuid attribute.
    */
    const existingCase = cases.existingCase;
    const existingClientCase = existingCase;
    const existingCaseType = (existingClientCase || {}).contact_type;

    const newClient = createNewClientDocument(item);

        //delete report pushed from EMR
    docsToCreate.push({ _id: item._id, _rev: item._rev, _deleted: true });
    if (!existingCaseType) {
      let parent;

      if (cases.transitionedContact) {
        // parent new case to the contact's grand parent -- the facility under which a client was registered
        parent = cases.transitionedContact.parent.parent;
        // change contact type
        newClient.contact_type = CONTACT_TYPES.case; // using this for universal_client

          console.warn(`Processing contact with uuid: ${item._id}.`);

          // for contacts that become cases
        const contact = Object.assign({}, cases.transitionedContact);
        //contact.transitioned_to_case = newClient._id;
        //contact.muted = true;
        //docsToCreate.push(contact);

        // try to delete the old record

        docsToCreate.push({ _id: contact._id, _rev: contact._rev, _deleted: true });

        // get the index client and update the list of registered contacts

          let indexClient = await getCase(db, CONTACT_TYPES.case, '_id', newClient.relation_uuid);
          console.info("Found this index: " + indexClient + " for uuid: " + newClient.relation_uuid)

          if (!indexClient) {
            indexClient = await getCase(db, CONTACT_TYPES.case, 'kemr_uuid', newClient.relation_uuid);
          }

          if (indexClient) {
            // extract the transitioned_contacts section
              const contacts = Object.assign([], indexClient.transitioned_contacts);
              const contactObj = {};
              contactObj.contact_name = newClient.name;
              contactObj.contact_relation_type = newClient.relation_type;
              contactObj.contact_uuid = newClient._id;

              contacts.push(contactObj);
              indexClient.transitioned_contacts = contacts;

              console.info("Contacts: " + contacts);

              docsToCreate.push(indexClient);
          }

      } else {
          if (item.fields.assignee) {
            const userObj = await getParentPlaceForUser(couchdbUsers, item.fields.assignee);
              parent = await db.get(userObj);
        }

        if (!parent || !newClient.assignee) {
          console.warn(`No assignee for record ${item._id}. Adding the client/patient to the parent health facility`);
          parent = parentHealthFacility;
        }
      }

      newClient.parent = minifyLineage({ _id: parent._id, parent: parent.parent });

      docsToCreate.push(newClient);

        // =================== trying to add contacts

      /*newClient.contacts.forEach(contactData => {
        const contact = extractContactDetails(contactData, true);
        contact.parent = minifyLineage({ _id: newClient._id, parent: newClient.parent });
        contact.kemr_uuid = newClient.kemr_uuid;
        docsToCreate.push(contact);
      });*/

      // =================== end of adding contacts

      console.info(`Effecting move for Record UUID: <${newClient._id}> KEMR REF: <${newClient.kemr_uuid}> to <${parent.name}>`);
      const results = await db.bulkDocs(docsToCreate);
      logPouchDBResults(results);

    } else { // delete any record with existing reference.
      if (docsToCreate) {
         const results = await db.bulkDocs(docsToCreate);
         logPouchDBResults(results);
      }
      console.info(`Successfully deleted record from the EMR with reference: ${item._id}`);
    }
  });
};

const updater = async () => {
  let DELAY_FACTOR = 1;
  const seqNumber = await getSeqNumber(cache);

  console.info(`Processing from Sequence Number: ${seqNumber.substring(0, 61)}`);

  const result = await getChangesAndLastSeq(couchdb, seqNumber);
  const docs = await getDocsFromChangeSet(couchdb, result.changeSet);

  const dataFromKEMR = docs.filter(doc => doc.type === 'data_record' && doc.form === 'case_information');
  await moveClientsToHealthFacility(couchdb, dataFromKEMR);

  await updateSeqNumber(cache, result.seqNumber);

  if (seqNumber === result.seqNumber) {
    DELAY_FACTOR = 30;
  } else {
    DELAY_FACTOR = 1;
  }

  return new Promise(() => setTimeout(updater, DELAY_FACTOR * SLEEP_DELAY));
};

(async () => {
  updater();
})();
