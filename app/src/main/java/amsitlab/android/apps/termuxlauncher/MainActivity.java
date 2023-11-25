package amsitlab.android.apps.termuxlauncher;
// vim:foldmethod=syntax


import android.os.Bundle;
import android.os.Environment;

import android.app.Activity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.util.Log;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;


public class MainActivity extends Activity{
	public static final String LOG_TAG = "termux-applist";

	public static final String EXTERNAL_PATH_NAME = "termuxlauncher";

	public static final String EXTERNAL_LAUNCH_FILE_NAME = ".apps-launcher";
	public static final String EXTERNAL_ALIAS_FILE_NAME = "aliasses.txt";

	private static final String LAUNCH_SCRIPT_COMMENT = ""
		+ "## This script created by Termux Launcher.\n"
		+ "## Do not change anything !!!\n"
		+ "## Your changes will be override!!!\n\n"
		+ "## Recomended:\n"
		+ "##\tsource this file to your\n"
		+ "##\t~/.bashrc or ~/.bash_profile to launch app with\n"
		+ "##\tcommand on termux by calling\n"
		+ "##\t'launch [appname]'\n\n"
		+ "## Author : Amsit (@amsitlab) <dezavue3@gmail.com>\n\n\n\n";

	private static final String LAUNCH_SCRIPT_START = ""
		+ "launch(){\n"
		+ "\tname=\"$(echo \"$1\" | tr 'A-Z' 'a-z' )\"\n"
		+ "\tcase \"$name\" in\n"
		+ "\t\t--help|-h)\n"
		+ "\t\tprintf \"Usage:\\n\"\n"
		+ "\t\tprintf \"\\tlaunch [appname]\\n\"\n"
		+ "\t\tprintf \"\\t\\tLaunching application\\n\"\n"
		+ "\t\tprintf \"\\t\\tType 'launch --list' to view list of applications name.\\n\"\n"
		+ "\t\tprintf \"\\t\\tExample: launch whatsapp\\n\\n\"\n"
		+ "\t\tprintf \"\\tlaunch [--list|-l]\\n\"\n"
		+ "\t\tprintf \"\\t\\tDisplaying available apps\\n\\n\"\n"
		+ "\t\tprintf \"\\tlaunch [--help|-h]\\n\"\n"
		+ "\t\tprintf \"\\t\\tDisplaying this message and exit .\\n\\n\"\n"
		+ "\t\t;;\n";

	private static final String LAUNCH_SCRIPT_END = ""
		+ "\t\t--list|-l)\n"
		+ "{{applications_list}}"
		+ "\n"
		+ "\t\t;;\n"
		+ "\t\t*)\n"
		+ "\t\tprintf \"Unknown command: type 'launch --help' for detail .\\n\"\n"
		+ "\t\treturn 1\n"
		+ "\t\t;;\n"
		+ "\tesac\n"
		+ "}";

	/** termux intent */
	private static Intent myIntent;

	/** Public external storage path name */
	private static File sExternalPath = Environment.getExternalStoragePublicDirectory(EXTERNAL_PATH_NAME);

	/** Launch file **/
	private static File sLaunchFile;

	/** user's alias file **/
	private static File sAliasFile;
	private static List<String> aliassedAppNames;
	private static List<String> aliasses;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		readAliasFile();
		createLaunchFile();
		createTermuxIntent();
		startActivity(myIntent);
	}

	@Override
	public void onResume(){
		super.onResume();
		readAliasFile();
		createLaunchFile();
		createTermuxIntent();
		startActivity(myIntent);
	}

	@Override
	public void onPause(){
		super.onPause();
		readAliasFile();
		createLaunchFile();
		createTermuxIntent();
		startActivity(myIntent);
	}

	private void createTermuxIntent(){
		if(null == myIntent){
			myIntent = getPackageManager().getLaunchIntentForPackage("com.termux");
		}
	}

	private static void createExternalPath(){
		if(!sExternalPath.exists()){
			sExternalPath.mkdirs();
		}
	}

	private static void readAliasFile(){
		sAliasFile = new File(sExternalPath,EXTERNAL_ALIAS_FILE_NAME);
		aliassedAppNames = new ArrayList<String>();
		aliasses = new ArrayList<String>();
		if(!sAliasFile.exists()) return;
		try {
			Scanner reader = new Scanner(sAliasFile);
			while(reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] data = line.split("=",2);
				aliasses.add(data[0]);
				aliassedAppNames.add(data[1]);
			}
			reader.close();
		} catch(Exception e) {
			Log.e(LOG_TAG,"Could not Read from " + sAliasFile.toString());
			return;
		}
		return;
		
	}

	private void createLaunchFile(){
		sLaunchFile = new File(sExternalPath,EXTERNAL_LAUNCH_FILE_NAME);


		new Thread(){
			public void run(){

				try{
					// Always up to date
					if(sLaunchFile.exists()){
						sLaunchFile.delete();
					}

					createExternalPath();
					final FileOutputStream fos = new FileOutputStream(sLaunchFile);
					final PrintStream printer = new PrintStream(fos);
					final PackageManager pm = getApplicationContext().getPackageManager();
					List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
					printer.print(LAUNCH_SCRIPT_COMMENT);
					printer.print(LAUNCH_SCRIPT_START);

					final StringBuilder appNameList = new StringBuilder();
					for(ApplicationInfo pkg : packages){
						String pkgName = pkg.packageName;
						String originalAppName = pkg.loadLabel(pm).toString();
						String appName = originalAppName.toLowerCase();
						Intent intent = pm.getLaunchIntentForPackage(pkgName);
						boolean isSystemApp = ((pkg.flags & ApplicationInfo.FLAG_SYSTEM) == 1) ? true : false;
						Log.d(LOG_TAG,"[" + intent + "] : [" + pkgName + "] : [" + isSystemApp + "] : [" + "] : [" + appName + "]");
						if(intent == null){
							continue;
						}
						String componentName = intent.getComponent().flattenToShortString();
						String toRemove = "'\"()&{}$!<>#+*";
						for(int i=0;i<toRemove.length();i++){
							appName = appName.replace(""+toRemove.charAt(i),"");
						}
						appName = appName.replace(" ","-");
						appNameList.append("\t\tprintf \"");
						appNameList.append( appName );
						appNameList.append("\\n\"\n");

						printer.print("\t\t" + appName);

						for(int i=0; i<aliassedAppNames.size();i++){
							if(appName.equals(aliassedAppNames.get(i).toLowerCase())){
								printer.print("|"+aliasses.get(i));
							}
						}
						printer.print(""
								+ ")\n"
								+ "\t\tam start -n '"
								+ componentName
								+ "' --user 0 &> /dev/null\n"
								+ "\t\tprintf \"Launching '"
								+ originalAppName.replace("\"","\\\"")
								+ "'\\n\"\n"
								+ "\t\t;;\n"
							     );

					}

					printer.print(LAUNCH_SCRIPT_END.replace("{{applications_list}}",appNameList.toString()));


					printer.flush();
					printer.close();
					fos.flush();
					fos.close();
				} catch(IOException ioe) {
					Log.e(LOG_TAG,"Could not write to " + sLaunchFile.toString());
				}

			}

		}.start();
	}


}
