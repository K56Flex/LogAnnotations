package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static wuxian.me.logannotations.compiler.JavaFileHelper.readClassInfo;
import static wuxian.me.logannotations.compiler.JavaFileHelper.getLongClassName;
import static wuxian.me.logannotations.compiler.JavaFileHelper.getLongSuperClass;

/**
 * Created by wuxian on 22/11/2016.
 * A tool used to get class inheritance information.
 * we can't get a @Element's super class,so we have to read java file to deal it.
 */

public class ClassInheritanceHelper {
    private static final String DOT = ".";
    private static ClassInheritanceHelper helper;

    public static ClassInheritanceHelper getInstance(@NonNull Messager messager
            , @NonNull Elements elements) throws ProcessingException {
        if (helper == null) {
            helper = new ClassInheritanceHelper(messager, elements);
        }
        return helper;
    }

    private Elements elements;

    private Messager messager;

    private boolean dumpOnce = false;

    //key:class value:super-class
    private Map<String, String> superClassMap = new HashMap<>();

    private ClassInheritanceHelper(@NonNull Messager messager, @NonNull Elements elements) throws ProcessingException {
        this.elements = elements;
        this.messager = messager;

        AndroidDirHelper.initMessager(messager);
    }

    private List<File> paths = new ArrayList<>();

    /**
     * 读取java root下的所有文件 记录继承关系到superClassMap中 --> ???效率?
     */
    public void dumpAllClassesOnce(File javaRoot) throws ProcessingException {
        if (dumpOnce) {
            return;
        }
        dumpOnce = true;
        if (!javaRoot.isDirectory()) {
            throw new ProcessingException(null, "java root is not directory!");
        }

        paths.clear();
        paths.add(javaRoot);

        int current = 0;
        while (current < paths.size()) {
            recursiveDealFile(paths.get(current));
            current++;
        }

        return;
    }

    private boolean checkDir(@NonNull File dir) {
        if (!dir.isDirectory() || !dir.canRead()) {
            return false;
        }
        return true;
    }

    private boolean checkJavaFile(@NonNull File file) {
        if (file.isDirectory() || !file.canRead() || file.length() == 0) {
            return false;
        }

        return file.getName().endsWith(".java");
    }

    private void recursiveDealFile(@NonNull File dir) {
        //LogAnnotationsProcessor.info(messager,null,String.format("recursive dir: %s",dir.getAbsolutePath()));
        if (!checkDir(dir)) {
            return;
        }

        File[] files;
        files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory() && checkDir(file)) {
                paths.add(file);
                continue;
            }

            if (checkJavaFile(file)) {
                getClassHeritance(file);
            }
        }
    }

    /**
     * 读取java文件 拿到继承关系
     */
    private void getClassHeritance(File file) {
        //LogAnnotationsProcessor.info(messager,null,String.format("get heritance file: %s",file.getAbsolutePath()));

        String classInfo = readClassInfo(file);
        if (null == classInfo) {
            return;
        }

        String wholeClass = getLongClassName(classInfo);  //xxx.xxx.xxx.class
        //LogAnnotationsProcessor.info(messager,null,String.format("get class: %s",wholeClass));
        String wholeSuperClass = getLongSuperClass(classInfo); //xxx.xxx.xxx.superclass
        //LogAnnotationsProcessor.info(messager,null,String.format("get super class: %s",wholeSuperClass));

        //LogAnnotationsProcessor.info(messager,null,String.format("class:%s superclass:%s",wholeClass,wholeSuperClass));

        if (wholeClass == null || wholeSuperClass == null) {
            return;
        }

        superClassMap.put(wholeClass, wholeSuperClass);
    }

    /**
     * 只返回上一层的sub-class A-->B-->C A不是C的sub-class
     * 1 先加入superClassMap里的类
     * 2 遍历java root下的所有file 跳过已经存入到list中的对应路径class
     */
    public List<String> getSubClasses(@NonNull String classNameString) {
        List<String> subClasses = new ArrayList<>();

        Set<String> set = superClassMap.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            if (superClassMap.get(iterator.next()).equals(classNameString)) {
                subClasses.add(iterator.next());
            }
        }

        return subClasses;
    }

    /**
     * currently NOT SUPPORT inner class,anonymous class... and currently only support one application module,not support library
     * this function may return null,because
     * 1 not support class listed above
     * 2 the class has no super class
     *
     * @param classNameString your.package.name.classname
     */
    @Nullable
    public String getSuperClass(@NonNull String classNameString) {
        //check if is an class element
        TypeElement classElement = elements.getTypeElement(classNameString);
        if (null == classElement) {
            //throw new ProcessingException(null, String.format("can't find class for name %s", classNameString));
            return null;
        }

        return superClassMap.get(classNameString);
    }
}
