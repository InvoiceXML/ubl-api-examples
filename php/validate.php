<?php
// Validate a UBL XML file using the InvoiceXML API.
// Runs the UBL 2.1 XSD, the EN 16931 Schematron, and the CIUS overlay the document declares.
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/validate/ubl

// Raw key only, without the "Bearer " prefix (it is added below).
$apiKey = 'YOUR_API_KEY';

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
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($status !== 200) {
    fwrite(STDERR, "InvoiceXML API error $status: $report\n");
    exit(1);
}

echo $report;
