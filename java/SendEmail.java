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
// Requires Java 15+ (uses text blocks).
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.time.Duration;

public class SendEmail {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        // options.profile decides both the CustomizationID stamped into the document
        // and the rules it is validated against. Peppol BIS Billing 3.0 is the default
        // and requires an electronic address on both parties plus a buyer reference.
        // "from" is any address on your verified domain; send the first test to yourself.
        // Optional: cc and bcc (2 addresses together) and replyTo.
        String json = """
            {
              "invoice": {
                "invoiceNumber": "UBL-2026-001",
                "issueDate": "2026-05-18",
                "currency": "EUR",
                "buyerReference": "PO-2026-5571",
                "seller": {
                  "name": "Acme GmbH",
                  "vatIdentifier": "DE123456789",
                  "legalRegistration": { "identifier": "HRB 12345" },
                  "postalAddress": {
                    "line1": "Hauptstraße 12",
                    "city": "Berlin",
                    "postCode": "10115",
                    "country": "DE"
                  },
                  "electronicAddress": { "identifier": "DE123456789", "schemeId": "9930" }
                },
                "buyer": {
                  "name": "Globex SAS",
                  "postalAddress": {
                    "line1": "15 rue de Rivoli",
                    "city": "Paris",
                    "postCode": "75001",
                    "country": "FR"
                  },
                  "electronicAddress": { "identifier": "FR40303265045", "schemeId": "9957" }
                },
                "paymentDetails": { "paymentAccountIdentifier": "DE89370400440532013000" },
                "lines": [
                  {
                    "quantity": 10,
                    "priceDetails": { "netPrice": 150.00 },
                    "vatInformation": { "rate": 19.00 },
                    "item": { "name": "Senior consulting" }
                  }
                ]
              },
              "options": { "profile": "peppol-bis-3" },
              "email": {
                "from": "invoices@yourcompany.com",
                "fromName": "Acme Billing",
                "to": "you@yourcompany.com",
                "subject": "Invoice UBL-2026-001",
                "message": "Hello,\\n\\nPlease find attached invoice UBL-2026-001.\\n\\nKind regards"
              }
            }
            """;

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/create/ubl/email")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        // The API can take up to 90 seconds. With OkHttp's default 10 second read timeout this
        // call could report a failure after the email went out, and a rerun would send it twice.
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(120)).build();

        try (Response response = client.newCall(request).execute()) {
            String text = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + text);
                System.exit(1);
            }
            // {"status":"accepted","messageId":"..."}
            System.out.println("Email accepted for delivery: " + text);
        }
    }
}
