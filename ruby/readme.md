# UBL for Ruby: InvoiceXML API Examples

Ruby code samples for creating, validating, parsing, and rendering **UBL 2.1** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**, across the Peppol BIS, NLCIUS, EHF, XRechnung UBL, and PINT profiles. Compatible with **Ruby 3.0 and later**, using only `Net::HTTP` from the standard library. Runs in Rails, Sinatra, Hanami, Sidekiq jobs, Rake tasks, or plain scripts. **Zero gems.**

For background on UBL itself (what it is, how it relates to Peppol and EN 16931), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

The examples read the key from the `INVOICEXML_API_KEY` environment variable:

```bash
export INVOICEXML_API_KEY=YOUR_API_KEY
```

Or on Windows (PowerShell):

```powershell
$env:INVOICEXML_API_KEY = "YOUR_API_KEY"
```

**Important:** set the variable to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Ruby 3.0 or later** (any currently supported Ruby)
- No gems, no Gemfile needed

The examples use `Net::HTTP`, `JSON`, and `URI` from the standard library. Ruby has a small advantage here over most languages: `Net::HTTP` supports multipart uploads natively via `request.set_form`, so no multipart helper gem is needed. In a Rails app you can swap in Faraday later without changing the shape of the calls.

Nokogiri is deliberately absent. Ruby can build UBL XML by hand, but the EN 16931 model carries about 200 interlocking business rules on top of the XSD, each Peppol-adjacent profile adds its own CIUS overlay, and the official rule sets compile to XSLT 2.0, which Nokogiri's libxslt (XSLT 1.0) cannot run. Generation and validation therefore happen server-side.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.rb`](./create.rb) | Build a UBL 2.1 XML invoice in any supported profile | [`POST /v1/create/ubl`](https://www.invoicexml.com/docs/api/create/ubl) |
| [`send_email.rb`](./send_email.rb) | Create a UBL invoice and email it from your own domain | [`POST /v1/create/ubl/email`](https://www.invoicexml.com/docs/api/create/ubl/email) |
| [`validate.rb`](./validate.rb) | Validate a UBL file against the XSD, EN 16931, and its CIUS | [`POST /v1/validate/ubl`](https://www.invoicexml.com/docs/api/validate/ubl) |
| [`extract_json.rb`](./extract_json.rb) | Parse a UBL XML into JSON (deterministic, no AI) | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`render.rb`](./render.rb) | Render UBL XML into a human-readable PDF | [`POST /v1/render/ubl/to/pdf`](https://www.invoicexml.com/docs/api/render/ubl/to/pdf) |
| [`parse_json.rb`](./parse_json.rb) | Parse an old-school invoice PDF (scan, photo) with AI, with confidence scores | [`POST /v1/parse/json`](https://www.invoicexml.com/docs/api/parse/json) |
| [`extract_attachments.rb`](./extract_attachments.rb) | Extract embedded supporting documents (BG-24) as a ZIP | [`POST /v1/extract/attachments`](https://www.invoicexml.com/docs/api/extract/attachments) |

Each file is standalone and runnable with `ruby create.rb`. Set `INVOICEXML_API_KEY` in your environment and execute. The files that take an input document default to `invoice-ubl.xml` (the output of `create.rb`), or accept a path as the first argument.

> **Note on the snippets below:** they are excerpts from those files and assume `BASE_URL`, `API_KEY`, and the `api_http` helper are already defined. When in doubt, copy the complete file.

All files share this small setup block:

```ruby
require "net/http"
require "json"
require "uri"

BASE_URL = "https://api.invoicexml.com"
API_KEY  = ENV.fetch("INVOICEXML_API_KEY")

def api_http(uri)
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true
  http.open_timeout = 10
  http.read_timeout = 120
  http
end
```

---

## Create a UBL invoice in Ruby

The payload is a plain Ruby hash serialized with `JSON.generate`. Send net prices, quantities, and rates; totals and the per-rate VAT breakdown are calculated from the line items. The `options.profile` value selects the CIUS: `en16931`, `peppol-bis-3` (default), `nlcius`, `ehf`, `xrechnung`, or `pint`. Everything else in the hash stays the same across profiles, so supporting a new market is a one-word change.

```ruby
payload = {
  invoice: {
    invoiceNumber: "UBL-2026-001",
    issueDate: "2026-05-18",
    currency: "EUR",
    # Peppol requires a buyer reference (or a purchase order reference).
    buyerReference: "PO-2026-5571",
    seller: {
      name: "Acme GmbH",
      vatIdentifier: "DE123456789",
      legalRegistration: { identifier: "HRB 12345" },
      postalAddress: { line1: "Hauptstraße 12", city: "Berlin", postCode: "10115", country: "DE" },
      # Peppol routing address. schemeId is a Peppol EAS code (9930 = German VAT).
      electronicAddress: { identifier: "DE123456789", schemeId: "9930" }
    },
    buyer: {
      name: "Globex SAS",
      postalAddress: { line1: "15 rue de Rivoli", city: "Paris", postCode: "75001", country: "FR" },
      electronicAddress: { identifier: "FR40303265045", schemeId: "9957" }
    },
    paymentDetails: { paymentAccountIdentifier: "DE89370400440532013000" },
    lines: [
      { quantity: 10, priceDetails: { netPrice: 150.00 }, vatInformation: { rate: 19.00 }, item: { name: "Senior consulting" } }
    ]
  },
  options: { profile: "peppol-bis-3" }
}

uri = URI("#{BASE_URL}/v1/create/ubl")
request = Net::HTTP::Post.new(uri)
request["Authorization"] = "Bearer #{API_KEY}"
request["Content-Type"]  = "application/json"
request.body = JSON.generate(payload)

response = api_http(uri).request(request)
raise response.body unless response.is_a?(Net::HTTPSuccess)

File.binwrite("invoice-ubl.xml", response.body)
```

The response is a UBL 2.1 XML document conforming to the requested profile, validated before it is returned. If the data cannot produce a compliant invoice, the API answers HTTP 400 with the violated rules as structured findings.

[Full example: `create.rb`](./create.rb) | [API reference](https://www.invoicexml.com/docs/api/create/ubl)

---

## Validate a UBL file in Ruby

`Net::HTTP` does multipart natively through `set_form`, so the upload is three lines. The endpoint detects the profile the document declares and applies the matching CIUS on top of the XSD and the EN 16931 Schematron:

```ruby
def validate_ubl(path)
  uri = URI("#{BASE_URL}/v1/validate/ubl")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    JSON.parse(api_http(uri).request(request).body)
  end
end

report = validate_ubl("invoice-ubl.xml")

unless report["valid"]
  report["errors"].each do |finding|
    layer = finding["layer"] # "xsd", "en16931", or "cius"
    puts "[#{finding["rule"]}] (#{layer}) #{finding["message"]}"
  end
end
```

A finished validation always answers HTTP 200: the pass or fail verdict lives in `valid`, and non-2xx statuses are reserved for transport problems such as a bad API key or an unreadable upload. The `layer` tag on each finding is worth surfacing to users: a `cius` failure means the document is a perfectly fine EN 16931 invoice that specifically misses a profile requirement, which is exactly the class of error a Peppol access point rejects on submission.

[Full example: `validate.rb`](./validate.rb) | [API reference](https://www.invoicexml.com/docs/api/validate/ubl)

---

## Parse UBL data as JSON in Ruby

Upload the XML and get the EN 16931 model back as clean JSON, ready for your models or a message queue. Deterministic parsing, no AI involved.

```ruby
def extract_invoice(path)
  uri = URI("#{BASE_URL}/v1/extract/json")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    JSON.parse(api_http(uri).request(request).body)
  end
end

# The invoice document sits under the "invoice" key of the response.
invoice = extract_invoice("invoice-ubl.xml")["invoice"]
puts "#{invoice["invoiceNumber"]} from #{invoice.dig("seller", "name")}"
puts invoice.dig("totals", "grandTotalAmount")  # BT-112
```

[Full example: `extract_json.rb`](./extract_json.rb) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## Render UBL as a readable PDF in Ruby

A UBL file has no visual layer. Render it when a human needs to read the invoice, in review workflows, customer portals, or an email attachment alongside the XML. The rendered PDF is a preview only: the XML file remains the authoritative invoice for compliance and tax purposes, and it should never be submitted to a Peppol access point in place of the XML.

```ruby
def render_ubl(xml_path, pdf_path)
  uri = URI("#{BASE_URL}/v1/render/ubl/to/pdf")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(xml_path, "rb") do |file|
    # Label language for the rendered PDF: en, de, or fr. Defaults to en.
    request.set_form([["file", file], ["language", "de"]], "multipart/form-data")
    File.binwrite(pdf_path, api_http(uri).request(request).body)
  end
end

render_ubl("invoice-ubl.xml", "invoice-preview.pdf")
```

[Full example: `render.rb`](./render.rb) | [API reference](https://www.invoicexml.com/docs/api/render/ubl/to/pdf)

---

## Parse old-school invoice PDFs with AI in Ruby

Not every supplier is on the Peppol network. For typed, scanned, or photographed PDFs with no XML at all, `POST /v1/parse/json` reads the document with AI and returns the same `InvoiceDocument` shape as the extract endpoint, plus a `confidence` object: an overall score and four area scores (seller identification, buyer identification, tax calculation, line items), each from 0.0 to 1.0. Automate the confident reads, route the shaky ones to a human:

```ruby
result     = parse_invoice_pdf("supplier-scan.pdf")
invoice    = result["invoice"]
confidence = result.dig("confidence", "overall")

if confidence < 0.7
  queue_for_human_review(invoice, result["confidence"])
else
  import_into_erp(invoice)
end
```

Because the `invoice` hash matches the `/v1/create/ubl` request body, a parsed supplier invoice can be piped straight back into UBL creation.

[Full example: `parse_json.rb`](./parse_json.rb) | [API reference](https://www.invoicexml.com/docs/api/parse/json)

---

## Extract embedded attachments in Ruby

E-invoices can carry their supporting documents inside the invoice itself: delivery notes, timesheets, or the original order, embedded base64 in the EN 16931 attachment group (BG-24). `POST /v1/extract/attachments` collects every embedded payload and returns the original files as one ZIP. An invoice without embedded attachments answers 404 rather than an empty archive, so the example treats that status as a normal outcome.

[Full example: `extract_attachments.rb`](./extract_attachments.rb) | [API reference](https://www.invoicexml.com/docs/api/extract/attachments)

---

## Rails integration

In a Rails app, promote the calls into a service object. Faraday with `faraday-multipart` is the idiomatic client, and Rails credentials keep the key out of environment-variable sprawl. Because the profile is just a request option, one service covers every market you sell into:

```ruby
# Gemfile
gem "faraday"
gem "faraday-multipart"

# app/services/ubl_service.rb
class UblService
  BASE_URL = "https://api.invoicexml.com"

  def create(invoice_attributes, profile: "peppol-bis-3")
    response = connection.post("/v1/create/ubl") do |req|
      req.headers["Content-Type"] = "application/json"
      req.body = JSON.generate(invoice: invoice_attributes, options: { profile: profile })
    end
    raise UblError, response.body unless response.success?
    response.body
  end

  def validate(xml)
    part = Faraday::Multipart::FilePart.new(
      StringIO.new(xml), "application/xml", "invoice.xml"
    )
    JSON.parse(connection.post("/v1/validate/ubl", { file: part }).body)
  end

  private

  def connection
    @@connection ||= Faraday.new(url: BASE_URL) do |f|
      f.request :multipart
      f.headers["Authorization"] =
        "Bearer #{Rails.application.credentials.dig(:invoicexml, :api_key)}"
    end
  end
end

class UblError < StandardError; end
```

Run generation in an ActiveJob rather than a request cycle, store the returned XML with ActiveStorage, and retry only transport failures and 5xx responses: an HTTP 400 carries validation findings about your data and will not change on resend. A request spec that creates and then validates an invoice per profile you support pins the whole mapping in CI.

---

## Common issues

- **`KeyError: key not found: "INVOICEXML_API_KEY"`**: the environment variable is not set in the shell running the script. Export it first (see [Get your API key](#get-your-api-key)), and remember that `ENV.fetch` reads the environment of the current process, so set it in the same terminal session or in your process manager.
- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting the variable to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `issueDate` not in ISO format (`YYYY-MM-DD`), `currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **Peppol rejects an invoice your code considers fine**: Peppol BIS requires a `buyerReference` (or a purchase order reference) and electronic addresses with valid EAS scheme ids for both parties. Validate with the `peppol-bis-3` profile before submitting, and read the `cius` layer findings.
- **Validate "fails" with HTTP 200**: not a failure. Valid and invalid documents both answer 200; branch on the `valid` flag in the JSON body. Non-2xx statuses mean transport problems, not rule violations.
- **Encoding trouble with umlauts**: write the returned XML with `File.binwrite` as the examples do. The API returns UTF-8 bytes, and `binwrite` stores them unchanged; `File.write` on a string tagged with a different external encoding can transcode them.
- **Multipart upload sends an empty file**: open the file in binary mode (`File.open(path, "rb")`) and call `set_form` inside the block, as the examples do, so the handle is still open when the request body is read.
- **`Net::ReadTimeout`**: AI parsing in particular can take 10 to 30 seconds for larger PDFs. The `api_http` helper sets `read_timeout = 120`; keep a generous value for `/v1/parse/json`.
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals before posting.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create UBL API reference](https://www.invoicexml.com/docs/api/create/ubl)
- [Validate UBL API reference](https://www.invoicexml.com/docs/api/validate/ubl)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Render UBL to PDF API reference](https://www.invoicexml.com/docs/api/render/ubl/to/pdf)
- [Parse JSON (AI) API reference](https://www.invoicexml.com/docs/api/parse/json)
- [Extract attachments API reference](https://www.invoicexml.com/docs/api/extract/attachments)
- [Ruby Net::HTTP documentation](https://docs.ruby-lang.org/en/master/Net/HTTP.html)
- [Main repository README](../README.md)
