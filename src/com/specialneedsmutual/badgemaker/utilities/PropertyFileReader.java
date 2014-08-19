package com.specialneedsmutual.badgemaker.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class designed to read from a single Properties file - in this case, only the
 * snmBadge.properties file.
 * 
 * @author Jacob
 * 
 */
public class PropertyFileReader {

	private static final Properties PROPS = new Properties();

	/**
	 * Initializes the property file reader with the specified Properties file
	 * 
	 * @return null if no errors. An error string if there were problems.
	 */
	public static String initialize(InputStream propertiesStream) {
		try {
			PROPS.clear();
			PROPS.load(propertiesStream);
			propertiesStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}
		return null;
	}

	/**
	 * Gets a property from the snmBadge.properties file
	 * 
	 * @param key The key, like "snmBadge.spaceInNameDelimiter"
	 * @param defaultValue Any default value, like "%"
	 * @return The String from the properties file, or the default string if
	 *         there was no matching key
	 */
	public static String getProperty(String key, String defaultValue) {
		String gottenValue;
		gottenValue = PROPS.getProperty(key, defaultValue);
		return gottenValue;
	}

}
