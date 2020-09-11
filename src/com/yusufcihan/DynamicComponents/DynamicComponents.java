package com.yusufcihan.DynamicComponents;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.YailDictionary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import java.util.Arrays;

// Used to cast value to known value in Invoke() method.
import gnu.math.IntNum;
import gnu.math.DFloNum;
import gnu.lists.FString;

@DesignerComponent(
    description = "Dynamic Components is an extension that supports every component in your App Inventor distribution, instead of having pre-defined components that was made with &#x2764;&#xfe0f; by Yusuf Cihan",
    category = ComponentCategory.EXTENSION,
    helpUrl = "https://github.com/ysfchn/DynamicComponents-AI2",
    iconName = "https://ysfchn.com/img/dynamiccomponents.png",
    nonVisible = true,
    version = 6,
    versionName = "2.1.0"
)
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

    // ------------------------
    //         EVENTS
    // ------------------------    


    /* 
        -----------------------
        SchemaCreated

        Raises after a schema has been created using the Schema block.

        -----------------------
    */
    @SimpleEvent(description = "Raises after a schema has been created using the Schema block.")
	public void SchemaCreated(String name, YailList parameters) {
		EventDispatcher.dispatchEvent(this, "SchemaCreated", name, parameters);
    }

    /* 
        -----------------------
        ComponentCreated

        Raises after a component has been created using the Create block.

        -----------------------
    */
    @SimpleEvent(description = "Raises after a component has been created using the Create block.")
	public void ComponentCreated(String id) {
		EventDispatcher.dispatchEvent(this, "ComponentCreated", id);
    }
    



    // ------------------------
    //       MAIN METHODS
    // ------------------------


    /* 
        -----------------------
        Create

        Creates a new dynamic component. It supports all component that added to your current AI2 distribution.
        In componentName, you can type the component's name like "Button", 
        or you can pass a static component then it can create a new instance of it.


        -- Parameters --
        AndroidViewComponent in        : To specify where component will be created in.
        Object componentName           : Name of the component like "Button" or add a static component block.
        String id                      : ID of the component to create 

        -----------------------
    */
    @SimpleFunction(description = "Creates a new dynamic component. It supports all component that added to your current AI2 distribution. In componentName, you can type the component's name like 'Button', or you can pass a static component then it can create a new instance of it, or just type the full class name of component.")
    public void Create(AndroidViewComponent in, Object componentName, String id) {
        // Variables
        String className = "";

        // Check if ID is used by another created dynamic component.
        if (COMPONENTS.containsKey(id))
            throw new YailRuntimeError("Duplicate ID: ID needs to be unique for all components", "DynamicComponents-AI2 Error");

        // Check if ID is blank/empty.
        if (id == null || id.trim().isEmpty())
            throw new YailRuntimeError("Invalid ID: ID can't be blank.", "DynamicComponents-AI2 Error");
            
        // If input is a full component class name, then just use it.
        if ((componentName instanceof String) && componentName.toString().contains(".")) {
            className = componentName.toString().replace(" ", "");
        // If input is a component name then append "com.google.appinventor.components.runtime" to the start.
        } else if (componentName instanceof String) {
            className = BASE_PACKAGE + "." + componentName.toString().replace(" ", "");
        // If input is a component block, then get the class name of it.                
        } else if (componentName instanceof Component) {
            className = componentName.getClass().getName();
        // Return an error if the input is not of these.
        } else {
            throw new YailRuntimeError("Invalid Component: Not a Component block or a String.", "DynamicComponents-AI2 Error");
        }

        // Try to create the component.
        try {
            if (!"".equals(className)) {
                // Create a Class object from class name.
                Class<?> clasz = Class.forName(className.trim().replace(" ", ""));
                // Create constructor object for creating a new instance.
                Constructor<?> constructor = clasz.getConstructor(new Class[]{ComponentContainer.class});
                // Create a new instance of specified component.
                Component component = (Component) constructor.newInstance((ComponentContainer) in);
                // Save the ID to LAST_ID variable.
                LAST_ID = id;
                // Save the component.
                COMPONENTS.put(id, component);
                // Finalize component creation
                ComponentCreated(id);
            }
        } catch (Exception exception) {
            throw new YailRuntimeError(exception.toString(), "DynamicComponents-AI2 Error");
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
    @SimpleFunction(description = "Imports a JSON string that is a template for creating the dynamic components automatically with single block. Templates can also contain parameters that will be replaced with the values which defined in the 'parameters' list.")
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
                // Invoke block.
                if (PROPERTIESARRAY.getJSONObject(i).has("properties"))
                {
                    JSONArray keys = PROPERTIESARRAY.getJSONObject(i).getJSONObject("properties").names();

                    for (int k = 0; k < keys.length(); k++) {
                        Invoke(
                            (Component)GetComponent(PROPERTIESARRAY.getJSONObject(i).getString("id")), 
                            keys.getString(k), 
                            YailList.makeList(new Object[] { PROPERTIESARRAY.getJSONObject(i).getJSONObject("properties").get(keys.getString(k)) })
                        );
                    }
                }
            }
            SchemaCreated(j.optString("name"), parameters);
        
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
        Move

        Moves the component to an another arrangement.


        -- Parameters --
        AndroidViewComponent arrangement : The arrangement that component placed in.
        Component component            : The component that will be reordered.
        int index                      : The index that specifies the position.

        -----------------------
    */
    @SimpleFunction(description = "Moves the component to an another arrangement.")
    public void Move(AndroidViewComponent arrangement, AndroidViewComponent component) {
        View comp = (View)component.getView();
        ViewGroup source = (ViewGroup)comp.getParent();
        source.removeView(comp);

        ViewGroup vg2 = (ViewGroup)arrangement.getView();
        ViewGroup target = (ViewGroup)vg2.getChildAt(0);

        target.addView(comp);
    }


    /* 
        -----------------------
        GetOrder

        Gets the position of the component according to its parent arrangement.
        Index starts from 1.


        -- Parameters --
        AndroidViewComponent component : The visible component.

        -----------------------
    */
    @SimpleFunction(description = "Gets the position of the component according to its parent arrangement. Index starts from 1.")
    public int GetOrder(AndroidViewComponent component){
        if ((component.getView() != null) && ((View)component.getView().getParent() != null))
        {
            View v = (View)component.getView();
            ViewGroup vg = (ViewGroup)v.getParent();
            int index = vg.indexOfChild(v);
            return index + 1;
        }
        else
        {
            return 0;
        }
    }


    /* 
        -----------------------
        SetOrder

        Sets the position of the component according to its parent arrangement.
        Index starts from 1.
        Typing 0 (zero) will move the component to the end.


        -- Parameters --
        AndroidViewComponent component : The component that will be reordered.
        int index                      : The index that specifies the position.

        -----------------------
    */
    @SimpleFunction(description = "Sets the position of the component according to its parent arrangement. Index starts from 1. Typing 0 (zero) will move the component to the end.")
    public void SetOrder(AndroidViewComponent component, int index) {
        View comp = (View)component.getView();
        ViewGroup source = (ViewGroup)comp.getParent();
        source.removeView(comp);

        // ViewGroup target = (ViewGroup)source.getChildAt(0);
        int i = index - 1;
        int childCount = source.getChildCount();

        if (i > childCount)
            i = childCount;
        source.addView(comp, i);
    }


    /* 
        -----------------------
        Remove

        Removes the component with specified ID from screen/layout and the component list. 
        So you will able to use its ID again as it will be deleted.


        -- Parameters --
        String id                      : The ID of the component that will be deleted.

        -----------------------
    */
    @SimpleFunction(description = "Removes the component with specified ID from screen/layout and the component list so you can use its ID again after it's deleted.")
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

        Returns the last created component's ID by Create block.

        -----------------------
    */
    @SimpleFunction(description = "Returns the last created component's ID by Create block.")
    public String LastUsedID() {
        return LAST_ID;
    }


    /* 
        -----------------------
        RandomUUID

        Makes a random unique UUID. Use this block in Create block if component ID is not required for you.

        -----------------------
    */
    @SimpleFunction(description = "Makes a random unique UUID. Use this block in Create block if component ID is not required for you.")
    public String RandomUUID() {
        String uuid = "";
        do {
            uuid = UUID.randomUUID().toString();
        }
        while (COMPONENTS.containsKey(uuid));
        return uuid;
    }


    /* 
        -----------------------
        UsedIDs

        Returns all used IDs of current components as App Inventor list.

        -----------------------
    */
    @SimpleFunction(description = "Returns all used IDs of current components as App Inventor list.")
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
        IsDynamic

        Returns "true" if component has created by Dynamic Components
        extension. Otherwise, "false".


        -- Parameters --
        Component component            : The component that will be checked.

        -----------------------
    */
    @SimpleFunction(description = "Returns 'true' if component has created by Dynamic Components extension. Otherwise, 'false'.")
    public Object IsDynamic(Component component) {
        return COMPONENTS.containsValue(component);
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
    @SimpleFunction(description = "Returns the ID of component. Component needs to be created by Create block. Otherwise it will return blank string.")
    public String GetId(Component component) {
        // Getting key from value,
        // Source: http://www.java2s.com/Code/Java/Collections-Data-Structure/GetakeyfromvaluewithanHashMap.htm
        for (String o : COMPONENTS.keySet()) {
            if (COMPONENTS.get(o).equals(component)) {
                return (String) o;
            }
        }
        return "";
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
        return component.getClass().getName();
    }


    /* 
        -----------------------
        SetProperty

        Set a property of a component by typing its property name. Can be known as a Setter property block.
        It can be also used to set properties that only exists in Designer. 
        Supported values are; "string", "boolean", "integer" and "float". For other values, you should use
        Any Component blocks.


        -- Parameters --
        Component component            : The component that will be modified.
        String name                    : Name of the property.
        String value                   : Value of the property.

        -----------------------
    */
    @SimpleFunction(description = "Set a property of a component by typing its property name. It behaves like a Setter property block. It can be also used to set properties that only exists in Designer. Supported values are; 'string', 'boolean', 'integer' and 'float'. For other values, you should use Any Component blocks.")
    public void SetProperty(Component component, String name, Object value) {
        // The method will be invoked.
        try {
            Invoke(component, name, YailList.makeList(new Object[] { value }));
        } catch (Exception exception) {
            throw new YailRuntimeError(exception.getMessage(), "Error");
        }
    }


    /* 
        -----------------------
        SetProperties

        Set multiple properties of a component by typing its property name and value. It behaves like a Setter property block. It can be also used to set properties that only exists in Designer. Supported values are; "string", "boolean", "integer" and "float". For other values, you should use "Any Component" blocks.


        -- Parameters --
        Component component - The component that will be modified.
        Dictionary properties - Names and values of the properties.

        -----------------------
    */
    @SimpleFunction(description = "Set multiple properties of a component at once.")
    public void SetProperties(Component component, YailDictionary properties) {
        JSONObject propertyObject = new JSONObject(properties.toString());
        JSONArray names = propertyObject.names();

        for (int i = 0; i < propertyObject.length(); i++) {
            String name = names.getString(i);
            Object value = propertyObject.get(name);
            Invoke(component, name, YailList.makeList(new Object[] { value }));
        }
    }


    /* 
        -----------------------
        GetProperty

        Get a property value of a component by typing its property name. Can be known as a Getter property block.
        It can be also used to get properties that only exists in Designer. 


        -- Parameters --
        Component component            : The component that property value will get from.
        String name                    : Name of the property.

        -----------------------
    */
    @SimpleFunction(description = "Get a property value of a component by typing its property name. Can be known as a Getter property block. It can be also used to get properties that only exists in Designer.")
    public Object GetProperty(Component component, String name) {
        // The method will be invoked.
        try {
            return Invoke(component, name, YailList.makeEmptyList());
        } catch (Exception exception) {
            // Throw an error when something goes wrong.
            throw new YailRuntimeError("" + exception, "Error");
        }
    }


    /* 
        -----------------------
        Invoke

        Invokes a method with parameters.


        -- Parameters --
        Component component            : The component that will be modified.
        String name                    : Name of the method.
        YailList parameters            : Parameters.

        -----------------------
    */
    @SimpleFunction(description = "Invokes a method with parameters.")
    public Object Invoke(Component component, String name, YailList parameters) {
        // The method will be invoked.
        try {
            if (component == null)
                throw new YailRuntimeError("Component is not specified.", "Error");

            Method method = findMethod(component.getClass().getMethods(), name.replace(" ", ""), parameters.toArray().length);
            // Method m = component.getClass().getMethod(name, value.getClass());
            if (method == null)
                throw new YailRuntimeError("Method can't found with that name.", "Error");

            Object[] typed_params = parameters.toArray();
            Class<?>[] requested_params = method.getParameterTypes();
            ArrayList<Object> params = new ArrayList<Object>();
            for (int i = 0; i < requested_params.length; i++)
            {
                if ("int".equals(requested_params[i].getName()))
                {
                    params.add(Integer.parseInt(typed_params[i].toString()));
                }
                else if ("float".equals(requested_params[i].getName()))
                {
                    params.add(Float.parseFloat(typed_params[i].toString()));
                }
                else if ("double".equals(requested_params[i].getName()))
                {
                    params.add(Double.parseDouble(typed_params[i].toString()));
                }
                else
                {
                    params.add(typed_params[i]);
                }
            }

            Object m = method.invoke(component, params.toArray());
            if (m == null)
                return "";
            else
                return m;
        } catch (Exception exception) {
            throw new YailRuntimeError(exception.toString(), "Error");
        }
    }


    /* 
        -----------------------
        ListDetails

        Gives the information of the specified component with all properties, events, methods as JSON text.


        -- Parameters --
        Component component            : The component that you want to get details for.

        -----------------------
    */
    @SimpleFunction(description = "Gives the information of the specified component with all properties, events, methods as JSON text.")
    public String ListDetails(Component component) throws ClassNotFoundException {

        // Create a Class object from class name.
        // Class<?> clasz = Class.forName(BASE_PACKAGE);

        JSONObject details = new JSONObject();
        Method[] allmethods = component.getClass().getMethods();

        details.put("type", component.getClass().getName());
        details.put("name", component.getClass().getSimpleName());
        details.put("external", component.getClass().getAnnotation(SimpleObject.class).external());
        details.put("version", component.getClass().getAnnotation(DesignerComponent.class).version());
        details.put("versionName", component.getClass().getAnnotation(DesignerComponent.class).versionName());
        details.put("dateBuilt", component.getClass().getAnnotation(DesignerComponent.class).dateBuilt());
        details.put("category", component.getClass().getAnnotation(DesignerComponent.class).category());
        details.put("description", component.getClass().getAnnotation(DesignerComponent.class).description());
        details.put("helpUrl", component.getClass().getAnnotation(DesignerComponent.class).helpUrl());
        details.put("showOnPalette", component.getClass().getAnnotation(DesignerComponent.class).showOnPalette());
        details.put("nonVisible", component.getClass().getAnnotation(DesignerComponent.class).nonVisible());
        details.put("iconName", component.getClass().getAnnotation(DesignerComponent.class).iconName());
        details.put("androidMinSdk", component.getClass().getAnnotation(DesignerComponent.class).androidMinSdk());

        JSONArray properties = new JSONArray();
        JSONArray blockProperties = new JSONArray();
        JSONArray events = new JSONArray();
        JSONArray methods = new JSONArray();

        // Get the component's class and return all methods from it.
        // Search for methods.
        for (Method mtd : allmethods) {
            JSONObject data = new JSONObject();

            data.put("name", mtd.getName());
            
            if (mtd.isAnnotationPresent(DesignerProperty.class))
            {
                data.put("editorType", mtd.getAnnotation(DesignerProperty.class).editorType());
                data.put("defaultValue", mtd.getAnnotation(DesignerProperty.class).defaultValue());
                data.put("editorArgs", new JSONArray(Arrays.asList(mtd.getAnnotation(DesignerProperty.class).editorArgs())));
                properties.put(data);
            }

            if (mtd.isAnnotationPresent(SimpleProperty.class))
            {
                data.put("description", mtd.getAnnotation(SimpleProperty.class).description());
                data.put("category", mtd.getAnnotation(SimpleProperty.class).category());
                data.put("visible", mtd.getAnnotation(SimpleProperty.class).userVisible());
                String rw = "read-write";

                boolean setter = findMethod(allmethods, mtd.getName(), 1) != null;
                boolean getter = findMethod(allmethods, mtd.getName(), 0) != null;

                if (setter && (getter == false))
                {
                    rw = "write-only";
                    data.put("type", findMethod(allmethods, mtd.getName(), 1).getParameterTypes()[0].getSimpleName());
                }
                else if (getter && (setter == false))
                {
                    rw = "read-only";
                    data.put("type", findMethod(allmethods, mtd.getName(), 0).getReturnType().getSimpleName());
                }
                else if (getter && setter)
                {
                    rw = "read-write";
                    data.put("type", findMethod(allmethods, mtd.getName(), 1).getParameterTypes()[0].getSimpleName());
                }

                data.put("rw", rw);
                data.put("deprecated", mtd.getAnnotation(SimpleProperty.class).category() == PropertyCategory.DEPRECATED);
                
                if (mtd.getAnnotation(SimpleProperty.class).category() != PropertyCategory.UNSET)
                    blockProperties.put(data);
            }

            if (mtd.isAnnotationPresent(SimpleEvent.class))
            {
                data.put("description", mtd.getAnnotation(SimpleEvent.class).description());
                data.put("visible", mtd.getAnnotation(SimpleEvent.class).userVisible());
                // Missing: "deprecated"
                // Missing: "params"
            }

            if (mtd.isAnnotationPresent(SimpleFunction.class))
            {
                data.put("description", mtd.getAnnotation(SimpleFunction.class).description());
                data.put("visible", mtd.getAnnotation(SimpleFunction.class).userVisible());
                data.put("returnType", mtd.getReturnType().getSimpleName());
                // Missing: "deprecated"
                // Missing: "params"
            }
        }

        details.put("properties", properties);
        details.put("blockProperties", blockProperties);
        details.put("events", events);
        details.put("methods", methods);

        return details.toString();
    }


    /* 
        -----------------------
        Version

        Returns the version of the extension.

        -----------------------
    */
    @SimpleProperty(description = "Returns the extension version.")
    public int Version() {
        return DynamicComponents.class.getAnnotation(DesignerComponent.class).version();
    }


    /* 
        -----------------------
        VersionName

        Returns the version name of the extension.

        -----------------------
    */
    @SimpleProperty(description = "Returns the extension version name.")
    public String VersionName() {
        return DynamicComponents.class.getAnnotation(DesignerComponent.class).versionName();
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
