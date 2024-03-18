package com.yusufcihan.DynamicComponents.classes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.util.YailDictionary;

public class Metadata {
  public static YailDictionary getComponentCommonInfo(Component component) {
    Class<?> mClass = component.getClass();
    DesignerComponent mDesignerAnnotation = mClass.getAnnotation(DesignerComponent.class);
    SimpleObject mObjectAnnotation = mClass.getAnnotation(SimpleObject.class);
    YailDictionary mMeta = new YailDictionary();

    if ((mDesignerAnnotation != null) && (mObjectAnnotation != null)) {
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
    } else if (!(mDesignerAnnotation != null) && (mObjectAnnotation != null)) {
      // Return some amount of metadata even if there is no @DesignerComponent annotation.
      mMeta.put("external", mObjectAnnotation.external());
      mMeta.put("package", mClass.getName());
      mMeta.put("type", mClass.getSimpleName());
    } else {
      // Return the least amount of metadata if no annotation is provided.
      mMeta.put("package", mClass.getName());
      mMeta.put("type", mClass.getSimpleName());
    }

    return mMeta;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static YailDictionary getComponentAnnotationInfo(Component component, Class annotationClass)
  throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    YailDictionary dictionaries = new YailDictionary();
    if (component == null) {
      return dictionaries;
    }
    for (Method method : component.getClass().getMethods()) {
      YailDictionary dictionary = new YailDictionary();
      Object annotation = method.getAnnotation(annotationClass);
      Class annotationC = annotation.getClass();
      final boolean isDeprecated = method.getAnnotation(Deprecated.class) != null;
      final String methodName = method.getName();

      if (annotation != null) {
        dictionary.put("description", annotationC.getMethod("description").invoke(annotation));
        dictionary.put("isDeprecated", isDeprecated);
        dictionary.put("userVisible", annotationC.getMethod("userVisible").invoke(annotation));
      }
      dictionary.put("isDeprecated", isDeprecated);
      dictionaries.put(methodName, dictionary);
    }
    return dictionaries;
  }

  public static YailDictionary getComponentPropertyInfo(Component component)
  throws IllegalAccessException, InvocationTargetException {
    Method[] mMethods = component.getClass().getMethods();
    YailDictionary mProperties = new YailDictionary();

    for (Method mMethod : mMethods) {
      DesignerProperty mDesignerAnnotation = mMethod.getAnnotation(DesignerProperty.class);
      SimpleProperty mPropertyAnnotation = mMethod.getAnnotation(SimpleProperty.class);
      YailDictionary mPropertyMeta = new YailDictionary();
      Object mValue = mMethod.invoke(component, new Object[] { });

      if (mPropertyAnnotation != null) {
        mPropertyMeta.put("description", mPropertyAnnotation.description());
        mPropertyMeta.put("category", mPropertyAnnotation.category());

        if (mDesignerAnnotation != null) {
          YailDictionary mDesignerMeta = new YailDictionary();
          mDesignerMeta.put("defaultValue", mDesignerAnnotation.defaultValue());
          mDesignerMeta.put("editorArgs", mDesignerAnnotation.editorArgs());
          mDesignerMeta.put("editorType", mDesignerAnnotation.editorType());
          mPropertyMeta.put("designer", mDesignerMeta);
        }

        mPropertyMeta.put("isDeprecated", mMethod.getAnnotation(Deprecated.class) != null);
        mPropertyMeta.put("isDesignerProperty", mDesignerAnnotation != null);
        mPropertyMeta.put("userVisible", mPropertyAnnotation.userVisible());
        mPropertyMeta.put("value", mValue);
        mProperties.put(mMethod.getName(), mPropertyMeta);
      }
    }

    return mProperties;
  }
}
