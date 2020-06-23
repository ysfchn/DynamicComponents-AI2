package com.yusufcihan.DynamicComponents;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.YailList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

@DesignerComponent(version = 4,
        description = "Dynamic Components extension to create any type of dynamic component in any arrangement.<br><br>- by Yusuf Cihan",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "https://yusufcihan.com/img/dynamiccomponents.png")
@SimpleObject(external = true)
public class DynamicComponents extends AndroidNonvisibleComponent implements Component {

    // ------------------------
    //       VARIABLES
    // ------------------------

    /* 
        -----------------------
        Hashtable<String, Component> COMPONENTS

        Contains the created components. Key is the ID of the components, and their values are the components
        that created with Create block.

        -----------------------
    */
    private Hashtable<String, Component> COMPONENTS = new Hashtable<String, Component>();

    /* 
        -----------------------
        String BASE_PACKAGE

        Specifies the base package for creating the components.

        -----------------------
    */
    private String BASE_PACKAGE = "com.google.appinventor.components.runtime";

    /* 
        -----------------------
        String LAST_ID

        Stores the last ID that created with the Create block.

        -----------------------
    */
    private String LAST_ID = "";

    /* 
        -----------------------
        JSONArray PROPERTIESARRAY

        Stores the component template. Needs to be cleared before rendering Schema operation.

        -----------------------
    */
    private JSONArray PROPERTIESARRAY = new JSONArray();

    public DynamicComponents(ComponentContainer container) {
        super(container.$form());
    }

    /*
    private String BasePackage() {
        return BASE_PACKAGE;
    }

    private void BasePackage(String packageName) {
        BASE_PACKAGE = packageName;
    }
    */

    // ------------------------
    //         EVENTS
    // ------------------------    


    /* 
        -----------------------
        SchemaCreated

        Raises after Schema has been created with Schema block.

        -----------------------
    */
    @SimpleEvent(description = "Raises after Schema has been created with Schema block.")
	public void SchemaCreated() {
		EventDispatcher.dispatchEvent(this, "SchemaCreated");
	}



    // ------------------------
    //       MAIN METHODS
    // ------------------------


    /* 
        -----------------------
        Create

        Creates a new dynamic component. It supports all component that added to your current AI2 builder.
        In componentName, you can type the component's name like "Button", 
        or you can pass a static component then it can create a new instance of it.


        -- Parameters --
        AndroidViewComponent in        : To specify where component will be created in.
        Object componentName           : Name of the component like "Button" or add a static component block.
        String id                      : ID of the component to create 

        -----------------------
    */
    @SimpleFunction(description =
            "Creates a new dynamic component. It supports all component that added to your current AI2 builder.\n"
                    + "In componentName, you can type the component's name like 'Button',\n"
                    + "or you can pass a static component then it can create a new instance of it.")
    public void Create(AndroidViewComponent in, Object componentName, String id) {
        Component component = null;
        LAST_ID = id;
        String error = null;
        // Check if id is used by another created dynamic component.
        if (!COMPONENTS.containsKey(id)) {
            try {
                // If input is a component name then create a instance of it.
                if (componentName instanceof String) {
                    // Return the component class by looking the its name.
                    Class<?> clasz = Class.forName(BASE_PACKAGE + "." + componentName.toString().replace(" ", ""));
                    // Create constructor object for creating a new instance.
                    Constructor<?> constructor = clasz.getConstructor(new Class[]{ComponentContainer.class});
                    // Create a new instance of specified component.
                    component = (Component) constructor.newInstance((ComponentContainer) in);
                // If input is a component's itself, then create a new component from itself.
                } else if (componentName instanceof Component) {
                    Class<?> clasz = Class.forName(componentName.getClass().getName());
                    Constructor<?> constructor = clasz.getConstructor(new Class[]{ComponentContainer.class});
                    component = (Component) constructor.newInstance((ComponentContainer) in);
                } else {
                    error = "Input is not a component block or a component name.";
                }
            } catch (Exception exception) {
                error = "" + exception;
            }
        } else {
            error = "This ID is already used for another component, please pick another. ID needs to be unique for all components!";
        }

        if (id == null || id.trim().isEmpty()) {
            error = "ID is blank. Please enter a valid ID.";
        }

        if (error != null) {
            throw new YailRuntimeError(error, "DynamicComponents-AI2 Error");
        } else {
            COMPONENTS.put(id, component);
        }
    }


