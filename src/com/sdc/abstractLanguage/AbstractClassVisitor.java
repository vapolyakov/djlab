package com.sdc.abstractLanguage;

import com.sdc.util.DeclarationWorker;
import org.objectweb.asm.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM4;

public abstract class AbstractClassVisitor extends ClassVisitor {
    protected AbstractClass myDecompiledClass;
    protected final int myTextWidth;
    protected final int myNestSize;

    protected boolean myIsLambdaFunction = false;

    protected AbstractLanguagePartFactory myLanguagePartFactory;
    protected AbstractVisitorFactory myVisitorFactory;

    protected DeclarationWorker.SupportedLanguage myLanguage;

    public AbstractClassVisitor(final int textWidth, final int nestSize) {
        super(ASM4);
        this.myTextWidth = textWidth;
        this.myNestSize = nestSize;
    }

    protected abstract String getDefaultImplementedInterface();
    protected abstract String getDefaultExtendedClass();
    protected abstract boolean checkForAutomaticallyGeneratedAnnotation(final String annotationName);

    public String getDecompiledCode() {
        return myDecompiledClass.toString();
    }

    public AbstractClass getDecompiledClass() {
        return myDecompiledClass;
    }

    public void setIsLambdaFunction(final boolean isLambdaFunction) {
        this.myIsLambdaFunction = isLambdaFunction;
    }

    @Override
    public void visit(final int version, final int access, final String name
            , final String signature, final String superName, final String[] interfaces)
    {
        final String modifier = DeclarationWorker.getAccess(access & ~Opcodes.ACC_SUPER, myLanguage);
        String type = "";

        if ((access & Opcodes.ACC_ENUM) == 0
                && (access & Opcodes.ACC_INTERFACE) == 0
                && (access & Opcodes.ACC_ANNOTATION) == 0) {
            type = "class ";
        }

        final String className = DeclarationWorker.getClassName(name);

        final String[] classParts = name.split("/");
        StringBuilder packageName = new StringBuilder("");
        for (int i = 0; i < classParts.length - 2; i++) {
            packageName.append(classParts[i]).append(".");
        }
        packageName.append(classParts[classParts.length - 2]);

        String superClass = "";
        String superClassImport = "";
        if (superName != null && !getDefaultExtendedClass().equals(superName)) {
            superClass = DeclarationWorker.getClassName(superName);
            superClassImport = DeclarationWorker.getDecompiledFullClassName(superName);
        }

        List<String> implementedInterfaces = new ArrayList<String>();
        List<String> implementedInterfacesImports = new ArrayList<String>();
        if (interfaces != null && interfaces.length > 0) {
            for (final String implInterface : interfaces) {
                if (!implInterface.equals(getDefaultImplementedInterface())) {
                    implementedInterfaces.add(DeclarationWorker.getClassName(implInterface));
                    implementedInterfacesImports.add(DeclarationWorker.getDecompiledFullClassName(implInterface));
                }
            }
        }

        List<String> genericTypesList = new ArrayList<String>();
        List<String> genericIdentifiersList = new ArrayList<String>();
        List<String> genericTypesImports = new ArrayList<String>();
        DeclarationWorker.parseGenericDeclaration(signature, genericTypesList, genericIdentifiersList, genericTypesImports);

        myDecompiledClass = myLanguagePartFactory.createClass(modifier, type, className, packageName.toString(), implementedInterfaces
                , superClass, genericTypesList, genericIdentifiersList, myTextWidth, myNestSize);
        myDecompiledClass.setIsLambdaFunctionClass(myIsLambdaFunction);

        if (!superClassImport.isEmpty()) {
            myDecompiledClass.appendImport(superClassImport);
        }

        myDecompiledClass.appendImports(implementedInterfacesImports);
        myDecompiledClass.appendImports(genericTypesImports);
    }

