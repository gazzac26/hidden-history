HIDDEN HISTORY

THIRD-PARTY DATA SOURCES

Status: PRODUCTION CONTROL DOCUMENT
Version: 1.0
Document Owner: Gary Lee Chantler
Operating Identity: Gary Lee Chantler, trading/operating as Hidden History
Last Reviewed: 3 September 2026
Contact: SovereignSoftwareLtd@gmail.com

---

1. PURPOSE

Hidden History may rely on external organisations and data services to obtain vehicle information used by the application.

This document identifies the current vehicle-information sources and explains the distinction between:

- information originating from an external source;
- processing performed by Hidden History; and
- analysis or presentation created by Hidden History.

Hidden History does not claim ownership of underlying third-party or government records merely because those records are accessed or displayed through the service.

---

2. CURRENT DATA SOURCES

At launch, Hidden History's vehicle-information sources include:

- Driver and Vehicle Licensing Agency (DVLA) vehicle information; and
- Driver and Vehicle Standards Agency (DVSA) MOT information.

DVSA's MOT History API provides authorised third-party organisations with access to vehicle and MOT test information.

The exact information available to Hidden History depends on the applicable source, access arrangement, API response and permissions in force at the time.

The production implementation must remain consistent with the applicable source terms and permissions.

---

3. DATA PROCESSING

Hidden History may retrieve information returned by connected vehicle-data sources and process it for the purposes of providing the service.

This may include:

- receiving source information;
- normalising data into Hidden History's internal format;
- combining information from permitted sources;
- associating information with the relevant vehicle;
- displaying source information to the user;
- using available information in vehicle analysis; and
- incorporating relevant information into a saved vehicle report where the user chooses to save the report.

Hidden History must not process or use source information beyond what is permitted by the applicable source arrangements and law.

---

4. SOURCE OWNERSHIP

Information supplied by DVLA, DVSA or another external organisation remains subject to the rights, licences, terms and legal restrictions applicable to that source.

Hidden History's own:

- software;
- interface;
- presentation;
- organisation;
- analysis;
- explanatory content; and
- original documentation

remain distinct from the underlying third-party information.

Nothing in this document transfers ownership of third-party material to Hidden History.

---

5. SOURCE ACCURACY

External sources may provide information that is incomplete, delayed, unavailable or subsequently corrected.

DVLA's current Vehicle Enquiry Service terms state that DVLA takes reasonable steps to ensure data is accurate and up to date before transmission but does not warrant the accuracy of data provided.

Hidden History therefore does not guarantee that every item of external vehicle information is complete or error-free.

Where an apparent error is identified, the Vehicle Data Correction and Source Dispute Policy applies.

---

6. SOURCE AVAILABILITY

External data services may:

- experience outages;
- experience temporary technical failures;
- change their APIs;
- change the information made available;
- return incomplete information;
- restrict or suspend access;
- change technical requirements;
- change their terms or licensing arrangements; or
- discontinue a service.

Hidden History cannot guarantee uninterrupted availability of any external data source.

If a source is unavailable, Hidden History may be unable to provide some vehicle information or analysis.

Missing information must not automatically be interpreted as confirmation that no relevant event or issue exists.

---

7. DATA-SOURCE LIMITATIONS

The information available to Hidden History depends on what the relevant source actually provides.

A source may not contain every event, condition or piece of historical information relating to a vehicle.

Hidden History should not represent the absence of information in an external source as proof that the underlying event or issue never occurred.

This distinction is particularly important when users are making vehicle-purchasing decisions.

---

8. DATA RETENTION

Hidden History does not retain ordinary unsaved vehicle searches merely because a user performs a search.

Where a user chooses to save a vehicle report, the information required for that saved report may be retained in accordance with the Hidden History Privacy Policy and applicable retention requirements.

External-source information must not be retained for longer or used for purposes beyond those permitted by applicable law and source arrangements.

---

9. FUTURE DATA SOURCES

Additional vehicle-information sources may be introduced in the future.

Any future source must be assessed before being connected to Hidden History, including where applicable:

- legal authority to access the information;
- licensing requirements;
- contractual restrictions;
- permitted purposes;
- data-protection requirements;
- technical security requirements;
- retention restrictions;
- disclosure restrictions; and
- applicable source terms.

No future source should be described as a current Hidden History source until it is operational and properly authorised.

---

10. MIB / MIAFTR

MIB/MIAFTR is NOT currently connected to Hidden History.

MIB/MIAFTR remains a potential future data source only.

It must not be represented in the application, privacy documentation, marketing material or user-facing reports as a current source unless and until:

1. the required access has been formally authorised;
2. the applicable licensing or contractual arrangements are in place;
3. the technical integration has been implemented;
4. the permitted use of the information has been verified; and
5. the production documentation has been updated.

---

11. OTHER FUTURE SOURCES

Other external vehicle-data sources may be considered in the future.

No specific future provider, database, dataset or commercial arrangement is promised by this document.

Any future provider must be assessed on its actual terms and permissions before integration.

---

12. SOURCE CHANGES

If a current data source materially changes:

- its API;
- available information;
- permitted use;
- licensing conditions;
- access requirements;
- retention requirements; or
- contractual terms,

Hidden History should review the effect on:

- the application;
- data processing;
- analysis;
- privacy documentation;
- legal documentation;
- user-facing disclosures; and
- saved reports.

Material changes should be recorded in the applicable internal change documentation.

---

13. SOURCE DISCONTINUATION

If a connected data source becomes unavailable or Hidden History no longer has permission to use it, Hidden History must not continue representing that source as available.

Where necessary, the application and relevant documentation should be updated to reflect the change.

---

14. DATA-SOURCE RESPONSIBILITY

Hidden History is responsible for the way it processes and presents information within its own systems.

The relevant external source remains responsible for its own underlying records and the operation of its own services, subject to its applicable terms and legal responsibilities.

Where a user needs an underlying government or external record corrected, Hidden History may direct the user to the relevant source or authority.

---

15. USER DISCLOSURE

Where appropriate, Hidden History may identify the source of vehicle information so users can understand whether information originates from:

- an official government source;
- another authorised external source; or
- Hidden History's own analysis.

Source information and Hidden History analysis should not be presented as though they are the same thing.

---

16. DOCUMENTATION CONSISTENCY

This document must remain consistent with the actual production configuration.

The current documented source position is:

Source| Current status
DVLA vehicle information| Current
DVSA/MOT information| Current
MIB/MIAFTR| Not connected — future only
Other external sources| Not currently specified

Any change to this position must be reflected in the relevant documentation before the changed functionality is represented to users.

---

17. DOCUMENT CONTROL

This document must be reviewed whenever:

- a new vehicle-data source is added;
- an existing source is removed;
- source permissions materially change;
- API integration changes materially;
- available data changes materially;
- licensing arrangements change;
- data-retention arrangements change; or
- applicable law or regulatory guidance materially changes.

This document must accurately describe the sources actually connected to Hidden History.

A future source must not be represented as operational merely because integration is planned.

---

END OF THIRD-PARTY DATA SOURCES