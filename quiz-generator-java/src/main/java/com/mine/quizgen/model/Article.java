package com.mine.quizgen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // Added to ignore unknown fields like "section" and others
public class Article {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("postNumber")  // Added to handle the postNumber field from JSON
    private Integer postNumber;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("subtitle")
    private String subtitle;
    
    @JsonProperty("body")  // This is the HTML content
    private String htmlBody;
    
    @JsonProperty("author")
    private Author author;
    
    // Calculated fields (will be set during processing)
    private String cleanText;
    private String fullText;
    private Integer originalLength;
    private Integer cleanedLength;
    private Integer tokenEstimate;
    
    // Nested class for author data
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        @JsonProperty("displayName")
        private String displayName;
        
        // You might want to add other author fields if present in JSON
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("username")
        private String username;
        
        // Default constructor for Jackson
        public Author() {}
    }
    
    // Helper method to get author display name safely
    public String getAuthorDisplayName() {
        return author != null ? author.getDisplayName() : null;
    }
    
    // Helper method to get a display title with subtitle
    public String getFullTitle() {
        if (subtitle != null && !subtitle.isEmpty()) {
            return title + ": " + subtitle;
        }
        return title;
    }
}