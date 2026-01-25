package com.mine.quizgen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class Quiz {
    @JsonProperty("quiz_title")
    private String quizTitle;
    
    @JsonProperty("article_id")
    private String articleId;
    
    @JsonProperty("generated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant generatedAt;
    
    // These fields come from API response
    @JsonProperty("model_used")
    private String modelUsed;
    
    @JsonProperty("token_estimate")
    private Integer tokenEstimate;
    
    @JsonProperty("questions")
    private List<Question> questions;
    
    // Additional fields for our Java logic (not from API)
    private String apiProvider = "Groq";
    private boolean success = true;
    private String errorMessage;
    
    // Helper method
    public int getQuestionCount() {
        return questions != null ? questions.size() : 0;
    }
}