package com.yusufcihan.DynamicComponents;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.util.Objects;
import java.util.UUID;

@DesignerComponent(
        description = "Dynamic Components is an extension that creates any component in your App Inventor distribution programmatically, " +
                "instead of having pre-defined components. Made with &#x2764;&#xfe0f; by Yusuf Cihan.",
        category = ComponentCategory.EXTENSION,
        helpUrl = "https://github.com/ysfchn/DynamicComponents-AI2/blob/main/README.md",
        iconName = "aiwebres/icon.png",
        nonVisible = true,
        version = 10,
        versionName = "2.2.3"
)
@SimpleObject(external = true)
public class DynamicComponents extends AndroidNonvisibleComponent {
  // Extension log tag
  private static final String TAG = "DynamicComponents";

  // Base package name for components
  private static final String BASE = "com.google.appinventor.components.runtime.";

  // Whether component creation should happen on the UI thread
  private boolean postOnUiThread = false;

  // Components created with Dynamic Components
  private final HashMap<String, Component> COMPONENTS = new HashMap<>();

  // IDs of components created with Dynamic Components
  private final HashMap<Component, String> COMPONENT_IDS = new HashMap<>();

  private Object lastUsedId = "";
  private final ArrayList<ComponentListener> componentListeners = new ArrayList<>();
  private JSONArray propertiesArray = new JSONArray();
  private final Util UTIL_INSTANCE = new Util();

  public DynamicComponents(ComponentContainer container) {
    super(container.$form());
  }

  interface ComponentListener {
    void onCreation(Component component, String id);
  }

  class Util {
    public boolean exists(String id) {
      return COMPONENTS.containsKey(id);
    }

    public String getClassName(Object componentName) {
      String regex = "[^.$@a-zA-Z0-9_]";
      String componentNameString = componentName.toString().replaceAll(regex, "");

      if (componentName instanceof String && componentNameString.contains(".")) {
        return componentNameString;
      } else if (componentName instanceof String) {
        return BASE + componentNameString;
      } else if (componentName instanceof Component) {
        return componentName.getClass().getName().replaceAll(regex, "");
      } else {
        throw new YailRuntimeError("Component is invalid.", TAG);
      }
    }

    public Method getMethod(Method[] methods, String name, int parameterCount) {
      name = name.replaceAll("[^a-zA-Z0-9]", "");
      for (Method method : methods) {
        int methodParameterCount = method.getParameterTypes().length;
        if (method.getName().equals(name) && methodParameterCount == parameterCount) {
          return method;
        }
      }

      return null;
    }

    public void newInstance(Constructor<?> constructor, String id, AndroidViewComponent input) {
      Component mComponent = null;

      try {
        mComponent = (Component) constructor.newInstance(input);
      } catch(Exception e) {
        throw new YailRuntimeError(e.getMessage(), TAG);
      } finally {
        if (mComponent != null) {
          String mComponentClassName = mComponent.getClass().getSimpleName();
          String[] mInitializeComponentClassName = ["Ball", "ImageSprite", "Sprite"];
                
          if (mInitializeComponentClassName.contains(mComponentClassName)) {
            Invoke(mComponent, "Initialize", new YailList());
          }

          COMPONENT_IDS.put(mComponent, id);
          COMPONENTS.put(id, mComponent);
          this.notifyListenersOfCreation(mComponent, id);
          ComponentBuilt(mComponent, id, mComponentClassName);
        }
      }
    }

    public void parse(String id, JSONObject json) throws JSONException {
      JSONObject data = new JSONObject(json.toString());
      data.remove("components");

      if (!"".equals(id)) {
        data.put("in", id);
      }

      propertiesArray.put(data);

      if (json.has("components")) {
        for (int i = 0; i < json.getJSONArray("components").length(); i++) {
          this.parse(data.optString("id", ""), json.getJSONArray("components").getJSONObject(i));
        }
      }
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
  }

  public boolean isNotEmptyOrNull(Object item) {
    return item instanceof String ? !
            ((String) item).replace(" ", "").isEmpty() 
            : item != null;
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
    UTIL_INSTANCE.dispatchEvent("ComponentBuilt", component, id, type);
  }

