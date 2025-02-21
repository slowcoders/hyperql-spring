package example.processors;

import com.google.auto.service.AutoService;
import example.annotations.CustomAnnotation;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

@SupportedAnnotationTypes("example.annotations.CustomAnnotation")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class CustomAnnotationProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(CustomAnnotation.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }

            TypeElement classElement = (TypeElement) element;
            String className = classElement.getSimpleName().toString();
            String packageName = elementUtils.getPackageOf(classElement).toString();

            try {
                generateBuilderClass(packageName, className, classElement);
            } catch (IOException e) {
                messager.printMessage(
                        javax.tools.Diagnostic.Kind.ERROR,
                        "Failed to generate builder for " + className + ": " + e.getMessage()
                );
            }
        }
        return true;
    }

    private void generateBuilderClass(String packageName, String className, TypeElement classElement)
            throws IOException {
        String builderClassName = className + "Gen";
        String builderFullClassName = packageName + "." + builderClassName;

        System.out.println(builderFullClassName);

        JavaFileObject builderFile = filer.createSourceFile(builderFullClassName, classElement);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.println("package " + packageName + ";");
            out.println();
            out.println("public class " + builderClassName + " {");

            // 필드 생성
            for (Element enclosedElement : classElement.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    VariableElement field = (VariableElement) enclosedElement;
                    String fieldName = field.getSimpleName().toString();
                    String fieldType = field.asType().toString();

                    out.println("    private " + fieldType + " " + fieldName + ";");
                }
            }

            out.println();

            // Builder 메소드 생성
            for (Element enclosedElement : classElement.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    VariableElement field = (VariableElement) enclosedElement;
                    String fieldName = field.getSimpleName().toString();
                    String fieldType = field.asType().toString();

                    out.println("    public " + builderClassName + " " + fieldName + "(" +
                            fieldType + " " + fieldName + ") {");
                    out.println("        this." + fieldName + " = " + fieldName + ";");
                    out.println("        return this;");
                    out.println("    }");
                    out.println();
                }
            }

            // build() 메소드 생성
            out.println("    public " + className + " build() {");
            out.println("        " + className + " instance = new " + className + "();");
            for (Element enclosedElement : classElement.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    String fieldName = enclosedElement.getSimpleName().toString();
                    out.println("        instance." + fieldName + " = this." + fieldName + ";");
                }
            }
            out.println("        return instance;");
            out.println("    }");

            out.println("}");
        }
    }