    @Override
    public void visitSource(final String source, final String debug) {
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String desc) {
        myDecompiledClass.setInnerClassIdentifier(getClassName(owner), name, desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        List<String> annotationImports = new ArrayList<String>();
        final String annotationName = getDescriptor(desc, 0, annotationImports);

        if (!checkForAutomaticallyGeneratedAnnotation(annotationName)) {
            AbstractAnnotation annotation = myLanguagePartFactory.createAnnotation();
            annotation.setName(annotationName);

            myDecompiledClass.appendAnnotation(annotation);
            myDecompiledClass.appendImports(annotationImports);

            return myVisitorFactory.createAnnotationVisitor(annotation);
        } else {
            return null;
        }
    }

    @Override
    public void visitAttribute(final Attribute attr) {
    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        final String currentClassName = myDecompiledClass.getName();
        final String innerClassName = getClassName(name);

        if (!currentClassName.equals(innerClassName) && !currentClassName.startsWith(innerClassName)) {
            try {
                AbstractClassVisitor cv = myVisitorFactory.createClassVisitor(myDecompiledClass.getTextWidth(), myDecompiledClass.getNestSize());
                ClassReader cr = new ClassReader(name);
                cr.accept(cv, 0);

                AbstractClass decompiledClass = cv.getDecompiledClass();
                decompiledClass.setIsNestedClass(true);
                if (innerName != null) {
                    myDecompiledClass.addInnerClassName(innerClassName, innerName);
                    myDecompiledClass.addInnerClass(innerClassName, decompiledClass);
                    decompiledClass.setName(myDecompiledClass.getInnerClassName(innerClassName));
                    if (outerName != null) {
                        decompiledClass.setInnerClassIdentifier(outerName, null, null);
                    }
                } else {
                    myDecompiledClass.addAnonymousClass(innerClassName, decompiledClass);
                }
            } catch (IOException e) {
            }
        }
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        List<String> fieldDeclarationImports = new ArrayList<String>();
        final String description = signature != null ? signature : desc;

        final AbstractClassField cf = myLanguagePartFactory.createClassField(DeclarationWorker.getAccess(access, myLanguage)
                , getDescriptor(description, 0, fieldDeclarationImports)
                , name, myTextWidth, myNestSize);

        myDecompiledClass.appendField(cf);
        myDecompiledClass.appendImports(fieldDeclarationImports);

        return null;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc
            , final String signature, final String[] exceptions)
    {
        final String description = signature != null ? signature : desc;
        final String modifier = DeclarationWorker.getAccess(access, myLanguage);

        List<String> throwedExceptions = new ArrayList<String>();
        if (exceptions != null) {
            for (final String exception : exceptions) {
                throwedExceptions.add(getClassName(exception));
            }
        }

        List<String> genericTypesList = new ArrayList<String>();
        List<String> genericIdentifiersList = new ArrayList<String>();
        List<String> genericTypesImports = new ArrayList<String>();
        DeclarationWorker.parseGenericDeclaration(description, genericTypesList, genericIdentifiersList, genericTypesImports);

        String returnType;
        String methodName;
        if (name.equals("<init>")) {
            returnType = "";
            methodName = myDecompiledClass.getName();
        } else {
            List<String> methodReturnTypeImports = new ArrayList<String>();
            final int returnTypeIndex = description.indexOf(')') + 1;
            returnType = getDescriptor(description, returnTypeIndex, methodReturnTypeImports);
            methodName = name;
            myDecompiledClass.appendImports(methodReturnTypeImports);
        }

        final AbstractMethod abstractMethod = myLanguagePartFactory.createMethod(modifier, returnType, methodName, desc
                , throwedExceptions.toArray(new String[throwedExceptions.size()])
                , myDecompiledClass, genericTypesList, genericIdentifiersList
                , myTextWidth, myNestSize);

        if (myDecompiledClass.isNormalClass()) {
            abstractMethod.addLocalVariableName(0, "this");
            abstractMethod.addLocalVariableType(0, myDecompiledClass.getName());
            abstractMethod.declareThisVariable();
        }

        myDecompiledClass.appendImports(genericTypesImports);

        final String parameters = description.substring(description.indexOf('(') + 1, description.indexOf(')'));
        final int startIndex = myDecompiledClass.isNormalClass() ? 1 : 0;

        DeclarationWorker.addInformationAboutParameters(parameters, abstractMethod, startIndex, myLanguage);

        myDecompiledClass.appendMethod(abstractMethod);

        return myVisitorFactory.createMethodVisitor(abstractMethod, myDecompiledClass.getPackage() + "." + myDecompiledClass.getName(), myDecompiledClass.getSuperClass());
    }

    @Override
    public void visitEnd() {
        for (final AbstractMethod method : myDecompiledClass.getMethods()) {
            myDecompiledClass.appendImports(method.getImports());
        }
    }

    protected String getClassName(final String fullClassName) {
        return myDecompiledClass.getClassName(fullClassName);
    }

    protected String getDescriptor(final String descriptor, final int pos, List<String> imports) {
        return myDecompiledClass.getDescriptor(descriptor, pos, imports, myLanguage);
    }
}
