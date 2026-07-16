// Experimental: convert a plain PDF invoice to UBL using AI.
// Review output manually before sending to a customer or tax authority.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/transform/to/ubl
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AiConvert {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "plain-invoice.pdf",
                RequestBody.create(new File("plain-invoice.pdf"), MediaType.parse("application/pdf")))
            .build();

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/transform/to/ubl")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            String xml = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + xml);
                System.exit(1);
            }
            Files.write(Paths.get("converted-ubl.xml"), xml.getBytes("UTF-8"));
            System.out.println("Saved converted-ubl.xml (" + xml.length() + " chars)");
            System.out.println("Review the output manually before sending.");
        }
    }
}
