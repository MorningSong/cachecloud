package com.sohu.cache.constant;

/**
 * @author leifu
 * @Date 2016-1-26
 * @Time 下午9:26:58
 */
public class BaseConstant {

    /** mill seconds of one day */
    public static long MILLISECONDS_OF_ONE_SECOND = 1000;

    /** mill seconds of one day */
    public static long MILLISECONDS_OF_ONE_MINUTE = 1000L * 60;

    /** mill seconds of one day */
    public static long MILLISECONDS_OF_ONE_DAY = 1000L * 60 * 60 * 24;

    /** mill seconds of one hour */
    public static long MILLISECONDS_OF_ONE_HOUR = 1000L * 60 * 60;

    public static String EMPTY_STRING = "";

    public static String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * WORD_SEPARATOR ( char )2
     */
    public static final String WORD_SEPARATOR = Character.toString((char) 2);

    public static final String SYSTEM_PROPERTY_CONFIG_FILE_PATH = "configFilePath";

    public static final int ONE = 1;

    public static final int ZERO = 0;

}
