# Create a UBL 2.1 invoice using the InvoiceXML API.
# Sends a JSON invoice model and receives the UBL XML.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/create/ubl
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.

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
      postalAddress: {
        line1: "Hauptstraße 12",
        city: "Berlin",
        postCode: "10115",
        country: "DE"
      },
      # Peppol routing address. schemeId is a Peppol EAS code (9930 = German VAT).
      electronicAddress: { identifier: "DE123456789", schemeId: "9930" }
    },
    buyer: {
      name: "Globex SAS",
      postalAddress: {
        line1: "15 rue de Rivoli",
        city: "Paris",
        postCode: "75001",
        country: "FR"
      },
      electronicAddress: { identifier: "FR40303265045", schemeId: "9957" }
    },
    paymentDetails: { paymentAccountIdentifier: "DE89370400440532013000" },
    lines: [
      {
        quantity: 10,
        priceDetails: { netPrice: 150.00 },
        vatInformation: { rate: 19.00 },
        item: { name: "Senior consulting" }
      }
    ]
  },
  # en16931, peppol-bis-3 (default), nlcius, ehf, xrechnung, or pint.
  options: { profile: "peppol-bis-3" }
}

uri = URI("#{BASE_URL}/v1/create/ubl")
request = Net::HTTP::Post.new(uri)
request["Authorization"] = "Bearer #{API_KEY}"
request["Content-Type"]  = "application/json"
request.body = JSON.generate(payload)

response = api_http(uri).request(request)

unless response.is_a?(Net::HTTPSuccess)
  warn "InvoiceXML API error #{response.code}: #{response.body}"
  exit 1
end

File.binwrite("invoice-ubl.xml", response.body)
puts "Saved invoice-ubl.xml (#{response.body.bytesize} bytes)"
