# Create a UBL 2.1 invoice using the InvoiceXML API.
# Sends a JSON invoice model and receives the UBL XML.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/create/ubl
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

payload = {
    "invoice": {
        "invoiceNumber": "UBL-2026-001",
        "issueDate":     "2026-05-18",
        "currency":      "EUR",
        # Peppol requires a buyer reference (or a purchase order reference).
        "buyerReference": "PO-2026-5571",
        "seller": {
            "name":              "Acme GmbH",
            "vatIdentifier":     "DE123456789",
            "legalRegistration": {"identifier": "HRB 12345"},
            "postalAddress": {
                "line1":    "Hauptstraße 12",
                "city":     "Berlin",
                "postCode": "10115",
                "country":  "DE",
            },
            # Peppol routing address. schemeId is a Peppol EAS code (9930 = German VAT).
            "electronicAddress": {"identifier": "DE123456789", "schemeId": "9930"},
        },
        "buyer": {
            "name": "Globex SAS",
            "postalAddress": {
                "line1":    "15 rue de Rivoli",
                "city":     "Paris",
                "postCode": "75001",
                "country":  "FR",
            },
            "electronicAddress": {"identifier": "FR40303265045", "schemeId": "9957"},
        },
        "paymentDetails": {"paymentAccountIdentifier": "DE89370400440532013000"},
        "lines": [
            {
                "quantity":       10,
                "priceDetails":   {"netPrice": 150.00},
                "vatInformation": {"rate": 19.00},
                "item":           {"name": "Senior consulting"},
            },
        ],
    },
    # en16931, peppol-bis-3 (default), nlcius, ehf, xrechnung, or pint.
    "options": {"profile": "peppol-bis-3"},
}

response = requests.post(
    "https://api.invoicexml.com/v1/create/ubl",
    headers={"Authorization": f"Bearer {api_key}"},
    json=payload,
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

with open("invoice-ubl.xml", "w", encoding="utf-8") as f:
    f.write(response.text)

print(f"Saved invoice-ubl.xml ({len(response.text)} chars)")
