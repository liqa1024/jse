package com.jtool.code;

import groovy.lang.MetaClass;
import jep.python.PyCallable;
import jep.python.PyObject;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * @author liqa
 * <p> Python Script Object </p>
 */
public class ScriptObjectPython implements IScriptObject {
    private final PyObject mPyObj;
    public ScriptObjectPython(PyObject aPyObj) {mPyObj = aPyObj;}
    
    @Override public Object invokeMethod(String name, Object args) {return mPyObj.getAttr(name, PyCallable.class).call(args);}
    @Override public Object getProperty(String propertyName) {return mPyObj.getAttr(propertyName);}
    @Override public void setProperty(String propertyName, Object newValue) {mPyObj.setAttr(propertyName, newValue);}
    
    
    private MetaClass mDelegate = InvokerHelper.getMetaClass(PyObject.class);
    @Override public MetaClass getMetaClass() {return mDelegate;}
    @Override public void setMetaClass(MetaClass metaClass) {mDelegate = metaClass;}
}
