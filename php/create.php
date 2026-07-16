<?php
// Create a UBL 2.1 invoice using the InvoiceXML API.
// Sends a JSON invoice model and receives the UBL XML.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/create/ubl

// Raw key only, without the "Bearer " prefix (it is added below).
$apiKey = 'YOUR_API_KEY';

$payload = [
    'invoice' => [
        'invoiceNumber' => 'UBL-2026-001',
        'issueDate'     => '2026-05-18',
        'currency'      => 'EUR',
        // Peppol requires a buyer reference (or a purchase order reference).
        'buyerReference' => 'PO-2026-5571',
        'seller' => [
            'name'              => 'Acme GmbH',
            'vatIdentifier'     => 'DE123456789',
            'legalRegistration' => ['identifier' => 'HRB 12345'],
            'postalAddress'     => [
                'line1'    => 'Hauptstraße 12',
                'city'     => 'Berlin',
                'postCode' => '10115',
                'country'  => 'DE',
            ],
            // Peppol routing address. schemeId is a Peppol EAS code (9930 = German VAT).
            'electronicAddress' => ['identifier' => 'DE123456789', 'schemeId' => '9930'],
        ],
        'buyer' => [
            'name' => 'Globex SAS',
            'postalAddress' => [
                'line1'    => '15 rue de Rivoli',
                'city'     => 'Paris',
                'postCode' => '75001',
                'country'  => 'FR',
            ],
            'electronicAddress' => ['identifier' => 'FR40303265045', 'schemeId' => '9957'],
        ],
        'paymentDetails' => ['paymentAccountIdentifier' => 'DE89370400440532013000'],
        'lines' => [
            [
                'quantity'       => 10,
                'priceDetails'   => ['netPrice' => 150.00],
                'vatInformation' => ['rate' => 19.00],
                'item'           => ['name' => 'Senior consulting'],
            ],
        ],
    ],
    // en16931, peppol-bis-3 (default), nlcius, ehf, xrechnung, or pint.
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

$xml    = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($status !== 200) {
    fwrite(STDERR, "InvoiceXML API error $status: $xml\n");
    exit(1);
}

file_put_contents('invoice-ubl.xml', $xml);
echo "Saved invoice-ubl.xml (" . strlen($xml) . " bytes)\n";
