package com.mine.quizgen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
public class Question {
    @JsonProperty("id")
    private Integer id;
    
    @JsonProperty("question")
    private String question;
    
    @JsonProperty("options")
    private Map<String, String> options; // Keys: "A", "B", "C", "D"
    
    @JsonProperty("correct_answer")
    private String correctAnswer;
    
    @JsonProperty("explanation")
    private String explanation;
    
    @JsonProperty("difficulty")
    private String difficulty; // "Easy", "Medium", "Hard"
    
    // Helper methods
    public String getOption(String letter) {
        return options != null ? options.get(letter.toUpperCase()) : null;
    }
    
    public boolean isCorrect(String answer) {
        return correctAnswer != null && correctAnswer.equalsIgnoreCase(answer);
    }
}