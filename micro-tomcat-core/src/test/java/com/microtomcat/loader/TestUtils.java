package com.microtomcat.loader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class TestUtils {
    
    public static void compileJavaFile(File sourceFile, String classpath) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No Java compiler found. Make sure you're running with JDK");
        }
        
        int result = compiler.run(null, null, null,
            "-cp", classpath,
            sourceFile.getPath());
            
        if (result != 0) {
            throw new IOException("Compilation failed for " + sourceFile);
        }
    }
    
    public static void createAndCompileJavaFile(
            File classesDir, 
            String className, 
            String sourceCode,
            String classpath) throws IOException {
        File sourceFile = new File(classesDir, className + ".java");
        Files.write(sourceFile.toPath(), sourceCode.getBytes());
        compileJavaFile(sourceFile, classpath);
    }
} 