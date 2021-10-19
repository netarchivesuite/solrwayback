package dk.kb.netarchivesuite.solrwayback.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonUtils {

	/*
	 * Define the path down to the value to extract. Example of path: user.name
	 * First it will get the user (JSONObject) Since name is last value in path,
	 * take the "name" value on the user object. If one of the subelements is an
	 * JSONArray the syntax uses [] example: entities.user_mentions[].screen_name
	 */
	public static String getValue(JSONObject json, String path) {
		// Split in tokens on .
		String[] tokens = path.split("\\."); // Have to escape the dot
		JSONObject parentJson = json;
		for (int i = 0; i < tokens.length - 1; i++) { // Skip last
			String token = tokens[i];
			parentJson = getSubObjectIfExists(parentJson, token);
			if (parentJson == null) {
				return null; //
			}
		}
		// Now take last which must be string value

		// Will convert both int and strings to string.
		String value = parentJson.get(tokens[tokens.length - 1]).toString(); // Last token
		return value;
	}

	public static String getValue(JSONObject json, String path, String defaultValue) {
		String value = getValue(json, path);
		return value == null || value.equals("null") ? defaultValue : value;
	}

	public static Set<String> addAllValues(ArrayList<JSONObject> jsonList, Set<String> values, String path) {
		for (JSONObject obj : jsonList) {
			addAllValues(obj, values, path);
		}
		return values;
	}

	public static Set<String> addAllValues(JSONObject json, Set<String> values, String path) {

		// Split in tokens on .
		String[] tokens = path.split("\\."); // Have to escape the dot

		JSONObject parentJson = json;
		for (int i = 0; i < tokens.length - 1; i++) { // Skip last
			String token = tokens[i];
			if (isArrayPath(token)) {
				//System.out.println("arrayfound:" + token);
				//System.out.println("arrayfound, elementaname:" + elementName(token));
				if (parentJson.has(elementName(token))) {
					JSONArray jsonArray = parentJson.getJSONArray(elementName(token));
					ArrayList<JSONObject> jsonObjectList = array2List(jsonArray);
					// Create a sub-array from the full array.
					String[] remainingTokens = Arrays.copyOfRange(tokens, i + 1, tokens.length);
					String remainingPath = String.join(". ", remainingTokens);
					//System.out.println("full path:" + path);
					//System.out.println("remaining path:" + remainingPath);
					addAllValues(jsonObjectList, values, remainingPath); // Call recursive
					return values;
				}
			}

			parentJson = getSubObjectIfExists(parentJson, token);
			if (parentJson == null) {
				return values;
			}
		}
		// Now take last which must be string value
		String value = getValueIfExistsByPriority(parentJson, tokens[tokens.length - 1]); // Last token
		if (value != null) {
			values.add(value); // Last part of the path and found a value
		}
		return values;
	}

	/*
	 * Will call the getValue method with each of the paths elements one by one.
	 * Return first value it finds this way
	 */
	public static String getValueIfExistsByPriority(JSONObject json, String... paths) {
		for (String path : paths) {
			String value = getValue(json, path);
			if (value != null) {
				return value; // first value not null
			}
		}
		return null; // none of the paths found a value
	}

	public static JSONObject getSubObjectIfExists(JSONObject json, String subJsonObject) {
		try {
			return json.getJSONObject(subJsonObject);
		} catch (Exception e) {
			//System.out.println("subelement not found:" + subJsonObject);
			return null;
		}

	}

	/*
	 * Transform JSONArray into List<JSONObject>
	 */
	private static ArrayList<JSONObject> array2List(JSONArray array) {
		ArrayList<JSONObject> list = new ArrayList<JSONObject>();
		for (int i = 0; i < array.length(); i++) {
			list.add(array.getJSONObject(i));
		}
		return list;
	}

	// ends with []
	private static boolean isArrayPath(String element) {
		return element.endsWith("[]");
	}

	// foo | foo[] -> foo
	private static String elementName(String element) {
		return element.endsWith("[]") ? element.substring(0, element.length() - 2) : element;
	}
}