    /* 
        -----------------------
        Schema

        Imports a JSON string that is a template for creating the dynamic components
        automatically with single block. Templates can also contain parameters that will be
        replaced with the values which defined in the "parameters" list.


        -- Parameters --
        AndroidViewComponent in        : To specify where base component will be created in.
        String template                : Template source as plain JSON text
        YailList parameters            : Parameters that will be replaced in template text.

        -----------------------
    */
    @SimpleFunction(description =
            "Imports a JSON string that is a template for creating the dynamic components\n" +
            "automatically with single block. Templates can also contain parameters that will be\n" +
            "replaced with the values which defined in the 'parameters' list.")
    public void Schema(AndroidViewComponent in, String template, YailList parameters) {
        try {
            // Remove the contents of the array by creating a new JSONArray.
            PROPERTIESARRAY = new JSONArray();
            // Create a JSONObject from template for checking.
            JSONObject j = new JSONObject(template);
            // Save the template string to a new variable for editing.
            String modifiedTemplate = template;
            // Check if JSON contains "keys".
            if (j.has("keys")) {
                // Throw a runtime error if parameter count is lower than required parameter count.
                if (j.optJSONArray("keys").length() > parameters.length())
                {
                    throw new YailRuntimeError("Input parameter count is lower than the requirement!", "Error");
                }
                else
                {
                    // Replace the template keys with their values.
                    // For example;
                    // {0} --> "a value" 
                    for (int i = 0; i < j.optJSONArray("keys").length(); i++) {
                        modifiedTemplate = modifiedTemplate.replace("{" + j.getJSONArray("keys").getString(i) + "}", parameters.getString(i).replace("\"", ""));
                    }
                }
            }

            // Check the metadata version for checking compatibility for next/previous versions of the extension. 
            // Will be used in the future releases.
            if (j.optInt("metadata-version", 0) == 0)
                throw new YailRuntimeError("Metadata version is not specified!", "Error");
            // Lastly parse the JSONObject.
            Parse(new JSONObject(modifiedTemplate), "");
            // Delete the first element, because it contains metadata instead of components.
            PROPERTIESARRAY.remove(0);

            // Start creating the extensions (finally).
            for (int i = 0; i < PROPERTIESARRAY.length(); i++) {
                // Check if component has an ID key.
                if (!PROPERTIESARRAY.getJSONObject(i).has("id"))
                {
                    throw new YailRuntimeError("One or more of the components has not an ID in template!", "Error");
                }

                // If a component JSONObject doesn't contain an "in" key then insert it in the main component
                // that specified as this method's "in" parameter. 
                if (!PROPERTIESARRAY.getJSONObject(i).has("in"))
                {
                    Create(in, PROPERTIESARRAY.getJSONObject(i).getString("type"), PROPERTIESARRAY.getJSONObject(i).getString("id"));
                }
                // Else, insert it in the another component that is specified with an ID.
                else
                {
                    Create((AndroidViewComponent)GetComponent(PROPERTIESARRAY.getJSONObject(i).getString("in")), PROPERTIESARRAY.getJSONObject(i).getString("type"), PROPERTIESARRAY.getJSONObject(i).getString("id"));
                }

                // If JSONObject contains a "properties" section, then set its properties with
                // SetProperty block.
                if (PROPERTIESARRAY.getJSONObject(i).has("properties"))
                {
                    JSONArray keys = PROPERTIESARRAY.getJSONObject(i).getJSONObject("properties").names();

                    for (int k = 0; k < keys.length(); k++) {
                        SetProperty(
                            (Component)GetComponent(PROPERTIESARRAY.getJSONObject(i).getString("id")), 
                            keys.getString(k), 
                            PROPERTIESARRAY.getJSONObject(i).getJSONObject("properties").get(keys.getString(k))
                        );
                    }
                }
            }
            SchemaCreated();
        
        } catch (Exception e) {
            throw new YailRuntimeError(e.getMessage(), "Error");
        }
    }


    /* 
        -----------------------
        ChangeId

        Changes ID of one of created components to a new one. 
        The old ID must be exist and new ID mustn't exist.


        -- Parameters --
        String id                      : The old ID that will be changed.
        String newId                   : The new ID that old ID will be changed to.

        -----------------------
    */
    @SimpleFunction(description = "Changes ID of one of created components to a new one. The old ID must be exist and new ID mustn't exist.")
    public void ChangeId(String id, String newId) {
        if (COMPONENTS.containsKey(id) && !COMPONENTS.containsKey(newId)) {
            Component component = COMPONENTS.remove(id);
            COMPONENTS.put(newId, component);
        } else {
            throw new YailRuntimeError("Old ID must exist and new ID mustn't exist.", "Error");
        }
    }


