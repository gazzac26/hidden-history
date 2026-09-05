HIDDEN HISTORY

CONSUMER PURCHASE AND DIGITAL SERVICE NOTICE

Status: CONTROLLED PRODUCTION DOCUMENT

Version: 1.0

Last updated: 3 September 2026

Document owner:
Gary Lee Chantler

Operating identity:
Gary Lee Chantler, trading/operating as Hidden History

Consumer/legal contact:
SovereignSoftwareLtd@gmail.com

---

1. PURPOSE

This document records the consumer information and purchase-flow
requirements applicable to paid Hidden History functionality.

It supports the implementation and review of the Hidden History Pro
purchase flow and must remain consistent with:

- The Hidden History Terms and Conditions
- The Privacy Policy
- The Pro Search Purchase Terms
- The Refund and Cancellation Policy
- The Google Play Billing implementation
- The actual Pro product configuration

The purchase interface must provide consumers with clear and
understandable information before they complete a purchase.

---

2. CURRENT PRO PURCHASE MODEL

Hidden History's current intended launch model is:

- Pro vehicle search and analysis
- One-off payment per applicable Pro report/search
- No recurring subscription
- No automatic renewal
- Exact consumer price displayed before purchase
- Payment processed through Google Play Billing

The current target consumer price range is:

£2.99–£4.99 per Pro report

The actual price charged must be the price displayed to the consumer
before purchase and may be changed in the production configuration.

The application must not describe Pro as a subscription.

Future subscription functionality, if introduced, requires separate
implementation and legal review before activation.

---

3. WHAT THE CONSUMER IS PURCHASING

The Pro purchase provides access to the applicable enhanced Hidden
History vehicle-analysis functionality.

Depending on the applicable Pro product configuration, this may include:

- Enhanced vehicle-data analysis
- Enhanced advert analysis
- Analysis of vehicle information available to Hidden History
- Hidden History's own deterministic analysis
- AI-assisted advert analysis where applicable
- A generated Pro vehicle report

The precise contents of the purchased Pro product must be reflected in
the production purchase screen and product description.

Hidden History must not represent information, data sources or
functionality as included where that functionality is not actually
available.

---

4. INFORMATION PRESENTED BEFORE PURCHASE

Before a consumer completes a Pro purchase, the purchase interface must
clearly present, as applicable:

- The name or description of the Pro product
- What the consumer will receive
- The price
- The currency
- Any applicable taxes or charges included in the displayed price
- Confirmation that the payment is a one-off purchase
- That the purchase is not a recurring subscription
- How the purchased functionality is delivered or made available
- Any material limitations affecting the service
- Relevant refund and cancellation information
- A route for obtaining support

The information must be presented clearly enough for the consumer to
understand the transaction before confirming payment.

---

5. PRICE

The Pro purchase price must be displayed clearly before the consumer
confirms payment.

The production purchase price is controlled by the actual Google Play
product configuration.

The legal documentation must not state a fixed price if the production
price is capable of being changed.

The price displayed to the consumer must correspond to the applicable
Google Play purchase.

---

6. PAYMENT PROCESSING

Payment provider:
Google Play Billing

Hidden History does not intend to collect or store complete payment-card
details directly.

Payment authentication, card processing and applicable payment
transactions are handled through Google Play's payment infrastructure.

Hidden History may receive or process transaction information necessary
to verify and provide the purchased Pro entitlement.

The actual information received and retained must match the production
Google Play Billing implementation and the Privacy Policy.

---

7. DELIVERY AND ACCESS

Following successful payment, the purchased Pro functionality should be
made available through the Hidden History application in accordance
with the applicable product configuration.

The consumer should be able to understand what has been purchased and
when the Pro functionality has been made available.

Where a Pro report is generated following a successful purchase, the
report should identify the relevant report-generation date/time where
that information forms part of the report.

---

8. PURCHASE CONFIRMATION

The purchase process must provide appropriate confirmation following a
successful transaction.

The confirmation should identify, where applicable:

- The Pro product purchased
- The amount paid
- The purchase date
- The relevant Google Play transaction or purchase reference where
  available to Hidden History
- The applicable terms
- Relevant refund/cancellation information
- How to contact Hidden History for support

The consumer must also retain access to any applicable Google Play
purchase record and receipt provided by Google Play.

---

9. REFUNDS

Refunds must be handled in accordance with:

- Applicable UK consumer law
- The Hidden History Refund and Cancellation Policy
- Google Play's applicable refund processes and policies
- The actual circumstances of the purchase

A refund may be appropriate where, for example:

- The purchased service is not supplied
- The service materially fails to match its description
- A technical failure prevents the purchased service from being
  delivered
- A duplicate or incorrect charge occurs
- A refund is otherwise required by applicable law

Hidden History must not attempt to remove statutory consumer rights by
contractual wording.

Google Play provides its own UK refund processes and policies, and
consumers may also contact the app developer regarding purchase issues.

---

