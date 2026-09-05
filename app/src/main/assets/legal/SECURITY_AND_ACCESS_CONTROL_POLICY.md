HIDDEN HISTORY

SECURITY AND ACCESS CONTROL POLICY

Status: PRODUCTION CONTROL DOCUMENT
Version: 1.0
Effective Date: Date of first adoption
Last Updated: 3 September 2026

Document Owner: Gary Lee Chantler
Operating Identity: Gary Lee Chantler, trading/operating as Hidden History
Contact: SovereignSoftwareLtd@gmail.com

---

1. PURPOSE

This policy defines the security and access-control principles used to protect Hidden History accounts, application infrastructure, vehicle information, saved reports and personal information.

The policy applies to the Hidden History Android application, backend services, databases, external service integrations and administrative access.

Hidden History applies a risk-based security approach and uses appropriate technical and organisational measures to protect information against unauthorised or unlawful access, alteration, loss, destruction or disclosure.

---

2. CORE SECURITY PRINCIPLE

Client applications must not be treated as trusted environments.

The Android application is considered an untrusted client for security purposes.

Security-sensitive decisions must therefore be enforced independently of client-controlled state.

This includes, where applicable:

- authentication;
- authorisation;
- Pro entitlement;
- access to saved reports;
- privileged operations;
- administrative functions; and
- access to protected backend resources.

A user must not be able to obtain restricted functionality simply by modifying the application, local storage, request parameters or other client-controlled values.

---

3. AUTHENTICATION

Hidden History uses authenticated user accounts for functionality that requires an account.

Authentication is provided through the approved authentication infrastructure used by Hidden History.

Authentication credentials and authentication-related information must be handled securely.

Hidden History must not:

- store user passwords in plaintext;
- expose authentication credentials to other users;
- place privileged credentials in the Android client;
- expose authentication secrets through logs; or
- rely on client-side authentication state as proof of backend authorisation.

Authentication must occur before access is granted to protected account functionality.

---

4. AUTHORISATION

Authentication and authorisation are separate security controls.

Successfully logging in does not automatically grant access to every Hidden History function or item of data.

Authorisation must determine whether the authenticated user is permitted to access the requested resource or functionality.

Access must follow the principle of least privilege.

Users should only have access to information and functionality necessary for their legitimate use of the service.

NCSC guidance similarly recommends restricting access permissions to the minimum necessary and ensuring requests are authenticated before access is granted.

---

5. PRO ACCESS AND ENTITLEMENT

Pro functionality must not rely solely upon:

- client-side flags;
- UI state;
- hidden buttons;
- local storage;
- locally stored entitlement values;
- client-controlled request parameters; or
- other information that can be modified by the user.

The backend must independently verify the user's Pro entitlement before providing restricted Pro functionality.

The client may display entitlement information for usability, but the client must not be the final authority determining whether paid functionality is authorised.

Where payment information is required to establish entitlement, the authoritative purchase/entitlement information must be validated through the applicable Google Play Billing and Hidden History backend processes.

---

6. API KEYS AND SECRETS

Government API credentials, service credentials, database credentials and other privileged secrets must not be embedded in the Android client.

Privileged secrets must remain within secure server-side configuration or secret-management systems.

Secrets must not be:

- committed to source control;
- included in publicly accessible repositories;
- displayed in application logs;
- returned to normal clients;
- placed in client-readable configuration;
- embedded in application binaries where they provide privileged access; or
- unnecessarily disclosed to third-party services.

If a secret is suspected of being exposed, it must be treated as compromised and rotated or revoked as appropriate.

---

7. DATABASE ACCESS

Database access must use appropriate authentication and authorisation controls.

Users must only be able to access information for which they are authorised.

Saved vehicle reports must be protected against unauthorised access, including attempts to access reports by guessing, modifying or enumerating identifiers.

Database security policies must enforce access boundaries independently of the Android application's user interface.

Where row-level or equivalent database access controls are available, they should be used to enforce user-level data isolation.

---

8. SERVICE-ROLE AND ADMINISTRATIVE CREDENTIALS

Supabase service-role credentials and equivalent administrative credentials are privileged credentials.

