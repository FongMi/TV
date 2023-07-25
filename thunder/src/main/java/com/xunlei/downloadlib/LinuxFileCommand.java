package com.xunlei.downloadlib;

import java.io.IOException;

/**
 * File operators. Like: delete, create, move...
 * Use linux shell to implement these operators.
 * So, only for linux like operator system. 
 * All return the execute process, and if the requested program can not be executed,
 * may throws IOException.
 * And all files or diretorys String parameters are absolute path.
 * The return new {@code Process} object that represents the native process.
 * */
public class LinuxFileCommand {
	public final Runtime shell;
	//private static final int c = 0;
	/**
	 * Constructor. /data/busybox/
	 * @param
	 * */
	public LinuxFileCommand(Runtime runtime) {
		// TODO Auto-generated constructor stub
		shell = runtime;
	}
	
	/** Delete {@code file} file, nerver prompt*/
	public final Process deleteFile(String file) throws IOException{
		String[] cmds = {"rm", file};
		return shell.exec(cmds);
	}
	
	public final Process delete(String file) throws IOException {
		String[] cmds = {"rm", "-r", file};
		return shell.exec(cmds);
	}
	
	public final Process deleteMult(String[] cmds) throws IOException{
		return shell.exec(cmds);
	}
	
	/** Delete {@code dire} directory, nerver prompt*/
	public final Process deleteDirectory(String dire) throws IOException{
		String[] cmds = {"rm", "-r", dire};
		return shell.exec(cmds);
	}
	
	/** Create {@code file} file, if file has already exist, update the
	 * file access and modification time to current time.
	 * @throws IOException 
	 * */
	public final Process createFile(String file) throws IOException {
		String[] cmds = {"touch", file};
		return shell.exec(cmds);
	}
	
	/** Create directory. If parent path is not existed, also create parent directory
	 * @throws IOException */
	public final Process createDirectory(String dire) throws IOException{
		String[] cmds = {"mkdir", dire};
		return shell.exec(cmds);
	}
	
	/** Move or rename(if they in the same directory) file or directory
	 * @throws IOException */
	public final Process moveFile(String src, String dir) throws IOException{
		String[] cmds = {"mv", src, dir};
		return shell.exec(cmds);
	}
	
	/** Copy file
	 * @throws IOException */
	public final Process copyFile(String src, String dir) throws IOException{
		String[] cmds = {"cp", "-r", src, dir};
		return shell.exec(cmds);
	}
	
	/**`
	 * File soft Link
	 * @throws IOException 
	 * */
	public final Process linkFile(String src, String dir) throws IOException{
		String[] cmds = {"ln", "-l", src, dir};
		return shell.exec(cmds);
	}
	
	
	public final Process du_s(String file) throws IOException{
		String[] cmds = {"du", "-s", file};
		return shell.exec(cmds);
	}

	public final Process ls_lhd(String file) throws IOException{
		String[] cmds = {"ls", "-l", file};
		return shell.exec(cmds);
	}
	
	public final Process ls_Directory(String directory) throws IOException{
		if (directory.equals("/"))
			directory = "";
		String[] cmds = {"ls", "-a", directory};
		return shell.exec(cmds);
	}
	
}
