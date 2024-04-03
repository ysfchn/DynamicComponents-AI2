package com.yusufcihan.DynamicComponents;

import com.yusufcihan.DynamicComponents.classes.Utils;
import com.yusufcihan.DynamicComponents.classes.Metadata;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
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
import com.google.appinventor.components.runtime.util.YailDictionary;
import com.google.appinventor.components.runtime.util.YailList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@DesignerComponent(
        description =
          "Create any component available in your App Inventor distribution and create instances of " +
          "other extensions programmatically in runtime. Made with &#x2764;&#xfe0f; by Yusuf Cihan.",
        category = ComponentCategory.EXTENSION,
        helpUrl = "https://github.com/ysfchn/DynamicComponents-AI2/blob/main/README.md",
        iconName = "aiwebres/icon.png",
        nonVisible = true,
        version = 10,
        versionName = "2.3.0"
)
@SimpleObject(external = true)
public class DynamicComponents extends AndroidNonvisibleComponent {
  private static final String TAG = Utils.TAG;

  // Whether component creation should happen on the UI thread
  private boolean postOnUiThread = false;

  // Components created with Dynamic Components
  private final HashMap<String, Component> COMPONENTS = new HashMap<>();

  // IDs of components created with Dynamic Components
  private final HashMap<Component, String> COMPONENT_IDS = new HashMap<>();

  private Object lastUsedId = "";
  private final ArrayList<ComponentListener> componentListeners = new ArrayList<>();

  public DynamicComponents(ComponentContainer container) {
    super(container.$form());
  }

  interface ComponentListener {
    void onCreation(Component component, String id);
  }

  public boolean isCreatedComponent(String id) {
    return COMPONENTS.containsKey(id);
  }

  public void notifyListenersOfCreation(Component component, String id) {
    for (ComponentListener listener : componentListeners) {
      listener.onCreation(component, id);
    }
  }

