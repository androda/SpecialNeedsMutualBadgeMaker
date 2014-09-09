package com.specialneedsmutual.badgemaker.helpers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import com.specialneedsmutual.badgemaker.utilities.PropertyFileReader;

/**
 * Convenience class which holds information about the selected source directory:<br>
 * -Number of JPG images in the directory<br>
 * -The number of pages it will take to show all the images<br>
 * -The file separator character<br>
 * -The child files in the directory
 * 
 * @author jacob.carter
 * 
 */
public class SourceDirectoryInfo {

	public final String		sourceDir;
	public final String[]	childFiles;
	public final String		fileSeparator;
	public final int		numberOfImages;
	public final int		numberOfPages;

	/**
	 * Creates a new instance and populates the data fields
	 * 
	 * @param sourceDir The source, like "c:\program files\someApplication"
	 * @throws IOException If something goes wrong
	 */
	public SourceDirectoryInfo(File sourceFile) throws IOException {
		File parentDirFile;
		int badgesPerPage;

		this.sourceDir = sourceFile.getCanonicalPath();
		parentDirFile = new File(this.sourceDir);

		childFiles = parentDirFile.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if(filename.startsWith("._")){
					return false;
				} else if (filename.endsWith(".jpg")) {
					return true;
				} else if (filename.endsWith(".JPG")) {
					return true;
				}
				return false;
			}
		});
		numberOfImages = childFiles.length;
		badgesPerPage = Integer.parseInt(PropertyFileReader.getProperty("badgesPerPage", "3"));
		numberOfPages = (int) Math.ceil((float) numberOfImages / (float) badgesPerPage);
		fileSeparator = System.getProperty("file.separator", "/");
	}

}
