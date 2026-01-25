package com.mine.quizgen.service;

import com.mine.quizgen.model.Article;
import com.mine.quizgen.model.Quiz;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroqAIService {
    
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper;
    
    private final List<String> modelPriority = Arrays.asList(
        "llama-3.3-70b-versatile",
        "mixtral-8x7b-32768",
        "llama-3.1-8b-instant"
    );
    
    private int currentModelIndex = 0;
    private int requestCount = 0;
    private final int dailyLimit = 10;
    private final int retryDelay = 30;
    private final String apiKey;
    private static final Pattern JSON_PATTERN = Pattern.compile("(\\{.*\\})", Pattern.DOTALL);
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile("```json\\s*|\\s*```");
    
    public GroqAIService(String apiKey) {
        this.apiKey = apiKey;
        // Configure ObjectMapper to handle Java 8 dates
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
    
    private String getCurrentModel() {
        return modelPriority.get(currentModelIndex % modelPriority.size());
    }
    
    private void rotateModel() {
        currentModelIndex++;
        System.out.println("🔄 Switching to model: " + getCurrentModel());
    }
    
    private String createEfficientPrompt(String articleText, String articleTitle, String articleId) {
        if (articleText.length() > 12000) {
            articleText = articleText.substring(0, 12000) + "... [truncated]";
        }
        
        String titlePreview = articleTitle.length() > 40 ? 
            articleTitle.substring(0, 40) : articleTitle;
        
        return "Create 20 multiple-choice questions from this article.\n\n" +
               "ARTICLE TITLE: " + articleTitle + "\n" +
               "ARTICLE CONTENT:\n" +
               articleText + "\n\n" +
               "INSTRUCTIONS - READ CAREFULLY:\n" +
               "1. Create EXACTLY 20 multiple-choice questions. Not 19, not 21. Exactly 20.\n" +
               "2. Difficulty distribution: 8 Easy, 7 Medium, 5 Hard.\n" +
               "3. For EACH question, provide:\n" +
               "   - A clear, complete question text\n" +
               "   - 4 distinct options labeled A, B, C, D\n" +
               "   - The correct answer letter (A, B, C, or D)\n" +
               "   - A brief explanation (1-2 sentences)\n" +
               "   - The difficulty level (Easy, Medium, or Hard)\n\n" +
               "4. Base ALL questions SOLELY on the article content provided. Do not use external knowledge.\n" +
               "5. Format the output as a VALID JSON object with this EXACT structure:\n\n" +
               "{\n" +
               "  \"quiz_title\": \"Quiz: " + titlePreview + "\",\n" +
               "  \"article_id\": \"" + articleId + "\",\n" +
               "  \"generated_at\": \"timestamp\",\n" +
               "  \"questions\": [\n" +
               "    {\n" +
               "      \"id\": 1,\n" +
               "      \"question\": \"Full question text here?\",\n" +
               "      \"options\": {\"A\": \"Option A text\", \"B\": \"Option B text\", \"C\": \"Option C text\", \"D\": \"Option D text\"},\n" +
               "      \"correct_answer\": \"A\",\n" +
               "      \"explanation\": \"Brief explanation here.\",\n" +
               "      \"difficulty\": \"Easy\"\n" +
               "    }\n" +
               "  ]\n" +
               "}\n\n" +
               "CRITICAL: The \"questions\" array must contain exactly 20 objects. Output ONLY the JSON, no additional text, no markdown formatting, no code blocks.";
    }
    
    public Quiz generateFromArticleData(Article article, int maxRetries) {
        if (requestCount >= dailyLimit) {
            Quiz errorQuiz = new Quiz();
            errorQuiz.setSuccess(false);
            errorQuiz.setErrorMessage("Daily limit (" + dailyLimit + ") reached");
            return errorQuiz;
        }
        
        int retryCount = 0;
        String prompt = createEfficientPrompt(
            article.getFullText(), 
            article.getTitle(),
            article.getId()
        );
        
        while (retryCount <= maxRetries) {
            try {
                String currentModel = getCurrentModel();
                System.out.println("🤖 Using Groq model: " + currentModel + " (Attempt " + (retryCount + 1) + ")");
                
                // Escape JSON properly for the request
                String escapedPrompt = prompt
                    .replace("\\", "\\\\")  // Escape backslashes first
                    .replace("\"", "\\\"")  // Escape quotes
                    .replace("\n", "\\n")   // Escape newlines
                    .replace("\r", "\\r")   // Escape carriage returns
                    .replace("\t", "\\t");  // Escape tabs
                
                // Build JSON request with proper formatting
                String requestBody = "{" +
                    "\"model\": \"" + currentModel + "\"," +
                    "\"messages\": [" +
                    "{" +
                    "\"role\": \"system\"," +
                    "\"content\": \"You are an expert quiz creator. You always output valid JSON without any additional text.\"" +
                    "}," +
                    "{" +
                    "\"role\": \"user\"," +
                    "\"content\": \"" + escapedPrompt + "\"" +
                    "}" +
                    "]," +
                    "\"temperature\": 0.7," +
                    "\"max_completion_tokens\": 3500," +
                    "\"n\": 1" +
                    "}";
                
                // Log request for debugging
                System.out.println("📤 Sending request with model: " + currentModel);
                System.out.println("📏 Prompt length: " + prompt.length() + " chars");
                
                Request request = new Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    
                    if (!response.isSuccessful()) {
                        // Try to extract more detailed error information
                        String errorDetail = "Status: " + response.code() + " " + response.message();
                        try {
                            Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
                            if (errorMap.containsKey("error") && errorMap.get("error") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> errorObj = (Map<String, Object>) errorMap.get("error");
                                errorDetail += " - " + errorObj.getOrDefault("message", "No error details");
                            }
                        } catch (Exception e) {
                            // If we can't parse the error JSON, just use the raw response
                            if (responseBody.length() > 0) {
                                errorDetail += " - Response: " + responseBody.substring(0, Math.min(200, responseBody.length()));
                            }
                        }
                        throw new IOException("Unexpected response: " + errorDetail);
                    }
                    
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (choices == null || choices.isEmpty()) {
                        throw new IOException("No choices in response");
                    }
                    
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    String responseText = (String) message.get("content");
                    
                    requestCount++;
                    
                    // Clean response
                    responseText = MARKDOWN_PATTERN.matcher(responseText).replaceAll("").trim();
                    
                    // Extract JSON
                    Matcher jsonMatcher = JSON_PATTERN.matcher(responseText);
                    if (jsonMatcher.find()) {
                        responseText = jsonMatcher.group(1);
                    } else {
                        // If no JSON found, try to find any JSON-like structure
                        int jsonStart = responseText.indexOf('{');
                        int jsonEnd = responseText.lastIndexOf('}');
                        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                            responseText = responseText.substring(jsonStart, jsonEnd + 1);
                        } else {
                            throw new IOException("No valid JSON found in response: " + 
                                responseText.substring(0, Math.min(100, responseText.length())) + "...");
                        }
                    }
                    
                    // Parse into Quiz object
                    Quiz quiz = objectMapper.readValue(responseText, Quiz.class);
                    quiz.setModelUsed(currentModel);
                    quiz.setTokenEstimate(article.getTokenEstimate());
                    quiz.setApiProvider("Groq");
                    
                    System.out.println("✅ Success with " + currentModel);
                    System.out.println("📊 Generated quiz with " + quiz.getQuestionCount() + " questions");
                    return quiz;
                }
                
            } catch (IOException e) {
                String errorStr = e.getMessage();
                System.out.println("⚠️ Error: " + errorStr);
                
                // Check for rate limiting
                if (errorStr != null && errorStr.contains("429") && retryCount < maxRetries) {
                    int waitTime = retryDelay * (retryCount + 1);
                    System.out.println("⏳ Rate limited. Waiting " + waitTime + "s...");
                    try {
                        Thread.sleep(waitTime * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    rotateModel();
                    retryCount++;
                    continue;
                } else if (errorStr != null && errorStr.contains("429")) {
                    Quiz errorQuiz = new Quiz();
                    errorQuiz.setSuccess(false);
                    errorQuiz.setErrorMessage("Rate limit exceeded. Try again later.");
                    return errorQuiz;
                }
                // Check for 400 errors
                else if (errorStr != null && errorStr.contains("400") && retryCount < maxRetries) {
                    System.out.println("🔄 400 error detected. Trying with a different model...");
                    rotateModel();
                    retryCount++;
                    
                    // Reduce max tokens for smaller models
                    if (getCurrentModel().contains("8b") || getCurrentModel().contains("mixtral")) {
                        System.out.println("📉 Reducing max_completion_tokens for smaller model...");
                    }
                    
                    continue;
                } else {
                    Quiz errorQuiz = new Quiz();
                    errorQuiz.setSuccess(false);
                    errorQuiz.setErrorMessage("Groq API error: " + 
                        (errorStr != null ? errorStr.substring(0, Math.min(200, errorStr.length())) : "Unknown error"));
                    return errorQuiz;
                }
            } catch (Exception e) {
                System.out.println("❌ Unexpected error: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                Quiz errorQuiz = new Quiz();
                errorQuiz.setSuccess(false);
                errorQuiz.setErrorMessage("Unexpected error: " + e.getMessage());
                return errorQuiz;
            }
        }
        
        Quiz errorQuiz = new Quiz();
        errorQuiz.setSuccess(false);
        errorQuiz.setErrorMessage("Max retries exceeded");
        return errorQuiz;
    }
    
    public String saveQuiz(Quiz quiz, String basePath) throws IOException {
        String articleId = quiz.getArticleId() != null ? quiz.getArticleId() : "unknown";
        String filename = "quiz_" + articleId + "_groq.json";
        File outputDir = new File(basePath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        String filepath = basePath + File.separator + filename;
        objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValue(new File(filepath), quiz);
        
        System.out.println("💾 Quiz saved to: " + filepath);
        return filepath;
    }
    
    public Map<String, Object> getUsage() {
        Map<String, Object> usage = new HashMap<>();
        usage.put("requests_today", requestCount);
        usage.put("daily_limit", dailyLimit);
        usage.put("remaining", dailyLimit - requestCount);
        usage.put("provider", "Groq");
        return usage;
    }
}