  @SimpleEvent(description = "Is called after a schema has/mostly finished component creation.")
  public void SchemaCreated(final String name, final YailList parameters) {
    UTIL_INSTANCE.dispatchEvent("SchemaCreated", name, parameters);
  }

  @SimpleFunction(description = "Assign a new ID to a previously created dynamic component.")
  public void ChangeId(String id, String newId) {
    if (checkBeforeReplacement(id, newId)) {
      for (String mId : UsedIDs().toStringArray()) {
        if (mId.contains(id)) {
          Component mComponent = (Component) GetComponent(mId);
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
      final Component component = (Component) GetComponent(id);

      COMPONENTS.remove(id);
      COMPONENT_IDS.remove(component);

      COMPONENTS.put(newId, component);
      COMPONENT_IDS.put(component, newId);
    }
  }

  private boolean checkBeforeReplacement(String id, String newId) {
    if (UTIL_INSTANCE.exists(id) && !UTIL_INSTANCE.exists(newId)) {
      return true;
    }
    throw new YailRuntimeError("The ID you used is either not a dynamic component, or the ID you've used" +
            " to replace the old ID is already taken.", TAG);
  }

  @SimpleFunction(description = "Creates a new dynamic component.")
  public void Create(final AndroidViewComponent in, Object componentName, final String id) throws Exception {
    if (!COMPONENTS.containsKey(id)) {
      lastUsedId = id;

      String mClassName = UTIL_INSTANCE.getClassName(componentName);
      Class<?> mClass = Class.forName(mClassName);
      final Constructor<?> mConstructor = mClass.getConstructor(ComponentContainer.class);

      if (postOnUiThread) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            UTIL_INSTANCE.newInstance(mConstructor, id, in);
          }
        });
      } else {
        UTIL_INSTANCE.newInstance(mConstructor, id, in);
      }
    } else {
      throw new YailRuntimeError("Expected a unique ID, got '" + id + "'.", TAG);
    }
  }

  @SimpleFunction(description = "Generates a random ID to create a component with.")
  public String GenerateID() {
    String id;

    do {
      id = UUID.randomUUID().toString();
    } while (UTIL_INSTANCE.exists(id));

    return id;
  }

  @SimpleFunction(description = "Returns the component associated with the specified ID.")
  public Object GetComponent(String id) {
    return COMPONENTS.get(id);
  }

  @SimpleFunction(description = "Get meta data about the specified component.")
  public YailDictionary GetComponentMeta(Component component) {
    Class<?> mClass = component.getClass();
    DesignerComponent mDesignerAnnotation = mClass.getAnnotation(DesignerComponent.class);
    boolean mHasDesigner = isNotEmptyOrNull(mDesignerAnnotation);
    boolean mHasObject;
    SimpleObject mObjectAnnotation = mClass.getAnnotation(SimpleObject.class);
    YailDictionary mMeta = new YailDictionary();
    mHasObject = isNotEmptyOrNull(mObjectAnnotation);

    if (mHasDesigner && mHasObject) {
      // Return all metadata
      mMeta.put("androidMinSdk", mDesignerAnnotation.androidMinSdk());
      mMeta.put("category", mDesignerAnnotation.category());
      mMeta.put("dateBuilt", mDesignerAnnotation.dateBuilt());
      mMeta.put("description", mDesignerAnnotation.description());
      mMeta.put("designerHelpDescription", mDesignerAnnotation.designerHelpDescription());
      mMeta.put("external", mObjectAnnotation.external());
      mMeta.put("helpUrl", mDesignerAnnotation.helpUrl());
      mMeta.put("iconName", mDesignerAnnotation.iconName());
      mMeta.put("nonVisible", mDesignerAnnotation.nonVisible());
      mMeta.put("package", mClass.getName());
      mMeta.put("showOnPalette", mDesignerAnnotation.showOnPalette());
      mMeta.put("type", mClass.getSimpleName());
      mMeta.put("version", mDesignerAnnotation.version());
      mMeta.put("versionName", mDesignerAnnotation.versionName());
    } else if (!mHasDesigner && mHasObject) {
      // Return some amount of metadata even if there is no
      // @DesignerComponent annotation provided
      mMeta.put("external", mObjectAnnotation.external());
      mMeta.put("package", mClass.getName());
      mMeta.put("type", mClass.getSimpleName());
    } else {
      // Return the least amount of metadata if no
      // annotation is provided
      mMeta.put("package", mClass.getName());
      mMeta.put("type", mClass.getSimpleName());
    }

    return mMeta;
  }

  @SimpleFunction(description = "Get meta data about events for the specified component.")
  public YailDictionary GetEventMeta(Component component) throws Exception {
    return getMetaDictionary(component, SimpleEvent.class);
  }

  @SimpleFunction(description = "Get meta data about functions for the specified component.")
  public YailDictionary GetFunctionMeta(Component component) throws Exception {
    return getMetaDictionary(component, SimpleFunction.class);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private YailDictionary getMetaDictionary(Component component, Class annotationClass) throws Exception {
    YailDictionary dictionaries = new YailDictionary();
    if (component == null) {
      return dictionaries;
    }

    for (Method method : component.getClass().getMethods()) {
      YailDictionary dictionary = new YailDictionary();
      Object annotation = method.getAnnotation(annotationClass);

      final boolean isDeprecated = isNotEmptyOrNull(
              method.getAnnotation(Deprecated.class));
      final String methodName = method.getName();

      if (annotation != null) {
        dictionary.put("description", invoke(annotation, "description"));
        dictionary.put("isDeprecated", isDeprecated);
        dictionary.put("userVisible", invoke(annotation, "userVisible"));
      }
      dictionary.put("isDeprecated", isDeprecated);
      dictionaries.put(methodName, dictionary);
    }
    return dictionaries;
  }

  /**
   * Does a simple invoke on the object given
   * @param object Invoke object
   * @param methodName Name of the method
   */

  private Object invoke(Object object, String methodName) throws Exception {
    return object.getClass().getMethod(
            methodName).invoke(object);
  }

  @SimpleFunction(description = "Returns the ID of the specified component.")
  public String GetId(Component component) {
    return COMPONENT_IDS.getOrDefault(component, "");
  }

  @Deprecated
  @SimpleFunction(description = "Do NOT use this function. Use 'GetComponentMeta' as a replacement.")
  public String GetName(Component component) {
    return component.getClass().getName();
  }

  @SimpleFunction(description = "Returns the position of the specified component according to its parent view. Index begins at one.")
  public int GetOrder(AndroidViewComponent component) {
    // (non null)
    View mComponent = component.getView();
    ViewGroup mParent = (ViewGroup) mComponent.getParent();

    if (isNotEmptyOrNull(mComponent) && isNotEmptyOrNull(mParent)) {
      return mParent.indexOfChild(mComponent) + 1;
    }
    return 0;
  }

  @SimpleFunction(description = "Get a properties value.")
  public Object GetProperty(Component component, String name) {
    return Invoke(component, name, YailList.makeEmptyList());
  }

  @SimpleFunction(description = "Get meta data about properties for the specified component, including their values.")
  public YailDictionary GetPropertyMeta(Component component) {
    Method[] mMethods = component.getClass().getMethods();
    YailDictionary mProperties = new YailDictionary();

    for (Method mMethod : mMethods) {
      DesignerProperty mDesignerAnnotation = mMethod.getAnnotation(DesignerProperty.class);
      boolean mHasDesigner = isNotEmptyOrNull(mDesignerAnnotation);
      boolean mHasProperty;
      SimpleProperty mPropertyAnnotation = mMethod.getAnnotation(SimpleProperty.class);
      String mName = mMethod.getName();
      YailDictionary mPropertyMeta = new YailDictionary();
      Object mValue = Invoke(component, mName, new YailList());
      mHasProperty = isNotEmptyOrNull(mPropertyAnnotation);

      if (mHasProperty) {
        mPropertyMeta.put("description", mPropertyAnnotation.description());
        mPropertyMeta.put("category", mPropertyAnnotation.category());

        if (mHasDesigner) {
          YailDictionary mDesignerMeta = new YailDictionary();
          mDesignerMeta.put("defaultValue", mDesignerAnnotation.defaultValue());
          mDesignerMeta.put("editorArgs", mDesignerAnnotation.editorArgs());
          mDesignerMeta.put("editorType", mDesignerAnnotation.editorType());
          mPropertyMeta.put("designer", mDesignerMeta);
        }

        mPropertyMeta.put("isDeprecated", (isNotEmptyOrNull(mMethod.getAnnotation(Deprecated.class))));
        mPropertyMeta.put("isDesignerProperty", mHasDesigner);
        mPropertyMeta.put("userVisible", mPropertyAnnotation.userVisible());
        mPropertyMeta.put("value", mValue);
        mProperties.put(mName, mPropertyMeta);
      }
    }

    return mProperties;
  }

  @SimpleFunction(description = "Invokes a method with parameters.")
  public Object Invoke(Component component, String name, YailList parameters) {
    if (isNotEmptyOrNull(component)) {
      Method[] mMethods = component.getClass().getMethods();

      try {
        Object[] mParameters = parameters.toArray();
        Method mMethod = UTIL_INSTANCE.getMethod(mMethods, name, mParameters.length);

        Class<?>[] mRequestedMethodParameters = mMethod.getParameterTypes();

        for (int i = 0; i < mRequestedMethodParameters.length; i++) {
          final Object object = mParameters[i];
          final String value = String.valueOf(object);

          switch (mRequestedMethodParameters[i].getName()) {
            case "int":
              mParameters[i] = Integer.parseInt(value);
              break;
            case "float":
              mParameters[i] = Float.parseFloat(value);
              break;
            case "double":
              mParameters[i] = Double.parseDouble(value);
              break;
            case "java.lang.String":
              mParameters[i] = value;
              break;
            case "boolean":
              mParameters[i] = Boolean.parseBoolean(value);
              break;
          }
        }
        Object mInvokedMethod = mMethod.invoke(component, mParameters);
        return mInvokedMethod == null ? "" : mInvokedMethod;
      } catch (Exception e) {
        throw new YailRuntimeError(e.getMessage(), TAG);
      }
    } else {
      throw new YailRuntimeError("Component cannot be null.", TAG);
    }
  }

  @SimpleFunction(description = "Returns if the specified component was created by the Dynamic Components extension.")
  public boolean IsDynamic(Component component) {
    return COMPONENTS.containsValue(component);
  }

  @SimpleFunction(description = "Returns the last used ID.")
  public Object LastUsedID() {
    return lastUsedId;
  }

  @Deprecated
  @SimpleFunction(description = "Do NOT use this function. Use 'GetComponentMeta', 'GetEventMeta', 'GetFunctionMeta', and 'GetPropertyMeta' as replacements.")
  public String ListDetails(Component component) {
    return "";
  }

  @SimpleFunction(description = "Moves the specified component to the specified view.")
  public void Move(AndroidViewComponent arrangement, AndroidViewComponent component) {
    View mComponent = component.getView();

    ((ViewGroup) mComponent.getParent()).
            removeView(mComponent);

    ViewGroup mArrangement = (ViewGroup) arrangement.getView();
    ViewGroup mTarget = (ViewGroup) mArrangement.getChildAt(0);

    mTarget.addView(mComponent);
  }

  @Deprecated
  @SimpleFunction(description = "Do NOT use this function. Use 'GenerateID' as a replacement.")
  public String RandomUUID() {
    return GenerateID();
  }

  @SimpleFunction(description = "Removes the component with the specified ID from the layout/screen so the ID can be reused.")
  public void Remove(String id) {
    Object component = COMPONENTS.get(id);

    if (component == null) {
      return;
    }
    try {
      Method mMethod = findMethod("getView", component);
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

      final String[] closeMethods = new String[] {
              "onPause", "onDestroy"
      };

      for (String methodName : closeMethods) {
        final Method invokeMethod = findMethod(methodName, component);
        if (invokeMethod != null)
          invokeMethod.invoke(component);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    COMPONENTS.remove(id);
    COMPONENT_IDS.remove(component);
  }

  private Method findMethod(String name, Object component) {
    try {
      return component.getClass().getMethod(name);
    } catch (NoSuchMethodException e) {
      // The method(s) are not present
      // We just log a simple message
      // and ignore them
      Log.e(TAG, "[priority=low] method not found name '" +  name + "'");
    }
    return null;
  }

  @SimpleFunction(description = "Sets the order of the specified component according to its parent view. Typing zero will move the component to the end, index begins at one.")
  public void SetOrder(AndroidViewComponent component, int index) {
    View mComponent = component.getView();
    ViewGroup mParent = (ViewGroup) mComponent.getParent();

    mParent.removeView(mComponent);
    mParent.addView(mComponent,
            Math.min(index - 1, mParent.getChildCount()));
  }

  @SimpleFunction(description = "Set a property of the specified component, including those only available from the Designer.")
  public void SetProperty(Component component, String name, Object value) {
    Invoke(component, name, YailList.makeList(new Object[] {
            value
    }));
  }

  @SimpleFunction(description = "Set multiple properties of the specified component using a dictionary, including those only available from the Designer.")
  public void SetProperties(Component component, YailDictionary properties) throws Exception {
    JSONObject mProperties = new JSONObject(properties.toString());
    JSONArray mPropertyNames = mProperties.names();

    for (int i = 0; i < mProperties.length(); i++) {
      String name = mPropertyNames.getString(i);
      Object value = mProperties.get(name);
      Invoke(component, name, YailList.makeList(new Object[] { value }));
    }
  }

  @SimpleFunction(description = "Uses a JSON Object to create dynamic components. Templates can also contain parameters that will be replaced with the values which are defined from the parameters list.")
  public void Schema(AndroidViewComponent in, final String template, final YailList parameters) throws Exception {
    JSONObject mScheme = new JSONObject(template);
    String newTemplate = template;

    if (isNotEmptyOrNull(template) && mScheme.has("components")) {
      propertiesArray = new JSONArray();

      JSONArray mKeys = (mScheme.has("keys") ? mScheme.getJSONArray("keys") : null);

      if (isNotEmptyOrNull(mKeys) && mKeys.length() == parameters.length() - 1) {
        for (int i = 0; i < mKeys.length(); i++) {
          String keyPercent = "%" + mKeys.getString(i);
          String keyBracket = "{" + mKeys.getString(i) + "}";
          String value = parameters.getString(i).replace("\"", "");
          newTemplate = newTemplate.replace(keyPercent, value);
          newTemplate = newTemplate.replace(keyBracket, value);
        }
      }

      mScheme = new JSONObject(newTemplate);
      UTIL_INSTANCE.parse("", mScheme);
      propertiesArray.remove(0);

      for (int i = 0; i < propertiesArray.length(); i++) {
        if (!propertiesArray.getJSONObject(i).has("id")) {
          throw new YailRuntimeError("One or multiple components do not have a specified ID in the template.", TAG);
        }

        final JSONObject mJson = propertiesArray.getJSONObject(i);
        final String mId = mJson.getString("id");
        AndroidViewComponent mRoot = (!mJson.has("in") ? in : (AndroidViewComponent) GetComponent(mJson.getString("in")));
        final String mType = mJson.getString("type");

        ComponentListener listener = new ComponentListener() {
          @Override
          public void onCreation(Component component, String id) {
            try {
              if (Objects.equals(id, mId) && mJson.has("properties")) {
                JSONObject mProperties = mJson.getJSONObject("properties");
                JSONArray keys = mProperties.names();

                if (keys != null) {
                  for (int k = 0; k < keys.length(); k++) {
                    Invoke(
                            (Component) GetComponent(mId),
                            keys.getString(k),
                            YailList.makeList(new Object[] {
                                    mProperties.get(keys.getString(k))
                            })
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

      SchemaCreated(mScheme.getString("name"), parameters);
    } else {
      throw new YailRuntimeError("The template is empty, or is does not have any components.", TAG);
    }
  }

  @SimpleFunction(description = "Returns all IDs of components created with the Dynamic Components extension.")
  public YailList UsedIDs() {
    return YailList.makeList(COMPONENTS.keySet());
  }

  @SimpleProperty(description = "Returns the version of the Dynamic Components extension.")
  public int Version() {
    return DynamicComponents.class.getAnnotation(DesignerComponent.class).version();
  }

  @SimpleProperty(description = "Returns the version name of the Dynamic Components extension.")
  public String VersionName() {
    return DynamicComponents.class.getAnnotation(DesignerComponent.class).versionName();
  }
}
