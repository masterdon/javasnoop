/*
 * Copyright, Aspect Security, Inc.
 *
 * This file is part of JavaSnoop.
 *
 * JavaSnoop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JavaSnoop is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaSnoop.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aspect.snoop.agent.manager;

import com.aspect.snoop.agent.classpath.manager.SmartURLClassPath;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javassist.ByteArrayClassPath;
import javassist.CtBehavior;
import javassist.LoaderClassPath;

public class InstrumentationManager {

    private HashMap<Integer,ClassHistory> modifiedClasses;

    private Instrumentation inst;
    private List<ClassLoader> classloaders;

    //private Logger logger = Logger.getLogger(InstrumentationManager.class);

    public List<String> getLoadedClassesAsStrings() {
        
        List<String> classes = new ArrayList<String>();
        for ( Class c  : inst.getAllLoadedClasses() ) {
            if ( ! c.isArray() ) {
                classes.add( c.getName() );
            }
        }
        return classes;
    }

    public List<Class> getLoadedClasses() {

        List<Class> classes = new ArrayList<Class>();
        for ( Class c : inst.getAllLoadedClasses() ) {
            classes.add( c );
        }
        return classes;
    }

    public InstrumentationManager(Instrumentation inst) {

        this.inst = inst;
        this.modifiedClasses  = new HashMap<Integer,ClassHistory>();
        this.classloaders = new ArrayList<ClassLoader>();

        ClassPool classPool = ClassPool.getDefault();
        
        // for applets we have to make sure we add all the proper code
        // sources (classpath entries, basically), since they could come
        // from remote URLs.

        HashMap<URL, SmartURLClassPath> urlSources = new HashMap<URL, SmartURLClassPath>();

        for ( Class c : inst.getAllLoadedClasses() ) {

            CodeSource cs = c.getProtectionDomain().getCodeSource();
            
            if ( cs != null && cs.getLocation() != null ) {

                URL url = cs.getLocation();

                SmartURLClassPath cp = urlSources.get(url);

                if ( cp == null ) {
                    cp = new SmartURLClassPath(url);
                    urlSources.put(url, cp);
                }

                cp.addClass(c.getName());

            }

            ClassLoader cl = c.getClassLoader();
            if ( cl != null && ! classloaders.contains(cl)) {
                classloaders.add(cl);
                classPool.insertClassPath(new LoaderClassPath(cl));
            }
        }

        for ( URL url : urlSources.keySet() ) {
            classPool.appendClassPath( urlSources.get(url) );
        }
        
    }

    public boolean hasClassBeenModified(String clazz)
            throws ClassNotFoundException {
        return hasClassBeenModified(Class.forName(clazz));
    }

    public boolean hasClassBeenModified(Class c) {
        return modifiedClasses.get(c.hashCode()) != null;
    }

    public void resetClass(Class clazz)
            throws ClassNotFoundException, UnmodifiableClassException {

        ClassHistory history = modifiedClasses.get(clazz.hashCode());

        if ( history != null ) {
            // re-instrument original code back in
            ClassDefinition def = new ClassDefinition(clazz, history.getOriginalClass());
            inst.redefineClasses(def);
            modifiedClasses.remove(clazz.hashCode());
        }
    }

    public void ensureClassIsLoaded(String clazz, ClassLoader loader)
            throws ClassNotFoundException {
        Class.forName(clazz, true, loader);
    }

    public void deinstrument(Class clazz)
            throws InstrumentationException {

        ClassHistory history = modifiedClasses.get(clazz.hashCode());

        try {
            
            if ( history == null ) {
                throw new InstrumentationException("Class to deinstrument '" + clazz.getName() + "' not found in history");
            }

            ClassDefinition definition = new ClassDefinition(clazz, history.getOriginalClass());
            inst.redefineClasses(definition);
            
        } catch (ClassNotFoundException cnfe) {
            throw new InstrumentationException(cnfe);
        } catch (UnmodifiableClassException cnfe) {
            throw new InstrumentationException(cnfe);
        }

    }

    public void instrument(
            Class clazz,
            MethodChanges[] methodChanges
            ) throws InstrumentationException {

        // step #1: get original class

        try {
     
            ClassPool classPool = ClassPool.getDefault();
            CtClass cls = classPool.get(clazz.getName());
            
            // get the original bytecode so we can change our mind later
            ClassHistory ch = modifiedClasses.get(clazz.hashCode());

            byte[] originalByteCode = null;
            byte[] lastVersionByteCode = null;
            
            if ( ch != null ) {
                // we're instrumented this class before. we've got to
                // be tricky here.
                originalByteCode = ch.getOriginalClass();
                //System.out.println("Restoring saved bytes for " + clazz.getName() + " (" + md5(originalByteCode) + ")");
                
                ClassPool cp = new ClassPool(classPool);
                cp.childFirstLookup = true;
                cp.insertClassPath(new ByteArrayClassPath(clazz.getName(),originalByteCode));
                cls = cp.get(clazz.getName());
                cp.childFirstLookup = false;
                //System.out.println("Retrieved bytes after save: " + md5(cls.toBytecode()));

                lastVersionByteCode = ch.getCurrentClass();
                
            } else {
                originalByteCode = cls.toBytecode();
                //System.out.println("New class " + clazz.getName() + " (" + md5(originalByteCode) + ")");
                lastVersionByteCode = originalByteCode;
            }

            // unfreeze the class so we can modify it
            cls.defrost();

            for ( MethodChanges change : methodChanges ) {
                UniqueMethod methodToChange = change.getUniqueMethod();

                // get the parameters in order so we can get the method to instrument
                String[] parameterTypes = methodToChange.getParameterTypes();

                CtClass[] classes = new CtClass[parameterTypes.length];

                //System.out.println(clazz.getName() + ": " + change.getUniqueMethod().getName() + "(" + parameterTypes.length);
                
                for(int i=0;i<parameterTypes.length;i++) {
                    classes[i] = classPool.get(parameterTypes[i]);
                }

                // get the method to instrument
                String methodName = methodToChange.getName();
                CtBehavior method = null;

                if ( "<init>".equals(methodName)) {
                    method = cls.getDeclaredConstructor(classes);
                } else {
                    method = cls.getDeclaredMethod(methodName, classes);
                }

                // instrument the method, adding any necessary vars first
                LocalVariable[] newVars = change.getNewLocalVariables();

                for(int i=0;i<newVars.length;i++) {
                    LocalVariable newVar = newVars[i];
                    method.addLocalVariable(newVar.getName(), newVar.getType());
                }

                method.insertBefore( " { " + change.getNewStartSrc() + " } ");
                method.insertAfter( " { " + change.getNewEndSrc() + " } ");

            }
           
            // save the instrumented version of the class
            byte[] newByteCode = cls.toBytecode();

            ClassDefinition definition = new ClassDefinition(clazz, newByteCode);

            try {

                inst.redefineClasses(definition);

            } catch (VerifyError error) {
                //logger.error(error);
            }

            // save the original
            ClassHistory history = new ClassHistory(clazz,originalByteCode,newByteCode);
            history.setLastClass(lastVersionByteCode);
            modifiedClasses.put(clazz.hashCode(), history);

        } catch (UnmodifiableClassException uce) {
            throw new InstrumentationException(uce);
        } catch (ClassNotFoundException cnfe) {
            throw new InstrumentationException(cnfe);
        } catch (IOException ioe) {
            throw new InstrumentationException(ioe);
        } catch (CannotCompileException cce) {
            throw new InstrumentationException(cce);
        } catch (NotFoundException nfe) {
            throw new InstrumentationException(nfe);
        }

    }

    
    public byte[] getClassBytes(String clazz) {

        try {

            CtClass cls = ClassPool.getDefault().get(clazz);

            return cls.toBytecode();

        } catch (IOException ex) {
            //logger.error(ex);
        } catch (CannotCompileException ex) {
            //logger.error(ex);
        } catch (NotFoundException ex) {
            // this will occasionally with applet-loading related classes (com.sun.deploy, sun.reflect, etc.)
        }
        
        return null;
    }
    
    
    public Class getFromAllClasses(String className) throws ClassNotFoundException {
        Class[] allClasses = inst.getAllLoadedClasses();

        for ( Class c : allClasses ) {
            if ( c.getName().equals(className)) {
                return c;
            }
        }

        throw new ClassNotFoundException(className);
    }

    public Class getFromAllClasses(int hash) throws ClassNotFoundException {
        Class[] allClasses = inst.getAllLoadedClasses();

        for ( Class c : allClasses ) {
            if ( c.hashCode() == hash) {
                return c;
            }
        }

        throw new ClassNotFoundException("For hash: " + hash);
    }

    public void resetAllClasses() throws InstrumentationException {
        for(Integer i: modifiedClasses.keySet()) {
            try {
                Class c = getFromAllClasses(i.intValue());
                deinstrument(c);
            } catch (ClassNotFoundException e) {
                //logger.error("Couldn't find class from hash " + i);
            }
            
        }
    }

    public static String md5(byte[] bytes) {

        String res = "";

        try {

            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(bytes);

            byte[] md5 = algorithm.digest();

            String tmp = "";

            for (int i = 0; i < md5.length; i++) {

                tmp = (Integer.toHexString(0xFF & md5[i]));

                if (tmp.length() == 1) {
                    res += "0" + tmp;
                } else {
                    res += tmp;
                }

            }

        } catch (NoSuchAlgorithmException ex) { }

        return res;

    }

    
}
