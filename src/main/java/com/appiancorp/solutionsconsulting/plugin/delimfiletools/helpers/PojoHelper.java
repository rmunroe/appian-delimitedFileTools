package com.appiancorp.solutionsconsulting.plugin.delimfiletools.helpers;

import com.appiancorp.ps.plugins.typetransformer.AppianObject;
import com.appiancorp.ps.plugins.typetransformer.AppianTypeFactory;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypeService;
import com.appiancorp.suiteapi.type.TypedValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PojoHelper {

    public static TypedValue pojoToDictionary(TypeService typeService, Object pojo) {
        AppianTypeFactory typeFactory = AppianTypeFactory.newInstance(typeService);
        AppianObject dictionary = (AppianObject) typeFactory.createElement(AppianType.DICTIONARY);

        Class aClass = pojo.getClass();

        Method[] methods = aClass.getMethods();
        for (Method method : methods) {
            if (isGetter(method)) {
                String key = method.getName().replaceFirst("^get(.*)$", "\\l$1");
                Object value;
                try {
                    value = method.invoke(pojo);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
//                if (value.getClass())
//                dictionary.put(key)
            }
        }


        return typeFactory.toTypedValue(dictionary);
    }

    public static boolean isGetter(Method method) {
        if (!method.getName().matches("^get[A-Z]")) return false;
        if (method.getParameterTypes().length != 0) return false;
        if (void.class.equals(method.getReturnType())) return false;
        return true;
    }

    public static boolean isSetter(Method method) {
        if (!method.getName().matches("^set[A-Z]")) return false;
        if (method.getParameterTypes().length != 1) return false;
        return true;
    }
}
