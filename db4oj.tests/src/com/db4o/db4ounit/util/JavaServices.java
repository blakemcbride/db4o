/* Copyright (C) 2007  Versant Inc.  http://www.db4o.com */

package com.db4o.db4ounit.util;

import java.io.*;
import java.util.*;

import com.db4o.foundation.*;
import com.db4o.foundation.io.*;

import db4ounit.Assert;
import db4ounit.extensions.util.IOServices;
import db4ounit.extensions.util.IOServices.ProcessRunner;


/**
 * @sharpen.ignore
 */
public class JavaServices {

    public static String java(String className) throws IOException, InterruptedException{
        return IOServices.exec(javaExecutable(), javaRunArguments(className));
    }
    
    public static String java(String className, String[] args) throws IOException, InterruptedException{
        return IOServices.exec(javaExecutable(), javaRunArguments(className, args, false));
    }

    public static ProcessRunner startJava(String className, String[] args) throws IOException {
        return IOServices.start(javaExecutable(), javaRunArguments(className, args, false));
    }

	public static String javac(String srcFile) throws IOException, InterruptedException
	{
			String[] javacArgs =
				new String[]{
					"-classpath",
					IOServices.joinArgs(
	        				File.pathSeparator,
	        				new String[]{
	        						currentClassPath(),
	        						db4oClassesPath("core"), 
	        						db4oClassesPath("optional"),
	        						db4oClassesPath("cs"),
	        				}, runningOnWindows()),
	        		srcFile};

			return IOServices.exec(WorkspaceServices.javacPath(),javacArgs);
	}	
	
    public static String startAndKillJavaProcess(String className, String expectedOutput, long timeout) throws IOException{
        return IOServices.execAndDestroy(javaExecutable(), javaRunArguments(className), expectedOutput, timeout);
    }

    private static String javaExecutable() {
        for (int i = 0; i < vmTypes.length; i++) {
            if(vmTypes[i].identified()){
                return vmTypes[i].executable();
            }
        }
        throw new NotImplementedException("VM " + vmName() + " not known. Please add as JavaVM class to end of JavaServices class.");
    }
    
    private static String[] javaRunArguments(String className) {
    	return javaRunArguments(className, new String[0], true);
    }

    private static String[] javaRunArguments(String className, String[] args, boolean includeDb4oJars) {
    	String[] allArgs = new String[args.length + 3];
    	allArgs[0] = "-classpath";
    	
    	List<String> cp = new ArrayList<String>();
    	cp.add(JavaServices.javaTempPath());
    	cp.add(currentClassPath());
    	if (includeDb4oJars) {
    		cp.add(db4oClassesPath("core"));
    		cp.add(db4oClassesPath("optional"));
    		cp.add(db4oClassesPath("cs"));
    	}
    	
    	allArgs[1] = IOServices.joinArgs(
						File.pathSeparator,
						cp.toArray(new String[cp.size()]), 
						runningOnWindows());
        allArgs[2] = className;
        System.arraycopy(args, 0, allArgs, 3, args.length);
        return allArgs;        
    }

    private static String currentClassPath(){
        return property("java.class.path");
    }
    
    static String javaHome(){
        return property("java.home");
    }
    
    static String vmName(){
        return property("java.vm.name");
    }
    
    static String property(String propertyName){
        return System.getProperty(propertyName);
    }
    
    private static final JavaVM[] vmTypes = new JavaVM[]{
        new JavaRumtime(),
        new DalvikRuntime(),
    };
    
    static interface JavaVM {
        boolean identified();
        String executable();
    }
    
    static class JavaRumtime implements JavaVM{
        public String executable() {
            return  Path4.combine(Path4.combine(javaHome(), "bin"), "java");
        }
        public boolean identified() {
            // Any VM other than Dalvik; VM names vary ("Java HotSpot(TM) ...", "OpenJDK ...", ...).
            return ! DalvikRuntime.isDalvik();
        }
    }
    
    static class DalvikRuntime implements JavaVM{
        public String executable() {
            return  Path4.combine(Path4.combine(javaHome(), "bin"), "dalvikvm");
        }
        public boolean identified() {
            return isDalvik();
        }
        static boolean isDalvik() {
            return "Dalvik".equals(System.getProperty("java.vm.name"));
        }
    }
    
	/**
	 * The compiled classes of a db4o module, as built by BLD ("./bld build").
	 */
	public static String db4oClassesPath(String module)
	{
		return WorkspaceServices.workspacePath("target/classes/" + module);
	}
	
	public static String javaTempPath()
	{
		return IOServices.buildTempPath("java");
	}
	
	private static boolean runningOnWindows() {
		String osName = System.getProperty("os.name");
		if(osName == null) {
			return false;
		}
		return osName.indexOf("Win") >= 0;
	}
	
}
