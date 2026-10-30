// Create a UBL invoice and email it with the InvoiceXML API, in one call.
// Builds and validates the invoice exactly like POST /v1/create/ubl, then
// sends it as an attachment from your own domain. This sends a real email.
// The answer is the message id of the email, not the file.
//
// Before the first run, add and verify your domain:
// https://www.invoicexml.com/account/sending-domains
// Your plan must include email delivery: https://www.invoicexml.com/pricing
//
// InvoiceXML:          https://www.invoicexml.com/
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/create/ubl/email
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class SendUblEmail
{
    private const string Endpoint = "https://api.invoicexml.com/v1/create/ubl/email";

    public static async Task<string> RunAsync(
        // en16931, peppol-bis-3 (default), nlcius, ehf, xrechnung, or pint.
        string profile = "peppol-bis-3")
    {
        // Get an InvoiceXML API key at:
        // https://www.invoicexml.com/account/authentication
        // Raw key only, without the "Bearer " prefix (WithOAuthBearerToken adds it).
        var apiKey = Environment.GetEnvironmentVariable("INVOICEXML_API_KEY")
            ?? throw new InvalidOperationException(
                "Set INVOICEXML_API_KEY environment variable. " +
                "Get an InvoiceXML API key at " +
                "https://www.invoicexml.com/account/authentication");

        // Minimal UBL invoice payload; totals and the VAT breakdown are calculated
        // from the line items. Peppol BIS Billing 3.0 additionally requires an
        // electronic address on both parties plus a buyer reference. Full field
        // reference: https://www.invoicexml.com/docs/api/create/ubl
        var payload = new
        {
            invoice = new
            {
                invoiceNumber = "UBL-2026-001",
                issueDate     = "2026-05-18",
                currency      = "EUR",
                buyerReference = "PO-2026-5571",
                seller = new
                {
                    name              = "Acme GmbH",
                    vatIdentifier     = "DE123456789",
                    legalRegistration = new { identifier = "HRB 12345" },
                    postalAddress = new
                    {
                        line1    = "Hauptstraße 12",
                        city     = "Berlin",
                        postCode = "10115",
                        country  = "DE"
                    },
                    // Peppol routing address; schemeId is a Peppol EAS code.
                    electronicAddress = new { identifier = "DE123456789", schemeId = "9930" }
                },
                buyer = new
                {
                    name = "Globex SAS",
                    postalAddress = new
                    {
                        line1    = "15 rue de Rivoli",
                        city     = "Paris",
                        postCode = "75001",
                        country  = "FR"
                    },
                    electronicAddress = new { identifier = "FR40303265045", schemeId = "9957" }
                },
                paymentDetails = new { paymentAccountIdentifier = "DE89370400440532013000" },
                lines = new[]
                {
                    new
                    {
                        quantity       = 10,
                        priceDetails   = new { netPrice = 150.00m },
                        vatInformation = new { rate = 19.00m },
                        item           = new { name = "Senior consulting" }
                    }
                }
            },
            options = new { profile },
            email = new
            {
                @from    = "invoices@yourcompany.com", // any address on your verified domain
                fromName = "Acme Billing",
                to       = "you@yourcompany.com",      // send the first test to yourself
                subject  = "Invoice UBL-2026-001",
                message  = "Hello,\n\nPlease find attached invoice UBL-2026-001.\n\nKind regards"
                // Optional: cc and bcc (2 addresses together) and replyTo.
            }
        };

        try
        {
            var answer = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostJsonAsync(payload)
                .ReceiveString();

            // {"status":"accepted","messageId":"..."}
            Console.WriteLine($"Email accepted for delivery: {answer}");
            return answer;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
