package com.yusufcihan.DynamicComponents.classes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import android.util.Log;

public class Utils {
  // Extension log tag
  public static final String TAG = "DynamicComponents";
  // Base package name for components
  private static final String BASE = "com.google.appinventor.components.runtime.";
  private static final Pattern classNamePattern = Pattern.compile("[^.$@a-zA-Z0-9_]");
  private static final Pattern methodNamePattern = Pattern.compile("[^a-zA-Z0-9]");

  public static boolean isNotEmptyOrNull(Object item) {
    return item instanceof String ? !((String) item).replace(" ", "").isEmpty() : item != null;
  }

  /*
      Get class name from a component object, component name,
      full class name.

      "Button" -> "com.google.appinventor.components.runtime.Button"
      (Component block) -> class name of the component
      "com.example.MyComponent" -> leave as-is
  */
  public static String getClassName(Object componentName) {
    String componentNameString = classNamePattern.matcher(componentName.toString()).replaceAll("");
    if (componentName instanceof String && componentNameString.contains(".")) {
      return componentNameString;
    } else if (componentName instanceof String) {
      return BASE + componentNameString;
    } else if (componentName instanceof Component) {
      Matcher componentNameResolved = classNamePattern.matcher(componentName.getClass().getName());
      return componentNameResolved.replaceAll("");
    } else {
      throw new YailRuntimeError("Component is invalid.", TAG);
    }
  }

  /*
      Create a new instance of component constructor and
      add it to the given "input" container.
  */
  public static Component createInstance(Constructor<?> constructor, AndroidViewComponent input) {
    Component createdComponent = null;
    try {
      createdComponent = (Component) constructor.newInstance(input);
    } catch(Exception e) {
      String errorMessage = e.getMessage() == null ? "Unknown error" : e.getMessage();
      throw new YailRuntimeError("Couldn't create an instance: " + errorMessage, TAG);
    }
    // Canvas components needs to be initialized with invoking "Initialize" method.
    String createdComponentClassName = createdComponent.getClass().getSimpleName();
    final String[] canvasComponents = {"Ball", "ImageSprite", "Sprite"};
    if (Arrays.asList(canvasComponents).contains(createdComponentClassName)) {
      callMethod(createdComponent, "Initialize", new Object[] { });
    }
    return createdComponent;
  }

  /*
      Find a method from list of methods by name and parameter count.
      Return null if not found.
  */
  public static Method getMethod(Object object, String name, int parameterCount) {
    String nameString = methodNamePattern.matcher(name).replaceAll("");
    for (Method method : object.getClass().getMethods()) {
      int methodParameterCount = method.getParameterTypes().length;
      if (method.getName().equals(nameString) && methodParameterCount == parameterCount) {
        return method;
      }
    }
    return null;
  }

  /*
      Get a method of a object by its name. Return null if not found.
  */
  public static Method getMethod(Object object, String name) {
    try {
      return object.getClass().getMethod(name);
    } catch (NoSuchMethodException e) {
      Log.e(TAG, "[priority=low] Method not found with name: '" +  name + "'");
    }
    return null;
  }

  /*
      Invoke a method of an object by its name and get its return value.
  */
  public static Object callMethod(Object object, String name, Object[] parameters) {
    if (!isNotEmptyOrNull(object)) {
      throw new YailRuntimeError("Component cannot be null.", TAG);
    }
    try {
      Method mMethod = getMethod(object, name, parameters.length);
      Class<?>[] mRequestedMethodParameters = mMethod.getParameterTypes();

      for (int i = 0; i < mRequestedMethodParameters.length; i++) {
        final String value = String.valueOf(parameters[i]);

        switch (mRequestedMethodParameters[i].getName()) {
          case "int":
            parameters[i] = Integer.parseInt(value);
            break;
          case "float":
            parameters[i] = Float.parseFloat(value);
            break;
          case "double":
            parameters[i] = Double.parseDouble(value);
            break;
          case "java.lang.String":
            parameters[i] = value;
            break;
          case "boolean":
            parameters[i] = Boolean.parseBoolean(value);
            break;
        }
      }
      Object mInvokedMethod = mMethod.invoke(object, parameters);
      return mInvokedMethod == null ? "" : mInvokedMethod;
    } catch (InvocationTargetException e) {
      String errorMessage = e.getCause().getMessage() == null ? e.getCause().toString() : e.getCause().getMessage();
      throw new YailRuntimeError("Got an error inside the invoke: " + errorMessage, TAG);
    } catch (Exception e) {
      e.printStackTrace();
      String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
      throw new YailRuntimeError("Couldn't invoke: " + errorMessage, TAG);
    }
  }

