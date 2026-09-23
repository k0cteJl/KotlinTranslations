package io.github.k0ctejl.translations.benchmarks;

import io.github.k0ctejl.translations.LangFileParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Measures how quickly a whole {@code .lang} file is parsed and compiled - a cost paid once at
 * plugin startup (or on a manual reload), not on the chat/command hot path.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class LangFileParserBenchmark {

    private static final int LINE_COUNT = 200;

    private byte[] content;

    @Setup(Level.Trial)
    public void setup() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LINE_COUNT; i++) {
            sb.append("key_").append(i)
                    .append("=\"Value number {0} with some filler text and a second {1} placeholder.\"\n");
        }
        content = sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Benchmark
    public Map<String, String> parseTwoHundredLines() {
        return LangFileParser.parse(new ByteArrayInputStream(content));
    }
}