    /* 
        -----------------------
        Remove

        Removes the component with specified ID from screen/layout and the component list. 
        So you will able to use its ID again as it will be deleted.


        -- Parameters --
        String id                      : The old ID that will be changed.
        String newId                   : The new ID that old ID will be changed to.

        -----------------------
    */
    @SimpleFunction(description = "Removes the component with specified ID from screen/layout and the component list.\n" +
                                  "So you will able to use its ID again as it will be deleted.")
    public void Remove(String id) {
        // Don't do anything if id is not in the components list.
        if (COMPONENTS.containsKey(id)) {
            // Get the component.
            Object cmp = COMPONENTS.get(id);
            try {
                if (cmp != null) {
                    Method method = cmp.getClass().getMethod("Visible", boolean.class);
                    method.invoke(cmp, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Remove its id from components list.
            COMPONENTS.remove(id);
        }
    }

    
    /* 
        -----------------------
        LastUsedID

        Returns last used ID by Create block.

        -----------------------
    */
    @SimpleFunction(description = "Returns last used ID by Create block.")
    public String LastUsedID() {
        return LAST_ID;
    }


    /* 
        -----------------------
        UsedIDs

        Returns all used IDs in the created components list.

        -----------------------
    */
    @SimpleFunction(description = "Returns all used IDs in the created components list.")
    public YailList UsedIDs() {
        Set<String> keys = COMPONENTS.keySet();
        return YailList.makeList(keys);
    }

    
    /* 
        -----------------------
        GetComponent

        Returns the component's itself for setting properties. 
        ID must be a valid ID which is added with Create block.
        ID --> Component


        -- Parameters --
        String id                      : The ID of the component.

        -----------------------
    */
    @SimpleFunction(description = "Returns the component's itself for setting properties. ID must be a valid ID which is added with Create block.")
    public Object GetComponent(String id) {
        return COMPONENTS.get(id);
    }


    /* 
        -----------------------
        GetID

        Returns the ID of component. Component needs to be created by Create block. 
        Otherwise it will return blank string. Also known as reverse of the GetComponent block.
        Component --> ID


        -- Parameters --
        Component component            : The component that has an ID.

        -----------------------
    */
    @SimpleFunction(description = "Returns the ID of component. Component needs to be created by Create block.\n" + 
                                  "Otherwise it will return blank string.")
    public String GetId(Component component) {
        return getKeyFromValue(COMPONENTS, component);
    }


    /* 
        -----------------------
        GetName

        Returns the internal name of any component or object.


        -- Parameters --
        Component component            : The component that its name will be returned.

        -----------------------
    */
    @SimpleFunction(description = "Returns the internal name of any component or object.")
    public String GetName(Object component) {
        return component.getClass().getName().replace(BASE_PACKAGE + ".", "");
    }


    /* 
        -----------------------
        SetProperty

        Set a property of a component by typing its property name. It behaves like a Setter property block.
        It can be also used to set properties that only exists in Designer. 
        Supported values are; "string", "boolean", "integer" and "float". For other values, you should use
        Any Component blocks.


        -- Parameters --
        Component component            : The component that will be modified.
        String name                    : Name of the property.
        String value                   : Value of the property.

        -----------------------
    */
    @SimpleFunction(description = "Set a property of a component by typing its property name. It behaves like a Setter property block.\n" +
                                  "It can be also used to set properties that only exists in Designer. Supported values are;\n" +
                                  "'string', 'boolean', 'integer' and 'float'. For other values, you should use Any Component blocks.")
    public void SetProperty(Component component, String name, Object value) {
        // The method will be invoked.
        try {
            if (component == null)
                throw new YailRuntimeError("Component is not specified.", "Error");

            Method method = findMethod(component.getClass().getMethods(), name, 1);
            // Method m = component.getClass().getMethod(name, value.getClass());
            if (method == null)
                throw new YailRuntimeError("Property can't found with that name.", "Error");

            String outputName = method.getParameterTypes()[0].getName().toString().trim();
            String inputName = value.getClass().getName().toString().trim();
            String v = "";

            // Parse the value and save it in a variable.
            if ("gnu.math.IntNum".equals(inputName)) {
                v = Integer.toString(((gnu.math.IntNum) value).intValue());
            } else if ("gnu.math.DFloNum".equals(inputName)) {
                v = Double.toString(((gnu.math.DFloNum) value).doubleValue());
            } else {
                v = value.toString();
            }

            // Check for requested parameter type.
            switch (outputName) {
                case "int":
                    method.invoke(component, Integer.parseInt(v));
                    break;
                case "double":
                    method.invoke(component, Double.parseDouble(v));
                    break;
                case "float":
                    method.invoke(component, Float.parseFloat(v));
                    break;
                default:
                    method.invoke(component, Class.forName(value.getClass().getName()).cast(value));
                    break;
            }
        } catch (Exception exception) {
            throw new YailRuntimeError(exception.getMessage(), "Error");
        }
    }


    /* 
        -----------------------
        GetProperty

        Get a property value of a component by typing its property name. It behaves like a Getter property block.
        It can be also used to get properties that only exists in Designer. 


        -- Parameters --
        Component component            : The component that property value will get from.
        String name                    : Name of the property.

        -----------------------
    */
    @SimpleFunction(description = "Get a property value of a component by typing its property name. It behaves like a Getter property block.\n" + 
                                  "It can be also used to get properties that only exists in Designer.")
    public Object GetProperty(Component component, String name) {
        // The method will be invoked.
        try {
            if (component == null)
                throw new YailRuntimeError("Component is not specified.", "Error");

            Method method = findMethod(component.getClass().getMethods(), name, 0);

            if (method == null)
                throw new YailRuntimeError("Property can't found with that name.", "Error");
            // Invoke the saved method and return its return value.
            return method.invoke(component);
        } catch (Exception exception) {
            // Throw an error when something goes wrong.
            throw new YailRuntimeError("" + exception, "Error");
        }
    }


    /* 
        -----------------------
        GetDesignerProperties

        Get all available properties of a component which can be set from Designer as list along with types. 
        Can be used to learn the properties of any component which is not static.
        Property values and names are joined with --- separator.


        -- Parameters --
        Component component            : The component that property values will be fetched.

        -----------------------
    */
    @SimpleFunction(description = "Get all available properties of a component which can be set from Designer as list along with types.\n" + 
                                  "Can be used to learn the properties of any component which is not static.\n" +
                                  "Property values and names are joined with --- separator.")
    public YailList GetDesignerProperties(Component component) {
        // A list which includes designer properties.
        List<String> properties = new ArrayList<>();
        // Get the component's class and return all methods from it.
        Method[] methods = component.getClass().getMethods();
        for (Method mtd : methods) {
            // Read for @DesignerProperty annotations.
            // So we can learn which method is used as property setter/getter.
            if ((mtd.getDeclaredAnnotations().length == 2) && (mtd.isAnnotationPresent(DesignerProperty.class))) {
                // Get the DesignerProperty annotation.
                DesignerProperty n = mtd.getAnnotation(DesignerProperty.class);
                // Add editorType value and method name to the list.
                properties.add(mtd.getName() + "---" + n.editorType());
            }
        }
        // Return the list.
        return YailList.makeList(properties);
    }



    // ------------------------
    //      PRIVATE METHODS
    // ------------------------

    // Get all available methods from a component.
    /*
    @SimpleFunction(description = "Get all available methods from a component.")
    private YailList GetMethods(Component component) {
        // A list which includes designer properties.
        List<String> names = new ArrayList<>();
        for (Method method : component.getClass().getMethods()) {
            names.add(method.getName());
        }
        // Return the list.
        return YailList.makeList(names);
    }
    */

    // Getting key from value, source: http://www.java2s.com/Code/Java/Collections-Data-Structure/GetakeyfromvaluewithanHashMap.htm
    public String getKeyFromValue(Hashtable<String, Component> hm, Object value) {
        for (String o : hm.keySet()) {
            if (hm.get(o).equals(value)) {
                return (String) o;
            }
        }
        return "";
    }

    // Finds a method in method list by checking the name and parameter count.
    private Method findMethod(Method[] methods, String name, Integer paramCount) {
        for (Method method : methods) {
            // Check for one parametered (setter) method.
            if ((method.getName().equals(name.trim())) && (method.getParameterTypes().length == paramCount)) {
                return method;
            }
        }
        return null;
    }

    private void Parse(JSONObject js, String id) throws JSONException {
        JSONObject data = new JSONObject(js.toString());
        data.remove("components");
        if (!"".equals(id))
            data.put("in", id);
        PROPERTIESARRAY.put(data);
        if (js.has("components")) {
            for (int i = 0; i < js.getJSONArray("components").length(); i++) {
                Parse(js.getJSONArray("components").getJSONObject(i), data.optString("id", ""));
            }
        }
    }
}
