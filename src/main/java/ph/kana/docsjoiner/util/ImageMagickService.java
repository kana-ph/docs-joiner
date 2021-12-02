package ph.kana.docsjoiner.util;

import ph.kana.docsjoiner.ui.InputFile;
import ph.kana.docsjoiner.ui.IntPair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import static java.nio.file.Files.*;

public class ImageMagickService {

    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"), "docs-joiner-temp");
    private static final Logger logger = Logger.getLogger(ImageMagickService.class.getName());

    public String imageMagickVersion() {
        try {
            var identifyVersion = executeCommand(new String[] { "identify", "-version" });
            return identifyVersion.lines()
                .findFirst()
                .orElse(null);
        } catch (InterruptedException | IOException e) {
            logger.severe(e::getMessage);
        }
        return null;
    }

    public InputFile identifyFile(InputFile inputFile) {
        var path = inputFile.getFile();

        try {
            var result = executeCommand(identifyCommand(path));
            var tokens = result.split(" ");

            var dimension = new IntPair(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
            var density = new IntPair((int) Double.parseDouble(tokens[2]), (int) Double.parseDouble(tokens[3]));

            inputFile.setDimension(dimension);
            inputFile.setDensity(density);
        } catch (InterruptedException | IOException e) {
            logger.severe(e::getMessage);
        }
        return inputFile;
    }

    public Path joinFiles(List<InputFile> sourceFiles, Path targetPath) {
        int minWidth = sourceFiles.stream()
            .map(InputFile::getDimension)
            .mapToInt(IntPair::first)
            .min()
            .orElse(-1);
        if (minWidth < 0) {
            throw new RuntimeException("No source files!");
        }

        int maxDensity = sourceFiles.stream()
            .map(InputFile::getDensity)
            .mapToInt(IntPair::max)
            .max()
            .orElse(72);

        try {
            ensureTempDirExists();

            var convertedFiles = prepareFiles(sourceFiles, minWidth, maxDensity);
            executeCommand(joinFilesCommand(convertedFiles, targetPath));
            return targetPath;
        } catch (InterruptedException | IOException e) {
            logger.severe(e::getMessage);
        } finally {
            cleanupTempDir();
        }

        return null;
    }

    private List<Path> prepareFiles(List<InputFile> inputFiles, int width, int density) throws InterruptedException, IOException {
        var preparedFiles = new ArrayList<Path>();
        for (int i =0; i < inputFiles.size(); i++) {
            var inputFile = inputFiles.get(i);
            var tempFile = TEMP_DIR.resolve(String.format("file-%03d.jpg", i));

            if (width == inputFile.getDimension().first() && density == inputFile.getDensity().max()) {
                copy(inputFile.getFile(), tempFile);
            } else {
                executeCommand(convertCommand(inputFile, tempFile, width, density));
            }
            preparedFiles.add(tempFile);
        }

        return preparedFiles;
    }

    private static void ensureTempDirExists() throws IOException {
        if (notExists(TEMP_DIR)) {
            Files.createDirectory(TEMP_DIR);
        }
    }

    private static void cleanupTempDir() {
        try (var tmpDirStream = walk(TEMP_DIR)) {
            tmpDirStream.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            logger.severe(e::getMessage);
        }
    }

    private static String executeCommand(String[] command) throws InterruptedException, IOException {
        logger.info("Executing command: " + Arrays.toString(command));
        var process = Runtime.getRuntime()
            .exec(command);
        process.waitFor();
        var stdoutBytes = process.getInputStream()
            .readAllBytes();
        return new String(stdoutBytes);
    }

    private static String[] identifyCommand(Path path) {
        return new String[] {
            "identify",
            "-format",
            "%w %h %x %y",
            path.toString()
        };
    }

    private static String[] convertCommand(InputFile inputFile, Path target, int width, int density) {
        var command = new ArrayList<String>();
        command.add("convert");
        command.add(inputFile.getFile().toString());

        double inputWidth = inputFile.getDimension().first();
        if (width != inputWidth) {
            command.add("-resize");
            int ratio = (int) (((double) width / inputWidth) * 100);
            command.add(ratio + "%");
        }

        int inputMaxDensity = inputFile.getDensity().max();
        if (density != inputMaxDensity) {
            command.add("-density");
            command.add(Integer.toString(density));
        }
        command.add(target.toString());

        return command.toArray(new String[0]);
    }

    private static String[] joinFilesCommand(List<Path> sourceFiles, Path outputFile) {
        var command = new ArrayList<String>();
        command.add("convert");
        sourceFiles.stream()
            .map(Path::toString)
            .forEach(command::add);
        command.add(outputFile.toString());
        return command.toArray(new String[0]);
    }

}
