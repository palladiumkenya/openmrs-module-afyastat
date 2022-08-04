//const extras = require('./nools-extras');
/*const {
  isActivePregnancy,
  countANCFacilityVisits,
} = extras;*/

module.exports = [
  // HTS tested
  {
    id: 'hts-number-tested-this-month',
    type: 'count',
    icon: 'icon-person',
    goal: -1,
    translation_key: 'targets.clients_tested.title',
    subtitle_translation_key: 'targets.this_month.subtitle',
    appliesTo: 'reports',
    appliesToType: ['hts_initial_form', 'hts_retest_form'],
    appliesIf: function (contact, report) {
      if (!report) {return false;}
      return report.form === 'hts_initial_form' || report.form ==='hts_retest_form';
    },
    date: 'reported',
    idType: 'contact'
  },

  // HTS number positive
  {
    id: 'hts-number-positive-this-month',
    type: 'count',
    icon: 'icon-person',
    goal: -1,
    translation_key: 'targets.clients_positive.title',
    subtitle_translation_key: 'targets.this_month.subtitle',
    appliesTo: 'reports',
    appliesToType: ['hts_initial_form','hts_retest_form'],
    appliesIf: function (contact, report) {
      if (!report) {return false;}
      return ((report.form === 'hts_initial_form' || report.form ==='hts_retest_form')  &&  Utils.getField(report,'observation._159427_finalResults_99DCT') === '_703_positive_99DCT');
    },
    date: 'reported',
    idType: 'contact'
  },

  // HTS number linked
  {
    id: 'hts-number-linked-this-month',
    type: 'count',
    icon: 'icon-clinic',
    goal: -1,
    translation_key: 'targets.clients_linked.title',
    subtitle_translation_key: 'targets.this_month.subtitle',
    appliesTo: 'reports',
    appliesToType: ['hts_linkage'],
    appliesIf: function (contact, report) {
      if (!report) {return false;}
      return ((report.form === 'hts_linkage')  &&  Utils.getField(report,'hts_linkage_assessment._162053_cccNumber_99DCT') !== '');
    },
    date: 'reported',
    idType: 'contact'
  }
];
