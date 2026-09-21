package io.github.k0ctejl.translations.benchmarks;

import io.github.k0ctejl.translations.Translator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Measures {@link Translator#translate} itself - i.e. the cost paid on every chat message or
 * command response once a language file is already loaded and compiled. No parsing or I/O
 * happens here, only a walk over the precompiled {@code MessageTemplate} segments.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class TranslateBenchmark {

    private Translator translator;

    @Setup(Level.Trial)
    public void setup() {
        translator = Translator.create("en").loadLanguage("en", Map.of(
                "plain", "Just a plain message with no placeholders at all.",
                "one", "Hello, {0}!",
                "many", "{0} sent {1} messages to {2} in #{3} at {4}."
        ));
    }

    @Benchmark
    public String plainLiteral() {
        return translator.translate("plain");
    }

    @Benchmark
    public String singlePlaceholder() {
        return translator.translate("one", "Bob");
    }

    @Benchmark
    public String manyPlaceholders() {
        return translator.translate("many", "Bob", 5, "Alice", "general", "10:00");
    }

    @Benchmark
    public String missingKey() {
        return translator.translate("nonexistent.key");
    }
}
