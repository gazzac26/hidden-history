# HIDDEN HISTORY
# CONTENT AND DATA CORRECTION PROCEDURE

Status: INTERNAL / PRODUCTION CONTROL DOCUMENT

Version: 1.0  
Effective date: Date of first adoption  
Last updated: 4 September 2026  
Document owner: Gary Lee Chantler  
Operating identity: Gary Lee Chantler, trading/operating as Hidden History  
Contact: SovereignSoftwareLtd@gmail.com

---

## 1. PURPOSE

This procedure explains how Hidden History handles reports that vehicle information, personal information or analytical output may be incorrect.

---

## 2. IMPORTANT DISTINCTION

Hidden History uses information supplied by external sources.

For current vehicle-record functionality, the principal sources are:

- DVLA vehicle information
- DVSA/MOT information

Hidden History does not control the underlying government source record.

A correction request must therefore establish whether the issue is:

1. A source-data issue;
2. A Hidden History processing/display issue; or
3. An analytical interpretation issue.

---

## 3. USER REPORT

Where practical, the user should provide:

- Vehicle registration
- Description of the suspected error
- Approximate date/time
- Screenshot
- Supporting evidence
- Account information where necessary

Do not request unnecessary personal information.

---

## 4. INITIAL REVIEW

Determine:

- Source of the information
- Information actually received
- Information displayed
- Any transformation or normalisation performed
- Whether the displayed result accurately reflects the source
- Whether an analysis engine introduced an error
- Whether the source itself appears incorrect

---

## 5. HIDDEN HISTORY PROCESSING ERROR

If valid source data was incorrectly:

- Retrieved
- Mapped
- Normalised
- Formatted
- Stored
- Displayed
- Associated with the wrong vehicle
- Used by an analysis process

Hidden History should correct the technical issue.

Where the error materially affects a saved report, the report should be corrected or the user informed in accordance with the actual production implementation.

Significant technical errors should be recorded in the appropriate secure operational record.

---

## 6. SOURCE DATA ERROR

If the underlying DVLA or DVSA/MOT record appears incorrect, Hidden History must not alter the external source record.

The user should be directed to the relevant source-provider correction or dispute route where appropriate.

Hidden History may separately correct any error in its own presentation or processing.

---

## 7. MOT RECORDS

MOT history and test information is source information.

A disagreement with an historical MOT result does not, by itself, authorise Hidden History to change the source record.

Where the user believes the MOT record is wrong, the appropriate DVSA/source process should be followed.

---

## 8. ANALYSIS RESULTS

If a user believes an analysis result is incorrect:

- Identify the input data
- Identify the relevant analysis process/rule
- Determine whether the system operated as designed
- Identify any technical error
- Correct the error where appropriate

Analysis is not to be represented as a definitive statement of mechanical condition, fraud, ownership or legal fact.

---

## 9. SAVED REPORTS

Saved reports are user-accessible records of the information and analysis available when the report was generated.

Where a material Hidden History error is identified, the production implementation should determine whether:

- The saved report is corrected;
- A corrected report is generated;
- The user is notified; or
- The original report is retained as a historical snapshot with a correction notice.

Historical report handling must remain consistent with the actual product design.

---

## 10. PERSONAL-DATA ACCURACY

Where the disputed information is personal information, the request must also be considered under the applicable right to rectification and related data-protection requirements.

See PRIVACY_REQUEST_HANDLING_PROCEDURE.md and DSAR_PROCEDURE.md.

---

## 11. RECORD KEEPING

Record significant correction requests and outcomes where necessary to:

- Demonstrate accountability
- Correct recurring technical problems
- Defend legal claims
- Evidence how a material dispute was handled

Do not create unnecessary long-term personal-data records.

---

## 12. CONTACT

Correction requests:

SovereignSoftwareLtd@gmail.com

---

## 13. REVIEW

Review this procedure if:

- A new data source is introduced
- Source correction routes change
- Analysis architecture changes materially
- Saved-report behaviour changes
- New categories of personal data are processed

---

END OF CONTENT AND DATA CORRECTION PROCEDURE
