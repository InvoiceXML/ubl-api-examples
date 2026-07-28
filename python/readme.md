# UBL for Python: InvoiceXML API Examples

Python code samples for creating, validating, and parsing **UBL** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **Python 3.7+** (3.10+ recommended). Runs in Django, Flask, FastAPI, Pandas pipelines, Jupyter notebooks, AWS Lambda, Google Cloud Functions, Azure Functions, or plain scripts.

For background on UBL itself (what it is, the profiles, where it is used), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `api_key` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Python 3.7 or later** (3.10+ recommended)
- The **`requests`** library

```bash
pip install requests
```

Or with [uv](https://github.com/astral-sh/uv) (the modern alternative):

```bash
uv pip install requests
```

`requests` is the standard HTTP client for Python: clean multipart support, automatic JSON decoding, and the most familiar API in the ecosystem. If you prefer the modern async alternative, `httpx` translates almost line-for-line.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.py`](./create.py) | Build a UBL 2.1 XML invoice | [`POST /v1/create/ubl`](https://www.invoicexml.com/docs/api/create/ubl) |
| [`validate.py`](./validate.py) | Validate a UBL file against the EN 16931 and Peppol rules | [`POST /v1/validate/ubl`](https://www.invoicexml.com/docs/api/validate/ubl) |
| [`extract_json.py`](./extract_json.py) | Parse a UBL XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`ai_convert.py`](./ai_convert.py) | (Experimental) Convert a plain PDF to UBL with AI | [`POST /v1/transform/to/ubl`](https://www.invoicexml.com/docs/api/transform/to/ubl) |
| [`render.py`](./render.py) | Render UBL XML into a human-readable PDF | [`POST /v1/render/ubl/to/pdf`](https://www.invoicexml.com/docs/api/render/ubl/to/pdf) |

Each file is standalone and runnable with `python create.py`. Open the file, replace `YOUR_API_KEY` with your real key, and execute.

> **Note on the snippets below:** they are excerpts from those files and assume `api_key` is already defined. When in doubt, copy the complete file.

---

## Create a UBL invoice in Python

```python
import requests

payload = {
    "invoice": {
        "invoiceNumber": "UBL-2026-001",
        "issueDate":     "2026-05-18",
        "currency":      "EUR",
        "buyerReference": "PO-2026-5571",
        "seller": {
            "name":              "Acme GmbH",
            "vatIdentifier":     "DE123456789",
            "legalRegistration": {"identifier": "HRB 12345"},
            "postalAddress": {"line1": "Hauptstraße 12", "city": "Berlin", "postCode": "10115", "country": "DE"},
            "electronicAddress": {"identifier": "DE123456789", "schemeId": "9930"},
        },
        "buyer": {
            "name": "Globex SAS",
            "postalAddress": {"line1": "15 rue de Rivoli", "city": "Paris", "postCode": "75001", "country": "FR"},
            "electronicAddress": {"identifier": "FR40303265045", "schemeId": "9957"},
        },
        "paymentDetails": {"paymentAccountIdentifier": "DE89370400440532013000"},
        "lines": [{
            "quantity":       10,
            "priceDetails":   {"netPrice": 150.00},
            "vatInformation": {"rate": 19.00},
            "item":           {"name": "Senior consulting"},
        }],
    },
    "options": {"profile": "peppol-bis-3"},
}

response = requests.post(
    "https://api.invoicexml.com/v1/create/ubl",
    headers={"Authorization": f"Bearer {api_key}"},
    json=payload,
)

with open("invoice-ubl.xml", "w", encoding="utf-8") as f:
    f.write(response.text)
```

`options.profile` decides both the `CustomizationID` stamped into the document and the rules it is validated against. Peppol BIS Billing 3.0 is the default and needs an `electronicAddress` on both parties plus a `buyerReference`; for invoices that never touch the Peppol network, set the profile to `en16931` and those become optional.

The response is the UBL 2.1 XML document, validated against the profile's rules before delivery.

[Full example: `create.py`](./create.py) | [API reference](https://www.invoicexml.com/docs/api/create/ubl)

---

## Validate a UBL file in Python

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/validate/ubl",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.xml", open("invoice.xml", "rb"), "application/xml")},
)
print(response.text)
```

Returns a JSON validation report listing any rule failures (EN 16931 BR-* and BR-CO-*, plus the CIUS rules, e.g. PEPPOL-EN16931-* for Peppol BIS).

[Full example: `validate.py`](./validate.py) | [API reference](https://www.invoicexml.com/docs/api/validate/ubl)

---

## Extract UBL data as JSON in Python

Useful for Pandas pipelines, Django/Flask models, or any system that prefers JSON over XML.

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/extract/json",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.xml", open("invoice.xml", "rb"), "application/xml")},
)

