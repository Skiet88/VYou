package za.ac.skiet.vyou2.screening;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedAnswerScreenerTest {

    private final AnswerScreener screener =
            new RuleBasedAnswerScreener(List.of(new MinimumLengthRule(5), new RepeatedQuestionRule()));

    @Test
    void passesReasonableAnswer() {
        assertThat(screener.screen("What is a JVM?",
                "The JVM runs Java bytecode and manages memory with garbage collection.").passed()).isTrue();
    }

    @Test
    void rejectsShortAnswer() {
        ScreeningResult r = screener.screen("What is a JVM?", "No idea");
        assertThat(r.passed()).isFalse();
        assertThat(r.reasons()).singleElement().asString().contains("too short");
    }

    @Test
    void rejectsEchoedQuestion() {
        String q = "Can you describe a time you resolved a conflict in your team?";
        assertThat(screener.screen(q, q.toUpperCase()).reasons()).contains("Answer repeats the question");
    }
}
