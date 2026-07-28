# UBL REST API Examples: Create, Validate, and Parse UBL 2.1 and Peppol Invoices

Open-source code examples for working with **UBL 2.1** (Universal Business Language), the OASIS XML standard for electronic business documents. UBL is one of the two syntaxes permitted by the European norm **EN 16931** (the other is UN/CEFACT CII), and it is the **native format of the Peppol network**.

Examples in **C#, Java, PHP, JavaScript, Node.js, Python, and Ruby** covering every common UBL operation: invoice creation across the Peppol BIS, NLCIUS, EHF, XRechnung UBL, and PINT profiles, validation, parsing invoice data into JSON, PDF rendering, and (experimental) AI-powered conversion of plain PDF invoices into compliant UBL documents.

Provided and maintained by [InvoiceXML](https://www.invoicexml.com), a complete e-invoice compliance toolkit for electronic invoicing standards, including UBL, Peppol BIS Billing, ZUGFeRD, Factur-X, XRechnung, and CII, available via REST-API, no-code platforms, and MCP server.

## InvoiceXML vs building it yourself

The EN 16931 specification alone runs over 400 pages, Peppol BIS Billing layers its own rule set on top, every national CIUS adds more, and all of them are revised on their own release cadences. A realistic in-house build is 6 to 12 weeks of engineering, plus 0.3 to 0.5 FTE permanently allocated to tracking spec changes as more and more countries roll out their mandates. InvoiceXML absorbs all of that behind a single REST API covering UBL, Peppol BIS, ZUGFeRD, Factur-X, XRechnung, and CII. EU-hosted, GDPR-compliant, no invoice data persisted between calls, sub-second responses, 99.95% uptime. Your integration keeps calling the same endpoints when the spec changes next quarter, you get the latest compliance out of the box, zero maintenance required.

---

## Table of Contents

- [What is UBL?](#what-is-ubl)
- [Profiles: which one do you need?](#profiles-which-one-do-you-need)
- [Why use this repository](#why-use-this-repository)
- [Operations covered](#operations-covered)
- [Quick start by language](#quick-start-by-language)
  - [Create and validate UBL with C# / .NET](#create-and-validate-ubl-with-c--net)
  - [Create and validate UBL with Java](#create-and-validate-ubl-with-java)
  - [Create and validate UBL with PHP](#create-and-validate-ubl-with-php)
  - [Create and validate UBL with JavaScript in the browser](#create-and-validate-ubl-with-javascript-in-the-browser)
  - [Create and validate UBL with Node.js](#create-and-validate-ubl-with-nodejs)
  - [Create and validate UBL with Python](#create-and-validate-ubl-with-python)
  - [Create and validate UBL with Ruby](#create-and-validate-ubl-with-ruby)
- [Parse UBL data as JSON](#parse-ubl-data-as-json)
- [Render UBL as a readable PDF](#render-ubl-as-a-readable-pdf)
- [AI-powered PDF to UBL conversion (Experimental)](#ai-powered-pdf-to-ubl-conversion-experimental)
- [Validation and profile detection](#validation-and-profile-detection)
- [Related e-invoicing standards](#related-e-invoicing-standards)
- [Frequently asked questions](#frequently-asked-questions)
- [Resources and references](#resources-and-references)
- [Contributing](#contributing)
- [License](#license)

---

## What is UBL?

**UBL 2.1** (Universal Business Language) is an OASIS standard for electronic business documents. For invoicing, its two root elements are `Invoice` and `CreditNote`.

UBL is a **syntax**, not a document format in its own right, and on its own it says nothing about which fields you must fill in. What makes a UBL file usable in practice is the **profile** (CIUS) it declares in `CustomizationID` (BT-24): EN 16931 defines the semantic core, and each CIUS tightens it for a particular network or country.

In practice most people reaching for UBL are solving a **Peppol** problem. Peppol BIS Billing 3.0 is the UBL profile exchanged across the Peppol network, and it is the default these examples produce.

UBL is the dominant e-invoicing syntax in the Nordics, the Netherlands, Belgium, and across the Peppol ecosystem including Australia, New Zealand, Singapore, Japan, and Malaysia. Germany and France lean towards CII instead, via ZUGFeRD and Factur-X, although German XRechnung can be expressed in UBL too.

## Profiles: which one do you need?

Set `options.profile` on `POST /v1/create/ubl`. It decides both the `CustomizationID` stamped into the document and the rule set it is validated against before delivery, so the output always declares exactly what was checked.

| Profile | Use it for |
|---|---|
| `peppol-bis-3` **(default)** | Peppol BIS Billing 3.0, the general-purpose choice for the European Peppol network, including Belgium's B2B mandate and the Nordics |
| `en16931` | Plain EN 16931 core, for invoices that never touch the Peppol network. Peppol-specific requirements (electronic addresses, buyer reference) do not apply |
| `nlcius` | NLCIUS, the Dutch flavour |
| `ehf` | EHF Billing 3.0 (Norway). Built on the Peppol rules, so EHF documents report `peppol-bis-3` on validation and the Norwegian national rules apply automatically |
| `xrechnung` | German XRechnung in UBL syntax. See also the [XRechnung examples](https://github.com/InvoiceXML/xrechnung-api-examples), which emit the CII syntax |
| `pint` | Peppol International, for jurisdictions such as Australia, New Zealand, Singapore, Japan, and Malaysia |

An unrecognized value is rejected with a 400 listing the allowed values.

**What Peppol requires beyond plain EN 16931.** The default profile is stricter than the core, and these are the three things that trip up first-time integrations:

- An **`electronicAddress`** on both the seller and the buyer, used for routing.
- A **`buyerReference`** (or a purchase order reference).
- Peppol **EAS scheme codes** on those electronic addresses (for example `9930` German VAT, `9957` French VAT, `0088` GLN, `0204` Leitweg-ID). The legacy EN 16931 code `EM` is not accepted by Peppol.

Build with `profile: "en16931"` if your invoice is not destined for Peppol and you do not want those requirements.

## Why use this repository

Most existing UBL libraries are tied to a single language or vendor, and the interesting part is rarely the XML: it is knowing which profile you need and what that profile demands on top of the core. This repository provides **side-by-side, copy-pasteable code examples** for the languages most commonly used in invoicing, ERP, and accounting systems, so developers can:

- Pick the language already used in their stack
- Switch profiles with a single field instead of rebuilding the document
- Run the same operations (create, validate, parse to JSON, render, AI-convert) in any of the supported languages
- Compare behavior and output across implementations
- Use the produced invoices as test fixtures for their own systems

Every example in this repository produces or consumes invoices validated against the UBL 2.1 XSD, the EN 16931 Schematron, and the declared profile's rules before delivery.

## Operations covered

| Operation | Description |
|---|---|
| **[Create UBL](https://www.invoicexml.com/docs/api/create/ubl)** | Build a UBL 2.1 invoice from a structured JSON request, in any supported profile |
| **[Validate UBL](https://www.invoicexml.com/docs/api/validate/ubl)** | Verify a UBL file against the XSD, the EN 16931 Schematron, and the CIUS it declares |
| **[Extract JSON](https://www.invoicexml.com/docs/api/extract/json)** | Parse a UBL XML file into structured invoice data as JSON for downstream systems |
| **[Render to PDF](https://www.invoicexml.com/docs/api/render/ubl/to/pdf)** | Render a UBL XML file into a human-readable PDF preview |
| **[AI Convert](https://www.invoicexml.com/docs/api/transform/to/ubl)** *(experimental)* | Convert a plain, unstructured PDF invoice into a compliant UBL document using a large language model |

> **Looking for a hybrid PDF invoice?** UBL is pure XML. If you need a PDF/A-3 with the data embedded, that is the CII family: see the [ZUGFeRD examples](https://github.com/InvoiceXML/zugferd-api-examples) (Germany) or the [Factur-X examples](https://github.com/InvoiceXML/facturx-api-examples) (France).

---

## Quick start by language

Each language has its own folder containing all five operations as standalone, runnable files. Open the folder for your language to see installation steps, dependencies, and usage.

### Create and validate UBL with C# / .NET

```
/csharp
  Create.cs         # Build a UBL 2.1 invoice (Peppol BIS 3.0 by default)
  Validate.cs       # Validate against the EN 16931 and Peppol rules
  ExtractJson.cs    # Parse a UBL XML into JSON
  Render.cs         # Render UBL XML into a readable PDF
  AiConvert.cs      # (Experimental) Convert plain PDF to UBL using AI
```

[Open the C# / .NET examples →](./csharp)

### Create and validate UBL with Java

```
/java
  Create.java
  Validate.java
  ExtractJson.java
  Render.java
  AiConvert.java
```

[Open the Java examples →](./java)

### Create and validate UBL with PHP

```
/php
  create.php
  validate.php
  extract-json.php
  render.php
  ai-convert.php
```

[Open the PHP examples →](./php)

### Create and validate UBL with JavaScript in the browser

Browser-side examples for generating and inspecting UBL invoices client-side, useful for web-based invoicing apps that want to avoid round-trips to a server.

```
/javascript
  create.html
  validate.html
  extract-json.html
  render.html
  ai-convert.html
```

[Open the JavaScript examples →](./javascript)

### Create and validate UBL with Node.js

```
/nodejs
  create.js
  validate.js
  extract-json.js
  render.js
  ai-convert.js
```

[Open the Node.js examples →](./nodejs)

### Create and validate UBL with Python

```
/python
  create.py
  validate.py
  extract_json.py
  render.py
  ai_convert.py
```

[Open the Python examples →](./python)

### Create and validate UBL with Ruby

Standard library only (`Net::HTTP`), no gems. The Ruby folder additionally covers AI parsing of old-school PDFs with confidence scores and embedded attachment extraction.

```
/ruby
  create.rb
  validate.rb
  extract_json.rb
  render.rb
  parse_json.rb
  extract_attachments.rb
```

[Open the Ruby examples →](./ruby)

---

## Parse UBL data as JSON

Many downstream systems (ERPs, accounting software, expense management tools) work natively with JSON rather than XML. The `ExtractJson` examples in every language upload a UBL XML file and receive the EN 16931 model back as clean JSON: seller, buyer, line items, tax breakdown, totals, and payment details.

The invoice document sits under the **`invoice`** key of the response, so you read `invoice.seller.name` or `invoice.totals.grandTotalAmount` (BT-112). A complete sample response with every field of the invoice model populated is in [json/extract-json-response.json](./json/extract-json-response.json); optional fields that are absent from the source document come back as `null` (empty arrays for lists).

This is the fastest way to get invoice data out of a UBL file and into a modern data pipeline.

---

## Render UBL as a readable PDF

UBL has no visual layer, so nobody can read it without tooling. The `Render` examples upload a UBL XML file and receive a formatted PDF preview, with every EN 16931 business term mapped to a readable layout. Both `Invoice` and `CreditNote` documents are supported, the profile is auto-detected from the `CustomizationID`, and the labels can be localized with the `language` field (`en`, `de`, `fr`).

Two things worth knowing:

- **The PDF has no legal standing.** It is a visual representation for humans; the XML file remains the authoritative invoice for compliance and tax purposes. If you need a legally exchangeable PDF with the data embedded, you want [ZUGFeRD](https://github.com/InvoiceXML/zugferd-api-examples) or [Factur-X](https://github.com/InvoiceXML/facturx-api-examples) instead.
- **Rendering is not validation.** A non-compliant file still renders. Use `POST /v1/validate/ubl` to check compliance.

---

## AI-powered PDF to UBL conversion (Experimental)

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, multilingual, or missing fields that EN 16931 requires. Producing a fully compliant UBL document from such an input is non-trivial, and AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Output from the `AiConvert` examples **must always be reviewed by a human** before the invoice is sent to a customer or submitted to a tax authority. This is a developer convenience for bootstrapping test data and prototyping, **not** an unattended production pipeline.

The `AiConvert` examples in each language demonstrate the same pattern:

1. Read a plain (non-UBL) PDF invoice
2. A large language model reads the document and infers the invoice fields (seller, buyer, line items, tax breakdown, totals, payment terms)
3. The API maps those fields to the EN 16931 semantic model and generates UBL 2.1 XML

Useful for bootstrapping test fixtures from invoices you already have.

---

## Validation and profile detection

`POST /v1/validate/ubl` runs the OASIS UBL 2.1 XSD for the detected document type (`Invoice` or `CreditNote`), then the rules matching the `CustomizationID` the document declares:

| Declared CustomizationID | Rules applied |
|---|---|
| Plain EN 16931 | The EN 16931 Schematron |
| Peppol BIS Billing 3.0, or EHF (which declares the Peppol identifier) | The Peppol BIS Billing 3.0 rules, including the Norwegian national rules for EHF |
| NLCIUS | SI-UBL |
| XRechnung | The KoSIT BR-DE rules |
| PINT | The Peppol International rules |
| Anything unrecognized | The EN 16931 base rules, plus a `PROFILE-DETECTION` warning explaining the fallback. An exotic CIUS is never auto-failed |

The response reports `data.customizationId` (what the document declared) and `data.profile` (the rule set actually applied), and every finding carries a `layer` field telling you whether it came from the schema (`xsd`), the core rules (`en16931`), or the CIUS overlay (`cius`).

Note that an invalid invoice still returns **HTTP 200**: the status reflects that validation ran to completion, and the verdict is in the `valid` flag. Non-2xx statuses are reserved for transport problems, such as 401 for a bad API key or 422 for an unreadable upload.

---

## Related e-invoicing standards

UBL is one syntax among several in the European e-invoicing landscape:

- **Peppol BIS Billing 3.0**: the UBL profile exchanged over the Peppol network, and the default these examples produce.
- **UN/CEFACT CII**: the other EN 16931 syntax, and the one embedded in ZUGFeRD and Factur-X. See the [CII examples](https://github.com/InvoiceXML/cii-api-examples).
- **ZUGFeRD 2.x**: German hybrid format, CII XML embedded in a PDF/A-3. See the [ZUGFeRD examples](https://github.com/InvoiceXML/zugferd-api-examples).
- **Factur-X**: French equivalent of ZUGFeRD 2.x, technically identical. See the [Factur-X examples](https://github.com/InvoiceXML/facturx-api-examples).
- **XRechnung**: the German CIUS of EN 16931, expressible in UBL or CII, mandatory for German B2G. See the [XRechnung examples](https://github.com/InvoiceXML/xrechnung-api-examples).
- **EN 16931**: the European norm that underlies all of the above.

For a side-by-side comparison of when to use which format, see the [e-invoicing standards comparison on invoicexml.com](https://invoicexml.com/standards).

---

## Frequently asked questions

**Is the output Peppol BIS Billing 3.0 compliant?**
Yes, by default. Every generated UBL invoice is validated against the EN 16931 Schematron plus the Peppol BIS Billing 3.0 constraints before delivery. If validation fails, the API returns a 400 with `errorCode` 4001 and the violated rules as findings, each carrying the rule id, layer, and the field paths to correct.

**Which profile do I need for which country?**
Peppol BIS 3.0 (the default) covers the European Peppol network, including Belgium's B2B mandate, the Netherlands, and the Nordics. Use `nlcius` for the Dutch flavour, `ehf` for Norwegian EHF Billing 3.0, `xrechnung` for German XRechnung in UBL syntax, and `pint` for Peppol International jurisdictions such as Australia, New Zealand, Singapore, Japan, and Malaysia.

**How does UBL differ from CII?**
Both are valid EN 16931 syntaxes expressing the same semantic model, but they are entirely different XML: UBL comes from OASIS, CII from UN/CEFACT. UBL dominates on the Peppol network and in the Nordics, the Netherlands, and Belgium; CII dominates in Germany and France via ZUGFeRD and Factur-X.

**Can I submit the output directly to a Peppol access point?**
Yes. The generated UBL 2.1 document is a standards-compliant XML file ready for submission to any Peppol-certified access point. You can also pass it to an ERP or accounting system that supports UBL import.

**Does this support credit notes?**
Yes. Both `Invoice` and `CreditNote` root elements are supported for validation and rendering.

**How does this differ from /create/xrechnung?**
Both can produce XRechnung-compliant output. `POST /v1/create/xrechnung` emits the CII syntax, while this endpoint with `options.profile` set to `xrechnung` emits XRechnung in UBL syntax. Both are valid for German submission channels; pick the syntax your recipient or portal integration expects.

**Do I need to calculate totals myself?**
No. The `invoice.totals` object and the `invoice.vatBreakdowns` array are calculated from your line items when omitted. Provide them explicitly only if you need to override the computed result.

---

## Resources and references

- [Peppol BIS Billing 3.0](https://docs.peppol.eu/poacc/billing/3.0/)
- [OASIS UBL 2.1 specification](https://docs.oasis-open.org/ubl/UBL-2.1.html)
- [Peppol EAS code list (electronic address schemes)](https://docs.peppol.eu/poacc/billing/3.0/codelist/eas/)
- [EN 16931 (European Committee for Standardization)](https://www.cencenelec.eu/)
- [invoicexml.com](https://invoicexml.com): guides, tutorials, validators, and standard comparisons for UBL, Peppol, ZUGFeRD, Factur-X, XRechnung, and CII

---

## Contributing

Pull requests are welcome. Useful contributions include:

- Additional languages (Go, Rust, Kotlin, Swift)
- Alternative library implementations in existing languages
- Sample invoice fixtures covering edge cases (multi-currency, intra-EU reverse charge, zero-rated supplies, credit notes)
- Translations of the README and inline comments
- Bug fixes and validation rule clarifications

Please include a working example, pinned dependency versions, and a short note in the relevant language folder's README.