  private void dispatchEvent(final String name, final Object... parameters) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(DynamicComponents.this, name, parameters);
      }
    });
  }

  @DesignerProperty(
    defaultValue = "UI",
    editorArgs = {"Main", "UI"},
    editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES
  )
  @SimpleProperty(userVisible = false)
  public void Thread(String thread) {
    if (thread.equalsIgnoreCase("UI")) {
      postOnUiThread = true;
    } else if (thread.equalsIgnoreCase("Main")) {
      postOnUiThread = false;
    } else {
      throw new YailRuntimeError("Unexpected value '" + thread + "'", TAG);
    }
  }

  @SimpleEvent(description = "Is called after a component has been created.")
  public void ComponentBuilt(final Component component, final String id, final String type) {
    dispatchEvent("ComponentBuilt", component, id, type);
  }

  @SimpleEvent(description = "Is called after a schema has/mostly finished component creation.")
  public void SchemaCreated(final String name, final YailList parameters) {
    dispatchEvent("SchemaCreated", name, parameters);
  }

  @SimpleFunction(description = "Assign a new ID to a previously created dynamic component.")
  public void ChangeId(String id, String newId) {
    if (checkBeforeReplacement(id, newId)) {
      for (String mId : COMPONENTS.keySet()) {
        if (mId.contains(id)) {
          Component mComponent = COMPONENTS.get(mId);
          String mReplacementId = mId.replace(id, newId);
          COMPONENT_IDS.remove(mComponent);
          COMPONENTS.put(mReplacementId, COMPONENTS.remove(mId));
          COMPONENT_IDS.put(mComponent, mReplacementId);
        }
      }
    }
  }

  @SimpleFunction(description = "Replace an existing ID with a new one.")
  public void ReplaceId(String id, String newId) {
    if (checkBeforeReplacement(id, newId)) {
      final Component component = COMPONENTS.get(id);
      COMPONENTS.remove(id);
      COMPONENT_IDS.remove(component);
      COMPONENTS.put(newId, component);
      COMPONENT_IDS.put(component, newId);
    }
  }

  private boolean checkBeforeReplacement(String id, String newId) {
    if (isCreatedComponent(id) && !isCreatedComponent(newId)) {
      return true;
    }
    throw new YailRuntimeError(
      "The ID you used is either not a dynamic component, or the ID you've used " +
      "to replace the old ID is already taken.", TAG
    );
  }

  @SimpleFunction(description =
    "Creates a new dynamic component in given container (arrangement/canvas) and assign to an ID to reference " +
    "the created component later. The 'ComponentBuilt' event will be invoked when the component has created. " +
    "Note that you can't create components in Screen directly, you will need to have an arrangement beforehand " +
    "inside a Screen to do that."
  )
  public void Create(final AndroidViewComponent in, Object componentName, final String id) throws Exception {
    if (!COMPONENTS.containsKey(id)) {
      lastUsedId = id;
      Class<?> mClass = Class.forName(Utils.getClassName(componentName));
      final Constructor<?> mConstructor = mClass.getConstructor(ComponentContainer.class);
      if (postOnUiThread) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            Component mComponent = Utils.createInstance(mConstructor, in);
            COMPONENT_IDS.put(mComponent, id);
            COMPONENTS.put(id, mComponent);
            notifyListenersOfCreation(mComponent, id);
            ComponentBuilt(mComponent, id, mComponent.getClass().getSimpleName());
          }
        });
      } else {
        Component mComponent = Utils.createInstance(mConstructor, in);
        COMPONENT_IDS.put(mComponent, id);
        COMPONENTS.put(id, mComponent);
        notifyListenersOfCreation(mComponent, id);
        ComponentBuilt(mComponent, id, mComponent.getClass().getSimpleName());
      }
    } else {
      throw new YailRuntimeError("All component IDs must be unique, the component ID '" + id + "' has already used before.", TAG);
    }
  }

  @SimpleFunction(description =
    "Creates a new dynamic component in given container (arrangement/canvas) and return it without saving it to the " +
    "created components list, so it won't be attached to an ID. Note that you can't create components " +
    "in Screen directly, you will need to have an arrangement beforehand inside a Screen to do that."
  )
  public Component CreateEphemeral(final AndroidViewComponent in, Object componentName) throws Exception {
    Class<?> mClass = Class.forName(Utils.getClassName(componentName));
    final Constructor<?> mConstructor = mClass.getConstructor(ComponentContainer.class);
    return Utils.createInstance(mConstructor, in);
  }

  @SimpleFunction(description = "Generates a random UUID, can be useful to create components with random ID.")
  public String GenerateID() {
    String id;
    do {
      id = UUID.randomUUID().toString();
    } while (isCreatedComponent(id));
    return id;
  }

  @SimpleFunction(description = "Returns the component associated with the specified ID. If not found, returns an empty string.")
  public Object GetComponent(String id) {
    Component component = COMPONENTS.get(id);
    if (component == null) {
      return "";
    }
    return component;
  }

  @SimpleFunction(description = "Get meta data about the specified component.")
  public YailDictionary GetComponentMeta(Component component) {
    return Metadata.getComponentCommonInfo(component);
  }

  @SimpleFunction(description = "Get meta data about events for the specified component.")
  public YailDictionary GetEventMeta(Component component) {
    try {
      return Metadata.getComponentAnnotationInfo(component, SimpleEvent.class);
    } catch (Exception e) {
      String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
      throw new YailRuntimeError("Couldn't read the metadata: " + errorMessage, TAG);
    }
  }

  @SimpleFunction(description = "Get meta data about functions for the specified component.")
  public YailDictionary GetFunctionMeta(Component component) {
    try {
      return Metadata.getComponentAnnotationInfo(component, SimpleFunction.class);
    } catch (Exception e) {
      String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
      throw new YailRuntimeError("Couldn't read the metadata: " + errorMessage, TAG);
    }
  }

  @SimpleFunction(description = "Returns the ID of the specified component. If not found, returns an empty string.")
  public String GetId(Component component) {
    return COMPONENT_IDS.getOrDefault(component, "");
  }

  @SimpleFunction(description =
    "Returns the position of the specified component according to its parent component. " +
    "Indexes begins at one. If there is no parent (which shouldn't happen, as the top-most parent is Screen) " +
    "then return zero (0)."
  )
  public int GetOrder(AndroidViewComponent component) {
    // (non null)
    View mComponent = component.getView();
    ViewGroup mParent = (ViewGroup) mComponent.getParent();

    if (Utils.isNotEmptyOrNull(mComponent) && Utils.isNotEmptyOrNull(mParent)) {
      return mParent.indexOfChild(mComponent) + 1;
    }
    return 0;
  }

  @SimpleFunction(description =
    "Get a property value of a component with given property name. The returned value can be " +
    "any type of value, but if the property value is null, this block will return an empty string instead so " +
    "it can be manipulated and compared with other App Inventor blocks."
  )
  public Object GetProperty(Component component, String name) {
    Object returnedValue = Utils.callMethod(component, name, new Object[] { });
    return returnedValue == null ? "" : returnedValue;
  }

  @SimpleFunction(description = "Get meta data about properties for the specified component, including their values.")
  public YailDictionary GetPropertyMeta(Component component) {
    try {
      return Metadata.getComponentPropertyInfo(component);
    } catch (Exception e) {
      String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
      throw new YailRuntimeError("Couldn't read the metadata: " + errorMessage, TAG);
    }
  }

  @SimpleFunction(description =
    "Calls any method of a component by its name and given parameters, and returns its result. " +
    "The returned value can be any type of value, but if the returned value is null, this block will return " +
    "an empty string instead so it can be manipulated and compared with other App Inventor blocks."
  )
  public Object Invoke(Component component, String name, YailList parameters) {
    Object returnedValue = Utils.callMethod(component, name, parameters.toArray());
    return returnedValue == null ? "" : returnedValue;
  }

  @SimpleFunction(description = "Returns true if the specified component was created by this extension, otherwise false.")
  public boolean IsDynamic(Component component) {
    return COMPONENTS.containsValue(component);
  }

  @SimpleFunction(description = "Returns the last used ID to create a component.")
  public Object LastUsedID() {
    return lastUsedId;
  }

  @SimpleFunction(description = "Moves the specified component to the specified view.")
  public void Move(AndroidViewComponent arrangement, AndroidViewComponent component) {
    View mComponent = component.getView();
    ((ViewGroup) mComponent.getParent()).removeView(mComponent);
    ViewGroup mArrangement = (ViewGroup) arrangement.getView();
    ViewGroup mTarget = (ViewGroup) mArrangement.getChildAt(0);
    mTarget.addView(mComponent);
  }

  @SimpleFunction(description =
    "Removes a component from the screen with its ID. The ID will also be de-registered, " +
    "so its ID can be reused for other components that are going to be created later."
  )
  public void Remove(String id) {
    Object component = COMPONENTS.get(id);
    if (component == null) {
      return;
    }
    RemoveComponent((AndroidViewComponent)component);
    COMPONENTS.remove(id);
    COMPONENT_IDS.remove(component);
  }

  @SimpleFunction(description =
    "Removes a component from the screen. It doesn't need to be created by this extension. " +
    "But if the given component is dynamically created by this extension, this block will also " +
    "de-register its ID so its ID can be reused for other components that are going to be created later."
  )
  public void RemoveComponent(AndroidViewComponent component) {
    try {
      Method mMethod = Utils.getMethod(component, "getView");
      if (mMethod != null) {
        final View mComponent = (View) mMethod.invoke(component);
        final ViewGroup mParent = (ViewGroup) mComponent.getParent();
        if (postOnUiThread) {
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              mParent.removeView(mComponent);
            }
          });
        } else {
          mParent.removeView(mComponent);
        }
      }
      final String[] closeMethods = new String[] { "onPause", "onDestroy" };
      for (String methodName : closeMethods) {
        final Method invokeMethod = Utils.getMethod(component, methodName);
        if (invokeMethod != null)
          invokeMethod.invoke(component);
      }
      // Also remove the component from component list if
      // it has created by DynamicComponents.
      Object storedComponentId = COMPONENT_IDS.get(component);
      if (storedComponentId != null) {
        COMPONENTS.remove(storedComponentId);
        COMPONENT_IDS.remove(component);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SimpleFunction(description =
    "Sets the order of the specified component according to its parent view. " +
    "Indexes begins at one, and setting to zero (0) will move the component to the end."
  )
  public void SetOrder(AndroidViewComponent component, int index) {
    View mComponent = component.getView();
    ViewGroup mParent = (ViewGroup) mComponent.getParent();
    mParent.removeView(mComponent);
    mParent.addView(mComponent, Math.min(index - 1, mParent.getChildCount()));
  }

  @SimpleFunction(description =
    "Set a property of the specified component by its name, including properties " +
    "those only available from the Designer."
  )
  public void SetProperty(Component component, String name, Object value) {
    Utils.callMethod(component, name, new Object[] { value });
  }

  @SimpleFunction(description =
    "Set multiple properties of the specified component using a dictionary, " +
    "including those only available from the Designer."
  )
  public void SetProperties(Component component, YailDictionary properties) throws Exception {
    for (Map.Entry<Object, Object> pair : properties.entrySet()) {
      Utils.callMethod(component, (String)pair.getKey(), new Object[] { pair.getValue() });
    }
  }

  @SimpleFunction(description =
    "Create components in bulk with a JSON template. Templates can also contain parameters " +
    "that will be replaced with the values which are defined from the parameters list. See " +
    "the documentation for more information about using and creating templates."
  )
  public void Schema(AndroidViewComponent in, final String template, final YailList parameters) throws Exception {
    JSONObject mScheme = new JSONObject(template);

    if (!mScheme.optString("metadata-version", "").equals("1")) {
      throw new YailRuntimeError("Metadata version ('metadata-version' key in JSON) must equal to 1.", TAG);
    }

    if (Utils.isNotEmptyOrNull(template) && mScheme.has("components")) {
      JSONArray mKeys = (mScheme.has("keys") ? mScheme.getJSONArray("keys") : new JSONArray());
      if (mKeys.length() != (parameters.length() - 1)) {
        throw new YailRuntimeError(
          "Given list of template parameters must contain same amount of items that defined in the schema. " +
          "The template expects: " + mKeys.length() + ", but given parameters are: " + (parameters.length() - 1), TAG
        );
      }
      LinkedList<String[]> formatMapping = new LinkedList<String[]>();
      for (int i = 0; i < mKeys.length(); i++) {
        formatMapping.addLast(new String[] { mKeys.getString(i), parameters.getString(i) });
      }
      LinkedList<JSONObject> componentsList = Utils.componentTreeToList(mScheme.getJSONArray("components"), formatMapping);

      for (final JSONObject child : componentsList) {
        final String mId = child.getString("id");
        AndroidViewComponent mRoot = (!child.has("parent") ? in : (AndroidViewComponent) COMPONENTS.get(child.getString("parent")));
        final String mType = child.getString("type");

        ComponentListener listener = new ComponentListener() {
          @Override
          public void onCreation(Component component, String id) {
            try {
              if (Objects.equals(id, mId)) {
                JSONObject mProperties = child.getJSONObject("properties");
                JSONArray keys = mProperties.names();
                if (keys != null) {
                  for (int k = 0; k < keys.length(); k++) {
                    Utils.callMethod(
                      COMPONENTS.get(mId),
                      keys.getString(k),
                      new Object[] { mProperties.get(keys.getString(k)) }
                    );
                  }
                }
                componentListeners.remove(this);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        };
        componentListeners.add(listener);
        Create(mRoot, mType, mId);
      }

      SchemaCreated(mScheme.optString("name", ""), parameters);
    } else {
      throw new YailRuntimeError("The template is empty, or is does not have any components.", TAG);
    }
  }

  @SimpleFunction(description = "Returns all IDs of components created with this extension as a list.")
  public YailList UsedIDs() {
    return YailList.makeList(COMPONENTS.keySet());
  }

  @SimpleProperty(description = "Returns the version of this extension.")
  public int Version() {
    return DynamicComponents.class.getAnnotation(DesignerComponent.class).version();
  }

  @SimpleProperty(description = "Returns the version name of this extension.")
  public String VersionName() {
    return DynamicComponents.class.getAnnotation(DesignerComponent.class).versionName();
  }
}
