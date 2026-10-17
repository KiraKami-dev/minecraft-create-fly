package dev.mcf.e2e.gametest;

import dev.mcf.e2e.E2E;
import net.minecraft.gametest.framework.GameTestException;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.gametest.framework.UnknownGameTestException;
import org.jetbrains.annotations.Nullable;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.lang.reflect.Field;

/**
 * Test reporter of the GameTest run: logs one {@code E2E: GAMETEST <result> <test id>} line per finished test (what
 * {@code tools/e2e.sh gametest} summarises), the stack trace of tests that died on an unexpected exception, and a
 * final {@code E2E: GAMETEST SUMMARY} line; it also writes a JUnit XML report.
 *
 * <p>Vanilla's GameTest server logs only failures, and only their message.
 */
public final class E2ETestReporter implements TestReporter {
    private final @Nullable TestReporter junit;
    private int passed;
    private int failed;
    private int failedOptional;

    private E2ETestReporter(@Nullable final TestReporter junit) {
        this.junit = junit;
    }

    /**
     * Replaces the global test reporter. Call before the server starts its tests.
     *
     * @param reportFile the JUnit XML report to write, relative to the run directory, or null for none
     */
    public static void install(@Nullable final String reportFile) {
        TestReporter junit = null;
        if (reportFile != null && !reportFile.isBlank()) {
            final File file = new File(reportFile).getAbsoluteFile();
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
            try {
                junit = new JUnitLikeTestReporter(file);
            } catch (final ParserConfigurationException e) {
                E2E.LOG.error("E2E: cannot write the GameTest report {}", file, e);
            }
        }

        GlobalTestReporter.replaceWith(new E2ETestReporter(junit));
    }

    @Override
    public void onTestSuccess(final GameTestInfo info) {
        this.passed++;
        E2E.LOG.info("E2E: GAMETEST PASS {} ({} ms)", info.id(), info.getRunTime());
        if (this.junit != null) {
            this.junit.onTestSuccess(info);
        }
    }

    @Override
    public void onTestFailed(final GameTestInfo info) {
        if (info.isRequired()) {
            this.failed++;
        } else {
            this.failedOptional++;
        }

        final GameTestException error = info.getError();
        final String message = error == null ? "no error recorded" : error.getDescription().getString().replace('\n', ' ');
        E2E.LOG.info("E2E: GAMETEST {} {}: {}", info.isRequired() ? "FAIL" : "FAIL(optional)", info.id(), message);

        final Throwable reason = reason(error);
        if (reason != null) {
            E2E.LOG.error("E2E: GAMETEST {} threw", info.id(), reason);
        }

        if (this.junit != null) {
            this.junit.onTestFailed(info);
        }
    }

    @Override
    public void finish() {
        E2E.LOG.info("E2E: GAMETEST SUMMARY {} passed, {} failed, {} optional failed", this.passed, this.failed, this.failedOptional);
        if (this.junit != null) {
            this.junit.finish();
        }
    }

    /**
     * @return the exception an {@link UnknownGameTestException} wraps (test code threw something other than an
     * assertion), or null
     */
    private static @Nullable Throwable reason(@Nullable final GameTestException error) {
        if (!(error instanceof UnknownGameTestException)) {
            return null;
        }

        try {
            final Field field = UnknownGameTestException.class.getDeclaredField("reason");
            field.setAccessible(true);
            return (Throwable) field.get(error);
        } catch (final ReflectiveOperationException | RuntimeException e) {
            return error;
        }
    }
}
