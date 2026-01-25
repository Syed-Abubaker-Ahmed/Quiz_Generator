package com.mine.quizgen.client;

import com.mine.quizgen.model.Article;
import com.mine.quizgen.model.Quiz;
import com.mine.quizgen.service.GroqAIService;
import java.util.Map;

/**
 * A client wrapper for the Groq AI Service.
 * This provides a cleaner API interface for the main application.
 */
public class GroqApiClient {
    
    private final GroqAIService aiService;
    
    public GroqApiClient(String apiKey) {
        this.aiService = new GroqAIService(apiKey);
    }
    
    /**
     * Generates a quiz from article data.
     * 
     * @param article The article to generate a quiz from
     * @param maxRetries Maximum number of retry attempts on failure
     * @return The generated quiz
     */
    public Quiz generateQuiz(Article article, int maxRetries) {
        return aiService.generateFromArticleData(article, maxRetries);
    }
    
    /**
     * Generates a quiz from article data with default retry settings.
     * 
     * @param article The article to generate a quiz from
     * @return The generated quiz
     */
    public Quiz generateQuiz(Article article) {
        return aiService.generateFromArticleData(article, 2);
    }
    
    /**
     * Gets the current usage statistics.
     * 
     * @return Map containing usage data
     */
    public Map<String, Object> getUsage() {
        return aiService.getUsage();
    }
    
    /**
     * Gets the current model being used.
     * 
     * @return The current model name
     */
    public String getCurrentModel() {
        // Note: This requires adding a getter to GroqAIService
        // For now, we'll return a placeholder
        return "llama-3.3-70b-versatile";
    }
    
    /**
     * Checks if the service is ready to process requests.
     * 
     * @return true if the service is ready
     */
    public boolean isReady() {
        Map<String, Object> usage = getUsage();
        int requestsToday = (int) usage.get("requests_today");
        int dailyLimit = (int) usage.get("daily_limit");
        return requestsToday < dailyLimit;
    }
}