They must remain server-side and must never be exposed to ordinary client applications.

Administrative credentials must only be used where the privileged operation genuinely requires them.

Where possible, privileged operations should be narrowly scoped and protected by appropriate authentication and access controls.

---

9. ADMINISTRATIVE ACCESS

Administrative access to Hidden History infrastructure and user data must be restricted to authorised personnel.

At launch, administrative access is controlled by the operator, Gary Lee Chantler.

Where administrative access is provided, the following principles apply:

- individual administrator accounts should be used where supported;
- shared administrator credentials should be avoided;
- strong authentication should be used;
- privileged access should be limited to what is necessary;
- unused access should be removed;
- access should be reviewed periodically;
- administrative interfaces should not be exposed unnecessarily; and
- privileged actions should be capable of being investigated through appropriate records or logs.

NCSC guidance identifies administrative access as privileged and recommends that management functions be restricted to authorised and authenticated administrators.

---

10. LOGGING AND SECURITY MONITORING

Hidden History may maintain security and diagnostic logs where necessary to operate, secure and investigate the service.

Logging must follow data-minimisation principles.

Logs should avoid unnecessary personal information and must not contain:

- passwords;
- authentication secrets;
- API keys;
- service-role credentials; or
- other sensitive secrets.

Security-related logging may include information necessary to investigate:

- authentication events;
- access-control failures;
- suspicious activity;
- system errors;
- security incidents; and
- privileged operations.

Logs must be access-controlled and retained only for as long as reasonably required for operational, security, legal or audit purposes.

Where a security event involves personal data, it must be handled in accordance with the Incident and Data Breach Procedure.

The ICO states that organisations should implement security measures appropriate to the risks and that security can include access controls, security monitoring and recovery arrangements.

---

11. CLIENT SECURITY

Hidden History must assume that users may be capable of:

- modifying the application;
- inspecting application behaviour;
- inspecting network requests;
- changing endpoint references;
- modifying local application state;
- attempting to replay requests;
- reverse engineering application code; or
- constructing requests outside the normal application.

These possibilities do not automatically represent a security incident.

However, security boundaries must not depend upon the assumption that the client cannot be modified.

The backend must independently enforce security-sensitive controls.

---

12. DATA IN TRANSIT AND AT REST

Personal information and other sensitive information must be protected using appropriate security measures.

Hidden History should use secure transport mechanisms for communications between the application, backend services and external services.

Where personal information is stored on systems controlled by Hidden History or its processors, appropriate protection must be applied according to the nature and risk of the information.

Encryption may form part of these measures where appropriate.

The ICO identifies encryption and other technical and organisational measures as potential safeguards for protecting personal data against unauthorised or unlawful processing.

---

13. ACCESS TO SAVED VEHICLE REPORTS

Saved vehicle reports are associated with user accounts and must only be accessible to the authorised account holder or authorised administrative processes.

A report identifier must not itself be treated as sufficient authorisation.

Where a user deletes a saved report, Hidden History must remove the report from the live user-accessible system in accordance with the applicable retention requirements.

Account deletion must be handled in accordance with:

ACCOUNT_DELETION_AND_DATA_RETENTION.md

---

14. THIRD-PARTY SERVICES AND SUPPLY CHAIN

Third-party services that process or provide access to Hidden History information must be assessed before being relied upon for sensitive processing.

Current relevant services include:

- Supabase;
- Google Play Billing;
- DVLA services;
- DVSA/MOT services;
- Gemini for Pro advert analysis; and
- eBay API functionality for relevant parts links.

Third-party services must only be used for their intended and documented purpose.

Hidden History must maintain appropriate records concerning relevant processors and third-party data services.

See:

DATA_PROCESSOR_REGISTER.md

DATA_PROCESSING_AGREEMENTS_REGISTER.md

THIRD_PARTY_DATA_SOURCES.md

---

15. SOFTWARE DEVELOPMENT AND SOURCE CONTROL

Security-sensitive information must not be committed to source control.

This includes:

- API keys;
- service-role credentials;
- passwords;
- private authentication credentials;
- production secrets; and
- other privileged configuration secrets.

Development and production configuration should be separated where reasonably practicable.

