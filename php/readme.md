# UBL for PHP: InvoiceXML API Examples

PHP code samples for creating, validating, and parsing **UBL** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **PHP 7.0+** (PHP 8.x recommended), works in plain scripts, Laravel, Symfony, WordPress, WooCommerce, Drupal, Magento, or any other PHP framework. **Zero Composer dependencies**: uses PHP's built-in cURL extension.

For background on UBL itself (what it is, the profiles, where it is used), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `$apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **PHP 7.0 or later** (PHP 8.2+ recommended)
- The **cURL extension** (`php-curl`), bundled with most PHP installations by default
- No Composer, no external libraries

These examples use the built-in `curl_*` functions and `CURLFile` (available since PHP 5.5), so they run on essentially any modern PHP install without `composer install`.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.php`](./create.php) | Build a UBL 2.1 XML invoice | [`POST /v1/create/ubl`](https://www.invoicexml.com/docs/api/create/ubl) |
| [`send-email.php`](./send-email.php) | Create a UBL invoice and email it from your own domain | [`POST /v1/create/ubl/email`](https://www.invoicexml.com/docs/api/create/ubl/email) |
| [`validate.php`](./validate.php) | Validate a UBL file against the EN 16931 and Peppol rules | [`POST /v1/validate/ubl`](https://www.invoicexml.com/docs/api/validate/ubl) |
| [`extract-json.php`](./extract-json.php) | Parse a UBL XML into JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`ai-convert.php`](./ai-convert.php) | (Experimental) Convert a plain PDF to UBL with AI | [`POST /v1/transform/to/ubl`](https://www.invoicexml.com/docs/api/transform/to/ubl) |
| [`render.php`](./render.php) | Render UBL XML into a human-readable PDF | [`POST /v1/render/ubl/to/pdf`](https://www.invoicexml.com/docs/api/render/ubl/to/pdf) |

Each file is standalone and runnable with `php create.php`. Open the file, replace `YOUR_API_KEY` with your real key, and execute.

> **Note on the snippets below:** they are excerpts from those files and assume `$apiKey` is already defined. When in doubt, copy the complete file.

---

## Create a UBL invoice in PHP

```php
$payload = [
    'invoice' => [
        'invoiceNumber' => 'UBL-2026-001',
        'issueDate'     => '2026-05-18',
        'currency'      => 'EUR',
        'buyerReference' => 'PO-2026-5571',
        'seller' => [
            'name'              => 'Acme GmbH',
            'vatIdentifier'     => 'DE123456789',
            'legalRegistration' => ['identifier' => 'HRB 12345'],
            'postalAddress'     => ['line1' => 'Hauptstraße 12', 'city' => 'Berlin', 'postCode' => '10115', 'country' => 'DE'],
            'electronicAddress' => ['identifier' => 'DE123456789', 'schemeId' => '9930'],
        ],
        'buyer' => [
            'name'              => 'Globex SAS',
            'postalAddress'     => ['line1' => '15 rue de Rivoli', 'city' => 'Paris', 'postCode' => '75001', 'country' => 'FR'],
            'electronicAddress' => ['identifier' => 'FR40303265045', 'schemeId' => '9957'],
        ],
        'paymentDetails' => ['paymentAccountIdentifier' => 'DE89370400440532013000'],
        'lines' => [[
            'quantity'       => 10,
            'priceDetails'   => ['netPrice' => 150.00],
            'vatInformation' => ['rate' => 19.00],
            'item'           => ['name' => 'Senior consulting'],
        ]],
    ],
    'options' => ['profile' => 'peppol-bis-3'],
];

$ch = curl_init('https://api.invoicexml.com/v1/create/ubl');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => json_encode($payload),
    CURLOPT_HTTPHEADER     => [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $apiKey,
    ],
]);
$xml = curl_exec($ch);
file_put_contents('invoice-ubl.xml', $xml);
```

`options.profile` decides both the `CustomizationID` stamped into the document and the rules it is validated against. Peppol BIS Billing 3.0 is the default and needs an `electronicAddress` on both parties plus a `buyerReference`; for invoices that never touch the Peppol network, set the profile to `en16931` and those become optional.

The response is the UBL 2.1 XML document, validated against the profile's rules before delivery.

[Full example: `create.php`](./create.php) | [API reference](https://www.invoicexml.com/docs/api/create/ubl)

---

## Validate a UBL file in PHP

```php
$ch = curl_init('https://api.invoicexml.com/v1/validate/ubl');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file' => new CURLFile('invoice.xml', 'application/xml'),
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$report = curl_exec($ch);
echo $report;
```

Returns a JSON validation report listing any rule failures (EN 16931 BR-* and BR-CO-*, plus the CIUS rules, e.g. PEPPOL-EN16931-* for Peppol BIS).

[Full example: `validate.php`](./validate.php) | [API reference](https://www.invoicexml.com/docs/api/validate/ubl)

---

## Extract UBL data as JSON in PHP

Useful for feeding UBL invoices into REST APIs, ERPs, accounting systems, or any pipeline that prefers JSON over XML.

```php
$ch = curl_init('https://api.invoicexml.com/v1/extract/json');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => ['file' => new CURLFile('invoice.xml', 'application/xml')],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$json = curl_exec($ch);
$data = json_decode($json, true);
// The invoice document sits under the "invoice" key of the response.
echo $data['invoice']['seller']['name'];
```

[Full example: `extract-json.php`](./extract-json.php) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

---

## (Experimental) Convert a plain PDF to UBL with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output before sending it to a customer or tax authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-ubl-conversion-experimental).

The endpoint takes the PDF alone; no extra parameters are needed.

```php
$ch = curl_init('https://api.invoicexml.com/v1/transform/to/ubl');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file'           => new CURLFile('plain-invoice.pdf', 'application/pdf'),
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$xml = curl_exec($ch);
file_put_contents('converted-ubl.xml', $xml);
```

[Full example: `ai-convert.php`](./ai-convert.php) | [API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)

---

## Render UBL as a readable PDF in PHP

UBL has no visual layer: the XML *is* the invoice, which is fine for machines and useless for the person in accounts payable who wants to read it. This endpoint renders the XML into a formatted PDF preview, auto-detecting whether the file is CII or UBL syntax. The PDF is for reading only; the XML file remains the authoritative invoice for compliance and tax purposes.

```php
$ch = curl_init('https://api.invoicexml.com/v1/render/ubl/to/pdf');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file'     => new CURLFile('invoice.xml', 'application/xml'),
        'language' => 'de',   // en, de, or fr
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$pdf = curl_exec($ch);
file_put_contents('invoice-preview.pdf', $pdf);
```

[Full example: `render.php`](./render.php) | [API reference](https://www.invoicexml.com/docs/api/render/ubl/to/pdf)

---

## Framework integration

### Laravel

Return a UBL invoice from a controller or route closure:

```php
use Illuminate\Support\Facades\Route;

Route::get('/invoices/{id}/ubl', function ($id) {
    $pdf = app(UBLService::class)->create($id);
    return response($pdf, 200, [
        'Content-Type'        => 'application/pdf',
        'Content-Disposition' => "attachment; filename=\"invoice-{$id}.pdf\"",
    ]);
});
```

Store the API key in `config/services.php` and read it via `config('services.invoicexml.key')`.

### Symfony

```php
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class InvoiceController
{
    #[Route('/invoices/{id}/ubl', name: 'ubl_download')]
    public function download(int $id): Response
    {
        $pdf = $this->ublService->create($id);
        return new Response($pdf, 200, [
            'Content-Type'        => 'application/pdf',
            'Content-Disposition' => "attachment; filename=\"invoice-{$id}.pdf\"",
        ]);
    }
}
```

### WordPress and WooCommerce

Hook into WooCommerce order lifecycle events to generate UBL invoices automatically:

```php
add_action('woocommerce_order_status_completed', function ($order_id) {
    $order = wc_get_order($order_id);
    $pdf = create_ubl_from_order($order);
    file_put_contents(WP_CONTENT_DIR . "/invoices/{$order_id}.pdf", $pdf);
});
```

The same pattern works in Magento, Drupal Commerce, and PrestaShop.

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting `$apiKey` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`Class "CURLFile" not found`**: PHP cURL extension is missing. Install with `apt-get install php-curl` (Debian/Ubuntu), `yum install php-curl` (RHEL), or enable the extension in `php.ini`.
- **`SSL certificate problem` errors**: the server's CA bundle is out of date. Update OpenSSL on the host, or set `CURLOPT_CAINFO` to a current `cacert.pem` from curl.se/ca/.
- **PEPPOL-EN16931-* failures on Validate**: a Peppol-specific requirement is missing. The most common are a missing electronic address on the seller or buyer, and a missing `buyerReference` (Peppol requires a buyer reference or a purchase order reference).

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create UBL API reference](https://www.invoicexml.com/docs/api/create/ubl)
- [Validate UBL API reference](https://www.invoicexml.com/docs/api/validate/ubl)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to UBL API reference](https://www.invoicexml.com/docs/api/transform/to/ubl)
- [PHP cURL documentation](https://www.php.net/manual/en/book.curl.php)
- [Main repository README](../README.md)
