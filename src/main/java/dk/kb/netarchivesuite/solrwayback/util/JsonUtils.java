package dk.kb.netarchivesuite.solrwayback.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonUtils {
	static ObjectWriter jsonWriter;
	static {
		ObjectMapper mapper = JsonMapper.builder().build();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		jsonWriter = mapper.writer(new MinimalPrettyPrinter());
	}

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

	/**
	 * Extract values from each JSONObject in the provided list using the given
	 * path and add them to the supplied collection.
	 */
	public static <T extends Collection<String>> void addAllValues(ArrayList<JSONObject> jsonList, T values, String path) {
		for (JSONObject obj : jsonList) {
			addAllValues(obj, values, path);
		}
	}

	/**
	 * Extract a value from the provided JSONObject at the given dot-separated
	 * path and add it to the provided collection. Supports array traversal
	 * using tokens that end with "[]" (for example "users[].name").
	 */
	public static <T extends Collection<String>> void addAllValues(JSONObject json, T values, String path) {
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
					String remainingPath = String.join(".", remainingTokens);
					// System.out.println("full path:" + path);
					// System.out.println("remaining path:" + remainingPath);
					addAllValues(jsonObjectList, values, remainingPath); // Call recursive
					return;
				}
			}

			parentJson = getSubObjectIfExists(parentJson, token);
			if (parentJson == null) {
				return;
			}
		}
		// Now take last which must be string value
		String value = getValueIfExistsByPriority(parentJson, tokens[tokens.length - 1]); // Last token
		if (value != null) {
			values.add(value); // Last part of the path and found a value
		}
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

	/**
	 * Return the named child JSONObject if present, otherwise return null.
	 * Any exception encountered while retrieving the child object is caught and null is returned.
	 */
	public static JSONObject getSubObjectIfExists(JSONObject json, String subJsonObject) {
		try {
			return json.getJSONObject(subJsonObject);
		} catch (Exception e) {
			//System.out.println("subelement not found:" + subJsonObject);
			return null;
		}

	}

	/**
	 * Transform JSONArray into List<JSONObject>
	 */
	private static ArrayList<JSONObject> array2List(JSONArray array) {
		ArrayList<JSONObject> list = new ArrayList<JSONObject>();
		for (int i = 0; i < array.length(); i++) {
			list.add(array.getJSONObject(i));
		}
		return list;
	}

	/**
	 * Return true if the path token denotes an array (i.e. ends with "[]").
	 *
	 * @param element path token
	 * @return true when token ends with "[]"
	 */
	private static boolean isArrayPath(String element) {
		return element.endsWith("[]");
	}

	/**
	 * Return the element name without the trailing "[]" if present.
	 * E.g: foo | foo[] -> foo
     *
	 * @param element token possibly ending with "[]"
	 * @return element name without array suffix
	 */
	private static String elementName(String element) {
		return element.endsWith("[]") ? element.substring(0, element.length() - 2) : element;
	}

	/**
	 * Converts the given Object to a JSON String using the {@link MinimalPrettyPrinter}.
	 * The result will be a single line of JSON.
	 * @param o an Object parsable by the Jackson {@link ObjectMapper}.
	 * @return the Object as JSON, represented as a String.
	 */
	public static String toJSON(Object o) {
		try {
			return jsonWriter.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Unable to write Object of class " + o.getClass() + " as JSON", e);
		}
	}
}
