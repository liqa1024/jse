package com.jtool.code;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;

/**
 * @author liqa
 * <p> Groovy Script Object for matlab usage </p>
 * <p> Now is a wrapper of {@link GroovyObject} </p>
 */
public class ScriptObjectGroovy implements IScriptObject {
    private final GroovyObject mObj;
    public ScriptObjectGroovy(GroovyObject aObj) {mObj = aObj;}
    
    @Override public Object invokeMethod(String name, Object args) {return mObj.invokeMethod(name, args);}
    @Override public Object getProperty(String propertyName) {return mObj.getProperty(propertyName);}
    @Override public void setProperty(String propertyName, Object newValue) {mObj.setProperty(propertyName, newValue);}
    @Override public MetaClass getMetaClass() {return mObj.getMetaClass();}
    @Override public void setMetaClass(MetaClass metaClass) {mObj.setMetaClass(metaClass);}
}
