package com.yusufcihan.DynamicComponents; 

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.*;
import com.google.appinventor.components.runtime.util.YailList;

import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.lang.Boolean;

@DesignerComponent(version = 3,
                   description = "Dynamic Components extension to create any type of dynamic component in any arrangement.<br><br>- by Yusuf Cihan",
                   category = ComponentCategory.EXTENSION,
                   nonVisible = true,
                   iconName = "https://yusufcihan.com/img/dynamiccomponents.png")
@SimpleObject(external = true)
public class DynamicComponents extends AndroidNonvisibleComponent implements Component {
    
    // Variables
    private Hashtable<String, Component> COMPONENTS = new Hashtable<String, Component>();
    private String BASE_PACKAGE = "com.google.appinventor.components.runtime";
    private String LAST_ID = "";
  
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
    //       MAIN METHODS
    // ------------------------

    /*
        Creates a new dynamic component. It supports all component that added to your current AI2 builder.
        In componentName, you can type the component's name like "Button", or you can pass a static component then it can create a new instance of it.
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
        if (!COMPONENTS.containsKey(id))
        {
            try 
            {
                // If input is a component name then create a instance of it.
                if (componentName instanceof String)
                {
                    // Return the component class by looking the its name.
                    Class<?> clasz = Class.forName(BASE_PACKAGE + "." + componentName.toString().replace(" ", ""));
                    // Create constructor object for creating a new instance.
                    Constructor<?> constructor = clasz.getConstructor(new Class[] { ComponentContainer.class });
                    // Create a new instance of specified component.
                    component = (Component)constructor.newInstance((ComponentContainer)in);
                }
                else
                {
                    String packageName = componentName.getClass().getPackage().getName();
                    if (packageName.equals(BASE_PACKAGE))
                    {
                        Class<?> clasz = Class.forName(componentName.getClass().getName());
                        Constructor<?> constructor = clasz.getConstructor(new Class[] { ComponentContainer.class });
                        component = (Component)constructor.newInstance((ComponentContainer)in);
                    }
                    else
                    {
                        error = "Input is not a string or a valid component type.";
                    }
                    
                }
                
            }
            catch (Exception e)
            {
                error = e.getMessage();
            }
        }
        else
        {
            error = "This ID is already used for another component, please pick another. ID needs to be unique for all components!";
        }    

        if ((id.trim().length() == 0) || (id == null))
        {
            error = "ID is blank. Please enter a valid ID.";
        }

        if (error != null)
        {
            throw new YailRuntimeError(error, "DynamicComponents-AI2 Error");
        }
        else {
            COMPONENTS.put(id, component);
        }           
    }

    /*
        Changes ID of one of created components to a new one. The old ID must be exist and new ID mustn't exist.
    */
    @SimpleFunction(description = "Changes ID of one of created components to a new one. The old ID must be exist and new ID mustn't exist.")
    public void ChangeId(String id, String newId) {
        if (COMPONENTS.containsKey(id) && !COMPONENTS.containsKey(newId))
        {
            Component component = COMPONENTS.remove(id);
            COMPONENTS.put(newId, component);
        }
        else
        {
            throw new YailRuntimeError("Old ID must exist and new ID mustn't exist.","Error");
        }
    }

