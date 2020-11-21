package com.yusufcihan.DynamicComponents;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.YailDictionary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@DesignerComponent(
  description = "Dynamic Components is an extension that creates any component in your App Inventor distribution programmatically, instead of having pre-defined components. Made with &#x2764;&#xfe0f; by Yusuf Cihan",
  category = ComponentCategory.EXTENSION,
  helpUrl = "https://github.com/ysfchn/DynamicComponents-AI2",
  iconName = "aiwebres/icon.png",
  nonVisible = true,
  version = 6,
  versionName = "2.1.0"
)
@SimpleObject(external = true)
public class DynamicComponents extends AndroidNonvisibleComponent {
  private Activity activity;
  private Internal internal = null;
  private boolean isAsync = false;

  /**
    * Contains the created components. Key is the ID of the components, and their values are the components
    * that created with Create block.
    */
  private final HashMap<String, Component> COMPONENTS = new HashMap<>();

  /**
    * Specifies the base package for creating the components.
    */
  private final String BASE_PACKAGE = "com.google.appinventor.components.runtime";

  /**
    * Stores the component template. Needs to be cleared before rendering Schema operation.
    */
  private JSONArray PROPERTIESARRAY = new JSONArray();

  public DynamicComponents(ComponentContainer container) {
    super(container.$form());
    this.activity = container.$context();
    this.internal = new Internal();
  }

  /**
    * Raises after a schema has created with the "Schema" block.
    *
    * @param name          Name of the template.
    * @param parameters    The JSON parameters in the schema.
    */
  @SimpleEvent(description = "Raises after a schema has created with the \"Schema\" block.")
  public void SchemaCreated(String name, YailList parameters) {
    EventDispatcher.dispatchEvent(this, "SchemaCreated", name, parameters);
  }

  /**
    * Raises after a component has been created using the Create block.
    *
    * @param id          The ID of the created component.
    * @param type        Type type name of the created component.
    */
  @SimpleEvent(description = "Raises after a component has been created using the Create block. It also will be raised for components that created with Schema.")
  public void ComponentCreated(String id, String type) {
    EventDispatcher.dispatchEvent(this, "ComponentCreated", id, type);
  }

