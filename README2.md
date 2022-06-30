
## Automation of HTML to JSON Schema migration


Description
-----------
This work automates the process of converting HFE schema to the new JSON form schema for o3. 
HFE forms in KenyaEMR were used to guide the process and therefore may not cover all the HFE tags.

The focus has been to generate the equivalent schema for the commonly used HFE tags i.e. obs and obsgroup.


What's working
--------------------
1. Generation of schema for obs tag. This works for the common question types i.e. coded, boolean, numeric, text, date/time, etc
2. Handling obs with defined answerConcepts and where the answers should be inferred from the dictionary
3. Handling of obs style for radio, checkbox, dropdown
4. Handling obs fields marked as required
5. Handling of diagnosis answerClass
6. Handling of custom labels as defined in answerLabel/answerLabels
7. Generation of schema for obsgroup tag. The logic is able to group questions within an obs group. It also generates schema for groups marked as repeating
8. Exporting the HFE schema to a configurable directory. This is helpful in cases where the HFE schema requires further updates in order to generate the correct JSON schema 
9. Grouping of related checkboxes as shown below
10. Provide labels for grouped obs. A new attribute labelText is supported for that 


    <obs conceptId="374AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" answerConceptId="160570AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
													 answerLabel="Emergency contraceptive pills" style="checkbox" /><br/>
    <obs conceptId="374AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" answerConceptId="780AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
													 answerLabel="Oral Contraceptives Pills" style="checkbox" /><br/>
    <obs conceptId="374AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" answerConceptId="5279AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
													 answerLabel="Injectible" style="checkbox" /><br/>



What's pending
------------
1. Support for other tags other than obs and obsgroup
2. Generation of pages and sections - this is all manual

Code organization
-----------------
1. Every tag, a data point, has an extractor. This is a class with logic on how to extract information about the tag
2. Every tag has a renderer class which generates the required JSON schema



