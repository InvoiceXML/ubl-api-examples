# Render a UBL XML file into a human-readable PDF using the InvoiceXML API.
# The rendered PDF is a preview for people to read: the XML file remains the
# authoritative invoice for compliance and tax purposes.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/render/ubl/to/pdf
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby render.rb [path/to/invoice.xml] [preview.pdf]

require "net/http"
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

def render_ubl(xml_path, pdf_path)
  uri = URI("#{BASE_URL}/v1/render/ubl/to/pdf")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(xml_path, "rb") do |file|
    # Label language for the rendered PDF: en, de, or fr. Defaults to en.
    request.set_form([["file", file], ["language", "de"]], "multipart/form-data")
    response = api_http(uri).request(request)

    unless response.is_a?(Net::HTTPSuccess)
      warn "InvoiceXML API error #{response.code}: #{response.body}"
      exit 1
    end

    File.binwrite(pdf_path, response.body)
    response.body.bytesize
  end
end

pdf_path = ARGV.fetch(1, "invoice-preview.pdf")
bytes = render_ubl(ARGV.fetch(0, "invoice-ubl.xml"), pdf_path)
puts "Saved #{pdf_path} (#{bytes} bytes)"
