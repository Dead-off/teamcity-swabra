/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.swabra;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.HashMap;

/**
 * User: vbedrosova
 * Date: 01.06.2009
 * Time: 17:09:05
 */
public class FileDirectorySnapshot extends DirectorySnapshot {
  private static final String SEPARATOR = "\t"; 

  private final File myWorkingDir;
  private Writer mySnapshotWriter;
  private BufferedReader mySnapshotReader;

  public FileDirectorySnapshot(File workingDir) {
    myWorkingDir = workingDir;
  }

  public void snapshot(@NotNull File dir, @NotNull SwabraLogger logger, boolean verbose) {
    try {
      mySnapshotWriter = new FileWriter(new File(myWorkingDir, dir.getName() + ".snapshot"));
      mySnapshotWriter.write("#Don't edit this file!\n");
      saveState(dir);
      mySnapshotWriter.close();
    } catch (Exception e) {
      logger.log("Unable to save working directory snapshot to file", false);
    }
  }

  public void collectGarbage(@NotNull File dir, @NotNull SwabraLogger logger, boolean verbose) {
    final File snapshot = new File(myWorkingDir, dir.getName() + ".snapshot");
    if (!snapshot.exists()) {
      logger.log("Unable to read working directory snapshot from file, no file exists", false);
      return;
    }
    myFiles = new HashMap<String, FileInfo>();
    try {
      mySnapshotReader = new BufferedReader(new FileReader(new File(myWorkingDir, dir.getName() + ".snapshot")));
      mySnapshotReader.readLine(); // read first comment
      String fileRecord = mySnapshotReader.readLine();
      while (fileRecord != null) {
        final int firstSeparator = fileRecord.indexOf(SEPARATOR);
        final int secondSeparator = fileRecord.indexOf(SEPARATOR, firstSeparator + 1);
        final String path = fileRecord.substring(0, firstSeparator);
        final String length = fileRecord.substring(firstSeparator + 1, secondSeparator);
        final String lastModified = fileRecord.substring(secondSeparator + 1);
        myFiles.put(path, new FileInfo(Long.parseLong(length), Long.parseLong(lastModified)));
        fileRecord = mySnapshotReader.readLine();
      }
    } catch (Exception e) {
      logger.log("Unable to read working directory snapshot from file", false);
      return;
    } finally {
      try {
        mySnapshotReader.close();
      } catch (IOException e) {
        logger.log("Unable to read working directory snapshot from file", false);
      }
    }
    if (!snapshot.delete()) {
      logger.log("Unable to delete file containig directory snapshot", false);      
    }
    super.collectGarbage(dir, logger, verbose);
  }

  void saveFileState(@NotNull final File file) throws Exception {
    mySnapshotWriter.write(file.getAbsolutePath() + SEPARATOR + file.length() +  SEPARATOR + file.lastModified() + "\n");
  }
}