  /*
      Replace template keys in a string for each given template
      keys and their corresponding values.

      "Hello, {name}!" -> "Hello, John!"

      We don't need additional formatting options, so we just
      simply replace the text here.
  */
  public static String formatTemplateString(String text, final Iterable<String[]> formatMapping) {
    String replacedText = text;
    for (String[] formatPair : formatMapping) {
      final String formatString = "{" + formatPair[0] + "}";
      if (replacedText.contains(formatString)) {
        replacedText = replacedText.replace(formatString, formatPair[1]);
      }
    }
    return replacedText;
  }

  /*
      Recursively traverse the children of a given component and
      return a flattened list with "parent" keys appended representing
      the ID of the parent component which the current component
      will be created in.
  */
  public static LinkedList<JSONObject> componentDataToList(
    String parentId, JSONObject componentData, final Iterable<String[]> formatMapping
  ) {
    LinkedList<JSONObject> componentsOutput = new LinkedList<JSONObject>();
    JSONObject currentComponent = new JSONObject();
    if (!componentData.has("id") || !componentData.has("type")) {
      throw new YailRuntimeError("All components in the schema at least must have an 'id' and 'type'.", TAG);
    }
    currentComponent.put("type", formatTemplateString(componentData.getString("type"), formatMapping));
    currentComponent.put("id", formatTemplateString(componentData.getString("id"), formatMapping));
    if (!parentId.isEmpty()) {
      currentComponent.put("parent", parentId);
    }
    JSONObject currentProperties = new JSONObject();
    if (componentData.has("properties")) {
      final JSONObject propertyObject = componentData.getJSONObject("properties");
      final Iterator<?> propertyObjectKeys = propertyObject.keys();
      while (propertyObjectKeys.hasNext()) {
        String key = (String)propertyObjectKeys.next();
        Object value = propertyObject.get(key);
        currentProperties.put(
          formatTemplateString(key, formatMapping),
          value instanceof String ? formatTemplateString((String)value, formatMapping) : value
        );
      }
    }
    currentComponent.put("properties", currentProperties);
    componentsOutput.addLast(currentComponent);
    if (componentData.has("components")) {
      final JSONArray childComponents = componentData.getJSONArray("components");
      for (int i = 0; i < childComponents.length(); i++) {
        final JSONObject childObject = childComponents.getJSONObject(i);
        final LinkedList<JSONObject> childTree = componentDataToList(
          formatTemplateString(currentComponent.getString("id"), formatMapping),
          childObject, formatMapping
        );
        for (JSONObject child : childTree) {
          componentsOutput.addLast(child);
        }
      }
    }
    return componentsOutput;
  }

  /*
      Recursively traverse children for each component in given list and
      return a flattened list in result.
  */
  public static LinkedList<JSONObject> componentTreeToList(
    JSONArray componentList, final Iterable<String[]> formatMapping
  ) {
    LinkedList<JSONObject> componentsOutput = new LinkedList<JSONObject>();
    try {
      for (int i = 0; i < componentList.length(); i++) {
        LinkedList<JSONObject> childTree = componentDataToList("", componentList.getJSONObject(i), formatMapping);
        for (JSONObject child : childTree) {
          componentsOutput.addLast(child);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
      throw new YailRuntimeError("Couldn't gather components from schema, reason: " + errorMessage, TAG);
    }
    return componentsOutput;
  }
}