  @DesignerProperty(defaultValue = "False", editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN)
  @SimpleProperty(description = "Sets whether component creation should rely on the UI thread.")
  public void Async(boolean async) {
    isAsync = async;
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Gets whether component creation should rely on the UI thread.")
  public boolean Async() {
    return isAsync;
  }

  /**
    * Creates a new dynamic component. It supports all component that added to your
    * current AI2 distribution. In componentName, you can type the component's name
    * like "Button", or you can pass a static component then it can create a new
    * instance of it.
    *
    * @param in                Layout/Component where the created component will be placed.
    * @param componentName     Name/FQCN of the component that will be created.
    * @param id                A unique identifier for this component.
    */
  @SimpleFunction(description = "Creates a new dynamic component. It supports all component that added to your current AI2 distribution. In componentName, you can type the component's name like 'Button', or you can pass a static component then it can create a new instance of it, or just type the full class name of component.")
  public void Create(final AndroidViewComponent in, final Object componentName, final String id) {
    if (isAsync) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          CreateMethod(in, componentName, id);
        }
      });
    } else {
      CreateMethod(in, componentName, id);
    }
  }

  public void CreateMethod(final AndroidViewComponent in, final Object componentName, final String id) {
    String className = internal.checkId(componentName, id);

    try {
      if (!"".equals(className)) {
        // Create a Class object from class name.
        Class<?> classObject = Class.forName(className.trim().replace(" ", ""));

        // Create constructor object for creating a new instance.
        Constructor<?> constructor = classObject.getConstructor(ComponentContainer.class);

        // Create a new instance of specified component.
        Component component = (Component) constructor.newInstance((ComponentContainer) in);

        if (className.contains("ImageSprite") || className.contains("Sprite")) {
          Invoke(component, "Initialize", new YailList());
        }

        // Save the component.
        COMPONENTS.put(id, component);

        // Finalize component creation.
        ComponentCreated(id, classObject.getSimpleName());
      }
    } catch (Exception exception) {
      throw new YailRuntimeError("DynamicComponents-AI2: " + exception.toString(), "Error");
    }
  }

  public void SchemaMethod(final AndroidViewComponent in, final YailList parameters, final String template) {
    try {
      // Remove the contents of the array by creating a new JSONArray.
      PROPERTIESARRAY = new JSONArray();
      // Create a JSONObject from template for checking.
      JSONObject j = new JSONObject(template);
      // Save the template string to a new variable for editing.
      String modifiedTemplate = internal.replaceKeys(j, parameters, template);

      // Check the metadata version for checking compatibility for next/previous versions of the extension.
      // Will be used in the future releases.
      if (j.optInt("metadata-version", 0) == 0) {
        throw new YailRuntimeError("Metadata version is not specified!", "Error");
      }
      // Lastly parse the JSONObject.
      internal.parseJson("", new JSONObject(modifiedTemplate));
      // Delete the first element, because it contains metadata instead of components.
      PROPERTIESARRAY.remove(0);

        // Start creating the extensions (finally).
      for (int i = 0; i < PROPERTIESARRAY.length(); i++) {
        // Check if component has an ID key.
        if (!PROPERTIESARRAY.getJSONObject(i).has("id")) {
          throw new YailRuntimeError("One or more of the components has not an ID in template!", "Error");
        }

        internal.createBySchema(in, i, PROPERTIESARRAY);

        // If JSONObject contains a "properties" section, then set its properties with
        // Invoke block.
        if (PROPERTIESARRAY.getJSONObject(i).has("properties")) {
          internal.setPropertiesBySchema(i, PROPERTIESARRAY);
        }
      }

      SchemaCreated(j.optString("name"), parameters);
    } catch (Exception e) {
      throw new YailRuntimeError(e.getMessage(), "Error");
    }
  }

  /**
    * Imports a JSON string that is a template for creating the dynamic components
    * automatically with single block. Templates can also contain parameters that
    * will be replaced with the values which defined in the "parameters" list.
    *
    * @param in            Layout/Component where the created component will be placed.
    * @param template      A JSON string containing information for creating the component.
    * @param parameters    Data for parameters defined in the above JSON string.
    */
  @SimpleFunction(description = "Imports a JSON string that is a template for creating the dynamic components automatically with single block. Templates can also contain parameters that will be replaced with the values which defined in the 'parameters' list.")
  public void Schema(final AndroidViewComponent in, final String template, final YailList parameters) {
    if (isAsync) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          SchemaMethod(in, parameters, template);
        }
      });
    } else {
      SchemaMethod(in, parameters, template);
    }
  }

  /**
    * Replaces the specified ID with the given new ID. The old ID must be bound to
    * a component and the new ID must be unique, i.e., not used already.
    *
    * @param id        Existing ID of the specified component.
    * @param newId     The ID to replace existing one with.
    */
  @SimpleFunction(description = "Replaces the specified ID with the given new ID. The old ID must be bound to a component and the new ID must be unique, i.e., not used already.")
  public void ChangeId(String id, String newId) {
    if (COMPONENTS.containsKey(id) && !COMPONENTS.containsKey(newId)) {
      Component component = COMPONENTS.remove(id);
      COMPONENTS.put(newId, component);
    } else {
      throw new YailRuntimeError("Old ID must exist and new ID mustn't exist.", "Error");
    }
  }

  /**
    * Moves the specified component to the given arrangement.
    *
    * @param arrangement   Target arrangement in which the specified component should be transferred.
    * @param component     Component that needs to be transferred.
    */
  @SimpleFunction(description = "Moves the specified component to the given arrangement.")
  public void Move(AndroidViewComponent arrangement, AndroidViewComponent component) {
    View comp = component.getView();
    ViewGroup source = (ViewGroup)comp.getParent();
    source.removeView(comp);

    ViewGroup vg2 = (ViewGroup)arrangement.getView();
    ViewGroup target = (ViewGroup)vg2.getChildAt(0);

    target.addView(comp);
  }

  /**
    * Returns the position of the component with respect to other sibling components
    * in it's parent arrangement. Index starts from 1.
    *
    * @param component Target component.
    * @return          Position index of the specified component in the parent arrangement.
    */
  @SimpleFunction(description = "Returns the position of the component with respect to other sibling components in it's parent arrangement. Index starts from 1.")
  public int GetOrder(AndroidViewComponent component){
    View comp = component.getView();
    View parent = (View) component.getView().getParent();

    if (comp != null && parent != null) {
      View v = component.getView();
      ViewGroup vg = (ViewGroup)v.getParent();
      int index = vg.indexOfChild(v);
      return index + 1;
    } else {
      return 0;
    }
  }

  /**
    * Sets the position of the specified component with respect to other sibling
    * components in it's parent arrangement. Index starts from 1. Typing 0 (zero)
    * will move the component to the end.
    *
    * @param component Target component.
    * @param index     Index at which the component should be placed in it's parent component.
    */
  @SimpleFunction(description = "Sets the position of the specified component with respect to other sibling components in it's parent arrangement. Index starts from 1. Typing 0 (zero) will move the component to the end.")
  public void SetOrder(AndroidViewComponent component, int index) {
    View comp = component.getView();
    ViewGroup source = (ViewGroup)comp.getParent();
    source.removeView(comp);

    int i = index - 1;
    int childCount = source.getChildCount();

    if (i > childCount) {
      i = childCount;
    }

    source.addView(comp, i);
  }

  /**
    * Removes the component with specified ID from screen/layout and the component
    * list. So you will able to use its ID again as it will be deleted.
    *
    * @param id    The ID of the component that is supposed to be removed.
    */
  @SimpleFunction(description = "Removes the component with specified ID from screen/layout and the component list so you can use its ID again after it's deleted.")
  public void Remove(String id) {
    if (internal.isIdTaken(id)) {
      Object component = COMPONENTS.get(id);

      try {
        if (component != null) {
          Method method = component.getClass().getMethod("getView");
          View componentView = (View) method.invoke(component);
          ViewGroup parent = (ViewGroup) componentView.getParent();
          parent.removeView(componentView);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      COMPONENTS.remove(id);
    }
  }

  /**
    * Returns the ID of the last component created using the "Create" block.
    *
    * @return  The last used ID.
    */
  @SimpleFunction(description = "Returns the ID of the last component created using the \"Create\" block.")
  public String LastUsedID() {
    Object[] COMPONENT_IDS = COMPONENTS.keySet().toArray();
    return (COMPONENT_IDS.length > 0 ? COMPONENT_IDS[COMPONENT_IDS.length - 1].toString() : "");
  }

  /**
    * Returns a random UUID.
    *
    * @return  A random UUID.
    */
  @SimpleFunction(description = "Generates a unique ID that can be used to create a component.")
  public String RandomUUID() {
    String uuid = "";

    do {
      uuid = UUID.randomUUID().toString();
    } while (internal.isIdTaken(uuid));

    return uuid;
  }

  /**
    * Returns a list of IDs of every created component.
    *
    * @return  A list of IDs.
    */
  @SimpleFunction(description = "Returns a list of IDs of every created component.")
  public YailList UsedIDs() {
    Set<String> keys = COMPONENTS.keySet();
    return YailList.makeList(keys);
  }

  /**
    * Returns the component itself for setting properties. ID must be a valid ID
    * which is added with Create block. ID --> Component
    *
    * @param id    ID of the desired component.
    * @return      A component bound to the given ID.
    */
  @SimpleFunction(description = "Returns the component itself for setting properties. ID must be a valid ID which is added with Create block.")
  public Object GetComponent(String id) {
    return COMPONENTS.get(id);
  }

  /**
    * Returns "true" if the specified component is created by Dynamic Components
    * extension. Otherwise, "false".
    *
    * @param component Component that needs to be analysed.
    * @return          A boolean value indicating whether the specified component is dynamically created or not.
    */
  @SimpleFunction(description = "Returns 'true' if component was created by the Dynamic Components extension. Otherwise, 'false'.")
  public Object IsDynamic(Component component) {
    return internal.isDynamicComponent(component);
  }

  /**
    * Returns the ID of the specified component. The desired component must created using the "Create" block, else it will return a blank string. Component --> ID
    *
    * @param component The component that its ID will be returned.
    * @return          ID of the specified component.
    */
  @SimpleFunction(description = "Returns the ID of the specified component. The desired component must created using the \"Create\" block, else it will return a blank string. Component --> ID")
  public String GetId(Component component) {
    // Getting key from value,
    for (String o : COMPONENTS.keySet()) {
      if (COMPONENTS.get(o).equals(component)) {
        return o;
      }
    }

    return "";
  }

  /**
    * Returns the internal name of any component or object.
    *
    * @param component The component that its internal name needs to be returned.
    * @return          Internal name of the specified component.
    */
  @SimpleFunction(description = "Returns the internal name of any component or object.")
  public String GetName(Object component) {
    return internal.getClass(component).getName();
  }

  /**
    * Sets the specified property of the component to the given value. It can be also
    * used to set properties that only exists in Designer. Supported values are:
    * "string", "boolean", "integer" and "float". For other values, you should use
    * "Any Component" blocks.
    *
    * @param component The component that property will set for.
    * @param name      The name of the property that needs to be set.
    * @param value     The value that needs to be set of the specified property.
    */
  @SimpleFunction(description = "Sets the specified property of the component to the given value. It can be also used to set properties that only exists in Designer. Supported values are; 'string', 'boolean', 'integer' and 'float'. For other values, you should use Any Component blocks.")
  public void SetProperty(Component component, String name, Object value) {
    // The method will be invoked.
    try {
      Invoke(component, name, YailList.makeList(new Object[] { value }));
    } catch (Exception exception) {
      throw new YailRuntimeError(exception.getMessage(), "Error");
    }
  }

  /**
    * Set multiple properties of a component by typing its property name and value.
    * It behaves like a Setter property block. It can be also used to set properties
    * that only exists in Designer. Supported values are; "string", "boolean",
    * "integer" and "float". For other values, you should use Any Component blocks.
    *
    * @param component         The component that property will set for.
    * @param properties        Properties and their respective values as a dictionary.
    * @throws JSONException    Creating new instance of JSONObject throws this exception.
    */
  @SimpleFunction(description = "Set multiple properties of a component at once.")
  public void SetProperties(Component component, YailDictionary properties) throws JSONException {
    JSONObject propertyObject = new JSONObject(properties.toString());
    JSONArray names = propertyObject.names();

    for (int i = 0; i < propertyObject.length(); i++) {
      String name = names.getString(i);
      Object value = propertyObject.get(name);
      Invoke(component, name, YailList.makeList(new Object[] { value }));
    }
  }

  /**
    * Returns the specified properties value of the given component. Can be known
    * as a Getter property block. It can be also used to get properties that only
    * exists in Designer.
    *
    * @param component The component that property will get from.
    * @param name      Name of the desired property.
    * @return          Value of the specified property of the specified component.
    */
  @SimpleFunction(description = "Returns the specified properties value of the given component. Can be known as a Getter property block. It can be also used to get properties that only exists in Designer.")
  public Object GetProperty(Component component, String name) {
    // The method will be invoked.
    try {
      return Invoke(component, name, YailList.makeEmptyList());
    } catch (Exception exception) {
      // Throw an error when something goes wrong.
      throw new YailRuntimeError("" + exception, "Error");
    }
  }

  /**
    * Invokes the specified method of the specified component.
    *
    * @param component     The component that method will be executed for.
    * @param name          Name of the method that needs to be invoked.
    * @param parameters    Parameters to pass to the specified method.
    * @return              Return value of the invoked method.
    */
  @SimpleFunction(description = "Invokes a method with parameters.")
  public Object Invoke(Component component, String name, YailList parameters) {
    // The method will be invoked.
    name = name.trim().replace(" ", "");

    try {
      if (component == null) {
        throw new YailRuntimeError("Component is not specified.", "Error");
      }

      Method method = internal.findMethod(internal.getClass(component).getMethods(), name.replace(" ", ""), parameters.toArray().length);

      if (method == null) {
        throw new YailRuntimeError("Method can't found with that name.", "Error");
      }

      Object[] typed_params = parameters.toArray();
      Class<?>[] requested_params = method.getParameterTypes();
      ArrayList<Object> params = new ArrayList<>();
      for (int i = 0; i < requested_params.length; i++) {
        if ("int".equals(requested_params[i].getName())) {
          // Integer
          params.add(Integer.parseInt(typed_params[i].toString()));
        } else if ("float".equals(requested_params[i].getName())) {
          // Float
          params.add(Float.parseFloat(typed_params[i].toString()));
        } else if ("double".equals(requested_params[i].getName())) {
          // Double
          params.add(Double.parseDouble(typed_params[i].toString()));
        } else if ("java.lang.String".equals(requested_params[i].getName())) {
          // String
          params.add(typed_params[i].toString());
        } else if ("boolean".equals(requested_params[i].getName())) {
          // Boolean
          params.add(Boolean.parseBoolean(typed_params[i].toString()));
        } else {
          params.add(typed_params[i]);
        }
      }

      Object m = method.invoke(component, params.toArray());
      if (m == null) {
        return "";
      } else {
        return m;
      }
    } catch (Exception exception) {
      throw new YailRuntimeError(exception.toString(), "Error");
    }
  }

  /**
    * Returns a JSON string with the information of the specified component containing
    * all of it's properties, events and methods.
    *
    * @param component         The component that information will be returned for.
    * @return                  Information of the specified component.
    * @throws JSONException    Thrown by "put()" method from the JSONObject class.
    */
  @SimpleFunction(description = "Gives the information of the specified component with all properties, events, methods as JSON text.")
  public String ListDetails(Component component) {
    Class componentClass = internal.getClass(component);
    DesignerComponent designerComponentAnnotation = internal.getAnnotation(DesignerComponent.class, component, null);
    JSONObject specifications = new JSONObject();

    try {
      // Alphabetical order
      specifications.put("androidMinSdk", designerComponentAnnotation.androidMinSdk());
      specifications.put("category", designerComponentAnnotation.category());
      specifications.put("dateBuilt", designerComponentAnnotation.dateBuilt());
      specifications.put("description", designerComponentAnnotation.description());
      specifications.put("helpUrl", designerComponentAnnotation.helpUrl());
      specifications.put("iconName", designerComponentAnnotation.iconName());
      specifications.put("name", componentClass.getSimpleName());
      specifications.put("nonVisible", designerComponentAnnotation.nonVisible());
      specifications.put("showOnPalette", designerComponentAnnotation.showOnPalette());
      specifications.put("type", componentClass.getName());
      specifications.put("version", designerComponentAnnotation.version());
      specifications.put("versionName", designerComponentAnnotation.versionName());
    } catch(JSONException e) {
      e.printStackTrace();
    }

    Method[] allMethods = componentClass.getMethods();
    JSONArray blockProperties = new JSONArray();
    JSONArray events = new JSONArray();
    JSONArray methods = new JSONArray();
    JSONArray properties = new JSONArray();

    // Get the component's class and return all methods from it.
    // Search for methods.
    for (Method mMethod : allMethods) {
      JSONObject data = new JSONObject();

      DesignerProperty designerPropertyAnnotation = internal.getAnnotation(DesignerProperty.class, null, mMethod);
      SimpleEvent simpleEventAnnotation = internal.getAnnotation(SimpleEvent.class, null, mMethod);
      SimpleFunction simpleFunctionAnnotation = internal.getAnnotation(SimpleFunction.class, null, mMethod);
      SimpleProperty simplePropertyAnnotation = internal.getAnnotation(SimpleProperty.class, null, mMethod);

      internal.putInJsonObject(data, "name", mMethod.getName());

      if (internal.methodHasAnnotation(DesignerProperty.class, mMethod)) {
        internal.putInJsonObject(data, "editorType", designerPropertyAnnotation.editorType());
        internal.putInJsonObject(data, "defaultValue", designerPropertyAnnotation.defaultValue());
        internal.putInJsonObject(data, "editorArgs", new JSONArray(Arrays.asList(designerPropertyAnnotation.editorArgs())));
        internal.putInJsonArray(properties, data);
      }

      if (internal.methodHasAnnotation(SimpleEvent.class, mMethod)) {
        internal.putInJsonObject(data, "description", simpleEventAnnotation.description());
        internal.putInJsonObject(data, "visible", simpleEventAnnotation.userVisible());

        JSONArray params = new JSONArray();
        for (Class<?> param : mMethod.getParameterTypes()) {
          internal.putInJsonArray(params, param.getName());
        }

        internal.putInJsonObject(data, "parameterTypes", params);
        // Missing: "deprecated"
        // Missing: "params"
      }

      if (internal.methodHasAnnotation(SimpleFunction.class, mMethod)) {
        internal.putInJsonObject(data, "description", simpleFunctionAnnotation.description());
        internal.putInJsonObject(data, "visible", simpleFunctionAnnotation.userVisible());
        internal.putInJsonObject(data, "returnType", mMethod.getReturnType().getSimpleName());
        
        JSONArray params = new JSONArray();
        for (Class<?> param : mMethod.getParameterTypes()) {
          internal.putInJsonArray(params, param.getName());
        }

        internal.putInJsonObject(data, "parameterTypes", params);
        // Missing: "deprecated"
        // Missing: "params"
      }

      if (internal.methodHasAnnotation(SimpleProperty.class, mMethod)) {
        internal.putInJsonObject(data, "category", simplePropertyAnnotation.category());
        internal.putInJsonObject(data, "description", simplePropertyAnnotation.description());
        internal.putInJsonObject(data, "visible", simplePropertyAnnotation.userVisible());

        String rw = "read-write";

        boolean setter = internal.findMethod(allMethods, mMethod.getName(), 1) != null;
        boolean getter = internal.findMethod(allMethods, mMethod.getName(), 0) != null;

        if (getter && !setter) {
          rw = "read-only";
          internal.putInJsonObject(data, "type", Objects.requireNonNull(internal.findMethod(allMethods, mMethod.getName(), 0)).getReturnType().getSimpleName());
        } else {
          rw = (setter && !getter ? "write-only" : "read-write");
          internal.putInJsonObject(data, "type", Objects.requireNonNull(internal.findMethod(allMethods, mMethod.getName(), 1)).getParameterTypes()[0].getSimpleName());
        }

        boolean isDeprecated = simplePropertyAnnotation.category() == PropertyCategory.DEPRECATED;
        internal.putInJsonObject(data, "rw", rw);
        internal.putInJsonObject(data, "deprecated", isDeprecated);

        if (simplePropertyAnnotation.category() != PropertyCategory.UNSET) {
          internal.putInJsonArray(blockProperties, data);
        }
      }
    }

    internal.putInJsonObject(specifications, "blockProperties", blockProperties);
    internal.putInJsonObject(specifications, "events", events);
    internal.putInJsonObject(specifications, "methods", methods);
    internal.putInJsonObject(specifications, "properties", properties);

    return specifications.toString();
  }

  /**
    * Returns the version number of the extension.
    *
    * @return Version number of the extension.
    */
  @SimpleProperty(description = "Returns the version number of the extension.")
  public int Version() {
    return DynamicComponents.class.getAnnotation(DesignerComponent.class).version();
  }

  /**
    * Returns the version name of the extension.
    *
    * @return  Version name of the extension.
    */
  @SimpleProperty(description = "Returns the extension version name.")
  public String VersionName() {
    return DynamicComponents.class.getAnnotation(DesignerComponent.class).versionName();
  }

  protected class Internal {
    public Internal() {}

    public String checkId(Object componentName, String id) {
      // Check if ID is used by another Dynamic Component
      if (id == null || id.trim().isEmpty()) {
        throw new YailRuntimeError("DynamicComponents-AI2: ID can't be blank.", "Invalid ID");
      } else {
        if (internal.isIdTaken(id)) {
          throw new YailRuntimeError("DynamicComponents-AI2: ID must be unique for all components.", "Duplicate ID");
        }
      }

      return internal.getClassName(componentName);
    }

    public void createBySchema(AndroidViewComponent in, int i, JSONArray properties) {
      if (!properties.getJSONObject(i).has("in")) {
        // If a component JSONObject doesn't contain an "in" key then insert it in the main // component that specified as this method's "in" parameter.
        Create(in, properties.getJSONObject(i).getString("type"), properties.getJSONObject(i).getString("id"));
      } else {
        // Else, insert it in the another component that is specified with an ID.
        Create((AndroidViewComponent) GetComponent(properties.getJSONObject(i).getString("in")), properties.getJSONObject(i).getString("type"), properties.getJSONObject(i).getString("id"));
      }
    }

    public Method findMethod(Method[] methods, String name, Integer paramCount) {
      for (Method method : methods) {
        // Check for one parameter (setter) method.
        if ((method.getName().equals(name.trim())) && (method.getParameterTypes().length == paramCount)) {
          return method;
        }
      }

      return null;
    }

    public Class<?> getClass(Object instance) {
      return instance.getClass();
    }

    public String getClassName(Object componentName) {
      if ((componentName instanceof String) && componentName.toString().contains(".")) {
        // Is the componentName a String with package name?
        return componentName.toString().replace(" ", "");
      } else if (componentName instanceof String) {
        // Is the componentName a String with no pacakge name?
        return BASE_PACKAGE + "." + componentName.toString().replace(" ", "");
      } else if (componentName instanceof Component) {
        // Is the componentName a Component instance?
        return componentName.getClass().getName();
      } else {
        // Throw an error if componentName is neither Component or String
        throw new YailRuntimeError("DynamicComponents-AI2: Not a Component block or a String.", "Invalid Component");
      }
    }

    public <T extends Annotation> T getAnnotation(Class<T> mClass, Component component, Method mMethod) {
      if (component != null) {
        return internal.getClass(component).getAnnotation(mClass);
      }

      return mMethod.getAnnotation(mClass);
    }

    public boolean methodHasAnnotation(Class<? extends Annotation> mClass, Method mMethod) {
      return mMethod.isAnnotationPresent(mClass);
    }

    public boolean isDynamicComponent(Component component) {
      return COMPONENTS.containsValue(component);
    }

    public boolean isIdTaken(String id) {
      return COMPONENTS.containsKey(id);
    }

    public void parseJson(String id, JSONObject json) throws JSONException {
      JSONObject data = new JSONObject(json.toString());
      String KEY = "components";

      data.remove(KEY);

      if (!"".equals(id)) {
        data.put("in", id);
      }

      PROPERTIESARRAY.put(data);

      if (json.has(KEY)) {
        for (int i = 0; i < json.getJSONArray(KEY).length(); i++) {
          parseJson(data.optString("id", ""), json.getJSONArray(KEY).getJSONObject(i));
        }
      }
    }

    public void putInJsonArray(JSONArray json, Object value) {
      try {
        json.put(value);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    public void putInJsonObject(JSONObject json, String key, Object value) {
      try {
        json.put(key, value);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    public String replaceKeys(JSONObject json, YailList parameters, String template) {
      String modified = template;

      // Check if JSON contains "keys".
      if (json.has("keys")) {
        // Throw a runtime error if parameter count is lower than required parameter count.
        if (json.optJSONArray("keys").length() > parameters.length()) {
          throw new YailRuntimeError("Input parameter count is lower than the requirement!", "Error");
        } else {
          /* Replace the template keys with their values.
            * For example;
            * {0} --> "a value" */
          for (int i = 0; i < json.optJSONArray("keys").length(); i++) {
            modified = modified.replace("{" + json.optJSONArray("keys").getString(i) + "}", parameters.getString(i).replace("\"", ""));
          }
        }
      }

      return modified;
    }

    public void setPropertiesBySchema(int i, JSONArray properties) {
      JSONArray keys = properties.getJSONObject(i).getJSONObject("properties").names();

      for (int k = 0; k < keys.length(); k++) {
        Invoke(
          (Component) GetComponent(properties.getJSONObject(i).getString("id")),
          keys.getString(k),
          YailList.makeList(new Object[] { properties.getJSONObject(i).getJSONObject("properties").get(keys.getString(k)) })
        );
      }
    }
  }
}