//
//    /**
//     * We need an {@code Element} representation which can be independent of the round. That's
//     * because {@code Element} information is populated as new rounds are coming (for example for
//     * the generated annotations), so we need to reload {@code Element}s on every round.
//     */
//    private static final class InterRoundElement {
//
//        //All public final to avoid getters and setters in order to save space for the answer post itself.
//        public final String simpleName, packageName, qualifiedName;
//
//        public InterRoundElement(final Elements elementUtils,
//                                 final Element element) {
//            this(element.getSimpleName().toString(), elementUtils.getPackageOf(element).getQualifiedName().toString());
//        }
//
//        public InterRoundElement(final String simpleName,
//                                 final String packageName) {
//            this.simpleName = simpleName;
//            this.packageName = packageName;
//            qualifiedName = packageName.isEmpty()? simpleName: (packageName + '.' + simpleName);
//        }
//
//        @Override
//        public String toString() {
//            return qualifiedName;
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hashCode(qualifiedName);
//        }
//
//        @Override
//        public boolean equals(final Object obj) {
//            if (this == obj)
//                return true;
//            if (obj == null || getClass() != obj.getClass())
//                return false;
//            return Objects.equals(qualifiedName, ((InterRoundElement) obj).qualifiedName);
//        }
//    }
//
//    private boolean isAnnotatedWith(final AnnotationMirror annotationMirror,
//                                    final TypeElement annotation) {
//        final TypeElement other = (TypeElement) annotationMirror.getAnnotationType().asElement();
//        //Note here: 'other.getKind()' may actually be 'CLASS' rather than 'ANNOTATION_TYPE' (it happens for annotations generated by annotation processing).
//        return Objects.equals(annotation.getQualifiedName().toString(), other.getQualifiedName().toString());
//    }
//
//    /**
//     * As <i>early elements</i> are named the {@code Element}s which are potentially annotated with
//     * an enum annotation which is going to be generated. For example root elements of the first
//     * round will not appear again in the following rounds, but they may be already annotated with
//     * an enum annotation which is not yet generated, so we need to maintain them until we find out
//     * what happens.
//     */
//    private final Set<InterRoundElement> earlyElements = new HashSet<>();
//
//    /**
//     * A {@code Map} from generated enum annotations to the {@code Element}s being annotated with
//     * them. If an enum annotation is registered as a key of this map, then its code is already
//     * generated even if no {@code Elements} are found to be annotated with it (ie for empty map
//     * value).
//     */
//    private final Map<InterRoundElement, Set<InterRoundElement>> processedElements = new HashMap<>();
//
//    /**
//     * Just a zero based index of the processing round.
//     */
//    private int roundSerial = -2;
//
//    /**
//     * For debugging messages.
//     * @param tokens
//     */
//    private void debug(final Object... tokens) {
//        System.out.print(String.format(">>>> [Round %2d]", roundSerial));
//        for (final Object token: tokens) {
//            System.out.print(' ');
//            System.out.print(token);
//        }
//        System.out.println();
//    }
//
//    /**
//     * Opens a {@code PrintStream} for writing/generating code.
//     * @param interRoundElement
//     * @param originatingElements
//     * @return
//     * @throws IOException
//     */
//    private PrintStream create(final InterRoundElement interRoundElement,
//                               final Element... originatingElements) throws IOException {
//        debug("Will generate output for", interRoundElement);
//        final JavaFileObject outputFileObject = processingEnv.getFiler().createSourceFile(interRoundElement.qualifiedName, originatingElements);
//        return new PrintStream(outputFileObject.openOutputStream());
//    }
//
//    /**
//     * Generates an enum annotation.
//     * @param origin
//     * @param output
//     * @param originatingElements
//     * @return {@code true} for success, otherwise {@code false}.
//     */
//    private boolean generateEnumAnnotation(final InterRoundElement origin,
//                                           final InterRoundElement output,
//                                           final Element... originatingElements) {
//        try (final PrintStream outputFileOutput = create(output, originatingElements)) {
//            if (!output.packageName.isEmpty()) { //The default package is represented as an empty 'packageName'.
//                outputFileOutput.println("package " + output.packageName + ";");
//                outputFileOutput.println();
//            }
//            for (final Object line: new Object[]{ //We obviously here need to utilize text blocks of the newer Java versions...
//                    "@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)",
//                    "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)",
//                    "public @interface " + output.simpleName + " {",
//                    "    " + origin.qualifiedName + " value();",
//                    "}"
//            })
//                outputFileOutput.println(line);
//            return true;
//        }
//        catch (final IOException ioe) {
//            ioe.printStackTrace(System.out);
//            return false;
//        }
//    }
//
//    private void reset() {
//        processedElements.clear();
//        earlyElements.clear();
//        roundSerial = -1;
//    }
//
//    @Override
//    public synchronized void init(final ProcessingEnvironment processingEnv) {
//        super.init(processingEnv);
//        if (super.isInitialized())
//            reset(); //Initialize, prepare for new processes.
//    }
//
//    /**
//     * This method handles elements annotated with {@code GenerateEnumAnnotation}.
//     * @param annotatedElement
//     */
//    private void handleGenerationAnnotation(final Element annotatedElement) {
//        final Messager messager = processingEnv.getMessager();
//        if (annotatedElement.getKind() != ElementKind.ENUM)
//            messager.printMessage(Diagnostic.Kind.ERROR, "Only enums are supported.", annotatedElement);
//        else {
//            final InterRoundElement origin = new InterRoundElement(processingEnv.getElementUtils(), (TypeElement) annotatedElement);
//            final String pack = (origin.packageName.isEmpty()? "": (origin.packageName + '.')) + "enum_annotations";
//            final InterRoundElement output = new InterRoundElement(origin.simpleName + "Annotation", pack); //The enum annotation element to generate...
//            if (generateEnumAnnotation(origin, output, annotatedElement))
//                processedElements.computeIfAbsent(output, dejaVu -> new HashSet<>()); //Store the generated enum annotation information.
//            else
//                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to create annotation " + output + " (for " + origin + ").", annotatedElement);
//        }
//    }
//
//    /**
//     * Handles {@code Element}s annotated with a generated enum annotation. Modify this according to
//     * your requirements. It assumes that generated enum annotations are not repeatable (otherwise
//     * it should take a {@code Collection} of {@code AnnotationMirror}s instead of a single one).
//     * @param enumAnnotation The generated enum annotation.
//     * @param annotatedElement The {@code Element} annotated with {@code enumAnnotation}.
//     * @param annotationMirror The mirror of annotating {@code annotatedElement} with {@code enumAnnotation}.
//     */
//    private void handleEnumAnnotation(final TypeElement enumAnnotation,
//                                      final Element annotatedElement,
//                                      final AnnotationMirror annotationMirror) {
//        //The current implementation just prints some messages of the annotation we've found...
//        debug("Processing", annotatedElement.getKind(), "element", annotatedElement);
//        debug("    Annotated by enum annotation", enumAnnotation);
//        debug("    With the following applicable mirror:", annotationMirror);
//    }
//
//    /**
//     * Find out if any "early elements" are annotated with a generated enum annotation, and handle them.
//     */
//    private void processEarlyElements() {
//        final Elements elementUtils = processingEnv.getElementUtils();
//        //We need a defensive shallow copy of 'earlyElements' because it is going to be modified inside the loop:
//        final Set<InterRoundElement> defensiveCopiedEarlyElements = new HashSet<>(earlyElements);
//        processedElements.forEach((annotationInterRoundElement, annotatedElements) -> {
//            final TypeElement generatedEnumAnnotation = elementUtils.getTypeElement(annotationInterRoundElement.qualifiedName);
//            //'roundEnv.getElementsAnnotatedWith(generatedEnumAnnotation)' will actually return an empty List here, so we have to find elements annotated with 'generatedEnumAnnotation' via its annotation mirrors...
//            defensiveCopiedEarlyElements.forEach(interRoundElement -> {
//                final TypeElement annotatedElement = elementUtils.getTypeElement(interRoundElement.qualifiedName); //Reload annotated element for the current round (ie don't rely on its previous Element occurences), because its annotations may not yet be ready.
//                //The following code assumes generated enum annotations are not repeatable...
//                annotatedElement.getAnnotationMirrors().stream()
//                        .filter(annotationMirror -> isAnnotatedWith(annotationMirror, generatedEnumAnnotation)) //Continue only for mirrors of type generatedEnumAnnotation.
//                        .filter(annotationMirror -> annotatedElements.add(interRoundElement)) //If we've seen the early element before then skip it, otherwise add it to annotatedElements and process it...
//                        .findAny()
//                        .ifPresent(annotationMirror -> {
//                            earlyElements.remove(interRoundElement); //No need to store it any more.
//                            handleEnumAnnotation(generatedEnumAnnotation, annotatedElement, annotationMirror);
//                        });
//            });
//        });
//    }
//
//    @Override
//    public boolean process(final Set<? extends TypeElement> annotations,
//                           final RoundEnvironment roundEnv) {
//        ++roundSerial;
//        final Elements elementUtils = processingEnv.getElementUtils();
//        final Set<? extends Element> rootElements = roundEnv.getRootElements();
//
//        //Store only root elements of type class in 'earlyElements':
//        rootElements.stream()
//                .filter(rootElement -> rootElement.getKind() == ElementKind.CLASS)
//                .map(rootElement -> new InterRoundElement(elementUtils, rootElement))
//                .forEachOrdered(earlyElements::add);
//
//        debug("Annotations:", annotations);
//        debug("Root elements:", rootElements);
//
//        /*First process early elements and then generate enum annotations. The sequence of these two
//        calls is ought to how their methods' body is implemented, and we know we won't loose any
//        enum annotations because any generated enum annotations in the current round will be
//        supplied as root elements in the next round and we already add "early elements" in the
//        beginning of the processor's 'process' method.*/
//        processEarlyElements();
//        roundEnv.getElementsAnnotatedWith(GenerateEnumAnnotation.class).forEach(this::handleGenerationAnnotation);
//
//        debug("Current early elements:", earlyElements);
//        processedElements.forEach((annotation, elements) -> debug("Created annotation", annotation, "with the following elements processed for it so far:", elements));
//
//        //Cleanup, prepare for later processes (if reused):
//        if (roundEnv.processingOver())
//            reset();
//
//        /*GenerateEnumAnnotations are always consumed. Even if there is a failure in generating the
//        resulting code from a GenerateEnumAnnotation annotated element, this doesn't mean that
//        repeating the handleGenerationAnnotation has the potential in succeeding in later rounds,
//        so we are not repeating the attempt (ie handling is finished, errors are handled, we move on).*/
//        return true;
//    }
}
