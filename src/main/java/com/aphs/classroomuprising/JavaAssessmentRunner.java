package com.aphs.classroomuprising;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Opt-in local Java execution and mechanical rubric evidence, not an automatic course grader.
 * Child JVMs have wall-time/output limits but are NOT an OS/filesystem/network security sandbox.
 * Use only trusted local submissions until a separately isolated execution environment is selected.
 */
public final class JavaAssessmentRunner {
    public record Limits(Duration compileTime, Duration testTime, int maxOutputBytes) {
        public Limits {
            if(compileTime==null || testTime==null || compileTime.toMillis()<1 || testTime.toMillis()<1 || maxOutputBytes<1)
                throw new IllegalArgumentException("Positive process limits required.");
        }
    }
    public record TestCase(String id, String input, String expectedOutput) {
        public TestCase { if(id==null||id.isBlank()||input==null||expectedOutput==null) throw new IllegalArgumentException("Invalid test case."); }
    }
    public enum Status { PASSED, WRONG_OUTPUT, COMPILE_ERROR, RUNTIME_ERROR, TIMEOUT, OUTPUT_LIMIT }
    public record ProcessEvidence(int exitCode, boolean timedOut, boolean outputLimit, String stdout, String stderr) {}
    public record TestResult(String id, Status status, ProcessEvidence process) {}
    public record Report(ProcessEvidence compilation, List<TestResult> tests, List<StyleRule.Finding> style) {
        public Report { tests=List.copyOf(tests);style=List.copyOf(style); }
    }
    private final Path jdk;
    private final Limits limits;
    public JavaAssessmentRunner(Path jdkHome, Limits limits) {
        this.jdk=Objects.requireNonNull(jdkHome);this.limits=Objects.requireNonNull(limits);
    }
    public Report evaluate(String className, String source, List<TestCase> tests, List<StyleRule> rubric)
            throws IOException, InterruptedException {
        if(className==null || !className.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*"))
            throw new IllegalArgumentException("Use a fully qualified Java class name, not a path.");
        Objects.requireNonNull(source);tests=List.copyOf(tests);rubric=List.copyOf(rubric);
        if(tests.isEmpty() || tests.stream().map(TestCase::id).distinct().count()!=tests.size()
                || rubric.stream().map(StyleRule::id).distinct().count()!=rubric.size())
            throw new IllegalArgumentException("Tests must be nonempty; test/rubric IDs must be distinct.");
        var findings=rubric.stream().map(rule->rule.inspect(source)).toList();
        Path work=Files.createTempDirectory("classroom-java-assessment-");
        try {
            Path file=work.resolve(className.replace('.','/')+".java");
            Files.createDirectories(file.getParent());Files.writeString(file,source,StandardCharsets.UTF_8);
            Path classes=Files.createDirectories(work.resolve("classes"));
            var compile=run(List.of(jdk.resolve("bin/javac").toString(),"-encoding","UTF-8","-proc:none","-d",classes.toString(),file.toString()),
                    "",work,limits.compileTime());
            var results=new ArrayList<TestResult>();
            if(compile.exitCode()!=0 || compile.timedOut() || compile.outputLimit()) {
                Status status=compile.timedOut()?Status.TIMEOUT:compile.outputLimit()?Status.OUTPUT_LIMIT:Status.COMPILE_ERROR;
                for(var test:tests) results.add(new TestResult(test.id(),status,compile));
                return new Report(compile,results,findings);
            }
            for(var test:tests) {
                var result=run(List.of(jdk.resolve("bin/java").toString(),"-cp",classes.toString(),className),
                        test.input(),work,limits.testTime());
                Status status=result.timedOut()?Status.TIMEOUT:result.outputLimit()?Status.OUTPUT_LIMIT:
                        result.exitCode()!=0?Status.RUNTIME_ERROR:result.stdout().equals(test.expectedOutput())?Status.PASSED:Status.WRONG_OUTPUT;
                results.add(new TestResult(test.id(),status,result));
            }
            return new Report(compile,results,findings);
        } finally {
            try(var paths=Files.walk(work)) {
                for(Path file:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }
    private ProcessEvidence run(List<String> command, String input, Path work, Duration timeout)
            throws IOException, InterruptedException {
        Path stdin=work.resolve("stdin.txt");Files.writeString(stdin,input,StandardCharsets.UTF_8);
        var builder=new ProcessBuilder(command).directory(work.toFile()).redirectInput(stdin.toFile());
        for(String key:List.of("CLASSPATH","JAVA_TOOL_OPTIONS","JDK_JAVA_OPTIONS","_JAVA_OPTIONS")) builder.environment().remove(key);
        Process process=builder.start();
        var used=new AtomicInteger();var exceeded=new AtomicBoolean();
        var out=new Collector(process.getInputStream(),process,used,exceeded);
        var err=new Collector(process.getErrorStream(),process,used,exceeded);
        Thread a=new Thread(out,"lab-stdout"),b=new Thread(err,"lab-stderr");
        a.setDaemon(true);b.setDaemon(true);a.start();b.start();
        boolean timeoutHit;
        try {
            timeoutHit=!process.waitFor(timeout.toMillis(),java.util.concurrent.TimeUnit.MILLISECONDS);
            if(timeoutHit) stop(process);
            process.waitFor(2,java.util.concurrent.TimeUnit.SECONDS);
            a.join(2000);b.join(2000);
            if(a.isAlive() || b.isAlive()) throw new IOException("Submission left output streams open.");
            if(out.failure.get()!=null || err.failure.get()!=null) {
                if(!timeoutHit && !exceeded.get()) throw new IOException("Failed reading child output.",out.failure.get()!=null?out.failure.get():err.failure.get());
            }
            return new ProcessEvidence(process.isAlive()?-1:process.exitValue(),timeoutHit,exceeded.get(),
                    out.output.toString(StandardCharsets.UTF_8),err.output.toString(StandardCharsets.UTF_8));
        } finally {
            try { stop(process); }
            finally { process.getInputStream().close();process.getErrorStream().close(); }
        }
    }
    private static void stop(Process p) {
        if (!p.isAlive()) return;
        try { p.descendants().forEach(ProcessHandle::destroyForcibly); }
        finally { if (p.isAlive()) p.destroyForcibly(); }
    }
    private final class Collector implements Runnable {
        private final InputStream input;private final Process process;
        private final AtomicInteger used;private final AtomicBoolean exceeded;
        private final ByteArrayOutputStream output=new ByteArrayOutputStream();
        private final AtomicReference<IOException> failure=new AtomicReference<>();
        Collector(InputStream input,Process process,AtomicInteger used,AtomicBoolean exceeded) {
            this.input=input;this.process=process;this.used=used;this.exceeded=exceeded;
        }
        public void run() {
            try {
                byte[] bytes=new byte[1024];int n;
                while((n=input.read(bytes))!=-1) {
                    int before=used.getAndAdd(n),keep=Math.min(n,Math.max(0,limits.maxOutputBytes()-before));
                    if(keep>0) output.write(bytes,0,keep);
                    if(before+n>limits.maxOutputBytes()) { exceeded.set(true);stop(process);break; }
                }
            } catch(IOException e) { failure.set(e); }
        }
    }
}
