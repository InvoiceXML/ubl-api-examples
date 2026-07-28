# UBL for Java: InvoiceXML API Examples

Java code samples for creating, validating, and parsing **UBL** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Requires **Java 15 or later** (Java 17 or 21 LTS recommended): `Create.java` uses text blocks, which were introduced in Java 15. The other examples also compile on Java 8. Runs in Spring Boot, Quarkus, Micronaut, Jakarta EE, Android, AWS Lambda, or plain `public static void main` console apps.

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

- **Java 15 or later** (Java 17 or 21 LTS recommended). `Create.java` uses text blocks, a Java 15+ feature; the other four examples also compile on Java 8.
- **OkHttp 4.x** for clean multipart handling and bearer auth

### Maven

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

OkHttp is the de facto standard HTTP client for modern Java. Java's built-in `java.net.http.HttpClient` does not support multipart out of the box, so doing this in pure JDK would triple the line count of every example.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`Create.java`](./Create.java) | Build a UBL 2.1 XML invoice | [`POST /v1/create/ubl`](https://www.invoicexml.com/docs/api/create/ubl) |
| [`Validate.java`](./Validate.java) | Validate a UBL file against the EN 16931 and Peppol rules | [`POST /v1/validate/ubl`](https://www.invoicexml.com/docs/api/validate/ubl) |
| [`ExtractJson.java`](./ExtractJson.java) | Parse a UBL XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`AiConvert.java`](./AiConvert.java) | (Experimental) Convert a plain PDF to UBL with AI | [`POST /v1/transform/to/ubl`](https://www.invoicexml.com/docs/api/transform/to/ubl) |
| [`Render.java`](./Render.java) | Render UBL XML into a human-readable PDF | [`POST /v1/render/ubl/to/pdf`](https://www.invoicexml.com/docs/api/render/ubl/to/pdf) |

Each file is a standalone `public class` with a `main` method, runnable with `javac` and `java` directly, or as part of any Maven or Gradle project.

> **Note on the snippets below:** they are excerpts from those files. The `try` blocks have no `catch` clause, so the enclosing method must declare `throws Exception` (or at least `throws IOException`), exactly as the full files do with `public static void main(String[] args) throws Exception`. Pasting a snippet into a method without that `throws` clause will not compile ("unreported exception IOException"). When in doubt, copy the complete file.

---

## Create a UBL invoice in Java

```java
String json = """
    {
      "invoice": {
        "invoiceNumber": "UBL-2026-001",
        "issueDate": "2026-05-18",
        "currency": "EUR",
        "buyerReference": "PO-2026-5571",
        "seller": {
          "name": "Acme GmbH",
          "vatIdentifier": "DE123456789",
          "legalRegistration": { "identifier": "HRB 12345" },
          "postalAddress": { "line1": "Hauptstraße 12", "city": "Berlin", "postCode": "10115", "country": "DE" },
          "electronicAddress": { "identifier": "DE123456789", "schemeId": "9930" }
        },
        "buyer": {
          "name": "Globex SAS",
          "postalAddress": { "line1": "15 rue de Rivoli", "city": "Paris", "postCode": "75001", "country": "FR" },
          "electronicAddress": { "identifier": "FR40303265045", "schemeId": "9957" }
        },
        "paymentDetails": { "paymentAccountIdentifier": "DE89370400440532013000" },
        "lines": [{
          "quantity": 10,
          "priceDetails": { "netPrice": 150.00 },
          "vatInformation": { "rate": 19.00 },
          "item": { "name": "Senior consulting" }
        }]
      },
      "options": { "profile": "peppol-bis-3" }
    }
    """;

RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

Request request = new Request.Builder()
    .url("https://api.invoicexml.com/v1/create/ubl")
    .header("Authorization", "Bearer " + apiKey)
    .post(body)
    .build();

try (Response response = new OkHttpClient().newCall(request).execute()) {
    String xml = response.body().string();
    Files.write(Paths.get("invoice-ubl.xml"), xml.getBytes("UTF-8"));
}
```

`options.profile` decides both the `CustomizationID` stamped into the document and the rules it is validated against. Peppol BIS Billing 3.0 is the default and needs an `electronicAddress` on both parties plus a `buyerReference`; for invoices that never touch the Peppol network, set the profile to `en16931` and those become optional.

The response is the UBL 2.1 XML document, validated against the profile's rules before delivery.

[Full example: `Create.java`](./Create.java) | [API reference](https://www.invoicexml.com/docs/api/create/ubl)

---

## Validate a UBL file in Java

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", "invoice.xml",
        RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
    .build();

Request request = new Request.Builder()
    .url("https://api.invoicexml.com/v1/validate/ubl")
    .header("Authorization", "Bearer " + apiKey)
    .post(body)
    .build();

try (Response response = new OkHttpClient().newCall(request).execute()) {
    System.out.println(response.body().string());
}
```

Returns a JSON validation report listing any rule failures (EN 16931 BR-* and BR-CO-*, plus the CIUS rules, e.g. PEPPOL-EN16931-* for Peppol BIS).

[Full example: `Validate.java`](./Validate.java) | [API reference](https://www.invoicexml.com/docs/api/validate/ubl)

---

## Extract UBL data as JSON in Java

Useful for feeding UBL invoices into Spring Boot services, message queues, or any pipeline that prefers JSON over XML.

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", "invoice.xml",
        RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
    .build();

try (Response response = new OkHttpClient().newCall(
        new Request.Builder()
            .url("https://api.invoicexml.com/v1/extract/json")
            .header("Authorization", "Bearer " + apiKey)
            .post(body).build()
    ).execute()) {
    String json = response.body().string();
    // Deserialize with Jackson, Gson, or your preferred JSON library.
    // The invoice document sits under the "invoice" key: seller, buyer, lines, totals.
}
```

[Full example: `ExtractJson.java`](./ExtractJson.java) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## (Experimental) Convert a plain PDF to UBL with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output before sending it to a customer or tax authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-ubl-conversion-experimental).

The endpoint takes the PDF alone; no extra parameters are needed.

[Full example: `AiConvert.java`](./AiConvert.java) | [API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)

---

## Render UBL as a readable PDF in Java

UBL has no visual layer: the XML *is* the invoice, which is fine for machines and useless for the person in accounts payable who wants to read it. This endpoint renders the XML into a formatted PDF preview, auto-detecting whether the file is CII or UBL syntax. The PDF is for reading only; the XML file remains the authoritative invoice for compliance and tax purposes.

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", "invoice.xml",
        RequestBody.create(new File("invoice.xml"), MediaType.parse("application/xml")))
    .addFormDataPart("language", "de")   // en, de, or fr
    .build();

Request request = new Request.Builder()
    .url("https://api.invoicexml.com/v1/render/ubl/to/pdf")
    .header("Authorization", "Bearer " + apiKey)
    .post(body)
    .build();

try (Response response = new OkHttpClient().newCall(request).execute()) {
    Files.write(Paths.get("invoice-preview.pdf"), response.body().bytes());
}
```

[Full example: `Render.java`](./Render.java) | [API reference](https://www.invoicexml.com/docs/api/render/ubl/to/pdf)

---

## Framework integration

### Spring Boot

Return a UBL invoice from a REST controller:

```java
@RestController
public class UBLController {

    @GetMapping(value = "/invoices/{id}/ubl", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable String id) throws Exception {
        byte[] pdf = ublService.create(id);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
            .body(pdf);
    }
}
```

Inject the API key from `application.properties` via `@Value("${invoicexml.api-key}")`.

### Quarkus

```java
@Path("/invoices")
public class UBLResource {

    @GET
    @Path("/{id}/ubl")
    @Produces("application/pdf")
    public Response download(@PathParam("id") String id) throws Exception {
        byte[] pdf = ublService.create(id);
        return Response.ok(pdf)
            .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
            .build();
    }
}
```

### Android

OkHttp is also Square's library for Android, so the examples run on Android 5.0+ unchanged. Move the call off the main thread using `enqueue()`, `CoroutineScope`, or `RxJava`. For Android 9+ ensure your network security config allows TLS to `api.invoicexml.com` (it does by default).

### AWS Lambda

Bundle OkHttp into your Lambda deployment package or layer. Cold-start latency is minimal because OkHttp is small (~700 KB).

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: pasting the whole `Bearer xxx` value as `apiKey`, which sends `Bearer Bearer xxx`. Set `apiKey` to the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`RequestBody.create()` argument order**: OkHttp 4.x reversed the parameter order from 3.x. Use `RequestBody.create(File, MediaType)` for 4.x. The 3.x signature `(MediaType, File)` is deprecated but still callable.
- **`Files.writeString` not found**: that method requires Java 11+. The examples use `Files.write(Path, byte[])` which works on Java 8+.
- **TLS handshake failures on old JDKs**: enable TLS 1.2 explicitly on Java 8 with `-Dhttps.protocols=TLSv1.2,TLSv1.3`, or upgrade to Java 11+.
- **PEPPOL-EN16931-* failures on Validate**: a Peppol-specific requirement is missing. The most common are a missing electronic address on the seller or buyer, and a missing `buyerReference` (Peppol requires a buyer reference or a purchase order reference).

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create UBL API reference](https://www.invoicexml.com/docs/api/create/ubl)
- [Validate UBL API reference](https://www.invoicexml.com/docs/api/validate/ubl)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to UBL API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)
- [OkHttp documentation](https://square.github.io/okhttp/)
- [Main repository README](../README.md)
