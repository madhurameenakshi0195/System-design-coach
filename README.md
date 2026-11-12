# System Design Coach (Spring Boot backend)
Java 17 + Spring Boot 3.3. The frontend lives in `src/main/resources/static` and is served by Spring itself.

## Run
Needs JDK 17+ and Maven (or open the folder in IntelliJ / VS Code and run `App`).

    export BHARATCODE_API_KEY=...        # Windows PowerShell: $env:BHARATCODE_API_KEY="..."
    export BHARATCODE_BASE_URL=https://YOUR-ENDPOINT/v1
    export BHARATCODE_MODEL=your-model
    mvn spring-boot:run                  # then open http://localhost:3000

Without the three variables it runs in demo mode.

## Code map (com.sdcoach)
- `App`    Spring Boot entry point
- `Api`    REST controller: /api/state, start, answer, next, estimate, finish, drill (stage state machine, estimation tool, mastery, next-problem picker)
- `Llm`    interviewer and evaluator prompts, BharatCode call via RestClient (OpenAI-style /chat/completions; edit `call()` if yours differs), demo fallback
- `Data`   stages, problems, concept knowledge base, drill scenarios
- `Store`  progress saved to data/db.json (swap for Spring Data JPA + PostgreSQL later)

## How to use
Pick a problem, answer the five stages, use the Calculator and Diagram tabs, press Finish and score.
