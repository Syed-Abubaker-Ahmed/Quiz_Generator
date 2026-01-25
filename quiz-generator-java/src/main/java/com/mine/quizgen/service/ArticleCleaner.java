package com.mine.quizgen.service;

import com.mine.quizgen.model.Article;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class ArticleCleaner {
    
    private static final int MAX_LENGTH = 6000;
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[\\d+\\]");
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Article cleanHtmlContent(String htmlContent) {
        Article article = new Article();
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return article;
        }
        
        try {
            // Parse HTML with Jsoup
            Document doc = Jsoup.parse(htmlContent);
            
            // Remove non-content elements
            doc.select("script, style, nav, footer, header, aside, iframe, button, form, input, img, a")
               .forEach(org.jsoup.nodes.Element::remove);
            
            // Get clean text
            String text = Jsoup.clean(doc.body().html(), Safelist.none());
            String[] lines = text.split("\\r?\\n");
            
            StringBuilder cleanTextBuilder = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (line.length() < 10) continue;
                
                line = MULTI_SPACE.matcher(line).replaceAll(" ");
                line = CITATION_PATTERN.matcher(line).replaceAll("");
                // HTML unescape is handled by Jsoup
                
                cleanTextBuilder.append(line).append(" ");
            }
            
            String cleanText = cleanTextBuilder.toString().trim();
            cleanText = MULTI_SPACE.matcher(cleanText).replaceAll(" ");
            
            // Smart truncation
            if (cleanText.length() > MAX_LENGTH) {
                String truncated = cleanText.substring(0, MAX_LENGTH);
                int lastPeriod = truncated.lastIndexOf(". ");
                if (lastPeriod > MAX_LENGTH * 0.7) {
                    cleanText = truncated.substring(0, lastPeriod + 1) + " [truncated]";
                } else {
                    cleanText = truncated + " [truncated]";
                }
            }
            
            article.setCleanText(cleanText);
            article.setOriginalLength(htmlContent.length());
            article.setCleanedLength(cleanText.length());
            
        } catch (Exception e) {
            System.err.println("❌ Error cleaning HTML: " + e.getMessage());
        }
        
        return article;
    }
    
    public Article extractFromJson(String jsonData) {
        try {
            Article article = objectMapper.readValue(jsonData, Article.class);
            
            // Clean the HTML body
            Article cleanedArticle = cleanHtmlContent(article.getHtmlBody());
            
            // Prepare text for API
            StringBuilder fullTextBuilder = new StringBuilder(article.getTitle());
            if (article.getSubtitle() != null && !article.getSubtitle().isEmpty()) {
                fullTextBuilder.append("\n\n").append(article.getSubtitle());
            }
            fullTextBuilder.append("\n\n").append(cleanedArticle.getCleanText());
            
            String fullText = fullTextBuilder.toString();
            int tokenEstimate = fullText.length() / 4;
            
            // Transfer all data to the article object
            article.setCleanText(cleanedArticle.getCleanText());
            article.setFullText(fullText);
            article.setTokenEstimate(tokenEstimate);
            article.setOriginalLength(cleanedArticle.getOriginalLength());
            article.setCleanedLength(cleanedArticle.getCleanedLength());
            
            return article;
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting article from JSON: " + e.getMessage());
            return new Article();
        }
    }
    
    public Article processAndSaveJson(String inputPath, String outputPath) throws IOException {
        System.out.println("📥 Processing: " + inputPath);
        
        // Load JSON file
        String jsonContent = new String(Files.readAllBytes(Paths.get(inputPath)));
        Article article = extractFromJson(jsonContent);
        
        if (article.getId() == null || article.getId().equals("unknown")) {
            System.out.println("❌ No article data extracted");
            return new Article();
        }
        
        // Save cleaned text
        File outputDir = new File("cleaned");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(article.getFullText());
        }
        
        System.out.println("✅ Article cleaned and saved to: " + outputPath);
        System.out.println("   Title: " + article.getTitle());
        System.out.println("   Original: " + String.format("%,d", article.getOriginalLength()) + " chars");
        System.out.println("   Cleaned: " + String.format("%,d", article.getCleanedLength()) + " chars");
        System.out.println("   Tokens: ~" + String.format("%,d", article.getTokenEstimate()));
        
        return article;
    }
}