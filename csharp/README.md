# UBL for C# and .NET: InvoiceXML REST API Examples

C# and .NET code samples for creating, validating, and parsing **UBL** electronic invoices using the **[invoicexml.com API](https://www.invoicexml.com)**. Requires **.NET 6 or later** (.NET 8 recommended), and ready to drop into console apps, ASP.NET Core Web APIs, Blazor, MAUI, Azure Functions, or AWS Lambda.

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** pass the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. Flurl's `WithOAuthBearerToken(apiKey)` adds the prefix itself.

## Requirements

- **.NET 6.0 or later** (recommended: .NET 8) with `ImplicitUsings` enabled (the default in new SDK-style projects)
- Runs on Windows, Linux, macOS, Docker, Azure, and AWS Lambda
- On **.NET Framework 4.7.2+** the API calls work too (Flurl.Http targets .NET Standard 2.0), but the files as written need small changes: add the `using System; using System.IO; using System.Threading.Tasks;` directives and replace `File.WriteAllBytesAsync` / `File.WriteAllTextAsync` (not available on .NET Framework) with the synchronous `File.WriteAllBytes` / `File.WriteAllText`
- One NuGet package: [Flurl.Http](https://www.nuget.org/packages/Flurl.Http) for clean multipart uploads and bearer auth

```bash
dotnet add package Flurl.Http
```

If you prefer plain `HttpClient` over Flurl, every example translates directly. Flurl just makes the multipart and authentication wiring a one-liner.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`Create.cs`](./Create.cs) | Build a UBL 2.1 XML invoice | [`POST /v1/create/ubl`](https://www.invoicexml.com/docs/api/create/ubl) |
| [`Validate.cs`](./Validate.cs) | Validate a UBL file against the EN 16931 and Peppol rules | [`POST /v1/validate/ubl`](https://www.invoicexml.com/docs/api/validate/ubl) |
| [`ExtractJson.cs`](./ExtractJson.cs) | Parse a UBL XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`AiConvert.cs`](./AiConvert.cs) | (Experimental) Convert a plain PDF to UBL with AI | [`POST /v1/transform/to/ubl`](https://www.invoicexml.com/docs/api/transform/to/ubl) |
| [`Render.cs`](./Render.cs) | Render UBL XML into a human-readable PDF | [`POST /v1/render/ubl/to/pdf`](https://www.invoicexml.com/docs/api/render/ubl/to/pdf) |

> **Note on the snippets below:** they are excerpts from those files and assume an `apiKey` variable is already defined and that the code runs inside an `async` method. The full files show the complete, compilable versions.

---

## Create a UBL invoice in C#

```csharp
using Flurl.Http;

var payload = new
{
    invoice = new
    {
        invoiceNumber = "UBL-2026-001",
        issueDate     = "2026-05-18",
        currency      = "EUR",
        buyerReference = "PO-2026-5571",
        seller = new
        {
            name              = "Acme GmbH",
            vatIdentifier     = "DE123456789",
            legalRegistration = new { identifier = "HRB 12345" },
            postalAddress     = new { line1 = "Hauptstraße 12", city = "Berlin", postCode = "10115", country = "DE" },
            electronicAddress = new { identifier = "DE123456789", schemeId = "9930" }
        },
        buyer = new
        {
            name              = "Globex SAS",
            postalAddress     = new { line1 = "15 rue de Rivoli", city = "Paris", postCode = "75001", country = "FR" },
            electronicAddress = new { identifier = "FR40303265045", schemeId = "9957" }
        },
        paymentDetails = new { paymentAccountIdentifier = "DE89370400440532013000" },
        lines = new[]
        {
            new {
                quantity       = 10,
                priceDetails   = new { netPrice = 150.00m },
                vatInformation = new { rate = 19.00m },
                item           = new { name = "Senior consulting" }
            }
        }
    },
    options = new { profile = "peppol-bis-3" }
};

var xml = await "https://api.invoicexml.com/v1/create/ubl"
    .WithOAuthBearerToken(apiKey)
    .PostJsonAsync(payload)
    .ReceiveString();

await File.WriteAllTextAsync("invoice-ubl.xml", xml);
```

`options.profile` decides both the `CustomizationID` stamped into the document and the rules it is validated against. Peppol BIS Billing 3.0 is the default and needs an `electronicAddress` on both parties plus a `buyerReference`; for invoices that never touch the Peppol network, set the profile to `en16931` and those become optional.

The response is the UBL 2.1 XML document, validated against the profile's rules before delivery.

[Full example: `Create.cs`](./Create.cs) | [API reference](https://www.invoicexml.com/docs/api/create/ubl)

---

## Validate a UBL file in C#

```csharp
using Flurl.Http;

var report = await "https://api.invoicexml.com/v1/validate/ubl"
    .WithOAuthBearerToken(apiKey)
    .PostMultipartAsync(mp => mp
        .AddFile("file", "invoice.xml", "application/xml"))
    .ReceiveString();

Console.WriteLine(report);
```

Returns a JSON validation report listing any rule failures (EN 16931 BR-* and BR-CO-*, plus the CIUS rules, e.g. PEPPOL-EN16931-* for Peppol BIS).

[Full example: `Validate.cs`](./Validate.cs) | [API reference](https://www.invoicexml.com/docs/api/validate/ubl)

---

## Extract UBL data as JSON in C#

A common need: get UBL invoice data into a JSON-friendly format for REST APIs, ERPs, or downstream pipelines.

```csharp
using Flurl.Http;

var json = await "https://api.invoicexml.com/v1/extract/json"
    .WithOAuthBearerToken(apiKey)
    .PostMultipartAsync(mp => mp
        .AddFile("file", "invoice.xml", "application/xml"))
    .ReceiveString();

await File.WriteAllTextAsync("invoice.json", json);
```

[Full example: `ExtractJson.cs`](./ExtractJson.cs) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## (Experimental) Convert a plain PDF to UBL with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, multilingual, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output before sending it to a customer or tax authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-ubl-conversion-experimental).

The endpoint takes the PDF alone; no extra parameters are needed.

```csharp
using Flurl.Http;

var xml = await "https://api.invoicexml.com/v1/transform/to/ubl"
    .WithOAuthBearerToken(apiKey)
    .PostMultipartAsync(mp => mp
        .AddFile("file", "plain-invoice.pdf", "application/pdf")
   )
    .ReceiveString();

await File.WriteAllTextAsync("converted-ubl.xml", xml);
```

[Full example: `AiConvert.cs`](./AiConvert.cs) | [API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)

---

## Render UBL as a readable PDF in C#

UBL has no visual layer: the XML *is* the invoice, which is fine for machines and useless for the person in accounts payable who wants to read it. This endpoint renders the XML into a formatted PDF preview, auto-detecting whether the file is CII or UBL syntax. The PDF is for reading only; the XML file remains the authoritative invoice for compliance and tax purposes.

```csharp
using Flurl.Http;

var pdfBytes = await "https://api.invoicexml.com/v1/render/ubl/to/pdf"
    .WithOAuthBearerToken(apiKey)
    .PostMultipartAsync(mp => mp
        .AddFile("file", "invoice.xml", "application/xml")
        .AddString("language", "de")   // en, de, or fr
    )
    .ReceiveBytes();

await File.WriteAllBytesAsync("invoice-preview.pdf", pdfBytes);
```

[Full example: `Render.cs`](./Render.cs) | [API reference](https://www.invoicexml.com/docs/api/render/ubl/to/pdf)

---

## Framework integration

### ASP.NET Core / Web API

Return a UBL invoice from a controller action by proxying the invoicexml.com API:

```csharp
[HttpGet("invoices/{id}/ubl")]
public async Task<IActionResult> GetUBL(string id)
{
    var xml = await CreateUbl.RunAsync(/* invoice data from your DB */);
    return File(System.Text.Encoding.UTF8.GetBytes(xml), "application/xml", $"invoice-{id}.xml");
}
```

### Console / Worker Service

Each example exposes a static `RunAsync` method. From `Program.cs`:

```csharp
await CreateUbl.RunAsync();
await ValidateUbl.RunAsync("invoice.xml");
await ExtractJson.RunAsync("invoice.xml");
```

---

## Common issues

- **401 Unauthorized**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: passing the whole `Bearer xxx` value as the key, which sends `Bearer Bearer xxx`. Pass the raw key only.
- **400 Bad Request on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals or leave empty for auto-calculation when posting.

---

## Resources

- [Get an API key](https://www.invoicexml.com/account/authentication)
- [Create UBL API reference](https://www.invoicexml.com/docs/api/create/ubl)
- [Validate UBL API reference](https://www.invoicexml.com/docs/api/validate/ubl)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to UBL API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)
- [Flurl.Http documentation](https://flurl.dev/docs/fluent-http/)
- [Main repository README](../README.md)
