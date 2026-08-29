# HIDDEN HISTORY
# SECURITY AND ACCESS CONTROL POLICY

Status: PRE-BETA / WORKING DRAFT

---

## 1. PURPOSE

This policy defines the security and access-control principles used to
protect Hidden History accounts, application infrastructure, vehicle
data and personal information.

---

## 2. CORE PRINCIPLE

Client applications must not be treated as trusted environments.

Security-sensitive decisions must be enforced server-side.

---

## 3. AUTHENTICATION

Hidden History uses authenticated user accounts where required.

Authentication credentials must be handled using the approved
authentication provider.

[TODO — CONFIRM FINAL AUTHENTICATION CONFIGURATION]

---

## 4. AUTHORISATION

Authentication and authorisation are separate controls.

Being logged in does not automatically grant access to Pro functionality.

Pro access must be verified server-side.

---

## 5. PRO ACCESS

Pro entitlement must not rely solely upon:

- Client-side flags
- UI state
- Hidden buttons
- Local storage
- Client-controlled request parameters

The server must verify the user's entitlement before returning
restricted Pro functionality.

---

## 6. API KEYS AND SECRETS

Government API credentials and other privileged secrets must not be
embedded in the Android client.

Secrets must be stored in secure server-side environment/configuration
systems.

Secrets must not be:

- Committed to source control
- Displayed in logs
- Returned to clients
- Embedded in application binaries

---

## 7. DATABASE ACCESS

Database access must use appropriate authentication and authorisation
controls.

Users should only be able to access information they are authorised to
access.

Saved reports must not be readable merely by guessing identifiers.

---

## 8. SERVICE-ROLE CREDENTIALS

Supabase service-role or equivalent administrative credentials must
remain server-side.

They must never be exposed to normal client applications.

---

## 9. LOGGING

Security and diagnostic logs should avoid unnecessary personal data.

Logs must be retained only for the required period.

[TODO — FINAL LOGGING/RETENTION POLICY]

---

## 10. ADMINISTRATIVE ACCESS

Administrative access must be restricted to authorised personnel.

Where possible:

- Use individual administrator accounts
- Avoid shared credentials
- Use strong authentication
- Review access periodically
- Remove access when no longer required

---

## 11. INCIDENT RESPONSE

Security incidents must be documented and investigated.

See:

INCIDENT_AND_DATA_BREACH_PROCEDURE.md

---

## 12. CLIENT SECURITY

The application must not assume that users cannot:

- Modify the client
- Inspect network requests
- Change endpoint references
- Reverse engineer application code
- Manipulate local state

Server-side controls must therefore enforce security boundaries.

---

## 13. THIRD-PARTY SECURITY

Third-party services must be assessed before being relied upon for
sensitive processing.

See:

DATA_PROCESSOR_REGISTER.md

---

## 14. VULNERABILITY REPORTING

Security concerns may be reported to:

[TODO — SECURITY CONTACT]

---

## 15. REVIEW

This policy must be reviewed whenever the application architecture or
security model materially changes.

---

END OF SECURITY AND ACCESS CONTROL POLICY