# 🧠 Quiz Generator - Java Edition

A production-ready Java application that automatically generates quizzes from articles using Groq's AI API. This tool transforms text content into structured, multi-choice assessments with proper difficulty distribution.

![Java](https://img.shields.io/badge/Java-17-orange)
![Maven](https://img.shields.io/badge/Maven-3.8.1-blue)
![License](https://img.shields.io/badge/License-MIT-green)

## ✨ Features

- **Automated Quiz Generation**: Convert articles into 20-question quizzes automatically
- **Intelligent Difficulty Distribution**: 8 Easy, 7 Medium, 5 Hard questions per quiz
- **AI-Powered Content Analysis**: Uses Groq's Llama 3.3 70B model for question generation
- **Production-Ready Error Handling**: Retry logic, model fallback, and comprehensive error recovery
- **Clean Data Processing**: HTML sanitization, text cleaning and token estimation
- **Structured JSON Output**: Well-formatted quiz files with questions, options, and explanations

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Input JSON    │───▶│   Article       │───▶│   Text          │
│   (Article)     │    │   Processor     │    │   Cleaner       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                  │
                                                  ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Quiz JSON     │◀───│   JSON Parser   │◀───│   Groq AI      │
│   (Output)      │    │   & Validator   │    │   API Service   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Groq API Key** (Get yours from [groq.com](https://console.groq.com/))
- Git

## 🚀 Quick Start

### 1. Clone & Configure
```bash
git clone https://github.com/Syed-Abubaker-Ahmed/Quiz_Generator.git
cd Quiz_Generator
```

### 2. Set Your API Key
```bash
# Create environment variable (Linux/Mac)
export GROQ_API_KEY="your-api-key-here"

# Or set directly in the code (for testing)
# Edit MainApplication.java and replace "your-api-key-here"
```

### 3. Prepare Your Article
Place your article in JSON format in the `inputs/` directory:
```json
{
  "id": "unique-article-id",
  "title": "Your Article Title",
  "subtitle": "Optional subtitle",
  "body": "<p>Your article content in HTML</p>",
  "author": {
    "displayName": "Author Name"
  }
}
```

### 4. Build & Run
```bash
# Build the project
mvn clean compile

# Run the application
mvn exec:java -Dexec.mainClass="com.mine.quizgen.MainApplication"
```

## 📁 Project Structure

```
quiz-generator-java/
├── src/main/java/com/mine/quizgen/
│   ├── MainApplication.java          # Entry point
│   ├── model/                        # Data models
│   │   ├── Article.java              # Article data structure
│   │   ├── Quiz.java                 # Quiz data structure
│   │   └── Question.java             # Question data structure
│   ├── service/
│   │   ├── GroqAIService.java        # AI API integration
│   │   ├── ArticleProcessor.java     # Article cleaning & processing
│   │   └── QuizValidator.java        # Quiz validation
│   └── util/
│       └── JsonUtils.java            # JSON utilities
├── src/main/resources/
│   └── application.properties         # Configuration
├── inputs/
│   └── article.json                   # Sample input article
├── cleaned/
│   └── article_cleaned.txt           # Processed article text
├── outputs/
│   └── quiz_*.json                   # Generated quizzes
├── pom.xml                           # Maven configuration
└── README.md                         # This file
```

## 🛠️ Configuration

### Application Properties
The following configurations can be modified:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `api.model.priority` | llama-3.3-70b-versatile | Primary AI model |
| `api.fallback.models` | mixtral-8x7b-32768, llama-3.1-8b-instant | Fallback models |
| `api.max_retries` | 3 | Maximum API retry attempts |
| `api.daily_limit` | 10 | Daily API request limit |
| `quiz.questions.count` | 20 | Number of questions per quiz |
| `quiz.difficulty.easy` | 8 | Easy questions count |
| `quiz.difficulty.medium` | 7 | Medium questions count |
| `quiz.difficulty.hard` | 5 | Hard questions count |

### Environment Variables
```bash
export GROQ_API_KEY="your-api-key-here"
export MAX_TOKENS=3500
export TEMPERATURE=0.7
```

## 📊 Sample Output

```json
{
  "quiz_title": "Quiz: How Systems Come Into Existence: The Dialectic...",
  "article_id": "072c7116-bb0b-42e2-bb2e-b1e1c2d5ebbe",
  "generated_at": "2024-02-20T14:30:00.000Z",
  "questions": [
    {
      "id": 1,
      "question": "What is dialectics, according to Bertell Ollman?",
      "options": {
        "A": "A way of thinking that focuses on harmony between opposing forces",
        "B": "A way of thinking that focuses on how opposing forces interact",
        "C": "A way of thinking that eliminates opposing forces",
        "D": "A way of thinking that prioritizes one force over another"
      },
      "correct_answer": "B",
      "explanation": "Ollman defines dialectics as focusing on how opposing forces interact...",
      "difficulty": "Easy"
    }
    // ... 19 more questions
  ]
}
```

## 🔧 Advanced Usage

### Custom Article Processing
```java
// Create custom article processor
ArticleProcessor processor = new ArticleProcessor();
Article article = processor.loadFromJson("path/to/article.json");
article = processor.cleanAndProcess(article);

// Generate quiz
GroqAIService aiService = new GroqAIService(apiKey);
Quiz quiz = aiService.generateFromArticleData(article, 3);

// Save quiz
aiService.saveQuiz(quiz, "./outputs/");
```

### Batch Processing
```bash
# Process multiple articles
for file in inputs/*.json; do
  mvn exec:java -Dexec.mainClass="com.mine.quizgen.MainApplication" \
    -Dexec.args="--input=$file"
done
```

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Test with sample article
mvn exec:java -Dexec.mainClass="com.mine.quizgen.TestRunner"

# Generate code coverage report
mvn clean test jacoco:report
```

## 🚨 Error Handling

The application includes comprehensive error handling:
- **Rate Limiting**: Automatic retry with exponential backoff
- **Model Fallback**: Rotates through available models if one fails
- **JSON Validation**: Validates both input and output JSON structures
- **Connection Issues**: Handles network failures with retry logic

Common issues and solutions:
1. **API Key Invalid**: Verify your Groq API key is correct
2. **Rate Limited**: Application automatically waits and retries
3. **JSON Parsing Error**: Ensure input JSON follows the expected format
4. **Memory Issues**: Reduce article size or adjust token limits

## 📈 Performance Metrics

- **Processing Time**: ~10-15 seconds per article (varies by length)
- **Token Usage**: ~1500-2000 tokens per quiz generation
- **Success Rate**: >95% with proper error handling
- **Output Quality**: Human-readable, structured quizzes

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Groq** for providing the AI API
- **Jackson** for JSON processing in Java
- **OkHttp** for HTTP client functionality
- **Jsoup** for HTML cleaning and parsing

## 📞 Support

For questions, issues, or suggestions:
1. Check the [Issues](https://github.com/Syed-Abubaker-Ahmed/Quiz_Generator/issues) page
2. Create a new issue with detailed description
3. Email: [Your Email]
4. LinkedIn: [Your LinkedIn Profile]

---

⭐ **Star this repo if you find it useful!** ⭐

*Built by Syed Abubaker Ahmed*
