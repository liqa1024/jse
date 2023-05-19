package com.jtool.code;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;

/**
 * @author liqa
 * <p> Script Object for matlab usage </p>
 * <p> Now is a wrapper of {@link GroovyObject} </p>
 */
public class ScriptObject implements GroovyObject {
    private final GroovyObject mObj;
    public ScriptObject(GroovyObject aObj) {mObj = aObj;}
    
    @Override public Object invokeMethod(String name, Object args) {return mObj.invokeMethod(name, args);}
    @Override public Object getProperty(String propertyName) {return mObj.getProperty(propertyName);}
    @Override public void setProperty(String propertyName, Object newValue) {mObj.setProperty(propertyName, newValue);}
    @Override public MetaClass getMetaClass() {return mObj.getMetaClass();}
    @Override public void setMetaClass(MetaClass metaClass) {mObj.setMetaClass(metaClass);}
}
