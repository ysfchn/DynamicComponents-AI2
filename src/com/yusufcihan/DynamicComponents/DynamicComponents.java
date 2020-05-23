package com.yusufcihan.DynamicComponents; 

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.*;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.lang.Boolean;

import android.view.View;
import android.view.ViewGroup;

@DesignerComponent(version = 3,
                   description = "Dynamic Components extension to create any type of dynamic component in any arrangement.<br><br>- by Yusuf Cihan",
                   category = ComponentCategory.EXTENSION,
                   nonVisible = true,
                   iconName = "https://yusufcihan.com/img/dynamiccomponents.png")
@SimpleObject(external = true)
public class DynamicComponents extends AndroidNonvisibleComponent implements Component {
    
    // Variables
    private Hashtable<String, Object> COMPONENTS = new Hashtable<String, Object>();
    private List<String> blacklist = Arrays.asList("CopyHeight", "CopyWidth", "wait", "onClick", "Column", "Row", "setLastWidth", "setLastHeight");
    private String BASE_PACKAGE = "com.google.appinventor.components.runtime";
    private String LAST_ID = "";
  
    public DynamicComponents(ComponentContainer container) {
        super(container.$form());
    }
  
    private String BasePackage() {
      return BASE_PACKAGE;
    }
  
    private void BasePackage(String packageName) {
      BASE_PACKAGE = packageName;
    }

    // ------------------------
    //       MAIN METHODS
    // ------------------------

    /*
        Creates a new dynamic component. It supports all component that added to your current AI2 builder.
        In componentName, you can type the component's name like "Button", or you can pass a static component then it can create a new instance of it.
    */
    @SimpleFunction()
    public void Create(AndroidViewComponent in, Object componentName, String id) {
        Object component = null;
        LAST_ID = id;
        boolean error = false;
        // Check if id is used by another created dynamic component.
        if (COMPONENTS.containsKey(id))
        {
            try 
            {
                // If input is a component name then create a instance of it.
                if (componentName instanceof String)
                {
                    // Return the component class by looking the its name.
                    Class clasz = Class.forName(BASE_PACKAGE + "." + componentName.toString().replace(" ", ""));
                    // Create constructor object for creating a new instance.
                    Constructor constructor = clasz.getConstructor(new Class[] { ComponentContainer.class });
                    // Create a new instance of specified component.
                    component = constructor.newInstance((ComponentContainer)in);
                }
                else
                {
                    String packageName = componentName.getClass().getPackage().getName();
                    if (packageName == BASE_PACKAGE)
                    {
                        Class clasz = Class.forName(componentName.getClass().getName());
                        Constructor constructor = clasz.getConstructor(new Class[] { ComponentContainer.class });
                        component = constructor.newInstance((ComponentContainer)in);
                    }
                    else
                    {
                        error = true;
                        throw new YailRuntimeError("Input is not a string or a valid component type.","Error");
                    }
                }
                
            }
            catch (Exception e)
            {
                error = true;
                // Throw a runtime error when something goes wrong.
                throw new YailRuntimeError(e.getMessage(),"Error");
            }
        }
        else
        {
            error = true;
            // Throw a runtime error when ID is already used for another component.
            throw new YailRuntimeError("This ID is already used for another component, please pick another. ID needs to be unique for all components!","Duplicate ID");
        }    

        if ((id.trim().length() == 0) || (id == null))
        {
            error = true;
            throw new YailRuntimeError("ID is blank. Please enter a valid ID.","Error");
        }

        if (error == false)
        {
            COMPONENTS.put(id, component);
        }                     
    }

