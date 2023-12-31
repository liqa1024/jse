package jtool.code.script;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;

/**
 * @author liqa
 * <p> Groovy Script Object for matlab usage </p>
 * <p> Now is a wrapper of {@link GroovyObject} </p>
 */
public class ScriptObjectGroovy implements IScriptObject {
    private final GroovyObject mObj;
    ScriptObjectGroovy(GroovyObject aObj) {mObj = aObj;}
    @Override public Object unwrap() {return mObj;}
    
    @Override public Object invokeMethod(String name, Object args) {return of(mObj.invokeMethod(name, args));}
    @Override public Object getProperty(String propertyName) {return of(mObj.getProperty(propertyName));}
    @Override public void setProperty(String propertyName, Object newValue) {mObj.setProperty(propertyName, newValue);}
    @Override public MetaClass getMetaClass() {return mObj.getMetaClass();}
    @Override public void setMetaClass(MetaClass metaClass) {mObj.setMetaClass(metaClass);}
    
    /** 主要用来判断是否需要外包这一层 */
    public static Object of(Object aObj) {return (!(aObj instanceof ScriptObjectGroovy) && (aObj instanceof GroovyObject)) ? (new ScriptObjectGroovy((GroovyObject)aObj)) : aObj;}
}
