// Create a UBL 2.1 invoice using the InvoiceXML API.
// Sends a JSON invoice model and receives the UBL XML.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/create/ubl
//
// Requires Java 15+ (uses text blocks).
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Create {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        // options.profile decides both the CustomizationID stamped into the document
        // and the rules it is validated against. Peppol BIS Billing 3.0 is the default
        // and requires an electronic address on both parties plus a buyer reference.
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
              "options": { "profile": "peppol-bis-3" }
            }
            """;

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/create/ubl")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            String xml = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + xml);
                System.exit(1);
            }
            Files.write(Paths.get("invoice-ubl.xml"), xml.getBytes("UTF-8"));
            System.out.println("Saved invoice-ubl.xml (" + xml.length() + " chars)");
        }
    }
}