10. CANCELLATION AND WITHDRAWAL RIGHTS

The applicable cancellation and withdrawal position must be determined
by the nature of the Pro service and the way in which it is supplied.

The purchase flow must not contain a blanket statement that a consumer
has no cancellation or withdrawal rights.

Where applicable law provides a statutory cancellation or withdrawal
right, the consumer must receive the required information and any
required consent or acknowledgement must be obtained through the
purchase flow.

For digital content and digital services, the applicable legal treatment
can differ. Google Play's current UK guidance distinguishes digital
content from digital services and identifies different withdrawal
treatments for each.

Accordingly, the final Pro purchase flow must be checked against the
actual classification and delivery mechanism of the Pro product before
production release.

---

11. CONSUMER RIGHTS

Nothing in the Pro purchase process or associated documentation removes
or restricts consumer rights that cannot lawfully be excluded.

Hidden History must provide services in accordance with applicable
consumer law and the contractual description presented to the consumer.

Contract terms and consumer notices must be fair and transparent.
Unfair terms may not be enforceable.

---

12. SUBSCRIPTIONS

Subscriptions are NOT currently part of Hidden History.

The current Pro model is a one-off purchase.

There is currently:

- No recurring Pro subscription
- No automatic Pro renewal
- No subscription billing cycle
- No subscription cancellation cycle

If subscriptions are introduced in the future, the purchase flow,
consumer information, cancellation process and legal documentation must
be reviewed and updated before implementation.

---

13. BUSINESS PLANS

Business pricing is NOT currently part of Hidden History.

Business, trader or commercial account pricing must not be implied by
this notice.

Any future business service must have its own appropriate commercial
terms and consumer/business purchase documentation.

---

14. FUTURE PAYMENT PROVIDERS

Google Play Billing is the current launch payment mechanism.

No additional payment provider is currently authorised by this document.

If another payment provider is introduced, the relevant:

- Privacy Policy
- Terms and Conditions
- Payment documentation
- Refund documentation
- Data-processing documentation
- Purchase flow
- Processor register

must be reviewed and updated before that provider is used.

---

15. PURCHASE-FLOW IMPLEMENTATION REQUIREMENTS

Before production release, the actual Pro purchase flow must be checked
to confirm that:

- The displayed price matches the production Google Play product.
- The product description accurately describes the service.
- The purchase is clearly identified as one-off.
- No subscription language is displayed.
- The consumer knows what is supplied after payment.
- Applicable refund information is accessible.
- Applicable cancellation/withdrawal information is accessible.
- Required consumer acknowledgements are captured where applicable.
- Successful purchases result in the correct Pro entitlement.
- Failed or cancelled transactions do not incorrectly grant Pro access.
- Duplicate transactions are handled appropriately.
- Purchase information does not expose unnecessary payment information.
- Support contact information is accessible.

---

16. RELATIONSHIP WITH OTHER DOCUMENTS

This document must be read together with:

- "TERMS_AND_CONDITIONS.md"
- "PRO_SEARCH_PURCHASE_TERMS.md"
- "REFUND_AND_CANCELLATION_POLICY.md"
- "PRIVACY_POLICY.md"
- "DATA_PROTECTION_IMPACT_ASSESSMENT.md"
- "DATA_PROCESSING_AGREEMENTS_REGISTER.md"
- "DATA_PROCESSOR_REGISTER.md"
- "DOCUMENT_REGISTER.md"

Where a conflict exists, the production implementation and applicable
law must be reviewed and the affected documentation corrected.

---

17. PRODUCTION GATE

Before paid Pro functionality is enabled for public users, the following
must be verified against the deployed application:

Product:
Pro product exists in the production Google Play configuration.

Price:
Production price is confirmed and displayed correctly.

Billing:
Google Play Billing is operational.

Entitlement:
Successful purchases correctly grant Pro access.

Refunds:
Refund handling is implemented and consistent with the applicable
policy.

Cancellation/withdrawal:
The applicable legal treatment has been confirmed against the actual
Pro delivery model.

Consumer information:
Required pre-purchase information is displayed clearly.

Support:
SovereignSoftwareLtd@gmail.com is available as the consumer support
contact.

---

18. APPROVAL

Document owner:
Gary Lee Chantler

Role:
Operator / Data Controller

Date reviewed:
3 September 2026

Production purchase flow approval:
To be recorded following final verification of the deployed Google Play
Billing implementation.

---

DOCUMENT CONTROL

Document: Consumer Purchase and Digital Service Notice

Version: 1.0

Status: Controlled Production Document

Owner: Gary Lee Chantler

Operating identity: Gary Lee Chantler, trading/operating as Hidden
History

Contact: SovereignSoftwareLtd@gmail.com

Last reviewed: 3 September 2026

Review trigger: Material change to Pro functionality, pricing,
payment provider, purchase flow, refund/cancellation arrangements,
consumer law or applicable Google Play requirements.

---

END OF CONSUMER PURCHASE AND DIGITAL SERVICE NOTICE