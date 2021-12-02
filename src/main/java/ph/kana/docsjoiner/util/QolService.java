package ph.kana.docsjoiner.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.*;

public class QolService {

    private static final Path LAST_OPEN_DIR_CACHE_FILE = Path
        .of(System.getProperty("java.io.tmpdir"), ".file-joiner_last-open-dir");
    private static final Path DEFAULT_OPEN_DIR = Path.of(System.getProperty("user.home"));

    public Path fetchInitialOpenDir() {
        try {
            if (exists(LAST_OPEN_DIR_CACHE_FILE)) {
                return Path.of(readString(LAST_OPEN_DIR_CACHE_FILE).trim());
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return DEFAULT_OPEN_DIR;
    }

    public void saveLastOpenDir(List<File> openedFiles) {
        File currentFile = openedFiles.get(0);
        while (!currentFile.isDirectory()) {
            currentFile = currentFile.getParentFile();
        }

        var absolutePath = currentFile.toPath()
            .toAbsolutePath()
            .toString();
        try {
            writeString(LAST_OPEN_DIR_CACHE_FILE, absolutePath);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
