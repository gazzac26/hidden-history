# HIDDEN HISTORY — LEGAL DOCUMENTATION

Status: PRODUCTION CONTROLLED LEGAL PACK

Version: 1.0  
Last updated: 4 September 2026  
Document owner: Gary Lee Chantler  
Legal operator/controller: Gary Lee Chantler, trading/operating as Hidden History  
Contact: SovereignSoftwareLtd@gmail.com

---

## PURPOSE

This folder contains the legal, privacy, consumer, disclosure and operational governance documentation for Hidden History.

The pack is intended to describe the service actually being operated, protect users and provide an accountable framework for release and future development.

It is not a substitute for independent solicitor advice where such advice is required.

---

## CURRENT HIDDEN HISTORY LAUNCH SCOPE

The current service comprises:

- Free vehicle registration search
- Free vehicle/advert analysis
- Official DVLA vehicle information
- Official DVSA/MOT information
- MOT failures and advisories where supplied by the source
- Saved vehicle reports
- User accounts/profiles
- Pro/paid enhanced vehicle search and analysis
- Hidden History's own deterministic analysis
- Gemini-assisted Pro advert analysis
- Vehicle-parts sourcing links associated with eBay functionality where the active commercial arrangement permits
- PDF generation for user download/share

The current service does not include:

- Marketplace
- Community
- Messaging
- Trader accounts
- Business accounts
- Recurring subscriptions
- Public social profiles
- General advertising
- Behavioural analytics

---

## CURRENT DATA ARCHITECTURE

Current production data storage:

Supabase.

Firebase is not currently used as a production data store for Hidden History.

Unsaved vehicle searches are not retained as persistent search history.

Saved reports are stored in Supabase for the user's access and may be deleted by the user.

Account deletion is intended to delete associated account-linked data from live systems, subject to lawful retention.

---

## CURRENT DATA SOURCES

Current vehicle-data sources:

1. DVLA
2. DVSA/MOT

Gemini is an analysis service, not a vehicle-record source.

eBay is associated with vehicle-parts sourcing functionality, not the authoritative vehicle-data record.

MIB/MIAFTR is future-only and is not connected.

---

## CURRENT PRO SERVICE

The current paid model is a one-off Pro purchase.

The target price range is £2.99–£4.99, with the exact price shown to the customer before purchase.

Payment is through Google Play Billing.

There is no recurring subscription at launch.

---

## PRO ADVERT ANALYSIS

For Pro advert analysis, Hidden History may send to Gemini:

- Hidden History's own advert-analysis results
- The raw advert
- Relevant vehicle data received for the analysis

Hidden History does not intentionally send account/profile information to Gemini.

Advert text may contain personal information inserted by a seller. Hidden History cannot guarantee that third-party advert content contains no personal information.

---

## PRIVACY PRINCIPLES

Hidden History's privacy model is deliberately limited.

The service does not operate:

- General-purpose advertising
- Behavioural advertising
- Firebase Analytics
- Google Analytics
- General behavioural analytics
- Sale of personal information
- Personal-data harvesting for unrelated purposes

The service should collect and retain only information necessary for the service, security, legal compliance and user-controlled saved functionality.

---

## AFFILIATE PRINCIPLES

Vehicle-parts links may use affiliate arrangements.

Where a link may generate commission, the relationship must be clearly disclosed.

Affiliate relationships must not influence vehicle-data results, MOT results, risk findings or analysis.

Part compatibility must not be represented as guaranteed unless it has actually been established.

---

## ACCOUNT DELETION

Because Hidden History allows account creation, production must provide:

- A clear in-app account-deletion route
- An external account-deletion web resource
- Deletion of associated account data, subject only to clearly disclosed lawful retention

The external deletion resource must be publicly accessible before Google Play publication.

---

## DATA PROTECTION COMPLAINTS

Hidden History maintains a data-protection complaints process.

Data-protection complaints must be acknowledged within 30 days and investigated appropriately, with the outcome communicated without unjustifiable or excessive delay.

---

## SECURITY

The client application is treated as untrusted.

Security-sensitive authorisation, including Pro entitlement, must be enforced server-side.

Secrets and service-role credentials must not be embedded in the client.

Saved reports must be protected against unauthorised access.

---

## LEGAL DOCUMENT CONTROL

The legal pack must always describe the actual production service.

A future feature must not be presented as live merely because it is planned.

A future partnership must not be presented as active until approval, contract, integration and required legal review are complete.

---

## FUTURE FEATURES

Potential future functionality includes:

- Marketplace
- Community
- Messaging
- Trader/business accounts
- Subscriptions
- MIB/MIAFTR integration
- Additional vehicle-data sources
- Expanded parts services

These are future capabilities and require separate legal, privacy, security and consumer-law review before activation.

---

## MASTER RELEASE GATES

Before public paid beta, verify:

1. Legal operator/controller details
2. Public address
3. Support/privacy contact
4. Privacy Policy URL
5. External account-deletion resource
6. Google Play Data Safety declaration
7. Actual production data flows
8. Actual processors and provider contracts
9. Actual Pro price
10. Google Play Billing configuration
11. Refund/cancellation flow
12. Account deletion behaviour
13. Retention configuration
14. Affiliate arrangement and disclosure
15. Gemini production configuration
16. Security controls
17. Final legal-document consistency
18. Final functional testing
19. Closed/beta testing requirements
20. Any required independent legal review

---

## CURRENT LEGAL REFERENCE POSITION

The documentation has been updated against current UK ICO and Google Play requirements, including the Data (Use and Access) Act 2025 changes now in force.

The pack must continue to be reviewed because ICO guidance is being updated as the new legal framework beds in.

---

END OF README
