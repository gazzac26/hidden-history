# HIDDEN HISTORY
# ACCOUNT DELETION AND DATA RETENTION

Status: PRODUCTION CONTROL DOCUMENT

Version: 1.0  
Effective date: Date of first publication  
Last updated: 4 September 2026  
Document owner: Gary Lee Chantler  
Legal operator/controller: Gary Lee Chantler, trading/operating as Hidden History  
Privacy/support contact: SovereignSoftwareLtd@gmail.com

---

## 1. PURPOSE

This document defines how Hidden History handles account deletion and retention of personal information.

Hidden History follows a data-minimisation approach. Personal information is not retained merely because it may be useful in the future.

This document reflects the current launch configuration and must be kept consistent with the actual production system.

---

## 2. ACCOUNT DELETION

Users may request deletion of their Hidden History account.

Where account creation is available, Hidden History will provide an accessible account-deletion route within the application and an external account-deletion route.

Account deletion is intended to remove the user's account and associated personal information from Hidden History's live systems, subject only to information that Hidden History has a lawful and documented reason to retain.

---

## 3. WHAT IS DELETED

Subject to any lawful retention requirement, account deletion will remove or anonymise account-linked information held by Hidden History, including where applicable:

- Account/profile information
- Saved vehicle reports
- Account-linked advert analysis records
- Account-linked vehicle information stored as part of saved reports
- Other personal information associated with the account

Deletion must cover information held by relevant service providers where Hidden History controls that processing and the applicable provider arrangement permits or requires deletion.

---

## 4. SAVED VEHICLE REPORTS

Saved reports are account-linked information.

When an account is deleted, associated saved reports should be deleted from Hidden History's live systems unless a lawful retention requirement applies.

Saved report records may include the vehicle information returned by the relevant sources, Hidden History analysis, relevant advert-analysis information and the report-generation timestamp.

---

## 5. UNSAVED SEARCHES

Ordinary vehicle searches that the user does not save are not retained as a vehicle-search history by Hidden History.

The application may process the registration temporarily to obtain and display the requested result, but the search itself is not retained as a saved user record.

This position does not prevent necessary short-lived technical/security processing where required for operation or security.

---

## 6. REPORT TIMESTAMPS AND PDF FILES

Saved vehicle reports contain a report-generation timestamp.

When a user generates a PDF for download or sharing, the PDF is generated for the user. Hidden History does not retain an archived copy of the generated PDF as a separate stored document.

The underlying saved report remains subject to the account/report retention rules described in this document.

---

## 7. LEGAL RETENTION

Information may be retained only where there is a documented lawful reason, such as:

- Compliance with a legal obligation
- Establishing, exercising or defending legal claims
- Fraud prevention or investigation
- Security and abuse prevention
- Accounting or transaction records where legally required
- Regulatory requirements

Any retained information must remain limited to what is necessary for the applicable purpose and must be protected appropriately.

---

## 8. RETENTION POSITION

| Information | Current retention position |
|---|---|
| Unsaved vehicle searches | Not retained as search history |
| Account information | Retained while the account is active, then deleted subject to lawful retention |
| Saved vehicle reports | Retained while saved/account-linked, then deleted when the user deletes them or the account is deleted, subject to lawful retention |
| Report-generation timestamp | Retained as part of the saved report while the report is retained |
| Generated PDFs | Not archived by Hidden History |
| Security/technical records | Only for as long as reasonably necessary for security, troubleshooting, abuse prevention or legal purposes |
| Payment/transaction records | Google Play controls payment processing; Hidden History retains only information it actually receives and needs for entitlement, support, accounting or legal purposes |
| Support/privacy requests | Retained only as reasonably necessary to handle the request, demonstrate compliance and defend legal claims |

No arbitrary universal retention period is adopted where the applicable purpose does not require one.

---

## 9. BACKUPS

Deleted information may remain temporarily in encrypted backups or disaster-recovery systems where immediate deletion is not technically possible.

Where this occurs, deleted information must not be returned to normal operational use and should be removed through the applicable backup lifecycle.

Actual backup retention periods must follow the production provider configuration and contractual arrangements.

---

## 10. THIRD-PARTY PROVIDERS

Current relevant services include:

- Supabase — application database/auth/backend processing
- Google Play / Google Play Billing — application distribution and paid purchase processing
- Gemini — Pro advert-analysis processing
- DVLA — source vehicle information
- DVSA/MOT — source MOT information
- eBay API — vehicle-parts sourcing functionality

Firebase is not currently used as a production data store for Hidden History.

MIB/MIAFTR is not currently connected and is future-only.

---

## 11. USER CONTROL

Users may delete saved reports where the application provides the relevant function.

Users may request account deletion.

Hidden History does not use retained vehicle-search information to build advertising profiles, sell personal information or conduct behavioural profiling.

---

## 12. CONTACT

Account deletion, privacy and retention enquiries:

SovereignSoftwareLtd@gmail.com

The external account-deletion resource must be kept publicly accessible and current before public release.

---

## 13. REVIEW TRIGGERS

This document must be reviewed if:

- The production database changes
- A new processor is introduced
- Retention behaviour changes
- New categories of personal data are introduced
- New payment arrangements are introduced
- Analytics or advertising are introduced
- New vehicle-data providers are introduced
- MIB/MIAFTR becomes operational
- The legal operating entity changes

---

END OF ACCOUNT DELETION AND DATA RETENTION
