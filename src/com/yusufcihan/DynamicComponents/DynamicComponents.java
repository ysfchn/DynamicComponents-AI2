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

    @SimpleFunction(description = "Create a dynamic component that you want. It supports all components that added to App Inventor sources. Use 'in' parameter to specify the arrangement or canvas which new component will be placed in. Type the name of component in the 'componentName' section. (case sensitive) Use any component blocks to edit dynamic component's properties! ")
    public void Create(AndroidViewComponent in, String componentName, String id) {
        Object component = null;
        LAST_ID = id;
        // Check if id is used by another created dynamic component.
        if (!COMPONENTS.containsKey(id))
        {
           try 
           {
              // Return the component class by looking the its name.
              Class clasz = Class.forName(BASE_PACKAGE + "." + componentName);
              // Create constructor object for creating a new instance.
              Constructor constructor = clasz.getConstructor(new Class[] { ComponentContainer.class });
              // Create a new instance of specified component.
              component = constructor.newInstance((ComponentContainer)in);
           }
           catch (Exception e)
           {
              // Throw a runtime error when something goes wrong.
              throw new YailRuntimeError(e.getMessage(),"Error");
           }
           COMPONENTS.put(id, component);
        }
        else
        {
           // Throw a runtime error when ID is already used for another component.
           throw new YailRuntimeError("This ID is already used, please pick another.","Duplicate ID");
        }                         
        
   }
  
   @SimpleFunction(description = "Removes the component with specified ID from screen/layout and the component list. So you will able to use its ID again as it will be deleted.")
    public void Remove(String id) {
        Method m = null;
        Object cmp = null;
        // Don't do anything if id is not in the components list.
        if (COMPONENTS.containsKey(id) == false)
          return;
       
        // Get the component.
        cmp = COMPONENTS.get(id);
        // Remove its id from components list.
        COMPONENTS.remove(id);
      
        try
        { 
          // Hide the component if possible.
          ((AndroidViewComponent)cmp).Visible(false);
        }
        catch (Exception eh) { }
    }
  
   @SimpleFunction(description = "Returns last used ID.")
    public String LastUsedID() {
        return LAST_ID;
    }
  
   @SimpleFunction(description = "Returns the component's itself for setting properties. Component needs to be created with Create block. Type an ID which you typed in Create block to return the component.")
    public Object GetComponent(String id) {
        return COMPONENTS.get(id);
    }
  
   @SimpleFunction(description = "Returns created components' IDs as a list.")
    public Object GetCreatedComponents() {
        Set<String> keys = COMPONENTS.keySet();
        return keys;
    }
  
   @SimpleFunction(description = "Returns the component type name.")
    public String GetName(Object component) {
        return component.getClass().getName();
    }
  
   @SimpleFunction(description = "Removes all created dynamic components. Same as Remove block, but for all created components.")
    public void RemoveAll() {
        Set<String> keys = COMPONENTS.keySet();
        for(String key: keys){
            Remove(key);
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
  
    @SimpleFunction(description = "Set a property of a component by typing its name.")
    public void SetProperty(Object component, String propertyName, Object propertyValue) {
        // Read methods of the component.
        Method[] methods = component.getClass().getMethods();
        // The method will be invoked.
        Method method = null;
        // Class for casting purpose.
        Class caster = null;
        try
        { 
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
           // Invoke the saved method.
           method.invoke(component, propertyValue);
        }
        catch (Exception eh)
        {
           // Throw an error when something goes wrong.
           throw new YailRuntimeError(eh.getMessage().toString(),"Error");
        }
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

   @SimpleFunction(description = "Returns the ID of component. Component needs to be created by Create block. Otherwise it will return -1.")
    public String GetId(Object component) {
        return getKeyFromValue(COMPONENTS, component);
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
