package com.kixeye.chassis.chassis.util;

/**
 * Property utils
 *
 * @author dturner@kixeye.com
 */
public class PropertyUtils {
    public static String getPropertyName(String placeHolder){
        return placeHolder.replace("${","").replace("}","");
    }

}
