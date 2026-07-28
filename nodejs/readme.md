# UBL for Node.js: InvoiceXML API Examples

Node.js code samples for creating, validating, and parsing **UBL** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **Node.js 18 and later** (native `fetch` and `FormData`). Runs in Express, NestJS, Fastify, Koa, Hapi, Next.js API routes, AWS Lambda, Cloudflare Workers, Vercel Functions, or plain scripts. **Zero npm dependencies.**

For background on UBL itself (what it is, the profiles, where it is used), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Node.js 18.0 or later** (current LTS recommended: Node 20 or 22)
- No npm packages, no `package.json` needed

The examples use Node's built-in global `fetch`, `FormData`, `Blob`, and `Buffer`. These have been stable in Node 18+. If you must support Node 16 or older, polyfill `fetch` with `node-fetch` and `FormData` with `form-data`.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.js`](./create.js) | Build a UBL 2.1 XML invoice | [`POST /v1/create/ubl`](https://www.invoicexml.com/docs/api/create/ubl) |
| [`validate.js`](./validate.js) | Validate a UBL file against the EN 16931 and Peppol rules | [`POST /v1/validate/ubl`](https://www.invoicexml.com/docs/api/validate/ubl) |
| [`extract-json.js`](./extract-json.js) | Parse a UBL XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`ai-convert.js`](./ai-convert.js) | (Experimental) Convert a plain PDF to UBL with AI | [`POST /v1/transform/to/ubl`](https://www.invoicexml.com/docs/api/transform/to/ubl) |
| [`render.js`](./render.js) | Render UBL XML into a human-readable PDF | [`POST /v1/render/ubl/to/pdf`](https://www.invoicexml.com/docs/api/render/ubl/to/pdf) |

Each file is standalone and runnable with `node create.js`. Open the file, replace `YOUR_API_KEY` with your real key, and execute.

> **Note on the snippets below:** they are excerpts from those files and assume `apiKey` (and `fs`) are already defined. They also use `await` at the top level, which is not valid in a plain CommonJS script; the full files wrap the calls in an `async` IIFE. When in doubt, copy the complete file.

---

## Create a UBL invoice in Node.js

```js
const payload = {
    invoice: {
        invoiceNumber: 'UBL-2026-001',
        issueDate:     '2026-05-18',
        currency:      'EUR',
        buyerReference: 'PO-2026-5571',
        seller: {
            name:              'Acme GmbH',
            vatIdentifier:     'DE123456789',
            legalRegistration: { identifier: 'HRB 12345' },
            postalAddress: { line1: 'Hauptstraße 12', city: 'Berlin', postCode: '10115', country: 'DE' },
            electronicAddress: { identifier: 'DE123456789', schemeId: '9930' },
        },
        buyer: {
            name: 'Globex SAS',
            postalAddress: { line1: '15 rue de Rivoli', city: 'Paris', postCode: '75001', country: 'FR' },
            electronicAddress: { identifier: 'FR40303265045', schemeId: '9957' },
        },
        paymentDetails: { paymentAccountIdentifier: 'DE89370400440532013000' },
        lines: [{
            quantity:       10,
            priceDetails:   { netPrice: 150.00 },
            vatInformation: { rate: 19.00 },
            item:           { name: 'Senior consulting' },
        }],
    },
    options: { profile: 'peppol-bis-3' },
};

const response = await fetch('https://api.invoicexml.com/v1/create/ubl', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + apiKey,
    },
    body: JSON.stringify(payload),
});

fs.writeFileSync('invoice-ubl.xml', await response.text());
```

`options.profile` decides both the `CustomizationID` stamped into the document and the rules it is validated against. Peppol BIS Billing 3.0 is the default and needs an `electronicAddress` on both parties plus a `buyerReference`; for invoices that never touch the Peppol network, set the profile to `en16931` and those become optional.

The response is the UBL 2.1 XML document, validated against the profile's rules before delivery.

[Full example: `create.js`](./create.js) | [API reference](https://www.invoicexml.com/docs/api/create/ubl)

---

## Validate a UBL file in Node.js

```js
const fs = require('fs');

const form = new FormData();
form.append('file', new Blob([fs.readFileSync('invoice.xml')], { type: 'application/xml' }), 'invoice.xml');

const response = await fetch('https://api.invoicexml.com/v1/validate/ubl', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + apiKey },
    body: form,
});

console.log(await response.text());
```

Returns a JSON validation report listing any rule failures (EN 16931 BR-* and BR-CO-*, plus the CIUS rules, e.g. PEPPOL-EN16931-* for Peppol BIS).

[Full example: `validate.js`](./validate.js) | [API reference](https://www.invoicexml.com/docs/api/validate/ubl)

---

## Extract UBL data as JSON in Node.js

Useful for feeding UBL invoices into Express controllers, message queues, or any pipeline that prefers JSON over XML.

```js
const fs = require('fs');

const form = new FormData();
form.append('file', new Blob([fs.readFileSync('invoice.xml')], { type: 'application/xml' }), 'invoice.xml');

const response = await fetch('https://api.invoicexml.com/v1/extract/json', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + apiKey },
    body: form,
});

const { invoice } = await response.json();
// The invoice document sits under the "invoice" key of the response.
console.log(invoice.seller.name, invoice.totals.grandTotalAmount);
```

[Full example: `extract-json.js`](./extract-json.js) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## (Experimental) Convert a plain PDF to UBL with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output before sending it to a customer or tax authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-ubl-conversion-experimental).

The endpoint takes the PDF alone; no extra parameters are needed.

[Full example: `ai-convert.js`](./ai-convert.js) | [API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)

---

## Render UBL as a readable PDF in Node.js

UBL has no visual layer: the XML *is* the invoice, which is fine for machines and useless for the person in accounts payable who wants to read it. This endpoint renders the XML into a formatted PDF preview, auto-detecting whether the file is CII or UBL syntax. The PDF is for reading only; the XML file remains the authoritative invoice for compliance and tax purposes.

```js
const fs = require('fs');

const form = new FormData();
form.append('file', new Blob([fs.readFileSync('invoice.xml')], { type: 'application/xml' }), 'invoice.xml');
form.append('language', 'de');   // en, de, or fr

const response = await fetch('https://api.invoicexml.com/v1/render/ubl/to/pdf', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + apiKey },
    body: form,
});

fs.writeFileSync('invoice-preview.pdf', Buffer.from(await response.arrayBuffer()));
```

[Full example: `render.js`](./render.js) | [API reference](https://www.invoicexml.com/docs/api/render/ubl/to/pdf)

---

## Framework integration

### Express

Return a UBL invoice from a route handler:

```js
const express = require('express');
const app = express();

app.get('/invoices/:id/ubl', async (req, res) => {
    const pdf = await createUBL(req.params.id);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="invoice-${req.params.id}.pdf"`);
    res.send(pdf);
});
```

Store the API key in `process.env.INVOICEXML_API_KEY` and read it from `dotenv` or your platform's secret manager.

### NestJS

```ts
@Controller('invoices')
export class InvoiceController {
    @Get(':id/ubl')
    @Header('Content-Type', 'application/pdf')
    async download(@Param('id') id: string, @Res() res: Response) {
        const pdf = await this.ublService.create(id);
        res.setHeader('Content-Disposition', `attachment; filename="invoice-${id}.pdf"`);
        res.send(pdf);
    }
}
```

### Next.js App Router

```ts
// app/api/invoices/[id]/ubl/route.ts
export async function GET(req: Request, { params }: { params: { id: string } }) {
    const pdf = await createUBL(params.id);
    return new Response(pdf, {
        headers: {
            'Content-Type': 'application/pdf',
            'Content-Disposition': `attachment; filename="invoice-${params.id}.pdf"`,
        },
    });
}
```

### Cloudflare Workers and Vercel Edge

The examples run on Workers and Edge runtimes unchanged because `fetch` and `FormData` are part of the runtime. Replace `fs.readFileSync` with an `await request.arrayBuffer()` from the inbound request and the same pattern works in serverless environments.

### AWS Lambda (Node.js 18+)

Lambda's Node.js 18+ runtime provides native `fetch` and `FormData`, so the examples run as-is.

---

## Common issues

- **`fetch is not defined`**: you are running Node 16 or older. Upgrade to Node 18+ (current LTS), or polyfill with `npm install node-fetch form-data` and import accordingly.
- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting `apiKey` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`ESM` vs `CommonJS`**: the examples use CommonJS (`require`). For ESM (`.mjs` extension or `"type": "module"` in `package.json`), swap `require('fs')` for `import fs from 'node:fs'` and you can use top-level await without the IIFE wrapper.
- **Relative file paths**: `fs.readFileSync('invoice.xml')` resolves from the current working directory, not the script file. Use `__dirname` (CommonJS) or `import.meta.dirname` (ESM, Node 20.11+) for absolute paths.
- **PEPPOL-EN16931-* failures on Validate**: a Peppol-specific requirement is missing. The most common are a missing electronic address on the seller or buyer, and a missing `buyerReference` (Peppol requires a buyer reference or a purchase order reference).

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create UBL API reference](https://www.invoicexml.com/docs/api/create/ubl)
- [Validate UBL API reference](https://www.invoicexml.com/docs/api/validate/ubl)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to UBL API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)
- [Node.js fetch API documentation](https://nodejs.org/api/globals.html#fetch)
- [Main repository README](../README.md)
