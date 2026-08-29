# HIDDEN HISTORY
# DATA PROTECTION IMPACT ASSESSMENT

Status: PRE-BETA / WORKING DRAFT

Document owner:
[TODO — TO BE CONFIRMED]

Date created:
[TODO — TO BE CONFIRMED]

Last reviewed:
[TODO — TO BE CONFIRMED]

---

## 1. PURPOSE

This Data Protection Impact Assessment (DPIA) documents the data
processing activities undertaken by Hidden History and assesses whether
those activities may create risks to individuals' privacy or other
data-protection rights.

The purpose of this document is to identify:

- What personal data Hidden History processes
- Why the data is processed
- How the data is processed
- Where risks may arise
- What controls are used to reduce those risks
- Whether additional safeguards are required

This document is a pre-beta working assessment and must be reviewed
against the final production implementation before public beta release.

---

## 2. CURRENT HIDDEN HISTORY SERVICE

The current Hidden History application includes:

- Free vehicle search
- Free vehicle/advert analysis
- Pro/paid vehicle search
- MOT history display
- MOT test results
- MOT advisories
- MOT failures
- Saved vehicle search reports
- User accounts and profiles
- Affiliate links to vehicle-parts suppliers

The current application does not include:

- Marketplace
- Messaging
- Community
- Trader accounts
- Business accounts
- Recurring subscriptions
- MIB/MIAFTR data
- Other future vehicle-history databases unless separately implemented

---

## 3. DATA PROCESSING IDENTIFIED

Potential processing includes:

### Account data

- Email address
- Authentication identifier
- Profile information supplied by the user
- Account status
- Pro entitlement information where applicable

### Vehicle-search data

- Vehicle registration number
- Vehicle information returned by official sources
- MOT history
- MOT mileage readings
- MOT advisories
- MOT failures
- Search timestamps

### Saved report data

- Vehicle information
- Search information
- Advert analysis information
- MOT information
- Report creation date
- Account association

### Technical/security data

- IP address where collected by infrastructure
- Device or application information
- Authentication/session information
- Security logs
- Error and diagnostic information

[TODO — CONFIRM FINAL DATA INVENTORY]

---

## 4. PURPOSES OF PROCESSING

Data may be processed to:

- Provide vehicle searches
- Retrieve vehicle information
- Provide MOT information
- Provide vehicle/advert analysis
- Save reports
- Maintain user accounts
- Authenticate users
- Provide Pro access
- Prevent abuse
- Maintain security
- Diagnose technical problems
- Respond to support requests
- Meet legal obligations

---

## 5. NECESSITY AND PROPORTIONALITY

Hidden History should only collect information that is reasonably
necessary for the relevant purpose.

The application should avoid collecting unnecessary personal information.

Vehicle registration information should only be processed where required
to provide vehicle-related functionality or another documented purpose.

Personal information should not be retained indefinitely without a
documented reason.

---

## 6. PRIVACY RISKS

Potential risks include:

### Risk 1 — Unauthorised account access

An attacker could gain access to a user's account and view saved reports
or other account information.

Control measures may include:

- Secure authentication
- Access controls
- Server-side authorisation
- Secure session management
- Appropriate password/security controls
- Monitoring for suspicious activity

---

### Risk 2 — Unauthorised Pro access

A user may attempt to bypass Pro restrictions.

Controls include:

- Server-side Pro entitlement verification
- Authentication before Pro access
- No reliance on client-side entitlement alone
- Restricted server endpoints
- Server-controlled entitlement information

---

### Risk 3 — Exposure of API credentials

Government-data API credentials could be exposed if stored in the
client application.

Control:

- Government API credentials must remain server-side.
- Secrets must not be embedded in the Android application.
- Edge Functions/server infrastructure should be used for protected
  API access.

---

### Risk 4 — Unauthorised access to saved reports

Saved vehicle reports may contain information associated with a user's
account.

Controls should include:

- Account-level access controls
- Database security policies
- Server-side authorisation
- Secure authentication
- Deletion mechanisms

---

### Risk 5 — Excessive data retention

Information may be retained longer than necessary.

Control:

- Maintain a formal Data Retention Schedule.
- Periodically review stored data.
- Delete or anonymise information when no longer required.

---

### Risk 6 — Third-party service compromise

Hidden History may rely on external providers.

Controls include:

- Appropriate provider due diligence
- Data-processing agreements where required
- Access limitation
- Data minimisation
- Appropriate security requirements

---

### Risk 7 — International data transfers

Some infrastructure providers may process information outside the UK.

Control:

- Maintain a processor register.
- Identify processing locations.
- Review international-transfer requirements.
- Implement appropriate safeguards where required.

---

## 7. VEHICLE DATA RISK

Vehicle information may appear non-personal in isolation.

However, information associated with an account, search history or
other identifying information may create a link to an individual.

Hidden History must therefore assess vehicle-search information in the
context in which it is processed rather than assuming that all vehicle
information is automatically non-personal.

---

## 8. SPECIAL CATEGORY DATA

Hidden History does not intend to collect special category personal data
as part of its normal vehicle-search service.

Users should not be encouraged to enter sensitive personal information
into vehicle-search fields.

[TODO — CONFIRM FINAL USER INPUT FIELDS]

---

## 9. AUTOMATED ANALYSIS

Hidden History may analyse vehicle and advertisement information.

The analysis is intended to assist users with vehicle research.

It should not be presented as making decisions about individuals.

The analysis should concern the vehicle, advertisement and available
vehicle information rather than profiling individuals.

---

## 10. CHILDREN

The final minimum-age position must be confirmed before beta.

[TODO — CONFIRM MINIMUM AGE]

The application should implement appropriate controls consistent with the
final age policy.

---

## 11. DATA SUBJECT RIGHTS

The application must provide an appropriate method for users to exercise
applicable rights, including where relevant:

- Access
- Correction
- Erasure
- Restriction
- Objection
- Data portability
- Withdrawal of consent

See:

PRIVACY_REQUEST_HANDLING_PROCEDURE.md

---

## 12. DATA BREACH RESPONSE

A documented incident-response procedure must exist.

See:

INCIDENT_AND_DATA_BREACH_PROCEDURE.md

Potential personal-data incidents must be assessed promptly.

---

## 13. RISK RATING

Overall privacy risk:

[TODO — COMPLETE FINAL RISK ASSESSMENT]

Initial assessment:

LOW / MEDIUM / HIGH

[TODO — SELECT FINAL RATING]

---

## 14. ADDITIONAL SAFEGUARDS REQUIRED

Before beta release, confirm:

- Final data inventory
- Final processors
- Final retention periods
- Final international-transfer position
- Final authentication/security architecture
- Final account deletion behaviour
- Final privacy request process
- Final minimum age
- Final analytics/SDK inventory

---

## 15. DPIA CONCLUSION

Based on the current planned Hidden History functionality, the principal
privacy risks appear capable of being controlled through appropriate
technical, organisational and procedural measures.

The DPIA must be reviewed whenever Hidden History introduces materially
new processing.

Examples include:

- New personal-data categories
- New external databases
- New analytics systems
- New payment providers
- Recurring subscriptions
- Business accounts
- Marketplace functionality
- Messaging
- Community functionality
- MIB/MIAFTR access

---

## 16. APPROVAL

DPIA completed by:

[TODO — NAME]

Role:

[TODO — ROLE]

Date:

[TODO — DATE]

Approved by:

[TODO — NAME]

Date:

[TODO — DATE]

---

END OF DATA PROTECTION IMPACT ASSESSMENT