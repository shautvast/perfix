package perfix.server.json;

import javassist.*;

import java.util.*;

import static java.util.Arrays.asList;

public class SynthSerializerFactory implements SerializerFactory {
    private static final String STRING = "java.lang.String";
    private static final String BOOLEAN = "java.lang.Boolean";
    private static final String CHARACTER = "java.lang.Character";
    private static final String BYTE = "java.lang.Byte";
    private static final String DOUBLE = "java.lang.Double";
    private static final String FLOAT = "java.lang.Float";
    private static final String LONG = "java.lang.Long";
    private static final String SHORT = "java.lang.Short";
    private static final String INTEGER = "java.lang.Integer";

    private final static Set<String> wrappersAndString = new HashSet<String>(asList(BOOLEAN, CHARACTER, BYTE, DOUBLE, FLOAT, LONG, SHORT, INTEGER,
            STRING));

    private static final String COLLECTION = "java.util.Collection";
    private static final String LIST = "java.util.List";
    private static final String SET = "java.util.Set";
    private static final List<String> mapInterfaces = asList("java.util.Map", "java.util.concurrent.ConcurrentHashMap");

    private static final Map<String, JSONSerializer<?>> serializers = new HashMap<>();
    private static final String ROOT_PACKAGE = "serializer.";

    private final ClassPool pool = ClassPool.getDefault();
    private final Map<String, CtClass> primitiveWrappers = new HashMap<String, CtClass>();
    private CtClass serializerBase;

    SynthSerializerFactory() {
        init();
    }

    private static boolean isPrimitiveOrWrapperOrString(CtClass beanClass) {
        return beanClass.isPrimitive() || wrappersAndString.contains(beanClass.getName());
    }

    void init() {
        try {
            serializerBase = pool.get(JSONSerializer.class.getName());

            primitiveWrappers.put("int", pool.get(INTEGER));
            primitiveWrappers.put("short", pool.get(SHORT));
            primitiveWrappers.put("byte", pool.get(BYTE));
            primitiveWrappers.put("long", pool.get(LONG));
            primitiveWrappers.put("float", pool.get(FLOAT));
            primitiveWrappers.put("double", pool.get(DOUBLE));
            primitiveWrappers.put("boolean", pool.get(BOOLEAN));
            primitiveWrappers.put("char", pool.get(CHARACTER));
        } catch (NotFoundException e) {
            throw new SerializerCreationException(e);
        }
    }

