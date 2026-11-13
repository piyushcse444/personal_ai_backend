package com.personal_ai.backend.Test;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

/**
 * Example usage of the Google GenAI SDK with Gemini.
 * Set the GOOGLE_API_KEY environment variable before running.
 */
public class geminai {

    public static void main(String[] args) {
        try {
            // Load API key from environment variable for security
            String apiKey = System.getenv("GOOGLE_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("Error: GOOGLE_API_KEY environment variable not set.");
                return;
            }

            // Initialize Gemini API client
            Client client = new Client.Builder()
                    .apiKey(apiKey)
                    .build();

            // Send request and get response
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    "Explain how AI works in a few words",
                    null
            );

            // Output result
            System.out.println("Gemini Response:");
            System.out.println(response.text());

        } catch (Exception e) {
            System.err.println("Error communicating with Gemini API: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