    /*
        Removes the component with specified ID from screen/layout and the component list. So you will able to use its ID again as it will be deleted.
    */
    @SimpleFunction(description = "Removes the component with specified ID from screen/layout and the component list. So you will able to use its ID again as it will be deleted.")
    public void Remove(String id) {
        // Don't do anything if id is not in the components list.
        if (COMPONENTS.containsKey(id))
        {
            // Get the component.
            Object cmp = COMPONENTS.get(id);

            try {
                Method m = cmp.getClass().getMethod("Visible", boolean.class);
                m.invoke(cmp, false);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Remove its id from components list.
            COMPONENTS.remove(id);
        }
    }

    /*
        Returns last used ID by Create block.
    */
    @SimpleFunction(description = "Returns last used ID by Create block.")
    public String LastUsedID() {
        return LAST_ID;
    }

    /*
        Returns all used IDs in the created components list.
    */
    @SimpleFunction(description = "Returns all used IDs in the created components list.")
    public YailList UsedIDs() {
        Set<String> keys = COMPONENTS.keySet();
        return YailList.makeList(keys);
    }

    /*
        Returns the component's itself for setting properties. ID must be a valid ID which is added with Create block.
    */
    @SimpleFunction(description = "Returns the component's itself for setting properties. ID must be a valid ID which is added with Create block.")
    public Component GetComponent(String id) {
        return COMPONENTS.get(id);
    }

    /*
        Returns the ID of component. Component needs to be created by Create block. Otherwise it will return blank string.
    */
    @SimpleFunction(description = "Returns the ID of component. Component needs to be created by Create block. Otherwise it will return blank string.")
    public String GetId(Component component) {
        return getKeyFromValue(COMPONENTS, component);
    }

    /*
        Returns the component's name.
    */
    @SimpleFunction(description = "Returns the component's name.")
    public String GetName(Component component) {
        return component.getClass().getName().replace(BASE_PACKAGE + ".", "");
    }

    /*
        Removes all created dynamic components. Same as Remove block, but for all created components.
    */
    @SimpleFunction(description = "Removes all created dynamic components. Same as Remove block, but for all created components.")
    private void RemoveAll() {
        Set<String> keys = COMPONENTS.keySet();
        for (String key : keys) {
            Remove(key);
        }
    }

    /*
        Set a property of a component by typing its name.
    */
    @SimpleFunction(description = "Set a property of a component by typing its property name.")
    public void SetProperty(Component component, String name, Object value) {
        // The method will be invoked.
        Method m = null;
        try
        { 
            m = FindMethod(component.getClass().getMethods(), name, 1);
            // Method m = component.getClass().getMethod(name, value.getClass());
            if (m == null)
                throw new YailRuntimeError("Property can't found with that name.", "Error");
            
            String outputName = m.getParameterTypes()[0].getName().toString().trim();
            String inputName = value.getClass().getName().toString().trim();
            String v = "";

            // Parse the value and save it in a variable.
            if (inputName.equals("gnu.math.IntNum"))
            {
                v = Integer.toString(((gnu.math.IntNum)value).intValue());
            }
            else if (inputName.equals("gnu.math.DFloNum"))
            {
                v = Double.toString(((gnu.math.DFloNum)value).doubleValue());
            }
            else {
                v = value.toString();
            }

            // Check for requested parameter type.
            if (outputName.equals("int")) {
                m.invoke(component, Integer.parseInt(v));
            }
            else if (outputName.equals("double")) {
                m.invoke(component, Double.parseDouble(v));
            }
            else if (outputName.equals("float")) {
                m.invoke(component, Float.parseFloat(v));
            }
            else {
                m.invoke(component, Class.forName(value.getClass().getName()).cast(value));
            }
        }
        catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException eh)
        {
            throw new YailRuntimeError(eh.getMessage().toString(),"Error");
        }
        catch (IllegalArgumentException eh) {
            throw new YailRuntimeError("Looks like parameters are invalid. If you think everything is correct, please report.", "Error");
        }
        catch (Exception e)
        {
            throw new YailRuntimeError(e.getClass().getName() + ": " + e.getMessage().toString(), "Error");
        }
    }

    /*
        Get property value of a component.
    */
    @SimpleFunction(description = "Get property value of a component.")
    public Object GetProperty(Component component, String name) {
        // The method will be invoked.
        Method m = null;
        try
        { 
            m = FindMethod(component.getClass().getMethods(), name, 0);
            // Invoke the saved method and return its return value.
            return m.invoke(component);
        }
        catch (Exception eh)
        {
           // Throw an error when something goes wrong.
           throw new YailRuntimeError(eh.getMessage().toString(),"Error");
        }
    }

    @SimpleFunction(description = "Get all available properties of a component which can be set from Designer as list along with types. Can be used to learn the properties of any component which is not static.")
    public YailList GetDesignerProperties(Component component) {
        // A list which includes designer properties.
        List<String> properties = new ArrayList<String>(); 
        // Get the component's class and return all methods from it.
        Method[] methods = component.getClass().getMethods();
        for (Method mtd : methods)
        {
            // Read for @DesignerProperty annotations.
            // So we can learn which method is used as property setter/getter.
            if ((mtd.getDeclaredAnnotations().length == 2) && (mtd.isAnnotationPresent(DesignerProperty.class)))
            {
                // Get the DesignerProperty annotation.
                DesignerProperty n = mtd.getAnnotation(DesignerProperty.class);
                // Add editorType value and method name to the list.
                properties.add(mtd.getName() + "---" + n.editorType());
            }
        }
        // Return the list.
        return YailList.makeList(properties);
    }

    @SimpleFunction(description = "Get all available methods from a component.")
    private YailList GetMethods(Component component) {
        // A list which includes designer properties.
        List<String> names = new ArrayList<String>(); 
        for (Method mtd : component.getClass().getMethods())
        {
            names.add(mtd.getName());
        }
        // Return the list.
        return YailList.makeList(names);
    }

   
  
    // ------------------------
    //      PRIVATE METHODS
    // ------------------------

    // Getting key from value, found on:
    // http://www.java2s.com/Code/Java/Collections-Data-Structure/GetakeyfromvaluewithanHashMap.htm
    public String getKeyFromValue(Hashtable<String, Component> hm, Object value) {
    for (String o : hm.keySet()) {
        if (hm.get(o).equals(value)) {
            return (String)o;
        }
    }
        return "";
    }

    private Method FindMethod(Method[] methods, String name, Integer paramCount) {
        Method m = null;
        for (Method mtd : methods)
            {
                // Check for one parametered (setter) method.
                if((mtd.getName() == name.trim()) && (mtd.getParameterCount() == paramCount))
                {
                    m = mtd;
                    break;
                }
            }
        return m;
    }
    
}