    public <T> JSONSerializer<T> createSerializer(Class<T> beanjavaClass) {
        try {
            CtClass beanClass = pool.get(beanjavaClass.getName());

            return createSerializer(beanClass);
        } catch (NotFoundException e) {
            throw new SerializerCreationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> JSONSerializer<T> createSerializer(CtClass beanClass) {
        if (serializers.containsKey(createSerializerName(beanClass))) {
            return (JSONSerializer<T>) serializers.get(createSerializerName(beanClass));
        }
        try {
            return tryCreateSerializer(beanClass);
        } catch (NotFoundException | CannotCompileException | InstantiationException | IllegalAccessException e) {
            throw new SerializerCreationException(e);
        }
    }

    private <T> JSONSerializer<T> tryCreateSerializer(CtClass beanClass) throws NotFoundException, CannotCompileException, InstantiationException,
            IllegalAccessException {
        CtClass serializerClass = pool.makeClass(createSerializerName(beanClass), serializerBase);

        addToJsonStringMethod(beanClass, serializerClass);

        JSONSerializer<T> jsonSerializer = createSerializerInstance(serializerClass);

        serializers.put(createSerializerName(beanClass), jsonSerializer);
        return jsonSerializer;
    }

    /*
     * create method source, compile it and add it to the class under construction
     */
    private void addToJsonStringMethod(CtClass beanClass, CtClass serializerClass) throws NotFoundException, CannotCompileException {
        String body = createToJSONStringMethodSource(beanClass);
        serializerClass.addMethod(CtNewMethod.make(body, serializerClass));
    }

    /*
     * Creates the source, handling the for JSON different types of classes
     */
    private <T> String createToJSONStringMethodSource(CtClass beanClass) throws NotFoundException {
        String source = "public String handle(Object object){\n";
        if (beanClass.isArray()) {
            source += "\tObject[] array=(Object[])object;\n";
            source += handleArray(beanClass);
        } else if (isCollection(beanClass)) {
            source += "\tObject[] array=((java.util.Collection)object).toArray();\n";
            source += handleArray(beanClass);
        } else if (isMap(beanClass)) {
            source += handleMap(beanClass);
        } else if (!isPrimitiveOrWrapperOrString(beanClass)) {
            List<CtMethod> getters = getGetters(beanClass);
            if (shouldAddGetterCallers(getters)) {
                source = addGetterCallers(beanClass, source, getters);
            }
        } else {
            source += "\treturn \"\";}";
        }
        return source;
    }

    /*
     * Any Collection is converted to an array, after which code is generated to handle the single elements.
     *
     * A subserializer is created for every single element, but most of the time it will be the same cached instance.
     *
     * The generated code fills a StringBuilder. The values are generated by the subserializers
     */
    private String handleArray(CtClass beanClass) {
        String source = "\tStringBuilder result=new StringBuilder(\"[\");\n";
        source += "\tfor (int i=0; i<array.length; i++){\n";
        source += "\t\tresult.append(" + Serializer.class.getName() + ".toJSONString(array[i]));\n";
        source += "\t\tresult.append(\", \");\n";
        source += "\t};\n";
        source += "\tresult.setLength(result.length()-2);\n";
        source += "\tresult.append(\"]\");\n";
        source += "\treturn result.toString();\n";
        source += "}";
        return source;
    }

    private String handleMap(CtClass beanClass) {
        String source = "StringBuilder result=new StringBuilder(\"{\");\n";
        source += "\tfor (java.util.Iterator entries=((java.util.Map)object).entrySet().iterator();entries.hasNext();){\n";
        source += "\t\tjava.util.Map.Entry entry=(java.util.Map.Entry)entries.next();\n";
        source += "\t\tresult.append(\"\\\"\"+entry.getKey().toString()+\"\\\"\");\n";
        source += "\t\tresult.append(\": \");\n";
        source += "\t\tresult.append(" + Serializer.class.getName() + ".toJSONString(entry.getValue()));\n";
        source += "\t\tresult.append(\", \");\n";
        source += "\t};\n";
        source += "\tresult.setLength(result.length()-2);\n";
        source += "\tresult.append(\"}\");\n";
        source += "\treturn result.toString();\n";
        source += "}";
        return source;
    }

    /*
     * If the class contains fields for which public getters are available, then these will be called in the generated code.
     */
    private String addGetterCallers(CtClass beanClass, String source, List<CtMethod> getters) throws NotFoundException {
        int index = 0;
        source += "\treturn ";
        source += "\"{";
        for (CtMethod getter : getters) {
            source = addPair(beanClass, source, getter);
            if (index++ < getters.size() - 1) {
                source += ",";
            }
        }
        source += "}\";\n}";
        return source;
    }

    @SuppressWarnings("unchecked")
    private <T> JSONSerializer<T> createSerializerInstance(CtClass serializerClass) throws InstantiationException, IllegalAccessException,
            CannotCompileException {
        return (JSONSerializer<T>) serializerClass.toClass().newInstance();
    }

    /*
     * custom root package is prepended to avoid the java.lang class in which it's illegal to create new classes
     *
     * Array marks ( '[]' ) are replaced by the 'Array', Otherwise the SerializerClassName would be syntactically incorrect
     */
    public String createSerializerName(CtClass beanClass) {
        return createSerializerName(beanClass.getName());
    }

    public String createSerializerName(String name) {
        return ROOT_PACKAGE + name.replaceAll("\\[\\]", "Array") + "Serializer";
    }

    private boolean isCollection(CtClass beanClass) throws NotFoundException {
        List<CtClass> interfaces = new ArrayList<CtClass>(asList(beanClass.getInterfaces()));
        interfaces.add(beanClass);
        for (CtClass interfaze : interfaces) {
            if (interfaze.getName().equals(COLLECTION) || interfaze.getName().equals(LIST) || interfaze.getName().equals(SET)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMap(CtClass beanClass) throws NotFoundException {
        List<CtClass> interfaces = new ArrayList<>(asList(beanClass.getInterfaces()));
        interfaces.add(beanClass);
        return interfaces.stream().anyMatch(i -> mapInterfaces.contains(i.getName()));
    }

    /*
     * The JSON vernacular for key:value is pair...
     */
    private String addPair(CtClass classToSerialize, String source, CtMethod getter) throws NotFoundException {
        source += jsonKey(getter);
        source += ": "; // what is the rule when it comes to spaces in json?
        source += jsonValue(classToSerialize, getter);
        return source;
    }

    /*
     * derive property key from getter
     */
    private String jsonKey(CtMethod getter) {
        return "\\\"" + toFieldName(getter.getName()) + "\\\"";
    }

    private String jsonValue(CtClass classToSerialize, CtMethod getter) throws NotFoundException {
        String source = "";
        CtClass returnType = getter.getReturnType();

        /* primitives are wrapped so the produced methods adhere to the JSONSerializer interface */
        source = createSubSerializerForReturnTypeAndAddInvocationToSource(classToSerialize, getter, source, returnType);

        return source;
    }

    private String createSubSerializerForReturnTypeAndAddInvocationToSource(CtClass classToSerialize, CtMethod getter, String source, CtClass returnType) {
        /* NB there does not seem to be auto(un))boxing nor generic types (or other jdk1.5 stuff) in javassist compileable code */

        source += "\"+" + Serializer.class.getName() + ".toJSONString(";

        // cast because of lack of generics
        source += "(" + cast(regularClassname(classToSerialize.getName())) + "object)." + getter.getName() + "()";

        source += ")+\"";
        return source;
    }

    /*
     * turns for example 'getValue' into 'value'
     */
    private String toFieldName(String name) {
        return name.substring(3, 4).toLowerCase() + (name.length() > 4 ? name.substring(4) : "");
    }

    public String regularClassname(String name) {
        return name.replaceAll("\\$", ".");
    }

    private String cast(String classToSerialize) {
        return "(" + classToSerialize + ")";
    }

    /*
     * Retrieves getter methods from a class
     */
    private List<CtMethod> getGetters(CtClass beanClass) {
        List<CtMethod> methods = new ArrayList<CtMethod>();
        List<CtField> fields = getAllFields(beanClass);
        for (CtField field : fields) {
            try {
                CtMethod method = beanClass.getMethod(getGetterMethod(field), getDescription(field));
                if (Modifier.isPublic(method.getModifiers())) {
                    methods.add(method);
                }
            } catch (NotFoundException n) {
                // ignore
            }
        }
        return methods;
    }

    private String getGetterMethod(CtField field) {
        return "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
    }

    private List<CtField> getAllFields(CtClass beanClass) {
        try {
            List<CtField> allfields = new ArrayList<>();
            for (CtField field : beanClass.getDeclaredFields()) {
                allfields.add(field);
            }
            if (beanClass.getSuperclass() != null) {
                return getAllFields(beanClass.getSuperclass(), allfields);
            }
            return allfields;
        } catch (NotFoundException e) {
            throw new SerializerCreationException(e);
        }

    }

    private List<CtField> getAllFields(CtClass beanClass, List<CtField> allfields) {
        for (CtField field : beanClass.getDeclaredFields()) {
            allfields.add(field);
        }

        return allfields;
    }

    /*
     * is getter list is not empty then callers should be added
     */
    boolean shouldAddGetterCallers(List<CtMethod> getters) {
        return !getters.isEmpty();
    }

    String getDescription(CtField field) throws NotFoundException {
        if (field.getType().isArray()) {
            return "()[" + innerClassName(field.getType().getName()) + ";";
        } else if (!field.getType().isPrimitive()) {
            return "()" + innerClassName(field.getType().getName()) + ";";
        } else {

            return "()" + asPrimitive(field.getType().getName());
        }
    }

    String asPrimitive(String name) {
        switch (name) {
            case "int":
                return "I";
            case "byte":
                return "B";
            case "float":
                return "F";
            case "long":
                return "J";
            case "boolean":
                return "Z";
            case "char":
                return "C";
            case "double":
                return "D";
            case "short":
                return "S";
        }
        return "";
    }

    String innerClassName(String name) {
        return "L" + name.replaceAll("\\.", "/").replaceAll("\\[\\]", "");
    }
}
