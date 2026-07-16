# Experimental: convert a plain PDF invoice to UBL using AI.
# Review output manually before sending to a customer or tax authority.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/transform/to/ubl
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

response = requests.post(
    "https://api.invoicexml.com/v1/transform/to/ubl",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("plain-invoice.pdf", open("plain-invoice.pdf", "rb"), "application/pdf")},
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

with open("converted-ubl.xml", "w", encoding="utf-8") as f:
    f.write(response.text)

print(f"Saved converted-ubl.xml ({len(response.text)} chars)")
print("Review the output manually before sending.")