# The invoice document sits under the "invoice" key of the response.
invoice = response.json()["invoice"]
print(invoice["seller"]["name"], invoice["totals"]["grandTotalAmount"])
```

[Full example: `extract_json.py`](./extract_json.py) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## (Experimental) Convert a plain PDF to UBL with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output before sending it to a customer or tax authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-ubl-conversion-experimental).

The endpoint takes the PDF alone; no extra parameters are needed.

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/transform/to/ubl",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("plain-invoice.pdf", open("plain-invoice.pdf", "rb"), "application/pdf")},
)

with open("converted-ubl.xml", "w", encoding="utf-8") as f:
    f.write(response.text)
```

[Full example: `ai_convert.py`](./ai_convert.py) | [API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)

---

## Render UBL as a readable PDF in Python

UBL has no visual layer: the XML *is* the invoice, which is fine for machines and useless for the person in accounts payable who wants to read it. This endpoint renders the XML into a formatted PDF preview, auto-detecting whether the file is CII or UBL syntax. The PDF is for reading only; the XML file remains the authoritative invoice for compliance and tax purposes.

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/render/ubl/to/pdf",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.xml", open("invoice.xml", "rb"), "application/xml")},
    data={"language": "de"},   # en, de, or fr
)

with open("invoice-preview.pdf", "wb") as f:
    f.write(response.content)
```

[Full example: `render.py`](./render.py) | [API reference](https://www.invoicexml.com/docs/api/render/ubl/to/pdf)

---

## Framework integration

### Django

Return a UBL invoice from a view:

```python
from django.http import HttpResponse

def download_ubl(request, invoice_id):
    pdf_bytes = create_ubl_for(invoice_id)
    response = HttpResponse(pdf_bytes, content_type="application/pdf")
    response["Content-Disposition"] = f'attachment; filename="invoice-{invoice_id}.pdf"'
    return response
```

Store the API key in `settings.py` via `INVOICEXML_API_KEY = os.environ["INVOICEXML_API_KEY"]`.

### Flask

```python
from flask import send_file
from io import BytesIO

@app.route("/invoices/<int:invoice_id>/ubl")
def get_ubl(invoice_id):
    pdf_bytes = create_ubl_for(invoice_id)
    return send_file(BytesIO(pdf_bytes), mimetype="application/pdf",
                     as_attachment=True, download_name=f"invoice-{invoice_id}.pdf")
```

### FastAPI

```python
from fastapi.responses import Response

@app.get("/invoices/{invoice_id}/ubl")
async def get_ubl(invoice_id: int):
    pdf_bytes = create_ubl_for(invoice_id)
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="invoice-{invoice_id}.pdf"'},
    )
```

### Pandas and data pipelines

The `extract_json.py` example fits naturally into a Pandas pipeline: walk a folder of UBL PDFs, extract each to JSON, and load into a DataFrame for analysis or bulk archival.

```python
import pandas as pd, requests, os

rows = []
for pdf in os.listdir("invoices/"):
    with open(f"invoices/{pdf}", "rb") as f:
        data = requests.post(
            "https://api.invoicexml.com/v1/extract/json",
            headers={"Authorization": f"Bearer {api_key}"},
            files={"file": (pdf, f, "application/pdf")},
        ).json()
    rows.append(data)

df = pd.DataFrame(rows)
```

### AWS Lambda

The `requests` library works in Lambda directly. Include it via Lambda layers or in your deployment zip.

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting `api_key` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`SSL: CERTIFICATE_VERIFY_FAILED` on macOS**: run `/Applications/Python\ 3.x/Install\ Certificates.command` to install Python's CA bundle. Do not disable SSL verification in production.
- **`ConnectionError` or timeout**: add an explicit `timeout=30` parameter to `requests.post()`. AI conversion in particular can take 10 to 30 seconds for larger PDFs.
- **File handle warnings**: the examples use inline `open()` for brevity. For production code, prefer `with open(...) as f:` to ensure file handles close deterministically.
- **PEPPOL-EN16931-* failures on Validate**: a Peppol-specific requirement is missing. The most common are a missing electronic address on the seller or buyer, and a missing `buyerReference` (Peppol requires a buyer reference or a purchase order reference).

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create UBL API reference](https://www.invoicexml.com/docs/api/create/ubl)
- [Validate UBL API reference](https://www.invoicexml.com/docs/api/validate/ubl)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to UBL API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)
- [requests library documentation](https://requests.readthedocs.io/)
- [Main repository README](../README.md)
