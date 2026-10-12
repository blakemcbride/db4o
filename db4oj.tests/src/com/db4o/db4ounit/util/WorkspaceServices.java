/* Copyright (C) 2004 - 2006 Versant Inc. http://www.db4o.com */

package com.db4o.db4ounit.util;

import java.io.*;
import java.net.*;

import com.db4o.foundation.io.*;

import db4ounit.*;
import db4ounit.extensions.util.*;

/**
 * @sharpen.ignore
 */
public class WorkspaceServices {
	
	public static String workspacePath(String fname) {
		return Path4.combine(workspaceRoot(), fname);
	}
	
	public static String workspaceTestFilePath(String fname) {
		return Path4.combine(WorkspaceLocations.getTestFolder(), fname);
	}
	
	/**
	 * @sharpen.property
	 */
	public static String workspaceRoot() {
       String property = System.getProperty("dir.workspace");
        if(property != null){
            return property;
        }
		return findFolderWithChild(pathToClass(WorkspaceServices.class), "db4oj.tests");
	}
	
	static String pathToClass(Class clazz) {
		/*
		 * FIXME: This will fail for paths containing special chars (e.g. spaces).
		 * The obvious workarounds (going through URI or URLDecoder) are not available on JDK<1.2 / are not .NET compatible.
		 */ 
		final URL resource = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class");
		return new File(resource.getFile()).getParent();
	}
	
	static String findFolderWithChild(String baseFolder, String folderChild) {
		
		File test = new File(baseFolder, folderChild);		
		if (test.exists()) return test.getParent(); 
		
		if (getParentFile(test) == null) return null;
		
		// we should test against root folder... :)		
		return findFolderWithChild(getParentFile(test).getParent(), folderChild);
	}
	
	private static String findProperty(String fname, String property) {
		FileReader fileReader = null; 
		try {
			fileReader = new FileReader(new File(fname));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		BufferedReader reader = new BufferedReader(fileReader);
		while (true){
			String line = null;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(line == null){
				return null;
			}
			if (line.startsWith(property))
			{
				return line.substring(property.length() + 1);
			}
		}
	}
	
	public static String readProperty(String fname, String property) 
	{
		return readProperty(fname, property, false);
	}

	public static String readProperty(String fname, String property, boolean lenient)
	{
		String value = findProperty(fname, property);
		if (value != null) return value;
		if (lenient) return null;
		throw new IllegalArgumentException("property '" + property + "' not found in '" + fname + "'");
	}
	
	private static File getParentFile(File file){
        String path = file.getParent();
        if (path == null){
            return null;
        }
        return new File(path);
	}

	public static File configurableWorkspacePath(String configurableProperty, String defaultWorkspacePath) {
		final String path = System.getProperty(configurableProperty, workspacePath(defaultWorkspacePath));
		final File file = new File(IOServices.safeCanonicalPath(path));
		Assert.isTrue(file.exists(), path); 
		return file;
	}
	
	/**
	 * The javac of the running JDK, so that compiled classes match the running VM.
	 */
	public static String javacPath()
	{
		String bin = Path4.combine(System.getProperty("java.home"), "bin");
		for (String name : new String[] { "javac", "javac.exe" }) {
			String path = Path4.combine(bin, name);
			if (File4.exists(path)) {
				return path;
			}
		}
		return "javac";
	}

}
