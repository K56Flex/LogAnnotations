package wuxian.me.logannotations.compiler.util;

import org.antlr.runtime.tree.TreeParser;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Messager;

import wuxian.me.logannotations.antlr.java.JavaLexer;
import wuxian.me.logannotations.antlr.java.JavaParser;
import wuxian.me.logannotations.compiler.LogAnnotationsProcessor;

/**
 * Created by wuxian on 30/11/2016.
 *
 * walking through a parser tree: http://stackoverflow.com/questions/15050137/once-grammar-is-complete-whats-the-best-way-to-walk-an-antlr-v4-tree
 */

public class AntlrJavaFileHelper implements IJavaHelper {

    private static Messager messager;

    public static void setMessager(@NonNull Messager msg) {
        messager = msg;
    }

    private static AntlrJavaFileHelper helper;

    private Map<File, ParserRuleContext> parsedContextMap = new HashMap<>();
    private Map<String, File> compatibleMap = new HashMap();

    public static AntlrJavaFileHelper getInstance() {
        if (helper == null) {
            helper = new AntlrJavaFileHelper();
        }
        return helper;
    }

    private AntlrJavaFileHelper() {
        ;
    }

    @Override
    public String getLongClassName(String classInfo) {

        LogAnnotationsProcessor.info(messager, null, String.format("getLongclassname"));
        File file = compatibleMap.get(classInfo);
        if (file == null) {
            return null;
        }

        ParserRuleContext context = parsedContextMap.get(file);
        if (context == null) {
            return null;
        }

        return null;
    }

    @Override
    public String getLongSuperClass(String classInfo) {
        return null;
    }

    @Override
    public boolean hasAllreadyImport(String className, String content) {
        return false;
    }

    @Override
    public String readClassInfo(@NonNull File file) {
        LogAnnotationsProcessor.info(messager, null, "read file: %s", file.getAbsolutePath());

        try {
            Lexer lexer = new JavaLexer(new ANTLRFileStream(file.getAbsolutePath()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);
            parser.addErrorListener(new DiagnosticErrorListener());
            parser.setErrorHandler(new BailErrorStrategy());
            parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
            parser.setBuildParseTree(true);

            ParserRuleContext t = parser.compilationUnit();  //start parsing
            //LogAnnotationsProcessor.info(messager, null, t.toStringTree());

            for (ParseTree p : t.children) {
                ParseTreeWalker.DEFAULT.walk(new ClassListener(), p);
            }
            //

            String text = t.getText();

            parsedContextMap.put(file, t);
            compatibleMap.put(text, file);

            return text;

        } catch (Exception e) {
            ;
        }

        return null;
    }
}