Security-sensitive changes should be reviewed and tested before production deployment.

Known vulnerabilities should be addressed according to their risk.

---

16. SECURITY TESTING

Hidden History must periodically review and test security controls appropriate to the application's risk.

Testing may include:

- authentication testing;
- authorisation testing;
- Pro entitlement testing;
- database access testing;
- identifier-access testing;
- API security testing;
- client tampering tests;
- secret-exposure checks;
- dependency/security review; and
- incident-response testing where appropriate.

Testing should confirm that security boundaries continue to be enforced after material application or backend changes.

---

17. INCIDENT RESPONSE

Security incidents must be documented, assessed and investigated.

Where an incident may involve personal data, the matter must be handled in accordance with:

INCIDENT_AND_DATA_BREACH_PROCEDURE.md

Security incidents may include:

- unauthorised account access;
- unauthorised database access;
- exposed credentials;
- compromised administrative accounts;
- unauthorised report access;
- malicious requests;
- significant application compromise; or
- other events that threaten the confidentiality, integrity or availability of protected information.

---

18. VULNERABILITY AND SECURITY REPORTING

Security concerns or suspected vulnerabilities relating to Hidden History should be reported to:

SovereignSoftwareLtd@gmail.com

Reports may concern:

- suspected security vulnerabilities;
- exposed credentials or secrets;
- unauthorised access;
- account-security issues;
- inappropriate data access;
- application security weaknesses; or
- other security concerns.

Reports should contain sufficient information to allow Hidden History to investigate the issue, where the reporter can safely provide that information.

Hidden History will assess legitimate security reports and may take reasonable steps to investigate, mitigate and remediate confirmed vulnerabilities.

---

19. SECURITY PRINCIPLES FOR FUTURE FEATURES

Any material change to Hidden History functionality must consider its security implications before release.

This includes future introduction of:

- marketplace functionality;
- messaging;
- community functionality;
- trader accounts;
- business accounts;
- public profiles;
- additional external data sources; or
- new payment or entitlement systems.

New functionality must not bypass the existing authentication, authorisation, data-isolation and least-privilege principles.

---

20. DATA PROTECTION BY DESIGN

Security must be considered during the design, development and operation of processing involving personal data.

Hidden History will seek to:

- minimise personal information collected;
- restrict access to authorised users;
- separate privileged and ordinary access;
- protect information against unauthorised processing;
- review security controls when processing changes; and
- maintain appropriate technical and organisational safeguards.

The ICO's current guidance states that data protection by design applies from the design stage and throughout the processing lifecycle.

---

21. CURRENT SECURITY POSITION

At the time this policy was last reviewed:

Authentication: Authenticated user accounts are used where required.

Authorisation: Backend access controls are required for protected functionality.

Pro entitlement: Must be verified server-side.

Database: Access controlled through appropriate authentication and authorisation mechanisms.

Service-role credentials: Server-side only.

API keys/secrets: Must remain server-side and must not be embedded in the Android client.

Analytics: None.

Advertising: None.

User tracking: None.

Saved reports: Account-controlled and protected against unauthorised access.

Security contact: SovereignSoftwareLtd@gmail.com

Incident procedure: INCIDENT_AND_DATA_BREACH_PROCEDURE.md

---

22. REVIEW

This policy must be reviewed:

- before beta release;
- before production release;
- after a material security incident;
- when authentication or authorisation architecture changes;
- when Pro entitlement architecture changes;
- when database security controls change;
- when a new privileged service or integration is introduced;
- when a material third-party SDK or service is introduced; or
- when applicable security or data-protection requirements materially change.

Security controls must remain consistent with the actual production architecture.

---

23. DOCUMENT CONTROL

This document forms part of the Hidden History legal, security and compliance documentation.

The policy must accurately reflect the security controls actually implemented in the production application and backend.

Document Owner: Gary Lee Chantler
Operating Identity: Gary Lee Chantler, trading/operating as Hidden History
Version: 1.0
Last Reviewed: 3 September 2026
Contact: SovereignSoftwareLtd@gmail.com

---

END OF SECURITY AND ACCESS CONTROL POLICY