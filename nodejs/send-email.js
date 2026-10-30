// Create a UBL invoice and email it with the InvoiceXML API, in one call.
// Builds and validates the invoice exactly like POST /v1/create/ubl, then
// sends it as an attachment from your own domain. This sends a real email.
// The answer is the message id of the email, not the file.
//
// Before the first run, add and verify your domain:
// https://www.invoicexml.com/account/sending-domains
// Your plan must include email delivery: https://www.invoicexml.com/pricing
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/create/ubl/email
//
// Requires Node.js 18+. No dependencies.

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

const payload = {
    invoice: {
        invoiceNumber: 'UBL-2026-001',
        issueDate:     '2026-05-18',
        currency:      'EUR',
        // Peppol requires a buyer reference (or a purchase order reference).
        buyerReference: 'PO-2026-5571',
        seller: {
            name:              'Acme GmbH',
            vatIdentifier:     'DE123456789',
            legalRegistration: { identifier: 'HRB 12345' },
            postalAddress: {
                line1:    'Hauptstraße 12',
                city:     'Berlin',
                postCode: '10115',
                country:  'DE',
            },
            // Peppol routing address. schemeId is a Peppol EAS code (9930 = German VAT).
            electronicAddress: { identifier: 'DE123456789', schemeId: '9930' },
        },
        buyer: {
            name: 'Globex SAS',
            postalAddress: {
                line1:    '15 rue de Rivoli',
                city:     'Paris',
                postCode: '75001',
                country:  'FR',
            },
            electronicAddress: { identifier: 'FR40303265045', schemeId: '9957' },
        },
        paymentDetails: { paymentAccountIdentifier: 'DE89370400440532013000' },
        lines: [
            {
                quantity:       10,
                priceDetails:   { netPrice: 150.00 },
                vatInformation: { rate: 19.00 },
                item:           { name: 'Senior consulting' },
            },
        ],
    },
    // en16931, peppol-bis-3 (default), nlcius, ehf, xrechnung, or pint.
    options: { profile: 'peppol-bis-3' },
    email: {
        from:     'invoices@yourcompany.com', // any address on your verified domain
        fromName: 'Acme Billing',
        to:       'you@yourcompany.com',      // send the first test to yourself
        subject:  'Invoice UBL-2026-001',
        message:  'Hello,\n\nPlease find attached invoice UBL-2026-001.\n\nKind regards',
        // Optional: cc and bcc (2 addresses together) and replyTo.
    },
};

(async () => {
    const response = await fetch('https://api.invoicexml.com/v1/create/ubl/email', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer ' + apiKey,
        },
        body: JSON.stringify(payload),
    });

    if (!response.ok) {
        console.error(`InvoiceXML API error ${response.status}: ${await response.text()}`);
        process.exit(1);
    }

    const { messageId } = await response.json();
    console.log(`Email accepted for delivery, message id ${messageId}`);
})();
