package org.slowcoders.jql.util;

import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class ClassUtils {
    private static HashMap<Class<?>, Object[]> emptyArrays = new HashMap<>();

    public static Collection asCollection(Object obj) {
        if (obj instanceof Collection) {
            return (Collection) obj;
        }
        if (obj instanceof Object[]) {
            return Arrays.asList((Object[]) obj);
        }
        return null;
    }

    public static <T> T newInstanceOrNull(String className) {
        try {
            Class<T> c = (Class)Class.forName(className);
            return newInstance(c);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T newInstance(Class<T> c) throws RuntimeException {
        try {
            Constructor constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T)constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> toClass(Type type) {
        if (type instanceof ParameterizedType) {
            return (Class<?>)((ParameterizedType)type).getRawType();
        }
        if (type instanceof WildcardType) {
            return (Class<?>)((WildcardType)type).getUpperBounds()[0];
        }
        if (type instanceof GenericArrayType) {
            return Array.newInstance((Class<?>)((GenericArrayType)type).getGenericComponentType(), 0).getClass();
        }
        return (Class<?>)type;
    }

    public static Class<?> getElementType(Field f) {
        Type t = f.getGenericType();
        if (Collection.class.isAssignableFrom(f.getType())) {
            t = ClassUtils.getFirstGenericParameter(t);
        }
        return ClassUtils.toClass(t);
    }

    public static Type getFirstGenericParameter(Type type) {
        Type[] paramTypes = getGenericParameters(type);
        if (paramTypes == null) {
            return null;
        }
        return paramTypes[0];
    }

    public static Type[] getGenericParameters(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType)type;
        return parameterizedType.getActualTypeArguments();
    }

    public static <T> T[] emptyArray(Class<T> class1) {
        synchronized (emptyArrays) {
            Object[] array = emptyArrays.get(class1);
            if (array == null) {
                array = (Object[])Array.newInstance(class1, 0);
                emptyArrays.put(class1, array);
            }
            return (T[])array;
        }
    }

    private static HashMap<Class<?>, Class<?>> arrayTypes = new HashMap<>();
    public static Class<?> getArrayType(Class<?> itemType) {
        Class<?> arrayType = arrayTypes.get(itemType);
        if (arrayType == null) {
            arrayType = Array.newInstance(itemType, 0).getClass();
            arrayTypes.put(itemType, arrayType);
        }
        return arrayType;
    }

    public static Field findAnnotatedField(Class<?> clazz, Class<? extends Annotation> annotation) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getAnnotation(annotation) != null) {
                return f;
            }
        }
        return findAnnotatedField(clazz.getSuperclass(), annotation);
    }

    public static Field findPublicField(Class<?> returnType, String name) {
        for (Field f : returnType.getFields()) {
            if (f.getName().equals(name)) return f;
        }
        return null;
    }

    public static Method findPublicMethod(Class<?> returnType, String name) {
        for (Method m : returnType.getMethods()) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }

    public static ArrayList<Field> getInstanceFields(Class entityType, boolean excludeJpaTransient) {
        ArrayList<Field> fields = new ArrayList<>();
        getColumnFields(fields, entityType, excludeJpaTransient);
        return fields;
    }

    private static void getColumnFields(ArrayList<Field> columns, Class entityType, boolean excludeJpaTransient) {
        Class<?> superClass = entityType.getSuperclass();
        if (superClass != Object.class) {
            getColumnFields(columns, superClass, excludeJpaTransient);
        }
        for (Field f : entityType.getDeclaredFields()) {
            if (excludeJpaTransient && f.getAnnotation(Transient.class) != null) continue;
            if ((f.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
                columns.add(f);
            }
        }
    }

    public static Class<?> getBoxedType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            clazz = primitiveBoxes.get(clazz);
        }
        return clazz;
    }

    private static final HashMap<Class<?>, Class<?>> primitiveBoxes = new HashMap<>();
    static {
        primitiveBoxes.put(boolean.class, Boolean.class);
        primitiveBoxes.put(byte.class, Byte.class);
        primitiveBoxes.put(char.class, Character.class);
        primitiveBoxes.put(short.class, Short.class);
        primitiveBoxes.put(int.class, Integer.class);
        primitiveBoxes.put(long.class, Long.class);
        primitiveBoxes.put(float.class, Float.class);
        primitiveBoxes.put(double.class, Double.class);
    }


}