    /*
        Changes ID of one of created components to a new one. The old ID must be exist and new ID mustn't exist.
    */
    @SimpleFunction()
    public void ChangeId(String id, String newId) {
        if (COMPONENTS.containsKey(id) && !COMPONENTS.containsKey(newId))
        {
            Object component = COMPONENTS.remove(id);
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
    @SimpleFunction()
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
    @SimpleFunction()
    public String LastUsedID() {
        return LAST_ID;
    }

    /*
        Returns all used IDs in the created components list.
    */
    @SimpleFunction()
    public YailList UsedIDs() {
        YailList yaillist = new YailList();
        Set<String> keys = COMPONENTS.keySet();
        for (String key : keys)
        {
            yaillist.add(key);
        }
        return yaillist;
    }

    /*
        Returns the component's itself for setting properties. ID must be a valid ID which is added with Create block.
    */
    @SimpleFunction()
    public Object GetComponent(String id) {
        return COMPONENTS.get(id);
    }

    /*
        Returns the ID of component. Component needs to be created by Create block. Otherwise it will return blank string.
    */
    @SimpleFunction()
    public String GetId(Object component) {
        return getKeyFromValue(COMPONENTS, component);
    }

    /*
        Returns the component's name.
    */
    @SimpleFunction()
    public String GetName(Object component) {
        return component.getClass().getName().replace(BASE_PACKAGE + ".", "");
    }

    /*
        Removes all created dynamic components. Same as Remove block, but for all created components.
    */
    @SimpleFunction()
    public void RemoveAll() {
        Set<String> keys = COMPONENTS.keySet();
        for (String key : keys) {
            Remove(key);
        }
    }

    /*
        Set a property of a component by typing its name.
    */
    @SimpleFunction()
    public void SetProperty(Object component, String propertyName, Object propertyValue) {
        // Read methods of the component.
        Method[] methods = component.getClass().getMethods();
        // The method will be invoked.
        Method method = null;
        // Class for casting purpose.
        Class caster = null;
        try
        { 
            Method m = component.getClass().getMethod(propertyName, propertyValue.getClass());
            /*
            for (Method mtd : methods)
            {
                // Check for one parametered (setter) method.
                if((mtd.getName() == propertyName) && (mtd.getParameterCount() == 1))
                {
                    // Save it for later.
                    caster = mtd.getParameterTypes()[0];
                    method = mtd;
                    break;
                }
            }
            */
            // Invoke the saved method.
            m.invoke(component, propertyValue);
        }
        catch (Exception eh)
        {
            // Throw an error when something goes wrong.
            throw new YailRuntimeError(eh.getMessage().toString(),"Error");
        }
    }



    @SimpleFunction(description = "Get all available properties of a component which can be set from Designer as list along with types. Can be used to learn the properties of any component which is not static.")
    public YailList GetDesignerProperties(Object component) {
        // A list which includes designer properties.
        ArrayList names = new ArrayList();
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
              names.add(YailList.makeList(new String[] { 
                mtd.getName(), 
                n.editorType() 
              }));
           }
        }
        // Return the list.
        return YailList.makeList(names);
    }
               
    @SimpleFunction(description = "Get property value of a component.")
    public Object GetProperty(Object component, String propertyName) {
        // Read methods of the component.
        Method[] methods = component.getClass().getMethods();  
        // The method will be invoked.
        Method method = null;
        try
        { 
            for (Method mtd : methods)
            {
                // Check for zero parametered (getter) method.
                if((mtd.getName() == propertyName) && (mtd.getParameterCount() == 0))
                {
                    // Save it for later.
                    method = mtd;
                    break;
                }
            }
            // Invoke the saved method and return its return value.
            return method.invoke(component);
        }
        catch (Exception eh)
        {
           // Throw an error when something goes wrong.
           throw new YailRuntimeError(eh.getMessage().toString(),"Error");
        }
    }

   
  
    // ------------------------
    //      PRIVATE METHODS
    // ------------------------

    // Getting key from value, found on:
    // http://www.java2s.com/Code/Java/Collections-Data-Structure/GetakeyfromvaluewithanHashMap.htm
    public String getKeyFromValue(Hashtable hm, Object value) {
    for (Object o : hm.keySet()) {
        if (hm.get(o).equals(value)) {
            return (String)o;
        }
    }
        return "";
    }
}
