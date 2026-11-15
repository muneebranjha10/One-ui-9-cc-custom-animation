package de.robv.android.xposed;

public final class XposedHelpers {
    public static Class<?> findClass(String className, ClassLoader classLoader) {
        throw new ClassNotFoundError(className);
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        return null;
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            String className,
            ClassLoader classLoader,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        return new XC_MethodHook.Unhook();
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            Class<?> clazz,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        return new XC_MethodHook.Unhook();
    }

    public static Object getObjectField(Object obj, String fieldName) {
        return null;
    }

    public static float getFloatField(Object obj, String fieldName) {
        return 0f;
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        return false;
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        return null;
    }

    public static final class ClassNotFoundError extends Error {
        public ClassNotFoundError(String message) { super(message); }
    }
}
