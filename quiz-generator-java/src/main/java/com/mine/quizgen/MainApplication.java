package com.mine.quizgen;

import com.mine.quizgen.service.QuizOrchestrator;
import java.io.InputStream;
import java.util.Properties;

public class MainApplication {
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("QUIZ GENERATOR - JAVA PORT (MINE)");
        System.out.println("=".repeat(60));
        
        try {
            // 1. Load API key from application.properties
            Properties props = new Properties();
            try (InputStream input = MainApplication.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (input == null) {
                    System.err.println("❌ ERROR: application.properties file not found!");
                    System.exit(1);
                }
                props.load(input);
            }
            
            String groqApiKey = props.getProperty("groq.api.key");
            
            if (groqApiKey == null || groqApiKey.isEmpty() || groqApiKey.equals("your_groq_api_key_here")) {
                System.err.println("❌ ERROR: Please set your Groq API key in src/main/resources/application.properties");
                System.err.println("   Add: groq.api.key=your_actual_key_here");
                System.exit(1);
            }
            
            // 2. Initialize and run the orchestrator
            QuizOrchestrator orchestrator = new QuizOrchestrator(groqApiKey);
            boolean success = orchestrator.runFullPipeline();
            
            if (success) {
                System.out.println("\n✅ PROCESS COMPLETE SUCCESSFULLY");
                System.exit(0);
            } else {
                System.out.println("\n❌ PROCESS FAILED");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("\n💥 FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}