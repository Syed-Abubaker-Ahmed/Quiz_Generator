package com.mine.quizgen.service;

import com.mine.quizgen.model.Article;
import com.mine.quizgen.model.Quiz;
import com.mine.quizgen.model.Question;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizOrchestrator {
    
    private final String groqApiKey;
    private final String inputJsonPath;
    private final String cleanedFilePath;
    private final String outputQuizPath;
    
    public QuizOrchestrator(String groqApiKey, String inputJsonPath, 
                           String cleanedFilePath, String outputQuizPath) {
        this.groqApiKey = groqApiKey;
        this.inputJsonPath = inputJsonPath;
        this.cleanedFilePath = cleanedFilePath;
        this.outputQuizPath = outputQuizPath;
    }
    
    public QuizOrchestrator(String groqApiKey) {
        this(groqApiKey, "./inputs/article.json", 
             "./cleaned/article_cleaned.txt", "./outputs");
    }
    
    private void cleanupTempFiles() {
        File cleanedFile = new File(cleanedFilePath);
        if (cleanedFile.exists()) {
            try {
                if (cleanedFile.delete()) {
                    System.out.println("🧹 Cleaned up temporary file: " + cleanedFilePath);
                } else {
                    System.out.println("⚠️ Could not remove " + cleanedFilePath);
                }
            } catch (SecurityException e) {
                System.out.println("⚠️ Security exception removing " + cleanedFilePath);
            }
        }
    }
    
    private void clearPyCache() {
        File pycacheDir = new File("__pycache__");
        if (pycacheDir.exists() && pycacheDir.isDirectory()) {
            try {
                deleteDirectory(pycacheDir);
                System.out.println("🧹 Cleared __pycache__");
            } catch (IOException e) {
                System.out.println("⚠️ Could not clear __pycache__: " + e.getMessage());
            }
        }
    }
    
    private void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] entries = dir.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("Failed to delete " + dir);
        }
    }
    
    public boolean runFullPipeline() {
        clearPyCache();
        
        System.out.println("=".repeat(60));
        System.out.println("QUIZ GENERATOR - CLEAN PIPELINE (GROQ - JAVA)");
        System.out.println("=".repeat(60));
        
        // 1. Initialize services
        ArticleCleaner articleCleaner = new ArticleCleaner();
        GroqAIService aiService = new GroqAIService(groqApiKey);
        
        System.out.println("✅ Generator initialized");
        System.out.println("   Provider: Groq");
        
        // 2. Check if input exists
        File inputFile = new File(inputJsonPath);
        if (!inputFile.exists()) {
            System.out.println("❌ Input file not found: " + inputJsonPath);
            System.out.println("   Please save your article JSON to: " + inputFile.getAbsolutePath());
            return false;
        }
        
        // 3. Process JSON -> Cleaned Text
        System.out.println("\n📥 Loading article from: " + inputJsonPath);
        Article article;
        try {
            article = articleCleaner.processAndSaveJson(inputJsonPath, cleanedFilePath);
        } catch (IOException e) {
            System.out.println("❌ Failed to process article: " + e.getMessage());
            return false;
        }
        
        if (article.getId() == null || article.getId().equals("unknown")) {
            System.out.println("❌ Failed to process article. Exiting.");
            return false;
        }
        
        // 4. Generate Quiz
        System.out.println("\n🎯 Generating 20-question quiz...");
        System.out.println("   Using ~" + String.format("%,d", article.getTokenEstimate()) + " estimated tokens");
        
        Quiz quiz = aiService.generateFromArticleData(article, 2);
        
        // 5. Handle Results
        if (!quiz.isSuccess()) {
            System.out.println("❌ Quiz generation failed: " + quiz.getErrorMessage());
            return false;
        }
        
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            System.out.println("❌ No questions generated");
            return false;
        }
        
        System.out.println("✅ Success! Generated " + quiz.getQuestions().size() + " questions");
        
        // 6. Show sample
        if (!quiz.getQuestions().isEmpty()) {
            Question firstQuestion = quiz.getQuestions().get(0);
            System.out.println("\n📝 Sample question:");
            String questionText = firstQuestion.getQuestion();
            int previewLength = Math.min(100, questionText.length());
            System.out.println("   Q: " + questionText.substring(0, previewLength) + 
                             (questionText.length() > previewLength ? "..." : ""));
            System.out.println("   Difficulty: " + firstQuestion.getDifficulty());
            System.out.println("   Correct: " + firstQuestion.getCorrectAnswer());
        }
        
        // 7. Save Quiz
        try {
            String savedPath = aiService.saveQuiz(quiz, outputQuizPath);
            System.out.println("\n💾 Quiz saved to: " + savedPath);
        } catch (IOException e) {
            System.out.println("❌ Failed to save quiz: " + e.getMessage());
            return false;
        }
        
        // 8. Show Statistics
        if (!quiz.getQuestions().isEmpty()) {
            Map<String, Long> difficultyCounts = quiz.getQuestions().stream()
                .collect(Collectors.groupingBy(Question::getDifficulty, Collectors.counting()));
            
            System.out.println("\n📊 Difficulty breakdown:");
            for (String diff : new String[]{"Easy", "Medium", "Hard"}) {
                long count = difficultyCounts.getOrDefault(diff, 0L);
                System.out.println("   " + diff + ": " + count);
            }
            
            if (quiz.getQuestions().size() != 20) {
                System.out.println("⚠️ Warning: Expected 20 questions, got " + quiz.getQuestions().size());
            }
        }
        
        // 9. Cleanup
        cleanupTempFiles();
        
        // 10. Final Report
        Map<String, Object> usage = aiService.getUsage();
        System.out.println("\n📈 API usage today: " + usage.get("requests_today") + 
                         "/" + usage.get("daily_limit"));
        System.out.println("   Remaining calls: " + usage.get("remaining"));
        System.out.println("   Provider: " + usage.get("provider"));
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PROCESS COMPLETE");
        System.out.println("Input:  " + String.format("%,d", article.getOriginalLength()) + " chars");
        System.out.println("Cleaned: " + String.format("%,d", article.getCleanedLength()) + " chars");
        System.out.println("=".repeat(60));
        
        return true;
    }
}